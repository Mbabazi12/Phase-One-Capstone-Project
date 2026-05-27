package com.igirepay.lab1.service;

import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidAmountException;
import com.igirepay.lab1.exceptions.InvalidPinException;
import com.igirepay.lab1.exceptions.WithdrawalLimitExceededException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.SavingsAccount;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab1.model.WalletAccount;
import com.igirepay.lab2.config.DBConnection;
import com.igirepay.lab2.dao.AccountDAO;
import com.igirepay.lab2.dao.TransactionDAO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TransactionService {
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public TransactionService() {
        this.accountDAO = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    public Transaction deposit(Account account, BigDecimal amount, String referenceId) {
        Connection connection = beginTransaction();
        try {
            String ref = requireReference(referenceId);
            Account working = loadAccount(account);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);

            Transaction transaction = working.deposit(normalizedAmount, ref);
            accountDAO.updateBalance(working.getAccountId(), working.getBalance());
            transactionDAO.create(transaction);

            commit(connection);
            syncAccount(account, working);
            return transaction;
        } catch (RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            finishTransaction(connection);
        }
    }

    public Transaction withdraw(Account account, BigDecimal amount, String pin, String referenceId) {
        Connection connection = beginTransaction();
        try {
            String ref = requireReference(referenceId);
            Account working = loadAccount(account);
            validateAccountPin(working, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            ensureSufficientBalance(working, normalizedAmount);
            ensureSavingsWithdrawalLimit(working);

            Transaction transaction = working.withdraw(normalizedAmount, ref);
            accountDAO.updateBalance(working.getAccountId(), working.getBalance());
            transactionDAO.create(transaction);

            commit(connection);
            syncAccount(account, working);
            return transaction;
        } catch (RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            finishTransaction(connection);
        }
    }

    /**
     * Transfer between wallet accounts of different customers.
     * Savings accounts cannot transfer to other users.
     */
    public List<Transaction> transfer(Account sender, String recipientPhone, BigDecimal amount, String pin,
                                      String referenceId, CustomerService customerService) {
        if (sender.getAccountType() == AccountType.SAVINGS) {
            throw new IllegalArgumentException("Savings accounts cannot transfer money to other users.");
        }

        Connection connection = beginTransaction();
        try {
            String ref = requireReference(referenceId);
            Account workingSender = loadAccount(sender);
            Customer recipient = findRecipient(recipientPhone, customerService);

            if (workingSender.getCustomerId().equals(recipient.getCustomerId())) {
                throw new IllegalArgumentException("Cannot transfer to yourself. Use savings deposit instead.");
            }

            Account recipientWallet = recipient.getAccountByType(AccountType.WALLET)
                    .orElseThrow(() -> new AccountNotFoundException(recipientPhone + " wallet account"));
            Account workingRecipient = loadAccount(recipientWallet);

            validateAccountPin(workingSender, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            BigDecimal fee = WalletAccount.calculateTransferFee(normalizedAmount);
            ensureSufficientBalance(workingSender, normalizedAmount.add(fee));

            List<Transaction> transactions = new ArrayList<>();

            Transaction transferOut = new Transaction(ref, workingSender.getAccountId(),
                    workingRecipient.getAccountId(), TransactionType.TRANSFER_OUT, normalizedAmount,
                    "Transfer to " + recipient.getFullName() + " (" + recipient.getPhoneNumber() + ")");
            workingSender.processTransaction(transferOut);
            transactionDAO.create(transferOut);
            transactions.add(transferOut);

            Transaction transferIn = new Transaction(ref, workingRecipient.getAccountId(),
                    workingSender.getAccountId(), TransactionType.TRANSFER_IN, normalizedAmount,
                    "Transfer from " + workingSender.getCustomerId());
            workingRecipient.processTransaction(transferIn);
            transactionDAO.create(transferIn);
            transactions.add(transferIn);

            Transaction feeTransaction = new Transaction(ref, workingSender.getAccountId(),
                    null, TransactionType.FEE, fee, "Transfer fee (1% of " + normalizedAmount.toPlainString() + ")");
            workingSender.processTransaction(feeTransaction);
            transactionDAO.create(feeTransaction);
            transactions.add(feeTransaction);

            accountDAO.updateBalance(workingSender.getAccountId(), workingSender.getBalance());
            accountDAO.updateBalance(workingRecipient.getAccountId(), workingRecipient.getBalance());

            commit(connection);
            syncAccount(sender, workingSender);
            return Collections.unmodifiableList(transactions);
        } catch (RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            finishTransaction(connection);
        }
    }

    /**
     * Move money from wallet to own savings account.
     */
    public Transaction moveToSavings(Account walletAccount, Account savingsAccount,
                                     BigDecimal amount, String pin, String referenceId) {
        if (walletAccount.getAccountType() != AccountType.WALLET) {
            throw new IllegalArgumentException("Source must be a wallet account.");
        }
        if (savingsAccount.getAccountType() != AccountType.SAVINGS) {
            throw new IllegalArgumentException("Destination must be a savings account.");
        }
        if (!walletAccount.getCustomerId().equals(savingsAccount.getCustomerId())) {
            throw new IllegalArgumentException("Both accounts must belong to the same customer.");
        }

        Connection connection = beginTransaction();
        try {
            String ref = requireReference(referenceId);
            Account workingWallet = loadAccount(walletAccount);
            Account workingSavings = loadAccount(savingsAccount);

            validateAccountPin(workingWallet, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            ensureSufficientBalance(workingWallet, normalizedAmount);

            Transaction out = new Transaction(ref, workingWallet.getAccountId(),
                    workingSavings.getAccountId(), TransactionType.TRANSFER_OUT, normalizedAmount,
                    "Move to savings");
            workingWallet.processTransaction(out);
            transactionDAO.create(out);

            Transaction in = new Transaction(ref, workingSavings.getAccountId(),
                    workingWallet.getAccountId(), TransactionType.TRANSFER_IN, normalizedAmount,
                    "Received from wallet");
            workingSavings.processTransaction(in);
            transactionDAO.create(in);

            accountDAO.updateBalance(workingWallet.getAccountId(), workingWallet.getBalance());
            accountDAO.updateBalance(workingSavings.getAccountId(), workingSavings.getBalance());

            commit(connection);
            syncAccount(walletAccount, workingWallet);
            syncAccount(savingsAccount, workingSavings);
            return out;
        } catch (RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            finishTransaction(connection);
        }
    }

    /**
     * Move money from savings back to own wallet account.
     */
    public Transaction moveToWallet(Account savingsAccount, Account walletAccount,
                                    BigDecimal amount, String pin, String referenceId) {
        if (savingsAccount.getAccountType() != AccountType.SAVINGS) {
            throw new IllegalArgumentException("Source must be a savings account.");
        }
        if (walletAccount.getAccountType() != AccountType.WALLET) {
            throw new IllegalArgumentException("Destination must be a wallet account.");
        }
        if (!savingsAccount.getCustomerId().equals(walletAccount.getCustomerId())) {
            throw new IllegalArgumentException("Both accounts must belong to the same customer.");
        }

        Connection connection = beginTransaction();
        try {
            String ref = requireReference(referenceId);
            Account workingSavings = loadAccount(savingsAccount);
            Account workingWallet = loadAccount(walletAccount);

            validateAccountPin(workingSavings, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            ensureSufficientBalance(workingSavings, normalizedAmount);
            ensureSavingsWithdrawalLimit(workingSavings);

            Transaction out = new Transaction(ref, workingSavings.getAccountId(),
                    workingWallet.getAccountId(), TransactionType.TRANSFER_OUT, normalizedAmount,
                    "Move to wallet");
            workingSavings.processTransaction(out);
            transactionDAO.create(out);

            Transaction in = new Transaction(ref, workingWallet.getAccountId(),
                    workingSavings.getAccountId(), TransactionType.TRANSFER_IN, normalizedAmount,
                    "Received from savings");
            workingWallet.processTransaction(in);
            transactionDAO.create(in);

            accountDAO.updateBalance(workingSavings.getAccountId(), workingSavings.getBalance());
            accountDAO.updateBalance(workingWallet.getAccountId(), workingWallet.getBalance());

            commit(connection);
            syncAccount(savingsAccount, workingSavings);
            syncAccount(walletAccount, workingWallet);
            return out;
        } catch (RuntimeException exception) {
            rollback(connection, exception);
            throw exception;
        } finally {
            finishTransaction(connection);
        }
    }

    public String lookupRecipientName(String phone, CustomerService customerService) {
        return findRecipient(phone, customerService).getFullName();
    }

    public BigDecimal previewTransferFee(BigDecimal amount) {
        return WalletAccount.calculateTransferFee(requirePositiveAmount(amount));
    }

    public void validateAccountPin(Account account, String rawPin) {
        Account working = loadAccount(account);
        AuthService.validatePinFormat(rawPin);
        if (!AuthService.hashPin(rawPin).equals(working.getHashedPin())) {
            throw new InvalidPinException(0);
        }
    }

    public List<Transaction> getHistory(UUID accountId) {
        return Collections.unmodifiableList(transactionDAO.findByAccountId(accountId));
    }

    public List<Transaction> getDailyWithdrawals(UUID accountId, LocalDate date) {
        return Collections.unmodifiableList(transactionDAO.findDailyWithdrawals(accountId, date));
    }

    private String requireReference(String referenceId) {
        String normalized = referenceId == null ? "" : referenceId.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Reference ID is required.");
        return normalized;
    }

    private Customer findRecipient(String recipientPhone, CustomerService customerService) {
        if (customerService == null) throw new AccountNotFoundException("customer service");
        String normalizedPhone = AuthService.validatePhone(recipientPhone);
        return customerService.findByPhone(normalizedPhone)
                .orElseThrow(() -> new AccountNotFoundException(normalizedPhone));
    }

    private void ensureSavingsWithdrawalLimit(Account account) {
        if (account instanceof SavingsAccount &&
                transactionDAO.findDailyWithdrawals(account.getAccountId(), LocalDate.now()).size()
                        >= SavingsAccount.DAILY_WITHDRAWAL_LIMIT) {
            throw new WithdrawalLimitExceededException(SavingsAccount.DAILY_WITHDRAWAL_LIMIT);
        }
    }

    private void ensureSufficientBalance(Account account, BigDecimal required) {
        if (account.getBalance().compareTo(required) < 0) {
            throw new InsufficientBalanceException(required, account.getBalance());
        }
    }

    private Account loadAccount(Account account) {
        if (account == null) throw new AccountNotFoundException("account");
        return accountDAO.findById(account.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(account.getAccountId())));
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidAmountException("Amount must be greater than zero.");
        return normalized;
    }

    private void syncAccount(Account target, Account source) {
        if (target == null || source == null) return;
        target.setBalance(source.getBalance());
        target.setActive(source.isActive());
        target.setHashedPin(source.getHashedPin());
        target.setCreatedAt(source.getCreatedAt());
    }

    private Connection beginTransaction() {
        Connection connection = DBConnection.getConnection();
        DBConnection.bindTransactionConnection(connection);
        try {
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException exception) {
            DBConnection.clearTransactionConnection();
            closeConnection(connection);
            throw new DatabaseException("Could not start database transaction", exception);
        }
    }

    private void commit(Connection connection) {
        try { connection.commit(); }
        catch (SQLException e) { throw new DatabaseException("Could not commit database transaction", e); }
    }

    private void rollback(Connection connection, RuntimeException original) {
        try { connection.rollback(); }
        catch (SQLException e) { original.addSuppressed(new DatabaseException("Could not roll back", e)); }
    }

    private void finishTransaction(Connection connection) {
        try { connection.setAutoCommit(true); }
        catch (SQLException e) { throw new DatabaseException("Could not reset transaction state", e); }
        finally { DBConnection.clearTransactionConnection(); closeConnection(connection); }
    }

    private void closeConnection(Connection connection) {
        try { connection.close(); }
        catch (SQLException e) { throw new DatabaseException("Could not close database connection", e); }
    }
}

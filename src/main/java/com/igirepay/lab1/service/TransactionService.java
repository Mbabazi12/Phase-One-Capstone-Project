package com.igirepay.lab1.service;

import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.DuplicateTransactionException;
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
import com.igirepay.lab2.dao.ProcessedRequestDAO;
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
    private final ProcessedRequestDAO processedRequestDAO;

    public TransactionService() {
        this.accountDAO = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
        this.processedRequestDAO = new ProcessedRequestDAO();
    }

    public Transaction deposit(Account account, BigDecimal amount, String referenceId) {
        Connection connection = beginTransaction();
        try {
            String ref = requireNewReference(referenceId);
            requireNotProcessed(ref);
            Account working = loadAccount(account);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);

            Transaction transaction = working.deposit(normalizedAmount, ref);
            accountDAO.updateBalance(working.getAccountId(), working.getBalance());
            transactionDAO.create(transaction);
            processedRequestDAO.insert(ref);

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
            String ref = requireNewReference(referenceId);
            requireNotProcessed(ref);
            Account working = loadAccount(account);
            validateAccountPin(working, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            ensureSufficientBalance(working, normalizedAmount);
            ensureSavingsWithdrawalLimit(working);

            Transaction transaction = working.withdraw(normalizedAmount, ref);
            accountDAO.updateBalance(working.getAccountId(), working.getBalance());
            transactionDAO.create(transaction);
            processedRequestDAO.insert(ref);

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

    public List<Transaction> transfer(Account sender, String recipientPhone, BigDecimal amount, String pin,
                                      String referenceId, CustomerService customerService) {
        Connection connection = beginTransaction();
        try {
            String ref = requireNewReference(referenceId);
            requireNotProcessed(ref);

            Account workingSender = loadAccount(sender);
            Customer recipient = findRecipient(recipientPhone, customerService);
            Account recipientAccount = loadAccount(resolveRecipientAccount(workingSender, recipient));

            validateAccountPin(workingSender, pin);
            BigDecimal normalizedAmount = requirePositiveAmount(amount);
            BigDecimal fee = calculateTransferFee(workingSender, recipient, normalizedAmount);
            ensureSufficientBalance(workingSender, normalizedAmount.add(fee));

            List<Transaction> transactions = new ArrayList<>();

            Transaction transferOut = new Transaction(ref, workingSender.getAccountId(),
                    recipientAccount.getAccountId(), TransactionType.TRANSFER_OUT, normalizedAmount,
                    "Transfer to " + recipient.getFullName() + " (" + recipient.getPhoneNumber() + ")");
            workingSender.processTransaction(transferOut);
            transactionDAO.create(transferOut);
            transactions.add(transferOut);

            Transaction transferIn = new Transaction(ref, recipientAccount.getAccountId(),
                    workingSender.getAccountId(), TransactionType.TRANSFER_IN, normalizedAmount,
                    "Transfer from account " + workingSender.getAccountId());
            recipientAccount.processTransaction(transferIn);
            transactionDAO.create(transferIn);
            transactions.add(transferIn);

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                Transaction feeTransaction = new Transaction(ref, workingSender.getAccountId(),
                        null, TransactionType.FEE, fee, "Transfer fee");
                workingSender.processTransaction(feeTransaction);
                transactionDAO.create(feeTransaction);
                transactions.add(feeTransaction);
            }

            accountDAO.updateBalance(workingSender.getAccountId(), workingSender.getBalance());
            accountDAO.updateBalance(recipientAccount.getAccountId(), recipientAccount.getBalance());
            processedRequestDAO.insert(ref);

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

    public String lookupRecipientName(String phone, CustomerService customerService) {
        return findRecipient(phone, customerService).getFullName();
    }

    public BigDecimal previewTransferFee(Account sender, String recipientPhone, BigDecimal amount,
                                         CustomerService customerService) {
        Account workingSender = loadAccount(sender);
        Customer recipient = findRecipient(recipientPhone, customerService);
        resolveRecipientAccount(workingSender, recipient);
        return calculateTransferFee(workingSender, recipient, requirePositiveAmount(amount));
    }

    public BigDecimal previewTransferTotal(Account sender, String recipientPhone, BigDecimal amount,
                                           CustomerService customerService) {
        BigDecimal normalizedAmount = requirePositiveAmount(amount);
        return normalizedAmount.add(previewTransferFee(sender, recipientPhone, normalizedAmount, customerService));
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

    public boolean isReferenceProcessed(String referenceId) {
        String normalized = referenceId == null ? "" : referenceId.trim();
        return processedRequestDAO.exists(normalized);
    }

    private String requireNewReference(String referenceId) {
        String normalized = referenceId == null ? "" : referenceId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Reference ID is required.");
        }
        return normalized;
    }

    private void requireNotProcessed(String referenceId) {
        if (processedRequestDAO.exists(referenceId)) {
            throw new DuplicateTransactionException(referenceId);
        }
    }

    private Customer findRecipient(String recipientPhone, CustomerService customerService) {
        if (customerService == null) {
            throw new AccountNotFoundException("customer service");
        }
        String normalizedPhone = AuthService.validatePhone(recipientPhone);
        return customerService.findByPhone(normalizedPhone)
                .orElseThrow(() -> new AccountNotFoundException(normalizedPhone));
    }

    private Account resolveRecipientAccount(Account sender, Customer recipient) {
        boolean sameOwner = sender.getCustomerId().equals(recipient.getCustomerId());
        AccountType targetType = sameOwner && sender.getAccountType() == AccountType.WALLET
                ? AccountType.SAVINGS
                : AccountType.WALLET;
        return recipient.getAccountByType(targetType)
                .orElseThrow(() -> new AccountNotFoundException(
                        recipient.getPhoneNumber() + " " + targetType.name().toLowerCase() + " account"
                ));
    }

    private BigDecimal calculateTransferFee(Account sender, Customer recipient, BigDecimal amount) {
        if (sender.getCustomerId().equals(recipient.getCustomerId())) {
            return BigDecimal.ZERO;
        }
        return WalletAccount.calculateTransferFee(amount);
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
        if (account == null) {
            throw new AccountNotFoundException("account");
        }
        return accountDAO.findById(account.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(account.getAccountId())));
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        return normalized;
    }

    private void syncAccount(Account target, Account source) {
        if (target == null || source == null) {
            return;
        }
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
        try {
            connection.commit();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not commit database transaction", exception);
        }
    }

    private void rollback(Connection connection, RuntimeException originalException) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            originalException.addSuppressed(new DatabaseException("Could not roll back database transaction", exception));
        }
    }

    private void finishTransaction(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not reset database transaction state", exception);
        } finally {
            DBConnection.clearTransactionConnection();
            closeConnection(connection);
        }
    }

    private void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not close database connection", exception);
        }
    }
}

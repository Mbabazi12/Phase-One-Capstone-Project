package com.igirepay.lab2.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidAmountException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Loan;
import com.igirepay.lab1.model.Loan.LoanStatus;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab2.config.DBConnection;
import com.igirepay.lab2.dao.AccountDAO;
import com.igirepay.lab2.dao.LoanDAO;
import com.igirepay.lab2.dao.TransactionDAO;

public class LoanService {

    private final LoanDAO        loanDAO;
    private final AccountDAO     accountDAO;
    private final TransactionDAO transactionDAO;

    public LoanService() {
        this.loanDAO        = new LoanDAO();
        this.accountDAO     = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Request a loan. Disburses the principal into the customer's wallet.
     * Limit: 100,000 RWF by default.
     * Premium limit: 500,000 RWF if total transaction volume > 300,000 RWF.
     * 10% flat interest applied on the principal.
     */
    public Loan requestLoan(Customer customer, BigDecimal amount) {
        validateAmount(amount);

        BigDecimal limit = resolveLoanLimit(customer);
        if (amount.compareTo(limit) > 0) {
            String hint = limit.compareTo(Loan.MAX_LOAN_AMOUNT) == 0
                    ? " Transact over 300,000 RWF total to unlock up to 500,000 RWF."
                    : "";
            throw new InvalidAmountException(
                    "Loan amount cannot exceed " + limit.toPlainString() + " RWF." + hint);
        }

        if (loanDAO.findActiveLoan(customer.getCustomerId()).isPresent()) {
            throw new IllegalStateException(
                    "You already have an active loan. Repay it fully before requesting a new one.");
        }

        Account wallet = customer.getAccountByType(AccountType.WALLET)
                .orElseThrow(() -> new IllegalStateException(
                        "You need a wallet account to receive a loan."));
        Account workingWallet = accountDAO.findById(wallet.getAccountId())
                .orElseThrow(() -> new IllegalStateException("Wallet account not found."));

        Connection connection = beginTransaction();
        try {
            Loan loan = Loan.create(customer.getCustomerId(), amount);
            loanDAO.create(loan);

            workingWallet.setBalance(workingWallet.getBalance().add(amount));
            accountDAO.updateBalance(workingWallet.getAccountId(), workingWallet.getBalance());

            transactionDAO.create(new Transaction(
                    UUID.randomUUID().toString(),
                    workingWallet.getAccountId(), 0,
                    TransactionType.LOAN_DISBURSEMENT, amount,
                    "Loan disbursement (loan #" + loan.getLoanId() + ")"));

            commit(connection);
            syncAccount(wallet, workingWallet);
            return loan;
        } catch (RuntimeException e) {
            rollback(connection, e);
            throw e;
        } finally {
            finishTransaction(connection);
        }
    }

    /**
     * Repay part or all of the active loan from the customer's wallet.
     * Caps repayment at remaining balance so the customer cannot overpay.
     */
    public Loan repayLoan(Customer customer, BigDecimal amount) {
        validateAmount(amount);

        Loan loan = loanDAO.findActiveLoan(customer.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("No active loan found."));

        BigDecimal repayAmount = amount.compareTo(loan.getRemainingBalance()) > 0
                ? loan.getRemainingBalance() : amount;

        Account wallet = customer.getAccountByType(AccountType.WALLET)
                .orElseThrow(() -> new IllegalStateException("Wallet account not found."));
        Account workingWallet = accountDAO.findById(wallet.getAccountId())
                .orElseThrow(() -> new IllegalStateException("Wallet account not found."));

        if (workingWallet.getBalance().compareTo(repayAmount) < 0) {
            throw new InsufficientBalanceException(repayAmount, workingWallet.getBalance());
        }

        Connection connection = beginTransaction();
        try {
            workingWallet.setBalance(workingWallet.getBalance().subtract(repayAmount));
            accountDAO.updateBalance(workingWallet.getAccountId(), workingWallet.getBalance());

            BigDecimal newPaid = loan.getAmountPaid().add(repayAmount)
                    .setScale(4, RoundingMode.HALF_UP);
            loan.setAmountPaid(newPaid);
            LoanStatus newStatus = loan.isFullyPaid() ? LoanStatus.FULLY_PAID : LoanStatus.ACTIVE;
            loan.setStatus(newStatus);
            loanDAO.updateRepayment(loan.getLoanId(), newPaid, newStatus);

            transactionDAO.create(new Transaction(
                    UUID.randomUUID().toString(),
                    workingWallet.getAccountId(), 0,
                    TransactionType.LOAN_REPAYMENT, repayAmount,
                    "Loan repayment (loan #" + loan.getLoanId() + ")"));

            commit(connection);
            syncAccount(wallet, workingWallet);
            return loan;
        } catch (RuntimeException e) {
            rollback(connection, e);
            throw e;
        } finally {
            finishTransaction(connection);
        }
    }

    public Optional<Loan> getActiveLoan(int customerId) {
        return loanDAO.findActiveLoan(customerId);
    }

    public List<Loan> getLoanHistory(int customerId) {
        return loanDAO.findByCustomerId(customerId);
    }

    /**
     * Returns the applicable loan limit for a customer.
     * 100,000 RWF by default; 500,000 RWF if total transaction volume > 300,000 RWF.
     */
    public BigDecimal getLoanLimit(Customer customer) {
        return resolveLoanLimit(customer);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private BigDecimal resolveLoanLimit(Customer customer) {
        BigDecimal totalVolume = customer.getAccounts().stream()
                .flatMap(a -> transactionDAO.findByAccountId(a.getAccountId()).stream())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalVolume.compareTo(Loan.PREMIUM_TX_THRESHOLD) > 0
                ? Loan.PREMIUM_LOAN_AMOUNT
                : Loan.MAX_LOAN_AMOUNT;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
    }

    private void syncAccount(Account target, Account source) {
        if (target == null || source == null) return;
        target.setBalance(source.getBalance());
    }

    private Connection beginTransaction() {
        Connection connection = DBConnection.getConnection();
        DBConnection.bindTransactionConnection(connection);
        try {
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            DBConnection.clearTransactionConnection();
            closeConnection(connection);
            throw new DatabaseException("Could not start transaction", e);
        }
    }

    private void commit(Connection connection) {
        try { connection.commit(); }
        catch (SQLException e) { throw new DatabaseException("Could not commit", e); }
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
        catch (SQLException e) { throw new DatabaseException("Could not close connection", e); }
    }
}

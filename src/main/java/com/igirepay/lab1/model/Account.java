package com.igirepay.lab1.model;

import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidAmountException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Account {
    private int accountId;
    private int customerId;
    private AccountType accountType;
    private String accountName;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private boolean active;
    private String hashedPin;
    private List<Transaction> transactionHistory;

    protected Account(AccountType accountType, int customerId, String hashedPin) {
        this(0, customerId, accountType, accountType.name(), BigDecimal.ZERO, LocalDateTime.now(), true, hashedPin);
    }

    protected Account(int accountId, int customerId, AccountType accountType, BigDecimal balance,
                      LocalDateTime createdAt, boolean active, String hashedPin) {
        this(accountId, customerId, accountType, accountType.name(), balance, createdAt, active, hashedPin);
    }

    protected Account(int accountId, int customerId, AccountType accountType, String accountName,
                      BigDecimal balance, LocalDateTime createdAt, boolean active, String hashedPin) {
        this.accountId = accountId;
        this.customerId = customerId;
        setAccountType(accountType);
        setAccountName(accountName);
        setBalance(balance);
        setCreatedAt(createdAt);
        setActive(active);
        setHashedPin(hashedPin);
        this.transactionHistory = new ArrayList<>();
    }

    public abstract Transaction withdraw(BigDecimal amount, String referenceId);

    public abstract Transaction deposit(BigDecimal amount, String referenceId);

    public abstract Transaction processTransaction(Transaction transaction);

    public int getAccountId() { return accountId; }

    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getCustomerId() { return customerId; }

    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getAccountName() { return accountName; }

    public void setAccountName(String accountName) {
        this.accountName = accountName == null || accountName.isBlank()
                ? (accountType != null ? accountType.name() : "ACCOUNT")
                : accountName.trim();
    }

    public AccountType getAccountType() { return accountType; }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType == null ? AccountType.WALLET : accountType;
    }

    public BigDecimal getBalance() { return balance; }

    public void setBalance(BigDecimal balance) {
        BigDecimal normalized = normalizeMoney(balance);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Balance cannot be negative.");
        }
        this.balance = normalized;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public String getHashedPin() { return hashedPin; }

    public void setHashedPin(String hashedPin) {
        this.hashedPin = hashedPin == null ? "" : hashedPin.trim();
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public void setTransactionHistory(List<Transaction> transactionHistory) {
        this.transactionHistory = new ArrayList<>();
        if (transactionHistory != null) {
            for (Transaction transaction : transactionHistory) {
                addTransaction(transaction);
            }
        }
    }

    public void addTransaction(Transaction transaction) {
        if (transaction != null) transactionHistory.add(transaction);
    }

    protected void requireActive() {
        if (!active) throw new IllegalStateException("Account is inactive.");
    }

    protected BigDecimal requireValidAmount(BigDecimal amount) {
        BigDecimal normalized = normalizeMoney(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        return normalized;
    }

    protected void credit(BigDecimal amount) {
        balance = normalizeMoney(balance.add(amount));
    }

    protected void debit(BigDecimal amount) {
        BigDecimal normalized = normalizeMoney(amount);
        if (balance.compareTo(normalized) < 0) {
            throw new InsufficientBalanceException(normalized, balance);
        }
        balance = normalizeMoney(balance.subtract(normalized));
    }

    protected Transaction createTransaction(String referenceId, TransactionType transactionType,
                                            BigDecimal amount, String description) {
        return new Transaction(
                0,
                referenceId,
                accountId,
                0,
                transactionType,
                amount,
                BigDecimal.ZERO,
                TransactionStatus.SUCCESS,
                LocalDateTime.now(),
                description
        );
    }

    protected BigDecimal normalizeMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
    }

    @Override
    public String toString() {
        return "Account{accountId=" + accountId + ", customerId=" + customerId
                + ", accountType=" + accountType + ", balance=" + balance
                + ", isActive=" + active + '}';
    }
}

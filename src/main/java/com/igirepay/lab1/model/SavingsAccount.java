package com.igirepay.lab1.model;

import com.igirepay.lab1.exceptions.InvalidAmountException;
import com.igirepay.lab1.exceptions.WithdrawalLimitExceededException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class SavingsAccount extends Account {
    public static final int DAILY_WITHDRAWAL_LIMIT = 3;

    public SavingsAccount(UUID customerId, String hashedPin) {
        super(AccountType.SAVINGS, customerId, hashedPin);
    }

    public SavingsAccount(UUID accountId, UUID customerId, BigDecimal balance, String hashedPin) {
        super(accountId, customerId, AccountType.SAVINGS, balance, LocalDateTime.now(), true, hashedPin);
    }

    @Override
    public Transaction withdraw(BigDecimal amount, String referenceId) {
        ensureDailyWithdrawalLimit();
        Transaction transaction = createTransaction(referenceId, TransactionType.WITHDRAWAL,
                requireValidAmount(amount), "Savings withdrawal");
        return processTransaction(transaction);
    }

    @Override
    public Transaction deposit(BigDecimal amount, String referenceId) {
        Transaction transaction = createTransaction(referenceId, TransactionType.DEPOSIT,
                requireValidAmount(amount), "Savings deposit");
        return processTransaction(transaction);
    }

    @Override
    public Transaction processTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new InvalidAmountException("Transaction is required.");
        }
        requireActive();
        BigDecimal amount = requireValidAmount(transaction.getAmount());
        transaction.setAccountId(getAccountId());
        transaction.setAmount(amount);

        switch (transaction.getTransactionType()) {
            case DEPOSIT, TRANSFER_IN -> credit(amount);
            case WITHDRAWAL -> {
                ensureDailyWithdrawalLimit();
                debit(amount);
            }
            case TRANSFER_OUT, FEE -> debit(amount);
        }

        addTransaction(transaction);
        return transaction;
    }

    private void ensureDailyWithdrawalLimit() {
        LocalDate today = LocalDate.now();
        long withdrawalsToday = getTransactionHistory().stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .filter(transaction -> transaction.getTimestamp().toLocalDate().equals(today))
                .count();
        if (withdrawalsToday >= DAILY_WITHDRAWAL_LIMIT) {
            throw new WithdrawalLimitExceededException(DAILY_WITHDRAWAL_LIMIT);
        }
    }

    @Override
    public String toString() {
        return "SavingsAccount{" +
                "accountId=" + getAccountId() +
                ", customerId=" + getCustomerId() +
                ", accountType=" + getAccountType() +
                ", balance=" + getBalance() +
                ", createdAt=" + getCreatedAt() +
                ", isActive=" + isActive() +
                ", dailyWithdrawalLimit=" + DAILY_WITHDRAWAL_LIMIT +
                ", transactionCount=" + getTransactionHistory().size() +
                '}';
    }
}

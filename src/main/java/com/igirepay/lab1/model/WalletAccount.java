package com.igirepay.lab1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class WalletAccount extends Account {
    public static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");
    public static final BigDecimal MIN_TRANSFER_FEE = new BigDecimal("10");
    public static final BigDecimal MAX_TRANSFER_FEE = new BigDecimal("500");

    public WalletAccount(UUID customerId, String hashedPin) {
        super(AccountType.WALLET, customerId, hashedPin);
    }

    public WalletAccount(UUID accountId, UUID customerId, BigDecimal balance, String hashedPin) {
        super(accountId, customerId, AccountType.WALLET, balance, LocalDateTime.now(), true, hashedPin);
    }

    @Override
    public Transaction withdraw(BigDecimal amount, String referenceId) {
        Transaction transaction = createTransaction(referenceId, TransactionType.WITHDRAWAL,
                requireValidAmount(amount), "Wallet withdrawal");
        return processTransaction(transaction);
    }

    @Override
    public Transaction deposit(BigDecimal amount, String referenceId) {
        Transaction transaction = createTransaction(referenceId, TransactionType.DEPOSIT,
                requireValidAmount(amount), "Wallet deposit");
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
            case WITHDRAWAL, TRANSFER_OUT, FEE -> debit(amount);
        }

        addTransaction(transaction);
        return transaction;
    }

    public static BigDecimal calculateTransferFee(BigDecimal amount) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        BigDecimal fee = normalizedAmount.multiply(TRANSFER_FEE_RATE).stripTrailingZeros();
        if (fee.compareTo(MIN_TRANSFER_FEE) < 0) {
            return MIN_TRANSFER_FEE;
        }
        if (fee.compareTo(MAX_TRANSFER_FEE) > 0) {
            return MAX_TRANSFER_FEE;
        }
        return fee;
    }

    @Override
    public String toString() {
        return "WalletAccount{" +
                "accountId=" + getAccountId() +
                ", customerId=" + getCustomerId() +
                ", accountType=" + getAccountType() +
                ", balance=" + getBalance() +
                ", createdAt=" + getCreatedAt() +
                ", isActive=" + isActive() +
                ", transactionCount=" + getTransactionHistory().size() +
                '}';
    }
}

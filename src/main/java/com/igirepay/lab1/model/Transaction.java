package com.igirepay.lab1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private int transactionId;
    private String referenceId;
    private int accountId;
    private int targetAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private String description;

    public Transaction() {
        this(0, "", 0, 0, TransactionType.DEPOSIT, BigDecimal.ZERO,
                BigDecimal.ZERO, TransactionStatus.SUCCESS, LocalDateTime.now(), "");
    }

    public Transaction(String referenceId, int accountId, int targetAccountId,
                       TransactionType transactionType, BigDecimal amount, String description) {
        this(0, referenceId, accountId, targetAccountId, transactionType, amount,
                BigDecimal.ZERO, TransactionStatus.SUCCESS, LocalDateTime.now(), description);
    }

    public Transaction(int transactionId, String referenceId, int accountId, int targetAccountId,
                       TransactionType transactionType, BigDecimal amount, BigDecimal fee,
                       TransactionStatus status, LocalDateTime timestamp, String description) {
        this.transactionId = transactionId;
        setReferenceId(referenceId);
        this.accountId = accountId;
        this.targetAccountId = targetAccountId;
        setTransactionType(transactionType);
        setAmount(amount);
        setFee(fee);
        setStatus(status);
        setTimestamp(timestamp);
        setDescription(description);
    }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId == null ? "" : referenceId.trim();
    }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getTargetAccountId() { return targetAccountId; }
    public void setTargetAccountId(int targetAccountId) { this.targetAccountId = targetAccountId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType == null ? TransactionType.DEPOSIT : transactionType;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
    }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) {
        this.fee = fee == null ? BigDecimal.ZERO : fee.stripTrailingZeros();
    }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) {
        this.status = status == null ? TransactionStatus.SUCCESS : status;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    @Override
    public String toString() {
        return "Transaction{transactionId=" + transactionId + ", referenceId='" + referenceId
                + "', accountId=" + accountId + ", targetAccountId=" + targetAccountId
                + ", type=" + transactionType + ", amount=" + amount + ", status=" + status + '}';
    }
}

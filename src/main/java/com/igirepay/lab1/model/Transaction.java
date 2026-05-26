package com.igirepay.lab1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    private UUID transactionId;
    private String referenceId;
    private UUID accountId;
    private UUID targetAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private String description;

    public Transaction() {
        this(UUID.randomUUID(), "", null, null, TransactionType.DEPOSIT, BigDecimal.ZERO,
                BigDecimal.ZERO, TransactionStatus.SUCCESS, LocalDateTime.now(), "");
    }

    public Transaction(String referenceId, UUID accountId, UUID targetAccountId,
                       TransactionType transactionType, BigDecimal amount, String description) {
        this(UUID.randomUUID(), referenceId, accountId, targetAccountId, transactionType, amount,
                BigDecimal.ZERO, TransactionStatus.SUCCESS, LocalDateTime.now(), description);
    }

    public Transaction(UUID transactionId, String referenceId, UUID accountId, UUID targetAccountId,
                       TransactionType transactionType, BigDecimal amount, BigDecimal fee,
                       TransactionStatus status, LocalDateTime timestamp, String description) {
        setTransactionId(transactionId);
        setReferenceId(referenceId);
        setAccountId(accountId);
        setTargetAccountId(targetAccountId);
        setTransactionType(transactionType);
        setAmount(amount);
        setFee(fee);
        setStatus(status);
        setTimestamp(timestamp);
        setDescription(description);
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId == null ? UUID.randomUUID() : transactionId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId == null ? "" : referenceId.trim();
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(UUID targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType == null ? TransactionType.DEPOSIT : transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee == null ? BigDecimal.ZERO : fee.stripTrailingZeros();
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status == null ? TransactionStatus.SUCCESS : status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", referenceId='" + referenceId + '\'' +
                ", accountId=" + accountId +
                ", targetAccountId=" + targetAccountId +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", fee=" + fee +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
}

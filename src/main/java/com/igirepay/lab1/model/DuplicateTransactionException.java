package com.igirepay.lab1.model;

public class DuplicateTransactionException extends RuntimeException {
    private final String referenceId;

    public DuplicateTransactionException(String referenceId) {
        super("Transaction reference has already been processed: " + referenceId);
        this.referenceId = referenceId;
    }

    public String getReferenceId() {
        return referenceId;
    }
}

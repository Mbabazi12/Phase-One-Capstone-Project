package com.igirepay.lab1.model;

public class InvalidAmountException extends RuntimeException {
    private final String reason;

    public InvalidAmountException(String reason) {
        super("Invalid amount: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}

package com.igirepay.lab1.exceptions;

public class InvalidPinFormatException extends RuntimeException {
    private final String reason;

    public InvalidPinFormatException(String reason) {
        super("Invalid PIN format: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}

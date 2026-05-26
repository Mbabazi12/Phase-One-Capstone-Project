package com.igirepay.lab1.exceptions;

public class InvalidPinException extends RuntimeException {
    private final int attemptsRemaining;

    public InvalidPinException(int attemptsRemaining) {
        super("Invalid PIN. Attempts remaining: " + attemptsRemaining + ".");
        this.attemptsRemaining = attemptsRemaining;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }
}

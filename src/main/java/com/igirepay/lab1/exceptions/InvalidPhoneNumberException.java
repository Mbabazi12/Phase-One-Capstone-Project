package com.igirepay.lab1.exceptions;

public class InvalidPhoneNumberException extends RuntimeException {
    private final String input;

    public InvalidPhoneNumberException(String input) {
        super("Phone number must be exactly 10 numeric digits. Received: " + input);
        this.input = input;
    }

    public String getInput() {
        return input;
    }
}

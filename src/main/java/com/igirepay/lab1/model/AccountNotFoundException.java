package com.igirepay.lab1.model;

public class AccountNotFoundException extends RuntimeException {
    private final String identifier;

    public AccountNotFoundException(String identifier) {
        super("Account or customer not found for: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}

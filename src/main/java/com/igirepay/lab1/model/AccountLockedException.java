package com.igirepay.lab1.model;

public class AccountLockedException extends RuntimeException {
    private final String phoneNumber;

    public AccountLockedException(String phoneNumber) {
        super("Account is locked for phone number: " + phoneNumber + ".");
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}

package com.igirepay.lab1.model;

import com.igirepay.lab1.exceptions.InvalidPhoneNumberException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Customer {
    private UUID customerId;
    private String fullName;
    private String phoneNumber;
    private String hashedPin;
    private List<Account> accounts;
    private LocalDateTime createdAt;

    public Customer() {
        this(UUID.randomUUID(), "Unknown Customer", "0000000000", "", LocalDateTime.now());
    }

    public Customer(String fullName, String phoneNumber, String hashedPin) {
        this(UUID.randomUUID(), fullName, phoneNumber, hashedPin, LocalDateTime.now());
    }

    public Customer(UUID customerId, String fullName, String phoneNumber, String hashedPin,
                    LocalDateTime createdAt) {
        setCustomerId(customerId);
        setFullName(fullName);
        setPhoneNumber(phoneNumber);
        setHashedPin(hashedPin);
        setCreatedAt(createdAt);
        this.accounts = new ArrayList<>();
    }

    public UUID getCustomerId() { return customerId; }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId == null ? UUID.randomUUID() : customerId;
    }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Full name is required.");
        this.fullName = normalized;
    }

    public String getPhoneNumber() { return phoneNumber; }

    public void setPhoneNumber(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.trim();
        if (!normalized.matches("\\d{10}")) throw new InvalidPhoneNumberException(phoneNumber);
        this.phoneNumber = normalized;
    }

    public String getHashedPin() { return hashedPin; }

    public void setHashedPin(String hashedPin) {
        this.hashedPin = hashedPin == null ? "" : hashedPin.trim();
        for (Account account : accounts == null ? List.<Account>of() : accounts) {
            account.setHashedPin(this.hashedPin);
        }
    }

    public List<Account> getAccounts() { return Collections.unmodifiableList(accounts); }

    public void setAccounts(List<Account> accounts) {
        this.accounts = new ArrayList<>();
        if (accounts != null) {
            for (Account account : accounts) addAccount(account);
        }
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public void addAccount(Account account) {
        if (account == null) return;
        account.setCustomerId(customerId);
        account.setHashedPin(hashedPin);
        accounts.add(account);
    }

    public Optional<Account> getAccountByType(AccountType accountType) {
        return accounts.stream().filter(a -> a.getAccountType() == accountType).findFirst();
    }

    public Optional<Account> getAccountById(UUID accountId) {
        return accounts.stream().filter(a -> a.getAccountId().equals(accountId)).findFirst();
    }

    public boolean validatePin(String rawPin) {
        String normalized = rawPin == null ? "" : rawPin.trim();
        return hashedPin.equals(normalized);
    }

    @Override
    public String toString() {
        return "Customer{customerId=" + customerId + ", fullName='" + fullName + "', phoneNumber='" + phoneNumber + "'}";
    }
}

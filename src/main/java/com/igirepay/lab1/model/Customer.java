package com.igirepay.lab1.model;

import com.igirepay.lab1.exceptions.InvalidPhoneNumberException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Customer {
    private UUID customerId;
    private String fullName;
    private String phoneNumber;
    private String hashedPin;
    private List<Account> accounts;
    private boolean locked;
    private int failedPinAttempts;
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
        this.locked = false;
        this.failedPinAttempts = 0;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId == null ? UUID.randomUUID() : customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        String normalizedName = fullName == null ? "" : fullName.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        this.fullName = normalizedName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        String normalizedPhone = phoneNumber == null ? "" : phoneNumber.trim();
        if (!normalizedPhone.matches("\\d{10}")) {
            throw new InvalidPhoneNumberException(phoneNumber);
        }
        this.phoneNumber = normalizedPhone;
    }

    public String getHashedPin() {
        return hashedPin;
    }

    public void setHashedPin(String hashedPin) {
        this.hashedPin = hashedPin == null ? "" : hashedPin.trim();
        for (Account account : accounts == null ? List.<Account>of() : accounts) {
            account.setHashedPin(this.hashedPin);
        }
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = new ArrayList<>();
        if (accounts != null) {
            for (Account account : accounts) {
                addAccount(account);
            }
        }
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getFailedPinAttempts() {
        return failedPinAttempts;
    }

    public void setFailedPinAttempts(int failedPinAttempts) {
        this.failedPinAttempts = Math.max(0, failedPinAttempts);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public void addAccount(Account account) {
        if (account == null) {
            return;
        }
        account.setCustomerId(customerId);
        account.setHashedPin(hashedPin);
        accounts.add(account);
    }

    public Optional<Account> getAccountByType(AccountType accountType) {
        return accounts.stream()
                .filter(account -> account.getAccountType() == accountType)
                .findFirst();
    }

    public Optional<Account> getAccountById(UUID accountId) {
        return accounts.stream()
                .filter(account -> account.getAccountId().equals(accountId))
                .findFirst();
    }

    public void lockAccount() {
        locked = true;
    }

    public void resetFailedAttempts() {
        failedPinAttempts = 0;
    }

    public void recordFailedPinAttempt() {
        failedPinAttempts++;
    }

    public boolean validatePin(String rawPin) {
        return hashedPin.equals(hashPin(rawPin));
    }

    private String hashPin(String rawPin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((rawPin == null ? "" : rawPin).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing is unavailable.", exception);
        }
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", fullName='" + fullName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", hashedPin='" + hashedPin + '\'' +
                ", accountCount=" + accounts.size() +
                ", isLocked=" + locked +
                ", failedPinAttempts=" + failedPinAttempts +
                ", createdAt=" + createdAt +
                '}';
    }
}

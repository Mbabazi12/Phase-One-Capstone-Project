package com.igirepay.lab1.service;

import com.igirepay.lab1.model.AccountLockedException;
import com.igirepay.lab1.model.AccountNotFoundException;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.InvalidPhoneNumberException;
import com.igirepay.lab1.model.InvalidPinException;
import com.igirepay.lab1.model.InvalidPinFormatException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

public class AuthService {
    public static final int MAX_FAILED_PIN_ATTEMPTS = 3;

    private final CustomerService customerService;

    public AuthService(CustomerService customerService) {
        this.customerService = customerService == null ? new CustomerService() : customerService;
    }

    public Customer register(String fullName, String phone, String pin) {
        String normalizedPhone = validatePhone(phone);
        validatePinFormat(pin);
        if (customerService.findByPhone(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("A customer already exists with phone number " + normalizedPhone + ".");
        }

        Customer customer = new Customer(
                UUID.randomUUID(),
                fullName,
                normalizedPhone,
                hashPin(pin),
                LocalDateTime.now()
        );
        return customerService.save(customer);
    }

    public Customer login(String phone, String pin) {
        String normalizedPhone = validatePhone(phone);
        Customer customer = customerService.findByPhone(normalizedPhone)
                .orElseThrow(() -> new AccountNotFoundException(normalizedPhone));

        if (customer.isLocked()) {
            throw new AccountLockedException(normalizedPhone);
        }

        validatePinFormat(pin);
        if (!customer.validatePin(pin)) {
            customer.recordFailedPinAttempt();
            int attemptsRemaining = MAX_FAILED_PIN_ATTEMPTS - customer.getFailedPinAttempts();
            if (attemptsRemaining <= 0) {
                customer.lockAccount();
                throw new AccountLockedException(normalizedPhone);
            }
            throw new InvalidPinException(attemptsRemaining);
        }

        customer.resetFailedAttempts();
        return customer;
    }

    public static String validatePhone(String phone) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (!normalizedPhone.matches("\\d{10}")) {
            throw new InvalidPhoneNumberException(phone);
        }
        return normalizedPhone;
    }

    public static void validatePinFormat(String pin) {
        String normalizedPin = pin == null ? "" : pin.trim();
        if (!normalizedPin.matches("\\d{5}")) {
            throw new InvalidPinFormatException("PIN must be exactly 5 numeric digits.");
        }
    }

    public static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((pin == null ? "" : pin).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing is unavailable.", exception);
        }
    }
}

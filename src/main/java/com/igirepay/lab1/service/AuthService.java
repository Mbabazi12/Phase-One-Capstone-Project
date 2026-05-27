package com.igirepay.lab1.service;

import com.igirepay.lab1.exceptions.AccountLockedException;
import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.InvalidPhoneNumberException;
import com.igirepay.lab1.exceptions.InvalidPinException;
import com.igirepay.lab1.exceptions.InvalidPinFormatException;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab2.dao.CustomerDAO;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuthService {
    public static final int MAX_FAILED_PIN_ATTEMPTS = 3;

    private final CustomerDAO customerDAO;
    private final CustomerService customerService;

    public AuthService(CustomerService customerService) {
        this.customerService = customerService == null ? new CustomerService() : customerService;
        this.customerDAO = new CustomerDAO();
    }

    public Customer register(String fullName, String phone, String pin) {
        String normalizedPhone = validatePhone(phone);
        validatePinFormat(pin);
        if (customerDAO.findByPhone(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("A customer already exists with phone number " + normalizedPhone + ".");
        }

        Customer customer = new Customer(
                UUID.randomUUID(),
                fullName,
                normalizedPhone,
                pin,
                LocalDateTime.now()
        );
        customerDAO.create(customer);
        return customerService.refresh(customer);
    }

    public Customer login(String phone, String pin) {
        String normalizedPhone = validatePhone(phone);
        Customer customer = customerDAO.findByPhone(normalizedPhone)
                .orElseThrow(() -> new AccountNotFoundException(normalizedPhone));

        if (customer.isLocked()) {
            throw new AccountLockedException(normalizedPhone);
        }

        validatePinFormat(pin);
        if (!customer.validatePin(pin)) {
            int failedAttempts = customer.getFailedPinAttempts() + 1;
            customerDAO.incrementFailedAttempts(customer.getCustomerId());
            customer.setFailedPinAttempts(failedAttempts);

            int attemptsRemaining = MAX_FAILED_PIN_ATTEMPTS - failedAttempts;
            if (attemptsRemaining <= 0) {
                customerDAO.lockAccount(customer.getCustomerId());
                customer.lockAccount();
                throw new AccountLockedException(normalizedPhone);
            }
            throw new InvalidPinException(attemptsRemaining);
        }

        customerDAO.resetFailedAttempts(customer.getCustomerId());
        customer.resetFailedAttempts();
        return customerService.refresh(customer);
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
        return pin == null ? "" : pin.trim();
    }
}

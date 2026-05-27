package com.igirepay.lab1.service;

import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.InvalidPhoneNumberException;
import com.igirepay.lab1.exceptions.InvalidPinException;
import com.igirepay.lab1.exceptions.InvalidPinFormatException;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab2.dao.CustomerDAO;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuthService {

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
        Customer customer = new Customer(UUID.randomUUID(), fullName, normalizedPhone, pin, LocalDateTime.now());
        customerDAO.create(customer);
        return customerService.refresh(customer);
    }

    public Customer login(String phone, String pin) {
        String normalizedPhone = validatePhone(phone);
        Customer customer = customerDAO.findByPhone(normalizedPhone)
                .orElseThrow(() -> new AccountNotFoundException(normalizedPhone));

        validatePinFormat(pin);
        if (!customer.validatePin(pin)) {
            throw new InvalidPinException(0);
        }

        return customerService.refresh(customer);
    }

    public static String validatePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (!normalized.matches("\\d{10}")) throw new InvalidPhoneNumberException(phone);
        return normalized;
    }

    public static void validatePinFormat(String pin) {
        String normalized = pin == null ? "" : pin.trim();
        if (!normalized.matches("\\d{5}")) throw new InvalidPinFormatException("PIN must be exactly 5 numeric digits.");
    }

    public static String hashPin(String pin) {
        return pin == null ? "" : pin.trim();
    }
}

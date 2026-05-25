package com.igirepay.lab1.service;

import com.igirepay.lab1.model.Customer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CustomerService {
    private final Map<String, Customer> customersByPhone;
    private final Map<UUID, Customer> customersById;

    public CustomerService() {
        this.customersByPhone = new LinkedHashMap<>();
        this.customersById = new LinkedHashMap<>();
    }

    public Customer save(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        customersByPhone.put(customer.getPhoneNumber(), customer);
        customersById.put(customer.getCustomerId(), customer);
        return customer;
    }

    public Optional<Customer> findByPhone(String phone) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        return Optional.ofNullable(customersByPhone.get(normalizedPhone));
    }

    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(customersById.get(id));
    }

    public List<Customer> getAllCustomers() {
        return Collections.unmodifiableList(new ArrayList<>(customersById.values()));
    }
}

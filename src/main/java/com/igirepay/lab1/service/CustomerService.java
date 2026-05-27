package com.igirepay.lab1.service;

import java.util.List;
import java.util.Optional;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab2.dao.AccountDAO;
import com.igirepay.lab2.dao.CustomerDAO;

public class CustomerService {
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
        this.accountDAO  = new AccountDAO();
    }

    public Customer save(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Customer is required.");
        return attachAccounts(customerDAO.create(customer));
    }

    public Optional<Customer> findByPhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        return customerDAO.findByPhone(normalized).map(this::attachAccounts);
    }

    public Optional<Customer> findById(int id) {
        return customerDAO.findById(id).map(this::attachAccounts);
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll().stream().map(this::attachAccounts).toList();
    }

    public void update(Customer customer) {
        customerDAO.update(customer);
        for (Account account : customer.getAccounts()) {
            accountDAO.updateHashedPin(account.getAccountId(), customer.getHashedPin());
        }
    }

    public Customer refresh(Customer customer) {
        if (customer == null) return null;
        return findById(customer.getCustomerId()).orElse(customer);
    }

    private Customer attachAccounts(Customer customer) {
        customer.setAccounts(accountDAO.findByCustomerId(customer.getCustomerId()));
        return customer;
    }
}

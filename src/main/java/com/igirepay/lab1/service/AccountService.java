package com.igirepay.lab1.service;

import java.math.BigDecimal;

import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.SavingsAccount;
import com.igirepay.lab1.model.WalletAccount;
import com.igirepay.lab2.dao.AccountDAO;

public class AccountService {
    private final AccountDAO accountDAO;

    public AccountService() {
        this.accountDAO = new AccountDAO();
    }

    public WalletAccount createWallet(Customer customer) {
        requireCustomer(customer);
        ensureAccountDoesNotExist(customer.getCustomerId(), AccountType.WALLET);
        WalletAccount wallet = new WalletAccount(customer.getCustomerId(), customer.getHashedPin());
        accountDAO.create(wallet, customer.getCustomerId());
        customer.addAccount(wallet);
        return wallet;
    }

    public SavingsAccount createSavings(Customer customer) {
        requireCustomer(customer);
        ensureAccountDoesNotExist(customer.getCustomerId(), AccountType.SAVINGS);
        SavingsAccount savings = new SavingsAccount(customer.getCustomerId(), customer.getHashedPin());
        accountDAO.create(savings, customer.getCustomerId());
        customer.addAccount(savings);
        return savings;
    }

    public BigDecimal getBalance(int accountId, Customer owner) {
        requireCustomer(owner);
        Account account = accountDAO.findById(accountId)
                .filter(found -> found.getCustomerId() == owner.getCustomerId())
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(accountId)));
        return account.getBalance();
    }

    private void ensureAccountDoesNotExist(int customerId, AccountType accountType) {
        boolean exists = accountDAO.findByCustomerId(customerId).stream()
                .anyMatch(a -> a.getAccountType() == accountType);
        if (exists) {
            throw new IllegalStateException("Customer already has a " + accountType.name().toLowerCase() + " account.");
        }
    }

    private void requireCustomer(Customer customer) {
        if (customer == null) throw new AccountNotFoundException("customer");
    }
}

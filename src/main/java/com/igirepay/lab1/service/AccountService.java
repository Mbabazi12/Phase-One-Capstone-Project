package com.igirepay.lab1.service;

import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.SavingsAccount;
import com.igirepay.lab1.model.WalletAccount;
import com.igirepay.lab2.dao.AccountDAO;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountService {
    private final AccountDAO accountDAO;

    public AccountService() {
        this.accountDAO = new AccountDAO();
    }

    public WalletAccount createWallet(Customer customer) {
        requireCustomer(customer);
        ensureAccountDoesNotExist(customer.getCustomerId(), AccountType.WALLET);
        WalletAccount walletAccount = new WalletAccount(customer.getCustomerId(), customer.getHashedPin());
        accountDAO.create(walletAccount, customer.getCustomerId());
        customer.addAccount(walletAccount);
        return walletAccount;
    }

    public SavingsAccount createSavings(Customer customer) {
        requireCustomer(customer);
        ensureAccountDoesNotExist(customer.getCustomerId(), AccountType.SAVINGS);
        SavingsAccount savingsAccount = new SavingsAccount(customer.getCustomerId(), customer.getHashedPin());
        accountDAO.create(savingsAccount, customer.getCustomerId());
        customer.addAccount(savingsAccount);
        return savingsAccount;
    }

    public BigDecimal getBalance(UUID accountId, Customer owner) {
        requireCustomer(owner);
        Account account = accountDAO.findById(accountId)
                .filter(found -> found.getCustomerId().equals(owner.getCustomerId()))
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(accountId)));
        return account.getBalance();
    }

    private void ensureAccountDoesNotExist(UUID customerId, AccountType accountType) {
        boolean exists = accountDAO.findByCustomerId(customerId).stream()
                .anyMatch(account -> account.getAccountType() == accountType);
        if (exists) {
            throw new IllegalStateException("Customer already has a " + accountType.name().toLowerCase() + " account.");
        }
    }

    private void requireCustomer(Customer customer) {
        if (customer == null) {
            throw new AccountNotFoundException("customer");
        }
    }
}

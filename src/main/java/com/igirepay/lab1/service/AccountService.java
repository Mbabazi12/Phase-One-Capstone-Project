package com.igirepay.lab1.service;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountNotFoundException;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.SavingsAccount;
import com.igirepay.lab1.model.WalletAccount;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountService {
    public WalletAccount createWallet(Customer customer) {
        requireCustomer(customer);
        if (customer.getAccountByType(AccountType.WALLET).isPresent()) {
            throw new IllegalStateException("Customer already has a wallet account.");
        }
        WalletAccount walletAccount = new WalletAccount(customer.getCustomerId(), customer.getHashedPin());
        customer.addAccount(walletAccount);
        return walletAccount;
    }

    public SavingsAccount createSavings(Customer customer) {
        requireCustomer(customer);
        if (customer.getAccountByType(AccountType.SAVINGS).isPresent()) {
            throw new IllegalStateException("Customer already has a savings account.");
        }
        SavingsAccount savingsAccount = new SavingsAccount(customer.getCustomerId(), customer.getHashedPin());
        customer.addAccount(savingsAccount);
        return savingsAccount;
    }

    public BigDecimal getBalance(UUID accountId, Customer owner) {
        requireCustomer(owner);
        Account account = owner.getAccountById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(String.valueOf(accountId)));
        return account.getBalance();
    }

    private void requireCustomer(Customer customer) {
        if (customer == null) {
            throw new AccountNotFoundException("customer");
        }
    }
}

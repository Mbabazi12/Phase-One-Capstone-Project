package com.igirepay.lab3.ui;

import com.igirepay.lab1.model.Customer;

public class SessionManager {

    private static Customer currentCustomer;

    public static void setCurrentCustomer(Customer customer) {
        currentCustomer = customer;
    }

    public static Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public static void clear() {
        currentCustomer = null;
    }
}

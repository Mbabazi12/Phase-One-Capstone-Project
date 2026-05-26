package com.igirepay.lab1.model;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super("Insufficient balance. Required: " + required + " RWF, available: " + available + " RWF.");
        this.required = required;
        this.available = available;
    }

    public BigDecimal getRequired() {
        return required;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}

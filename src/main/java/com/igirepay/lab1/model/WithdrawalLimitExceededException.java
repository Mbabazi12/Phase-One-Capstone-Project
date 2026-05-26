package com.igirepay.lab1.model;

public class WithdrawalLimitExceededException extends RuntimeException {
    private final int dailyLimit;

    public WithdrawalLimitExceededException(int dailyLimit) {
        super("Daily withdrawal limit exceeded. Maximum withdrawals per day: " + dailyLimit + ".");
        this.dailyLimit = dailyLimit;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}

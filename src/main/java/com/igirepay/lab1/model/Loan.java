package com.igirepay.lab1.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Loan {

    public static final BigDecimal MAX_LOAN_AMOUNT     = new BigDecimal("100000");
    public static final BigDecimal PREMIUM_LOAN_AMOUNT = new BigDecimal("500000");
    public static final BigDecimal PREMIUM_TX_THRESHOLD = new BigDecimal("300000");
    public static final BigDecimal INTEREST_RATE       = new BigDecimal("0.10");

    public enum LoanStatus { ACTIVE, FULLY_PAID }

    private int          loanId;
    private int          customerId;
    private BigDecimal   principal;       // amount requested
    private BigDecimal   totalRepayable;  // principal + 10% interest
    private BigDecimal   amountPaid;
    private LoanStatus   status;
    private LocalDateTime createdAt;

    public Loan() {}

    public Loan(int loanId, int customerId, BigDecimal principal,
                BigDecimal totalRepayable, BigDecimal amountPaid,
                LoanStatus status, LocalDateTime createdAt) {
        this.loanId         = loanId;
        this.customerId     = customerId;
        this.principal      = principal;
        this.totalRepayable = totalRepayable;
        this.amountPaid     = amountPaid;
        this.status         = status;
        this.createdAt      = createdAt;
    }

    public static Loan create(int customerId, BigDecimal principal) {
        BigDecimal interest  = principal.multiply(INTEREST_RATE).setScale(4, RoundingMode.HALF_UP);
        BigDecimal repayable = principal.add(interest).setScale(4, RoundingMode.HALF_UP);
        return new Loan(0, customerId, principal.setScale(4, RoundingMode.HALF_UP),
                repayable, BigDecimal.ZERO, LoanStatus.ACTIVE, LocalDateTime.now());
    }

    public BigDecimal getRemainingBalance() {
        return totalRepayable.subtract(amountPaid).setScale(4, RoundingMode.HALF_UP);
    }

    public boolean isFullyPaid() {
        return getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0;
    }


    public int getLoanId()                        { return loanId; }
    public void setLoanId(int loanId)             { this.loanId = loanId; }

    public int getCustomerId()                    { return customerId; }
    public void setCustomerId(int customerId)     { this.customerId = customerId; }

    public BigDecimal getPrincipal()              { return principal; }
    public void setPrincipal(BigDecimal p)        { this.principal = p; }

    public BigDecimal getTotalRepayable()         { return totalRepayable; }
    public void setTotalRepayable(BigDecimal t)   { this.totalRepayable = t; }

    public BigDecimal getAmountPaid()             { return amountPaid; }
    public void setAmountPaid(BigDecimal a)       { this.amountPaid = a; }

    public LoanStatus getStatus()                 { return status; }
    public void setStatus(LoanStatus s)           { this.status = s; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime d)     { this.createdAt = d; }

    @Override
    public String toString() {
        return "Loan{loanId=" + loanId + ", customerId=" + customerId
                + ", principal=" + principal + ", totalRepayable=" + totalRepayable
                + ", amountPaid=" + amountPaid + ", status=" + status + '}';
    }
}

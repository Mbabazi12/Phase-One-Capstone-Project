package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidAmountException;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Loan;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab2.service.LoanService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoanController {

    @FXML private TextField   requestAmountField;
    @FXML private PasswordField requestPinField;
    @FXML private Label       requestResultLabel;

    @FXML private TextField   repayAmountField;
    @FXML private PasswordField repayPinField;
    @FXML private Label       repayResultLabel;

    @FXML private Label loanStatusLabel;

    private final LoanService    loanService;
    private final CustomerService customerService;

    public LoanController() {
        this.loanService     = new LoanService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        refreshLoanStatus();
    }

    @FXML
    private void handleRequestLoan() {
        requestResultLabel.setText("");
        Customer customer = SessionManager.getCurrentCustomer();

        BigDecimal amount = parseAmount(requestAmountField, requestResultLabel);
        if (amount == null) return;

        String pin = requestPinField.getText().trim();
        if (!customer.validatePin(pin)) {
            setError(requestResultLabel, "Incorrect PIN.");
            return;
        }

        try {
            Loan loan = loanService.requestLoan(customer, amount);
            refreshSession();
            setSuccess(requestResultLabel,
                    "Loan approved! " + fmt(loan.getPrincipal()) + " disbursed to your wallet.\n" +
                    "Total repayable: " + fmt(loan.getTotalRepayable()) +
                    " (includes 10% interest)");
            requestAmountField.clear();
            requestPinField.clear();
            refreshLoanStatus();
        } catch (InvalidAmountException | IllegalStateException e) {
            setError(requestResultLabel, e.getMessage());
        } catch (DatabaseException e) {
            showError("Database error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    @FXML
    private void handleRepayLoan() {
        repayResultLabel.setText("");
        Customer customer = SessionManager.getCurrentCustomer();

        BigDecimal amount = parseAmount(repayAmountField, repayResultLabel);
        if (amount == null) return;

        String pin = repayPinField.getText().trim();
        if (!customer.validatePin(pin)) {
            setError(repayResultLabel, "Incorrect PIN.");
            return;
        }

        try {
            Loan loan = loanService.repayLoan(customer, amount);
            refreshSession();
            if (loan.getStatus() == Loan.LoanStatus.FULLY_PAID) {
                setSuccess(repayResultLabel, "Loan fully repaid. Well done!");
            } else {
                setSuccess(repayResultLabel,
                        "Repayment successful. Remaining balance: " +
                        fmt(loan.getRemainingBalance()));
            }
            repayAmountField.clear();
            repayPinField.clear();
            refreshLoanStatus();
        } catch (InsufficientBalanceException e) {
            setError(repayResultLabel, "Insufficient wallet balance.");
        } catch (IllegalStateException e) {
            setError(repayResultLabel, e.getMessage());
        } catch (DatabaseException e) {
            showError("Database error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private void refreshLoanStatus() {
        Customer customer = SessionManager.getCurrentCustomer();
        Optional<Loan> activeLoan = loanService.getActiveLoan(customer.getCustomerId());
        BigDecimal limit = loanService.getLoanLimit(customer);
        String limitLine = "Your limit: " + fmt(limit) +
                (limit.compareTo(Loan.PREMIUM_LOAN_AMOUNT) == 0
                        ? " (Premium — high transaction volume)"
                        : " (Transact 300,000+ RWF to unlock 500,000 RWF)");

        if (activeLoan.isPresent()) {
            Loan loan = activeLoan.get();
            loanStatusLabel.setText(
                    "Active Loan\n" +
                    "Principal:        " + fmt(loan.getPrincipal()) + "\n" +
                    "Total repayable:  " + fmt(loan.getTotalRepayable()) + "\n" +
                    "Amount paid:      " + fmt(loan.getAmountPaid()) + "\n" +
                    "Remaining:        " + fmt(loan.getRemainingBalance()));
        } else {
            loanStatusLabel.setText("No active loan.\n" + limitLine + "\nInterest: 10% flat");
        }
    }

    private void refreshSession() {
        Customer refreshed = customerService.refresh(SessionManager.getCurrentCustomer());
        SessionManager.setCurrentCustomer(refreshed);
    }

    private BigDecimal parseAmount(TextField field, Label errorLabel) {
        try {
            BigDecimal value = new BigDecimal(field.getText().trim().replace(",", ""));
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                setError(errorLabel, "Amount must be greater than zero.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            setError(errorLabel, "Please enter a valid numeric amount.");
            return null;
        }
    }

    private String fmt(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v)
                .setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF";
    }

    private void setSuccess(Label label, String message) {
        label.setStyle("-fx-text-fill: green;");
        label.setText(message);
    }

    private void setError(Label label, String message) {
        label.setStyle("-fx-text-fill: red;");
        label.setText(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

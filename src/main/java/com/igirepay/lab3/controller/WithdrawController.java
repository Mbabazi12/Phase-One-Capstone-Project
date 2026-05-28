package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidPinException;
import com.igirepay.lab1.exceptions.WithdrawalLimitExceededException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab1.service.TransactionService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class WithdrawController {

    @FXML private ComboBox<String> accountCombo;
    @FXML private TextField amountField;
    @FXML private PasswordField pinField;
    @FXML private Label resultLabel;

    private final TransactionService transactionService;
    private final CustomerService customerService;
    private List<Account> accounts;

    public WithdrawController() {
        this.transactionService = new TransactionService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        // Withdraw from wallet only — savings withdrawals go through the Savings screen
        accounts = customer.getAccounts().stream()
                .filter(a -> a.getAccountType() == com.igirepay.lab1.model.AccountType.WALLET)
                .toList();
        for (Account account : accounts) {
            accountCombo.getItems().add(account.getAccountName() + " (WALLET)");
        }
        if (accounts.isEmpty()) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("No wallet account found. Create one from Manage Accounts.");
        }
    }

    @FXML
    private void handleWithdraw() {
        resultLabel.setText("");
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            setError("Please select an account.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) return;

        String pin = pinField.getText().trim();
        if (pin.isEmpty()) {
            setError("Please enter your PIN.");
            return;
        }

        try {
            transactionService.withdraw(accounts.get(index), amount, pin, UUID.randomUUID().toString());
            refreshSession();
            resultLabel.setStyle("-fx-text-fill: green;");
            resultLabel.setText("Withdrawal successful. New balance: "
                    + formatBalance(accounts.get(index)));
            amountField.clear();
            pinField.clear();
        } catch (InsufficientBalanceException e) {
            setError("Insufficient balance.");
        } catch (InvalidPinException e) {
            setError("Incorrect PIN. Please try again.");
        } catch (WithdrawalLimitExceededException e) {
            setError("Daily withdrawal limit reached (max 3 per day for savings).");
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private BigDecimal parseAmount() {
        try {
            BigDecimal value = new BigDecimal(amountField.getText().trim().replace(",", ""));
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                setError("Please enter a valid amount greater than zero.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            setError("Please enter a valid numeric amount.");
            return null;
        }
    }

    private void setError(String message) {
        resultLabel.setStyle("-fx-text-fill: red;");
        resultLabel.setText(message);
    }

    private String formatBalance(Account account) {
        return account.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF";
    }

    private void refreshSession() {
        Customer refreshed = customerService.refresh(SessionManager.getCurrentCustomer());
        SessionManager.setCurrentCustomer(refreshed);
        accounts = refreshed.getAccounts();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

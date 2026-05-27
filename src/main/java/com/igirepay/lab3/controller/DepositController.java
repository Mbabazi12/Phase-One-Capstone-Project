package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import com.igirepay.lab1.exceptions.DatabaseException;
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
import javafx.scene.control.TextField;

public class DepositController {

    @FXML private ComboBox<String> accountCombo;
    @FXML private TextField amountField;
    @FXML private Label resultLabel;

    private final TransactionService transactionService;
    private final CustomerService customerService;
    private List<Account> accounts;

    public DepositController() {
        this.transactionService = new TransactionService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        accounts = customer.getAccounts();
        for (Account account : accounts) {
            accountCombo.getItems().add(account.getAccountName() + " (" + account.getAccountType().name() + ")");
        }
    }

    @FXML
    private void handleDeposit() {
        resultLabel.setText("");
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            setError("Please select an account.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) return;

        try {
            transactionService.deposit(accounts.get(index), amount, UUID.randomUUID().toString());
            refreshSession();
            resultLabel.setStyle("-fx-text-fill: green;");
            resultLabel.setText("Deposit successful. New balance: "
                    + formatBalance(accounts.get(index)));
            amountField.clear();
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

package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidPinException;
import com.igirepay.lab1.exceptions.WithdrawalLimitExceededException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
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

public class SavingsController {

    @FXML private ComboBox<String> directionCombo;
    @FXML private TextField amountField;
    @FXML private PasswordField pinField;
    @FXML private Label resultLabel;

    private final TransactionService transactionService;
    private final CustomerService customerService;

    public SavingsController() {
        this.transactionService = new TransactionService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        boolean hasWallet  = customer.getAccounts().stream().anyMatch(a -> a.getAccountType() == AccountType.WALLET);
        boolean hasSavings = customer.getAccounts().stream().anyMatch(a -> a.getAccountType() == AccountType.SAVINGS);

        if (hasWallet && hasSavings) {
            directionCombo.getItems().addAll("Wallet → Savings", "Savings → Wallet");
        } else if (!hasWallet) {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("You need a wallet account to use savings transfers.");
        } else {
            resultLabel.setStyle("-fx-text-fill: red;");
            resultLabel.setText("You need a savings account. Create one from Manage Accounts.");
        }
    }

    @FXML
    private void handleMove() {
        resultLabel.setText("");
        int dirIndex = directionCombo.getSelectionModel().getSelectedIndex();
        if (dirIndex < 0) {
            setError("Please select a direction.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) return;

        String pin = pinField.getText().trim();
        if (pin.isEmpty()) {
            setError("Please enter your PIN.");
            return;
        }

        Customer customer = SessionManager.getCurrentCustomer();
        Optional<Account> walletOpt  = customer.getAccounts().stream()
                .filter(a -> a.getAccountType() == AccountType.WALLET).findFirst();
        Optional<Account> savingsOpt = customer.getAccounts().stream()
                .filter(a -> a.getAccountType() == AccountType.SAVINGS).findFirst();

        if (walletOpt.isEmpty() || savingsOpt.isEmpty()) {
            setError("You need both a wallet and savings account.");
            return;
        }

        try {
            if (dirIndex == 0) {
                transactionService.moveToSavings(walletOpt.get(), savingsOpt.get(), amount, pin,
                        UUID.randomUUID().toString());
                resultLabel.setStyle("-fx-text-fill: green;");
                resultLabel.setText("Moved to savings successfully.");
            } else {
                transactionService.moveToWallet(savingsOpt.get(), walletOpt.get(), amount, pin,
                        UUID.randomUUID().toString());
                resultLabel.setStyle("-fx-text-fill: green;");
                resultLabel.setText("Moved to wallet successfully.");
            }
            refreshSession();
            amountField.clear();
            pinField.clear();
        } catch (InsufficientBalanceException e) {
            setError("Insufficient balance.");
        } catch (InvalidPinException e) {
            setError("Incorrect PIN. Please try again.");
        } catch (WithdrawalLimitExceededException e) {
            setError("Daily savings withdrawal limit reached (max 3 per day).");
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

    private void refreshSession() {
        Customer refreshed = customerService.refresh(SessionManager.getCurrentCustomer());
        SessionManager.setCurrentCustomer(refreshed);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

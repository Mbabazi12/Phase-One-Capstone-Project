package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InsufficientBalanceException;
import com.igirepay.lab1.exceptions.InvalidPinException;
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

public class TransferController {

    @FXML private ComboBox<String> accountCombo;
    @FXML private TextField recipientPhoneField;
    @FXML private Label recipientNameLabel;
    @FXML private TextField amountField;
    @FXML private Label feeLabel;
    @FXML private PasswordField pinField;
    @FXML private Label resultLabel;

    private final TransactionService transactionService;
    private final CustomerService customerService;
    private List<Account> walletAccounts;

    public TransferController() {
        this.transactionService = new TransactionService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        walletAccounts = customer.getAccounts().stream()
                .filter(a -> a.getAccountType() == AccountType.WALLET)
                .toList();
        for (Account account : walletAccounts) {
            accountCombo.getItems().add(account.getAccountName() + " (WALLET)");
        }
    }

    /**
     * Look up recipient by phone number and display their name before the transfer.
     */
    @FXML
    private void handleLookupRecipient() {
        recipientNameLabel.setText("");
        feeLabel.setText("");
        String phone = recipientPhoneField.getText().trim();
        if (phone.isEmpty()) {
            recipientNameLabel.setStyle("-fx-text-fill: red;");
            recipientNameLabel.setText("Enter a phone number first.");
            return;
        }

        try {
            String name = transactionService.lookupRecipientName(phone, customerService);
            recipientNameLabel.setStyle("-fx-text-fill: green;");
            recipientNameLabel.setText("Sending to: " + name);
            updateFeePreview();
        } catch (AccountNotFoundException e) {
            recipientNameLabel.setStyle("-fx-text-fill: red;");
            recipientNameLabel.setText("No customer found with that phone number.");
        } catch (DatabaseException e) {
            recipientNameLabel.setStyle("-fx-text-fill: red;");
            recipientNameLabel.setText("Database error. Please try again.");
        }
    }

    @FXML
    private void handleAmountChanged() {
        updateFeePreview();
    }

    @FXML
    private void handleTransfer() {
        resultLabel.setText("");
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            setError("Please select a source wallet account.");
            return;
        }

        String recipientPhone = recipientPhoneField.getText().trim();
        if (recipientPhone.isEmpty()) {
            setError("Please enter the recipient's phone number.");
            return;
        }

        // Require name lookup before transfer
        if (recipientNameLabel.getText().isEmpty() || recipientNameLabel.getText().startsWith("No customer")) {
            setError("Please look up the recipient first to confirm their name.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) return;

        String pin = pinField.getText().trim();
        if (pin.isEmpty()) {
            setError("Please enter your PIN.");
            return;
        }

        Account sender = walletAccounts.get(index);

        try {
            transactionService.transfer(sender, recipientPhone, amount, pin,
                    UUID.randomUUID().toString(), customerService);
            refreshSession();
            resultLabel.setStyle("-fx-text-fill: green;");
            resultLabel.setText("Transfer successful.");
            amountField.clear();
            pinField.clear();
            feeLabel.setText("");
        } catch (InsufficientBalanceException e) {
            setError("Insufficient balance (amount + fee).");
        } catch (InvalidPinException e) {
            setError("Incorrect PIN. Please try again.");
        } catch (AccountNotFoundException e) {
            setError("Recipient not found.");
        } catch (IllegalArgumentException e) {
            setError(e.getMessage());
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private void updateFeePreview() {
        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            feeLabel.setText("");
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountText.replace(",", ""));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal fee = transactionService.previewTransferFee(amount);
                BigDecimal total = amount.add(fee);
                feeLabel.setText("Fee: " + fee.setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + " RWF  |  Total deducted: " + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF");
            }
        } catch (IllegalArgumentException ignored) {
            feeLabel.setText("");
        }
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
        walletAccounts = refreshed.getAccounts().stream()
                .filter(a -> a.getAccountType() == AccountType.WALLET)
                .toList();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

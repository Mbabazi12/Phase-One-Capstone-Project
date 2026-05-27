package com.igirepay.lab3.controller;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.DuplicateTransactionException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class TransactionController {

    @FXML private ComboBox<String> depositAccountCombo;
    @FXML private TextField depositAmountField;

    @FXML private ComboBox<String> withdrawAccountCombo;
    @FXML private TextField withdrawAmountField;
    @FXML private PasswordField withdrawPinField;

    @FXML private ComboBox<String> transferAccountCombo;
    @FXML private TextField transferRecipientPhoneField;
    @FXML private TextField transferAmountField;
    @FXML private PasswordField transferPinField;

    @FXML private Label resultLabel;

    private final TransactionService transactionService;
    private final CustomerService customerService;
    private List<Account> accounts;

    public TransactionController() {
        this.transactionService = new TransactionService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        accounts = customer.getAccounts();
        for (Account account : accounts) {
            String label = account.getAccountType().name() + " (" + account.getAccountId().toString().substring(0, 8) + "...)";
            depositAccountCombo.getItems().add(label);
            withdrawAccountCombo.getItems().add(label);
            transferAccountCombo.getItems().add(label);
        }
    }

    @FXML
    private void handleDeposit() {
        resultLabel.setText("");
        int index = depositAccountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { resultLabel.setText("Select an account."); return; }

        BigDecimal amount = parseAmount(depositAmountField.getText());
        if (amount == null) return;

        try {
            transactionService.deposit(accounts.get(index), amount, UUID.randomUUID().toString());
            refreshSession();
            resultLabel.setText("Deposit successful.");
        } catch (DuplicateTransactionException e) {
            resultLabel.setText("Duplicate transaction detected. Request rejected.");
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleWithdraw() {
        resultLabel.setText("");
        int index = withdrawAccountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { resultLabel.setText("Select an account."); return; }

        BigDecimal amount = parseAmount(withdrawAmountField.getText());
        if (amount == null) return;

        String pin = withdrawPinField.getText().trim();

        try {
            transactionService.withdraw(accounts.get(index), amount, pin, UUID.randomUUID().toString());
            refreshSession();
            resultLabel.setText("Withdrawal successful.");
        } catch (InsufficientBalanceException e) {
            resultLabel.setText("Insufficient balance for this transaction.");
        } catch (InvalidPinException e) {
            resultLabel.setText("Invalid PIN.");
        } catch (WithdrawalLimitExceededException e) {
            resultLabel.setText("Daily withdrawal limit reached.");
        } catch (DuplicateTransactionException e) {
            resultLabel.setText("Duplicate transaction detected. Request rejected.");
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleTransfer() {
        resultLabel.setText("");
        int index = transferAccountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { resultLabel.setText("Select a source account."); return; }

        String recipientPhone = transferRecipientPhoneField.getText().trim();
        BigDecimal amount = parseAmount(transferAmountField.getText());
        if (amount == null) return;

        String pin = transferPinField.getText().trim();
        Account sender = accounts.get(index);

        try {
            transactionService.transfer(sender, recipientPhone, amount, pin,
                    UUID.randomUUID().toString(), customerService);
            refreshSession();
            resultLabel.setText("Transfer successful.");
        } catch (InsufficientBalanceException e) {
            resultLabel.setText("Insufficient balance for this transaction.");
        } catch (InvalidPinException e) {
            resultLabel.setText("Invalid PIN.");
        } catch (DuplicateTransactionException e) {
            resultLabel.setText("Duplicate transaction detected. Request rejected.");
        } catch (IllegalArgumentException e) {
            resultLabel.setText(e.getMessage());
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private BigDecimal parseAmount(String text) {
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(",", ""));
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                resultLabel.setText("Please enter a valid amount.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            resultLabel.setText("Please enter a valid amount.");
            return null;
        }
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

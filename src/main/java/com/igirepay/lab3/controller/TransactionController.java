package com.igirepay.lab3.controller;

import com.igirepay.lab1.exceptions.AccountNotFoundException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransactionController {

    // Deposit tab
    @FXML private ComboBox<String> depositAccountCombo;
    @FXML private TextField depositAmountField;
    @FXML private Label depositResultLabel;

    // Withdraw tab
    @FXML private ComboBox<String> withdrawAccountCombo;
    @FXML private TextField withdrawAmountField;
    @FXML private PasswordField withdrawPinField;
    @FXML private Label withdrawResultLabel;

    // Transfer tab
    @FXML private ComboBox<String> transferAccountCombo;
    @FXML private TextField transferRecipientPhoneField;
    @FXML private Label transferRecipientNameLabel;
    @FXML private Label transferFeeLabel;
    @FXML private TextField transferAmountField;
    @FXML private PasswordField transferPinField;
    @FXML private Label transferResultLabel;

    // Savings move tab
    @FXML private ComboBox<String> savingsDirectionCombo;
    @FXML private TextField savingsAmountField;
    @FXML private PasswordField savingsPinField;
    @FXML private Label savingsResultLabel;

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
            String label = account.getAccountName() + " (" + account.getAccountType().name() + ")";
            depositAccountCombo.getItems().add(label);
            withdrawAccountCombo.getItems().add(label);

            if (account.getAccountType() == AccountType.WALLET) {
                transferAccountCombo.getItems().add(label);
            }
        }

        boolean hasWallet = accounts.stream().anyMatch(a -> a.getAccountType() == AccountType.WALLET);
        boolean hasSavings = accounts.stream().anyMatch(a -> a.getAccountType() == AccountType.SAVINGS);
        if (hasWallet && hasSavings) {
            savingsDirectionCombo.getItems().addAll("Wallet → Savings", "Savings → Wallet");
        }
    }

    // ── Deposit ──────────────────────────────────────────────────────────────

    @FXML
    private void handleDeposit() {
        depositResultLabel.setText("");
        int index = depositAccountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { depositResultLabel.setText("Select an account."); return; }

        BigDecimal amount = parseAmount(depositAmountField.getText(), depositResultLabel);
        if (amount == null) return;

        try {
            transactionService.deposit(accounts.get(index), amount, UUID.randomUUID().toString());
            refreshSession();
            depositResultLabel.setStyle("-fx-text-fill: green;");
            depositResultLabel.setText("Deposit successful. New balance: " + formatBalance(accounts.get(index)));
            depositAmountField.clear();
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    // ── Withdraw ─────────────────────────────────────────────────────────────

    @FXML
    private void handleWithdraw() {
        withdrawResultLabel.setText("");
        int index = withdrawAccountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { withdrawResultLabel.setText("Select an account."); return; }

        BigDecimal amount = parseAmount(withdrawAmountField.getText(), withdrawResultLabel);
        if (amount == null) return;

        String pin = withdrawPinField.getText().trim();

        try {
            transactionService.withdraw(accounts.get(index), amount, pin, UUID.randomUUID().toString());
            refreshSession();
            withdrawResultLabel.setStyle("-fx-text-fill: green;");
            withdrawResultLabel.setText("Withdrawal successful. New balance: " + formatBalance(accounts.get(index)));
            withdrawAmountField.clear();
            withdrawPinField.clear();
        } catch (InsufficientBalanceException e) {
            withdrawResultLabel.setStyle("-fx-text-fill: red;");
            withdrawResultLabel.setText("Insufficient balance.");
        } catch (InvalidPinException e) {
            withdrawResultLabel.setStyle("-fx-text-fill: red;");
            withdrawResultLabel.setText("Incorrect PIN.");
        } catch (WithdrawalLimitExceededException e) {
            withdrawResultLabel.setStyle("-fx-text-fill: red;");
            withdrawResultLabel.setText("Daily withdrawal limit reached.");
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    // ── Transfer ─────────────────────────────────────────────────────────────

    @FXML
    private void handleLookupRecipient() {
        transferRecipientNameLabel.setText("");
        transferFeeLabel.setText("");
        String phone = transferRecipientPhoneField.getText().trim();
        if (phone.isEmpty()) return;

        try {
            String name = transactionService.lookupRecipientName(phone, customerService);
            transferRecipientNameLabel.setStyle("-fx-text-fill: green;");
            transferRecipientNameLabel.setText("Sending to: " + name);

            String amountText = transferAmountField.getText().trim();
            if (!amountText.isEmpty()) {
                try {
                    BigDecimal amount = new BigDecimal(amountText.replace(",", ""));
                    BigDecimal fee = transactionService.previewTransferFee(amount);
                    transferFeeLabel.setText("Fee: " + fee.setScale(2, RoundingMode.HALF_UP).toPlainString()
                            + " RWF  |  Total deducted: " + amount.add(fee).setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF");
                } catch (NumberFormatException ignored) {}
            }
        } catch (AccountNotFoundException e) {
            transferRecipientNameLabel.setStyle("-fx-text-fill: red;");
            transferRecipientNameLabel.setText("No customer found with that phone number.");
        } catch (DatabaseException e) {
            transferRecipientNameLabel.setStyle("-fx-text-fill: red;");
            transferRecipientNameLabel.setText("Database error.");
        }
    }

    @FXML
    private void handleTransferAmountChanged() {
        String phone = transferRecipientPhoneField.getText().trim();
        String amountText = transferAmountField.getText().trim();
        if (phone.isEmpty() || amountText.isEmpty()) return;
        try {
            BigDecimal amount = new BigDecimal(amountText.replace(",", ""));
            BigDecimal fee = transactionService.previewTransferFee(amount);
            transferFeeLabel.setText("Fee: " + fee.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + " RWF  |  Total deducted: " + amount.add(fee).setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF");
        } catch (NumberFormatException | IllegalArgumentException ignored) {
            transferFeeLabel.setText("");
        }
    }

    @FXML
    private void handleTransfer() {
        transferResultLabel.setText("");
        int index = transferAccountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { transferResultLabel.setText("Select a source account."); return; }

        String recipientPhone = transferRecipientPhoneField.getText().trim();
        if (recipientPhone.isEmpty()) { transferResultLabel.setText("Enter recipient phone number."); return; }

        BigDecimal amount = parseAmount(transferAmountField.getText(), transferResultLabel);
        if (amount == null) return;

        String pin = transferPinField.getText().trim();

        // Find the wallet account from the combo selection
        List<Account> walletAccounts = accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.WALLET).toList();
        if (index >= walletAccounts.size()) { transferResultLabel.setText("Invalid account selection."); return; }
        Account sender = walletAccounts.get(index);

        try {
            transactionService.transfer(sender, recipientPhone, amount, pin,
                    UUID.randomUUID().toString(), customerService);
            refreshSession();
            transferResultLabel.setStyle("-fx-text-fill: green;");
            transferResultLabel.setText("Transfer successful.");
            transferAmountField.clear();
            transferPinField.clear();
            transferFeeLabel.setText("");
        } catch (InsufficientBalanceException e) {
            transferResultLabel.setStyle("-fx-text-fill: red;");
            transferResultLabel.setText("Insufficient balance (including fee).");
        } catch (InvalidPinException e) {
            transferResultLabel.setStyle("-fx-text-fill: red;");
            transferResultLabel.setText("Incorrect PIN.");
        } catch (AccountNotFoundException e) {
            transferResultLabel.setStyle("-fx-text-fill: red;");
            transferResultLabel.setText("Recipient not found.");
        } catch (IllegalArgumentException e) {
            transferResultLabel.setStyle("-fx-text-fill: red;");
            transferResultLabel.setText(e.getMessage());
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    // ── Savings Move ─────────────────────────────────────────────────────────

    @FXML
    private void handleSavingsMove() {
        savingsResultLabel.setText("");
        int dirIndex = savingsDirectionCombo.getSelectionModel().getSelectedIndex();
        if (dirIndex < 0) { savingsResultLabel.setText("Select a direction."); return; }

        BigDecimal amount = parseAmount(savingsAmountField.getText(), savingsResultLabel);
        if (amount == null) return;

        String pin = savingsPinField.getText().trim();

        Optional<Account> walletOpt = accounts.stream().filter(a -> a.getAccountType() == AccountType.WALLET).findFirst();
        Optional<Account> savingsOpt = accounts.stream().filter(a -> a.getAccountType() == AccountType.SAVINGS).findFirst();

        if (walletOpt.isEmpty() || savingsOpt.isEmpty()) {
            savingsResultLabel.setText("You need both a wallet and savings account.");
            return;
        }

        try {
            if (dirIndex == 0) {
                // Wallet → Savings
                transactionService.moveToSavings(walletOpt.get(), savingsOpt.get(), amount, pin, UUID.randomUUID().toString());
                savingsResultLabel.setStyle("-fx-text-fill: green;");
                savingsResultLabel.setText("Moved to savings successfully.");
            } else {
                // Savings → Wallet
                transactionService.moveToWallet(savingsOpt.get(), walletOpt.get(), amount, pin, UUID.randomUUID().toString());
                savingsResultLabel.setStyle("-fx-text-fill: green;");
                savingsResultLabel.setText("Moved to wallet successfully.");
            }
            refreshSession();
            savingsAmountField.clear();
            savingsPinField.clear();
        } catch (InsufficientBalanceException e) {
            savingsResultLabel.setStyle("-fx-text-fill: red;");
            savingsResultLabel.setText("Insufficient balance.");
        } catch (InvalidPinException e) {
            savingsResultLabel.setStyle("-fx-text-fill: red;");
            savingsResultLabel.setText("Incorrect PIN.");
        } catch (WithdrawalLimitExceededException e) {
            savingsResultLabel.setStyle("-fx-text-fill: red;");
            savingsResultLabel.setText("Daily savings withdrawal limit reached.");
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private BigDecimal parseAmount(String text, Label errorLabel) {
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(",", ""));
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText("Please enter a valid amount.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Please enter a valid amount.");
            return null;
        }
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

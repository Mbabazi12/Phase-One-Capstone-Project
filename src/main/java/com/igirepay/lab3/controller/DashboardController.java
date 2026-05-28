package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Loan;
import com.igirepay.lab2.service.LoanService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML private Label welcomeLabel;

    // Balance cards
    @FXML private Label walletBalanceLabel;
    @FXML private Label savingsBalanceLabel;
    @FXML private Label loanBalanceLabel;

    // Shown when customer has no accounts yet
    @FXML private VBox  emptyStateBox;
    @FXML private HBox  cardsBox;

    private final LoanService loanService = new LoanService();

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        welcomeLabel.setText("Welcome, " + customer.getFullName());

        Optional<Account> wallet  = customer.getAccountByType(AccountType.WALLET);
        Optional<Account> savings = customer.getAccountByType(AccountType.SAVINGS);
        boolean hasAny = wallet.isPresent() || savings.isPresent();

        if (hasAny) {
            emptyStateBox.setVisible(false);
            emptyStateBox.setManaged(false);
            cardsBox.setVisible(true);
            cardsBox.setManaged(true);

            walletBalanceLabel.setText(wallet
                    .map(a -> fmt(a.getBalance()))
                    .orElse("No wallet"));
            savingsBalanceLabel.setText(savings
                    .map(a -> fmt(a.getBalance()))
                    .orElse("No savings"));

            Optional<Loan> activeLoan = loanService.getActiveLoan(customer.getCustomerId());
            loanBalanceLabel.setText(activeLoan
                    .map(l -> fmt(l.getRemainingBalance()))
                    .orElse("No active loan"));
        } else {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            cardsBox.setVisible(false);
            cardsBox.setManaged(false);
        }
    }

    @FXML private void handleDeposit()        { SceneManager.switchScene("/fxml/deposit.fxml"); }
    @FXML private void handleWithdraw()       { SceneManager.switchScene("/fxml/withdraw.fxml"); }
    @FXML private void handleTransfer()       { SceneManager.switchScene("/fxml/transfer.fxml"); }
    @FXML private void handleSavings()        { SceneManager.switchScene("/fxml/savings.fxml"); }
    @FXML private void handleLoan()           { SceneManager.switchScene("/fxml/loan.fxml"); }
    @FXML private void handleHistory()        { SceneManager.switchScene("/fxml/history.fxml"); }
    @FXML private void handleReports()        { SceneManager.switchScene("/fxml/reports.fxml"); }
    @FXML private void handleChangePin()      { SceneManager.switchScene("/fxml/changePin.fxml"); }
    @FXML private void handleManageAccounts() { SceneManager.switchScene("/fxml/account.fxml"); }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.switchScene("/fxml/login.fxml");
    }

    private String fmt(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v)
                .setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF";
    }
}

package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.service.AccountService;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab2.dao.AccountDAO;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class AccountController {

    @FXML private ListView<String> accountListView;
    @FXML private Label messageLabel;

    private final AccountService accountService;
    private final AccountDAO accountDAO;
    private final CustomerService customerService;

    public AccountController() {
        this.accountService = new AccountService();
        this.accountDAO = new AccountDAO();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        refreshList();
    }

    @FXML
    private void handleCreateWallet() {
        messageLabel.setText("");
        Customer customer = SessionManager.getCurrentCustomer();
        try {
            accountService.createWallet(customer);
            refreshSession();
            messageLabel.setText("Wallet account created.");
            refreshList();
        } catch (IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleCreateSavings() {
        messageLabel.setText("");
        Customer customer = SessionManager.getCurrentCustomer();
        try {
            accountService.createSavings(customer);
            refreshSession();
            messageLabel.setText("Savings account created.");
            refreshList();
        } catch (IllegalStateException e) {
            messageLabel.setText(e.getMessage());
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleDeleteSelected() {
        messageLabel.setText("");
        int selectedIndex = accountListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            messageLabel.setText("Select an account to delete.");
            return;
        }

        Customer customer = SessionManager.getCurrentCustomer();
        Account account = customer.getAccounts().get(selectedIndex);

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            messageLabel.setText("Cannot delete account with non-zero balance.");
            return;
        }

        try {
            accountDAO.deactivate(account.getAccountId());
            refreshSession();
            messageLabel.setText("Account deactivated.");
            refreshList();
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private void refreshList() {
        Customer customer = SessionManager.getCurrentCustomer();
        accountListView.getItems().clear();
        for (Account account : customer.getAccounts()) {
            String entry = account.getAccountType().name()
                    + " | ID: " + account.getAccountId()
                    + " | Balance: " + account.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"
                    + (account.isActive() ? "" : " [INACTIVE]");
            accountListView.getItems().add(entry);
        }
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

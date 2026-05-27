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
        this.accountDAO     = new AccountDAO();
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
            setSuccess("Wallet account created.");
            refreshList();
        } catch (IllegalStateException e) {
            setError(e.getMessage());
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
            setSuccess("Savings account created.");
            refreshList();
        } catch (IllegalStateException e) {
            setError(e.getMessage());
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleDeactivateSelected() {
        messageLabel.setText("");
        int selectedIndex = accountListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            setError("Select an account first.");
            return;
        }

        Customer customer = SessionManager.getCurrentCustomer();
        Account account = customer.getAccounts().get(selectedIndex);

        if (!account.isActive()) {
            setError("Account is already inactive.");
            return;
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            setError("Cannot deactivate an account with a non-zero balance.");
            return;
        }

        try {
            accountDAO.deactivate(account.getAccountId());
            refreshSession();
            setSuccess("Account deactivated.");
            refreshList();
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleReactivateSelected() {
        messageLabel.setText("");
        int selectedIndex = accountListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            setError("Select an account first.");
            return;
        }

        Customer customer = SessionManager.getCurrentCustomer();
        Account account = customer.getAccounts().get(selectedIndex);

        if (account.isActive()) {
            setError("Account is already active.");
            return;
        }

        try {
            accountDAO.activate(account.getAccountId());
            refreshSession();
            setSuccess("Account reactivated successfully.");
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
            String status = account.isActive() ? "ACTIVE" : "INACTIVE";
            String entry = account.getAccountType().name()
                    + " | ID: " + account.getAccountId()
                    + " | Balance: " + account.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"
                    + " | " + status;
            accountListView.getItems().add(entry);
        }
    }

    private void refreshSession() {
        Customer refreshed = customerService.refresh(SessionManager.getCurrentCustomer());
        SessionManager.setCurrentCustomer(refreshed);
    }

    private void setSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText(message);
    }

    private void setError(String message) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

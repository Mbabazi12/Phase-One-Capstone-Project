package com.igirepay.lab3.controller;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.RoundingMode;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<Account> accountsTable;
    @FXML private TableColumn<Account, String> colAccountId;
    @FXML private TableColumn<Account, String> colAccountType;
    @FXML private TableColumn<Account, String> colBalance;

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        welcomeLabel.setText("Welcome, " + customer.getFullName());

        colAccountId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAccountId().toString()));
        colAccountType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAccountType().name()));
        colBalance.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBalance()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"));

        accountsTable.setItems(FXCollections.observableArrayList(customer.getAccounts()));
    }

    @FXML private void handleDeposit()      { SceneManager.switchScene("/fxml/transaction.fxml"); }
    @FXML private void handleWithdraw()     { SceneManager.switchScene("/fxml/transaction.fxml"); }
    @FXML private void handleTransfer()     { SceneManager.switchScene("/fxml/transaction.fxml"); }
    @FXML private void handleHistory()      { SceneManager.switchScene("/fxml/history.fxml"); }
    @FXML private void handleReports()      { SceneManager.switchScene("/fxml/reports.fxml"); }
    @FXML private void handleChangePin()    { SceneManager.switchScene("/fxml/changePin.fxml"); }
    @FXML private void handleManageAccounts() { SceneManager.switchScene("/fxml/account.fxml"); }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.switchScene("/fxml/login.fxml");
    }
}

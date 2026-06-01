package com.igirepay.lab3.controller;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab2.dao.TransactionDAO;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class HistoryController {

    @FXML private ComboBox<String>  accountCombo;
    @FXML private ComboBox<String>  typeFilterCombo;
    @FXML private DatePicker        fromDatePicker;
    @FXML private DatePicker        toDatePicker;
    @FXML private Label             countLabel;

    @FXML private TableView<Transaction>           historyTable;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, String> colDesc;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private List<Account> accounts;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        accounts = customer.getAccounts();

        // Populate account combo
        for (Account account : accounts) {
            accountCombo.getItems().add(
                    account.getAccountName() + " (" + account.getAccountType().name() + ")");
        }

        // Populate type filter — "All" + each TransactionType
        typeFilterCombo.getItems().add("All types");
        for (TransactionType type : TransactionType.values()) {
            typeFilterCombo.getItems().add(type.name());
        }
        typeFilterCombo.getSelectionModel().selectFirst();

        // Wire table columns
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTimestamp().format(FORMATTER)));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTransactionType().name()));
        colAmount.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAmount()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"));
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().name()));
        colDesc.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
    }

    @FXML
    private void handleLoadHistory() {
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            countLabel.setText("Please select an account.");
            return;
        }

        try {
            List<Transaction> transactions =
                    transactionDAO.findByAccountId(accounts.get(index).getAccountId());

            // Filter by type
            String selectedType = typeFilterCombo.getValue();
            if (selectedType != null && !selectedType.equals("All types")) {
                TransactionType filterType = TransactionType.valueOf(selectedType);
                transactions = transactions.stream()
                        .filter(t -> t.getTransactionType() == filterType)
                        .collect(Collectors.toList());
            }

            // Filter by from date
            LocalDate from = fromDatePicker.getValue();
            if (from != null) {
                transactions = transactions.stream()
                        .filter(t -> !t.getTimestamp().toLocalDate().isBefore(from))
                        .collect(Collectors.toList());
            }

            // Filter by to date
            LocalDate to = toDatePicker.getValue();
            if (to != null) {
                transactions = transactions.stream()
                        .filter(t -> !t.getTimestamp().toLocalDate().isAfter(to))
                        .collect(Collectors.toList());
            }

            historyTable.setItems(FXCollections.observableArrayList(transactions));
            countLabel.setText(transactions.size() + " transaction(s) found.");

        } catch (DatabaseException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Database error. Please try again.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleClearFilters() {
        typeFilterCombo.getSelectionModel().selectFirst();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        historyTable.getItems().clear();
        countLabel.setText("");
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }
}

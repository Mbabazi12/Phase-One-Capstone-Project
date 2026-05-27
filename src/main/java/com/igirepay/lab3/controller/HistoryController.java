package com.igirepay.lab3.controller;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab2.dao.TransactionDAO;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoryController {

    @FXML private ComboBox<String> accountCombo;
    @FXML private TableView<Transaction> historyTable;
    @FXML private TableColumn<Transaction, String> colTxId;
    @FXML private TableColumn<Transaction, String> colRefId;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colDate;

    private final TransactionDAO transactionDAO;
    private List<Account> accounts;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HistoryController() {
        this.transactionDAO = new TransactionDAO();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        accounts = customer.getAccounts();

        for (Account account : accounts) {
            accountCombo.getItems().add(account.getAccountType().name()
                    + " (" + account.getAccountId().toString().substring(0, 8) + "...)");
        }

        colTxId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionId().toString().substring(0, 8) + "..."));
        colRefId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReferenceId().substring(0, 8) + "..."));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionType().name()));
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTimestamp().format(FORMATTER)));
    }

    @FXML
    private void handleLoadHistory() {
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) return;

        try {
            List<Transaction> transactions = transactionDAO.findByAccountId(accounts.get(index).getAccountId());
            historyTable.setItems(FXCollections.observableArrayList(transactions));
        } catch (DatabaseException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("A database error occurred. Please try again.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }
}

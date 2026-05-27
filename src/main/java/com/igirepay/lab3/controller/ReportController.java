package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab2.dao.TransactionDAO;
import com.igirepay.lab2.service.ReportService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ReportController {

    @FXML private ComboBox<String> accountCombo;
    @FXML private Label summaryLabel;
    @FXML private Label exportLabel;
    @FXML private TableView<Transaction> statementTable;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colFee;
    @FXML private TableColumn<Transaction, String> colBalance;

    private final TransactionDAO transactionDAO;
    private final ReportService reportService;
    private List<Account> accounts;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReportController() {
        this.transactionDAO = new TransactionDAO();
        this.reportService  = new ReportService();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        accounts = customer.getAccounts();

        for (Account account : accounts) {
            accountCombo.getItems().add(
                    account.getAccountName() + " (" + account.getAccountType().name() + ")");
        }

        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTimestamp().format(FORMATTER)));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTransactionType().name()));
        colAmount.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAmount()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"));
        colFee.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFee()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"));
        colBalance.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
    }

    @FXML
    private void handleDailySummary() {
        summaryLabel.setText("");
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { summaryLabel.setText("Select an account first."); return; }

        try {
            List<Transaction> txs = transactionDAO.findByDateRange(
                    accounts.get(index).getAccountId(), LocalDate.now(), LocalDate.now());

            BigDecimal deposited   = sum(txs, TransactionType.DEPOSIT);
            BigDecimal withdrawn   = sum(txs, TransactionType.WITHDRAWAL);
            BigDecimal transferIn  = sum(txs, TransactionType.TRANSFER_IN);
            BigDecimal transferOut = sum(txs, TransactionType.TRANSFER_OUT);
            BigDecimal fees        = sum(txs, TransactionType.FEE);

            summaryLabel.setText(
                    "Today  (" + LocalDate.now() + ")  —  " + txs.size() + " transaction(s)\n" +
                    "Deposited:     " + fmt(deposited)   + "\n" +
                    "Withdrawn:     " + fmt(withdrawn)   + "\n" +
                    "Transfer in:   " + fmt(transferIn)  + "\n" +
                    "Transfer out:  " + fmt(transferOut) + "\n" +
                    "Fees:          " + fmt(fees));
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleExportCSV() {
        exportLabel.setText("");
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { exportLabel.setText("Select an account first."); return; }

        try {
            String path = reportService.exportToCSV(
                    accounts.get(index).getAccountId(), null, LocalDate.now());
            exportLabel.setStyle("-fx-text-fill: green;");
            exportLabel.setText("Exported to: " + path);
        } catch (DatabaseException e) {
            showError("Could not export: " + e.getMessage());
        }
    }

    @FXML
    private void handleFullStatement() {
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) return;

        try {
            List<Transaction> txs = transactionDAO.findByAccountId(
                    accounts.get(index).getAccountId());

            BigDecimal running = BigDecimal.ZERO;
            for (Transaction tx : txs) {
                switch (tx.getTransactionType()) {
                    case DEPOSIT, TRANSFER_IN          -> running = running.add(tx.getAmount());
                    case WITHDRAWAL, TRANSFER_OUT, FEE -> running = running.subtract(tx.getAmount());
                }
                tx.setDescription(running.setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF");
            }
            statementTable.setItems(FXCollections.observableArrayList(txs));
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private BigDecimal sum(List<Transaction> txs, TransactionType type) {
        return txs.stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String fmt(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

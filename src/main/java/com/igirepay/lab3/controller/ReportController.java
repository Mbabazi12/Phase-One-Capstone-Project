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
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;
import com.igirepay.lab3.util.CsvExporter;

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
    @FXML private TableColumn<Transaction, String> colTxId;
    @FXML private TableColumn<Transaction, String> colRefId;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colRunningBalance;
    @FXML private TableColumn<Transaction, String> colDate;

    private final TransactionDAO transactionDAO;
    private List<Account> accounts;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReportController() {
        this.transactionDAO = new TransactionDAO();
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        accounts = customer.getAccounts();

        for (Account account : accounts) {
            accountCombo.getItems().add(account.getAccountType().name()
                    + " (ID: " + account.getAccountId() + ")");
        }

        colTxId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getTransactionId())));
        colRefId.setCellValueFactory(d -> {
            String ref = d.getValue().getReferenceId();
            return new SimpleStringProperty(ref.substring(0, Math.min(8, ref.length())) + "...");
        });
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTransactionType().name()));
        colAmount.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAmount()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF"));
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTimestamp().format(FORMATTER)));
        colRunningBalance.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
    }

    @FXML
    private void handleDailySummary() {
        summaryLabel.setText("");
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { summaryLabel.setText("Select an account."); return; }

        try {
            List<Transaction> transactions = transactionDAO.findByDateRange(
                    accounts.get(index).getAccountId(), LocalDate.now(), LocalDate.now());

            BigDecimal deposited   = sumByType(transactions, TransactionType.DEPOSIT);
            BigDecimal withdrawn   = sumByType(transactions, TransactionType.WITHDRAWAL);
            BigDecimal transferIn  = sumByType(transactions, TransactionType.TRANSFER_IN);
            BigDecimal transferOut = sumByType(transactions, TransactionType.TRANSFER_OUT);
            BigDecimal fees        = sumByType(transactions, TransactionType.FEE);

            summaryLabel.setText(
                    "Today's Summary (" + LocalDate.now() + ")\n" +
                    "Transactions:  " + transactions.size() + "\n" +
                    "Deposited:     " + fmt(deposited)   + " RWF\n" +
                    "Withdrawn:     " + fmt(withdrawn)   + " RWF\n" +
                    "Transfer In:   " + fmt(transferIn)  + " RWF\n" +
                    "Transfer Out:  " + fmt(transferOut) + " RWF\n" +
                    "Fees:          " + fmt(fees)        + " RWF"
            );
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleExportCSV() {
        exportLabel.setText("");
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) { exportLabel.setText("Select an account."); return; }

        Account account = accounts.get(index);
        try {
            List<Transaction> transactions = transactionDAO.findByAccountId(account.getAccountId());
            String filePath = CsvExporter.export(account.getAccountId(), transactions);
            exportLabel.setText("Exported to: " + filePath);
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        } catch (Exception e) {
            showError("Could not export file: " + e.getMessage());
        }
    }

    @FXML
    private void handleFullStatement() {
        int index = accountCombo.getSelectionModel().getSelectedIndex();
        if (index < 0) return;

        try {
            List<Transaction> transactions = transactionDAO.findByAccountId(
                    accounts.get(index).getAccountId());
            // Compute running balance and store in description for display
            BigDecimal running = BigDecimal.ZERO;
            for (Transaction tx : transactions) {
                switch (tx.getTransactionType()) {
                    case DEPOSIT, TRANSFER_IN          -> running = running.add(tx.getAmount());
                    case WITHDRAWAL, TRANSFER_OUT, FEE -> running = running.subtract(tx.getAmount());
                }
                tx.setDescription(running.setScale(2, RoundingMode.HALF_UP).toPlainString() + " RWF");
            }
            statementTable.setItems(FXCollections.observableArrayList(transactions));
        } catch (DatabaseException e) {
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String fmt(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

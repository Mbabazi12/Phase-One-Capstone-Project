package com.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.model.Loan;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab2.dao.TransactionDAO;
import com.igirepay.lab2.service.LoanService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML private Label welcomeLabel;

    // Balance cards
    @FXML private Label walletBalanceLabel;
    @FXML private Label savingsBalanceLabel;
    @FXML private Label loanBalanceLabel;

    // Empty state / cards visibility
    @FXML private VBox  emptyStateBox;
    @FXML private HBox  cardsBox;

    // Recent transactions table
    @FXML private TableView<Transaction>          recentTxTable;
    @FXML private TableColumn<Transaction, String> colTxDate;
    @FXML private TableColumn<Transaction, String> colTxType;
    @FXML private TableColumn<Transaction, String> colTxAmount;
    @FXML private TableColumn<Transaction, String> colTxStatus;
    @FXML private TableColumn<Transaction, String> colTxDesc;

    private final LoanService    loanService    = new LoanService();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
                    .map(a -> fmt(a.getBalance())).orElse("No wallet"));
            savingsBalanceLabel.setText(savings
                    .map(a -> fmt(a.getBalance())).orElse("No savings"));

            Optional<Loan> activeLoan = loanService.getActiveLoan(customer.getCustomerId());
            loanBalanceLabel.setText(activeLoan
                    .map(l -> fmt(l.getRemainingBalance())).orElse("No active loan"));

            loadRecentTransactions(customer);
        } else {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            cardsBox.setVisible(false);
            cardsBox.setManaged(false);
            recentTxTable.setVisible(false);
            recentTxTable.setManaged(false);
        }
    }

    private void loadRecentTransactions(Customer customer) {
        // Wire columns
        colTxDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTimestamp().format(DATE_FMT)));
        colTxType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTransactionType().name()));
        colTxAmount.setCellValueFactory(d ->
                new SimpleStringProperty(fmt(d.getValue().getAmount())));
        colTxStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().name()));
        colTxDesc.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));

        // Collect transactions from all accounts, sort newest first, show last 20
        List<Transaction> all = new ArrayList<>();
        for (Account account : customer.getAccounts()) {
            all.addAll(transactionDAO.findByAccountId(account.getAccountId()));
        }
        all.sort(Comparator.comparing(Transaction::getTimestamp).reversed());
        List<Transaction> recent = all.size() > 20 ? all.subList(0, 20) : all;
        recentTxTable.setItems(FXCollections.observableArrayList(recent));
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

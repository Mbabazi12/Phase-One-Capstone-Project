package com.igirepay.lab2.service;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab2.dao.AccountDAO;
import com.igirepay.lab2.dao.TransactionDAO;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class ReportService {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter STATEMENT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public ReportService() {
        this.accountDAO = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    public void exportToCSV(UUID accountId, LocalDate from, LocalDate to, String filePath) {
        List<Transaction> transactions = transactionDAO.findByDateRange(accountId, from, to);
        Path outputPath = Path.of(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("date,type,amount,fee,referenceId,description,status");
            writer.newLine();
            for (Transaction transaction : transactions) {
                writer.write(toCsvLine(transaction));
                writer.newLine();
            }
        } catch (IOException exception) {
            throw new DatabaseException("Could not export transactions to CSV", exception);
        }
    }

    public void printDailySummary(UUID accountId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        List<Transaction> transactions = transactionDAO.findByDateRange(accountId, targetDate, targetDate);

        BigDecimal deposits = total(transactions, TransactionType.DEPOSIT);
        BigDecimal withdrawals = total(transactions, TransactionType.WITHDRAWAL);
        BigDecimal transferIn = total(transactions, TransactionType.TRANSFER_IN);
        BigDecimal transferOut = total(transactions, TransactionType.TRANSFER_OUT);
        BigDecimal fees = total(transactions, TransactionType.FEE);
        BigDecimal netChange = deposits
                .add(transferIn)
                .subtract(withdrawals)
                .subtract(transferOut)
                .subtract(fees);

        System.out.println("Daily summary for " + targetDate + ":");
        System.out.println("Total deposits: " + formatMoney(deposits));
        System.out.println("Total withdrawals: " + formatMoney(withdrawals));
        System.out.println("Total transfers in: " + formatMoney(transferIn));
        System.out.println("Total transfers out: " + formatMoney(transferOut));
        System.out.println("Total fees: " + formatMoney(fees));
        System.out.println("Net change: " + formatMoney(netChange));
    }

    public void printFullStatement(UUID customerId) {
        List<Account> accounts = accountDAO.findByCustomerId(customerId);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException(String.valueOf(customerId));
        }

        for (Account account : accounts) {
            System.out.println();
            System.out.println(account.getAccountType() + " account " + account.getAccountId());
            List<Transaction> transactions = transactionDAO.findByAccountId(account.getAccountId());
            if (transactions.isEmpty()) {
                System.out.println("No transactions found.");
                continue;
            }
            for (Transaction transaction : transactions) {
                System.out.println(formatStatementLine(transaction));
            }
        }
    }

    private String toCsvLine(Transaction transaction) {
        return escape(transaction.getTimestamp().format(STATEMENT_TIME_FORMAT)) + "," +
                escape(transaction.getTransactionType().name()) + "," +
                escape(transaction.getAmount().toPlainString()) + "," +
                escape(transaction.getFee().toPlainString()) + "," +
                escape(transaction.getReferenceId()) + "," +
                escape(transaction.getDescription()) + "," +
                escape(transaction.getStatus().name());
    }

    private String escape(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private BigDecimal total(List<Transaction> transactions, TransactionType transactionType) {
        return transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == transactionType)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatStatementLine(Transaction transaction) {
        String fee = transaction.getFee().compareTo(BigDecimal.ZERO) > 0
                ? ", fee " + formatMoney(transaction.getFee())
                : "";
        return transaction.getTimestamp().format(STATEMENT_TIME_FORMAT) + " | " +
                transaction.getTransactionType() + " | " +
                formatMoney(transaction.getAmount()) + fee + " | " +
                transaction.getStatus() + " | Ref: " +
                transaction.getReferenceId();
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return MONEY_FORMAT.format(safeAmount.setScale(2, RoundingMode.HALF_UP)) + " RWF";
    }
}

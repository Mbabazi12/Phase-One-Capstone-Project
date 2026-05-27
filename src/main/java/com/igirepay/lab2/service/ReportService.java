package com.igirepay.lab2.service;

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

import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab2.dao.AccountDAO;
import com.igirepay.lab2.dao.TransactionDAO;

public class ReportService {
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public ReportService() {
        this.accountDAO = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    public String exportToCSV(int accountId, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionDAO.findByDateRange(accountId, from, to);
        Path dir = Path.of("reports");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            String fileName = "statement_acc" + accountId + "_" + System.currentTimeMillis() + ".csv";
            Path outputPath = dir.resolve(fileName);
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                writer.write("date,type,amount,fee,reference_id,description,status");
                writer.newLine();
                for (Transaction tx : transactions) {
                    writer.write(toCsvLine(tx));
                    writer.newLine();
                }
            }
            return outputPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new DatabaseException("Could not export transactions to CSV", e);
        }
    }

    public void printDailySummary(int accountId, LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        List<Transaction> transactions = transactionDAO.findByDateRange(accountId, target, target);

        BigDecimal deposits    = total(transactions, TransactionType.DEPOSIT);
        BigDecimal withdrawals = total(transactions, TransactionType.WITHDRAWAL);
        BigDecimal transferIn  = total(transactions, TransactionType.TRANSFER_IN);
        BigDecimal transferOut = total(transactions, TransactionType.TRANSFER_OUT);
        BigDecimal fees        = total(transactions, TransactionType.FEE);
        BigDecimal net         = deposits.add(transferIn).subtract(withdrawals).subtract(transferOut).subtract(fees);

        System.out.println("Daily summary for " + target + ":");
        System.out.println("  Deposits:      " + fmt(deposits));
        System.out.println("  Withdrawals:   " + fmt(withdrawals));
        System.out.println("  Transfer in:   " + fmt(transferIn));
        System.out.println("  Transfer out:  " + fmt(transferOut));
        System.out.println("  Fees:          " + fmt(fees));
        System.out.println("  Net change:    " + fmt(net));
    }

    public List<Transaction> getFullStatement(int customerId) {
        List<Account> accounts = accountDAO.findByCustomerId(customerId);
        if (accounts.isEmpty()) throw new AccountNotFoundException(String.valueOf(customerId));
        return accounts.stream()
                .flatMap(a -> transactionDAO.findByAccountId(a.getAccountId()).stream())
                .toList();
    }

    private String toCsvLine(Transaction tx) {
        return escape(tx.getTimestamp().format(TIME_FORMAT)) + "," +
               escape(tx.getTransactionType().name()) + "," +
               escape(tx.getAmount().toPlainString()) + "," +
               escape(tx.getFee().toPlainString()) + "," +
               escape(tx.getReferenceId()) + "," +
               escape(tx.getDescription()) + "," +
               escape(tx.getStatus().name());
    }

    private String escape(String value) {
        String s = value == null ? "" : value;
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private BigDecimal total(List<Transaction> txs, TransactionType type) {
        return txs.stream()
                .filter(t -> t.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String fmt(BigDecimal amount) {
        return MONEY_FORMAT.format((amount == null ? BigDecimal.ZERO : amount)
                .setScale(2, RoundingMode.HALF_UP)) + " RWF";
    }
}

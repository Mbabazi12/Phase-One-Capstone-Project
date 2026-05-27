package com.igirepay.lab3.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.igirepay.lab1.model.Transaction;

public class CsvExporter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String export(int accountId, List<Transaction> transactions) throws IOException {
        Path reportsDir = Path.of("reports");
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }

        String fileName = "statement_acc" + accountId + "_" + System.currentTimeMillis() + ".csv";
        Path outputPath = reportsDir.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("transaction_id,reference_id,type,amount,date");
            writer.newLine();
            for (Transaction tx : transactions) {
                writer.write(
                        escape(String.valueOf(tx.getTransactionId())) + "," +
                        escape(tx.getReferenceId()) + "," +
                        escape(tx.getTransactionType().name()) + "," +
                        escape(tx.getAmount().toPlainString()) + "," +
                        escape(tx.getTimestamp().format(FORMATTER))
                );
                writer.newLine();
            }
        }

        return outputPath.toAbsolutePath().toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

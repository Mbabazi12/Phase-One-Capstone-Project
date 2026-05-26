package com.igirepay.lab2.dao;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionStatus;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab2.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionDAO {
    private static final String TRANSACTION_COLUMNS =
            "transaction_id, reference_id, account_id, target_account_id, transaction_type, amount, fee, " +
                    "status, timestamp, description";

    public Transaction create(Transaction transaction) {
        String sql = "INSERT INTO transactions (" + TRANSACTION_COLUMNS + ") VALUES (?::uuid, ?, ?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?)";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transaction.getTransactionId().toString());
            statement.setString(2, transaction.getReferenceId());
            statement.setString(3, transaction.getAccountId().toString());
            setNullableUuid(statement, 4, transaction.getTargetAccountId());
            statement.setString(5, transaction.getTransactionType().name());
            statement.setBigDecimal(6, transaction.getAmount());
            statement.setBigDecimal(7, transaction.getFee());
            statement.setString(8, transaction.getStatus().name());
            statement.setTimestamp(9, Timestamp.valueOf(transaction.getTimestamp()));
            statement.setString(10, transaction.getDescription());
            statement.executeUpdate();
            return transaction;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create transaction", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Transaction> findByAccountId(UUID accountId) {
        String sql = "SELECT " + TRANSACTION_COLUMNS + " FROM transactions WHERE account_id = ?::uuid ORDER BY timestamp";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactions(resultSet);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find transactions by account", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Transaction> findDailyWithdrawals(UUID accountId, LocalDate date) {
        String sql = "SELECT " + TRANSACTION_COLUMNS + " FROM transactions " +
                "WHERE account_id = ?::uuid AND transaction_type = ? AND status = ? AND timestamp >= ? AND timestamp < ? " +
                "ORDER BY timestamp";
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId.toString());
            statement.setString(2, TransactionType.WITHDRAWAL.name());
            statement.setString(3, TransactionStatus.SUCCESS.name());
            statement.setTimestamp(4, Timestamp.valueOf(targetDate.atStartOfDay()));
            statement.setTimestamp(5, Timestamp.valueOf(targetDate.plusDays(1).atStartOfDay()));
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactions(resultSet);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find daily withdrawals", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Transaction> findByDateRange(UUID accountId, LocalDate from, LocalDate to) {
        String sql = "SELECT " + TRANSACTION_COLUMNS + " FROM transactions " +
                "WHERE account_id = ?::uuid AND timestamp >= ? AND timestamp < ? ORDER BY timestamp";
        LocalDate startDate = from == null ? LocalDate.now() : from;
        LocalDate endDate = to == null ? startDate : to;
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId.toString());
            statement.setTimestamp(2, Timestamp.valueOf(startDate.atStartOfDay()));
            statement.setTimestamp(3, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactions(resultSet);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find transactions by date range", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    private List<Transaction> mapTransactions(ResultSet resultSet) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        while (resultSet.next()) {
            transactions.add(mapTransaction(resultSet));
        }
        return transactions;
    }

    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        String targetId = resultSet.getString("target_account_id");
        return new Transaction(
                UUID.fromString(resultSet.getString("transaction_id")),
                resultSet.getString("reference_id"),
                UUID.fromString(resultSet.getString("account_id")),
                targetId != null ? UUID.fromString(targetId) : null,
                TransactionType.valueOf(resultSet.getString("transaction_type")),
                resultSet.getBigDecimal("amount"),
                resultSet.getBigDecimal("fee"),
                TransactionStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("timestamp").toLocalDateTime(),
                resultSet.getString("description")
        );
    }

    private void setNullableUuid(PreparedStatement statement, int index, UUID value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.OTHER);
            return;
        }
        statement.setString(index, value.toString());
    }

    private void closeIfStandalone(Connection connection) {
        if (DBConnection.isTransactionConnectionActive()) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not close database connection", exception);
        }
    }
}

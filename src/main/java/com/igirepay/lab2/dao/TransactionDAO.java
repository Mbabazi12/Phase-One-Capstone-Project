package com.igirepay.lab2.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Transaction;
import com.igirepay.lab1.model.TransactionStatus;
import com.igirepay.lab1.model.TransactionType;
import com.igirepay.lab2.config.DBConnection;

public class TransactionDAO {
    private static final String TRANSACTION_COLUMNS =
            "transaction_id, reference_id, account_id, target_account_id, transaction_type, " +
            "amount, fee, status, timestamp, description";

    public Transaction create(Transaction transaction) {
        String sql = "INSERT INTO transactions (reference_id, account_id, target_account_id, " +
                     "transaction_type, amount, fee, status, timestamp, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transaction.getReferenceId());
            statement.setInt(2, transaction.getAccountId());
            // target_account_id = 0 means no target — store as NULL
            if (transaction.getTargetAccountId() == 0) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, transaction.getTargetAccountId());
            }
            statement.setString(4, transaction.getTransactionType().name());
            statement.setBigDecimal(5, transaction.getAmount());
            statement.setBigDecimal(6, transaction.getFee());
            statement.setString(7, transaction.getStatus().name());
            statement.setTimestamp(8, Timestamp.valueOf(transaction.getTimestamp()));
            statement.setString(9, transaction.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setTransactionId(keys.getInt(1));
                }
            }
            return transaction;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create transaction", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Transaction> findByAccountId(int accountId) {
        String sql = "SELECT " + TRANSACTION_COLUMNS +
                     " FROM transactions WHERE account_id = ? ORDER BY timestamp";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactions(resultSet);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find transactions by account", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Transaction> findDailyWithdrawals(int accountId, LocalDate date) {
        String sql = "SELECT " + TRANSACTION_COLUMNS +
                     " FROM transactions WHERE account_id = ? AND transaction_type = ? " +
                     "AND status = ? AND timestamp >= ? AND timestamp < ? ORDER BY timestamp";
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
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

    public List<Transaction> findByDateRange(int accountId, LocalDate from, LocalDate to) {
        String sql = "SELECT " + TRANSACTION_COLUMNS +
                     " FROM transactions WHERE account_id = ? AND timestamp >= ? AND timestamp < ? " +
                     "ORDER BY timestamp";
        LocalDate startDate = from == null ? LocalDate.now() : from;
        LocalDate endDate   = to   == null ? startDate       : to;
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
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
        while (resultSet.next()) transactions.add(mapTransaction(resultSet));
        return transactions;
    }

    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        int targetId = resultSet.getInt("target_account_id");
        // getInt returns 0 when the column is NULL — wasNull() confirms it
        if (resultSet.wasNull()) targetId = 0;

        return new Transaction(
                resultSet.getInt("transaction_id"),
                resultSet.getString("reference_id"),
                resultSet.getInt("account_id"),
                targetId,
                TransactionType.valueOf(resultSet.getString("transaction_type")),
                resultSet.getBigDecimal("amount"),
                resultSet.getBigDecimal("fee"),
                TransactionStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("timestamp").toLocalDateTime(),
                resultSet.getString("description")
        );
    }

    private void closeIfStandalone(Connection connection) {
        if (DBConnection.isTransactionConnectionActive()) return;
        try { connection.close(); }
        catch (SQLException exception) { throw new DatabaseException("Could not close database connection", exception); }
    }
}

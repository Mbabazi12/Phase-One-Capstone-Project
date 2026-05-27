package com.igirepay.lab2.dao;

import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.SavingsAccount;
import com.igirepay.lab1.model.WalletAccount;
import com.igirepay.lab2.config.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountDAO {
    private static final String ACCOUNT_COLUMNS =
            "account_id, customer_id, account_type, account_name, balance, created_at, is_active, pin";

    public Account create(Account account, UUID customerId) {
        String sql = "INSERT INTO accounts (" + ACCOUNT_COLUMNS + ") VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?)";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getAccountId().toString());
            statement.setString(2, customerId.toString());
            statement.setString(3, account.getAccountType().name());
            statement.setString(4, account.getAccountName());
            statement.setBigDecimal(5, account.getBalance());
            statement.setTimestamp(6, Timestamp.valueOf(account.getCreatedAt()));
            statement.setBoolean(7, account.isActive());
            statement.setString(8, account.getHashedPin());
            statement.executeUpdate();
            return account;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create account", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public Optional<Account> findById(UUID id) {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts WHERE account_id = ?::uuid";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapAccount(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find account by id", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Account> findByCustomerId(UUID customerId) {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts WHERE customer_id = ?::uuid ORDER BY created_at";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (resultSet.next()) {
                    accounts.add(mapAccount(resultSet));
                }
                return accounts;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find customer accounts", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void updateBalance(UUID accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?::uuid";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, newBalance);
            statement.setString(2, accountId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update account balance", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void updateHashedPin(UUID accountId, String hashedPin) {
        String sql = "UPDATE accounts SET pin = ? WHERE account_id = ?::uuid";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hashedPin);
            statement.setString(2, accountId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update account PIN hash", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void deactivate(UUID accountId) {
        String sql = "UPDATE accounts SET is_active = ? WHERE account_id = ?::uuid";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, false);
            statement.setString(2, accountId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not deactivate account", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        AccountType accountType = AccountType.valueOf(resultSet.getString("account_type"));
        UUID accountId = UUID.fromString(resultSet.getString("account_id"));
        UUID customerId = UUID.fromString(resultSet.getString("customer_id"));
        String accountName = resultSet.getString("account_name");
        BigDecimal balance = resultSet.getBigDecimal("balance");
        String hashedPin = resultSet.getString("pin");

        Account account = accountType == AccountType.WALLET
                ? new WalletAccount(accountId, customerId, accountName, balance, hashedPin)
                : new SavingsAccount(accountId, customerId, accountName, balance, hashedPin);
        account.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        account.setActive(resultSet.getBoolean("is_active"));
        return account;
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

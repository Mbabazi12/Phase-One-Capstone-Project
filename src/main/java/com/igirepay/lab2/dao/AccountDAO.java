package com.igirepay.lab2.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.model.Account;
import com.igirepay.lab1.model.AccountType;
import com.igirepay.lab1.model.SavingsAccount;
import com.igirepay.lab1.model.WalletAccount;
import com.igirepay.lab2.config.DBConnection;

public class AccountDAO {
    private static final String ACCOUNT_COLUMNS =
            "account_id, customer_id, account_type, account_name, balance, created_at, is_active, pin";

    public Account create(Account account, int customerId) {
        String sql = "INSERT INTO accounts (customer_id, account_type, account_name, balance, created_at, is_active, pin)" +
                     " VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, customerId);
            statement.setString(2, account.getAccountType().name());
            statement.setString(3, account.getAccountName());
            statement.setBigDecimal(4, account.getBalance());
            statement.setTimestamp(5, Timestamp.valueOf(account.getCreatedAt()));
            statement.setBoolean(6, account.isActive());
            statement.setString(7, account.getHashedPin());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setAccountId(keys.getInt(1));
                }
            }
            return account;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create account", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public Optional<Account> findById(int id) {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts WHERE account_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return Optional.of(mapAccount(resultSet));
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find account by id", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Account> findByCustomerId(int customerId) {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts WHERE customer_id = ? ORDER BY created_at";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (resultSet.next()) accounts.add(mapAccount(resultSet));
                return accounts;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find customer accounts", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void updateBalance(int accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, newBalance);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update account balance", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void updateHashedPin(int accountId, String hashedPin) {
        String sql = "UPDATE accounts SET pin = ? WHERE account_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hashedPin);
            statement.setInt(2, accountId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update account PIN", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void deactivate(int accountId) {
        String sql = "UPDATE accounts SET is_active = FALSE WHERE account_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not deactivate account", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        AccountType accountType = AccountType.valueOf(resultSet.getString("account_type"));
        int accountId   = resultSet.getInt("account_id");
        int customerId  = resultSet.getInt("customer_id");
        String accountName = resultSet.getString("account_name");
        BigDecimal balance = resultSet.getBigDecimal("balance");
        String hashedPin   = resultSet.getString("pin");

        Account account = accountType == AccountType.WALLET
                ? new WalletAccount(accountId, customerId, accountName, balance, hashedPin)
                : new SavingsAccount(accountId, customerId, accountName, balance, hashedPin);
        account.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        account.setActive(resultSet.getBoolean("is_active"));
        return account;
    }

    private void closeIfStandalone(Connection connection) {
        if (DBConnection.isTransactionConnectionActive()) return;
        try { connection.close(); }
        catch (SQLException exception) { throw new DatabaseException("Could not close database connection", exception); }
    }
}

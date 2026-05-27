package com.igirepay.lab2.dao;

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
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab2.config.DBConnection;

public class CustomerDAO {
    private static final String CUSTOMER_COLUMNS =
            "customer_id, full_name, phone_number, pin, created_at";

    public Customer create(Customer customer) {
        String sql = "INSERT INTO customers (full_name, phone_number, pin, created_at) VALUES (?, ?, ?, ?)";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getPhoneNumber());
            statement.setString(3, customer.getHashedPin());
            statement.setTimestamp(4, Timestamp.valueOf(customer.getCreatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setCustomerId(keys.getInt(1));
                }
            }
            return customer;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not create customer", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public Optional<Customer> findByPhone(String phone) {
        String sql = "SELECT " + CUSTOMER_COLUMNS + " FROM customers WHERE phone_number = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return Optional.of(mapCustomer(resultSet));
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find customer by phone", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public Optional<Customer> findById(int id) {
        String sql = "SELECT " + CUSTOMER_COLUMNS + " FROM customers WHERE customer_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return Optional.of(mapCustomer(resultSet));
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not find customer by id", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public List<Customer> findAll() {
        String sql = "SELECT " + CUSTOMER_COLUMNS + " FROM customers ORDER BY created_at";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Customer> customers = new ArrayList<>();
            while (resultSet.next()) customers.add(mapCustomer(resultSet));
            return customers;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not list customers", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void update(Customer customer) {
        String sql = "UPDATE customers SET full_name = ?, pin = ? WHERE customer_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getHashedPin());
            statement.setInt(3, customer.getCustomerId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update customer", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not delete customer", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    private Customer mapCustomer(ResultSet resultSet) throws SQLException {
        return new Customer(
                resultSet.getInt("customer_id"),
                resultSet.getString("full_name"),
                resultSet.getString("phone_number"),
                resultSet.getString("pin"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private void closeIfStandalone(Connection connection) {
        if (DBConnection.isTransactionConnectionActive()) return;
        try { connection.close(); }
        catch (SQLException exception) { throw new DatabaseException("Could not close database connection", exception); }
    }
}

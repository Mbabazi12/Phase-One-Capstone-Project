package com.igirepay.lab2.dao;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab2.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProcessedRequestDAO {
    public boolean exists(String referenceId) {
        String sql = "SELECT 1 FROM processed_requests WHERE reference_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, referenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not check processed request", exception);
        } finally {
            closeIfStandalone(connection);
        }
    }

    public void insert(String referenceId) {
        String sql = "INSERT INTO processed_requests (reference_id) VALUES (?)";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, referenceId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save processed request", exception);
        } finally {
            closeIfStandalone(connection);
        }
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

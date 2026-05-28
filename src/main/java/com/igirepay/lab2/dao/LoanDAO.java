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
import com.igirepay.lab1.model.Loan;
import com.igirepay.lab1.model.Loan.LoanStatus;
import com.igirepay.lab2.config.DBConnection;

public class LoanDAO {

    public Loan create(Loan loan) {
        String sql = "INSERT INTO loans (customer_id, principal, total_repayable, " +
                     "amount_paid, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, loan.getCustomerId());
            stmt.setBigDecimal(2, loan.getPrincipal());
            stmt.setBigDecimal(3, loan.getTotalRepayable());
            stmt.setBigDecimal(4, loan.getAmountPaid());
            stmt.setString(5, loan.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(loan.getCreatedAt()));
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) loan.setLoanId(keys.getInt(1));
            }
            return loan;
        } catch (SQLException e) {
            throw new DatabaseException("Could not create loan", e);
        } finally {
            closeIfStandalone(connection);
        }
    }

    /** Returns the single ACTIVE loan for a customer, if any. */
    public Optional<Loan> findActiveLoan(int customerId) {
        String sql = "SELECT * FROM loans WHERE customer_id = ? AND status = ? LIMIT 1";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setString(2, LoanStatus.ACTIVE.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapLoan(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not find active loan", e);
        } finally {
            closeIfStandalone(connection);
        }
    }

    /** Returns all loans for a customer (active + paid). */
    public List<Loan> findByCustomerId(int customerId) {
        String sql = "SELECT * FROM loans WHERE customer_id = ? ORDER BY created_at DESC";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Loan> loans = new ArrayList<>();
                while (rs.next()) loans.add(mapLoan(rs));
                return loans;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not find loans", e);
        } finally {
            closeIfStandalone(connection);
        }
    }

    /** Update amount_paid and status after a repayment. */
    public void updateRepayment(int loanId, BigDecimal newAmountPaid, LoanStatus status) {
        String sql = "UPDATE loans SET amount_paid = ?, status = ? WHERE loan_id = ?";
        Connection connection = DBConnection.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newAmountPaid);
            stmt.setString(2, status.name());
            stmt.setInt(3, loanId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not update loan repayment", e);
        } finally {
            closeIfStandalone(connection);
        }
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        return new Loan(
                rs.getInt("loan_id"),
                rs.getInt("customer_id"),
                rs.getBigDecimal("principal"),
                rs.getBigDecimal("total_repayable"),
                rs.getBigDecimal("amount_paid"),
                LoanStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private void closeIfStandalone(Connection connection) {
        if (DBConnection.isTransactionConnectionActive()) return;
        try { connection.close(); }
        catch (SQLException e) { throw new DatabaseException("Could not close connection", e); }
    }
}

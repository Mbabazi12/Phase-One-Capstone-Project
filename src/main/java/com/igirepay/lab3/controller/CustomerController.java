package com.igirepay.lab3.controller;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InvalidPhoneNumberException;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.service.AuthService;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class CustomerController {

    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField pinField;
    @FXML private Label messageLabel;

    private final AuthService authService;
    private final CustomerService customerService;

    public CustomerController() {
        this.customerService = new CustomerService();
        this.authService = new AuthService(customerService);
    }

    @FXML
    public void initialize() {
        // If a customer is logged in, pre-fill fields for update
        Customer customer = SessionManager.getCurrentCustomer();
        if (customer != null) {
            fullNameField.setText(customer.getFullName());
            phoneField.setText(customer.getPhoneNumber());
            phoneField.setDisable(true);
        }
    }

    @FXML
    private void handleRegister() {
        messageLabel.setText("");
        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String pin = pinField.getText().trim();

        if (fullName.isEmpty() || phone.isEmpty() || pin.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        try {
            authService.register(fullName, phone, pin);
            messageLabel.setText("Registration successful! You can now log in.");
            fullNameField.clear();
            phoneField.clear();
            pinField.clear();
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
        } catch (InvalidPhoneNumberException e) {
            messageLabel.setText("Phone number must be exactly 10 digits.");
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleUpdate() {
        messageLabel.setText("");
        Customer customer = SessionManager.getCurrentCustomer();
        if (customer == null) return;

        String fullName = fullNameField.getText().trim();
        if (fullName.isEmpty()) {
            messageLabel.setText("Full name is required.");
            return;
        }

        try {
            customer.setFullName(fullName);
            customerService.update(customer);
            SessionManager.setCurrentCustomer(customerService.refresh(customer));
            messageLabel.setText("Profile updated successfully.");
        } catch (DatabaseException e) {
            showError("Database error. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        if (SessionManager.getCurrentCustomer() != null) {
            SceneManager.switchScene("/fxml/dashboard.fxml");
        } else {
            SceneManager.switchScene("/fxml/login.fxml");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

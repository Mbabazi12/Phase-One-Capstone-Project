package com.igirepay.lab3.controller;

import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InvalidPinFormatException;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.service.AuthService;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class ChangePinController {

    @FXML private PasswordField currentPinField;
    @FXML private PasswordField newPinField;
    @FXML private PasswordField confirmPinField;
    @FXML private Label messageLabel;

    private final CustomerService customerService;

    public ChangePinController() {
        this.customerService = new CustomerService();
    }

    @FXML
    private void handleUpdatePin() {
        messageLabel.setText("");
        String currentPin = currentPinField.getText().trim();
        String newPin = newPinField.getText().trim();
        String confirmPin = confirmPinField.getText().trim();

        if (currentPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        if (!newPin.equals(confirmPin)) {
            messageLabel.setText("New PIN and confirmation do not match.");
            return;
        }

        Customer customer = SessionManager.getCurrentCustomer();
        if (!customer.validatePin(currentPin)) {
            messageLabel.setText("Current PIN is incorrect.");
            return;
        }

        try {
            AuthService.validatePinFormat(newPin);
            customer.setHashedPin(AuthService.hashPin(newPin));
            customerService.update(customer);
            SessionManager.setCurrentCustomer(customerService.refresh(customer));
            messageLabel.setText("PIN updated successfully.");
            currentPinField.clear();
            newPinField.clear();
            confirmPinField.clear();
        } catch (InvalidPinFormatException e) {
            messageLabel.setText(e.getMessage());
        } catch (DatabaseException e) {
            messageLabel.setText("Database error. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/fxml/dashboard.fxml");
    }
}

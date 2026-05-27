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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CustomerController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     phoneField;
    @FXML private VBox          pinSection;
    @FXML private PasswordField pinField;
    @FXML private PasswordField confirmPinField;
    @FXML private Label         messageLabel;

    private final AuthService    authService;
    private final CustomerService customerService;

    public CustomerController() {
        this.customerService = new CustomerService();
        this.authService     = new AuthService(customerService);
    }

    @FXML
    public void initialize() {
        Customer customer = SessionManager.getCurrentCustomer();
        if (customer != null) {
            // Update mode — pre-fill name and phone, hide PIN section
            fullNameField.setText(customer.getFullName());
            phoneField.setText(customer.getPhoneNumber());
            phoneField.setDisable(true);
            pinSection.setVisible(false);
            pinSection.setManaged(false);
        }
    }

    @FXML
    private void handleRegister() {
        messageLabel.setText("");
        String fullName    = fullNameField.getText().trim();
        String phone       = phoneField.getText().trim();
        String pin         = pinField.getText().trim();
        String confirmPin  = confirmPinField.getText().trim();

        if (fullName.isEmpty() || phone.isEmpty() || pin.isEmpty() || confirmPin.isEmpty()) {
            setError("All fields are required.");
            return;
        }

        if (!pin.equals(confirmPin)) {
            setError("PINs do not match. Please try again.");
            pinField.clear();
            confirmPinField.clear();
            pinField.requestFocus();
            return;
        }

        try {
            authService.register(fullName, phone, pin);
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Registration successful! You can now log in.");
            fullNameField.clear();
            phoneField.clear();
            pinField.clear();
            confirmPinField.clear();
        } catch (IllegalArgumentException e) {
            setError(e.getMessage());
        } catch (InvalidPhoneNumberException e) {
            setError("Phone number must be exactly 10 digits.");
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
            setError("Full name is required.");
            return;
        }

        try {
            customer.setFullName(fullName);
            customerService.update(customer);
            SessionManager.setCurrentCustomer(customerService.refresh(customer));
            messageLabel.setStyle("-fx-text-fill: green;");
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

    private void setError(String message) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

package com.igirepay.lab3.controller;

import com.igirepay.lab1.exceptions.AccountLockedException;
import com.igirepay.lab1.exceptions.AccountNotFoundException;
import com.igirepay.lab1.exceptions.DatabaseException;
import com.igirepay.lab1.exceptions.InvalidPinException;
import com.igirepay.lab1.model.Customer;
import com.igirepay.lab1.service.AuthService;
import com.igirepay.lab1.service.CustomerService;
import com.igirepay.lab3.ui.SceneManager;
import com.igirepay.lab3.ui.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField phoneField;
    @FXML private PasswordField pinField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    private final AuthService authService;
    private int failedAttempts = 0;

    public LoginController() {
        CustomerService customerService = new CustomerService();
        this.authService = new AuthService(customerService);
    }

    @FXML
    private void handleLogin() {
        errorLabel.setText("");
        String phone = phoneField.getText().trim();
        String pin = pinField.getText().trim();

        if (phone.isEmpty() || pin.isEmpty()) {
            errorLabel.setText("Phone number and PIN are required.");
            return;
        }

        try {
            Customer customer = authService.login(phone, pin);
            SessionManager.setCurrentCustomer(customer);
            SceneManager.switchScene("/fxml/dashboard.fxml");
        } catch (AccountLockedException e) {
            failedAttempts = 3;
            loginButton.setDisable(true);
            errorLabel.setText("Too many failed attempts. Please restart the application.");
        } catch (InvalidPinException e) {
            failedAttempts++;
            if (failedAttempts >= 3) {
                loginButton.setDisable(true);
                errorLabel.setText("Too many failed attempts. Please restart the application.");
            } else {
                errorLabel.setText("Invalid PIN. " + e.getAttemptsRemaining() + " attempt(s) remaining.");
            }
        } catch (AccountNotFoundException e) {
            errorLabel.setText("No account found for that phone number.");
        } catch (DatabaseException e) {
            errorLabel.setText("Database error. Please try again.");
        }
    }

    @FXML
    private void handleGoToRegister() {
        SceneManager.switchScene("/fxml/customer.fxml");
    }
}

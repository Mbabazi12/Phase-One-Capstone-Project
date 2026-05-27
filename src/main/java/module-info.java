module com.igirepay {
    requires java.sql;
    requires org.postgresql.jdbc;
    requires javafx.controls;
    requires javafx.fxml;

    exports com.igirepay.lab1.model;
    exports com.igirepay.lab1.exceptions;
    exports com.igirepay.lab1.service;
    exports com.igirepay.lab2.config;
    exports com.igirepay.lab2.dao;
    exports com.igirepay.lab2.service;
    exports com.igirepay.lab3.ui;
    exports com.igirepay.lab3.controller;
    exports com.igirepay.lab3.util;
    exports com.igirepay;

    opens com.igirepay.lab3.controller to javafx.fxml;
    opens com.igirepay.lab3.ui to javafx.fxml;
}

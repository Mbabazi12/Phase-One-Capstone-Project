module com.igirepay {
    requires java.sql;
    requires org.postgresql.jdbc;
    exports com.igirepay.lab1.model;
    exports com.igirepay.lab1.exceptions;
    exports com.igirepay.lab1.service;
    exports com.igirepay.lab2.config;
    exports com.igirepay.lab2.dao;
    exports com.igirepay.lab2.service;
    exports com.igirepay;
}

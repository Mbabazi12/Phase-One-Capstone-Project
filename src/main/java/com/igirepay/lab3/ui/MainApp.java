package com.igirepay.lab3.ui;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("IgirePay Wallet System");
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        SceneManager.init(stage);
        SceneManager.switchScene("/fxml/login.fxml");
        stage.show();
    }
}

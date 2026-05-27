package com.igirepay.lab3.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Could not load screen: " + fxmlPath, e);
        }
    }

    public static <T> T switchSceneAndGetController(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            return loader.getController();
        } catch (IOException e) {
            throw new RuntimeException("Could not load screen: " + fxmlPath, e);
        }
    }

    public static Stage getStage() {
        return stage;
    }
}

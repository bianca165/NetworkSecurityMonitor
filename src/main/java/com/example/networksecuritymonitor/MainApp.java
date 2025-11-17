package com.example.networksecuritymonitor;

import com.example.networksecuritymonitor.controller.DashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    private DashboardController dashboardController;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        BorderPane root = fxmlLoader.load();
        Scene scene = new Scene(root, 1200, 750);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

        dashboardController = fxmlLoader.getController();
        stage.setTitle("Network Security Monitor");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("[INFO] inchidere...");
        if (dashboardController != null) {
            dashboardController.stopScheduler();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}

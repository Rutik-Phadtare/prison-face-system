package com.prison.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CoAdminDashboardController {

    @FXML
    private Label prisonerCountLabel;

    @FXML
    public void initialize() {
        // Later connect DAO
        prisonerCountLabel.setText("25");
    }

    @FXML
    public void openPrisoners() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/prisoner_management.fxml")
            );

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) prisonerCountLabel.getScene().getWindow();
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

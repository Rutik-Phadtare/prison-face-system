package com.prison.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class AdminDashboardController {

    /**
     * Opens the Prisoner Management screen
     */
    @FXML
    public void openPrisonerManagement() {

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/prisoner_management.fxml")
            );

            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Prisoner Management");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the Guard Management screen
     * (Will be implemented in Phase 3)
     */
    @FXML
    public void openGuardManagement() {

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/guard_management.fxml")
            );

            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Guard Management");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Opens the Face Recognition screen
     * (Will be implemented after Guard Management)
     */
    @FXML
    public void openFaceRecognition() {

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/face_recognition.fxml")
            );

            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Face Recognition");
            stage.show();

        } catch (Exception e) {
            System.out.println("Face Recognition not implemented yet.");
            e.printStackTrace();
        }
    }

    /**
     * Logout and return to Login screen
     */
    @FXML
    public void logout(ActionEvent event) {

        try {
            // Close current dashboard window
            Stage currentStage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // Open login screen again
            Stage loginStage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml")
            );

            loginStage.setScene(new Scene(loader.load()));
            loginStage.setTitle("Login");
            loginStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

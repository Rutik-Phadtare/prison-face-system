package com.prison.controller;

import com.prison.model.User;
import com.prison.session.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class AdminDashboardController {

    private void animate(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(600), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
    /* =========================
       OPTIONAL: BUTTON REFERENCES
       (Add fx:id in FXML if not already)
       ========================= */
    @FXML
    private Button guardManagementBtn;

    @FXML
    private Button faceRecognitionBtn;

    /* =========================
       INITIALIZE (ROLE CHECK)
       ========================= */
    @FXML
    public void initialize() {

        User user = UserSession.getUser();

        // Safety check
        if (user == null) {
            return;
        }

        // If Co-Admin, hide Admin-only features
        if ("CO_ADMIN".equals(user.getRole())) {

            if (guardManagementBtn != null) {
                guardManagementBtn.setVisible(false);
                guardManagementBtn.setManaged(false);
            }

            if (faceRecognitionBtn != null) {
                faceRecognitionBtn.setVisible(false);
                faceRecognitionBtn.setManaged(false);
            }
        }
    }
    @FXML
    public void openRecognitionLogs() {

        if (!isAdmin()) return;

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/recognition_logs.fxml")
            );

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/modern.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("Recognition Logs");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /* =========================
       PRISONER MANAGEMENT
       (Admin + Co-Admin allowed)
       ========================= */
    @FXML
    public void openPrisonerManagement() {

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/prisoner_management.fxml")
            );

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/modern.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("Prisoner Management");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
       GUARD MANAGEMENT
       (ADMIN ONLY)
       ========================= */
    @FXML
    public void openGuardManagement() {

        if (!isAdmin()) {
            System.out.println("Access denied: Guard Management is Admin-only");
            return;
        }

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/guard_management.fxml")
            );

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/modern.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("Guard Management");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
       FACE RECOGNITION
       (ADMIN ONLY)
       ========================= */
    @FXML
    public void openFaceRecognition() {

        if (!isAdmin()) {
            System.out.println("Access denied: Face Recognition is Admin-only");
            return;
        }

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/face_recognition.fxml")
            );

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/modern.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("Face Recognition");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
       LOGOUT
       ========================= */
    @FXML
    public void logout(ActionEvent event) {

        try {
            // Clear session
            UserSession.clear();

            // Close current window
            Stage currentStage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // Open login screen
            Stage loginStage = new Stage();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml")
            );

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/modern.css").toExternalForm()
            );

            loginStage.setScene(scene);
            loginStage.setTitle("Login");
            loginStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
       HELPER: ADMIN CHECK
       ========================= */
    private boolean isAdmin() {
        User user = UserSession.getUser();
        return user != null && "ADMIN".equals(user.getRole());
    }
}

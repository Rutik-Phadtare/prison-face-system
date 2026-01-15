package com.prison.controller;

import com.prison.model.User;
import com.prison.service.AuthService;
import com.prison.session.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    /* =========================
       ADMIN LOGIN BUTTON
       ========================= */
    @FXML
    public void handleLogin() {
        loginWithRole("ADMIN");
    }

    /* =========================
       CO-ADMIN LOGIN BUTTON
       ========================= */
    @FXML
    public void handleCoAdminLogin() {
        loginWithRole("CO_ADMIN");
    }

    /* =========================
       COMMON LOGIN LOGIC
       ========================= */
    private void loginWithRole(String expectedRole) {

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Enter username and password");
            return;
        }

        // 🔐 Authenticate user
        User user = authService.authenticate(username, password);

        if (user == null) {
            messageLabel.setText("Invalid credentials");
            return;
        }

        // 🔒 Role check
        if (!expectedRole.equals(user.getRole())) {
            messageLabel.setText("Unauthorized access for role: " + user.getRole());
            return;
        }

        // ✅ STORE USER IN SESSION (CORRECT PLACE)
        UserSession.setUser(user);

        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader;

            if ("ADMIN".equals(user.getRole())) {
                loader = new FXMLLoader(
                        getClass().getResource("/fxml/admin_dashboard.fxml")
                );
            } else {
                loader = new FXMLLoader(
                        getClass().getResource("/fxml/coadmin_dashboard.fxml")
                );
            }

            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );

            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Unable to load dashboard");
        }
    }
}

package com.prison.controller;

import com.prison.model.User;
import com.prison.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin() throws IOException {

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Enter username and password");
            return;
        }

        User user = authService.authenticate(username, password);

        if (user == null) {
            messageLabel.setText("Invalid credentials");
            return;
        }

        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader;

            if ("ADMIN".equals(user.getRole())) {
                loader = new FXMLLoader(
                        getClass().getResource("/fxml/admin_dashboard.fxml"));
            } else {
                loader = new FXMLLoader(
                        getClass().getResource("/fxml/coadmin_dashboard.fxml"));
            }

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );
            stage.setScene(scene);


        } catch (Exception e) {
            e.printStackTrace();
        }
        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/admin_dashboard.fxml")
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );

        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(scene);
    }

}

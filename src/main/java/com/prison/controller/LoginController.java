package com.prison.controller;

import com.prison.model.User;
import com.prison.service.AuthService;
import com.prison.session.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private StackPane passwordWrapper;
    @FXML private Label messageLabel;

    private final boolean[] loginPasswordShowing = {false};
    private TextField loginVisibleField;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        loginVisibleField = new TextField();
        loginVisibleField.setPromptText("Password");
        loginVisibleField.setVisible(false);
        loginVisibleField.setManaged(false);
        loginVisibleField.setMaxWidth(Double.MAX_VALUE);
        loginVisibleField.setStyle(
                "-fx-background-color: #020617;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 6 36 6 10;"
        );
        loginVisibleField.getStyleClass().add("login-input");

        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!loginVisibleField.isFocused()) loginVisibleField.setText(n);
        });
        loginVisibleField.textProperty().addListener((obs, o, n) -> {
            if (!passwordField.isFocused()) passwordField.setText(n);
        });

        Button eyeBtn = new Button("👁");
        eyeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 8 0 0;" +
                        "-fx-border-width: 0;"
        );
        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);

        eyeBtn.setOnAction(e -> {
            loginPasswordShowing[0] = !loginPasswordShowing[0];
            if (loginPasswordShowing[0]) {
                loginVisibleField.setText(passwordField.getText());
                loginVisibleField.setVisible(true);   loginVisibleField.setManaged(true);
                passwordField.setVisible(false);       passwordField.setManaged(false);
                eyeBtn.setText("🙈");
            } else {
                passwordField.setText(loginVisibleField.getText());
                passwordField.setVisible(true);       passwordField.setManaged(true);
                loginVisibleField.setVisible(false);  loginVisibleField.setManaged(false);
                eyeBtn.setText("👁");
            }
        });

        passwordWrapper.getChildren().addAll(loginVisibleField, eyeBtn);
    }

    @FXML public void handleLogin()        { loginWithRole("ADMIN");    }
    @FXML public void handleCoAdminLogin() { loginWithRole("CO_ADMIN"); }


    private void loginWithRole(String expectedRole) {
        String username = usernameField.getText().trim();
        String password = loginPasswordShowing[0] ? loginVisibleField.getText() : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Enter username and password");
            return;
        }

        // Visual feedback for the user
        messageLabel.setText("Authenticating...");
        usernameField.setDisable(true);
        passwordField.setDisable(true);

        // 1. Run DB authentication on a background thread
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return authService.authenticate(username, password);
            }
        };

        // 2. Handle successful authentication
        loginTask.setOnSucceeded(event -> {
            User user = loginTask.getValue();

            if (user == null) {
                showLoginError("Invalid credentials");
            } else if (!expectedRole.equals(user.getRole())) {
                showLoginError("Unauthorized role: " + user.getRole());
            } else {
                UserSession.setUser(user);
                switchToMainShell(user);
            }
        });

        // 3. Handle connection failures
        loginTask.setOnFailed(event -> showLoginError("Database Connection Error"));

        new Thread(loginTask).start();
    }

    private void showLoginError(String message) {
        usernameField.setDisable(false);
        passwordField.setDisable(false);
        messageLabel.setText(message);
    }

    private void switchToMainShell(User user) {
        try {
            // A. Prepare the Shell and Controller
            FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/main_shell.fxml"));
            Parent shellRoot = shellLoader.load();
            MainShellController shellCtrl = shellLoader.getController();

            // B. Prepare Scene/Stage
            Scene scene = usernameField.getScene();
            Stage stage = (Stage) scene.getWindow();

            // C. Pre-apply CSS to prevent "unformatted flash"
            java.net.URL css = getClass().getResource("/css/modern.css");
            if (css == null) css = getClass().getResource("/css/style.css");
            if (css != null) shellRoot.getStylesheets().add(css.toExternalForm());

            // D. SWAP ROOT (Fastest transition - stage stays open)
            shellRoot.setOpacity(0);
            scene.setRoot(shellRoot);
            stage.setMaximized(true);
            stage.setTitle("Prison Management System - " + user.getRole());

            // E. Navigate to specific dashboard
            String startDashboard = "ADMIN".equals(user.getRole())
                    ? "/fxml/admin_dashboard.fxml"
                    : "/fxml/coadmin_dashboard.fxml";
            shellCtrl.navigate(startDashboard, "Dashboard");

            // F. Smooth Fade Transition
            FadeTransition fade = new FadeTransition(Duration.millis(450), shellRoot);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Fatal Error: Unable to load Shell");
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════
       FORGET PASSWORD / ACCOUNT DIALOG (Security Pass Key Section)
       ═══════════════════════════════════════════════════════════════════════ */
    @FXML
    public void handleacount() {
        String secretKey = "PRISON-RESET-2025";
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Security Verification");
        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(0);
        root.setPrefWidth(480);
        root.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #999999; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 18, 0, 0, 4);");

        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(0, 0, 0, 10));
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPrefHeight(32);
        titleBar.setStyle("-fx-background-color: linear-gradient(to bottom, #4a7bc8, #2a5aa8);");

        Label titleText = new Label("Security Verification — Account Access");
        titleText.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setPrefSize(46, 32);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #c42b1c; -fx-text-fill: white;"));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;"));

        titleBar.getChildren().addAll(titleText, spacer, closeBtn);

        HBox headerSection = new HBox(16);
        headerSection.setPadding(new Insets(24, 28, 16, 28));
        headerSection.setAlignment(Pos.CENTER_LEFT);
        headerSection.setStyle("-fx-background-color: #ffffff;");

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinWidth(54); iconBox.setMinHeight(54);
        iconBox.setStyle("-fx-background-color: #dce6f7; -fx-background-radius: 4; -fx-border-color: #b0c4e8;");
        Label shieldIcon = new Label("🔐"); shieldIcon.setStyle("-fx-font-size: 26px;");
        iconBox.getChildren().add(shieldIcon);

        VBox headerText = new VBox(4);
        Label headerTitle = new Label("Access Restricted");
        headerTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label headerSub = new Label("This area is restricted to authorized administrators only.\nPlease enter your security pass key to proceed.");
        headerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555; -fx-wrap-text: true;");
        headerText.getChildren().addAll(headerTitle, headerSub);
        headerSection.getChildren().addAll(iconBox, headerText);

        VBox formBody = new VBox(14);
        formBody.setPadding(new Insets(20, 28, 20, 28));
        formBody.setStyle("-fx-background-color: #f5f5f5;");

        Label fieldLabel = new Label("Pass Key:");
        fieldLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        StackPane passWrapper = new StackPane();
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your pass key");
        passField.setPrefHeight(36);
        passField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #aaaaaa; -fx-border-radius: 3; -fx-padding: 6 40 6 10;");

        TextField visibleField = new TextField();
        visibleField.setPromptText("Enter your pass key");
        visibleField.setPrefHeight(36);
        visibleField.setVisible(false); visibleField.setManaged(false);
        visibleField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #aaaaaa; -fx-border-radius: 3; -fx-padding: 6 40 6 10;");

        passField.textProperty().addListener((obs, o, n) -> visibleField.setText(n));
        visibleField.textProperty().addListener((obs, o, n) -> passField.setText(n));

        Button eyeBtn2 = new Button("👁");
        eyeBtn2.setStyle("-fx-background-color: transparent; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 8 0 0;");
        StackPane.setAlignment(eyeBtn2, Pos.CENTER_RIGHT);

        final boolean[] showing = {false};
        eyeBtn2.setOnAction(e -> {
            showing[0] = !showing[0];
            if (showing[0]) {
                visibleField.setVisible(true); visibleField.setManaged(true);
                passField.setVisible(false); passField.setManaged(false); eyeBtn2.setText("🙈");
            } else {
                passField.setVisible(true); passField.setManaged(true);
                visibleField.setVisible(false); visibleField.setManaged(false); eyeBtn2.setText("👁");
            }
        });

        passWrapper.getChildren().addAll(passField, visibleField, eyeBtn2);
        Label errorLabel = new Label("⚠   Incorrect pass key. Access denied.");
        errorLabel.setStyle("-fx-text-fill: #c42b1c; -fx-font-size: 12px;");
        errorLabel.setVisible(false); errorLabel.setManaged(false);
        formBody.getChildren().addAll(fieldLabel, passWrapper, errorLabel);

        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(14, 20, 14, 20));
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setStyle("-fx-background-color: #e8e8e8;");

        Button cancelBtn  = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #e1e1e1; -fx-border-color: #adadad; -fx-cursor: hand;");
        Button confirmBtn = new Button("Verify Access");
        confirmBtn.setStyle("-fx-background-color: #2a5aa8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        buttonBar.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(titleBar, headerSection, new Separator(), formBody, new Separator(), buttonBar);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        closeBtn.setOnAction(e  -> dialog.close());
        cancelBtn.setOnAction(e  -> dialog.close());

        confirmBtn.setOnAction(e -> {
            String input = passField.getText();
            if (input.equals(secretKey)) {
                dialog.close();
                try { loadAccountManagement(); } catch (IOException ex) { ex.printStackTrace(); }
            } else {
                errorLabel.setVisible(true); errorLabel.setManaged(true);
                TranslateTransition shake = new TranslateTransition(Duration.millis(50), passWrapper);
                shake.setByX(10); shake.setCycleCount(6); shake.setAutoReverse(true); shake.play();
            }
        });

        dialog.showAndWait();
    }

    private void loadAccountManagement() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_account_management.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle("Account Management");
        stage.show();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setContentText(message);
        alert.showAndWait();
    }
}
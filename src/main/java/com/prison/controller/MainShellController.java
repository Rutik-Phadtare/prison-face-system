package com.prison.controller;

import com.prison.session.UserSession;
import com.prison.model.User;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

public class MainShellController {

    @FXML private StackPane contentArea;
    @FXML private Button    backBtn;
    @FXML private Button    forwardBtn;
    @FXML private Button    homeBtn;
    @FXML private Label     breadcrumbLabel;
    @FXML private Label     shellUserLabel;

    // ── Singleton so AdminDashboardController can reach it ────────────────
    private static MainShellController instance;

    // ── Navigation history ────────────────────────────────────────────────
    private static class NavEntry {
        final String fxmlPath;
        final String title;
        NavEntry(String p, String t) { fxmlPath = p; title = t; }
    }

    private final Deque<NavEntry> backStack    = new ArrayDeque<>();
    private final Deque<NavEntry> forwardStack = new ArrayDeque<>();
    private NavEntry current;

    // ════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        instance = this;
        backBtn.setDisable(true);
        forwardBtn.setDisable(true);

        // Show logged-in user
        User u = UserSession.getUser();
        if (u != null && shellUserLabel != null) {
            shellUserLabel.setText("● " + u.getUsername().toUpperCase() + " [" + u.getRole() + "]");

            // AUTO-LOAD correct dashboard on start
            goHome();
        }

        // Hover effects on nav buttons
        styleNavBtn(backBtn);
        styleNavBtn(forwardBtn);
        styleNavBtn(homeBtn);
    }
    public static MainShellController getInstance() { return instance; }

    public void navigate(String fxmlPath, String title) {
        // Only push to backStack if we actually have a current page to leave
        if (current != null) {
            backStack.push(current);
            forwardStack.clear();
        }

        current = new NavEntry(fxmlPath, title);
        loadPage(fxmlPath, title, true);
        updateNavButtons();
    }
    // ── BACK ─────────────────────────────────────────────────────────────
    @FXML
    public void goBack() {
        if (backStack.isEmpty()) return;
        forwardStack.push(current);
        current = backStack.pop();
        loadPage(current.fxmlPath, current.title, false);
        updateNavButtons();
    }

    // ── FORWARD ──────────────────────────────────────────────────────────
    @FXML
    public void goForward() {
        if (forwardStack.isEmpty()) return;
        backStack.push(current);
        current = forwardStack.pop();
        loadPage(current.fxmlPath, current.title, true);
        updateNavButtons();
    }

    // ── HOME ─────────────────────────────────────────────────────────────
// ── HOME ─────────────────────────────────────────────────────────────
    @FXML
    public void goHome() {
        User u = UserSession.getUser();
        if (u == null) {
            navigateToLogin();
            return;
        }

        if ("ADMIN".equals(u.getRole())) {
            navigate("/fxml/admin_dashboard.fxml", "Admin Dashboard");
        } else if ("CO_ADMIN".equals(u.getRole())) {
            // REMOVED THE UNDERSCORE TO MATCH YOUR FILENAME
            navigate("/fxml/coadmin_dashboard.fxml", "Co-Admin Dashboard");
        }
    }
    private void loadPage(String fxmlPath, String title, boolean slideRight) {
        try {
            // 1. Safety Check: Get the resource URL
            java.net.URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                System.err.println("ERROR: FXML file not found at: " + fxmlPath);
                return; // Stop here to prevent the "Location is not set" exception
            }

            // Build breadcrumb
            boolean isDash = fxmlPath.contains("dashboard");
            breadcrumbLabel.setText(isDash ? "" : "›  " + title);

            // 2. Set the location on the loader EXPLICITLY
            FXMLLoader loader = new FXMLLoader(resource);
            Node newPage = loader.load();

            // ... rest of your animation code stays the same ...
            newPage.setOpacity(0);
            newPage.setTranslateX(slideRight ? 40 : -40);
            contentArea.getChildren().setAll(newPage);

            FadeTransition fade = new FadeTransition(Duration.millis(220), newPage);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(220), newPage);
            slide.setFromX(slideRight ? 40 : -40);
            slide.setToX(0);
            new ParallelTransition(fade, slide).play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ── Called by AdminDashboardController.logout() ───────────────────────
    public void navigateToLogin() {
        try {
            instance = null;
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/modern.css").toExternalForm());
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.setWidth(1530);
            stage.setHeight(860);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Update disabled state of nav buttons ─────────────────────────────
    private void updateNavButtons() {
        backBtn.setDisable(backStack.isEmpty());
        forwardBtn.setDisable(forwardStack.isEmpty());

        String activeStyle  = "-fx-background-color: transparent; -fx-text-fill: #c8d8f0;" +
                "-fx-font-size: 18px; -fx-font-weight: bold;" +
                "-fx-padding: 0 10 2 10; -fx-cursor: hand; -fx-background-radius: 4;";
        String disabledStyle = "-fx-background-color: transparent; -fx-text-fill: #2a3a5a;" +
                "-fx-font-size: 18px; -fx-font-weight: bold;" +
                "-fx-padding: 0 10 2 10; -fx-background-radius: 4;";

        backBtn.setStyle(backStack.isEmpty()    ? disabledStyle : activeStyle);
        forwardBtn.setStyle(forwardStack.isEmpty() ? disabledStyle : activeStyle);
    }

    // ── Hover highlight for nav buttons ───────────────────────────────────
    private void styleNavBtn(Button btn) {
        String base = btn.getStyle();
        btn.setOnMouseEntered(e -> btn.setStyle(base +
                "-fx-background-color: rgba(255,255,255,0.07);"));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
    }
}
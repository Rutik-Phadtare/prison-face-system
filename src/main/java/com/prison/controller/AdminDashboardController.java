package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.dao.PrisonerDao;
import com.prison.dao.UserDao;
import com.prison.model.User;
import com.prison.session.UserSession;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AdminDashboardController {

    /* ── Original FXML bindings (unchanged) ─────────────────────────────── */
    @FXML private Button guardManagementBtn;
    @FXML private Button faceRecognitionBtn;
    @FXML private Button prisonerCountButton;
    @FXML private Button guardCountButton;
    @FXML private Button guardCount;

    /* ── New UI bindings ──────────────────────────────────────────────────── */
    @FXML private Label liveClock;
    @FXML private Label liveDate;
    @FXML private Label loggedInUser;
    @FXML private Label alertTicker;
    @FXML private Label alertTime;

    // Alert slideshow
    @FXML private Label alertSlideTitle;
    @FXML private Label alertSlideDetail;
    @FXML private Label alertSlideIndex;
    @FXML private Label alertDot1;
    @FXML private Label alertDot2;
    @FXML private Label alertDot3;
    @FXML private Label alertDot4;
    @FXML private Label alertDot5;

    /* ── DAOs (unchanged) ────────────────────────────────────────────────── */
    private final PrisonerDao prisonerDao = new PrisonerDao();
    private final GuardDao    guardDao    = new GuardDao();
    private final UserDao     userDao     = new UserDao();

    /* ── Alert data for slideshow ─────────────────────────────────────────── */
    private static final String[][] ALERTS = {
            {
                    "● Cell Block D — Routine headcount discrepancy detected",
                    "Count mismatch of 1 inmate in Block D reported at 08:42. Investigation underway by duty officer. Status: PENDING RESOLUTION."
            },
            {
                    "● Visitor Access — 3 approval requests pending",
                    "Three visitor access requests submitted this morning are awaiting administrator approval. Review required before 14:00 to avoid gate delay."
            },
            {
                    "● Medical — Inmate #2841 appointment scheduled",
                    "Scheduled medical consultation for Inmate #2841 at 14:00 hrs. Medical wing notified. Escort detail to be confirmed by Block C duty guard."
            },
            {
                    "● Perimeter — Sensor self-test scheduled at 22:00",
                    "Routine perimeter sensor diagnostic test will run tonight at 22:00 for approximately 45 minutes. No action required. System will auto-resume."
            },
            {
                    "● Shift Change — 06:00 roster confirmed",
                    "Morning shift handover at 06:00 has been logged and confirmed. All 14 guard positions filled. Night shift debrief report filed."
            }
    };

    private int currentAlertIndex = 0;
    private final Label[] dots = new Label[5];

    /* ═══════════════════════════════════════════════════════════════════════
       initialize  (original logic preserved, new UI logic appended)
    ═══════════════════════════════════════════════════════════════════════ */
    @FXML
    public void initialize() {
        /* ── ORIGINAL LOGIC (do not remove) ─────────────────────────────── */
        updateCounts();
        startAutoRefresh();

        User user = UserSession.getUser();
        if (user == null) return;

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

        /* ── NEW UI LOGIC ─────────────────────────────────────────────────── */
        if (loggedInUser != null && user.getUsername() != null) {
            loggedInUser.setText(user.getUsername().toUpperCase() + "  [" + user.getRole() + "]");
        }

        dots[0] = alertDot1;
        dots[1] = alertDot2;
        dots[2] = alertDot3;
        dots[3] = alertDot4;
        dots[4] = alertDot5;

        startLiveClock();
        startAlertSlideshow();
        startAlertTickerAnimation();
    }

    /* ── Original: updateCounts (unchanged) ─────────────────────────────── */
    private void updateCounts() {
        prisonerCountButton.setText(String.valueOf(prisonerDao.countActivePrisoners()));
        guardCountButton.setText(String.valueOf(guardDao.countActiveGuards()));
        guardCount.setText(String.valueOf(guardDao.countGuards()));
    }

    /* ── Original: startAutoRefresh (unchanged) ──────────────────────────── */
    private void startAutoRefresh() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), e -> updateCounts()));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    /* ── NEW: Live clock ─────────────────────────────────────────────────── */
    private void startLiveClock() {
        if (liveClock == null) return;
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            liveClock.setText(LocalTime.now().format(timeFmt));
            if (liveDate != null) liveDate.setText(LocalDate.now().format(dateFmt).toUpperCase());
            if (alertTime != null) alertTime.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        liveClock.setText(LocalTime.now().format(timeFmt));
        if (liveDate != null) liveDate.setText(LocalDate.now().format(dateFmt).toUpperCase());
    }

    /* ── NEW: Alert slideshow ─────────────────────────────────────────────── */
    private void startAlertSlideshow() {
        if (alertSlideTitle == null) return;
        showSlide(0);

        Timeline slideshow = new Timeline(new KeyFrame(Duration.seconds(6), e -> {
            currentAlertIndex = (currentAlertIndex + 1) % ALERTS.length;
            FadeTransition fadeOut = new FadeTransition(Duration.millis(350), alertSlideTitle);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            FadeTransition fadeOut2 = new FadeTransition(Duration.millis(350), alertSlideDetail);
            fadeOut2.setFromValue(1.0);
            fadeOut2.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                showSlide(currentAlertIndex);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(350), alertSlideTitle);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                FadeTransition fadeIn2 = new FadeTransition(Duration.millis(350), alertSlideDetail);
                fadeIn2.setFromValue(0.0);
                fadeIn2.setToValue(1.0);
                fadeIn.play();
                fadeIn2.play();
            });
            fadeOut.play();
            fadeOut2.play();
        }));
        slideshow.setCycleCount(Animation.INDEFINITE);
        slideshow.play();
    }

    private void showSlide(int index) {
        alertSlideTitle.setText(ALERTS[index][0]);
        alertSlideDetail.setText(ALERTS[index][1]);
        if (alertSlideIndex != null)
            alertSlideIndex.setText((index + 1) + " of " + ALERTS.length);
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] != null)
                dots[i].setStyle("-fx-font-size: 8; -fx-text-fill: " + (i == index ? "#cc0000" : "#2a3a4a") + ";");
        }
    }

    /* ── NEW: Alert ticker pulse ─────────────────────────────────────────── */
    private void startAlertTickerAnimation() {
        if (alertTicker == null) return;
        FadeTransition pulse = new FadeTransition(Duration.seconds(2), alertTicker);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.55);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    /* ══════════════════════════════════════════════════════════════════════
       ORIGINAL ACTION METHODS — unchanged
    ══════════════════════════════════════════════════════════════════════ */

    @FXML
    public void openRecognitionLogs() {
        if (!isAdmin()) return;
        openWindow("/fxml/recognition_logs.fxml", "Recognition Logs");
    }

    @FXML
    public void openPrisonerManagement() {
        openWindow("/fxml/prisoner_management.fxml", "Prisoner Management");
    }

    @FXML
    public void openGuardManagement() {
        if (!isAdmin()) return;
        openWindow("/fxml/guard_management.fxml", "Guard Management");
    }

    @FXML
    public void openFaceRecognition() {
        if (!isAdmin()) return;
        openWindow("/fxml/face_recognition.fxml", "Face Recognition");
    }

    @FXML
    public void openCoAdminManagement() {
        if (!isAdmin()) return;
        openWindow("/fxml/co_admin_management.fxml", "Co-Admin Management");
    }

    /* ── NEW METHOD — only addition to this file ─────────────────────────── */
    @FXML
    public void openAdminAccountManagement() {
        if (!isAdmin()) return;
        openWindow("/fxml/admin_account_management.fxml", "Admin Account Management");
    }

    @FXML
    public void logout(ActionEvent event) {
        if (UserSession.getLoginLogId() > 0) {
            userDao.recordLogout(UserSession.getLoginLogId());
        }
        UserSession.clear();

        try {
            Stage current = (Stage) ((Node) event.getSource()).getScene().getWindow();
            current.close();

            Stage loginStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/modern.css").toExternalForm());
            loginStage.setScene(scene);
            loginStage.setTitle("Login");
            loginStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /* ── helpers (unchanged) ─────────────────────────────────────────────── */
    private boolean isAdmin() {
        User u = UserSession.getUser();
        return u != null && "ADMIN".equals(u.getRole());
    }

    private void openWindow(String fxmlPath, String title) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "FXML file not found on classpath:\n" + fxmlPath +
                                "\n\nMake sure the file is saved at:\nsrc/main/resources" + fxmlPath);
                a.showAndWait();
                return;
            }
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(url);
            Scene scene = new Scene(loader.load());
            java.net.URL css = getClass().getResource("/css/modern.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Failed to open " + title + ":\n" + e.getMessage()).showAndWait();
        }
    }
}
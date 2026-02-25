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
    @FXML private Button securityAlertCount;

    /* ── New UI bindings ──────────────────────────────────────────────────── */
    @FXML private Label liveClock;
    @FXML private Label liveDate;
    @FXML private Label loggedInUser;
    @FXML private Label alertTicker;
    @FXML private Label alertTime;

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
    /* ── Alert data — dynamic daily alerts ───────────────────────────────── */
    private static final String[][] ALL_ALERTS = {
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
            },
            {
                    "● Gate B — Access card reader malfunction reported",
                    "Gate B access reader reported unresponsive at 07:15. Maintenance team dispatched. Manual check-in protocol in effect until resolved."
            },
            {
                    "● Block A — Noise complaint filed by night shift",
                    "Night shift officer filed a noise disturbance report from Block A at 02:30. Situation de-escalated. Incident logged for review."
            },
            {
                    "● Transfer Order — 2 inmates scheduled for relocation",
                    "Two inmates scheduled for inter-facility transfer at 11:00. Paperwork cleared. Escort team assigned and briefed."
            },
            {
                    "● CCTV — Camera 7 offline since midnight",
                    "Camera 7 covering north corridor has been offline since 00:00. IT department notified. Manual patrol coverage assigned to the area."
            },
            {
                    "● Kitchen — Supplier delivery delayed by 2 hours",
                    "Morning food supply delivery delayed. Revised ETA 10:00. Kitchen staff notified. No impact on scheduled meal times."
            }
    };

    // Daily alerts — count changes every day (0–10), consistent within same day
    private String[][] ALERTS;
    private int currentAlertIndex = 0;
    private final Label[] dots = new Label[5];

    /* ═══════════════════════════════════════════════════════════════════════
       initialize  (original logic preserved)
    ═══════════════════════════════════════════════════════════════════════ */
    @FXML
    public void initialize() {
        // Generate today's alert count (1–5 max shown, seeded by date)
        java.util.Random rng = new java.util.Random(LocalDate.now().toEpochDay());
        int alertCount = 1 + rng.nextInt(5); // 1 to 5 alerts per day
        ALERTS = new String[alertCount][];

        // Pick random alerts for today without repeating
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        for (int i = 0; i < ALL_ALERTS.length; i++) indices.add(i);
        java.util.Collections.shuffle(indices, rng);
        for (int i = 0; i < alertCount; i++) {
            ALERTS[i] = ALL_ALERTS[indices.get(i)];
        }
        if (securityAlertCount != null)
            securityAlertCount.setText(String.valueOf(ALERTS.length));

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

        if (loggedInUser != null && user.getUsername() != null) {
            loggedInUser.setText(user.getUsername().toUpperCase() + "  [" + user.getRole() + "]");
        }

        dots[0] = alertDot1; dots[1] = alertDot2; dots[2] = alertDot3;
        dots[3] = alertDot4; dots[4] = alertDot5;

        // Hide dots that exceed today's alert count
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] != null) {
                dots[i].setVisible(i < alertCount);
                dots[i].setManaged(i < alertCount);
            }
        }

        startLiveClock();
        startAlertSlideshow();
        startAlertTickerAnimation();
    }

    /* ── Original: updateCounts ──────────────────────────────────────────── */
    private void updateCounts() {
        prisonerCountButton.setText(String.valueOf(prisonerDao.countActivePrisoners()));
        guardCountButton.setText(String.valueOf(guardDao.countActiveGuards()));
        guardCount.setText(String.valueOf(guardDao.countGuards()));
    }

    /* ── Original: startAutoRefresh ──────────────────────────────────────── */
    private void startAutoRefresh() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), e -> updateCounts()));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    /* ── Live clock ──────────────────────────────────────────────────────── */
    private void startLiveClock() {
        if (liveClock == null) return;
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            liveClock.setText(LocalTime.now().format(timeFmt));
            if (liveDate  != null) liveDate.setText(LocalDate.now().format(dateFmt).toUpperCase());
            if (alertTime != null) alertTime.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        liveClock.setText(LocalTime.now().format(timeFmt));
        if (liveDate != null) liveDate.setText(LocalDate.now().format(dateFmt).toUpperCase());
    }

    /* ── Alert slideshow ─────────────────────────────────────────────────── */
    private void startAlertSlideshow() {
        if (alertSlideTitle == null) return;
        showSlide(0);

        // Only cycle if more than 1 alert today
        if (ALERTS.length <= 1) return;

        Timeline slideshow = new Timeline(new KeyFrame(Duration.seconds(6), e -> {
            currentAlertIndex = (currentAlertIndex + 1) % ALERTS.length;

            FadeTransition fo1 = new FadeTransition(Duration.millis(350), alertSlideTitle);
            fo1.setFromValue(1.0); fo1.setToValue(0.0);

            FadeTransition fo2 = new FadeTransition(Duration.millis(350), alertSlideDetail);
            fo2.setFromValue(1.0); fo2.setToValue(0.0);

            fo1.setOnFinished(ev -> {
                showSlide(currentAlertIndex);

                FadeTransition fi1 = new FadeTransition(Duration.millis(350), alertSlideTitle);
                fi1.setFromValue(0.0); fi1.setToValue(1.0); fi1.play();

                FadeTransition fi2 = new FadeTransition(Duration.millis(350), alertSlideDetail);
                fi2.setFromValue(0.0); fi2.setToValue(1.0); fi2.play();
            });

            fo1.play();
            fo2.play();
        }));

        slideshow.setCycleCount(Animation.INDEFINITE);
        slideshow.play();
    }
    private void showSlide(int index) {
        alertSlideTitle.setText(ALERTS[index][0]);
        alertSlideDetail.setText(ALERTS[index][1]);
        if (alertSlideIndex != null)
            alertSlideIndex.setText((index + 1) + " of " + ALERTS.length);
        for (int i = 0; i < dots.length; i++)
            if (dots[i] != null && i < ALERTS.length)
                dots[i].setStyle("-fx-font-size: 8; -fx-text-fill: " +
                        (i == index ? "#cc0000" : "#2a3a4a") + ";");
    }

    /* ── Alert ticker ────────────────────────────────────────────────────── */
    private void startAlertTickerAnimation() {
        if (alertTicker == null) return;
        FadeTransition pulse = new FadeTransition(Duration.seconds(2), alertTicker);
        pulse.setFromValue(1.0); pulse.setToValue(0.55);
        pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    /* ══════════════════════════════════════════════════════════════════════
       ORIGINAL ACTION METHODS — module buttons unchanged, routing updated
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

    @FXML
    public void openAdminAccountManagement() {
        if (!isAdmin()) return;
        openWindow("/fxml/admin_account_management.fxml", "Admin Account Management");
    }

    /* ══════════════════════════════════════════════════════════════════════
       LOGOUT — updated to route through MainShellController
    ══════════════════════════════════════════════════════════════════════ */
    @FXML
    public void logout(ActionEvent event) {
        if (UserSession.getLoginLogId() > 0) {
            userDao.recordLogout(UserSession.getLoginLogId());
        }
        UserSession.clear();

        // ── If running inside the shell, let the shell handle login routing ──
        MainShellController shell = MainShellController.getInstance();
        if (shell != null) {
            shell.navigateToLogin();
            return;
        }

        // ── Fallback: original behaviour (direct stage load) ──────────────
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

    /* ── helpers ─────────────────────────────────────────────────────────── */
    private boolean isAdmin() {
        User u = UserSession.getUser();
        return u != null && "ADMIN".equals(u.getRole());
    }

    /*
     * ══════════════════════════════════════════════════════════════════════
     *  openWindow — KEY CHANGE
     *  If the MainShellController shell is active, navigate inside it.
     *  Falls back to opening a new Stage if the shell is not available
     *  (e.g. launched standalone during development/testing).
     * ══════════════════════════════════════════════════════════════════════
     */
    private void openWindow(String fxmlPath, String title) {
        // ── SPA mode: load page inside the shell content area ─────────────
        MainShellController shell = MainShellController.getInstance();
        if (shell != null) {
            shell.navigate(fxmlPath, title);
            return;
        }

        // ── Fallback: original new-Stage behaviour ─────────────────────────
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "FXML not found:\n" + fxmlPath).showAndWait();
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
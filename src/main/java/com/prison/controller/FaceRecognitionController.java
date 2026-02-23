package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.dao.PrisonerDao;
import com.prison.dao.RecognitionLogDao;
import com.prison.model.Guard;
import com.prison.model.Prisoner;
import com.prison.model.RecognitionLog;
import com.prison.service.FaceRecognitionService;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FaceRecognitionController {

    // ── FXML ───────────────────────────────────────────────────────────────
    @FXML private VBox    infoBox;
    @FXML private VBox    idlePane;
    @FXML private Label   clockLabel;
    @FXML private Label   dateLabel;
    @FXML private Label   scanStatusLabel;
    @FXML private Label   scanPulse;
    @FXML private Label   scanCountLabel;
    @FXML private Label   lastScanLabel;
    @FXML private Button  scanButton;

    // ── State ──────────────────────────────────────────────────────────────
    private final FaceRecognitionService service = new FaceRecognitionService();
    private int scanCount = 0;

    // ── Theme constants ────────────────────────────────────────────────────
    private static final String GUARD_ACCENT   = "#0ea5e9";
    private static final String GUARD_BG       = "#0c1a2e";
    private static final String GUARD_CARD     = "#0f2744";
    private static final String GUARD_BORDER   = "#1e4a7a";
    private static final String GUARD_TITLE_FG = "#7dd3fc";

    private static final String PRIS_ACCENT    = "#ef4444";
    private static final String PRIS_BG        = "#1a0a0a";
    private static final String PRIS_CARD      = "#2d0a0a";
    private static final String PRIS_BORDER    = "#7f1d1d";
    private static final String PRIS_TITLE_FG  = "#fca5a5";

    private static final String UNK_ACCENT     = "#f59e0b";

    @FXML
    public void initialize() {
        startClock();
        startPulse();
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            if (clockLabel != null)
                clockLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            if (dateLabel != null)
                dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void startPulse() {
        String[] dots = {"● ○ ○", "○ ● ○", "○ ○ ●", "○ ● ○"};
        final int[] idx = {0};
        Timeline pulse = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (scanPulse != null) {
                scanPulse.setText(dots[idx[0] % dots.length]);
                idx[0]++;
            }
        }));
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ▶ START RECOGNITION (UPDATED TO BE NON-BLOCKING)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void startRecognition() {
        // UI feedback before starting
        scanButton.setDisable(true);
        scanButton.setText("📡  Camera Running...");
        if (scanStatusLabel != null) scanStatusLabel.setText("SCANNING...");
        infoBox.getChildren().clear();

        // Run recognition in a background thread to prevent UI freezing
        Thread recognitionThread = new Thread(() -> {
            try {
                // This is the heavy part (Python script)
                final String result = service.recognize();

                // Move back to UI thread to update the screen
                Platform.runLater(() -> {
                    processRecognitionResult(result);

                    // Restore button state
                    scanButton.setDisable(false);
                    scanButton.setText("🔍  SCAN FACE");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("An unexpected error occurred during scan.");
                    scanButton.setDisable(false);
                });
            }
        });

        recognitionThread.setDaemon(true);
        recognitionThread.start();
    }

    private void processRecognitionResult(String result) {
        if (result == null || !result.startsWith("OK")) {
            showError("Recognition failed or camera unavailable.");
            if (scanStatusLabel != null) scanStatusLabel.setText("SCAN FAILED");
            return;
        }

        if (scanStatusLabel != null) scanStatusLabel.setText("SCAN COMPLETE");

        // ✅ LOG EVENT
        logRecognition(result);

        // Update counters
        scanCount++;
        if (scanCountLabel != null) scanCountLabel.setText(String.valueOf(scanCount));
        if (lastScanLabel != null)
            lastScanLabel.setText("Last: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        String[] parts = result.split("\\|");
        String type = parts[1];

        if ("UNKNOWN".equals(type)) {
            showUnknown();
            return;
        }

        int id = Integer.parseInt(parts[2]);

        if ("GUARD".equals(type)) {
            Guard g = new GuardDao().findById(id);
            if (g != null) showGuardProfile(g);
            else           showError("Guard record not found in database.");

        } else if ("PRISONER".equals(type)) {
            Prisoner p = new PrisonerDao().findById(id);
            if (p != null) showPrisonerProfile(p);
            else           showError("Prisoner record not found in database.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GUARD FULL PROFILE DISPLAY
    // ══════════════════════════════════════════════════════════════════════
    private void showGuardProfile(Guard g) {
        infoBox.setStyle("-fx-background-color: " + GUARD_BG + ";");

        HBox banner = banner("✅  GUARD IDENTIFIED", GUARD_ACCENT, GUARD_CARD, GUARD_BORDER);
        infoBox.getChildren().add(banner);

        HBox headerCard = new HBox(24);
        headerCard.setStyle("-fx-background-color:" + GUARD_CARD + ";" +
                "-fx-background-radius:14; -fx-border-color:" + GUARD_BORDER +
                "; -fx-border-width:2; -fx-border-radius:14;");
        headerCard.setPadding(new Insets(24));
        VBox.setMargin(headerCard, new Insets(16, 0, 0, 0));

        ImageView photo = new ImageView();
        photo.setFitWidth(130); photo.setFitHeight(145); photo.setPreserveRatio(true);
        File imgFile = new File("python-face/photos/guards/" + g.getGuardId() + ".jpg");
        if (imgFile.exists()) photo.setImage(new Image(imgFile.toURI().toString()));
        VBox photoBox = new VBox(8, photo);
        photoBox.setAlignment(Pos.CENTER);
        photoBox.setStyle("-fx-background-color:#0f2038; -fx-background-radius:10;" +
                "-fx-border-color:" + GUARD_BORDER + "; -fx-border-width:2; -fx-border-radius:10;");
        photoBox.setPadding(new Insets(12));

        VBox idBlock = new VBox(6);
        idBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(idBlock, Priority.ALWAYS);
        idBlock.getChildren().addAll(
                chip("GUARD", GUARD_ACCENT),
                bigLabel(g.getName() != null ? g.getName().toUpperCase() : "—", GUARD_TITLE_FG, 22),
                smallRow("ID",          "GJR-" + String.format("%06d", g.getGuardId()), GUARD_ACCENT),
                smallRow("Designation", nvl(g.getDesignation()), "#94a3b8"),
                smallRow("Batch No.",   nvl(g.getBatchId()),     "#94a3b8"),
                smallRow("Status",      liveStatus(g),           liveStatusColor(g))
        );

        headerCard.getChildren().addAll(photoBox, idBlock);
        infoBox.getChildren().add(headerCard);

        infoBox.getChildren().add(sectionHeader("Personal Details", GUARD_ACCENT));
        GridPane personal = gridCard(GUARD_CARD, GUARD_BORDER);
        addGridRow(personal, 0, "Age",         nvl(g.getAge() > 0 ? g.getAge() + " years" : null));
        addGridRow(personal, 1, "Gender",       nvl(g.getGender()));
        addGridRow(personal, 2, "Date of Birth", nvl(g.getBirthDate() != null ? g.getBirthDate().toString() : null));
        addGridRow(personal, 3, "Aadhar No.",   nvl(g.getAadharNumber()));
        addGridRow(personal, 4, "Phone",        nvl(g.getPhoneNumber()));
        addGridRow(personal, 5, "Email",        nvl(g.getEmail()));
        addGridRow(personal, 6, "Address",      nvl(g.getAddress()));
        infoBox.getChildren().add(personal);

        infoBox.getChildren().add(sectionHeader("Employment", GUARD_ACCENT));
        GridPane employ = gridCard(GUARD_CARD, GUARD_BORDER);
        addGridRow(employ, 0, "Shift",         nvl(g.getShift()));
        addGridRow(employ, 1, "Joining Date",  nvl(g.getJoiningDate() != null ? g.getJoiningDate().toString() : null));
        addGridRow(employ, 2, "Salary",        g.getSalary() > 0 ? "₹ " + String.format("%.2f", g.getSalary()) : "—");
        addGridRow(employ, 3, "Transfer From", nvl(g.getTransferFrom()));
        infoBox.getChildren().add(employ);

        if (g.getDescription() != null && !g.getDescription().trim().isEmpty()) {
            infoBox.getChildren().add(sectionHeader("Internal Remarks", GUARD_ACCENT));
            infoBox.getChildren().add(notesCard(g.getDescription(), GUARD_CARD, GUARD_BORDER));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRISONER FULL PROFILE DISPLAY
    // ══════════════════════════════════════════════════════════════════════
    private void showPrisonerProfile(Prisoner p) {
        infoBox.setStyle("-fx-background-color: " + PRIS_BG + ";");
        infoBox.getChildren().add(banner("⚠️  PRISONER IDENTIFIED", PRIS_ACCENT, PRIS_CARD, PRIS_BORDER));

        HBox headerCard = new HBox(24);
        headerCard.setStyle("-fx-background-color:" + PRIS_CARD + ";" +
                "-fx-background-radius:14; -fx-border-color:" + PRIS_BORDER +
                "; -fx-border-width:2; -fx-border-radius:14;");
        headerCard.setPadding(new Insets(24));
        VBox.setMargin(headerCard, new Insets(16, 0, 0, 0));

        ImageView photo = new ImageView();
        photo.setFitWidth(130); photo.setFitHeight(145); photo.setPreserveRatio(true);
        File imgFile = new File("python-face/photos/prisoners/" + p.getPrisonerId() + ".jpg");
        if (imgFile.exists()) photo.setImage(new Image(imgFile.toURI().toString()));
        VBox photoBox = new VBox(8, photo);
        photoBox.setAlignment(Pos.CENTER);
        photoBox.setStyle("-fx-background-color:#200808; -fx-background-radius:10;" +
                "-fx-border-color:" + PRIS_BORDER + "; -fx-border-width:2; -fx-border-radius:10;");
        photoBox.setPadding(new Insets(12));

        VBox idBlock = new VBox(6);
        idBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(idBlock, Priority.ALWAYS);

        String dangerColor = switch (nvl(p.getDangerLevel())) {
            case "MAXIMUM" -> "#450a0a";
            case "HIGH"    -> "#7f1d1d";
            case "MEDIUM"  -> "#92400e";
            default        -> "#14532d";
        };

        idBlock.getChildren().addAll(
                chip("PRISONER — " + nvl(p.getDangerLevel()) + " RISK", dangerColor),
                bigLabel(p.getName() != null ? p.getName().toUpperCase() : "—", PRIS_TITLE_FG, 22),
                smallRow("ID",          "PRN-" + String.format("%06d", p.getPrisonerId()), PRIS_ACCENT),
                smallRow("Crime",       nvl(p.getCrime()),  "#94a3b8"),
                smallRow("Cell",        nvl(p.getCellNo()), "#94a3b8"),
                smallRow("Status",      nvl(p.getStatus()),
                        "IN_CUSTODY".equals(p.getStatus()) ? "#22c55e" : "#f87171")
        );

        headerCard.getChildren().addAll(photoBox, idBlock);
        infoBox.getChildren().add(headerCard);

        infoBox.getChildren().add(sectionHeader("Personal Identification", PRIS_ACCENT));
        GridPane personal = gridCard(PRIS_CARD, PRIS_BORDER);
        addGridRow(personal, 0, "Age / Gender",  nvl(p.getAge() > 0 ? p.getAge() + " yrs" : null) + "  |  " + nvl(p.getGender()));
        addGridRow(personal, 1, "Nationality",   nvl(p.getNationality()));
        addGridRow(personal, 2, "Aadhar No.",    nvl(p.getAadharNumber()));
        addGridRow(personal, 3, "Blood Type",    nvl(p.getBloodType()));
        addGridRow(personal, 4, "Height / Weight", nvl(p.getHeight()) + "  /  " + nvl(p.getWeight()));
        addGridRow(personal, 5, "ID Marks",      nvl(p.getIdentificationMarks()));
        addGridRow(personal, 6, "Home Address",  nvl(p.getHomeAddress()));
        infoBox.getChildren().add(personal);

        infoBox.getChildren().add(sectionHeader("Criminal Record & Sentence", PRIS_ACCENT));
        GridPane criminal = gridCard(PRIS_CARD, PRIS_BORDER);
        addGridRow(criminal, 0, "Crime",          nvl(p.getCrime()));
        addGridRow(criminal, 1, "Cell No.",        nvl(p.getCellNo()));
        addGridRow(criminal, 2, "Danger Level",    nvl(p.getDangerLevel()));
        addGridRow(criminal, 3, "Behavior Rating", nvl(p.getBehaviorRating()));
        addGridRow(criminal, 4, "Sentence",        p.getSentenceYears() + " years");
        addGridRow(criminal, 5, "Start Date",      p.getSentenceStartDate() != null ? p.getSentenceStartDate().toString() : "—");
        addGridRow(criminal, 6, "Release Date",    p.getReleaseDate() != null ? p.getReleaseDate().toString() : "—");

        if (p.getReleaseDate() != null && !p.getReleaseDate().isAfter(LocalDate.now())) {
            Label warning = new Label("⚠  SENTENCE EXPIRED — IMMEDIATE REVIEW REQUIRED");
            warning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 800; -fx-padding: 8 14;");
            criminal.add(warning, 0, 7, 2, 1);
        }
        infoBox.getChildren().add(criminal);

        infoBox.getChildren().add(sectionHeader("Emergency & Legal Contacts", PRIS_ACCENT));
        GridPane contacts = gridCard(PRIS_CARD, PRIS_BORDER);
        addGridRow(contacts, 0, "Emergency Contact", nvl(p.getEmergencyContact()));
        addGridRow(contacts, 1, "Emergency Phone",   nvl(p.getEmergencyPhone()));
        addGridRow(contacts, 2, "Legal Counsel",      nvl(p.getLawyerName()));
        addGridRow(contacts, 3, "Lawyer Phone",       nvl(p.getLawyerPhone()));
        infoBox.getChildren().add(contacts);

        if (p.getIncidentNotes() != null && !p.getIncidentNotes().trim().isEmpty()) {
            infoBox.getChildren().add(sectionHeader("Incident & Disciplinary Log", PRIS_ACCENT));
            infoBox.getChildren().add(notesCard(p.getIncidentNotes(), PRIS_CARD, PRIS_BORDER));
        }
        if (p.getVisitorLog() != null && !p.getVisitorLog().trim().isEmpty()) {
            infoBox.getChildren().add(sectionHeader("Activity Monitor — Visits / Calls / Digital", PRIS_ACCENT));
            infoBox.getChildren().add(notesCard(p.getVisitorLog(), PRIS_CARD, PRIS_BORDER));
        }
        if (p.getDescription() != null && !p.getDescription().trim().isEmpty()) {
            infoBox.getChildren().add(sectionHeader("Case Notes", PRIS_ACCENT));
            infoBox.getChildren().add(notesCard(p.getDescription(), PRIS_CARD, PRIS_BORDER));
        }
    }

    private void showUnknown() {
        infoBox.setStyle("-fx-background-color: #1c1300;");
        infoBox.getChildren().add(banner("⚠  UNKNOWN PERSON DETECTED", UNK_ACCENT, "#1c1300", "#78350f"));

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color:#1c1300; -fx-background-radius:14;" +
                "-fx-border-color:#78350f; -fx-border-width:2; -fx-border-radius:14;");
        card.setPadding(new Insets(28));
        card.setAlignment(Pos.CENTER);
        VBox.setMargin(card, new Insets(20, 0, 0, 0));

        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size: 52; -fx-text-fill: #f59e0b;");

        Label msg = new Label("No matching guard or prisoner found in database.");
        msg.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 16; -fx-font-weight: 700; -fx-wrap-text: true;");
        msg.setWrapText(true);

        Label sub = new Label("Alert has been raised. Security personnel have been notified.");
        sub.setStyle("-fx-text-fill: #78350f; -fx-font-size: 13; -fx-wrap-text: true;");
        sub.setWrapText(true);

        card.getChildren().addAll(icon, msg, sub);
        infoBox.getChildren().add(card);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Security Alert");
        alert.setHeaderText("Unknown Person Detected");
        alert.setContentText("No matching guard or prisoner found.\nPlease verify immediately.");
        alert.show(); // Use show instead of showAndWait to avoid blocking
    }

    private void showError(String message) {
        infoBox.setStyle("-fx-background-color: #0f172a;");
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color:#1e293b; -fx-background-radius:14;" +
                "-fx-border-color:#475569; -fx-border-width:2; -fx-border-radius:14;");
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER);
        VBox.setMargin(card, new Insets(20, 0, 0, 0));

        Label icon = new Label("❌");
        icon.setStyle("-fx-font-size: 36;");
        Label errLabel = new Label(message);
        errLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 15; -fx-font-weight: 700;");
        errLabel.setWrapText(true);

        card.getChildren().addAll(icon, errLabel);
        infoBox.getChildren().add(card);
    }

    private void logRecognition(String pythonOutput) {
        try {
            String[] parts = pythonOutput.split("\\|");
            RecognitionLog log = new RecognitionLog();
            if ("UNKNOWN".equals(parts[1])) {
                log.setPersonType("UNKNOWN");
                log.setPersonId(null);
                log.setResult("UNKNOWN");
            } else {
                log.setPersonType(parts[1]);
                log.setPersonId(Integer.parseInt(parts[2]));
                log.setResult("RECOGNIZED");
            }
            new RecognitionLogDao().save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── UI BUILDER HELPERS (UNTOUCHED) ────────────────────────────────────────

    private HBox banner(String text, String accent, String bg, String border) {
        HBox b = new HBox();
        b.setAlignment(Pos.CENTER_LEFT);
        b.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12;" +
                "-fx-border-color:" + border + "; -fx-border-width:2; -fx-border-radius:12;" +
                "-fx-border-style: solid inside;");
        b.setPadding(new Insets(14, 20, 14, 20));
        Region stripe = new Region();
        stripe.setPrefWidth(5); stripe.setPrefHeight(28);
        stripe.setStyle("-fx-background-color:" + accent + "; -fx-background-radius:4;");
        HBox.setMargin(stripe, new Insets(0, 14, 0, 0));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill:" + accent + "; -fx-font-size: 15; -fx-font-weight: 900;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label time = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        time.setStyle("-fx-text-fill: #475569; -fx-font-size: 12; -fx-font-family: monospace;");
        b.getChildren().addAll(stripe, lbl, spacer, time);
        return b;
    }

    private Label chip(String text, String bg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:white; -fx-font-size:10; -fx-font-weight:800; -fx-padding:4 10; -fx-background-radius:20;");
        return l;
    }

    private Label bigLabel(String text, String color, double size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + color + "; -fx-font-size:" + size + "; -fx-font-weight:900;");
        l.setWrapText(true);
        return l;
    }

    private HBox smallRow(String key, String value, String valueColor) {
        Label k = new Label(key + ":  ");
        k.setStyle("-fx-text-fill:#475569; -fx-font-size:13; -fx-font-weight:700;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:" + valueColor + "; -fx-font-size:13; -fx-font-weight:800;");
        return new HBox(k, v);
    }

    private HBox sectionHeader(String title, String accent) {
        Region bar = new Region();
        bar.setPrefWidth(4); bar.setPrefHeight(20);
        bar.setStyle("-fx-background-color:" + accent + "; -fx-background-radius:2;");
        HBox.setMargin(bar, new Insets(0, 12, 0, 0));
        Label lbl = new Label(title.toUpperCase());
        lbl.setStyle("-fx-text-fill:" + accent + "; -fx-font-size:11; -fx-font-weight:800;");
        HBox h = new HBox(bar, lbl);
        h.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(h, new Insets(20, 0, 6, 0));
        return h;
    }

    private GridPane gridCard(String bg, String border) {
        GridPane g = new GridPane();
        g.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12; -fx-border-color:" + border + "; -fx-border-width:1.5; -fx-border-radius:12;");
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints(160);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(col1, col2);
        return g;
    }

    private void addGridRow(GridPane grid, int row, String key, String value) {
        String keyBg   = grid.getStyle().contains("2d0a0a") ? "#250808" : "#0c2038";
        String borderC = grid.getStyle().contains("2d0a0a") ? "#3d1212" : "#1e3a5f";
        Label k = new Label(key);
        k.setStyle("-fx-text-fill:#64748b; -fx-font-size:12; -fx-font-weight:700; -fx-padding: 11 16; -fx-background-color:" + keyBg + "; -fx-border-color: transparent " + borderC + " " + borderC + " transparent; -fx-border-width: 0 1 1 0;");
        k.setMaxWidth(Double.MAX_VALUE);
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:13; -fx-font-weight:600; -fx-padding: 11 16; -fx-border-color: transparent transparent " + borderC + " transparent; -fx-border-width: 0 0 1 0;");
        v.setMaxWidth(Double.MAX_VALUE); v.setWrapText(true);
        grid.add(k, 0, row); grid.add(v, 1, row);
    }

    private VBox notesCard(String text, String bg, String border) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#cbd5e1; -fx-font-size:13; -fx-line-spacing:4;");
        l.setWrapText(true);
        VBox box = new VBox(l);
        box.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12; -fx-border-color:" + border + "; -fx-border-width:1.5; -fx-border-radius:12;");
        box.setPadding(new Insets(16));
        return box;
    }

    private String liveStatus(Guard g) {
        String shift = g.getShift();
        if (shift == null || shift.equalsIgnoreCase("On Leave")) return "INACTIVE";
        try {
            String[] parts = shift.replaceAll("[^0-9-]", "").split("-");
            int s = Integer.parseInt(parts[0]), e = Integer.parseInt(parts[1]);
            int c = java.time.LocalTime.now().getHour();
            return ((s < e) ? (c >= s && c < e) : (c >= s || c < e)) ? "ACTIVE" : "INACTIVE";
        } catch (Exception ex) { return "INACTIVE"; }
    }

    private String liveStatusColor(Guard g) {
        return "ACTIVE".equals(liveStatus(g)) ? "#22c55e" : "#f87171";
    }

    private String nvl(String s) {
        return s != null && !s.isEmpty() ? s : "—";
    }
}
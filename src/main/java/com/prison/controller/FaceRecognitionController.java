package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.dao.PrisonerDao;
import com.prison.dao.RecognitionLogDao;
import com.prison.model.Guard;
import com.prison.model.Prisoner;
import com.prison.model.RecognitionLog;
import com.prison.service.FaceRecognitionService;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class FaceRecognitionController {

    @FXML
    private VBox infoBox;   // 🔴 IMPORTANT: styled container in FXML

    private final FaceRecognitionService service =
            new FaceRecognitionService();

    /* =========================
       START FACE RECOGNITION
       ========================= */
    @FXML
    public void startRecognition() {

        String result = service.recognize();

        infoBox.getChildren().clear();

        if (result == null || !result.startsWith("OK")) {
            showError("Recognition failed");
            return;
        }

        // ✅ LOG EVERY EVENT
        logRecognition(result);

        String[] parts = result.split("\\|");
        String type = parts[1];

        // UNKNOWN
        if ("UNKNOWN".equals(type)) {
            showUnknown();
            return;
        }

        int id = Integer.parseInt(parts[2]);

        if ("GUARD".equals(type)) {
            Guard g = new GuardDao().findById(id);
            if (g != null) {
                showGuardInfo(g);
            } else {
                showError("Guard record not found");
            }

        } else if ("PRISONER".equals(type)) {
            Prisoner p = new PrisonerDao().findById(id);
            if (p != null) {
                showPrisonerInfo(p);
            } else {
                showError("Prisoner record not found");
            }
        }
    }

    /* =========================
       DISPLAY GUARD INFO
       ========================= */
    private void showGuardInfo(Guard g) {

        infoBox.getStyleClass().add("info-card");

        Label title = new Label("GUARD IDENTIFIED");
        title.getStyleClass().add("guard-title");

        Label name = new Label("Name: " + g.getName());
        name.getStyleClass().add("guard-text");

        Label id = new Label("ID: " + g.getGuardId());
        id.getStyleClass().add("guard-text");

        Label status = new Label("Status: " + g.getStatus());
        status.getStyleClass().add("guard-text");

        infoBox.getChildren().addAll(title, name, id, status);
    }

    /* =========================
       DISPLAY PRISONER INFO
       ========================= */
    private void showPrisonerInfo(Prisoner p) {

        infoBox.getStyleClass().add("info-card");

        Label title = new Label("PRISONER IDENTIFIED");
        title.getStyleClass().add("prisoner-title");

        Label name = new Label("Name: " + p.getName());
        name.getStyleClass().add("prisoner-text");

        Label id = new Label("ID: " + p.getPrisonerId());
        id.getStyleClass().add("prisoner-text");

        Label crime = new Label("Crime: " + p.getCrime());
        crime.getStyleClass().add("prisoner-text");

        Label release = new Label("Release Date: "+p.getReleaseDate());
        release.getStyleClass().add("Prisoner-text");

        infoBox.getChildren().addAll(title, name, id, crime,release);
    }

    /* =========================
       UNKNOWN HANDLING
       ========================= */
    private void showUnknown() {

        infoBox.getStyleClass().add("info-card");

        Label title = new Label("UNKNOWN PERSON DETECTED");
        title.getStyleClass().add("prisoner-title");

        Label msg = new Label("No matching guard or prisoner found.");
        msg.getStyleClass().add("prisoner-text");

        infoBox.getChildren().addAll(title, msg);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Security Alert");
        alert.setHeaderText("Unknown Person Detected");
        alert.setContentText(
                "No matching guard or prisoner found.\n" +
                        "Please verify immediately."
        );
        alert.showAndWait();
    }

    /* =========================
       ERROR DISPLAY
       ========================= */
    private void showError(String message) {

        infoBox.getStyleClass().add("info-card");

        Label error = new Label(message);
        error.getStyleClass().add("prisoner-title");

        infoBox.getChildren().add(error);
    }

    /* =========================
       DATABASE LOGGING
       ========================= */
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
}

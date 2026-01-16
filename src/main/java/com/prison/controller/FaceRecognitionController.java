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

public class FaceRecognitionController {

    @FXML
    private Label resultLabel;

    private final FaceRecognitionService service =
            new FaceRecognitionService();

    @FXML
    public void startRecognition() {

        String result = service.recognize();

        if (result == null || !result.startsWith("OK")) {
            resultLabel.setText("Recognition failed");
            return;
        }

        // ✅ LOG EVERY RECOGNITION EVENT (IMPORTANT)
        logRecognition(result);

        String[] parts = result.split("\\|");
        String type = parts[1];

        // UNKNOWN CASE
        if ("UNKNOWN".equals(type)) {
            handleUnknown();
            return;
        }

        int id = Integer.parseInt(parts[2]);

        if ("GUARD".equals(type)) {
            GuardDao guardDao = new GuardDao();
            Guard g = guardDao.findById(id);

            if (g != null) {
                resultLabel.setText(
                        "Guard Detected:\n" +
                                g.getName() + " (ID: " + g.getGuardId() + ")\n" +
                                "Status: " + g.getStatus()
                );
            } else {
                resultLabel.setText("Guard record not found");
            }

        } else if ("PRISONER".equals(type)) {
            PrisonerDao prisonerDao = new PrisonerDao();
            Prisoner p = prisonerDao.findById(id);

            if (p != null) {
                resultLabel.setText(
                        "Prisoner Detected:\n" +
                                p.getName() + " (ID: " + p.getPrisonerId() + ")\n" +
                                "Crime: " + p.getCrime()
                );
            } else {
                resultLabel.setText("Prisoner record not found");
            }
        }
    }

    /* =========================
       DATABASE LOGGING
       ========================= */
    private void logRecognition(String pythonOutput) {

        try {
            // Example outputs:
            // OK|GUARD|10
            // OK|PRISONER|5
            // OK|UNKNOWN|0

            String[] parts = pythonOutput.split("\\|");

            RecognitionLog log = new RecognitionLog();

            if ("UNKNOWN".equals(parts[1])) {
                log.setPersonType("UNKNOWN");
                log.setPersonId(null);
                log.setResult("UNKNOWN");
            } else {
                log.setPersonType(parts[1]); // GUARD or PRISONER
                log.setPersonId(Integer.parseInt(parts[2]));
                log.setResult("RECOGNIZED");
            }

            new RecognitionLogDao().save(log);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =========================
       UNKNOWN PERSON HANDLING
       ========================= */
    private void handleUnknown() {

        resultLabel.setText("UNKNOWN PERSON DETECTED");

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Security Alert");
        alert.setHeaderText("Unknown Person Detected");
        alert.setContentText(
                "No matching guard or prisoner found.\n" +
                        "Please verify immediately."
        );
        alert.showAndWait();
    }
}

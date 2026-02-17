package com.prison.controller;

import com.prison.model.Guard;
import com.prison.service.GuardService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

public class GuardProfileController {

    @FXML private ImageView guardImage;
    @FXML private TextField nameField;
    @FXML private TextField designationField;
    @FXML private ComboBox<String> shiftBox;
    @FXML private Label statusBadge;
    @FXML private DatePicker joiningDatePicker;
    @FXML private TextArea descriptionArea;

    private Guard currentGuard;
    private final GuardService guardService = new GuardService();

    /* ================= INIT ================= */
    @FXML
    public void initialize() {

        shiftBox.setItems(FXCollections.observableArrayList(
                "SHIFT_A (06:00 - 14:00)",
                "SHIFT_B (14:00 - 22:00)",
                "SHIFT_C (22:00 - 06:00)"
        ));
    }

    /* ================= LOAD DATA ================= */
    public void setGuard(Guard g) {

        this.currentGuard = g;

        nameField.setText(g.getName());
        designationField.setText(g.getDesignation());
        shiftBox.setValue(g.getShift());
        descriptionArea.setText(g.getDescription());

        if (g.getJoiningDate() != null)
            joiningDatePicker.setValue(g.getJoiningDate());

        applyStatusColor(g.getStatus());
        loadGuardImage(g.getGuardId());
    }

    /* ================= STATUS COLOR ================= */
    private void applyStatusColor(String status) {

        statusBadge.setText(status);

        if ("ACTIVE".equals(status)) {
            statusBadge.setStyle(
                    "-fx-background-color:#16a34a; -fx-text-fill:white; -fx-padding:6 12; -fx-background-radius:15;");
        } else {
            statusBadge.setStyle(
                    "-fx-background-color:#dc2626; -fx-text-fill:white; -fx-padding:6 12; -fx-background-radius:15;");
        }
    }

    /* ================= LOAD IMAGE ================= */
    private void loadGuardImage(int guardId) {

        try {
            String path = "python-face/photos/guards/" + guardId + ".jpg";
            File file = new File(path);

            if (file.exists()) {
                guardImage.setImage(new Image(file.toURI().toString()));
            }

        } catch (Exception ignored) {}
    }

    /* ================= UPLOAD NEW PHOTO ================= */
    @FXML
    private void uploadPhoto() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Guard Photo");

        File file = chooser.showOpenDialog(guardImage.getScene().getWindow());

        if (file != null) {
            guardImage.setImage(new Image(file.toURI().toString()));
        }
    }

    /* ================= SAVE CHANGES ================= */
    @FXML
    private void saveChanges() {

        if (currentGuard == null) return;

        currentGuard.setName(nameField.getText());
        currentGuard.setDesignation(designationField.getText());
        currentGuard.setShift(shiftBox.getValue());
        currentGuard.setDescription(descriptionArea.getText());

        LocalDate date = joiningDatePicker.getValue();
        if (date != null) {
            currentGuard.setJoiningDate(date);
        }

        guardService.updateGuard(currentGuard);

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText("Guard Updated Successfully");
        a.showAndWait();
    }
}

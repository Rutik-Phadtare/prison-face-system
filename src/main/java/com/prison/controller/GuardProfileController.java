package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.model.Guard;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalTime;

public class GuardProfileController {

    @FXML private ImageView guardImage;
    @FXML private TextField nameField, ageField, addressField, transferField, salaryField;
    @FXML private ComboBox<String> shiftBox, genderBox, designationBox; // Added designationBox
    @FXML private Label statusBadge;
    @FXML private DatePicker joiningDatePicker, birthDatePicker;
    @FXML private TextArea descriptionArea;

    private Guard currentGuard;
    private final GuardDao guardDao = new GuardDao();
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        // Initialize ComboBox items
        shiftBox.setItems(FXCollections.observableArrayList("Morning (06-14)", "Evening (14-22)", "Night (22-06)", "On Leave"));
        genderBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));

        // Match the roles from your main GuardController
        designationBox.setItems(FXCollections.observableArrayList(
                "Gate Guard", "Tower Guard", "Control Room", "Escort Officer", "Supervisor"));

        // Logic for auto-salary calculation when the ComboBox selection changes
        designationBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateSalary(newVal);
        });
    }

    /**
     * Logic to calculate salary based on role.
     * Centralized so it can be called on load and on change.
     */
    private void updateSalary(String role) {
        if (role == null || role.isEmpty()) return;

        double amount = switch (role) {
            case "Supervisor" -> 5500.0;
            case "Control Room" -> 4800.0;
            case "Tower Guard" -> 4200.0;
            case "Escort Officer" -> 4000.0;
            case "Gate Guard" -> 3800.0;
            default -> 3500.0;
        };
        salaryField.setText(String.valueOf(amount));
    }

    public void setGuard(Guard g) {
        this.currentGuard = g;

        // 1. Set basic text fields
        nameField.setText(g.getName());
        ageField.setText(String.valueOf(g.getAge()));
        addressField.setText(g.getAddress());
        transferField.setText(g.getTransferFrom());
        genderBox.setValue(g.getGender());
        birthDatePicker.setValue(g.getBirthDate());
        shiftBox.setValue(g.getShift());
        joiningDatePicker.setValue(g.getJoiningDate());
        descriptionArea.setText(g.getDescription());

        // 2. Set the Designation ComboBox
        // Setting this might not always trigger the listener automatically on load
        designationBox.setValue(g.getDesignation());

        // 3. FORCE the salary update based on the role we just set
        // This ensures that even if you changed it in the other window,
        // the profile window recalculates it immediately upon opening.
        updateSalary(g.getDesignation());

        // 4. Update UI Status
        applyStatusColor(calculateLiveStatus(g.getShift()));
        loadGuardImage(g.getGuardId());
    }

    @FXML
    private void updateGuard() {
        if (currentGuard == null) return;

        currentGuard.setName(nameField.getText());
        // 🔥 Get designation from ComboBox instead of TextField
        currentGuard.setDesignation(designationBox.getValue());

        currentGuard.setShift(shiftBox.getValue());
        currentGuard.setJoiningDate(joiningDatePicker.getValue());
        currentGuard.setDescription(descriptionArea.getText());

        // Save Numeric Fields
        try {
            currentGuard.setAge(Integer.parseInt(ageField.getText()));
            currentGuard.setSalary(Double.parseDouble(salaryField.getText()));
        } catch (Exception e) {
            // Optional: Handle invalid number input
        }

        currentGuard.setBirthDate(birthDatePicker.getValue());
        currentGuard.setAddress(addressField.getText());
        currentGuard.setGender(genderBox.getValue());
        currentGuard.setTransferFrom(transferField.getText());

        currentGuard.setStatus(calculateLiveStatus(currentGuard.getShift()));
        guardDao.update(currentGuard);

        if (onSaveCallback != null) onSaveCallback.run();
        applyStatusColor(currentGuard.getStatus());

        Alert a = new Alert(Alert.AlertType.INFORMATION, "Guard Profile Updated Successfully");
        a.showAndWait();
    }

    public void setOnSaveCallback(Runnable callback) { this.onSaveCallback = callback; }

    private String calculateLiveStatus(String shift) {
        if (shift == null || shift.equalsIgnoreCase("On Leave")) return "INACTIVE";
        try {
            String hoursOnly = shift.replaceAll("[^0-9-]", "");
            String[] parts = hoursOnly.split("-");
            int startHour = Integer.parseInt(parts[0]);
            int endHour = Integer.parseInt(parts[1]);
            int currentHour = LocalTime.now().getHour();
            boolean isActive = (startHour < endHour) ? (currentHour >= startHour && currentHour < endHour) : (currentHour >= startHour || currentHour < endHour);
            return isActive ? "ACTIVE" : "INACTIVE";
        } catch (Exception e) { return "INACTIVE"; }
    }

    private void applyStatusColor(String status) {
        statusBadge.setText(status);
        if ("ACTIVE".equals(status)) {
            statusBadge.setStyle("-fx-background-color:#16a34a; -fx-text-fill:white; -fx-padding:6 12; -fx-background-radius:15; -fx-font-weight:bold;");
        } else {
            statusBadge.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-padding:6 12; -fx-background-radius:15; -fx-font-weight:bold;");
        }
    }

    private void loadGuardImage(int guardId) {
        try {
            File file = new File("python-face/photos/guards/" + guardId + ".jpg");
            if (file.exists()) {
                guardImage.setImage(new Image(file.toURI().toString()));
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void uploadPhoto() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(guardImage.getScene().getWindow());
        if (file != null) {
            guardImage.setImage(new Image(file.toURI().toString()));
        }
    }
}
package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.model.Guard;
import com.prison.util.PythonRunnerUtil;
import com.prison.util.StyledCell;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalTime;

public class GuardController {

    @FXML private TableView<Guard> guardTable;
    @FXML private TableColumn<Guard, Integer> idCol;
    @FXML private TableColumn<Guard, String> nameCol, designationCol, shiftCol, statusCol, joiningCol, descCol;

    @FXML private TextField nameField;
    @FXML private ComboBox<String> designationField, shiftField;
    @FXML private DatePicker joiningDatePicker;
    @FXML private TextArea descriptionArea;
    @FXML private Label statusInfoLabel;

    private final GuardDao dao = new GuardDao();
    private Guard selectedGuard;

    @FXML
    public void initialize() {
        setupTable();
        setupForm();
        refreshTable();

        /* Selection → edit mode */
        guardTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, g) -> {
            selectedGuard = g;
            if (g == null) return;

            nameField.setText(g.getName());
            designationField.setValue(g.getDesignation());
            shiftField.setValue(g.getShift());
            joiningDatePicker.setValue(g.getJoiningDate());
            descriptionArea.setText(g.getDescription());

            statusInfoLabel.setText("Current Status: " + calculateLiveStatus(g.getShift()));
        });
    }

    /* ============================================================
       🔥 LIVE STATUS LOGIC (TIME-BASED)
       ============================================================ */
    private String calculateLiveStatus(String shift) {
        if (shift == null || shift.equalsIgnoreCase("On Leave")) {
            return "INACTIVE";
        }

        try {
            // Extract numbers from shift string like "Morning (06-14)"
            String hoursOnly = shift.replaceAll("[^0-9-]", ""); // Result: "06-14"
            String[] parts = hoursOnly.split("-");
            int startHour = Integer.parseInt(parts[0]);
            int endHour = Integer.parseInt(parts[1]);

            int currentHour = LocalTime.now().getHour();

            boolean isActive;
            if (startHour < endHour) {
                // Normal day shift (e.g., 06 to 14)
                isActive = (currentHour >= startHour && currentHour < endHour);
            } else {
                // Overnight shift (e.g., 22 to 06)
                isActive = (currentHour >= startHour || currentHour < endHour);
            }

            return isActive ? "ACTIVE" : "INACTIVE";

        } catch (Exception e) {
            return "INACTIVE"; // Fallback if parsing fails
        }
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("guardId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        designationCol.setCellValueFactory(new PropertyValueFactory<>("designation"));

        // 🔥 SHIFT & LIVE STATUS LOGIC
        shiftCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getShift()));

        statusCol.setCellValueFactory(data -> {
            Guard g = data.getValue();
            String liveStatus = calculateLiveStatus(g.getShift());

            // Sync current calculated status to the object/DB if it changed
            if (!liveStatus.equals(g.getStatus())) {
                g.setStatus(liveStatus);
                dao.update(g);
            }

            return new SimpleStringProperty(liveStatus);
        });

        // 🔥 STATUS CELL FACTORY (Mirroring Prisoner's color logic)
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                if ("ACTIVE".equals(status)) {
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        });

        joiningCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getJoiningDate() != null ? data.getValue().getJoiningDate().toString() : "N/A"
        ));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        /* Styling matching Prisoner file */
        idCol.setCellFactory(col -> new StyledCell<>("prisoner-id"));
        nameCol.setCellFactory(col -> new StyledCell<>("prisoner-name"));
        descCol.setCellFactory(col -> new StyledCell<>("guard-description"));
        joiningCol.setCellFactory(col -> new StyledCell<>("prisoner-name"));
    }

    private void setupForm() {
        designationField.setItems(FXCollections.observableArrayList(
                "Gate Guard",
                "Tower Guard",
                "Control Room",
                "Escort Officer",
                "Supervisor"));
        // IMPORTANT: Keep this format (HH-HH) for the parser to work
        shiftField.setItems(FXCollections.observableArrayList(
                "Morning (06-14)",
                "Evening (14-22)",
                "Night (22-06)",
                "On Leave"
        ));
    }

    @FXML
    public void addGuard() {
        Guard g = new Guard();
        g.setName(nameField.getText());
        g.setDesignation(designationField.getValue());
        g.setShift(shiftField.getValue());
        g.setJoiningDate(joiningDatePicker.getValue());
        g.setDescription(descriptionArea.getText());

        // Set initial live status
        g.setStatus(calculateLiveStatus(g.getShift()));

        int id = dao.saveAndReturnId(g);
        if (id > 0) PythonRunnerUtil.trainFace("GUARD", id);

        refreshTable();
        clearFields();
    }

    @FXML
    public void updateGuard() {
        if (selectedGuard == null) return;

        selectedGuard.setName(nameField.getText());
        selectedGuard.setDesignation(designationField.getValue());
        selectedGuard.setShift(shiftField.getValue());
        selectedGuard.setJoiningDate(joiningDatePicker.getValue());
        selectedGuard.setDescription(descriptionArea.getText());

        // Update to current live status
        selectedGuard.setStatus(calculateLiveStatus(selectedGuard.getShift()));

        dao.update(selectedGuard);
        refreshTable();
        clearFields();
    }

    @FXML
    public void deleteGuard() {
        if (selectedGuard == null) return;
        dao.delete(selectedGuard.getGuardId());
        refreshTable();
        clearFields();
    }

    private void refreshTable() {
        guardTable.setItems(FXCollections.observableArrayList(dao.findAll()));
    }

    private void clearFields() {
        nameField.clear();
        designationField.setValue(null);
        shiftField.setValue(null);
        descriptionArea.clear();
        joiningDatePicker.setValue(null);
        statusInfoLabel.setText("");
        selectedGuard = null;
    }
}
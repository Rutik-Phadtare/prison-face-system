package com.prison.controller;

import com.prison.dao.PrisonerDao;
import com.prison.model.Prisoner;
import com.prison.util.PythonRunnerUtil;
import com.prison.util.StyledCell;
import com.prison.util.TimeUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;


import java.time.LocalDate;

public class PrisonerController {

    /* =========================
       TABLE & COLUMNS
       ========================= */
    @FXML private TableView<Prisoner> prisonerTable;
    @FXML private TableColumn<Prisoner, Integer> idCol;
    @FXML private TableColumn<Prisoner, String> nameCol;
    @FXML private TableColumn<Prisoner, String> crimeCol;
    @FXML private TableColumn<Prisoner, String> cellCol;
    @FXML private TableColumn<Prisoner, String> yearsCol;
    @FXML private TableColumn<Prisoner, String> statusCol;
    @FXML private TableColumn<Prisoner, String> remainingCol;
    @FXML private TableColumn<Prisoner, String> descCol;

    /* =========================
       FORM FIELDS
       ========================= */
    @FXML private TextField nameField;
    @FXML private ComboBox<String> crimeField;
    @FXML private ComboBox<String> cellField;
    @FXML private Spinner<Integer> yearSpinner;
    @FXML private Spinner<Integer> monthSpinner;
    @FXML private TextArea descriptionArea;
    @FXML private Label remainingTimeLabel;

    private final PrisonerDao dao = new PrisonerDao();
    private Prisoner selectedPrisoner;

    /* =========================
       LOAD CELLS (MAX 2 PER CELL)
       ========================= */
    private void loadCellList() {
        ObservableList<String> cells = FXCollections.observableArrayList();

        for (int i = 1; i <= 100; i++) {
            int count = dao.countPrisonersInCell(String.valueOf(i));
            if (count < 2) {
                cells.add(String.valueOf(i));
            }
        }
        cellField.setItems(cells);
    }

    /* =========================
       INITIALIZE
       ========================= */
    @FXML
    public void initialize() {

        /* Crime list (editable) */
        crimeField.setEditable(true);
        crimeField.setItems(FXCollections.observableArrayList(
                "Murder",
                "Robbery",
                "Theft",
                "Assault",
                "Fraud",
                "Rape",
                "Kidnapping",
                "Cyber Crime"
        ));

        /* Load cell list */
        loadCellList();

        /* Spinners */
        yearSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100,0)
        );
        monthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 11,1)
        );

        /* Table bindings */
        idCol.setCellValueFactory(new PropertyValueFactory<>("prisonerId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        crimeCol.setCellValueFactory(new PropertyValueFactory<>("crime"));
        cellCol.setCellValueFactory(new PropertyValueFactory<>("cellNo"));
        yearsCol.setCellValueFactory(data -> {
            Prisoner p = data.getValue();

            if (p.getSentenceStartDate() == null || p.getReleaseDate() == null) {
                return new SimpleStringProperty("-");
            }

            long months =
                    java.time.temporal.ChronoUnit.MONTHS.between(
                            p.getSentenceStartDate(),
                            p.getReleaseDate()
                    );

            return new SimpleStringProperty(
                    String.format("%.1f", months / 12.0)
            );
        });



        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        remainingCol.setCellValueFactory(data -> {
            Prisoner p = data.getValue();

            LocalDate releaseDate = p.getReleaseDate();

            // Fallback for old records
            if (releaseDate == null) {
                releaseDate = LocalDate.now().plusYears(p.getSentenceYears());
                p.setReleaseDate(releaseDate);
                dao.update(p);
            }

            // 🔥 AUTO-RELEASE LOGIC
            if (!releaseDate.isAfter(LocalDate.now())
                    && !"RELEASED".equals(p.getStatus())) {

                p.setStatus("RELEASED");
                dao.updateStatus(p.getPrisonerId(), "RELEASED");
            }

            return new SimpleStringProperty(releaseDate.toString());
        });
        remainingCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);

                if (empty || date == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                LocalDate releaseDate = LocalDate.parse(date);

                setText(date);

                if (releaseDate.isBefore(LocalDate.now())) {
                    // 🔴 Expired
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    // 🟢 Active
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            }
        });
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

                if ("RELEASED".equals(status)) {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            }
        });

        yearsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                setText(value + " yrs");
            }
        });


        descCol.setCellValueFactory(
                new PropertyValueFactory<>("description")
        );

        /* Styling */
        idCol.setCellFactory(col -> new StyledCell<>("prisoner-prisonerId"));
        nameCol.setCellFactory(col -> new StyledCell<>("prisoner-name"));
        crimeCol.setCellFactory(col -> new StyledCell<>("prisoner-crime"));
       // statusCol.setCellFactory(col -> new StyledCell<>("prisoner-status"));
       // yearsCol.setCellFactory(col -> new StyledCell<>("prisoner-years"));
       // remainingCol.setCellFactory(col -> new StyledCell<>("prisoner-remaining"));
        descCol.setCellFactory(col->new StyledCell<>("prisoner-description"));

        refreshTable();

        /* Selection → edit mode */
        prisonerTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, p) -> {
                    selectedPrisoner = p;
                    if (p == null) return;

                    nameField.setText(p.getName());
                    crimeField.setValue(p.getCrime());
                    cellField.setValue(p.getCellNo());
                    descriptionArea.setText(p.getDescription());

                    yearSpinner.getValueFactory().setValue(p.getSentenceYears());
                    monthSpinner.getValueFactory().setValue(0);

                    remainingTimeLabel.setText(
                            TimeUtil.calculateRemainingTime(p.getReleaseDate())
                    );
                });
    }

    /* =========================
       ADD PRISONER
       ========================= */
    @FXML
    public void addPrisoner() {

        if (!validateInput()) return;

        Prisoner p = new Prisoner();
        p.setName(nameField.getText());
        p.setCrime(crimeField.getValue());
        p.setCellNo(cellField.getValue());

        int years = yearSpinner.getValue();
        int months = monthSpinner.getValue();

        LocalDate start = LocalDate.now();
        LocalDate releaseDate = start.plusYears(years).plusMonths(months);

        p.setSentenceYears(years);
        p.setSentenceStartDate(start);
        p.setReleaseDate(releaseDate);
        p.setDescription(descriptionArea.getText());
        p.setStatus("IN_CUSTODY");

        int id = dao.saveAndReturnId(p);
        if (id > 0) {
            PythonRunnerUtil.trainFace("PRISONER", id);
        }

        refreshTable();
        clearFields();
    }

    /* =========================
       UPDATE PRISONER
       ========================= */
    @FXML
    public void updatePrisoner() {

        if (selectedPrisoner == null) {
            showAlert("Select Prisoner", "Choose a prisoner to update");
            return;
        }

        int years = yearSpinner.getValue();
        int months = monthSpinner.getValue();

        selectedPrisoner.setName(nameField.getText());
        selectedPrisoner.setCrime(crimeField.getValue());
        selectedPrisoner.setCellNo(cellField.getValue());
        selectedPrisoner.setSentenceYears(years);
        selectedPrisoner.setReleaseDate(
                LocalDate.now().plusYears(years).plusMonths(months)
        );
        selectedPrisoner.setDescription(descriptionArea.getText());

        dao.update(selectedPrisoner);

        refreshTable();
        clearFields();
        prisonerTable.getSelectionModel().clearSelection();
    }

    /* =========================
       DELETE
       ========================= */
    @FXML
    public void deletePrisoner() {

        Prisoner p = prisonerTable.getSelectionModel().getSelectedItem();
        if (p == null) return;

        dao.delete(p.getPrisonerId());
        refreshTable();
        clearFields();
    }

    /* =========================
       HELPERS
       ========================= */
    private void refreshTable() {
        prisonerTable.setItems(
                FXCollections.observableArrayList(dao.findAll())
        );
    }

    private void clearFields() {
        nameField.clear();
        crimeField.setValue(null);
        cellField.setValue(null);
        descriptionArea.clear();
        yearSpinner.getValueFactory().setValue(1);
        monthSpinner.getValueFactory().setValue(0);
        remainingTimeLabel.setText("");
        selectedPrisoner = null;
    }

    private boolean validateInput() {
        if (nameField.getText().isEmpty()
                || crimeField.getValue() == null
                || cellField.getValue() == null) {

            showAlert("Validation Error", "All fields are required");
            return false;
        }
        return true;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

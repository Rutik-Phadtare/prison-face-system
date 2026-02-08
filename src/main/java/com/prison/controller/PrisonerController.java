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
    @FXML private TableColumn<Prisoner, Integer> yearsCol;
    @FXML private TableColumn<Prisoner, String> statusCol;

    @FXML private TableColumn<Prisoner, String> remainingCol;
    @FXML private TableColumn<Prisoner, String> descCol;

    /* =========================
       FORM FIELDS
       ========================= */
    @FXML private TextField nameField;
    @FXML private TextField crimeField;
    @FXML private TextField cellField;
    @FXML private TextField yearsField;
    @FXML private TextField descriptionArea;
    @FXML private Label remainingTimeLabel;

    private final PrisonerDao dao = new PrisonerDao();
    private Prisoner selectedPrisoner;

    /* =========================
       INITIALIZE
       ========================= */
    @FXML
    public void initialize() {

        idCol.setCellValueFactory(new PropertyValueFactory<>("prisonerId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        crimeCol.setCellValueFactory(new PropertyValueFactory<>("crime"));
        cellCol.setCellValueFactory(new PropertyValueFactory<>("cellNo"));
        yearsCol.setCellValueFactory(new PropertyValueFactory<>("sentenceYears"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 🔹 Remaining time (computed)
        remainingCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        TimeUtil.calculateRemainingTime(data.getValue().getReleaseDate())
                )
        );

        // 🔹 Description
        descCol.setCellValueFactory(
                new PropertyValueFactory<>("description")
        );

        // Styling
        nameCol.setCellFactory(col -> new StyledCell<>("prisoner-name"));
        crimeCol.setCellFactory(col -> new StyledCell<>("prisoner-crime"));
        statusCol.setCellFactory(col -> new StyledCell<>("prisoner-status"));
        yearsCol.setCellFactory(col -> new StyledCell<>("prisoner-years"));
        remainingCol.setCellFactory(col -> new StyledCell<>("prisoner-remaining"));

        refreshTable();

        prisonerTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, p) -> {

                    selectedPrisoner = p;
                    if (p == null) return;

                    nameField.setText(p.getName());
                    crimeField.setText(p.getCrime());
                    cellField.setText(p.getCellNo());
                    yearsField.setText(String.valueOf(p.getSentenceYears()));
                    descriptionArea.setText(p.getDescription());

                    if (remainingTimeLabel != null) {
                        remainingTimeLabel.setText(
                                TimeUtil.calculateRemainingTime(p.getReleaseDate())
                        );
                    }
                });
    }


      // ADD PRISONER + TRAIN FACE

    @FXML
    public void addPrisoner() {

        if (!validateInput()) return;

        Prisoner p = new Prisoner();
        p.setName(nameField.getText());
        p.setCrime(crimeField.getText());
        p.setCellNo(cellField.getText());

        int years = Integer.parseInt(yearsField.getText());
        p.setSentenceYears(years);
        p.setReleaseDate(LocalDate.now().plusYears(years));
        p.setDescription(descriptionArea.getText());
        p.setStatus("IN_CUSTODY");

        int id = dao.saveAndReturnId(p);
        if (id > 0) {
            PythonRunnerUtil.trainFace("PRISONER", id);
        }

        refreshTable();
        clearFields();
    }


       //UPDATE PRISONER

    @FXML
    public void updatePrisoner() {

        if (selectedPrisoner == null) {
            showAlert("Select prisoner", "Choose a prisoner to update");
            return;
        }

        int years = Integer.parseInt(yearsField.getText());

        selectedPrisoner.setName(nameField.getText());
        selectedPrisoner.setCrime(crimeField.getText());
        selectedPrisoner.setCellNo(cellField.getText());
        selectedPrisoner.setSentenceYears(years);
        selectedPrisoner.setReleaseDate(LocalDate.now().plusYears(years));
        selectedPrisoner.setDescription(descriptionArea.getText());

        dao.update(selectedPrisoner);

        refreshTable();
        clearFields();
        prisonerTable.getSelectionModel().clearSelection();
    }

    /* =========================
       DELETE PRISONER
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
        crimeField.clear();
        cellField.clear();
        yearsField.clear();
        descriptionArea.clear();
        if (remainingTimeLabel != null) remainingTimeLabel.setText("");
        selectedPrisoner = null;
    }

    private boolean validateInput() {
        return !nameField.getText().isEmpty()
                && !crimeField.getText().isEmpty()
                && !cellField.getText().isEmpty()
                && !yearsField.getText().isEmpty();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

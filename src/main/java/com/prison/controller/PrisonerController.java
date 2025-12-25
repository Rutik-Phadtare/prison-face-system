package com.prison.controller;

import com.prison.model.Prisoner;
import com.prison.service.PrisonerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class PrisonerController implements Initializable {

    @FXML
    private TableView<Prisoner> prisonerTable;

    @FXML
    private TableColumn<Prisoner, Integer> idCol;

    @FXML
    private TableColumn<Prisoner, String> nameCol;

    @FXML
    private TableColumn<Prisoner, String> crimeCol;

    @FXML
    private TableColumn<Prisoner, String> cellCol;

    @FXML
    private TableColumn<Prisoner, Integer> yearsCol;

    @FXML
    private TableColumn<Prisoner, String> statusCol;

    @FXML
    private TextField nameField, crimeField, cellField, yearsField;

    private final PrisonerService prisonerService = new PrisonerService();
    private final ObservableList<Prisoner> prisonerList =
            FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        idCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getPrisonerId()).asObject());

        nameCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getName()));

        crimeCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCrime()));

        cellCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCellNo()));

        yearsCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getSentenceYears()).asObject());

        statusCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatus()));

        loadPrisoners();
    }

    private void loadPrisoners() {
        prisonerList.clear();
        prisonerList.addAll(prisonerService.getAllPrisoners());
        prisonerTable.setItems(prisonerList);
    }

    @FXML
    public void addPrisoner() {

        Prisoner p = new Prisoner();
        p.setName(nameField.getText());
        p.setCrime(crimeField.getText());
        p.setCellNo(cellField.getText());
        p.setSentenceYears(Integer.parseInt(yearsField.getText()));
        p.setStatus("IN_CUSTODY");

        int prisonerId =
                prisonerService.addPrisonerAndReturnId(p);

        if (prisonerId > 0) {
            String result =
                    com.prison.util.PythonRunnerUtil
                            .trainFace("PRISONER", prisonerId);

            if (result == null) {
                System.out.println("Training failed: No response from Python");
            } else {
                System.out.println("Training Result: " + result);

                if (!result.startsWith("OK")) {
                    javafx.scene.control.Alert alert =
                            new javafx.scene.control.Alert(
                                    javafx.scene.control.Alert.AlertType.ERROR
                            );
                    alert.setTitle("Face Training Failed");
                    alert.setHeaderText("Guard face training failed");
                    alert.setContentText(
                            "Reason: " + result +
                                    "\n\nPlease ensure:\n" +
                                    "- Face is clearly visible\n" +
                                    "- Only one person in camera\n" +
                                    "- Good lighting\n" +
                                    "- Face centered"
                    );
                    alert.showAndWait();
                }
            }

        }

        loadPrisoners();

        nameField.clear();
        crimeField.clear();
        cellField.clear();
        yearsField.clear();
    }


    @FXML
    public void deletePrisoner() {

        Prisoner selected = prisonerTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            return;
        }

        prisonerService.deletePrisoner(selected.getPrisonerId());
        loadPrisoners();
    }
}

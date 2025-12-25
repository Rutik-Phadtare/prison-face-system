package com.prison.controller;

import com.prison.model.Guard;
import com.prison.service.GuardService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class GuardController implements Initializable {

    @FXML
    private TableView<Guard> guardTable;

    @FXML
    private TableColumn<Guard, Integer> idCol;

    @FXML
    private TableColumn<Guard, String> nameCol;

    @FXML
    private TableColumn<Guard, String> designationCol;

    @FXML
    private TableColumn<Guard, String> shiftCol;

    @FXML
    private TableColumn<Guard, String> statusCol;

    @FXML
    private TextField nameField, designationField, shiftField;

    private final GuardService guardService = new GuardService();
    private final ObservableList<Guard> guardList =
            FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        idCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getGuardId()).asObject());

        nameCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getName()));

        designationCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDesignation()));

        shiftCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getShift()));

        statusCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatus()));

        loadGuards();
    }

    private void loadGuards() {
        guardList.clear();
        guardList.addAll(guardService.getAllGuards());
        guardTable.setItems(guardList);
    }

    @FXML
    public void addGuard() {

        Guard g = new Guard();
        g.setName(nameField.getText());
        g.setDesignation(designationField.getText());
        g.setShift(shiftField.getText());
        g.setStatus("ACTIVE");

        int guardId = guardService.addGuardAndReturnId(g);

        if (guardId > 0) {
            String result =
                    com.prison.util.PythonRunnerUtil
                            .trainFace("GUARD", guardId);

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

        loadGuards();

        nameField.clear();
        designationField.clear();
        shiftField.clear();
    }


    @FXML
    public void deleteGuard() {

        Guard selected = guardTable.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            return;
        }

        guardService.deleteGuard(selected.getGuardId());
        loadGuards();
    }
}

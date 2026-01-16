package com.prison.controller;

import com.prison.dao.RecognitionLogDao;
import com.prison.model.RecognitionLog;
import com.prison.session.UserSession;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class RecognitionLogController {

    @FXML
    private TableView<RecognitionLog> logTable;

    @FXML
    private TableColumn<RecognitionLog, String> typeCol;

    @FXML
    private TableColumn<RecognitionLog, Integer> idCol;

    @FXML
    private TableColumn<RecognitionLog, String> resultCol;

    @FXML
    private TableColumn<RecognitionLog, Object> timeCol;

    @FXML
    public void initialize() {

        /* =========================
           COLUMN BINDINGS
           ========================= */
        typeCol.setCellValueFactory(
                new PropertyValueFactory<>("personType"));

        idCol.setCellValueFactory(
                new PropertyValueFactory<>("personId"));

        resultCol.setCellValueFactory(
                new PropertyValueFactory<>("result"));

        timeCol.setCellValueFactory(
                new PropertyValueFactory<>("detectedAt"));

        /* =========================
           COLOR-CODE PERSON TYPE
           ========================= */
        typeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);

                if (empty || type == null) {
                    setText(null);
                    getStyleClass().removeAll(
                            "status-guard",
                            "status-prisoner",
                            "status-unknown"
                    );
                    return;
                }

                setText(type);
                getStyleClass().removeAll(
                        "status-guard",
                        "status-prisoner",
                        "status-unknown"
                );

                switch (type) {
                    case "GUARD" -> getStyleClass().add("status-guard");
                    case "PRISONER" -> getStyleClass().add("status-prisoner");
                    case "UNKNOWN" -> getStyleClass().add("status-unknown");
                }
            }
        });

        /* =========================
           LOAD DATA
           ========================= */
        RecognitionLogDao dao = new RecognitionLogDao();
        logTable.setItems(
                FXCollections.observableArrayList(dao.findAll())
        );

        /* =========================
           SORT BY LATEST FIRST
           ========================= */
        timeCol.setSortType(TableColumn.SortType.DESCENDING);
        logTable.getSortOrder().add(timeCol);

        /* =========================
           ROLE-BASED UI (CO-ADMIN)
           ========================= */
        if ("CO_ADMIN".equals(UserSession.getUser().getRole())) {
            idCol.setVisible(false);   // hide person ID
        }
    }
}

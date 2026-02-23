package com.prison.controller;

import com.prison.dao.RecognitionLogDao;
import com.prison.model.RecognitionLog;
import com.prison.session.UserSession;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RecognitionLogController {

    @FXML private Label subtitleLabel;
    @FXML private Label totalScansLbl;
    @FXML private Label recognizedLbl;
    @FXML private Label unknownLbl;
    @FXML private Label failedLbl;

    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  typeFilter;
    @FXML private ComboBox<String>  resultFilter;
    @FXML private DatePicker        fromDate;
    @FXML private DatePicker        toDate;

    @FXML private TableView<RecognitionLog>            logTable;
    @FXML private TableColumn<RecognitionLog, Void>    rowNumCol;
    @FXML private TableColumn<RecognitionLog, Object>  timeCol;
    @FXML private TableColumn<RecognitionLog, String>  nameCol;
    @FXML private TableColumn<RecognitionLog, String>  typeCol;
    @FXML private TableColumn<RecognitionLog, Integer> idCol;
    @FXML private TableColumn<RecognitionLog, String>  deptCol;
    @FXML private TableColumn<RecognitionLog, Object>  confidenceCol;
    @FXML private TableColumn<RecognitionLog, String>  locationCol;
    @FXML private TableColumn<RecognitionLog, String>  resultCol;
    @FXML private TableColumn<RecognitionLog, Void>    actionCol;

    @FXML private Label             rowCountLabel;
    @FXML private Label             pageLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;
    @FXML private Button            prevPageBtn;
    @FXML private Button            nextPageBtn;

    private ObservableList<RecognitionLog> masterData;
    private FilteredList<RecognitionLog>   filteredData;
    private int currentPage = 0;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss");

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupFilters();
        setupColumns();
        setupPagination();
        loadData();

        if ("CO_ADMIN".equals(UserSession.getUser().getRole())) {
            idCol.setVisible(false);
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList("All", "GUARD", "PRISONER", "UNKNOWN"));
        typeFilter.setValue("All");
        resultFilter.setItems(FXCollections.observableArrayList("All", "RECOGNIZED", "FAILED", "UNKNOWN"));
        resultFilter.setValue("All");

        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        typeFilter.valueProperty().addListener((o, ov, nv)   -> applyFilters());
        resultFilter.valueProperty().addListener((o, ov, nv) -> applyFilters());
        fromDate.valueProperty().addListener((o, ov, nv)     -> applyFilters());
        toDate.valueProperty().addListener((o, ov, nv)       -> applyFilters());
    }

    private void applyFilters() {
        if (filteredData == null) return;
        filteredData.setPredicate(log -> {
            // Text search – safely handle models that may not have new fields yet
            String q = searchField.getText();
            if (q != null && !q.isBlank()) {
                String lower = q.toLowerCase();
                String name  = safeStr(getPersonName(log));
                String dept  = safeStr(getDepartment(log));
                String cam   = safeStr(getCameraLocation(log));
                boolean hit  = name.toLowerCase().contains(lower)
                        || String.valueOf(log.getPersonId()).contains(lower)
                        || dept.toLowerCase().contains(lower)
                        || cam.toLowerCase().contains(lower);
                if (!hit) return false;
            }

            String tf = typeFilter.getValue();
            if (tf != null && !"All".equals(tf) && !tf.equals(log.getPersonType())) return false;

            String rf = resultFilter.getValue();
            if (rf != null && !"All".equals(rf) && !rf.equals(log.getResult())) return false;

            // Date filter – works whether detectedAt is LocalDateTime or Timestamp
            LocalDateTime ldt = toLocalDateTime(log.getDetectedAt());
            if (ldt != null) {
                LocalDate d = ldt.toLocalDate();
                if (fromDate.getValue() != null && d.isBefore(fromDate.getValue())) return false;
                if (toDate.getValue()   != null && d.isAfter(toDate.getValue()))    return false;
            }
            return true;
        });
        currentPage = 0;
        refreshPage();
        updateStats();
    }

    // ── Columns ───────────────────────────────────────────────────────────────
    private void setupColumns() {

        // Row number
        rowNumCol.setCellFactory(col -> new TableCell<RecognitionLog, Void>() {
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px; -fx-alignment:center;");
            }
        });

        // Timestamp
        timeCol.setCellValueFactory(new PropertyValueFactory<>("detectedAt"));
        timeCol.setCellFactory(col -> new TableCell<RecognitionLog, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                LocalDateTime ldt = toLocalDateTime(item);
                setText(ldt != null ? ldt.format(DISPLAY_FMT) : item.toString());
                setStyle("-fx-font-family:monospace; -fx-font-size:12px; -fx-text-fill:#374151;");
            }
        });

        // Full name with avatar badge
        nameCol.setCellValueFactory(new PropertyValueFactory<>("personName"));
        nameCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                // Fall back to "Unknown" if the model doesn't expose personName yet
                if (empty) { setGraphic(null); setText(null); return; }

                RecognitionLog log = getTableRow() != null
                        ? (RecognitionLog) getTableRow().getItem() : null;
                String displayName = (name != null && !name.isBlank())
                        ? name : getPersonName(log);

                if (displayName == null || displayName.isBlank()) {
                    setGraphic(null); setText("—"); return;
                }

                String[] parts  = displayName.trim().split("\\s+");
                String initials = parts.length >= 2
                        ? "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)
                        : displayName.substring(0, Math.min(2, displayName.length()));

                String type = log != null && log.getPersonType() != null ? log.getPersonType() : "";
                String bg;
                switch (type) {
                    case "GUARD":    bg = "#3b82f6"; break;
                    case "PRISONER": bg = "#f59e0b"; break;
                    default:         bg = "#6b7280"; break;
                }

                Label badge = new Label(initials.toUpperCase());
                badge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:white;" +
                        "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-min-width:28px; -fx-min-height:28px;" +
                        "-fx-max-width:28px; -fx-max-height:28px;" +
                        "-fx-alignment:center; -fx-background-radius:50%;");

                Label nameLbl = new Label(displayName);
                nameLbl.setStyle("-fx-font-weight:600; -fx-font-size:13px; -fx-text-fill:#0f172a;");

                HBox box = new HBox(8, badge, nameLbl);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });

        // Person type badge
        typeCol.setCellValueFactory(new PropertyValueFactory<>("personType"));
        typeCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setGraphic(null); setText(null); return; }
                String style;
                switch (type) {
                    case "GUARD":    style = "-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8;"; break;
                    case "PRISONER": style = "-fx-background-color:#fef3c7; -fx-text-fill:#b45309;"; break;
                    default:         style = "-fx-background-color:#f3f4f6; -fx-text-fill:#374151;"; break;
                }
                Label badge = new Label(type);
                badge.setStyle(style + "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-padding:3 10 3 10; -fx-background-radius:20;");
                setGraphic(badge);
                setText(null);
            }
        });

        // Person ID
        idCol.setCellValueFactory(new PropertyValueFactory<>("personId"));
        idCol.setCellFactory(col -> new TableCell<RecognitionLog, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty || id == null ? null : "#" + id);
                setStyle("-fx-text-fill:#64748b; -fx-font-family:monospace;");
            }
        });

        // Department / Block
        deptCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        deptCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                // If model doesn't have department, fall back gracefully
                if (empty) { setText(null); return; }
                RecognitionLog log = getTableRow() != null
                        ? (RecognitionLog) getTableRow().getItem() : null;
                String dept = (val != null && !val.isBlank()) ? val : getDepartment(log);
                setText(dept != null && !dept.isBlank() ? dept : "—");
                setStyle("-fx-text-fill:#64748b; -fx-font-size:12px;");
            }
        });

        // Confidence bar
        confidenceCol.setCellValueFactory(new PropertyValueFactory<>("confidence"));
        confidenceCol.setCellFactory(col -> new TableCell<RecognitionLog, Object>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label       lbl = new Label();
            private final HBox        box = new HBox(6, bar, lbl);
            {
                bar.setPrefWidth(55);
                bar.setPrefHeight(10);
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Object val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setGraphic(null); setText("—"); return; }
                double pct;
                try { pct = Double.parseDouble(val.toString()); }
                catch (NumberFormatException ex) { setGraphic(null); setText("—"); return; }

                bar.setProgress(pct / 100.0);
                String color = pct >= 85 ? "#22c55e" : pct >= 60 ? "#f59e0b" : "#ef4444";
                bar.setStyle("-fx-accent:" + color + ";");
                lbl.setText(String.format("%.0f%%", pct));
                lbl.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
                setGraphic(box);
                setText(null);
            }
        });

        // Camera / Location
        locationCol.setCellValueFactory(new PropertyValueFactory<>("cameraLocation"));
        locationCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty) { setText(null); return; }
                RecognitionLog log = getTableRow() != null
                        ? (RecognitionLog) getTableRow().getItem() : null;
                String cam = (val != null && !val.isBlank()) ? val : getCameraLocation(log);
                setText(cam != null && !cam.isBlank() ? "\uD83D\uDCF7  " + cam : "—");
                setStyle("-fx-font-size:12px; -fx-text-fill:#374151;");
            }
        });

        // Result badge
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override
            protected void updateItem(String res, boolean empty) {
                super.updateItem(res, empty);
                if (empty || res == null) { setGraphic(null); setText(null); return; }
                String icon, style;
                switch (res) {
                    case "RECOGNIZED":
                        icon  = "\u2714";
                        style = "-fx-background-color:#dcfce7; -fx-text-fill:#15803d;"; break;
                    case "FAILED":
                        icon  = "\u2718";
                        style = "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;"; break;
                    default:
                        icon  = "?";
                        style = "-fx-background-color:#f3f4f6; -fx-text-fill:#374151;"; break;
                }
                Label badge = new Label(icon + "  " + res);
                badge.setStyle(style + "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-padding:3 10 3 10; -fx-background-radius:20;");
                setGraphic(badge);
                setText(null);
            }
        });

        // Detail button
        actionCol.setCellFactory(col -> new TableCell<RecognitionLog, Void>() {
            private final Button btn = new Button("View");
            {
                btn.setStyle("-fx-background-color:#6366f1; -fx-text-fill:white;" +
                        "-fx-font-size:11px; -fx-background-radius:6;" +
                        "-fx-padding:4 12 4 12; -fx-cursor:hand;");
                btn.setOnAction(e -> {
                    RecognitionLog log = getTableView().getItems().get(getIndex());
                    openDetailDialog(log);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    private void setupPagination() {
        pageSizeCombo.setItems(FXCollections.observableArrayList(25, 50, 100, 200));
        pageSizeCombo.setValue(50);
        pageSizeCombo.valueProperty().addListener((o, ov, nv) -> {
            currentPage = 0;
            refreshPage();
        });
    }

    private void refreshPage() {
        if (filteredData == null) return;
        int size  = pageSizeCombo.getValue() == null ? 50 : pageSizeCombo.getValue();
        int total = filteredData.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / size));
        currentPage = Math.min(currentPage, pages - 1);

        int from = currentPage * size;
        int to   = Math.min(from + size, total);

        ObservableList<RecognitionLog> page =
                FXCollections.observableArrayList(filteredData.subList(from, to));

        // Sort latest first – safe regardless of detectedAt type
        page.sort((a, b) -> {
            LocalDateTime la = toLocalDateTime(a.getDetectedAt());
            LocalDateTime lb = toLocalDateTime(b.getDetectedAt());
            if (la == null && lb == null) return 0;
            if (la == null) return 1;
            if (lb == null) return -1;
            return lb.compareTo(la);
        });

        logTable.setItems(page);
        rowCountLabel.setText(total + " records");
        pageLabel.setText("Page " + (currentPage + 1) + " of " + pages);
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable(currentPage >= pages - 1);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private void updateStats() {
        if (filteredData == null) return;
        long total      = filteredData.size();
        long recognized = filteredData.stream().filter(l -> "RECOGNIZED".equals(l.getResult())).count();
        long unknown    = filteredData.stream().filter(l -> "UNKNOWN".equals(l.getPersonType())).count();
        long failed     = filteredData.stream().filter(l -> "FAILED".equals(l.getResult())).count();

        totalScansLbl.setText(String.valueOf(total));
        recognizedLbl.setText(String.valueOf(recognized));
        unknownLbl.setText(String.valueOf(unknown));
        failedLbl.setText(String.valueOf(failed));
        subtitleLabel.setText(total + " recognition events");
    }

    // ── Load data ─────────────────────────────────────────────────────────────
    private void loadData() {
        List<RecognitionLog> all = new RecognitionLogDao().findAll();
        masterData   = FXCollections.observableArrayList(all);
        filteredData = new FilteredList<>(masterData, p -> true);
        currentPage  = 0;
        refreshPage();
        updateStats();
    }

    // ── Button handlers ───────────────────────────────────────────────────────
    @FXML private void onRefresh()  { loadData(); }
    @FXML private void onPrevPage() { currentPage--; refreshPage(); }
    @FXML private void onNextPage() { currentPage++; refreshPage(); }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        typeFilter.setValue("All");
        resultFilter.setValue("All");
        fromDate.setValue(null);
        toDate.setValue(null);
    }

    @FXML
    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Recognition Logs");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("recognition_logs.csv");
        File file = chooser.showSaveDialog(logTable.getScene().getWindow());
        if (file == null) return;

        try (FileWriter w = new FileWriter(file)) {
            w.write("ID,Name,Type,Department,Camera,Confidence,Result,DetectedAt\n");
            for (RecognitionLog log : filteredData) {
                Object conf = getConfidence(log);
                double confVal = conf != null ? Double.parseDouble(conf.toString()) : 0.0;
                w.write(String.format("%d,%s,%s,%s,%s,%.1f,%s,%s\n",
                        log.getPersonId(),
                        csv(getPersonName(log)),
                        csv(log.getPersonType()),
                        csv(getDepartment(log)),
                        csv(getCameraLocation(log)),
                        confVal,
                        csv(log.getResult()),
                        log.getDetectedAt()));
            }
            new Alert(Alert.AlertType.INFORMATION,
                    "Saved to:\n" + file.getAbsolutePath(), ButtonType.OK).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Export failed:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    // ── Detail dialog ─────────────────────────────────────────────────────────
    private void openDetailDialog(RecognitionLog log) {
        String personType = nvl(log.getPersonType());
        String name       = nvl(getPersonName(log));
        String dept       = nvl(getDepartment(log));
        String cam        = nvl(getCameraLocation(log));
        Object conf       = getConfidence(log);
        String confStr    = conf != null ? String.format("%.1f%%", Double.parseDouble(conf.toString())) : "N/A";
        LocalDateTime ldt = toLocalDateTime(log.getDetectedAt());
        String detectedAt = ldt != null ? ldt.format(DISPLAY_FMT) : (log.getDetectedAt() != null ? log.getDetectedAt().toString() : "N/A");

        // Avatar badge
        String[] parts  = name.trim().split("\\s+");
        String initials = parts.length >= 2
                ? "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)
                : name.substring(0, Math.min(2, name.length()));

        String avatarBg;
        switch (personType) {
            case "GUARD":    avatarBg = "#3b82f6"; break;
            case "PRISONER": avatarBg = "#f59e0b"; break;
            default:         avatarBg = "#6b7280"; break;
        }

        Label avatar = new Label(initials.toUpperCase());
        avatar.setStyle("-fx-background-color:" + avatarBg + "; -fx-text-fill:white;" +
                "-fx-font-size:18px; -fx-font-weight:bold;" +
                "-fx-min-width:52px; -fx-min-height:52px;" +
                "-fx-max-width:52px; -fx-max-height:52px;" +
                "-fx-alignment:center; -fx-background-radius:50%;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label idLabel = new Label("ID: #" + log.getPersonId());
        idLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        String badgeBg, badgeFg;
        switch (personType) {
            case "GUARD":    badgeBg = "#dbeafe"; badgeFg = "#1d4ed8"; break;
            case "PRISONER": badgeBg = "#fef3c7"; badgeFg = "#b45309"; break;
            default:         badgeBg = "#f3f4f6"; badgeFg = "#374151"; break;
        }
        Label typeBadge = new Label(personType);
        typeBadge.setStyle("-fx-background-color:" + badgeBg + "; -fx-text-fill:" + badgeFg + ";" +
                "-fx-font-size:11px; -fx-font-weight:bold;" +
                "-fx-padding:3 10 3 10; -fx-background-radius:20;");

        VBox nameBox = new VBox(4, nameLabel, idLabel, typeBadge);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        HBox personHeader = new HBox(14, avatar, nameBox);
        personHeader.setAlignment(Pos.CENTER_LEFT);
        personHeader.setStyle("-fx-background-color:#f8fafc; -fx-padding:16;" +
                "-fx-background-radius:10; -fx-border-color:#e2e8f0;" +
                "-fx-border-radius:10; -fx-border-width:1;");

        // Info rows
        String[][] rows = {
                {"\uD83C\uDFE2", "Block / Dept",  dept},
                {"\uD83D\uDCF7", "Camera",        cam},
                {"\uD83C\uDFAF", "Confidence",    confStr},
                {"\u2705",       "Result",        nvl(log.getResult())},
                {"\uD83D\uDD50", "Detected At",   detectedAt},
        };

        VBox grid = new VBox(0);
        grid.setStyle("-fx-background-color:white; -fx-background-radius:10;" +
                "-fx-border-color:#e2e8f0; -fx-border-radius:10; -fx-border-width:1;");

        for (int i = 0; i < rows.length; i++) {
            Label icon  = new Label(rows[i][0]);
            icon.setStyle("-fx-font-size:14px; -fx-min-width:24px;");
            Label key   = new Label(rows[i][1]);
            key.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b; -fx-min-width:110px;");
            Label value = new Label(rows[i][2]);
            value.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#0f172a;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox rowBox = new HBox(10, icon, key, spacer, value);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            String border = i < rows.length - 1
                    ? "-fx-border-color:transparent transparent #f1f5f9 transparent; -fx-border-width:0 0 1 0;"
                    : "";
            rowBox.setStyle("-fx-padding:12 16 12 16;" + border);
            grid.getChildren().add(rowBox);
        }

        VBox content = new VBox(14, personHeader, grid);
        content.setPrefWidth(460);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Recognition Detail");
        dlg.setHeaderText(null);
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setStyle("-fx-background-color:white; -fx-padding:20;");
        dlg.showAndWait();
    }

    // ── Safe field accessors (handles models with or without new fields) ───────
    /**
     * Try to get personName. Falls back gracefully if the field doesn't exist yet.
     * Once you add getPersonName() to RecognitionLog, this will use it automatically.
     */
    private String getPersonName(RecognitionLog log) {
        if (log == null) return null;
        try { return log.getPersonName(); }
        catch (Exception e) { return null; }
    }

    private String getDepartment(RecognitionLog log) {
        if (log == null) return null;
        try { return log.getDepartment(); }
        catch (Exception e) { return null; }
    }

    private String getCameraLocation(RecognitionLog log) {
        if (log == null) return null;
        try { return log.getCameraLocation(); }
        catch (Exception e) { return null; }
    }

    private Object getConfidence(RecognitionLog log) {
        if (log == null) return null;
        try { return log.getConfidence(); }
        catch (Exception e) { return null; }
    }

    /**
     * Converts whatever detectedAt returns (LocalDateTime, Timestamp, String)
     * into a LocalDateTime so everything else can use it consistently.
     */
    private LocalDateTime toLocalDateTime(Object raw) {
        if (raw == null) return null;
        if (raw instanceof LocalDateTime) return (LocalDateTime) raw;
        if (raw instanceof Timestamp)     return ((Timestamp) raw).toLocalDateTime();
        if (raw instanceof java.util.Date) {
            return new java.util.Date(((java.util.Date) raw).getTime())
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        // Last resort: try parsing the string
        try { return LocalDateTime.parse(raw.toString()); }
        catch (Exception e) { return null; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String nvl(String s)  { return s != null && !s.isBlank() ? s : "—"; }
    private String safeStr(String s) { return s != null ? s : ""; }
    private String csv(String s)  { return s != null ? "\"" + s.replace("\"", "\"\"") + "\"" : "\"\""; }
}
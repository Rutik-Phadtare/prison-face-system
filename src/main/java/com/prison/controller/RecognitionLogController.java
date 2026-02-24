package com.prison.controller;

import com.prison.dao.RecognitionLogDao;
import com.prison.model.RecognitionLog;
import com.prison.session.UserSession;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RecognitionLogController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label subtitleLabel;
    @FXML private Label totalScansLbl;
    @FXML private Label recognizedLbl;
    @FXML private Label unknownLbl;
    @FXML private Label failedLbl;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> resultFilter;
    @FXML private DatePicker       fromDate;
    @FXML private DatePicker       toDate;

    @FXML private TableView<RecognitionLog>            logTable;
    @FXML private TableColumn<RecognitionLog, Void>    rowNumCol;
    @FXML private TableColumn<RecognitionLog, Object>  timeCol;
    @FXML private TableColumn<RecognitionLog, String>  nameCol;
    @FXML private TableColumn<RecognitionLog, String>  typeCol;
    @FXML private TableColumn<RecognitionLog, Integer> idCol;
    @FXML private TableColumn<RecognitionLog, String>  deptCol;
    @FXML private TableColumn<RecognitionLog, String>  confidenceCol;   // Shift / Crime
    @FXML private TableColumn<RecognitionLog, String>  locationCol;     // Phone / Danger
    @FXML private TableColumn<RecognitionLog, String>  resultCol;
    @FXML private TableColumn<RecognitionLog, Void>    actionCol;

    @FXML private Label             rowCountLabel;
    @FXML private Label             pageLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;
    @FXML private Button            prevPageBtn;
    @FXML private Button            nextPageBtn;
    @FXML private Button            exportBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private ObservableList<RecognitionLog> masterData;
    private FilteredList<RecognitionLog>   filteredData;
    private int currentPage = 0;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss");

    // ── PDF colour theme  ─────────────────────────────────────────────────────
    // Deep Maroon / Silver  — same feel as GuardController but distinct
    private static final Color PDF_DARK     = new Color(80,  10,  10);   // deep maroon
    private static final Color PDF_ACCENT   = new Color(139, 20,  20);   // rich red
    private static final Color PDF_SILVER   = new Color(180, 180, 190);  // silver rule
    private static final Color PDF_ROW_ALT  = new Color(252, 245, 245);  // blush alt row
    private static final Color PDF_GREEN    = new Color(212, 248, 220);
    private static final Color PDF_RED      = new Color(255, 218, 218);
    private static final Color PDF_AMBER    = new Color(255, 245, 200);
    private static final Color PDF_GUARD    = new Color(214, 230, 255);
    private static final Color PDF_PRISONER = new Color(255, 240, 210);

    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        confidenceCol.setText("Shift / Crime");
        locationCol.setText("Phone / Danger");
        setupFilters();
        setupColumns();
        setupPagination();
        loadData();
        if ("CO_ADMIN".equals(UserSession.getUser().getRole())) {
            idCol.setVisible(false);
        }
    }

    // ── Image loader ──────────────────────────────────────────────────────────
    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        File f = new File(path);
        if (f.exists()) {
            try { return new Image(f.toURI().toString()); }
            catch (Exception ignored) {}
        }
        return null;
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList("All","GUARD","PRISONER","UNKNOWN"));
        typeFilter.setValue("All");
        resultFilter.setItems(FXCollections.observableArrayList("All","RECOGNIZED","FAILED","UNKNOWN"));
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
            String q = searchField.getText();
            if (q != null && !q.isBlank()) {
                String lower = q.toLowerCase();
                boolean hit = safe(log.getPersonName()).toLowerCase().contains(lower)
                        || String.valueOf(log.getPersonId()).contains(lower)
                        || safe(log.getDepartment()).toLowerCase().contains(lower)
                        || safe(log.getExtraInfo()).toLowerCase().contains(lower)
                        || safe(log.getContactInfo()).toLowerCase().contains(lower);
                if (!hit) return false;
            }
            String tf = typeFilter.getValue();
            if (tf != null && !"All".equals(tf) && !tf.equals(log.getPersonType())) return false;
            String rf = resultFilter.getValue();
            if (rf != null && !"All".equals(rf) && !rf.equals(log.getResult())) return false;
            if (log.getDetectedAt() != null) {
                LocalDate d = log.getDetectedAt().toLocalDate();
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

        rowNumCol.setCellFactory(col -> new TableCell<RecognitionLog, Void>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px; -fx-alignment:center;");
            }
        });

        timeCol.setCellValueFactory(new PropertyValueFactory<>("detectedAt"));
        timeCol.setCellFactory(col -> new TableCell<RecognitionLog, Object>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item instanceof LocalDateTime
                        ? ((LocalDateTime) item).format(DISPLAY_FMT) : item.toString());
                setStyle("-fx-font-family:monospace; -fx-font-size:12px; -fx-text-fill:#374151;");
            }
        });

        nameCol.setCellValueFactory(new PropertyValueFactory<>("personName"));
        nameCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty) { setGraphic(null); setText(null); return; }
                RecognitionLog log = getTableRow() != null
                        ? (RecognitionLog) getTableRow().getItem() : null;
                if (name == null || name.isBlank()) { setGraphic(null); setText("—"); return; }
                String bg = typeColor(log != null ? log.getPersonType() : "");
                Region avatar = buildAvatar(log != null ? log.getImagePath() : null, name, bg, 30);
                Label nameLbl = new Label(name);
                nameLbl.setStyle("-fx-font-weight:600; -fx-font-size:13px; -fx-text-fill:#0f172a;");
                HBox box = new HBox(8, avatar, nameLbl);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        typeCol.setCellValueFactory(new PropertyValueFactory<>("personType"));
        typeCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String type, boolean empty) {
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
                setGraphic(badge); setText(null);
            }
        });

        idCol.setCellValueFactory(new PropertyValueFactory<>("personId"));
        idCol.setCellFactory(col -> new TableCell<RecognitionLog, Integer>() {
            @Override protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty || id == null ? "—" : "#" + id);
                setStyle("-fx-text-fill:#64748b; -fx-font-family:monospace;");
            }
        });

        deptCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        deptCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null || val.isBlank() ? "—" : val);
                setStyle("-fx-text-fill:#374151; -fx-font-size:12px;");
            }
        });

        confidenceCol.setCellValueFactory(new PropertyValueFactory<>("extraInfo"));
        confidenceCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null || "—".equals(val)) { setText("—"); setGraphic(null); return; }
                RecognitionLog log = getTableRow() != null ? (RecognitionLog) getTableRow().getItem() : null;
                if ("PRISONER".equals(log != null ? log.getPersonType() : "")) {
                    Label b = new Label("\u2696 " + val);
                    b.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;" +
                            "-fx-font-size:11px; -fx-padding:2 8 2 8; -fx-background-radius:12;");
                    setGraphic(b); setText(null);
                } else {
                    setText("\uD83D\uDD52 " + val); setGraphic(null);
                    setStyle("-fx-font-size:12px; -fx-text-fill:#374151;");
                }
            }
        });

        locationCol.setCellValueFactory(new PropertyValueFactory<>("contactInfo"));
        locationCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null || "—".equals(val)) { setText("—"); setGraphic(null); return; }
                RecognitionLog log = getTableRow() != null ? (RecognitionLog) getTableRow().getItem() : null;
                if ("PRISONER".equals(log != null ? log.getPersonType() : "")) {
                    String style;
                    switch (val.toUpperCase()) {
                        case "HIGH":   style = "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;"; break;
                        case "MEDIUM": style = "-fx-background-color:#fef3c7; -fx-text-fill:#b45309;"; break;
                        default:       style = "-fx-background-color:#dcfce7; -fx-text-fill:#15803d;"; break;
                    }
                    Label b = new Label("\u26A0 " + val);
                    b.setStyle(style + "-fx-font-size:11px; -fx-font-weight:bold;" +
                            "-fx-padding:2 8 2 8; -fx-background-radius:12;");
                    setGraphic(b); setText(null);
                } else {
                    setText("\uD83D\uDCDE " + val); setGraphic(null);
                    setStyle("-fx-font-size:12px; -fx-text-fill:#374151;");
                }
            }
        });

        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String res, boolean empty) {
                super.updateItem(res, empty);
                if (empty || res == null) { setGraphic(null); setText(null); return; }
                String icon, style;
                switch (res) {
                    case "RECOGNIZED": icon = "\u2714"; style = "-fx-background-color:#dcfce7; -fx-text-fill:#15803d;"; break;
                    case "FAILED":     icon = "\u2718"; style = "-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c;"; break;
                    default:           icon = "?";      style = "-fx-background-color:#f3f4f6; -fx-text-fill:#374151;"; break;
                }
                Label b = new Label(icon + "  " + res);
                b.setStyle(style + "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-padding:3 10 3 10; -fx-background-radius:20;");
                setGraphic(b); setText(null);
            }
        });

        actionCol.setCellFactory(col -> new TableCell<RecognitionLog, Void>() {
            private final Button btn = new Button("View");
            {
                btn.setStyle("-fx-background-color:#6366f1; -fx-text-fill:white;" +
                        "-fx-font-size:11px; -fx-background-radius:6; -fx-padding:4 12 4 12;");
                btn.setOnAction(e -> openDetailDialog(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // ── Avatar helper ─────────────────────────────────────────────────────────
    private Region buildAvatar(String imagePath, String name, String bgColor, int size) {
        Image img = loadImage(imagePath);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size); iv.setFitHeight(size); iv.setPreserveRatio(false);
            StackPane sp = new StackPane(iv);
            sp.setMinWidth(size); sp.setMaxWidth(size);
            sp.setMinHeight(size); sp.setMaxHeight(size);
            sp.setStyle("-fx-background-radius:" + (size/2) + "; -fx-border-color:" + bgColor +
                    "; -fx-border-radius:" + (size/2) + "; -fx-border-width:2;");
            return sp;
        }
        String[] parts  = safe(name).trim().split("\\s+");
        String initials = parts.length >= 2
                ? "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)
                : safe(name).substring(0, Math.min(2, safe(name).length()));
        Label badge = new Label(initials.isEmpty() ? "?" : initials.toUpperCase());
        badge.setStyle("-fx-background-color:" + bgColor + "; -fx-text-fill:white;" +
                "-fx-font-size:" + (size/3) + "px; -fx-font-weight:bold;" +
                "-fx-min-width:" + size + "px; -fx-min-height:" + size + "px;" +
                "-fx-max-width:" + size + "px; -fx-max-height:" + size + "px;" +
                "-fx-alignment:center; -fx-background-radius:50%;");
        return badge;
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    private void setupPagination() {
        pageSizeCombo.setItems(FXCollections.observableArrayList(25, 50, 100, 200));
        pageSizeCombo.setValue(50);
        pageSizeCombo.valueProperty().addListener((o, ov, nv) -> { currentPage = 0; refreshPage(); });
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
        page.sort((a, b) -> {
            if (a.getDetectedAt() == null) return 1;
            if (b.getDetectedAt() == null) return -1;
            return b.getDetectedAt().compareTo(a.getDetectedAt());
        });
        logTable.setItems(page);
        rowCountLabel.setText(total + " records");
        pageLabel.setText("Page " + (currentPage + 1) + " of " + pages);
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable(currentPage >= pages - 1);
    }

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

    private void loadData() {
        List<RecognitionLog> all = new RecognitionLogDao().findAll();
        masterData   = FXCollections.observableArrayList(all);
        filteredData = new FilteredList<>(masterData, p -> true);
        currentPage  = 0;
        refreshPage();
        updateStats();
    }

    // ── Button handlers ───────────────────────────────────────────────────────
    @FXML private void onRefresh()      { loadData(); }
    @FXML private void onPrevPage()     { currentPage--; refreshPage(); }
    @FXML private void onNextPage()     { currentPage++; refreshPage(); }
    @FXML private void onClearFilters() {
        searchField.clear(); typeFilter.setValue("All");
        resultFilter.setValue("All"); fromDate.setValue(null); toDate.setValue(null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PDF EXPORT  —  Deep Maroon / Silver government theme
    //  Follows the exact same structure as GuardController.printGuardTable()
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    public void onExport() {
        List<RecognitionLog> records = new java.util.ArrayList<>(filteredData);
        if (records.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No records to export.").showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Recognition Log Report");
        fileChooser.setInitialFileName("FACE_RECOGNITION_LOG_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(logTable.getScene().getWindow());
        if (file == null) return;

        try {
            // ── Document setup ────────────────────────────────────────────────
            Document document = new Document(PageSize.A4.rotate(), 40, 40, 60, 55);
            PdfWriter writer  = PdfWriter.getInstance(document, new FileOutputStream(file));

            // ── Page border + footer (same pattern as GuardController) ─────────
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    try {
                        PdfContentByte cb = w.getDirectContent();

                        // Outer maroon border
                        cb.setLineWidth(2f);
                        cb.setColorStroke(PDF_DARK);
                        cb.rectangle(25, 25,
                                doc.getPageSize().getWidth() - 50,
                                doc.getPageSize().getHeight() - 50);
                        cb.stroke();

                        // Inner silver border
                        cb.setLineWidth(0.4f);
                        cb.setColorStroke(PDF_SILVER);
                        cb.rectangle(30, 30,
                                doc.getPageSize().getWidth() - 60,
                                doc.getPageSize().getHeight() - 60);
                        cb.stroke();

                        // Footer divider
                        cb.setLineWidth(0.8f);
                        cb.setColorStroke(new Color(200, 180, 180));
                        cb.moveTo(35, 42);
                        cb.lineTo(doc.getPageSize().getWidth() - 35, 42);
                        cb.stroke();

                        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase("CONFIDENTIAL — PRISON SECURITY SURVEILLANCE", footerFont),
                                40, 32, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Face Recognition Surveillance Register", footerFont),
                                doc.getPageSize().getWidth() / 2, 32, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                                new Phrase("Page " + w.getPageNumber() + "  |  " +
                                        LocalDateTime.now().format(
                                                DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")),
                                        footerFont),
                                doc.getPageSize().getWidth() - 40, 32, 0);
                    } catch (Exception ignored) {}
                }
            });

            document.open();

            // ── Header — logo left, text centre, logo right ───────────────────
            PdfPTable headerTable = new PdfPTable(new float[]{1, 5, 1});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(18f);

            // Left logo
            PdfPCell leftLogo = new PdfPCell();
            leftLogo.setBorder(PdfPCell.NO_BORDER);
            leftLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(
                        "src/main/resources/images/logo.jpeg");
                logo.scaleToFit(55, 55);
                leftLogo.addElement(logo);
            } catch (Exception ignored) {}
            headerTable.addCell(leftLogo);

            // Centre text
            PdfPCell centre = new PdfPCell();
            centre.setBorder(PdfPCell.NO_BORDER);
            centre.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph dept = new Paragraph(
                    "DEPARTMENT OF CORRECTIONS & PRISON SECURITY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PDF_DARK));
            dept.setAlignment(Element.ALIGN_CENTER);
            dept.setSpacingAfter(4f);
            centre.addElement(dept);

            Paragraph titleP = new Paragraph(
                    "FACE RECOGNITION SURVEILLANCE REGISTER",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PDF_DARK));
            titleP.setAlignment(Element.ALIGN_CENTER);
            titleP.setSpacingAfter(3f);
            centre.addElement(titleP);

            Paragraph subP = new Paragraph(
                    "Security Access & Detection Report  —  Classification: Restricted",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
            subP.setAlignment(Element.ALIGN_CENTER);
            centre.addElement(subP);
            headerTable.addCell(centre);

            // Right logo
            PdfPCell rightLogo = new PdfPCell();
            rightLogo.setBorder(PdfPCell.NO_BORDER);
            rightLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            rightLogo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                com.lowagie.text.Image logo2 = com.lowagie.text.Image.getInstance(
                        "src/main/resources/images/logo.jpeg");
                logo2.scaleToFit(55, 55);
                rightLogo.addElement(logo2);
            } catch (Exception ignored) {}
            headerTable.addCell(rightLogo);

            document.add(headerTable);

            // ── Meta bar ──────────────────────────────────────────────────────
            long recognized = records.stream().filter(l -> "RECOGNIZED".equals(l.getResult())).count();
            long unknown    = records.stream().filter(l -> "UNKNOWN".equals(l.getPersonType())).count();
            long failed     = records.stream().filter(l -> "FAILED".equals(l.getResult())).count();
            long guards     = records.stream().filter(l -> "GUARD".equals(l.getPersonType())).count();
            long prisoners  = records.stream().filter(l -> "PRISONER".equals(l.getPersonType())).count();

            PdfPTable metaBar = new PdfPTable(3);
            metaBar.setWidthPercentage(100);
            metaBar.setSpacingAfter(14f);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);
            addMetaCell(metaBar, "TOTAL EVENTS: " + records.size(), metaFont, PDF_DARK);
            addMetaCell(metaBar,
                    "RECOGNIZED: " + recognized + "  |  UNKNOWN: " + unknown + "  |  FAILED: " + failed,
                    metaFont, PDF_ACCENT);
            addMetaCell(metaBar,
                    "GENERATED: " + LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")),
                    metaFont, PDF_DARK);
            document.add(metaBar);

            // ── Data table ────────────────────────────────────────────────────
            // Columns: #, Detected At, Full Name, Type, ID, Dept/Cell, Shift/Crime, Phone/Danger, Result
            PdfPTable table = new PdfPTable(new float[]{0.5f, 2.2f, 2.2f, 1.1f, 0.8f, 1.8f, 2.0f, 1.8f, 1.4f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(20f);
            table.setHeaderRows(1);

            String[] headers = {"#", "Detected At", "Full Name", "Type", "ID",
                    "Dept / Cell", "Shift / Crime", "Phone / Danger", "Result"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : headers) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
                hCell.setBackgroundColor(PDF_ACCENT);
                hCell.setPadding(9);
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                hCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                hCell.setBorderColor(new Color(180, 140, 140));
                table.addCell(hCell);
            }

            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA,      9,    Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  9,    Color.BLACK);
            Font italFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8,  Color.DARK_GRAY);

            for (int i = 0; i < records.size(); i++) {
                RecognitionLog log = records.get(i);
                Color rowBg = (i % 2 == 0) ? Color.WHITE : PDF_ROW_ALT;

                // Result colour
                Color resBg;
                switch (nvl(log.getResult())) {
                    case "RECOGNIZED": resBg = PDF_GREEN; break;
                    case "FAILED":     resBg = PDF_RED;   break;
                    default:           resBg = PDF_AMBER; break;
                }

                // Type colour
                Color typeBg;
                switch (nvl(log.getPersonType())) {
                    case "GUARD":    typeBg = PDF_GUARD;    break;
                    case "PRISONER": typeBg = PDF_PRISONER; break;
                    default:         typeBg = new Color(240, 240, 240); break;
                }

                addDataCell(table, String.valueOf(i + 1),
                        dataFont, rowBg, Element.ALIGN_CENTER);
                addDataCell(table, fmtTime(log),
                        dataFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, nvl(log.getPersonName()),
                        boldFont, rowBg, Element.ALIGN_LEFT);

                // Type badge cell (coloured)
                PdfPCell typeCell = new PdfPCell(new Phrase(nvl(log.getPersonType()),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PDF_DARK)));
                typeCell.setBackgroundColor(typeBg);
                typeCell.setPadding(8);
                typeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                typeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                typeCell.setBorderColor(new Color(200, 180, 180));
                table.addCell(typeCell);

                addDataCell(table,
                        log.getPersonId() != null ? "#" + log.getPersonId() : "—",
                        dataFont, rowBg, Element.ALIGN_CENTER);
                addDataCell(table, nvl(log.getDepartment()),  dataFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, nvl(log.getExtraInfo()),   italFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, nvl(log.getContactInfo()), dataFont, rowBg, Element.ALIGN_LEFT);

                // Result badge cell (coloured)
                PdfPCell resCell = new PdfPCell(new Phrase(nvl(log.getResult()),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PDF_DARK)));
                resCell.setBackgroundColor(resBg);
                resCell.setPadding(8);
                resCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                resCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                resCell.setBorderColor(new Color(200, 180, 180));
                table.addCell(resCell);
            }
            document.add(table);

            // ── Summary section (same as GuardController) ─────────────────────
            PdfPTable summaryTable = new PdfPTable(5);
            summaryTable.setWidthPercentage(75);
            summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            summaryTable.setSpacingAfter(15f);

            Font sumLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,  PDF_DARK);
            Font sumVal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);

            addSummaryCell(summaryTable, "Total Events",  String.valueOf(records.size()), sumLabel, sumVal, new Color(252, 245, 245));
            addSummaryCell(summaryTable, "Recognized",    String.valueOf(recognized),     sumLabel, sumVal, new Color(212, 248, 220));
            addSummaryCell(summaryTable, "Unknown",       String.valueOf(unknown),         sumLabel, sumVal, new Color(255, 245, 200));
            addSummaryCell(summaryTable, "Guards",        String.valueOf(guards),           sumLabel, sumVal, new Color(214, 230, 255));
            addSummaryCell(summaryTable, "Prisoners",     String.valueOf(prisoners),        sumLabel, sumVal, new Color(255, 240, 210));
            document.add(summaryTable);

            // ── Verification strip (same as GuardController) ──────────────────
            PdfPTable verifyTable = new PdfPTable(1);
            verifyTable.setWidthPercentage(100);
            Font verifyFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY);
            PdfPCell verifyCell = new PdfPCell(new Phrase(
                    "This is an official government document. Unauthorized reproduction or disclosure is prohibited. " +
                            "Report ID: FRL-" + (System.currentTimeMillis() % 1000000), verifyFont));
            verifyCell.setBorder(PdfPCell.TOP);
            verifyCell.setBorderColor(Color.LIGHT_GRAY);
            verifyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            verifyCell.setPadding(8);
            verifyCell.setBackgroundColor(new Color(252, 248, 248));
            verifyTable.addCell(verifyCell);
            document.add(verifyTable);

            document.close();

            new Alert(Alert.AlertType.INFORMATION,
                    "Recognition Log Report exported successfully!").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage()).show();
        }
    }

    // ── PDF helper methods (same signature as GuardController) ────────────────
    private void addMetaCell(PdfPTable t, String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setBorder(PdfPCell.NO_BORDER);
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void addDataCell(PdfPTable t, String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "—" : text, f));
        c.setBackgroundColor(bg);
        c.setPadding(8);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(new Color(210, 190, 190));
        t.addCell(c);
    }

    private void addSummaryCell(PdfPTable t, String label, String value,
                                Font lf, Font vf, Color bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setPadding(10);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorderColor(new Color(210, 190, 190));
        c.addElement(new Phrase(label, lf));
        c.addElement(new Phrase(value, vf));
        t.addCell(c);
    }

    // ── Detail dialog ─────────────────────────────────────────────────────────
    private void openDetailDialog(RecognitionLog log) {
        String personType = nvl(log.getPersonType());
        String name       = nvl(log.getPersonName());
        boolean isGuard   = "GUARD".equals(personType);
        boolean isPrison  = "PRISONER".equals(personType);
        String  avatarBg  = typeColor(personType);

        // Photo or initials
        Region photoArea;
        Image img = loadImage(log.getImagePath());
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(120); iv.setFitHeight(140); iv.setPreserveRatio(true);
            StackPane sp = new StackPane(iv);
            sp.setMinWidth(120); sp.setMaxWidth(120);
            sp.setMinHeight(140); sp.setMaxHeight(140);
            sp.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                    "-fx-border-color:" + avatarBg + "; -fx-border-radius:12;" +
                    "-fx-border-width:3;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3);");
            photoArea = sp;
        } else {
            String[] parts  = safe(name).trim().split("\\s+");
            String initials = "—".equals(name) ? "?"
                    : parts.length >= 2 ? "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)
                    : safe(name).substring(0, Math.min(2, safe(name).length()));
            Label av = new Label(initials.toUpperCase());
            av.setStyle("-fx-background-color:" + avatarBg + "; -fx-text-fill:white;" +
                    "-fx-font-size:32px; -fx-font-weight:bold;" +
                    "-fx-min-width:100px; -fx-min-height:100px;" +
                    "-fx-max-width:100px; -fx-max-height:100px;" +
                    "-fx-alignment:center; -fx-background-radius:50%;");
            StackPane sp = new StackPane(av); sp.setMinWidth(120);
            photoArea = sp;
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        nameLabel.setWrapText(true);

        Label idLabel = new Label("ID: " + (log.getPersonId() != null ? "#" + log.getPersonId() : "—"));
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

        VBox nameBox = new VBox(6, nameLabel, idLabel, typeBadge);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox personHeader = new HBox(16, photoArea, nameBox);
        personHeader.setAlignment(Pos.CENTER_LEFT);
        personHeader.setStyle("-fx-background-color:#f8fafc; -fx-padding:16;" +
                "-fx-background-radius:10; -fx-border-color:#e2e8f0;" +
                "-fx-border-radius:10; -fx-border-width:1;");

        String[][] rows = isGuard ? new String[][]{
                {"\uD83C\uDFE2", "Designation",  nvl(log.getDepartment())},
                {"\uD83D\uDD52", "Shift",         nvl(log.getExtraInfo())},
                {"\uD83D\uDCDE", "Phone",         nvl(log.getContactInfo())},
                {"\uD83C\uDD94", "Aadhar",        nvl(log.getAadharNumber())},
                {"\u2705",       "Result",        nvl(log.getResult())},
                {"\uD83D\uDD50", "Detected At",   fmtTime(log)},
        } : isPrison ? new String[][]{
                {"\uD83D\uDEA8", "Crime",         nvl(log.getExtraInfo())},
                {"\uD83C\uDFE0", "Cell No",       nvl(log.getDepartment())},
                {"\u26A0",       "Danger Level",  nvl(log.getContactInfo())},
                {"\uD83C\uDD94", "Aadhar",        nvl(log.getAadharNumber())},
                {"\u2705",       "Result",        nvl(log.getResult())},
                {"\uD83D\uDD50", "Detected At",   fmtTime(log)},
        } : new String[][]{
                {"\u2705",       "Result",      nvl(log.getResult())},
                {"\uD83D\uDD50", "Detected At", fmtTime(log)},
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
            value.setWrapText(true);
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
        content.setPrefWidth(480);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Recognition Detail — " + name);
        dlg.setHeaderText(null);
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setStyle("-fx-background-color:white; -fx-padding:20;");
        dlg.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String typeColor(String type) {
        switch (type != null ? type : "") {
            case "GUARD":    return "#3b82f6";
            case "PRISONER": return "#f59e0b";
            default:         return "#6b7280";
        }
    }

    private String fmtTime(RecognitionLog log) {
        return log.getDetectedAt() != null ? log.getDetectedAt().format(DISPLAY_FMT) : "N/A";
    }

    private String nvl(String s)  { return s != null && !s.isBlank() ? s : "—"; }
    private String safe(String s) { return s != null ? s : ""; }
}
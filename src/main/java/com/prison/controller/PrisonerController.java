package com.prison.controller;

import com.prison.dao.PrisonerDao;
import com.prison.model.Prisoner;
import com.prison.util.StyledCell;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;          // explicit — avoids lowagie wildcard clash
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrisonerController {

    /* ── Table ─────────────────────────────────────────────────────────────── */
    @FXML private TableView<Prisoner> prisonerTable;
    @FXML private TableColumn<Prisoner, Integer> idCol;
    @FXML private TableColumn<Prisoner, String>  nameCol, crimeCol, cellCol,
            yearsCol, statusCol, remainingCol, descCol;

    /* ── Header labels ─────────────────────────────────────────────────────── */
    @FXML private Label prisonerCount;
    @FXML private Label releasedCount;
    @FXML private Label statusInfoLabel;
    @FXML private Label searchResultLabel;

    /** Search field — javafx.scene.control.TextField (explicit import, no ambiguity) */
    @FXML private TextField searchField;

    private final PrisonerDao dao        = new PrisonerDao();
    private final PrisonerDao prisonerDao = new PrisonerDao();
    private Prisoner selectedPrisoner;

    private ObservableList<Prisoner> masterList  = FXCollections.observableArrayList();
    private FilteredList<Prisoner>   filteredList;

    // ══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        updateCounts();
        startAutoRefresh();
        setupTable();
        refreshTable();

        // Double-click → open full profile for editing
        prisonerTable.setRowFactory(tv -> {
            TableRow<Prisoner> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty())
                    openPrisonerProfile(row.getItem());
            });
            return row;
        });

        // Single-click → track for delete + update status label
        prisonerTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, p) -> {
                    selectedPrisoner = p;
                    if (p == null) {
                        statusInfoLabel.setText("No prisoner selected");
                        return;
                    }
                    String rd = p.getReleaseDate() != null ? "Release: " + p.getReleaseDate() : "";
                    statusInfoLabel.setText(
                            "Selected: " + p.getName()
                                    + "  |  Status: " + p.getStatus()
                                    + (rd.isEmpty() ? "" : "  |  " + rd));
                });

        // ── SEARCH: filter on every keystroke ────────────────────────────────
        // Matches ID (prefix), name (contains), or crime (contains) — case-insensitive
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            String query = (newText == null) ? "" : newText.trim().toLowerCase();
            filteredList.setPredicate(prisoner -> {
                if (query.isEmpty()) {
                    searchResultLabel.setText("");
                    return true;
                }
                boolean match =
                        String.valueOf(prisoner.getPrisonerId()).startsWith(query) ||
                                (prisoner.getName()  != null && prisoner.getName().toLowerCase().contains(query)) ||
                                (prisoner.getCrime() != null && prisoner.getCrime().toLowerCase().contains(query));
                return match;
            });
            // Update result count label
            long count = filteredList.size();
            searchResultLabel.setText(query.isEmpty() ? "" : count + " result" + (count == 1 ? "" : "s") + " found");
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COUNTS + AUTO-REFRESH
    // ══════════════════════════════════════════════════════════════════════
    private void updateCounts() {
        List<Prisoner> all = prisonerDao.findAll();
        long inCustody = all.stream().filter(p -> "IN_CUSTODY".equals(p.getStatus())).count();
        long released  = all.size() - inCustody;
        prisonerCount.setText(String.valueOf(inCustody));
        releasedCount.setText(String.valueOf(released));
    }

    private void startAutoRefresh() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> updateCounts())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  OPEN EMPTY PROFILE PAGE FOR NEW PRISONER
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void openNewPrisonerProfile() {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                            getClass().getResource("/fxml/prisoner_profile.fxml"));
            javafx.scene.Parent root = loader.load();

            PrisonerProfileController controller = loader.getController();
            controller.setNewPrisonerMode();
            controller.setOnSaveCallback(this::refreshTable);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add New Prisoner");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  OPEN EXISTING PRISONER PROFILE (double-click)
    // ══════════════════════════════════════════════════════════════════════
    private void openPrisonerProfile(Prisoner prisoner) {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                            getClass().getResource("/fxml/prisoner_profile.fxml"));
            javafx.scene.Parent root = loader.load();

            PrisonerProfileController controller = loader.getController();
            controller.setPrisoner(prisoner);
            controller.setOnSaveCallback(this::refreshTable);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Prisoner Profile — " + prisoner.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE SELECTED PRISONER
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void deletePrisoner() {
        if (selectedPrisoner == null) {
            showAlert("No Selection", "Please select a prisoner from the table first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Prisoner: " + selectedPrisoner.getName());
        confirm.setContentText("This action cannot be undone. Proceed?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                dao.delete(selectedPrisoner.getPrisonerId());
                selectedPrisoner = null;
                statusInfoLabel.setText("Prisoner record removed.");
                refreshTable();
                updateCounts();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRINT PRISONER TABLE — crimson themed PDF
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void printPrisonerTable() {
        List<Prisoner> prisoners = dao.findAll();
        if (prisoners == null || prisoners.isEmpty()) {
            showAlert("No Data", "There are no prisoner records in the database to print.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Prisoner Registry Report");
        fileChooser.setInitialFileName("PRISONER_REGISTRY_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(prisonerTable.getScene().getWindow());
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4.rotate(), 40, 40, 60, 55);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));

            Color crimson   = new Color(127, 29, 29);
            Color accentRed = new Color(185, 28, 28);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        cb.setLineWidth(2f);
                        cb.setColorStroke(crimson);
                        cb.rectangle(25, 25, doc.getPageSize().getWidth() - 50, doc.getPageSize().getHeight() - 50);
                        cb.stroke();
                        cb.setLineWidth(0.4f);
                        cb.setColorStroke(new Color(200, 150, 150));
                        cb.rectangle(30, 30, doc.getPageSize().getWidth() - 60, doc.getPageSize().getHeight() - 60);
                        cb.stroke();
                        cb.setLineWidth(0.8f);
                        cb.setColorStroke(new Color(200, 150, 150));
                        cb.moveTo(35, 42);
                        cb.lineTo(doc.getPageSize().getWidth() - 35, 42);
                        cb.stroke();
                        Font ff = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(140, 80, 80));
                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase("CONFIDENTIAL — DEPARTMENT OF CORRECTIONS", ff), 40, 32, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Prisoner Registry — Official Government Record", ff),
                                doc.getPageSize().getWidth() / 2, 32, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                                new Phrase("Page " + w.getPageNumber() + " | " +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), ff),
                                doc.getPageSize().getWidth() - 40, 32, 0);
                    } catch (Exception ignored) {}
                }
            });

            document.open();

            // Header
            PdfPTable ht = new PdfPTable(new float[]{1, 5, 1});
            ht.setWidthPercentage(100);
            ht.setSpacingAfter(18f);
            ht.addCell(logoCell());
            PdfPCell center = new PdfPCell();
            center.setBorder(PdfPCell.NO_BORDER);
            center.setHorizontalAlignment(Element.ALIGN_CENTER);
            center.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph dept = new Paragraph("DEPARTMENT OF CORRECTIONS & PRISON SECURITY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, crimson));
            dept.setAlignment(Element.ALIGN_CENTER); dept.setSpacingAfter(4f);
            center.addElement(dept);
            Paragraph titleP = new Paragraph("PRISONER REGISTRY — CLASSIFIED RECORD",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, crimson));
            titleP.setAlignment(Element.ALIGN_CENTER); titleP.setSpacingAfter(3f);
            center.addElement(titleP);
            Paragraph subP = new Paragraph("Security Classification: RESTRICTED — Authorised Personnel Only",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(120, 50, 50)));
            subP.setAlignment(Element.ALIGN_CENTER);
            center.addElement(subP);
            ht.addCell(center);
            ht.addCell(logoCell());
            document.add(ht);

            // Meta bar
            long inCustody = prisoners.stream().filter(p -> "IN_CUSTODY".equals(p.getStatus())).count();
            long released  = prisoners.size() - inCustody;
            PdfPTable metaBar = new PdfPTable(3);
            metaBar.setWidthPercentage(100); metaBar.setSpacingAfter(14f);
            Font mf = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);
            addMetaCell(metaBar, "TOTAL PRISONERS: " + prisoners.size(), mf, crimson);
            addMetaCell(metaBar, "IN CUSTODY: " + inCustody + "  |  RELEASED: " + released, mf, new Color(69, 10, 10));
            addMetaCell(metaBar, "GENERATED: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), mf, crimson);
            document.add(metaBar);

            // Data table
            PdfPTable table = new PdfPTable(new float[]{0.5f, 2f, 1.8f, 0.8f, 1.2f, 1.0f, 1.2f, 1.0f, 2f});
            table.setWidthPercentage(100); table.setSpacingAfter(20f); table.setHeaderRows(1);
            String[] headers = {"ID", "Name", "Crime", "Cell", "Sentence", "Status", "Release", "Risk", "Notes"};
            Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : headers) {
                PdfPCell hc = new PdfPCell(new Phrase(h, hf));
                hc.setBackgroundColor(accentRed); hc.setPadding(9);
                hc.setHorizontalAlignment(Element.ALIGN_CENTER); hc.setVerticalAlignment(Element.ALIGN_MIDDLE);
                hc.setBorderColor(new Color(180, 100, 100));
                table.addCell(hc);
            }
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Color rowAlt   = new Color(255, 245, 245);
            Color rowWhite = Color.WHITE;
            for (int i = 0; i < prisoners.size(); i++) {
                Prisoner p  = prisoners.get(i);
                Color rowBg = (i % 2 == 0) ? rowWhite : rowAlt;
                String liveStatus = p.getStatus() != null ? p.getStatus() : "—";
                addDataCell(table, String.valueOf(p.getPrisonerId()), dataFont, rowBg, Element.ALIGN_CENTER);
                addDataCell(table, nvl(p.getName()),   boldFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, nvl(p.getCrime()),  dataFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, nvl(p.getCellNo()), dataFont, rowBg, Element.ALIGN_CENTER);
                addDataCell(table, p.getSentenceYears() > 0 ? p.getSentenceYears() + " yrs" : "—", dataFont, rowBg, Element.ALIGN_CENTER);
                boolean custody = "IN_CUSTODY".equals(liveStatus);
                PdfPCell sc = new PdfPCell(new Phrase(liveStatus,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                                custody ? new Color(22, 101, 52) : new Color(153, 27, 27))));
                sc.setBackgroundColor(custody ? new Color(220, 252, 231) : new Color(254, 226, 226));
                sc.setPadding(8); sc.setHorizontalAlignment(Element.ALIGN_CENTER);
                sc.setVerticalAlignment(Element.ALIGN_MIDDLE); sc.setBorderColor(new Color(210, 170, 170));
                table.addCell(sc);
                LocalDate rd = p.getReleaseDate();
                boolean overdue = rd != null && !rd.isAfter(LocalDate.now());
                PdfPCell rdc = new PdfPCell(new Phrase(rd != null ? rd.toString() : "—",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                                overdue ? new Color(153, 27, 27) : new Color(22, 101, 52))));
                rdc.setBackgroundColor(overdue ? new Color(254, 226, 226) : new Color(220, 252, 231));
                rdc.setPadding(8); rdc.setHorizontalAlignment(Element.ALIGN_CENTER);
                rdc.setVerticalAlignment(Element.ALIGN_MIDDLE); rdc.setBorderColor(new Color(210, 170, 170));
                table.addCell(rdc);
                String dl = p.getDangerLevel() != null ? p.getDangerLevel() : "LOW";
                Color dlColor = switch (dl) {
                    case "MAXIMUM" -> new Color(127, 29, 29);
                    case "HIGH"    -> new Color(185, 28, 28);
                    case "MEDIUM"  -> new Color(180, 100, 0);
                    default        -> new Color(20, 100, 20);
                };
                PdfPCell dlCell = new PdfPCell(new Phrase(dl, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, dlColor)));
                dlCell.setBackgroundColor(rowBg); dlCell.setPadding(8);
                dlCell.setHorizontalAlignment(Element.ALIGN_CENTER); dlCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                dlCell.setBorderColor(new Color(210, 170, 170));
                table.addCell(dlCell);
                addDataCell(table, nvl(p.getDescription()), dataFont, rowBg, Element.ALIGN_LEFT);
            }
            document.add(table);

            // Summary
            long maxRisk  = prisoners.stream().filter(p -> "MAXIMUM".equals(p.getDangerLevel())).count();
            long highRisk = prisoners.stream().filter(p -> "HIGH".equals(p.getDangerLevel())).count();
            PdfPTable sumT = new PdfPTable(4);
            sumT.setWidthPercentage(65); sumT.setHorizontalAlignment(Element.ALIGN_LEFT); sumT.setSpacingAfter(15f);
            Font sl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, crimson);
            Font sv = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            addSummaryCell(sumT, "Total Prisoners", String.valueOf(prisoners.size()), sl, sv, new Color(255, 240, 240));
            addSummaryCell(sumT, "In Custody",      String.valueOf(inCustody),        sl, sv, new Color(254, 226, 226));
            addSummaryCell(sumT, "MAXIMUM Risk",    String.valueOf(maxRisk),          sl, sv, new Color(254, 202, 202));
            addSummaryCell(sumT, "HIGH Risk",       String.valueOf(highRisk),         sl, sv, new Color(255, 215, 215));
            document.add(sumT);

            // Verification strip
            PdfPTable vt = new PdfPTable(1);
            vt.setWidthPercentage(100);
            PdfPCell vc = new PdfPCell(new Phrase(
                    "CLASSIFIED DOCUMENT. Unauthorised access, reproduction or disclosure is a criminal offence. " +
                            "Report ID: RPT-" + System.currentTimeMillis() % 1000000,
                    FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(120, 50, 50))));
            vc.setBorder(PdfPCell.TOP); vc.setBorderColor(new Color(180, 100, 100));
            vc.setHorizontalAlignment(Element.ALIGN_CENTER); vc.setPadding(8);
            vc.setBackgroundColor(new Color(255, 245, 245));
            vt.addCell(vc);
            document.add(vt);

            document.close();
            new Alert(Alert.AlertType.INFORMATION, "Prisoner Registry Report exported successfully!").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage()).show();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLE SETUP
    // ══════════════════════════════════════════════════════════════════════
    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("prisonerId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        crimeCol.setCellValueFactory(new PropertyValueFactory<>("crime"));
        cellCol.setCellValueFactory(new PropertyValueFactory<>("cellNo"));

        yearsCol.setCellValueFactory(data -> {
            Prisoner p = data.getValue();
            if (p.getSentenceStartDate() == null || p.getReleaseDate() == null)
                return new SimpleStringProperty("—");
            long months = java.time.temporal.ChronoUnit.MONTHS.between(
                    p.getSentenceStartDate(), p.getReleaseDate());
            return new SimpleStringProperty(String.format("%.1f", months / 12.0));
        });
        yearsCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v + " yrs");
            }
        });

        statusCol.setCellValueFactory(data -> {
            Prisoner p  = data.getValue();
            LocalDate rd    = p.getReleaseDate();
            LocalDate today = LocalDate.now();
            if (rd != null) {
                if (!rd.isAfter(today) && !"RELEASED".equals(p.getStatus())) {
                    p.setStatus("RELEASED");
                    dao.updateStatus(p.getPrisonerId(), "RELEASED");
                } else if (rd.isAfter(today) && "RELEASED".equals(p.getStatus())) {
                    p.setStatus("IN_CUSTODY");
                    dao.updateStatus(p.getPrisonerId(), "IN_CUSTODY");
                }
            }
            return new SimpleStringProperty(p.getStatus());
        });
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("IN_CUSTODY".equals(s)
                        ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                        : "-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        });

        remainingCol.setCellValueFactory(data -> {
            Prisoner p = data.getValue();
            if (p.getReleaseDate() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(p.getReleaseDate().toString());
        });
        remainingCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null || "—".equals(date)) { setText(null); setStyle(""); return; }
                setText(date);
                boolean past = !LocalDate.parse(date).isAfter(LocalDate.now());
                setStyle(past ? "-fx-text-fill: red; -fx-font-weight: bold;"
                        : "-fx-text-fill: green; -fx-font-weight: bold;");
            }
        });

        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        idCol.setCellFactory(col    -> new StyledCell<>("prisoner-prisonerId"));
        nameCol.setCellFactory(col  -> new StyledCell<>("prisoner-name"));
        crimeCol.setCellFactory(col -> new StyledCell<>("prisoner-crime"));
        descCol.setCellFactory(col  -> new StyledCell<>("prisoner-description"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PDF HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private PdfPCell logoCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            com.lowagie.text.Image logo =
                    com.lowagie.text.Image.getInstance("src/main/resources/images/logo.jpeg");
            logo.scaleToFit(55, 55);
            cell.addElement(logo);
        } catch (Exception ignored) {}
        return cell;
    }

    private void addMetaCell(PdfPTable t, String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg); c.setBorder(PdfPCell.NO_BORDER);
        c.setPadding(6); c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void addDataCell(PdfPTable t, String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg); c.setPadding(8);
        c.setHorizontalAlignment(align); c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(new Color(210, 170, 170));
        t.addCell(c);
    }

    private void addSummaryCell(PdfPTable t, String label, String value, Font lf, Font vf, Color bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg); c.setPadding(10);
        c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setBorderColor(new Color(210, 170, 170));
        c.addElement(new Phrase(label, lf)); c.addElement(new Phrase(value, vf));
        t.addCell(c);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    // Rebuild master + filtered list; preserve active search query after add/delete
    private void refreshTable() {
        masterList   = FXCollections.observableArrayList(dao.findAll());
        filteredList = new FilteredList<>(masterList, p -> true);

        if (searchField != null) {
            String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            if (!query.isEmpty()) {
                filteredList.setPredicate(prisoner -> {
                    if (String.valueOf(prisoner.getPrisonerId()).startsWith(query)) return true;
                    if (prisoner.getName()  != null && prisoner.getName().toLowerCase().contains(query))  return true;
                    return prisoner.getCrime() != null && prisoner.getCrime().toLowerCase().contains(query);
                });
            }
        }

        prisonerTable.setItems(filteredList);
        updateCounts();
    }

    private String nvl(String s) {
        return s != null && !s.isEmpty() ? s : "";
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.model.Guard;
import com.prison.util.StyledCell;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GuardController {

    @FXML private TableView<Guard> guardTable;
    @FXML private TableColumn<Guard, Integer> idCol;
    @FXML private TableColumn<Guard, String> nameCol, designationCol, shiftCol, statusCol, joiningCol, descCol;

    @FXML private Label statusInfoLabel;

    private final GuardDao dao = new GuardDao();
    private Guard selectedGuard;

    @FXML
    public void initialize() {
        updateCounts();
        startAutoRefresh();
        setupTable();
        refreshTable();

        // Double-click → open full profile for editing
        guardTable.setRowFactory(tv -> {
            TableRow<Guard> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openGuardProfile(row.getItem());
                }
            });
            return row;
        });

        // Selection → track for delete + update status label
        guardTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, g) -> {
            selectedGuard = g;
            if (g == null) {
                statusInfoLabel.setText("No guard selected");
                return;
            }
            String liveStatus = calculateLiveStatus(g.getShift());
            statusInfoLabel.setText("Selected: " + g.getName() + "  |  Live Status: " + liveStatus);
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
            String hoursOnly = shift.replaceAll("[^0-9-]", "");
            String[] parts = hoursOnly.split("-");
            int startHour = Integer.parseInt(parts[0]);
            int endHour   = Integer.parseInt(parts[1]);
            int currentHour = LocalTime.now().getHour();
            boolean isActive;
            if (startHour < endHour) {
                isActive = (currentHour >= startHour && currentHour < endHour);
            } else {
                isActive = (currentHour >= startHour || currentHour < endHour);
            }
            return isActive ? "ACTIVE" : "INACTIVE";
        } catch (Exception e) {
            return "INACTIVE";
        }
    }

    @FXML
    private Label guardCount;

    private final GuardDao guardDao = new GuardDao();
    private void updateCounts() {


        guardCount.setText(
                "🧑‍✈️ " + guardDao.countGuards()
        );

    }
    private void startAutoRefresh() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> updateCounts())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
    /* ============================================================
       🆕 OPEN EMPTY PROFILE PAGE FOR NEW GUARD
       ============================================================ */
    @FXML
    public void openNewGuardProfile() {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/guard_profile.fxml"));
            javafx.scene.Parent root = loader.load();

            GuardProfileController controller = loader.getController();
            controller.setNewGuardMode(); // ← puts profile in INSERT mode
            controller.setOnSaveCallback(this::refreshTable);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add New Guard");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ============================================================
       OPEN EXISTING GUARD PROFILE (double-click)
       ============================================================ */
    private void openGuardProfile(Guard guard) {
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/guard_profile.fxml"));
            javafx.scene.Parent root = loader.load();

            GuardProfileController controller = loader.getController();
            controller.setGuard(guard);
            controller.setOnSaveCallback(this::refreshTable);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Guard Profile — " + guard.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ============================================================
       🗑️ DELETE SELECTED GUARD
       ============================================================ */
    @FXML
    public void deleteGuard() {
        if (selectedGuard == null) {
            showAlert("No Selection", "Please select a guard from the table first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Guard: " + selectedGuard.getName());
        confirm.setContentText("This action cannot be undone. Proceed?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                dao.delete(selectedGuard.getGuardId());
                selectedGuard = null;
                statusInfoLabel.setText("Guard removed.");
                refreshTable();
            }
        });
    }

    /* ============================================================
       🖨️ PRINT GUARD TABLE — full styled PDF of all guards
       ============================================================ */
    @FXML
    public void printGuardTable() {
        List<Guard> guards = dao.findAll();
        if (guards == null || guards.isEmpty()) {
            showAlert("No Data", "There are no guards in the database to print.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Guard Table Report");
        fileChooser.setInitialFileName("GUARD_TABLE_REPORT_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(guardTable.getScene().getWindow());

        if (file == null) return;

        try {
            // ── Document Setup ──────────────────────────────────────────
            Document document = new Document(PageSize.A4.rotate(), 40, 40, 60, 55);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));

            Color darkBlue   = new Color(0, 51, 102);
            Color accentBlue = new Color(94, 114, 228);

            // Page event: border + footer
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        cb.setLineWidth(2f);
                        cb.setColorStroke(darkBlue);
                        cb.rectangle(25, 25, doc.getPageSize().getWidth() - 50, doc.getPageSize().getHeight() - 50);
                        cb.stroke();

                        cb.setLineWidth(0.4f);
                        cb.setColorStroke(new Color(180, 180, 180));
                        cb.rectangle(30, 30, doc.getPageSize().getWidth() - 60, doc.getPageSize().getHeight() - 60);
                        cb.stroke();

                        // Footer divider
                        cb.setLineWidth(0.8f);
                        cb.setColorStroke(new Color(200, 200, 200));
                        cb.moveTo(35, 42);
                        cb.lineTo(doc.getPageSize().getWidth() - 35, 42);
                        cb.stroke();

                        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase("CONFIDENTIAL — DEPARTMENT OF CORRECTIONS", footerFont), 40, 32, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Guard Force Personnel Registry", footerFont),
                                doc.getPageSize().getWidth() / 2, 32, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                                new Phrase("Page " + w.getPageNumber() + " | " +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")),
                                        footerFont),
                                doc.getPageSize().getWidth() - 40, 32, 0);
                    } catch (Exception ignored) {}
                }
            });

            document.open();

            // ── Header ──────────────────────────────────────────────────
            PdfPTable headerTable = new PdfPTable(new float[]{1, 5, 1});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(18f);

            // Left logo
            PdfPCell leftLogo = new PdfPCell();
            leftLogo.setBorder(PdfPCell.NO_BORDER);
            leftLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                Image logo = Image.getInstance("src/main/resources/images/logo.jpeg");
                logo.scaleToFit(55, 55);
                leftLogo.addElement(logo);
            } catch (Exception ignored) {}
            headerTable.addCell(leftLogo);

            // Center text
            PdfPCell center = new PdfPCell();
            center.setBorder(PdfPCell.NO_BORDER);
            center.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph dept = new Paragraph("DEPARTMENT OF CORRECTIONS & PRISON SECURITY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, darkBlue));
            dept.setAlignment(Element.ALIGN_CENTER);
            dept.setSpacingAfter(4f);
            center.addElement(dept);
            Paragraph titleP = new Paragraph("GUARD FORCE PERSONNEL REGISTRY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, darkBlue));
            titleP.setAlignment(Element.ALIGN_CENTER);
            titleP.setSpacingAfter(3f);
            center.addElement(titleP);
            Paragraph subP = new Paragraph("Confidential Personnel Report — Security Clearance: Restricted",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
            subP.setAlignment(Element.ALIGN_CENTER);
            center.addElement(subP);
            headerTable.addCell(center);

            // Right logo
            PdfPCell rightLogo = new PdfPCell();
            rightLogo.setBorder(PdfPCell.NO_BORDER);
            rightLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            rightLogo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                Image logo2 = Image.getInstance("src/main/resources/images/logo.jpeg");
                logo2.scaleToFit(55, 55);
                rightLogo.addElement(logo2);
            } catch (Exception ignored) {}
            headerTable.addCell(rightLogo);

            document.add(headerTable);

            // ── Meta bar ────────────────────────────────────────────────
            PdfPTable metaBar = new PdfPTable(3);
            metaBar.setWidthPercentage(100);
            metaBar.setSpacingAfter(14f);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);
            addMetaCell(metaBar, "TOTAL PERSONNEL: " + guards.size(), metaFont, darkBlue);
            addMetaCell(metaBar, "ACTIVE: " + guards.stream().filter(g -> "ACTIVE".equals(g.getStatus())).count() +
                    "  |  INACTIVE: " + guards.stream().filter(g -> !"ACTIVE".equals(g.getStatus())).count(), metaFont, darkBlue);
            addMetaCell(metaBar, "GENERATED: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), metaFont, darkBlue);
            document.add(metaBar);

            // ── Data Table ───────────────────────────────────────────────
            PdfPTable table = new PdfPTable(new float[]{0.5f, 2.2f, 1.6f, 1.8f, 1.0f, 1.4f, 2.5f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(20f);
            table.setHeaderRows(1);

            // Header row
            String[] headers = {"ID", "Guard Name", "Designation", "Shift", "Status", "Joined", "Notes"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : headers) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
                hCell.setBackgroundColor(accentBlue);
                hCell.setPadding(9);
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                hCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                hCell.setBorderColor(new Color(180, 180, 180));
                table.addCell(hCell);
            }

            // Data rows
            Font dataFont  = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font boldFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Color rowAlt   = new Color(245, 247, 255);
            Color rowWhite = Color.WHITE;
            Color activeGreen = new Color(220, 252, 231);
            Color inactiveRed = new Color(254, 226, 226);

            for (int i = 0; i < guards.size(); i++) {
                Guard g = guards.get(i);
                Color rowBg = (i % 2 == 0) ? rowWhite : rowAlt;
                String liveStatus = calculateLiveStatus(g.getShift());

                addDataCell(table, String.valueOf(g.getGuardId()), dataFont, rowBg, Element.ALIGN_CENTER);
                addDataCell(table, g.getName() != null ? g.getName() : "—", boldFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, g.getDesignation() != null ? g.getDesignation() : "—", dataFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, g.getShift() != null ? g.getShift() : "—", dataFont, rowBg, Element.ALIGN_CENTER);

                // Status cell with color
                PdfPCell statusCell = new PdfPCell(new Phrase(liveStatus,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                                "ACTIVE".equals(liveStatus) ? new Color(22, 101, 52) : new Color(153, 27, 27))));
                statusCell.setBackgroundColor("ACTIVE".equals(liveStatus) ? activeGreen : inactiveRed);
                statusCell.setPadding(8);
                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                statusCell.setBorderColor(new Color(200, 200, 200));
                table.addCell(statusCell);

                addDataCell(table,
                        g.getJoiningDate() != null ? g.getJoiningDate().toString() : "—",
                        dataFont, rowBg, Element.ALIGN_CENTER);
                addDataCell(table,
                        g.getDescription() != null ? g.getDescription() : "—",
                        dataFont, rowBg, Element.ALIGN_LEFT);
            }

            document.add(table);

            // ── Summary section ─────────────────────────────────────────
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(60);
            summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            summaryTable.setSpacingAfter(15f);

            Font sumLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, darkBlue);
            Font sumVal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

            long activeCount   = guards.stream().filter(g -> "ACTIVE".equals(calculateLiveStatus(g.getShift()))).count();
            long inactiveCount = guards.size() - activeCount;

            addSummaryCell(summaryTable, "Total Guards",      String.valueOf(guards.size()), sumLabel, sumVal, new Color(240, 245, 250));
            addSummaryCell(summaryTable, "Active Now",        String.valueOf(activeCount),   sumLabel, sumVal, new Color(220, 252, 231));
            addSummaryCell(summaryTable, "Off Duty / Leave",  String.valueOf(inactiveCount), sumLabel, sumVal, new Color(254, 226, 226));
            addSummaryCell(summaryTable, "Report Generated",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),        sumLabel, sumVal, new Color(255, 248, 225));

            document.add(summaryTable);

            // Verification strip
            PdfPTable verifyTable = new PdfPTable(1);
            verifyTable.setWidthPercentage(100);
            Font verifyFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY);
            PdfPCell verifyCell = new PdfPCell(new Phrase(
                    "This is an official government document. Unauthorized reproduction or disclosure is prohibited. " +
                            "Report ID: RPT-" + System.currentTimeMillis() % 1000000, verifyFont));
            verifyCell.setBorder(PdfPCell.TOP);
            verifyCell.setBorderColor(Color.LIGHT_GRAY);
            verifyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            verifyCell.setPadding(8);
            verifyCell.setBackgroundColor(new Color(248, 248, 248));
            verifyTable.addCell(verifyCell);
            document.add(verifyTable);

            document.close();

            new Alert(Alert.AlertType.INFORMATION, "Guard Table Report exported successfully!").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage()).show();
        }
    }

    // ── PDF helper methods ───────────────────────────────────────────────────
    private void addMetaCell(PdfPTable t, String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setBorder(PdfPCell.NO_BORDER);
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void addDataCell(PdfPTable t, String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setPadding(8);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(new Color(210, 210, 210));
        t.addCell(c);
    }

    private void addSummaryCell(PdfPTable t, String label, String value, Font lf, Font vf, Color bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setPadding(10);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorderColor(new Color(210, 210, 210));
        c.addElement(new Phrase(label, lf));
        c.addElement(new Phrase(value,  vf));
        t.addCell(c);
    }

    // ── Table setup ─────────────────────────────────────────────────────────
    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("guardId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        designationCol.setCellValueFactory(new PropertyValueFactory<>("designation"));
        shiftCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getShift()));

        statusCol.setCellValueFactory(data -> {
            Guard g = data.getValue();
            String liveStatus = calculateLiveStatus(g.getShift());
            if (!liveStatus.equals(g.getStatus())) {
                g.setStatus(liveStatus);
                dao.update(g);
            }
            return new SimpleStringProperty(liveStatus);
        });

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                setStyle("ACTIVE".equals(status)
                        ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                        : "-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        });

        joiningCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getJoiningDate() != null ? data.getValue().getJoiningDate().toString() : "N/A"
        ));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        idCol.setCellFactory(col -> new StyledCell<>("prisoner-id"));
        nameCol.setCellFactory(col -> new StyledCell<>("prisoner-name"));
        descCol.setCellFactory(col -> new StyledCell<>("guard-description"));
        joiningCol.setCellFactory(col -> new StyledCell<>("prisoner-name"));
    }

    private void refreshTable() {
        guardTable.setItems(FXCollections.observableArrayList(dao.findAll()));
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
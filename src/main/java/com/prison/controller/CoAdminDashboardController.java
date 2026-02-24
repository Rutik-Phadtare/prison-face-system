package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.dao.PrisonerDao;
import com.prison.dao.RecognitionLogDao;
import com.prison.dao.UserDao;
import com.prison.model.RecognitionLog;
import com.prison.model.User;
import com.prison.session.UserSession;
import com.prison.util.DatabaseUtil;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CoAdminDashboardController {

    // ── Stat labels ───────────────────────────────────────────────────────────
    @FXML private Label clockLbl;
    @FXML private Label greetingLbl;
    @FXML private Label usernameLbl;
    @FXML private Label prisonerCountLbl;
    @FXML private Label guardCountLbl;
    @FXML private Label todayScansLbl;
    @FXML private Label highDangerLbl;
    @FXML private Label unknownLbl;

    // ── Recent scans table ────────────────────────────────────────────────────
    @FXML private TableView<RecognitionLog>           recentTable;
    @FXML private TableColumn<RecognitionLog, Object> rtTimeCol;
    @FXML private TableColumn<RecognitionLog, String> rtNameCol;
    @FXML private TableColumn<RecognitionLog, String> rtTypeCol;
    @FXML private TableColumn<RecognitionLog, String> rtResultCol;

    // ── High-danger table ─────────────────────────────────────────────────────
    @FXML private TableView<DangerRow>           dangerTable;
    @FXML private TableColumn<DangerRow, String> dtName;
    @FXML private TableColumn<DangerRow, String> dtCrime;
    @FXML private TableColumn<DangerRow, String> dtCell;
    @FXML private TableColumn<DangerRow, String> dtLevel;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
    private static final Color C_DARK  = new Color(10, 31, 92);
    private static final Color C_ALT   = new Color(232, 236, 243);
    private static final Color C_GREEN = new Color(198, 239, 206);
    private static final Color C_RED   = new Color(255, 205, 210);
    private static final Color C_AMBER = new Color(255, 245, 200);

    // ── Simple read-model for the danger table ────────────────────────────────
    public static class DangerRow {
        public final String name, crime, cell, level;
        public DangerRow(String n, String cr, String ce, String l) { name=n; crime=cr; cell=ce; level=l; }
        public String getName()  { return name; }
        public String getCrime() { return crime; }
        public String getCell()  { return cell; }
        public String getLevel() { return level; }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        startClock();
        loadUserGreeting();
        setupRecentTable();
        setupDangerTable();
        loadAll();
        startAutoRefresh();
    }

    // ── Clock ─────────────────────────────────────────────────────────────────
    private void startClock() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLbl.setText(LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss  |  dd MMM yyyy")))));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    // ── Greeting ──────────────────────────────────────────────────────────────
    private void loadUserGreeting() {
        User u = UserSession.getUser();
        String name = u != null ? (u.getDisplayName() != null ? u.getDisplayName() : u.getUsername()) : "Officer";
        usernameLbl.setText(name);
        int hour = LocalDateTime.now().getHour();
        greetingLbl.setText(hour < 12 ? "Good Morning" : hour < 17 ? "Good Afternoon" : "Good Evening");
    }

    // ── Auto-refresh every 30 s ───────────────────────────────────────────────
    private void startAutoRefresh() {
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadAll()));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    private void loadAll() {
        loadStats();
        loadRecentScans();
        loadDangerPrisoners();
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private void loadStats() {
        try (Connection c = DatabaseUtil.getConnection()) {
            prisonerCountLbl.setText(String.valueOf(
                    queryInt(c, "SELECT COUNT(*) FROM prisoners WHERE status='IN_CUSTODY'")));
            guardCountLbl.setText(String.valueOf(
                    queryInt(c, "SELECT COUNT(*) FROM guards WHERE status='ACTIVE'")));
            highDangerLbl.setText(String.valueOf(
                    queryInt(c, "SELECT COUNT(*) FROM prisoners WHERE danger_level='HIGH' AND status='IN_CUSTODY'")));
            String today = LocalDate.now().toString();
            todayScansLbl.setText(String.valueOf(queryInt(c,
                    "SELECT COUNT(*) FROM recognition_logs WHERE DATE(detected_at) = '" + today + "'")));
            unknownLbl.setText(String.valueOf(queryInt(c,
                    "SELECT COUNT(*) FROM recognition_logs WHERE person_type='UNKNOWN' AND DATE(detected_at) = '" + today + "'")));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int queryInt(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Recent recognition scans (last 15 today) ──────────────────────────────
    private void setupRecentTable() {
        rtTimeCol.setCellValueFactory(new PropertyValueFactory<>("detectedAt"));
        rtTimeCol.setCellFactory(col -> new TableCell<RecognitionLog, Object>() {
            @Override protected void updateItem(Object v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v instanceof LocalDateTime ? ((LocalDateTime)v).format(FMT) : v.toString());
                setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
            }
        });
        rtNameCol.setCellValueFactory(new PropertyValueFactory<>("personName"));
        rtTypeCol.setCellValueFactory(new PropertyValueFactory<>("personType"));
        rtTypeCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty||t==null){setGraphic(null);return;}
                String s = "GUARD".equals(t) ? "-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;"
                        :"PRISONER".equals(t)? "-fx-background-color:#fef3c7;-fx-text-fill:#b45309;"
                        :"-fx-background-color:#f3f4f6;-fx-text-fill:#374151;";
                Label b=new Label(t); b.setStyle(s+"-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:2 8 2 8;-fx-background-radius:20;");
                setGraphic(b);setText(null);
            }
        });
        rtResultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        rtResultCol.setCellFactory(col -> new TableCell<RecognitionLog, String>() {
            @Override protected void updateItem(String r, boolean empty) {
                super.updateItem(r, empty);
                if (empty||r==null){setGraphic(null);return;}
                String s="RECOGNIZED".equals(r)?"-fx-background-color:#dcfce7;-fx-text-fill:#166534;"
                        :"-fx-background-color:#fee2e2;-fx-text-fill:#991b1b;";
                Label b=new Label(r); b.setStyle(s+"-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:2 8 2 8;-fx-background-radius:20;");
                setGraphic(b);setText(null);
            }
        });
    }

    private void loadRecentScans() {
        try {
            List<RecognitionLog> all = new RecognitionLogDao().findAll();
            ObservableList<RecognitionLog> top = FXCollections.observableArrayList(
                    all.stream().limit(15).collect(Collectors.toList()));
            recentTable.setItems(top);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── High-danger prisoners ────────────────────────────────────────────────
    private void setupDangerTable() {
        dtName.setCellValueFactory(new PropertyValueFactory<>("name"));
        dtCrime.setCellValueFactory(new PropertyValueFactory<>("crime"));
        dtCell.setCellValueFactory(new PropertyValueFactory<>("cell"));
        dtLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        dtLevel.setCellFactory(col -> new TableCell<DangerRow, String>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty||v==null){setGraphic(null);return;}
                Label b=new Label("⚠ "+v);
                b.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#991b1b;-fx-font-weight:bold;" +
                        "-fx-font-size:10px;-fx-padding:2 8 2 8;-fx-background-radius:20;");
                setGraphic(b);setText(null);
            }
        });
    }

    private void loadDangerPrisoners() {
        ObservableList<DangerRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT name, crime, cell_no, danger_level FROM prisoners " +
                "WHERE danger_level='HIGH' AND status='IN_CUSTODY' ORDER BY name";
        try (Connection c = DatabaseUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new DangerRow(rs.getString("name"), rs.getString("crime"),
                        rs.getString("cell_no"), rs.getString("danger_level")));
        } catch (Exception e) { e.printStackTrace(); }
        dangerTable.setItems(rows);
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML public void openPrisoners() {
        openWindow("/fxml/prisoner_management.fxml", "Prisoner Management");
    }

    @FXML public void openRecognitionLogs() {
        openWindow("/fxml/recognition_logs.fxml", "Recognition Logs");
    }

    private void openWindow(String path, String title) {
        try {
            Stage s = new Stage();
            FXMLLoader l = new FXMLLoader(getClass().getResource(path));
            Scene sc = new Scene(l.load());
            sc.getStylesheets().add(getClass().getResource("/css/modern.css").toExternalForm());
            s.setScene(sc); s.setTitle(title); s.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Export today's log PDF ────────────────────────────────────────────────
    @FXML
    public void exportTodayLog() {
        List<RecognitionLog> all = new RecognitionLogDao().findAll();
        LocalDate today = LocalDate.now();
        List<RecognitionLog> records = all.stream()
                .filter(l -> l.getDetectedAt() != null && l.getDetectedAt().toLocalDate().equals(today))
                .collect(Collectors.toList());

        if (records.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No recognition events for today.").showAndWait();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Today's Log");
        fc.setInitialFileName("TODAY_LOG_" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(recentTable.getScene().getWindow());
        if (file == null) return;

        try {
            Document doc = new Document(PageSize.A4.rotate(), 40, 40, 60, 55);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, Document d) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        cb.setLineWidth(2.5f); cb.setColorStroke(C_DARK);
                        cb.rectangle(22, 22, d.getPageSize().getWidth()-44, d.getPageSize().getHeight()-44);
                        cb.stroke();
                        Font ff = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                                new Phrase("Page " + w.getPageNumber(), ff), d.getPageSize().getWidth()-40, 33, 0);
                    } catch (Exception ignored) {}
                }
            });
            doc.open();

            Paragraph title = new Paragraph(
                    "DAILY RECOGNITION LOG — " + today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, C_DARK));
            title.setAlignment(Element.ALIGN_CENTER); title.setSpacingAfter(18f);
            doc.add(title);

            PdfPTable table = new PdfPTable(new float[]{0.5f, 2.5f, 2.5f, 1.2f, 1.5f});
            table.setWidthPercentage(100); table.setHeaderRows(1);
            Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : new String[]{"#","Time","Name","Type","Result"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, hf));
                c.setBackgroundColor(C_DARK); c.setPadding(9); c.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c);
            }
            Font df = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            for (int i = 0; i < records.size(); i++) {
                RecognitionLog l = records.get(i);
                Color bg = i % 2 == 0 ? Color.WHITE : C_ALT;
                Color resBg = "RECOGNIZED".equals(l.getResult()) ? C_GREEN
                        : "FAILED".equals(l.getResult()) ? C_RED : C_AMBER;
                pdfCell(table, String.valueOf(i+1), df, bg, Element.ALIGN_CENTER);
                pdfCell(table, l.getDetectedAt() != null ? l.getDetectedAt().format(FMT) : "—", df, bg, Element.ALIGN_LEFT);
                pdfCell(table, l.getPersonName() != null ? l.getPersonName() : "—", df, bg, Element.ALIGN_LEFT);
                pdfCell(table, l.getPersonType() != null ? l.getPersonType() : "—", df, bg, Element.ALIGN_CENTER);
                PdfPCell rc = new PdfPCell(new Phrase(l.getResult() != null ? l.getResult() : "—",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, C_DARK)));
                rc.setBackgroundColor(resBg); rc.setPadding(8); rc.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(rc);
            }
            doc.add(table);
            doc.close();
            new Alert(Alert.AlertType.INFORMATION, "Today's log exported!").show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
        }
    }

    private void pdfCell(PdfPTable t, String txt, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(txt == null ? "—" : txt, f));
        c.setBackgroundColor(bg); c.setPadding(8);
        c.setHorizontalAlignment(align); c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(new Color(190, 200, 215));
        t.addCell(c);
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    public void logout(ActionEvent event) {
        if (UserSession.getLoginLogId() > 0)
            new UserDao().recordLogout(UserSession.getLoginLogId());
        UserSession.clear();
        try {
            Stage cur = (Stage) ((Node) event.getSource()).getScene().getWindow();
            cur.close();
            Stage ls = new Stage();
            FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene sc = new Scene(l.load());
            sc.getStylesheets().add(getClass().getResource("/css/modern.css").toExternalForm());
            ls.setScene(sc); ls.setTitle("Login"); ls.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
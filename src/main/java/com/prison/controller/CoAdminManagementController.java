package com.prison.controller;

import com.prison.dao.UserDao;
import com.prison.dao.UserDao.LoginLogRow;
import com.prison.model.User;
import com.prison.session.UserSession;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class CoAdminManagementController {

    // ── FXML: stat labels ─────────────────────────────────────────────────────
    @FXML private Label totalLbl;
    @FXML private Label activeLbl;
    @FXML private Label inactiveLbl;
    @FXML private Label totalSessionsLbl;

    // ── FXML: accounts table ──────────────────────────────────────────────────
    @FXML private TableView<User>                  accountsTable;
    @FXML private TableColumn<User, Void>          colRow;
    @FXML private TableColumn<User, String>        colDisplay;
    @FXML private TableColumn<User, String>        colUsername;
    @FXML private TableColumn<User, String>        colCreatedBy;
    @FXML private TableColumn<User, Boolean>       colStatus;
    @FXML private TableColumn<User, Void>          colActions;

    // ── FXML: form (right panel) ──────────────────────────────────────────────
    @FXML private Label         formTitle;
    @FXML private TextField     fldDisplayName;
    @FXML private TextField     fldUsername;
    @FXML private PasswordField fldPassword;
    @FXML private PasswordField fldConfirm;
    @FXML private Label         lblStrength;
    @FXML private ProgressBar   barStrength;
    @FXML private Label         lblError;
    @FXML private Button        btnSave;

    // ── FXML: login log table ─────────────────────────────────────────────────
    @FXML private TableView<LoginLogRow>           logTable;
    @FXML private TableColumn<LoginLogRow, Void>   lColRow;
    @FXML private TableColumn<LoginLogRow, String> lColUser;
    @FXML private TableColumn<LoginLogRow, String> lColDisplay;
    @FXML private TableColumn<LoginLogRow, String> lColLogin;
    @FXML private TableColumn<LoginLogRow, String> lColLogout;
    @FXML private TableColumn<LoginLogRow, String> lColDuration;
    @FXML private TableColumn<LoginLogRow, String> lColStatus;
    @FXML private TextField logSearchField;
    @FXML private Label     logCountLbl;

    // ── state ─────────────────────────────────────────────────────────────────
    private final UserDao userDao = new UserDao();
    private ObservableList<User>         masterAccounts;
    private ObservableList<LoginLogRow>  masterLogs;
    private FilteredList<LoginLogRow>    filteredLogs;
    private User editTarget = null;   // non-null means "change password" mode

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    // ── Password rules ────────────────────────────────────────────────────────
    private static final int     PW_MIN      = 8;
    private static final Pattern HAS_UPPER   = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWER   = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT   = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"|,.<>/?].*");
    private static final Pattern UNAME_OK    = Pattern.compile("^[a-zA-Z0-9_]{4,30}$");

    // ── PDF theme (navy/gold — matches project) ───────────────────────────────
    private static final Color C_DARK   = new Color(10, 31, 92);
    private static final Color C_GOLD   = new Color(180, 140,  0);
    private static final Color C_ALT    = new Color(232, 236, 243);
    private static final Color C_GREEN  = new Color(198, 239, 206);
    private static final Color C_AMBER  = new Color(255, 245, 200);
    private static final Color C_LGRAY  = new Color(220, 220, 220);

    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        setupAccountsTable();
        setupLogTable();
        setupFormListeners();
        if (logSearchField != null)
            logSearchField.textProperty().addListener((o, ov, nv) -> applyLogFilter());
        loadAccounts();
        loadLogs();
    }

    // ── Accounts table ────────────────────────────────────────────────────────
    private void setupAccountsTable() {
        colRow.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-alignment: center; -fx-text-fill: #888;");
            }
        });

        colDisplay.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDisplayName() != null ? d.getValue().getDisplayName() : d.getValue().getUsername()));

        colUsername.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUsername()));

        colCreatedBy.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCreatedBy() != null ? d.getValue().getCreatedBy() : "—"));

        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleBooleanProperty(d.getValue().isActive()).asObject());
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) { setGraphic(null); return; }
                Label b = new Label(active ? "ACTIVE" : "INACTIVE");
                b.setStyle(active
                        ? "-fx-background-color:#dcfce7; -fx-text-fill:#166534; -fx-font-weight:bold;" +
                        "-fx-font-size:11px; -fx-padding:2 10 2 10; -fx-background-radius:20;"
                        : "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; -fx-font-weight:bold;" +
                        "-fx-font-size:11px; -fx-padding:2 10 2 10; -fx-background-radius:20;");
                setGraphic(b); setText(null);
            }
        });

        colActions.setCellFactory(c -> new TableCell<>() {
            private final Button pwBtn  = makeBtn("Change PW", "#0a1f5c");
            private final Button togBtn = makeBtn("Toggle",    "#b45309");
            private final Button delBtn = makeBtn("Delete",    "#c0392b");
            {
                pwBtn.setOnAction(e  -> loadChangePwMode(getTableView().getItems().get(getIndex())));
                togBtn.setOnAction(e -> { User u = getTableView().getItems().get(getIndex());
                    userDao.setActive(u.getUserId(), !u.isActive()); loadAccounts(); });
                delBtn.setOnAction(e -> { User u = getTableView().getItems().get(getIndex());
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete account '" + u.getUsername() + "'? This cannot be undone.");
                    a.showAndWait().filter(r -> r == ButtonType.OK)
                            .ifPresent(r -> { userDao.deleteCoAdmin(u.getUserId()); loadAccounts(); }); });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, pwBtn, togBtn, delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    private Button makeBtn(String text, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:white; -fx-font-size:10px;" +
                "-fx-background-radius:5; -fx-padding:3 9 3 9; -fx-cursor:hand;");
        return b;
    }

    private void loadAccounts() {
        List<User> all = userDao.findAllCoAdmins();
        masterAccounts = FXCollections.observableArrayList(all);
        accountsTable.setItems(masterAccounts);
        long active = all.stream().filter(User::isActive).count();
        totalLbl.setText(String.valueOf(all.size()));
        activeLbl.setText(String.valueOf(active));
        inactiveLbl.setText(String.valueOf(all.size() - active));
    }

    // ── Form ──────────────────────────────────────────────────────────────────
    private void setupFormListeners() {
        fldPassword.textProperty().addListener((o, ov, nv) -> updateStrength(nv));
        fldConfirm.textProperty().addListener((o, ov, nv) -> {
            if (!nv.isBlank() && !nv.equals(fldPassword.getText()))
                fldConfirm.setStyle("-fx-border-color:#c0392b; -fx-border-width:2;");
            else fldConfirm.setStyle("");
        });
    }

    private void updateStrength(String pw) {
        int score = 0;
        if (pw.length() >= PW_MIN)          score++;
        if (pw.length() >= 12)              score++;
        if (HAS_UPPER.matcher(pw).matches()) score++;
        if (HAS_LOWER.matcher(pw).matches()) score++;
        if (HAS_DIGIT.matcher(pw).matches()) score++;
        if (HAS_SPECIAL.matcher(pw).matches()) score++;
        double pct = score / 6.0;
        String label, color;
        if      (pct <= 0.2) { label = "Very Weak";  color = "#c0392b"; }
        else if (pct <= 0.4) { label = "Weak";        color = "#e67e22"; }
        else if (pct <= 0.6) { label = "Fair";        color = "#f1c40f"; }
        else if (pct <= 0.8) { label = "Strong";      color = "#27ae60"; }
        else                 { label = "Very Strong"; color = "#1a6b3c"; }
        lblStrength.setText(label);
        lblStrength.setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold; -fx-font-size:11px;");
        barStrength.setProgress(pct);
        barStrength.setStyle("-fx-accent:" + color + ";");
    }

    private void loadChangePwMode(User u) {
        editTarget = u;
        fldDisplayName.setText(u.getDisplayName() != null ? u.getDisplayName() : u.getUsername());
        fldDisplayName.setDisable(true);
        fldUsername.setText(u.getUsername());
        fldUsername.setDisable(true);
        fldPassword.clear(); fldConfirm.clear();
        formTitle.setText("🔑  Change Password");
        btnSave.setText("Update Password");
        clearError();
    }

    @FXML public void onFormClear() {
        editTarget = null;
        fldDisplayName.setDisable(false);
        fldUsername.setDisable(false);
        fldDisplayName.clear(); fldUsername.clear();
        fldPassword.clear(); fldConfirm.clear();
        if (lblStrength != null) lblStrength.setText("");
        if (barStrength  != null) barStrength.setProgress(0);
        formTitle.setText("➕  New Co-Admin");
        btnSave.setText("Create Account");
        clearError();
    }

    @FXML public void onFormSave() {
        clearError();
        String pw      = fldPassword.getText();
        String confirm = fldConfirm.getText();

        // ── Password validation ───────────────────────────────────────────────
        if (pw.isBlank())                         { showError("Password is required."); return; }
        if (pw.length() < PW_MIN)                 { showError("Password must be at least " + PW_MIN + " characters."); return; }
        if (!HAS_UPPER.matcher(pw).matches())     { showError("Password must contain at least one uppercase letter (A–Z)."); return; }
        if (!HAS_LOWER.matcher(pw).matches())     { showError("Password must contain at least one lowercase letter (a–z)."); return; }
        if (!HAS_DIGIT.matcher(pw).matches())     { showError("Password must contain at least one digit (0–9)."); return; }
        if (!HAS_SPECIAL.matcher(pw).matches())   { showError("Password must contain at least one special character (!@#$%...)."); return; }
        if (!pw.equals(confirm))                   { showError("Passwords do not match."); return; }

        if (editTarget != null) {
            // Change password
            boolean ok = userDao.changePassword(editTarget.getUserId(), pw);
            if (ok) { info("Password updated for '" + editTarget.getUsername() + "'."); onFormClear(); }
            else      showError("Failed to update password.");
        } else {
            // Create new account
            String display = fldDisplayName.getText().trim();
            String uname   = fldUsername.getText().trim();
            if (display.isBlank())                      { showError("Display name is required."); return; }
            if (uname.isBlank())                        { showError("Username is required."); return; }
            if (!UNAME_OK.matcher(uname).matches())     { showError("Username: 4–30 chars, letters/digits/underscore only."); return; }
            if (userDao.usernameExists(uname))           { showError("Username '" + uname + "' is already taken."); return; }

            String by = UserSession.getUser() != null ? UserSession.getUser().getUsername() : "ADMIN";
            boolean ok = userDao.createCoAdmin(uname, pw, display, by);
            if (ok) { info("Co-Admin '" + uname + "' created."); onFormClear(); loadAccounts(); }
            else      showError("Failed to create account. Try a different username.");
        }
    }

    // ── Login log table ───────────────────────────────────────────────────────
    private void setupLogTable() {
        lColRow.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-alignment:center; -fx-text-fill:#888;");
            }
        });
        lColUser.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().username));
        lColDisplay.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().displayName != null ? d.getValue().displayName : "—"));
        lColLogin.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(fmtTs(d.getValue().loginAt)));
        lColLogout.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(fmtTs(d.getValue().logoutAt)));
        lColDuration.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(fmtMins(d.getValue().sessionMins)));
        lColStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().status));
        lColStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                String style = "ACTIVE".equals(s)
                        ? "-fx-background-color:#dcfce7; -fx-text-fill:#166534;"
                        : "-fx-background-color:#e8ecf3; -fx-text-fill:#374151;";
                Label b = new Label(s.replace("_", " "));
                b.setStyle(style + "-fx-font-size:10px; -fx-font-weight:bold;" +
                        "-fx-padding:2 8 2 8; -fx-background-radius:20;");
                setGraphic(b); setText(null);
            }
        });
    }

    private void loadLogs() {
        List<LoginLogRow> all = userDao.findAllLoginLogs();
        masterLogs   = FXCollections.observableArrayList(all);
        filteredLogs = new FilteredList<>(masterLogs, p -> true);
        logTable.setItems(filteredLogs);
        totalSessionsLbl.setText(String.valueOf(all.size()));
        if (logCountLbl != null) logCountLbl.setText(all.size() + " records");
    }

    private void applyLogFilter() {
        if (filteredLogs == null) return;
        String q = logSearchField.getText();
        filteredLogs.setPredicate(l -> {
            if (q == null || q.isBlank()) return true;
            String lower = q.toLowerCase();
            return (l.username    != null && l.username.toLowerCase().contains(lower))
                    || (l.displayName != null && l.displayName.toLowerCase().contains(lower));
        });
        logCountLbl.setText(filteredLogs.size() + " records");
    }

    @FXML public void onRefreshAccounts() { loadAccounts(); }
    @FXML public void onRefreshLogs()     { loadLogs(); }

    // ── PDF Export ────────────────────────────────────────────────────────────
    @FXML
    public void onExportPdf() {
        List<LoginLogRow> records = new java.util.ArrayList<>(filteredLogs);
        if (records.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No records to export.").showAndWait();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Login Activity Report");
        fc.setInitialFileName("CO_ADMIN_LOGIN_LOG_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(logTable.getScene().getWindow());
        if (file == null) return;

        try {
            Document doc = new Document(PageSize.A4.rotate(), 40, 40, 60, 55);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));

            // Page borders + footer
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, Document d) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        cb.setLineWidth(2.5f); cb.setColorStroke(C_DARK);
                        cb.rectangle(22, 22, d.getPageSize().getWidth()-44, d.getPageSize().getHeight()-44);
                        cb.stroke();
                        cb.setLineWidth(0.8f); cb.setColorStroke(C_GOLD);
                        cb.rectangle(28, 28, d.getPageSize().getWidth()-56, d.getPageSize().getHeight()-56);
                        cb.stroke();
                        cb.setLineWidth(0.5f); cb.setColorStroke(C_LGRAY);
                        cb.moveTo(35, 44); cb.lineTo(d.getPageSize().getWidth()-35, 44); cb.stroke();
                        Font ff = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase("CONFIDENTIAL — PRISON SECURITY SYSTEM", ff), 40, 33, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Co-Admin Login Activity Register", ff),
                                d.getPageSize().getWidth()/2, 33, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                                new Phrase("Page " + w.getPageNumber() + "  |  " +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), ff),
                                d.getPageSize().getWidth()-40, 33, 0);
                    } catch (Exception ignored) {}
                }
            });

            doc.open();

            // Header
            PdfPTable hdr = new PdfPTable(new float[]{1, 5, 1});
            hdr.setWidthPercentage(100); hdr.setSpacingAfter(16f);

            PdfPCell lc = new PdfPCell(); lc.setBorder(PdfPCell.NO_BORDER);
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance("src/main/resources/images/logo.jpeg");
                logo.scaleToFit(55, 55); lc.addElement(logo);
            } catch (Exception ignored) {}
            hdr.addCell(lc);

            PdfPCell mc = new PdfPCell(); mc.setBorder(PdfPCell.NO_BORDER); mc.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph p1 = new Paragraph("DEPARTMENT OF CORRECTIONS & PRISON SECURITY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, C_DARK));
            p1.setAlignment(Element.ALIGN_CENTER); p1.setSpacingAfter(3f); mc.addElement(p1);
            Paragraph p2 = new Paragraph("CO-ADMIN LOGIN ACTIVITY REGISTER",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, C_DARK));
            p2.setAlignment(Element.ALIGN_CENTER); p2.setSpacingAfter(3f); mc.addElement(p2);
            Paragraph p3 = new Paragraph("System Access Audit  —  Classification: Restricted",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
            p3.setAlignment(Element.ALIGN_CENTER); mc.addElement(p3);
            hdr.addCell(mc);

            PdfPCell rc = new PdfPCell(); rc.setBorder(PdfPCell.NO_BORDER); rc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                com.lowagie.text.Image logo2 = com.lowagie.text.Image.getInstance("src/main/resources/images/logo.jpeg");
                logo2.scaleToFit(55, 55); rc.addElement(logo2);
            } catch (Exception ignored) {}
            hdr.addCell(rc);
            doc.add(hdr);

            // Meta bar
            long active   = records.stream().filter(r -> "ACTIVE".equals(r.status)).count();
            long loggedOut= records.size() - active;
            PdfPTable meta = new PdfPTable(3); meta.setWidthPercentage(100); meta.setSpacingAfter(12f);
            Font mf = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);
            pdfMetaCell(meta, "TOTAL SESSIONS: " + records.size(), mf, C_DARK);
            pdfMetaCell(meta, "ACTIVE: " + active + "  |  LOGGED OUT: " + loggedOut, mf, new Color(10, 70, 160));
            pdfMetaCell(meta, "GENERATED: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), mf, C_DARK);
            doc.add(meta);

            // Data table
            PdfPTable table = new PdfPTable(new float[]{0.5f, 2f, 2f, 2.5f, 2.5f, 1.5f, 1.8f});
            table.setWidthPercentage(100); table.setSpacingAfter(18f); table.setHeaderRows(1);
            Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : new String[]{"#","Username","Display Name","Login Time","Logout Time","Duration","Status"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, hf));
                cell.setBackgroundColor(C_DARK); cell.setPadding(9);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(new Color(50, 80, 150));
                table.addCell(cell);
            }
            Font df  = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font bdf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            for (int i = 0; i < records.size(); i++) {
                LoginLogRow l = records.get(i);
                Color rowBg = i % 2 == 0 ? Color.WHITE : C_ALT;
                Color stBg  = "ACTIVE".equals(l.status) ? C_GREEN : C_LGRAY;
                pdfDataCell(table, String.valueOf(i+1),            df,  rowBg, Element.ALIGN_CENTER);
                pdfDataCell(table, nvl(l.username),                bdf, rowBg, Element.ALIGN_LEFT);
                pdfDataCell(table, nvl(l.displayName),             df,  rowBg, Element.ALIGN_LEFT);
                pdfDataCell(table, fmtTs(l.loginAt),               df,  rowBg, Element.ALIGN_LEFT);
                pdfDataCell(table, fmtTs(l.logoutAt),              df,  rowBg, Element.ALIGN_LEFT);
                pdfDataCell(table, fmtMins(l.sessionMins),         df,  rowBg, Element.ALIGN_CENTER);
                PdfPCell sc = new PdfPCell(new Phrase(nvl(l.status).replace("_"," "),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, C_DARK)));
                sc.setBackgroundColor(stBg); sc.setPadding(8);
                sc.setHorizontalAlignment(Element.ALIGN_CENTER); sc.setVerticalAlignment(Element.ALIGN_MIDDLE);
                sc.setBorderColor(new Color(180,190,210));
                table.addCell(sc);
            }
            doc.add(table);

            // Verification strip
            PdfPTable vt = new PdfPTable(1); vt.setWidthPercentage(100);
            PdfPCell vc = new PdfPCell(new Phrase(
                    "Official audit document. Unauthorized disclosure is prohibited. Report ID: CAL-" +
                            (System.currentTimeMillis() % 1_000_000),
                    FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY)));
            vc.setBorder(PdfPCell.TOP); vc.setBorderColor(Color.LIGHT_GRAY);
            vc.setHorizontalAlignment(Element.ALIGN_CENTER); vc.setPadding(8);
            vc.setBackgroundColor(C_ALT);
            vt.addCell(vc); doc.add(vt);

            doc.close();
            new Alert(Alert.AlertType.INFORMATION, "Login Activity Report exported successfully!").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
        }
    }

    // ── PDF helpers ───────────────────────────────────────────────────────────
    private void pdfMetaCell(PdfPTable t, String txt, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(txt, f));
        c.setBackgroundColor(bg); c.setBorder(PdfPCell.NO_BORDER);
        c.setPadding(7); c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void pdfDataCell(PdfPTable t, String txt, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(txt == null ? "—" : txt, f));
        c.setBackgroundColor(bg); c.setPadding(8);
        c.setHorizontalAlignment(align); c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(new Color(190, 200, 215));
        t.addCell(c);
    }

    // ── misc helpers ──────────────────────────────────────────────────────────
    private void showError(String msg) { lblError.setText(msg); lblError.setStyle("-fx-text-fill:#c0392b; -fx-font-size:12px;"); }
    private void clearError()          { lblError.setText(""); }
    private void info(String msg)      { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }

    private String nvl(String s)        { return s != null && !s.isBlank() ? s : "—"; }
    private String fmtTs(Timestamp ts)  { return ts != null ? ts.toLocalDateTime().format(FMT) : "—"; }
    private String fmtMins(Integer m) {
        if (m == null) return "—";
        return m < 60 ? m + " min" : (m / 60) + "h " + (m % 60) + "m";
    }
}
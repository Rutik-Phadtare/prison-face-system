package com.prison.controller;

import com.prison.dao.UserDao;
import com.prison.model.User;
import com.prison.session.UserSession;
import com.prison.util.DatabaseUtil;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import org.mindrot.jbcrypt.BCrypt;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AdminManagementController {

    // ── FXML: stat labels ─────────────────────────────────────────────────────
    @FXML private Label totalAdminsLbl;
    @FXML private Label activeAdminsLbl;

    // ── FXML: accounts table ──────────────────────────────────────────────────
    @FXML private TableView<AdminRow>           adminTable;
    @FXML private TableColumn<AdminRow, Void>   colRow;
    @FXML private TableColumn<AdminRow, String> colDisplay;
    @FXML private TableColumn<AdminRow, String> colUsername;
    @FXML private TableColumn<AdminRow, String> colCreatedAt;
    @FXML private TableColumn<AdminRow, String> colLastLogin;
    @FXML private TableColumn<AdminRow, String> colStatus;
    @FXML private TableColumn<AdminRow, Void>   colActions;

    // ── FXML: form ────────────────────────────────────────────────────────────
    @FXML private Label         formTitle;
    @FXML private TextField     fldDisplayName;
    @FXML private TextField     fldUsername;
    @FXML private PasswordField fldPassword;
    @FXML private PasswordField fldConfirm;
    @FXML private Label         lblStrength;
    @FXML private ProgressBar   barStrength;
    @FXML private Label         lblError;
    @FXML private Button        btnSave;

    // ── FXML: emergency reset section ─────────────────────────────────────────
    @FXML private TextField     fldMasterKey;
    @FXML private TextField     fldResetUsername;
    @FXML private PasswordField fldResetNewPw;
    @FXML private PasswordField fldResetConfirm;
    @FXML private Label         lblResetStrength;
    @FXML private ProgressBar   barResetStrength;
    @FXML private Label         lblResetError;

    // ── state ─────────────────────────────────────────────────────────────────
    private AdminRow editTarget = null;

    /*
     *  MASTER RESET KEY
     *  ─────────────────────────────────────────────────────────────────────
     *  This is the emergency key shown on the Login screen "Forgot Password"
     *  button. Store this somewhere safe (print it, write it down).
     *  You can change it to any value you want.
     *  ─────────────────────────────────────────────────────────────────────
     */
    private static final String MASTER_RESET_KEY = "PRISON-RESET-2025";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    // Password rules (same as Co-Admin)
    private static final int     PW_MIN      = 8;
    private static final Pattern HAS_UPPER   = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWER   = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT   = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"|,.<>/?].*");
    private static final Pattern UNAME_OK    = Pattern.compile("^[a-zA-Z0-9_]{4,30}$");

    // PDF colours (navy/gold)
    private static final Color C_DARK  = new Color(10, 31, 92);
    private static final Color C_GOLD  = new Color(180, 140,  0);
    private static final Color C_ALT   = new Color(232, 236, 243);
    private static final Color C_GREEN = new Color(198, 239, 206);
    private static final Color C_LGRAY = new Color(220, 220, 220);

    // ── Simple read model ─────────────────────────────────────────────────────
    public static class AdminRow {
        public final int    userId;
        public final String username, displayName, createdAt, lastLogin;
        public final boolean active;

        public AdminRow(int id, String u, String d, String ca, String ll, boolean a) {
            userId=id; username=u; displayName=d; createdAt=ca; lastLogin=ll; active=a;
        }
        public String getUsername()    { return username; }
        public String getDisplayName() { return displayName; }
        public String getCreatedAt()   { return createdAt; }
        public String getLastLogin()   { return lastLogin; }
        public boolean isActive()      { return active; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        setupTable();
        setupFormListeners();
        setupResetListeners();
        loadAdmins();
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    private void setupTable() {
        colRow.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-alignment:center; -fx-text-fill:#888;");
            }
        });

        colDisplay.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().displayName != null ? d.getValue().displayName : d.getValue().username));
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().username));
        colCreatedAt.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().createdAt));
        colLastLogin.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().lastLogin));

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().active ? "ACTIVE" : "INACTIVE"));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label b = new Label(s);
                b.setStyle("ACTIVE".equals(s)
                        ? "-fx-background-color:#dcfce7;-fx-text-fill:#166534;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:2 10 2 10;-fx-background-radius:20;"
                        : "-fx-background-color:#fee2e2;-fx-text-fill:#991b1b;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:2 10 2 10;-fx-background-radius:20;");
                setGraphic(b); setText(null);
            }
        });

        colActions.setCellFactory(c -> new TableCell<>() {
            private final Button pwBtn  = makeBtn("Change PW", "#0a1f5c");
            private final Button togBtn = makeBtn("Toggle",    "#b45309");
            {
                pwBtn.setOnAction(e  -> loadChangePwMode(getTableView().getItems().get(getIndex())));
                togBtn.setOnAction(e -> {
                    AdminRow r = getTableView().getItems().get(getIndex());
                    // Prevent disabling the last active admin
                    if (r.active) {
                        long activeCount = adminTable.getItems().stream().filter(a -> a.active).count();
                        if (activeCount <= 1) {
                            alert(Alert.AlertType.WARNING, "Cannot deactivate the last active admin account.");
                            return;
                        }
                    }
                    setAdminActive(r.userId, !r.active);
                    loadAdmins();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                // Hide toggle button for self
                AdminRow r = getTableView().getItems().get(getIndex());
                User me = UserSession.getUser();
                if (me != null && r.userId == me.getUserId()) {
                    HBox box = new HBox(6, pwBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                } else {
                    HBox box = new HBox(6, pwBtn, togBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadAdmins() {
        List<AdminRow> rows = fetchAllAdmins();
        adminTable.setItems(FXCollections.observableArrayList(rows));
        long active = rows.stream().filter(r -> r.active).count();
        totalAdminsLbl.setText(String.valueOf(rows.size()));
        activeAdminsLbl.setText(String.valueOf(active));
    }

    // ── Form: create admin / change password ──────────────────────────────────
    private void setupFormListeners() {
        fldPassword.textProperty().addListener((o, ov, nv) -> updateStrength(nv, lblStrength, barStrength));
        fldConfirm.textProperty().addListener((o, ov, nv) -> {
            if (!nv.isBlank() && !nv.equals(fldPassword.getText()))
                fldConfirm.setStyle("-fx-border-color:#c0392b;-fx-border-width:2;");
            else fldConfirm.setStyle("");
        });
    }

    private void loadChangePwMode(AdminRow row) {
        editTarget = row;
        fldDisplayName.setText(row.displayName != null ? row.displayName : row.username);
        fldDisplayName.setDisable(true);
        fldUsername.setText(row.username);
        fldUsername.setDisable(true);
        fldPassword.clear(); fldConfirm.clear();
        formTitle.setText("🔑  Change Password — " + row.username);
        btnSave.setText("Update Password");
        clearError(lblError);
    }

    @FXML public void onFormClear() {
        editTarget = null;
        fldDisplayName.setDisable(false);
        fldUsername.setDisable(false);
        fldDisplayName.clear(); fldUsername.clear();
        fldPassword.clear(); fldConfirm.clear();
        lblStrength.setText(""); barStrength.setProgress(0);
        formTitle.setText("➕  New Admin Account");
        btnSave.setText("Create Admin");
        clearError(lblError);
    }

    @FXML public void onFormSave() {
        clearError(lblError);
        String pw = fldPassword.getText(), confirm = fldConfirm.getText();

        if (!validatePassword(pw, confirm, lblError)) return;

        if (editTarget != null) {
            if (changePasswordById(editTarget.userId, pw)) {
                alert(Alert.AlertType.INFORMATION, "Password updated for '" + editTarget.username + "'.");
                onFormClear(); loadAdmins();
            } else {
                showError(lblError, "Failed to update password.");
            }
        } else {
            String display = fldDisplayName.getText().trim();
            String uname   = fldUsername.getText().trim();
            if (display.isBlank()) { showError(lblError, "Display name is required."); return; }
            if (uname.isBlank())   { showError(lblError, "Username is required."); return; }
            if (!UNAME_OK.matcher(uname).matches()) {
                showError(lblError, "Username: 4–30 chars, letters/digits/underscore only."); return; }
            if (usernameExists(uname)) {
                showError(lblError, "Username '" + uname + "' is already taken."); return; }

            if (createAdmin(uname, pw, display)) {
                alert(Alert.AlertType.INFORMATION, "Admin account '" + uname + "' created.");
                onFormClear(); loadAdmins();
            } else {
                showError(lblError, "Failed to create account.");
            }
        }
    }

    // ── Emergency Reset section ───────────────────────────────────────────────
    private void setupResetListeners() {
        fldResetNewPw.textProperty().addListener((o, ov, nv) ->
                updateStrength(nv, lblResetStrength, barResetStrength));
        fldResetConfirm.textProperty().addListener((o, ov, nv) -> {
            if (!nv.isBlank() && !nv.equals(fldResetNewPw.getText()))
                fldResetConfirm.setStyle("-fx-border-color:#c0392b;-fx-border-width:2;");
            else fldResetConfirm.setStyle("");
        });
    }

    @FXML public void onEmergencyReset() {
        clearError(lblResetError);
        String key      = fldMasterKey.getText().trim();
        String uname    = fldResetUsername.getText().trim();
        String pw       = fldResetNewPw.getText();
        String confirm  = fldResetConfirm.getText();

        // 1. Verify master key
        if (!MASTER_RESET_KEY.equals(key)) {
            showError(lblResetError, "Invalid master reset key.");
            return;
        }
        // 2. Username exists check
        if (uname.isBlank()) { showError(lblResetError, "Enter the admin username to reset."); return; }
        if (!usernameExists(uname)) {
            showError(lblResetError, "No account found with username '" + uname + "'."); return; }

        // 3. Password validation
        if (!validatePassword(pw, confirm, lblResetError)) return;

        // 4. Apply reset (works for ANY role — admin or co-admin)
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(pw, BCrypt.gensalt(10)));
            ps.setString(2, uname);
            if (ps.executeUpdate() > 0) {
                fldMasterKey.clear(); fldResetUsername.clear();
                fldResetNewPw.clear(); fldResetConfirm.clear();
                lblResetStrength.setText(""); barResetStrength.setProgress(0);
                alert(Alert.AlertType.INFORMATION,
                        "✅ Password reset successfully for '" + uname + "'.\nYou can now log in with the new password.");
            } else {
                showError(lblResetError, "Reset failed — no rows updated.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError(lblResetError, "DB error: " + e.getMessage());
        }
    }

    // ── DB helpers ────────────────────────────────────────────────────────────
    private List<AdminRow> fetchAllAdmins() {
        List<AdminRow> list = new ArrayList<>();
        String sql = "SELECT user_id, username, display_name, created_at, last_login, is_active " +
                "FROM users WHERE role = 'ADMIN' ORDER BY created_at";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ca = rs.getTimestamp("created_at");
                Timestamp ll = rs.getTimestamp("last_login");
                boolean active;
                try { active = rs.getInt("is_active") == 1; }
                catch (Exception e) { active = true; } // column may not exist yet
                list.add(new AdminRow(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        ca != null ? ca.toLocalDateTime().format(FMT) : "—",
                        ll != null ? ll.toLocalDateTime().format(FMT) : "Never",
                        active
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private boolean createAdmin(String username, String plainPw, String displayName) {
        String hash = BCrypt.hashpw(plainPw, BCrypt.gensalt(10));
        String by   = UserSession.getUser() != null ? UserSession.getUser().getUsername() : "SYSTEM";
        String sql  = "INSERT INTO users (username, password_hash, role, display_name, created_by, is_active) " +
                "VALUES (?, ?, 'ADMIN', ?, ?, 1)";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, hash);
            ps.setString(3, displayName); ps.setString(4, by);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private boolean changePasswordById(int userId, String plainPw) {
        String hash = BCrypt.hashpw(plainPw, BCrypt.gensalt(10));
        String sql  = "UPDATE users SET password_hash = ? WHERE user_id = ? AND role = 'ADMIN'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hash); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void setAdminActive(int userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0); ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean usernameExists(String uname) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uname);
            return ps.executeQuery().next();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    @FXML public void onRefresh() { loadAdmins(); }

    // ── Password strength ──────────────────────────────────────────────────────
    private void updateStrength(String pw, Label lbl, ProgressBar bar) {
        int score = 0;
        if (pw.length() >= PW_MIN)             score++;
        if (pw.length() >= 12)                 score++;
        if (HAS_UPPER.matcher(pw).matches())   score++;
        if (HAS_LOWER.matcher(pw).matches())   score++;
        if (HAS_DIGIT.matcher(pw).matches())   score++;
        if (HAS_SPECIAL.matcher(pw).matches()) score++;
        double pct = score / 6.0;
        String label, color;
        if      (pct <= 0.2) { label = "Very Weak";  color = "#c0392b"; }
        else if (pct <= 0.4) { label = "Weak";        color = "#e67e22"; }
        else if (pct <= 0.6) { label = "Fair";        color = "#f1c40f"; }
        else if (pct <= 0.8) { label = "Strong";      color = "#27ae60"; }
        else                 { label = "Very Strong"; color = "#1a6b3c"; }
        lbl.setText(label);
        lbl.setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;-fx-font-size:11px;");
        bar.setProgress(pct);
        bar.setStyle("-fx-accent:" + color + ";");
    }

    private boolean validatePassword(String pw, String confirm, Label errLbl) {
        if (pw.isBlank())                         { showError(errLbl, "Password is required."); return false; }
        if (pw.length() < PW_MIN)                 { showError(errLbl, "Password must be at least " + PW_MIN + " characters."); return false; }
        if (!HAS_UPPER.matcher(pw).matches())     { showError(errLbl, "Must contain at least one uppercase letter (A–Z)."); return false; }
        if (!HAS_LOWER.matcher(pw).matches())     { showError(errLbl, "Must contain at least one lowercase letter (a–z)."); return false; }
        if (!HAS_DIGIT.matcher(pw).matches())     { showError(errLbl, "Must contain at least one digit (0–9)."); return false; }
        if (!HAS_SPECIAL.matcher(pw).matches())   { showError(errLbl, "Must contain at least one special character (!@#$...)."); return false; }
        if (!pw.equals(confirm))                   { showError(errLbl, "Passwords do not match."); return false; }
        return true;
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private Button makeBtn(String text, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;-fx-font-size:10px;" +
                "-fx-background-radius:5;-fx-padding:3 9 3 9;-fx-cursor:hand;");
        return b;
    }

    private void showError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill:#c0392b;-fx-font-size:12px;");
    }

    private void clearError(Label lbl) { lbl.setText(""); }

    private void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }
}
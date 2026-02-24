package com.prison.dao;

import com.prison.model.User;
import com.prison.util.DatabaseUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // ─── existing findByUsername ──────────────────────────────────────────────
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ─── fetch all CO_ADMIN accounts ──────────────────────────────────────────
    public List<User> findAllCoAdmins() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.*, " +
                "(SELECT COUNT(*) FROM co_admin_login_logs l WHERE l.user_id = u.user_id) AS login_count " +
                "FROM users u WHERE u.role = 'CO_ADMIN' ORDER BY u.created_at DESC";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ─── create new CO_ADMIN ──────────────────────────────────────────────────
    public boolean createCoAdmin(String username, String plainPw,
                                 String displayName, String createdBy) {
        String hash = BCrypt.hashpw(plainPw, BCrypt.gensalt(10));
        String sql  = "INSERT INTO users (username, password_hash, role, display_name, created_by, is_active) " +
                "VALUES (?, ?, 'CO_ADMIN', ?, ?, 1)";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, hash);
            ps.setString(3, displayName != null ? displayName.trim() : username.trim());
            ps.setString(4, createdBy);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false; // duplicate username
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ─── change password ──────────────────────────────────────────────────────
    public boolean changePassword(int userId, String newPlainPw) {
        String hash = BCrypt.hashpw(newPlainPw, BCrypt.gensalt(10));
        String sql  = "UPDATE users SET password_hash = ? WHERE user_id = ? AND role = 'CO_ADMIN'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ─── toggle active / inactive ────────────────────────────────────────────
    public boolean setActive(int userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ? AND role = 'CO_ADMIN'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ─── delete CO_ADMIN ─────────────────────────────────────────────────────
    public boolean deleteCoAdmin(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ? AND role = 'CO_ADMIN'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ─── username exists check ────────────────────────────────────────────────
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ─── insert login log row, returns generated log_id ──────────────────────
    public int insertLoginLog(int userId, String username, String displayName) {
        String sql = "INSERT INTO co_admin_login_logs (user_id, username, display_name, status) " +
                "VALUES (?, ?, ?, 'ACTIVE')";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, displayName != null ? displayName : username);
            ps.executeUpdate();
            // also update last_login on user row
            updateLastLogin(con, userId);
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    // ─── mark logout ─────────────────────────────────────────────────────────
    public void recordLogout(int logId) {
        if (logId < 0) return;
        String sql = "UPDATE co_admin_login_logs " +
                "SET logout_at = NOW(), status = 'LOGGED_OUT', " +
                "    session_mins = TIMESTAMPDIFF(MINUTE, login_at, NOW()) " +
                "WHERE log_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, logId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─── fetch all login log rows ─────────────────────────────────────────────
    public List<LoginLogRow> findAllLoginLogs() {
        List<LoginLogRow> list = new ArrayList<>();
        String sql = "SELECT * FROM co_admin_login_logs ORDER BY login_at DESC";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LoginLogRow r = new LoginLogRow();
                r.logId       = rs.getInt("log_id");
                r.userId      = rs.getInt("user_id");
                r.username    = rs.getString("username");
                r.displayName = rs.getString("display_name");
                r.loginAt     = rs.getTimestamp("login_at");
                r.logoutAt    = rs.getTimestamp("logout_at");
                r.sessionMins = rs.getObject("session_mins") != null ? rs.getInt("session_mins") : null;
                r.ipAddress   = rs.getString("ip_address");
                r.status      = rs.getString("status");
                list.add(r);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ─── inner data class for login log rows ─────────────────────────────────
    public static class LoginLogRow {
        public int       logId;
        public int       userId;
        public String    username;
        public String    displayName;
        public Timestamp loginAt;
        public Timestamp logoutAt;
        public Integer   sessionMins;
        public String    ipAddress;
        public String    status;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────
    private void updateLastLogin(Connection con, int userId) {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET last_login = NOW() WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        try { u.setDisplayName(rs.getString("display_name")); } catch (Exception ignored) {}
        try { u.setActive(rs.getInt("is_active") == 1); }      catch (Exception ignored) {}
        try { u.setCreatedBy(rs.getString("created_by")); }    catch (Exception ignored) {}
        return u;
    }
}
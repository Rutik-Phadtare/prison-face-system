package com.prison.service;

import com.prison.dao.UserDao;
import com.prison.model.User;
import com.prison.session.UserSession;
import com.prison.util.DatabaseUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public User authenticate(String username, String password) {

        String sql = "SELECT user_id, username, password_hash, role, display_name, is_active " +
                "FROM users WHERE username = ?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            // Check account is active (new column; defaults to 1 for existing rows)
            try {
                if (rs.getInt("is_active") == 0) return null;
            } catch (Exception ignored) {} // column may not exist yet on old DB

            if (!BCrypt.checkpw(password, rs.getString("password_hash"))) return null;

            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setUsername(rs.getString("username"));
            user.setRole(rs.getString("role"));
            try { user.setDisplayName(rs.getString("display_name")); } catch (Exception ignored) {}

            // ── Record login for CO_ADMIN accounts ────────────────────────────
            if ("CO_ADMIN".equals(user.getRole())) {
                int logId = userDao.insertLoginLog(
                        user.getUserId(),
                        user.getUsername(),
                        user.getDisplayName()
                );
                UserSession.setLoginLogId(logId);
            }

            return user;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
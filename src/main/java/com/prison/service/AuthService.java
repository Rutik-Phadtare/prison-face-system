package com.prison.service;

import com.prison.model.User;
import com.prison.util.DatabaseUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    public User authenticate(String username, String password) {

        String sql = "SELECT user_id, username, password_hash, role FROM users WHERE username=?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            if (!BCrypt.checkpw(password, rs.getString("password_hash"))) {
                return null;
            }

            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setUsername(rs.getString("username"));
            user.setRole(rs.getString("role"));
            return user;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}

package com.prison.dao;

import com.prison.model.Guard;
import com.prison.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuardDao {

    /* =========================
       COUNT ACTIVE GUARDS (Fixes Dashboard Error)
       ========================= */
    public int countActiveGuards() {
        String sql = "SELECT COUNT(*) FROM guards WHERE status = 'ACTIVE'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int saveAndReturnId(Guard guard) {
        String sql = "INSERT INTO guards (name, designation, shift, status, joining_date, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, guard.getName());
            ps.setString(2, guard.getDesignation());
            ps.setString(3, guard.getShift());
            ps.setString(4, guard.getStatus());
            ps.setDate(5, guard.getJoiningDate() != null ? Date.valueOf(guard.getJoiningDate()) : null);
            ps.setString(6, guard.getDescription());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Guard findById(int id) {
        String sql = "SELECT * FROM guards WHERE guard_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToGuard(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void update(Guard guard) {
        String sql = "UPDATE guards SET name=?, designation=?, shift=?, status=?, joining_date=?, description=? WHERE guard_id=?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, guard.getName());
            ps.setString(2, guard.getDesignation());
            ps.setString(3, guard.getShift());
            ps.setString(4, guard.getStatus());
            ps.setDate(5, guard.getJoiningDate() != null ? Date.valueOf(guard.getJoiningDate()) : null);
            ps.setString(6, guard.getDescription());
            ps.setInt(7, guard.getGuardId());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Guard> findAll() {
        List<Guard> list = new ArrayList<>();
        String sql = "SELECT * FROM guards";
        try (Connection con = DatabaseUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToGuard(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void delete(int id) {
        String sql = "DELETE FROM guards WHERE guard_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Guard mapResultSetToGuard(ResultSet rs) throws SQLException {
        Guard g = new Guard();
        g.setGuardId(rs.getInt("guard_id"));
        g.setName(rs.getString("name"));
        g.setDesignation(rs.getString("designation"));
        g.setShift(rs.getString("shift"));
        g.setStatus(rs.getString("status"));
        g.setDescription(rs.getString("description"));
        if (rs.getDate("joining_date") != null) {
            g.setJoiningDate(rs.getDate("joining_date").toLocalDate());
        }
        return g;
    }
}
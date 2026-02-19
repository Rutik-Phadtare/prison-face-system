package com.prison.dao;

import com.prison.model.Guard;
import com.prison.util.DatabaseUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuardDao {

    /* =========================
       DASHBOARD / STATS METHODS
       ========================= */
    public int countActiveGuards() {
        String sql = "SELECT COUNT(*) FROM guards WHERE status = 'ACTIVE'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // 🔥 This fixes the "Cannot resolve method 'countGuards'" error
    public int countGuards() {
        String sql = "SELECT COUNT(*) FROM guards";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /* =========================
       CRUD OPERATIONS
       ========================= */
    public int saveAndReturnId(Guard guard) {
        String sql = "INSERT INTO guards (name, designation, shift, status, joining_date, description, age, birth_date, address, gender, transfer_from, salary) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, guard.getName());
            ps.setString(2, guard.getDesignation());
            ps.setString(3, guard.getShift());
            ps.setString(4, guard.getStatus());
            ps.setDate(5, guard.getJoiningDate() != null ? Date.valueOf(guard.getJoiningDate()) : null);
            ps.setString(6, guard.getDescription());
            ps.setInt(7, guard.getAge());
            ps.setDate(8, guard.getBirthDate() != null ? Date.valueOf(guard.getBirthDate()) : null);
            ps.setString(9, guard.getAddress());
            ps.setString(10, guard.getGender());
            ps.setString(11, guard.getTransferFrom());
            ps.setDouble(12, guard.getSalary());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public void update(Guard guard) {
        String sql = "UPDATE guards SET name=?, designation=?, shift=?, status=?, joining_date=?, description=?, age=?, birth_date=?, address=?, gender=?, transfer_from=?, salary=? WHERE guard_id=?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, guard.getName());
            ps.setString(2, guard.getDesignation());
            ps.setString(3, guard.getShift());
            ps.setString(4, guard.getStatus());
            ps.setDate(5, guard.getJoiningDate() != null ? Date.valueOf(guard.getJoiningDate()) : null);
            ps.setString(6, guard.getDescription());
            ps.setInt(7, guard.getAge());
            ps.setDate(8, guard.getBirthDate() != null ? Date.valueOf(guard.getBirthDate()) : null);
            ps.setString(9, guard.getAddress());
            ps.setString(10, guard.getGender());
            ps.setString(11, guard.getTransferFrom());
            ps.setDouble(12, guard.getSalary());
            ps.setInt(13, guard.getGuardId());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
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
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public void delete(int id) {
        String sql = "DELETE FROM guards WHERE guard_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Guard findById(int id) {
        String sql = "SELECT * FROM guards WHERE guard_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToGuard(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    /* =========================
       MAPPING HELPER
       ========================= */
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

        // Map new fields
        g.setAge(rs.getInt("age"));
        if (rs.getDate("birth_date") != null) {
            g.setBirthDate(rs.getDate("birth_date").toLocalDate());
        }
        g.setAddress(rs.getString("address"));
        g.setGender(rs.getString("gender"));
        g.setTransferFrom(rs.getString("transfer_from"));
        g.setSalary(rs.getDouble("salary"));

        return g;
    }
}
package com.prison.dao;

import com.prison.model.Guard;
import com.prison.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GuardDao {

    public int saveAndReturnId(Guard guard) {

        String sql = """
        INSERT INTO guards (name, designation, shift, status)
        VALUES (?, ?, ?, ?)
    """;

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, guard.getName());
            ps.setString(2, guard.getDesignation());
            ps.setString(3, guard.getShift());
            ps.setString(4, guard.getStatus());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

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

            if (rs.next()) {
                Guard g = new Guard();
                g.setGuardId(rs.getInt("guard_id"));
                g.setName(rs.getString("name"));
                g.setDesignation(rs.getString("designation"));
                g.setShift(rs.getString("shift"));
                g.setStatus(rs.getString("status"));
                return g;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

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


    public List<Guard> findAll() {

        List<Guard> list = new ArrayList<>();
        String sql = "SELECT * FROM guards";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Guard g = new Guard();
                g.setGuardId(rs.getInt("guard_id"));
                g.setName(rs.getString("name"));
                g.setDesignation(rs.getString("designation"));
                g.setShift(rs.getString("shift"));
                g.setStatus(rs.getString("status"));
                list.add(g);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void delete(int guardId) {

        String sql = "DELETE FROM guards WHERE guard_id = ?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, guardId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

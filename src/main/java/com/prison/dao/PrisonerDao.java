package com.prison.dao;

import com.prison.model.Prisoner;
import com.prison.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PrisonerDao {

    public int saveAndReturnId(Prisoner prisoner) {

        String sql = """
        INSERT INTO prisoners (name, crime, cell_no, sentence_years, status)
        VALUES (?, ?, ?, ?, ?)
    """;

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, prisoner.getName());
            ps.setString(2, prisoner.getCrime());
            ps.setString(3, prisoner.getCellNo());
            ps.setInt(4, prisoner.getSentenceYears());
            ps.setString(5, prisoner.getStatus());

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

    public Prisoner findById(int id) {

        String sql = "SELECT * FROM prisoners WHERE prisoner_id = ?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Prisoner p = new Prisoner();
                p.setPrisonerId(rs.getInt("prisoner_id"));
                p.setName(rs.getString("name"));
                p.setCrime(rs.getString("crime"));
                p.setCellNo(rs.getString("cell_no"));
                p.setSentenceYears(rs.getInt("sentence_years"));
                p.setStatus(rs.getString("status"));
                return p;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public List<Prisoner> findAll() {

        List<Prisoner> list = new ArrayList<>();
        String sql = "SELECT * FROM prisoners";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Prisoner p = new Prisoner();
                p.setPrisonerId(rs.getInt("prisoner_id"));
                p.setName(rs.getString("name"));
                p.setCrime(rs.getString("crime"));
                p.setCellNo(rs.getString("cell_no"));
                p.setSentenceYears(rs.getInt("sentence_years"));
                p.setStatus(rs.getString("status"));
                list.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void delete(int prisonerId) {

        String sql = "DELETE FROM prisoners WHERE prisoner_id = ?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, prisonerId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

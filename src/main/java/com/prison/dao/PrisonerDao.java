package com.prison.dao;

import com.prison.model.Prisoner;
import com.prison.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;



public class PrisonerDao {

    public int countPrisonersInCell(String cellNo) {
        String sql = "SELECT COUNT(*) FROM prisoners WHERE cell_no = ? AND status != 'Released'";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cellNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /* =========================
       SAVE (CONTROLLER-FRIENDLY)
       ========================= */
    public void save(Prisoner prisoner) {
        saveAndReturnId(prisoner);
    }

    /* =========================
       SAVE AND RETURN ID
       ========================= */
    public int saveAndReturnId(Prisoner prisoner) {

        String sql = """
    INSERT INTO prisoners
    (name, crime, cell_no, sentence_years,
     sentence_start_date, release_date, description, status)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
""";


        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, prisoner.getName());
            ps.setString(2, prisoner.getCrime());
            ps.setString(3, prisoner.getCellNo());
            ps.setInt(4, prisoner.getSentenceYears());

            ps.setDate(5, Date.valueOf(prisoner.getSentenceStartDate()));
            ps.setDate(6, Date.valueOf(prisoner.getReleaseDate()));

            ps.setString(7, prisoner.getDescription());
            ps.setString(8, prisoner.getStatus());

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


    /* =========================
       FIND BY ID
       ========================= */
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
                p.setDescription(rs.getString("description"));

                Date rd = rs.getDate("release_date");
                if (rd != null) {
                    p.setReleaseDate(rd.toLocalDate());
                }

                return p;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /* =========================
       FIND ALL
       ========================= */
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
                p.setSentenceStartDate(
                        rs.getDate("sentence_start_date").toLocalDate()
                );
                p.setReleaseDate(
                        rs.getDate("release_date").toLocalDate()
                );
                p.setDescription(rs.getString("description"));

                p.setStatus(rs.getString("status"));
                p.setDescription(rs.getString("description"));

                Date rd = rs.getDate("release_date");
                if (rd != null) {
                    p.setReleaseDate(rd.toLocalDate());
                }

                list.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /* =========================
       DELETE
       ========================= */
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
    public int countActivePrisoners() {

        String sql = "SELECT COUNT(*) FROM prisoners WHERE status = 'IN_CUSTODY'";

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

    public void update(Prisoner prisoner) {

        String sql = """
        UPDATE prisoners
        SET name = ?, crime = ?, cell_no = ?, sentence_years = ?,
            status = ?, description = ?, release_date = ?
        WHERE prisoner_id = ?
    """;

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, prisoner.getName());
            ps.setString(2, prisoner.getCrime());
            ps.setString(3, prisoner.getCellNo());
            ps.setInt(4, prisoner.getSentenceYears());
            ps.setString(5, prisoner.getStatus());
            ps.setString(6, prisoner.getDescription());
            ps.setDate(7, Date.valueOf(prisoner.getReleaseDate()));
            ps.setInt(8, prisoner.getPrisonerId());


            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /* =========================
       UPDATE STATUS ONLY
       ========================= */
    public void updateStatus(int prisonerId, String status) {

        String sql = "UPDATE prisoners SET status = ? WHERE prisoner_id = ?";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, prisonerId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

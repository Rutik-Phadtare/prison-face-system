package com.prison.dao;

import com.prison.model.Prisoner;
import com.prison.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrisonerDao {

    // ══════════════════════════════════════════════════════════════════════
    //  CELL COUNT (unchanged)
    // ══════════════════════════════════════════════════════════════════════
    public int countPrisonersInCell(String cellNo) {
        String sql = "SELECT COUNT(*) FROM prisoners WHERE cell_no = ? AND status != 'RELEASED'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cellNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SAVE (wrapper)
    // ══════════════════════════════════════════════════════════════════════
    public void save(Prisoner prisoner) { saveAndReturnId(prisoner); }

    // ══════════════════════════════════════════════════════════════════════
    //  SAVE AND RETURN ID
    // ══════════════════════════════════════════════════════════════════════
    public int saveAndReturnId(Prisoner prisoner) {
        String sql = """
            INSERT INTO prisoners
            (name, crime, cell_no, sentence_years,
             sentence_start_date, release_date, description, status,
             age, gender, nationality, home_address, aadhar_number,
             blood_type, height, weight, identification_marks,
             emergency_contact, emergency_phone,
             lawyer_name, lawyer_phone,
             danger_level, behavior_rating,
             incident_notes, visitor_log)
            VALUES (?,?,?,?, ?,?,?,?, ?,?,?,?,?, ?,?,?,?, ?,?, ?,?, ?,?, ?,?)
        """;
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1,  prisoner.getName());
            ps.setString(2,  prisoner.getCrime());
            ps.setString(3,  prisoner.getCellNo());
            ps.setInt(4,     prisoner.getSentenceYears());
            ps.setDate(5,    prisoner.getSentenceStartDate() != null
                    ? Date.valueOf(prisoner.getSentenceStartDate()) : null);
            ps.setDate(6,    prisoner.getReleaseDate() != null
                    ? Date.valueOf(prisoner.getReleaseDate()) : null);
            ps.setString(7,  prisoner.getDescription());
            ps.setString(8,  prisoner.getStatus());
            ps.setInt(9,     prisoner.getAge());
            ps.setString(10, prisoner.getGender());
            ps.setString(11, prisoner.getNationality());
            ps.setString(12, prisoner.getHomeAddress());
            ps.setString(13, prisoner.getAadharNumber());
            ps.setString(14, prisoner.getBloodType());
            ps.setString(15, prisoner.getHeight());
            ps.setString(16, prisoner.getWeight());
            ps.setString(17, prisoner.getIdentificationMarks());
            ps.setString(18, prisoner.getEmergencyContact());
            ps.setString(19, prisoner.getEmergencyPhone());
            ps.setString(20, prisoner.getLawyerName());
            ps.setString(21, prisoner.getLawyerPhone());
            ps.setString(22, prisoner.getDangerLevel() != null ? prisoner.getDangerLevel() : "LOW");
            ps.setString(23, prisoner.getBehaviorRating() != null ? prisoner.getBehaviorRating() : "GOOD");
            ps.setString(24, prisoner.getIncidentNotes());
            ps.setString(25, prisoner.getVisitorLog());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════════════
    public void update(Prisoner prisoner) {
        String sql = """
        UPDATE prisoners SET
            name=?, crime=?, cell_no=?, sentence_years=?,
            status=?, description=?, 
            sentence_start_date=?, release_date=?, 
            age=?, gender=?, nationality=?, home_address=?, aadhar_number=?,
            blood_type=?, height=?, weight=?, identification_marks=?,
            emergency_contact=?, emergency_phone=?,
            lawyer_name=?, lawyer_phone=?,
            danger_level=?, behavior_rating=?,
            incident_notes=?, visitor_log=?
        WHERE prisoner_id=?
    """;
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1,  prisoner.getName());
            ps.setString(2,  prisoner.getCrime());
            ps.setString(3,  prisoner.getCellNo());
            ps.setInt(4,     prisoner.getSentenceYears());
            ps.setString(5,  prisoner.getStatus());
            ps.setString(6,  prisoner.getDescription());

            // ADDED: Sentence Start Date
            ps.setDate(7,    prisoner.getSentenceStartDate() != null
                    ? Date.valueOf(prisoner.getSentenceStartDate()) : null);

            // Release Date shifted to 8
            ps.setDate(8,    prisoner.getReleaseDate() != null
                    ? Date.valueOf(prisoner.getReleaseDate()) : null);

            ps.setInt(9,     prisoner.getAge());
            ps.setString(10, prisoner.getGender());
            ps.setString(11, prisoner.getNationality());
            ps.setString(12, prisoner.getHomeAddress());
            ps.setString(13, prisoner.getAadharNumber());
            ps.setString(14, prisoner.getBloodType());
            ps.setString(15, prisoner.getHeight());
            ps.setString(16, prisoner.getWeight());
            ps.setString(17, prisoner.getIdentificationMarks());
            ps.setString(18, prisoner.getEmergencyContact());
            ps.setString(19, prisoner.getEmergencyPhone());
            ps.setString(20, prisoner.getLawyerName());
            ps.setString(21, prisoner.getLawyerPhone());
            ps.setString(22, prisoner.getDangerLevel());
            ps.setString(23, prisoner.getBehaviorRating());
            ps.setString(24, prisoner.getIncidentNotes());
            ps.setString(25, prisoner.getVisitorLog());
            ps.setInt(26,    prisoner.getPrisonerId()); // Index shifted to 26

            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
    // ══════════════════════════════════════════════════════════════════════
    //  FIND ALL
    // ══════════════════════════════════════════════════════════════════════
    public List<Prisoner> findAll() {
        List<Prisoner> list = new ArrayList<>();
        String sql = "SELECT * FROM prisoners";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FIND BY ID
    // ══════════════════════════════════════════════════════════════════════
    public Prisoner findById(int id) {
        String sql = "SELECT * FROM prisoners WHERE prisoner_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════════════
    public void delete(int prisonerId) {
        String sql = "DELETE FROM prisoners WHERE prisoner_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prisonerId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COUNT ACTIVE
    // ══════════════════════════════════════════════════════════════════════
    public int countActivePrisoners() {
        String sql = "SELECT COUNT(*) FROM prisoners WHERE status = 'IN_CUSTODY'";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UPDATE STATUS ONLY
    // ══════════════════════════════════════════════════════════════════════
    public void updateStatus(int prisonerId, String status) {
        String sql = "UPDATE prisoners SET status = ? WHERE prisoner_id = ?";
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, prisonerId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIVATE: MAP ROW → PRISONER
    // ══════════════════════════════════════════════════════════════════════
    private Prisoner mapRow(ResultSet rs) throws SQLException {
        Prisoner p = new Prisoner();
        p.setPrisonerId(rs.getInt("prisoner_id"));
        p.setName(rs.getString("name"));
        p.setCrime(rs.getString("crime"));
        p.setCellNo(rs.getString("cell_no"));
        p.setSentenceYears(rs.getInt("sentence_years"));
        p.setStatus(rs.getString("status"));
        p.setDescription(rs.getString("description"));

        Date sd = rs.getDate("sentence_start_date");
        if (sd != null) p.setSentenceStartDate(sd.toLocalDate());

        Date rd = rs.getDate("release_date");
        if (rd != null) p.setReleaseDate(rd.toLocalDate());

        // New personal fields
        p.setAge(rs.getInt("age"));
        p.setGender(safeStr(rs, "gender"));
        p.setNationality(safeStr(rs, "nationality"));
        p.setHomeAddress(safeStr(rs, "home_address"));
        p.setAadharNumber(safeStr(rs, "aadhar_number"));
        p.setBloodType(safeStr(rs, "blood_type"));
        p.setHeight(safeStr(rs, "height"));
        p.setWeight(safeStr(rs, "weight"));
        p.setIdentificationMarks(safeStr(rs, "identification_marks"));

        p.setEmergencyContact(safeStr(rs, "emergency_contact"));
        p.setEmergencyPhone(safeStr(rs, "emergency_phone"));
        p.setLawyerName(safeStr(rs, "lawyer_name"));
        p.setLawyerPhone(safeStr(rs, "lawyer_phone"));

        p.setDangerLevel(safeStr(rs, "danger_level"));
        p.setBehaviorRating(safeStr(rs, "behavior_rating"));
        p.setIncidentNotes(safeStr(rs, "incident_notes"));
        p.setVisitorLog(safeStr(rs, "visitor_log"));

        return p;
    }

    private String safeStr(ResultSet rs, String col) throws SQLException {
        String v = rs.getString(col);
        return v != null ? v : "";
    }
}
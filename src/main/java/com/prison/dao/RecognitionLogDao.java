package com.prison.dao;

import com.prison.model.RecognitionLog;
import com.prison.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RecognitionLogDao {

    private static final String INSERT_SQL =
            "INSERT INTO recognition_logs (person_type, person_id, result) VALUES (?, ?, ?)";

    private static final String SELECT_ALL =
            "SELECT " +
                    "    rl.log_id, " +
                    "    rl.person_type, " +
                    "    rl.person_id, " +
                    "    rl.result, " +
                    "    rl.detected_at, " +

                    // Name
                    "    CASE " +
                    "        WHEN rl.person_type = 'GUARD'    THEN g.name " +
                    "        WHEN rl.person_type = 'PRISONER' THEN p.name " +
                    "        ELSE NULL " +
                    "    END AS person_name, " +

                    // Department: guard role/designation OR prisoner cell
                    "    CASE " +
                    "        WHEN rl.person_type = 'GUARD'    THEN COALESCE(g.designation, g.role, '—') " +
                    "        WHEN rl.person_type = 'PRISONER' THEN CONCAT('Cell: ', COALESCE(p.cell_no, '—')) " +
                    "        ELSE NULL " +
                    "    END AS department, " +

                    // Extra info: guard shift OR prisoner crime
                    "    CASE " +
                    "        WHEN rl.person_type = 'GUARD'    THEN COALESCE(g.shift, '—') " +
                    "        WHEN rl.person_type = 'PRISONER' THEN COALESCE(p.crime, '—') " +
                    "        ELSE NULL " +
                    "    END AS extra_info, " +

                    // Contact: guard phone OR prisoner danger level
                    "    CASE " +
                    "        WHEN rl.person_type = 'GUARD'    THEN COALESCE(g.phone_number, '—') " +
                    "        WHEN rl.person_type = 'PRISONER' THEN COALESCE(p.danger_level, '—') " +
                    "        ELSE NULL " +
                    "    END AS contact_info, " +

                    // Aadhar
                    "    CASE " +
                    "        WHEN rl.person_type = 'GUARD'    THEN COALESCE(g.aadhar_number, '—') " +
                    "        WHEN rl.person_type = 'PRISONER' THEN COALESCE(p.aadhar_number, '—') " +
                    "        ELSE NULL " +
                    "    END AS aadhar_number, " +

                    // Image path
                    "    CASE " +
                    "        WHEN rl.person_type = 'GUARD' " +
                    "            THEN CONCAT('python-face/photos/guards/', rl.person_id, '.jpg') " +
                    "        WHEN rl.person_type = 'PRISONER' " +
                    "            THEN CONCAT('python-face/photos/prisoners/', rl.person_id, '.jpg') " +
                    "        ELSE NULL " +
                    "    END AS image_path " +

                    "FROM recognition_logs rl " +
                    "LEFT JOIN guards    g ON rl.person_type = 'GUARD'    AND rl.person_id = g.guard_id " +
                    "LEFT JOIN prisoners p ON rl.person_type = 'PRISONER' AND rl.person_id = p.prisoner_id " +
                    "ORDER BY rl.detected_at DESC";

    public void save(RecognitionLog log) {
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            ps.setString(1, log.getPersonType());
            if (log.getPersonId() != null) ps.setInt(2, log.getPersonId());
            else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, log.getResult());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<RecognitionLog> findAll() {
        List<RecognitionLog> logs = new ArrayList<>();
        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                RecognitionLog log = new RecognitionLog();
                log.setPersonType(rs.getString("person_type"));
                log.setPersonId((Integer) rs.getObject("person_id"));
                log.setResult(rs.getString("result"));
                if (rs.getTimestamp("detected_at") != null)
                    log.setDetectedAt(rs.getTimestamp("detected_at").toLocalDateTime());
                log.setPersonName(rs.getString("person_name"));
                log.setDepartment(rs.getString("department"));
                log.setExtraInfo(rs.getString("extra_info"));    // shift / crime
                log.setContactInfo(rs.getString("contact_info")); // phone / danger level
                log.setAadharNumber(rs.getString("aadhar_number"));
                log.setImagePath(rs.getString("image_path"));
                logs.add(log);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logs;
    }
}
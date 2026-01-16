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

    public void save(RecognitionLog log) {

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {

            ps.setString(1, log.getPersonType());

            if (log.getPersonId() != null) {
                ps.setInt(2, log.getPersonId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }

            ps.setString(3, log.getResult());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public List<RecognitionLog> findAll() {

        List<RecognitionLog> logs = new ArrayList<>();

        String sql = "SELECT person_type, person_id, result, detected_at " +
                "FROM recognition_logs ORDER BY detected_at DESC";

        try (Connection con = DatabaseUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                RecognitionLog log = new RecognitionLog();
                log.setPersonType(rs.getString("person_type"));
                log.setPersonId((Integer) rs.getObject("person_id"));
                log.setResult(rs.getString("result"));
                log.setDetectedAt(rs.getTimestamp("detected_at").toLocalDateTime());
                logs.add(log);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return logs;
    }

}

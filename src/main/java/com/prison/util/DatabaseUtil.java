package com.prison.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseUtil {

    private static final String URL =
            "jdbc:mysql://127.0.01:3306/prison_face_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Rutik@1234";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

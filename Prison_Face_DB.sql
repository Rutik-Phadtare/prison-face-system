/*
--------------------------------------------------------------------
Database: prison_face_db
Description: Management system for Guards, Prisoners, and Face Recognition Logs.
Author: System Architect
Date: 2026-03-09
--------------------------------------------------------------------
*/

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

-- 1. Database Initialization
DROP DATABASE IF EXISTS prison_face_db;
CREATE DATABASE prison_face_db;
USE prison_face_db;

-- ---------------------------------------------------------
-- 2. Table: users
-- ---------------------------------------------------------
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN', 'CO_ADMIN') NOT NULL,
    display_name  VARCHAR(100) DEFAULT NULL,
    is_active     TINYINT(1)    DEFAULT 1,
    created_by    VARCHAR(50)   DEFAULT NULL,
    last_login    TIMESTAMP     DEFAULT NULL,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 3. Table: co_admin_login_logs
-- ---------------------------------------------------------
CREATE TABLE co_admin_login_logs (
    log_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    username     VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) DEFAULT NULL,
    login_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_at    TIMESTAMP DEFAULT NULL,
    session_mins INT DEFAULT NULL,
    ip_address   VARCHAR(45) DEFAULT 'localhost',
    status       ENUM('ACTIVE', 'LOGGED_OUT', 'TIMEOUT') DEFAULT 'ACTIVE',
    CONSTRAINT fk_cal_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_cal_user ON co_admin_login_logs(user_id);
CREATE INDEX idx_cal_login ON co_admin_login_logs(login_at);

-- ---------------------------------------------------------
-- 4. Table: guards
-- ---------------------------------------------------------
CREATE TABLE guards (
    guard_id      INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(100),
    designation   VARCHAR(100),
    shift         VARCHAR(20),
    joining_date  DATE,
    birth_date    DATE,
    age           INT,
    gender        VARCHAR(20),
    address       VARCHAR(255),
    phone_number  VARCHAR(15),
    email         VARCHAR(100),
    aadhar_number VARCHAR(20),
    batch_id      VARCHAR(20),
    transfer_from VARCHAR(100),
    salary        DOUBLE,
    status        VARCHAR(20) DEFAULT 'ACTIVE',
    description   TEXT,
    image_path    VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 5. Table: prisoners
-- ---------------------------------------------------------
CREATE TABLE prisoners (
    prisoner_id         INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    age                 INT DEFAULT 0,
    gender              VARCHAR(20),
    nationality         VARCHAR(100),
    blood_type          VARCHAR(10),
    height              VARCHAR(30),
    weight              VARCHAR(30),
    identification_marks TEXT,
    aadhar_number       VARCHAR(12),
    home_address        TEXT,
    
    -- Legal Details
    crime               VARCHAR(255),
    cell_no             VARCHAR(20),
    sentence_years      INT,
    sentence_start_date DATE,
    release_date        DATE,
    status              ENUM('IN_CUSTODY', 'RELEASED') DEFAULT 'IN_CUSTODY',
    danger_level        VARCHAR(20) DEFAULT 'LOW',
    behavior_rating     VARCHAR(20) DEFAULT 'GOOD',
    
    -- Contacts
    emergency_contact   VARCHAR(120),
    emergency_phone     VARCHAR(20),
    lawyer_name         VARCHAR(120),
    lawyer_phone        VARCHAR(20),
    
    -- Logs
    description         TEXT,
    incident_notes      TEXT,
    visitor_log         TEXT
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 6. Table: face_encodings
-- ---------------------------------------------------------
CREATE TABLE face_encodings (
    face_id       INT AUTO_INCREMENT PRIMARY KEY,
    person_type   ENUM('GUARD', 'PRISONER') NOT NULL,
    person_ref_id INT NOT NULL,
    encoding_file VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 7. Table: recognition_logs
-- ---------------------------------------------------------
CREATE TABLE recognition_logs (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    person_type VARCHAR(20),
    person_id   INT,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    result      VARCHAR(50)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- 8. Initial Seed Data
-- ---------------------------------------------------------
-- Default Admin Password (admin123 hashed)
INSERT INTO users (username, password_hash, role, display_name)
VALUES (
    'admin', 
    '$2a$10$m01/t9RmuSAsVkI1AsALQ.J9BZ3LLxeTNTtq2IXXUEatOcaBkPJBm', 
    'ADMIN', 
    'System Administrator'
);

INSERT INTO users (username, password_hash, role, display_name)
VALUES (
    'Rutik', 
    '$2a$10$m01/t9RmuSAsVkI1AsALQ.J9BZ3LLxeTNTtq2IXXUEatOcaBkPJBm', 
    'ADMIN', 
    'Rutik'
);

SHOW TABLES FROM prison_face_db;

USE prison_face_db;
SHOW TABLES;

SELECT table_name, table_type, engine 
FROM information_schema.tables 
WHERE table_schema = 'prison_face_db';

SELECT * FROM users;
SELECT * FROM co_admin_login_logs;
SELECT * FROM guards;
SELECT * FROM prisoners;
SELECT * FROM face_encodings;
SELECT * FROM recognition_logs;

SELECT 
    table_name,
    table_rows AS approx_row_count
FROM information_schema.tables
WHERE table_schema = 'prison_face_db';
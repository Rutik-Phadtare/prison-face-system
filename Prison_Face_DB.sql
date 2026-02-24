DROP DATABASE prison_face_db;   
CREATE DATABASE prison_face_db;
USE prison_face_db;
-- hello

-- USERS TABLE
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'CO_ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1. Update Users Table
-- Note: MySQL does not support 'IF NOT EXISTS' within ALTER TABLE.
-- If these columns already exist, this statement will throw an error.
ALTER TABLE users 
    ADD COLUMN display_name VARCHAR(100)  DEFAULT NULL,
    ADD COLUMN is_active    TINYINT(1)   DEFAULT 1,
    ADD COLUMN created_by  VARCHAR(50)   DEFAULT NULL,
    ADD COLUMN last_login  TIMESTAMP     DEFAULT NULL;

-- 2. Co-Admin login activity log
CREATE TABLE IF NOT EXISTS co_admin_login_logs (
    log_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT          NOT NULL,
    username     VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) DEFAULT NULL,
    login_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    logout_at    TIMESTAMP    DEFAULT NULL,
    session_mins INT          DEFAULT NULL,
    ip_address   VARCHAR(45)  DEFAULT 'localhost',
    status       ENUM('ACTIVE','LOGGED_OUT','TIMEOUT') DEFAULT 'ACTIVE',
    CONSTRAINT fk_cal_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. Create Indexes
-- In MySQL, indexes are usually created during CREATE TABLE or via CREATE INDEX (without IF NOT EXISTS).
CREATE INDEX idx_cal_user  ON co_admin_login_logs(user_id);
CREATE INDEX idx_cal_login ON co_admin_login_logs(login_at);

-- GUARDS TABLE
drop table guards;
CREATE TABLE guards (
    guard_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    role VARCHAR(100),
    shift VARCHAR(20),
    joining_date DATE,
    status VARCHAR(20),
    description TEXT,
    image_path VARCHAR(255)
);
ALTER TABLE guards ADD COLUMN designation VARCHAR(100);
select guard_id,name from guards;
ALTER TABLE guards ADD COLUMN age INT;
ALTER TABLE guards ADD COLUMN birthDate DATE;
ALTER TABLE guards ADD COLUMN address VARCHAR(255);
ALTER TABLE guards ADD COLUMN gender VARCHAR(20);
ALTER TABLE guards ADD COLUMN transferFrom VARCHAR(100);
ALTER TABLE guards ADD COLUMN salary DOUBLE;

ALTER TABLE guards 
ADD COLUMN aadhar_number VARCHAR(20),
ADD COLUMN phone_number VARCHAR(15),
ADD COLUMN batch_id VARCHAR(20),
ADD COLUMN email VARCHAR(100);

ALTER TABLE guards CHANGE COLUMN birthDate birth_date DATE;
ALTER TABLE guards CHANGE COLUMN transferFrom transfer_from VARCHAR(100);

-- PRISONERS TABLE
drop table prisoners;
CREATE TABLE prisoners (
    prisoner_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    crime VARCHAR(255),
    cell_no VARCHAR(20),
    sentence_years INT,
    status ENUM('IN_CUSTODY', 'RELEASED') DEFAULT 'IN_CUSTODY'
);
ALTER TABLE prisoners
ADD COLUMN description TEXT,
ADD COLUMN release_date DATE;
ALTER TABLE prisoners
ADD COLUMN sentence_start_date DATE;
ALTER TABLE prisoners 
DROP COLUMN sentence_start;
ALTER TABLE prisoners 
    ADD COLUMN age                  INT           DEFAULT 0,
    ADD COLUMN gender               VARCHAR(20)   DEFAULT NULL,
    ADD COLUMN nationality          VARCHAR(100)  DEFAULT NULL,
    ADD COLUMN home_address         TEXT          DEFAULT NULL,
    ADD COLUMN aadhar_number        VARCHAR(12)   DEFAULT NULL,
    ADD COLUMN blood_type           VARCHAR(10)   DEFAULT NULL,
    ADD COLUMN height               VARCHAR(30)   DEFAULT NULL,
    ADD COLUMN weight               VARCHAR(30)   DEFAULT NULL,
    ADD COLUMN identification_marks TEXT          DEFAULT NULL,
    -- Contact & Legal
    ADD COLUMN emergency_contact    VARCHAR(120)  DEFAULT NULL,
    ADD COLUMN emergency_phone      VARCHAR(20)   DEFAULT NULL,
    ADD COLUMN lawyer_name          VARCHAR(120)  DEFAULT NULL,
    ADD COLUMN lawyer_phone         VARCHAR(20)   DEFAULT NULL,
    -- Classification
    ADD COLUMN danger_level         VARCHAR(20)   DEFAULT 'LOW',
    ADD COLUMN behavior_rating      VARCHAR(20)   DEFAULT 'GOOD',
    -- Behavioral Logs
    ADD COLUMN incident_notes       TEXT          DEFAULT NULL,
    ADD COLUMN visitor_log          TEXT          DEFAULT NULL;

-- FACE ENCODING METADATA
drop table face_encodings;
CREATE TABLE face_encodings (
    face_id INT AUTO_INCREMENT PRIMARY KEY,
    person_type ENUM('GUARD', 'PRISONER') NOT NULL,
    person_ref_id INT NOT NULL,
    encoding_file VARCHAR(255) NOT NULL
);

-- RECOGNITION LOGS
drop table recognition_logs;
CREATE TABLE recognition_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    person_type VARCHAR(20),
    person_id INT,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    result VARCHAR(50)
);
INSERT INTO users (username, password_hash, role)
VALUES ('admi', 'admin123', 'ADMN');
DELETE FROM users 
WHERE username = 'admin' AND role = 'ADMIN';
INSERT INTO users (username, password_hash, role)
VALUES (
    'admin',
    '$2a$10$m01/t9RmuSAsVkI1AsALQ.J9BZ3LLxeTNTtq2IXXUEatOcaBkPJBm',
    'ADMIN'
);
INSERT INTO users (username, password_hash, role)
VALUES (
    'Rutik',
    '$2a$10$m01/t9RmuSAsVkI1AsALQ.J9BZ3LLxeTNTtq2IXXUEatOcaBkPJBm',
    'ADMIN'
);
SELECT user_id, username,password_hash, role FROM users;
DELETE FROM users;
SELECT * FROM recognition_logs ORDER BY detected_at DESC;
select prisoner_id ,name,crime,cell_no,status,release_date,description,sentence_start_date,sentence_years from prisoners;
SELECT COUNT(*) FROM prisoners WHERE status = 'IN_CUSTODY';
SELECT COUNT(*) FROM guards WHERE status = 'ACTIVE';
SELECT COUNT(*) FROM guards;

INSERT INTO prisoners
(name, crime, cell_no, sentence_years, status, description, release_date)
VALUES (?, ?, ?, ?, ?, ?, ?);

update prisoners
SET release_date = DATE_ADD(CURDATE(), INTERVAL sentence_years YEAR)
WHERE release_date IS NULL;

SET SQL_SAFE_UPDATES = 0;
UPDATE prisoners
SET sentence_start_date = CURDATE(),
    release_date = DATE_ADD(CURDATE(), INTERVAL sentence_years YEAR)
WHERE sentence_start_date IS NULL;


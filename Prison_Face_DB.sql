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

-- GUARDS TABLE
drop table guards;
CREATE TABLE guards (
    guard_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    designation VARCHAR(50),
    shift VARCHAR(20),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'
);

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
ADD COLUMN sentence_start DATE;

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
select prisoner_id ,name,crime,cell_no,status,release_date,description,sentence_start,sentence_years from prisoners;
SELECT COUNT(*) FROM prisoners WHERE status = 'IN_CUSTODY';
SELECT COUNT(*) FROM guards WHERE status = 'ACTIVE';

INSERT INTO prisoners
(name, crime, cell_no, sentence_years, status, description, release_date)
VALUES (?, ?, ?, ?, ?, ?, ?);

update prisoners
SET release_date = DATE_ADD(CURDATE(), INTERVAL sentence_years YEAR)
WHERE release_date IS NULL;

SET SQL_SAFE_UPDATES = 0;


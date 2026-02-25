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

INSERT INTO guards (
    name, role, shift, joining_date, status, description, image_path, 
    designation, age, birth_date, address, gender, transfer_from, 
    salary, aadhar_number, phone_number, batch_id, email
) VALUES 
('Rajesh Kumar', 'Security', 'Day', '2022-01-15', 'ACTIVE', 'Lead floor supervisor for Block A.', '/images/guards/g1.jpg', 'Head Guard', 34, '1992-05-12', '123, MG Road, Mumbai', 'Male', 'Pune Central', 45000.00, '123456789012', '9876543210', 'B-2022-01', 'rajesh.k@prison.gov'),
('Sneha Patil', 'Surveillance', 'Night', '2023-03-10', 'ACTIVE', 'CCTV monitoring expert.', '/images/guards/g2.jpg', 'Senior Watchman', 29, '1997-08-22', '45, Station Road, Thane', 'Female', 'Direct Recruitment', 38000.00, '223456789013', '9876543211', 'B-2023-05', 'sneha.p@prison.gov'),
('Amit Sharma', 'Patrol', 'Evening', '2021-11-05', 'ACTIVE', 'Field patrol for perimeter fence.', '/images/guards/g3.jpg', 'Guard Grade I', 31, '1995-12-01', 'Flat 402, Sunshine Apts, Nashik', 'Male', 'Nagpur District', 40000.00, '323456789014', '9876543212', 'B-2021-12', 'amit.s@prison.gov'),
('Vikram Singh', 'Security', 'Night', '2020-06-20', 'ACTIVE', 'Expert in riot control tactics.', '/images/guards/g4.jpg', 'Sergeant', 42, '1984-02-14', 'Village Post, Kolhapur', 'Male', 'Army Reserve', 52000.00, '423456789015', '9876543213', 'B-2020-02', 'vikram.s@prison.gov'),
('Priya Verma', 'Medical Wing', 'Day', '2024-01-05', 'ACTIVE', 'Guard assigned to infirmary security.', '/images/guards/g5.jpg', 'Security Officer', 27, '1999-10-30', 'B-22, Civil Lines, Nagpur', 'Female', 'Direct Recruitment', 36000.00, '523456789016', '9876543214', 'B-2024-01', 'priya.v@prison.gov'),
('Arjun Das', 'Security', 'Day', '2019-09-12', 'ACTIVE', 'Responsible for visitor screening.', '/images/guards/g6.jpg', 'Inspector', 38, '1988-04-18', '78, Old Town, Aurangabad', 'Male', 'Solapur Jail', 48000.00, '623456789017', '9876543215', 'B-2019-08', 'arjun.d@prison.gov'),
('Meera Reddy', 'Logistics', 'Evening', '2022-07-15', 'ACTIVE', 'Oversees supply deliveries to kitchen.', '/images/guards/g7.jpg', 'Guard Grade II', 30, '1996-01-25', 'Green View, Navi Mumbai', 'Female', 'Direct Recruitment', 37500.00, '723456789018', '9876543216', 'B-2022-04', 'meera.r@prison.gov'),
('Sanjay Gupta', 'Patrol', 'Night', '2021-02-18', 'ACTIVE', 'Night shift wall perimeter duty.', '/images/guards/g8.jpg', 'Guard Grade I', 33, '1993-07-07', 'MIG Colony, Ratnagiri', 'Male', 'Pune Central', 41000.00, '823456789019', '9876543217', 'B-2021-03', 'sanjay.g@prison.gov'),
('Rahul Deshmukh', 'Security', 'Day', '2020-12-01', 'ACTIVE', 'Handles high-security cell block.', '/images/guards/g9.jpg', 'Head Guard', 36, '1990-11-11', 'Sector 10, Nerul', 'Male', 'Yerwada Jail', 46000.00, '923456789020', '9876543218', 'B-2020-10', 'rahul.d@prison.gov'),
('Anjali Menon', 'Surveillance', 'Day', '2023-08-20', 'ACTIVE', 'Digital evidence management.', '/images/guards/g10.jpg', 'Tech Specialist', 28, '1998-03-05', 'Palm Grove, Kalyan', 'Female', 'Direct Recruitment', 39000.00, '103456789021', '9876543219', 'B-2023-09', 'anjali.m@prison.gov'),
('Karan Malhotra', 'Security', 'Evening', '2021-05-30', 'ACTIVE', 'Entry point verification officer.', '/images/guards/g11.jpg', 'Security Officer', 32, '1994-06-15', 'Hill Crest, Satara', 'Male', 'Mumbai Police', 44000.00, '113456789022', '9876543220', 'B-2021-06', 'karan.m@prison.gov'),
('Deepak Chawla', 'Armory', 'Day', '2018-03-14', 'ACTIVE', 'Responsible for weapon maintenance.', '/images/guards/g12.jpg', 'Armorer', 45, '1981-09-09', 'Cantt Area, Ahmednagar', 'Male', 'State Reserve', 55000.00, '123456789023', '9876543221', 'B-2018-02', 'deepak.c@prison.gov'),
('Sunita Rao', 'Security', 'Night', '2024-02-01', 'ACTIVE', 'Assigned to Female Ward.', '/images/guards/g13.jpg', 'Guard Grade II', 26, '2000-12-25', 'Laxmi Nagar, Jalgaon', 'Female', 'Direct Recruitment', 35000.00, '133456789024', '9876543222', 'B-2024-02', 'sunita.r@prison.gov'),
('Manoj Tiwari', 'Patrol', 'Day', '2022-10-10', 'ACTIVE', 'Yard monitoring during lunch.', '/images/guards/g14.jpg', 'Guard Grade I', 35, '1991-04-04', 'Sai Sadan, Borivali', 'Male', 'Amravati Jail', 40500.00, '143456789025', '9876543223', 'B-2022-09', 'manoj.t@prison.gov'),
('Rohit Bansal', 'Logistics', 'Evening', '2023-06-18', 'ACTIVE', 'Inventory manager for equipment.', '/images/guards/g15.jpg', 'Officer', 31, '1995-02-28', 'Dombivli West, Thane', 'Male', 'Direct Recruitment', 38500.00, '153456789026', '9876543224', 'B-2023-06', 'rohit.b@prison.gov'),
('Pooja Hegde', 'Medical Wing', 'Night', '2021-09-25', 'ACTIVE', 'Shift lead for hospital security.', '/images/guards/g16.jpg', 'Senior Guard', 33, '1993-01-12', 'Bandra West, Mumbai', 'Female', 'Nagpur District', 42000.00, '163456789027', '9876543225', 'B-2021-10', 'pooja.h@prison.gov'),
('Vijay Kulkarni', 'Security', 'Day', '2017-11-11', 'ACTIVE', 'Senior disciplinary officer.', '/images/guards/g17.jpg', 'Chief Guard', 48, '1978-08-08', 'Sadashiv Peth, Pune', 'Male', 'Yerwada Jail', 60000.00, '173456789028', '9876543226', 'B-2017-05', 'vijay.k@prison.gov'),
('Suresh Raina', 'Patrol', 'Night', '2023-11-20', 'ACTIVE', 'Perimeter vehicle patrol.', '/images/guards/g18.jpg', 'Guard Grade II', 27, '1999-05-19', 'Gokuldham, Goregaon', 'Male', 'Direct Recruitment', 36000.00, '183456789029', '9876543227', 'B-2023-11', 'suresh.r@prison.gov'),
('Kavita Singh', 'Security', 'Day', '2022-04-14', 'ACTIVE', 'Visitor lounge coordinator.', '/images/guards/g19.jpg', 'Senior Watchman', 32, '1994-07-30', 'Model Town, Panvel', 'Female', 'Aurangabad Jail', 41000.00, '193456789030', '9876543228', 'B-2022-03', 'kavita.s@prison.gov'),
('Abhishek Jain', 'Surveillance', 'Evening', '2020-01-10', 'ACTIVE', 'Data backup and IT support.', '/images/guards/g20.jpg', 'Tech Specialist', 35, '1991-02-10', 'Viman Nagar, Pune', 'Male', 'Direct Recruitment', 43000.00, '203456789031', '9876543229', 'B-2020-01', 'abhishek.j@prison.gov');


INSERT INTO prisoners (
    name, crime, cell_no, sentence_years, status, description, 
    sentence_start_date, release_date, age, gender, nationality, 
    home_address, aadhar_number, blood_type, height, weight, 
    identification_marks, emergency_contact, emergency_phone, 
    lawyer_name, lawyer_phone, danger_level, behavior_rating
) VALUES 
('Deepak Hooda', 'Trespassing', '15', 1, 'IN_CUSTODY', 'Illegal entry into govt property.', '2025-01-10', '2026-01-10', 28, 'Male', 'Indian', 'Rohtak, Haryana', '112211221122', 'A+', '180cm', '78kg', 'Scar on right chin', 'Rajesh Hooda', '9812000001', 'Adv. Malik', '9812000002', 'LOW', 'GOOD'),
('Shikhar Dhawan', 'Tax Fraud', '22', 4, 'IN_CUSTODY', 'Major GST evasion case.', '2023-05-12', '2027-05-12', 38, 'Male', 'Indian', 'Lajpat Nagar, Delhi', '223322332233', 'B+', '175cm', '74kg', 'Tattoo on left bicep', 'Ayesha', '9811000003', 'Adv. Bhullar', '9811000004', 'LOW', 'EXCELLENT'),
('Mohammed Shami', 'Domestic Violence', '45', 3, 'IN_CUSTODY', 'Pending further investigation.', '2024-08-20', '2027-08-20', 33, 'Male', 'Indian', 'Amroha, UP', '334433443344', 'O+', '173cm', '72kg', 'None', 'Hasin', '9813000005', 'Adv. Siddiqui', '9813000006', 'MEDIUM', 'FAIR'),
('Yuzvendra Chahal', 'Pickpocketing', '09', 2, 'IN_CUSTODY', 'Caught in railway station.', '2025-02-01', '2027-02-01', 31, 'Male', 'Indian', 'Jind, Haryana', '445544554455', 'AB-', '165cm', '55kg', 'Very thin build', 'Dhanashree', '9814000007', 'Adv. Verma', '9814000008', 'LOW', 'GOOD'),
('Kuldeep Yadav', 'Cyber Stalking', '31', 2, 'IN_CUSTODY', 'Harassment via social media.', '2024-11-15', '2026-11-15', 29, 'Male', 'Indian', 'Kanpur, UP', '556655665566', 'A-', '168cm', '64kg', 'Birthmark on left hand', 'Ram Singh', '9815000009', 'Adv. Tiwari', '9815000010', 'LOW', 'GOOD'),
('Bhuvneshwar Kumar', 'Illegal Mining', '67', 6, 'IN_CUSTODY', 'Sand mining syndicate.', '2021-03-10', '2027-03-10', 34, 'Male', 'Indian', 'Meerut, UP', '667766776677', 'O-', '172cm', '68kg', 'None', 'Nupur', '9816000011', 'Adv. Rastogi', '9816000012', 'MEDIUM', 'EXCELLENT'),
('Ishan Kishan', 'Public Vandalism', '12', 1, 'IN_CUSTODY', 'Damaging public transport.', '2025-01-30', '2026-01-30', 25, 'Male', 'Indian', 'Patna, Bihar', '778877887788', 'B-', '167cm', '60kg', 'Mole on neck', 'Pranav', '9817000013', 'Adv. Singh', '9817000014', 'LOW', 'FAIR'),
('Shreyas Iyer', 'Identity Theft', '88', 5, 'IN_CUSTODY', 'Using fake passports.', '2022-10-05', '2027-10-05', 29, 'Male', 'Indian', 'Worli, Mumbai', '889988998899', 'AB+', '178cm', '70kg', 'None', 'Santosh', '9818000015', 'Adv. Hegde', '9818000016', 'LOW', 'GOOD'),
('Suryakumar Yadav', 'Assault on Officer', '99', 7, 'IN_CUSTODY', 'Physical altercation with police.', '2021-06-18', '2028-06-18', 33, 'Male', 'Indian', 'Chembur, Mumbai', '990099009900', 'O+', '175cm', '75kg', 'Tattoos on both arms', 'Devika', '9819000017', 'Adv. Sawant', '9819000018', 'HIGH', 'POOR'),
('Sanju Samson', 'Liquor Smuggling', '04', 3, 'IN_CUSTODY', 'Transporting illicit alcohol.', '2024-04-12', '2027-04-12', 29, 'Male', 'Indian', 'Vizhinjam, Kerala', '101010101010', 'A+', '174cm', '71kg', 'None', 'Charulatha', '9820000019', 'Adv. Nair', '9820000020', 'MEDIUM', 'GOOD'),
('Washington Sundar', 'Embezzlement', '52', 4, 'IN_CUSTODY', 'NGO fund misappropriation.', '2023-09-01', '2027-09-01', 24, 'Male', 'Indian', 'Chennai, TN', '121314151617', 'B+', '185cm', '75kg', 'Tall and lanky', 'Mani', '9821000021', 'Adv. Ram', '9821000022', 'LOW', 'EXCELLENT'),
('Axar Patel', 'Land Grabbing', '73', 5, 'IN_CUSTODY', 'Illegal occupation of farm land.', '2022-12-20', '2027-12-20', 30, 'Male', 'Indian', 'Anand, Gujarat', '181920212223', 'O-', '182cm', '73kg', 'None', 'Meha', '9822000023', 'Adv. Joshi', '9822000024', 'MEDIUM', 'GOOD'),
('Ravi Bishnoi', 'Juvenile Delinquency', '01', 2, 'IN_CUSTODY', 'Repeated minor offenses.', '2024-07-05', '2026-07-05', 23, 'Male', 'Indian', 'Jodhpur, Rajasthan', '242526272829', 'A-', '170cm', '62kg', 'Freckles on face', 'Mangilal', '9823000025', 'Adv. Gehlot', '9823000026', 'LOW', 'GOOD'),
('Arshdeep Singh', 'Noise Pollution', '05', 1, 'IN_CUSTODY', 'Illegal use of loudspeakers.', '2025-02-10', '2026-02-10', 25, 'Male', 'Indian', 'Kharar, Punjab', '303132333435', 'B+', '191cm', '80kg', 'None', 'Darshan', '9824000027', 'Adv. Dhillon', '9824000028', 'LOW', 'GOOD'),
('Umran Malik', 'Speeding/Accident', '10', 2, 'IN_CUSTODY', 'High-speed hit and run.', '2024-05-30', '2026-05-30', 24, 'Male', 'Indian', 'Gujjar Nagar, Jammu', '363738394041', 'O+', '178cm', '76kg', 'Surgical scar on knee', 'Abdul', '9825000029', 'Adv. Baig', '9825000030', 'MEDIUM', 'FAIR'),
('Prithvi Shaw', 'Disorderly Conduct', '19', 1, 'IN_CUSTODY', 'Bar fight involvement.', '2025-01-22', '2026-01-22', 24, 'Male', 'Indian', 'Virar, Mumbai', '424344454647', 'A+', '165cm', '68kg', 'None', 'Pankaj', '9826000031', 'Adv. Parab', '9826000032', 'LOW', 'POOR'),
('Shubman Gill', 'IP Theft', '55', 3, 'IN_CUSTODY', 'Stealing proprietary code.', '2024-02-14', '2027-02-14', 24, 'Male', 'Indian', 'Fazilka, Punjab', '484950515253', 'AB+', '178cm', '72kg', 'None', 'Lakhwinder', '9827000033', 'Adv. Sidhu', '9827000034', 'LOW', 'EXCELLENT'),
('Navdeep Saini', 'Rash Driving', '81', 2, 'IN_CUSTODY', 'Endangering public life.', '2024-09-10', '2026-09-10', 31, 'Male', 'Indian', 'Karnal, Haryana', '545556575859', 'B-', '178cm', '74kg', 'None', 'Amarjeet', '9828000035', 'Adv. Chautala', '9828000036', 'LOW', 'GOOD'),
('Deepak Chahar', 'Animal Poaching', '92', 5, 'IN_CUSTODY', 'Hunting protected species.', '2023-01-05', '2028-01-05', 31, 'Male', 'Indian', 'Agra, UP', '606162636465', 'O+', '176cm', '73kg', 'None', 'Lokendra', '9829000037', 'Adv. Yadav', '9829000038', 'MEDIUM', 'GOOD'),
('Yashasvi Jaiswal', 'Vagrancy', '02', 1, 'IN_CUSTODY', 'Unauthorized camping in park.', '2025-02-20', '2026-02-20', 22, 'Male', 'Indian', 'Bhadohi, UP', '666768697071', 'A+', '170cm', '65kg', 'None', 'Bhupendra', '9830000039', 'Adv. Pathak', '9830000040', 'LOW', 'EXCELLENT');

-- 1. First, clear any mixed-up logs
UPDATE prisoners SET incident_notes = 'Routine behavior.', visitor_log = 'No recent visitors.';

-- 2. Bulk Update with Unique, logical data
UPDATE prisoners SET 
    incident_notes = 'Participates in the prison garden project. Very disciplined.',
    visitor_log = 'Weekly: Father (Lakhwinder); Monthly: Sister (Shahneel).'
WHERE name = 'Shubman Gill';

UPDATE prisoners SET 
    incident_notes = 'High risk. Attempted to smuggle a mobile phone in Cell 99.',
    visitor_log = 'DENIED: Only Lawyer (Adv. Sawant) permitted.'
WHERE name = 'Suryakumar Yadav';

UPDATE prisoners SET 
    incident_notes = 'Leader of the prison yoga group. Excellent influence.',
    visitor_log = '2025-02-14: Wife (Sanjana); 2025-02-21: Child.'
WHERE name = 'Jasprit Bumrah';

UPDATE prisoners SET 
    incident_notes = 'Currently in physiotherapy for a leg injury.',
    visitor_log = '2025-02-10: Mother (Saroj); 2025-02-18: Medical Team.'
WHERE name = 'Rishabh Pant';

UPDATE prisoners SET 
    incident_notes = 'Frequent disputes over canteen coupons. Needs monitoring.',
    visitor_log = '2025-01-30: Brother (Pranav); 2025-02-10: Adv. Singh.'
WHERE name = 'Ishan Kishan';

UPDATE prisoners SET 
    incident_notes = 'Assists in the prison library. Highly literate.',
    visitor_log = '2025-02-05: Mother (Lakshmi Iyer); 2025-02-15: Adv. Mani.'
WHERE name = 'Rohan Iyer';

-- 3. Logical "Catch-all" for the rest using their specific Emergency Contacts
UPDATE prisoners 
SET visitor_log = CONCAT('Last visited by: ', emergency_contact, ' (Emergency Contact)')
WHERE visitor_log = 'No recent visitors.';

INSERT INTO prisoners (
    name, crime, cell_no, sentence_years, status, description, 
    sentence_start_date, release_date, age, gender, nationality, 
    home_address, aadhar_number, blood_type, height, weight, 
    identification_marks, emergency_contact, emergency_phone, 
    lawyer_name, lawyer_phone, danger_level, behavior_rating
) VALUES 
('Virat Kohli', 'Aggravated Assault', '18', 5, 'IN_CUSTODY', 'Physical altercation in public space.', '2023-11-05', '2028-11-05', 35, 'Male', 'Indian', 'Gurugram, Haryana', '776655443322', 'B+', '175cm', '76kg', 'Tattoo on left forearm', 'Anushka', '9811001818', 'Adv. Salve', '9811001919', 'MEDIUM', 'FAIR'),
('Rohit Sharma', 'White Collar Fraud', '45', 4, 'IN_CUSTODY', 'Misappropriation of corporate funds.', '2024-01-20', '2028-01-20', 36, 'Male', 'Indian', 'Worli, Mumbai', '112233998877', 'O+', '173cm', '80kg', 'Mole on right cheek', 'Ritika', '9822004545', 'Adv. Singh', '9822004646', 'LOW', 'EXCELLENT'),
('Lokesh Rahul', 'Copyright Infringement', '01', 2, 'IN_CUSTODY', 'Operating illegal streaming servers.', '2024-06-15', '2026-06-15', 31, 'Male', 'Indian', 'Bengaluru, Karnataka', '445566778811', 'A-', '180cm', '75kg', 'None', 'Athiya', '9833000101', 'Adv. Shetty', '9833000202', 'LOW', 'GOOD'),
('Ravindra Jadeja', 'Illegal Possession of Wildlife', '08', 3, 'IN_CUSTODY', 'Keeping protected species without permits.', '2024-03-10', '2027-03-10', 35, 'Male', 'Indian', 'Jamnagar, Gujarat', '998877665544', 'AB+', '170cm', '68kg', 'None', 'Riva', '9844000808', 'Adv. Adani', '9844000909', 'LOW', 'GOOD'),
('Dinesh Karthik', 'Breach of Peace', '21', 1, 'IN_CUSTODY', 'Inciting a riot during a protest.', '2025-02-01', '2026-02-01', 38, 'Male', 'Indian', 'Chennai, Tamil Nadu', '332211665544', 'O-', '170cm', '70kg', 'Stitch mark on forehead', 'Dipika', '9855002121', 'Adv. Mani', '9855002222', 'LOW', 'EXCELLENT'),
('Ravichandran Ashwin', 'Data Theft', '99', 4, 'IN_CUSTODY', 'Exfiltration of government server data.', '2023-12-12', '2027-12-12', 37, 'Male', 'Indian', 'West Mambalam, Chennai', '667788223344', 'B-', '188cm', '82kg', 'Wears spectacles', 'Prithi', '9866009999', 'Adv. Iyer', '9866009900', 'LOW', 'EXCELLENT'),
('Cheteshwar Pujara', 'Adulteration', '25', 5, 'IN_CUSTODY', 'Supplying sub-standard construction material.', '2022-05-20', '2027-05-20', 36, 'Male', 'Indian', 'Rajkot, Gujarat', '114477885522', 'A+', '178cm', '78kg', 'None', 'Arvind', '9877002525', 'Adv. Shah', '9877002626', 'LOW', 'GOOD'),
('Ajinkya Rahane', 'Document Forgery', '03', 3, 'IN_CUSTODY', 'Forging property sale deeds.', '2024-02-28', '2027-02-28', 35, 'Male', 'Indian', 'Dombivli, Maharashtra', '225588774411', 'O+', '168cm', '65kg', 'None', 'Radhika', '9888000303', 'Adv. Patil', '9888000404', 'LOW', 'EXCELLENT'),
('Suryakumar Yadav', 'Culpable Homicide', '63', 10, 'IN_CUSTODY', 'Death caused by extreme negligence.', '2020-08-15', '2030-08-15', 33, 'Male', 'Indian', 'Chembur, Mumbai', '556644332211', 'B+', '175cm', '74kg', 'Tattoo on right shoulder', 'Devika', '9899006363', 'Adv. Sawant', '9899006464', 'HIGH', 'FAIR'),
('Varun Chakravarthy', 'Stock Manipulation', '29', 6, 'IN_CUSTODY', 'Pump and dump scheme in small-cap stocks.', '2021-11-11', '2027-11-11', 32, 'Male', 'Indian', 'Bidar, Karnataka', '991122334455', 'AB-', '178cm', '72kg', 'None', 'Neha', '9911002929', 'Adv. Reddy', '9911003030', 'LOW', 'GOOD'),
('Sanju Samson', 'Visa Fraud', '14', 3, 'IN_CUSTODY', 'Running a fake travel consultancy.', '2024-05-01', '2027-05-01', 29, 'Male', 'Indian', 'Trivandrum, Kerala', '110022334455', 'A+', '174cm', '73kg', 'Birthmark on neck', 'Charulatha', '9922001414', 'Adv. Nair', '9922001515', 'MEDIUM', 'GOOD'),
('Mayank Agarwal', 'Embezzlement', '16', 4, 'IN_CUSTODY', 'Misuse of temple trust funds.', '2023-08-14', '2027-08-14', 33, 'Male', 'Indian', 'Indiranagar, Bengaluru', '223344556600', 'B+', '172cm', '74kg', 'Small scar on chin', 'Aashita', '9933001616', 'Adv. Bhat', '9933001717', 'LOW', 'EXCELLENT'),
('Prithvi Shaw', 'Narcotics Trafficking', '10', 12, 'IN_CUSTODY', 'Caught with commercial quantity of MDMA.', '2019-10-10', '2031-10-10', 24, 'Male', 'Indian', 'Virar, Mumbai', '445566112233', 'O-', '165cm', '67kg', 'None', 'Siddharth', '9944001010', 'Adv. Parab', '9944001111', 'HIGH', 'POOR'),
('Ishan Kishan', 'Cyber Terrorism', '32', 8, 'IN_CUSTODY', 'Attacking power grid infrastructure.', '2022-01-05', '2030-01-05', 25, 'Male', 'Indian', 'Patna, Bihar', '556677889944', 'A-', '168cm', '64kg', 'Mole on left ear', 'Pranav', '9955003232', 'Adv. Sinha', '9955003333', 'HIGH', 'FAIR'),
('Mohammed Siraj', 'Illegal Arms Dealing', '73', 7, 'IN_CUSTODY', 'Selling unlicenced country-made pistols.', '2022-09-15', '2029-09-15', 29, 'Male', 'Indian', 'Toli Chowki, Hyderabad', '990011882277', 'B+', '178cm', '70kg', 'Scar on right knee', 'Shabana', '9966007373', 'Adv. Hyder', '9966007474', 'HIGH', 'GOOD'),
('Shreyas Iyer', 'Identity Theft', '41', 3, 'IN_CUSTODY', 'Stole bank credentials of senior citizens.', '2024-07-22', '2027-07-22', 29, 'Male', 'Indian', 'Worli, Mumbai', '887766112233', 'AB+', '178cm', '75kg', 'None', 'Santosh', '9977004141', 'Adv. Hegde', '9977004242', 'LOW', 'GOOD'),
('Deepak Hooda', 'Land Encroachment', '56', 5, 'IN_CUSTODY', 'Illegal construction on forest land.', '2021-04-10', '2026-04-10', 28, 'Male', 'Indian', 'Rohtak, Haryana', '115599443322', 'A+', '180cm', '78kg', 'Scar on right chin', 'Rajesh', '9988005656', 'Adv. Malik', '9988005757', 'MEDIUM', 'FAIR'),
('Shardul Thakur', 'Tax Evasion', '54', 2, 'IN_CUSTODY', 'Concealing overseas assets.', '2024-11-20', '2026-11-20', 32, 'Male', 'Indian', 'Palghar, Maharashtra', '774411223366', 'O+', '175cm', '82kg', 'Birthmark on back', 'Narendra', '9999005454', 'Adv. More', '9999005555', 'LOW', 'GOOD'),
('Washington Sundar', 'Bribery', '52', 3, 'IN_CUSTODY', 'Offering bribes to municipal officials.', '2024-03-05', '2027-03-05', 24, 'Male', 'Indian', 'Chennai, Tamil Nadu', '121234345656', 'B+', '185cm', '76kg', 'None', 'Mani', '9811223344', 'Adv. Ram', '9811223355', 'LOW', 'EXCELLENT'),
('Axar Patel', 'Assault on Public Servant', '77', 4, 'IN_CUSTODY', 'Attacked a traffic warden.', '2023-05-15', '2027-05-15', 30, 'Male', 'Indian', 'Anand, Gujarat', '998811220033', 'O-', '182cm', '75kg', 'None', 'Meha', '9811007777', 'Adv. Joshi', '9811007788', 'MEDIUM', 'GOOD');


-- Batch Update for the 20 newly added prisoners
UPDATE prisoners SET 
    incident_notes = 'Maintains a highly disciplined fitness routine in the yard.\nNo verbal or physical altercations recorded since admission.',
    visitor_log = '2025-11-10: Wife (Anushka) for 30 mins.\n2025-12-05: Legal counsel (Adv. Salve) regarding appeal.'
WHERE name = 'Virat Kohli';

UPDATE prisoners SET 
    incident_notes = 'Assists in the prison kitchen management and logistics.\nShows leadership qualities and helps resolve minor inmate disputes.',
    visitor_log = '2025-01-25: Wife (Ritika) with daughter.\n2025-02-10: Brother regarding family business matters.'
WHERE name = 'Rohit Sharma';

UPDATE prisoners SET 
    incident_notes = 'Spends significant time in the computer lab under supervision.\nAssists staff with digitizing old paper records.',
    visitor_log = '2025-06-20: Father for a scheduled 15-minute call.\n2025-08-15: Wife (Athiya) during Independence Day visit.'
WHERE name = 'Lokesh Rahul';

UPDATE prisoners SET 
    incident_notes = 'Active participant in the prison vocational training (woodworking).\nMaintains a clean cell and follows all roll-call protocols.',
    visitor_log = '2024-04-12: Wife (Riva) for a private consultation.\n2024-10-10: Monthly family visit (4 members present).'
WHERE name = 'Ravindra Jadeja';

UPDATE prisoners SET 
    incident_notes = 'Very vocal during group sessions; occasionally needs a warning for noise.\nOverall cooperative with the wardens during evening lockup.',
    visitor_log = '2025-02-15: Wife (Dipika) brought authorized reading material.\n2025-03-01: Attorney (Adv. Mani) regarding bail hearing.'
WHERE name = 'Dinesh Karthik';

UPDATE prisoners SET 
    incident_notes = 'Extremely high intelligence; currently restricted from unsupervised IT access.\nOrganizes chess tournaments for other inmates during recreation.',
    visitor_log = '2024-01-10: Wife (Prithi) for an emotional support visit.\n2024-05-22: Academic colleague for research-related query.'
WHERE name = 'Ravichandran Ashwin';

UPDATE prisoners SET 
    incident_notes = 'Quiet and reserved; prefers the library over the recreation yard.\nConsidered a low-risk inmate with exemplary conduct scores.',
    visitor_log = '2022-06-01: Father (Arvind) for a short duration.\n2023-01-15: Family visit; brought homemade food (denied per policy).'
WHERE name = 'Cheteshwar Pujara';

UPDATE prisoners SET 
    incident_notes = 'Very polite to guards; often acts as a mediator in Cell Block A.\nVolunteers for extra cleaning duties on weekends.',
    visitor_log = '2024-03-20: Wife (Radhika) for a standard visit.\n2024-09-05: Personal lawyer (Adv. Patil) for case review.'
WHERE name = 'Ajinkya Rahane';

UPDATE prisoners SET 
    incident_notes = 'Currently in solitary following a violent outburst in the canteen.\nRequires 2-guard escort for all movement within the facility.',
    visitor_log = 'VISITOR BAN: Suspended for 30 days due to behavioral issues.\n2020-09-01: Wife (Devika) - Last recorded visit.'
WHERE name = 'Suryakumar Yadav';

UPDATE prisoners SET 
    incident_notes = 'Keeps to himself; shows interest in prison accounting tasks.\nComplies with all search and seizure drills without resistance.',
    visitor_log = '2021-12-01: Sister for initial contact.\n2023-05-18: Adv. Reddy for a 60-minute legal strategy session.'
WHERE name = 'Varun Chakravarthy';

UPDATE prisoners SET 
    incident_notes = 'Requested transfer to a different cell due to snoring roommates.\nParticipates in religious services every Sunday morning.',
    visitor_log = '2024-06-10: Wife (Charulatha) for a 20-minute glass-partition visit.\n2024-11-30: Local priest for spiritual counseling.'
WHERE name = 'Sanju Samson';

UPDATE prisoners SET 
    incident_notes = 'Assists the prison clerk with filing non-sensitive documents.\nNo disciplinary marks on record for the last six months.',
    visitor_log = '2023-09-15: Wife (Aashita) for a scheduled visit.\n2024-02-10: Brother regarding property management.'
WHERE name = 'Mayank Agarwal';

UPDATE prisoners SET 
    incident_notes = 'Refuses to participate in morning drills; showing signs of depression.\nUnder psychiatric evaluation following a self-harm threat.',
    visitor_log = '2020-01-15: Father (Siddharth) for a high-security visit.\n2024-02-10: Court-appointed psychologist.'
WHERE name = 'Prithvi Shaw';

UPDATE prisoners SET 
    incident_notes = 'Suspected of organizing a small gambling ring using cigarette rations.\nCell searched twice; no contraband found but remaining under watch.',
    visitor_log = '2022-03-10: Brother (Pranav) for a short visit.\n2024-08-15: Adv. Sinha for trial update.'
WHERE name = 'Ishan Kishan';

UPDATE prisoners SET 
    incident_notes = 'Known to have influence over younger inmates; potential gang leader.\nMaintains a neutral relationship with staff but shows low empathy.',
    visitor_log = '2022-10-01: Mother (Shabana) for a 15-minute visit.\n2023-12-10: Unknown associate (Entry Denied - No ID).'
WHERE name = 'Mohammed Siraj';

UPDATE prisoners SET 
    incident_notes = 'Complains frequently about the quality of food and medical care.\nHas filed three formal grievances against the night-shift guards.',
    visitor_log = '2024-08-10: Father (Santosh) for a 10-minute brief.\n2025-01-05: Legal Aid representative regarding grievance filing.'
WHERE name = 'Shreyas Iyer';

UPDATE prisoners SET 
    incident_notes = 'Involved in a minor scuffle over a gym equipment time slot.\nReceived a formal warning but has been cooperative since.',
    visitor_log = '2021-05-20: Father (Rajesh) for a monthly check-in.\n2023-11-11: Adv. Malik regarding land dispute hearing.'
WHERE name = 'Deepak Hooda';

UPDATE prisoners SET 
    incident_notes = 'Assists in the prison infirmary as a helper; shows medical aptitude.\nWell-liked by the medical staff for his helpful nature.',
    visitor_log = '2024-12-15: Father (Narendra) for a holiday visit.\n2025-02-01: Adv. More regarding document submission.'
WHERE name = 'Shardul Thakur';

UPDATE prisoners SET 
    incident_notes = 'Working on a distance-education degree via the prison program.\nSpends most of his free time studying in the quiet zone.',
    visitor_log = '2024-04-10: Father (Mani) brought educational textbooks.\n2024-09-20: Adv. Ram for a 15-minute briefing.'
WHERE name = 'Washington Sundar';

UPDATE prisoners SET 
    incident_notes = 'Hardworking in the prison laundry department.\nTakes instructions well and has had zero negative reports.',
    visitor_log = '2023-06-15: Wife (Meha) for a standard visit.\n2024-12-10: Adv. Joshi for a final case review.'
WHERE name = 'Axar Patel';
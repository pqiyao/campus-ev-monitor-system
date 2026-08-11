-- 10 full linked demo records for the student client.
-- Includes users, vehicles, BLE tags, GPS devices, violations,
-- license point logs, and BLE pass records.
-- Login password for every demo user: 123456

SET NAMES utf8mb4;
USE ev_monitor;

CREATE TABLE IF NOT EXISTS org_class (
    id INT AUTO_INCREMENT PRIMARY KEY,
    college VARCHAR(64) NOT NULL,
    major VARCHAR(64) NOT NULL,
    class_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_org_class (college, major, class_name),
    KEY idx_sort (sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS license_point_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    delta INT NOT NULL,
    balance_after INT NOT NULL,
    biz_type VARCHAR(32) NOT NULL,
    ref_illegal_id INT NULL,
    ref_welfare_id INT NULL,
    remark VARCHAR(500) NULL,
    evidence_urls VARCHAR(2000) NULL,
    operator_user_id INT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pass_record (
    pass_id INT NOT NULL AUTO_INCREMENT,
    etc_number VARCHAR(64) DEFAULT NULL,
    vehicle_id INT DEFAULT NULL,
    license_plate VARCHAR(20) DEFAULT NULL,
    user_id INT DEFAULT NULL,
    reader_id VARCHAR(32) DEFAULT NULL,
    reader_name VARCHAR(64) DEFAULT NULL,
    direction VARCHAR(10) DEFAULT 'in',
    pass_result VARCHAR(10) DEFAULT 'pass',
    pass_time DATETIME DEFAULT NULL,
    image_url VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    PRIMARY KEY (pass_id),
    KEY idx_etc_number (etc_number),
    KEY idx_vehicle_id (vehicle_id),
    KEY idx_pass_time (pass_time),
    KEY idx_direction (direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO org_class (college, major, class_name, sort_order) VALUES
('Info Engineering College', 'Computer Science', 'CS2401', 101),
('Info Engineering College', 'Computer Science', 'CS2402', 102),
('Software College', 'Software Engineering', 'SE2401', 201),
('AI College', 'Artificial Intelligence', 'AI2401', 301)
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order);

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_users;
CREATE TEMPORARY TABLE tmp_demo_users (user_id INT PRIMARY KEY);
INSERT INTO tmp_demo_users
SELECT user_id
FROM `user`
WHERE username IN (
    '彭淇垚','demo_stu_001','demo_stu_002','demo_stu_003','demo_stu_004','demo_stu_005',
    'demo_stu_006','demo_stu_007','demo_stu_008','demo_stu_009','demo_stu_010'
)
OR student_id IN (
    '202425001','202425002','202425003','202425004','202425005',
    '202425006','202425007','202425008','202425009','202425010'
);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_vehicles;
CREATE TEMPORARY TABLE tmp_demo_vehicles (vehicle_id INT PRIMARY KEY);
INSERT INTO tmp_demo_vehicles
SELECT vehicle_id
FROM vehicle_info
WHERE user_id IN (SELECT user_id FROM tmp_demo_users)
OR license_plate IN (
    '湘A0533305','湘A0533306','湘A0533307','湘A0533308','湘A0533309',
    '湘A0533310','湘A0533311','湘A0533312','湘A0533313','湘A0533314'
);

DELETE FROM license_point_log
WHERE user_id IN (SELECT user_id FROM tmp_demo_users)
   OR remark LIKE 'demo full linked data%';

DELETE FROM pass_record
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_demo_vehicles)
   OR etc_number LIKE 'BLE-DEMO-%'
   OR etc_number LIKE 'BLE-HY-%';

DELETE FROM vehicle_illegal_record
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_demo_vehicles)
   OR user_id IN (SELECT user_id FROM tmp_demo_users);

DELETE FROM location_device
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_demo_vehicles)
   OR device_number LIKE 'GPS-DEMO-%'
   OR device_number LIKE 'GPS-HY-%';

DELETE FROM etc_card
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_demo_vehicles)
   OR etc_number LIKE 'BLE-DEMO-%'
   OR etc_number LIKE 'BLE-HY-%';

DELETE FROM vehicle_info
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_demo_vehicles);

DELETE FROM `user`
WHERE user_id IN (SELECT user_id FROM tmp_demo_users);

INSERT INTO `user`
(username, password, real_name, user_type, student_id, job_id, phone, email, avatar, department, college, major, class_name, status, license_points, create_time, update_time, deleted)
VALUES
('彭淇垚', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '彭淇垚', 'student', '202425001', NULL, '13866010001', 'pengqiyao@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2401班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_002', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '陈思源', 'student', '202425002', NULL, '13866010002', 'chen-siyuan@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2401班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_003', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '李雨桐', 'student', '202425003', NULL, '13866010003', 'li-yutong@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2401班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_004', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '王子涵', 'student', '202425004', NULL, '13866010004', 'wang-zihan@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2402班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_005', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '刘昊然', 'student', '202425005', NULL, '13866010005', 'liu-haoran@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2402班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_006', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '张佳怡', 'student', '202425006', NULL, '13866010006', 'zhang-jiayi@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2402班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_007', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '周俊杰', 'student', '202425007', NULL, '13866010007', 'zhou-junjie@campus.edu.cn', NULL, '软件学院', '软件学院', '软件工程', '软工2401班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_008', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '黄梓涵', 'student', '202425008', NULL, '13866010008', 'huang-zihan@campus.edu.cn', NULL, '软件学院', '软件学院', '软件工程', '软工2401班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_009', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '赵一鸣', 'student', '202425009', NULL, '13866010009', 'zhao-yiming@campus.edu.cn', NULL, '人工智能学院', '人工智能学院', '人工智能', '智科2401班', 'active', 10, NOW(), NOW(), 0),
('demo_stu_010', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '吴雨薇', 'student', '202425010', NULL, '13866010010', 'wu-yuwei@campus.edu.cn', NULL, '人工智能学院', '人工智能学院', '人工智能', '智科2401班', 'active', 10, NOW(), NOW(), 0);

SET @u01 = (SELECT user_id FROM `user` WHERE username = '彭淇垚');
SET @u02 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_002');
SET @u03 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_003');
SET @u04 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_004');
SET @u05 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_005');
SET @u06 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_006');
SET @u07 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_007');
SET @u08 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_008');
SET @u09 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_009');
SET @u10 = (SELECT user_id FROM `user` WHERE username = 'demo_stu_010');

INSERT INTO vehicle_info
(user_id, license_plate, brand, color, frame_num, battery_type, vehicle_type, vehicle_photo, status, is_frozen, is_blacklist, register_time, audit_time, audit_user_id, valid_date, create_time, update_time, deleted)
VALUES
(@u01, '湘A0533305', '雅迪', '白色', 'HYDD202605010001', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u02, '湘A0533306', '爱玛', '黑色', 'HYDD202605010002', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 26 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u03, '湘A0533307', '台铃', '蓝色', 'HYDD202605010003', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u04, '湘A0533308', '小刀', '银色', 'HYDD202605010004', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u05, '湘A0533309', '绿源', '红色', 'HYDD202605010005', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u06, '湘A0533310', '新日', '灰色', 'HYDD202605010006', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u07, '湘A0533311', '雅迪', '绿色', 'HYDD202605010007', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u08, '湘A0533312', '爱玛', '黄色', 'HYDD202605010008', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u09, '湘A0533313', '台铃', '白色', 'HYDD202605010009', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u10, '湘A0533314', '九号', '黑色', 'HYDD202605010010', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0);

SET @v01 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533305');
SET @v02 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533306');
SET @v03 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533307');
SET @v04 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533308');
SET @v05 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533309');
SET @v06 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533310');
SET @v07 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533311');
SET @v08 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533312');
SET @v09 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533313');
SET @v10 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A0533314');

INSERT INTO etc_card
(vehicle_id, etc_number, balance, status, issue_date, expire_date, create_time, update_time)
VALUES
(@v01, 'BLE-HY-0001', 58.50, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v02, 'BLE-HY-0002', 42.00, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v03, 'BLE-HY-0003', 76.20, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v04, 'BLE-HY-0004', 31.80, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v05, 'BLE-HY-0005', 64.00, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v06, 'BLE-HY-0006', 25.50, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v07, 'BLE-HY-0007', 88.00, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v08, 'BLE-HY-0008', 19.90, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v09, 'BLE-HY-0009', 51.60, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v10, 'BLE-HY-0010', 37.30, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW());

INSERT INTO location_device
(vehicle_id, device_number, device_type, current_lng, current_lat, status, battery_level, last_report_time, create_time, update_time)
VALUES
(@v01, 'GPS-HY-0001', 'GPS', 113.006211, 28.142386, 'active', 96, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
(@v02, 'GPS-HY-0002', 'GPS', 113.007392, 28.142951, 'active', 91, DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
(@v03, 'GPS-HY-0003', 'GPS', 113.008246, 28.141716, 'active', 87, DATE_SUB(NOW(), INTERVAL 3 MINUTE), NOW(), NOW()),
(@v04, 'GPS-HY-0004', 'GPS', 113.009188, 28.143284, 'active', 94, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
(@v05, 'GPS-HY-0005', 'GPS', 113.010071, 28.142104, 'active', 83, DATE_SUB(NOW(), INTERVAL 4 MINUTE), NOW(), NOW()),
(@v06, 'GPS-HY-0006', 'GPS', 113.006816, 28.140932, 'active', 89, DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
(@v07, 'GPS-HY-0007', 'GPS', 113.008918, 28.140361, 'active', 97, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
(@v08, 'GPS-HY-0008', 'GPS', 113.010822, 28.141487, 'active', 78, DATE_SUB(NOW(), INTERVAL 5 MINUTE), NOW(), NOW()),
(@v09, 'GPS-HY-0009', 'GPS', 113.011436, 28.142777, 'active', 85, DATE_SUB(NOW(), INTERVAL 3 MINUTE), NOW(), NOW()),
(@v10, 'GPS-HY-0010', 'GPS', 113.007751, 28.139972, 'active', 92, DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW());

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v01, @u01, 1, 113.006211, 28.142386, DATE_SUB(NOW(), INTERVAL 10 DAY), 2, 2, NULL, '/demo/evidence/violation-001.jpg', 2, 0, NOW(), NOW(), 0);
SET @i01 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v02, @u02, 2, 113.007392, 28.142951, DATE_SUB(NOW(), INTERVAL 9 DAY), 2, 2, NULL, '/demo/evidence/violation-002.jpg', 2, 0, NOW(), NOW(), 0);
SET @i02 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v03, @u03, 3, 113.008246, 28.141716, DATE_SUB(NOW(), INTERVAL 8 DAY), 2, 2, NULL, '/demo/evidence/violation-003.jpg', 2, 0, NOW(), NOW(), 0);
SET @i03 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v04, @u04, 5, 113.009188, 28.143284, DATE_SUB(NOW(), INTERVAL 7 DAY), 2, 2, NULL, '/demo/evidence/violation-004.jpg', 2, 0, NOW(), NOW(), 0);
SET @i04 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v05, @u05, 6, 113.010071, 28.142104, DATE_SUB(NOW(), INTERVAL 6 DAY), 2, 2, NULL, '/demo/evidence/violation-005.jpg', 2, 0, NOW(), NOW(), 0);
SET @i05 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v06, @u06, 7, 113.006816, 28.140932, DATE_SUB(NOW(), INTERVAL 5 DAY), 2, 2, NULL, '/demo/evidence/violation-006.jpg', 2, 0, NOW(), NOW(), 0);
SET @i06 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v07, @u07, 1, 113.008918, 28.140361, DATE_SUB(NOW(), INTERVAL 4 DAY), 2, 2, NULL, '/demo/evidence/violation-007.jpg', 2, 0, NOW(), NOW(), 0);
SET @i07 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v08, @u08, 3, 113.010822, 28.141487, DATE_SUB(NOW(), INTERVAL 3 DAY), 2, 2, NULL, '/demo/evidence/violation-008.jpg', 2, 0, NOW(), NOW(), 0);
SET @i08 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v09, @u09, 5, 113.011436, 28.142777, DATE_SUB(NOW(), INTERVAL 2 DAY), 2, 2, NULL, '/demo/evidence/violation-009.jpg', 2, 0, NOW(), NOW(), 0);
SET @i09 = LAST_INSERT_ID();

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_id, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@v10, @u10, 8, 113.007751, 28.139972, DATE_SUB(NOW(), INTERVAL 1 DAY), 2, 2, NULL, '/demo/evidence/violation-010.jpg', 2, 0, NOW(), NOW(), 0);
SET @i10 = LAST_INSERT_ID();

INSERT INTO license_point_log
(user_id, delta, balance_after, biz_type, ref_illegal_id, ref_welfare_id, remark, evidence_urls, operator_user_id, create_time)
VALUES
(@u01, -2, 10, 'VIOLATION_DEDUCT', @i01, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-001.jpg', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(@u02, -2, 10, 'VIOLATION_DEDUCT', @i02, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-002.jpg', NULL, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(@u03, -2, 10, 'VIOLATION_DEDUCT', @i03, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-003.jpg', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(@u04, -2, 10, 'VIOLATION_DEDUCT', @i04, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-004.jpg', NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(@u05, -2, 10, 'VIOLATION_DEDUCT', @i05, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-005.jpg', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(@u06, -2, 10, 'VIOLATION_DEDUCT', @i06, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-006.jpg', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(@u07, -2, 10, 'VIOLATION_DEDUCT', @i07, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-007.jpg', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(@u08, -2, 10, 'VIOLATION_DEDUCT', @i08, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-008.jpg', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(@u09, -2, 10, 'VIOLATION_DEDUCT', @i09, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-009.jpg', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@u10, -2, 10, 'VIOLATION_DEDUCT', @i10, NULL, 'demo full linked data: violation -2 points', '/demo/evidence/violation-010.jpg', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO pass_record
(etc_number, vehicle_id, license_plate, user_id, reader_id, reader_name, direction, pass_result, pass_time, image_url, create_time, deleted)
VALUES
('BLE-HY-0001', @v01, '湘A0533305', @u01, 'GATE-EAST-01', 'East Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 9 HOUR), NULL, NOW(), 0),
('BLE-HY-0001', @v01, '湘A0533305', @u01, 'GATE-EAST-01', 'East Gate BLE Gateway', 'out', 'pass', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NOW(), 0),
('BLE-HY-0002', @v02, '湘A0533306', @u02, 'GATE-SOUTH-01', 'South Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL, NOW(), 0),
('BLE-HY-0002', @v02, '湘A0533306', @u02, 'GATE-SOUTH-01', 'South Gate BLE Gateway', 'out', 'pass', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NOW(), 0),
('BLE-HY-0003', @v03, '湘A0533307', @u03, 'GATE-WEST-01', 'West Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 7 HOUR), NULL, NOW(), 0),
('BLE-HY-0004', @v04, '湘A0533308', @u04, 'GATE-NORTH-01', 'North Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, NOW(), 0),
('BLE-HY-0005', @v05, '湘A0533309', @u05, 'GATE-EAST-01', 'East Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NOW(), 0),
('BLE-HY-0006', @v06, '湘A0533310', @u06, 'GATE-SOUTH-01', 'South Gate BLE Gateway', 'out', 'pass', DATE_SUB(NOW(), INTERVAL 4 HOUR), NULL, NOW(), 0),
('BLE-HY-0007', @v07, '湘A0533311', @u07, 'GATE-WEST-01', 'West Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NOW(), 0),
('BLE-HY-0008', @v08, '湘A0533312', @u08, 'GATE-NORTH-01', 'North Gate BLE Gateway', 'out', 'pass', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NOW(), 0),
('BLE-HY-0009', @v09, '湘A0533313', @u09, 'GATE-EAST-01', 'East Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NOW(), 0),
('BLE-HY-0010', @v10, '湘A0533314', @u10, 'GATE-SOUTH-01', 'South Gate BLE Gateway', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, NOW(), 0);

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_vehicles;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_users;


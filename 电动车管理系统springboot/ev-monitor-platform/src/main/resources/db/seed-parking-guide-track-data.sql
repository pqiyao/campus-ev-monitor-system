-- Parking guidance and track playback linked seed data.
-- Password for every student account: 123456
-- Run this file after the base tables have been created.

SET NAMES utf8mb4;
USE ev_monitor;

CREATE TABLE IF NOT EXISTS notification_user_inbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT        NOT NULL,
    notification_id INT        NOT NULL,
    hidden          TINYINT(1) NOT NULL DEFAULT 0,
    read_at         DATETIME   NULL,
    create_time     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_notification (user_id, notification_id),
    KEY idx_user_hidden (user_id, hidden)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_guide_users;
CREATE TEMPORARY TABLE tmp_guide_users (user_id INT PRIMARY KEY);

INSERT INTO tmp_guide_users
SELECT user_id
FROM `user`
WHERE username IN (
    'guide_stu_001','guide_stu_002','guide_stu_003','guide_stu_004','guide_stu_005',
    'guide_stu_006','guide_stu_007','guide_stu_008','guide_stu_009','guide_stu_010'
)
OR student_id IN (
    '20260503001','20260503002','20260503003','20260503004','20260503005',
    '20260503006','20260503007','20260503008','20260503009','20260503010'
);

DROP TEMPORARY TABLE IF EXISTS tmp_guide_vehicles;
CREATE TEMPORARY TABLE tmp_guide_vehicles (vehicle_id INT PRIMARY KEY);

INSERT INTO tmp_guide_vehicles
SELECT vehicle_id
FROM vehicle_info
WHERE user_id IN (SELECT user_id FROM tmp_guide_users)
   OR license_plate IN (
       '湘A90001','湘A90002','湘A90003','湘A90004','湘A90005',
       '湘A90006','湘A90007','湘A90008','湘A90009','湘A90010'
   )
   OR frame_num LIKE 'TRACKGUIDE20260503%';

DROP TEMPORARY TABLE IF EXISTS tmp_guide_notifications;
CREATE TEMPORARY TABLE tmp_guide_notifications (notification_id INT PRIMARY KEY);

INSERT INTO tmp_guide_notifications
SELECT notification_id
FROM notification
WHERE title IN ('停车引导', '车辆历史轨迹回放')
   OR content LIKE '%附近区域车位已满%'
   OR content LIKE '%推荐前往 东侧停车场%'
   OR content LIKE '%推荐前往 北区停车场%';

DELETE FROM notification_user_inbox
WHERE notification_id IN (SELECT notification_id FROM tmp_guide_notifications)
   OR user_id IN (SELECT user_id FROM tmp_guide_users);

DELETE FROM notification
WHERE notification_id IN (SELECT notification_id FROM tmp_guide_notifications);

DELETE FROM geofence_alert
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_guide_vehicles)
   OR user_id IN (SELECT user_id FROM tmp_guide_users)
   OR fence_name IN ('南门停车场', '东侧停车场', '北区停车场');

DELETE FROM geofence
WHERE fence_name IN ('南门停车场', '东侧停车场', '北区停车场');

DELETE FROM location_device
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_guide_vehicles)
   OR device_number LIKE 'GPS-GUIDE-%';

DELETE FROM etc_card
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_guide_vehicles)
   OR etc_number LIKE 'RFID-GUIDE-%';

DELETE FROM vehicle_info
WHERE vehicle_id IN (SELECT vehicle_id FROM tmp_guide_vehicles);

DELETE FROM `user`
WHERE user_id IN (SELECT user_id FROM tmp_guide_users);

INSERT INTO `user`
(username, password, real_name, user_type, student_id, job_id, phone, email, avatar, department, college, major, class_name, status, license_points, create_time, update_time, deleted)
VALUES
('guide_stu_001', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '陈思源', 'student', '20260503001', NULL, '13870010001', 'guide_stu_001@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2401班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_002', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '李雨桐', 'student', '20260503002', NULL, '13870010002', 'guide_stu_002@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2401班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_003', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '王子涵', 'student', '20260503003', NULL, '13870010003', 'guide_stu_003@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2401班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_004', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '赵一鸣', 'student', '20260503004', NULL, '13870010004', 'guide_stu_004@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2402班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_005', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '刘浩然', 'student', '20260503005', NULL, '13870010005', 'guide_stu_005@campus.edu.cn', NULL, '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2402班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_006', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '张佳怡', 'student', '20260503006', NULL, '13870010006', 'guide_stu_006@campus.edu.cn', NULL, '软件学院', '软件学院', '软件工程', '软工2401班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_007', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '周俊杰', 'student', '20260503007', NULL, '13870010007', 'guide_stu_007@campus.edu.cn', NULL, '软件学院', '软件学院', '软件工程', '软工2401班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_008', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '黄梓涵', 'student', '20260503008', NULL, '13870010008', 'guide_stu_008@campus.edu.cn', NULL, '人工智能学院', '人工智能学院', '人工智能', '智科2401班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_009', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '吴雨薇', 'student', '20260503009', NULL, '13870010009', 'guide_stu_009@campus.edu.cn', NULL, '人工智能学院', '人工智能学院', '人工智能', '智科2401班', 'active', 10, NOW(), NOW(), 0),
('guide_stu_010', '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '彭启耀', 'student', '20260503010', NULL, '13870010010', 'guide_stu_010@campus.edu.cn', NULL, '人工智能学院', '人工智能学院', '人工智能', '智科2401班', 'active', 10, NOW(), NOW(), 0);

SET @u01 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_001');
SET @u02 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_002');
SET @u03 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_003');
SET @u04 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_004');
SET @u05 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_005');
SET @u06 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_006');
SET @u07 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_007');
SET @u08 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_008');
SET @u09 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_009');
SET @u10 = (SELECT user_id FROM `user` WHERE username = 'guide_stu_010');

INSERT INTO vehicle_info
(user_id, license_plate, brand, color, frame_num, battery_type, vehicle_type, vehicle_photo, status, is_frozen, is_blacklist, register_time, audit_time, audit_user_id, valid_date, create_time, update_time, deleted)
VALUES
(@u01, '湘A90001', '雅迪', '白色', 'TRACKGUIDE202605030001', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 55 DAY), DATE_SUB(NOW(), INTERVAL 54 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u02, '湘A90002', '爱玛', '黑色', 'TRACKGUIDE202605030002', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 50 DAY), DATE_SUB(NOW(), INTERVAL 49 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u03, '湘A90003', '台铃', '蓝色', 'TRACKGUIDE202605030003', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 44 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u04, '湘A90004', '小刀', '银色', 'TRACKGUIDE202605030004', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 42 DAY), DATE_SUB(NOW(), INTERVAL 41 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u05, '湘A90005', '绿源', '红色', 'TRACKGUIDE202605030005', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u06, '湘A90006', '新日', '灰色', 'TRACKGUIDE202605030006', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 34 DAY), DATE_SUB(NOW(), INTERVAL 33 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u07, '湘A90007', '雅迪', '绿色', 'TRACKGUIDE202605030007', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u08, '湘A90008', '爱玛', '黄色', 'TRACKGUIDE202605030008', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u09, '湘A90009', '台铃', '白色', 'TRACKGUIDE202605030009', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0),
(@u10, '湘A90010', '九号', '黑色', 'TRACKGUIDE202605030010', '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY), NULL, DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NOW(), NOW(), 0);

SET @v01 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90001');
SET @v02 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90002');
SET @v03 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90003');
SET @v04 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90004');
SET @v05 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90005');
SET @v06 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90006');
SET @v07 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90007');
SET @v08 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90008');
SET @v09 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90009');
SET @v10 = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = '湘A90010');

INSERT INTO etc_card
(vehicle_id, etc_number, balance, status, issue_date, expire_date, create_time, update_time)
VALUES
(@v01, 'RFID-GUIDE-0001', 58.50, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v02, 'RFID-GUIDE-0002', 42.00, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v03, 'RFID-GUIDE-0003', 76.20, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v04, 'RFID-GUIDE-0004', 31.80, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v05, 'RFID-GUIDE-0005', 64.00, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v06, 'RFID-GUIDE-0006', 25.50, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v07, 'RFID-GUIDE-0007', 88.00, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v08, 'RFID-GUIDE-0008', 19.90, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v09, 'RFID-GUIDE-0009', 51.60, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW()),
(@v10, 'RFID-GUIDE-0010', 37.30, 'normal', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW());

INSERT INTO location_device
(vehicle_id, device_number, device_type, current_lng, current_lat, status, battery_level, last_report_time, create_time, update_time)
VALUES
(@v01, 'GPS-GUIDE-0001', 'GPS', 112.68182, 26.88520, 'active', 96, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
(@v02, 'GPS-GUIDE-0002', 'GPS', 112.68210, 26.88536, 'active', 91, DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
(@v03, 'GPS-GUIDE-0003', 'GPS', 112.68238, 26.88552, 'active', 87, DATE_SUB(NOW(), INTERVAL 3 MINUTE), NOW(), NOW()),
(@v04, 'GPS-GUIDE-0004', 'GPS', 112.68266, 26.88570, 'active', 94, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
(@v05, 'GPS-GUIDE-0005', 'GPS', 112.68302, 26.88530, 'active', 83, DATE_SUB(NOW(), INTERVAL 4 MINUTE), NOW(), NOW()),
(@v06, 'GPS-GUIDE-0006', 'GPS', 112.68328, 26.88562, 'active', 89, DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW()),
(@v07, 'GPS-GUIDE-0007', 'GPS', 112.68158, 26.88576, 'active', 97, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW()),
(@v08, 'GPS-GUIDE-0008', 'GPS', 112.68362, 26.88644, 'active', 78, DATE_SUB(NOW(), INTERVAL 5 MINUTE), NOW(), NOW()),
(@v09, 'GPS-GUIDE-0009', 'GPS', 112.68418, 26.88690, 'active', 85, DATE_SUB(NOW(), INTERVAL 3 MINUTE), NOW(), NOW()),
(@v10, 'GPS-GUIDE-0010', 'GPS', 112.68218, 26.88742, 'active', 92, DATE_SUB(NOW(), INTERVAL 2 MINUTE), NOW(), NOW());

INSERT INTO geofence
(fence_name, fence_type, fence_color, coordinates, center_lng, center_lat, alert_level, alert_mode, description, status, create_user_id, create_time, update_time, deleted)
VALUES
('南门停车场', 5, '#ff6b6b', '[{"lng":112.68150,"lat":26.88495},{"lng":112.68335,"lat":26.88500},{"lng":112.68325,"lat":26.88588},{"lng":112.68156,"lat":26.88582}]', 112.682425, 26.885415, 3, 'both', '车位数8，当前占用接近满载，用于触发停车引导。', 1, NULL, NOW(), NOW(), 0),
('东侧停车场', 5, '#00d4ff', '[{"lng":112.68315,"lat":26.88618},{"lng":112.68470,"lat":26.88624},{"lng":112.68462,"lat":26.88722},{"lng":112.68322,"lat":26.88712}]', 112.683925, 26.886700, 2, 'both', '推荐分流区域，适合学生端接收引导信息。', 1, NULL, NOW(), NOW(), 0),
('北区停车场', 5, '#00ff7f', '[{"lng":112.68120,"lat":26.88702},{"lng":112.68295,"lat":26.88708},{"lng":112.68285,"lat":26.88786},{"lng":112.68126,"lat":26.88778}]', 112.682075, 26.887440, 2, 'both', '备用停车区域，保留少量空位。', 1, NULL, NOW(), NOW(), 0);

SET @f_south = (SELECT fence_id FROM geofence WHERE fence_name = '南门停车场' AND deleted = 0 ORDER BY fence_id DESC LIMIT 1);
SET @f_east  = (SELECT fence_id FROM geofence WHERE fence_name = '东侧停车场' AND deleted = 0 ORDER BY fence_id DESC LIMIT 1);
SET @f_north = (SELECT fence_id FROM geofence WHERE fence_name = '北区停车场' AND deleted = 0 ORDER BY fence_id DESC LIMIT 1);

INSERT INTO geofence_alert
(fence_id, fence_name, vehicle_id, license_plate, user_id, user_name, alert_type, trigger_lng, trigger_lat, alert_level, is_processed, create_time)
VALUES
(@f_south, '南门停车场', @v01, '湘A90001', @u01, '陈思源', 'enter', 112.68182, 26.88520, 3, 0, DATE_SUB(NOW(), INTERVAL 26 MINUTE)),
(@f_south, '南门停车场', @v02, '湘A90002', @u02, '李雨桐', 'enter', 112.68210, 26.88536, 3, 0, DATE_SUB(NOW(), INTERVAL 24 MINUTE)),
(@f_south, '南门停车场', @v03, '湘A90003', @u03, '王子涵', 'enter', 112.68238, 26.88552, 3, 0, DATE_SUB(NOW(), INTERVAL 22 MINUTE)),
(@f_south, '南门停车场', @v04, '湘A90004', @u04, '赵一鸣', 'enter', 112.68266, 26.88570, 3, 0, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(@f_south, '南门停车场', @v05, '湘A90005', @u05, '刘浩然', 'enter', 112.68302, 26.88530, 3, 0, DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(@f_south, '南门停车场', @v06, '湘A90006', @u06, '张佳怡', 'enter', 112.68328, 26.88562, 3, 0, DATE_SUB(NOW(), INTERVAL 16 MINUTE)),
(@f_south, '南门停车场', @v07, '湘A90007', @u07, '周俊杰', 'enter', 112.68158, 26.88576, 3, 0, DATE_SUB(NOW(), INTERVAL 14 MINUTE)),
(@f_east,  '东侧停车场', @v08, '湘A90008', @u08, '黄梓涵', 'enter', 112.68362, 26.88644, 2, 1, DATE_SUB(NOW(), INTERVAL 11 MINUTE)),
(@f_east,  '东侧停车场', @v09, '湘A90009', @u09, '吴雨薇', 'enter', 112.68418, 26.88690, 2, 1, DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(@f_north, '北区停车场', @v10, '湘A90010', @u10, '彭启耀', 'enter', 112.68218, 26.88742, 2, 1, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

INSERT INTO notification
(title, content, type, is_urgent, receiver_type, receiver_ids, send_time, read_count, total_count, status, create_time, update_time, deleted)
VALUES
('停车引导', CONCAT('附近区域车位已满，推荐前往 东侧停车场。当前车辆：湘A90001、湘A90002、湘A90003、湘A90004、湘A90005、湘A90006、湘A90007。'), 'guide', 1, 'specified', CONCAT(@u01, ',', @u02, ',', @u03, ',', @u04, ',', @u05, ',', @u06, ',', @u07, ',', @u08, ',', @u09, ',', @u10), DATE_SUB(NOW(), INTERVAL 3 MINUTE), 0, 10, 'sent', NOW(), NOW(), 0);
SET @n_guide = LAST_INSERT_ID();

INSERT INTO notification
(title, content, type, is_urgent, receiver_type, receiver_ids, send_time, read_count, total_count, status, create_time, update_time, deleted)
VALUES
('车辆历史轨迹回放', CONCAT('车辆 湘A90008 已进入东侧停车场，可在数据大屏查看历史轨迹回放。'), 'location', 0, 'specified', CONCAT(@u08, ',', @u09, ',', @u10), DATE_SUB(NOW(), INTERVAL 1 MINUTE), 0, 3, 'sent', NOW(), NOW(), 0);
SET @n_track = LAST_INSERT_ID();

INSERT INTO notification_user_inbox
(user_id, notification_id, hidden, read_at, create_time, update_time)
VALUES
(@u01, @n_guide, 0, NULL, NOW(), NOW()),
(@u02, @n_guide, 0, NULL, NOW(), NOW()),
(@u03, @n_guide, 0, NULL, NOW(), NOW()),
(@u04, @n_guide, 0, NULL, NOW(), NOW()),
(@u05, @n_guide, 0, NULL, NOW(), NOW()),
(@u06, @n_guide, 0, NULL, NOW(), NOW()),
(@u07, @n_guide, 0, NULL, NOW(), NOW()),
(@u08, @n_guide, 0, NULL, NOW(), NOW()),
(@u09, @n_guide, 0, NULL, NOW(), NOW()),
(@u10, @n_guide, 0, NULL, NOW(), NOW()),
(@u08, @n_track, 0, NULL, NOW(), NOW()),
(@u09, @n_track, 0, NULL, NOW(), NOW()),
(@u10, @n_track, 0, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE hidden = VALUES(hidden), read_at = VALUES(read_at), update_time = NOW();

COMMIT;

SELECT
    u.user_id,
    u.username,
    u.real_name,
    u.student_id,
    v.vehicle_id,
    v.license_plate,
    e.etc_number AS rfid_number,
    d.device_number AS gps_number,
    d.current_lng,
    d.current_lat
FROM `user` u
JOIN vehicle_info v ON v.user_id = u.user_id AND v.deleted = 0
LEFT JOIN etc_card e ON e.vehicle_id = v.vehicle_id
LEFT JOIN location_device d ON d.vehicle_id = v.vehicle_id
WHERE u.username LIKE 'guide_stu_%'
ORDER BY u.username;

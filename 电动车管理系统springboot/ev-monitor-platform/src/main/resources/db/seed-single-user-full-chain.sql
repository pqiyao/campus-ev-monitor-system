-- Single complete linked demo chain for the student client.
-- Login: link_demo_001 / 123456

SET NAMES utf8mb4;
USE ev_monitor;
START TRANSACTION;

SET @demo_username = 'link_demo_001';
SET @demo_plate = '湘A0599001';
SET @demo_frame = 'DEMOCHAIN20260814001';
SET @demo_etc = 'BLE-CHAIN-0001';
SET @demo_gps = 'GPS-CHAIN-0001';
SET @demo_event_code = 'EVT-CHAIN-FIRE-001';
SET @demo_dedupe = 'demo-chain-fire-001';

SET @old_user = (SELECT user_id FROM `user` WHERE username = @demo_username LIMIT 1);
SET @old_vehicle = (SELECT vehicle_id FROM vehicle_info WHERE license_plate = @demo_plate LIMIT 1);
SET @old_illegal = (SELECT illegal_id FROM vehicle_illegal_record WHERE user_id = @old_user LIMIT 1);
SET @old_notification = (SELECT notification_id FROM notification WHERE title = '模拟用户违规闭环通知' LIMIT 1);
SET @old_event = (SELECT event_id FROM ai_event WHERE event_code = @demo_event_code LIMIT 1);

DELETE FROM governance_incident WHERE event_id = @old_event OR incident_code = 'INC-CHAIN-FIRE-001';
DELETE FROM ai_event WHERE event_id = @old_event OR event_code = @demo_event_code OR dedupe_key = @demo_dedupe;
DELETE FROM notification_user_inbox WHERE user_id = @old_user OR notification_id = @old_notification;
DELETE FROM user_notification WHERE user_id = @old_user OR notification_id = @old_notification;
DELETE FROM notification WHERE notification_id = @old_notification;
DELETE FROM illegal_appeal_record WHERE user_id = @old_user OR illegal_id = @old_illegal;
DELETE FROM illegal_rectify_record WHERE user_id = @old_user OR illegal_id = @old_illegal;
DELETE FROM license_point_log WHERE user_id = @old_user OR remark LIKE '完整链路演示:%';
DELETE FROM pass_record WHERE user_id = @old_user OR etc_number = @demo_etc;
DELETE FROM vehicle_illegal_record WHERE user_id = @old_user OR vehicle_id = @old_vehicle;
DELETE FROM location_device WHERE vehicle_id = @old_vehicle OR device_number = @demo_gps;
DELETE FROM etc_card WHERE vehicle_id = @old_vehicle OR etc_number = @demo_etc;
DELETE FROM vehicle_info WHERE user_id = @old_user OR license_plate = @demo_plate OR frame_num = @demo_frame;
DELETE FROM `user` WHERE user_id = @old_user OR username = @demo_username;

INSERT INTO `user`
(username, password, real_name, user_type, student_id, phone, email, department, college, major, class_name, status, license_points, create_time, update_time, deleted)
VALUES
(@demo_username, '$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK', '林小满', 'student', '2026081401', '13866018801', 'linxiaoman@campus.edu.cn', '信息工程学院', '信息工程学院', '计算机科学与技术', '计科2401班', 'active', 10, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW(), 0);
SET @demo_user = LAST_INSERT_ID();

INSERT INTO vehicle_info
(user_id, license_plate, brand, color, frame_num, battery_type, vehicle_type, vehicle_photo, status, is_frozen, is_blacklist, register_time, audit_time, valid_date, create_time, update_time, deleted)
VALUES
(@demo_user, @demo_plate, '雅迪', '白色', @demo_frame, '锂电池', '电动自行车', '', 'approved', 0, 0, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_ADD(CURDATE(), INTERVAL 1 YEAR), DATE_SUB(NOW(), INTERVAL 25 DAY), NOW(), 0);
SET @demo_vehicle = LAST_INSERT_ID();

INSERT INTO etc_card
(vehicle_id, etc_number, balance, status, issue_date, expire_date, create_time, update_time)
VALUES
(@demo_vehicle, @demo_etc, 36.50, 'normal', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), NOW(), NOW());

INSERT INTO location_device
(vehicle_id, device_number, device_type, current_lng, current_lat, status, battery_level, last_report_time, create_time, update_time)
VALUES
(@demo_vehicle, @demo_gps, 'GPS', 113.006211, 28.142386, 'online', 92, DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NOW());

INSERT INTO pass_record
(etc_number, vehicle_id, license_plate, user_id, reader_id, reader_name, direction, pass_result, pass_time, image_url, create_time, deleted)
VALUES
(@demo_etc, @demo_vehicle, @demo_plate, @demo_user, 'GATE-EAST-01', '东门蓝牙网关', 'in', 'pass', DATE_SUB(NOW(), INTERVAL 3 DAY), '/demo/pass/chain-entry.jpg', DATE_SUB(NOW(), INTERVAL 3 DAY), 0);

INSERT INTO vehicle_illegal_record
(vehicle_id, user_id, illegal_type, illegal_lng, illegal_lat, illegal_time, deduction_points, points_deducted, evidence_url, illegal_source, illegal_state, create_time, update_time, deleted)
VALUES
(@demo_vehicle, @demo_user, 9, 113.006211, 28.142386, DATE_SUB(NOW(), INTERVAL 2 DAY), 2, 2, '/demo/evidence/chain-no-helmet.jpg', 1, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0);
SET @demo_illegal = LAST_INSERT_ID();

INSERT INTO license_point_log
(user_id, delta, balance_after, biz_type, ref_illegal_id, remark, evidence_urls, create_time)
VALUES
(@demo_user, -2, 10, 'VIOLATION_DEDUCT', @demo_illegal, '完整链路演示: 未佩戴头盔扣2分', '/demo/evidence/chain-no-helmet.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO illegal_rectify_record
(illegal_id, user_id, rectify_photo, rectify_desc, rectify_time, verify_state, verify_remark, verify_time, create_time, update_time)
VALUES
(@demo_illegal, @demo_user, '/demo/rectify/chain-wearing-helmet.jpg', '已按要求佩戴安全头盔，并提交整改照片。', DATE_SUB(NOW(), INTERVAL 1 DAY), 2, '整改材料清晰，核实通过。', DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

INSERT INTO illegal_appeal_record
(illegal_id, user_id, appeal_reason, appeal_evidence, appeal_time, appeal_state, audit_remark, audit_time, appeal_source, urgency_level, appeal_result, audit_opinion, create_time, update_time)
VALUES
(@demo_illegal, @demo_user, '对识别结果有疑问，申请查看原始证据。', '/demo/appeal/chain-explanation.jpg', DATE_SUB(NOW(), INTERVAL 36 HOUR), 3, '原始证据能够确认未佩戴头盔，维持原处理。', DATE_SUB(NOW(), INTERVAL 18 HOUR), 'user', 1, 2, '证据完整，申诉不成立。', DATE_SUB(NOW(), INTERVAL 36 HOUR), NOW());

INSERT INTO notification
(title, content, type, is_urgent, receiver_type, receiver_ids, send_time, read_count, total_count, status, create_time, update_time, deleted)
VALUES
('模拟用户违规闭环通知', CONCAT('车辆', @demo_plate, '存在未佩戴头盔记录，整改已核实通过，申诉审核已完成。'), 'violation', 1, 'student', CAST(@demo_user AS CHAR), DATE_SUB(NOW(), INTERVAL 2 DAY), 0, 1, 'sent', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0);
SET @demo_notification = LAST_INSERT_ID();

INSERT INTO notification_user_inbox
(user_id, notification_id, hidden, read_at, create_time, update_time)
VALUES
(@demo_user, @demo_notification, 0, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

INSERT INTO user_notification
(notification_id, user_id, is_read, read_time, create_time)
VALUES
(@demo_notification, @demo_user, 0, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO ai_event
(event_code, event_type, source_type, device_id, occurred_at, location_name, longitude, latitude, license_plate, vehicle_id, user_id, identity_method, identity_confidence, detection_confidence, evidence_urls, raw_payload, status, risk_level, dedupe_key, create_time, update_time)
VALUES
(@demo_event_code, 'FIRE_DETECTED', 'CAMERA', 'camera-3', DATE_SUB(NOW(), INTERVAL 4 HOUR), '三号停车棚', 113.0062110, 28.1423860, @demo_plate, @demo_vehicle, @demo_user, 'PLATE', 0.9600, 0.9100, '["/demo/evidence/chain-fire.jpg"]', '{"cameraId":3,"detector":"YOLOv8-fire-smoke","demo":true}', 'REVIEW_PENDING', 'HIGH', @demo_dedupe, DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW());
SET @demo_event = LAST_INSERT_ID();

INSERT INTO governance_incident
(incident_code, event_id, vehicle_id, user_id, event_type, title, summary, risk_level, decision_type, state, deadline, violation_id, create_time, update_time)
VALUES
('INC-CHAIN-FIRE-001', @demo_event, @demo_vehicle, @demo_user, 'FIRE_DETECTED', '三号停车棚明火预警', '摄像头3检测到明火，请值班人员立即复核处置。', 'HIGH', 'MANUAL_REVIEW', 'PENDING', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW());

COMMIT;

SELECT @demo_user AS user_id, @demo_vehicle AS vehicle_id, @demo_illegal AS illegal_id,
       @demo_notification AS notification_id, @demo_event AS ai_event_id,
       @demo_username AS username, '123456' AS login_password;

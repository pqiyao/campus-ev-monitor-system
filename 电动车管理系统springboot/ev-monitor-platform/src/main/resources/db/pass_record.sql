-- 通行记录表
CREATE TABLE IF NOT EXISTS `pass_record` (
  `pass_id` INT NOT NULL AUTO_INCREMENT COMMENT '通行记录ID',
  `etc_number` VARCHAR(64) DEFAULT NULL COMMENT 'BLE蓝牙标签编号',
  `vehicle_id` INT DEFAULT NULL COMMENT '车辆ID',
  `license_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
  `user_id` INT DEFAULT NULL COMMENT '用户ID',
  `reader_id` VARCHAR(32) DEFAULT NULL COMMENT '蓝牙网关ID',
  `reader_name` VARCHAR(64) DEFAULT NULL COMMENT '蓝牙网关名称',
  `direction` VARCHAR(10) DEFAULT 'in' COMMENT '通行方向：in=入校, out=出校',
  `pass_result` VARCHAR(10) DEFAULT 'pass' COMMENT '通行结果：pass=通过, deny=拒绝',
  `pass_time` DATETIME DEFAULT NULL COMMENT '通行时间',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '抓拍图片URL',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`pass_id`),
  KEY `idx_etc_number` (`etc_number`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_pass_time` (`pass_time`),
  KEY `idx_direction` (`direction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BLE蓝牙识别通行记录表';

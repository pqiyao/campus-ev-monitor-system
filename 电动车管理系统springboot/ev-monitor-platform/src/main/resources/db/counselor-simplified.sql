-- 简化版：学生组织信息 + 导员管辖班级（执行前请确认库名）
-- USE ev_monitor;

-- 若列已存在会报错，可跳过对应行
ALTER TABLE `user` ADD COLUMN college VARCHAR(64) NULL DEFAULT NULL COMMENT '学院' AFTER department;
ALTER TABLE `user` ADD COLUMN major VARCHAR(64) NULL DEFAULT NULL COMMENT '专业' AFTER college;
ALTER TABLE `user` ADD COLUMN class_name VARCHAR(128) NULL DEFAULT NULL COMMENT '班级名称' AFTER major;

CREATE TABLE IF NOT EXISTS counselor_class (
    id              INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    counselor_user_id INT NOT NULL COMMENT '导员 user.user_id',
    college         VARCHAR(64)  NULL DEFAULT NULL COMMENT '学院（展示分组）',
    major           VARCHAR(64)  NULL DEFAULT NULL COMMENT '专业（展示分组）',
    class_name      VARCHAR(128) NOT NULL COMMENT '班级名称，与学生 user.class_name 一致',
    create_time     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_counselor_class (counselor_user_id, class_name),
    KEY idx_counselor (counselor_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导员管辖班级（简化版）';

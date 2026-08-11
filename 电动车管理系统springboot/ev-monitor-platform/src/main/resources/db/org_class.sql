-- 标准学籍班级（下拉数据源）。执行一次即可；可与 counselor-simplified.sql 同库执行。
-- 学生注册、导员绑定均从 GET /api/org-class/options 拉取（含本表 + 已有学生去重班级）。

CREATE TABLE IF NOT EXISTS org_class (
    id           INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    college      VARCHAR(64)  NOT NULL COMMENT '学院（含年级）',
    major        VARCHAR(64)  NOT NULL COMMENT '专业',
    class_name   VARCHAR(128) NOT NULL COMMENT '班级名称',
    sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序，小在前',
    create_time  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_org_class (college, major, class_name),
    KEY idx_sort (sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准班级字典';

INSERT INTO org_class (college, major, class_name, sort_order) VALUES
('2024级计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术1班', 1),
('2024级计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术2班', 2),
('2024级计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术3班', 3),
('2024级计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术4班', 4),
('计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术1班', 11),
('计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术2班', 12),
('计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术3班', 13),
('计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术4班', 14),
('计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术5班', 15),
('计算机科学与技术学院', '计算机科学与技术', '计算机科学与技术6班', 16),
('软件学院', '软件工程', '软件工程1班', 21),
('软件学院', '软件工程', '软件工程2班', 22),
('软件学院', '软件工程', '软件工程3班', 23),
('人工智能学院', '人工智能', '人工智能1班', 31),
('人工智能学院', '人工智能', '人工智能2班', 32)
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order);

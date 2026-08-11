-- 校园电动车 12 分制 + 公益加分 / 违规删除恢复
-- 请在目标库执行一次（若列已存在会报错，可跳过对应语句）

ALTER TABLE `user`
    ADD COLUMN `license_points` INT NOT NULL DEFAULT 12 COMMENT '剩余记分(满分12)' AFTER `status`;
UPDATE `user` SET `license_points` = 12 WHERE `license_points` IS NULL;

ALTER TABLE `vehicle_illegal_record`
    ADD COLUMN `points_deducted` INT NOT NULL DEFAULT 0 COMMENT '本条实际扣除记分' AFTER `deduction_points`;

CREATE TABLE IF NOT EXISTS `license_point_log` (
    `log_id`        INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`       INT         NOT NULL,
    `delta`         INT         NOT NULL COMMENT '正为加分负为扣分',
    `balance_after` INT         NOT NULL COMMENT '变更后剩余记分',
    `biz_type`      VARCHAR(32) NOT NULL COMMENT 'VIOLATION_DEDUCT/DELETION_RESTORE/ADMIN_BONUS/WELFARE_BONUS',
    `ref_illegal_id` INT        NULL,
    `ref_welfare_id` INT        NULL,
    `remark`        VARCHAR(500) NULL,
    `evidence_urls` VARCHAR(2000) NULL,
    `operator_user_id` INT     NULL,
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记分流水';

CREATE TABLE IF NOT EXISTS `welfare_point_request` (
    `request_id`    INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`       INT         NOT NULL,
    `content`       VARCHAR(800) NOT NULL COMMENT '公益说明',
    `image_urls`    VARCHAR(2000) NULL COMMENT '凭证图URL，逗号分隔',
    `status`        TINYINT     NOT NULL DEFAULT 0 COMMENT '0待审1通过2拒绝',
    `points_awarded` INT        NOT NULL DEFAULT 0,
    `admin_remark`  VARCHAR(500) NULL,
    `reviewer_id`   INT         NULL,
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME    NULL ON UPDATE CURRENT_TIMESTAMP,
    `audit_time`    DATETIME    NULL,
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公益加分申请';

-- 用户侧通知状态：已读时间、从收件箱移除（软删，不影响管理端全局通知记录）
USE ev_monitor;

CREATE TABLE IF NOT EXISTS notification_user_inbox (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT          NOT NULL,
    notification_id INT        NOT NULL,
    hidden        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1=用户已从列表移除',
    read_at       DATETIME     NULL COMMENT '用户已读时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_notification (user_id, notification_id),
    KEY idx_user_hidden (user_id, hidden)
) COMMENT='通知用户维度的已读/隐藏';

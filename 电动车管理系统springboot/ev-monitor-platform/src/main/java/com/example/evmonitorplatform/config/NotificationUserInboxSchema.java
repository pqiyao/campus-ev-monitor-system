package com.example.evmonitorplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时自动建表，避免未执行 SQL 脚本时 /hide、已读 等接口 500。
 * CREATE IF NOT EXISTS 可重复执行。
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class NotificationUserInboxSchema implements ApplicationRunner {

    private final DataSource dataSource;

    private volatile boolean ready;

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS notification_user_inbox (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                notification_id INT NOT NULL,
                hidden TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=用户已从列表移除',
                read_at DATETIME NULL COMMENT '用户已读时间',
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_user_notification (user_id, notification_id),
                KEY idx_user_hidden (user_id, hidden)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知用户维度的已读/隐藏'
            """;

    @Override
    public void run(ApplicationArguments args) {
        if (ensureTableExists()) {
            log.info("数据表 notification_user_inbox 已就绪");
        }
    }

    /**
     * @return 表可用（已存在或新建成功）
     */
    public synchronized boolean ensureTableExists() {
        if (ready) {
            return true;
        }
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute(DDL);
            ready = true;
            return true;
        } catch (Exception e) {
            log.error("创建或校验 notification_user_inbox 失败: {}", e.getMessage());
            return false;
        }
    }
}

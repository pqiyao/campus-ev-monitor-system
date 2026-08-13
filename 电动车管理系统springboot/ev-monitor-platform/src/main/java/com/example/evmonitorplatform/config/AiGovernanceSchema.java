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
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AiGovernanceSchema implements ApplicationRunner {

    private final DataSource dataSource;
    private volatile boolean ready;

    private static final List<String> DDL = List.of(
            """
            CREATE TABLE IF NOT EXISTS ai_event (
                event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_code VARCHAR(48) NOT NULL,
                event_type VARCHAR(32) NOT NULL,
                source_type VARCHAR(32) NOT NULL,
                device_id VARCHAR(64) NULL,
                occurred_at DATETIME NOT NULL,
                location_name VARCHAR(128) NULL,
                longitude DECIMAL(11, 7) NULL,
                latitude DECIMAL(10, 7) NULL,
                license_plate VARCHAR(32) NULL,
                vehicle_id INT NULL,
                user_id INT NULL,
                identity_method VARCHAR(32) NULL,
                identity_confidence DECIMAL(5, 4) NOT NULL DEFAULT 0,
                detection_confidence DECIMAL(5, 4) NOT NULL DEFAULT 0,
                evidence_urls TEXT NULL,
                raw_payload LONGTEXT NULL,
                status VARCHAR(32) NOT NULL,
                risk_level VARCHAR(16) NULL,
                dedupe_key VARCHAR(96) NOT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_ai_event_code (event_code),
                UNIQUE KEY uk_ai_event_dedupe (dedupe_key),
                KEY idx_ai_event_status_time (status, occurred_at),
                KEY idx_ai_event_vehicle (vehicle_id, occurred_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI原始事件'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_analysis (
                analysis_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_id BIGINT NOT NULL,
                provider VARCHAR(32) NOT NULL,
                model_name VARCHAR(64) NOT NULL,
                prompt_version VARCHAR(32) NOT NULL,
                valid_event TINYINT(1) NOT NULL DEFAULT 0,
                confidence DECIMAL(5, 4) NOT NULL DEFAULT 0,
                risk_level VARCHAR(16) NULL,
                identity_reliable TINYINT(1) NOT NULL DEFAULT 0,
                evidence_sufficient TINYINT(1) NOT NULL DEFAULT 0,
                summary VARCHAR(1000) NULL,
                recommended_action VARCHAR(64) NULL,
                notification_text VARCHAR(1000) NULL,
                observations_json TEXT NULL,
                raw_response LONGTEXT NULL,
                latency_ms INT NOT NULL DEFAULT 0,
                input_tokens INT NULL,
                output_tokens INT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_ai_analysis_event (event_id, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI研判结果'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_evidence (
                evidence_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_id BIGINT NOT NULL,
                evidence_type VARCHAR(24) NOT NULL,
                evidence_url VARCHAR(1000) NOT NULL,
                sha256 VARCHAR(64) NULL,
                metadata_json TEXT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_ai_evidence_event (event_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI事件证据'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_identity_match (
                match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_id BIGINT NOT NULL,
                vehicle_id INT NULL,
                user_id INT NULL,
                match_method VARCHAR(32) NOT NULL,
                confidence DECIMAL(5, 4) NOT NULL DEFAULT 0,
                match_status VARCHAR(24) NOT NULL,
                explanation VARCHAR(500) NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_ai_match_event (event_id),
                KEY idx_ai_match_vehicle (vehicle_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI事件身份匹配'
            """,
            """
            CREATE TABLE IF NOT EXISTS governance_incident (
                incident_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                incident_code VARCHAR(48) NOT NULL,
                event_id BIGINT NOT NULL,
                vehicle_id INT NULL,
                user_id INT NULL,
                event_type VARCHAR(32) NOT NULL,
                title VARCHAR(200) NOT NULL,
                summary VARCHAR(1000) NULL,
                risk_level VARCHAR(16) NOT NULL,
                decision_type VARCHAR(32) NOT NULL,
                state VARCHAR(32) NOT NULL,
                deadline DATETIME NULL,
                violation_id INT NULL,
                reviewer_id INT NULL,
                review_remark VARCHAR(500) NULL,
                reviewed_at DATETIME NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_incident_code (incident_code),
                UNIQUE KEY uk_incident_event (event_id),
                KEY idx_incident_state_time (state, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI治理事件'
            """,
            """
            CREATE TABLE IF NOT EXISTS workflow_task (
                task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                incident_id BIGINT NOT NULL,
                task_type VARCHAR(32) NOT NULL,
                task_title VARCHAR(200) NOT NULL,
                assignee_id INT NULL,
                state VARCHAR(24) NOT NULL,
                priority VARCHAR(16) NOT NULL,
                due_at DATETIME NULL,
                completed_at DATETIME NULL,
                result_remark VARCHAR(500) NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                KEY idx_workflow_incident (incident_id),
                KEY idx_workflow_state_due (state, due_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI治理任务'
            """,
            """
            CREATE TABLE IF NOT EXISTS decision_log (
                decision_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_id BIGINT NOT NULL,
                incident_id BIGINT NULL,
                decision_source VARCHAR(24) NOT NULL,
                decision_code VARCHAR(32) NOT NULL,
                rule_hits_json TEXT NULL,
                explanation VARCHAR(1000) NULL,
                operator_user_id INT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_decision_event (event_id, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI治理决策审计'
            """,
            """
            CREATE TABLE IF NOT EXISTS event_timeline (
                timeline_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_id BIGINT NOT NULL,
                incident_id BIGINT NULL,
                stage VARCHAR(32) NOT NULL,
                title VARCHAR(128) NOT NULL,
                detail VARCHAR(1000) NULL,
                actor_type VARCHAR(24) NOT NULL,
                actor_id INT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_timeline_event (event_id, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI事件处理时间线'
            """,
            """
            CREATE TABLE IF NOT EXISTS policy_document (
                document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                category VARCHAR(64) NOT NULL,
                version VARCHAR(32) NULL,
                effective_date DATE NULL,
                content LONGTEXT NOT NULL,
                source VARCHAR(500) NULL,
                status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                KEY idx_policy_status_category (status, category)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园治理规则知识库'
            """,
            """
            CREATE TABLE IF NOT EXISTS policy_chunk (
                chunk_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                document_id BIGINT NOT NULL,
                section_code VARCHAR(64) NULL,
                heading VARCHAR(200) NOT NULL,
                content TEXT NOT NULL,
                keywords VARCHAR(500) NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_policy_chunk_document (document_id),
                KEY idx_policy_chunk_section (section_code)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则知识分段'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_event_relation (
                relation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                source_event_id BIGINT NOT NULL,
                target_event_id BIGINT NOT NULL,
                relation_type VARCHAR(32) NOT NULL,
                confidence DECIMAL(5,4) NOT NULL DEFAULT 0,
                explanation VARCHAR(1000) NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_event_relation (source_event_id, target_event_id, relation_type),
                KEY idx_relation_source (source_event_id),
                KEY idx_relation_target (target_event_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨设备与重复事件关联'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_assistant_session (
                session_id VARCHAR(48) PRIMARY KEY,
                user_id INT NOT NULL,
                title VARCHAR(200) NOT NULL,
                context_event_id BIGINT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                KEY idx_assistant_user_time (user_id, update_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI治理助手会话'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_assistant_message (
                message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                session_id VARCHAR(48) NOT NULL,
                role VARCHAR(16) NOT NULL,
                content LONGTEXT NOT NULL,
                intent VARCHAR(48) NULL,
                tool_names VARCHAR(500) NULL,
                citations_json TEXT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_assistant_message_session (session_id, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI治理助手消息'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_generated_report (
                report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                report_code VARCHAR(48) NOT NULL,
                report_type VARCHAR(32) NOT NULL,
                title VARCHAR(200) NOT NULL,
                period_start DATETIME NOT NULL,
                period_end DATETIME NOT NULL,
                content_json LONGTEXT NOT NULL,
                generated_by INT NOT NULL,
                model_name VARCHAR(64) NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_ai_report_code (report_code),
                KEY idx_ai_report_time (create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI自动治理报告'
            """,
            """
            CREATE TABLE IF NOT EXISTS ai_appeal_review (
                review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                appeal_id INT NOT NULL,
                conclusion VARCHAR(32) NOT NULL,
                confidence DECIMAL(5,4) NOT NULL DEFAULT 0,
                evidence_analysis TEXT NULL,
                policy_refs_json TEXT NULL,
                recommendation VARCHAR(64) NOT NULL,
                model_name VARCHAR(64) NULL,
                raw_response LONGTEXT NULL,
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                KEY idx_ai_appeal_review (appeal_id, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI申诉预审建议'
            """,
            """
            INSERT INTO violation_type(type_id, type_name, description, deduction_points, penalty_amount, appeal_deadline)
            SELECT 9, '未佩戴头盔', '骑乘校园电动车时未按规定佩戴安全头盔', 2, 0, 72
            WHERE NOT EXISTS (SELECT 1 FROM violation_type WHERE type_id = 9)
            """
    );

    @Override
    public void run(ApplicationArguments args) {
        if (ensureTablesExist()) {
            log.info("AI治理数据表已就绪");
        }
    }

    public synchronized boolean ensureTablesExist() {
        if (ready) {
            return true;
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String ddl : DDL) {
                statement.execute(ddl);
            }
            ready = true;
            return true;
        } catch (Exception ex) {
            log.error("创建或校验AI治理数据表失败: {}", ex.getMessage(), ex);
            return false;
        }
    }
}

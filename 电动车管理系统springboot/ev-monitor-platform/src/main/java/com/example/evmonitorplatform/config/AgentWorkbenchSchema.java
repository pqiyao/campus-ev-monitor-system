package com.example.evmonitorplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates only the optional Agent audit tables; no existing business table is changed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentWorkbenchSchema implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_agent_run (
                  run_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  run_code VARCHAR(64) NOT NULL UNIQUE,
                  user_id INT NULL,
                  goal TEXT NOT NULL,
                  dsh_session_id VARCHAR(128) NULL,
                  preset VARCHAR(64) NULL,
                  state VARCHAR(24) NOT NULL,
                  context_json LONGTEXT NULL,
                  result_json LONGTEXT NULL,
                  error_message VARCHAR(1000) NULL,
                  started_at DATETIME NOT NULL,
                  finished_at DATETIME NULL,
                  KEY idx_agent_run_session(dsh_session_id),
                  KEY idx_agent_run_state_time(state, started_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_agent_tool_call (
                  call_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  run_id BIGINT NOT NULL,
                  tool_name VARCHAR(128) NOT NULL,
                  access_mode VARCHAR(16) NOT NULL DEFAULT 'READ',
                  request_summary TEXT NULL,
                  response_summary TEXT NULL,
                  state VARCHAR(24) NOT NULL,
                  duration_ms BIGINT NULL,
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  KEY idx_agent_tool_run(run_id, create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        } catch (Exception ex) {
            // The workbench remains usable in read-only mode if a deployment
            // account cannot create optional audit tables.
            log.warn("Agent 审计表初始化失败，工作台仍可运行: {}", ex.getMessage());
        }
    }
}

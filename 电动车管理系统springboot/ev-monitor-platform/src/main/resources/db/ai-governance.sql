-- AI 全链路治理表。应用启动时会用 IF NOT EXISTS 自动校验；此文件用于正式迁移和审计。
CREATE TABLE IF NOT EXISTS ai_event (
  event_id BIGINT AUTO_INCREMENT PRIMARY KEY, event_code VARCHAR(48) NOT NULL, event_type VARCHAR(32) NOT NULL,
  source_type VARCHAR(32) NOT NULL, device_id VARCHAR(64), occurred_at DATETIME NOT NULL, location_name VARCHAR(128),
  longitude DECIMAL(11,7), latitude DECIMAL(10,7), license_plate VARCHAR(32), vehicle_id INT, user_id INT,
  identity_method VARCHAR(32), identity_confidence DECIMAL(5,4) NOT NULL DEFAULT 0, detection_confidence DECIMAL(5,4) NOT NULL DEFAULT 0,
  evidence_urls TEXT, raw_payload LONGTEXT, status VARCHAR(32) NOT NULL, risk_level VARCHAR(16), dedupe_key VARCHAR(96) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_event_code(event_code), UNIQUE KEY uk_ai_event_dedupe(dedupe_key), KEY idx_ai_event_status_time(status, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_analysis (
  analysis_id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL, provider VARCHAR(32) NOT NULL, model_name VARCHAR(64) NOT NULL,
  prompt_version VARCHAR(32) NOT NULL, valid_event TINYINT(1) NOT NULL DEFAULT 0, confidence DECIMAL(5,4) NOT NULL DEFAULT 0,
  risk_level VARCHAR(16), identity_reliable TINYINT(1) NOT NULL DEFAULT 0, evidence_sufficient TINYINT(1) NOT NULL DEFAULT 0,
  summary VARCHAR(1000), recommended_action VARCHAR(64), notification_text VARCHAR(1000), observations_json TEXT, raw_response LONGTEXT,
  latency_ms INT NOT NULL DEFAULT 0, input_tokens INT, output_tokens INT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_analysis_event(event_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_evidence (
  evidence_id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL, evidence_type VARCHAR(24) NOT NULL,
  evidence_url VARCHAR(1000) NOT NULL, sha256 VARCHAR(64), metadata_json TEXT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_evidence_event(event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_identity_match (
  match_id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL, vehicle_id INT, user_id INT, match_method VARCHAR(32) NOT NULL,
  confidence DECIMAL(5,4) NOT NULL DEFAULT 0, match_status VARCHAR(24) NOT NULL, explanation VARCHAR(500), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_match_event(event_id), KEY idx_ai_match_vehicle(vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS governance_incident (
  incident_id BIGINT AUTO_INCREMENT PRIMARY KEY, incident_code VARCHAR(48) NOT NULL, event_id BIGINT NOT NULL, vehicle_id INT, user_id INT,
  event_type VARCHAR(32) NOT NULL, title VARCHAR(200) NOT NULL, summary VARCHAR(1000), risk_level VARCHAR(16) NOT NULL,
  decision_type VARCHAR(32) NOT NULL, state VARCHAR(32) NOT NULL, deadline DATETIME, violation_id INT, reviewer_id INT,
  review_remark VARCHAR(500), reviewed_at DATETIME, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE KEY uk_incident_code(incident_code), UNIQUE KEY uk_incident_event(event_id), KEY idx_incident_state_time(state, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS workflow_task (
  task_id BIGINT AUTO_INCREMENT PRIMARY KEY, incident_id BIGINT NOT NULL, task_type VARCHAR(32) NOT NULL, task_title VARCHAR(200) NOT NULL,
  assignee_id INT, state VARCHAR(24) NOT NULL, priority VARCHAR(16) NOT NULL, due_at DATETIME, completed_at DATETIME, result_remark VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_workflow_incident(incident_id), KEY idx_workflow_state_due(state, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS decision_log (
  decision_id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL, incident_id BIGINT, decision_source VARCHAR(24) NOT NULL,
  decision_code VARCHAR(32) NOT NULL, rule_hits_json TEXT, explanation VARCHAR(1000), operator_user_id INT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_decision_event(event_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_timeline (
  timeline_id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL, incident_id BIGINT, stage VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL, detail VARCHAR(1000), actor_type VARCHAR(24) NOT NULL, actor_id INT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_timeline_event(event_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS policy_document (
  document_id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200) NOT NULL, category VARCHAR(64) NOT NULL,
  version VARCHAR(32), effective_date DATE, content LONGTEXT NOT NULL, source VARCHAR(500), status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_policy_status_category(status, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS policy_chunk (
  chunk_id BIGINT AUTO_INCREMENT PRIMARY KEY, document_id BIGINT NOT NULL, section_code VARCHAR(64), heading VARCHAR(200) NOT NULL,
  content TEXT NOT NULL, keywords VARCHAR(500), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_policy_chunk_document(document_id), KEY idx_policy_chunk_section(section_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_event_relation (
  relation_id BIGINT AUTO_INCREMENT PRIMARY KEY, source_event_id BIGINT NOT NULL, target_event_id BIGINT NOT NULL,
  relation_type VARCHAR(32) NOT NULL, confidence DECIMAL(5,4) NOT NULL DEFAULT 0, explanation VARCHAR(1000),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_event_relation(source_event_id, target_event_id, relation_type),
  KEY idx_relation_source(source_event_id), KEY idx_relation_target(target_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_assistant_session (
  session_id VARCHAR(48) PRIMARY KEY, user_id INT NOT NULL, title VARCHAR(200) NOT NULL, context_event_id BIGINT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_assistant_user_time(user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_assistant_message (
  message_id BIGINT AUTO_INCREMENT PRIMARY KEY, session_id VARCHAR(48) NOT NULL, role VARCHAR(16) NOT NULL,
  content LONGTEXT NOT NULL, intent VARCHAR(48), tool_names VARCHAR(500), citations_json TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_assistant_message_session(session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_generated_report (
  report_id BIGINT AUTO_INCREMENT PRIMARY KEY, report_code VARCHAR(48) NOT NULL, report_type VARCHAR(32) NOT NULL,
  title VARCHAR(200) NOT NULL, period_start DATETIME NOT NULL, period_end DATETIME NOT NULL, content_json LONGTEXT NOT NULL,
  generated_by INT NOT NULL, model_name VARCHAR(64), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ai_report_code(report_code), KEY idx_ai_report_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_appeal_review (
  review_id BIGINT AUTO_INCREMENT PRIMARY KEY, appeal_id INT NOT NULL, conclusion VARCHAR(32) NOT NULL,
  confidence DECIMAL(5,4) NOT NULL DEFAULT 0, evidence_analysis TEXT, policy_refs_json TEXT,
  recommendation VARCHAR(64) NOT NULL, model_name VARCHAR(64), raw_response LONGTEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_ai_appeal_review(appeal_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

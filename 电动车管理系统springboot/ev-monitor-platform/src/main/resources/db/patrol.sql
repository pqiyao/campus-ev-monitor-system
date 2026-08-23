-- Patrol extension tables. These tables extend the existing governance system;
-- they do not replace ai_event, governance_incident, or workflow_task.
CREATE TABLE IF NOT EXISTS patrol_mission (
  mission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_code VARCHAR(48) NOT NULL UNIQUE,
  goal TEXT NOT NULL,
  area_name VARCHAR(128) NULL,
  center_lng DECIMAL(11,7) NOT NULL,
  center_lat DECIMAL(10,7) NOT NULL,
  duration_limit_sec INT NOT NULL DEFAULT 720,
  state VARCHAR(24) NOT NULL,
  battery_start DECIMAL(5,2) NOT NULL DEFAULT 100,
  battery_current DECIMAL(5,2) NOT NULL DEFAULT 100,
  plan_version INT NOT NULL DEFAULT 0,
  replans INT NOT NULL DEFAULT 0,
  created_by INT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_patrol_mission_state_time(state, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patrol_waypoint (
  waypoint_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  lng DECIMAL(11,7) NOT NULL,
  lat DECIMAL(10,7) NOT NULL,
  altitude_m DECIMAL(7,2) NOT NULL DEFAULT 30,
  dwell_sec INT NOT NULL DEFAULT 3,
  risk_score DECIMAL(8,4) NOT NULL DEFAULT 0,
  state VARCHAR(24) NOT NULL DEFAULT 'PLANNED',
  UNIQUE KEY uk_patrol_waypoint(mission_id, sequence_no),
  KEY idx_patrol_waypoint_mission(mission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS drone_telemetry (
  telemetry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  recorded_at DATETIME NOT NULL,
  lng DECIMAL(11,7) NOT NULL,
  lat DECIMAL(10,7) NOT NULL,
  altitude_m DECIMAL(7,2) NULL,
  speed_mps DECIMAL(7,2) NULL,
  battery DECIMAL(5,2) NULL,
  gps_quality DECIMAL(5,2) NULL,
  payload_state VARCHAR(32) NULL,
  raw_payload TEXT NULL,
  KEY idx_drone_telemetry_mission(mission_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_step (
  step_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  phase VARCHAR(24) NOT NULL,
  step_no INT NOT NULL,
  title VARCHAR(128) NOT NULL,
  detail TEXT NULL,
  state VARCHAR(24) NOT NULL,
  duration_ms BIGINT NULL,
  create_time DATETIME NOT NULL,
  KEY idx_agent_step_mission(mission_id, step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tool_execution (
  execution_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  step_id BIGINT NULL,
  tool_name VARCHAR(64) NOT NULL,
  input_summary TEXT NULL,
  output_summary TEXT NULL,
  state VARCHAR(24) NOT NULL,
  duration_ms BIGINT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  error_message TEXT NULL,
  create_time DATETIME NOT NULL,
  KEY idx_tool_execution_mission(mission_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patrol_goal_plan (
  plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL UNIQUE,
  provider VARCHAR(32) NOT NULL,
  model_name VARCHAR(64) NOT NULL,
  plan_json LONGTEXT NOT NULL,
  raw_response LONGTEXT NULL,
  latency_ms BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  KEY idx_patrol_goal_plan_mission(mission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patrol_plan_change (
  change_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  from_version INT NOT NULL,
  to_version INT NOT NULL,
  reason VARCHAR(500) NULL,
  preserved_count INT NOT NULL DEFAULT 0,
  removed_count INT NOT NULL DEFAULT 0,
  added_count INT NOT NULL DEFAULT 0,
  diff_json LONGTEXT NULL,
  create_time DATETIME NOT NULL,
  KEY idx_patrol_plan_change_mission(mission_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patrol_fault_injection (
  fault_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  fault_type VARCHAR(32) NOT NULL,
  parameters_json TEXT NULL,
  injected_at DATETIME NOT NULL,
  recovered_at DATETIME NULL,
  state VARCHAR(24) NOT NULL DEFAULT 'INJECTED',
  KEY idx_patrol_fault_mission(mission_id, injected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patrol_event_link (
  link_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  waypoint_id BIGINT NULL,
  relation_type VARCHAR(32) NOT NULL DEFAULT 'DISCOVERED',
  trigger_replan TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_patrol_event_link(mission_id, event_id),
  KEY idx_patrol_event_mission(mission_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patrol_risk_grid (
  grid_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  col_no INT NOT NULL,
  center_lng DECIMAL(11,7) NOT NULL,
  center_lat DECIMAL(10,7) NOT NULL,
  recent_score DECIMAL(8,4) NOT NULL DEFAULT 0,
  density_score DECIMAL(8,4) NOT NULL DEFAULT 0,
  fence_score DECIMAL(8,4) NOT NULL DEFAULT 0,
  blind_score DECIMAL(8,4) NOT NULL DEFAULT 0,
  risk_score DECIMAL(8,4) NOT NULL DEFAULT 0,
  selected TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_patrol_grid(mission_id, row_no, col_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mission_report (
  report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NOT NULL UNIQUE,
  report_code VARCHAR(48) NOT NULL UNIQUE,
  content_json LONGTEXT NOT NULL,
  create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patrol_audit_log (
  audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mission_id BIGINT NULL,
  operator_user_id INT NULL,
  action VARCHAR(48) NOT NULL,
  input_summary TEXT NULL,
  result_summary TEXT NULL,
  state VARCHAR(24) NOT NULL,
  create_time DATETIME NOT NULL,
  KEY idx_patrol_audit_mission(mission_id, create_time),
  KEY idx_patrol_audit_operator(operator_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

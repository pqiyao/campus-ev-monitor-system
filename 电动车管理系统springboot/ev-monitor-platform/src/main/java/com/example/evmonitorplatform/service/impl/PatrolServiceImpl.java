package com.example.evmonitorplatform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.service.AiGovernanceService;
import com.example.evmonitorplatform.service.PatrolService;
import com.example.evmonitorplatform.service.drone.DroneAdapter;
import com.example.evmonitorplatform.service.drone.DroneEvent;
import com.example.evmonitorplatform.service.drone.DronePlan;
import com.example.evmonitorplatform.service.drone.DroneTelemetry;
import com.example.evmonitorplatform.service.drone.DroneWaypoint;
import com.example.evmonitorplatform.service.ai.PatrolGoalPlanner;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolServiceImpl implements PatrolService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AiGovernanceService aiGovernanceService;
    private final DroneAdapter droneAdapter;
    private final PatrolGoalPlanner patrolGoalPlanner;
    private final ScheduledExecutorService simulator = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<Long, ScheduledFuture<?>> simulationTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SimulationState> simulationStates = new ConcurrentHashMap<>();
    private volatile boolean schemaReady;

    private void ensureSchema() {
        if (schemaReady) return;
        synchronized (this) {
            if (schemaReady) return;
            List<String> ddl = List.of(
                    "CREATE TABLE IF NOT EXISTS patrol_mission (mission_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_code VARCHAR(48) NOT NULL UNIQUE, goal TEXT NOT NULL, area_name VARCHAR(128) NULL, center_lng DECIMAL(11,7) NOT NULL, center_lat DECIMAL(10,7) NOT NULL, duration_limit_sec INT NOT NULL DEFAULT 720, state VARCHAR(24) NOT NULL, battery_start DECIMAL(5,2) NOT NULL DEFAULT 100, battery_current DECIMAL(5,2) NOT NULL DEFAULT 100, plan_version INT NOT NULL DEFAULT 0, replans INT NOT NULL DEFAULT 0, created_by INT NULL, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS patrol_waypoint (waypoint_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, sequence_no INT NOT NULL, lng DECIMAL(11,7) NOT NULL, lat DECIMAL(10,7) NOT NULL, altitude_m DECIMAL(7,2) NOT NULL DEFAULT 30, dwell_sec INT NOT NULL DEFAULT 3, risk_score DECIMAL(8,4) NOT NULL DEFAULT 0, state VARCHAR(24) NOT NULL DEFAULT 'PLANNED', UNIQUE KEY uk_patrol_waypoint (mission_id, sequence_no), KEY idx_patrol_waypoint_mission (mission_id))",
                    "CREATE TABLE IF NOT EXISTS drone_telemetry (telemetry_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, recorded_at DATETIME NOT NULL, lng DECIMAL(11,7) NOT NULL, lat DECIMAL(10,7) NOT NULL, altitude_m DECIMAL(7,2) NULL, speed_mps DECIMAL(7,2) NULL, battery DECIMAL(5,2) NULL, gps_quality DECIMAL(5,2) NULL, payload_state VARCHAR(32) NULL, raw_payload TEXT NULL, KEY idx_drone_telemetry_mission (mission_id, recorded_at))",
                    "CREATE TABLE IF NOT EXISTS agent_step (step_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, phase VARCHAR(24) NOT NULL, step_no INT NOT NULL, title VARCHAR(128) NOT NULL, detail TEXT NULL, state VARCHAR(24) NOT NULL, duration_ms BIGINT NULL, create_time DATETIME NOT NULL, KEY idx_agent_step_mission (mission_id, step_no))",
                    "CREATE TABLE IF NOT EXISTS tool_execution (execution_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, step_id BIGINT NULL, tool_name VARCHAR(64) NOT NULL, input_summary TEXT NULL, output_summary TEXT NULL, state VARCHAR(24) NOT NULL, duration_ms BIGINT NULL, retry_count INT NOT NULL DEFAULT 0, error_message TEXT NULL, create_time DATETIME NOT NULL, KEY idx_tool_execution_mission (mission_id, create_time))",
                    "CREATE TABLE IF NOT EXISTS patrol_goal_plan (plan_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL UNIQUE, provider VARCHAR(32) NOT NULL, model_name VARCHAR(64) NOT NULL, plan_json LONGTEXT NOT NULL, raw_response LONGTEXT NULL, latency_ms BIGINT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL, KEY idx_patrol_goal_plan_mission (mission_id))",
                    "CREATE TABLE IF NOT EXISTS patrol_plan_change (change_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, from_version INT NOT NULL, to_version INT NOT NULL, reason VARCHAR(500) NULL, preserved_count INT NOT NULL DEFAULT 0, removed_count INT NOT NULL DEFAULT 0, added_count INT NOT NULL DEFAULT 0, diff_json LONGTEXT NULL, create_time DATETIME NOT NULL, KEY idx_patrol_plan_change_mission (mission_id,create_time))",
                    "CREATE TABLE IF NOT EXISTS patrol_fault_injection (fault_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, fault_type VARCHAR(32) NOT NULL, parameters_json TEXT NULL, injected_at DATETIME NOT NULL, recovered_at DATETIME NULL, state VARCHAR(24) NOT NULL DEFAULT 'INJECTED', KEY idx_patrol_fault_mission (mission_id,injected_at))",
                    "CREATE TABLE IF NOT EXISTS patrol_event_link (link_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, event_id BIGINT NOT NULL, waypoint_id BIGINT NULL, relation_type VARCHAR(32) NOT NULL DEFAULT 'DISCOVERED', trigger_replan TINYINT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL, UNIQUE KEY uk_patrol_event_link (mission_id,event_id), KEY idx_patrol_event_mission (mission_id,create_time))",
                    "CREATE TABLE IF NOT EXISTS patrol_risk_grid (grid_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL, row_no INT NOT NULL, col_no INT NOT NULL, center_lng DECIMAL(11,7) NOT NULL, center_lat DECIMAL(10,7) NOT NULL, recent_score DECIMAL(8,4) NOT NULL DEFAULT 0, density_score DECIMAL(8,4) NOT NULL DEFAULT 0, fence_score DECIMAL(8,4) NOT NULL DEFAULT 0, blind_score DECIMAL(8,4) NOT NULL DEFAULT 0, risk_score DECIMAL(8,4) NOT NULL DEFAULT 0, selected TINYINT NOT NULL DEFAULT 0, UNIQUE KEY uk_patrol_grid (mission_id,row_no,col_no))",
                    "CREATE TABLE IF NOT EXISTS mission_report (report_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NOT NULL UNIQUE, report_code VARCHAR(48) NOT NULL UNIQUE, content_json LONGTEXT NOT NULL, create_time DATETIME NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS patrol_audit_log (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NULL, operator_user_id INT NULL, action VARCHAR(48) NOT NULL, input_summary TEXT NULL, result_summary TEXT NULL, state VARCHAR(24) NOT NULL, create_time DATETIME NOT NULL, KEY idx_patrol_audit_mission (mission_id,create_time), KEY idx_patrol_audit_operator (operator_user_id,create_time))"
            );
            ddl.forEach(jdbcTemplate::execute);
            schemaReady = true;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createMission(Map<String, Object> request, Integer operatorUserId) {
        ensureSchema();
        String goal = text(request, "goal", "巡查东校区电动车安全风险");
        String area = text(request, "areaName", "东校区");
        double lng = number(request, "centerLng", 110.3239130);
        double lat = number(request, "centerLat", 25.2875179);
        validateCoordinate(lng, lat, "巡检中心点");
        int duration = Math.max(60, Math.min(3600, integer(request, "durationLimitSec", 720)));
        String code = "PM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        jdbcTemplate.update("INSERT INTO patrol_mission (mission_code,goal,area_name,center_lng,center_lat,duration_limit_sec,state,created_by,create_time,update_time) VALUES (?,?,?,?,?,?,'DRAFT',?,?,?)", code, goal, area, lng, lat, duration, operatorUserId, LocalDateTime.now(), LocalDateTime.now());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        recordAudit(id, operatorUserId, "CREATE_MISSION", Map.of("area", area, "durationLimitSec", duration), Map.of("missionCode", code), "SUCCEEDED");
        PatrolGoalPlanner.PlannerResult goalPlan = patrolGoalPlanner.plan(goal, area, duration);
        jdbcTemplate.update("INSERT INTO patrol_goal_plan (mission_id,provider,model_name,plan_json,raw_response,latency_ms,create_time) VALUES (?,?,?,?,?,?,?)",
                id, goalPlan.provider(), goalPlan.model(), json(goalPlan.plan()), goalPlan.rawResponse(), goalPlan.latencyMs(), LocalDateTime.now());
        recordStep(id, "UNDERSTAND", 1, "解析巡检目标", "区域=" + area + ";时限=" + duration + "秒", "COMPLETED", 0L);
        recordTool(id, "PARSE_PATROL_GOAL", Map.of("goal", goal, "area", area),
                Map.of("provider", goalPlan.provider(), "plan", goalPlan.plan()), "SUCCEEDED", goalPlan.latencyMs(), 0, null);
        return getMission(id);
    }

    @Override
    public Map<String, Object> getMission(Long id) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        mission.put("waypoints", jdbcTemplate.queryForList("SELECT * FROM patrol_waypoint WHERE mission_id=? ORDER BY sequence_no", id));
        mission.put("riskGrid", jdbcTemplate.queryForList("SELECT * FROM patrol_risk_grid WHERE mission_id=? ORDER BY risk_score DESC", id));
        mission.put("telemetry", jdbcTemplate.queryForList("SELECT * FROM drone_telemetry WHERE mission_id=? ORDER BY recorded_at DESC LIMIT 100", id));
        mission.put("fences", safeRows("SELECT fence_id,fence_name,fence_type,coordinates,center_lng,center_lat,alert_level FROM geofence WHERE status=1 AND deleted=0"));
        mission.put("events", safeRows("SELECT e.*,l.link_id,l.waypoint_id,l.relation_type,l.trigger_replan FROM patrol_event_link l JOIN ai_event e ON e.event_id=l.event_id WHERE l.mission_id=? ORDER BY e.occurred_at DESC", id));
        mission.put("steps", jdbcTemplate.queryForList("SELECT * FROM agent_step WHERE mission_id=? ORDER BY step_no, create_time", id));
        mission.put("tools", jdbcTemplate.queryForList("SELECT * FROM tool_execution WHERE mission_id=? ORDER BY create_time", id));
        mission.put("audit", safeRows("SELECT audit_id,operator_user_id,action,input_summary,result_summary,state,create_time FROM patrol_audit_log WHERE mission_id=? ORDER BY create_time DESC LIMIT 100", id));
        List<Map<String, Object>> goalPlans = safeRows("SELECT provider,model_name,plan_json,raw_response,latency_ms,create_time FROM patrol_goal_plan WHERE mission_id=?", id);
        mission.put("goalPlan", goalPlans.isEmpty() ? null : goalPlans.get(0));
        mission.put("planChanges", safeRows("SELECT change_id,from_version,to_version,reason,preserved_count,removed_count,added_count,diff_json,create_time FROM patrol_plan_change WHERE mission_id=? ORDER BY create_time DESC LIMIT 20", id));
        mission.put("faults", safeRows("SELECT fault_type,parameters_json,injected_at,recovered_at,state FROM patrol_fault_injection WHERE mission_id=? ORDER BY injected_at DESC LIMIT 20", id));
        return mission;
    }

    @Override
    public List<Map<String, Object>> listMissions(String state, Integer limit) {
        ensureSchema();
        int size = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        String base = "SELECT m.*, (SELECT COUNT(*) FROM patrol_event_link l WHERE l.mission_id=m.mission_id) AS event_count " +
                "FROM patrol_mission m ";
        if (StringUtils.hasText(state)) {
            return jdbcTemplate.queryForList(base + "WHERE m.state=? ORDER BY m.create_time DESC LIMIT " + size, state.toUpperCase(Locale.ROOT));
        }
        return jdbcTemplate.queryForList(base + "ORDER BY m.create_time DESC LIMIT " + size);
    }

    @Override
    @Transactional
    public Map<String, Object> planMission(Long id, Integer operator) {
        return planMissionInternal(id, operator, false);
    }

    private Map<String, Object> planMissionInternal(Long id, Integer operator, boolean preserveCompleted) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        List<Map<String, Object>> preserved = preserveCompleted
                ? safeRows("SELECT waypoint_id,sequence_no,lng,lat,altitude_m,dwell_sec,risk_score,state FROM patrol_waypoint WHERE mission_id=? AND state='ARRIVED' ORDER BY sequence_no", id)
                : List.of();
        int oldWaypointCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", Integer.class, id);
        if (preserveCompleted) {
            jdbcTemplate.update("DELETE FROM patrol_waypoint WHERE mission_id=? AND state<>'ARRIVED'", id);
        } else {
            jdbcTemplate.update("DELETE FROM patrol_waypoint WHERE mission_id=?", id);
        }
        recordAudit(id, operator, preserveCompleted ? "REPLAN_MISSION" : "PLAN_MISSION", Map.of("preserveCompleted", preserveCompleted), Map.of("oldWaypointCount", oldWaypointCount), "STARTED");
        jdbcTemplate.update("DELETE FROM patrol_risk_grid WHERE mission_id=?", id);
        double centerLng = ((Number) mission.get("center_lng")).doubleValue();
        double centerLat = ((Number) mission.get("center_lat")).doubleValue();
        List<Map<String, Object>> events = safeRows("SELECT longitude,latitude FROM ai_event WHERE occurred_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND longitude IS NOT NULL AND latitude IS NOT NULL");
        List<Map<String, Object>> devices = safeRows("SELECT current_lng,current_lat FROM location_device WHERE current_lng IS NOT NULL AND current_lat IS NOT NULL");
        List<Map<String, Object>> fences = safeRows("SELECT fence_type,center_lng,center_lat,coordinates,status FROM geofence WHERE status=1 AND deleted=0");
        List<GridCandidate> candidates = new ArrayList<>();
        for (int row = -2; row <= 2; row++) for (int col = -2; col <= 2; col++) {
            double lng = centerLng + col * 0.00022;
            double lat = centerLat + row * 0.00018;
            double recent = densityNear(events, lng, lat, "longitude", "latitude", 0.00028, 3.0);
            double density = densityNear(devices, lng, lat, "current_lng", "current_lat", 0.00028, 5.0);
            double fence = restrictedFenceNear(fences, lng, lat);
            double blind = (Math.abs(row) == 2 || Math.abs(col) == 2) ? 0.65 : 0.15;
            double baseline = row == 0 && col == 0 ? 0.35 : 0.05;
            double score = Math.min(1.5, baseline + recent * 0.35 + density * 0.20 + fence * 0.30 + blind * 0.15);
            boolean blocked = fence >= 0.95;
            int selected = !blocked && (score >= 0.32 || row == 0 && col == 0) ? 1 : 0;
            jdbcTemplate.update("INSERT INTO patrol_risk_grid (mission_id,row_no,col_no,center_lng,center_lat,recent_score,density_score,fence_score,blind_score,risk_score,selected) VALUES (?,?,?,?,?,?,?,?,?,?,?)", id, row, col, lng, lat, recent, density, fence, blind, score, selected);
            if (selected == 1) candidates.add(new GridCandidate(lng, lat, score));
        }
        candidates.sort(Comparator.comparingDouble((GridCandidate c) -> c.score).reversed());
        double routeStartLng = centerLng;
        double routeStartLat = centerLat;
        int sequence = 0;
        if (!preserved.isEmpty()) {
            Map<String, Object> last = preserved.get(preserved.size() - 1);
            routeStartLng = number(last, "lng", centerLng);
            routeStartLat = number(last, "lat", centerLat);
            sequence = integer(last, "sequence_no", -1) + 1;
        } else {
            insertWaypoint(id, sequence++, centerLng, centerLat, 0);
        }
        List<GridCandidate> route = greedyRoute(candidates.subList(0, Math.min(8, candidates.size())), routeStartLng, routeStartLat);
        route = constrainRoute(route, routeStartLng, routeStartLat, centerLng, centerLat, fences,
                integer(mission, "duration_limit_sec", 720), number(mission, "battery_current", 100));
        if (route.isEmpty()) {
            throw new BusinessException("当前风险网格没有满足禁飞区、时限和返航电量约束的可行航线");
        }
        for (GridCandidate point : route) insertWaypoint(id, sequence++, point.lng, point.lat, point.score);
        jdbcTemplate.update("UPDATE patrol_mission SET state='PLANNED', plan_version=plan_version+1, update_time=? WHERE mission_id=?", LocalDateTime.now(), id);
        int newWaypointCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", Integer.class, id);
        recordStep(id, "PLAN", 2, "生成风险网格与初始航线", "读取近期事件、车辆位置和围栏，生成" + sequence + "个航点", "COMPLETED", 0L);
        recordTool(id, "PLAN_PATROL_ROUTE", Map.of("gridSizeMeters", 20, "durationLimitSec", mission.get("duration_limit_sec")), Map.of("waypointCount", sequence, "hardConstraints", List.of("禁飞区", "返航电量")), "SUCCEEDED", 0L, 0, null);
        if (preserveCompleted) {
            int fromVersion = integer(mission, "plan_version", 0);
            int toVersion = fromVersion + 1;
            Map<String, Object> diff = new LinkedHashMap<>();
            diff.put("preservedWaypointIds", preserved.stream().map(row -> row.get("waypoint_id")).toList());
            diff.put("preservedSequenceEnd", preserved.isEmpty() ? null : preserved.get(preserved.size() - 1).get("sequence_no"));
            diff.put("routeStart", Map.of("lng", routeStartLng, "lat", routeStartLat));
            jdbcTemplate.update("INSERT INTO patrol_plan_change (mission_id,from_version,to_version,reason,preserved_count,removed_count,added_count,diff_json,create_time) VALUES (?,?,?,?,?,?,?, ?,?)",
                    id, fromVersion, toVersion, "局部重规划", preserved.size(), Math.max(0, oldWaypointCount - preserved.size()),
                    Math.max(0, newWaypointCount - preserved.size()), json(diff), LocalDateTime.now());
        }
        return getMission(id);
    }

    @Override
    @Transactional
    public Map<String, Object> startMission(Long id, Integer operator) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        if (!Set.of("PLANNED", "PAUSED").contains(String.valueOf(mission.get("state")))) throw new BusinessException("任务必须先完成航线规划");
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", Integer.class, id) == 0) throw new BusinessException("任务没有可执行航点");
        jdbcTemplate.update("UPDATE patrol_mission SET state='RUNNING',update_time=? WHERE mission_id=?", LocalDateTime.now(), id);
        droneAdapter.start(buildDronePlan(id), this::handleDroneTelemetry, this::handleDroneEvent);
        recordStep(id, "EXECUTE", 3, "启动仿真无人机", "开始沿规划航点执行巡检", "RUNNING", 0L);
        recordTool(id, "START_SIMULATION", Map.of("missionId", id), Map.of("state", "RUNNING", "telemetryIntervalMs", 500), "SUCCEEDED", 0L, 0, null);
        recordAudit(id, operator, "START_MISSION", Map.of(), Map.of("state", "RUNNING"), "SUCCEEDED");
        return getMission(id);
    }

    @Override
    @Transactional
    public Map<String, Object> pauseMission(Long id, Integer operator) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        String state = String.valueOf(mission.get("state"));
        if (!Set.of("RUNNING", "PAUSED").contains(state)) {
            throw new BusinessException("当前任务状态不可暂停：" + state);
        }
        jdbcTemplate.update("UPDATE patrol_mission SET state='PAUSED',update_time=? WHERE mission_id=?", LocalDateTime.now(), id);
        droneAdapter.stop(id);
        cancelSimulation(id);
        recordStep(id, "MONITOR", 4, "暂停仿真任务", "等待人工或智能体继续", "PAUSED", 0L);
        recordAudit(id, operator, "PAUSE_MISSION", Map.of(), Map.of("state", "PAUSED"), "SUCCEEDED");
        return getMission(id);
    }

    @Override
    @Transactional
    public Map<String, Object> replanMission(Long id, String reason, Integer operator) {
        ensureSchema();
        Map<String, Object> before = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        boolean wasRunning = "RUNNING".equals(String.valueOf(before.get("state")));
        if (wasRunning) droneAdapter.stop(id);
        if (wasRunning) cancelSimulation(id);
        planMissionInternal(id, operator, true);
        jdbcTemplate.update("UPDATE patrol_mission SET state=?,replans=replans+1,update_time=? WHERE mission_id=?", wasRunning ? "RUNNING" : "PLANNED", LocalDateTime.now(), id);
        if (wasRunning) {
            droneAdapter.start(buildDronePlan(id), this::handleDroneTelemetry, this::handleDroneEvent);
        }
        recordStep(id, "REPLAN", 5, "动态重规划", reason == null ? "检测到新风险" : reason, "COMPLETED", 0L);
        recordTool(id, "REPLAN_ROUTE", Map.of("reason", reason == null ? "new-risk" : reason), Map.of("planVersion", jdbcTemplate.queryForObject("SELECT plan_version FROM patrol_mission WHERE mission_id=?", Integer.class, id)), "SUCCEEDED", 0L, 0, null);
        recordAudit(id, operator, "REPLAN_MISSION", Map.of("reason", reason == null ? "new-risk" : reason), Map.of("state", wasRunning ? "RUNNING" : "PLANNED"), "SUCCEEDED");
        return getMission(id);
    }

    @Override
    @Transactional
    public Map<String, Object> appendTelemetry(Long id, Map<String, Object> telemetry) {
        ensureSchema();
        one("SELECT mission_id FROM patrol_mission WHERE mission_id=?", id);
        double lng = number(telemetry, "lng", Double.NaN);
        double lat = number(telemetry, "lat", Double.NaN);
        double battery = number(telemetry, "battery", Double.NaN);
        validateCoordinate(lng, lat, "遥测");
        if (!Double.isFinite(battery) || battery < 0 || battery > 100) throw new BusinessException("遥测电量必须在0到100之间");
        double gpsQuality = number(telemetry, "gpsQuality", 1);
        if (!Double.isFinite(gpsQuality) || gpsQuality < 0 || gpsQuality > 1) throw new BusinessException("GPS质量必须在0到1之间");
        double altitude = number(telemetry, "altitudeM", 30);
        double speed = number(telemetry, "speedMps", 3);
        if (!Double.isFinite(altitude) || altitude < 0 || altitude > 500) throw new BusinessException("遥测高度必须在0到500米之间");
        if (!Double.isFinite(speed) || speed < 0 || speed > 100) throw new BusinessException("遥测速度必须在0到100米/秒之间");
        appendTelemetryInternal(id, lng, lat, altitude, speed, battery, gpsQuality, text(telemetry, "payloadState", "READY"));
        recordAudit(id, null, "INGEST_TELEMETRY", Map.of("lng", lng, "lat", lat), Map.of("battery", battery), "SUCCEEDED");
        return Map.of("accepted", true, "missionId", id, "recordedAt", LocalDateTime.now());
    }

    @Override
    @Transactional
    public Map<String, Object> injectFault(Long id, String faultType, Map<String, Object> parameters, Integer operator) {
        ensureSchema();
        one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        String type = faultType == null ? "" : faultType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("GPS_OFFSET", "PACKET_LOSS", "NETWORK_DELAY", "LOW_BATTERY").contains(type)) {
            throw new BusinessException("不支持的故障类型：" + type);
        }
        try {
            droneAdapter.injectFault(id, type, parameters == null ? Map.of() : parameters);
        } catch (UnsupportedOperationException | IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }
        jdbcTemplate.update("INSERT INTO patrol_fault_injection (mission_id,fault_type,parameters_json,injected_at,state) VALUES (?,?,?,?, 'INJECTED')",
                id, type, json(parameters == null ? Map.of() : parameters), LocalDateTime.now());
        recordStep(id, "MONITOR", 4, "注入仿真故障", type + " " + String.valueOf(parameters), "INJECTED", 0L);
        recordTool(id, "INJECT_DRONE_FAULT", Map.of("faultType", type, "parameters", parameters == null ? Map.of() : parameters), Map.of("accepted", true), "SUCCEEDED", 0L, 0, null);
        recordAudit(id, operator, "INJECT_FAULT", Map.of("faultType", type), Map.of("accepted", true), "SUCCEEDED");
        return Map.of("accepted", true, "missionId", id, "faultType", type, "parameters", parameters == null ? Map.of() : parameters);
    }

    @Override
    @Transactional
    public Map<String, Object> clearFault(Long id, Integer operator) {
        ensureSchema();
        one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        try {
            droneAdapter.clearFault(id);
        } catch (UnsupportedOperationException ex) {
            throw new BusinessException(ex.getMessage());
        }
        jdbcTemplate.update("UPDATE patrol_fault_injection SET recovered_at=?,state='RECOVERED' WHERE mission_id=? AND state='INJECTED'", LocalDateTime.now(), id);
        recordStep(id, "MONITOR", 4, "恢复仿真故障", "已清除当前任务故障注入", "RECOVERED", 0L);
        recordTool(id, "CLEAR_DRONE_FAULT", Map.of("missionId", id), Map.of("recovered", true), "SUCCEEDED", 0L, 0, null);
        recordAudit(id, operator, "CLEAR_FAULT", Map.of(), Map.of("recovered", true), "SUCCEEDED");
        return Map.of("accepted", true, "missionId", id, "recovered", true);
    }

    @Override
    @Transactional
    public Map<String, Object> attachEvent(Long id, Map<String, Object> request, Integer operator) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        Long eventId = request.get("eventId") == null ? null : (long) integer(request, "eventId", 0);
        if (eventId == null || eventId <= 0) {
            AiEventCreateRequest event = new AiEventCreateRequest();
            event.setEventType(text(request, "eventType", "NO_HELMET"));
            String sourceType = text(request, "sourceType", "DRONE").toUpperCase(Locale.ROOT);
            if (!Set.of("DRONE", "GROUND_ROBOT", "VEHICLE_TERMINAL").contains(sourceType)) {
                throw new BusinessException("巡检事件来源必须是 DRONE、GROUND_ROBOT 或 VEHICLE_TERMINAL");
            }
            event.setSourceType(sourceType);
            event.setDeviceId("patrol-" + text(mission, "mission_code", String.valueOf(id)));
            event.setOccurredAt(LocalDateTime.now());
            event.setLocationName(text(mission, "area_name", "巡检区域"));
            Map<String, Object> latest = latestTelemetry(id);
            event.setLongitude(number(request, "longitude", number(latest, "lng", number(mission, "center_lng", 0))));
            event.setLatitude(number(request, "latitude", number(latest, "lat", number(mission, "center_lat", 0))));
            event.setLicensePlate(text(request, "licensePlate", null));
            event.setDetectionConfidence(number(request, "detectionConfidence", 0.95));
             String evidenceUrl = text(request, "evidenceUrl", "");
             event.setEvidenceUrls(StringUtils.hasText(evidenceUrl) ? List.of(evidenceUrl) : List.of());
             event.setRawPayload(Map.of("patrolMissionId", id, "source", "patrol-adapter", "evidenceMode", StringUtils.hasText(evidenceUrl) ? "uploaded" : "telemetry-only"));
            event.setAutoAnalyze(!request.containsKey("autoAnalyze") || Boolean.parseBoolean(String.valueOf(request.get("autoAnalyze"))));
            Map<String, Object> result = aiGovernanceService.ingest(event);
            eventId = longValue(result.get("event_id"));
            if (eventId == null) eventId = longValue(result.get("eventId"));
            if (eventId == null) throw new BusinessException("AI事件已接入但未返回事件编号");
        }
        List<Map<String, Object>> exists = safeRows("SELECT event_id FROM ai_event WHERE event_id=?", eventId);
        if (exists.isEmpty()) throw new BusinessException("AI事件不存在：" + eventId);
        int trigger = Boolean.parseBoolean(String.valueOf(request.getOrDefault("triggerReplan", "true"))) ? 1 : 0;
        jdbcTemplate.update("INSERT IGNORE INTO patrol_event_link (mission_id,event_id,relation_type,trigger_replan,create_time) VALUES (?,?,?, ?,?)", id, eventId, text(request, "relationType", "DISCOVERED"), trigger, LocalDateTime.now());
        recordStep(id, "OBSERVE", 4, "接入巡检AI事件", "事件 " + eventId + " 已关联到当前巡检任务", "COMPLETED", 0L);
        recordTool(id, "INGEST_PATROL_EVENT", Map.of("missionId", id, "eventId", eventId), Map.of("triggerReplan", trigger == 1), "SUCCEEDED", 0L, 0, null);
        if (trigger == 1 && Set.of("RUNNING", "PLANNED").contains(String.valueOf(mission.get("state")))) {
            replanMission(id, "发现巡检AI事件 " + eventId + "，自动插入重点复核航点", operator);
        }
        recordAudit(id, operator, "ATTACH_EVENT", Map.of("eventId", eventId), Map.of("triggerReplan", trigger == 1), "SUCCEEDED");
        return getMission(id);
    }

    @Override
    @Transactional
    public Map<String, Object> generateReport(Long id, Integer operator) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("missionCode", mission.get("mission_code"));
        report.put("goal", mission.get("goal"));
        report.put("state", mission.get("state"));
        report.put("waypointCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", Integer.class, id));
        report.put("telemetryCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM drone_telemetry WHERE mission_id=?", Integer.class, id));
        report.put("eventCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_event_link WHERE mission_id=?", Integer.class, id));
        report.put("batteryStart", mission.get("battery_start"));
        report.put("batteryCurrent", mission.get("battery_current"));
        report.put("replans", mission.get("replans"));
        report.put("planChanges", safeRows("SELECT from_version,to_version,reason,preserved_count,removed_count,added_count,diff_json,create_time FROM patrol_plan_change WHERE mission_id=? ORDER BY create_time", id));
        report.put("faults", safeRows("SELECT fault_type,parameters_json,injected_at,recovered_at,state FROM patrol_fault_injection WHERE mission_id=? ORDER BY injected_at", id));
        report.put("steps", safeRows("SELECT phase,step_no,title,detail,state,duration_ms,create_time FROM agent_step WHERE mission_id=? ORDER BY step_no,create_time", id));
        report.put("audit", safeRows("SELECT action,state,create_time FROM patrol_audit_log WHERE mission_id=? ORDER BY create_time", id));
        report.put("metrics", buildMetrics(id, mission));
        report.put("generatedAt", LocalDateTime.now());
        String code = "RPT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        report.put("reportCode", code);
        jdbcTemplate.update("INSERT INTO mission_report (mission_id,report_code,content_json,create_time) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE content_json=VALUES(content_json),create_time=VALUES(create_time)", id, code, json(report), LocalDateTime.now());
        recordStep(id, "DELIVER", 6, "生成巡检报告", "输出航线、遥测、重规划次数和风险摘要", "COMPLETED", 0L);
        recordAudit(id, operator, "GENERATE_REPORT", Map.of(), Map.of("reportCode", code), "SUCCEEDED");
        return report;
    }

    @Override
    public Map<String, Object> getReport(Long id) {
        ensureSchema();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT report_code,content_json,create_time FROM mission_report WHERE mission_id=?", id);
        if (rows.isEmpty()) throw new BusinessException("巡检报告不存在");
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        try {
            result.put("content", objectMapper.readValue(String.valueOf(result.get("content_json")), Map.class));
        } catch (Exception ex) {
            result.put("content", Map.of());
        }
        return result;
    }

    private Map<String, Object> buildMetrics(Long id, Map<String, Object> mission) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        int totalWaypoints = scalarInt("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", id);
        int arrivedWaypoints = scalarInt("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=? AND state='ARRIVED'", id);
        int telemetryCount = scalarInt("SELECT COUNT(*) FROM drone_telemetry WHERE mission_id=?", id);
        int eventCount = scalarInt("SELECT COUNT(*) FROM patrol_event_link WHERE mission_id=?", id);
        int packetLoss = scalarInt("SELECT COUNT(*) FROM patrol_fault_injection WHERE mission_id=? AND fault_type='PACKET_LOSS'", id);
        metrics.put("waypointArrivalRate", totalWaypoints == 0 ? 0D : arrivedWaypoints * 1D / totalWaypoints);
        metrics.put("telemetryRecords", telemetryCount);
        metrics.put("patrolEvents", eventCount);
        metrics.put("replanCount", mission.get("replans"));
        metrics.put("packetLossFaults", packetLoss);
        metrics.put("completionState", mission.get("state"));
        return metrics;
    }

    private int scalarInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private DronePlan buildDronePlan(Long missionId) {
        Map<String, Object> mission = one("SELECT mission_id, plan_version, battery_current, center_lng, center_lat FROM patrol_mission WHERE mission_id=?", missionId);
        List<DroneWaypoint> waypoints = jdbcTemplate.queryForList(
                        "SELECT waypoint_id, sequence_no, lng, lat, altitude_m, dwell_sec FROM patrol_waypoint WHERE mission_id=? ORDER BY sequence_no",
                        missionId)
                .stream()
                .map(row -> new DroneWaypoint(
                        longValue(row.get("waypoint_id")),
                        integer(row, "sequence_no", 0),
                        number(row, "lng", 0),
                        number(row, "lat", 0),
                        number(row, "altitude_m", 30),
                        integer(row, "dwell_sec", 0)))
                .toList();
        return new DronePlan(
                missionId,
                integer(mission, "plan_version", 0),
                waypoints,
                number(mission, "battery_current", 100),
                number(mission, "center_lng", 0),
                number(mission, "center_lat", 0));
    }

    private void handleDroneTelemetry(DroneTelemetry telemetry) {
        if (telemetry == null || telemetry.missionId() == null) return;
        appendTelemetryInternal(
                telemetry.missionId(), telemetry.lng(), telemetry.lat(), telemetry.altitudeM(),
                telemetry.speedMps(), telemetry.battery(), telemetry.gpsQuality(), telemetry.payloadState());
        String state = telemetry.flightState();
        if ("RETURNING".equalsIgnoreCase(state)) {
            jdbcTemplate.update("UPDATE patrol_mission SET state='RETURNING',battery_current=?,update_time=? WHERE mission_id=?",
                    telemetry.battery(), LocalDateTime.now(), telemetry.missionId());
        } else {
            jdbcTemplate.update("UPDATE patrol_mission SET battery_current=?,update_time=? WHERE mission_id=?",
                    telemetry.battery(), LocalDateTime.now(), telemetry.missionId());
        }
        if (telemetry.waypointArrived() && telemetry.waypointId() != null) {
            jdbcTemplate.update("UPDATE patrol_waypoint SET state='ARRIVED' WHERE waypoint_id=? AND mission_id=?",
                    telemetry.waypointId(), telemetry.missionId());
        }
    }

    private void handleDroneEvent(DroneEvent event) {
        if (event == null || event.missionId() == null) return;
        String type = event.type() == null ? "OBSERVE" : event.type().toUpperCase(Locale.ROOT);
        if (event.waypointId() != null && "ARRIVED".equals(type)) {
            jdbcTemplate.update("UPDATE patrol_waypoint SET state='ARRIVED' WHERE waypoint_id=? AND mission_id=?",
                    event.waypointId(), event.missionId());
        }
        if (Set.of("RETURNING", "LOW_BATTERY").contains(type)) {
            jdbcTemplate.update("UPDATE patrol_mission SET state='RETURNING',update_time=? WHERE mission_id=?",
                    LocalDateTime.now(), event.missionId());
        } else if ("COMPLETED".equals(type)) {
            jdbcTemplate.update("UPDATE patrol_mission SET state='COMPLETED',update_time=? WHERE mission_id=?",
                    LocalDateTime.now(), event.missionId());
        } else if ("FAILED".equals(type)) {
            jdbcTemplate.update("UPDATE patrol_mission SET state='FAILED',update_time=? WHERE mission_id=?",
                    LocalDateTime.now(), event.missionId());
        }
        recordStep(event.missionId(), "EXECUTE", Math.max(3, event.sequenceNo()), type, event.detail(),
                "COMPLETED".equals(type) ? "COMPLETED" : type, 0L);
    }

    private void tickSimulation(Long id) {
        try {
            Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
            if (!"RUNNING".equals(String.valueOf(mission.get("state")))) { cancelSimulation(id); return; }
            List<Map<String, Object>> points = jdbcTemplate.queryForList("SELECT * FROM patrol_waypoint WHERE mission_id=? ORDER BY sequence_no", id);
            SimulationState state = simulationStates.computeIfAbsent(id, key -> new SimulationState());
            if (state.index >= points.size()) { finishSimulation(id); return; }
            Map<String, Object> target = points.get(state.index);
            double targetLng = number(target, "lng", 0), targetLat = number(target, "lat", 0);
            if (state.lng == 0 && state.lat == 0) { state.lng = targetLng; state.lat = targetLat; }
            double dLng = targetLng - state.lng, dLat = targetLat - state.lat;
            double distance = Math.sqrt(dLng * dLng + dLat * dLat);
            double step = 0.000035;
            if (distance <= step) { state.lng = targetLng; state.lat = targetLat; state.index++; }
            else { state.lng += dLng / distance * step; state.lat += dLat / distance * step; }
            state.battery = Math.max(0, state.battery - 0.12);
            appendTelemetryInternal(id, state.lng, state.lat, 30, 3, state.battery, 1, "READY");
            jdbcTemplate.update("UPDATE patrol_mission SET battery_current=?,update_time=? WHERE mission_id=?", state.battery, LocalDateTime.now(), id);
            if (state.battery <= 25 || state.index >= points.size()) finishSimulation(id);
        } catch (Exception ex) {
            cancelSimulation(id);
            try { jdbcTemplate.update("UPDATE patrol_mission SET state='FAILED',update_time=? WHERE mission_id=?", LocalDateTime.now(), id); }
            catch (Exception ignored) { log.warn("巡检仿真失败状态写入失败 missionId={}", id); }
        }
    }

    private void finishSimulation(Long id) {
        cancelSimulation(id);
        jdbcTemplate.update("UPDATE patrol_mission SET state='COMPLETED',update_time=? WHERE mission_id=?", LocalDateTime.now(), id);
        recordStep(id, "DELIVER", 6, "仿真巡检完成", "无人机已到达最后航点并完成遥测记录", "COMPLETED", 0L);
    }

    private void cancelSimulation(Long id) { ScheduledFuture<?> task = simulationTasks.remove(id); if (task != null) task.cancel(false); }

    private void appendTelemetryInternal(Long id, double lng, double lat, double altitude, double speed, double battery, double gpsQuality, String payload) {
        jdbcTemplate.update("INSERT INTO drone_telemetry (mission_id,recorded_at,lng,lat,altitude_m,speed_mps,battery,gps_quality,payload_state,raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)", id, LocalDateTime.now(), lng, lat, altitude, speed, battery, gpsQuality, payload, "simulated");
    }

    private List<Map<String, Object>> safeRows(String sql, Object... args) { try { return jdbcTemplate.queryForList(sql, args); } catch (Exception ex) { log.debug("巡检环境数据不可用: {}", ex.getMessage()); return List.of(); } }

    private Map<String, Object> latestTelemetry(Long missionId) {
        List<Map<String, Object>> rows = safeRows("SELECT lng,lat FROM drone_telemetry WHERE mission_id=? ORDER BY recorded_at DESC LIMIT 1", missionId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private double densityNear(List<Map<String, Object>> rows, double lng, double lat, String lngKey, String latKey, double radius, double divisor) {
        long count = rows.stream().filter(row -> Math.abs(number(row, lngKey, 999) - lng) <= radius && Math.abs(number(row, latKey, 999) - lat) <= radius).count();
        return Math.min(1, count / divisor);
    }

    private double restrictedFenceNear(List<Map<String, Object>> rows, double lng, double lat) {
        for (Map<String, Object> row : rows) {
            int type = integer(row, "fence_type", 0);
            double fenceLng = number(row, "center_lng", 999), fenceLat = number(row, "center_lat", 999);
            if (type > 2) continue;
            String coordinates = text(row, "coordinates", "");
            if (pointInPolygon(lng, lat, coordinates)
                    || (Math.abs(fenceLng - lng) <= 0.0003 && Math.abs(fenceLat - lat) <= 0.0003)) return 1;
        }
        return 0;
    }

    private boolean pointInPolygon(double lng, double lat, String coordinatesJson) {
        if (!StringUtils.hasText(coordinatesJson)) return false;
        try {
            List<?> points = objectMapper.readValue(coordinatesJson, List.class);
            if (points.size() < 3) return false;
            boolean inside = false;
            for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
                if (!(points.get(i) instanceof Map<?, ?> current) || !(points.get(j) instanceof Map<?, ?> previous)) continue;
                double xi = number((Map<String, Object>) current, "lng", number((Map<String, Object>) current, "longitude", 999));
                double yi = number((Map<String, Object>) current, "lat", number((Map<String, Object>) current, "latitude", 999));
                double xj = number((Map<String, Object>) previous, "lng", number((Map<String, Object>) previous, "longitude", 999));
                double yj = number((Map<String, Object>) previous, "lat", number((Map<String, Object>) previous, "latitude", 999));
                boolean crosses = ((yi > lat) != (yj > lat))
                        && (lng < (xj - xi) * (lat - yi) / ((yj - yi) == 0 ? 1e-12 : (yj - yi)) + xi);
                if (crosses) inside = !inside;
            }
            return inside;
        } catch (Exception ex) {
            log.debug("巡检围栏坐标解析失败: {}", ex.getMessage());
            return false;
        }
    }

    private List<GridCandidate> greedyRoute(List<GridCandidate> points, double startLng, double startLat) {
        List<GridCandidate> remaining = new ArrayList<>(points);
        List<GridCandidate> result = new ArrayList<>();
        double lng = startLng, lat = startLat;
        while (!remaining.isEmpty()) {
            final double currentLng = lng;
            final double currentLat = lat;
            GridCandidate next = remaining.stream().min(Comparator.comparingDouble(point -> distance(point.lng, point.lat, currentLng, currentLat))).orElseThrow();
            result.add(next); remaining.remove(next); lng = next.lng; lat = next.lat;
        }
        return result;
    }

    /**
     * Apply hard flight constraints after risk ranking. A candidate is only
     * accepted when the segment does not enter a restricted polygon and the
     * complete route (including return-to-home) remains within time and battery
     * reserve. This keeps the planner honest instead of merely drawing points.
     */
    private List<GridCandidate> constrainRoute(List<GridCandidate> ranked,
                                                double startLng, double startLat,
                                                double homeLng, double homeLat,
                                                List<Map<String, Object>> fences,
                                                int durationLimitSec,
                                                double batteryCurrent) {
        List<GridCandidate> selected = new ArrayList<>();
        double previousLng = startLng;
        double previousLat = startLat;
        for (GridCandidate candidate : ranked) {
            if (!segmentAllowed(previousLng, previousLat, candidate.lng, candidate.lat, fences)) continue;
            List<GridCandidate> trial = new ArrayList<>(selected);
            trial.add(candidate);
            double seconds = estimateRouteSeconds(trial, startLng, startLat, homeLng, homeLat);
            double batteryRequired = seconds * 0.24D;
            if (seconds > Math.max(60, durationLimitSec) || batteryRequired > Math.max(0D, batteryCurrent - 25D)) {
                continue;
            }
            selected.add(candidate);
            previousLng = candidate.lng;
            previousLat = candidate.lat;
        }
        return selected;
    }

    private boolean segmentAllowed(double lng1, double lat1, double lng2, double lat2,
                                   List<Map<String, Object>> fences) {
        int samples = Math.max(2, (int) Math.ceil(distance(lng1, lat1, lng2, lat2) / 0.00008D));
        for (int index = 0; index <= samples; index++) {
            double ratio = index / (double) samples;
            double lng = lng1 + (lng2 - lng1) * ratio;
            double lat = lat1 + (lat2 - lat1) * ratio;
            if (restrictedFenceNear(fences, lng, lat) >= 0.95D) return false;
        }
        return true;
    }

    private double estimateRouteSeconds(List<GridCandidate> route,
                                        double startLng, double startLat,
                                        double homeLng, double homeLat) {
        double previousLng = startLng;
        double previousLat = startLat;
        double meters = 0D;
        for (GridCandidate point : route) {
            meters += distanceMeters(previousLng, previousLat, point.lng, point.lat);
            previousLng = point.lng;
            previousLat = point.lat;
        }
        meters += distanceMeters(previousLng, previousLat, homeLng, homeLat);
        return meters / 3D + route.size() * 3D;
    }

    private double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double lngMeters = (lng1 - lng2) * 111_000D * Math.cos(Math.toRadians((lat1 + lat2) / 2D));
        double latMeters = (lat1 - lat2) * 111_000D;
        return Math.sqrt(lngMeters * lngMeters + latMeters * latMeters);
    }

    private double distance(double lng1, double lat1, double lng2, double lat2) { double x = (lng1 - lng2) * 0.9, y = lat1 - lat2; return Math.sqrt(x * x + y * y); }
    private void insertWaypoint(Long id, int sequence, double lng, double lat, double score) { jdbcTemplate.update("INSERT INTO patrol_waypoint (mission_id,sequence_no,lng,lat,altitude_m,dwell_sec,risk_score,state) VALUES (?,?,?,?,?,?,?,'PLANNED')", id, sequence, lng, lat, 30, 3, score); }
    private Map<String, Object> one(String sql, Object... args) { List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args); if (rows.isEmpty()) throw new BusinessException("巡检任务不存在"); return new LinkedHashMap<>(rows.get(0)); }
    private void recordStep(Long id, String phase, int no, String title, String detail, String state, long duration) { jdbcTemplate.update("INSERT INTO agent_step (mission_id,phase,step_no,title,detail,state,duration_ms,create_time) VALUES (?,?,?,?,?,?,?,?)", id, phase, no, title, detail, state, duration, LocalDateTime.now()); }
    private void recordTool(Long id, String name, Object input, Object output, String state, long duration, int retry, String error) { jdbcTemplate.update("INSERT INTO tool_execution (mission_id,tool_name,input_summary,output_summary,state,duration_ms,retry_count,error_message,create_time) VALUES (?,?,?,?,?,?,?,?,?)", id, name, String.valueOf(input), String.valueOf(output), state, duration, retry, error, LocalDateTime.now()); }
    private void recordAudit(Long id, Integer operator, String action, Object input, Object output, String state) { jdbcTemplate.update("INSERT INTO patrol_audit_log (mission_id,operator_user_id,action,input_summary,result_summary,state,create_time) VALUES (?,?,?,?,?,?,?)", id, operator, action, json(input), json(output), state, LocalDateTime.now()); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { return "{}"; } }
    private static Long longValue(Object value) { try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (Exception ex) { return null; } }
    private static double numeric(Object value, double fallback) { try { return value == null ? fallback : Double.parseDouble(String.valueOf(value)); } catch (Exception ex) { return fallback; } }
    private static String text(Map<String, Object> map, String key, String fallback) { Object value = map == null ? null : map.get(key); return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value); }
    private static double number(Map<String, Object> map, String key, double fallback) { Object value = map == null ? null : map.get(key); try { return value == null ? fallback : Double.parseDouble(String.valueOf(value)); } catch (Exception ex) { return fallback; } }
    private static int integer(Map<String, Object> map, String key, int fallback) { return (int) Math.round(number(map, key, fallback)); }

    private static void validateCoordinate(double lng, double lat, String label) {
        if (!Double.isFinite(lng) || !Double.isFinite(lat) || lng < -180D || lng > 180D || lat < -90D || lat > 90D) {
            throw new BusinessException(label + "经纬度无效");
        }
    }

    @PreDestroy
    public void shutdownSimulator() {
        simulationTasks.values().forEach(task -> task.cancel(false));
        simulator.shutdownNow();
        droneAdapter.shutdown();
    }

    private static final class GridCandidate { private final double lng; private final double lat; private final double score; private GridCandidate(double lng, double lat, double score) { this.lng = lng; this.lat = lat; this.score = score; } }
    private static final class SimulationState { private int index; private double lng; private double lat; private double battery = 100; }
}

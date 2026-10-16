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
import com.example.evmonitorplatform.service.coverage.CoveragePlannerClient;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final CoveragePlannerClient coveragePlannerClient;
    private final ScheduledExecutorService simulator = Executors.newScheduledThreadPool(2);

    @Value("${ev-monitor.coverage.enabled:true}")
    private boolean coverageEnabled;
    @Value("${ev-monitor.coverage.cov-width:42}")
    private double coverageWidth;
    @Value("${ev-monitor.coverage.min-radius:10}")
    private double coverageMinRadius;
    @Value("${ev-monitor.coverage.sample-spacing:24}")
    private double coverageSampleSpacing;
    @Value("${ev-monitor.campus.longitude:112.6827629}")
    private double campusLongitude;
    @Value("${ev-monitor.campus.latitude:26.8860959}")
    private double campusLatitude;
    @Value("${ev-monitor.campus.name:衡阳师范学院东校区}")
    private String campusName;
    private final ConcurrentMap<Long, ScheduledFuture<?>> simulationTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SimulationState> simulationStates = new ConcurrentHashMap<>();
    private volatile boolean schemaReady;

    private void ensureSchema() {
        if (schemaReady)
            return;
        synchronized (this) {
            if (schemaReady)
                return;
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
                    "CREATE TABLE IF NOT EXISTS patrol_audit_log (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, mission_id BIGINT NULL, operator_user_id INT NULL, action VARCHAR(48) NOT NULL, input_summary TEXT NULL, result_summary TEXT NULL, state VARCHAR(24) NOT NULL, create_time DATETIME NOT NULL, KEY idx_patrol_audit_mission (mission_id,create_time), KEY idx_patrol_audit_operator (operator_user_id,create_time))");
            ddl.forEach(jdbcTemplate::execute);
            schemaReady = true;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createMission(Map<String, Object> request, Integer operatorUserId) {
        ensureSchema();
        String goal = text(request, "goal", "巡查校区电动车安全风险");
        String area = text(request, "areaName", campusName);
        // 默认中心直接读取 application.yml，管理端传入的中心点仍可用于切换校区。
        double lng = number(request, "centerLng", campusLongitude);
        double lat = number(request, "centerLat", campusLatitude);
        validateCoordinate(lng, lat, "巡检中心点");
        int duration = Math.max(60, Math.min(3600, integer(request, "durationLimitSec", 720)));
        String code = "PM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        jdbcTemplate.update(
                "INSERT INTO patrol_mission (mission_code,goal,area_name,center_lng,center_lat,duration_limit_sec,state,created_by,create_time,update_time) VALUES (?,?,?,?,?,?,'DRAFT',?,?,?)",
                code, goal, area, lng, lat, duration, operatorUserId, LocalDateTime.now(), LocalDateTime.now());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        recordAudit(id, operatorUserId, "CREATE_MISSION", Map.of("area", area, "durationLimitSec", duration),
                Map.of("missionCode", code), "SUCCEEDED");
        PatrolGoalPlanner.PlannerResult goalPlan = patrolGoalPlanner.plan(goal, area, duration);
        jdbcTemplate.update(
                "INSERT INTO patrol_goal_plan (mission_id,provider,model_name,plan_json,raw_response,latency_ms,create_time) VALUES (?,?,?,?,?,?,?)",
                id, goalPlan.provider(), goalPlan.model(), json(goalPlan.plan()), goalPlan.rawResponse(),
                goalPlan.latencyMs(), LocalDateTime.now());
        recordStep(id, "UNDERSTAND", 1, "解析巡检目标", "区域=" + area + ";时限=" + duration + "秒", "COMPLETED", 0L);
        recordTool(id, "PARSE_PATROL_GOAL", Map.of("goal", goal, "area", area),
                Map.of("provider", goalPlan.provider(), "plan", goalPlan.plan()), "SUCCEEDED", goalPlan.latencyMs(), 0,
                null);
        return getMission(id);
    }

    @Override
    public Map<String, Object> getMission(Long id) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        mission.put("waypoints",
                jdbcTemplate.queryForList("SELECT * FROM patrol_waypoint WHERE mission_id=? ORDER BY sequence_no", id));
        mission.put("routeMetrics", routeMetricsFromRows(mission, castRows(mission.get("waypoints")), latestReturnHome(id)));
        mission.put("riskGrid", jdbcTemplate
                .queryForList("SELECT * FROM patrol_risk_grid WHERE mission_id=? ORDER BY risk_score DESC", id));
        mission.put("telemetry", jdbcTemplate.queryForList(
                "SELECT * FROM drone_telemetry WHERE mission_id=? ORDER BY recorded_at DESC LIMIT 100", id));
        mission.put("fences", safeRows(
                "SELECT fence_id,fence_name,fence_type,coordinates,center_lng,center_lat,alert_level FROM geofence WHERE status=1 AND deleted=0"));
        mission.put("events", safeRows(
                "SELECT e.*,l.link_id,l.waypoint_id,l.relation_type,l.trigger_replan FROM patrol_event_link l JOIN ai_event e ON e.event_id=l.event_id WHERE l.mission_id=? ORDER BY e.occurred_at DESC",
                id));
        mission.put("steps", jdbcTemplate
                .queryForList("SELECT * FROM agent_step WHERE mission_id=? ORDER BY step_no, create_time", id));
        mission.put("tools",
                jdbcTemplate.queryForList("SELECT * FROM tool_execution WHERE mission_id=? ORDER BY create_time", id));
        mission.put("audit", safeRows(
                "SELECT audit_id,operator_user_id,action,input_summary,result_summary,state,create_time FROM patrol_audit_log WHERE mission_id=? ORDER BY create_time DESC LIMIT 100",
                id));
        List<Map<String, Object>> goalPlans = safeRows(
                "SELECT provider,model_name,plan_json,raw_response,latency_ms,create_time FROM patrol_goal_plan WHERE mission_id=?",
                id);
        mission.put("goalPlan", goalPlans.isEmpty() ? null : goalPlans.get(0));
        mission.put("planChanges", safeRows(
                "SELECT change_id,from_version,to_version,reason,preserved_count,removed_count,added_count,diff_json,create_time FROM patrol_plan_change WHERE mission_id=? ORDER BY create_time DESC LIMIT 20",
                id));
        mission.put("faults", safeRows(
                "SELECT fault_type,parameters_json,injected_at,recovered_at,state FROM patrol_fault_injection WHERE mission_id=? ORDER BY injected_at DESC LIMIT 20",
                id));
        return mission;
    }

    @Override
    public List<Map<String, Object>> listMissions(String state, Integer limit) {
        return listMissions(state, limit, false);
    }

    @Override
    public List<Map<String, Object>> listMissions(String state, Integer limit, boolean activeOnly) {
        ensureSchema();
        int size = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        String base = "SELECT m.*, (SELECT COUNT(*) FROM patrol_event_link l WHERE l.mission_id=m.mission_id) AS event_count "
                +
                "FROM patrol_mission m ";
        if (activeOnly) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(base
                    + "WHERE m.state IN ('RUNNING','RETURNING','PAUSED') "
                    + "AND m.update_time >= DATE_SUB(NOW(), INTERVAL 30 MINUTE) "
                    + "ORDER BY m.update_time DESC LIMIT " + size);
            return enrichCoverage(rows);
        }
        if (StringUtils.hasText(state)) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(base + "WHERE m.state=? ORDER BY m.create_time DESC LIMIT " + size,
                    state.toUpperCase(Locale.ROOT));
            return enrichCoverage(rows);
        }
        return enrichCoverage(jdbcTemplate.queryForList(base + "ORDER BY m.create_time DESC LIMIT " + size));
    }

    /** 为任务列表补充 Fields2Cover 全覆盖航线摘要（扫掠带/面积/长度），非全覆盖任务为 null。 */
    private List<Map<String, Object>> enrichCoverage(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Long mid = longValue(row.get("mission_id"));
            if (mid == null) continue;
            row.put("coverage", coverageSummary(mid));
        }
        return rows;
    }

    private Map<String, Object> coverageSummary(Long missionId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT output_summary FROM tool_execution " +
                            "WHERE mission_id=? AND tool_name='PLAN_COVERAGE_ROUTE' AND state='SUCCEEDED' " +
                            "ORDER BY create_time DESC LIMIT 1", missionId);
            if (rows.isEmpty()) return null;
            String raw = String.valueOf(rows.get(0).get("output_summary"));
            if (raw == null || raw.isEmpty() || "null".equals(raw)) return null;
            String body = raw.trim();
            if (body.startsWith("{")) body = body.substring(1);
            if (body.endsWith("}")) body = body.substring(0, body.length() - 1);
            Map<String, Object> cov = new LinkedHashMap<>();
            for (String pair : body.split(",")) {
                String kv = pair.trim();
                int eq = kv.indexOf('=');
                if (eq <= 0) continue;
                String key = kv.substring(0, eq).trim();
                String val = kv.substring(eq + 1).trim();
                if ("swathCount".equals(key)) cov.put("swathCount", (int) Double.parseDouble(val));
                else if ("areaM2".equals(key)) cov.put("areaM2", Math.round(Double.parseDouble(val)));
                else if ("pathLengthM".equals(key)) cov.put("pathLengthM", Math.round(Double.parseDouble(val)));
            }
            return cov.isEmpty() ? null : cov;
        } catch (Exception ex) {
            log.debug("解析覆盖航线摘要失败 missionId={}: {}", missionId, ex.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> planMission(Long id, Integer operator) {
        return planMissionInternal(id, operator, false);
    }

    @Override
    @Transactional
    public Map<String, Object> customPlanMission(Long id, Map<String, Object> request, Integer operator) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        String state = String.valueOf(mission.get("state"));
        if (!Set.of("DRAFT", "PLANNED").contains(state)) {
            throw new BusinessException("只有草稿或已规划任务可以编辑航线，当前状态：" + state);
        }
        Object rawWaypoints = request == null ? null : request.get("waypoints");
        if (!(rawWaypoints instanceof List<?> rawList) || rawList.size() < 2) {
            throw new BusinessException("自定义航线至少需要 2 个航点");
        }
        if (rawList.size() > 240) {
            throw new BusinessException("航点数量不能超过 240 个");
        }
        double centerLng = number(mission, "center_lng", campusLongitude);
        double centerLat = number(mission, "center_lat", campusLatitude);
        List<Map<String, Object>> fences = safeRows(
                "SELECT fence_type,center_lng,center_lat,coordinates,status FROM geofence WHERE status=1 AND deleted=0");
        List<CustomWaypoint> points = new ArrayList<>();
        for (int index = 0; index < rawList.size(); index++) {
            Object item = rawList.get(index);
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw new BusinessException("第 " + (index + 1) + " 个航点格式无效");
            }
            double lng = objectNumber(rawMap.get("lng"), Double.NaN);
            double lat = objectNumber(rawMap.get("lat"), Double.NaN);
            double altitude = objectNumber(rawMap.get("altitudeM"), 30D);
            int dwell = (int) Math.round(objectNumber(rawMap.get("dwellSec"), 3D));
            if (!Double.isFinite(lng) || !Double.isFinite(lat)) {
                throw new BusinessException("第 " + (index + 1) + " 个航点经纬度无效");
            }
            if (Math.abs(lng - centerLng) > 0.0048D || Math.abs(lat - centerLat) > 0.0045D) {
                throw new BusinessException("第 " + (index + 1) + " 个航点超出当前校区规划范围");
            }
            if (!Double.isFinite(altitude) || altitude < 10D || altitude > 120D) {
                throw new BusinessException("第 " + (index + 1) + " 个航点高度必须在 10-120 米之间");
            }
            if (dwell < 0 || dwell > 60) {
                throw new BusinessException("第 " + (index + 1) + " 个航点悬停时间必须在 0-60 秒之间");
            }
            if (restrictedFenceNear(fences, lng, lat) >= 0.95D) {
                throw new BusinessException("第 " + (index + 1) + " 个航点位于禁飞区内");
            }
            points.add(new CustomWaypoint(lng, lat, altitude, dwell));
        }
        for (int index = 1; index < points.size(); index++) {
            CustomWaypoint previous = points.get(index - 1);
            CustomWaypoint current = points.get(index);
            if (!segmentAllowed(previous.lng, previous.lat, current.lng, current.lat, fences)) {
                throw new BusinessException("航段 " + index + " 穿过禁飞区，请调整航线");
            }
        }
        boolean returnHome = request == null || !Boolean.FALSE.equals(request.get("returnHome"));
        if (returnHome) {
            CustomWaypoint last = points.get(points.size() - 1);
            if (distanceMeters(last.lng, last.lat, centerLng, centerLat) > 8D) {
                if (!segmentAllowed(last.lng, last.lat, centerLng, centerLat, fences)) {
                    throw new BusinessException("返航航段穿过禁飞区，请关闭自动返航或调整最后一个航点");
                }
                points.add(new CustomWaypoint(centerLng, centerLat, last.altitude, 0));
            }
        }
        Map<String, Object> metrics = routeMetrics(mission, points, returnHome);
        double durationSeconds = numeric(metrics.get("estimatedSeconds"), 0D);
        double batteryRequired = durationSeconds * 0.24D;
        int durationLimit = integer(mission, "duration_limit_sec", 720);
        double batteryCurrent = number(mission, "battery_current", 100D);
        if (durationSeconds > Math.max(60, durationLimit)) {
            throw new BusinessException("航线预计用时 " + Math.round(durationSeconds) + " 秒，超过任务时限 " + durationLimit + " 秒");
        }
        if (batteryRequired > Math.max(0D, batteryCurrent - 25D)) {
            throw new BusinessException("航线预计消耗电量 " + Math.round(batteryRequired) + "%，不足以保留返航电量");
        }
        int oldCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", Integer.class, id);
        int fromVersion = integer(mission, "plan_version", 0);
        jdbcTemplate.update("DELETE FROM patrol_waypoint WHERE mission_id=?", id);
        for (int index = 0; index < points.size(); index++) {
            CustomWaypoint point = points.get(index);
            insertWaypoint(id, index, point.lng, point.lat, point.altitude, point.dwell, 0D);
        }
        jdbcTemplate.update("UPDATE patrol_mission SET state='PLANNED', plan_version=plan_version+1, update_time=? WHERE mission_id=?",
                LocalDateTime.now(), id);
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("mode", "MANUAL");
        diff.put("returnHome", returnHome);
        diff.put("routeMetrics", metrics);
        diff.put("waypointCount", points.size());
        jdbcTemplate.update(
                "INSERT INTO patrol_plan_change (mission_id,from_version,to_version,reason,preserved_count,removed_count,added_count,diff_json,create_time) VALUES (?,?,?,?,?,?,?, ?,?)",
                id, fromVersion, fromVersion + 1, "用户自定义航线", 0, oldCount, points.size(), json(diff), LocalDateTime.now());
        recordStep(id, "PLAN", 2, "保存用户自定义航线",
                "人工航点 " + points.size() + " 个，预计 " + Math.round(durationSeconds) + " 秒，返航=" + returnHome,
                "COMPLETED", 0L);
        recordTool(id, "SAVE_CUSTOM_ROUTE", Map.of("waypointCount", points.size(), "returnHome", returnHome), metrics,
                "SUCCEEDED", 0L, 0, null);
        recordAudit(id, operator, "CUSTOM_PLAN_MISSION", request, metrics, "SUCCEEDED");
        return getMission(id);
    }

    private Map<String, Object> planMissionInternal(Long id, Integer operator, boolean preserveCompleted) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        List<Map<String, Object>> preserved = preserveCompleted
                ? safeRows(
                        "SELECT waypoint_id,sequence_no,lng,lat,altitude_m,dwell_sec,risk_score,state FROM patrol_waypoint WHERE mission_id=? AND state='ARRIVED' ORDER BY sequence_no",
                        id)
                : List.of();
        int oldWaypointCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?",
                Integer.class, id);
        if (preserveCompleted) {
            jdbcTemplate.update("DELETE FROM patrol_waypoint WHERE mission_id=? AND state<>'ARRIVED'", id);
        } else {
            jdbcTemplate.update("DELETE FROM patrol_waypoint WHERE mission_id=?", id);
        }
        recordAudit(id, operator, preserveCompleted ? "REPLAN_MISSION" : "PLAN_MISSION",
                Map.of("preserveCompleted", preserveCompleted), Map.of("oldWaypointCount", oldWaypointCount),
                "STARTED");
        jdbcTemplate.update("DELETE FROM patrol_risk_grid WHERE mission_id=?", id);
        double centerLng = ((Number) mission.get("center_lng")).doubleValue();
        double centerLat = ((Number) mission.get("center_lat")).doubleValue();
        List<Map<String, Object>> events = safeRows(
                "SELECT longitude,latitude FROM ai_event WHERE occurred_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND longitude IS NOT NULL AND latitude IS NOT NULL");
        List<Map<String, Object>> devices = safeRows(
                "SELECT current_lng,current_lat FROM location_device WHERE current_lng IS NOT NULL AND current_lat IS NOT NULL");
        List<Map<String, Object>> fences = safeRows(
                "SELECT fence_type,center_lng,center_lat,coordinates,status FROM geofence WHERE status=1 AND deleted=0");
        List<GridCandidate> candidates = new ArrayList<>();
        for (int row = -2; row <= 2; row++)
            for (int col = -2; col <= 2; col++) {
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
                jdbcTemplate.update(
                        "INSERT INTO patrol_risk_grid (mission_id,row_no,col_no,center_lng,center_lat,recent_score,density_score,fence_score,blind_score,risk_score,selected) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                        id, row, col, lng, lat, recent, density, fence, blind, score, selected);
                if (selected == 1)
                    candidates.add(new GridCandidate(lng, lat, score));
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
        // 全覆盖优先：首次规划时用 Fields2Cover 生成牛耕式全覆盖航线；桥接不可用或
        // 局部重规划（需从已到达航点续飞）时，回退到原有风险网格贪心 + 硬约束航线。
        List<GridCandidate> route = null;
        int swathCount = 0;
        double coverageArea = 0D;
        double coverageLength = 0D;
        String plannerEngine = "risk-grid-greedy";
        if (coverageEnabled && preserved.isEmpty()) {
            try {
                CoveragePlannerClient.CoveragePlan plan = coveragePlannerClient.plan(
                        patrolPolygon(centerLng, centerLat, fences), restrictedHoles(fences),
                        coverageWidth, coverageMinRadius, coverageSampleSpacing);
                route = new ArrayList<>();
                for (CoveragePlannerClient.GeoPoint point : plan.waypoints()) {
                    route.add(new GridCandidate(point.lng(), point.lat(),
                            nearestRisk(candidates, point.lng(), point.lat())));
                }
                swathCount = plan.swathCount();
                coverageArea = plan.areaM2();
                coverageLength = plan.pathLengthM();
                plannerEngine = plan.engine() + " v" + plan.version();
            } catch (Exception ex) {
                log.warn("全覆盖航线规划失败，回退风险网格贪心航线 missionId={}: {}", id, ex.getMessage());
            }
        }
        if (route == null || route.isEmpty()) {
            route = constrainRoute(
                    greedyRoute(candidates.subList(0, Math.min(8, candidates.size())), routeStartLng, routeStartLat),
                    routeStartLng, routeStartLat, centerLng, centerLat, fences,
                    integer(mission, "duration_limit_sec", 720), number(mission, "battery_current", 100));
            plannerEngine = "risk-grid-greedy";
            swathCount = 0;
        }
        if (route.isEmpty()) {
            throw new BusinessException("当前风险网格没有满足禁飞区、时限和返航电量约束的可行航线");
        }
        for (GridCandidate point : route)
            insertWaypoint(id, sequence++, point.lng, point.lat, point.score);
        jdbcTemplate.update(
                "UPDATE patrol_mission SET state='PLANNED', plan_version=plan_version+1, update_time=? WHERE mission_id=?",
                LocalDateTime.now(), id);
        int newWaypointCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?",
                Integer.class, id);
        String planDetail = swathCount > 0
                ? "Fields2Cover 全覆盖规划：牛耕式扫掠带 " + swathCount + " 条，覆盖面积 "
                        + Math.round(coverageArea) + " ㎡，航线全长 " + Math.round(coverageLength) + " 米"
                : "读取近期事件、车辆位置和围栏，生成" + sequence + "个航点";
        recordStep(id, "PLAN", 2, "生成风险网格与初始航线", planDetail, "COMPLETED", 0L);
        if (swathCount > 0) {
            recordTool(id, "PLAN_COVERAGE_ROUTE",
                    Map.of("engine", plannerEngine, "covWidthM", coverageWidth, "minRadiusM", coverageMinRadius,
                            "sampleSpacingM", coverageSampleSpacing, "holes", restrictedHoles(fences).size()),
                    Map.of("waypointCount", sequence, "swathCount", swathCount, "areaM2", coverageArea,
                            "pathLengthM", coverageLength),
                    "SUCCEEDED", 0L, 0, null);
        } else {
            recordTool(id, "PLAN_PATROL_ROUTE",
                    Map.of("gridSizeMeters", 20, "durationLimitSec", mission.get("duration_limit_sec")),
                    Map.of("waypointCount", sequence, "hardConstraints", List.of("禁飞区", "返航电量")), "SUCCEEDED", 0L, 0,
                    null);
        }
        if (preserveCompleted) {
            int fromVersion = integer(mission, "plan_version", 0);
            int toVersion = fromVersion + 1;
            Map<String, Object> diff = new LinkedHashMap<>();
            diff.put("preservedWaypointIds", preserved.stream().map(row -> row.get("waypoint_id")).toList());
            diff.put("preservedSequenceEnd",
                    preserved.isEmpty() ? null : preserved.get(preserved.size() - 1).get("sequence_no"));
            diff.put("routeStart", Map.of("lng", routeStartLng, "lat", routeStartLat));
            jdbcTemplate.update(
                    "INSERT INTO patrol_plan_change (mission_id,from_version,to_version,reason,preserved_count,removed_count,added_count,diff_json,create_time) VALUES (?,?,?,?,?,?,?, ?,?)",
                    id, fromVersion, toVersion, "局部重规划", preserved.size(),
                    Math.max(0, oldWaypointCount - preserved.size()),
                    Math.max(0, newWaypointCount - preserved.size()), json(diff), LocalDateTime.now());
        }
        return getMission(id);
    }

    @Override
    @Transactional
    public Map<String, Object> startMission(Long id, Integer operator) {
        ensureSchema();
        Map<String, Object> mission = one("SELECT * FROM patrol_mission WHERE mission_id=?", id);
        if (!Set.of("PLANNED", "PAUSED").contains(String.valueOf(mission.get("state"))))
            throw new BusinessException("任务必须先完成航线规划");
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", Integer.class,
                id) == 0)
            throw new BusinessException("任务没有可执行航点");
        jdbcTemplate.update("UPDATE patrol_mission SET state='RUNNING',update_time=? WHERE mission_id=?",
                LocalDateTime.now(), id);
        droneAdapter.start(buildDronePlan(id), this::handleDroneTelemetry, this::handleDroneEvent);
        recordStep(id, "EXECUTE", 3, "启动仿真无人机", "开始沿规划航点执行巡检", "RUNNING", 0L);
        recordTool(id, "START_SIMULATION", Map.of("missionId", id),
                Map.of("state", "RUNNING", "telemetryIntervalMs", 500), "SUCCEEDED", 0L, 0, null);
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
        jdbcTemplate.update("UPDATE patrol_mission SET state='PAUSED',update_time=? WHERE mission_id=?",
                LocalDateTime.now(), id);
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
        if (wasRunning)
            droneAdapter.stop(id);
        if (wasRunning)
            cancelSimulation(id);
        planMissionInternal(id, operator, true);
        jdbcTemplate.update("UPDATE patrol_mission SET state=?,replans=replans+1,update_time=? WHERE mission_id=?",
                wasRunning ? "RUNNING" : "PLANNED", LocalDateTime.now(), id);
        if (wasRunning) {
            droneAdapter.start(buildDronePlan(id), this::handleDroneTelemetry, this::handleDroneEvent);
        }
        recordStep(id, "REPLAN", 5, "动态重规划", reason == null ? "检测到新风险" : reason, "COMPLETED", 0L);
        recordTool(id, "REPLAN_ROUTE", Map.of("reason", reason == null ? "new-risk" : reason),
                Map.of("planVersion", jdbcTemplate.queryForObject(
                        "SELECT plan_version FROM patrol_mission WHERE mission_id=?", Integer.class, id)),
                "SUCCEEDED", 0L, 0, null);
        recordAudit(id, operator, "REPLAN_MISSION", Map.of("reason", reason == null ? "new-risk" : reason),
                Map.of("state", wasRunning ? "RUNNING" : "PLANNED"), "SUCCEEDED");
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
        if (!Double.isFinite(battery) || battery < 0 || battery > 100)
            throw new BusinessException("遥测电量必须在0到100之间");
        double gpsQuality = number(telemetry, "gpsQuality", 1);
        if (!Double.isFinite(gpsQuality) || gpsQuality < 0 || gpsQuality > 1)
            throw new BusinessException("GPS质量必须在0到1之间");
        double altitude = number(telemetry, "altitudeM", 30);
        double speed = number(telemetry, "speedMps", 3);
        if (!Double.isFinite(altitude) || altitude < 0 || altitude > 500)
            throw new BusinessException("遥测高度必须在0到500米之间");
        if (!Double.isFinite(speed) || speed < 0 || speed > 100)
            throw new BusinessException("遥测速度必须在0到100米/秒之间");
        appendTelemetryInternal(id, lng, lat, altitude, speed, battery, gpsQuality,
                text(telemetry, "payloadState", "READY"), text(telemetry, "source", "external-device"));
        recordAudit(id, null, "INGEST_TELEMETRY", Map.of("lng", lng, "lat", lat), Map.of("battery", battery),
                "SUCCEEDED");
        return Map.of("accepted", true, "missionId", id, "recordedAt", LocalDateTime.now());
    }

    @Override
    @Transactional
    public Map<String, Object> injectFault(Long id, String faultType, Map<String, Object> parameters,
            Integer operator) {
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
        jdbcTemplate.update(
                "INSERT INTO patrol_fault_injection (mission_id,fault_type,parameters_json,injected_at,state) VALUES (?,?,?,?, 'INJECTED')",
                id, type, json(parameters == null ? Map.of() : parameters), LocalDateTime.now());
        recordStep(id, "MONITOR", 4, "注入仿真故障", type + " " + String.valueOf(parameters), "INJECTED", 0L);
        recordTool(id, "INJECT_DRONE_FAULT",
                Map.of("faultType", type, "parameters", parameters == null ? Map.of() : parameters),
                Map.of("accepted", true), "SUCCEEDED", 0L, 0, null);
        recordAudit(id, operator, "INJECT_FAULT", Map.of("faultType", type), Map.of("accepted", true), "SUCCEEDED");
        return Map.of("accepted", true, "missionId", id, "faultType", type, "parameters",
                parameters == null ? Map.of() : parameters);
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
        jdbcTemplate.update(
                "UPDATE patrol_fault_injection SET recovered_at=?,state='RECOVERED' WHERE mission_id=? AND state='INJECTED'",
                LocalDateTime.now(), id);
        recordStep(id, "MONITOR", 4, "恢复仿真故障", "已清除当前任务故障注入", "RECOVERED", 0L);
        recordTool(id, "CLEAR_DRONE_FAULT", Map.of("missionId", id), Map.of("recovered", true), "SUCCEEDED", 0L, 0,
                null);
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
            event.setRawPayload(Map.of("patrolMissionId", id, "source", "patrol-adapter", "evidenceMode",
                    StringUtils.hasText(evidenceUrl) ? "uploaded" : "telemetry-only"));
            event.setAutoAnalyze(!request.containsKey("autoAnalyze")
                    || Boolean.parseBoolean(String.valueOf(request.get("autoAnalyze"))));
            Map<String, Object> result = aiGovernanceService.ingest(event);
            eventId = longValue(result.get("event_id"));
            if (eventId == null)
                eventId = longValue(result.get("eventId"));
            if (eventId == null)
                throw new BusinessException("AI事件已接入但未返回事件编号");
        }
        List<Map<String, Object>> exists = safeRows("SELECT event_id FROM ai_event WHERE event_id=?", eventId);
        if (exists.isEmpty())
            throw new BusinessException("AI事件不存在：" + eventId);
        int trigger = Boolean.parseBoolean(String.valueOf(request.getOrDefault("triggerReplan", "true"))) ? 1 : 0;
        jdbcTemplate.update(
                "INSERT IGNORE INTO patrol_event_link (mission_id,event_id,relation_type,trigger_replan,create_time) VALUES (?,?,?, ?,?)",
                id, eventId, text(request, "relationType", "DISCOVERED"), trigger, LocalDateTime.now());
        recordStep(id, "OBSERVE", 4, "接入巡检AI事件", "事件 " + eventId + " 已关联到当前巡检任务", "COMPLETED", 0L);
        recordTool(id, "INGEST_PATROL_EVENT", Map.of("missionId", id, "eventId", eventId),
                Map.of("triggerReplan", trigger == 1), "SUCCEEDED", 0L, 0, null);
        if (trigger == 1 && Set.of("RUNNING", "PLANNED").contains(String.valueOf(mission.get("state")))) {
            replanMission(id, "发现巡检AI事件 " + eventId + "，自动插入重点复核航点", operator);
        }
        recordAudit(id, operator, "ATTACH_EVENT", Map.of("eventId", eventId), Map.of("triggerReplan", trigger == 1),
                "SUCCEEDED");
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
        report.put("waypointCount", jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=?", Integer.class, id));
        report.put("telemetryCount", jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM drone_telemetry WHERE mission_id=?", Integer.class, id));
        report.put("eventCount", jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM patrol_event_link WHERE mission_id=?", Integer.class, id));
        report.put("batteryStart", mission.get("battery_start"));
        report.put("batteryCurrent", mission.get("battery_current"));
        report.put("replans", mission.get("replans"));
        report.put("planChanges", safeRows(
                "SELECT from_version,to_version,reason,preserved_count,removed_count,added_count,diff_json,create_time FROM patrol_plan_change WHERE mission_id=? ORDER BY create_time",
                id));
        report.put("faults", safeRows(
                "SELECT fault_type,parameters_json,injected_at,recovered_at,state FROM patrol_fault_injection WHERE mission_id=? ORDER BY injected_at",
                id));
        report.put("steps", safeRows(
                "SELECT phase,step_no,title,detail,state,duration_ms,create_time FROM agent_step WHERE mission_id=? ORDER BY step_no,create_time",
                id));
        report.put("audit", safeRows(
                "SELECT action,state,create_time FROM patrol_audit_log WHERE mission_id=? ORDER BY create_time", id));
        report.put("metrics", buildMetrics(id, mission));
        report.put("generatedAt", LocalDateTime.now());
        String code = "RPT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        report.put("reportCode", code);
        jdbcTemplate.update(
                "INSERT INTO mission_report (mission_id,report_code,content_json,create_time) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE content_json=VALUES(content_json),create_time=VALUES(create_time)",
                id, code, json(report), LocalDateTime.now());
        recordStep(id, "DELIVER", 6, "生成巡检报告", "输出航线、遥测、重规划次数和风险摘要", "COMPLETED", 0L);
        recordAudit(id, operator, "GENERATE_REPORT", Map.of(), Map.of("reportCode", code), "SUCCEEDED");
        return report;
    }

    @Override
    public Map<String, Object> getReport(Long id) {
        ensureSchema();
        List<Map<String, Object>> rows = jdbcTemplate
                .queryForList("SELECT report_code,content_json,create_time FROM mission_report WHERE mission_id=?", id);
        if (rows.isEmpty())
            throw new BusinessException("巡检报告不存在");
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
        int arrivedWaypoints = scalarInt("SELECT COUNT(*) FROM patrol_waypoint WHERE mission_id=? AND state='ARRIVED'",
                id);
        int telemetryCount = scalarInt("SELECT COUNT(*) FROM drone_telemetry WHERE mission_id=?", id);
        int eventCount = scalarInt("SELECT COUNT(*) FROM patrol_event_link WHERE mission_id=?", id);
        int packetLoss = scalarInt(
                "SELECT COUNT(*) FROM patrol_fault_injection WHERE mission_id=? AND fault_type='PACKET_LOSS'", id);
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
        Map<String, Object> mission = one(
                "SELECT mission_id, plan_version, battery_current, center_lng, center_lat FROM patrol_mission WHERE mission_id=?",
                missionId);
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
        if (telemetry == null || telemetry.missionId() == null)
            return;
            appendTelemetryInternal(
                telemetry.missionId(), telemetry.lng(), telemetry.lat(), telemetry.altitudeM(),
                telemetry.speedMps(), telemetry.battery(), telemetry.gpsQuality(), telemetry.payloadState(),
                "pybullet-physics");
        String state = telemetry.flightState();
        if ("RETURNING".equalsIgnoreCase(state)) {
            jdbcTemplate.update(
                    "UPDATE patrol_mission SET state='RETURNING',battery_current=?,update_time=? WHERE mission_id=?",
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
        if (event == null || event.missionId() == null)
            return;
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
            if (!"RUNNING".equals(String.valueOf(mission.get("state")))) {
                cancelSimulation(id);
                return;
            }
            List<Map<String, Object>> points = jdbcTemplate
                    .queryForList("SELECT * FROM patrol_waypoint WHERE mission_id=? ORDER BY sequence_no", id);
            SimulationState state = simulationStates.computeIfAbsent(id, key -> new SimulationState());
            if (state.index >= points.size()) {
                finishSimulation(id);
                return;
            }
            Map<String, Object> target = points.get(state.index);
            double targetLng = number(target, "lng", 0), targetLat = number(target, "lat", 0);
            if (state.lng == 0 && state.lat == 0) {
                state.lng = targetLng;
                state.lat = targetLat;
            }
            double dLng = targetLng - state.lng, dLat = targetLat - state.lat;
            double distance = Math.sqrt(dLng * dLng + dLat * dLat);
            double step = 0.000035;
            if (distance <= step) {
                state.lng = targetLng;
                state.lat = targetLat;
                state.index++;
            } else {
                state.lng += dLng / distance * step;
                state.lat += dLat / distance * step;
            }
            state.battery = Math.max(0, state.battery - 0.12);
            appendTelemetryInternal(id, state.lng, state.lat, 30, 3, state.battery, 1, "READY", "java-simulation");
            jdbcTemplate.update("UPDATE patrol_mission SET battery_current=?,update_time=? WHERE mission_id=?",
                    state.battery, LocalDateTime.now(), id);
            if (state.battery <= 25 || state.index >= points.size())
                finishSimulation(id);
        } catch (Exception ex) {
            cancelSimulation(id);
            try {
                jdbcTemplate.update("UPDATE patrol_mission SET state='FAILED',update_time=? WHERE mission_id=?",
                        LocalDateTime.now(), id);
            } catch (Exception ignored) {
                log.warn("巡检仿真失败状态写入失败 missionId={}", id);
            }
        }
    }

    private void finishSimulation(Long id) {
        cancelSimulation(id);
        jdbcTemplate.update("UPDATE patrol_mission SET state='COMPLETED',update_time=? WHERE mission_id=?",
                LocalDateTime.now(), id);
        recordStep(id, "DELIVER", 6, "仿真巡检完成", "无人机已到达最后航点并完成遥测记录", "COMPLETED", 0L);
    }

    private void cancelSimulation(Long id) {
        ScheduledFuture<?> task = simulationTasks.remove(id);
        if (task != null)
            task.cancel(false);
    }

    private void appendTelemetryInternal(Long id, double lng, double lat, double altitude, double speed, double battery,
            double gpsQuality, String payload) {
        appendTelemetryInternal(id, lng, lat, altitude, speed, battery, gpsQuality, payload, "external-device");
    }

    private void appendTelemetryInternal(Long id, double lng, double lat, double altitude, double speed, double battery,
            double gpsQuality, String payload, String source) {
        jdbcTemplate.update(
                "INSERT INTO drone_telemetry (mission_id,recorded_at,lng,lat,altitude_m,speed_mps,battery,gps_quality,payload_state,raw_payload) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, LocalDateTime.now(), lng, lat, altitude, speed, battery, gpsQuality, payload,
                source == null || source.isBlank() ? "external-device" : source);
    }

    private List<Map<String, Object>> safeRows(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, args);
        } catch (Exception ex) {
            log.debug("巡检环境数据不可用: {}", ex.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> latestTelemetry(Long missionId) {
        List<Map<String, Object>> rows = safeRows(
                "SELECT lng,lat FROM drone_telemetry WHERE mission_id=? ORDER BY recorded_at DESC LIMIT 1", missionId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private double densityNear(List<Map<String, Object>> rows, double lng, double lat, String lngKey, String latKey,
            double radius, double divisor) {
        long count = rows.stream().filter(row -> Math.abs(number(row, lngKey, 999) - lng) <= radius
                && Math.abs(number(row, latKey, 999) - lat) <= radius).count();
        return Math.min(1, count / divisor);
    }

    private double restrictedFenceNear(List<Map<String, Object>> rows, double lng, double lat) {
        for (Map<String, Object> row : rows) {
            int type = integer(row, "fence_type", 0);
            double fenceLng = number(row, "center_lng", 999), fenceLat = number(row, "center_lat", 999);
            if (type > 2)
                continue;
            String coordinates = text(row, "coordinates", "");
            if (pointInPolygon(lng, lat, coordinates)
                    || (Math.abs(fenceLng - lng) <= 0.0003 && Math.abs(fenceLat - lat) <= 0.0003))
                return 1;
        }
        return 0;
    }

    private boolean pointInPolygon(double lng, double lat, String coordinatesJson) {
        if (!StringUtils.hasText(coordinatesJson))
            return false;
        try {
            List<?> points = objectMapper.readValue(coordinatesJson, List.class);
            if (points.size() < 3)
                return false;
            boolean inside = false;
            for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
                if (!(points.get(i) instanceof Map<?, ?> current) || !(points.get(j) instanceof Map<?, ?> previous))
                    continue;
                double xi = number((Map<String, Object>) current, "lng",
                        number((Map<String, Object>) current, "longitude", 999));
                double yi = number((Map<String, Object>) current, "lat",
                        number((Map<String, Object>) current, "latitude", 999));
                double xj = number((Map<String, Object>) previous, "lng",
                        number((Map<String, Object>) previous, "longitude", 999));
                double yj = number((Map<String, Object>) previous, "lat",
                        number((Map<String, Object>) previous, "latitude", 999));
                boolean crosses = ((yi > lat) != (yj > lat))
                        && (lng < (xj - xi) * (lat - yi) / ((yj - yi) == 0 ? 1e-12 : (yj - yi)) + xi);
                if (crosses)
                    inside = !inside;
            }
            return inside;
        } catch (Exception ex) {
            log.debug("巡检围栏坐标解析失败: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 巡检作业区：以 OSM 校园边界和当前启用电子围栏校准后的核心道路区，约 490m × 420m。
     * 这是飞行作业区，不是屏幕百分比；Fields2Cover 在该经纬度地块内生成牛耕式航线，
     * 再将禁飞围栏作为洞剔除，航线可以和地图上的校园建筑/围栏位置对应。
     */
    private List<CoveragePlannerClient.GeoPoint> patrolPolygon(double centerLng, double centerLat,
            List<Map<String, Object>> fences) {
        // 百度坐标在衡阳约 1e-5 度对应 1m；作业区覆盖校园中部道路与建筑群，
        // 保留足够的安全边界，避免把校外道路或整个校区边缘误当作低空作业区。
        double halfLng = 0.00220D;
        double halfLat = 0.00190D;
        double minFenceLng = Double.POSITIVE_INFINITY;
        double maxFenceLng = Double.NEGATIVE_INFINITY;
        double minFenceLat = Double.POSITIVE_INFINITY;
        double maxFenceLat = Double.NEGATIVE_INFINITY;
        for (Map<String, Object> fence : fences == null ? List.<Map<String, Object>>of() : fences) {
            double fenceCenterLng = number(fence, "center_lng", centerLng);
            double fenceCenterLat = number(fence, "center_lat", centerLat);
            if (Math.abs(fenceCenterLng - centerLng) > 0.01D || Math.abs(fenceCenterLat - centerLat) > 0.01D)
                continue;
            for (CoveragePlannerClient.GeoPoint point : parseFenceRing(text(fence, "coordinates", ""))) {
                minFenceLng = Math.min(minFenceLng, point.lng());
                maxFenceLng = Math.max(maxFenceLng, point.lng());
                minFenceLat = Math.min(minFenceLat, point.lat());
                maxFenceLat = Math.max(maxFenceLat, point.lat());
            }
        }
        if (Double.isFinite(minFenceLng) && Double.isFinite(maxFenceLng)) {
            // 电子围栏给出作业区的真实空间锚点；限制最大范围，避免把周边城市道路纳入航线。
            centerLng = Math.max(centerLng - 0.001D, Math.min(centerLng + 0.001D,
                    (minFenceLng + maxFenceLng) / 2D));
            centerLat = Math.max(centerLat - 0.001D, Math.min(centerLat + 0.001D,
                    (minFenceLat + maxFenceLat) / 2D));
            halfLng = Math.max(0.00180D, Math.min(0.00220D, (maxFenceLng - minFenceLng) / 2D + 0.00060D));
            halfLat = Math.max(0.00160D, Math.min(0.00190D, (maxFenceLat - minFenceLat) / 2D + 0.00055D));
        }
        return List.of(
                new CoveragePlannerClient.GeoPoint(centerLng - halfLng, centerLat - halfLat),
                new CoveragePlannerClient.GeoPoint(centerLng + halfLng, centerLat - halfLat),
                new CoveragePlannerClient.GeoPoint(centerLng + halfLng, centerLat + halfLat),
                new CoveragePlannerClient.GeoPoint(centerLng - halfLng, centerLat + halfLat));
    }

    /** 禁飞区镂空：受限围栏多边形作为覆盖地块的洞，避免全覆盖航线穿越禁飞区。 */
    private List<List<CoveragePlannerClient.GeoPoint>> restrictedHoles(List<Map<String, Object>> fences) {
        List<List<CoveragePlannerClient.GeoPoint>> holes = new ArrayList<>();
        for (Map<String, Object> row : fences) {
            if (integer(row, "fence_type", 0) > 2)
                continue;
            List<CoveragePlannerClient.GeoPoint> ring = parseFenceRing(text(row, "coordinates", ""));
            if (ring.size() >= 3)
                holes.add(ring);
        }
        return holes;
    }

    private List<CoveragePlannerClient.GeoPoint> parseFenceRing(String coordinatesJson) {
        if (!StringUtils.hasText(coordinatesJson))
            return List.of();
        try {
            List<?> points = objectMapper.readValue(coordinatesJson, List.class);
            List<CoveragePlannerClient.GeoPoint> ring = new ArrayList<>();
            for (Object item : points) {
                if (!(item instanceof Map<?, ?> map))
                    continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> point = (Map<String, Object>) map;
                double lng = number(point, "lng", number(point, "longitude", 999D));
                double lat = number(point, "lat", number(point, "latitude", 999D));
                if (lng < 900D || lat < 900D)
                    ring.add(new CoveragePlannerClient.GeoPoint(lng, lat));
            }
            return ring;
        } catch (Exception ex) {
            log.debug("巡检围栏坐标解析失败: {}", ex.getMessage());
            return List.of();
        }
    }

    /** 覆盖航点的风险标注：取最近风险网格候选的评分。 */
    private double nearestRisk(List<GridCandidate> cells, double lng, double lat) {
        double best = 0D;
        double bestDistance = Double.MAX_VALUE;
        for (GridCandidate cell : cells) {
            double candidateDistance = distance(cell.lng, cell.lat, lng, lat);
            if (candidateDistance < bestDistance) {
                bestDistance = candidateDistance;
                best = cell.score;
            }
        }
        return best;
    }

    private List<GridCandidate> greedyRoute(List<GridCandidate> points, double startLng, double startLat) {
        List<GridCandidate> remaining = new ArrayList<>(points);
        List<GridCandidate> result = new ArrayList<>();
        double lng = startLng, lat = startLat;
        while (!remaining.isEmpty()) {
            final double currentLng = lng;
            final double currentLat = lat;
            GridCandidate next = remaining.stream()
                    .min(Comparator.comparingDouble(point -> distance(point.lng, point.lat, currentLng, currentLat)))
                    .orElseThrow();
            result.add(next);
            remaining.remove(next);
            lng = next.lng;
            lat = next.lat;
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
            if (!segmentAllowed(previousLng, previousLat, candidate.lng, candidate.lat, fences))
                continue;
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
            if (restrictedFenceNear(fences, lng, lat) >= 0.95D)
                return false;
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

    private double distance(double lng1, double lat1, double lng2, double lat2) {
        double x = (lng1 - lng2) * 0.9, y = lat1 - lat2;
        return Math.sqrt(x * x + y * y);
    }

    private void insertWaypoint(Long id, int sequence, double lng, double lat, double score) {
        insertWaypoint(id, sequence, lng, lat, 30D, 3, score);
    }

    private void insertWaypoint(Long id, int sequence, double lng, double lat, double altitude, int dwell, double score) {
        jdbcTemplate.update(
                "INSERT INTO patrol_waypoint (mission_id,sequence_no,lng,lat,altitude_m,dwell_sec,risk_score,state) VALUES (?,?,?,?,?,?,?,'PLANNED')",
                id, sequence, lng, lat, altitude, dwell, score);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castRows(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) rows.add((Map<String, Object>) map);
        }
        return rows;
    }

    private Map<String, Object> routeMetricsFromRows(Map<String, Object> mission, List<Map<String, Object>> rows, boolean includeReturn) {
        List<CustomWaypoint> points = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            points.add(new CustomWaypoint(
                    number(row, "lng", 0D), number(row, "lat", 0D),
                    number(row, "altitude_m", number(row, "altitudeM", 30D)),
                    integer(row, "dwell_sec", integer(row, "dwellSec", 3))));
        }
        return routeMetrics(mission, points, includeReturn);
    }

    private Map<String, Object> routeMetrics(Map<String, Object> mission, List<CustomWaypoint> points) {
        return routeMetrics(mission, points, true);
    }

    private Map<String, Object> routeMetrics(Map<String, Object> mission, List<CustomWaypoint> points, boolean includeReturn) {
        double homeLng = number(mission, "center_lng", campusLongitude);
        double homeLat = number(mission, "center_lat", campusLatitude);
        double lng = homeLng;
        double lat = homeLat;
        double distanceM = 0D;
        int dwellSeconds = 0;
        double minAltitude = Double.POSITIVE_INFINITY;
        double maxAltitude = Double.NEGATIVE_INFINITY;
        for (CustomWaypoint point : points) {
            distanceM += distanceMeters(lng, lat, point.lng, point.lat);
            lng = point.lng;
            lat = point.lat;
            dwellSeconds += point.dwell;
            minAltitude = Math.min(minAltitude, point.altitude);
            maxAltitude = Math.max(maxAltitude, point.altitude);
        }
        double returnDistanceM = includeReturn ? distanceMeters(lng, lat, homeLng, homeLat) : 0D;
        distanceM += returnDistanceM;
        if (points.isEmpty()) {
            minAltitude = 0D;
            maxAltitude = 0D;
        }
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("waypointCount", points.size());
        metrics.put("distanceM", Math.round(distanceM * 10D) / 10D);
        metrics.put("returnDistanceM", Math.round(returnDistanceM * 10D) / 10D);
        metrics.put("estimatedSeconds", Math.round(distanceM / 4.5D + dwellSeconds));
        metrics.put("dwellSeconds", dwellSeconds);
        metrics.put("minAltitudeM", Math.round(minAltitude * 10D) / 10D);
        metrics.put("maxAltitudeM", Math.round(maxAltitude * 10D) / 10D);
        metrics.put("cruiseSpeedMps", 4.5D);
        metrics.put("returnHome", includeReturn);
        return metrics;
    }

    private boolean latestReturnHome(Long missionId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT diff_json FROM patrol_plan_change WHERE mission_id=? ORDER BY create_time DESC LIMIT 1", missionId);
            if (rows.isEmpty()) return true;
            Object raw = rows.get(0).get("diff_json");
            if (raw == null) return true;
            @SuppressWarnings("unchecked")
            Map<String, Object> diff = objectMapper.readValue(String.valueOf(raw), Map.class);
            return !diff.containsKey("returnHome") || Boolean.TRUE.equals(diff.get("returnHome"));
        } catch (Exception ex) {
            return true;
        }
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        if (rows.isEmpty())
            throw new BusinessException("巡检任务不存在");
        return new LinkedHashMap<>(rows.get(0));
    }

    private void recordStep(Long id, String phase, int no, String title, String detail, String state, long duration) {
        jdbcTemplate.update(
                "INSERT INTO agent_step (mission_id,phase,step_no,title,detail,state,duration_ms,create_time) VALUES (?,?,?,?,?,?,?,?)",
                id, phase, no, title, detail, state, duration, LocalDateTime.now());
    }

    private void recordTool(Long id, String name, Object input, Object output, String state, long duration, int retry,
            String error) {
        jdbcTemplate.update(
                "INSERT INTO tool_execution (mission_id,tool_name,input_summary,output_summary,state,duration_ms,retry_count,error_message,create_time) VALUES (?,?,?,?,?,?,?,?,?)",
                id, name, String.valueOf(input), String.valueOf(output), state, duration, retry, error,
                LocalDateTime.now());
    }

    private void recordAudit(Long id, Integer operator, String action, Object input, Object output, String state) {
        jdbcTemplate.update(
                "INSERT INTO patrol_audit_log (mission_id,operator_user_id,action,input_summary,result_summary,state,create_time) VALUES (?,?,?,?,?,?,?)",
                id, operator, action, json(input), json(output), state, LocalDateTime.now());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static Long longValue(Object value) {
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private static double numeric(Object value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String text(Map<String, Object> map, String key, String fallback) {
        Object value = map == null ? null : map.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private static double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static int integer(Map<String, Object> map, String key, int fallback) {
        return (int) Math.round(number(map, key, fallback));
    }

    private static double objectNumber(Object value, double fallback) {
        return numeric(value, fallback);
    }

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

    private static final class GridCandidate {
        private final double lng;
        private final double lat;
        private final double score;

        private GridCandidate(double lng, double lat, double score) {
            this.lng = lng;
            this.lat = lat;
            this.score = score;
        }
    }

    private static final class CustomWaypoint {
        private final double lng;
        private final double lat;
        private final double altitude;
        private final int dwell;

        private CustomWaypoint(double lng, double lat, double altitude, int dwell) {
            this.lng = lng;
            this.lat = lat;
            this.altitude = altitude;
            this.dwell = dwell;
        }
    }

    private static final class SimulationState {
        private int index;
        private double lng;
        private double lat;
        private double battery = 100;
    }
}

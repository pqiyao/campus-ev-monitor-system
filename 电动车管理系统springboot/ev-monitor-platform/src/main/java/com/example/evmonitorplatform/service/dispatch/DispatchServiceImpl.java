package com.example.evmonitorplatform.service.dispatch;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.service.AiGovernanceService;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 空地协同调度编排实现：DispatchScore 择优派单 → SimulatedGroundRobotAdapter 执行
 * → 到场复核取证生成 GROUND_ROBOT 来源 AI 事件（进入跨源融合与人工确认闭环）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {
    private static final Set<String> ACTIVE_ORDER_STATES = Set.of("CREATED", "EXECUTING", "ARRIVED", "BLOCKED");
    private static final String DEFAULT_SPEED_MPS = "3.0";

    private final JdbcTemplate jdbcTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final DispatchEngine dispatchEngine;
    private final GroundRobotAdapter groundRobotAdapter;
    private final AiGovernanceService aiGovernanceService;
    private volatile boolean schemaReady;

    private void ensureSchema() {
        if (schemaReady) return;
        synchronized (this) {
            if (schemaReady) return;
            List<String> ddl = List.of(
                    "CREATE TABLE IF NOT EXISTS ground_robot (robot_id BIGINT AUTO_INCREMENT PRIMARY KEY, robot_code VARCHAR(48) NOT NULL UNIQUE, name VARCHAR(96) NOT NULL, capabilities VARCHAR(96) NOT NULL DEFAULT 'CAMERA', current_lng DECIMAL(11,7) NOT NULL, current_lat DECIMAL(10,7) NOT NULL, battery DECIMAL(5,2) NOT NULL DEFAULT 100, state VARCHAR(24) NOT NULL DEFAULT 'IDLE', home_lng DECIMAL(11,7) NULL, home_lat DECIMAL(10,7) NULL, speed_mps DECIMAL(5,2) NOT NULL DEFAULT 3.00, update_time DATETIME NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS dispatch_order (order_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_code VARCHAR(48) NOT NULL UNIQUE, event_id BIGINT NOT NULL, robot_id BIGINT NOT NULL, robot_code VARCHAR(48) NOT NULL, plan_version INT NOT NULL DEFAULT 0, score_total DECIMAL(8,4) NOT NULL, urgency_score DECIMAL(8,4) NOT NULL DEFAULT 0, proximity_score DECIMAL(8,4) NOT NULL DEFAULT 0, capability_score DECIMAL(8,4) NOT NULL DEFAULT 0, battery_score DECIMAL(8,4) NOT NULL DEFAULT 0, availability_score DECIMAL(8,4) NOT NULL DEFAULT 0, distance_meters DECIMAL(10,1) NOT NULL DEFAULT 0, score_detail LONGTEXT NULL, state VARCHAR(24) NOT NULL DEFAULT 'CREATED', last_event VARCHAR(32) NULL, remark VARCHAR(500) NULL, dispatched_at DATETIME NOT NULL, arrived_at DATETIME NULL, completed_at DATETIME NULL, replans INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL, update_time DATETIME NOT NULL, KEY idx_dispatch_order_event (event_id), KEY idx_dispatch_order_state (state, create_time))",
                    "CREATE TABLE IF NOT EXISTS ground_telemetry (telemetry_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, recorded_at DATETIME NOT NULL, lng DECIMAL(11,7) NOT NULL, lat DECIMAL(10,7) NOT NULL, speed_mps DECIMAL(5,2) NULL, battery DECIMAL(5,2) NULL, gps_quality DECIMAL(5,2) NULL, payload_state VARCHAR(32) NULL, motion_state VARCHAR(32) NULL, KEY idx_ground_telemetry_order (order_id, recorded_at))"
            );
            ddl.forEach(jdbcTemplate::execute);
            schemaReady = true;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> registerRobot(Map<String, Object> request) {
        ensureSchema();
        String code = text(request, "robotCode", null);
        if (!StringUtils.hasText(code)) throw new BusinessException("机器人编号不能为空");
        double lng = number(request, "currentLng", 110.3239130);
        double lat = number(request, "currentLat", 25.2875179);
        int updated = jdbcTemplate.update(
                "UPDATE ground_robot SET name=COALESCE(?,name), capabilities=COALESCE(?,capabilities), current_lng=?, current_lat=?, battery=?, state=COALESCE(?,state), home_lng=COALESCE(?,home_lng), home_lat=COALESCE(?,home_lat), speed_mps=COALESCE(?,speed_mps), update_time=? WHERE robot_code=?",
                text(request, "name", null), text(request, "capabilities", "CAMERA"), lng, lat,
                number(request, "battery", 100D), text(request, "state", null),
                number(request, "homeLng", lng), number(request, "homeLat", lat),
                number(request, "speedMps", Double.parseDouble(DEFAULT_SPEED_MPS)),
                LocalDateTime.now(), code);
        if (updated <= 0) {
            jdbcTemplate.update(
                    "INSERT INTO ground_robot (robot_code, name, capabilities, current_lng, current_lat, battery, state, home_lng, home_lat, speed_mps, update_time) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    code, text(request, "name", "地面巡检车-" + code), text(request, "capabilities", "CAMERA"),
                    lng, lat, number(request, "battery", 100D), text(request, "state", "IDLE"),
                    number(request, "homeLng", lng), number(request, "homeLat", lat),
                    number(request, "speedMps", Double.parseDouble(DEFAULT_SPEED_MPS)), LocalDateTime.now());
        }
        return jdbcTemplate.queryForMap("SELECT * FROM ground_robot WHERE robot_code=?", code);
    }

    @Override
    public List<Map<String, Object>> listRobots() {
        ensureSchema();
        return jdbcTemplate.queryForList("SELECT * FROM ground_robot ORDER BY robot_id");
    }

    @Override
    public List<Map<String, Object>> previewScores(Long eventId) {
        ensureSchema();
        Map<String, Object> event = requireEvent(eventId);
        List<Map<String, Object>> robots = jdbcTemplate.queryForList(
                "SELECT * FROM ground_robot WHERE state<>'OFFLINE'");
        List<Map<String, Object>> scores = new ArrayList<>();
        for (Map<String, Object> robot : robots) {
            DispatchEngine.DispatchScore score = scoreFor(event, robot);
            Map<String, Object> item = score.toMap();
            item.put("dispatchable", dispatchEngine.dispatchable(score));
            scores.add(item);
        }
        scores.sort((a, b) -> Double.compare((Double) b.get("total"), (Double) a.get("total")));
        return scores;
    }

    @Override
    @Transactional
    public Map<String, Object> dispatchForEvent(Long eventId) {
        ensureSchema();
        Map<String, Object> event = requireEvent(eventId);
        Map<String, Object> active = findActiveOrder(eventId);
        if (active != null) {
            Map<String, Object> result = new LinkedHashMap<>(active);
            result.put("dispatched", false);
            result.put("reason", "该事件已有进行中的派单");
            return result;
        }
        List<Map<String, Object>> robots = jdbcTemplate.queryForList(
                "SELECT * FROM ground_robot WHERE state='IDLE' AND battery>20");
        if (robots.isEmpty()) {
            throw new BusinessException("暂无空闲且电量充足的地面巡检车");
        }
        List<DispatchEngine.DispatchScore> scores = new ArrayList<>();
        for (Map<String, Object> robot : robots) scores.add(scoreFor(event, robot));
        scores.sort((a, b) -> Double.compare(b.total(), a.total()));
        DispatchEngine.DispatchScore best = scores.get(0);
        if (!dispatchEngine.dispatchable(best)) {
            throw new BusinessException("最优候选未达派单阈值：" + best.explanation());
        }
        return createOrder(event, best, scores);
    }

    @Override
    @Transactional
    public Map<String, Object> dispatchPendingEvents(Integer limit) {
        ensureSchema();
        int size = limit == null ? 5 : Math.max(1, Math.min(limit, 20));
        List<Map<String, Object>> pending = jdbcTemplate.queryForList(
                "SELECT event_id, event_type, risk_level, status FROM ai_event WHERE status IN ('INGESTED','ANALYZING') AND risk_level IN ('HIGH','MEDIUM') ORDER BY FIELD(risk_level,'HIGH','MEDIUM'), occurred_at LIMIT ?",
                size);
        List<Map<String, Object>> dispatched = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Map<String, Object> event : pending) {
            Long eventId = ((Number) event.get("event_id")).longValue();
            try {
                Map<String, Object> result = dispatchForEvent(eventId);
                if (Boolean.TRUE.equals(result.get("dispatched")) || result.containsKey("order_id")) {
                    dispatched.add(result);
                } else {
                    skipped.add(eventId + ":" + result.get("reason"));
                }
            } catch (BusinessException ex) {
                skipped.add(eventId + ":" + ex.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidates", pending.size());
        result.put("dispatchedCount", dispatched.size());
        result.put("orders", dispatched);
        result.put("skipped", skipped);
        return result;
    }

    /** 评分择优 + 建单 + 启动执行器。 */
    private Map<String, Object> createOrder(Map<String, Object> event, DispatchEngine.DispatchScore best,
                                            List<DispatchEngine.DispatchScore> allScores) {
        Map<String, Object> robot = jdbcTemplate.queryForMap(
                "SELECT * FROM ground_robot WHERE robot_code=?", best.robotCode());
        jdbcTemplate.update(
                "INSERT INTO dispatch_order (order_code, event_id, robot_id, robot_code, score_total, urgency_score, proximity_score, capability_score, battery_score, availability_score, distance_meters, score_detail, state, dispatched_at, create_time, update_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "DP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT),
                ((Number) event.get("event_id")).longValue(),
                ((Number) robot.get("robot_id")).longValue(),
                best.robotCode(),
                best.total(), best.urgency(), best.proximity(), best.capability(), best.battery(),
                best.availability(), best.distanceMeters(),
                json(Map.of("allScores", allScores.stream().map(DispatchEngine.DispatchScore::toMap).toList(),
                        "winner", best.toMap())),
                "EXECUTING", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        Long orderId = jdbcTemplate.queryForObject(
                "SELECT order_id FROM dispatch_order WHERE event_id=? AND state='EXECUTING' ORDER BY order_id DESC LIMIT 1",
                Long.class, ((Number) event.get("event_id")).longValue());
        jdbcTemplate.update("UPDATE ground_robot SET state='BUSY', update_time=? WHERE robot_id=?",
                LocalDateTime.now(), robot.get("robot_id"));

        double homeLng = asDouble(robot.get("home_lng"), asDouble(robot.get("current_lng"), 0D));
        double homeLat = asDouble(robot.get("home_lat"), asDouble(robot.get("current_lat"), 0D));
        double eventLng = asDouble(event.get("longitude"), homeLng);
        double eventLat = asDouble(event.get("latitude"), homeLat);
        GroundMissionPlan plan = new GroundMissionPlan(orderId, 0,
                List.of(new GroundWaypoint(null, 1, eventLng, eventLat, 5, "REVIEW")),
                asDouble(robot.get("battery"), 100D), homeLng, homeLat,
                asDouble(robot.get("speed_mps"), 3D));
        groundRobotAdapter.start(plan, telemetrySink(orderId, best.robotCode()), eventSink(orderId, event));
        log.info("空地协同派单完成 orderId={}, eventId={}, robot={}, score={}",
                orderId, event.get("event_id"), best.robotCode(), best.total());
        return orderResult(orderId, best, allScores);
    }

    private Consumer<GroundRobotTelemetry> telemetrySink(Long orderId, String robotCode) {
        return telemetry -> {
            try {
                jdbcTemplate.update("INSERT INTO ground_telemetry (order_id, recorded_at, lng, lat, speed_mps, battery, gps_quality, payload_state, motion_state) VALUES (?,?,?,?,?,?,?,?,?)",
                        orderId, LocalDateTime.now(), telemetry.lng(), telemetry.lat(),
                        telemetry.speedMps(), telemetry.battery(), telemetry.gpsQuality(),
                        telemetry.payloadState(), telemetry.motionState());
                jdbcTemplate.update("UPDATE ground_robot SET current_lng=?, current_lat=?, battery=?, update_time=? WHERE robot_code=?",
                        telemetry.lng(), telemetry.lat(), telemetry.battery(), LocalDateTime.now(), robotCode);
            } catch (Exception ex) {
                log.warn("地面车遥测落库失败 orderId={}: {}", orderId, ex.getMessage());
            }
        };
    }

    private Consumer<GroundRobotEvent> eventSink(Long orderId, Map<String, Object> event) {
        return robotEvent -> {
            try {
                String type = robotEvent.type() == null ? "" : robotEvent.type();
                jdbcTemplate.update("UPDATE dispatch_order SET last_event=?, update_time=? WHERE order_id=?",
                        type, LocalDateTime.now(), orderId);
                switch (type) {
                    case "ARRIVED" -> jdbcTemplate.update(
                            "UPDATE dispatch_order SET state='ARRIVED', arrived_at=? WHERE order_id=?",
                            LocalDateTime.now(), orderId);
                    case "CAPTURED" -> submitReviewEvent(orderId, event, robotEvent);
                    case "BLOCKED" -> {
                        jdbcTemplate.update("UPDATE dispatch_order SET state='BLOCKED', remark=? WHERE order_id=?",
                                truncate(robotEvent.detail(), 480), orderId);
                        jdbcTemplate.update("UPDATE ground_robot SET state='IDLE', update_time=? WHERE robot_code=(SELECT robot_code FROM dispatch_order WHERE order_id=?)",
                                LocalDateTime.now(), orderId);
                    }
                    case "COMPLETED" -> jdbcTemplate.update(
                            "UPDATE dispatch_order SET state='COMPLETED', completed_at=? WHERE order_id=?",
                            LocalDateTime.now(), orderId);
                    case "FAILED" -> jdbcTemplate.update(
                            "UPDATE dispatch_order SET state='FAILED', remark=?, completed_at=? WHERE order_id=?",
                            truncate(robotEvent.detail(), 480), LocalDateTime.now(), orderId);
                    default -> { /* RETURNING/LOW_BATTERY/PACKET_LOSS 记录在 last_event */ }
                }
                if ("COMPLETED".equals(type) || "FAILED".equals(type)) {
                    jdbcTemplate.update("UPDATE ground_robot SET state='IDLE', update_time=? WHERE robot_code=(SELECT robot_code FROM dispatch_order WHERE order_id=?)",
                            LocalDateTime.now(), orderId);
                }
            } catch (Exception ex) {
                log.warn("地面车事件处理失败 orderId={}, type={}: {}", orderId, robotEvent.type(), ex.getMessage());
            }
        };
    }

    /** 到场取证：生成 GROUND_ROBOT 来源复核事件，跨源融合自动关联原事件。 */
    private void submitReviewEvent(Long orderId, Map<String, Object> originEvent, GroundRobotEvent trigger) {
        String originEventId = String.valueOf(originEvent.get("event_id"));
        AiEventCreateRequest request = new AiEventCreateRequest();
        request.setEventType(text(originEvent, "event_type", "ILLEGAL_PARKING"));
        request.setSourceType("GROUND_ROBOT");
        request.setDeviceId(text(originEvent, "device_id", "ground-robot"));
        request.setOccurredAt(LocalDateTime.now());
        request.setLongitude(asDouble(originEvent.get("longitude"), null));
        request.setLatitude(asDouble(originEvent.get("latitude"), null));
        request.setLocationName(text(originEvent, "location_name", "地面巡检车复核现场"));
        request.setDetectionConfidence(0.9D);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", "GROUND_ROBOT_REVIEW");
        payload.put("originEventId", originEventId);
        payload.put("dispatchOrderId", orderId);
        payload.put("note", "地面巡检车已到场复核取证");
        request.setRawPayload(payload);
        request.setEvidenceUrls(List.of());
        request.setIdempotencyKey("ground-review-" + orderId);
        request.setAutoAnalyze(false);
        Map<String, Object> result = aiGovernanceService.ingest(request);
        log.info("地面车复核事件已接入 orderId={}, originEventId={}, reviewEventId={}",
                orderId, originEventId, result.get("eventId"));
    }

    @Override
    @Transactional
    public Map<String, Object> injectFault(Long orderId, String faultType, Map<String, Object> parameters) {
        ensureSchema();
        Map<String, Object> order = requireOrder(orderId);
        if (!ACTIVE_ORDER_STATES.contains(String.valueOf(order.get("state")))) {
            throw new BusinessException("派单已完成或已取消，不能注入故障");
        }
        groundRobotAdapter.injectFault(orderId, faultType, parameters);
        jdbcTemplate.update("UPDATE dispatch_order SET remark=?, update_time=? WHERE order_id=?",
                "故障注入:" + faultType, LocalDateTime.now(), orderId);
        return Map.of("orderId", orderId, "faultType", faultType, "injected", true);
    }

    @Override
    @Transactional
    public Map<String, Object> clearFault(Long orderId) {
        ensureSchema();
        Map<String, Object> order = requireOrder(orderId);
        groundRobotAdapter.clearFault(orderId);
        jdbcTemplate.update("UPDATE dispatch_order SET state='EXECUTING', remark='故障已恢复，继续执行', update_time=? WHERE order_id=? AND state='BLOCKED'",
                LocalDateTime.now(), orderId);
        return Map.of("orderId", orderId, "cleared", true, "state", order.get("state"));
    }

    @Override
    public List<Map<String, Object>> listOrders(String state, Integer limit) {
        ensureSchema();
        int size = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
        StringBuilder sql = new StringBuilder("SELECT o.*, e.event_type, e.risk_level AS event_risk_level, e.location_name FROM dispatch_order o LEFT JOIN ai_event e ON e.event_id=o.event_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(state)) {
            sql.append(" AND o.state=?");
            args.add(state.trim().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY o.create_time DESC LIMIT ").append(size);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Override
    public Map<String, Object> getOrderDetail(Long orderId) {
        ensureSchema();
        Map<String, Object> order = requireOrder(orderId);
        Map<String, Object> result = new LinkedHashMap<>(order);
        result.put("telemetry", jdbcTemplate.queryForList(
                "SELECT * FROM ground_telemetry WHERE order_id=? ORDER BY recorded_at LIMIT 500", orderId));
        result.put("reviewEvents", jdbcTemplate.queryForList(
                "SELECT event_id, event_code, event_type, status, risk_level FROM ai_event WHERE JSON_UNQUOTE(JSON_EXTRACT(raw_payload,'$.dispatchOrderId'))=?",
                String.valueOf(orderId)));
        return result;
    }

    private DispatchEngine.DispatchScore scoreFor(Map<String, Object> event, Map<String, Object> robot) {
        return dispatchEngine.score(
                text(event, "event_type", null),
                text(event, "risk_level", "MEDIUM"),
                asDouble(event.get("longitude"), 110.3239130),
                asDouble(event.get("latitude"), 25.2875179),
                text(robot, "robot_code", "unknown"),
                asDouble(robot.get("current_lng"), 110.3239130),
                asDouble(robot.get("current_lat"), 25.2875179),
                asDouble(robot.get("battery"), 0D),
                text(robot, "state", "IDLE"),
                text(robot, "capabilities", "CAMERA"));
    }

    private Map<String, Object> requireEvent(Long eventId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT event_id, event_type, risk_level, longitude, latitude, location_name, device_id, status FROM ai_event WHERE event_id=?", eventId);
        if (rows.isEmpty()) throw new BusinessException("AI事件不存在：" + eventId);
        return rows.get(0);
    }

    private Map<String, Object> findActiveOrder(Long eventId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT order_id, order_code, state, robot_code FROM dispatch_order WHERE event_id=? AND state IN ('CREATED','EXECUTING','ARRIVED','BLOCKED') ORDER BY order_id DESC LIMIT 1",
                eventId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> requireOrder(Long orderId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM dispatch_order WHERE order_id=?", orderId);
        if (rows.isEmpty()) throw new BusinessException("派单不存在：" + orderId);
        return rows.get(0);
    }

    private Map<String, Object> orderResult(Long orderId, DispatchEngine.DispatchScore best, List<DispatchEngine.DispatchScore> allScores) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dispatched", true);
        result.put("order_id", orderId);
        result.put("robotCode", best.robotCode());
        result.put("score", best.toMap());
        result.put("allScores", allScores.stream().map(DispatchEngine.DispatchScore::toMap).toList());
        return result;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Double asDouble(Object value, Double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private String text(Map<String, Object> map, String key, String fallback) {
        Object value = map == null ? null : map.get(key);
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value);
    }

    private double number(Map<String, Object> request, String key, double fallback) {
        try {
            Object value = request == null ? null : request.get(key);
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

package com.example.evmonitorplatform.agent;

import com.example.evmonitorplatform.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a bounded, read-only snapshot for the Agent. It references the
 * platform's existing business tables; it does not create a second event
 * center or mutate governance state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiSourceContextService {
    private final JdbcTemplate jdbcTemplate;
    private final MonitorService monitorService;

    public Map<String, Object> snapshot(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("generatedAt", java.time.OffsetDateTime.now().toString());
        context.put("map", safeMap());
        context.put("sources", List.of("CAMERA", "VEHICLE_LOCATION", "GEOFENCE", "AI_EVENT", "PATROL", "DRONE_TELEMETRY", "FENCE_ALERT"));
        context.put("cameras", query("SELECT camera_id, camera_name, location, status, location_lng, location_lat FROM camera_monitor ORDER BY camera_id LIMIT ?", safeLimit));
        context.put("locations", query("SELECT d.device_id, d.vehicle_id, v.license_plate, d.current_lat, d.current_lng, d.status, d.last_report_time FROM location_device d LEFT JOIN vehicle_info v ON v.vehicle_id=d.vehicle_id ORDER BY d.last_report_time DESC LIMIT ?", safeLimit));
        context.put("geofences", query("SELECT fence_id, fence_name, fence_type, coordinates, center_lng, center_lat, alert_level, status FROM geofence WHERE deleted = 0 ORDER BY fence_id LIMIT ?", safeLimit));
        context.put("events", query("SELECT event_id, event_code, event_type, source_type, device_id, occurred_at, location_name, longitude, latitude, license_plate, risk_level, status FROM ai_event ORDER BY occurred_at DESC LIMIT ?", safeLimit));
        context.put("fusions", query("SELECT event_id, source_count, related_event_count, fused_risk_score, fused_risk_level, source_types FROM ai_event_fusion ORDER BY event_id DESC LIMIT ?", safeLimit));
        context.put("violations", query("SELECT illegal_id, illegal_type, illegal_time, illegal_state, illegal_lng, illegal_lat, vehicle_id, user_id FROM vehicle_illegal_record WHERE deleted = 0 ORDER BY illegal_time DESC LIMIT ?", safeLimit));
        context.put("patrolMissions", query("SELECT mission_id, mission_code, state, area_name, plan_version, battery_current, create_time, update_time FROM patrol_mission ORDER BY update_time DESC LIMIT ?", safeLimit));
        context.put("droneTelemetry", query("SELECT mission_id, recorded_at, lng, lat, altitude_m, speed_mps, battery, gps_quality, payload_state FROM drone_telemetry ORDER BY recorded_at DESC LIMIT ?", safeLimit));
        context.put("patrolWaypoints", query("SELECT mission_id, sequence_no, lng, lat, altitude_m, dwell_sec, risk_score, state FROM patrol_waypoint ORDER BY mission_id DESC, sequence_no LIMIT ?", safeLimit));
        context.put("fenceAlerts", query("SELECT alert_id, fence_id, vehicle_id, alert_type, alert_time, lng, lat, processed FROM geofence_alert ORDER BY alert_time DESC LIMIT ?", safeLimit));
        context.put("stats", safeStats());
        context.put("fusionSummary", fusionSummary(safeLimit));
        return context;
    }

    private Map<String, Object> fusionSummary(int limit) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pipeline", List.of("采集", "时空对齐", "身份匹配", "证据融合", "风险分级", "人工确认"));
        summary.put("recentEvidence", query("""
                SELECT e.event_id, e.event_type, e.source_type, e.device_id, e.location_name,
                       e.occurred_at, f.source_count, f.source_types, f.related_event_count,
                       f.spatial_score, f.temporal_score, f.identity_score, f.evidence_score,
                       f.fused_risk_score, f.fused_risk_level, f.explanation
                FROM ai_event_fusion f JOIN ai_event e ON e.event_id=f.event_id
                ORDER BY f.update_time DESC LIMIT ?
                """, limit));
        summary.put("riskDistribution", query("SELECT fused_risk_level, COUNT(*) AS count FROM ai_event_fusion GROUP BY fused_risk_level ORDER BY count DESC", limit));
        summary.put("relationDistribution", query("SELECT relation_type, COUNT(*) AS count FROM ai_event_relation GROUP BY relation_type ORDER BY count DESC", limit));
        return summary;
    }

    private Map<String, Object> safeMap() {
        try { return monitorService.getMapData(); }
        catch (Exception ex) { log.warn("Agent 地图快照读取失败", ex); return Map.of(); }
    }

    private Map<String, Object> safeStats() {
        try { return monitorService.getCaseStatistics(); }
        catch (Exception ex) { return Map.of(); }
    }

    private List<Map<String, Object>> query(String sql, int limit) {
        try { return jdbcTemplate.queryForList(sql, limit); }
        catch (Exception ex) {
            log.debug("Agent 数据源暂不可用: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }
}

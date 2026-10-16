package com.example.evmonitorplatform.service.impl;

import com.example.evmonitorplatform.entity.Camera;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.mapper.CameraMapper;
import com.example.evmonitorplatform.mapper.ViolationMapper;
import com.example.evmonitorplatform.mapper.VehicleMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.MonitorService;
import com.example.evmonitorplatform.vo.response.AlertVO;
import com.example.evmonitorplatform.vo.response.CameraVO;
import com.example.evmonitorplatform.vo.response.DashboardVO;
import com.example.evmonitorplatform.vo.response.WeatherVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final ViolationMapper violationMapper;
    private final CameraMapper cameraMapper;
    private final VehicleMapper vehicleMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${ev-monitor.campus.longitude:112.6827629}")
    private double campusLongitude;

    @Value("${ev-monitor.campus.latitude:26.8860959}")
    private double campusLatitude;

    @Value("${ev-monitor.campus.name:衡阳师范学院东校区}")
    private String campusName;

    @Override
    public DashboardVO getDashboardData() {
        DashboardVO dashboard = new DashboardVO();
        dashboard.setStatistics(getCaseStatistics());
        dashboard.setAlerts(getRecentAlerts(7));
        dashboard.setCameras(getAllCameras());
        dashboard.setWeather(getWeather());
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dashboard.setCurrentDate(LocalDateTime.now().format(dateFmt));
        dashboard.setCurrentTime(LocalDateTime.now().format(timeFmt));
        return dashboard;
    }

    /**
     * 从数据库获取最近违规记录作为报警列表
     */
    @Override
    public List<AlertVO> getRecentAlerts(int limit) {
        try {
            String sql = "SELECT v.illegal_id, v.illegal_type, v.illegal_time, v.illegal_state, " +
                    "       v.illegal_lng, v.illegal_lat, " +
                    "       ve.license_plate, u.real_name " +
                    "FROM vehicle_illegal_record v " +
                    "LEFT JOIN vehicle_info ve ON v.vehicle_id = ve.vehicle_id " +
                    "LEFT JOIN `user` u ON v.user_id = u.user_id " +
                    "WHERE v.deleted = 0 " +
                    "ORDER BY v.illegal_time DESC " +
                    "LIMIT ?";

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                AlertVO alert = new AlertVO();

                // 位置：有坐标则显示坐标，否则提示未知
                double lng = rs.getDouble("illegal_lng");
                double lat = rs.getDouble("illegal_lat");
                alert.setLocation(lng != 0 && lat != 0
                        ? String.format("经度 %.4f, 纬度 %.4f", lng, lat)
                        : "校园内（坐标未记录）");

                // 时间格式化
                Object illegalTime = rs.getObject("illegal_time");
                if (illegalTime instanceof LocalDateTime) {
                    alert.setTime(((LocalDateTime) illegalTime)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd / HH:mm")));
                } else {
                    alert.setTime(illegalTime != null ? illegalTime.toString() : "");
                }

                // 描述
                String plate = rs.getString("license_plate");
                String realName = rs.getString("real_name");
                int illegalType = rs.getInt("illegal_type");
                alert.setDescription(buildAlertDesc(illegalType, plate, realName));

                // 状态
                int state = rs.getInt("illegal_state");
                alert.setStatus(getViolationStateName(state));
                alert.setLevel(illegalType >= 3 ? "high" : "medium");
                return alert;
            }, limit);
        } catch (Exception e) {
            log.error("获取报警列表失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<CameraVO> getAllCameras() {
        return cameraMapper.selectList(null).stream().map(c -> {
            CameraVO vo = new CameraVO();
            vo.setId(c.getCameraId());
            vo.setName(c.getCameraName());
            vo.setLocation(c.getLocation());
            vo.setStatus(c.getStatus());
            vo.setLng(c.getLocationLng());
            vo.setLat(c.getLocationLat());
            vo.setQuality(c.getQuality());
            vo.setRtspUrl(c.getRtspUrl());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public WeatherVO getWeather() {
        // 天气需要对接第三方 API，这里保留占位但用真实城市名
        WeatherVO weather = new WeatherVO();
        weather.setCondition("晴");
        weather.setTemperature(25);
        weather.setHumidity(60);
        weather.setWindSpeed(3);
        weather.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        return weather;
    }

    /**
     * 从数据库统计真实数据
     */
    @Override
    public Map<String, Object> getCaseStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 总违规数
            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vehicle_illegal_record WHERE deleted = 0", Integer.class);
            stats.put("totalCases", total != null ? total : 0);

            // 已整改数
            Integer processed = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vehicle_illegal_record WHERE deleted = 0 AND illegal_state = 2",
                    Integer.class);
            stats.put("processedCases", processed != null ? processed : 0);

            // 申诉数
            Integer appealed = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM illegal_appeal_record", Integer.class);
            stats.put("complaintCases", appealed != null ? appealed : 0);

            // 处理率
            double rate = (total != null && total > 0)
                    ? (double) (processed != null ? processed : 0) / total * 100
                    : 0;
            stats.put("processingRate", String.format("%.1f%%", rate));

            // 申诉率
            double complaintRate = (total != null && total > 0)
                    ? (double) (appealed != null ? appealed : 0) / total * 100
                    : 0;
            stats.put("complaintRate", String.format("%.1f%%", complaintRate));

            // 违规类型分布（真实数据）
            List<Map<String, Object>> typeDistribution = jdbcTemplate.queryForList(
                    "SELECT illegal_type AS `type`, COUNT(*) AS count " +
                            "FROM vehicle_illegal_record WHERE deleted = 0 GROUP BY illegal_type ORDER BY count DESC");
            List<Map<String, Object>> typeDist = typeDistribution.stream().map(row -> {
                Map<String, Object> item = new HashMap<>();
                item.put("type", getViolationTypeName(((Number) row.get("type")).intValue()));
                item.put("count", row.get("count"));
                return item;
            }).collect(Collectors.toList());
            stats.put("typeDistribution", typeDist);

            // 今日新增
            Integer todayCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vehicle_illegal_record " +
                            "WHERE deleted = 0 AND DATE(illegal_time) = CURDATE()",
                    Integer.class);
            stats.put("todayViolations", todayCount != null ? todayCount : 0);

        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            stats.put("totalCases", 0);
            stats.put("processedCases", 0);
            stats.put("complaintCases", 0);
        }
        return stats;
    }

    @Override
    public Map<String, Object> getMapData() {
        Map<String, Object> mapData = new HashMap<>();
        // 中心点由当前部署校区配置提供，前端和巡检任务共用同一坐标基准。
        Map<String, Double> center = new HashMap<>();
        center.put("lng", campusLongitude);
        center.put("lat", campusLatitude);
        mapData.put("center", center);
        mapData.put("campusName", campusName);

        List<Map<String, Object>> markers = new ArrayList<>();

        // 摄像头标记（真实数据）
        cameraMapper.selectList(null).forEach(camera -> {
            if (camera.getLocationLng() != null && camera.getLocationLat() != null
                    && isNearCampus(camera.getLocationLng(), camera.getLocationLat())) {
                Map<String, Object> marker = new HashMap<>();
                marker.put("lng", camera.getLocationLng());
                marker.put("lat", camera.getLocationLat());
                marker.put("title", camera.getCameraName());
                marker.put("type", "camera");
                marker.put("status", camera.getStatus());
                markers.add(marker);
            }
        });

        // 违规热点标记（真实数据，最近100条有坐标的）
        try {
            jdbcTemplate.queryForList(
                    "SELECT illegal_lng, illegal_lat, illegal_type, illegal_state " +
                     "FROM vehicle_illegal_record WHERE deleted = 0 " +
                     "AND illegal_lng IS NOT NULL AND illegal_lat IS NOT NULL " +
                            "ORDER BY illegal_time DESC LIMIT 100")
                    .stream()
                    .filter(row -> isNearCampus(row.get("illegal_lng"), row.get("illegal_lat")))
                    .forEach(row -> {
                        Map<String, Object> marker = new HashMap<>();
                        marker.put("lng", row.get("illegal_lng"));
                        marker.put("lat", row.get("illegal_lat"));
                        marker.put("title", getViolationTypeName(((Number) row.get("illegal_type")).intValue()));
                        marker.put("type", "violation");
                        marker.put("state", row.get("illegal_state"));
                        markers.add(marker);
                    });
        } catch (Exception e) {
            log.warn("查询违规坐标失败", e);
        }

        // 统一 AI 事件图层：摄像头、定位、围栏、人工和其他设备事件都回到原地图。
        try {
            jdbcTemplate.queryForList(
                    "SELECT e.event_id, e.event_type, e.source_type, e.status, e.risk_level, " +
                            "e.location_name, e.longitude, e.latitude, e.occurred_at, " +
                            "f.source_count, f.related_event_count, f.fused_risk_score, f.fused_risk_level " +
                     "FROM ai_event e LEFT JOIN ai_event_fusion f ON f.event_id=e.event_id " +
                     "WHERE e.longitude IS NOT NULL AND e.latitude IS NOT NULL " +
                            "ORDER BY e.occurred_at DESC LIMIT 100")
                    .stream()
                    .filter(row -> isNearCampus(row.get("longitude"), row.get("latitude")))
                    .forEach(row -> {
                        Map<String, Object> marker = new HashMap<>();
                        marker.put("eventId", row.get("event_id"));
                        marker.put("lng", row.get("longitude"));
                        marker.put("lat", row.get("latitude"));
                        marker.put("title", row.get("event_type"));
                        marker.put("type", "ai-event");
                        marker.put("sourceType", row.get("source_type"));
                        marker.put("status", row.get("status"));
                        marker.put("riskLevel", row.get("fused_risk_level") == null ? row.get("risk_level")
                                : row.get("fused_risk_level"));
                        marker.put("sourceCount", row.get("source_count"));
                        marker.put("relatedEventCount", row.get("related_event_count"));
                        marker.put("fusedRiskScore", row.get("fused_risk_score"));
                        marker.put("locationName", row.get("location_name"));
                        marker.put("occurredAt", row.get("occurred_at"));
                        markers.add(marker);
                    });
        } catch (Exception e) {
            log.debug("查询 AI 事件地图图层失败: {}", e.getMessage());
        }

        mapData.put("markers", markers);
        mapData.put("paths", Collections.emptyList()); // 轨迹由 WebSocket 实时推送
        // 巡检数据作为原地图数据的扩展图层返回，避免巡检模块维护第二套地图状态。
        mapData.put("patrol", getPatrolOverlayData());
        return mapData;
    }

    /**
     * 返回原大屏需要的巡检叠加数据。巡检表属于可选扩展模块，表尚未创建或历史数据异常时
     * 只返回空图层，不能影响原车辆、摄像头和围栏地图。
     */
    private Map<String, Object> getPatrolOverlayData() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> missions = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT mission_id, mission_code, area_name, state, plan_version, battery_current, " +
                            "center_lng, center_lat, update_time " +
                    "FROM patrol_mission WHERE state IN ('RUNNING','PAUSED','RETURNING','MANUAL_CONTROL') " +
                            "AND update_time >= DATE_SUB(NOW(), INTERVAL 30 MINUTE) " +
                            "ORDER BY CASE WHEN state IN ('RUNNING','RETURNING','MANUAL_CONTROL') THEN 0 ELSE 1 END, update_time DESC LIMIT 6");
            for (Map<String, Object> mission : rows) {
                Long missionId = asLong(mission.get("mission_id"));
                if (missionId == null)
                    continue;
                Map<String, Object> item = new LinkedHashMap<>(mission);
                item.put("executionSource", "PYBULLET_PHYSICS_SIMULATION");
                item.put("telemetryTruth", "BACKEND_TELEMETRY");
                item.put("waypoints", jdbcTemplate.queryForList(
                        "SELECT waypoint_id, sequence_no, lng, lat, altitude_m, risk_score, state " +
                                "FROM patrol_waypoint WHERE mission_id=? ORDER BY sequence_no",
                        missionId));
                item.put("trail", jdbcTemplate.queryForList(
                        "SELECT lng, lat, recorded_at, battery, gps_quality " +
                                "FROM drone_telemetry WHERE mission_id=? ORDER BY recorded_at DESC LIMIT 240",
                        missionId));
                item.put("events", jdbcTemplate.queryForList(
                        "SELECT e.event_id, e.event_type, e.source_type, e.status, e.risk_level, " +
                                "e.location_name, e.longitude, e.latitude, e.occurred_at, l.relation_type, " +
                                "f.source_count, f.related_event_count, f.fused_risk_score, f.fused_risk_level " +
                                "FROM patrol_event_link l JOIN ai_event e ON e.event_id=l.event_id " +
                                "LEFT JOIN ai_event_fusion f ON f.event_id=e.event_id " +
                                "WHERE l.mission_id=? ORDER BY e.occurred_at DESC LIMIT 20",
                        missionId));
                List<Map<String, Object>> latest = jdbcTemplate.queryForList(
                        "SELECT lng, lat, altitude_m, speed_mps, battery, gps_quality, payload_state, raw_payload, recorded_at " +
                                "FROM drone_telemetry WHERE mission_id=? ORDER BY recorded_at DESC LIMIT 1",
                        missionId);
                item.put("latestTelemetry", latest.isEmpty() ? null : latest.get(0));
                item.put("coverage", queryCoverageSummary(missionId));
                missions.add(item);
            }
        } catch (Exception ex) {
            log.debug("巡检叠加数据不可用: {}", ex.getMessage());
        }
        result.put("missions", missions);
        result.put("updatedAt", LocalDateTime.now());
        return result;
    }

    private Long asLong(Object value) {
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    /** Only show map facts in the configured campus area; old demo seeds from other campuses stay in history. */
    private boolean isNearCampus(Object lngValue, Object latValue) {
        try {
            double lng = Double.parseDouble(String.valueOf(lngValue));
            double lat = Double.parseDouble(String.valueOf(latValue));
            return Math.abs(lng - campusLongitude) <= 0.02D
                    && Math.abs(lat - campusLatitude) <= 0.02D;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 读取任务最新一次 Fields2Cover 全覆盖规划的工具记录，解析出扫掠带数、覆盖面积、航线长度。
     * tool_execution.output_summary 是 Java Map.toString 格式：{swathCount=4,
     * areaM2=11014.9, pathLengthM=334.7}
     * 返回 null 表示该任务走的是风险网格贪心航线（非全覆盖）。
     */
    private Map<String, Object> queryCoverageSummary(Long missionId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT output_summary FROM tool_execution " +
                            "WHERE mission_id=? AND tool_name='PLAN_COVERAGE_ROUTE' AND state='SUCCEEDED' " +
                            "ORDER BY create_time DESC LIMIT 1",
                    missionId);
            if (rows.isEmpty())
                return null;
            String raw = String.valueOf(rows.get(0).get("output_summary"));
            if (raw == null || raw.isEmpty() || "null".equals(raw))
                return null;
            String body = raw.trim();
            if (body.startsWith("{"))
                body = body.substring(1);
            if (body.endsWith("}"))
                body = body.substring(0, body.length() - 1);
            Map<String, Object> cov = new LinkedHashMap<>();
            for (String pair : body.split(",")) {
                String kv = pair.trim();
                int eq = kv.indexOf('=');
                if (eq <= 0)
                    continue;
                String key = kv.substring(0, eq).trim();
                String val = kv.substring(eq + 1).trim();
                if ("swathCount".equals(key)) {
                    cov.put("swathCount", (int) Double.parseDouble(val));
                } else if ("areaM2".equals(key)) {
                    cov.put("areaM2", Math.round(Double.parseDouble(val)));
                } else if ("pathLengthM".equals(key)) {
                    cov.put("pathLengthM", Math.round(Double.parseDouble(val)));
                }
            }
            return cov.isEmpty() ? null : cov;
        } catch (Exception ex) {
            log.debug("解析覆盖航线摘要失败 missionId={}: {}", missionId, ex.getMessage());
            return null;
        }
    }

    // ===== 工具方法 =====

    private String buildAlertDesc(int illegalType, String plate, String realName) {
        String typeName = getViolationTypeName(illegalType);
        String who = (realName != null && !realName.isEmpty()) ? realName : "未知人员";
        String vehicleStr = (plate != null && !plate.isEmpty()) ? "（" + plate + "）" : "";
        return who + vehicleStr + " — " + typeName;
    }

    private String getViolationTypeName(int type) {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "违规停放");
        map.put(2, "超速行驶");
        map.put(3, "违规充电");
        map.put(4, "未登记车辆");
        map.put(5, "进入禁行区");
        map.put(6, "违规载人");
        map.put(7, "遮挡车牌");
        map.put(8, "其他违规");
        return map.getOrDefault(type, "未知违规");
    }

    private String getViolationStateName(int state) {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "待整改");
        map.put(1, "整改中");
        map.put(2, "已整改");
        map.put(3, "逾期未整改");
        map.put(4, "申诉中");
        return map.getOrDefault(state, "未知");
    }

    @Override
    public Map<String, Object> getRealtimeData() {
        Map<String, Object> data = new HashMap<>();

        try {
            Integer todayViolations = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vehicle_illegal_record WHERE deleted = 0 AND DATE(illegal_time) = CURDATE()",
                    Integer.class);
            data.put("realtimeViolations", todayViolations != null ? todayViolations : 0);

            Integer processingViolations = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vehicle_illegal_record WHERE deleted = 0 AND illegal_state = 1",
                    Integer.class);
            data.put("realtimeProcessing", processingViolations != null ? processingViolations : 0);

            Integer onlineCameras = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM camera WHERE status = 'online'",
                    Integer.class);
            data.put("realtimeOnlineCameras", onlineCameras != null ? onlineCameras : 0);

            Integer approvedVehicles = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vehicle_info WHERE deleted = 0 AND status = 'approved'",
                    Integer.class);
            data.put("realtimeVehicles", approvedVehicles != null ? approvedVehicles : 0);

            Integer todayPassIn = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pass_record WHERE deleted = 0 AND DATE(pass_time) = CURDATE() AND direction = 'in'",
                    Integer.class);
            data.put("todayPassIn", todayPassIn != null ? todayPassIn : 0);

            Integer todayPassOut = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pass_record WHERE deleted = 0 AND DATE(pass_time) = CURDATE() AND direction = 'out'",
                    Integer.class);
            data.put("todayPassOut", todayPassOut != null ? todayPassOut : 0);

        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            data.put("realtimeViolations", 0);
            data.put("realtimeProcessing", 0);
            data.put("realtimeOnlineCameras", 0);
            data.put("realtimeVehicles", 0);
            data.put("todayPassIn", 0);
            data.put("todayPassOut", 0);
        }

        return data;
    }
}

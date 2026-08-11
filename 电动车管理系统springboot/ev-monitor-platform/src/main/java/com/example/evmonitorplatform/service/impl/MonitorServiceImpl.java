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
    private final CameraMapper    cameraMapper;
    private final VehicleMapper   vehicleMapper;
    private final UserMapper      userMapper;
    private final JdbcTemplate    jdbcTemplate;

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
            String sql =
                    "SELECT v.illegal_id, v.illegal_type, v.illegal_time, v.illegal_state, " +
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
                String plate    = rs.getString("license_plate");
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
                    ? (double)(processed != null ? processed : 0) / total * 100 : 0;
            stats.put("processingRate", String.format("%.1f%%", rate));

            // 申诉率
            double complaintRate = (total != null && total > 0)
                    ? (double)(appealed != null ? appealed : 0) / total * 100 : 0;
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
                            "WHERE deleted = 0 AND DATE(illegal_time) = CURDATE()", Integer.class);
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
        // 中心点（衡阳师范学院）
        Map<String, Double> center = new HashMap<>();
        center.put("lng", 112.6827629);
        center.put("lat", 26.8860959);
        mapData.put("center", center);

        List<Map<String, Object>> markers = new ArrayList<>();

        // 摄像头标记（真实数据）
        cameraMapper.selectList(null).forEach(camera -> {
            if (camera.getLocationLng() != null && camera.getLocationLat() != null) {
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
                            "ORDER BY illegal_time DESC LIMIT 100"
            ).forEach(row -> {
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

        mapData.put("markers", markers);
        mapData.put("paths", Collections.emptyList()); // 轨迹由 WebSocket 实时推送
        return mapData;
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
        map.put(1, "违规停放"); map.put(2, "超速行驶"); map.put(3, "违规充电");
        map.put(4, "未登记车辆"); map.put(5, "进入禁行区"); map.put(6, "违规载人");
        map.put(7, "遮挡车牌"); map.put(8, "其他违规");
        return map.getOrDefault(type, "未知违规");
    }

    private String getViolationStateName(int state) {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "待整改"); map.put(1, "整改中"); map.put(2, "已整改");
        map.put(3, "逾期未整改"); map.put(4, "申诉中");
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
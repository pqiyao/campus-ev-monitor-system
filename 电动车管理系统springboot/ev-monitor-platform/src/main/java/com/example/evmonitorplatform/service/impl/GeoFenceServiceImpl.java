package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.entity.GeoFence;
import com.example.evmonitorplatform.entity.GeoFenceAlert;
import com.example.evmonitorplatform.mapper.GeoFenceAlertMapper;
import com.example.evmonitorplatform.mapper.GeoFenceMapper;
import com.example.evmonitorplatform.service.GeoFenceService;
import com.example.evmonitorplatform.service.AiGovernanceService;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoFenceServiceImpl extends ServiceImpl<GeoFenceMapper, GeoFence>
        implements GeoFenceService {

    private final GeoFenceMapper       fenceMapper;
    private final GeoFenceAlertMapper  alertMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiGovernanceService aiGovernanceService;
    private final ObjectMapper         objectMapper;

    /**
     * 车辆上一次位置状态缓存：vehicleId -> Set<fenceId>(当前在哪些围栏内)
     * 用于判断"入围 / 出围"（状态变化才触发，避免持续重复报警）
     */
    private final ConcurrentHashMap<Integer, Set<Integer>> vehicleFenceState = new ConcurrentHashMap<>();

    // =====================================================================
    // CRUD
    // =====================================================================

    @Override
    public List<GeoFence> getAllEnabled() {
        return fenceMapper.selectAllEnabled();
    }

    @Override
    public List<GeoFence> getAll() {
        return fenceMapper.selectList(
                new LambdaQueryWrapper<GeoFence>().eq(GeoFence::getDeleted, 0)
                        .orderByDesc(GeoFence::getCreateTime));
    }

    @Override
    public GeoFence create(GeoFence fence) {
        fence.setStatus(fence.getStatus() == null ? 1 : fence.getStatus());
        fence.setDeleted(0);
        fence.setCreateTime(LocalDateTime.now());
        fence.setUpdateTime(LocalDateTime.now());
        // 自动计算中心点
        calcCenter(fence);
        fenceMapper.insert(fence);
        return fence;
    }

    @Override
    public boolean update(GeoFence fence) {
        fence.setUpdateTime(LocalDateTime.now());
        calcCenter(fence);
        return fenceMapper.updateById(fence) > 0;
    }

    @Override
    public boolean delete(Integer fenceId) {
        return fenceMapper.deleteById(fenceId) > 0;
    }

    @Override
    public boolean toggleStatus(Integer fenceId) {
        GeoFence fence = fenceMapper.selectById(fenceId);
        if (fence == null) return false;
        fence.setStatus(fence.getStatus() == 1 ? 0 : 1);
        return fenceMapper.updateById(fence) > 0;
    }

    // =====================================================================
    // 越界检测（核心算法）
    // =====================================================================

    @Override
    public List<GeoFenceAlert> checkAndAlert(Double lng, Double lat,
                                              Integer vehicleId, String licensePlate,
                                              Integer userId, String userName) {
        if (lng == null || lat == null) return Collections.emptyList();

        List<GeoFence> fences = getAllEnabled();
        List<GeoFenceAlert> triggered = new ArrayList<>();

        // 当前在哪些围栏内
        Set<Integer> currentIn = new HashSet<>();
        for (GeoFence fence : fences) {
            if (isPointInPolygon(lng, lat, fence.getCoordinates())) {
                currentIn.add(fence.getFenceId());
            }
        }

        // 上次状态
        Integer vid = vehicleId != null ? vehicleId : -1;
        Set<Integer> previousIn = vehicleFenceState.getOrDefault(vid, Collections.emptySet());

        // 入围：本次在，上次不在
        for (Integer fenceId : currentIn) {
            if (!previousIn.contains(fenceId)) {
                GeoFence fence = fences.stream()
                        .filter(f -> f.getFenceId().equals(fenceId)).findFirst().orElse(null);
                if (fence != null && Integer.valueOf(1).equals(fence.getFenceType())) {
                    GeoFenceAlert alert = buildAlert(fence, vehicleId, licensePlate,
                            userId, userName, lng, lat, "enter");
                    alertMapper.insert(alert);
                    triggered.add(alert);
                    pushAlertWs(alert, fence);
                    recordRestrictedAreaEvent(alert, fence);
                }
            }
        }

        // 出围：上次在，本次不在（用于"禁止驶出"类型）
        for (Integer fenceId : previousIn) {
            if (!currentIn.contains(fenceId)) {
                GeoFence fence = fences.stream()
                        .filter(f -> f.getFenceId().equals(fenceId)).findFirst().orElse(null);
                if (fence != null && fence.getFenceType() == 2) {
                    GeoFenceAlert alert = buildAlert(fence, vehicleId, licensePlate,
                            userId, userName, lng, lat, "exit");
                    alertMapper.insert(alert);
                    triggered.add(alert);
                    pushAlertWs(alert, fence);
                    recordRestrictedAreaEvent(alert, fence);
                }
            }
        }

        // 更新状态缓存
        vehicleFenceState.put(vid, new HashSet<>(currentIn));
        return triggered;
    }

    // =====================================================================
    // 预警管理
    // =====================================================================

    @Override
    public List<GeoFenceAlert> getRecentAlerts(int limit) {
        return alertMapper.selectRecentAlerts(Math.min(limit, 100));
    }

    @Override
    public boolean processAlert(Integer alertId) {
        return alertMapper.markProcessed(alertId) > 0;
    }

    @Override
    public int countUnprocessed() {
        return alertMapper.countUnprocessed();
    }

    @Override
    public int countToday() {
        return alertMapper.countToday();
    }

    @Override
    public Map<String, Object> getSummary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalFences",     this.count(new LambdaQueryWrapper<GeoFence>().eq(GeoFence::getDeleted, 0)));
        map.put("enabledFences",   this.count(new LambdaQueryWrapper<GeoFence>().eq(GeoFence::getStatus, 1).eq(GeoFence::getDeleted, 0)));
        map.put("todayAlerts",     countToday());
        map.put("unprocessed",     countUnprocessed());
        return map;
    }

    @Override
    public boolean isInsideAnyParkingZone(double lng, double lat) {
        for (GeoFence fence : getAllEnabled()) {
            if (fence.getFenceType() != null && fence.getFenceType() == 5
                    && isPointInPolygon(lng, lat, fence.getCoordinates())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasParkingZoneConfigured() {
        for (GeoFence fence : getAllEnabled()) {
            if (fence.getFenceType() != null && fence.getFenceType() == 5) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    // 内部工具方法
    // =====================================================================

    /**
     * Ray-casting 射线法：判断点 (lng, lat) 是否在多边形内
     * coordinatesJson 格式：[{"lng":x,"lat":y},...]
     */
    private boolean isPointInPolygon(double lng, double lat, String coordinatesJson) {
        try {
            List<Map<String, Object>> points = objectMapper.readValue(
                    coordinatesJson, new TypeReference<List<Map<String, Object>>>() {});
            if (points == null || points.size() < 3) return false;

            int n = points.size();
            boolean inside = false;
            int j = n - 1;
            for (int i = 0; i < n; i++) {
                double xi = toDouble(points.get(i).get("lng"));
                double yi = toDouble(points.get(i).get("lat"));
                double xj = toDouble(points.get(j).get("lng"));
                double yj = toDouble(points.get(j).get("lat"));

                boolean intersect = ((yi > lat) != (yj > lat))
                        && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
                if (intersect) inside = !inside;
                j = i;
            }
            return inside;
        } catch (Exception e) {
            log.warn("围栏坐标解析失败 fenceCoord={}", coordinatesJson, e);
            return false;
        }
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(val.toString());
    }

    private GeoFenceAlert buildAlert(GeoFence fence, Integer vehicleId, String licensePlate,
                                     Integer userId, String userName,
                                     double lng, double lat, String alertType) {
        GeoFenceAlert alert = new GeoFenceAlert();
        alert.setFenceId(fence.getFenceId());
        alert.setFenceName(fence.getFenceName());
        alert.setVehicleId(vehicleId);
        alert.setLicensePlate(licensePlate != null ? licensePlate : "未知");
        alert.setUserId(userId);
        alert.setUserName(userName != null ? userName : "未知");
        alert.setAlertType(alertType);
        alert.setTriggerLng(lng);
        alert.setTriggerLat(lat);
        alert.setAlertLevel(fence.getAlertLevel() != null ? fence.getAlertLevel() : 2);
        alert.setIsProcessed(0);
        alert.setCreateTime(LocalDateTime.now());
        return alert;
    }

    /**
     * 通过 WebSocket 推送围栏预警到前端大屏
     * 前端订阅 /topic/fence-alert
     */
    private void pushAlertWs(GeoFenceAlert alert, GeoFence fence) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type",         "FENCE_ALERT");
            payload.put("alertId",      alert.getAlertId());
            payload.put("fenceId",      alert.getFenceId());
            payload.put("fenceName",    alert.getFenceName());
            payload.put("fenceColor",   fence.getFenceColor());
            payload.put("alertType",    alert.getAlertType()); // enter / exit
            payload.put("alertLevel",   alert.getAlertLevel());
            payload.put("licensePlate", alert.getLicensePlate());
            payload.put("userName",     alert.getUserName());
            payload.put("lng",          alert.getTriggerLng());
            payload.put("lat",          alert.getTriggerLat());
            payload.put("time",         alert.getCreateTime().toString());
            messagingTemplate.convertAndSend("/topic/fence-alert", payload);
        } catch (Exception e) {
            log.warn("围栏预警 WebSocket 推送失败", e);
        }
    }

    private void recordRestrictedAreaEvent(GeoFenceAlert alert, GeoFence fence) {
        try {
            AiEventCreateRequest request = new AiEventCreateRequest();
            request.setEventType("RESTRICTED_AREA");
            request.setSourceType("GEOFENCE");
            request.setDeviceId("geofence-" + fence.getFenceId());
            request.setOccurredAt(alert.getCreateTime());
            request.setLocationName(fence.getFenceName());
            request.setLongitude(alert.getTriggerLng());
            request.setLatitude(alert.getTriggerLat());
            request.setLicensePlate(alert.getLicensePlate());
            request.setVehicleId(alert.getVehicleId());
            request.setUserId(alert.getUserId());
            request.setDetectionConfidence(1.0D);
            request.setEvidenceUrls(List.of());
            request.setRawPayload(Map.of(
                    "alertId", alert.getAlertId(),
                    "fenceId", fence.getFenceId(),
                    "fenceType", fence.getFenceType(),
                    "alertType", alert.getAlertType()));
            request.setIdempotencyKey("geofence-alert-" + alert.getAlertId());
            request.setAutoAnalyze(false);
            aiGovernanceService.ingest(request);
        } catch (Exception ex) {
            log.warn("围栏预警写入AI治理事件失败 alertId={}: {}", alert.getAlertId(), ex.getMessage());
        }
    }

    /** 根据坐标 JSON 计算中心点，写入 fence */
    private void calcCenter(GeoFence fence) {
        if (fence.getCoordinates() == null) return;
        try {
            List<Map<String, Object>> points = objectMapper.readValue(
                    fence.getCoordinates(), new TypeReference<List<Map<String, Object>>>() {});
            if (points == null || points.isEmpty()) return;
            double sumLng = 0, sumLat = 0;
            for (Map<String, Object> p : points) {
                sumLng += toDouble(p.get("lng"));
                sumLat += toDouble(p.get("lat"));
            }
            fence.setCenterLng(sumLng / points.size());
            fence.setCenterLat(sumLat / points.size());
        } catch (Exception ignored) {}
    }
}

package com.example.evmonitorplatform.service.impl;

import com.example.evmonitorplatform.entity.GeoFenceAlert;
import com.example.evmonitorplatform.entity.LocationDevice;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.mapper.VehicleMapper;
import com.example.evmonitorplatform.service.GeoFenceService;
import com.example.evmonitorplatform.service.LocationDeviceService;
import com.example.evmonitorplatform.service.LocationService;
import com.example.evmonitorplatform.service.ViolationService;
import com.example.evmonitorplatform.vo.request.SimPointRequest;
import com.example.evmonitorplatform.vo.response.VehicleLocationVO;
import com.example.evmonitorplatform.websocket.LocationWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 位置：payload {N:纬度,W:经度,D:终端标识}。D 可与注册信息关联：车架号、定位 MAC（location_device）、或车牌号。
 * 主题支持 ev/location 或 ev/location/{MAC}（MAC 亦可仅在主题后缀、D 省略时生效）。
 * 若库中无匹配车辆，仍按设备号写入 Redis 并上屏（未绑定）；绑定后同一设备下次上报将走正式车辆逻辑并清除未绑定缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate          messagingTemplate;
    private final ObjectMapper                   objectMapper;
    private final GeoFenceService                geoFenceService;
    private final VehicleMapper                  vehicleMapper;
    private final UserMapper                     userMapper;
    private final LocationDeviceService          locationDeviceService;
    private final ViolationService               violationService;
    private final LocationWebSocketHandler       locationWebSocketHandler;

    @Value("${ev-monitor.parking.stationary-minutes:150}")
    private int stationaryMinutes;

    @Value("${ev-monitor.parking.move-threshold-meters:35}")
    private double moveThresholdMeters;

    @Value("${ev-monitor.violation.illegal-parking-type-id:1}")
    private int illegalParkingTypeId;

    private static final String REDIS_LOCATION_PREFIX   = "ev:location:";
    /** 屏幕点位与真实点共用前缀扫描 ev:location:*，子前缀 sim 避免与 vehicleId 数字键冲突 */
    private static final String REDIS_SIM_SUBPREFIX     = "sim:";
    /** 未绑定车辆的 MQTT 设备：ev:location:unbound:{suffix} */
    private static final String REDIS_UNBOUND_SUBPREFIX = "unbound:";
    private static final String REDIS_STATIONARY_PREFIX = "ev:stationary:";
    private static final String REDIS_PARK_VIOL_PREFIX  = "ev:parkviol:";
    private static final long   LOCATION_TTL_SECONDS    = 300;

    @Value("${ev-monitor.location.sim-ttl-minutes:1440}")
    private int simTtlMinutesDefault;

    @Value("${ev-monitor.location.stale-seconds:300}")
    private int locationStaleSeconds;

    @Override
    public Map<String, Object> getParkingRuntimeConfig() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("stationaryMinutes", stationaryMinutes);
        cfg.put("moveThresholdMeters", moveThresholdMeters);
        cfg.put("illegalParkingTypeId", illegalParkingTypeId);
        return cfg;
    }

    @Override
    public Map<String, Object> updateParkingRuntimeConfig(Integer stationaryMinutes, Double moveThresholdMeters) {
        if (stationaryMinutes != null && stationaryMinutes > 0) {
            this.stationaryMinutes = stationaryMinutes;
        }
        if (moveThresholdMeters != null && moveThresholdMeters > 0) {
            this.moveThresholdMeters = moveThresholdMeters;
        }
        return getParkingRuntimeConfig();
    }

    @Override
    public void handleLocationMessage(String topic, String payload) {
        System.out.println("后台控制打印: 接收到位置消息 - topic: " + topic + ", payload: " + payload);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            Double lat = toDouble(data.get("N"));
            Double lng = toDouble(data.get("W"));
            Object dRaw = data.get("D");
            String dField = dRaw == null ? null : dRaw.toString().trim();

            System.out.println("后台控制打印: 解析后 - lat: " + lat + ", lng: " + lng + ", dField: " + dField);

            String topicSuffix = "";
            if (topic != null && topic.startsWith("ev/location/") && topic.length() > "ev/location/".length()) {
                topicSuffix = normalizeDeviceId(topic.substring("ev/location/".length()));
            }

            String deviceKey = null;
            if (dField != null && !dField.isEmpty()) {
                deviceKey = normalizeDeviceId(dField);
            } else if (!topicSuffix.isEmpty()) {
                deviceKey = topicSuffix;
            }

            if (lat == null || lng == null || deviceKey == null || deviceKey.isEmpty()) {
                log.warn("位置消息缺少必要字段: topic={}, payload={}", topic, payload);
                // 即使缺少经纬度，也打印设备信息
                System.out.println("后台控制打印: 设备信息 - deviceKey: " + deviceKey + ", lat: " + lat + ", lng: " + lng);
                return;
            }

            // 打印接收到的车辆定位经纬度
            log.info("MQTT收到消息 topic={}, 设备={}, 经度={}, 纬度={}", topic, deviceKey, lng, lat);

            Vehicle vehicle = resolveVehicle(deviceKey, dField);
            if (vehicle == null) {
                // 如果经纬度为空，设置默认值0.0以便保存未绑定设备
                double safeLat = lat != null ? lat : 0.0;
                double safeLng = lng != null ? lng : 0.0;
                saveUnboundDeviceLocation(deviceKey, safeLat, safeLng, data);
                return;
            }

            redisTemplate.delete(redisKeyUnbound(deviceKey));

            Integer vehicleId = vehicle.getVehicleId();
            User user = vehicle.getUserId() != null ? userMapper.selectById(vehicle.getUserId()) : null;
            String licensePlate = vehicle.getLicensePlate();
            Integer userId = vehicle.getUserId();
            String userName = user != null ? user.getRealName() : "";
            String frameNum = vehicle.getFrameNum();

            Map<String, Object> locationData = new LinkedHashMap<>();
            locationData.put("vehicleId", vehicleId);
            locationData.put("lng", lng);
            locationData.put("lat", lat);
            locationData.put("speed", 0);
            locationData.put("licensePlate", licensePlate);
            locationData.put("userId", userId);
            locationData.put("userName", userName);
            locationData.put("frameNum", frameNum);
            locationData.put("deviceId", deviceKey);
            locationData.put("updateTime", LocalDateTime.now().toString());

            redisTemplate.opsForValue().set(REDIS_LOCATION_PREFIX + vehicleId, locationData, LOCATION_TTL_SECONDS, TimeUnit.SECONDS);

            Map<String, Object> wsPayload = new LinkedHashMap<>(locationData);
            wsPayload.put("type", "LOCATION_UPDATE");
            if (userName != null && !userName.isEmpty()) {
                wsPayload.put("ownerName", userName);
            }
            messagingTemplate.convertAndSend("/topic/location", wsPayload);
            try {
                locationWebSocketHandler.broadcastJson(objectMapper.writeValueAsString(wsPayload));
            } catch (JsonProcessingException e) {
                log.warn("位置 JSON 序列化失败 vehicleId={}", vehicleId);
            }

            List<GeoFenceAlert> alerts = geoFenceService.checkAndAlert(
                    lng, lat, vehicleId, licensePlate, userId, userName);
            if (!alerts.isEmpty()) {
                log.info("车辆 {} ({}) 触发围栏预警 {} 条", vehicleId, licensePlate, alerts.size());
            }

            checkLongStayIllegalParking(vehicleId, userId, lng, lat);

            syncLocationDevicePosition(vehicleId, lng, lat);

        } catch (Exception e) {
            log.error("处理位置消息失败，topic={}, payload={}", topic, payload, e);
        }
    }

    private void syncLocationDevicePosition(Integer vehicleId, double lng, double lat) {
        try {
            LocationDevice dev = locationDeviceService.getByVehicleId(vehicleId);
            if (dev == null) return;
            dev.setCurrentLng(lng);
            dev.setCurrentLat(lat);
            dev.setLastReportTime(LocalDateTime.now());
            locationDeviceService.updateById(dev);
        } catch (Exception e) {
            log.debug("同步 location_device 坐标失败 vehicleId={}", vehicleId, e);
        }
    }

    /**
     * 在已配置「允许停车区」(fenceType=5) 的前提下：车辆处于非停车区且位置几乎不变超过 stationaryMinutes，则记一条违停。
     */
    private void checkLongStayIllegalParking(Integer vehicleId, Integer userId, double lng, double lat) {
        if (!geoFenceService.hasParkingZoneConfigured()) {
            return;
        }
        if (geoFenceService.isInsideAnyParkingZone(lng, lat)) {
            redisTemplate.delete(REDIS_STATIONARY_PREFIX + vehicleId);
            return;
        }

        String key = REDIS_STATIONARY_PREFIX + vehicleId;
        @SuppressWarnings("unchecked")
        Map<String, Object> st = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        long now = System.currentTimeMillis();

        if (st == null) {
            st = new LinkedHashMap<>();
            st.put("since", now);
            st.put("lng", lng);
            st.put("lat", lat);
            redisTemplate.opsForValue().set(key, st, 48, TimeUnit.HOURS);
            return;
        }

        long since = ((Number) st.get("since")).longValue();
        double ol = toDouble(st.get("lng"));
        double oa = toDouble(st.get("lat"));
        if (distanceMeters(ol, oa, lng, lat) > moveThresholdMeters) {
            st.put("since", now);
            st.put("lng", lng);
            st.put("lat", lat);
            redisTemplate.opsForValue().set(key, st, 48, TimeUnit.HOURS);
            return;
        }

        long minutes = (now - since) / 60_000L;
        if (minutes < stationaryMinutes) {
            return;
        }

        String dedupe = REDIS_PARK_VIOL_PREFIX + vehicleId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupe, "1", java.time.Duration.ofHours(12));
        if (!Boolean.TRUE.equals(first)) {
            return;
        }

        Violation v = new Violation();
        v.setVehicleId(vehicleId);
        v.setUserId(userId);
        v.setIllegalType(illegalParkingTypeId);
        v.setIllegalLng(lng);
        v.setIllegalLat(lat);
        v.setIllegalTime(LocalDateTime.now());
        v.setDeductionPoints(3);
        v.setIllegalSource(2);
        violationService.reportViolation(v);
        log.info("非停车区静置超阈值，已写入违停 vehicleId={} minutes≈{}", vehicleId, minutes);
    }

    private String redisKeyUnbound(String deviceKey) {
        return REDIS_LOCATION_PREFIX + REDIS_UNBOUND_SUBPREFIX
                + sanitizeSimKey(deviceKey, 0, deviceKey);
    }

    /**
     * 库中无绑定车辆时仍保存位置，供地图展示；围栏/违停等业务仅在绑定后执行。
     */
    private void saveUnboundDeviceLocation(String deviceKey, double lat, double lng, Map<String, Object> raw) {
        Map<String, Object> locationData = new LinkedHashMap<>();
        locationData.put("vehicleId", null);
        locationData.put("licensePlate", null);
        locationData.put("userId", null);
        locationData.put("userName", null);
        locationData.put("frameNum", null);
        locationData.put("deviceId", deviceKey);
        locationData.put("unbound", Boolean.TRUE);
        locationData.put("lat", lat);
        locationData.put("lng", lng);
        Double spd = toDouble(raw.get("S"));
        if (spd == null) {
            spd = toDouble(raw.get("speed"));
        }
        locationData.put("speed", spd != null ? spd : 0);
        Integer bat = toInt(raw.get("battery"));
        locationData.put("battery", bat != null ? bat : 100);
        locationData.put("updateTime", LocalDateTime.now().toString());

        redisTemplate.opsForValue().set(redisKeyUnbound(deviceKey), locationData, LOCATION_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, Object> wsPayload = new LinkedHashMap<>(locationData);
        wsPayload.put("type", "LOCATION_UPDATE");
        messagingTemplate.convertAndSend("/topic/location", wsPayload);
        try {
            locationWebSocketHandler.broadcastJson(objectMapper.writeValueAsString(wsPayload));
        } catch (JsonProcessingException e) {
            log.warn("未绑定设备位置 JSON 序列化失败 deviceId={}", deviceKey);
        }
        log.info("未绑定设备已上屏 deviceId={}（后续注册绑定后将显示车辆信息）", deviceKey);
    }

    /**
     * 按注册数据多路匹配：车架号、定位设备 MAC（location_device.device_number）、车牌号。
     * MAC 与注册时一致（bindDevice 会规范化存储）；终端上报的 D 经 normalizeDeviceId 后应对得上。
     */
    private Vehicle resolveVehicle(String normalizedId, String rawD) {
        if (normalizedId != null && !normalizedId.isEmpty()) {
            Vehicle byFrame = vehicleMapper.selectByFrameNum(normalizedId);
            if (byFrame != null) {
                return byFrame;
            }
            LocationDevice dev = locationDeviceService.getByDeviceMac(normalizedId);
            if (dev != null && dev.getVehicleId() != null) {
                Vehicle byDev = vehicleMapper.selectById(dev.getVehicleId());
                if (byDev != null) {
                    return byDev;
                }
            }
        }
        if (rawD != null && !rawD.isEmpty()) {
            String plate = rawD.trim();
            Vehicle byPlate = vehicleMapper.selectByLicensePlate(plate);
            if (byPlate != null) {
                return byPlate;
            }
            String upper = plate.toUpperCase(Locale.ROOT);
            if (!upper.equals(plate)) {
                byPlate = vehicleMapper.selectByLicensePlate(upper);
                if (byPlate != null) {
                    return byPlate;
                }
            }
        }
        return null;
    }

    /** 去分隔符、大写，便于与设备上报一致 */
    public static String normalizeDeviceId(String raw) {
        if (raw == null) return "";
        return raw.replace(":", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
    }

    private static double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public List<VehicleLocationVO> getAllActiveLocations() {
        List<VehicleLocationVO> out = new ArrayList<>();
        for (Map<String, Object> loc : getAllOnlineLocations()) {
            VehicleLocationVO vo = new VehicleLocationVO();
            vo.setVehicleId(toInt(loc.get("vehicleId")));
            vo.setDeviceId(asStr(loc.get("deviceId")));
            vo.setUnbound(Boolean.TRUE.equals(loc.get("unbound")));
            vo.setLicensePlate(asStr(loc.get("licensePlate")));
            vo.setLat(toDouble(loc.get("lat")));
            vo.setLng(toDouble(loc.get("lng")));
            vo.setSpeed(toDouble(loc.get("speed")));
            vo.setUpdateTime(asStr(loc.get("updateTime")));
            vo.setOwnerName(asStr(loc.get("userName")));
            vo.setFrameNum(asStr(loc.get("frameNum")));
            vo.setStatus("online");
            Integer bat = toInt(loc.get("battery"));
            vo.setBattery(bat != null ? bat : 100);
            out.add(vo);
        }
        return out;
    }

    @Override
    public VehicleLocationVO getLocation(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            return null;
        }
        String want = licensePlateKey(licensePlate);
        if (want.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> all = getAllOnlineLocations();
        for (Map<String, Object> m : all) {
            String lp = asStr(m.get("licensePlate"));
            if (!want.equals(licensePlateKey(lp))) {
                continue;
            }
            VehicleLocationVO vo = new VehicleLocationVO();
            vo.setLicensePlate(lp.isEmpty() ? licensePlate.trim() : lp);
            vo.setVehicleId(toInt(m.get("vehicleId")));
            vo.setDeviceId(asStr(m.get("deviceId")));
            vo.setUnbound(Boolean.TRUE.equals(m.get("unbound")));
            vo.setFrameNum(asStr(m.get("frameNum")));
            vo.setLat(toDouble(m.get("lat")));
            vo.setLng(toDouble(m.get("lng")));
            vo.setSpeed(toDouble(m.get("speed")));
            Integer bat = toInt(m.get("battery"));
            vo.setBattery(bat);
            vo.setUpdateTime(asStr(m.get("updateTime")));
            vo.setOwnerName(asStr(m.get("userName")));
            vo.setStatus(resolveLocationStatus(vo.getUpdateTime()));
            return vo;
        }
        return null;
    }

    private static String licensePlateKey(String plate) {
        if (plate == null) {
            return "";
        }
        return plate.replace("·", "").replace(" ", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
    }

    private String resolveLocationStatus(String updateTimeStr) {
        if (updateTimeStr == null || updateTimeStr.isBlank()) {
            return "stale";
        }
        try {
            LocalDateTime t = LocalDateTime.parse(updateTimeStr.trim());
            long sec = Duration.between(t, LocalDateTime.now()).getSeconds();
            return sec > locationStaleSeconds ? "stale" : "online";
        } catch (Exception e) {
            return "online";
        }
    }

    @Override
    public Map<String, Object> getVehicleLocation(Integer vehicleId) {
        Object cached = redisTemplate.opsForValue().get(REDIS_LOCATION_PREFIX + vehicleId);
        if (cached instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> loc = (Map<String, Object>) cached;
            return loc;
        }
        return Collections.emptyMap();
    }

    @Override
    public List<Map<String, Object>> getAllOnlineLocations() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys(REDIS_LOCATION_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return list;
            for (String key : keys) {
                Object val = redisTemplate.opsForValue().get(key);
                if (val instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> loc = (Map<String, Object>) val;
                    list.add(loc);
                }
            }
        } catch (Exception e) {
            log.error("获取在线位置失败", e);
        }
        return list;
    }

    @Override
    public int upsertSimLocations(List<SimPointRequest> points, Integer ttlMinutes) {
        if (points == null || points.isEmpty()) {
            return 0;
        }
        int ttl = ttlMinutes != null && ttlMinutes > 0 ? ttlMinutes : simTtlMinutesDefault;
        int saved = 0;
        for (int i = 0; i < points.size(); i++) {
            SimPointRequest p = points.get(i);
            if (p == null || p.getLat() == null || p.getLng() == null) {
                continue;
            }
            String plate = p.getLicensePlate() != null ? p.getLicensePlate().trim() : "";
            if (plate.isEmpty()) {
                continue;
            }
            String simKey = sanitizeSimKey(p.getId(), i, plate);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vehicleId", null);
            m.put("simulated", Boolean.TRUE);
            m.put("licensePlate", plate);
            m.put("userName", p.getOwnerName() != null ? p.getOwnerName().trim() : "");
            m.put("lat", p.getLat());
            m.put("lng", p.getLng());
            m.put("speed", p.getSpeed() != null ? p.getSpeed() : 0);
            m.put("battery", p.getBattery() != null ? p.getBattery() : 100);
            m.put("updateTime", LocalDateTime.now().toString());
            m.put("frameNum", "");
            m.put("deviceId", "sim:" + simKey);
            redisTemplate.opsForValue().set(REDIS_LOCATION_PREFIX + REDIS_SIM_SUBPREFIX + simKey, m, ttl, TimeUnit.MINUTES);
            saved++;
        }
        log.info("已更新屏幕点位 {} 条，TTL={} 分钟", saved, ttl);
        return saved;
    }

    @Override
    public void clearSimLocations() {
        Set<String> keys = redisTemplate.keys(REDIS_LOCATION_PREFIX + REDIS_SIM_SUBPREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys);
        log.info("已清除屏幕点位 {} 条", keys.size());
    }

    @Override
    public void clearVehicleLocation(Integer vehicleId) {
        if (vehicleId == null || vehicleId <= 0) {
            return;
        }
        redisTemplate.delete(REDIS_LOCATION_PREFIX + vehicleId);
        redisTemplate.delete(REDIS_STATIONARY_PREFIX + vehicleId);
        redisTemplate.delete(REDIS_PARK_VIOL_PREFIX + vehicleId);
    }

    private static String sanitizeSimKey(String id, int index, String licensePlate) {
        String base = id != null ? id.trim() : "";
        if (base.isEmpty()) {
            base = "auto-" + index + "-" + Integer.toUnsignedString(Objects.hash(licensePlate, index));
        }
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private static String asStr(Object o) {
        return o == null ? null : o.toString();
    }
}

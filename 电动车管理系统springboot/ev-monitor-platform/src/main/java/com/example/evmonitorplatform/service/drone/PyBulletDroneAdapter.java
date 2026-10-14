package com.example.evmonitorplatform.service.drone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * PyBullet 物理仿真执行器：把任务下发给本机 pybullet-drone-bridge（CtrlAviary + DSLPIDControl，
 * 真实六自由度刚体动力学），定时拉取遥测/事件并按 DroneAdapter 契约回调。
 * 业务层（PatrolServiceImpl）与 SimulatedDroneAdapter 完全无感切换。
 */
@Slf4j
@Component
public class PyBulletDroneAdapter implements DroneAdapter {
    private static final long PULL_INTERVAL_MS = 500L;

    @Value("${ev-monitor.drone.bridge-url:http://127.0.0.1:9030}")
    private String bridgeUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<Long, ScheduledFuture<?>> pullTasks = new ConcurrentHashMap<>();

    @Override
    public void start(DronePlan plan, Consumer<DroneTelemetry> telemetrySink, Consumer<DroneEvent> eventSink) {
        if (plan == null || plan.missionId() == null) throw new IllegalArgumentException("无人机任务不能为空");
        stop(plan.missionId());
        // 下发任务（经纬度航点由桥接服务转为 PyBullet NE 米制坐标）
        List<Map<String, Object>> waypoints = new ArrayList<>();
        for (DroneWaypoint waypoint : plan.waypoints()) {
            waypoints.add(Map.of(
                    "waypointId", waypoint.waypointId() == null ? 0L : waypoint.waypointId(),
                    "sequenceNo", waypoint.sequenceNo(),
                    "lng", waypoint.lng(),
                    "lat", waypoint.lat(),
                    "altitudeM", waypoint.altitudeM(),
                    "dwellSec", waypoint.dwellSec()));
        }
        Map<String, Object> payload = Map.of(
                "missionId", String.valueOf(plan.missionId()),
                "home", Map.of("lng", plan.homeLng(), "lat", plan.homeLat()),
                "waypoints", waypoints);
        post("/missions", payload);
        // 启动遥测/事件拉取
        pullTasks.put(plan.missionId(), executor.scheduleAtFixedRate(
                () -> pull(plan.missionId(), telemetrySink, eventSink),
                PULL_INTERVAL_MS, PULL_INTERVAL_MS, TimeUnit.MILLISECONDS));
        log.info("PyBullet任务已下发 missionId={}, waypoints={}", plan.missionId(), waypoints.size());
    }

    @Override
    public void injectFault(Long missionId, String faultType, Map<String, Object> parameters) {
        if (missionId == null || faultType == null || faultType.isBlank()) throw new IllegalArgumentException("故障参数不能为空");
        post("/missions/" + missionId + "/fault", Map.of("faultType", faultType, "parameters", parameters == null ? Map.of() : parameters));
    }

    @Override
    public void clearFault(Long missionId) {
        if (missionId == null) return;
        post("/missions/" + missionId + "/clear-fault", Map.of());
    }

    @Override
    public void stop(Long missionId) {
        if (missionId == null) return;
        ScheduledFuture<?> task = pullTasks.remove(missionId);
        if (task != null) task.cancel(false);
        post("/missions/" + missionId + "/stop", Map.of());
    }

    @Override
    @PreDestroy
    public void shutdown() {
        pullTasks.keySet().forEach(this::stop);
        pullTasks.clear();
        executor.shutdownNow();
    }

    private void pull(Long missionId, Consumer<DroneTelemetry> telemetrySink, Consumer<DroneEvent> eventSink) {
        try {
            String body = get("/missions/" + missionId + "/status");
            if (body == null) return;
            JsonNode root = objectMapper.readTree(body);
            JsonNode position = root.path("position");
            if (position.isMissingNode() || position.isNull()) return;
            double lng = position.path("lng").asDouble();
            double lat = position.path("lat").asDouble();
            if (lng == 0D && lat == 0D) return; // 已降落归零，不再上报
            telemetrySink.accept(new DroneTelemetry(
                    missionId,
                    position.hasNonNull("waypointId") ? position.get("waypointId").asLong() : null,
                    position.path("sequenceNo").asInt(-1),
                    lng,
                    lat,
                    position.path("altitudeM").asDouble(30D),
                    position.path("speedMps").asDouble(0D),
                    position.path("battery").asDouble(100D),
                    1D,
                    "READY",
                    position.path("flightState").asText("FLYING"),
                    false));
            for (JsonNode node : root.path("events")) {
                eventSink.accept(new DroneEvent(
                        missionId,
                        node.hasNonNull("waypointId") ? node.get("waypointId").asLong() : null,
                        node.path("sequenceNo").asInt(-1),
                        node.path("type").asText("OBSERVE"),
                        node.path("detail").asText("")));
            }
            if (root.path("completed").asBoolean(false)) {
                ScheduledFuture<?> task = pullTasks.remove(missionId);
                if (task != null) task.cancel(false);
            }
        } catch (Exception ex) {
            log.warn("PyBullet遥测拉取失败 missionId={}: {}", missionId, ex.getMessage());
        }
    }

    private void post(String path, Object payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bridgeUrl + path))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new IllegalStateException("PyBullet桥接服务不可用(" + bridgeUrl + ")：" + ex.getMessage(), ex);
        }
    }

    private String get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bridgeUrl + path))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception ex) {
            log.debug("PyBullet桥接GET失败 {}: {}", path, ex.getMessage());
            return null;
        }
    }
}

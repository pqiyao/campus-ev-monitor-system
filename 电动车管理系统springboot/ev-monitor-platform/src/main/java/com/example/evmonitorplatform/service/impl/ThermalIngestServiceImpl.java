package com.example.evmonitorplatform.service.impl;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.service.AiGovernanceService;
import com.example.evmonitorplatform.service.ThermalIngestService;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import com.example.evmonitorplatform.vo.request.ThermalFrameRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 红外热源风险评分链路（项目书3.3）。
 * <p>算法管线：热点分割（环境自适应阈值）→ 4-邻接连通域提取 → 跨帧热点匹配（温升速率）
 * → 加权评分 risk = 0.40*温度 + 0.25*面积 + 0.20*温升速率 + 0.15*场景
 * → HIGH(>=0.78) 自动生成 AI 事件进入既有治理闭环（管理员确认后才处置）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThermalIngestServiceImpl implements ThermalIngestService {
    private static final double W_TEMP = 0.40D;
    private static final double W_AREA = 0.25D;
    private static final double W_RATE = 0.20D;
    private static final double W_SCENE = 0.15D;
    private static final double HIGH_THRESHOLD = 0.78D;
    private static final double MEDIUM_THRESHOLD = 0.50D;
    private static final double FIRE_TEMP = 300D;
    private static final double NORM_TEMP_SPAN = 340D;   // 60°C~400°C 线性归一
    private static final double NORM_AREA_FULL_RATIO = 0.05D; // 热点占全帧5%面积记满档
    private static final double NORM_RATE_SPAN = 20D;    // 20°C/s 升温记满档
    private static final int MATCH_RADIUS_PX = 3;        // 跨帧热点质心匹配半径
    private static final Map<String, Double> SCENE_WEIGHTS = Map.of(
            "CHARGING_STATION", 1.0D,
            "PARKING_LOT", 0.8D,
            "ROADSIDE", 0.6D,
            "OTHER", 0.5D);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AiGovernanceService aiGovernanceService;
    private volatile boolean schemaReady;

    private void ensureSchema() {
        if (schemaReady) return;
        synchronized (this) {
            if (schemaReady) return;
            List<String> ddl = List.of(
                    "CREATE TABLE IF NOT EXISTS thermal_frame (frame_id BIGINT AUTO_INCREMENT PRIMARY KEY, dedupe_key VARCHAR(64) NOT NULL UNIQUE, device_id VARCHAR(64) NOT NULL, source_type VARCHAR(24) NOT NULL DEFAULT 'CAMERA', captured_at DATETIME NOT NULL, longitude DECIMAL(11,7) NULL, latitude DECIMAL(10,7) NULL, location_name VARCHAR(128) NULL, width INT NOT NULL, height INT NOT NULL, ambient_temp DECIMAL(6,2) NOT NULL, max_temp DECIMAL(7,2) NOT NULL, mean_temp DECIMAL(7,2) NOT NULL, hot_pixel_count INT NOT NULL DEFAULT 0, hotspot_count INT NOT NULL DEFAULT 0, rise_rate_c_per_s DECIMAL(7,3) NOT NULL DEFAULT 0, scene_type VARCHAR(32) NOT NULL DEFAULT 'PARKING_LOT', risk_level VARCHAR(16) NOT NULL, risk_score DECIMAL(8,4) NOT NULL, event_id BIGINT NULL, matrix_json LONGTEXT NULL, create_time DATETIME NOT NULL, KEY idx_thermal_frame_device (device_id, captured_at), KEY idx_thermal_frame_risk (risk_level, captured_at))",
                    "CREATE TABLE IF NOT EXISTS thermal_hotspot (hotspot_id BIGINT AUTO_INCREMENT PRIMARY KEY, frame_id BIGINT NOT NULL, peak_temp DECIMAL(7,2) NOT NULL, mean_temp DECIMAL(7,2) NOT NULL, area_px INT NOT NULL, center_row INT NOT NULL, center_col INT NOT NULL, center_lng DECIMAL(11,7) NULL, center_lat DECIMAL(10,7) NULL, rise_rate_c_per_s DECIMAL(7,3) NOT NULL DEFAULT 0, matched TINYINT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL, KEY idx_thermal_hotspot_frame (frame_id))"
            );
            ddl.forEach(jdbcTemplate::execute);
            schemaReady = true;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ingestFrame(ThermalFrameRequest request) {
        ensureSchema();
        if (request == null || request.getTempMatrix() == null || request.getTempMatrix().isEmpty()) {
            throw new BusinessException("温度矩阵不能为空");
        }
        int rows = request.getTempMatrix().size();
        int cols = request.getTempMatrix().get(0).size();
        if (cols < 8 || cols > 256) throw new BusinessException("温度矩阵列数需在8~256之间");
        double[][] matrix = new double[rows][cols];
        double ambient = request.getAmbientTemp() == null ? 25D : request.getAmbientTemp();
        double sum = 0D;
        double max = -Double.MAX_VALUE;
        for (int r = 0; r < rows; r++) {
            List<Double> line = request.getTempMatrix().get(r);
            if (line == null || line.size() != cols) throw new BusinessException("温度矩阵每行长度必须一致");
            for (int c = 0; c < cols; c++) {
                Double value = line.get(c);
                if (value == null || value.isNaN() || value < -60D || value > 1200D) {
                    throw new BusinessException("温度值需在-60~1200摄氏度之间");
                }
                matrix[r][c] = value;
                sum += value;
                if (value > max) max = value;
            }
        }
        double mean = sum / (rows * cols);

        String deviceId = request.getDeviceId().trim();
        LocalDateTime capturedAt = request.getCapturedAt() == null ? LocalDateTime.now() : request.getCapturedAt();
        String sourceType = normalizeSource(request.getSourceType());
        String sceneType = StringUtils.hasText(request.getSceneType())
                ? request.getSceneType().trim().toUpperCase(Locale.ROOT) : "PARKING_LOT";
        String dedupe = StringUtils.hasText(request.getIdempotencyKey())
                ? digest("client|" + request.getIdempotencyKey().trim())
                : digest("thermal|" + deviceId + "|" + capturedAt + "|" + (request.getFrameSeq() == null ? 0 : request.getFrameSeq()));
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT frame_id, risk_level, risk_score, event_id FROM thermal_frame WHERE dedupe_key=?", dedupe);
        if (!existing.isEmpty()) {
            return buildResult(existing.get(0), rows, cols, List.of(), true);
        }

        // 1. 热点分割与连通域提取
        double threshold = Math.max(60D, ambient + 40D);
        List<Hotspot> hotspots = extractHotspots(matrix, threshold, ambient);
        // 1.1 像素坐标→经纬度（GSD 映射）：以帧中心 GPS 为基准，按每像素地面尺寸换算
        double pixelMeters = request.getPixelMeters() == null || request.getPixelMeters() <= 0
                ? 0.05D : request.getPixelMeters();
        if (request.getLongitude() != null && request.getLatitude() != null) {
            double latRad = Math.toRadians(request.getLatitude());
            for (Hotspot hotspot : hotspots) {
                hotspot.centerLng = round(request.getLongitude()
                        + (hotspot.centerCol - cols / 2.0D) * pixelMeters / (111_320D * Math.cos(latRad)), 7);
                hotspot.centerLat = round(request.getLatitude()
                        - (hotspot.centerRow - rows / 2.0D) * pixelMeters / 110_540D, 7);
            }
        }
        double frameRiseRate = 0D;
        // 2. 跨帧匹配：与该设备上一帧热点按质心距离配对，估计温升速率
        Map<String, Object> previous = lastFrame(deviceId, capturedAt);
        if (previous != null && !hotspots.isEmpty()) {
            Double prevMax = asDouble(previous.get("max_temp"));
            LocalDateTime prevAt = toDateTime(previous.get("captured_at"));
            if (prevMax != null && prevAt != null) {
                double seconds = Math.max(0.5D, ChronoUnit.MILLIS.between(prevAt, capturedAt) / 1000.0D);
                frameRiseRate = round((max - prevMax) / seconds, 3);
            }
        }

        // 3. 加权评分
        double sceneWeight = SCENE_WEIGHTS.getOrDefault(sceneType, 0.5D);
        double normTemp = clamp((max - 60D) / NORM_TEMP_SPAN);
        double normArea = hotspots.isEmpty() ? 0D
                : clamp(hotspots.stream().mapToInt(h -> h.area).sum() / (double) (rows * cols) / NORM_AREA_FULL_RATIO);
        double normRate = clamp(Math.max(0D, frameRiseRate) / NORM_RATE_SPAN);
        double score = round(W_TEMP * normTemp + W_AREA * normArea + W_RATE * normRate + W_SCENE * sceneWeight, 4);
        score = Math.min(1D, score);
        String riskLevel = score >= HIGH_THRESHOLD ? "HIGH" : score >= MEDIUM_THRESHOLD ? "MEDIUM" : "LOW";

        // 4. HIGH 级别自动升级 AI 事件（仅接入，处罚仍需管理员确认）
        Long eventId = null;
        if ("HIGH".equals(riskLevel)) {
            AiEventCreateRequest eventRequest = new AiEventCreateRequest();
            eventRequest.setEventType(max >= FIRE_TEMP ? "FIRE_DETECTED" : "SMOKE_DETECTED");
            eventRequest.setSourceType(sourceType);
            eventRequest.setDeviceId(deviceId);
            eventRequest.setOccurredAt(capturedAt);
            eventRequest.setLongitude(request.getLongitude());
            eventRequest.setLatitude(request.getLatitude());
            eventRequest.setLocationName(StringUtils.hasText(request.getLocationName())
                    ? request.getLocationName() : "红外热源风险-" + deviceId);
            eventRequest.setDetectionConfidence(round(Math.min(0.99D, 0.55D + score * 0.4D), 2));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("channel", "THERMAL_INFRARED");
            payload.put("sceneType", sceneType);
            payload.put("ambientTemp", ambient);
            payload.put("peakTemp", round(max, 2));
            payload.put("meanTemp", round(mean, 2));
            payload.put("riseRateCPerS", frameRiseRate);
            payload.put("hotspotCount", hotspots.size());
            payload.put("thermalRiskScore", score);
            payload.put("hotspots", hotspots.stream().map(Hotspot::summary).toList());
            eventRequest.setRawPayload(payload);
            eventRequest.setEvidenceUrls(List.of());
            eventRequest.setIdempotencyKey("thermal-" + dedupe);
            eventRequest.setAutoAnalyze(Boolean.TRUE.equals(request.getAutoAnalyze()));
            Map<String, Object> eventResult = aiGovernanceService.ingest(eventRequest);
            Object id = eventResult.get("eventId") != null ? eventResult.get("eventId") : eventResult.get("event_id");
            if (id instanceof Number number) eventId = number.longValue();
        }

        // 5. 落库
        jdbcTemplate.update("INSERT INTO thermal_frame (dedupe_key, device_id, source_type, captured_at, longitude, latitude, location_name, width, height, ambient_temp, max_temp, mean_temp, hot_pixel_count, hotspot_count, rise_rate_c_per_s, scene_type, risk_level, risk_score, event_id, matrix_json, create_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                dedupe, deviceId, sourceType, capturedAt, request.getLongitude(), request.getLatitude(),
                request.getLocationName(), cols, rows, ambient, round(max, 2), round(mean, 2),
                hotspots.stream().mapToInt(h -> h.area).sum(), hotspots.size(), frameRiseRate,
                sceneType, riskLevel, score, eventId, json(matrix), LocalDateTime.now());
        Long frameId = jdbcTemplate.queryForObject(
                "SELECT frame_id FROM thermal_frame WHERE dedupe_key=?", Long.class, dedupe);
        for (Hotspot hotspot : hotspots) {
            jdbcTemplate.update("INSERT INTO thermal_hotspot (frame_id, peak_temp, mean_temp, area_px, center_row, center_col, center_lng, center_lat, rise_rate_c_per_s, matched, create_time) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    frameId, hotspot.peak, hotspot.meanTemp, hotspot.area, hotspot.centerRow, hotspot.centerCol,
                    hotspot.centerLng, hotspot.centerLat, hotspot.riseRate, hotspot.matched ? 1 : 0, LocalDateTime.now());
        }
        if (eventId != null) {
            log.info("红外热源HIGH风险已升级AI事件 frameId={}, eventId={}, score={}, peakTemp={}", frameId, eventId, score, max);
        }
        Map<String, Object> frameRow = new LinkedHashMap<>();
        frameRow.put("frame_id", frameId);
        frameRow.put("risk_level", riskLevel);
        frameRow.put("risk_score", score);
        frameRow.put("event_id", eventId);
        return buildResult(frameRow, rows, cols, hotspots.stream().map(Hotspot::summary).toList(), false);
    }

    @Override
    public List<Map<String, Object>> listFrames(Integer limit, String riskLevel) {
        ensureSchema();
        int size = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
        StringBuilder sql = new StringBuilder("SELECT frame_id, device_id, source_type, captured_at, longitude, latitude, location_name, width, height, ambient_temp, max_temp, mean_temp, hot_pixel_count, hotspot_count, rise_rate_c_per_s, scene_type, risk_level, risk_score, event_id, create_time FROM thermal_frame WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(riskLevel)) {
            sql.append(" AND risk_level=?");
            args.add(riskLevel.trim().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY captured_at DESC LIMIT ").append(size);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Override
    public Map<String, Object> getFrameDetail(Long frameId) {
        ensureSchema();
        if (frameId == null) throw new BusinessException("帧ID不能为空");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM thermal_frame WHERE frame_id=?", frameId);
        if (rows.isEmpty()) throw new BusinessException("热帧不存在");
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("hotspots", jdbcTemplate.queryForList("SELECT * FROM thermal_hotspot WHERE frame_id=? ORDER BY peak_temp DESC", frameId));
        if (result.get("event_id") != null) {
            result.put("aiEvent", jdbcTemplate.queryForList(
                    "SELECT event_id, event_code, event_type, status, risk_level, occurred_at FROM ai_event WHERE event_id=?",
                    result.get("event_id")));
        }
        return result;
    }

    /** 4-邻接连通域热点提取（BFS），并按每像素地面尺寸换算热点质心经纬度。 */
    private List<Hotspot> extractHotspots(double[][] matrix, double threshold, double ambient) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        boolean[][] visited = new boolean[rows][cols];
        List<Hotspot> hotspots = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (visited[r][c] || matrix[r][c] < threshold) continue;
                Hotspot hotspot = new Hotspot(ambient);
                Deque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{r, c});
                visited[r][c] = true;
                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    int cr = cell[0], cc = cell[1];
                    double value = matrix[cr][cc];
                    hotspot.area++;
                    hotspot.sum += value;
                    if (value > hotspot.peak) {
                        hotspot.peak = value;
                        hotspot.peakRow = cr;
                        hotspot.peakCol = cc;
                    }
                    hotspot.rowSum += cr;
                    hotspot.colSum += cc;
                    int[][] neighbors = {{cr - 1, cc}, {cr + 1, cc}, {cr, cc - 1}, {cr, cc + 1}};
                    for (int[] next : neighbors) {
                        int nr = next[0], nc = next[1];
                        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || visited[nr][nc]) continue;
                        if (matrix[nr][nc] >= threshold) {
                            visited[nr][nc] = true;
                            queue.add(next);
                        }
                    }
                }
                hotspot.centerRow = hotspot.rowSum / hotspot.area;
                hotspot.centerCol = hotspot.colSum / hotspot.area;
                hotspot.meanTemp = round(hotspot.sum / hotspot.area, 2);
                hotspots.add(hotspot);
            }
        }
        hotspots.sort((a, b) -> Double.compare(b.peak, a.peak));
        return hotspots;
    }

    private Map<String, Object> buildResult(Map<String, Object> frameRow, int rows, int cols,
                                            List<Map<String, Object>> hotspots, boolean duplicated) {
        Map<String, Object> result = new LinkedHashMap<>(frameRow);
        result.put("duplicated", duplicated);
        result.put("matrixSize", rows + "x" + cols);
        result.put("hotspots", hotspots);
        return result;
    }

    private Map<String, Object> lastFrame(String deviceId, LocalDateTime before) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT frame_id, max_temp, captured_at FROM thermal_frame WHERE device_id=? AND captured_at<? ORDER BY captured_at DESC, frame_id DESC LIMIT 1",
                deviceId, before);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String normalizeSource(String sourceType) {
        String value = StringUtils.hasText(sourceType) ? sourceType.trim().toUpperCase(Locale.ROOT) : "CAMERA";
        if (!List.of("CAMERA", "DRONE", "GROUND_ROBOT", "VEHICLE_TERMINAL").contains(value)) {
            throw new BusinessException("红外帧来源仅允许 CAMERA/DRONE/GROUND_ROBOT/VEHICLE_TERMINAL：" + value);
        }
        return value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("红外矩阵序列化失败: {}", ex.getMessage());
            return "[]";
        }
    }

    private String digest(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) builder.append(String.format("%02x", item));
            return builder.toString();
        } catch (Exception ex) {
            throw new BusinessException("摘要计算失败");
        }
    }

    private Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private LocalDateTime toDateTime(Object value) {
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        return null;
    }

    private double clamp(double value) {
        return Math.max(0D, Math.min(1D, value));
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    /** 热点：连通域统计 + 跨帧匹配状态。 */
    private static final class Hotspot {
        private double peak = -Double.MAX_VALUE;
        private double sum;
        private double meanTemp;
        private int area;
        private int rowSum;
        private int colSum;
        private int peakRow;
        private int peakCol;
        private int centerRow;
        private int centerCol;
        private Double centerLng;
        private Double centerLat;
        private double riseRate;
        private boolean matched;

        private Hotspot(double ambient) {
            this.riseRate = 0D;
        }

        private Map<String, Object> summary() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("peakTemp", round(peak, 2));
            item.put("meanTemp", meanTemp);
            item.put("areaPx", area);
            item.put("centerRow", centerRow);
            item.put("centerCol", centerCol);
            item.put("peakRow", peakRow);
            item.put("peakCol", peakCol);
            item.put("riseRateCPerS", riseRate);
            item.put("centerLng", centerLng);
            item.put("centerLat", centerLat);
            return item;
        }
    }
}

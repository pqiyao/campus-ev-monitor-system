package com.example.evmonitorplatform.service.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fields2Cover 全覆盖航线规划客户端。
 *
 * <p>把巡检地块多边形（可含禁飞区镂空）交给本机 Fields2Cover 桥接服务
 * （tools/f2c-bridge），由 C++ 覆盖路径规划库真实计算「头地→牛耕式条带→
 * 蛇形排序→Dubins 平滑」的全覆盖航线。桥接不可用时由上层回退到风险网格贪心航线，
 * 因此这里不做重试，只做一次快速调用并抛出异常。</p>
 */
@Slf4j
@Component
public class CoveragePlannerClient {

    @Value("${ev-monitor.coverage.bridge-url:http://127.0.0.1:9031}")
    private String bridgeUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public record GeoPoint(double lng, double lat) {
    }

    public record CoveragePlan(List<GeoPoint> waypoints, int swathCount, double areaM2,
                               double pathLengthM, String engine, String version) {
    }

    /**
     * 计算全覆盖航线。
     *
     * @param polygon       地块外边界（至少 3 个顶点，经纬度）
     * @param holes         镂空区域（禁飞区等），无则传空列表
     * @param covWidth      覆盖带宽（米，等效相机/传感器幅宽）
     * @param minRadius     最小转弯半径（米，直升机可设小值）
     * @param sampleSpacing 输出航点抽稀间距（米）
     */
    public CoveragePlan plan(List<GeoPoint> polygon, List<List<GeoPoint>> holes,
                             double covWidth, double minRadius, double sampleSpacing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("polygon", toPairs(polygon));
        if (holes != null && !holes.isEmpty()) {
            List<List<List<Double>>> holePairs = new ArrayList<>();
            for (List<GeoPoint> hole : holes) {
                if (hole.size() >= 3) holePairs.add(toPairs(hole));
            }
            if (!holePairs.isEmpty()) payload.put("holes", holePairs);
        }
        payload.put("covWidth", covWidth);
        payload.put("minRadius", minRadius);
        payload.put("sampleSpacingM", sampleSpacing);

        JsonNode root = post("/coverage", payload);
        List<GeoPoint> waypoints = new ArrayList<>();
        for (JsonNode node : root.path("waypoints")) {
            waypoints.add(new GeoPoint(node.path("lng").asDouble(), node.path("lat").asDouble()));
        }
        if (waypoints.size() < 2) {
            throw new IllegalStateException("Fields2Cover 未返回有效覆盖航点");
        }
        return new CoveragePlan(
                waypoints,
                root.path("swathCount").asInt(0),
                root.path("areaM2").asDouble(0D),
                root.path("pathLengthM").asDouble(0D),
                root.path("engine").asText("fields2cover"),
                root.path("version").asText(""));
    }

    private List<List<Double>> toPairs(List<GeoPoint> points) {
        List<List<Double>> pairs = new ArrayList<>();
        for (GeoPoint point : points) pairs.add(List.of(point.lng(), point.lat()));
        return pairs;
    }

    private JsonNode post(String path, Object payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bridgeUrl + path))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("覆盖规划桥接返回 " + response.statusCode() + "：" + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (Exception ex) {
            throw new IllegalStateException("Fields2Cover 桥接服务不可用(" + bridgeUrl + ")：" + ex.getMessage(), ex);
        }
    }
}

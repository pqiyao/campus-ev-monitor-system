package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.service.LocationService;
import com.example.evmonitorplatform.vo.request.SimLocationBatchRequest;
import com.example.evmonitorplatform.vo.response.VehicleLocationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Tag(name = "实时位置", description = "车辆GPS位置接口")
public class LocationController {

    private final LocationService locationService;
    private final ObjectMapper    objectMapper;

    @GetMapping("/all")
    public ApiResponse<List<VehicleLocationVO>> getAllLocations() {
        return ApiResponse.success(locationService.getAllActiveLocations());
    }

    @GetMapping("/{licensePlate}")
    public ApiResponse<VehicleLocationVO> getLocation(@PathVariable String licensePlate) {
        return ApiResponse.success(locationService.getLocation(licensePlate));
    }

    /**
     * 终端位置上报：body 示例 {"N":26.88,"W":112.68,"D":"AABBCCDDEEFF"}，可选 topic 覆盖默认 ev/location
     */
    @PostMapping("/mock")
    public ApiResponse<Boolean> mockLocation(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        String topic = body.get("topic") != null ? body.get("topic").toString() : "ev/location";
        // 兼容控制台测试脚本：lat/lng/licensePlate → N/W/D
        if (body.get("N") == null && body.get("lat") != null) {
            body.put("N", body.get("lat"));
        }
        if (body.get("W") == null && body.get("lng") != null) {
            body.put("W", body.get("lng"));
        }
        if (body.get("D") == null && body.get("licensePlate") != null) {
            body.put("D", body.get("licensePlate").toString().trim());
        }
        String payload = objectMapper.writeValueAsString(body);
        locationService.handleLocationMessage(topic, payload);
        return ApiResponse.success(true);
    }

    /**
     * 地图点位：批量写入虚拟车辆点（不查库、不触发围栏/违停），与真实 MQTT 点位一起在 GET /all 中返回。
     */
    @PostMapping("/sim/batch")
    public ApiResponse<Integer> simBatch(@RequestBody SimLocationBatchRequest body) {
        if (body == null || body.getPoints() == null || body.getPoints().isEmpty()) {
            return ApiResponse.success(0);
        }
        int n = locationService.upsertSimLocations(body.getPoints(), body.getTtlMinutes());
        return ApiResponse.success(n);
    }

    @DeleteMapping("/sim")
    public ApiResponse<Void> clearSim() {
        locationService.clearSimLocations();
        return ApiResponse.success();
    }

    @DeleteMapping("/vehicle/{vehicleId}")
    public ApiResponse<Void> clearVehicleLocation(@PathVariable Integer vehicleId) {
        locationService.clearVehicleLocation(vehicleId);
        return ApiResponse.success();
    }

    @GetMapping("/parking-config")
    public ApiResponse<Map<String, Object>> getParkingConfig() {
        return ApiResponse.success(locationService.getParkingRuntimeConfig());
    }

    @PutMapping("/parking-config")
    public ApiResponse<Map<String, Object>> updateParkingConfig(@RequestBody Map<String, Object> body) {
        Integer stationaryMinutes = toInt(body.get("stationaryMinutes"));
        Double moveThresholdMeters = toDouble(body.get("moveThresholdMeters"));
        return ApiResponse.success(locationService.updateParkingRuntimeConfig(stationaryMinutes, moveThresholdMeters));
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return null; }
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        try { return Double.parseDouble(value.toString()); } catch (Exception e) { return null; }
    }
}

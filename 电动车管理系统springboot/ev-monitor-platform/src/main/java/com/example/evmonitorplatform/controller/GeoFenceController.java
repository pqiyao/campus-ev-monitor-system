package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.entity.GeoFence;
import com.example.evmonitorplatform.entity.GeoFenceAlert;
import com.example.evmonitorplatform.service.GeoFenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/geofence")
@RequiredArgsConstructor
public class GeoFenceController {

    private final GeoFenceService geoFenceService;

    @GetMapping("/enabled")
    public ApiResponse<List<GeoFence>> getEnabled() {
        return ApiResponse.success(geoFenceService.getAllEnabled());
    }

    @GetMapping("/list")
    public ApiResponse<List<GeoFence>> getAll() {
        return ApiResponse.success(geoFenceService.getAll());
    }

    @PostMapping("/create")
    public ApiResponse<GeoFence> create(@RequestBody GeoFence fence) {
        return ApiResponse.success(geoFenceService.create(fence));
    }

    @PutMapping("/update")
    public ApiResponse<Boolean> update(@RequestBody GeoFence fence) {
        return ApiResponse.success(geoFenceService.update(fence));
    }

    @DeleteMapping("/{fenceId}")
    public ApiResponse<Boolean> delete(@PathVariable Integer fenceId) {
        return ApiResponse.success(geoFenceService.delete(fenceId));
    }

    @PutMapping("/{fenceId}/toggle")
    public ApiResponse<Boolean> toggle(@PathVariable Integer fenceId) {
        return ApiResponse.success(geoFenceService.toggleStatus(fenceId));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<GeoFenceAlert>> getAlerts(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(geoFenceService.getRecentAlerts(limit));
    }

    @PutMapping("/alerts/{alertId}/process")
    public ApiResponse<Boolean> processAlert(@PathVariable Integer alertId) {
        return ApiResponse.success(geoFenceService.processAlert(alertId));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary() {
        return ApiResponse.success(geoFenceService.getSummary());
    }
}
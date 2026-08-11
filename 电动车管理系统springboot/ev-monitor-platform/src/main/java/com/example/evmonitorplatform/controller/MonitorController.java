package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.service.MonitorService;
import com.example.evmonitorplatform.vo.response.AlertVO;
import com.example.evmonitorplatform.vo.response.CameraVO;
import com.example.evmonitorplatform.vo.response.DashboardVO;
import com.example.evmonitorplatform.vo.response.WeatherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "监控平台", description = "大屏监控相关接口")
public class MonitorController {
    
    private final MonitorService monitorService;
    
    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘数据", description = "获取监控大屏所需的所有数据")
    public ApiResponse<DashboardVO> getDashboardData() {
        DashboardVO dashboard = monitorService.getDashboardData();
        return ApiResponse.success(dashboard);
    }
    
    @GetMapping("/alerts")
    @Operation(summary = "获取报警信息", description = "获取最新的报警信息列表")
    public ApiResponse<List<AlertVO>> getAlerts(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<AlertVO> alerts = monitorService.getRecentAlerts(limit);
        return ApiResponse.success(alerts);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "获取案件统计", description = "获取案件统计数据")
    public ApiResponse<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = monitorService.getCaseStatistics();
        return ApiResponse.success(stats);
    }
    
    @GetMapping("/cameras")
    @Operation(summary = "获取摄像头状态", description = "获取所有摄像头状态")
    public ApiResponse<List<CameraVO>> getCameras() {
        List<CameraVO> cameras = monitorService.getAllCameras();
        return ApiResponse.success(cameras);
    }
    
    @GetMapping("/weather")
    @Operation(summary = "获取天气信息", description = "获取当前位置的天气信息")
    public ApiResponse<WeatherVO> getWeather() {
        WeatherVO weather = monitorService.getWeather();
        return ApiResponse.success(weather);
    }
    
    @GetMapping("/map-data")
    @Operation(summary = "获取地图数据", description = "获取地图标记和路径数据")
    public ApiResponse<Map<String, Object>> getMapData() {
        Map<String, Object> mapData = monitorService.getMapData();
        return ApiResponse.success(mapData);
    }
    
    @GetMapping("/realtime")
    @Operation(summary = "获取实时数据", description = "获取实时监控数据")
    public ApiResponse<Map<String, Object>> getRealtimeData() {
        return ApiResponse.success(monitorService.getRealtimeData());
    }
}
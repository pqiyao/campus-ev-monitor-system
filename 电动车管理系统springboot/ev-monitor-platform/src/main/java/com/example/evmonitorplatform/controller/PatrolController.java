package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.service.PatrolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patrol")
@RequiredArgsConstructor
public class PatrolController {
    private final PatrolService patrolService;

    @PostMapping("/missions")
    public ApiResponse<Map<String,Object>> create(@RequestBody Map<String,Object> request, @RequestParam(required=false) Integer operatorUserId) { return ApiResponse.success(patrolService.createMission(request, operatorUserId)); }
    @GetMapping("/missions")
    public ApiResponse<?> list(@RequestParam(required=false) String state, @RequestParam(defaultValue="20") Integer limit) { return ApiResponse.success(patrolService.listMissions(state, limit)); }
    @GetMapping("/missions/{missionId}")
    public ApiResponse<Map<String,Object>> detail(@PathVariable Long missionId) { return ApiResponse.success(patrolService.getMission(missionId)); }
    @PostMapping("/missions/{missionId}/plan")
    public ApiResponse<Map<String,Object>> plan(@PathVariable Long missionId, @RequestParam(required=false) Integer operatorUserId) { return ApiResponse.success(patrolService.planMission(missionId, operatorUserId)); }
    @PostMapping("/missions/{missionId}/start")
    public ApiResponse<Map<String,Object>> start(@PathVariable Long missionId, @RequestParam(required=false) Integer operatorUserId) { return ApiResponse.success(patrolService.startMission(missionId, operatorUserId)); }
    @PostMapping("/missions/{missionId}/pause")
    public ApiResponse<Map<String,Object>> pause(@PathVariable Long missionId, @RequestParam(required=false) Integer operatorUserId) { return ApiResponse.success(patrolService.pauseMission(missionId, operatorUserId)); }
    @PostMapping("/missions/{missionId}/replan")
    public ApiResponse<Map<String,Object>> replan(@PathVariable Long missionId, @RequestBody(required=false) Map<String,Object> body, @RequestParam(required=false) Integer operatorUserId) { return ApiResponse.success(patrolService.replanMission(missionId, body == null ? null : String.valueOf(body.get("reason")), operatorUserId)); }
    @PostMapping("/missions/{missionId}/telemetry")
    public ApiResponse<Map<String,Object>> telemetry(@PathVariable Long missionId, @RequestBody Map<String,Object> body) { return ApiResponse.success(patrolService.appendTelemetry(missionId, body)); }
    @PostMapping("/missions/{missionId}/report")
    public ApiResponse<Map<String,Object>> report(@PathVariable Long missionId, @RequestParam(required=false) Integer operatorUserId) { return ApiResponse.success(patrolService.generateReport(missionId, operatorUserId)); }
}

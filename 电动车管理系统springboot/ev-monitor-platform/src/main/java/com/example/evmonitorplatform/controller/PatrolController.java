package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.PatrolService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patrol")
@RequiredArgsConstructor
public class PatrolController {
    private final PatrolService patrolService;
    private final UserMapper userMapper;
    private final AiGovernanceProperties aiProperties;

    @PostMapping("/missions")
    public ApiResponse<Map<String,Object>> create(@RequestBody Map<String,Object> request, Authentication authentication) { return ApiResponse.success(patrolService.createMission(request, requireOperator(authentication))); }
    @GetMapping("/missions")
    public ApiResponse<?> list(@RequestParam(required=false) String state, @RequestParam(defaultValue="20") Integer limit) { return ApiResponse.success(patrolService.listMissions(state, limit)); }
    @GetMapping("/missions/{missionId}")
    public ApiResponse<Map<String,Object>> detail(@PathVariable Long missionId) { return ApiResponse.success(patrolService.getMission(missionId)); }
    @PostMapping("/missions/{missionId}/plan")
    public ApiResponse<Map<String,Object>> plan(@PathVariable Long missionId, Authentication authentication) { return ApiResponse.success(patrolService.planMission(missionId, requireOperator(authentication))); }
    @PostMapping("/missions/{missionId}/start")
    public ApiResponse<Map<String,Object>> start(@PathVariable Long missionId, Authentication authentication) { return ApiResponse.success(patrolService.startMission(missionId, requireOperator(authentication))); }
    @PostMapping("/missions/{missionId}/pause")
    public ApiResponse<Map<String,Object>> pause(@PathVariable Long missionId, Authentication authentication) { return ApiResponse.success(patrolService.pauseMission(missionId, requireOperator(authentication))); }
    @PostMapping("/missions/{missionId}/replan")
    public ApiResponse<Map<String,Object>> replan(@PathVariable Long missionId, @RequestBody(required=false) Map<String,Object> body, Authentication authentication) { return ApiResponse.success(patrolService.replanMission(missionId, body == null ? null : String.valueOf(body.get("reason")), requireOperator(authentication))); }
    @PostMapping("/missions/{missionId}/telemetry")
    public ApiResponse<Map<String,Object>> telemetry(@PathVariable Long missionId, @RequestBody Map<String,Object> body, Authentication authentication, @RequestHeader(value="X-AI-Ingest-Key", required=false) String ingestKey) { requireDeviceOrOperator(authentication, ingestKey); return ApiResponse.success(patrolService.appendTelemetry(missionId, body)); }
    @PostMapping("/missions/{missionId}/faults")
    @SuppressWarnings("unchecked")
    public ApiResponse<Map<String,Object>> fault(@PathVariable Long missionId, @RequestBody(required=false) Map<String,Object> body, Authentication authentication) {
        Map<String,Object> request = body == null ? Map.of() : body;
        String type = request.get("faultType") == null ? "" : String.valueOf(request.get("faultType"));
        Object rawParameters = request.get("parameters");
        Map<String,Object> parameters = rawParameters instanceof Map<?,?> map ? (Map<String,Object>) map : request;
        return ApiResponse.success(patrolService.injectFault(missionId, type, parameters, requireOperator(authentication)));
    }
    @DeleteMapping("/missions/{missionId}/faults")
    public ApiResponse<Map<String,Object>> clearFault(@PathVariable Long missionId, Authentication authentication) {
        return ApiResponse.success(patrolService.clearFault(missionId, requireOperator(authentication)));
    }
    @PostMapping("/missions/{missionId}/events")
    public ApiResponse<Map<String,Object>> event(@PathVariable Long missionId, @RequestBody(required=false) Map<String,Object> body, Authentication authentication) { return ApiResponse.success(patrolService.attachEvent(missionId, body == null ? Map.of() : body, requireOperator(authentication))); }
    @PostMapping("/missions/{missionId}/report")
    public ApiResponse<Map<String,Object>> report(@PathVariable Long missionId, Authentication authentication) { return ApiResponse.success(patrolService.generateReport(missionId, requireOperator(authentication))); }

    @GetMapping("/missions/{missionId}/report")
    public ApiResponse<Map<String,Object>> reportDetail(@PathVariable Long missionId, Authentication authentication) { requireOperator(authentication); return ApiResponse.success(patrolService.getReport(missionId)); }

    private Integer requireOperator(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("请先登录");
        }
        User user = userMapper.selectByUsername(authentication.getName());
        if (user == null || !("admin".equalsIgnoreCase(user.getUserType())
                || "service".equalsIgnoreCase(user.getUserType()))) {
            throw new BusinessException("仅管理员或后勤巡检员可以控制巡检任务");
        }
        return user.getUserId();
    }

    private void requireDeviceOrOperator(Authentication authentication, String ingestKey) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userMapper.selectByUsername(authentication.getName());
            if (user != null && ("admin".equalsIgnoreCase(user.getUserType())
                    || "service".equalsIgnoreCase(user.getUserType()))) return;
        }
        if (org.springframework.util.StringUtils.hasText(aiProperties.getIngestKey())
                && aiProperties.getIngestKey().equals(ingestKey)) return;
        throw new BusinessException("设备接入凭证无效");
    }
}

package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.AiGovernanceService;
import com.example.evmonitorplatform.vo.request.AiDemoRunRequest;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import com.example.evmonitorplatform.vo.request.AiIncidentReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiGovernanceController {
    private final AiGovernanceService governanceService;
    private final AiGovernanceProperties properties;
    private final UserMapper userMapper;

    @PostMapping("/demo/run")
    public ApiResponse<Map<String, Object>> runDemo(@RequestBody(required = false) AiDemoRunRequest request) { return ApiResponse.success(governanceService.runDemo(request)); }

    @PostMapping("/events")
    public ApiResponse<Map<String, Object>> ingest(@Valid @RequestBody AiEventCreateRequest request,
                                                    Authentication authentication,
                                                    @RequestHeader(value = "X-AI-Ingest-Key", required = false) String ingestKey) {
        requireAdminOrIngestKey(authentication, ingestKey);
        return ApiResponse.success(governanceService.ingest(request));
    }

    @GetMapping("/events")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String eventType, @RequestParam(defaultValue = "50") Integer limit) { return ApiResponse.success(governanceService.listEvents(status, eventType, limit)); }

    @GetMapping("/events/{eventId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long eventId) { return ApiResponse.success(governanceService.getEventDetail(eventId)); }

    @PostMapping("/events/{eventId}/analyze")
    public ApiResponse<Map<String, Object>> analyze(@PathVariable Long eventId) { return ApiResponse.success(governanceService.analyze(eventId)); }

    @PostMapping("/incidents/{incidentId}/confirm")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long incidentId, @Valid @RequestBody AiIncidentReviewRequest request, Authentication authentication) {
        return ApiResponse.success(governanceService.confirm(incidentId, requireAdmin(authentication), request));
    }

    @PostMapping("/incidents/{incidentId}/reject")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long incidentId, @Valid @RequestBody AiIncidentReviewRequest request, Authentication authentication) {
        return ApiResponse.success(governanceService.reject(incidentId, requireAdmin(authentication), request));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() { return ApiResponse.success(governanceService.stats()); }

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() { return ApiResponse.success(Map.of("mode", properties.getMode(), "model", properties.getModel(), "apiStyle", properties.getApiStyle(), "baseUrl", properties.getBaseUrl(), "apiKeyConfigured", org.springframework.util.StringUtils.hasText(properties.getApiKey()))); }

    private Integer requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("请先登录");
        }
        User user = userMapper.selectByUsername(authentication.getName());
        if (user == null || !"admin".equalsIgnoreCase(user.getUserType())) {
            throw new BusinessException("仅管理员可审核AI治理事件");
        }
        return user.getUserId();
    }

    private void requireAdminOrIngestKey(Authentication authentication, String ingestKey) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userMapper.selectByUsername(authentication.getName());
            if (user != null && "admin".equalsIgnoreCase(user.getUserType())) return;
        }
        if (org.springframework.util.StringUtils.hasText(properties.getIngestKey())
                && properties.getIngestKey().equals(ingestKey)) return;
        throw new BusinessException("设备接入凭证无效");
    }
}

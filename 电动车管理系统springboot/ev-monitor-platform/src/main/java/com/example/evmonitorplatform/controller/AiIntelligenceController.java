package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.AiIntelligenceService;
import com.example.evmonitorplatform.vo.request.AiAssistantRequest;
import com.example.evmonitorplatform.vo.request.AiReportRequest;
import com.example.evmonitorplatform.vo.request.PolicyDocumentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/intelligence")
@RequiredArgsConstructor
public class AiIntelligenceController {
    private final AiIntelligenceService intelligenceService;
    private final UserMapper userMapper;

    @PostMapping("/assistant/chat")
    public ApiResponse<Map<String, Object>> chat(@Valid @RequestBody AiAssistantRequest request,
                                                  Authentication authentication) {
        return ApiResponse.success(intelligenceService.chat(request, requireAdmin(authentication)));
    }

    @GetMapping("/assistant/sessions/{sessionId}/messages")
    public ApiResponse<List<Map<String, Object>>> messages(@PathVariable String sessionId,
                                                            Authentication authentication) {
        return ApiResponse.success(intelligenceService.getSessionMessages(sessionId, requireAdmin(authentication)));
    }

    @GetMapping("/policies")
    public ApiResponse<List<Map<String, Object>>> policies(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(intelligenceService.listPolicies(keyword));
    }

    @PostMapping("/policies")
    public ApiResponse<Map<String, Object>> createPolicy(@Valid @RequestBody PolicyDocumentRequest request,
                                                          Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.success(intelligenceService.createPolicy(request));
    }

    @PostMapping("/events/{eventId}/correlate")
    public ApiResponse<List<Map<String, Object>>> correlate(@PathVariable Long eventId) {
        return ApiResponse.success(intelligenceService.correlateEvent(eventId));
    }

    @GetMapping("/correlations")
    public ApiResponse<List<Map<String, Object>>> correlations(@RequestParam(defaultValue = "30") Integer limit) {
        return ApiResponse.success(intelligenceService.listCorrelations(limit));
    }

    @PostMapping("/appeals/{appealId}/review")
    public ApiResponse<Map<String, Object>> reviewAppeal(@PathVariable Integer appealId,
                                                         Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.success(intelligenceService.reviewAppeal(appealId));
    }

    @PostMapping("/reports")
    public ApiResponse<Map<String, Object>> generateReport(@RequestBody(required = false) AiReportRequest request,
                                                            Authentication authentication) {
        return ApiResponse.success(intelligenceService.generateReport(
                request == null ? new AiReportRequest() : request, requireAdmin(authentication)));
    }

    @GetMapping("/reports")
    public ApiResponse<List<Map<String, Object>>> reports(@RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.success(intelligenceService.listReports(limit));
    }

    private Integer requireAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new BusinessException("请先登录");
        User user = userMapper.selectByUsername(authentication.getName());
        if (user == null || !"admin".equalsIgnoreCase(user.getUserType())) {
            throw new BusinessException("仅管理员可使用AI治理分析助手");
        }
        return user.getUserId();
    }
}

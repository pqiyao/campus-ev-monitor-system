package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.ThermalIngestService;
import com.example.evmonitorplatform.vo.request.ThermalFrameRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 红外热源风险接入端点：机载红外相机/地面机器人/枪机复用 X-AI-Ingest-Key 设备凭证。
 */
@RestController
@RequestMapping("/api/thermal")
@RequiredArgsConstructor
public class ThermalIngestController {
    private final ThermalIngestService thermalIngestService;
    private final UserMapper userMapper;
    private final AiGovernanceProperties properties;

    @PostMapping("/ingest")
    public ApiResponse<Map<String, Object>> ingest(@Valid @RequestBody ThermalFrameRequest request,
                                                   Authentication authentication,
                                                   @RequestHeader(value = "X-AI-Ingest-Key", required = false) String ingestKey) {
        requireAdminOrIngestKey(authentication, ingestKey);
        return ApiResponse.success(thermalIngestService.ingestFrame(request));
    }

    @GetMapping("/frames")
    public ApiResponse<List<Map<String, Object>>> frames(@RequestParam(defaultValue = "50") Integer limit,
                                                         @RequestParam(required = false) String riskLevel) {
        return ApiResponse.success(thermalIngestService.listFrames(limit, riskLevel));
    }

    @GetMapping("/frames/{frameId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long frameId) {
        return ApiResponse.success(thermalIngestService.getFrameDetail(frameId));
    }

    private void requireAdminOrIngestKey(Authentication authentication, String ingestKey) {
        if (authentication != null && authentication.isAuthenticated()) return;
        if (org.springframework.util.StringUtils.hasText(properties.getIngestKey())
                && properties.getIngestKey().equals(ingestKey)) return;
        throw new BusinessException("设备接入凭证无效");
    }
}

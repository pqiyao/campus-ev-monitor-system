package com.example.evmonitorplatform.controller;

import com.example.evmonitorplatform.common.response.ApiResponse;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.CameraMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.AiGovernanceService;
import com.example.evmonitorplatform.vo.request.AiDemoRunRequest;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import com.example.evmonitorplatform.vo.request.AiIncidentReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiGovernanceController {
    private final AiGovernanceService governanceService;
    private final AiGovernanceProperties properties;
    private final UserMapper userMapper;
    private final CameraMapper cameraMapper;

    @Value("${ev-monitor.file.upload-path}")
    private String uploadPath;

    @PostMapping("/demo/run")
    public ApiResponse<Map<String, Object>> runDemo(@RequestBody(required = false) AiDemoRunRequest request) { return ApiResponse.success(governanceService.runDemo(request)); }

    @PostMapping("/events")
    public ApiResponse<Map<String, Object>> ingest(@Valid @RequestBody AiEventCreateRequest request,
                                                    Authentication authentication,
                                                    @RequestHeader(value = "X-AI-Ingest-Key", required = false) String ingestKey) {
        requireAdminOrIngestKey(authentication, ingestKey);
        return ApiResponse.success(governanceService.ingest(request));
    }

    @PostMapping("/evidence/upload")
    public ApiResponse<Map<String, String>> uploadEvidence(@RequestParam("file") MultipartFile file,
                                                            Authentication authentication,
                                                            @RequestHeader(value = "X-AI-Ingest-Key", required = false) String ingestKey) {
        requireAdminOrIngestKey(authentication, ingestKey);
        if (file == null || file.isEmpty()) throw new BusinessException("证据图片不能为空");
        String contentType = file.getContentType();
        if (contentType == null || !List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new BusinessException("证据只允许 JPG、PNG 或 WEBP 图片");
        }
        if (file.getSize() > 10L * 1024 * 1024) throw new BusinessException("证据图片不能超过10MB");
        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path directory = root.resolve(Paths.get("ai-evidence", datePath)).normalize();
        if (!directory.startsWith(root)) throw new BusinessException("证据保存路径无效");
        try {
            Files.createDirectories(directory);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = directory.resolve(filename).normalize();
            try (java.io.InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            return ApiResponse.success(Map.of("fileUrl", "/uploads/ai-evidence/" + datePath + "/" + filename));
        } catch (IOException ex) {
            throw new BusinessException("证据图片保存失败：" + ex.getMessage());
        }
    }

    @GetMapping("/events")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String eventType, @RequestParam(defaultValue = "50") Integer limit) { return ApiResponse.success(governanceService.listEvents(status, eventType, limit)); }

    /**
     * Device metadata is read by camera/robot edge services with the same ingest
     * credential used for event submission. It keeps coordinates authoritative in
     * the campus database instead of duplicating them in detector code.
     */
    @GetMapping("/devices/cameras")
    public ApiResponse<List<Map<String, Object>>> cameraDevices(
            Authentication authentication,
            @RequestHeader(value = "X-AI-Ingest-Key", required = false) String ingestKey) {
        requireAdminOrIngestKey(authentication, ingestKey);
        List<Map<String, Object>> result = cameraMapper.selectList(null).stream().map(camera -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", camera.getCameraId());
            item.put("name", camera.getCameraName());
            item.put("location", camera.getLocation());
            item.put("status", camera.getStatus());
            item.put("longitude", camera.getLocationLng());
            item.put("latitude", camera.getLocationLat());
            item.put("quality", camera.getQuality());
            return item;
        }).toList();
        return ApiResponse.success(result);
    }

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

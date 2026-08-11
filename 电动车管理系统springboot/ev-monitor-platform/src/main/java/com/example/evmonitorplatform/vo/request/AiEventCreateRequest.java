package com.example.evmonitorplatform.vo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class AiEventCreateRequest {
    @NotBlank(message = "事件类型不能为空")
    private String eventType;
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;
    private String deviceId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;
    private String locationName;
    private Double longitude;
    private Double latitude;
    private String licensePlate;
    private Integer vehicleId;
    private Integer userId;
    @DecimalMin(value = "0.0", message = "检测置信度不能小于0")
    @DecimalMax(value = "1.0", message = "检测置信度不能大于1")
    private Double detectionConfidence;
    private List<String> evidenceUrls;
    private Map<String, Object> rawPayload;
    private String idempotencyKey;
    private Boolean autoAnalyze;
}

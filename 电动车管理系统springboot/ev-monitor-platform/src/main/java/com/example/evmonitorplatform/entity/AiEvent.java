package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_event")
public class AiEvent {
    @TableId(type = IdType.AUTO)
    private Long eventId;
    private String eventCode;
    private String eventType;
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
    private String identityMethod;
    private Double identityConfidence;
    private Double detectionConfidence;
    private String evidenceUrls;
    private String rawPayload;
    private String status;
    private String riskLevel;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String dedupeKey;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

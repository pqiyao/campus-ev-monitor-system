package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_analysis")
public class AiAnalysis {
    @TableId(type = IdType.AUTO)
    private Long analysisId;
    private Long eventId;
    private String provider;
    private String modelName;
    private String promptVersion;
    private Boolean validEvent;
    private Double confidence;
    private String riskLevel;
    private Boolean identityReliable;
    private Boolean evidenceSufficient;
    private String summary;
    private String recommendedAction;
    private String notificationText;
    private String observationsJson;
    private String rawResponse;
    private Integer latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

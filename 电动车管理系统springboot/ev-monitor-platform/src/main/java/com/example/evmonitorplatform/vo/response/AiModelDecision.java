package com.example.evmonitorplatform.vo.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiModelDecision {
    private boolean valid;
    private String eventType;
    private double confidence;
    private String riskLevel;
    private boolean identityReliable;
    private boolean evidenceSufficient;
    private String summary;
    private String recommendedAction;
    private String notificationText;
    private List<String> observations = new ArrayList<>();
    private String rawResponse;
    private String provider;
    private String modelName;
    private int latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
}

package com.example.evmonitorplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "ev-monitor.ai")
public class AiGovernanceProperties {
    private String mode = "mock";
    private String baseUrl = "https://newapi.prorisehub.com/v1";
    private String apiKey = "";
    private String apiStyle = "responses";
    private String model = "gpt-5.6";
    private String reasoningEffort = "none";
    private int timeoutSeconds = 45;
    private boolean fallbackToMock = true;
    private String ingestKey = "";
    private double autoAcceptThreshold = 0.90;
    private double reviewThreshold = 0.75;
    private double identityThreshold = 0.90;
    private Map<String, Integer> violationTypeMapping = defaultViolationTypeMapping();

    private static Map<String, Integer> defaultViolationTypeMapping() {
        Map<String, Integer> mapping = new LinkedHashMap<>();
        mapping.put("ILLEGAL_PARKING", 1);
        mapping.put("RESTRICTED_AREA", 5);
        mapping.put("OVERLOAD", 6);
        mapping.put("NO_HELMET", 9);
        return mapping;
    }
}

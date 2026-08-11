package com.example.evmonitorplatform.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiTextResult {
    private String text;
    private String provider;
    private String modelName;
    private int latencyMs;
    private String rawResponse;
}

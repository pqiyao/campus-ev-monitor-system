package com.example.evmonitorplatform.vo.request;

import lombok.Data;

@Data
public class AiDemoRunRequest {
    private String scenario = "NO_HELMET";
    private String licensePlate;
    private String locationName;
    private Boolean autoAnalyze = true;
}

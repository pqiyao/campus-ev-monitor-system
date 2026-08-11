package com.example.evmonitorplatform.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ViolationDTO {
    private Integer illegalId;
    private Integer vehicleId;
    private Integer userId;
    private String licensePlate;
    private String ownerName;
    private Integer illegalType;
    private String typeName;
    private Double illegalLng;
    private Double illegalLat;
    private String location;
    private LocalDateTime illegalTime;
    private Integer deductionPoints;
    private Integer evidenceId;
    private String evidenceUrl;
    private Integer illegalSource;
    private String sourceName;
    private Integer illegalState;
    private String stateName;
    private LocalDateTime createTime;
    private String rectifyStatus;
    private String appealStatus;
}
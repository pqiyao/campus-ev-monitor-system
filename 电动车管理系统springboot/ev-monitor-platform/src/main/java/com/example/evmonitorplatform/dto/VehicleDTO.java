package com.example.evmonitorplatform.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
public class VehicleDTO {
    private Integer vehicleId;
    private Integer userId;
    private String username;
    private String realName;
    private String licensePlate;
    private String brand;
    private String color;
    private String frameNum;
    private String batteryType;
    private String vehicleType;
    private String vehiclePhoto;
    private String status;
    private Boolean isFrozen;
    private Boolean isBlacklist;
    private LocalDateTime registerTime;
    private LocalDateTime auditTime;
    private Integer auditUserId;
    private String auditorName;
    private LocalDate validDate;
    private String etcNumber;
    private String deviceStatus;
    private Integer violationCount;
}
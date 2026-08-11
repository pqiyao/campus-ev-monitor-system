package com.example.evmonitorplatform.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleRegisterVO {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;
    
    private String licensePlate;
    
    private String brand;
    
    private String color;
    
    private String frameNum;
    
    private String batteryType;
    
    private String vehicleType;
    
    private String vehiclePhoto;
    
    private String deviceMac;
}

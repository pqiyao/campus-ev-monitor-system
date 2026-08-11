package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("vehicle_info")
public class Vehicle {
    @TableId(type = IdType.AUTO)
    private Integer vehicleId;
    
    private Integer userId;
    
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
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registerTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    
    private Integer auditUserId;
    
    private LocalDate validDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
    
    @TableField(exist = false)
    private String ownerName;
    
    @TableField(exist = false)
    private String phone;
    
    @TableField(exist = false)
    private String etcNumber;
    
    @TableField(exist = false)
    private String gpsStatus;
    
    @TableField(exist = false)
    private Integer battery;
}
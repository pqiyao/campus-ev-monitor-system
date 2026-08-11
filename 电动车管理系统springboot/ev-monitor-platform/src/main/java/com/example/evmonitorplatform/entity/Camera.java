package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("camera_monitor")
public class Camera {
    @TableId(type = IdType.AUTO)
    private Integer cameraId;
    
    private String cameraName;
    
    private String location;
    
    private Double locationLng;
    
    private Double locationLat;
    
    private String rtspUrl;
    
    private String status;
    
    private String quality;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeat;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
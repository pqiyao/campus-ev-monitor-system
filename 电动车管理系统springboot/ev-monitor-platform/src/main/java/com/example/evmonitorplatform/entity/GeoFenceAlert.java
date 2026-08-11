package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 围栏预警记录实体
 */
@Data
@TableName("geofence_alert")
public class GeoFenceAlert {

    @TableId(value = "alert_id", type = IdType.AUTO)
    private Integer alertId;

    private Integer fenceId;
    private String  fenceName;
    private Integer vehicleId;
    private String  licensePlate;
    private Integer userId;
    private String  userName;

    /** enter=入围 exit=出围 */
    private String  alertType;

    private Double  triggerLng;
    private Double  triggerLat;

    /** 1=提示 2=警告 3=严重 */
    private Integer alertLevel;

    /** 0=未处理 1=已处理 */
    private Integer isProcessed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
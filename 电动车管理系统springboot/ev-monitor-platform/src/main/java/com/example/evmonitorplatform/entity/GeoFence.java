package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子围栏实体
 */
@Data
@TableName("geofence")
public class GeoFence {

    @TableId(value = "fence_id", type = IdType.AUTO)
    private Integer fenceId;

    private String fenceName;

    /** 1=禁止进入 2=禁止驶出 3=限速区域 4=监控区域 5=允许停车区（违停判定：在非停车区静置超时则记违规） */
    private Integer fenceType;

    private String fenceColor;

    /**
     * 边界坐标，JSON 字符串存储，例如:
     * [{"lng":121.47,"lat":31.23},...]
     * 读写均用 String，前端原样传回
     */
    private String coordinates;

    private Double centerLng;
    private Double centerLat;

    /** 1=提示 2=警告 3=严重 */
    private Integer alertLevel;

    /** notify / mqtt / both */
    private String alertMode;

    private String description;

    /** 0=禁用 1=启用 */
    private Integer status;

    private Integer createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
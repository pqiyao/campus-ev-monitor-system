package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("vehicle_illegal_record")
public class Violation {
    @TableId(type = IdType.AUTO)
    private Integer illegalId;

    private Integer vehicleId;

    private Integer userId;

    private Integer illegalType;

    private Double illegalLng;

    private Double illegalLat;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime illegalTime;

    private Integer deductionPoints;

    /** 本条违规实际从用户剩余分中扣除的分值 */
    private Integer pointsDeducted;

    private Integer evidenceId;

    // 修改点：移除 @TableField(exist = false)
    private String evidenceUrl;

    private Integer illegalSource;

    private Integer illegalState;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;

    // 以下为虚拟字段（不映射数据库）
    @TableField(exist = false)
    private String licensePlate;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String vehiclePhoto;
    @TableField(exist = false)
    private String illegalTypeName;

    /** 学生班级（来自 user.class_name） */
    @TableField(exist = false)
    private String className;
}
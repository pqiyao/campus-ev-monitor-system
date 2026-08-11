package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("illegal_appeal_record")
public class AppealRecord {
    @TableId(type = IdType.AUTO)
    private Integer appealId;

    private Integer illegalId;

    private Integer userId;

    private String appealReason;

    private String appealEvidence;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appealTime;

    private Integer appealState; // 使用AppealStateEnum

    private String appealSource; // 申诉来源: user-用户, system-系统, admin-管理员

    private Integer urgencyLevel; // 紧急程度：1-一般, 2-紧急, 3-特急

    private Integer auditUserId;

    private String auditOpinion;

    private Integer appealResult; // 使用AppealResultEnum

    private String auditRemark;

    private String rejectReason;

    private String evidencePhotos; // 审核证据照片，多个用逗号分隔

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 表 illegal_appeal_record 多数库脚本无 deleted 列：不参与 SQL，避免逻辑删除条件报错 */
    @TableField(exist = false)
    private Boolean deleted;

    // 以下为虚拟字段，用于展示
    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userPhone;

    @TableField(exist = false)
    private String className;

    @TableField(exist = false)
    private String userType;

    @TableField(exist = false)
    private String auditUserName;

    @TableField(exist = false)
    private String licensePlate;

    @TableField(exist = false)
    private String illegalTypeName;

    /** 违规类型编码（与 vehicle_illegal_record.illegal_type 一致） */
    @TableField(exist = false)
    private Integer illegalType;

    @TableField(exist = false)
    private String illegalTime;

    @TableField(exist = false)
    private String location;

    @TableField(exist = false)
    private String vehiclePhoto;

    @TableField(exist = false)
    private String evidenceUrl;

    /**
     * 获取申诉状态描述
     */
    public String getAppealStateDesc() {
        if (this.appealState == null) return "";
        return com.example.evmonitorplatform.common.enums.AppealStateEnum.getDescriptionByCode(this.appealState);
    }

    /**
     * 获取申诉结果描述
     */
    public String getAppealResultDesc() {
        if (this.appealResult == null) return "";
        com.example.evmonitorplatform.common.enums.AppealResultEnum resultEnum =
                com.example.evmonitorplatform.common.enums.AppealResultEnum.getByCode(this.appealResult);
        return resultEnum != null ? resultEnum.getDescription() : "";
    }
}
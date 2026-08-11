package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("illegal_rectify_record")
public class RectifyRecord {
    @TableId(type = IdType.AUTO)
    private Integer rectifyId;
    
    private Integer illegalId;
    
    private Integer userId;
    
    private String rectifyPhoto;
    
    private String rectifyDesc;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rectifyTime;
    
    private Integer verifyState;
    
    private Integer verifyUserId;
    
    private String verifyEvidence;
    
    private String verifyRemark;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifyTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private String userName;
    
    @TableField(exist = false)
    private String userRole;
    
    @TableField(exist = false)
    private String verifyUserName;

    /** 联表：绑定违规的车辆车牌 */
    @TableField(exist = false)
    private String licensePlate;

    /** 联表：违规类型编码 */
    @TableField(exist = false)
    private Integer illegalType;
}
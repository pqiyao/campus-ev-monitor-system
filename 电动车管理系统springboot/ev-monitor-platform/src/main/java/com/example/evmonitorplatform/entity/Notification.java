package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Integer notificationId;
    
    private String title;
    
    private String content;
    
    private String type;
    
    private Boolean isUrgent;
    
    private String receiverType;
    
    private String receiverIds;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;
    
    private Integer readCount;
    
    private Integer totalCount;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;

    /** 当前查询用户是否已读（仅用户通知列表等接口填充） */
    @TableField(exist = false)
    private Boolean readByMe;
}
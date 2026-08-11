package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer userId;
    
    private String username;
    
    private String password;
    
    private String realName;
    
    private String userType;
    
    private String studentId;
    
    private String jobId;
    
    private String phone;
    
    private String email;
    
    private String avatar;
    
    private String department;

    /** 学院（简化版组织信息，与学生档案一致） */
    private String college;

    /** 专业 */
    private String major;

    /** 班级名称（与 counselor_class.class_name 对应） */
    @TableField("class_name")
    private String className;
    
    private String status;

    /** 校园电动车剩余记分，满分 12 */
    private Integer licensePoints;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
    
    @TableField(exist = false)
    private String token;
}
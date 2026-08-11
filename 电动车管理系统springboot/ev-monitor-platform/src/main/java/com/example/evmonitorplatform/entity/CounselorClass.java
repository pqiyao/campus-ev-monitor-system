package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("counselor_class")
public class CounselorClass {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer counselorUserId;

    private String college;

    private String major;

    @TableField("class_name")
    private String className;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("org_class")
public class OrgClass {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String college;

    private String major;

    @TableField("class_name")
    private String className;

    private Integer sortOrder;

    private LocalDateTime createTime;
}

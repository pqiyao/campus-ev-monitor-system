package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("license_point_log")
public class LicensePointLog {
    @TableId(type = IdType.AUTO)
    private Integer logId;
    private Integer userId;
    /** 正为加分，负为扣分 */
    private Integer delta;
    private Integer balanceAfter;
    private String bizType;
    private Integer refIllegalId;
    private Integer refWelfareId;
    private String remark;
    private String evidenceUrls;
    private Integer operatorUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

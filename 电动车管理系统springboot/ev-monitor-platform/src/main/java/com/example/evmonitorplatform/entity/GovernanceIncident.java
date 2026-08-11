package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("governance_incident")
public class GovernanceIncident {
    @TableId(type = IdType.AUTO)
    private Long incidentId;
    private String incidentCode;
    private Long eventId;
    private Integer vehicleId;
    private Integer userId;
    private String eventType;
    private String title;
    private String summary;
    private String riskLevel;
    private String decisionType;
    private String state;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
    private Integer violationId;
    private Integer reviewerId;
    private String reviewRemark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

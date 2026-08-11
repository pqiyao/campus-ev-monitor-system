package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("welfare_point_request")
public class WelfarePointRequest {
    @TableId(type = IdType.AUTO)
    private Integer requestId;
    private Integer userId;
    private String content;
    private String imageUrls;
    /** 0 待审 1 通过 2 拒绝 */
    private Integer status;
    private Integer pointsAwarded;
    private String adminRemark;
    private Integer reviewerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
}

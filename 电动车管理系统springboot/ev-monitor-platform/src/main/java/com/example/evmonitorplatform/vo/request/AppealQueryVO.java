package com.example.evmonitorplatform.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "申诉查询请求参数")
public class AppealQueryVO {
    
    @Schema(description = "申诉ID")
    private Integer appealId;
    
    @Schema(description = "违规ID")
    private Integer illegalId;
    
    @Schema(description = "用户ID")
    private Integer userId;
    
    @Schema(description = "申诉状态：0-待审核, 1-审核中, 2-申诉成功, 3-申诉失败, 4-已撤销, 5-已过期")
    private Integer appealState;
    
    @Schema(description = "申诉结果：1-通过, 2-拒绝, 3-部分通过, 4-转办")
    private Integer appealResult;
    
    @Schema(description = "审核用户ID")
    private Integer auditUserId;
    
    @Schema(description = "车牌号")
    private String licensePlate;
    
    @Schema(description = "用户姓名")
    private String userName;
    
    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "导员 userId：仅返回其管辖班级内学生的申诉")
    private Integer counselorId;
    
    @Schema(description = "开始时间（yyyy-MM-dd HH:mm:ss）")
    private String startTime;
    
    @Schema(description = "结束时间（yyyy-MM-dd HH:mm:ss）")
    private String endTime;
    
    @Schema(description = "申诉来源：user-用户, system-系统, admin-管理员")
    private String appealSource;
    
    @Schema(description = "紧急程度：1-一般, 2-紧急, 3-特急")
    private Integer urgencyLevel;
    
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    
    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
    
    @Schema(description = "排序字段", example = "appeal_time")
    private String orderBy = "appeal_time";
    
    @Schema(description = "排序方向：asc-升序, desc-降序", example = "desc")
    private String orderDirection = "desc";
}
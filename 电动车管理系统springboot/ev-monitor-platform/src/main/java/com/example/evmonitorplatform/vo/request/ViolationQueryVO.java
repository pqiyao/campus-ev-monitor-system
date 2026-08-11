package com.example.evmonitorplatform.vo.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ViolationQueryVO {
    private Integer illegalType;
    private Integer illegalState;
    private String vehiclePlate;
    /** 车主姓名（联表 user.real_name 模糊查询） */
    private String ownerName;
    private String startTime;
    private String endTime;
    /** 导员 userId：仅返回其 counselor_class 中所辖班级的违规 */
    private Integer counselorId;
    /** 按学生 user.class_name 精确筛选 */
    private String className;
    private Integer pageNum;
    private Integer pageSize;
}
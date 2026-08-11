package com.example.evmonitorplatform.vo.request;

import lombok.Data;

@Data
public class LicensePointWelfareReviewVO {
    private Integer requestId;
    private Boolean approve;
    private Integer points;
    private String adminRemark;
    private Integer reviewerId;
}

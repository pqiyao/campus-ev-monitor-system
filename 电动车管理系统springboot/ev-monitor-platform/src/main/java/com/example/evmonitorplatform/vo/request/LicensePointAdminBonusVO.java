package com.example.evmonitorplatform.vo.request;

import lombok.Data;

@Data
public class LicensePointAdminBonusVO {
    private Integer userId;
    private Integer points;
    private String remark;
    private String evidenceUrls;
    private Integer operatorUserId;
}

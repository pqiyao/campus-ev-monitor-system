package com.example.evmonitorplatform.vo.request;

import lombok.Data;

import java.util.List;

@Data
public class SimLocationBatchRequest {
    /**
     * 屏幕点位在 Redis 中的存活时间（分钟），默认 1440（24 小时）
     */
    private Integer ttlMinutes;
    private List<SimPointRequest> points;
}

package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.vo.request.ThermalFrameRequest;

import java.util.List;
import java.util.Map;

/**
 * 红外热源风险链路：热帧接入 → 热点分割与连通域提取 → 热源风险评分 → 高危自动升级AI事件。
 */
public interface ThermalIngestService {

    /** 接入一帧红外温度矩阵，执行热点提取与风险评分，HIGH 级别自动生成 AI 事件（仍需管理员确认）。 */
    Map<String, Object> ingestFrame(ThermalFrameRequest request);

    /** 最近热帧列表。 */
    List<Map<String, Object>> listFrames(Integer limit, String riskLevel);

    /** 热帧详情，含热点明细与关联AI事件。 */
    Map<String, Object> getFrameDetail(Long frameId);
}

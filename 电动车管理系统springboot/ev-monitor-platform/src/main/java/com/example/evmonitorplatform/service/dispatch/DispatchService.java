package com.example.evmonitorplatform.service.dispatch;

import java.util.List;
import java.util.Map;

/**
 * 空地协同调度编排：事件驱动派单 → 地面巡检车执行 → 到场复核取证 → 回传治理闭环。
 */
public interface DispatchService {

    /** 注册/更新地面巡检车（fleet 档案，坐标 WGS84）。 */
    Map<String, Object> registerRobot(Map<String, Object> request);

    /** 机器人车队列表。 */
    List<Map<String, Object>> listRobots();

    /** 对单个 AI 事件执行 DispatchScore 评分并派单最优空闲机器人。 */
    Map<String, Object> dispatchForEvent(Long eventId);

    /** 只评分不派单（评分引擎透明演示）。 */
    List<Map<String, Object>> previewScores(Long eventId);

    /** 扫描未处置 AI 事件批量调度（自动模式）。 */
    Map<String, Object> dispatchPendingEvents(Integer limit);

    /** 派单列表。 */
    List<Map<String, Object>> listOrders(String state, Integer limit);

    /** 派单详情（含评分明细、遥测轨迹）。 */
    Map<String, Object> getOrderDetail(Long orderId);

    /** 派单故障注入（BLOCKED/WHEEL_SLIP/GPS_OFFSET/...），触发调度重派评估。 */
    Map<String, Object> injectFault(Long orderId, String faultType, Map<String, Object> parameters);

    /** 故障恢复。 */
    Map<String, Object> clearFault(Long orderId);
}

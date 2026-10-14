package com.example.evmonitorplatform.service.dispatch;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 地面巡检车执行器契约（与 DroneAdapter 同构）。业务层只依赖遥测/事件回调，
 * 可替换为真实底盘适配器（ROS2/协议网关）而不改调度与治理代码。
 */
public interface GroundRobotAdapter {
    void start(GroundMissionPlan plan, Consumer<GroundRobotTelemetry> telemetrySink, Consumer<GroundRobotEvent> eventSink);

    default void injectFault(Long missionId, String faultType, Map<String, Object> parameters) {
        throw new UnsupportedOperationException("当前执行器不支持故障注入");
    }

    default void clearFault(Long missionId) {
        throw new UnsupportedOperationException("当前执行器不支持故障恢复");
    }

    void stop(Long missionId);

    void shutdown();
}

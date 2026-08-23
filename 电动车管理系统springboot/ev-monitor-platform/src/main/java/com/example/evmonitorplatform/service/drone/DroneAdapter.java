package com.example.evmonitorplatform.service.drone;

import java.util.function.Consumer;
import java.util.Map;

public interface DroneAdapter {
    void start(DronePlan plan, Consumer<DroneTelemetry> telemetrySink, Consumer<DroneEvent> eventSink);

    default void injectFault(Long missionId, String faultType, Map<String, Object> parameters) {
        throw new UnsupportedOperationException("当前执行器不支持故障注入");
    }

    default void clearFault(Long missionId) {
        throw new UnsupportedOperationException("当前执行器不支持故障恢复");
    }

    void stop(Long missionId);

    void shutdown();
}

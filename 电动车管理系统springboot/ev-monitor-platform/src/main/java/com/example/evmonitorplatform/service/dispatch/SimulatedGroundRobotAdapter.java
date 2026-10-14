package com.example.evmonitorplatform.service.dispatch;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 地面巡检车确定性仿真执行器：轮式运动学（匀速+打滑降速）、里程电量消耗、
 * 到站取证停留、六类故障注入（GPS_OFFSET/PACKET_LOSS/NETWORK_DELAY/LOW_BATTERY/WHEEL_SLIP/BLOCKED）。
 * 与 SimulatedDroneAdapter 同构，业务层无感切换。
 */
@Slf4j
@Component
public class SimulatedGroundRobotAdapter implements GroundRobotAdapter {
    private static final long TICK_MS = 500L;
    private static final double EARTH_DEGREES_PER_METER = 1D / 111_000D;
    private static final double BATTERY_DRAIN_PER_METER = 0.004D;
    private static final double RETURN_BATTERY_THRESHOLD = 20D;
    private static final double BLOCKED_PROGRESS = 0.6D; // BLOCKED 时走到路线60%处抛锚

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<Long, Simulation> simulations = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, FaultProfile> faults = new ConcurrentHashMap<>();

    @Override
    public void start(GroundMissionPlan plan, Consumer<GroundRobotTelemetry> telemetrySink, Consumer<GroundRobotEvent> eventSink) {
        if (plan == null || plan.missionId() == null) throw new IllegalArgumentException("地面巡检任务不能为空");
        Simulation simulation = simulations.compute(plan.missionId(), (id, old) -> {
            if (old == null || old.completed) return Simulation.create(plan);
            if (old.plan.planVersion() != plan.planVersion()) return old.replacePlan(plan);
            return old;
        });
        tasks.compute(plan.missionId(), (id, old) -> {
            if (old != null && !old.isCancelled()) return old;
            return executor.scheduleAtFixedRate(
                    () -> tick(simulation, telemetrySink, eventSink), TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
        });
    }

    @Override
    public void injectFault(Long missionId, String faultType, Map<String, Object> parameters) {
        if (missionId == null || faultType == null || faultType.isBlank()) throw new IllegalArgumentException("故障参数不能为空");
        FaultProfile profile = faults.computeIfAbsent(missionId, id -> new FaultProfile());
        String type = faultType.toUpperCase(java.util.Locale.ROOT);
        switch (type) {
            case "GPS_OFFSET" -> profile.gpsOffset = number(parameters, "meters", 8D);
            case "PACKET_LOSS" -> profile.packetLossRate = Math.max(0D, Math.min(1D, number(parameters, "rate", 0.3D)));
            case "NETWORK_DELAY" -> profile.delayMs = Math.max(0L, Math.min(5000L, Math.round(number(parameters, "milliseconds", 800D))));
            case "LOW_BATTERY" -> profile.forceBattery = Math.max(1D, Math.min(100D, number(parameters, "battery", 18D)));
            case "WHEEL_SLIP" -> profile.slipFactor = Math.max(0.1D, Math.min(0.9D, number(parameters, "factor", 0.5D)));
            case "BLOCKED" -> {
                profile.blocked = true;
                Simulation sim = simulations.get(missionId);
                if (sim != null) sim.blockedAtMeters = sim.meters + Math.max(10D, number(parameters, "afterMeters", 50D));
            }
            default -> throw new IllegalArgumentException("不支持的地面车仿真故障：" + type);
        }
        profile.lastFault = type;
    }

    @Override
    public void clearFault(Long missionId) {
        if (missionId != null) faults.remove(missionId);
    }

    @Override
    public void stop(Long missionId) {
        if (missionId == null) return;
        ScheduledFuture<?> task = tasks.remove(missionId);
        if (task != null) task.cancel(false);
    }

    @Override
    @PreDestroy
    public void shutdown() {
        tasks.values().forEach(task -> task.cancel(false));
        tasks.clear();
        executor.shutdownNow();
        simulations.clear();
        faults.clear();
    }

    private void tick(Simulation simulation, Consumer<GroundRobotTelemetry> telemetrySink, Consumer<GroundRobotEvent> eventSink) {
        try {
            if (simulation.completed) {
                stop(simulation.plan.missionId());
                return;
            }
            FaultProfile fault = faults.getOrDefault(simulation.plan.missionId(), new FaultProfile());
            if (fault.delayMs > 0) {
                try { Thread.sleep(fault.delayMs); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
            }
            if (fault.blocked && simulation.blockedReported) {
                // 抛锚等待清障：不移动，仅上报遥测
                emitTelemetry(simulation, fault, telemetrySink, "BLOCKED", false);
                return;
            }
            GroundWaypoint target;
            boolean returning = simulation.returning;
            if (returning) {
                target = new GroundWaypoint(null, -1, simulation.plan.homeLng(), simulation.plan.homeLat(), 0, "RETURN");
            } else if (simulation.index >= simulation.plan.waypoints().size()) {
                simulation.returning = true;
                eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), null, -1, "RETURNING", "取证完成，返回停靠点"));
                target = new GroundWaypoint(null, -1, simulation.plan.homeLng(), simulation.plan.homeLat(), 0, "RETURN");
                returning = true;
            } else {
                target = simulation.plan.waypoints().get(simulation.index);
            }

            double speedMps = simulation.plan.speedMps() * (fault.slipFactor < 1D ? fault.slipFactor : 1D);
            double stepDegrees = speedMps * (TICK_MS / 1000.0D) * EARTH_DEGREES_PER_METER;
            double dLng = target.lng() - simulation.lng;
            double dLat = target.lat() - simulation.lat;
            double distance = Math.sqrt(dLng * dLng + dLat * dLat);
            boolean arrived = distance <= stepDegrees;
            if (fault.blocked && !simulation.blockedReported && simulation.meters >= simulation.blockedAtMeters) {
                simulation.blockedReported = true;
                eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), null, -1,
                        "BLOCKED", "前方通道阻断，车轮受阻无法通行，原地等待调度重派"));
                emitTelemetry(simulation, fault, telemetrySink, "BLOCKED", false);
                return;
            }
            if (arrived) {
                simulation.lng = target.lng();
                simulation.lat = target.lat();
                // 到站事件不依赖 waypointId（调度临时复核点无业务航点ID）
                if (!returning) {
                    eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), target.waypointId(), target.sequenceNo(),
                            "ARRIVED", "到达任务点，开始现场复核取证"));
                    if (target.dwellSec() > 0 && !simulation.dwellReported) {
                        simulation.dwellReported = true;
                        eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), target.waypointId(), target.sequenceNo(),
                                "CAPTURED", "现场图像与热源数据已采集上传"));
                    }
                    simulation.index++;
                    simulation.dwellReported = false;
                }
            } else {
                simulation.lng += dLng / distance * stepDegrees;
                simulation.lat += dLat / distance * stepDegrees;
                simulation.meters += stepDegrees / EARTH_DEGREES_PER_METER;
            }

            if (!returning) {
                simulation.battery = Math.max(0D, simulation.battery - speedMps * (TICK_MS / 1000.0D) * BATTERY_DRAIN_PER_METER);
                if (fault.forceBattery != null) simulation.battery = Math.min(simulation.battery, fault.forceBattery);
                if (simulation.battery <= RETURN_BATTERY_THRESHOLD) {
                    simulation.returning = true;
                    eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), null, -1, "LOW_BATTERY", "电量低于返航阈值，返回停靠点"));
                }
            }

            String motionState = simulation.returning ? "RETURNING"
                    : fault.blocked && simulation.blockedReported ? "BLOCKED" : "DRIVING";
            if (fault.packetLossRate > 0 && Math.random() < fault.packetLossRate) {
                eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), null, -1, "PACKET_LOSS", "遥测数据包丢失，等待下一帧恢复"));
                return;
            }
            emitTelemetry(simulation, fault, telemetrySink, motionState, arrived);

            if (simulation.returning && distance <= stepDegrees) {
                simulation.completed = true;
                eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), null, -1, "COMPLETED", "地面巡检车已返回停靠点，任务完成"));
                stop(simulation.plan.missionId());
            }
        } catch (Exception ex) {
            log.warn("地面巡检车仿真执行失败 missionId={}: {}", simulation.plan.missionId(), ex.getMessage());
            eventSink.accept(new GroundRobotEvent(simulation.plan.missionId(), null, -1, "FAILED", ex.getMessage()));
            stop(simulation.plan.missionId());
        }
    }

    private void emitTelemetry(Simulation simulation, FaultProfile fault,
                               Consumer<GroundRobotTelemetry> telemetrySink, String motionState, boolean arrived) {
        GroundWaypoint target = simulation.returning
                ? new GroundWaypoint(null, -1, simulation.plan.homeLng(), simulation.plan.homeLat(), 0, "RETURN")
                : simulation.plan.waypoints().get(Math.min(simulation.index, simulation.plan.waypoints().size() - 1));
        double telemetryLng = simulation.lng + fault.gpsOffset * (1D / 111_000D);
        double telemetryLat = simulation.lat + fault.gpsOffset * (1D / 111_000D);
        telemetrySink.accept(new GroundRobotTelemetry(
                simulation.plan.missionId(),
                simulation.returning ? null : target.waypointId(),
                simulation.returning ? -1 : target.sequenceNo(),
                telemetryLng,
                telemetryLat,
                simulation.plan.speedMps(),
                simulation.battery,
                1D,
                fault.slipFactor < 1D ? "SLIP_COMPENSATING" : "READY",
                motionState,
                arrived));
    }

    private static final class Simulation {
        private GroundMissionPlan plan;
    private int index;
    private double lng;
    private double lat;
    private double battery;
    private double meters;
    private double blockedAtMeters = Double.MAX_VALUE;
    private boolean returning;
    private boolean completed;
    private boolean blockedReported;
    private boolean dwellReported;

        private static Simulation create(GroundMissionPlan plan) {
            Simulation simulation = new Simulation();
            simulation.plan = plan;
            simulation.battery = plan.initialBattery();
            // 从停靠点真实起步行驶到任务点（不再首帧传送），保证遥测轨迹可视化完整
            simulation.lng = plan.homeLng();
            simulation.lat = plan.homeLat();
            return simulation;
        }

        private Simulation replacePlan(GroundMissionPlan next) {
            plan = next;
            index = 0;
            returning = false;
            completed = false;
            blockedReported = false;
            dwellReported = false;
            blockedAtMeters = Double.MAX_VALUE;
            return this;
        }
    }

    private static final class FaultProfile {
        private double gpsOffset;
        private double packetLossRate;
        private long delayMs;
        private Double forceBattery;
        private double slipFactor = 1D;
        private boolean blocked;
        private String lastFault;
    }

    private static double number(Map<String, Object> values, String key, double fallback) {
        try {
            return values == null || values.get(key) == null ? fallback : Double.parseDouble(String.valueOf(values.get(key)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}

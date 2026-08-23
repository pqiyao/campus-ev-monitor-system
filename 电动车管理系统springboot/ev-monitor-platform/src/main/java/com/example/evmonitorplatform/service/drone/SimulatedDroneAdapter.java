package com.example.evmonitorplatform.service.drone;

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
 * Deterministic adapter used for local validation and simulation. The business
 * service only sees typed telemetry/events and can be paired with a real adapter later.
 */
@Slf4j
@Component
public class SimulatedDroneAdapter implements DroneAdapter {
    private static final double STEP_DEGREES = 0.000035D;
    private static final double BATTERY_DRAIN_PER_TICK = 0.12D;
    private static final double RETURN_BATTERY_THRESHOLD = 25D;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<Long, Simulation> simulations = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, FaultProfile> faults = new ConcurrentHashMap<>();

    @Override
    public void start(DronePlan plan, Consumer<DroneTelemetry> telemetrySink, Consumer<DroneEvent> eventSink) {
        if (plan == null || plan.missionId() == null) throw new IllegalArgumentException("无人机任务不能为空");
        Simulation simulation = simulations.compute(plan.missionId(), (id, old) -> {
            if (old == null || old.completed) return Simulation.create(plan);
            if (old.plan.planVersion() != plan.planVersion()) return old.replacePlan(plan);
            return old;
        });
        tasks.compute(plan.missionId(), (id, old) -> {
            if (old != null && !old.isCancelled()) return old;
            return executor.scheduleAtFixedRate(
                    () -> tick(simulation, telemetrySink, eventSink), 500, 500, TimeUnit.MILLISECONDS);
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
            case "LOW_BATTERY" -> profile.forceBattery = Math.max(1D, Math.min(100D, number(parameters, "battery", 24D)));
            default -> throw new IllegalArgumentException("不支持的仿真故障：" + type);
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

    private void tick(Simulation simulation, Consumer<DroneTelemetry> telemetrySink, Consumer<DroneEvent> eventSink) {
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
            DroneWaypoint target;
            boolean returning = simulation.returning;
            if (returning) {
                target = new DroneWaypoint(null, -1, simulation.plan.homeLng(), simulation.plan.homeLat(), 30D, 0);
            } else if (simulation.index >= simulation.plan.waypoints().size()) {
                simulation.returning = true;
                eventSink.accept(new DroneEvent(simulation.plan.missionId(), null, -1, "RETURNING", "完成巡检航点，开始返航"));
                target = new DroneWaypoint(null, -1, simulation.plan.homeLng(), simulation.plan.homeLat(), 30D, 0);
                returning = true;
            } else {
                target = simulation.plan.waypoints().get(simulation.index);
            }

            if (!simulation.positionInitialized) {
                simulation.lng = target.lng();
                simulation.lat = target.lat();
                simulation.positionInitialized = true;
            }
            double dLng = target.lng() - simulation.lng;
            double dLat = target.lat() - simulation.lat;
            double distance = Math.sqrt(dLng * dLng + dLat * dLat);
            boolean arrived = distance <= STEP_DEGREES;
            if (arrived) {
                simulation.lng = target.lng();
                simulation.lat = target.lat();
                if (!returning && target.waypointId() != null) {
                    eventSink.accept(new DroneEvent(simulation.plan.missionId(), target.waypointId(), target.sequenceNo(), "ARRIVED", "到达规划航点"));
                    simulation.index++;
                }
            } else {
                simulation.lng += dLng / distance * STEP_DEGREES;
                simulation.lat += dLat / distance * STEP_DEGREES;
            }

            if (!returning) {
                simulation.battery = Math.max(0D, simulation.battery - BATTERY_DRAIN_PER_TICK);
                if (fault.forceBattery != null) simulation.battery = Math.min(simulation.battery, fault.forceBattery);
                if (simulation.battery <= RETURN_BATTERY_THRESHOLD) {
                    simulation.returning = true;
                    eventSink.accept(new DroneEvent(simulation.plan.missionId(), null, -1, "LOW_BATTERY", "电量低于返航阈值，开始返航"));
                }
            }

            String flightState = simulation.returning ? "RETURNING" : "FLYING";
            if (fault.packetLossRate > 0 && Math.random() < fault.packetLossRate) {
                eventSink.accept(new DroneEvent(simulation.plan.missionId(), null, -1, "PACKET_LOSS", "遥测数据包丢失，等待下一帧恢复"));
                return;
            }
            double telemetryLng = simulation.lng + fault.gpsOffset / 111_000D;
            double telemetryLat = simulation.lat + fault.gpsOffset / 111_000D;
            telemetrySink.accept(new DroneTelemetry(
                    simulation.plan.missionId(),
                    returning ? null : target.waypointId(),
                    returning ? -1 : target.sequenceNo(),
                    telemetryLng,
                    telemetryLat,
                    target.altitudeM(),
                    3D,
                    simulation.battery,
                    1D,
                    "READY",
                    flightState,
                    arrived));

            if (simulation.returning && distance <= STEP_DEGREES) {
                simulation.completed = true;
                eventSink.accept(new DroneEvent(simulation.plan.missionId(), null, -1, "COMPLETED", "无人机已返航，巡检任务完成"));
                stop(simulation.plan.missionId());
            }
        } catch (Exception ex) {
            log.warn("仿真无人机执行失败 missionId={}: {}", simulation.plan.missionId(), ex.getMessage());
            eventSink.accept(new DroneEvent(simulation.plan.missionId(), null, -1, "FAILED", ex.getMessage()));
            stop(simulation.plan.missionId());
        }
    }

    private static final class Simulation {
        private DronePlan plan;
        private int index;
        private double lng;
        private double lat;
        private double battery;
        private boolean positionInitialized;
        private boolean returning;
        private boolean completed;

        private static Simulation create(DronePlan plan) {
            Simulation simulation = new Simulation();
            simulation.plan = plan;
            simulation.battery = plan.initialBattery();
            return simulation;
        }

        private Simulation replacePlan(DronePlan next) {
            plan = next;
            index = 0;
            returning = false;
            completed = false;
            return this;
        }
    }

    private static final class FaultProfile {
        private double gpsOffset;
        private double packetLossRate;
        private long delayMs;
        private Double forceBattery;
        private String lastFault;
    }

    private static double number(Map<String, Object> values, String key, double fallback) {
        try { return values == null || values.get(key) == null ? fallback : Double.parseDouble(String.valueOf(values.get(key))); }
        catch (Exception ignored) { return fallback; }
    }
}

package com.example.evmonitorplatform.service.dispatch;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * 空地协同调度评分引擎（项目书3.7 DispatchScore）。
 * <p>score = 0.35*urgency + 0.25*proximity + 0.20*capability + 0.10*battery + 0.10*availability</p>
 * <ul>
 *   <li>urgency：事件风险等级映射（HIGH=1.0 / MEDIUM=0.6 / LOW=0.3）</li>
 *   <li>proximity：机器人到事件点直线距离归一（500m 内满分，2000m 以上趋零）</li>
 *   <li>capability：机器人能力与事件处置需求的匹配（THERMAL/CAMERA/PATROL）</li>
 *   <li>battery：当前电量充足度（低于返航阈值不给分）</li>
 *   <li>availability：空闲=1.0，忙=0（已派单机器人不重复参与）</li>
 * </ul>
 */
@Component
public class DispatchEngine {
    private static final double W_URGENCY = 0.35D;
    private static final double W_PROXIMITY = 0.25D;
    private static final double W_CAPABILITY = 0.20D;
    private static final double W_BATTERY = 0.10D;
    private static final double W_AVAILABILITY = 0.10D;
    private static final double PROXIMITY_FULL_METERS = 500D;
    private static final double PROXIMITY_ZERO_METERS = 2000D;
    private static final double MIN_DISPATCH_SCORE = 0.35D;
    private static final double MIN_BATTERY = 20D;

    /** 评分明细（落库可解释，路演可审计）。 */
    public record DispatchScore(
            String robotCode,
            double urgency,
            double proximity,
            double capability,
            double battery,
            double availability,
            double total,
            double distanceMeters,
            String explanation) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "robotCode", robotCode,
                    "urgency", round(urgency),
                    "proximity", round(proximity),
                    "capability", round(capability),
                    "battery", round(battery),
                    "availability", round(availability),
                    "total", round(total),
                    "distanceMeters", Math.round(distanceMeters),
                    "explanation", explanation);
        }
    }

    /**
     * @param eventType      AI事件类型（SMOKE_DETECTED/ILLEGAL_PARKING...）
     * @param riskLevel      事件风险等级（HIGH/MEDIUM/LOW/UNKNOWN）
     * @param eventLng       事件经度
     * @param eventLat       事件纬度
     * @param robotCode      机器人编号
     * @param robotLng       机器人当前经度
     * @param robotLat       机器人当前纬度
     * @param robotBattery   机器人电量百分比
     * @param robotState     机器人状态（IDLE/BUSY/CHARGING/OFFLINE）
     * @param robotCaps      能力标签（如 THERMAL,CAMERA）
     */
    public DispatchScore score(String eventType, String riskLevel,
                               double eventLng, double eventLat,
                               String robotCode, double robotLng, double robotLat,
                               double robotBattery, String robotState, String robotCaps) {
        double urgency = urgencyOf(riskLevel);
        double distance = distanceMeters(robotLng, robotLat, eventLng, eventLat);
        double proximity = proximityOf(distance);
        double capability = capabilityOf(eventType, robotCaps);
        double battery = robotBattery <= MIN_BATTERY ? 0D : Math.min(1D, robotBattery / 100D);
        boolean idle = "IDLE".equalsIgnoreCase(robotState);
        double availability = idle ? 1D : 0D;
        double total = W_URGENCY * urgency + W_PROXIMITY * proximity + W_CAPABILITY * capability
                + W_BATTERY * battery + W_AVAILABILITY * availability;
        String explanation = String.format(Locale.ROOT,
                "紧急度%.2f/距离%.0fm贴近度%.2f/能力匹配%.2f/电量%.0f%%=%.2f/空闲%.1f → 综合得分%.3f%s",
                urgency, distance, proximity, capability, robotBattery, battery, availability, total,
                dispatchable(total, idle, robotBattery) ? "（可派单）" : "（不满足派单条件）");
        return new DispatchScore(robotCode, urgency, proximity, capability, battery, availability,
                total, distance, explanation);
    }

    /** 是否满足派单条件：达到阈值、空闲、电量充足。 */
    public boolean dispatchable(DispatchScore score) {
        return dispatchable(score.total(), score.availability() >= 1D, score.battery() * 100D);
    }

    private boolean dispatchable(double total, boolean idle, double battery) {
        return total >= MIN_DISPATCH_SCORE && idle && battery > MIN_BATTERY;
    }

    private double urgencyOf(String riskLevel) {
        if (riskLevel == null) return 0.3D;
        return switch (riskLevel.toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 1.0D;
            case "MEDIUM" -> 0.6D;
            default -> 0.3D;
        };
    }

    private double proximityOf(double distanceMeters) {
        if (distanceMeters <= PROXIMITY_FULL_METERS) return 1D;
        if (distanceMeters >= PROXIMITY_ZERO_METERS) return 0D;
        return 1D - (distanceMeters - PROXIMITY_FULL_METERS) / (PROXIMITY_ZERO_METERS - PROXIMITY_FULL_METERS);
    }

    /** 事件处置需求→能力标签映射：热源类需 THERMAL，其余 CAMERA 即可，PATROL 兜底。 */
    private double capabilityOf(String eventType, String robotCaps) {
        if (robotCaps == null || robotCaps.isBlank()) return 0.3D;
        String caps = robotCaps.toUpperCase(Locale.ROOT);
        boolean thermal = caps.contains("THERMAL");
        boolean camera = caps.contains("CAMERA");
        String type = eventType == null ? "" : eventType.toUpperCase(Locale.ROOT);
        boolean needsThermal = type.contains("SMOKE") || type.contains("FIRE");
        if (needsThermal) return thermal ? 1.0D : camera ? 0.5D : 0.2D;
        return camera ? 1.0D : 0.4D;
    }

    /** 球面近似直线距离（米），校园尺度误差可忽略。 */
    public double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double dLng = (lng2 - lng1) * 111_320D * Math.cos(Math.toRadians((lat1 + lat2) / 2D));
        double dLat = (lat2 - lat1) * 110_540D;
        return Math.sqrt(dLng * dLng + dLat * dLat);
    }

    private static double round(double value) {
        return Math.round(value * 1000D) / 1000D;
    }
}

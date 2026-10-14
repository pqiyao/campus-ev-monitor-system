package com.example.evmonitorplatform.service.dispatch;

import java.util.List;

public record GroundMissionPlan(
        Long missionId,
        int planVersion,
        List<GroundWaypoint> waypoints,
        double initialBattery,
        double homeLng,
        double homeLat,
        double speedMps) {
    public GroundMissionPlan {
        waypoints = List.copyOf(waypoints == null ? List.of() : waypoints);
    }
}

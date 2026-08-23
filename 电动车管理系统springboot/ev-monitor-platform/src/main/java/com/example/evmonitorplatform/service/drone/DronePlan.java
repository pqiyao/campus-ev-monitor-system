package com.example.evmonitorplatform.service.drone;

import java.util.List;

public record DronePlan(
        Long missionId,
        int planVersion,
        List<DroneWaypoint> waypoints,
        double initialBattery,
        double homeLng,
        double homeLat) {
    public DronePlan {
        waypoints = List.copyOf(waypoints == null ? List.of() : waypoints);
    }
}

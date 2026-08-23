package com.example.evmonitorplatform.service.drone;

public record DroneWaypoint(
        Long waypointId,
        int sequenceNo,
        double lng,
        double lat,
        double altitudeM,
        int dwellSec) {
}

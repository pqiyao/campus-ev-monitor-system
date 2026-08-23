package com.example.evmonitorplatform.service.drone;

public record DroneEvent(
        Long missionId,
        Long waypointId,
        int sequenceNo,
        String type,
        String detail) {
}

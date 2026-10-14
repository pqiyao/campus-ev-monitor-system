package com.example.evmonitorplatform.service.dispatch;

public record GroundRobotTelemetry(
        Long missionId,
        Long waypointId,
        int sequenceNo,
        double lng,
        double lat,
        double speedMps,
        double battery,
        double gpsQuality,
        String payloadState,
        String motionState,
        boolean waypointArrived) {
}

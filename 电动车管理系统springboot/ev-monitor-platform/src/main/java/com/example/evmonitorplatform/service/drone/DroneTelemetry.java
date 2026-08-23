package com.example.evmonitorplatform.service.drone;

public record DroneTelemetry(
        Long missionId,
        Long waypointId,
        int sequenceNo,
        double lng,
        double lat,
        double altitudeM,
        double speedMps,
        double battery,
        double gpsQuality,
        String payloadState,
        String flightState,
        boolean waypointArrived) {
}

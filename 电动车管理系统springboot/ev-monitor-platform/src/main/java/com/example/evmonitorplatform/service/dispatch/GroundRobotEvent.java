package com.example.evmonitorplatform.service.dispatch;

public record GroundRobotEvent(
        Long missionId,
        Long waypointId,
        int sequenceNo,
        String type,
        String detail) {
}

package com.example.evmonitorplatform.service.dispatch;

/**
 * 地面巡检车路径点。taskType: REVIEW(现场复核取证) / PATROL(常规巡查) / RETURN(返航)。
 */
public record GroundWaypoint(
        Long waypointId,
        int sequenceNo,
        double lng,
        double lat,
        int dwellSec,
        String taskType) {
}

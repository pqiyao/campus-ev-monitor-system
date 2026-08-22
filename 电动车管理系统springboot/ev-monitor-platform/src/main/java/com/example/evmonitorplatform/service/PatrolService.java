package com.example.evmonitorplatform.service;

import java.util.List;
import java.util.Map;

public interface PatrolService {
    Map<String, Object> createMission(Map<String, Object> request, Integer operatorUserId);
    Map<String, Object> getMission(Long missionId);
    List<Map<String, Object>> listMissions(String state, Integer limit);
    Map<String, Object> planMission(Long missionId, Integer operatorUserId);
    Map<String, Object> startMission(Long missionId, Integer operatorUserId);
    Map<String, Object> pauseMission(Long missionId, Integer operatorUserId);
    Map<String, Object> replanMission(Long missionId, String reason, Integer operatorUserId);
    Map<String, Object> appendTelemetry(Long missionId, Map<String, Object> telemetry);
    Map<String, Object> generateReport(Long missionId, Integer operatorUserId);
}

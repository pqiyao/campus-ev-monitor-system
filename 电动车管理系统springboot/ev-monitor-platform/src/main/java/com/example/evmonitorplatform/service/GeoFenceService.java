package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.entity.GeoFence;
import com.example.evmonitorplatform.entity.GeoFenceAlert;

import java.util.List;
import java.util.Map;

public interface GeoFenceService {

    // ---- 围栏 CRUD ----
    List<GeoFence> getAllEnabled();
    List<GeoFence> getAll();
    GeoFence create(GeoFence fence);
    boolean update(GeoFence fence);
    boolean delete(Integer fenceId);
    boolean toggleStatus(Integer fenceId);

    // ---- 核心：越界检测 + 写入预警 + 推 WebSocket ----
    List<GeoFenceAlert> checkAndAlert(Double lng, Double lat,
                                      Integer vehicleId, String licensePlate,
                                      Integer userId, String userName);

    // ---- 预警管理 ----
    List<GeoFenceAlert> getRecentAlerts(int limit);
    boolean processAlert(Integer alertId);
    int countUnprocessed();
    int countToday();

    // ---- 统计概览 ----
    Map<String, Object> getSummary();

    /** fenceType=5 为「允许停车区」：用于违停静置判定 */
    boolean isInsideAnyParkingZone(double lng, double lat);

    boolean hasParkingZoneConfigured();
}
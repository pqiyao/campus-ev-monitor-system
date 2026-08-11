// service/LocationService.java (接口)
package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.vo.request.SimPointRequest;
import com.example.evmonitorplatform.vo.response.VehicleLocationVO;

import java.util.List;
import java.util.Map;

public interface LocationService {
    void handleLocationMessage(String topic, String payload);
    List<VehicleLocationVO> getAllActiveLocations();
    VehicleLocationVO getLocation(String licensePlate);

    // =====================================================================
    // 获取某辆车的最新位置（Redis）
    // =====================================================================
    Map<String, Object> getVehicleLocation(Integer vehicleId);

    // =====================================================================
    // 获取所有在线车辆位置（Redis scan）
    // =====================================================================
    List<Map<String, Object>> getAllOnlineLocations();

    /** 批量写入屏幕点位（不校验车辆表，与真实点位一起在地图上展示） */
    int upsertSimLocations(List<SimPointRequest> points, Integer ttlMinutes);

    /** 清除全部屏幕点位 */
    void clearSimLocations();

    void clearVehicleLocation(Integer vehicleId);

    Map<String, Object> getParkingRuntimeConfig();

    Map<String, Object> updateParkingRuntimeConfig(Integer stationaryMinutes, Double moveThresholdMeters);
}

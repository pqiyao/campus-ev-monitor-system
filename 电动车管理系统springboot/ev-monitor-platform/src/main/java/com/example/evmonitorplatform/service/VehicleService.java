package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.vo.request.VehicleQueryVO;
import com.example.evmonitorplatform.vo.request.VehicleRegisterVO;
import java.util.List;
import java.util.Map;

public interface VehicleService extends IService<Vehicle> {
    
    boolean registerVehicle(VehicleRegisterVO registerVO);
    
    boolean approveVehicle(Integer vehicleId, Integer auditorId, String remark);
    
    boolean rejectVehicle(Integer vehicleId, Integer auditorId, String reason);
    
    boolean batchApprove(List<Integer> vehicleIds, Integer auditorId);
    
    boolean toggleFreeze(Integer vehicleId, boolean freeze);
    
    boolean toggleBlacklist(Integer vehicleId, boolean blacklist);
    
    PageResponse<List<Vehicle>> queryVehicles(VehicleQueryVO queryVO);
    
    Map<String, Object> getVehicleStats();
    
    boolean updateVehiclePhoto(Integer vehicleId, String photoUrl);
    
    List<Vehicle> searchVehicles(String plate, String brand, String color);
}
package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.entity.LocationDevice;

public interface LocationDeviceService extends IService<LocationDevice> {
    
    LocationDevice getByDeviceMac(String deviceMac);
    
    LocationDevice getByVehicleId(Integer vehicleId);
    
    boolean bindDevice(Integer vehicleId, String deviceMac);
    
    boolean unbindDevice(Integer vehicleId);
}

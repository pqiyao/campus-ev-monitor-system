package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.entity.LocationDevice;
import com.example.evmonitorplatform.mapper.LocationDeviceMapper;
import com.example.evmonitorplatform.service.LocationDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationDeviceServiceImpl extends ServiceImpl<LocationDeviceMapper, LocationDevice> implements LocationDeviceService {
    
    private final LocationDeviceMapper locationDeviceMapper;
    
    @Override
    public LocationDevice getByDeviceMac(String deviceMac) {
        if (deviceMac == null || deviceMac.isEmpty()) return null;
        return locationDeviceMapper.selectByDeviceMac(
                com.example.evmonitorplatform.service.impl.LocationServiceImpl.normalizeDeviceId(deviceMac));
    }
    
    @Override
    public LocationDevice getByVehicleId(Integer vehicleId) {
        return locationDeviceMapper.selectByVehicleId(vehicleId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindDevice(Integer vehicleId, String deviceMac) {
        String normalized = com.example.evmonitorplatform.service.impl.LocationServiceImpl.normalizeDeviceId(deviceMac);
        if (normalized.isEmpty()) {
            throw new BusinessException("设备MAC不能为空");
        }
        LocationDevice existing = locationDeviceMapper.selectByDeviceMac(normalized);
        if (existing != null) {
            if (Objects.equals(existing.getVehicleId(), vehicleId)) {
                return true;
            }
            throw new BusinessException("该设备已绑定到其他车辆");
        }

        LocationDevice device = new LocationDevice();
        device.setVehicleId(vehicleId);
        device.setDeviceNumber(normalized);
        device.setDeviceType("GPS");
        device.setStatus("active");
        device.setBatteryLevel(100);
        device.setLastReportTime(LocalDateTime.now());

        return locationDeviceMapper.insert(device) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindDevice(Integer vehicleId) {
        LocationDevice device = locationDeviceMapper.selectByVehicleId(vehicleId);
        if (device != null) {
            return locationDeviceMapper.deleteById(device.getDeviceId()) > 0;
        }
        return true;
    }
}

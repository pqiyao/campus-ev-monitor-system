package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.mapper.VehicleMapper;
import com.example.evmonitorplatform.service.LocationDeviceService;
import com.example.evmonitorplatform.service.VehicleService;
import com.example.evmonitorplatform.vo.request.VehicleQueryVO;
import com.example.evmonitorplatform.vo.request.VehicleRegisterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.evmonitorplatform.entity.LocationDevice;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl extends ServiceImpl<VehicleMapper, Vehicle> implements VehicleService {
    
    private final UserMapper userMapper;
    private final LocationDeviceService locationDeviceService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerVehicle(VehicleRegisterVO registerVO) {
        fillRelaxedVehicleFields(registerVO);
        log.info("车辆注册: {}", registerVO.getLicensePlate());

        User owner = userMapper.selectById(registerVO.getUserId());
        if (owner == null) {
            throw new BusinessException("车主用户不存在，请重新注册账号或刷新后重试");
        }
        
        // 检查车牌号是否已存在
        Long count = this.lambdaQuery()
                .eq(Vehicle::getLicensePlate, registerVO.getLicensePlate())
                .eq(Vehicle::getDeleted, false)
                .count();
        if (count > 0) {
            throw new BusinessException("车牌号已存在");
        }
        
        // 检查车架号是否已存在
        count = this.lambdaQuery()
                .eq(Vehicle::getFrameNum, registerVO.getFrameNum())
                .eq(Vehicle::getDeleted, false)
                .count();
        if (count > 0) {
            throw new BusinessException("车架号已存在");
        }

        if (StringUtils.hasText(registerVO.getDeviceMac())) {
            LocationDevice taken = locationDeviceService.getByDeviceMac(registerVO.getDeviceMac());
            if (taken != null && taken.getVehicleId() != null) {
                throw new BusinessException("该定位设备(MAC)已绑定其他车辆");
            }
        }

        Vehicle vehicle = new Vehicle();
        BeanUtils.copyProperties(registerVO, vehicle);
        vehicle.setStatus("pending");
        vehicle.setRegisterTime(LocalDateTime.now());
        vehicle.setIsFrozen(false);
        vehicle.setIsBlacklist(false);
        
        boolean saved = this.save(vehicle);

        if (saved && StringUtils.hasText(registerVO.getDeviceMac())) {
            locationDeviceService.unbindDevice(vehicle.getVehicleId());
            locationDeviceService.bindDevice(vehicle.getVehicleId(), registerVO.getDeviceMac());
        }

        return saved;
    }

    private void fillRelaxedVehicleFields(VehicleRegisterVO registerVO) {
        String suffix = (registerVO.getUserId() == null ? "0" : registerVO.getUserId()) + "-"
                + (System.currentTimeMillis() % 10000);
        if (!StringUtils.hasText(registerVO.getLicensePlate())) {
            registerVO.setLicensePlate("TEMP-" + suffix);
        } else {
            registerVO.setLicensePlate(registerVO.getLicensePlate().trim());
        }
        if (!StringUtils.hasText(registerVO.getFrameNum())) {
            registerVO.setFrameNum("TMPF-" + suffix);
        } else {
            registerVO.setFrameNum(registerVO.getFrameNum().trim());
        }
        if (!StringUtils.hasText(registerVO.getBrand())) {
            registerVO.setBrand("未填写");
        } else {
            registerVO.setBrand(registerVO.getBrand().trim());
        }
        if (!StringUtils.hasText(registerVO.getColor())) {
            registerVO.setColor("未填写");
        } else {
            registerVO.setColor(registerVO.getColor().trim());
        }
        if (registerVO.getBatteryType() != null) {
            registerVO.setBatteryType(registerVO.getBatteryType().trim());
        }
        if (registerVO.getVehicleType() != null) {
            registerVO.setVehicleType(registerVO.getVehicleType().trim());
        }
        if (registerVO.getDeviceMac() != null) {
            registerVO.setDeviceMac(registerVO.getDeviceMac().trim());
        }
        if (registerVO.getVehiclePhoto() != null) {
            registerVO.setVehiclePhoto(registerVO.getVehiclePhoto().trim());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveVehicle(Integer vehicleId, Integer auditorId, String remark) {
        Vehicle vehicle = this.getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        
        if (!"pending".equals(vehicle.getStatus())) {
            throw new BusinessException("车辆状态不正确");
        }
        
        vehicle.setStatus("approved");
        vehicle.setAuditUserId(auditorId);
        vehicle.setAuditTime(LocalDateTime.now());
        vehicle.setValidDate(LocalDateTime.now().plusYears(1).toLocalDate());
        
        return this.updateById(vehicle);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectVehicle(Integer vehicleId, Integer auditorId, String reason) {
        Vehicle vehicle = this.getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        
        if (!"pending".equals(vehicle.getStatus())) {
            throw new BusinessException("车辆状态不正确");
        }
        
        vehicle.setStatus("rejected");
        vehicle.setAuditUserId(auditorId);
        vehicle.setAuditTime(LocalDateTime.now());
        
        return this.updateById(vehicle);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchApprove(List<Integer> vehicleIds, Integer auditorId) {
        List<Vehicle> vehicles = this.listByIds(vehicleIds);
        LocalDateTime now = LocalDateTime.now();
        
        vehicles.forEach(vehicle -> {
            if ("pending".equals(vehicle.getStatus())) {
                vehicle.setStatus("approved");
                vehicle.setAuditUserId(auditorId);
                vehicle.setAuditTime(now);
                vehicle.setValidDate(now.plusYears(1).toLocalDate());
            }
        });
        
        return this.updateBatchById(vehicles);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFreeze(Integer vehicleId, boolean freeze) {
        Vehicle vehicle = this.getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        
        if (!"approved".equals(vehicle.getStatus())) {
            throw new BusinessException("只有已通过的车辆才能冻结");
        }
        
        vehicle.setIsFrozen(freeze);
        return this.updateById(vehicle);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleBlacklist(Integer vehicleId, boolean blacklist) {
        Vehicle vehicle = this.getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        
        vehicle.setIsBlacklist(blacklist);
        return this.updateById(vehicle);
    }
    
    @Override
    public PageResponse<List<Vehicle>> queryVehicles(VehicleQueryVO queryVO) {
        Page<Vehicle> page = new Page<>(queryVO.getPageNum(), queryVO.getPageSize());
        Page<Vehicle> resultPage = (Page<Vehicle>) baseMapper.selectVehiclePage(page, queryVO);
        
        // 补充车主信息
        resultPage.getRecords().forEach(vehicle -> {
            if (vehicle.getUserId() != null) {
                User user = userMapper.selectById(vehicle.getUserId());
                if (user != null) {
                    vehicle.setOwnerName(user.getRealName());
                    vehicle.setPhone(user.getPhone());
                }
            }
        });
        
        return new PageResponse<>(
            200,
            "success",
            resultPage.getRecords(),
            (int) resultPage.getCurrent(),
            (int) resultPage.getSize(),
            resultPage.getTotal()
        );
    }
    
    @Override
    public Map<String, Object> getVehicleStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", baseMapper.countTotal());
        stats.put("approved", baseMapper.countApproved());
        stats.put("pending", baseMapper.countPending());
        stats.put("blacklist", baseMapper.countBlacklist());
        stats.put("typeStats", baseMapper.countByUserType());
        
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVehiclePhoto(Integer vehicleId, String photoUrl) {
        Vehicle vehicle = this.getById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        vehicle.setVehiclePhoto(photoUrl);
        return this.updateById(vehicle);
    }
    
    @Override
    public List<Vehicle> searchVehicles(String plate, String brand, String color) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vehicle::getDeleted, false);
        
        if (plate != null && !plate.isEmpty()) {
            wrapper.like(Vehicle::getLicensePlate, plate);
        }
        if (brand != null && !brand.isEmpty()) {
            wrapper.like(Vehicle::getBrand, brand);
        }
        if (color != null && !color.isEmpty()) {
            wrapper.eq(Vehicle::getColor, color);
        }
        
        List<Vehicle> vehicles = this.list(wrapper);
        
        // 补充车主信息
        vehicles.forEach(vehicle -> {
            if (vehicle.getUserId() != null) {
                User user = userMapper.selectById(vehicle.getUserId());
                if (user != null) {
                    vehicle.setOwnerName(user.getRealName());
                    vehicle.setPhone(user.getPhone());
                }
            }
        });
        
        return vehicles;
    }
}

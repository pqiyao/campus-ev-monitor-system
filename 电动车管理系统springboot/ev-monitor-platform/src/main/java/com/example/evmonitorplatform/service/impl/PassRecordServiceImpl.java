package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.ETCard;
import com.example.evmonitorplatform.entity.PassRecord;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.mapper.ETCardMapper;
import com.example.evmonitorplatform.mapper.PassRecordMapper;
import com.example.evmonitorplatform.mapper.VehicleMapper;
import com.example.evmonitorplatform.service.PassRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassRecordServiceImpl extends ServiceImpl<PassRecordMapper, PassRecord> implements PassRecordService {

    private final ETCardMapper etcCardMapper;
    private final VehicleMapper vehicleMapper;

    @Override
    public void savePassRecord(PassRecord passRecord) {
        passRecord.setPassTime(LocalDateTime.now());
        passRecord.setCreateTime(LocalDateTime.now());
        passRecord.setDeleted(false);

        if (passRecord.getEtcNumber() != null && !passRecord.getEtcNumber().isEmpty()) {
            ETCard card = etcCardMapper.selectOne(
                new LambdaQueryWrapper<ETCard>()
                    .eq(ETCard::getEtcNumber, passRecord.getEtcNumber())
                    .eq(ETCard::getStatus, "normal")
            );

            if (card != null) {
                passRecord.setVehicleId(card.getVehicleId());
                if (card.getVehicleId() != null) {
                    Vehicle v = vehicleMapper.selectById(card.getVehicleId());
                    if (v != null) {
                        passRecord.setUserId(v.getUserId());
                        passRecord.setLicensePlate(v.getLicensePlate());
                    }
                }
            }
        }

        this.save(passRecord);
        log.info("通行记录已保存: {} {} {}", passRecord.getEtcNumber(), passRecord.getDirection(), passRecord.getPassResult());
    }

    @Override
    public PageResponse<List<PassRecord>> queryPassRecords(String keyword, String direction, String passResult,
                                              Integer vehicleId, Integer userId,
                                              Integer pageNum, Integer pageSize) {
        Page<PassRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PassRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PassRecord::getDeleted, false);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(PassRecord::getEtcNumber, keyword)
                .or().like(PassRecord::getLicensePlate, keyword)
            );
        }
        if (direction != null && !direction.isEmpty()) {
            wrapper.eq(PassRecord::getDirection, direction);
        }
        if (passResult != null && !passResult.isEmpty()) {
            wrapper.eq(PassRecord::getPassResult, passResult);
        }
        if (vehicleId != null) {
            wrapper.eq(PassRecord::getVehicleId, vehicleId);
        }
        if (userId != null) {
            wrapper.eq(PassRecord::getUserId, userId);
        }

        wrapper.orderByDesc(PassRecord::getPassTime);

        Page<PassRecord> result = this.page(page, wrapper);

        result.getRecords().forEach(record -> {
            if (record.getVehicleId() != null) {
                Vehicle vehicle = vehicleMapper.selectById(record.getVehicleId());
                if (vehicle != null) {
                    record.setOwnerName(vehicle.getUserId() != null ?
                        "用户" + vehicle.getUserId() : null);
                }
            }
        });

        return PageResponse.success(result.getRecords(), (int) result.getCurrent(), (int) result.getSize(), result.getTotal());
    }

    @Override
    public List<PassRecord> getRecentRecords(Integer limit) {
        return baseMapper.selectRecentRecords(limit);
    }

    @Override
    public Map<String, Object> getPassStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayTotal", baseMapper.countToday());
        stats.put("todayIn", baseMapper.countTodayIn());
        stats.put("todayOut", baseMapper.countTodayOut());
        stats.put("recentRecords", baseMapper.selectRecentRecords(10));
        return stats;
    }
}

package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.RectifyRecord;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.entity.Vehicle;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.mapper.RectifyRecordMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.mapper.VehicleMapper;
import com.example.evmonitorplatform.mapper.ViolationMapper;
import com.example.evmonitorplatform.service.LicensePointService;
import com.example.evmonitorplatform.service.ViolationService;
import com.example.evmonitorplatform.vo.request.ViolationQueryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;  // 新增导入
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationServiceImpl extends ServiceImpl<ViolationMapper, Violation> implements ViolationService {

    private final RectifyRecordMapper rectifyRecordMapper;
    private final JdbcTemplate jdbcTemplate;  // 新增 JdbcTemplate 注入
    private final LicensePointService licensePointService;
    private final VehicleMapper vehicleMapper;
    private final UserMapper userMapper;

    @Override
    public PageResponse<List<Violation>> queryViolations(ViolationQueryVO queryVO) {
        Page<Violation> page = new Page<>(queryVO.getPageNum(), queryVO.getPageSize());
        Page<Violation> resultPage = (Page<Violation>) baseMapper.selectViolationPage(page, queryVO);

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
    public PageResponse<List<RectifyRecord>> queryRectifyRecords(Integer verifyState, String licensePlate,
                                                                 Integer pageNum, Integer pageSize) {
        Page<RectifyRecord> page = new Page<>(pageNum, pageSize);
        Page<RectifyRecord> resultPage = (Page<RectifyRecord>) rectifyRecordMapper.selectRectifyListPage(
                page, verifyState, licensePlate);
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
    public Map<String, Object> resolveVehicleForReport(String studentId, String licensePlate, String frameNum) {
        String frame = frameNum == null ? "" : frameNum.trim();
        String plate = licensePlate == null ? "" : licensePlate.trim();
        String sid = studentId == null ? "" : studentId.trim();
        if (frame.isEmpty() && plate.isEmpty() && sid.isEmpty()) {
            throw new BusinessException("请填写学号、车牌号或车架号至少一项");
        }
        Vehicle vehicle = null;
        String matchBy;
        if (!frame.isEmpty()) {
            vehicle = vehicleMapper.selectByFrameNum(frame);
            matchBy = "frameNum";
            if (vehicle == null) {
                throw new BusinessException("未找到该车架号对应的车辆");
            }
        } else if (!plate.isEmpty()) {
            vehicle = vehicleMapper.selectByLicensePlate(plate);
            matchBy = "licensePlate";
            if (vehicle == null) {
                throw new BusinessException("未找到该车牌号对应的车辆");
            }
        } else {
            User user = userMapper.selectByStudentId(sid);
            if (user == null) {
                throw new BusinessException("未找到该学号对应的用户");
            }
            LambdaQueryWrapper<Vehicle> q = new LambdaQueryWrapper<Vehicle>()
                    .eq(Vehicle::getUserId, user.getUserId())
                    .orderByDesc(Vehicle::getVehicleId)
                    .last("LIMIT 5");
            List<Vehicle> list = vehicleMapper.selectList(q);
            if (list == null || list.isEmpty()) {
                throw new BusinessException("该学号用户下没有登记车辆");
            }
            vehicle = list.stream()
                    .filter(v -> "approved".equalsIgnoreCase(String.valueOf(v.getStatus())))
                    .findFirst()
                    .orElse(list.get(0));
            matchBy = "studentId";
        }
        User owner = userMapper.selectById(vehicle.getUserId());
        String ownerName = owner != null ? owner.getRealName() : null;
        Map<String, Object> m = new HashMap<>();
        m.put("vehicleId", vehicle.getVehicleId());
        m.put("userId", vehicle.getUserId());
        m.put("licensePlate", vehicle.getLicensePlate());
        m.put("frameNum", vehicle.getFrameNum());
        m.put("ownerName", ownerName);
        m.put("matchBy", matchBy);
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reportViolation(Violation violation) {
        if (violation == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (violation.getVehicleId() == null || violation.getUserId() == null) {
            throw new BusinessException("缺少 vehicleId 或 userId：请先在页面上用学号/车牌/车架号「查询并绑定车辆」后再提交");
        }
        Vehicle vehicle = vehicleMapper.selectById(violation.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException("车辆不存在（vehicleId 无效），请重新查询绑定");
        }
        if (vehicle.getUserId() == null || !vehicle.getUserId().equals(violation.getUserId())) {
            throw new BusinessException("用户与车辆不一致：该车绑定车主 userId 为 " + vehicle.getUserId()
                    + "，与提交的 userId 不符，请重新「查询并绑定车辆」");
        }
        violation.setIllegalState(0); // 待整改
        LocalDateTime now = LocalDateTime.now();
        // 库表 illegal_time 多为 NOT NULL；人工上报前端常不传，此前会导致插入失败并返回 400
        if (violation.getIllegalTime() == null) {
            violation.setIllegalTime(now);
        }
        violation.setCreateTime(now);
        if (violation.getPointsDeducted() == null) {
            violation.setPointsDeducted(0);
        }
        if (violation.getDeductionPoints() == null || violation.getDeductionPoints() < 0) {
            violation.setDeductionPoints(2);
        }
        if (violation.getIllegalSource() == null) {
            violation.setIllegalSource(2);
        }
        boolean ok = this.save(violation);
        if (ok) {
            licensePointService.applyDeductionForViolation(violation);
            this.updateById(violation);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminDeleteViolation(Integer illegalId, Integer operatorUserId, String reason) {
        Violation v = this.getById(illegalId);
        if (v == null) {
            throw new BusinessException("违规记录不存在");
        }
        licensePointService.restoreOnViolationDeletion(v, operatorUserId, reason);
        return this.removeById(illegalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startRectify(Integer violationId, Integer userId, String reason) {
        Violation violation = this.getById(violationId);
        if (violation == null) {
            throw new BusinessException("违规记录不存在");
        }

        if (violation.getIllegalState() != 0 && violation.getIllegalState() != 3) {
            throw new BusinessException("当前状态不能整改");
        }

        violation.setIllegalState(1); // 整改中
        this.updateById(violation);

        // 创建整改记录
        RectifyRecord record = new RectifyRecord();
        record.setIllegalId(violationId);
        record.setUserId(userId);
        record.setRectifyDesc(reason);
        record.setRectifyTime(LocalDateTime.now());
        record.setVerifyState(0); // 待核实

        return rectifyRecordMapper.insert(record) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitRectify(Integer rectifyId, String photos, String description) {
        RectifyRecord record = rectifyRecordMapper.selectById(rectifyId);
        if (record == null) {
            throw new BusinessException("整改记录不存在");
        }

        record.setRectifyPhoto(photos);
        record.setRectifyDesc(description);
        record.setRectifyTime(LocalDateTime.now());

        return rectifyRecordMapper.updateById(record) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyRectify(Integer rectifyId, Integer verifyUserId, boolean pass, String remark, String evidence) {
        RectifyRecord record = rectifyRecordMapper.selectById(rectifyId);
        if (record == null) {
            throw new BusinessException("整改记录不存在");
        }

        record.setVerifyState(pass ? 2 : 3);
        record.setVerifyUserId(verifyUserId);
        record.setVerifyRemark(remark);
        record.setVerifyEvidence(evidence);
        record.setVerifyTime(LocalDateTime.now());

        // 更新违规记录状态
        if (pass) {
            Violation violation = this.getById(record.getIllegalId());
            if (violation != null) {
                violation.setIllegalState(2); // 已整改
                this.updateById(violation);
            }
        }

        return rectifyRecordMapper.updateById(record) > 0;
    }

    @Override
    public Map<String, Object> getViolationStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", baseMapper.countTotal());
        stats.put("pendingRectify", baseMapper.countPendingRectify());
        stats.put("typeStats", baseMapper.countByType());
        stats.put("trendStats", baseMapper.countLast7Days());

        // 计算整改率
        Long total = Long.valueOf(baseMapper.countTotal());
        Long rectified = this.lambdaQuery()
                .eq(Violation::getIllegalState, 2)
                .count();
        double rectifyRate = total > 0 ? (double) rectified / total * 100 : 0;
        stats.put("rectifyRate", String.format("%.1f%%", rectifyRate));

        return stats;
    }

    @Override
    public Violation getViolationDetail(Integer violationId) {
        return baseMapper.selectWithVehicleInfo(violationId);
    }

    /**
     * 处理来自MQTT的违规上报消息
     */
    @Override
    public void handleMqttViolation(String payload) {
        log.info("收到MQTT违规消息: {}", payload);
        try {
            JSONObject data = JSON.parseObject(payload);
            Violation violation = new Violation();

            if (data.getInteger("vehicleId") != null) {
                violation.setVehicleId(data.getInteger("vehicleId"));
            }
            if (data.getInteger("userId") != null) {
                violation.setUserId(data.getInteger("userId"));
            }

            Integer illegalType = data.getInteger("illegalType");
            if (illegalType == null) {
                log.warn("MQTT违规消息缺少illegalType，忽略: {}", payload);
                return;
            }
            violation.setIllegalType(illegalType);

            if (data.getDouble("illegalLng") != null) {
                violation.setIllegalLng(data.getDouble("illegalLng"));
            }
            if (data.getDouble("illegalLat") != null) {
                violation.setIllegalLat(data.getDouble("illegalLat"));
            }

            String illegalTime = data.getString("illegalTime");
            if (illegalTime != null && !illegalTime.isEmpty()) {
                violation.setIllegalTime(
                        java.time.LocalDateTime.parse(
                                illegalTime,
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        )
                );
            } else {
                violation.setIllegalTime(java.time.LocalDateTime.now());
            }

            violation.setDeductionPoints(
                    data.getInteger("deductionPoints") != null
                            ? data.getInteger("deductionPoints") : 2
            );

            violation.setIllegalSource(
                    data.getInteger("illegalSource") != null
                            ? data.getInteger("illegalSource") : 1
            );

            violation.setIllegalState(0);
            this.save(violation);
            licensePointService.applyDeductionForViolation(violation);
            this.updateById(violation);
            log.info("MQTT违规记录已保存，illegalId={}, type={}", violation.getIllegalId(), illegalType);

        } catch (Exception e) {
            log.error("处理MQTT违规消息异常", e);
        }
    }

    // ===== 新增方法实现 =====
    @Override
    public List<Map<String, Object>> getViolationTypes() {
        String sql = "SELECT type_id AS value, type_name AS label, deduction_points, description, penalty_amount, appeal_deadline " +
                "FROM violation_type ORDER BY type_id";
        return jdbcTemplate.queryForList(sql);
    }
}
package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.RectifyRecord;
import com.example.evmonitorplatform.entity.Violation;
import com.example.evmonitorplatform.vo.request.ViolationQueryVO;
import java.util.List;
import java.util.Map;

public interface ViolationService extends IService<Violation> {

    PageResponse<List<Violation>> queryViolations(ViolationQueryVO queryVO);

    PageResponse<List<RectifyRecord>> queryRectifyRecords(Integer verifyState, String licensePlate,
                                                          Integer pageNum, Integer pageSize);

    boolean reportViolation(Violation violation);

    /**
     * 人工上报前：按学号 / 车牌号 / 车架号（可填多项，优先车架号 → 车牌 → 学号）解析车辆与车主。
     */
    Map<String, Object> resolveVehicleForReport(String studentId, String licensePlate, String frameNum);

    boolean startRectify(Integer violationId, Integer userId, String reason);

    boolean submitRectify(Integer rectifyId, String photos, String description);

    boolean verifyRectify(Integer rectifyId, Integer verifyUserId, boolean pass, String remark, String evidence);

    Map<String, Object> getViolationStats();

    Violation getViolationDetail(Integer violationId);

    void handleMqttViolation(String payload);

    /** 管理端删除违规（软删），并恢复当时扣除的记分 */
    boolean adminDeleteViolation(Integer illegalId, Integer operatorUserId, String reason);

    // ===== 新增方法 =====
    List<Map<String, Object>> getViolationTypes();
}
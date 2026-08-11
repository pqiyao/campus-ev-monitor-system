package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.AppealRecord;
import com.example.evmonitorplatform.vo.request.AppealAuditVO;
import com.example.evmonitorplatform.vo.request.AppealQueryVO;
import com.example.evmonitorplatform.vo.request.AppealSubmitVO;
import java.util.List;
import java.util.Map;

/**
 * 申诉服务接口
 */
public interface AppealService extends IService<AppealRecord> {

    /**
     * 提交申诉
     * @param submitVO 申诉提交参数
     * @return 是否成功
     */
    boolean submitAppeal(AppealSubmitVO submitVO);

    /**
     * 审核申诉
     * @param auditVO 申诉审核参数
     * @return 是否成功
     */
    boolean auditAppeal(AppealAuditVO auditVO);

    /**
     * 撤销申诉
     * @param appealId 申诉ID
     * @param userId 用户ID
     * @param reason 撤销原因
     * @return 是否成功
     */
    boolean cancelAppeal(Integer appealId, Integer userId, String reason);

    /**
     * 获取申诉详情
     * @param appealId 申诉ID
     * @return 申诉详情
     */
    AppealRecord getAppealDetail(Integer appealId);

    /**
     * 分页查询申诉记录
     * @param queryVO 查询参数
     * @return 分页结果
     */
    PageResponse<List<AppealRecord>> queryAppeals(AppealQueryVO queryVO);

    /**
     * 获取用户申诉列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResponse<List<AppealRecord>> getUserAppeals(Integer userId, Integer pageNum, Integer pageSize);

    /**
     * 获取待审核申诉列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResponse<List<AppealRecord>> getPendingAppeals(Integer pageNum, Integer pageSize);

    /**
     * 获取申诉统计信息
     * @return 统计信息
     */
    Map<String, Object> getAppealStats();

    /**
     * 获取用户申诉统计
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getUserAppealStats(Integer userId);

    /**
     * 获取申诉仪表盘数据
     * @return 仪表盘数据
     */
    Map<String, Object> getAppealDashboard();

    /**
     * 导出申诉记录
     * @param queryVO 查询参数
     * @return 导出数据
     */
    List<AppealRecord> exportAppeals(AppealQueryVO queryVO);

    /**
     * 批量审核申诉
     * @param appealIds 申诉ID列表
     * @param auditUserId 审核用户ID
     * @param result 审核结果
     * @param remark 备注
     * @return 成功数量
     */
    int batchAuditAppeals(List<Integer> appealIds, Integer auditUserId, Integer result, String remark);

    /**
     * 获取申诉处理时效统计
     * @param days 天数
     * @return 时效统计
     */
    Map<String, Object> getAppealEfficiencyStats(Integer days);

    /**
     * 检查是否可以申诉
     * @param illegalId 违规ID
     * @param userId 用户ID
     * @return 检查结果
     */
    Map<String, Object> checkAppealable(Integer illegalId, Integer userId);

    /**
     * 更新申诉状态
     * @param appealId 申诉ID
     * @param state 状态
     * @param remark 备注
     * @return 是否成功
     */
    boolean updateAppealState(Integer appealId, Integer state, String remark);

    /**
     * 重新提交申诉
     * @param appealId 原申诉ID
     * @param submitVO 申诉提交参数
     * @return 是否成功
     */
    boolean resubmitAppeal(Integer appealId, AppealSubmitVO submitVO);
}
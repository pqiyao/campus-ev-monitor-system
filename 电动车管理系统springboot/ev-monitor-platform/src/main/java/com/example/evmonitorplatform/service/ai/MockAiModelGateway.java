package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.vo.response.AiModelDecision;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class MockAiModelGateway implements AiModelGateway {

    @Override
    public AiModelDecision analyze(AiEvent event) {
        long started = System.nanoTime();
        String type = event.getEventType().toUpperCase(Locale.ROOT);
        AiModelDecision decision = new AiModelDecision();
        decision.setValid(true);
        decision.setEventType(type);
        decision.setIdentityReliable(event.getVehicleId() != null && event.getUserId() != null);
        decision.setEvidenceSufficient(event.getEvidenceUrls() != null && !event.getEvidenceUrls().isBlank());
        decision.setProvider("mock");
        decision.setModelName("mock-governance-v1");

        switch (type) {
            case "OVERLOAD" -> fill(decision, 0.93, "HIGH", "检测到电动车存在违规载人行为",
                    "车辆与乘员目标在连续画面中保持稳定关联", "建议生成高优先级复核任务");
            case "ILLEGAL_PARKING" -> fill(decision, 0.86, "MEDIUM", "车辆在非停车区域持续停留",
                    "位置与禁停区域重叠且停留时间超过阈值", "建议人工复核停车时长和围栏范围");
            case "THEFT_RISK" -> fill(decision, 0.97, "CRITICAL", "车辆出现异常移动，存在疑似被盗风险",
                    "布防状态、移动轨迹与正常通行记录不一致", "建议立即告警并启动轨迹关注");
            case "RESTRICTED_AREA" -> fill(decision, 0.92, "HIGH", "车辆进入校园禁行区域",
                    "轨迹穿越启用中的禁行围栏", "建议生成复核任务并通知车主驶离");
            default -> fill(decision, 0.95, "MEDIUM", "检测到驾驶人员疑似未佩戴安全头盔",
                    "连续关键帧均未发现有效头盔目标", "建议生成待确认治理事件");
        }

        decision.setNotificationText("系统检测到您的车辆可能存在" + displayType(type)
                + "行为，请及时查看事件详情并按要求整改。");
        decision.setRecommendedAction("CREATE_REVIEW_TASK");
        decision.setLatencyMs((int) ((System.nanoTime() - started) / 1_000_000L) + 180);
        decision.setRawResponse("mock:" + type);
        return decision;
    }

    private void fill(AiModelDecision decision, double confidence, String riskLevel,
                      String summary, String observation, String recommendation) {
        decision.setConfidence(confidence);
        decision.setRiskLevel(riskLevel);
        decision.setSummary(summary);
        decision.setObservations(List.of(observation, recommendation));
    }

    private String displayType(String type) {
        return switch (type) {
            case "OVERLOAD" -> "违规载人";
            case "ILLEGAL_PARKING" -> "违规停车";
            case "THEFT_RISK" -> "异常移动";
            case "RESTRICTED_AREA" -> "禁区通行";
            default -> "未佩戴头盔";
        };
    }
}

package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.vo.response.AiModelDecision;
import com.example.evmonitorplatform.vo.response.AiRuleDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiDecisionRuleService {

    private final AiGovernanceProperties properties;

    public AiRuleDecision decide(AiEvent event, AiModelDecision model) {
        List<String> hits = new ArrayList<>();
        if (!model.isValid()) {
            hits.add("AI判定事件无效");
            return new AiRuleDecision("REJECTED", "事件未通过AI有效性校验", hits);
        }
        if (model.getConfidence() < properties.getReviewThreshold()) {
            hits.add("AI置信度低于人工复核阈值");
            return new AiRuleDecision("REJECTED", "低置信度事件自动驳回并保留样本", hits);
        }

        boolean identityOk = event.getVehicleId() != null
                && event.getUserId() != null
                && event.getIdentityConfidence() != null
                && event.getIdentityConfidence() >= properties.getIdentityThreshold()
                && model.isIdentityReliable();
        boolean confidenceOk = model.getConfidence() >= properties.getAutoAcceptThreshold();
        boolean evidenceOk = model.isEvidenceSufficient();

        hits.add(identityOk ? "车辆与用户身份匹配可信" : "身份匹配需要人工确认");
        hits.add(confidenceOk ? "AI置信度达到自动受理阈值" : "AI置信度达到复核阈值");
        hits.add(evidenceOk ? "事件证据完整" : "事件证据需要补充");

        if (identityOk && confidenceOk && evidenceOk) {
            return new AiRuleDecision("AUTO_ACCEPTED", "事件自动受理，等待管理员最终确认", hits);
        }
        return new AiRuleDecision("PENDING_REVIEW", "事件进入人工复核队列", hits);
    }
}

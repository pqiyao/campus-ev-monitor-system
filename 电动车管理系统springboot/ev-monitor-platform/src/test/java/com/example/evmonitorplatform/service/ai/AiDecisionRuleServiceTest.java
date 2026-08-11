package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.config.AiGovernanceProperties;
import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.vo.response.AiModelDecision;
import com.example.evmonitorplatform.vo.response.AiRuleDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiDecisionRuleServiceTest {
    private AiDecisionRuleService service;
    private AiEvent event;
    private AiModelDecision model;

    @BeforeEach
    void setUp() {
        AiGovernanceProperties properties = new AiGovernanceProperties();
        service = new AiDecisionRuleService(properties);
        event = new AiEvent();
        event.setVehicleId(10);
        event.setUserId(20);
        event.setIdentityConfidence(0.98D);
        model = new AiModelDecision();
        model.setValid(true);
        model.setIdentityReliable(true);
        model.setEvidenceSufficient(true);
        model.setConfidence(0.95D);
    }

    @Test
    void acceptsHighConfidenceCompleteEvidence() {
        AiRuleDecision result = service.decide(event, model);
        assertEquals("AUTO_ACCEPTED", result.getDecisionCode());
    }

    @Test
    void sendsUnreliableIdentityToManualReview() {
        event.setIdentityConfidence(0.40D);
        AiRuleDecision result = service.decide(event, model);
        assertEquals("PENDING_REVIEW", result.getDecisionCode());
    }

    @Test
    void rejectsLowConfidenceEvent() {
        model.setConfidence(0.60D);
        AiRuleDecision result = service.decide(event, model);
        assertEquals("REJECTED", result.getDecisionCode());
    }
}

package com.example.evmonitorplatform.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AiRuleDecision {
    private String decisionCode;
    private String explanation;
    private List<String> ruleHits;
}

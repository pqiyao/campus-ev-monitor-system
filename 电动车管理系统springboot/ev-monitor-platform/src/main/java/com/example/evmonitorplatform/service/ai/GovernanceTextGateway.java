package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.vo.response.AiTextResult;

public interface GovernanceTextGateway {
    AiTextResult complete(String instructions, String prompt);
}

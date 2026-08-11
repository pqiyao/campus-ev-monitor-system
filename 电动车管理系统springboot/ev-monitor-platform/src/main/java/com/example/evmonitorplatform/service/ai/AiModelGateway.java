package com.example.evmonitorplatform.service.ai;

import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.vo.response.AiModelDecision;

public interface AiModelGateway {
    AiModelDecision analyze(AiEvent event);
}

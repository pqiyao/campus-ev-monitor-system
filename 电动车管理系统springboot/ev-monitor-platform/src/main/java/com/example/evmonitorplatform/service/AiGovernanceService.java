package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.entity.AiEvent;
import com.example.evmonitorplatform.vo.request.AiDemoRunRequest;
import com.example.evmonitorplatform.vo.request.AiEventCreateRequest;
import com.example.evmonitorplatform.vo.request.AiIncidentReviewRequest;

import java.util.List;
import java.util.Map;

public interface AiGovernanceService {
    Map<String, Object> runDemo(AiDemoRunRequest request);

    Map<String, Object> ingest(AiEventCreateRequest request);

    List<Map<String, Object>> listEvents(String status, String eventType, Integer limit);

    Map<String, Object> getEventDetail(Long eventId);

    Map<String, Object> analyze(Long eventId);

    Map<String, Object> confirm(Long incidentId, Integer operatorUserId, AiIncidentReviewRequest request);

    Map<String, Object> reject(Long incidentId, Integer operatorUserId, AiIncidentReviewRequest request);

    Map<String, Object> stats();
}

package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.vo.request.AiAssistantRequest;
import com.example.evmonitorplatform.vo.request.AiReportRequest;
import com.example.evmonitorplatform.vo.request.PolicyDocumentRequest;

import java.util.List;
import java.util.Map;

public interface AiIntelligenceService {
    Map<String, Object> chat(AiAssistantRequest request, Integer operatorUserId);
    List<Map<String, Object>> getSessionMessages(String sessionId, Integer operatorUserId);
    List<Map<String, Object>> listPolicies(String keyword);
    Map<String, Object> createPolicy(PolicyDocumentRequest request);
    List<Map<String, Object>> correlateEvent(Long eventId);
    List<Map<String, Object>> listCorrelations(Integer limit);
    Map<String, Object> reviewAppeal(Integer appealId);
    Map<String, Object> generateReport(AiReportRequest request, Integer operatorUserId);
    List<Map<String, Object>> listReports(Integer limit);
}

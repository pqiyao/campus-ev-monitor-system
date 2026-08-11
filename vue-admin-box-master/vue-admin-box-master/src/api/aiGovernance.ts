import request from '@/utils/system/request'

export function getAiConfig() { return request({ url: '/ai/config', method: 'get', baseURL: '/api' }) }
export function getAiStats() { return request({ url: '/ai/stats', method: 'get', baseURL: '/api' }) }
export function getAiEvents(params: Record<string, unknown> = {}) { return request({ url: '/ai/events', method: 'get', params, baseURL: '/api' }) }
export function getAiEventDetail(eventId: number) { return request({ url: `/ai/events/${eventId}`, method: 'get', baseURL: '/api' }) }
export function runAiDemo(data: object) { return request({ url: '/ai/demo/run', method: 'post', data, baseURL: '/api', timeout: 60000 }) }
export function analyzeAiEvent(eventId: number) { return request({ url: `/ai/events/${eventId}/analyze`, method: 'post', baseURL: '/api', timeout: 60000 }) }
export function confirmAiIncident(incidentId: number, data: object) { return request({ url: `/ai/incidents/${incidentId}/confirm`, method: 'post', data, baseURL: '/api' }) }
export function rejectAiIncident(incidentId: number, data: object) { return request({ url: `/ai/incidents/${incidentId}/reject`, method: 'post', data, baseURL: '/api' }) }

export function askGovernanceAssistant(data: object) { return request({ url: '/ai/intelligence/assistant/chat', method: 'post', data, baseURL: '/api', timeout: 60000 }) }
export function getAssistantMessages(sessionId: string) { return request({ url: `/ai/intelligence/assistant/sessions/${sessionId}/messages`, method: 'get', baseURL: '/api' }) }
export function getPolicyKnowledge(params: Record<string, unknown> = {}) { return request({ url: '/ai/intelligence/policies', method: 'get', params, baseURL: '/api' }) }
export function createPolicyKnowledge(data: object) { return request({ url: '/ai/intelligence/policies', method: 'post', data, baseURL: '/api' }) }
export function correlateAiEvent(eventId: number) { return request({ url: `/ai/intelligence/events/${eventId}/correlate`, method: 'post', baseURL: '/api' }) }
export function getEventCorrelations(params: Record<string, unknown> = {}) { return request({ url: '/ai/intelligence/correlations', method: 'get', params, baseURL: '/api' }) }
export function reviewAppealByAi(appealId: number) { return request({ url: `/ai/intelligence/appeals/${appealId}/review`, method: 'post', baseURL: '/api', timeout: 60000 }) }
export function generateGovernanceReport(data: object = {}) { return request({ url: '/ai/intelligence/reports', method: 'post', data, baseURL: '/api', timeout: 60000 }) }
export function getGovernanceReports(params: Record<string, unknown> = {}) { return request({ url: '/ai/intelligence/reports', method: 'get', params, baseURL: '/api' }) }

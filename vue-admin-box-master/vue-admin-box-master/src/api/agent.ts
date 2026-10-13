import request from '@/utils/request'

export interface AgentStatus {
  state?: string
  enabled?: boolean
  baseUrl?: string
  profile?: string
  integration?: string
  dataSources?: string[]
  writeBackPolicy?: string
}

export const getAgentStatus = () => request.get('/agent/status') as Promise<AgentStatus>
export const getAgentContext = (limit = 30) => request.get('/agent/context', { params: { limit } })
export const runAgent = (payload: { goal: string; sessionId?: string; preset?: string; limit?: number }) => request.post('/agent/runs', payload)
export const getAgentHistory = (sessionId: string) => request.get(`/agent/sessions/${encodeURIComponent(sessionId)}/history`)

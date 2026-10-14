import request from '@/utils/request'
import { getToken } from '@/utils/auth'

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
export const listAgentSessions = (limit = 50) => request.get('/agent/sessions', { params: { limit } })
export const deleteAgentSession = (sessionId: string) => request.delete(`/agent/sessions/${encodeURIComponent(sessionId)}`)

// 复用治理工作台的 KPI 统计接口
export const getAiStats = () => request.get('/ai/stats')

// ---------- SSE streaming (native fetch — axios does not support streaming) ----------

export interface StreamCallbacks {
  onContext?: (ctx: any) => void
  onContent?: (textDelta: string) => void
  onToolCallStart?: (name: string, args: any) => void
  onToolCallResult?: (name: string, result: string) => void
  onDone?: (envelope: any) => void
  onError?: (message: string) => void
}

/**
 * POST /api/agent/runs/stream — server-sent events.
 * Parses `event:` / `data:` frames and dispatches to the matching callback.
 * Returns an abort function so the caller can cancel mid-flight.
 */
export function runAgentStream(
  payload: { goal: string; sessionId?: string; preset?: string; limit?: number },
  callbacks: StreamCallbacks,
): () => void {
  const controller = new AbortController()
  const { signal } = controller

  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  // Vite dev proxy strips the /api prefix? No — proxy target is http://localhost:8083,
  // so /api/agent/runs/stream goes through as-is. Prod build: same pattern.
  const url = '/api/agent/runs/stream'

    ; (async () => {
      let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
      let buffer = ''
      try {
        const resp = await fetch(url, {
          method: 'POST',
          headers,
          body: JSON.stringify(payload),
          signal,
        })
        if (!resp.ok || !resp.body) {
          const text = await resp.text().catch(() => '')
          callbacks.onError?.(`HTTP ${resp.status} ${text.slice(0, 200)}`)
          return
        }
        reader = resp.body.getReader()
        const decoder = new TextDecoder('utf-8')
        while (true) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          // split SSE frames by blank line
          let sepIdx: number
          while ((sepIdx = buffer.indexOf('\n\n')) !== -1) {
            const raw = buffer.slice(0, sepIdx)
            buffer = buffer.slice(sepIdx + 2)
            processSseFrame(raw.trim(), callbacks)
          }
        }
        if (buffer.trim()) processSseFrame(buffer.trim(), callbacks)
      } catch (err: any) {
        if (err?.name === 'AbortError') return
        callbacks.onError?.(err?.message || String(err))
      } finally {
        try { reader?.releaseLock() } catch { }
      }
    })()

  return () => controller.abort()
}

function processSseFrame(raw: string, cb: StreamCallbacks) {
  const lines = raw.split(/\r?\n/)
  let event = 'message'
  const dataLines: string[] = []
  for (const line of lines) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5))
  }
  const dataStr = dataLines.join('\n').trim()
  if (!dataStr) return
  if (event === 'context') {
    try { cb.onContext?.(JSON.parse(dataStr)) } catch { }
  } else if (event === 'content') {
    try {
      const obj = JSON.parse(dataStr)
      if (typeof obj.text === 'string') cb.onContent?.(obj.text)
    } catch { }
  } else if (event === 'tool_call_start') {
    try {
      const obj = JSON.parse(dataStr)
      cb.onToolCallStart?.(obj.name || 'unknown', obj.args || {})
    } catch { }
  } else if (event === 'tool_call_result') {
    try {
      const obj = JSON.parse(dataStr)
      cb.onToolCallResult?.(obj.name || 'unknown', String(obj.result || ''))
    } catch { }
  } else if (event === 'done') {
    try { cb.onDone?.(JSON.parse(dataStr)) } catch { }
  } else if (event === 'error') {
    try { cb.onError?.(JSON.parse(dataStr).message || dataStr) } catch { cb.onError?.(dataStr) }
  }
}

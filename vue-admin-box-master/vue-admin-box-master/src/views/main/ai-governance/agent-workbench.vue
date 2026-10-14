<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <aside class="chat-sidebar">
      <div class="sidebar-head">
        <div class="brand">
          <div class="brand-dot"></div>
          <div class="brand-text">
            <strong>校园智能体</strong>
            <small>DeepSeek Harness Runtime</small>
          </div>
        </div>
        <button class="new-chat-btn" @click="newChat" title="新建对话">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
          新建对话
        </button>
      </div>

      <div class="session-list">
        <div class="session-list-label">历史会话（本地）</div>
        <div
          v-for="s in sessions"
          :key="s.sessionId"
          class="session-item"
          :class="{ active: s.sessionId === currentSessionId }"
          @click="switchSession(s.sessionId)"
        >
          <div class="session-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </div>
          <div class="session-body">
            <div class="session-title">{{ s.title }}</div>
            <div class="session-time">{{ formatTime(s.updatedAt) }}</div>
          </div>
          <button class="session-del" @click.stop="deleteSession(s.sessionId)" title="删除">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
          </button>
        </div>
        <div v-if="sessions.length === 0" class="session-empty">暂无历史对话</div>
      </div>

      <div class="sidebar-foot">
        <div class="runtime-pill" :class="stateClass">
          <span class="dot"></span>
          {{ statusStateText }}
        </div>
      </div>
    </aside>

    <!-- 中间对话主区 -->
    <main class="chat-main">
      <!-- 顶栏 -->
      <header class="chat-header">
        <div class="chat-title">
          <strong>校园电动车时空治理智能体</strong>
          <span class="subtitle">基于平台多源只读事实推理 · 工具可行动</span>
        </div>
        <div class="header-right">
          <span v-if="status.baseUrl" class="baseurl-tag">{{ status.baseUrl }}</span>
          <button class="drawer-toggle" :class="{ active: drawerVisible }" @click="drawerVisible = !drawerVisible" title="证据与轨迹">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            {{ drawerVisible ? '隐藏证据' : '查看证据' }}
          </button>
        </div>
      </header>

      <!-- KPI 仪表盘（原治理工作台关键指标融合进来） -->
      <section class="stats-bar">
        <div v-for="c in kpiCards" :key="c.key" class="kpi-card" :class="c.tone">
          <div class="kpi-label">{{ c.label }}</div>
          <div class="kpi-value">{{ c.value }}</div>
        </div>
        <div class="kpi-refresh" @click="loadStats" title="刷新">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10"/><path d="M20.49 15A9 9 0 0 1 5.64 18.36L1 14"/></svg>
        </div>
      </section>

      <!-- 空态 -->
      <div v-if="messages.length === 0" class="chat-welcome">
        <div class="welcome-orb"></div>
        <h2>让我帮你读懂校园里的每一台电动车</h2>
        <p>我可以调用摄像头、车辆定位、电子围栏、AI 事件和历史违规数据，帮你分析风险、排序巡检、生成确认清单。</p>
        <div class="quick-prompts">
          <button
            v-for="q in quickPrompts"
            :key="q.text"
            class="prompt-chip"
            @click="sendPrompt(q.text)"
          >
            <span class="chip-icon">{{ q.icon }}</span>
            <span>{{ q.text }}</span>
          </button>
        </div>
      </div>

      <!-- 消息区 -->
      <div v-else class="chat-messages" ref="messagesRef">
        <div v-for="(msg, idx) in messages" :key="idx" class="chat-bubble" :class="msg.role">
          <div class="bubble-avatar">
            <template v-if="msg.role === 'user'">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </template>
            <template v-else>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><circle cx="15.5" cy="15.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
            </template>
          </div>
          <div class="bubble-body">
            <!-- Tool-call execution trace (ReAct) -->
            <div v-if="msg.toolCalls?.length" class="tool-trace">
              <div
                v-for="(tc, idx) in msg.toolCalls"
                :key="idx"
                class="tool-card"
                :class="{ running: tc.status === 'running', done: tc.status === 'done' }"
              >
                <div class="tool-card-head">
                  <span class="tool-icon">🔧</span>
                  <span class="tool-name">{{ tc.name }}</span>
                  <span v-if="tc.status === 'running'" class="tool-status running">执行中…</span>
                  <span v-else class="tool-status done">✓ {{ summarize(tc.result) }}</span>
                </div>
                <details class="tool-details">
                  <summary>参数</summary>
                  <pre>{{ JSON.stringify(tc.args, null, 2) }}</pre>
                </details>
                <details v-if="tc.result" class="tool-details">
                  <summary>返回结果</summary>
                  <pre>{{ truncate(tc.result, 2000) }}</pre>
                </details>
              </div>
            </div>
            <div
              class="bubble-text markdown-body"
              :class="{ 'is-user': msg.role === 'user' }"
              v-html="renderMarkdown(msg.content)"
            ></div>
            <div class="bubble-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>

        <!-- 运行中 typing 动画 -->
        <div v-if="running" class="chat-bubble assistant">
          <div class="bubble-avatar">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><circle cx="15.5" cy="15.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
          </div>
          <div class="bubble-body typing">
            <span class="typing-dots"><i></i><i></i><i></i></span>
            <small>智能体正在读取多源事实并推理…</small>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-input-wrap">
        <div class="chat-input-box">
          <textarea
            v-model="inputText"
            class="chat-input"
            rows="1"
            placeholder="输入你的任务目标，例如：分析近7天高风险区域的巡检优先级…（Enter 发送，Shift+Enter 换行）"
            :disabled="running"
            @keydown="handleKeydown"
            @input="autoResize"
            ref="inputRef"
          ></textarea>
          <button class="send-btn" :disabled="!inputText.trim() || running" :class="{ active: inputText.trim() && !running }" @click="submitRun">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
          </button>
        </div>
        <div class="input-foot">
          <span v-if="running" class="running-hint">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" class="spin"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
            正在推理（后端 queue 模式，多源事实已注入 Prompt）
          </span>
          <span v-else class="idle-hint">只读分析 · 不会直接处罚、发通知或修改申诉</span>
        </div>
      </div>
    </main>

    <!-- 右侧证据抽屉 -->
    <aside class="chat-drawer" :class="{ show: drawerVisible }">
      <div class="drawer-head">
        <strong>多源证据 & 运行轨迹</strong>
        <button class="drawer-close" @click="drawerVisible = false">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
      </div>

      <div class="drawer-section">
        <div class="section-title">数据源概览</div>
        <div class="source-grid">
          <div v-for="s in sourceCounts" :key="s.key" class="source-item">
            <div class="source-icon">{{ s.icon }}</div>
            <div>
              <div class="source-value">{{ s.value }}</div>
              <div class="source-label">{{ s.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="drawer-section">
        <div class="section-title">融合处理链路</div>
        <div class="pipeline">
          <span v-for="(step, i) in pipeline" :key="step" class="pipeline-step">
            <b>{{ i + 1 }}</b>{{ step }}
          </span>
        </div>
      </div>

      <div class="drawer-section">
        <div class="section-title">本次运行轨迹 <small v-if="trace.length">({{ trace.length }} 事件)</small></div>
        <div class="trace-list" v-if="trace.length">
          <div v-for="(t, i) in trace" :key="i" class="trace-row" :class="traceClass(t.type)">
            <span class="trace-type">{{ shortenType(t.type) }}</span>
            <span v-if="t.tool" class="trace-detail">tool: {{ t.tool }}</span>
            <span v-else-if="t.step" class="trace-detail">{{ t.step }}</span>
            <span v-else-if="t.turn" class="trace-detail">turn #{{ t.turn }}</span>
            <span class="trace-time">{{ formatTime(t.time) }}</span>
          </div>
        </div>
        <div v-else class="trace-empty">运行完成后，这里会显示 DSH 内部的 step/tool 调用轨迹。</div>
      </div>

      <div class="drawer-section">
        <div class="section-title">运行时</div>
        <dl class="runtime-dl">
          <div><dt>状态</dt><dd :class="stateClass">{{ status.state || '—' }}</dd></div>
          <div><dt>BaseURL</dt><dd class="mono">{{ status.baseUrl || '—' }}</dd></div>
          <div><dt>当前会话</dt><dd class="mono">{{ currentSessionId || '—' }}</dd></div>
          <div><dt>数据源</dt><dd>{{ status.dataSources?.join('、') || '—' }}</dd></div>
          <div><dt>写回策略</dt><dd>{{ status.writeBackPolicy || '—' }}</dd></div>
        </dl>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAgentContext, getAgentHistory, getAgentStatus, getAiStats, listAgentSessions, runAgentStream } from '@/api/agent'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// ---- configure marked for clean GFM rendering ----
marked.setOptions({
  breaks: true,
  gfm: true,
})

// incremental render cache keyed by content string — streaming keeps growing it
const markdownCache = new Map<string, string>()
function renderMarkdown(content: string): string {
  if (!content) return ''
  const cached = markdownCache.get(content)
  if (cached != null) return cached
  try {
    // marked parses markdown → HTML, DOMPurify strips any dangerous nodes
    const raw = marked.parse(content, { async: false }) as string
    const safe = DOMPurify.sanitize(raw, { ADD_ATTR: ['target'] })
    markdownCache.set(content, safe)
    // keep cache bounded — SSE produces many intermediate strings we don't need
    if (markdownCache.size > 128) {
      const firstKey = markdownCache.keys().next().value
      markdownCache.delete(firstKey)
    }
    return safe
  } catch {
    return ''
  }
}

// ============ 状态 ============
interface ChatMsg { role: 'user' | 'assistant'; content: string; seq?: any; time?: any }
interface SessionMeta { sessionId: string; title: string; updatedAt: number }
interface TraceEvent { type: string; seq?: any; time?: any; step?: any; turn?: any; tool?: any }
interface ContextSnapshot { events?: any[]; locations?: any[]; cameras?: any[]; geofences?: any[]; patrolMissions?: any[]; droneTelemetry?: any[]; violations?: any[]; fusions?: any[]; fenceAlerts?: any[]; fusionSummary?: { pipeline?: string[]; recentEvidence?: any[] } }

const LS_KEY = 'ev-agent-sessions-v1'
const DEFAULT_PRESET = 'campus-spatiotemporal-agent'
const DEFAULT_CONTEXT_LIMIT = 30

const status = ref<Record<string, any>>({})
const context = ref<ContextSnapshot>({})
const messages = ref<ChatMsg[]>([])
const trace = ref<TraceEvent[]>([])
const inputText = ref('')
const running = ref(false)
const currentSessionId = ref('')
const drawerVisible = ref(true)
const sessions = ref<SessionMeta[]>([])

// KPI 仪表盘（融合自治理工作台）
const kpiStats = reactive({ total: 0, pendingReview: 0, confirmed: 0, openTasks: 0 })
const kpiCards = computed(() => [
  { key: 'total', label: '接入事件', value: kpiStats.total, tone: 'info' },
  { key: 'pendingReview', label: '待人工审', value: kpiStats.pendingReview, tone: 'warn' },
  { key: 'confirmed', label: '已闭环', value: kpiStats.confirmed, tone: 'ok' },
  { key: 'openTasks', label: '开放任务', value: kpiStats.openTasks, tone: 'brand' },
])
const messagesRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)

// ============ 快捷问题 ============
const quickPrompts = [
  { icon: '⚠', text: '分析近7天校园内的高风险区域，给出今天的巡检优先级' },
  { icon: '◉', text: '当前有多少台电动车在围栏外？列出车牌和位置' },
  { icon: '⌄', text: '统计本月未戴头盔 AI 事件，按摄像头分组' },
  { icon: '↯', text: '生成今日管理员确认清单，按风险等级排序' },
  { icon: '⊛', text: '哪些摄像头最近30分钟没有产生任何事件？' },
  { icon: '↗', text: '对比上周和本周的违规趋势，需要管理员关注什么？' }
]

// ============ 计算属性 ============
const stateClass = computed(() => String(status.value.state || 'UNAVAILABLE').toLowerCase())
const statusStateText = computed(() => {
  const s = status.value.state
  if (s === 'READY') return '运行中'
  if (s === 'DISABLED') return '未启用'
  return '未连接'
})
const sourceCounts = computed(() => [
  { key: 'events',        icon: '◈', label: 'AI 事件',    value: count('events') },
  { key: 'locations',     icon: '⌁', label: '车辆定位',    value: count('locations') },
  { key: 'cameras',       icon: '⊙', label: '摄像头',      value: count('cameras') },
  { key: 'geofences',     icon: '⌂', label: '电子围栏',    value: count('geofences') },
  { key: 'patrolMissions', icon: '↯', label: '巡检任务',   value: count('patrolMissions') },
  { key: 'violations',    icon: '△', label: '历史违规',    value: count('violations') }
])
const pipeline = computed(() => context.value.fusionSummary?.pipeline || ['采集', '时空对齐', '身份匹配', '证据融合', '风险分级', '人工确认'])

function count(key: string) { return Array.isArray((context.value as any)[key]) ? (context.value as any)[key].length : 0 }

// ============ 工具 ============
function formatTime(t: any): string {
  if (!t) return ''
  const d = new Date(typeof t === 'number' ? t : t)
  if (isNaN(d.getTime())) return String(t)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function summarize(result: string | null | undefined): string {
  if (!result) return ''
  try {
    const j = JSON.parse(result)
    if (Array.isArray(j)) return `${j.length} 条结果`
    if (j && typeof j === 'object') {
      if (j.ok) return '执行成功 ✓'
      if (j.error) return '错误'
      const keys = Object.keys(j)
      if (keys.length <= 3) return keys.join(', ')
    }
  } catch {}
  // fallback: count JSON array-like patterns in raw text
  const m = result.match(/"camera_name"/g) || result.match(/"event_id"/g)
  if (m) return `${m.length} 条结果`
  return '完成'
}
function truncate(s: string, n: number): string {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '\n…(truncated)' : s
}
function traceClass(type: string): string {
  if (type?.startsWith('tool/')) return 'tool'
  if (type?.startsWith('step/')) return 'step'
  if (type?.startsWith('turn/')) return 'turn'
  return 'other'
}
function shortenType(type: string): string {
  if (!type) return ''
  return type.replace(/^(tool|step|turn|assistant|user)\//, '')
}

// ============ 会话持久化 ============
// 优先从后端 DB 加载真实历史列表；localStorage 仅作瞬时兜底
async function loadSessions() {
  try {
    // axios 拦截器自动 unwrap ApiResponse → payload 就是 List<session>
    const payload: any = await listAgentSessions(50)
    const arr = Array.isArray(payload) ? payload : []
    if (arr.length) {
      sessions.value = arr.map((s: any) => ({
        sessionId: s.session_id || s.sessionId,
        title: s.title || '对话',
        updatedAt: new Date(s.update_time || s.updateTime || s.create_time || s.createTime || Date.now()).getTime()
      }))
      // 后端有 2+ 条时，默认选第一条；否则继续用 localStorage 里的
      if (sessions.value.length >= 2) {
        currentSessionId.value = sessions.value[0].sessionId
      }
      return
    }
  } catch (e) {
    console.warn('loadSessions 后端加载失败，走 localStorage', e)
  }
  // 后端空或失败 → 兜底 localStorage
  try {
    const raw = localStorage.getItem(LS_KEY)
    const arr = raw ? JSON.parse(raw) : []
    sessions.value = Array.isArray(arr) ? arr : []
  } catch { sessions.value = [] }
}
function saveSessions() {
  try { localStorage.setItem(LS_KEY, JSON.stringify(sessions.value)) } catch {}
}
function upsertSession(sessionId: string, title: string) {
  const existing = sessions.value.find(s => s.sessionId === sessionId)
  if (existing) {
    existing.title = title || existing.title
    existing.updatedAt = Date.now()
  } else {
    sessions.value.unshift({ sessionId, title: title || '对话', updatedAt: Date.now() })
  }
  saveSessions()
}
function deleteSession(sessionId: string) {
  sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
  saveSessions()
  if (currentSessionId.value === sessionId) newChat()
}

// ============ 页面操作 ============
async function newChat() {
  currentSessionId.value = ''
  messages.value = []
  trace.value = []
}
async function switchSession(sessionId: string) {
  if (sessionId === currentSessionId.value) return
  currentSessionId.value = sessionId
  try {
    const payload: any = await getAgentHistory(sessionId)
    applyHistory(payload)
    // 空 messages → 后端重启过，内存 session 丢失
    if (!messages.value.length) {
      ElMessage.warning('该会话已过期（后端内存 session 已清除），将开启新对话')
      newChat()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '会话历史加载失败')
  }
}
async function deleteSessionConfirm(sessionId: string) {
  await ElMessageBox.confirm('确定删除这条本地会话记录？（远端 DSH 会话不会被删除）', '删除确认', { type: 'warning' })
  deleteSession(sessionId)
}
function sendPrompt(text: string) {
  inputText.value = text
  submitRun()
}

// ============ 核心提交（SSE 流式打字机） ============
let currentAbort: (() => void) | null = null
async function submitRun() {
  const goal = inputText.value.trim()
  if (!goal || running.value) return
  messages.value.push({ role: 'user', content: goal, time: Date.now() })
  // placeholder assistant bubble — content grows as SSE chunks arrive
  messages.value.push({ role: 'assistant', content: '', time: Date.now() })
  inputText.value = ''
  autoResize()
  running.value = true
  await scrollToBottom()

  const lastAssistantIdx = messages.value.length - 1

  currentAbort = runAgentStream(
    {
      goal,
      sessionId: currentSessionId.value || undefined,
      preset: DEFAULT_PRESET,
      limit: DEFAULT_CONTEXT_LIMIT,
    },
    {
      onContext(_ctx) {
        // right-hand drawer can refresh here if we want
      },
      onContent(delta) {
        try {
          const msg = messages.value[lastAssistantIdx]
          if (msg) {
            msg.content += delta
            queueMicrotask(() => scrollToBottom())
          }
        } catch {}
      },
      onToolCallStart(name, args) {
        try {
          const msg = messages.value[lastAssistantIdx]
          if (msg) {
            if (!msg.toolCalls) msg.toolCalls = []
            msg.toolCalls.push({ name, args, status: 'running', result: null })
            queueMicrotask(() => scrollToBottom())
          }
        } catch {}
      },
      onToolCallResult(name, result) {
        try {
          const msg = messages.value[lastAssistantIdx]
          if (msg?.toolCalls) {
            const tc = msg.toolCalls.find((t: any) => t.name === name && t.status === 'running')
            if (tc) { tc.status = 'done'; tc.result = result }
          }
        } catch {}
      },
      onDone(envelope: any) {
        try {
          if (envelope?.sessionId) {
            currentSessionId.value = envelope.sessionId
            const title = goal.length > 24 ? goal.slice(0, 24) + '…' : goal
            upsertSession(envelope.sessionId, title)
          }
          if (envelope?.assistantReply && messages.value[lastAssistantIdx]) {
            messages.value[lastAssistantIdx].content = envelope.assistantReply
          }
          // trace drawer —— 直接用已存的 message.toolCalls，不再额外 GET history
        } finally {
          running.value = false
          currentAbort = null
          scrollToBottom()
        }
      },
      onError(msg) {
        try {
          if (messages.value[lastAssistantIdx]) {
            messages.value[lastAssistantIdx].content =
              messages.value[lastAssistantIdx].content || '❌ ' + (msg || '未知错误')
          }
        } finally {
          running.value = false
          currentAbort = null
          scrollToBottom()
        }
      },
    },
  )
}

function abortRun() {
  currentAbort?.()
  currentAbort = null
  running.value = false
}

function applyHistory(payload: any) {
  // axios 拦截器已经 unwrap ApiResponse.data — payload 就是 { sessionId, messages, trace }
  const msgs: ChatMsg[] = Array.isArray(payload?.messages) ? payload.messages : []
  const tr: TraceEvent[] = Array.isArray(payload?.trace) ? payload.trace : []
  msgs.sort((a, b) => compareOrder(a, b))
  const existingKeys = new Set(messages.value.map(m => `${m.role}|${m.content}|${m.seq ?? ''}|${m.time ?? ''}`))
  for (const m of msgs) {
    const k = `${m.role}|${m.content}|${m.seq ?? ''}|${m.time ?? ''}`
    if (!existingKeys.has(k) && m.content?.trim()) messages.value.push(m)
  }
  trace.value = tr
}

function compareOrder(a: ChatMsg, b: ChatMsg): number {
  const ta = a.time ?? a.seq
  const tb = b.time ?? b.seq
  if (ta == null && tb == null) return 0
  if (ta == null) return -1
  if (tb == null) return 1
  if (typeof ta === 'number' && typeof tb === 'number') return ta - tb
  return String(ta).localeCompare(String(tb))
}

// ============ 输入框 ============
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    submitRun()
  }
}
function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 180) + 'px'
}

async function scrollToBottom() {
  await nextTick()
  const container = messagesRef.value
  if (container) container.scrollTop = container.scrollHeight
}

// ============ 生命周期 ============
async function loadStats() {
  try {
    // axios 已 unwrap ApiResponse → 直接是 { total, pendingReview, confirmed, openTasks }
    const s: any = await getAiStats()
    Object.assign(kpiStats, s || {})
  } catch {}
}
async function loadInitial() {
  try {
    const [s, c] = await Promise.all([getAgentStatus(), getAgentContext(DEFAULT_CONTEXT_LIMIT), loadStats()])
    status.value = s || {}
    context.value = c || {}
  } catch (e: any) {
    // 后端没起就静默——页面是空态，用户点发送时会提示
  }
}

onMounted(async () => {
  await loadSessions()
  loadInitial()
  // 自动刷新一次 runtime + 数据源（30s）
  window.setInterval(loadInitial, 30000)
})
onBeforeUnmount(() => { running.value = false })
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 84px);
  background: #f7f9fc;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #1f2937;
  overflow: hidden;
}

/* ============ 左侧会话 ============ */
.chat-sidebar {
  width: 256px;
  flex-shrink: 0;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 12px;
}
.sidebar-head .brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 6px 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;
}
.brand-dot {
  width: 32px; height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  flex-shrink: 0;
}
.brand-text strong {
  display: block;
  font-size: 13px;
  color: #111827;
}
.brand-text small {
  font-size: 10px;
  color: #9ca3af;
}
.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s;
}
.new-chat-btn:hover { background: #374151; }

.session-list { flex: 1; overflow-y: auto; }
.session-list-label {
  font-size: 10px;
  color: #9ca3af;
  padding: 4px 6px 8px;
  letter-spacing: .08em;
  text-transform: uppercase;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .12s;
  position: relative;
}
.session-item:hover { background: #f3f4f6; }
.session-item.active { background: #eef2ff; }
.session-item.active .session-title { color: #3730a3; }
.session-icon {
  width: 28px; height: 28px;
  display: grid; place-items: center;
  border-radius: 6px;
  background: #f3f4f6;
  color: #6b7280;
  flex-shrink: 0;
}
.session-item.active .session-icon { background: #e0e7ff; color: #4f46e5; }
.session-body { flex: 1; min-width: 0; }
.session-title {
  font-size: 12px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-time {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 2px;
}
.session-del {
  width: 22px; height: 22px;
  border: none; background: transparent;
  color: #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: all .15s;
  display: grid; place-items: center;
}
.session-item:hover .session-del { opacity: 1; }
.session-del:hover { background: #fee2e2; color: #ef4444; }
.session-empty {
  text-align: center;
  padding: 32px 0;
  font-size: 12px;
  color: #9ca3af;
}
.sidebar-foot { padding-top: 10px; border-top: 1px solid #f0f0f0; }
.runtime-pill {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 11px;
  background: #ecfdf5;
  color: #047857;
}
.runtime-pill.unavailable { background: #fef2f2; color: #b91c1c; }
.runtime-pill.disabled { background: #f3f4f6; color: #6b7280; }
.runtime-pill .dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, .6);
}
.runtime-pill.unavailable .dot { background: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, .5); }
.runtime-pill.disabled .dot { background: #9ca3af; box-shadow: none; }

/* ============ 主对话区 ============ */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f7f9fc;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.chat-title strong {
  display: block;
  font-size: 14px;
  color: #111827;
}
.chat-title .subtitle {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 3px;
}
.header-right { display: flex; align-items: center; gap: 10px; }
.baseurl-tag {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4px 8px;
  border-radius: 4px;
}

/* ============ KPI 仪表盘 ============ */
.stats-bar {
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 12px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.kpi-card {
  flex: 1;
  min-width: 0;
  border-radius: 8px;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  display: flex; flex-direction: column; justify-content: center;
  transition: box-shadow 0.2s;
}
.kpi-card:hover { box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06); }
.kpi-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
  letter-spacing: 0.3px;
}
.kpi-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  margin-top: 2px;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}
.kpi-card.info   { border-color: #bae6fd; background: #f0f9ff; }
.kpi-card.info   .kpi-value { color: #0369a1; }
.kpi-card.warn   { border-color: #fde68a; background: #fffbeb; }
.kpi-card.warn   .kpi-value { color: #b45309; }
.kpi-card.ok     { border-color: #bbf7d0; background: #f0fdf4; }
.kpi-card.ok     .kpi-value { color: #15803d; }
.kpi-card.brand  { border-color: #c7d2fe; background: #eef2ff; }
.kpi-card.brand  .kpi-value { color: #4338ca; }
.kpi-refresh {
  display: flex; align-items: center; justify-content: center;
  width: 34px; border-radius: 8px;
  border: 1px solid #e2e8f0; background: #fff;
  color: #64748b; cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.kpi-refresh:hover { color: #2563eb; border-color: #93c5fd; background: #eff6ff; }
.drawer-toggle {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 12px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  color: #374151;
  cursor: pointer;
  transition: all .15s;
}
.drawer-toggle:hover { background: #e5e7eb; }
.drawer-toggle.active { background: #eef2ff; color: #4f46e5; }

/* ============ 欢迎态 ============ */
.chat-welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  text-align: center;
}
.welcome-orb {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 8px 32px rgba(79, 70, 229, .3);
  margin-bottom: 20px;
}
.chat-welcome h2 {
  font-size: 20px;
  color: #111827;
  margin: 0 0 8px;
}
.chat-welcome p {
  font-size: 13px;
  color: #6b7280;
  max-width: 480px;
  margin: 0 0 28px;
  line-height: 1.7;
}
.quick-prompts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  max-width: 600px;
  width: 100%;
}
.prompt-chip {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
  font-size: 12px;
  color: #374151;
  line-height: 1.5;
  transition: all .15s;
}
.prompt-chip:hover {
  border-color: #4f46e5;
  background: #fafaff;
  color: #3730a3;
  transform: translateY(-1px);
}
.chip-icon { flex-shrink: 0; font-size: 14px; }

/* ============ 消息区 ============ */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 28px 24px;
  background: #f1f3f5;
}
.chat-bubble {
  display: flex;
  gap: 12px;
  max-width: 800px;
  margin: 0 auto 18px;
}
.chat-bubble.user { flex-direction: row-reverse; }
.bubble-avatar {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: grid; place-items: center;
  flex-shrink: 0;
}
.chat-bubble.user .bubble-avatar {
  background: #4f46e5;
  color: #fff;
}
.chat-bubble.assistant .bubble-avatar {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
}
.bubble-body {
  max-width: calc(100% - 50px);
  padding: 12px 16px;
  border-radius: 14px;
  line-height: 1.7;
  font-size: 14px;
}
.chat-bubble.user .bubble-body {
  background: #4f46e5;
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.35);
}
.chat-bubble.assistant .bubble-body {
  background: #ffffff;
  color: #111827;
  border: 1px solid #e2e8f0;
  border-top-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble-text { white-space: pre-wrap; word-break: break-word; }

/* ---------- Markdown body (assistant bubble) ---------- */
/* ---------- Tool-call trace cards (ReAct Agent visualization) ---------- */
.tool-trace {
  margin-bottom: 8px;
  display: flex; flex-direction: column; gap: 6px;
}
.tool-card {
  font-size: 12px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px 10px;
  transition: border-color 0.2s;
}
.tool-card.running {
  border-color: #60a5fa;
  background: #eff6ff;
}
.tool-card.done {
  border-color: #86efac;
  background: #f0fdf4;
}
.tool-card-head {
  display: flex; align-items: center; gap: 6px;
  font-family: 'SF Mono', Consolas, Menlo, monospace;
}
.tool-icon { font-size: 13px; }
.tool-name {
  font-weight: 600; color: #075985;
  background: #bae6fd; padding: 1px 8px; border-radius: 4px;
}
.tool-card.done .tool-name { color: #15803d; background: #bbf7d0; }
.tool-status { margin-left: auto; font-size: 11px; font-weight: 500; }
.tool-status.running { color: #1d4ed8; animation: toolPulse 1.2s ease-in-out infinite; }
.tool-status.done { color: #15803d; }
@keyframes toolPulse { 0%,100% { opacity: 1 } 50% { opacity: 0.4 } }
.tool-details { margin-top: 6px; border-top: 1px dashed #94a3b8; padding-top: 4px; }
.tool-details summary {
  cursor: pointer; color: #475569; font-size: 11px;
  user-select: none;
}
.tool-details summary:hover { color: #1e293b; }
.tool-details pre {
  margin: 4px 0 0; font-size: 11px; line-height: 1.45;
  background: #ffffff; border: 1px solid #cbd5e1; border-radius: 4px;
  padding: 6px 8px; overflow-x: auto;
  max-height: 220px;
}

.bubble-text.markdown-body {
  white-space: normal;
  line-height: 1.7;
  font-size: 14px;
  color: #0f172a;
}
.bubble-text.markdown-body.is-user { color: #ffffff; }
.bubble-text.markdown-body :first-child { margin-top: 0; }
.bubble-text.markdown-body :last-child  { margin-bottom: 0; }
.bubble-text.markdown-body p    { margin: 6px 0; color: #0f172a; }
.bubble-text.markdown-body h1,
.bubble-text.markdown-body h2,
.bubble-text.markdown-body h3,
.bubble-text.markdown-body h4   { margin: 14px 0 6px; font-weight: 700; line-height: 1.4; color: #0f172a; }
.bubble-text.markdown-body h1   { font-size: 17px; }
.bubble-text.markdown-body h2   { font-size: 15px; border-bottom: 1px solid #cbd5e1; padding-bottom: 3px; }
.bubble-text.markdown-body h3   { font-size: 14px; color: #1e3a8a; }
.bubble-text.markdown-body h4   { font-size: 13px; color: #1e3a8a; }
.bubble-text.markdown-body ul,
.bubble-text.markdown-body ol   { margin: 4px 0; padding-left: 20px; color: #0f172a; }
.bubble-text.markdown-body ul   { list-style: disc; }
.bubble-text.markdown-body ol   { list-style: decimal; }
.bubble-text.markdown-body li   { margin: 2px 0; color: #0f172a; }
.bubble-text.markdown-body blockquote {
  margin: 6px 0; padding: 6px 12px; border-left: 3px solid #4f46e5;
  background: #eef2ff; color: #312e81; border-radius: 0 4px 4px 0;
}
.bubble-text.markdown-body code {
  font-family: 'SF Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 3px;
  padding: 1px 5px;
  color: #9d174d;
}
.bubble-text.markdown-body pre {
  margin: 8px 0; padding: 12px 14px;
  background: #0f172a; border-radius: 6px; overflow-x: auto;
}
.bubble-text.markdown-body pre code {
  background: transparent; border: none; padding: 0;
  color: #e2e8f0; font-size: 12px; line-height: 1.55;
}
.bubble-text.markdown-body table {
  margin: 8px 0; border-collapse: collapse; width: 100%;
  font-size: 13px;
}
.bubble-text.markdown-body table th,
.bubble-text.markdown-body table td {
  border: 1px solid #cbd5e1; padding: 7px 10px; text-align: left;
}
.bubble-text.markdown-body table th {
  background: #e2e8f0; font-weight: 700; color: #0f172a;
}
.bubble-text.markdown-body table tr:nth-child(even) td { background: #f8fafc; }
.bubble-text.markdown-body hr {
  border: none; border-top: 1px solid #cbd5e1; margin: 12px 0;
}
.bubble-text.markdown-body a {
  color: #2563eb; text-decoration: none; font-weight: 500;
}
.bubble-text.markdown-body a:hover { text-decoration: underline; }
.bubble-text.markdown-body strong { color: #0f172a; font-weight: 700; }
.bubble-text.markdown-body em { color: #334155; }
.bubble-time {
  font-size: 11px;
  color: #64748b;
  margin-top: 6px;
}
.chat-bubble.user .bubble-time { text-align: right; color: rgba(255,255,255,0.85); }

/* typing */
.typing {
  display: flex; flex-direction: column; gap: 8px;
  padding: 14px 16px 12px;
}
.typing-dots { display: flex; gap: 4px; align-items: center; }
.typing-dots i {
  width: 6px; height: 6px; border-radius: 50%;
  background: #9ca3af;
  animation: typing-bounce 1.2s ease-in-out infinite;
  display: inline-block;
}
.typing-dots i:nth-child(1) { animation-delay: 0s; }
.typing-dots i:nth-child(2) { animation-delay: .2s; }
.typing-dots i:nth-child(3) { animation-delay: .4s; }
@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: .4; }
  30% { transform: translateY(-4px); opacity: 1; }
}
.typing small { color: #9ca3af; font-size: 11px; }

/* ============ 输入区 ============ */
.chat-input-wrap {
  padding: 16px 24px 20px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.chat-input-box {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 10px 10px 10px 16px;
  transition: border-color .15s, box-shadow .15s;
}
.chat-input-box:focus-within {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, .1);
}
.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  line-height: 1.6;
  color: #1f2937;
  resize: none;
  max-height: 180px;
  padding: 4px 0;
  font-family: inherit;
}
.chat-input::placeholder { color: #9ca3af; }
.send-btn {
  width: 36px; height: 36px;
  border-radius: 10px;
  border: none;
  background: #d1d5db;
  color: #fff;
  cursor: not-allowed;
  display: grid; place-items: center;
  flex-shrink: 0;
  transition: all .15s;
}
.send-btn.active {
  background: #4f46e5;
  cursor: pointer;
}
.send-btn.active:hover { background: #4338ca; }
.input-foot {
  max-width: 800px;
  margin: 8px auto 0;
  font-size: 11px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 6px;
}
.running-hint { color: #4f46e5; display: flex; align-items: center; gap: 6px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ============ 右侧抽屉 ============ */
.chat-drawer {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  transform: translateX(0);
  transition: transform .25s;
}
.chat-drawer:not(.show) {
  width: 0;
  border-left: none;
  overflow: hidden;
}
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.drawer-head strong { font-size: 13px; color: #111827; }
.drawer-close {
  width: 26px; height: 26px;
  border: none; background: transparent;
  color: #9ca3af;
  border-radius: 6px;
  cursor: pointer;
  display: grid; place-items: center;
}
.drawer-close:hover { background: #f3f4f6; color: #374151; }
.drawer-section {
  padding: 16px 18px;
  border-bottom: 1px solid #f3f4f6;
}
.drawer-section:last-child { border-bottom: none; }
.section-title {
  font-size: 11px;
  color: #6b7280;
  letter-spacing: .06em;
  text-transform: uppercase;
  margin-bottom: 10px;
}
.section-title small {
  color: #9ca3af;
  font-weight: normal;
  text-transform: none;
  letter-spacing: 0;
}
.source-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.source-item {
  display: flex; gap: 8px; align-items: center;
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 8px;
}
.source-icon {
  width: 26px; height: 26px;
  display: grid; place-items: center;
  background: #eef2ff;
  color: #4f46e5;
  border-radius: 6px;
  font-size: 13px;
  flex-shrink: 0;
}
.source-value { font-size: 15px; font-weight: 600; color: #111827; }
.source-label { font-size: 10px; color: #9ca3af; }

.pipeline {
  display: flex; flex-wrap: wrap; gap: 5px;
}
.pipeline-step {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 8px;
  font-size: 10px;
  color: #6b7280;
  background: #f3f4f6;
  border-radius: 999px;
}
.pipeline-step b { color: #4f46e5; font-weight: 600; font-size: 11px; }

.trace-list { max-height: 240px; overflow-y: auto; }
.trace-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  padding: 6px 8px;
  font-size: 11px;
  border-radius: 6px;
  margin-bottom: 3px;
  align-items: center;
}
.trace-row.tool { background: #f0fdf4; color: #166534; }
.trace-row.step { background: #eff6ff; color: #1d4ed8; }
.trace-row.turn { background: #fef3c7; color: #92400e; }
.trace-row.other { background: #f9fafb; color: #6b7280; }
.trace-type { font-weight: 600; }
.trace-detail { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.trace-time { font-size: 10px; color: inherit; opacity: .7; }
.trace-empty { padding: 8px; font-size: 11px; color: #9ca3af; line-height: 1.6; }

.runtime-dl { margin: 0; display: flex; flex-direction: column; gap: 6px; }
.runtime-dl div { display: flex; justify-content: space-between; gap: 10px; font-size: 11px; }
.runtime-dl dt { color: #9ca3af; }
.runtime-dl dd { margin: 0; color: #374151; text-align: right; max-width: 60%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.runtime-dl dd.mono { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 10px; }
</style>

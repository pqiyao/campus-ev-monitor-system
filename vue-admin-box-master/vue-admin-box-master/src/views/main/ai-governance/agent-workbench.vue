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
          <span class="subtitle">基于平台多源只读事实推理 · 不能直接处罚</span>
        </div>
        <div class="header-right">
          <span v-if="status.baseUrl" class="baseurl-tag">{{ status.baseUrl }}</span>
          <button class="drawer-toggle" :class="{ active: drawerVisible }" @click="drawerVisible = !drawerVisible" title="证据与轨迹">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            {{ drawerVisible ? '隐藏证据' : '查看证据' }}
          </button>
        </div>
      </header>

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
            <div class="bubble-text">{{ msg.content }}</div>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAgentContext, getAgentHistory, getAgentStatus, runAgent } from '@/api/agent'

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

let historyTimer: number | undefined  // kept for backward compat, not used in sync mode
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

// ============ 会话持久化（localStorage） ============
function loadSessions() {
  try {
    const raw = localStorage.getItem(LS_KEY)
    sessions.value = raw ? JSON.parse(raw) : []
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

// ============ 核心提交（run() 直接调 LLM 同步返回 assistantReply） ============
async function submitRun() {
  const goal = inputText.value.trim()
  if (!goal || running.value) return
  messages.value.push({ role: 'user', content: goal, time: Date.now() })
  inputText.value = ''
  autoResize()
  running.value = true
  await scrollToBottom()

  try {
    const result: any = await runAgent({
      goal,
      sessionId: currentSessionId.value || undefined,
      preset: DEFAULT_PRESET,
      limit: DEFAULT_CONTEXT_LIMIT
    })
    const data = result?.result || result
    if (data?.sessionId) currentSessionId.value = data.sessionId
    // 直接用后端返回的 assistantReply（后端 run() 已经 append 到服务端 history）
    if (data?.assistantReply) {
      messages.value.push({ role: 'assistant', content: data.assistantReply, time: Date.now() })
    }
    if (data?.sessionId) {
      const title = goal.length > 24 ? goal.slice(0, 24) + '…' : goal
      upsertSession(data.sessionId, title)
    }
    // 刷新 trace（右侧抽屉用）
    if (currentSessionId.value) {
      try {
        const h: any = await getAgentHistory(currentSessionId.value)
        const hd = h?.result || h
        trace.value = Array.isArray(hd?.trace) ? hd.trace : []
      } catch {}
    }
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: '❌ 请求失败：' + (e?.message || '未知错误'), time: Date.now() })
  } finally {
    running.value = false
    await scrollToBottom()
  }
}

function applyHistory(payload: any) {
  const root = payload?.result?.value || payload?.value || payload
  const msgs: ChatMsg[] = Array.isArray(root?.messages) ? root.messages : []
  const tr: TraceEvent[] = Array.isArray(root?.trace) ? root.trace : []
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
async function loadInitial() {
  try {
    const [s, c] = await Promise.all([getAgentStatus(), getAgentContext(DEFAULT_CONTEXT_LIMIT)])
    status.value = s || {}
    context.value = c || {}
  } catch (e: any) {
    // 后端没起就静默——页面是空态，用户点发送时会提示
  }
}

onMounted(() => {
  loadSessions()
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
  font-size: 13px;
}
.chat-bubble.user .bubble-body {
  background: #111827;
  color: #fff;
  border-top-right-radius: 4px;
}
.chat-bubble.assistant .bubble-body {
  background: #fff;
  color: #1f2937;
  border: 1px solid #e5e7eb;
  border-top-left-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble-text { white-space: pre-wrap; word-break: break-word; }
.bubble-time {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 6px;
}
.chat-bubble.user .bubble-time { text-align: right; }

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

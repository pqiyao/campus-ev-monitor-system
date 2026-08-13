<template>
  <div class="monitor-container page-bg-shell">
    <header class="header">
      <span class="head-logo">校园电动车监测</span>
      <ul class="head-ul">
        <li><a class="active-a" href="javascript:;">烟火视觉预警</a></li>
        <li><a href="javascript:;" @click="openGovernance">事件复核</a></li>
      </ul>
      <div class="header-right">
        <span class="service-dot" :class="serviceState"></span>
        <span>{{ serviceStateText }}</span>
        <span>{{ currentTime }}</span>
      </div>
    </header>

    <section class="section">
      <div class="main-layout">
        <div class="layout-item video-alert-section">
          <div class="panel-card video-panel">
            <div class="panel-header">
              <span>实时识别画面</span>
              <div class="camera-controls">
                <select
                  v-model.number="selectedCameraId"
                  class="camera-select"
                  :disabled="availableCameras.length === 0"
                  @change="changeCamera"
                >
                  <option v-if="availableCameras.length === 0" :value="0">未发现可用摄像头</option>
                  <option v-for="camera in availableCameras" :key="camera.id" :value="camera.id">
                    {{ camera.name }}
                  </option>
                </select>
                <button class="refresh-btn" :disabled="cameraLoading" @click="refreshCameras">
                  {{ cameraLoading ? '检测中' : '刷新设备' }}
                </button>
                <button
                  class="refresh-btn"
                  :class="{ danger: streamState === 'connected' }"
                  :disabled="!selectedCameraId"
                  @click="toggleStream"
                >
                  {{ streamState === 'connected' ? '断开' : '连接' }}
                </button>
              </div>
            </div>
            <div class="panel-body video-body">
              <div class="camera-item">
                <div class="camera-header">
                  <span class="camera-name">{{ selectedCamera?.name || '本机摄像头' }}</span>
                  <span class="camera-actions">
                    <span>{{ selectedCamera?.resolution || '--' }}</span>
                    <span class="camera-status" :class="streamStateClass">{{ streamStateText }}</span>
                  </span>
                </div>

                <div class="camera-video">
                  <img v-if="frameUrl" class="camera-img" :src="frameUrl" alt="实时识别画面" />
                  <div v-for="(box, index) in safetyBoxes" :key="`${box.cls}-${index}`" class="detect-box" :class="box.cls" :style="boxStyle(box)">
                    <span>{{ boxLabel(box) }}</span>
                  </div>
                  <div v-if="!frameUrl" class="camera-placeholder">
                    <div>
                      <strong>{{ videoEmptyTitle }}</strong>
                      <span>{{ videoEmptyHint }}</span>
                    </div>
                  </div>
                  <div v-if="fireCount || smokeCount" class="risk-banner">
                    当前画面检测到 {{ fireCount ? `${fireCount} 处明火` : '' }}{{ fireCount && smokeCount ? '、' : '' }}{{ smokeCount ? `${smokeCount} 处烟雾` : '' }}
                  </div>
                </div>

                <div class="frame-metrics">
                  <div><span>明火目标</span><b class="fire-text">{{ fireCount }}</b></div>
                  <div><span>烟雾目标</span><b class="smoke-text">{{ smokeCount }}</b></div>
                  <div><span>检测框</span><b>{{ safetyBoxes.length }}</b></div>
                  <div><span>画面帧率</span><b>{{ streamFps }} FPS</b></div>
                </div>
              </div>
            </div>
          </div>

          <div class="panel-card alert-panel">
            <div class="panel-header">
              <span>最新烟火事件</span>
              <button class="refresh-btn" :disabled="eventsLoading" @click="loadSafetyEvents(false)">
                {{ eventsLoading ? '同步中' : '刷新' }}
              </button>
            </div>
            <div class="panel-body">
              <ul class="alert-list">
                <li v-if="!eventsLoading && safetyEvents.length === 0" class="empty-state">暂无烟火事件</li>
                <li v-for="record in safetyEvents" :key="record.event_id" class="alert-record">
                  <div class="alert-record-top">
                    <span class="alert-type" :class="eventTone(record.event_type)">{{ eventTypeText(record.event_type) }}</span>
                    <span class="status-pill" :class="eventStatusClass(record.status)">{{ eventStatusText(record.status) }}</span>
                  </div>
                  <div class="alert-record-row">
                    <span>{{ record.location_name || '未标注地点' }}</span>
                    <span>{{ formatTime(record.occurred_at) }}</span>
                  </div>
                  <div class="alert-record-row secondary-row">
                    <span>{{ record.device_id || record.source_type || '视觉识别服务' }}</span>
                    <button class="mini-btn" @click="openGovernance(record)">进入复核</button>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div class="layout-item data-search-section">
          <div class="panel-card stats-panel">
            <div class="panel-header">核心数据统计</div>
            <div class="panel-body">
              <div class="stats-grid">
                <div class="stat-item">
                  <div class="stat-value fire-ring">{{ eventStats.fire }}</div>
                  <div class="stat-label">明火事件</div>
                  <div class="stats-trend">今日 {{ eventStats.todayFire }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value smoke-ring">{{ eventStats.smoke }}</div>
                  <div class="stat-label">烟雾事件</div>
                  <div class="stats-trend">今日 {{ eventStats.todaySmoke }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value review-ring">{{ eventStats.pending }}</div>
                  <div class="stat-label">待复核</div>
                  <div class="stats-trend">共 {{ safetyEvents.length }} 条</div>
                </div>
              </div>
              <div class="stats-compare">
                <div class="compare-item">
                  <span>已完成研判</span>
                  <div class="progress-bar"><div class="progress-fill" :style="{ width: `${reviewRate}%` }"></div></div>
                  <span>{{ reviewRate }}%</span>
                </div>
              </div>
            </div>
          </div>

          <div class="panel-card camera-status-panel">
            <div class="panel-header">
              <span>识别服务状态</span>
              <span class="status-summary">{{ serviceStateText }}</span>
            </div>
            <div class="panel-body service-grid">
              <div class="service-item">
                <span>事件队列</span>
                <b>{{ aiStatus.queueSize ?? '--' }} / {{ aiStatus.queueCapacity ?? '--' }}</b>
              </div>
              <div class="service-item">
                <span>处理进程</span>
                <b>{{ aiStatus.workers ?? '--' }} / {{ aiStatus.workerLimit ?? '--' }}</b>
              </div>
              <div class="service-item">
                <span>重试上限</span>
                <b>{{ aiStatus.retryLimit ?? '--' }}</b>
              </div>
              <div class="service-item">
                <span>当前摄像头</span>
                <b>{{ selectedCamera ? '已发现' : '未发现' }}</b>
              </div>
            </div>
          </div>

          <div class="panel-card search-panel">
            <div class="panel-header">
              <span>实时检测结果</span>
              <span class="status-summary">{{ safetyBoxes.length }} 个风险目标</span>
            </div>
            <div class="panel-body">
              <div v-if="safetyBoxes.length === 0" class="detection-empty">
                <strong>{{ frameUrl ? '当前画面未识别到烟火目标' : '等待实时画面' }}</strong>
                <span>识别结果将由烟火模型持续更新</span>
              </div>
              <div v-else class="detection-list">
                <div v-for="(box, index) in safetyBoxes" :key="`result-${index}`" class="detection-item">
                  <span class="target-index">{{ String(index + 1).padStart(2, '0') }}</span>
                  <div>
                    <b :class="eventTone(box.cls === 'fire' ? 'FIRE_DETECTED' : 'SMOKE_DETECTED')">{{ box.cls === 'fire' ? '明火目标' : '烟雾目标' }}</b>
                    <small>置信度 {{ percent(box.conf) }}</small>
                  </div>
                  <em>{{ boxPosition(box) }}</em>
                </div>
              </div>
              <div class="result-footer">
                <span>最后画面：{{ lastFrameAt || '--' }}</span>
                <button class="btn primary" @click="openGovernance">打开 AI 治理工作台</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="initialLoading" class="loading-mask">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p>正在连接识别服务...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAiEvents } from '@/api/aiGovernance'
import { HARDWARE_CAMERAS } from '@/config/hardwareCameras'

type Camera = {
  id: number
  deviceIndex: number | null
  name: string
  location: string
  status: string
  resolution: string
  frameRate: number
  videoUrl?: string
  detectionUrl?: string
}

type DetectionBox = {
  cls: string
  conf: number
  x1: number
  y1: number
  x2: number
  y2: number
}

type SafetyEvent = Record<string, any> & {
  event_id: number
  event_type: 'FIRE_DETECTED' | 'SMOKE_DETECTED'
}

const router = useRouter()
const YOLO_ORIGIN = String(import.meta.env.VITE_YOLO_ORIGIN || 'http://127.0.0.1:8000').replace(/\/$/, '')
const YOLO_WS_ORIGIN = YOLO_ORIGIN.replace(/^http/i, 'ws')
// 8081/8082 只提供 MJPEG；本机 Python 读取视频并运行 fire_smoke_best.pt。
const HARDWARE_MODE = true
const HARDWARE_PAGE_CAMERAS = HARDWARE_CAMERAS

const currentTime = ref('')
const initialLoading = ref(true)
const cameraLoading = ref(false)
const eventsLoading = ref(false)
const serviceState = ref<'checking' | 'online' | 'offline'>('checking')
const cameras = ref<Camera[]>([])
const selectedCameraId = ref(0)
const streamState = ref<'idle' | 'connecting' | 'connected' | 'error'>('idle')
const streamError = ref('')
const frameUrl = ref('')
const safetyBoxes = ref<DetectionBox[]>([])
const fireCount = ref(0)
const smokeCount = ref(0)
const streamFps = ref(0)
const lastFrameAt = ref('')
const safetyEvents = ref<SafetyEvent[]>([])
const aiStatus = reactive<Record<string, any>>({})

let clockTimer: ReturnType<typeof setInterval> | null = null
let pollTimer: ReturnType<typeof setInterval> | null = null
let fpsTimer: ReturnType<typeof setInterval> | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let socket: WebSocket | null = null
let frameCounter = 0
let shouldReconnect = false
let connectionGeneration = 0
let hardwarePollTimer: number | null = null
let hardwarePollGeneration = 0

const availableCameras = computed(() => cameras.value.filter(camera => camera.deviceIndex !== null && camera.status === 'online'))
const selectedCamera = computed(() => cameras.value.find(camera => camera.id === selectedCameraId.value) || null)
const serviceStateText = computed(() => ({ checking: '服务检测中', online: '识别服务在线', offline: '识别服务离线' }[serviceState.value]))
const streamStateText = computed(() => ({
  idle: selectedCameraId.value ? '未连接' : '无可用设备',
  connecting: '连接中',
  connected: '实时识别中',
  error: '连接异常'
}[streamState.value]))
const streamStateClass = computed(() => streamState.value === 'connected' ? 'online' : streamState.value === 'error' ? 'warning' : '')
const videoEmptyTitle = computed(() => {
  if (serviceState.value === 'offline') return '识别服务未连接'
  if (!selectedCameraId.value) return '未发现可用摄像头'
  if (streamState.value === 'connecting') return '正在建立视频连接'
  if (streamState.value === 'error') return '视频连接失败'
  return '实时画面尚未连接'
})
const videoEmptyHint = computed(() => streamError.value || (selectedCameraId.value ? '点击连接开始烟火识别' : '接入摄像头后刷新设备列表'))

const eventStats = computed(() => {
  const today = dateKey(new Date())
  return safetyEvents.value.reduce((stats, event) => {
    const isFire = event.event_type === 'FIRE_DETECTED'
    if (isFire) stats.fire += 1
    else stats.smoke += 1
    if (dateKey(new Date(String(event.occurred_at || event.create_time || '').replace(' ', 'T'))) === today) {
      if (isFire) stats.todayFire += 1
      else stats.todaySmoke += 1
    }
    if (!['CONFIRMED', 'REJECTED'].includes(event.status)) stats.pending += 1
    return stats
  }, { fire: 0, smoke: 0, todayFire: 0, todaySmoke: 0, pending: 0 })
})

const reviewRate = computed(() => {
  if (!safetyEvents.value.length) return 0
  return Math.round(((safetyEvents.value.length - eventStats.value.pending) / safetyEvents.value.length) * 100)
})

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function dateKey(date: Date) {
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatTime(value?: string | Date) {
  const date = value instanceof Date ? value : value ? new Date(String(value).replace(' ', 'T')) : new Date()
  if (Number.isNaN(date.getTime())) return value ? String(value) : '--'
  return `${dateKey(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function updateClock() {
  currentTime.value = formatTime(new Date())
}

function percent(value: unknown) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '--'
  return `${Math.round((number <= 1 ? number * 100 : number))}%`
}

function boxStyle(box: DetectionBox) {
  return {
    left: `${Math.max(0, box.x1) * 100}%`,
    top: `${Math.max(0, box.y1) * 100}%`,
    width: `${Math.max(0, box.x2 - box.x1) * 100}%`,
    height: `${Math.max(0, box.y2 - box.y1) * 100}%`
  }
}

function boxLabel(box: DetectionBox) {
  return `${box.cls === 'fire' ? '明火' : '烟雾'} ${percent(box.conf)}`
}

function boxPosition(box: DetectionBox) {
  return `X ${Math.round(box.x1 * 100)}% · Y ${Math.round(box.y1 * 100)}%`
}

function eventTypeText(type: string) {
  return type === 'FIRE_DETECTED' ? '明火风险' : '烟雾风险'
}

function eventTone(type: string) {
  return type === 'FIRE_DETECTED' ? 'fire-text' : 'smoke-text'
}

function eventStatusText(status: string) {
  return ({
    INGESTED: '待研判',
    ANALYZING: '研判中',
    REVIEW_PENDING: '待复核',
    AUTO_ACCEPTED: '已受理',
    CONFIRMED: '已确认',
    REJECTED: '已驳回',
    ANALYSIS_FAILED: '研判失败'
  } as Record<string, string>)[status] || status || '待处理'
}

function eventStatusClass(status: string) {
  if (status === 'CONFIRMED') return 'status-finished'
  if (status === 'REJECTED') return 'status-muted'
  if (['ANALYZING', 'AUTO_ACCEPTED'].includes(status)) return 'status-processing'
  return 'status-pending'
}

async function fetchJson(path: string) {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 5000)
  try {
    const response = await fetch(`${YOLO_ORIGIN}${path}`, { signal: controller.signal })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const body = await response.json()
    if (body.code !== 200) throw new Error(body.message || '识别服务响应异常')
    return body.data
  } finally {
    window.clearTimeout(timeout)
  }
}

async function fetchHardwareDetections(camera: Camera) {
  if (!camera.detectionUrl) return
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 1500)
  let data: any
  try {
    const response = await fetch(camera.detectionUrl, { cache: 'no-store', signal: controller.signal })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    data = await response.json()
  } finally {
    window.clearTimeout(timeout)
  }
  const boxes = Array.isArray(data.detections) ? data.detections : []
  safetyBoxes.value = boxes.map((item: any) => {
    const bbox = item.bbox || item.box || [item.x1, item.y1, item.x2, item.y2]
    const values = Array.isArray(bbox) ? bbox.map(Number) : []
    const normalized = values.length === 4 && values[2] > 1
      ? [values[0] / (data.image_width || 640), values[1] / (data.image_height || 480), values[2] / (data.image_width || 640), values[3] / (data.image_height || 480)]
      : values
    return { cls: normalizeSafetyClass(item.class || item.label || item.name), conf: Number(item.confidence ?? item.conf ?? item.score ?? 0), x1: normalized[0] || 0, y1: normalized[1] || 0, x2: normalized[2] || 0, y2: normalized[3] || 0 }
  }).filter((box: DetectionBox) => box.cls === 'fire' || box.cls === 'smoke')
  fireCount.value = safetyBoxes.value.filter(box => box.cls === 'fire').length
  smokeCount.value = safetyBoxes.value.filter(box => box.cls === 'smoke').length
  streamFps.value = Number(data.result_fps ?? data.fps) || 0
  lastFrameAt.value = formatTime(new Date()).slice(11)
  frameCounter += 1
}

function normalizeSafetyClass(value: unknown): string {
  const label = String(value || '').trim().toLowerCase().replace(/[\s-]+/g, '_')
  if (/^(fire|fires|flame|flames|fire_detected)$/.test(label) || /明火|火焰/.test(label)) return 'fire'
  if (/^(smoke|smokes|smoking|smoke_detected)$/.test(label) || /烟雾|浓烟/.test(label)) return 'smoke'
  return label
}

async function loadServiceStatus() {
  try {
    const data = await fetchJson('/api/ai/status')
    Object.assign(aiStatus, data || {})
    serviceState.value = 'online'
  } catch {
    serviceState.value = 'offline'
  }
}

async function refreshCameras(options: { silent?: boolean; connect?: boolean } = {}) {
  cameraLoading.value = true
  try {
    if (HARDWARE_MODE) {
      cameras.value = HARDWARE_PAGE_CAMERAS.map(camera => ({ ...camera, deviceIndex: camera.id, status: 'online' })) as Camera[]
      serviceState.value = 'checking'
      if (!selectedCameraId.value) selectedCameraId.value = cameras.value[0].id
      if (options.connect && selectedCameraId.value) connectStream()
      return
    }
    const data = await fetchJson('/api/cameras')
    cameras.value = Array.isArray(data) ? data : []
    serviceState.value = 'online'
    const stillAvailable = availableCameras.value.some(camera => camera.id === selectedCameraId.value)
    if (!stillAvailable) selectedCameraId.value = availableCameras.value[0]?.id || 0
    if (options.connect && selectedCameraId.value) connectStream()
  } catch (error: any) {
    cameras.value = []
    selectedCameraId.value = 0
    serviceState.value = 'offline'
    disconnectStream(false)
    if (!options.silent) ElMessage.error(error?.message || '无法连接烟火识别服务')
  } finally {
    cameraLoading.value = false
  }
}

async function loadSafetyEvents(silent = true) {
  eventsLoading.value = true
  try {
    const [fireResponse, smokeResponse]: any[] = await Promise.all([
      getAiEvents({ eventType: 'FIRE_DETECTED', limit: 30 }),
      getAiEvents({ eventType: 'SMOKE_DETECTED', limit: 30 })
    ])
    const combined = [...(fireResponse.data || []), ...(smokeResponse.data || [])]
      .filter(event => ['FIRE_DETECTED', 'SMOKE_DETECTED'].includes(event.event_type))
    const unique = new Map<number, SafetyEvent>()
    combined.forEach(event => unique.set(event.event_id, event))
    safetyEvents.value = [...unique.values()]
      .sort((a, b) => new Date(String(b.occurred_at || b.create_time || 0).replace(' ', 'T')).getTime() - new Date(String(a.occurred_at || a.create_time || 0).replace(' ', 'T')).getTime())
      .slice(0, 30)
  } catch (error: any) {
    if (!silent) ElMessage.error(error?.message || '烟火事件同步失败')
  } finally {
    eventsLoading.value = false
  }
}

async function decodeBinaryFrame(buffer: ArrayBuffer) {
  if (buffer.byteLength < 5) throw new Error('视频帧格式不完整')
  const view = new DataView(buffer)
  const jsonLength = view.getUint32(0, false)
  if (jsonLength <= 0 || jsonLength + 4 >= buffer.byteLength) throw new Error('视频帧元数据异常')
  const jsonBytes = new Uint8Array(buffer, 4, jsonLength)
  const jpegBytes = new Uint8Array(buffer, 4 + jsonLength)
  const metadata = JSON.parse(new TextDecoder().decode(jsonBytes))
  const nextUrl = URL.createObjectURL(new Blob([jpegBytes], { type: 'image/jpeg' }))
  const previousUrl = frameUrl.value
  frameUrl.value = nextUrl
  if (previousUrl) URL.revokeObjectURL(previousUrl)

  const boxes = Array.isArray(metadata.boxes) ? metadata.boxes : []
  safetyBoxes.value = boxes.filter((box: DetectionBox) => ['fire', 'smoke'].includes(String(box.cls).toLowerCase()))
  fireCount.value = Number(metadata.fire_count) || safetyBoxes.value.filter(box => box.cls === 'fire').length
  smokeCount.value = Number(metadata.smoke_count) || safetyBoxes.value.filter(box => box.cls === 'smoke').length
  lastFrameAt.value = formatTime(new Date()).slice(11)
  frameCounter += 1
}

function connectStream() {
  if (!selectedCameraId.value || serviceState.value === 'offline') return
  disconnectStream(false)
  const generation = ++connectionGeneration
  shouldReconnect = true
  streamState.value = 'connecting'
  streamError.value = ''

  if (HARDWARE_MODE) {
    const camera = selectedCamera.value
    if (!camera?.videoUrl) return
  }

  const streamPath = HARDWARE_MODE ? 'network_camera' : 'local_camera'
  const currentSocket = new WebSocket(`${YOLO_WS_ORIGIN}/ws/${streamPath}/${selectedCameraId.value}`)
  currentSocket.binaryType = 'arraybuffer'
  socket = currentSocket

  currentSocket.onopen = () => {
    if (generation !== connectionGeneration) return
    streamState.value = 'connected'
  }
  currentSocket.onmessage = async event => {
    if (generation !== connectionGeneration) return
    try {
      if (typeof event.data === 'string') {
        const message = JSON.parse(event.data)
        if (message.success === false) throw new Error(message.error || '摄像头不可用')
        return
      }
      const buffer = event.data instanceof Blob ? await event.data.arrayBuffer() : event.data
      await decodeBinaryFrame(buffer)
    } catch (error: any) {
      streamError.value = error?.message || '实时画面解析失败'
    }
  }
  currentSocket.onerror = () => {
    if (generation !== connectionGeneration) return
    streamState.value = 'error'
    streamError.value = '请确认识别服务和摄像头均可用'
  }
  currentSocket.onclose = () => {
    if (generation !== connectionGeneration) return
    socket = null
    streamState.value = shouldReconnect ? 'connecting' : 'idle'
    if (shouldReconnect) reconnectTimer = setTimeout(connectStream, 2500)
  }
}

function disconnectStream(clearFrame = true) {
  shouldReconnect = false
  connectionGeneration += 1
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (socket) {
    socket.onclose = null
    socket.close()
    socket = null
  }
  hardwarePollGeneration += 1
  if (hardwarePollTimer) { clearTimeout(hardwarePollTimer); hardwarePollTimer = null }
  streamState.value = 'idle'
  safetyBoxes.value = []
  fireCount.value = 0
  smokeCount.value = 0
  if (clearFrame && frameUrl.value) {
    URL.revokeObjectURL(frameUrl.value)
    frameUrl.value = ''
  }
}

function changeCamera() {
  disconnectStream()
  if (selectedCameraId.value) connectStream()
}

function toggleStream() {
  if (streamState.value === 'connected' || streamState.value === 'connecting') disconnectStream()
  else connectStream()
}

function openGovernance(record?: SafetyEvent) {
  router.push({ path: '/ai-governance/workbench', query: record?.event_id ? { eventId: String(record.event_id) } : undefined })
}

async function initializePage() {
  initialLoading.value = true
  await Promise.all([loadServiceStatus(), loadSafetyEvents(true)])
  await refreshCameras({ silent: true, connect: true })
  initialLoading.value = false
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  fpsTimer = setInterval(() => {
    streamFps.value = frameCounter
    frameCounter = 0
  }, 1000)
  pollTimer = setInterval(() => {
    loadServiceStatus()
    loadSafetyEvents(true)
  }, 30000)
  initializePage()
})

onUnmounted(() => {
  disconnectStream()
  if (clockTimer) clearInterval(clockTimer)
  if (pollTimer) clearInterval(pollTimer)
  if (fpsTimer) clearInterval(fpsTimer)
})
</script>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.monitor-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  color: #fff;
  background: #001428;
  font-size: 14px;
}

.header {
  z-index: 100;
  display: flex;
  height: 44px;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #007acc;
  background: linear-gradient(to right, #001e42, #002855);
  box-shadow: 0 2px 10px rgba(0, 122, 204, 0.3);
}

.head-logo {
  color: #00e5ff;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 2px;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.5);
}

.head-ul { display: flex; list-style: none; gap: 5px; }
.head-ul li a {
  display: block;
  padding: 8px 15px;
  border-radius: 4px;
  color: #85acd5;
  font-size: 16px;
  text-decoration: none;
}
.head-ul li a.active-a { color: #00e5ff; background: rgba(0, 229, 255, 0.1); border-bottom: 2px solid #00e5ff; }
.head-ul li a:hover { color: #00e5ff; background: rgba(0, 229, 255, 0.05); }

.header-right { display: flex; align-items: center; gap: 10px; color: #85acd5; font-size: 13px; }
.service-dot { width: 8px; height: 8px; border-radius: 50%; background: #8294a8; }
.service-dot.online { background: #4cd964; box-shadow: 0 0 8px rgba(76, 217, 100, 0.7); }
.service-dot.offline { background: #ff6b6b; }

.section { width: 100%; height: calc(100% - 44px); padding: 10px; overflow: hidden; }
.main-layout { display: grid; width: 100%; height: 100%; grid-template-columns: 60% 40%; gap: 10px; }
.layout-item { display: flex; height: 100%; min-height: 0; flex-direction: column; gap: 10px; }

.panel-card {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #007acc;
  border-radius: 8px;
  background: #001a36;
}

.panel-header {
  display: flex;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  border-bottom: 1px solid #007acc;
  border-radius: 7px 7px 0 0;
  background: linear-gradient(to right, #001e42, #002855);
  color: #00e5ff;
  font-weight: bold;
}

.panel-body { flex: 1; min-height: 0; padding: 10px; overflow-y: auto; }
.video-alert-section { gap: 10px; }
.video-panel { flex: 2; }
.alert-panel { flex: 1; }
.data-search-section { gap: 10px; }
.stats-panel { flex: 1; }
.camera-status-panel { flex: 0 0 160px; }
.search-panel { flex: 1.4; }

.camera-controls { display: flex; align-items: center; gap: 7px; }
.camera-select {
  width: 190px;
  height: 26px;
  padding: 0 7px;
  border: 1px solid #007acc;
  border-radius: 4px;
  outline: none;
  color: #dceeff;
  background: #001428;
}
.camera-select:disabled { color: #6886a6; }

.refresh-btn, .mini-btn {
  padding: 3px 8px;
  border: 1px solid #007acc;
  border-radius: 4px;
  color: #00e5ff;
  background: rgba(0, 122, 204, 0.2);
  cursor: pointer;
  font-size: 12px;
}
.refresh-btn:hover, .mini-btn:hover { background: rgba(0, 122, 204, 0.45); }
.refresh-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.refresh-btn.danger { border-color: rgba(255, 107, 107, 0.75); color: #ff8b8b; }

.video-body { overflow: hidden; }
.camera-item { display: flex; height: 100%; min-height: 0; flex-direction: column; }
.camera-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; color: #85acd5; font-size: 12px; }
.camera-actions { display: flex; align-items: center; gap: 8px; }
.camera-status { padding: 2px 6px; border: 1px solid #547391; border-radius: 3px; color: #85acd5; font-size: 11px; }
.camera-status.online { border-color: #4cd964; color: #4cd964; background: rgba(76, 217, 100, 0.12); }
.camera-status.warning { border-color: #ff6b6b; color: #ff8b8b; background: rgba(255, 107, 107, 0.12); }

.camera-video { position: relative; flex: 1; min-height: 180px; overflow: hidden; border-radius: 4px; background: #000; }
.camera-img { display: block; width: 100%; height: 100%; object-fit: contain; }
.camera-placeholder { display: grid; width: 100%; height: 100%; place-items: center; color: rgba(226, 232, 240, 0.78); text-align: center; background: linear-gradient(135deg, rgba(15, 23, 42, 0.98), rgba(2, 6, 23, 0.98)); }
.camera-placeholder strong, .camera-placeholder span { display: block; }
.camera-placeholder strong { margin-bottom: 8px; color: #dceeff; font-size: 15px; }
.camera-placeholder span { color: #708da9; font-size: 12px; }

.detect-box { position: absolute; border: 2px solid #ffb020; pointer-events: none; }
.detect-box.fire { border-color: #ff4d4f; box-shadow: 0 0 8px rgba(255, 77, 79, 0.5); }
.detect-box.smoke { border-color: #f2c94c; box-shadow: 0 0 8px rgba(242, 201, 76, 0.4); }
.detect-box span { position: absolute; top: -22px; left: -2px; white-space: nowrap; padding: 2px 5px; color: #fff; background: #ff4d4f; font-size: 11px; }
.detect-box.smoke span { color: #17202a; background: #f2c94c; }
.risk-banner { position: absolute; right: 10px; bottom: 10px; left: 10px; padding: 7px 10px; border: 1px solid rgba(255, 77, 79, 0.75); border-radius: 4px; color: #fff; background: rgba(143, 20, 28, 0.86); font-weight: bold; text-align: center; }

.frame-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 8px; }
.frame-metrics div { display: flex; align-items: center; justify-content: space-between; padding: 7px 9px; border: 1px solid rgba(0, 122, 204, 0.32); border-radius: 6px; background: rgba(0, 26, 54, 0.62); }
.frame-metrics span { color: #85acd5; font-size: 11px; }
.frame-metrics b { color: #00e5ff; font-size: 14px; }
.fire-text { color: #ff7474 !important; }
.smoke-text { color: #f2c94c !important; }

.alert-list { height: 100%; overflow-y: auto; list-style: none; }
.alert-record { padding: 9px 10px; border: 1px solid rgba(0, 122, 204, 0.48); border-radius: 6px; margin-bottom: 7px; background: rgba(0, 26, 54, 0.58); }
.alert-record-top, .alert-record-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.alert-record-top { margin-bottom: 6px; }
.alert-type { font-size: 12px; font-weight: bold; }
.alert-record-row { margin: 3px 0; color: #dceeff; font-size: 12px; }
.alert-record-row span:last-child { color: #85acd5; }
.secondary-row { color: #85acd5; }
.secondary-row span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.status-pill { padding: 2px 7px; border-radius: 999px; font-size: 11px; }
.status-pending { border: 1px solid rgba(255, 77, 79, 0.6); color: #ff8b8b; background: rgba(255, 77, 79, 0.16); }
.status-processing { border: 1px solid rgba(255, 152, 0, 0.65); color: #ffb74d; background: rgba(255, 152, 0, 0.16); }
.status-finished { border: 1px solid rgba(76, 217, 100, 0.65); color: #4cd964; background: rgba(76, 217, 100, 0.15); }
.status-muted { border: 1px solid rgba(133, 172, 213, 0.45); color: #85acd5; background: rgba(133, 172, 213, 0.1); }

.stats-grid { display: flex; justify-content: space-around; margin-bottom: 15px; text-align: center; }
.stat-item { flex: 1; padding: 0 8px; }
.stat-value { display: flex; width: 68px; height: 68px; align-items: center; justify-content: center; margin: 0 auto 7px; border: 1px solid #007acc; border-radius: 50%; color: #00e5ff; background: rgba(0, 26, 54, 0.8); font-size: 19px; font-weight: bold; }
.stat-value.fire-ring { border-color: #ff5d5f; color: #ff7474; }
.stat-value.smoke-ring { border-color: #d6ad35; color: #f2c94c; }
.stat-value.review-ring { border-color: #00a7ba; }
.stat-label { margin-bottom: 4px; color: #85acd5; font-size: 12px; }
.stats-trend { color: #4cd964; font-size: 11px; }
.stats-compare { padding: 9px; border-radius: 6px; background: rgba(0, 26, 54, 0.5); }
.compare-item { display: flex; align-items: center; gap: 10px; color: #85acd5; font-size: 12px; }
.progress-bar { flex: 1; height: 8px; overflow: hidden; border-radius: 4px; background: rgba(0, 122, 204, 0.2); }
.progress-fill { height: 100%; border-radius: 4px; background: linear-gradient(to right, #00e5ff, #007acc); transition: width 0.4s ease; }

.status-summary { color: #85acd5; font-size: 11px; font-weight: normal; }
.service-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.service-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border-bottom: 1px solid rgba(0, 122, 204, 0.25); color: #85acd5; font-size: 12px; }
.service-item b { color: #dceeff; }

.detection-empty { display: grid; min-height: 120px; place-content: center; text-align: center; }
.detection-empty strong { margin-bottom: 8px; color: #dceeff; font-size: 13px; }
.detection-empty span { color: #708da9; font-size: 11px; }
.detection-list { display: flex; flex-direction: column; gap: 7px; }
.detection-item { display: grid; grid-template-columns: 34px 1fr auto; align-items: center; gap: 8px; padding: 8px; border: 1px solid rgba(0, 122, 204, 0.32); border-radius: 6px; background: rgba(0, 26, 54, 0.55); }
.target-index { color: #557898; font-family: monospace; }
.detection-item b, .detection-item small { display: block; }
.detection-item b { margin-bottom: 2px; font-size: 12px; }
.detection-item small, .detection-item em { color: #85acd5; font-size: 11px; font-style: normal; }
.result-footer { display: flex; align-items: center; justify-content: space-between; padding-top: 10px; margin-top: 10px; border-top: 1px solid rgba(0, 122, 204, 0.25); color: #85acd5; font-size: 11px; }
.btn { padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; }
.btn.primary { color: #fff; background: #007acc; }
.btn.primary:hover { background: #0588db; }

.empty-state { padding: 20px; color: #85acd5; font-size: 12px; text-align: center; }
.loading-mask { position: fixed; z-index: 9999; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0, 20, 40, 0.82); }
.loading-content { color: #00e5ff; text-align: center; }
.loading-spinner { width: 40px; height: 40px; margin: 0 auto 10px; border: 3px solid rgba(0, 229, 255, 0.3); border-top-color: #00e5ff; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

::-webkit-scrollbar { width: 4px; height: 4px; }
::-webkit-scrollbar-track { border-radius: 2px; background: rgba(0, 26, 54, 0.3); }
::-webkit-scrollbar-thumb { border-radius: 2px; background: #007acc; }

@media screen and (max-width: 1200px) {
  .main-layout { grid-template-columns: 58% 42%; }
  .camera-select { width: 140px; }
  .frame-metrics { grid-template-columns: repeat(2, 1fr); }
}

@media screen and (max-width: 900px) {
  .monitor-container { height: auto; min-height: 100vh; overflow: auto; }
  .section { height: auto; overflow: visible; }
  .main-layout { grid-template-columns: 1fr; }
  .layout-item { min-height: 720px; }
  .head-ul { display: none; }
}
</style>

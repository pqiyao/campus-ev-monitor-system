<template>
  <div class="camera-monitor-container">
    <!-- 顶部导航 -->
    <header class="header left">
      <div class="left nav">
        <ul>
          <li class="nav_active">
            <i class="nav_2"></i>
            <span>摄像头监控</span>
          </li>
          <li @click="switchTab('history')">
            <i class="nav_icon history"></i>
            <span>历史回放</span>
          </li>
        </ul>
      </div>
      <div class="header-right">
        <div class="header-status-chips">
          <span class="chip chip-time">{{ currentTime }}</span>
          <span class="chip">在线 {{ onlineCamerasCount }} / {{ cameraList.length }}</span>
          <span class="chip" :class="aiConnectionStatus === 'connected' ? 'chip-ok' : 'chip-warn'">
            AI {{ aiConnectionStatus === 'connected' ? '已连接' : '未连接' }}
          </span>
          <span v-if="slotFrameReady.some(Boolean)" class="chip chip-live">LIVE</span>
          <span v-if="serverPreviewMode" class="chip chip-videotest">服务端预览·二进制</span>
          <span v-if="selectedCamera" class="chip chip-muted">{{ selectedCamera.name }}</span>
        </div>
      </div>
    </header>
 
    <!-- 内容部分 -->
    <div class="con1 left" id="car_control">
      <!-- 左侧摄像头列表和信息面板 -->
      <div class="left camera_left">
        <div class="left_up bow_shadow">
          <div class="search-box">
            <input
              type="text"
              class="search-input"
              v-model="searchKeyword"
              placeholder="搜索摄像头名称或位置"
              @keyup.enter="handleSearch"
            />
            <button class="find_but" @click="handleSearch">
              <i class="search-icon"></i>
            </button>
          </div>
 
          <div class="filter-tabs"></div>
 
          <p class="set_list">
            <i class="list_i"></i>
            摄像头列表 ({{ listTotal }})
          </p>
 
          <div v-if="camerasLoading" class="list-skeleton">
            <div v-for="s in 5" :key="s" class="sk-row"></div>
          </div>
          <div v-else-if="!cameraList.length" class="list-empty">暂无摄像头数据，请启动 Python 检测服务 (8000)</div>
          <div v-else class="camera-list">
            <div
              v-for="camera in displayedCameras"
              :key="camera.id"
              class="camera-item"
              :class="{
                active: selectedCamera?.id === camera.id,
                online: camera.status === 'online',
                offline: camera.status === 'offline',
              }"
              @click="selectCamera(camera)"
            >
              <div class="camera-icon">
                <i class="camera-icon-img"></i>
              </div>
              <div class="camera-info">
                <div class="camera-name">{{ camera.name }}</div>
                <div class="camera-location">{{ camera.location }}</div>
              </div>
              <div class="camera-status">
                <span class="status-dot" :class="camera.status || 'offline'"></span>
                <span class="status-text">{{ camera.status === 'online' ? '在线' : '离线' }}</span>
              </div>
            </div>
 
            <div class="pagination" v-if="listTotalPages > 1">
              <button class="page-btn" :disabled="pagination.pageNum === 1" @click="prevPage">
                <i class="arrow left"></i>
              </button>
              <span class="page-info">第 {{ pagination.pageNum }} / {{ listTotalPages }} 页</span>
              <button class="page-btn" :disabled="pagination.pageNum === listTotalPages" @click="nextPage">
                <i class="arrow right"></i>
              </button>
            </div>
          </div>
        </div>
 
        <div class="left_down bow_shadow">
          <div class="text_center">
            <a href="javascript:void(0)" class="tab_a" :class="{ tab_aActive: activeTab === 'basic' }" @click="activeTab = 'basic'">
              <i class="tab-icon basic"></i> 基本信息
            </a>
            <a href="javascript:void(0)" class="tab_a" :class="{ tab_aActive: activeTab === 'video' }" @click="activeTab = 'video'">
              <i class="tab-icon video"></i> 视频信息
            </a>
          </div>
 
          <div class="camera_content">
            <div v-if="activeTab === 'basic'" class="info-panel">
              <div class="info-section">
                <div class="info-title">摄像头信息</div>
                <div class="info-row" v-if="cameraInfo.name">
                  <span class="info-label">摄像头名称：</span>
                  <span class="info-value">{{ cameraInfo.name }}</span>
                </div>
                <div class="info-row" v-if="cameraInfo.location">
                  <span class="info-label">安装位置：</span>
                  <span class="info-value">{{ cameraInfo.location }}</span>
                </div>
                <div class="info-row" v-if="cameraInfo.cameraType">
                  <span class="info-label">摄像头类型：</span>
                  <span class="info-value">{{ cameraInfo.cameraType }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">当前状态：</span>
                  <span class="info-value status-badge" :class="cameraInfo.status || 'offline'">
                    {{ cameraInfo.status === 'online' ? '在线' : '离线' }}
                  </span>
                </div>
                <div class="info-row" v-if="cameraInfo.rtspUrl">
                  <span class="info-label">视频流地址：</span>
                  <span class="info-value url-value">{{ cameraInfo.rtspUrl }}</span>
                </div>
              </div>
              <div class="info-section">
                <div class="info-title">位置信息</div>
                <div class="info-row" v-if="cameraInfo.lng && cameraInfo.lat">
                  <span class="info-label">位置坐标：</span>
                  <span class="info-value location">
                    {{ cameraInfo.lng.toFixed(6) }}, {{ cameraInfo.lat.toFixed(6) }}
                    <i class="location-icon" @click="centerMapOnCamera" title="定位摄像头"></i>
                  </span>
                </div>
                <div class="info-row" v-if="cameraInfo.installTime">
                  <span class="info-label">安装时间：</span>
                  <span class="info-value">{{ formatDate(cameraInfo.installTime) }}</span>
                </div>
              </div>
            </div>
 
            <div v-if="activeTab === 'video'" class="video-info-panel">
              <div class="info-section">
                <div class="info-title">视频配置</div>
                <div class="config-item"><div class="config-label">分辨率：</div><div class="config-value">{{ cameraInfo.resolution || '640x480' }}</div></div>
                <div class="config-item"><div class="config-label">帧率：</div><div class="config-value">{{ cameraInfo.frameRate || '30' }} FPS</div></div>
                <div class="config-item"><div class="config-label">码率：</div><div class="config-value">{{ cameraInfo.bitRate || '1024' }} Kbps</div></div>
                <div class="config-item"><div class="config-label">视频编码：</div><div class="config-value">{{ cameraInfo.videoCodec || 'H.264' }}</div></div>
              </div>
              <div class="info-section">
                <div class="info-title">实时状态</div>
                <div class="stats-item"><div class="stats-label">检测FPS：</div><div class="stats-value">{{ detectionFps }}</div></div>
                <div class="stats-item"><div class="stats-label">显示FPS：</div><div class="stats-value">{{ displayFps }}</div></div>
                <div class="stats-item"><div class="stats-label">CPU使用率：</div><div class="stats-value">{{ cameraInfo.cpuUsage || '0' }}%</div></div>
                <div class="stats-item"><div class="stats-label">内存使用：</div><div class="stats-value">{{ cameraInfo.memoryUsage || '0' }} MB</div></div>
              </div>
            </div>
          </div>
        </div>
      </div>
 
      <!-- 中间视频区域 -->
      <div class="left camera_center">
        <div class="video-controls-bar">
          <div class="video-controls-left">
            <div class="layout-controls">
              <button class="video-btn layout-btn" :class="{ active: videoLayout === 1 }" @click="changeVideoLayout(1)">
                <i class="layout-icon single"></i> 单画面
              </button>
              <button class="video-btn layout-btn" :class="{ active: videoLayout === 4 }" @click="changeVideoLayout(4)">
                <i class="layout-icon four"></i> 四画面
              </button>
              <button class="video-btn layout-btn" :class="{ active: videoLayout === 6 }" @click="changeVideoLayout(6)">
                <i class="layout-icon six"></i> 六画面
              </button>
            </div>
          </div>
          <div class="video-controls-right">
            <div class="fps-inline" v-if="streamActive">
              <span>显示 {{ displayFps }} FPS</span>
              <span>检测 {{ detectionFps }} FPS</span>
            </div>
            <label class="toggle-server-vt" title="两种模式均使用 8081/8082 局域网视频；关闭后改为浏览器直接播放 MJPEG，减少 Canvas 重绘。">
              <input type="checkbox" v-model="hardwareMode" @change="onHardwareModeChange" />
              {{ hardwareMode ? '局域网硬件' : '局域网流畅模式' }}
            </label>
            <label class="toggle-boxes">
              <input type="checkbox" v-model="showBoundingBoxes" />
              检测框
            </label>
            <div class="action-controls">
              <button class="video-btn action-btn" @click="takeSnapshot" :disabled="!selectedCamera || !streamActive">
                <i class="snapshot-icon"></i> 抓拍
              </button>
              <button class="video-btn action-btn" @click="toggleRecording" :disabled="!selectedCamera || !streamActive">
                <i class="record-icon" :class="{ recording: isRecording }"></i>
                {{ isRecording ? '停止录像' : '开始录像' }}
              </button>
              <button class="video-btn action-btn fullscreen-btn" @click="toggleFullscreenCanvas(activeVideo)" :disabled="!streamActive">
                <i class="fullscreen-icon"></i> 全屏
              </button>
            </div>
          </div>
        </div>
 
        <div class="video-container" :class="`layout-${videoLayout}`">
          <div
            v-for="n in videoLayout"
            :key="n"
            class="video-item"
            :class="{ active: activeVideo === n }"
            @click="activateVideo(n)"
          >
            <div class="video-wrapper" :ref="el => setVideoWrapperRef(el, n)">
              <div
                v-if="isSlotConnecting(n)"
                class="video-placeholder"
              >
                <p>正在连接局域网视频流…</p>
                <p class="sub">
                  正在读取硬件节点的 MJPEG 实时视频。
                </p>
              </div>
              <div
                v-if="isSlotOffline(n)"
                class="video-placeholder warn"
              >
                <p>设备离线</p>
                <p class="sub">无法建立视频与检测链路</p>
              </div>
              <!--
                canvas 同时承担两个职责：
                  1. 显示摄像头原始视频帧（由 rAF 循环写入）
                  2. 叠加 YOLO 检测框（由 drawDetectionBoxes 写入）
              -->
              <img
                v-if="!hardwareMode && getSlotCamera(n)?.videoUrl"
                class="video-img hardware-direct-img"
                :src="getSlotCamera(n).videoUrl"
                alt="局域网实时视频"
                @load="markHardwareFrameReady(n - 1)"
              />
              <div
                v-if="!hardwareMode && showBoundingBoxes"
                class="direct-box-layer"
              >
                <div
                  v-for="(box, boxIndex) in (detectionResults[n - 1]?.boxes || [])"
                  :key="`${box.cls}-${boxIndex}`"
                  class="direct-detect-box"
                  :style="directBoxStyle(box)"
                >
                  <span>{{ box.cls }} {{ Math.round((box.conf || 0) * 100) }}%</span>
                </div>
              </div>
              <canvas
                v-show="hardwareMode"
                :ref="el => setCanvasRef(el, n)"
                class="video-img"
                width="640"
                height="480"
              ></canvas>
              <div class="video-overlay">
                <div class="video-title">
                  {{ getSlotCamera(n)?.name || `摄像头${n}` }}
                  <span class="live-badge" v-if="serverPreviewMode ? hasSlotFrame(n) : (n === activeVideo && aiConnectionStatus === 'connected')">● AI</span>
                </div>
                <div class="video-status" v-if="detectionResults[n - 1]">
                  <span>人 {{ detectionResults[n - 1].person_count || 0 }}</span>
                  <span>车 {{ detectionResults[n - 1].moto_count || 0 }}</span>
                  <span v-if="detectionResults[n - 1].load_violation" class="warn">载人</span>
                </div>
              </div>
              <div class="video-controls-mini">
                <button class="tool-btn" @click.stop="takeSnapshotByIndex(n)">
                  <i class="snapshot-small-icon"></i>
                </button>
                <button class="tool-btn" @click.stop="toggleFullscreenCanvas(n)">
                  <i class="fullscreen-small-icon"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
 
      <!-- 右侧地图/AI 信息区域（地图暂不显示，盒子保留） -->
      <div class="right camera_right">
        <div class="map-container">
          <div class="map-header">
            <div class="map-title">摄像头分布</div>
            <div class="map-controls">
              <button class="map-btn" :class="{ active: mapMode === 'camera' }" @click="switchMapMode('camera')">
                <i class="camera-dist-icon"></i> 摄像头分布
              </button>
              <button class="map-btn" :class="{ active: mapMode === 'ai' }" @click="switchMapMode('ai')">
                <i class="ai-info-icon"></i> AI检测信息
              </button>
            </div>
          </div>
          <div v-show="mapMode === 'camera'" class="map-placeholder">地图暂未启用</div>
          <div class="ai-info-panel" v-show="mapMode === 'ai'">
            <div class="ai-stats-header">
              <span class="ai-title">AI检测信息</span>
              <span class="camera-tag">{{ selectedCamera?.name || '未选择摄像头' }}</span>
            </div>
            <div class="ai-panel-placeholder"></div>
            <div class="ai-status">
              <span class="status-led" :class="aiConnectionStatus"></span>
              {{ aiConnectionStatus === 'connected' ? 'AI 检测服务已连接' : 'AI 检测服务未连接' }}
            </div>
          </div>
          <div class="map-info"></div>
        </div>
      </div>
    </div>
 
    <!-- 全屏模态框 -->
    <div class="fullscreen-modal" v-if="fullscreenVideo" @click="exitFullscreen">
      <div class="fullscreen-content" @click.stop>
        <img :src="fullscreenImageSrc" class="fullscreen-img" alt="全屏检测画面" />
        <button class="fullscreen-close" @click="exitFullscreen">
          <i class="close-icon"></i>
        </button>
      </div>
    </div>
 
    <!--
      不可用 display:none：多数浏览器不会对完全隐藏的 video 解码，
      canvas.drawImage 会一直是黑屏。移出视口、保留尺寸才能正常出画。
    -->
    <video
      ref="hiddenVideo"
      class="video-capture-source"
      playsinline
      muted
      preload="auto"
    ></video>
  </div>
</template>
 
<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { HARDWARE_CAMERAS } from '@/config/hardwareCameras'
 
// ============================================================
// 配置
// ============================================================
const API_BASE_URL = 'http://localhost:8000/api'
const WS_BASE_URL  = 'ws://localhost:8000/ws/detection'
/** 服务端预览：二进制 [uint32 jsonLen][json][jpeg] */
const WS_VIDEO_PREVIEW_BASE = 'ws://localhost:8000/ws/local_camera'
const hardwareMode = ref(true)
const HARDWARE_PAGE_CAMERAS = HARDWARE_CAMERAS.slice(0, 2)
 
// 向后端发送检测帧的间隔（毫秒）。
// 20ms ≈ 50fps 发送，后端会自动丢弃积压帧，实际推理受 GPU 速度决定。
// 如果后端 CPU-only 推理较慢，可调大为 100~200ms。
const SEND_INTERVAL_MS = 20
 
// 检测框颜色映射
const BOX_COLORS = {
  person:     '#00FF7F',
  motorcycle: '#FF4500',
  bike:       '#FF4500',
  no_helmet:  '#FF2D55',
  helmet:     '#7CFC00',
  default:    '#00BFFF',
}
 
const token = localStorage.getItem('token') || sessionStorage.getItem('token')
if (token) axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
 
// ============================================================
// 响应式数据
// ============================================================
const activeTab      = ref('basic')
const searchKeyword  = ref('')
const currentTime    = ref('')
const cameraList     = ref([])
const selectedCamera = ref(null)
const pagination     = ref({ pageNum: 1, pageSize: 10, total: 0, totalPages: 0 })
const cameraInfo     = ref({})
const videoLayout    = ref(4)
const activeVideo    = ref(1)
const detectionResults = ref([])   // 最新一帧检测结果（含 boxes）
const canvasRefs     = ref([])
 
const fullscreenVideo    = ref(false)
const fullscreenImageSrc = ref('')
const isRecording        = ref(false)
const camerasLoading     = ref(false)
const streamActive       = ref(false)
const showBoundingBoxes  = ref(true)
const videoWrapperRefs   = ref([])
/** 服务端 OpenCV + 二进制 JPEG 预览（本机摄像头模式仍为浏览器推流） */
const serverPreviewMode = ref(true)
 
// WebSocket
const ws = ref(null)
const previewWsList = ref([])
const slotFrameReady = ref([])
const hiddenVideo = ref(null)
let   mediaStream = null
/** 服务端预览：最近一帧 JPEG 解码图（已含 plot 框线） */
let   serverFrameBitmap = null
let   serverFrameBitmaps = []
let   lastPreviewObjectUrl = null
let   lastPreviewObjectUrls = []
let   hardwareImages = []
let   hardwarePollTimer = null
const hardwarePreviousCounts = new Map()
const hardwarePreviousFrames = new Map()
let   hardwarePollGeneration = 0
let   lastHardwareRenderAt = 0
 
// ── 渲染循环相关 ──────────────────────────────────────────
// 发送帧用 setInterval（固定频率，与 rAF 解耦）
let sendIntervalId  = null
// 渲染循环用 requestAnimationFrame
let renderRafId     = null
// 用于 toBlob 防止重叠调用
let blobPending     = false
// 抓帧用的离屏 canvas（640×480，固定尺寸）
let captureCanvas   = null
let captureCtx      = null
 
// FPS 统计
const detectionFps    = ref(0)
const displayFps      = ref(0)
let   detectionFrames = 0
let   displayFrames   = 0
let   fpsTick         = Date.now()
 
// 地图（百度地图暂不显示，右侧盒子保留）
const showMap = false
const mapMode        = ref('ai')
const mapContainer   = ref(null)
let   map            = null
let   mapMarkers     = []
 
// AI 统计
const totalStats = ref({
  frames: 0, persons: 0, motos: 0,
  helmetViolations: 0, helmetWorn: 0, loadViolations: 0,
})
const currentFrameStats = ref({
  person: 0, moto: 0, helmetViolation: 0, helmetWorn: 0, loadViolation: false,
})
const aiConnectionStatus = ref('disconnected')
 
// ============================================================
// 初始化数组
// ============================================================
const initArraysByLayout = (layout) => {
  detectionResults.value = Array(layout).fill(null)
  canvasRefs.value       = Array(layout).fill(null)
  videoWrapperRefs.value = Array(layout).fill(null)
  previewWsList.value    = Array(layout).fill(null)
  slotFrameReady.value   = Array(layout).fill(false)
  serverFrameBitmaps     = Array(layout).fill(null)
  lastPreviewObjectUrls  = Array(layout).fill(null)
}
initArraysByLayout(videoLayout.value)
 
const setCanvasRef = (el, index) => {
  if (el) canvasRefs.value[index - 1] = el
}

const setVideoWrapperRef = (el, index) => {
  if (el) videoWrapperRefs.value[index - 1] = el
}
 
// ============================================================
// 计算属性
// ============================================================
const onlineCamerasCount = computed(() =>
  cameraList.value.filter(c => c.status === 'online').length
)
const filteredCameras = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return cameraList.value
  return cameraList.value.filter(
    (c) =>
      (c.name || '').toLowerCase().includes(kw) ||
      (c.location || '').toLowerCase().includes(kw)
  )
})

const listTotal = computed(() => filteredCameras.value.length)
const listTotalPages = computed(() => Math.max(1, Math.ceil(listTotal.value / pagination.value.pageSize)))

const displayedCameras = computed(() => {
  const start = (pagination.value.pageNum - 1) * pagination.value.pageSize
  return filteredCameras.value.slice(start, start + pagination.value.pageSize)
})

const slotCameras = computed(() =>
  Array.from({ length: videoLayout.value }, (_, i) => cameraList.value[i] || null)
)

const getSlotCamera = (index) => slotCameras.value[index - 1] || null
const hasSlotFrame = (index) => !!slotFrameReady.value[index - 1]
const isSlotConnecting = (index) => {
  if (serverPreviewMode.value) {
    const cam = getSlotCamera(index)
    return !!cam && cam.status === 'online' && !hasSlotFrame(index)
  }
  return index === activeVideo.value && !streamActive.value && selectedCamera.value?.status === 'online'
}
const isSlotOffline = (index) => {
  if (serverPreviewMode.value) {
    return false
  }
  return index === activeVideo.value && selectedCamera.value?.status === 'offline'
}

const activateVideo = (index) => {
  activeVideo.value = index
  const cam = getSlotCamera(index)
  if (!cam) return
  selectedCamera.value = cam
  cameraInfo.value = {
    ...cam,
    networkDelay: '0', packetLoss: '0', cpuUsage: '0', memoryUsage: '0',
  }
  if (map && cam.status === 'online') locateCameraOnMap(cam)
}

watch(listTotalPages, (tp) => {
  if (pagination.value.pageNum > tp) pagination.value.pageNum = tp || 1
})

// ============================================================
// 时间
// ============================================================
const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  })
}
 
// ============================================================
// 摄像头列表
// ============================================================
const fetchCameras = async () => {
  camerasLoading.value = true
  try {
    if (HARDWARE_PAGE_CAMERAS.length) {
      cameraList.value = HARDWARE_PAGE_CAMERAS.map(camera => ({ ...camera, status: 'online', deviceIndex: camera.id, cameraType: '局域网视频检测节点' }))
      pagination.value.pageNum = 1
      if (!selectedCamera.value) selectedCamera.value = cameraList.value[0] || null
      if (serverPreviewMode.value && !streamActive.value) startMultiCameraPreview()
      return
    }
    const response = await axios.get(`${API_BASE_URL}/cameras`)
    if (response.data.code === 200) {
      const backendCameras = response.data.data || []
      cameraList.value = backendCameras.map((cam, index) => ({
        id: cam.id, name: cam.name,
        location: cam.location || '本地摄像头',
        status: cam.status,
        lng: cam.lng || 112.6827629 + index * 0.0001,
        lat: cam.lat || 26.8860959  + index * 0.0001,
        quality: cam.status === 'online' ? 'good' : 'poor',
        rtspUrl: cam.rtspUrl || '',
        cameraType: cam.type || 'USB摄像头',
        resolution: cam.resolution || '640x480',
        frameRate:  cam.frameRate  || 30,
        installTime: new Date().toISOString(),
      }))
      pagination.value.pageNum = 1
      if (cameraList.value.length > 0 && !selectedCamera.value) {
        const onlineCam = cameraList.value.find(c => c.status === 'online')
        selectCamera(onlineCam || cameraList.value[0])
      }
      if (serverPreviewMode.value && !streamActive.value) startMultiCameraPreview()
      updateCameraMarkers()
    } else {
      ElMessage.error('获取摄像头列表失败')
    }
  } catch (error) {
    console.error('摄像头列表接口错误:', error)
    ElMessage.error('无法连接 Python 服务，请确认 MainProgram 已在 8000 端口运行')
  } finally {
    camerasLoading.value = false
  }
}
 
// ============================================================
// 选择摄像头
// ============================================================
const selectCamera = async (camera) => {
  if (!camera) return
  totalStats.value = { frames: 0, persons: 0, motos: 0, helmetViolations: 0, helmetWorn: 0, loadViolations: 0 }
  currentFrameStats.value = { person: 0, moto: 0, helmetViolation: 0, helmetWorn: 0, loadViolation: false }
 
  stopCapture()
  disconnectWebSocket()
  streamActive.value = false

  selectedCamera.value = camera
  const slotIndex = cameraList.value.findIndex(c => c.id === camera.id)
  activeVideo.value    = slotIndex >= 0 ? slotIndex + 1 : 1
  detectionResults.value.fill(null)
 
  cameraInfo.value = {
    ...camera,
    networkDelay: '0', packetLoss: '0', cpuUsage: '0', memoryUsage: '0',
  }
 
  if (camera.status === 'online') {
    if (serverPreviewMode.value) {
      if (!streamActive.value) startMultiCameraPreview()
    } else {
      await startCapture()
      connectWebSocket(camera.id)
    }
  } else {
    ElMessage.warning('摄像头离线，无法启动 AI 检测')
  }
 
  if (map) locateCameraOnMap(camera)
}

const onServerPreviewModeChange = async () => {
  stopCapture()
  disconnectWebSocket()
  streamActive.value = false
  serverFrameBitmap = null
  revokePreviewObjectUrl()
  if (serverPreviewMode.value) {
    if (!selectedCamera.value && cameraList.value.length) {
      selectedCamera.value = cameraList.value.find(c => c.status === 'online') || cameraList.value[0]
    }
    startMultiCameraPreview()
  } else {
    if (!selectedCamera.value || selectedCamera.value.status !== 'online') return
    await startCapture()
    connectWebSocket(selectedCamera.value.id)
  }
}

const onHardwareModeChange = async () => {
  stopCapture()
  disconnectWebSocket()
  hardwareImages = []
  hardwarePreviousCounts.clear()
  streamActive.value = false
  serverPreviewMode.value = true
  await nextTick()
  startMultiCameraPreview()
}

const markHardwareFrameReady = (index) => {
  slotFrameReady.value[index] = true
  aiConnectionStatus.value = 'connected'
}

const directBoxStyle = (box) => ({
  left: `${Math.max(0, Number(box.x1) || 0) * 100}%`,
  top: `${Math.max(0, Number(box.y1) || 0) * 100}%`,
  width: `${Math.max(0, (Number(box.x2) || 0) - (Number(box.x1) || 0)) * 100}%`,
  height: `${Math.max(0, (Number(box.y2) || 0) - (Number(box.y1) || 0)) * 100}%`,
})

const revokePreviewObjectUrl = (index = null) => {
  if (index !== null) {
    const oldUrl = lastPreviewObjectUrls[index]
    if (oldUrl) URL.revokeObjectURL(oldUrl)
    lastPreviewObjectUrls[index] = null
    slotFrameReady.value[index] = false
    if (index === activeVideo.value - 1) lastPreviewObjectUrl = null
    return
  }
  if (lastPreviewObjectUrl) URL.revokeObjectURL(lastPreviewObjectUrl)
  lastPreviewObjectUrl = null
  lastPreviewObjectUrls.forEach(url => {
    if (url) URL.revokeObjectURL(url)
  })
  lastPreviewObjectUrls = Array(videoLayout.value).fill(null)
  slotFrameReady.value = Array(videoLayout.value).fill(false)
}

const startMultiCameraPreview = () => {
  disconnectWebSocket()
  serverFrameBitmap = null
  serverFrameBitmaps = Array(videoLayout.value).fill(null)
  slotFrameReady.value = Array(videoLayout.value).fill(false)
  detectionResults.value = Array(videoLayout.value).fill(null)
  if (HARDWARE_PAGE_CAMERAS.length) {
    hardwareImages = hardwareMode.value
      ? slotCameras.value.map((camera, index) => {
          if (!camera?.videoUrl) return null
          const image = new Image()
          image.onload = () => {
            slotFrameReady.value[index] = true
            aiConnectionStatus.value = 'connected'
          }
          image.src = camera.videoUrl
          return image
        })
      : Array(videoLayout.value).fill(null)
    streamActive.value = true
    if (hardwareMode.value) startRenderLoop()
    stopHardwarePolling()
    const generation = ++hardwarePollGeneration
    const pollHardwareDetections = async () => {
      if (generation !== hardwarePollGeneration) return
      if (document.visibilityState === 'hidden') {
        hardwarePollTimer = window.setTimeout(pollHardwareDetections, 1000)
        return
      }
      await Promise.allSettled(slotCameras.value.map(async (camera, index) => {
        if (!camera?.detectionUrl) return
        const controller = new AbortController()
        const timeout = window.setTimeout(() => controller.abort(), 1500)
        try {
          const response = await fetch(camera.detectionUrl, { cache: 'no-store', signal: controller.signal })
          if (!response.ok) throw new Error(`HTTP ${response.status}`)
          const data = await response.json()
          const frame = Number(data.frame)
          if (Number.isFinite(frame) && hardwarePreviousFrames.get(camera.id) === frame) return
          if (Number.isFinite(frame)) hardwarePreviousFrames.set(camera.id, frame)
          const detections = Array.isArray(data.detections) ? data.detections : []
          const boxes = detections.map(item => {
            const raw = item.bbox || item.box || [item.x1, item.y1, item.x2, item.y2]
            const values = Array.isArray(raw) ? raw.map(Number) : []
            const width = Number(data.image_width) || 640
            const height = Number(data.image_height) || 480
            const normalized = values.length === 4 && values[2] > 1 ? [values[0] / width, values[1] / height, values[2] / width, values[3] / height] : values
            return { cls: String(item.class || item.label || item.name || 'target'), conf: Number(item.confidence ?? item.conf ?? item.score ?? 0), x1: normalized[0] || 0, y1: normalized[1] || 0, x2: normalized[2] || 0, y2: normalized[3] || 0 }
          })
          const personCount = boxes.filter(item => item.cls.toLowerCase() === 'person').length
          const motoCount = boxes.filter(item => /motor|bike|bicycle|电动车/i.test(item.cls)).length
          const noHelmetCount = boxes.filter(item => /no[_ -]?helmet|未戴头盔/i.test(item.cls)).length
          const helmetCount = boxes.filter(item => /^helmet$|已戴头盔/i.test(item.cls)).length
          const previous = hardwarePreviousCounts.get(camera.id) || { noHelmet: 0, helmet: 0 }
          const stats = {
            success: true,
            boxes,
            person_count: personCount,
            moto_count: motoCount,
            helmet_violations: noHelmetCount,
            helmet_new_incidents: Math.max(0, noHelmetCount - previous.noHelmet),
            helmet_worn: Math.max(0, helmetCount - previous.helmet),
            load_violation: false
          }
          hardwarePreviousCounts.set(camera.id, { noHelmet: noHelmetCount, helmet: helmetCount })
          handleDetectionResult(stats, index)
        } finally {
          window.clearTimeout(timeout)
        }
      }))
      if (generation === hardwarePollGeneration) {
        hardwarePollTimer = window.setTimeout(pollHardwareDetections, 750)
      }
    }
    void pollHardwareDetections()
    return
  }
  startDisplayOnlyPipeline()
  slotCameras.value.forEach((cam, index) => {
    if (cam && cam.status === 'online') connectServerPreviewSlot(index, cam)
  })
}

/** 仅开画布 rAF，不采摄像头、不上传帧（由服务端 WS 推图） */
const startDisplayOnlyPipeline = () => {
  streamActive.value = true
  startRenderLoop()
}
 
// ============================================================
// 启动本机摄像头捕获（推流至 /ws/detection）
// ============================================================
const startCapture = async () => {
  const v = hiddenVideo.value
  if (!v) return
  try {
    v.removeAttribute('src')
    v.src = ''
    mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false })
    v.srcObject = mediaStream
    await v.play()

    captureCanvas    = document.createElement('canvas')
    captureCanvas.width  = 320
    captureCanvas.height = 240
    captureCtx = captureCanvas.getContext('2d', { willReadFrequently: false })
 
    streamActive.value = true
    startRenderLoop()
    startSendLoop()
  } catch (err) {
    console.error('无法打开摄像头:', err)
    streamActive.value = false
    ElMessage.error('摄像头权限被拒绝或设备不可用')
  }
}
 
// ============================================================
// 渲染循环（requestAnimationFrame）
// ─ 始终以显示器刷新率运行（通常 60fps）
// ─ 直接把原始视频帧画到 canvas，再叠加最新检测框
// ─ 完全不等待 WebSocket 响应 → 显示永远流畅
// ============================================================
const startRenderLoop = () => {
  if (renderRafId) { cancelAnimationFrame(renderRafId); renderRafId = null }
  const render = () => {
    const renderNow = performance.now()
    if (hardwareMode.value && renderNow - lastHardwareRenderAt < 40) {
      renderRafId = requestAnimationFrame(render)
      return
    }
    if (hardwareMode.value) lastHardwareRenderAt = renderNow
    const video = hiddenVideo.value
    const layout = videoLayout.value
    const detIdx = activeVideo.value - 1
    const useServerPlot = serverPreviewMode.value

    if (useServerPlot || (video && video.readyState >= 2)) {
      for (let i = 0; i < layout; i++) {
        const canvas = canvasRefs.value[i]
        if (!canvas) continue
        const ctx = canvas.getContext('2d')
        const result = detectionResults.value[useServerPlot ? i : detIdx]
        if (useServerPlot) {
          ctx.fillStyle = '#000'
          ctx.fillRect(0, 0, canvas.width, canvas.height)
          if (hardwareMode.value && hardwareImages[i]?.complete) {
            ctx.drawImage(hardwareImages[i], 0, 0, canvas.width, canvas.height)
            if (showBoundingBoxes.value && result?.boxes?.length) drawDetectionBoxes(ctx, canvas.width, canvas.height, result.boxes)
          } else if (serverFrameBitmaps[i]) {
            ctx.drawImage(serverFrameBitmaps[i], 0, 0, canvas.width, canvas.height)
            if (showBoundingBoxes.value && result?.boxes?.length) {
              drawDetectionBoxes(ctx, canvas.width, canvas.height, result.boxes)
            }
          }
        } else {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
          if (showBoundingBoxes.value && result?.boxes?.length) {
            drawDetectionBoxes(ctx, canvas.width, canvas.height, result.boxes)
          }
        }
      }

      displayFrames++
      const now = Date.now()
      if (now - fpsTick >= 1000) {
        displayFps.value   = displayFrames
        detectionFps.value = detectionFrames
        displayFrames    = 0
        detectionFrames  = 0
        fpsTick          = now
      }
    }
    renderRafId = requestAnimationFrame(render)
  }
  renderRafId = requestAnimationFrame(render)
}
 
// ============================================================
// 绘制检测框（前端自己画，不依赖后端标注图）
// ============================================================
const drawDetectionBoxes = (ctx, cw, ch, boxes) => {
  ctx.save()
  ctx.lineWidth   = 2
  ctx.font        = 'bold 12px monospace'
  ctx.textBaseline = 'top'
 
  for (const box of boxes) {
    const color = BOX_COLORS[box.cls] || BOX_COLORS.default
    const x1 = box.x1 * cw
    const y1 = box.y1 * ch
    const x2 = box.x2 * cw
    const y2 = box.y2 * ch
    const w  = x2 - x1
    const h  = y2 - y1
    const label = `${box.cls} ${(box.conf * 100).toFixed(0)}%`
 
    // 框
    ctx.strokeStyle = color
    ctx.strokeRect(x1, y1, w, h)
 
    // 标签背景
    const tw = ctx.measureText(label).width
    ctx.fillStyle = color
    ctx.fillRect(x1, y1 - 16, tw + 6, 16)
 
    // 标签文字
    ctx.fillStyle = '#000'
    ctx.fillText(label, x1 + 3, y1 - 15)
  }
  ctx.restore()
}
 
// ============================================================
// 发送循环（setInterval，固定频率）
// ─ 与渲染循环完全解耦：即使 WS 慢/断，渲染不受影响
// ─ 用 toBlob 异步压缩，blobPending 防止重叠调用
// ============================================================
const startSendLoop = () => {
  sendIntervalId = setInterval(() => {
    if (
      blobPending ||
      !ws.value || ws.value.readyState !== WebSocket.OPEN ||
      !hiddenVideo.value || hiddenVideo.value.readyState < 2
    ) return
 
    blobPending = true
    // 先把视频帧画到小离屏 canvas（320×240）
    captureCtx.drawImage(hiddenVideo.value, 0, 0, captureCanvas.width, captureCanvas.height)
    captureCanvas.toBlob(blob => {
      blobPending = false
      if (blob && ws.value?.readyState === WebSocket.OPEN) {
        ws.value.send(blob)
      }
    }, 'image/jpeg', 0.6)   // 质量 0.6，约 5~15KB，比 640×480 节省 4倍带宽
  }, SEND_INTERVAL_MS)
}
 
const stopCapture = () => {
  if (renderRafId) { cancelAnimationFrame(renderRafId); renderRafId = null }
  if (sendIntervalId) { clearInterval(sendIntervalId); sendIntervalId = null }
  blobPending = false
  streamActive.value = false
  serverFrameBitmap = null
  revokePreviewObjectUrl()
  if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null }
  if (hiddenVideo.value) {
    hiddenVideo.value.pause()
    hiddenVideo.value.srcObject = null
    hiddenVideo.value.removeAttribute('src')
    hiddenVideo.value.src = ''
  }
  captureCanvas = null; captureCtx = null
}
 
// ============================================================
// WebSocket
// ============================================================
const parseBinaryPreviewPacket = (arrayBuffer) => {
  const view = new DataView(arrayBuffer)
  const jsonLen = view.getUint32(0, false)
  const jsonBytes = new Uint8Array(arrayBuffer, 4, jsonLen)
  const jpegBytes = new Uint8Array(arrayBuffer, 4 + jsonLen)
  const stats = JSON.parse(new TextDecoder().decode(jsonBytes))
  return { stats, jpegBytes }
}

const applyPreviewJpeg = (jpegBytes, slotIndex = activeVideo.value - 1) => {
  revokePreviewObjectUrl(slotIndex)
  const blob = new Blob([jpegBytes], { type: 'image/jpeg' })
  const objectUrl = URL.createObjectURL(blob)
  lastPreviewObjectUrls[slotIndex] = objectUrl
  if (slotIndex === activeVideo.value - 1) lastPreviewObjectUrl = objectUrl
  const im = new Image()
  im.onload = () => {
    serverFrameBitmaps[slotIndex] = im
    slotFrameReady.value[slotIndex] = true
    if (slotIndex === activeVideo.value - 1) serverFrameBitmap = im
  }
  im.src = objectUrl
}

const updateAiConnectionStatus = () => {
  const anyPreviewOpen = previewWsList.value.some(item => item && item.readyState === WebSocket.OPEN)
  aiConnectionStatus.value = anyPreviewOpen || (ws.value && ws.value.readyState === WebSocket.OPEN)
    ? 'connected'
    : 'disconnected'
}

const connectServerPreviewSlot = (slotIndex, camera) => {
  if (!camera || camera.status !== 'online') return
  const existing = previewWsList.value[slotIndex]
  if (existing) existing.close()

  const socket = new WebSocket(`${WS_VIDEO_PREVIEW_BASE}/${camera.id}`)
  socket.binaryType = 'arraybuffer'
  previewWsList.value[slotIndex] = socket

  socket.onopen = () => {
    aiConnectionStatus.value = 'connected'
  }

  socket.onmessage = (event) => {
    if (typeof event.data === 'string') {
      try {
        const j = JSON.parse(event.data)
        if (j.success === false && j.error) ElMessage.error(j.error)
      } catch (_) { /* ignore */ }
      return
    }
    try {
      const { stats, jpegBytes } = parseBinaryPreviewPacket(event.data)
      if (stats.success === false) {
        if (stats.error) ElMessage.error(stats.error)
        return
      }
      applyPreviewJpeg(jpegBytes, slotIndex)
      handleDetectionResult(stats, slotIndex)
    } catch (e) {
      console.error('解析本机摄像头预览失败', e)
    }
  }

  socket.onerror = () => {
    previewWsList.value[slotIndex] = null
    updateAiConnectionStatus()
  }
  socket.onclose = () => {
    previewWsList.value[slotIndex] = null
    updateAiConnectionStatus()
  }
}

const connectWebSocket = (cameraId) => {
  const wsUrl = serverPreviewMode.value
    ? `${WS_VIDEO_PREVIEW_BASE}/${cameraId}`
    : `${WS_BASE_URL}/${cameraId}`
  ws.value = new WebSocket(wsUrl)
  if (serverPreviewMode.value) ws.value.binaryType = 'arraybuffer'

  ws.value.onopen = () => {
    aiConnectionStatus.value = 'connected'
  }

  ws.value.onmessage = (event) => {
    if (serverPreviewMode.value) {
      if (typeof event.data === 'string') {
        try {
          const j = JSON.parse(event.data)
          if (j.success === false && j.error) ElMessage.error(j.error)
        } catch (_) { /* ignore */ }
        return
      }
      try {
        const { stats, jpegBytes } = parseBinaryPreviewPacket(event.data)
        if (stats.success === false) {
          if (stats.error) ElMessage.error(stats.error)
          return
        }
        applyPreviewJpeg(jpegBytes)
        handleDetectionResult(stats)
      } catch (e) {
        console.error('解析二进制预览失败', e)
      }
      return
    }

    if (typeof event.data !== 'string') return
    try {
      const stats = JSON.parse(event.data)
      handleDetectionResult(stats)
    } catch (e) {
      console.error('解析检测结果失败', e)
    }
  }

  ws.value.onerror  = () => { aiConnectionStatus.value = 'disconnected'; ElMessage.error('AI 检测服务连接失败') }
  ws.value.onclose  = () => { aiConnectionStatus.value = 'disconnected' }
}
 
const disconnectWebSocket = () => {
  if (ws.value) { ws.value.close(); ws.value = null }
  previewWsList.value.forEach(item => { if (item) item.close() })
  previewWsList.value = Array(videoLayout.value).fill(null)
  revokePreviewObjectUrl()
  aiConnectionStatus.value = 'disconnected'
  stopHardwarePolling()
}

const stopHardwarePolling = () => {
  hardwarePollGeneration += 1
  if (hardwarePollTimer) { clearTimeout(hardwarePollTimer); hardwarePollTimer = null }
  hardwarePreviousFrames.clear()
}
 
// ============================================================
// 处理检测结果
// ============================================================
const handleDetectionResult = (stats, slotIndex = activeVideo.value - 1) => {
  if (!stats.success) return
  const idx = slotIndex
  detectionResults.value[idx] = stats   // 渲染循环会在下一帧自动叠加
 
  detectionFrames++
 
  totalStats.value.frames += 1
  totalStats.value.persons += stats.person_count || 0
  totalStats.value.motos   += stats.moto_count   || 0
  // 与 Python 轨迹去重一致：优先用本帧实际上报的「事件数」，旧服务无该字段时退回每帧检测个数
  totalStats.value.helmetViolations +=
    stats.helmet_new_incidents ?? stats.helmet_violations ?? 0
  totalStats.value.helmetWorn       += stats.helmet_worn || 0
  if (stats.load_violation) totalStats.value.loadViolations += 1
 
  if (idx === activeVideo.value - 1) {
    currentFrameStats.value = {
      person:         stats.person_count || 0,
      moto:           stats.moto_count   || 0,
      helmetViolation: stats.helmet_violations || 0,
      helmetWorn:     stats.helmet_worn   || 0,
      loadViolation:  stats.load_violation,
    }
  }
}
 
// ============================================================
// 视频布局切换
// ============================================================
const changeVideoLayout = (layout) => {
  if (serverPreviewMode.value) disconnectWebSocket()
  videoLayout.value      = layout
  detectionResults.value = Array(layout).fill(null)
  canvasRefs.value       = Array(layout).fill(null)
  videoWrapperRefs.value = Array(layout).fill(null)
  previewWsList.value    = Array(layout).fill(null)
  slotFrameReady.value   = Array(layout).fill(false)
  serverFrameBitmaps     = Array(layout).fill(null)
  lastPreviewObjectUrls  = Array(layout).fill(null)
  activeVideo.value      = 1
  if (serverPreviewMode.value) {
    nextTick(() => startMultiCameraPreview())
  }
}
 
// ============================================================
// 抓拍（从当前 canvas 取帧）
// ============================================================
const takeSnapshot = async () => {
  const cam = serverPreviewMode.value ? getSlotCamera(activeVideo.value) : selectedCamera.value
  if (!cam || cam.status !== 'online') { ElMessage.warning('请先选择摄像头'); return }
  const canvas = canvasRefs.value[activeVideo.value - 1]
  if (!canvas) { ElMessage.warning('当前没有可抓拍的画面'); return }
  const base64Data = canvas.toDataURL('image/jpeg', 0.8).split(',')[1]
  try {
    await axios.post(`${API_BASE_URL}/snapshot`, { camera_id: cam.id, frame: base64Data })
    ElMessage.success('抓拍成功，已保存至服务器')
  } catch { ElMessage.error('抓拍保存失败') }
}
 
const takeSnapshotByIndex = async (index) => {
  const prev = activeVideo.value; activeVideo.value = index
  await takeSnapshot(); activeVideo.value = prev
}
 
// ============================================================
// 全屏、录像
// ============================================================
const toggleRecording = () => { isRecording.value = !isRecording.value; ElMessage.success(isRecording.value ? '开始录像' : '停止录像') }
 
const openFullscreenSnapshot = (index) => {
  const canvas = canvasRefs.value[index - 1]
  if (!canvas) { ElMessage.warning('当前画面无图像'); return }
  fullscreenImageSrc.value = canvas.toDataURL('image/jpeg', 0.9)
  fullscreenVideo.value    = true
}

const toggleFullscreenCanvas = async (index) => {
  const wrap = videoWrapperRefs.value[index - 1]
  if (!wrap) {
    ElMessage.warning('画面未就绪')
    return
  }
  try {
    if (document.fullscreenElement === wrap) {
      await document.exitFullscreen()
    } else if (wrap.requestFullscreen) {
      await wrap.requestFullscreen()
    } else {
      openFullscreenSnapshot(index)
    }
  } catch {
    openFullscreenSnapshot(index)
  }
}
const exitFullscreen = () => { fullscreenVideo.value = false; fullscreenImageSrc.value = '' }
 
const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN', { year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit' }) : ''
const handleSearch = () => { pagination.value.pageNum = 1 }
const prevPage = () => { if (pagination.value.pageNum > 1) pagination.value.pageNum-- }
const nextPage = () => { if (pagination.value.pageNum < listTotalPages.value) pagination.value.pageNum++ }
const switchTab = (tab) => ElMessage.info(`切换到 ${tab === 'history' ? '历史回放' : '摄像头监控'}`)
 
// ============================================================
// 地图（百度地图）—— 与原版完全一致
// ============================================================
const initMap = () => {
  if (typeof BMap === 'undefined') { loadBaiduMapAPI(); return }
  nextTick(() => {
    try {
      if (!mapContainer.value) return
      map = new BMap.Map(mapContainer.value)
      const point = new BMap.Point(112.6827629, 26.8860959)
      map.centerAndZoom(point, 16)
      map.enableScrollWheelZoom(true)
      map.addControl(new BMap.ScaleControl({ anchor: BMAP_ANCHOR_BOTTOM_LEFT }))
      map.addControl(new BMap.ZoomControl())
      map.addControl(new BMap.NavigationControl())
      map.addControl(new BMap.MapTypeControl({ mapTypes: [BMAP_NORMAL_MAP, BMAP_HYBRID_MAP] }))
      updateCameraMarkers()
    } catch { ElMessage.error('地图初始化失败') }
  })
}
const loadBaiduMapAPI = () => {
  if (typeof BMap !== 'undefined') { initMap(); return }
  const script = document.createElement('script')
  script.src = 'https://api.map.baidu.com/api?v=3.0&ak=SUBe4C3SHSL9Uyd9P4xgq6GMdaZF2Irj&callback=initBaiduMap'
  window.initBaiduMap = initMap
  script.onerror = () => ElMessage.error('百度地图API加载失败')
  document.head.appendChild(script)
}
const updateCameraMarkers = () => {
  if (!map || !cameraList.value.length) return
  map.clearOverlays(); mapMarkers = []
  cameraList.value.forEach(camera => {
    if (!camera.lng || !camera.lat) return
    const point = new BMap.Point(camera.lng, camera.lat)
    const icon  = new BMap.Icon(
      camera.status === 'online' ? 'https://api.map.baidu.com/images/marker_red.png' : 'https://api.map.baidu.com/images/marker_gray.png',
      new BMap.Size(23, 25)
    )
    const marker = new BMap.Marker(point, { icon })
    marker.addEventListener('click', () => selectCamera(camera))
    const iw = new BMap.InfoWindow(`<div style="padding:10px;min-width:200px"><h4 style="margin:0 0 10px;color:#4b8df8">${camera.name}</h4><p><strong>位置：</strong>${camera.location}</p><p><strong>状态：</strong><span style="color:${camera.status==='online'?'green':'red'}">${camera.status==='online'?'在线':'离线'}</span></p><button onclick="window.selectCameraById(${camera.id})" style="margin-top:10px;padding:5px 10px;background:#4b8df8;color:#fff;border:none;border-radius:3px;cursor:pointer">切换到此摄像头</button></div>`)
    marker.addEventListener('click', () => map.openInfoWindow(iw, point))
    map.addOverlay(marker)
    mapMarkers.push({ marker, camera })
    if (selectedCamera.value?.id === camera.id) marker.setAnimation(BMAP_ANIMATION_BOUNCE)
  })
  window.selectCameraById = (id) => { const c = cameraList.value.find(c => c.id == id); if (c) selectCamera(c) }
}
const locateCameraOnMap = (camera) => {
  if (!map || !camera.lng || !camera.lat) return
  const point = new BMap.Point(camera.lng, camera.lat)
  map.panTo(point); map.setZoom(18)
  mapMarkers.forEach(m => m.marker.setAnimation(null))
  const found = mapMarkers.find(m => m.camera.id === camera.id)
  if (found) found.marker.setAnimation(BMAP_ANIMATION_BOUNCE)
}
const centerMapOnCamera = () => { if (selectedCamera.value) locateCameraOnMap(selectedCamera.value) }
const switchMapMode = (mode) => {
  mapMode.value = mode
  if (mode === 'camera' && map) nextTick(() => map.resize())
}
 
// ============================================================
// 生命周期
// ============================================================
let timeTimer = null
const adjustLayout = () => {
  const el = document.getElementById('car_control')
  if (el) el.style.height = `${window.innerHeight - 80}px`
}

onMounted(() => {
  adjustLayout()
  window.addEventListener('resize', adjustLayout)
  fetchCameras()
  if (showMap) setTimeout(loadBaiduMapAPI, 500)
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  window.removeEventListener('resize', adjustLayout)
  if (timeTimer) clearInterval(timeTimer)
  stopCapture()
  disconnectWebSocket()
  if (map) { map.clearOverlays(); map = null }
  delete window.selectCameraById
})
</script>
 
<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; list-style: none; text-decoration: none; }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,.1); border-radius: 3px; }
::-webkit-scrollbar-thumb { background: rgba(75,141,248,.6); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(75,141,248,.8); }
body { font-family: 'Microsoft YaHei', Arial, sans-serif; font-size: 14px; color: #fff; background-color: #081832; overflow: hidden; }
.camera-monitor-container { height: 100vh; overflow: hidden; background: #081832; display: flex; flex-direction: column; }
/* 供 canvas 抓帧：必须可见尺寸，不能用 display:none，否则多浏览器不解码 */
.video-capture-source {
  position: fixed;
  width: 640px;
  height: 480px;
  left: -9999px;
  top: 0;
  opacity: 0.001;
  pointer-events: none;
  z-index: -1;
  object-fit: fill;
}
.left { float: left; }
.right { float: right; }
.header { width: 100%; height: 70px; background: linear-gradient(135deg,#030829 0%,#0a1e3d 100%); display: flex; align-items: center; justify-content: space-between; padding: 0 20px; box-shadow: 0 2px 10px rgba(0,0,0,.3); z-index: 100; flex-shrink: 0; }
.nav > ul { display: flex; align-items: center; gap: 10px; }
.nav > ul > li { padding: 8px 20px; border-radius: 5px; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all .3s; position: relative; }
.nav > ul > li:hover { background: rgba(75,141,248,.2); }
.nav > ul > li.nav_active { background: rgba(75,141,248,.3); box-shadow: 0 0 15px rgba(75,141,248,.5); }
.nav > ul > li.nav_active::after { content: ''; position: absolute; bottom: -2px; left: 20%; width: 60%; height: 3px; background: #4b8df8; border-radius: 3px; }
.nav_2 { width: 20px; height: 20px; background: url('/img/nav-monitor.png') no-repeat center; background-size: contain; }
.nav_icon { width: 18px; height: 18px; display: inline-block; }
.nav_icon.history { background: url('/img/nav-history.png') no-repeat center; background-size: contain; }
.header-right { display: flex; align-items: center; flex-wrap: wrap; justify-content: flex-end; gap: 10px; font-size: 12px; color: #27d4ff; }
.header-status-chips { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.chip { padding: 4px 10px; border-radius: 4px; background: rgba(7,41,81,.85); border: 1px solid rgba(75,141,248,.25); color: rgba(255,255,255,.9); white-space: nowrap; }
.chip-time { color: #27d4ff; }
.chip-ok { border-color: rgba(82,196,26,.5); color: #95de64; }
.chip-warn { border-color: rgba(255,77,79,.45); color: #ff9c9e; }
.chip-live { background: rgba(255,77,79,.25); border-color: #ff4d4f; color: #ffccc7; font-weight: 600; letter-spacing: 0.05em; animation: live-pulse 2s infinite; }
.chip-demo { border-color: rgba(250, 219, 20, .55); color: #ffe58f; background: rgba(250, 219, 20, .12); }
.chip-videotest { border-color: rgba(82, 196, 26, .55); color: #b7eb8f; background: rgba(82, 196, 26, .15); }
.toggle-demo-video, .toggle-server-vt { display: flex; align-items: center; gap: 6px; font-size: 12px; color: rgba(255,255,255,.85); cursor: pointer; user-select: none; margin-right: 8px; }
.toggle-demo-video input { accent-color: #faad14; cursor: pointer; }
.toggle-server-vt input { accent-color: #52c41a; cursor: pointer; }
.chip-muted { opacity: .85; max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
.fps-inline { display: flex; gap: 12px; font-size: 11px; color: rgba(255,255,255,.65); margin-right: 8px; }
.toggle-boxes { display: flex; align-items: center; gap: 6px; font-size: 12px; color: rgba(255,255,255,.8); cursor: pointer; user-select: none; margin-right: 8px; }
.toggle-boxes input { accent-color: #4b8df8; cursor: pointer; }
.list-skeleton { display: flex; flex-direction: column; gap: 10px; padding: 8px 0; }
.sk-row { height: 48px; border-radius: 6px; background: linear-gradient(90deg, rgba(7,41,81,.5) 25%, rgba(75,141,248,.15) 50%, rgba(7,41,81,.5) 75%); background-size: 200% 100%; animation: sk-shine 1.2s ease-in-out infinite; }
@keyframes sk-shine { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.list-empty { padding: 24px 12px; text-align: center; font-size: 12px; color: rgba(255,255,255,.5); line-height: 1.6; }
.video-placeholder { position: absolute; inset: 0; z-index: 3; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 16px; text-align: center; pointer-events: none; background: rgba(0,0,0,.72); color: #fff; font-size: 13px; }
.video-placeholder .sub { margin-top: 8px; font-size: 11px; color: rgba(255,255,255,.55); line-height: 1.5; max-width: 280px; }
.video-placeholder.warn { background: rgba(60,20,20,.78); }
.video-placeholder.warn .sub { color: rgba(255,200,200,.7); }
.con1 { width: 100%; height: calc(100vh - 70px); min-height: 0; background: transparent; display: flex; padding: 10px; gap: 10px; overflow: hidden; }
.camera_left { width: 23%; height: 100%; display: flex; flex-direction: column; gap: 10px; }
.bow_shadow { background: rgba(10,30,61,.9); border-radius: 8px; border: 1px solid rgba(75,141,248,.2); box-shadow: 0 4px 15px rgba(0,0,0,.3); backdrop-filter: blur(10px); overflow: hidden; }
.left_up { flex: 1.5; display: flex; flex-direction: column; padding: 15px; }
.search-box { display: flex; gap: 5px; margin-bottom: 15px; }
.search-input { flex: 1; background: rgba(7,41,81,.8); border: 1px solid rgba(75,141,248,.3); border-radius: 4px; color: #fff; padding: 8px 12px; font-size: 12px; outline: none; transition: border-color .3s; }
.search-input:focus { border-color: #4b8df8; }
.search-input::placeholder { color: rgba(255,255,255,.5); }
.find_but { width: 36px; height: 36px; background: rgba(75,141,248,.6); border: none; border-radius: 4px; cursor: pointer; transition: background-color .3s; }
.find_but:hover { background-color: rgba(75,141,248,.8); }
.filter-tabs { display: flex; gap: 5px; margin-bottom: 15px; }
.set_list { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #eee; font-size: 13px; }
.list_i { width: 16px; height: 16px; background: url('/img/list.png') no-repeat center; background-size: contain; }
.camera-list { flex: 1; overflow-y: auto; }
.camera-item { display: flex; align-items: center; padding: 10px; margin-bottom: 8px; background: rgba(7,41,81,.6); border-radius: 6px; cursor: pointer; transition: all .3s; border: 1px solid transparent; }
.camera-item:hover { background: rgba(75,141,248,.2); transform: translateY(-2px); }
.camera-item.active { background: rgba(75,141,248,.3); border-color: #4b8df8; box-shadow: 0 0 10px rgba(75,141,248,.3); }
.camera-icon { width: 36px; height: 36px; background: rgba(75,141,248,.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 10px; flex-shrink: 0; }
.camera-icon-img { width: 20px; height: 20px; background: url('/img/camera-icon.png') no-repeat center; background-size: contain; filter: invert(1); }
.camera-info { flex: 1; min-width: 0; overflow: hidden; }
.camera-name { font-weight: 500; margin-bottom: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.camera-location { font-size: 11px; color: rgba(255,255,255,.7); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.camera-status { display: flex; flex-direction: column; align-items: center; gap: 2px; margin-left: 5px; flex-shrink: 0; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.online { background: #52c41a; box-shadow: 0 0 6px #52c41a; }
.status-dot.offline { background: #ff4d4f; box-shadow: 0 0 6px #ff4d4f; }
.status-text { font-size: 11px; color: rgba(255,255,255,.7); }
.pagination { display: flex; justify-content: center; align-items: center; gap: 10px; margin-top: 15px; padding: 10px; }
.page-btn { width: 28px; height: 28px; background: rgba(7,41,81,.8); border: 1px solid rgba(75,141,248,.3); border-radius: 4px; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .3s; }
.page-btn:hover:not(:disabled) { background: rgba(75,141,248,.3); }
.page-btn:disabled { opacity: .5; cursor: not-allowed; }
.page-info { font-size: 12px; color: rgba(255,255,255,.7); }
.left_down { flex: 1; display: flex; flex-direction: column; padding: 15px; }
.text_center { display: flex; gap: 5px; margin-bottom: 15px; }
.tab_a { flex: 1; padding: 10px; text-align: center; background: rgba(102,127,167,.3); border-radius: 6px; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 5px; transition: all .3s; font-size: 13px; }
.tab_a:hover { background: rgba(75,141,248,.3); }
.tab_aActive { background: rgba(75,141,248,.6) !important; font-weight: 500; box-shadow: 0 0 10px rgba(75,141,248,.3); }
.tab-icon { width: 14px; height: 14px; display: inline-block; }
.camera_content { flex: 1; overflow-y: auto; padding: 5px 0; }
.info-panel, .video-info-panel { display: flex; flex-direction: column; gap: 15px; }
.info-section { background: rgba(7,41,81,.4); border-radius: 6px; padding: 12px; }
.info-title { color: #4b8df8; font-size: 13px; font-weight: 500; margin-bottom: 10px; padding-bottom: 5px; border-bottom: 1px solid rgba(75,141,248,.3); }
.info-row { display: flex; padding: 6px 0; border-bottom: 1px solid rgba(255,255,255,.1); }
.info-row:last-child { border-bottom: none; }
.info-label { width: 90px; color: #4b8df8; font-size: 12px; flex-shrink: 0; }
.info-value { flex: 1; color: #fff; font-size: 12px; word-break: break-all; }
.url-value { color: #27d4ff; font-size: 11px; }
.status-badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 500; }
.status-badge.online { background: rgba(82,196,26,.2); color: #52c41a; }
.status-badge.offline { background: rgba(255,77,79,.2); color: #ff4d4f; }
.location { display: flex; align-items: center; gap: 5px; }
.location-icon { width: 16px; height: 16px; background: url('/img/location.png') no-repeat center; background-size: contain; cursor: pointer; opacity: .7; flex-shrink: 0; }
.location-icon:hover { opacity: 1; }
.config-item, .stats-item { display: flex; padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,.1); }
.config-item:last-child, .stats-item:last-child { border-bottom: none; }
.config-label, .stats-label { width: 70px; color: #4b8df8; font-size: 12px; flex-shrink: 0; }
.config-value, .stats-value { flex: 1; color: #fff; font-size: 12px; }
.camera_center { width: 48%; height: 100%; min-height: 0; display: flex; flex-direction: column; gap: 10px; }
.video-controls-bar { background: rgba(10,30,61,.9); border-radius: 8px; border: 1px solid rgba(75,141,248,.2); padding: 12px 15px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.video-controls-left, .video-controls-right { display: flex; align-items: center; gap: 10px; }
.layout-controls, .action-controls { display: flex; gap: 5px; }
.video-btn { padding: 6px 12px; border-radius: 4px; color: #fff; cursor: pointer; font-size: 12px; transition: all .3s; display: flex; align-items: center; gap: 5px; border: 1px solid rgba(75,141,248,.3); background: rgba(7,41,81,.8); }
.video-btn:hover:not(:disabled) { background: rgba(75,141,248,.3); }
.video-btn.active { background: rgba(75,141,248,.6); }
.video-btn:disabled { opacity: .5; cursor: not-allowed; }
.layout-btn { min-width: 70px; }
.action-btn { min-width: 80px; }
.fullscreen-btn { background: rgba(82,196,26,.2); border-color: rgba(82,196,26,.3); }
.layout-icon { width: 12px; height: 12px; display: inline-block; }
.snapshot-icon, .record-icon, .fullscreen-icon { width: 12px; height: 12px; background: center/contain no-repeat; }
.snapshot-icon  { background-image: url('/img/snapshot.png'); }
.record-icon    { background-image: url('/img/record.png'); }
.record-icon.recording { background-image: url('/img/recording.png'); animation: recording-pulse 1.5s infinite; }
.fullscreen-icon { width: 14px; height: 14px; background-image: url('/img/fullscreen.png'); }
@keyframes recording-pulse { 0%,100%{opacity:1} 50%{opacity:.5} }
.video-container { flex: 1; min-height: 0; display: grid; gap: 10px; background: rgba(10,30,61,.9); border-radius: 8px; border: 1px solid rgba(75,141,248,.2); padding: 15px; overflow: hidden; }
.video-container.layout-1 { grid-template-columns: 1fr; grid-template-rows: 1fr; }
.video-container.layout-4 { grid-template-columns: repeat(2,1fr); grid-template-rows: repeat(2,1fr); }
.video-container.layout-6 { grid-template-columns: repeat(3,1fr); grid-template-rows: repeat(2,1fr); }
.video-item { position: relative; border-radius: 6px; overflow: hidden; border: 2px solid transparent; transition: all .3s; background: #000; }
.video-item:hover { border-color: rgba(75,141,248,.5); }
.video-item.active { border-color: #4b8df8; box-shadow: 0 0 15px rgba(75,141,248,.5); }
.video-wrapper { width: 100%; height: 100%; min-height: 0; position: relative; }
.video-img { width: 100%; height: 100%; object-fit: cover; background: #000; display: block; }
.hardware-direct-img { position: absolute; inset: 0; }
.direct-box-layer { position: absolute; inset: 0; z-index: 2; pointer-events: none; }
.direct-detect-box { position: absolute; border: 2px solid #00bfff; box-shadow: 0 0 6px rgba(0,191,255,.55); }
.direct-detect-box span { position: absolute; top: -18px; left: -2px; padding: 1px 4px; color: #001428; background: #00bfff; font: bold 11px monospace; white-space: nowrap; }
.video-overlay { position: absolute; top: 0; left: 0; right: 0; padding: 8px 10px; background: linear-gradient(to bottom,rgba(0,0,0,.7) 0%,rgba(0,0,0,0) 100%); display: flex; justify-content: space-between; align-items: center; }
.video-title { font-size: 12px; color: #fff; display: flex; align-items: center; gap: 5px; }
.live-badge { background: #ff4d4f; color: white; padding: 1px 5px; border-radius: 3px; font-size: 10px; animation: live-pulse 2s infinite; }
@keyframes live-pulse { 0%,100%{opacity:1} 50%{opacity:.7} }
.video-status { font-size: 10px; padding: 2px 6px; border-radius: 3px; background: rgba(0,0,0,.5); color: #fff; display: flex; gap: 6px; }
.video-status .warn { color: #ff4d4f; font-weight: bold; }
.video-controls-mini { position: absolute; bottom: 8px; right: 8px; display: flex; gap: 5px; opacity: 0; transition: opacity .3s; }
.video-item:hover .video-controls-mini { opacity: 1; }
.tool-btn { width: 24px; height: 24px; background: rgba(0,0,0,.7); border-radius: 4px; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .3s; }
.tool-btn:hover { background: rgba(75,141,248,.8); }
.snapshot-small-icon { width: 12px; height: 12px; background: url('/img/snapshot-small.png') center/contain no-repeat; }
.fullscreen-small-icon { width: 12px; height: 12px; background: url('/img/fullscreen-small.png') center/contain no-repeat; }
.camera_right { width: 29%; height: 100%; display: flex; flex-direction: column; gap: 10px; }
.map-container { flex: 1; display: flex; flex-direction: column; background: rgba(10,30,61,.9); border-radius: 8px; border: 1px solid rgba(75,141,248,.2); overflow: hidden; }
.map-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 15px; background: rgba(7,41,81,.6); }
.map-title { font-size: 14px; font-weight: 500; color: #4b8df8; }
.map-controls { display: flex; gap: 5px; }
.map-btn { display: flex; align-items: center; gap: 5px; padding: 5px 10px; background: rgba(75,141,248,.3); border: 1px solid rgba(75,141,248,.5); border-radius: 4px; color: #fff; cursor: pointer; font-size: 12px; transition: all .3s; }
.map-btn:hover { background: rgba(75,141,248,.5); }
.map-btn.active { background: rgba(75,141,248,.8); border-color: #4b8df8; }
.camera-dist-icon, .ai-info-icon { width: 12px; height: 12px; display: inline-block; background-size: contain; background-repeat: no-repeat; background-position: center; }
.camera-dist-icon { background-image: url('/img/refresh.png'); }
.ai-info-icon { background-image: url('/img/locate.png'); }
#map { flex: 1; background: #f0f0f0; width: 100%; height: 100%; }
.map-placeholder { flex: 1; display: flex; align-items: center; justify-content: center; color: rgba(75,141,248,.5); font-size: 14px; }
.map-info { padding: 12px 15px; background: rgba(7,41,81,.6); border-top: 1px solid rgba(75,141,248,.2); }
.ai-info-panel { flex: 1; background: rgba(10,30,61,.95); padding: 20px; overflow-y: auto; display: flex; flex-direction: column; }
.ai-panel-placeholder { flex: 1; }
.ai-stats-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 1px solid rgba(75,141,248,.3); }
.ai-title { font-size: 16px; font-weight: 500; color: #4b8df8; letter-spacing: 1px; }
.camera-tag { background: rgba(75,141,248,.2); padding: 4px 12px; border-radius: 16px; font-size: 12px; color: #27d4ff; }
.ai-stats-grid { display: flex; flex-direction: column; gap: 20px; }
.stat-card { background: rgba(7,41,81,.6); border-radius: 8px; padding: 15px; border: 1px solid rgba(75,141,248,.2); }
.stat-card.current { border-left: 4px solid #52c41a; }
.stat-card.total    { border-left: 4px solid #4b8df8; }
.stat-label { color: #4b8df8; font-size: 14px; font-weight: 500; margin-bottom: 12px; }
.stat-value { color: #fff; font-size: 13px; line-height: 1.8; padding: 2px 0; display: flex; align-items: center; gap: 6px; }
.stat-value.warn { color: #ff4d4f; font-weight: 500; }
.ai-status { margin-top: 20px; padding-top: 15px; border-top: 1px solid rgba(255,255,255,.1); display: flex; align-items: center; gap: 8px; font-size: 12px; color: rgba(255,255,255,.7); }
.status-led { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.status-led.connected    { background: #52c41a; box-shadow: 0 0 8px #52c41a; }
.status-led.disconnected { background: #ff4d4f; box-shadow: 0 0 8px #ff4d4f; }
.fullscreen-modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,.95); z-index: 2000; display: flex; align-items: center; justify-content: center; }
.fullscreen-content { position: relative; width: 90%; height: 90%; display: flex; align-items: center; justify-content: center; }
.fullscreen-img { max-width: 100%; max-height: 100%; object-fit: contain; }
.fullscreen-close { position: absolute; top: 20px; right: 20px; width: 40px; height: 40px; background: rgba(0,0,0,.7); border: none; border-radius: 50%; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .3s; }
.fullscreen-close:hover { background: rgba(255,77,79,.8); transform: scale(1.1); }
.close-icon { width: 20px; height: 20px; background: url('/img/close.png') center/contain no-repeat; }
@media screen and (max-width:1600px) { .camera_left{width:25%} .camera_center{width:45%} .camera_right{width:30%} }
@media screen and (max-width:1400px) { .camera_left{width:28%} .camera_center{width:42%} .camera_right{width:30%} }
@media screen and (max-width:1200px) { .con1{flex-direction:column;height:auto;overflow-y:auto} .camera_left,.camera_center,.camera_right{width:100%;height:auto;margin-bottom:15px} .camera_left{flex-direction:row;gap:15px} .left_up{flex:2;min-height:500px} .left_down{flex:1;min-height:500px} }
@media screen and (max-width:768px) { .video-container.layout-4{grid-template-columns:1fr;grid-template-rows:repeat(4,200px)} }
</style>

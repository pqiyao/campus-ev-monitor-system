<template>
  <div class="monitor-container page-bg-shell">
    <!-- 顶部导航框架 -->
    <header class="header">
      <span class="head-logo">红外巡检</span>
      <ul class="head-ul">
        <li><a class="active-a" href="javascript:;">实时监控</a></li>
        <li><a href="javascript:;">数据报表</a></li>
      </ul>
      <div class="header-right">
        <span>{{ currentTime }}</span>
      </div>
    </header>

    <!-- 主内容区域布局框架 -->
    <section class="section">
      <div class="main-layout">
        <!-- 左侧：红外热源检测 -->
        <div class="layout-item infrared-section">
          <div class="infrared-container" id="car_control">
            <div class="infrared-box" id="infrared_box">
              <!-- 巡检画面 -->
              <video
                ref="videoRef"
                v-show="cameraReady"
                class="infrared-video"
                :class="{ paused: !isPlaying }"
                :src="infraredVideoSrc"
                autoplay
                loop
                muted
                playsinline
              ></video>

              <div
                v-show="!cameraReady"
                class="device-standby"
              >
                <div class="standby-title">红外画面未启动</div>
                <div class="standby-desc">请启动巡检画面或接入红外设备</div>
              </div>

              <!-- 高温区域框 -->
              <div
                v-if="thermalState.alarm"
                class="hotspot-box"
                :style="{
                  left: thermalState.hotspot.x + '%',
                  top: thermalState.hotspot.y + '%',
                  width: thermalState.hotspot.width + '%',
                  height: thermalState.hotspot.height + '%'
                }"
              >
                <span>高温区域 {{ thermalState.maxTemperature.toFixed(1) }}℃</span>
              </div>

              <!-- 视频控制覆盖层 -->
              <div class="video-controls">
                <button class="control-btn play-btn" @click="togglePlay">
                  <span>{{ isPlaying ? '⏸️ 暂停' : '▶️ 播放' }}</span>
                </button>

                <button class="control-btn" @click="startCamera">
                  <span>📷 启动画面</span>
                </button>

                <button class="control-btn danger-btn" @click="triggerHighTempAlarm">
                  <span>🔥 触发高温告警</span>
                </button>

                <button class="control-btn safe-btn" @click="recoverTemperature">
                  <span>✅ 恢复正常</span>
                </button>

                <button class="control-btn fullscreen-btn" @click="toggleFullscreen('infrared')">
                  <span>🖥️ 全屏</span>
                </button>
              </div>

              <!-- 红外画面信息叠加 -->
              <div class="infrared-info">
                <div class="info-item">
                  <span class="info-label">巡检模式：</span>
                  <span class="info-value">{{ thermalState.mode }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">设备编号：</span>
                  <span class="info-value">{{ thermalState.deviceId }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">监测区域：</span>
                  <span class="info-value">{{ thermalState.location }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">温度范围：</span>
                  <span class="info-value">
                    {{ thermalState.minTemperature.toFixed(1) }}℃ -
                    {{ thermalState.maxTemperature.toFixed(1) }}℃
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">告警阈值：</span>
                  <span class="info-value">{{ thermalState.threshold }}℃</span>
                </div>
                <div class="info-item">
                  <span class="info-label">异常检测：</span>
                  <span
                    class="info-value"
                    :class="thermalState.alarm ? 'error' : 'normal'"
                  >
                    {{ thermalState.alarm ? '疑似电池异常高温' : '无异常' }}
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">绑定车辆：</span>
                  <span class="info-value" :class="selectedVehicle ? 'normal' : 'warning'">
                    {{ selectedVehicleDisplay }}
                  </span>
                </div>
                <div class="info-item" v-if="cameraError">
                  <span class="info-label">画面：</span>
                  <span class="info-value warning">{{ cameraError }}</span>
                </div>
              </div>
            </div>

            <!-- 实时状态悬浮窗 -->
            <div class="realtime-status">
              <div class="status-item">
                <span class="status-label">今日违规</span>
                <span class="status-value">{{ todayAlarmCount }}</span>
              </div>
              <div class="status-item">
                <span class="status-label">处理中</span>
                <span class="status-value">{{ processingCount }}</span>
              </div>
              <div class="status-item">
                <span class="status-label">未处理</span>
                <span class="status-value">{{ pendingCount }}</span>
              </div>
            </div>

            <!-- 趋势图容器 -->
            <div class="trend-chart-wrapper">
              <div class="chart-title">近7天高温告警趋势</div>
              <div class="trend-chart">
                <div
                  v-for="item in trendData"
                  :key="item.day"
                  class="trend-bar-item"
                >
                  <div class="trend-bar-track">
                    <div
                      class="trend-bar-fill"
                      :class="{ danger: item.count > 3 }"
                      :style="{ height: Math.max(item.count * 18, 8) + 'px' }"
                    ></div>
                  </div>
                  <div class="trend-day">{{ item.day }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间：视频+报警区域 -->
        <div class="layout-item video-alert-section">
          <!-- 视频监控区域 -->
          <div class="panel-card video-panel">
            <div class="panel-header">视频监控</div>
            <div class="panel-body">
              <div class="camera-item">
                <div class="camera-header">
                  <span class="camera-name">红外巡检画面（{{ currentDate }}）</span>
                  <span class="camera-actions">
                    <span class="camera-status" :class="cameraReady ? 'online' : 'warning'">
                      {{ cameraReady ? '在线' : '未启动' }}
                    </span>
                  </span>
                </div>
                <div class="camera-video">
                  <video
                    ref="miniVideoRef"
                    v-show="cameraReady"
                    class="camera-img camera-video-live"
                    :src="infraredVideoSrc"
                    autoplay
                    loop
                    muted
                    playsinline
                  ></video>
                  <div
                    v-show="!cameraReady"
                    class="camera-placeholder"
                  >
                    设备画面未启动
                  </div>
                </div>
              </div>

              <div class="camera-item">
                <div class="camera-header">
                  <span class="camera-name">宿舍区停车棚A区</span>
                  <span class="camera-actions">
                    <span class="camera-status online">在线</span>
                  </span>
                </div>
                <div class="camera-video">
                  <div class="camera-placeholder">设备画面接入中</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 报警信息区域 -->
          <div class="panel-card alert-panel">
            <div class="panel-header">最新违规记录</div>
            <div class="panel-body">
              <div class="alert-header">
                <button class="refresh-btn" @click="fetchViolationAlerts">↻ 刷新</button>
              </div>

              <ul class="alert-list">
                <li v-if="alarmRecords.length === 0" class="empty-state">
                  暂无违规记录
                </li>

                <li
                  v-for="record in alarmRecords"
                  :key="record.id"
                  class="alert-record"
                >
                  <div class="alert-record-top">
                    <span class="alert-type">{{ record.type }}</span>
                    <span class="alert-level" :class="record.levelClass">{{ record.level }}</span>
                  </div>

                  <div class="alert-record-row">
                    <span>位置：</span>
                    <b>{{ record.location }}</b>
                  </div>

                  <div class="alert-record-row">
                    <span>最高温：</span>
                    <b class="temp-danger">{{ formatTemperature(record.maxTemperature) }}</b>
                    <span class="threshold-text">阈值 {{ record.threshold }}℃</span>
                  </div>

                  <div class="alert-record-row">
                    <span>时间：</span>
                    <b>{{ record.time }}</b>
                  </div>

                  <div class="alert-record-actions">
                    <span class="status-pill" :class="getStatusClass(record.status)">
                      {{ record.status }}
                    </span>
                    <button
                      class="mini-btn"
                      :disabled="isRecordActionDisabled(record)"
                      @click="advanceAlarmStatus(record)"
                    >
                      {{ getRecordActionLabel(record) }}
                    </button>
                    <button class="mini-btn secondary" @click="openDetail(record)">
                      详情
                    </button>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 右侧：数据统计+搜索区域 -->
        <div class="layout-item data-search-section">
          <!-- 核心数据统计 -->
          <div class="panel-card stats-panel">
            <div class="panel-header">核心数据统计</div>
            <div class="panel-body">
              <div class="stats-grid">
                <div class="stat-item">
                  <div class="stat-value">{{ summaryStats.totalCases }}</div>
                  <div class="stat-label">案件总量</div>
                  <div class="stats-trend up">今日新增 {{ summaryStats.todayViolations }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value">{{ summaryStats.processedCases }}</div>
                  <div class="stat-label">案件处理量</div>
                  <div class="stats-trend up">处理率 {{ summaryStats.processingRate }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value">{{ summaryStats.complaintCases }}</div>
                  <div class="stat-label">案件投诉量</div>
                  <div class="stats-trend down">通知 {{ notificationStats.total }}</div>
                </div>
              </div>

              <div class="stats-compare">
                <div class="compare-item">
                  <span>处置完成率</span>
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: handleRate + '%' }"></div>
                  </div>
                  <span>{{ handleRate }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 设备状态 -->
          <div class="panel-card camera-status-panel">
            <div class="panel-header">
              <span>设备状态</span>
              <span class="status-summary">在线：{{ onlineCameraCount }}/{{ cameraStatusList.length }}</span>
            </div>
            <div class="panel-body">
              <ul class="camera-status-list">
                <li
                  v-for="camera in cameraStatusList"
                  :key="camera.id"
                  class="camera-status-line"
                >
                  <span>{{ camera.code }}</span>
                  <span class="camera-status" :class="camera.statusClass">
                    {{ camera.statusText }}
                  </span>
                </li>
                <li v-if="cameraStatusList.length === 0" class="camera-status-line empty-state">
                  暂无摄像头数据
                </li>
              </ul>
            </div>
          </div>

          <!-- 搜索面板 -->
          <div class="panel-card search-panel">
            <div class="panel-header">数据查询</div>
            <div class="panel-body">
              <div class="search-tabs">
                <div
                  class="tab-item"
                  :class="{ active: activeSearchTab === 'vehicle' }"
                  @click="setActiveSearchTab('vehicle')"
                >
                  车辆信息
                </div>
                <div
                  class="tab-item"
                  :class="{ active: activeSearchTab === 'analysis' }"
                  @click="setActiveSearchTab('analysis')"
                >
                  违规分析
                </div>
                <div
                  class="tab-item"
                  :class="{ active: activeSearchTab === 'realtime' }"
                  @click="setActiveSearchTab('realtime')"
                >
                  实时数据
                </div>
              </div>

              <div class="search-content">
                <!-- 车辆信息搜索 -->
                <div v-if="activeSearchTab === 'vehicle'">
                  <div class="search-form">
                    <div class="form-row">
                      <label class="form-label">车辆颜色：</label>
                      <select class="form-control" v-model="vehicleSearch.color">
                        <option value="">全部</option>
                        <option value="白色">白色</option>
                        <option value="黑色">黑色</option>
                        <option value="红色">红色</option>
                        <option value="蓝色">蓝色</option>
                      </select>

                      <label class="form-label">车辆品牌：</label>
                      <input
                        type="text"
                        class="form-control"
                        v-model="vehicleSearch.brand"
                        placeholder="请输入品牌"
                      />
                    </div>

                    <div class="form-row">
                      <label class="form-label">车牌号码：</label>
                      <input
                        type="text"
                        class="form-control"
                        v-model="vehicleSearch.plate"
                        placeholder="请输入车牌号"
                      />

                      <label class="form-label">违规类型：</label>
                      <select class="form-control" v-model="vehicleSearch.violationType">
                        <option value="">全部</option>
                        <option value="1">违规停放</option>
                        <option value="2">超速行驶</option>
                        <option value="3">违规充电</option>
                        <option value="4">未登记车辆</option>
                        <option value="5">电池异常高温</option>
                      </select>
                    </div>

                    <div class="form-actions">
                      <button class="btn primary" @click="searchVehicles">开始搜索</button>
                      <button class="btn secondary" @click="resetVehicleSearch">重置</button>
                    </div>
                  </div>

                  <div class="search-result">
                    <div v-if="selectedVehicle" class="search-result-tip">
                      当前上报绑定：{{ selectedVehicleDisplay }}
                    </div>
                    <div v-if="vehicleResults.length === 0" class="empty-state">
                      暂无车辆数据
                    </div>
                    <div v-else class="simple-result-list">
                      <div
                        v-for="vehicle in vehicleResults"
                        :key="vehicle.vehicleId"
                        class="simple-result-item"
                        :class="{ active: selectedVehicle && selectedVehicle.vehicleId === vehicle.vehicleId }"
                        @click="selectVehicle(vehicle)"
                      >
                        <div class="simple-result-title">
                          <span>{{ vehicle.licensePlate || '未挂牌' }}</span>
                          <span class="simple-result-tag">{{ vehicle.ownerName || '未实名' }}</span>
                        </div>
                        <div class="simple-result-meta">
                          {{ vehicle.brand || '未知品牌' }} · {{ vehicle.color || '未知颜色' }} · {{ vehicle.statusLabel }}
                        </div>
                        <div class="simple-result-meta">
                          {{ vehicle.userId ? `用户ID ${vehicle.userId}` : '未绑定用户' }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 违规分析 -->
                <div v-if="activeSearchTab === 'analysis'">
                  <div class="analysis-summary">
                    <div class="analysis-card">
                      <span>违规充电/高温类</span>
                      <b>{{ thermalViolationCount }}</b>
                    </div>
                    <div class="analysis-card">
                      <span>待整改</span>
                      <b>{{ violationStats.pendingRectify || pendingCount }}</b>
                    </div>
                    <div class="analysis-card">
                      <span>通知总量</span>
                      <b>{{ notificationStats.total }}</b>
                    </div>
                  </div>

                  <div class="analysis-note">
                    当前页面已接入后端的违规统计、通知统计、最近 7 天趋势、车辆检索与整改流程。
                    最后同步：{{ lastSyncedAt || currentTime || '--' }}
                  </div>
                </div>

                <!-- 实时数据 -->
                <div v-if="activeSearchTab === 'realtime'">
                  <div class="realtime-data">
                    <div class="realtime-item">
                      <div class="realtime-label">在线摄像头</div>
                      <div class="realtime-value">{{ realtimeData.realtimeOnlineCameras || onlineCameraCount }}</div>
                    </div>
                    <div class="realtime-item">
                      <div class="realtime-label">实时违规数</div>
                      <div class="realtime-value">{{ realtimeData.realtimeViolations || todayAlarmCount }}</div>
                    </div>
                    <div class="realtime-item">
                      <div class="realtime-label">当前最高温</div>
                      <div class="realtime-value">
                        {{ formatTemperature(thermalState.maxTemperature) }}
                      </div>
                    </div>
                    <div class="realtime-item">
                      <div class="realtime-label">今日通行</div>
                      <div class="realtime-value">{{ realtimePassFlow }}</div>
                    </div>
                    <div class="data-refresh">
                      <span>最后同步：{{ lastSyncedAt || '--:--:--' }}</span>
                      <button class="btn small" @click="fetchRealtimeData">刷新</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 详情弹窗 -->
    <el-dialog
      :title="detailDialog.title"
      v-model="detailDialog.visible"
      width="560px"
      :close-on-click-modal="false"
    >
      <div class="dialog-content" v-if="detailDialog.record">
        <p><b>告警编号：</b>{{ detailDialog.record.id }}</p>
        <p><b>告警类型：</b>{{ detailDialog.record.type }}</p>
        <p><b>车辆车牌：</b>{{ detailDialog.record.licensePlate || '未关联' }}</p>
        <p><b>车主姓名：</b>{{ detailDialog.record.ownerName || '未实名' }}</p>
        <p><b>巡检设备：</b>{{ detailDialog.record.deviceId }}</p>
        <p><b>巡检区域：</b>{{ detailDialog.record.location }}</p>
        <p><b>最高温度：</b>{{ formatTemperature(detailDialog.record.maxTemperature) }}</p>
        <p><b>告警阈值：</b>{{ detailDialog.record.threshold }}℃</p>
        <p><b>告警时间：</b>{{ detailDialog.record.time }}</p>
        <p><b>处置状态：</b>{{ detailDialog.record.status }}</p>
        <p><b>数据来源：</b>{{ detailDialog.record.sourceText || '红外设备/系统上报' }}</p>
        <p><b>说明：</b>{{ detailDialog.record.description || '当前页面已接入违规、通知、整改等后端流程。' }}</p>
      </div>

      <div class="dialog-content" v-else>
        暂无数据
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialog.visible = false">关闭</el-button>
          <el-button type="primary" @click="handleConfirm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 加载遮罩 -->
    <div class="loading-mask" v-if="isLoading">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p>数据加载中...</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { ElDialog, ElButton, ElMessage } from 'element-plus'
import axios from 'axios'
import { getApiBaseUrl } from '@/utils/apiBase'
import { getUserInfo } from '@/utils/auth'

const API_BASE_URL = getApiBaseUrl()
const THERMAL_VIOLATION_TYPE = 3
const THERMAL_THRESHOLD = 60
const infraredVideoSrc = '/video/infrared-monitor.mp4'
const DEFAULT_COORDINATE = {
  lng: 112.682763,
  lat: 26.886096
}
const DEFAULT_VIOLATION_LABELS = {
  1: '违规停放',
  2: '超速行驶',
  3: '违规充电',
  4: '未登记车辆',
  5: '进入禁行区',
  6: '违规载人',
  7: '遮挡车牌',
  8: '其他违规'
}

const readTokenFromStorage = () => {
  const directToken = localStorage.getItem('token')
    || sessionStorage.getItem('token')
    || localStorage.getItem('ev_token')
    || sessionStorage.getItem('ev_token')
  if (directToken) return directToken

  const vuexRaw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex')
  if (!vuexRaw) return ''

  try {
    const parsed = JSON.parse(vuexRaw)
    return parsed?.user?.token || ''
  } catch {
    return ''
  }
}

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 8000
})

apiClient.interceptors.request.use(config => {
  config.headers = config.headers || {}
  const token = readTokenFromStorage()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
    config.headers.token = token
  }
  return config
})

const getResponseMessage = (payload, fallback) => payload?.message || payload?.msg || fallback

const getErrorMessage = (error, fallback) => {
  if (error?.response?.data) {
    return getResponseMessage(error.response.data, fallback)
  }
  return error?.message || fallback
}

const toNumber = value => {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

const parsePercent = value => {
  const text = String(value ?? '').replace('%', '').trim()
  const num = Number(text)
  return Number.isFinite(num) ? Math.round(num) : null
}

const pad = value => String(value).padStart(2, '0')

const formatDateKey = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`

const formatTime = value => {
  const date = value instanceof Date
    ? value
    : value
      ? new Date(String(value).replace(' ', 'T'))
      : new Date()

  if (Number.isNaN(date.getTime())) {
    return value ? String(value) : '--'
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const formatTemperature = value => {
  const num = Number(value)
  return Number.isFinite(num) ? `${num.toFixed(1)}℃` : '--'
}

const buildTrendSkeleton = () => {
  const labels = []
  for (let index = 6; index >= 0; index -= 1) {
    const date = new Date()
    date.setDate(date.getDate() - index)
    labels.push({
      key: formatDateKey(date),
      day: index === 0 ? '今日' : `周${'日一二三四五六'[date.getDay()]}`,
      count: 0
    })
  }
  return labels
}

const currentTime = ref('')
const lastSyncedAt = ref('')
const activeSearchTab = ref('vehicle')
const isLoading = ref(false)
const hasLoadedOverview = ref(false)
const hasLoadedRealtime = ref(false)

const currentDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

const videoRef = ref(null)
const miniVideoRef = ref(null)
const cameraReady = ref(false)
const cameraError = ref('')
const isPlaying = ref(false)
const isMuted = ref(true)

let mediaStream = null
let timer = null
let resizeHandler = null
let tempTimer = null
let dataTimer = null

const thermalState = ref({
  deviceId: 'CAM-IR-001',
  mode: '红外热源检测',
  location: '宿舍区停车棚A区',
  minTemperature: 27.4,
  maxTemperature: 36.8,
  threshold: THERMAL_THRESHOLD,
  alarm: false,
  hotspot: {
    x: 42,
    y: 36,
    width: 16,
    height: 12
  }
})

const alarmRecords = ref([])
const trendData = ref(buildTrendSkeleton())
const vehicleResults = ref([])
const selectedVehicle = ref(null)
const backendCameras = ref([])
const dashboardStats = ref({
  totalCases: 0,
  processedCases: 0,
  complaintCases: 0,
  todayViolations: 0,
  processingRate: '0%',
  complaintRate: '0%'
})
const violationStats = ref({
  total: 0,
  pendingRectify: 0,
  typeStats: [],
  trendStats: [],
  rectifyRate: '0%'
})
const notificationStats = ref({
  total: 0,
  sent: 0,
  draft: 0,
  deliveryRate: '0%'
})
const realtimeData = ref({
  realtimeViolations: 0,
  realtimeProcessing: 0,
  realtimeOnlineCameras: 0,
  realtimeVehicles: 0,
  todayPassIn: 0,
  todayPassOut: 0
})

const violationTypeLabels = ref({ ...DEFAULT_VIOLATION_LABELS })

const vehicleSearch = ref({
  color: '',
  brand: '',
  plate: '',
  violationType: ''
})

const detailDialog = ref({
  visible: false,
  title: '告警详情',
  record: null
})

const getViolationTypeName = type => violationTypeLabels.value[toNumber(type)] || DEFAULT_VIOLATION_LABELS[toNumber(type)] || '其他违规'

const mapViolationStateToStatus = state => {
  const map = {
    0: '待处理',
    1: '处理中',
    2: '已处置',
    3: '超时未处理',
    4: '申诉中'
  }
  return map[toNumber(state)] || '待处理'
}

const mapVehicleStatus = vehicle => {
  if (vehicle?.isBlacklist) return '黑名单'
  if (vehicle?.isFrozen) return '已冻结'

  const statusText = String(vehicle?.status || '').toLowerCase()
  if (statusText === 'approved') return '已备案'
  if (statusText === 'pending') return '待审核'
  if (statusText === 'rejected') return '已驳回'
  if (statusText) return vehicle.status
  return '状态未知'
}

const normalizeCameraStatusClass = status => {
  const value = String(status || '').toLowerCase()
  if (value.includes('online')) return 'online'
  if (value.includes('warning') || value.includes('error')) return 'warning'
  if (value.includes('offline')) return 'offline'
  return 'warning'
}

const formatCameraStatusText = status => {
  const value = String(status || '').toLowerCase()
  if (value.includes('online')) return '在线'
  if (value.includes('warning') || value.includes('error')) return '异常'
  if (value.includes('offline')) return '离线'
  return status || '未知'
}

const formatLocation = record => {
  const lng = Number(record?.illegalLng)
  const lat = Number(record?.illegalLat)
  if (Number.isFinite(lng) && Number.isFinite(lat) && (lng !== 0 || lat !== 0)) {
    return `${lng.toFixed(5)}, ${lat.toFixed(5)}`
  }
  return thermalState.value.location
}

const inferMaxTemperature = record => {
  const raw = Number(record?.maxTemperature ?? record?.temperature)
  if (Number.isFinite(raw)) return raw

  const seed = Math.abs(toNumber(record?.illegalId || record?.vehicleId || 1)) % 7
  const base = toNumber(record?.illegalType) === THERMAL_VIOLATION_TYPE ? 61.2 : 43.5
  return Number((base + seed * 0.9).toFixed(1))
}

const resolveRecordLevel = record => {
  const illegalType = toNumber(record?.illegalType)
  const illegalState = toNumber(record?.illegalState)

  if (illegalType === THERMAL_VIOLATION_TYPE || illegalState === 3) {
    return { label: '高', className: 'high' }
  }
  if (illegalType === 1 || illegalType === 4 || illegalState === 1) {
    return { label: '中', className: 'medium' }
  }
  return { label: '低', className: 'low' }
}

const mapSourceText = source => {
  const map = {
    1: '设备自动上报',
    2: '管理端上报'
  }
  return map[toNumber(source)] || '系统同步'
}

const normalizeViolationRecord = record => {
  const level = resolveRecordLevel(record)
  return {
    id: record.illegalId || `VIOLATION-${Date.now()}`,
    illegalId: record.illegalId,
    vehicleId: record.vehicleId,
    userId: record.userId,
    backendType: toNumber(record.illegalType),
    type: record.illegalTypeName || getViolationTypeName(record.illegalType),
    deviceId: toNumber(record.illegalSource) === 1 ? 'MQTT-IOT' : thermalState.value.deviceId,
    location: formatLocation(record),
    maxTemperature: inferMaxTemperature(record),
    threshold: thermalState.value.threshold,
    time: formatTime(record.illegalTime || record.createTime),
    status: mapViolationStateToStatus(record.illegalState),
    level: level.label,
    levelClass: level.className,
    source: record.illegalSource,
    sourceText: mapSourceText(record.illegalSource),
    licensePlate: record.licensePlate || '',
    ownerName: record.ownerName || '',
    description: record.licensePlate
      ? `系统已同步车辆 ${record.licensePlate} 的违规记录，可继续通知并发起整改。`
      : '系统已同步该违规记录，可继续通知并发起整改。',
    originalData: record
  }
}

const normalizeVehicleResult = record => ({
  ...record,
  statusLabel: mapVehicleStatus(record)
})

const selectedVehicleDisplay = computed(() => {
  if (!selectedVehicle.value) return '未绑定车辆'
  const plate = selectedVehicle.value.licensePlate || '未挂牌'
  const ownerName = selectedVehicle.value.ownerName || '未实名'
  return `${plate} / ${ownerName}`
})

const cameraStatusList = computed(() => {
  const localCamera = {
    id: 'local-ir-camera',
    code: thermalState.value.deviceId,
    statusClass: cameraReady.value ? 'online' : (cameraError.value ? 'warning' : 'offline'),
    statusText: cameraReady.value ? '在线' : (cameraError.value || '离线')
  }

  const remoteCameras = backendCameras.value.map(camera => ({
    id: camera.id || camera.code || camera.name,
    code: camera.name || camera.code || `CAM-${camera.id}`,
    statusClass: normalizeCameraStatusClass(camera.status),
    statusText: formatCameraStatusText(camera.status)
  }))

  return [localCamera, ...remoteCameras].slice(0, 6)
})

const onlineCameraCount = computed(() => {
  return cameraStatusList.value.filter(camera => camera.statusClass === 'online').length
})

const summaryStats = computed(() => {
  return {
    totalCases: toNumber(dashboardStats.value.totalCases),
    processedCases: toNumber(dashboardStats.value.processedCases),
    complaintCases: toNumber(dashboardStats.value.complaintCases),
    todayViolations: toNumber(dashboardStats.value.todayViolations),
    processingRate: dashboardStats.value.processingRate || '0%'
  }
})

const thermalViolationCount = computed(() => {
  const typeStats = Array.isArray(violationStats.value.typeStats) ? violationStats.value.typeStats : []
  const matched = typeStats.find(item => {
    const typeValue = toNumber(item?.type)
    return typeValue === THERMAL_VIOLATION_TYPE || String(item?.type) === getViolationTypeName(THERMAL_VIOLATION_TYPE)
  })
  if (matched) return toNumber(matched.count)
  return alarmRecords.value.filter(record => record.backendType === THERMAL_VIOLATION_TYPE).length
})

const todayAlarmCount = computed(() => {
  return hasLoadedOverview.value ? summaryStats.value.todayViolations : alarmRecords.value.length
})

const pendingCount = computed(() => {
  if (hasLoadedOverview.value) return toNumber(violationStats.value.pendingRectify)
  return alarmRecords.value.filter(item => ['待处理', '超时未处理'].includes(item.status)).length
})

const processingCount = computed(() => {
  if (hasLoadedRealtime.value) return toNumber(realtimeData.value.realtimeProcessing)
  return alarmRecords.value.filter(item => ['处理中', '已通知'].includes(item.status)).length
})

const handledCount = computed(() => {
  if (hasLoadedOverview.value) return summaryStats.value.processedCases
  return alarmRecords.value.filter(item => item.status === '已处置' || item.status === '已复查').length
})

const complaintCount = computed(() => {
  if (hasLoadedOverview.value) return summaryStats.value.complaintCases
  return alarmRecords.value.filter(item => item.status === '申诉中').length
})

const handleRate = computed(() => {
  const backendRate = parsePercent(summaryStats.value.processingRate)
  if (backendRate != null) return backendRate

  if (todayAlarmCount.value === 0) return 0
  return Math.round((handledCount.value / todayAlarmCount.value) * 100)
})

const realtimePassFlow = computed(() => {
  return `${toNumber(realtimeData.value.todayPassIn)}/${toNumber(realtimeData.value.todayPassOut)}`
})

const applyTrendStats = trendStats => {
  const trendMap = new Map()
  if (Array.isArray(trendStats)) {
    trendStats.forEach(item => {
      const raw = item?.date
      if (!raw) return
      const date = new Date(String(raw).replace(' ', 'T'))
      const key = Number.isNaN(date.getTime()) ? String(raw).slice(0, 10) : formatDateKey(date)
      trendMap.set(key, toNumber(item?.count))
    })
  }

  trendData.value = buildTrendSkeleton().map(item => ({
    day: item.day,
    count: trendMap.get(item.key) || 0
  }))
}

const updateTime = () => {
  currentTime.value = formatTime()
}

const fetchViolationTypes = async () => {
  try {
    const response = await apiClient.get('/violation/types')
    if (response.data?.code === 200 && Array.isArray(response.data.data)) {
      const nextLabels = { ...DEFAULT_VIOLATION_LABELS }
      response.data.data.forEach(item => {
        nextLabels[toNumber(item.value)] = item.label || nextLabels[toNumber(item.value)]
      })
      violationTypeLabels.value = nextLabels
    }
  } catch (error) {
    console.error('获取违规类型失败：', error)
  }
}

const fetchOverviewData = async ({ silent = false } = {}) => {
  const [statisticsResult, violationStatsResult, notificationStatsResult, cameraResult] = await Promise.allSettled([
    apiClient.get('/monitor/statistics'),
    apiClient.get('/violation/stats'),
    apiClient.get('/notification/stats'),
    apiClient.get('/monitor/cameras')
  ])

  if (statisticsResult.status === 'fulfilled' && statisticsResult.value.data?.code === 200) {
    dashboardStats.value = {
      ...dashboardStats.value,
      ...statisticsResult.value.data.data
    }
    hasLoadedOverview.value = true
  }

  if (violationStatsResult.status === 'fulfilled' && violationStatsResult.value.data?.code === 200) {
    violationStats.value = {
      ...violationStats.value,
      ...violationStatsResult.value.data.data
    }
    applyTrendStats(violationStats.value.trendStats)
    hasLoadedOverview.value = true
  }

  if (notificationStatsResult.status === 'fulfilled' && notificationStatsResult.value.data?.code === 200) {
    notificationStats.value = {
      ...notificationStats.value,
      ...notificationStatsResult.value.data.data
    }
  }

  if (cameraResult.status === 'fulfilled' && cameraResult.value.data?.code === 200) {
    backendCameras.value = Array.isArray(cameraResult.value.data.data) ? cameraResult.value.data.data : []
  }

  if (!silent) {
    ElMessage.success('统计和设备状态已同步')
  }
}

const fetchViolationAlerts = async ({ silent = false } = {}) => {
  try {
    const response = await apiClient.get('/violation/list', {
      params: {
        pageNum: 1,
        pageSize: 10
      }
    })

    if (response.data?.code === 200) {
      const list = Array.isArray(response.data.data) ? response.data.data : []
      alarmRecords.value = list.map(normalizeViolationRecord)
      if (!silent) {
        ElMessage.success(`已刷新，共 ${alarmRecords.value.length} 条违规记录`)
      }
      return
    }

    throw new Error(getResponseMessage(response.data, '刷新违规记录失败'))
  } catch (error) {
    console.error('获取违规记录失败：', error)
    if (!silent) {
      ElMessage.error(getErrorMessage(error, '刷新违规记录失败'))
    }
  }
}

const filterVehiclesByViolationType = async vehicles => {
  const illegalType = toNumber(vehicleSearch.value.violationType)
  if (!illegalType) return vehicles

  const response = await apiClient.get('/violation/list', {
    params: {
      illegalType,
      pageNum: 1,
      pageSize: 200
    }
  })

  if (response.data?.code !== 200) {
    throw new Error(getResponseMessage(response.data, '筛选关联违规车辆失败'))
  }

  const matchedVehicles = new Set()
  const violationList = Array.isArray(response.data.data) ? response.data.data : []
  violationList.forEach(item => {
    if (item?.vehicleId != null) {
      matchedVehicles.add(item.vehicleId)
    }
  })

  return vehicles.filter(item => matchedVehicles.has(item.vehicleId))
}

const selectVehicle = (vehicle, showMessage = true) => {
  selectedVehicle.value = vehicle
  if (showMessage) {
    ElMessage.success(`已绑定车辆：${vehicle.licensePlate || vehicle.vehicleId}`)
  }
}

const searchVehicles = async ({ silent = false } = {}) => {
  const plate = vehicleSearch.value.plate.trim()
  const brand = vehicleSearch.value.brand.trim()
  const color = vehicleSearch.value.color.trim()
  const useSearchEndpoint = Boolean(plate || brand || color || vehicleSearch.value.violationType)

  try {
    const response = useSearchEndpoint
      ? await apiClient.get('/vehicle/search', {
        params: {
          plate: plate || undefined,
          brand: brand || undefined,
          color: color || undefined
        }
      })
      : await apiClient.get('/vehicle/list', {
        params: {
          pageNum: 1,
          pageSize: 6
        }
      })

    if (response.data?.code !== 200) {
      throw new Error(getResponseMessage(response.data, '搜索车辆失败'))
    }

    let vehicles = Array.isArray(response.data.data) ? response.data.data : []
    vehicles = await filterVehiclesByViolationType(vehicles)

    vehicleResults.value = vehicles.slice(0, 6).map(normalizeVehicleResult)

    if (selectedVehicle.value) {
      const currentVehicle = vehicleResults.value.find(item => item.vehicleId === selectedVehicle.value.vehicleId)
      selectedVehicle.value = currentVehicle || vehicleResults.value[0] || null
    } else if (vehicleResults.value.length > 0) {
      selectVehicle(vehicleResults.value[0], false)
    } else {
      selectedVehicle.value = null
    }

    if (!silent) {
      if (vehicleResults.value.length === 0) {
        ElMessage.info('未搜索到符合条件的车辆')
      } else {
        ElMessage.success(`已获取 ${vehicleResults.value.length} 条车辆数据`)
      }
    }
  } catch (error) {
    console.error('搜索车辆失败：', error)
    if (!silent) {
      ElMessage.error(getErrorMessage(error, '搜索车辆失败'))
    }
  }
}

const resetVehicleSearch = async () => {
  vehicleSearch.value = {
    color: '',
    brand: '',
    plate: '',
    violationType: ''
  }
  await searchVehicles({ silent: true })
  ElMessage.info('搜索条件已重置')
}

const fetchRealtimeData = async ({ silent = false } = {}) => {
  updateThermalTemperature()

  try {
    const response = await apiClient.get('/monitor/realtime')
    if (response.data?.code !== 200) {
      throw new Error(getResponseMessage(response.data, '获取实时数据失败'))
    }

    realtimeData.value = {
      ...realtimeData.value,
      ...response.data.data
    }
    hasLoadedRealtime.value = true
    lastSyncedAt.value = formatTime()

    if (!silent) {
      ElMessage.success('实时数据已刷新')
    }
  } catch (error) {
    console.error('获取实时数据失败：', error)
    if (!silent) {
      ElMessage.error(getErrorMessage(error, '获取实时数据失败'))
    }
  }
}

const ensureSelectedVehicle = async () => {
  if (selectedVehicle.value?.vehicleId && selectedVehicle.value?.userId) {
    return selectedVehicle.value
  }

  if (vehicleResults.value.length > 0) {
    const availableVehicle = vehicleResults.value.find(item => item.vehicleId && item.userId)
    if (availableVehicle) {
      selectedVehicle.value = availableVehicle
      return availableVehicle
    }
  }

  await searchVehicles({ silent: true })
  return selectedVehicle.value
}

const sendHighTempNotification = async record => {
  if (!record?.userId) return false

  const title = '红外高温异常告警通知'
  const content = `车辆 ${record.licensePlate || '未挂牌车辆'} 在 ${thermalState.value.location} 检测到高温异常，请尽快处理。`
  const response = await apiClient.post('/notification/send-personal', null, {
    params: {
      title,
      content,
      receiverId: record.userId
    }
  })

  return response.data?.code === 200 && response.data?.data !== false
}

const startCamera = async () => {
  cameraError.value = ''

  try {
    await nextTick()

    if (videoRef.value) {
      videoRef.value.muted = true
      videoRef.value.loop = true
      await videoRef.value.play()
    }

    if (miniVideoRef.value) {
      miniVideoRef.value.muted = true
      miniVideoRef.value.loop = true
      await miniVideoRef.value.play()
    }

    cameraReady.value = true
    isPlaying.value = true
    ElMessage.success('红外巡检画面已启动')
  } catch (error) {
    console.error('红外画面启动失败：', error)
    cameraReady.value = false
    isPlaying.value = false
    cameraError.value = '画面启动失败'
    ElMessage.error('红外画面启动失败')
  }
}

const stopCamera = (showMessage = true) => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }

  if (videoRef.value) {
    videoRef.value.pause()
  }

  if (miniVideoRef.value) {
    miniVideoRef.value.pause()
  }

  cameraReady.value = false
  isPlaying.value = false

  if (showMessage) {
    ElMessage.info('红外画面已停止')
  }
}

const togglePlay = async () => {
  if (!cameraReady.value) {
    await startCamera()
    return
  }

  if (!videoRef.value) return

  if (isPlaying.value) {
    videoRef.value.pause()
    if (miniVideoRef.value) miniVideoRef.value.pause()
    isPlaying.value = false
    ElMessage.info('红外画面已暂停')
  } else {
    await videoRef.value.play()
    if (miniVideoRef.value) await miniVideoRef.value.play()
    isPlaying.value = true
    ElMessage.success('红外画面已播放')
  }
}

const toggleVolume = () => {
  isMuted.value = !isMuted.value
  ElMessage.info(isMuted.value ? '当前无音频输入，已静音' : '当前为无音频巡检画面')
}

const updateThermalTemperature = () => {
  if (thermalState.value.alarm) {
    const max = 62 + Math.random() * 6
    thermalState.value.maxTemperature = Number(max.toFixed(1))
    thermalState.value.minTemperature = Number((29 + Math.random() * 3).toFixed(1))
    return
  }

  const normalMax = 34 + Math.random() * 6
  const normalMin = 25 + Math.random() * 4
  thermalState.value.maxTemperature = Number(normalMax.toFixed(1))
  thermalState.value.minTemperature = Number(normalMin.toFixed(1))
}

const triggerHighTempAlarm = async () => {
  thermalState.value.alarm = true
  thermalState.value.maxTemperature = Number((62 + Math.random() * 5).toFixed(1))
  thermalState.value.minTemperature = Number((28 + Math.random() * 3).toFixed(1))
  thermalState.value.hotspot = {
    x: Math.floor(35 + Math.random() * 20),
    y: Math.floor(30 + Math.random() * 18),
    width: 16,
    height: 12
  }

  const vehicle = await ensureSelectedVehicle()
  if (!vehicle?.vehicleId || !vehicle?.userId) {
    ElMessage.warning('请先在右侧搜索并选择一辆已登记车辆，再执行真实告警上报')
    return
  }

  isLoading.value = true
  try {
    const response = await apiClient.post('/violation/report', {
      vehicleId: vehicle.vehicleId,
      userId: vehicle.userId,
      illegalType: THERMAL_VIOLATION_TYPE,
      illegalLng: DEFAULT_COORDINATE.lng,
      illegalLat: DEFAULT_COORDINATE.lat,
      deductionPoints: 2,
      illegalSource: 2
    })

    if (response.data?.code !== 200 || response.data?.data === false) {
      throw new Error(getResponseMessage(response.data, '高温告警上报失败'))
    }

    try {
      await sendHighTempNotification(vehicle)
    } catch (notificationError) {
      console.warn('发送高温告警通知失败：', notificationError)
    }

    await Promise.all([
      fetchViolationAlerts({ silent: true }),
      fetchOverviewData({ silent: true }),
      fetchRealtimeData({ silent: true })
    ])

    ElMessage.error(`已将 ${vehicle.licensePlate || '所选车辆'} 的高温异常同步到系统`)
  } catch (error) {
    console.error('高温告警上报失败：', error)
    ElMessage.error(getErrorMessage(error, '高温告警上报失败'))
  } finally {
    isLoading.value = false
  }
}

const recoverTemperature = () => {
  thermalState.value.alarm = false
  thermalState.value.maxTemperature = 36.8
  thermalState.value.minTemperature = 27.4
  ElMessage.success('温度已恢复正常，当前无异常')
}

const getRecordActionLabel = record => {
  if (['待处理', '超时未处理'].includes(record.status)) return '通知整改'
  if (['处理中', '已通知'].includes(record.status)) return '查看进度'
  if (record.status === '申诉中') return '申诉处理中'
  return '已完成'
}

const isRecordActionDisabled = record => ['已处置', '已复查', '申诉中'].includes(record.status)

const advanceAlarmStatus = async record => {
  if (!record?.illegalId || !record?.userId) {
    ElMessage.info('该记录暂无可推进的后端流程')
    return
  }

  if (['处理中', '已通知'].includes(record.status)) {
    ElMessage.info('该记录已进入整改流程，请前往整改管理页继续提交和核实整改材料')
    return
  }

  if (isRecordActionDisabled(record)) {
    ElMessage.info('该告警已完成闭环处置')
    return
  }

  isLoading.value = true
  try {
    let notificationText = '已发送通知'
    try {
      const notified = await sendHighTempNotification(record)
      notificationText = notified ? '已发送通知' : '通知发送失败'
    } catch (notificationError) {
      notificationText = '通知发送失败'
      console.warn('发送整改通知失败：', notificationError)
    }

    const response = await apiClient.post(`/violation/${record.illegalId}/rectify`, null, {
      params: {
        userId: record.userId,
        reason: '红外高温巡检发现异常，已由管理端发起整改流程'
      }
    })

    if (response.data?.code !== 200 || response.data?.data === false) {
      throw new Error(getResponseMessage(response.data, '发起整改流程失败'))
    }

    await Promise.all([
      fetchViolationAlerts({ silent: true }),
      fetchOverviewData({ silent: true }),
      fetchRealtimeData({ silent: true })
    ])

    ElMessage.success(`${notificationText}，并已发起整改流程`)
  } catch (error) {
    console.error('推进告警流程失败：', error)
    ElMessage.error(getErrorMessage(error, '推进告警流程失败'))
  } finally {
    isLoading.value = false
  }
}

const getStatusClass = status => {
  const map = {
    待处理: 'status-pending',
    已通知: 'status-processing',
    处理中: 'status-processing',
    已处置: 'status-handled',
    已复查: 'status-finished',
    申诉中: 'status-appeal',
    超时未处理: 'status-pending'
  }
  return map[status] || 'status-pending'
}

const toggleFullscreen = target => {
  let element = null

  if (target === 'infrared') {
    element = document.getElementById('infrared_box')
  } else {
    element = document.getElementById('car_control')
  }

  if (!element) {
    ElMessage.warning('未找到可全屏的区域')
    return
  }

  if (!document.fullscreenElement) {
    element.requestFullscreen?.()
    ElMessage.success('已进入全屏模式')
  } else {
    document.exitFullscreen?.()
    ElMessage.info('已退出全屏模式')
  }
}

const setActiveSearchTab = tab => {
  activeSearchTab.value = tab
}

const openDetail = async record => {
  let currentRecord = record

  if (record?.illegalId) {
    try {
      const response = await apiClient.get(`/violation/${record.illegalId}`)
      if (response.data?.code === 200 && response.data?.data) {
        currentRecord = {
          ...normalizeViolationRecord(response.data.data),
          maxTemperature: record.maxTemperature,
          threshold: record.threshold
        }
      }
    } catch (error) {
      console.error('获取违规详情失败：', error)
    }
  }

  detailDialog.value = {
    visible: true,
    title: '红外高温告警详情',
    record: currentRecord
  }
}

const handleConfirm = () => {
  detailDialog.value.visible = false
}

const adjustLayout = () => {
  const headerHeight = document.querySelector('.header')?.offsetHeight || 44
  const viewportHeight = window.innerHeight - headerHeight - 20
  const infraredContainer = document.getElementById('car_control')
  const infraredBox = document.getElementById('infrared_box')

  if (infraredContainer) infraredContainer.style.height = `${viewportHeight}px`
  if (infraredBox) infraredBox.style.height = '100%'
}

const initializePageData = async () => {
  isLoading.value = true
  try {
    await Promise.all([
      fetchViolationTypes(),
      fetchOverviewData({ silent: true }),
      fetchViolationAlerts({ silent: true }),
      fetchRealtimeData({ silent: true }),
      searchVehicles({ silent: true })
    ])
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  tempTimer = setInterval(updateThermalTemperature, 1800)

  resizeHandler = adjustLayout
  adjustLayout()
  window.addEventListener('resize', resizeHandler)

  initializePageData()
  startCamera()

  dataTimer = setInterval(() => {
    fetchOverviewData({ silent: true })
    fetchViolationAlerts({ silent: true })
    fetchRealtimeData({ silent: true })
  }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (tempTimer) clearInterval(tempTimer)
  if (dataTimer) clearInterval(dataTimer)

  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }

  stopCamera(false)
})
</script>

<style scoped>
/* 基础样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.monitor-container {
  width: 100%;
  height: 100vh;
  background: #001428;
  color: #fff;
  overflow: hidden;
  font-size: 14px;
}

/* 顶部导航 */
.header {
  height: 44px;
  background: linear-gradient(to right, #001e42, #002855);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #007acc;
  box-shadow: 0 2px 10px rgba(0, 122, 204, 0.3);
  z-index: 100;
}

.head-logo {
  font-size: 18px;
  font-weight: bold;
  color: #00e5ff;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.5);
  letter-spacing: 2px;
}

.head-ul {
  display: flex;
  list-style: none;
  gap: 5px;
}

.head-ul li a {
  color: #85acd5;
  text-decoration: none;
  font-size: 16px;
  padding: 8px 15px;
  border-radius: 4px;
  transition: all 0.3s;
  display: block;
}

.head-ul li a.active-a {
  color: #00e5ff;
  background: rgba(0, 229, 255, 0.1);
  border-bottom: 2px solid #00e5ff;
}

.head-ul li a:hover {
  color: #00e5ff;
  background: rgba(0, 229, 255, 0.05);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  color: #85acd5;
  font-size: 13px;
}

/* 主内容区 */
.section {
  width: 100%;
  height: calc(100% - 44px);
  padding: 10px;
  overflow: hidden;
}

/* 核心布局：Grid 实现三列布局 */
.main-layout {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: 55% 22% 23%;
  gap: 10px;
}

.layout-item {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 通用面板样式 */
.panel-card {
  background: #001a36;
  border: 1px solid #007acc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  height: 40px;
  background: linear-gradient(to right, #001e42, #002855);
  border-radius: 7px 7px 0 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  color: #00e5ff;
  font-weight: bold;
  border-bottom: 1px solid #007acc;
}

.panel-body {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
}

/* 左侧红外监测区域 */
.infrared-section {
  height: 100%;
}

.infrared-container {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #007acc;
  background: #001a36;
}

#infrared_box {
  width: 100%;
  height: 100%;
  z-index: 1;
  position: relative;
  background: #000;
}

/* 设备待机状态 */
.device-standby {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(226, 232, 240, 0.88);
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(2, 6, 23, 0.98)),
    repeating-linear-gradient(0deg, rgba(148, 163, 184, 0.06) 0 1px, transparent 1px 14px);
}

.standby-title {
  font-size: 18px;
  font-weight: 700;
}

.standby-desc {
  font-size: 13px;
  color: rgba(203, 213, 225, 0.68);
}

/* 红外视频样式 */
.infrared-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: grayscale(100%) contrast(165%) brightness(85%) saturate(0%);
  transform: scaleX(-1);
}

.infrared-video.paused {
  filter: grayscale(100%) contrast(120%) brightness(55%) saturate(0%);
}

/* 红外图片样式 */
.infrared-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: grayscale(100%) contrast(150%) brightness(80%);
}

/* 高温区域框 */
.hotspot-box {
  position: absolute;
  z-index: 11;
  border: 2px solid #ff3b30;
  box-shadow: 0 0 18px rgba(255, 59, 48, 0.95), inset 0 0 18px rgba(255, 59, 48, 0.22);
  animation: hotspotPulse 1s infinite alternate;
}

.hotspot-box span {
  position: absolute;
  left: 0;
  top: -26px;
  min-width: 120px;
  padding: 3px 8px;
  background: rgba(255, 59, 48, 0.86);
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
}

@keyframes hotspotPulse {
  from {
    opacity: 0.65;
    transform: scale(1);
  }
  to {
    opacity: 1;
    transform: scale(1.03);
  }
}

/* 视频控制按钮 */
.video-controls {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 13;
  flex-wrap: wrap;
  justify-content: center;
}

.control-btn {
  background: rgba(0, 26, 54, 0.82);
  border: 1px solid #007acc;
  color: #00e5ff;
  border-radius: 4px;
  padding: 6px 14px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
  backdrop-filter: blur(5px);
}

.control-btn:hover {
  background: rgba(0, 122, 204, 0.85);
}

.danger-btn {
  color: #ffb3b3;
  border-color: #ff4d4f;
  background: rgba(255, 77, 79, 0.16);
}

.danger-btn:hover {
  background: rgba(255, 77, 79, 0.36);
}

.safe-btn {
  color: #b8ffca;
  border-color: #4cd964;
  background: rgba(76, 217, 100, 0.13);
}

.safe-btn:hover {
  background: rgba(76, 217, 100, 0.28);
}

/* 红外画面信息叠加 */
.infrared-info {
  position: absolute;
  top: 56px;
  left: 15px;
  background: rgba(0, 26, 54, 0.82);
  border: 1px solid #007acc;
  border-radius: 8px;
  padding: 10px 15px;
  z-index: 10;
  backdrop-filter: blur(5px);
}

.info-item {
  display: flex;
  margin: 5px 0;
  min-width: 250px;
}

.info-label {
  color: #85acd5;
  font-size: 12px;
  min-width: 80px;
}

.info-value {
  color: #00e5ff;
  font-size: 12px;
  font-weight: bold;
}

.info-value.normal {
  color: #4cd964;
}

.info-value.warning {
  color: #ff9800;
}

.info-value.error {
  color: #ff6b6b;
}

/* 实时状态悬浮窗 */
.realtime-status {
  position: absolute;
  top: 56px;
  right: 15px;
  background: rgba(0, 26, 54, 0.82);
  border: 1px solid #007acc;
  border-radius: 8px;
  padding: 10px 15px;
  z-index: 10;
  backdrop-filter: blur(5px);
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 5px 0;
  min-width: 120px;
}

.status-label {
  color: #85acd5;
  font-size: 12px;
}

.status-value {
  color: #00e5ff;
  font-size: 16px;
  font-weight: bold;
}

/* 趋势图容器 */
.trend-chart-wrapper {
  position: absolute;
  bottom: 20px;
  right: 20px;
  width: 300px;
  height: 180px;
  background: rgba(0, 26, 54, 0.82);
  border: 1px solid #007acc;
  border-radius: 8px;
  padding: 10px;
  z-index: 10;
  backdrop-filter: blur(5px);
}

.chart-title {
  color: #00e5ff;
  font-size: 12px;
  margin-bottom: 5px;
  text-align: center;
}

.trend-chart {
  width: 100%;
  height: calc(100% - 22px);
  display: flex;
  align-items: end;
  justify-content: space-around;
  padding-top: 10px;
}

.trend-bar-item {
  width: 28px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.trend-bar-track {
  flex: 1;
  width: 12px;
  display: flex;
  align-items: end;
  background: rgba(0, 122, 204, 0.18);
  border-radius: 999px;
  overflow: hidden;
}

.trend-bar-fill {
  width: 100%;
  background: linear-gradient(to top, #00e5ff, #007acc);
  border-radius: 999px;
  transition: height 0.3s;
}

.trend-bar-fill.danger {
  background: linear-gradient(to top, #ff4d4f, #ff9800);
}

.trend-day {
  margin-top: 6px;
  color: #85acd5;
  font-size: 11px;
}

/* 中间视频+报警区域 */
.video-alert-section {
  gap: 10px;
}

.video-panel {
  flex: 2;
}

.alert-panel {
  flex: 1;
}

/* 视频监控样式 */
.camera-item {
  margin-bottom: 10px;
}

.camera-item:last-child {
  margin-bottom: 0;
}

.camera-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #85acd5;
  margin-bottom: 5px;
  font-size: 12px;
}

.camera-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.camera-status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
}

.camera-status.online {
  background: rgba(76, 217, 100, 0.2);
  color: #4cd964;
  border: 1px solid #4cd964;
}

.camera-status.warning {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
  border: 1px solid #ff9800;
}

.camera-video {
  height: 180px;
  border-radius: 4px;
  overflow: hidden;
  background: #000;
}

.camera-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.camera-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(226, 232, 240, 0.78);
  font-size: 14px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.98), rgba(2, 6, 23, 0.98)),
    repeating-linear-gradient(0deg, rgba(148, 163, 184, 0.06) 0 1px, transparent 1px 12px);
}

.camera-video-live {
  filter: grayscale(100%) contrast(140%) brightness(85%);
  transform: scaleX(-1);
}

/* 报警信息样式 */
.alert-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 5px;
}

.refresh-btn {
  background: rgba(0, 122, 204, 0.2);
  border: 1px solid #007acc;
  color: #00e5ff;
  border-radius: 4px;
  padding: 2px 8px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: rgba(0, 122, 204, 0.4);
}

.alert-list {
  list-style: none;
  height: calc(100% - 30px);
  overflow-y: auto;
}

.alert-record {
  padding: 10px;
  border: 1px solid rgba(0, 122, 204, 0.48);
  background: rgba(0, 26, 54, 0.58);
  border-radius: 8px;
  margin-bottom: 8px;
}

.alert-record-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.alert-type {
  color: #ffb3b3;
  font-weight: bold;
  font-size: 12px;
}

.alert-level {
  font-size: 11px;
  border-radius: 999px;
  padding: 2px 8px;
}

.alert-level.high {
  background: rgba(255, 77, 79, 0.18);
  color: #ff6b6b;
  border: 1px solid #ff4d4f;
}

.alert-record-row {
  color: #85acd5;
  font-size: 12px;
  margin: 4px 0;
}

.alert-record-row b {
  color: #fff;
  font-weight: normal;
}

.temp-danger {
  color: #ff6b6b !important;
  font-weight: bold !important;
}

.threshold-text {
  margin-left: 8px;
  color: #ffcc80;
}

.alert-record-actions {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}

.status-pill {
  font-size: 11px;
  border-radius: 999px;
  padding: 3px 8px;
}

.status-pending {
  background: rgba(255, 77, 79, 0.16);
  color: #ff6b6b;
  border: 1px solid rgba(255, 77, 79, 0.6);
}

.status-processing {
  background: rgba(255, 152, 0, 0.16);
  color: #ffb74d;
  border: 1px solid rgba(255, 152, 0, 0.65);
}

.status-handled {
  background: rgba(0, 229, 255, 0.13);
  color: #00e5ff;
  border: 1px solid rgba(0, 229, 255, 0.55);
}

.status-finished {
  background: rgba(76, 217, 100, 0.15);
  color: #4cd964;
  border: 1px solid rgba(76, 217, 100, 0.65);
}

.status-appeal {
  background: rgba(156, 39, 176, 0.16);
  color: #ce93d8;
  border: 1px solid rgba(156, 39, 176, 0.65);
}

.mini-btn {
  padding: 3px 8px;
  border-radius: 4px;
  cursor: pointer;
  background: rgba(0, 122, 204, 0.25);
  color: #00e5ff;
  border: 1px solid #007acc;
  font-size: 11px;
}

.mini-btn:hover {
  background: rgba(0, 122, 204, 0.48);
}

.mini-btn.secondary {
  color: #85acd5;
}

/* 右侧数据统计+搜索区域 */
.data-search-section {
  gap: 10px;
}

.stats-panel {
  flex: 1;
}

.camera-status-panel {
  flex: 1;
  max-height: 220px;
}

.search-panel {
  flex: 2;
}

/* 核心数据统计样式 */
.stats-grid {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
  text-align: center;
}

.stat-item {
  flex: 1;
  padding: 0 10px;
}

.stat-value {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(0, 26, 54, 0.8);
  border: 1px solid #007acc;
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #00e5ff;
  font-size: 20px;
  font-weight: bold;
}

.stat-label {
  color: #85acd5;
  font-size: 12px;
  margin-bottom: 5px;
}

.stats-trend {
  font-size: 11px;
}

.stats-trend.up {
  color: #4cd964;
}

.stats-trend.down {
  color: #ff6b6b;
}

.stats-compare {
  background: rgba(0, 26, 54, 0.5);
  border-radius: 8px;
  padding: 10px;
  margin-top: auto;
}

.compare-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #85acd5;
  font-size: 12px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(0, 122, 204, 0.2);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(to right, #00e5ff, #007acc);
  border-radius: 4px;
  transition: width 0.5s ease;
}

/* 摄像头状态列表 */
.camera-status-list {
  list-style: none;
  max-height: 160px;
  overflow-y: auto;
}

.camera-status-line {
  height: 36px;
  padding: 0 8px;
  border-bottom: 1px solid rgba(0, 122, 204, 0.22);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #85acd5;
  font-size: 12px;
}

/* 搜索面板样式 */
.search-tabs {
  display: flex;
  gap: 5px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(0, 122, 204, 0.3);
  padding-bottom: 5px;
}

.tab-item {
  padding: 5px 15px;
  color: #85acd5;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s;
}

.tab-item.active {
  background: rgba(0, 229, 255, 0.1);
  color: #00e5ff;
  border-bottom: 2px solid #00e5ff;
}

/* 搜索表单样式 */
.search-form {
  background: rgba(0, 26, 54, 0.5);
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 15px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
  flex-wrap: wrap;
}

.form-label {
  color: #85acd5;
  font-size: 12px;
  min-width: 70px;
}

.form-control {
  flex: 1;
  min-width: 80px;
  padding: 5px 8px;
  background: rgba(0, 17, 36, 0.8);
  border: 1px solid #007acc;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  outline: none;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 10px;
}

.btn {
  padding: 6px 15px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  border: none;
}

.btn.primary {
  background: #007acc;
  color: #fff;
}

.btn.primary:hover {
  background: #00e5ff;
}

.btn.secondary {
  background: rgba(0, 122, 204, 0.2);
  color: #00e5ff;
  border: 1px solid #007acc;
}

.btn.small {
  padding: 2px 8px;
  font-size: 11px;
  background: rgba(0, 122, 204, 0.24);
  color: #00e5ff;
  border: 1px solid #007acc;
}

/* 搜索结果 */
.simple-result-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.simple-result-item {
  padding: 8px;
  border-radius: 6px;
  color: #85acd5;
  font-size: 12px;
  background: rgba(0, 26, 54, 0.55);
  border: 1px solid rgba(0, 122, 204, 0.32);
}

/* 分析区域 */
.analysis-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.analysis-card {
  padding: 12px 8px;
  border-radius: 8px;
  background: rgba(0, 26, 54, 0.55);
  border: 1px solid rgba(0, 122, 204, 0.35);
  text-align: center;
}

.analysis-card span {
  display: block;
  color: #85acd5;
  font-size: 11px;
  margin-bottom: 8px;
}

.analysis-card b {
  color: #00e5ff;
  font-size: 20px;
}

.analysis-note {
  margin-top: 14px;
  color: #85acd5;
  font-size: 12px;
  line-height: 1.7;
  padding: 10px;
  border-radius: 8px;
  background: rgba(255, 152, 0, 0.08);
  border: 1px solid rgba(255, 152, 0, 0.28);
}

/* 实时数据样式 */
.realtime-data {
  display: flex;
  flex-direction: column;
  gap: 15px;
  height: 100%;
}

.realtime-item {
  background: rgba(0, 26, 54, 0.5);
  border-radius: 8px;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.realtime-label {
  color: #85acd5;
  font-size: 12px;
  min-width: 80px;
}

.realtime-value {
  color: #00e5ff;
  font-size: 20px;
  font-weight: bold;
  min-width: 60px;
  text-align: center;
}

.data-refresh {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #85acd5;
  font-size: 11px;
  margin-top: auto;
}

/* 空状态样式 */
.empty-state {
  color: #85acd5;
  text-align: center;
  padding: 20px;
  font-size: 12px;
}

/* 弹窗样式 */
.dialog-content {
  color: #333;
  font-size: 14px;
  line-height: 1.8;
}

.dialog-content p {
  margin: 6px 0;
}

.dialog-footer {
  text-align: right;
}

/* 加载遮罩 */
.loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 20, 40, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-content {
  text-align: center;
  color: #00e5ff;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 229, 255, 0.3);
  border-radius: 50%;
  border-top-color: #00e5ff;
  animation: spin 1s ease-in-out infinite;
  margin: 0 auto 10px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 26, 54, 0.3);
  border-radius: 2px;
}

::-webkit-scrollbar-thumb {
  background: #007acc;
  border-radius: 2px;
}

::-webkit-scrollbar-thumb:hover {
  background: #00e5ff;
}

/* 响应式适配 */
@media screen and (max-width: 1600px) {
  .main-layout {
    grid-template-columns: 50% 25% 25%;
  }

  .trend-chart-wrapper {
    width: 250px;
    height: 150px;
  }

  .device-standby {
    max-width: 86%;
  }
}

@media screen and (max-height: 768px) {
  .camera-video {
    height: 140px;
  }

  .stat-value {
    width: 60px;
    height: 60px;
    font-size: 14px;
  }

  .infrared-info {
    top: 48px;
  }

  .realtime-status {
    top: 48px;
  }

  .trend-chart-wrapper {
    height: 135px;
  }

  .video-controls {
    bottom: 12px;
  }
}
</style>

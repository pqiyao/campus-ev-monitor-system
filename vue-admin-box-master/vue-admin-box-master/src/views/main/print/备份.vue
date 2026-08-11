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
        <!-- 左侧：红外热源检测流程模拟 -->
        <div class="layout-item infrared-section">
          <div class="infrared-container" id="car_control">
            <div class="infrared-box" id="infrared_box">
              <!-- 模拟说明：这不是物理红外，是本地摄像头 + 黑白滤镜 + 模拟温度 -->
              <div class="simulation-banner">
                当前为红外热源检测流程模拟｜本地摄像头 + 灰度滤镜 + 模拟温度数据
              </div>

              <!-- 本地摄像头画面 -->
              <video
                ref="videoRef"
                v-show="cameraReady"
                class="infrared-video"
                :class="{ paused: !isPlaying }"
                autoplay
                muted
                playsinline
              ></video>

              <!-- 摄像头不可用时的兜底图 -->
              <img
                v-show="!cameraReady"
                src="@/assets/images/hw.jpg"
                alt="红外监测兜底画面"
                class="infrared-img fallback-img"
              />

              <!-- 模拟高温区域框 -->
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
                  <span>📷 启动摄像头</span>
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
                <div class="info-item" v-if="cameraError">
                  <span class="info-label">摄像头：</span>
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

            <!-- 趋势图容器：不用额外引入 ECharts，先做轻量模拟趋势 -->
            <div class="trend-chart-wrapper">
              <div class="chart-title">近7天高温告警趋势</div>
              <div class="mock-trend-chart">
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
                  <span class="camera-name">本地巡检摄像头（{{ currentDate }}）</span>
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
                    autoplay
                    muted
                    playsinline
                  ></video>
                  <img
                    v-show="!cameraReady"
                    src="@/assets/images/hw.jpg"
                    alt="监控画面1"
                    class="camera-img"
                  />
                </div>
              </div>

              <div class="camera-item">
                <div class="camera-header">
                  <span class="camera-name">宿舍区停车棚A区</span>
                  <span class="camera-actions">
                    <span class="camera-status online">模拟在线</span>
                  </span>
                </div>
                <div class="camera-video">
                  <img src="@/assets/images/hw.jpg" alt="监控画面2" class="camera-img camera-gray" />
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
                    <b class="temp-danger">{{ record.maxTemperature.toFixed(1) }}℃</b>
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
                    <button class="mini-btn" @click="advanceAlarmStatus(record)">
                      下一步
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
                  <div class="stat-value">{{ todayAlarmCount }}</div>
                  <div class="stat-label">案件总量</div>
                  <div class="stats-trend up">+{{ todayAlarmCount * 8 }}%</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value">{{ handledCount }}</div>
                  <div class="stat-label">案件处理量</div>
                  <div class="stats-trend up">+{{ handledCount * 6 }}%</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value">{{ complaintCount }}</div>
                  <div class="stat-label">案件投诉量</div>
                  <div class="stats-trend down">-0%</div>
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

          <!-- 摄像头状态 -->
          <div class="panel-card camera-status-panel">
            <div class="panel-header">
              <span>摄像头状态</span>
              <span class="status-summary">在线：{{ cameraReady ? 2 : 1 }}/2</span>
            </div>
            <div class="panel-body">
              <ul class="camera-status-list">
                <li class="camera-status-line">
                  <span>CAM-IR-SIM-001</span>
                  <span class="camera-status" :class="cameraReady ? 'online' : 'warning'">
                    {{ cameraReady ? '在线' : '未授权' }}
                  </span>
                </li>
                <li class="camera-status-line">
                  <span>CAM-PARK-A-002</span>
                  <span class="camera-status online">模拟在线</span>
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
                    <div v-if="alarmRecords.length === 0" class="empty-state">
                      暂无车辆数据
                    </div>
                    <div v-else class="simple-result-list">
                      <div
                        v-for="record in alarmRecords.slice(0, 3)"
                        :key="record.id"
                        class="simple-result-item"
                      >
                        {{ record.location }}｜{{ record.type }}｜{{ record.status }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 违规分析 -->
                <div v-if="activeSearchTab === 'analysis'">
                  <div class="analysis-summary">
                    <div class="analysis-card">
                      <span>电池异常高温</span>
                      <b>{{ todayAlarmCount }}</b>
                    </div>
                    <div class="analysis-card">
                      <span>待处理</span>
                      <b>{{ pendingCount }}</b>
                    </div>
                    <div class="analysis-card">
                      <span>已处置/复查</span>
                      <b>{{ handledCount }}</b>
                    </div>
                  </div>

                  <div class="analysis-note">
                    当前原型使用模拟温度数据验证“红外热源检测 → 阈值告警 → 管理端闭环处置”流程；
                    后续可替换为真实红外热成像模块数据源。
                  </div>
                </div>

                <!-- 实时数据 -->
                <div v-if="activeSearchTab === 'realtime'">
                  <div class="realtime-data">
                    <div class="realtime-item">
                      <div class="realtime-label">在线摄像头</div>
                      <div class="realtime-value">{{ cameraReady ? 2 : 1 }}</div>
                    </div>
                    <div class="realtime-item">
                      <div class="realtime-label">实时违规数</div>
                      <div class="realtime-value">{{ pendingCount }}</div>
                    </div>
                    <div class="realtime-item">
                      <div class="realtime-label">当前最高温</div>
                      <div class="realtime-value">
                        {{ thermalState.maxTemperature.toFixed(1) }}℃
                      </div>
                    </div>
                    <div class="data-refresh">
                      <span>最后更新：{{ currentTime || '--:--:--' }}</span>
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
        <p><b>巡检设备：</b>{{ detailDialog.record.deviceId }}</p>
        <p><b>巡检区域：</b>{{ detailDialog.record.location }}</p>
        <p><b>最高温度：</b>{{ detailDialog.record.maxTemperature.toFixed(1) }}℃</p>
        <p><b>告警阈值：</b>{{ detailDialog.record.threshold }}℃</p>
        <p><b>告警时间：</b>{{ detailDialog.record.time }}</p>
        <p><b>处置状态：</b>{{ detailDialog.record.status }}</p>
        <p><b>说明：</b>当前为红外热源检测流程模拟，温度由前端模拟生成，后续可替换真实红外热像仪数据源。</p>
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

// 基础状态
const currentTime = ref('')
const activeSearchTab = ref('vehicle')
const isLoading = ref(false)

const currentDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

// 摄像头状态
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

// 热源检测模拟状态
const thermalState = ref({
  deviceId: 'CAM-IR-SIM-001',
  mode: '红外热源检测流程模拟',
  location: '宿舍区停车棚A区',
  minTemperature: 27.4,
  maxTemperature: 36.8,
  threshold: 60,
  alarm: false,
  hotspot: {
    x: 42,
    y: 36,
    width: 16,
    height: 12
  }
})

// 违规告警记录
const alarmRecords = ref([])

const trendData = ref([
  { day: '周一', count: 1 },
  { day: '周二', count: 0 },
  { day: '周三', count: 2 },
  { day: '周四', count: 1 },
  { day: '周五', count: 0 },
  { day: '周六', count: 1 },
  { day: '今日', count: 0 }
])

// 车辆搜索基础绑定
const vehicleSearch = ref({
  color: '',
  brand: '',
  plate: '',
  violationType: ''
})

// 详情弹窗基础状态
const detailDialog = ref({
  visible: false,
  title: '告警详情',
  record: null
})

// 统计计算
const todayAlarmCount = computed(() => alarmRecords.value.length)

const pendingCount = computed(() => {
  return alarmRecords.value.filter(item => item.status === '待处理').length
})

const processingCount = computed(() => {
  return alarmRecords.value.filter(item => item.status === '已通知').length
})

const handledCount = computed(() => {
  return alarmRecords.value.filter(item => item.status === '已处置' || item.status === '已复查').length
})

const complaintCount = computed(() => {
  return alarmRecords.value.filter(item => item.status === '申诉中').length
})

const handleRate = computed(() => {
  if (todayAlarmCount.value === 0) return 0
  return Math.round((handledCount.value / todayAlarmCount.value) * 100)
})

// 时间更新
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-')
}

const formatTime = () => {
  const now = new Date()
  const pad = num => String(num).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

const createAlarmId = () => {
  const now = new Date()
  const pad = num => String(num).padStart(2, '0')
  const timeText = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  const random = Math.floor(Math.random() * 900 + 100)
  return `IR-${timeText}-${random}`
}

// 启动本地摄像头
const startCamera = async () => {
  cameraError.value = ''

  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    cameraReady.value = false
    cameraError.value = '浏览器不支持摄像头'
    ElMessage.error('当前浏览器不支持 getUserMedia，请使用 Chrome / Edge')
    return
  }

  try {
    if (mediaStream) {
      stopCamera(false)
    }

    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        facingMode: 'environment'
      },
      audio: false
    })

    await nextTick()

    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
      videoRef.value.muted = true
      await videoRef.value.play()
    }

    if (miniVideoRef.value) {
      miniVideoRef.value.srcObject = mediaStream
      miniVideoRef.value.muted = true
      await miniVideoRef.value.play()
    }

    cameraReady.value = true
    isPlaying.value = true
    ElMessage.success('本地摄像头已启动，当前为黑白红外流程模拟画面')
  } catch (error) {
    console.error('摄像头启动失败：', error)
    cameraReady.value = false
    isPlaying.value = false

    if (error.name === 'NotAllowedError') {
      cameraError.value = '摄像头权限被拒绝'
      ElMessage.error('摄像头权限被拒绝，请在浏览器地址栏允许摄像头权限')
    } else if (error.name === 'NotFoundError') {
      cameraError.value = '未找到摄像头'
      ElMessage.error('未找到摄像头设备')
    } else {
      cameraError.value = '摄像头启动失败'
      ElMessage.error('摄像头启动失败，请使用 localhost 或 HTTPS 环境运行')
    }
  }
}

// 停止摄像头
const stopCamera = (showMessage = true) => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }

  if (videoRef.value) {
    videoRef.value.srcObject = null
  }

  if (miniVideoRef.value) {
    miniVideoRef.value.srcObject = null
  }

  cameraReady.value = false
  isPlaying.value = false

  if (showMessage) {
    ElMessage.info('摄像头已停止')
  }
}

// 播放/暂停
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
    ElMessage.info('红外模拟画面已暂停')
  } else {
    await videoRef.value.play()
    if (miniVideoRef.value) await miniVideoRef.value.play()
    isPlaying.value = true
    ElMessage.success('红外模拟画面已播放')
  }
}

// 保留原音量逻辑，虽然摄像头禁用音频
const toggleVolume = () => {
  isMuted.value = !isMuted.value
  ElMessage.info(isMuted.value ? '当前无音频输入，已静音' : '当前为无音频巡检画面')
}

// 模拟温度自动波动
const updateSimulatedTemperature = () => {
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

// 手动触发高温告警
const triggerHighTempAlarm = () => {
  thermalState.value.alarm = true
  thermalState.value.maxTemperature = Number((62 + Math.random() * 5).toFixed(1))
  thermalState.value.minTemperature = Number((28 + Math.random() * 3).toFixed(1))
  thermalState.value.hotspot = {
    x: Math.floor(35 + Math.random() * 20),
    y: Math.floor(30 + Math.random() * 18),
    width: 16,
    height: 12
  }

  const record = {
    id: createAlarmId(),
    type: '疑似电动车电池异常高温',
    deviceId: thermalState.value.deviceId,
    location: thermalState.value.location,
    maxTemperature: thermalState.value.maxTemperature,
    threshold: thermalState.value.threshold,
    time: formatTime(),
    status: '待处理',
    level: '高',
    levelClass: 'high',
    source: 'SIMULATED_THERMAL_CAMERA'
  }

  alarmRecords.value.unshift(record)
  if (alarmRecords.value.length > 20) {
    alarmRecords.value.pop()
  }

  trendData.value[6].count += 1

  ElMessage.error('已生成“疑似电动车电池异常高温”告警')
}

// 恢复正常温度
const recoverTemperature = () => {
  thermalState.value.alarm = false
  thermalState.value.maxTemperature = 36.8
  thermalState.value.minTemperature = 27.4
  ElMessage.success('温度已恢复正常，当前无异常')
}

// 告警状态流转
const advanceAlarmStatus = record => {
  const statusFlow = ['待处理', '已通知', '已处置', '已复查']
  const index = statusFlow.indexOf(record.status)

  if (index === -1 || index === statusFlow.length - 1) {
    ElMessage.info('该告警已完成闭环处置')
    return
  }

  record.status = statusFlow[index + 1]

  const messageMap = {
    已通知: '已通知管理员/车主',
    已处置: '已完成现场处置',
    已复查: '已完成二次复查，闭环结束'
  }

  ElMessage.success(messageMap[record.status] || '状态已更新')
}

// 状态样式
const getStatusClass = status => {
  const map = {
    待处理: 'status-pending',
    已通知: 'status-processing',
    已处置: 'status-handled',
    已复查: 'status-finished',
    申诉中: 'status-appeal'
  }
  return map[status] || 'status-pending'
}

// 全屏
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

// 刷新违规记录
const fetchViolationAlerts = () => {
  if (alarmRecords.value.length === 0) {
    ElMessage.info('当前暂无违规记录，可点击“触发高温告警”生成演示数据')
    return
  }

  ElMessage.success(`已刷新，共 ${alarmRecords.value.length} 条违规记录`)
}

// 搜索车辆
const searchVehicles = () => {
  ElMessage.success('已根据条件完成查询，当前展示红外高温告警关联记录')
}

const resetVehicleSearch = () => {
  vehicleSearch.value = {
    color: '',
    brand: '',
    plate: '',
    violationType: ''
  }
  ElMessage.info('搜索条件已重置')
}

const fetchRealtimeData = () => {
  updateSimulatedTemperature()
  ElMessage.success('实时数据已刷新')
}

const setActiveSearchTab = tab => {
  activeSearchTab.value = tab
}

const openDetail = record => {
  detailDialog.value = {
    visible: true,
    title: '红外高温告警详情',
    record
  }
}

const handleConfirm = () => {
  detailDialog.value.visible = false
}

// 布局适配
const adjustLayout = () => {
  const headerHeight = document.querySelector('.header')?.offsetHeight || 44
  const viewportHeight = window.innerHeight - headerHeight - 20
  const infraredContainer = document.getElementById('car_control')
  const infraredBox = document.getElementById('infrared_box')

  if (infraredContainer) infraredContainer.style.height = `${viewportHeight}px`
  if (infraredBox) infraredBox.style.height = '100%'
}

// 生命周期
onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)

  tempTimer = setInterval(updateSimulatedTemperature, 1800)

  resizeHandler = adjustLayout
  adjustLayout()
  window.addEventListener('resize', resizeHandler)

  // 进入页面自动尝试启动摄像头
  // 浏览器若限制自动授权，用户点击“启动摄像头”即可
  startCamera()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (tempTimer) clearInterval(tempTimer)

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

/* 模拟说明条 */
.simulation-banner {
  position: absolute;
  top: 15px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 12;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 152, 0, 0.16);
  border: 1px solid rgba(255, 152, 0, 0.75);
  color: #ffd08a;
  font-size: 12px;
  letter-spacing: 1px;
  backdrop-filter: blur(5px);
}

/* 红外视频样式：本地摄像头 + 黑白滤镜 */
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

.fallback-img {
  opacity: 0.88;
}

/* 模拟高温区域框 */
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

.mock-trend-chart {
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

.camera-video-live {
  filter: grayscale(100%) contrast(140%) brightness(85%);
  transform: scaleX(-1);
}

.camera-gray {
  filter: grayscale(100%) contrast(145%) brightness(85%);
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

  .simulation-banner {
    max-width: 86%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
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
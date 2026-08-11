<template>
  <div class="system-config-container page-bg-shell">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <div class="header-bg">
        <div class="header-left">
          <h1 class="title-text">⚙️ 系统配置中心</h1>
          <p class="title-sub">统一管理违规类型、电子围栏、系统阈值和通知模板</p>
        </div>
        <div class="header-actions">
          <el-button class="action-btn" @click="refreshAll" :icon="Refresh">刷新</el-button>
          <el-button type="primary" class="action-btn primary" @click="saveAll" :icon="Check">保存所有</el-button>
        </div>
      </div>
    </div>

    <!-- 标签页导航 -->
    <el-tabs v-model="activeConfigTab" class="config-tabs">
      <el-tab-pane label="违规类型" name="violation">
        <template #label>
          <span><span class="tab-icon">⚠️</span> 违规类型</span>
        </template>
        <div class="tab-content">
          <div class="pane-header">
            <span class="pane-title">违规类型配置</span>
            <el-button type="primary" size="small" @click="addViolationType">+ 新增类型</el-button>
          </div>
          <el-table :data="violationTypes" stripe style="width: 100%" class="config-table">
            <el-table-column prop="typeId" label="ID" width="80" align="center" />
            <el-table-column label="类型名称">
              <template #default="{ row }">
                <el-input v-model="row.typeName" placeholder="请输入" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="描述">
              <template #default="{ row }">
                <el-input v-model="row.description" placeholder="描述" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="扣分" width="100">
              <template #default="{ row }">
                <el-input-number v-model.number="row.deductionPoints" :min="0" :max="12" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="罚款(元)" width="120">
              <template #default="{ row }">
                <el-input-number v-model.number="row.penaltyAmount" :min="0" :precision="2" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="申诉时限(小时)" width="130">
              <template #default="{ row }">
                <el-input-number v-model.number="row.appealDeadline" :min="1" :max="720" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="saveViolationType(row)">保存</el-button>
                <el-button size="small" type="danger" plain @click="deleteViolationType(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="电子围栏" name="fence" lazy>
        <template #label>
          <span><span class="tab-icon">🗺️</span> 电子围栏</span>
        </template>
        <div class="tab-content fence-content">
          <div class="pane-header">
            <span class="pane-title">围栏管理</span>
            <div class="pane-actions">
              <el-button size="small" @click="refreshFenceData" :icon="Refresh">刷新围栏数据</el-button>
              <el-button type="primary" size="small" @click="startDrawFence" :icon="Plus">绘制围栏</el-button>
              <el-button type="warning" size="small" @click="completeFenceDrawing">结束绘制</el-button>
              <el-button type="success" size="small" @click="createNewFence">新建围栏</el-button>
            </div>
          </div>

          <!-- 汇总：对接 GET /geofence/summary -->
          <div v-if="fenceSummary" class="fence-stats">
            <div class="stat-card">
              <span class="stat-val">{{ fenceSummary.totalFences ?? '—' }}</span>
              <span class="stat-label">围栏总数</span>
            </div>
            <div class="stat-card">
              <span class="stat-val accent">{{ fenceSummary.enabledFences ?? '—' }}</span>
              <span class="stat-label">已启用</span>
            </div>
            <div class="stat-card">
              <span class="stat-val warn">{{ fenceSummary.todayAlerts ?? '—' }}</span>
              <span class="stat-label">今日告警</span>
            </div>
            <div class="stat-card">
              <span class="stat-val danger">{{ fenceSummary.unprocessed ?? '—' }}</span>
              <span class="stat-label">未处理告警</span>
            </div>
          </div>

          <!-- 已启用列表：对接 GET /geofence/enabled（与地图判定一致） -->
          <div v-if="fenceEnabledList.length" class="fence-enabled-bar">
            <span class="fence-enabled-label">已启用围栏（运行时生效）</span>
            <div class="fence-enabled-tags">
              <el-tag
                v-for="f in fenceEnabledList"
                :key="f.fenceId"
                size="small"
                type="success"
                effect="plain"
                class="fence-tag"
              >
                {{ f.fenceName }}
              </el-tag>
            </div>
          </div>

          <!-- 告警：对接 GET /geofence/alerts + PUT .../process -->
          <div class="fence-alerts-wrap">
            <div class="fence-alerts-head">
              <span class="pane-title sub">围栏告警</span>
              <span class="fence-alerts-tip">最近记录，可标记已处理</span>
            </div>
            <el-table
              v-if="fenceAlerts.length"
              :data="fenceAlerts"
              size="small"
              stripe
              class="fence-alerts-table"
              max-height="280"
            >
              <el-table-column prop="alertId" label="ID" width="70" align="center" />
              <el-table-column prop="fenceName" label="围栏" min-width="100" show-overflow-tooltip />
              <el-table-column prop="licensePlate" label="车牌" width="100" />
              <el-table-column label="类型" width="88">
                <template #default="{ row }">
                  {{ row.alertType === 'exit' ? '出围' : row.alertType === 'enter' ? '入围' : row.alertType || '—' }}
                </template>
              </el-table-column>
              <el-table-column label="级别" width="72" align="center">
                <template #default="{ row }">
                  {{ alertLevelLabel(row.alertLevel) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="88" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.isProcessed === 1 ? 'info' : 'warning'" size="small">
                    {{ row.isProcessed === 1 ? '已处理' : '未处理' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="160">
                <template #default="{ row }">
                  {{ formatFenceTime(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.isProcessed !== 1"
                    type="primary"
                    link
                    size="small"
                    @click="markAlertProcessed(row)"
                  >
                    标记已处理
                  </el-button>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无围栏告警" :image-size="72" class="fence-alerts-empty" />
          </div>

          <div class="fence-layout">
            <!-- 左侧围栏列表 -->
            <div class="fence-list">
              <el-card v-for="fence in fenceList" :key="fence.fenceId" class="fence-card" :body-style="{ padding: '12px' }">
                <div class="fence-item" :style="{ borderLeftColor: fence.fenceColor }">
                  <div class="fence-header">
                    <span class="fence-name">{{ fence.fenceName }}</span>
                    <el-tag size="small" :type="fence.status === 1 ? 'success' : 'info'">{{ fence.status === 1 ? '启用' : '禁用' }}</el-tag>
                  </div>
                  <div class="fence-desc">{{ fence.description || '无描述' }}</div>
                  <div class="fence-meta">
                    <span>类型：{{ getFenceTypeName(fence.fenceType) }}</span>
                    <span>级别：{{ getAlertLevelName(fence.alertLevel) }}</span>
                  </div>
                  <div class="fence-actions">
                    <el-button size="small" type="primary" plain @click="editFence(fence)">编辑</el-button>
                    <el-button size="small" type="warning" plain @click="toggleFenceStatus(fence)">
                      {{ fence.status === 1 ? '禁用' : '启用' }}
                    </el-button>
                    <el-button size="small" type="danger" plain @click="deleteFence(fence.fenceId)">删除</el-button>
                  </div>
                </div>
              </el-card>
              <el-empty
                v-if="fenceList.length === 0"
                class="fence-empty"
                description="暂无围栏，点击绘制围栏创建"
                :image-size="100"
              />
            </div>
            <!-- 右侧地图绘制区 -->
            <div class="fence-map" id="fence_map"></div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统阈值" name="threshold">
        <template #label>
          <span><span class="tab-icon">⚙️</span> 系统阈值</span>
        </template>
        <div class="tab-content">
          <div class="pane-header">
            <span class="pane-title">阈值配置</span>
          </div>
          <el-form label-position="top" class="threshold-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="超速阈值 (km/h)">
                  <el-slider v-model="thresholds.speed" :min="5" :max="50" show-input />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="低电量预警 (%)">
                  <el-slider v-model="thresholds.battery" :min="5" :max="50" show-input />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="离线预警 (分钟)">
                  <el-slider v-model="thresholds.offline" :min="1" :max="120" show-input />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="申诉截止时间 (小时)">
                  <el-input-number v-model="thresholds.appealDeadline" :min="1" :max="720" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="通知配置" name="notification">
        <template #label>
          <span><span class="tab-icon">📢</span> 通知配置</span>
        </template>
        <div class="tab-content">
          <div class="pane-header">
            <span class="pane-title">通知模板</span>
          </div>
          <el-form label-position="top" class="notification-form">
            <el-form-item label="违规提醒模板">
              <el-input v-model="notifyTemplates.violation" type="textarea" :rows="3" placeholder="支持变量：${plate}, ${time}, ${location}, ${type}, ${points}" />
            </el-form-item>
            <el-form-item label="整改提醒模板">
              <el-input v-model="notifyTemplates.rectify" type="textarea" :rows="3" placeholder="变量：${id}" />
            </el-form-item>
            <el-form-item label="申诉结果模板">
              <el-input v-model="notifyTemplates.appealResult" type="textarea" :rows="3" placeholder="变量：${id}, ${result}, ${remark}" />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="通知重试次数">
                  <el-input-number v-model="notifyConfig.retryCount" :min="0" :max="10" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="通知超时(秒)">
                  <el-input-number v-model="notifyConfig.timeout" :min="1" :max="60" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 围栏编辑/创建抽屉：底部操作栏放在主体内，避免页脚被遮挡或 teleport 下样式异常 -->
    <el-drawer
      v-model="fenceDrawer.visible"
      :title="fenceDrawer.isEdit ? '编辑围栏' : '新建围栏'"
      size="420px"
      destroy-on-close
      append-to-body
      custom-class="fence-drawer-panel"
    >
      <div class="fence-drawer-inner">
        <div class="fence-drawer-scroll">
          <el-form label-position="top" :model="fenceDrawer.form">
            <el-form-item label="名称 *" required>
              <el-input v-model="fenceDrawer.form.name" placeholder="围栏名称" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="fenceDrawer.form.type" placeholder="请选择" popper-class="site-config-select-dropdown">
                <el-option label="禁止进入" value="1" />
                <el-option label="禁止驶出" value="2" />
                <el-option label="限速区域" value="3" />
                <el-option label="监控区域" value="4" />
                <el-option label="允许停车区" value="5" />
              </el-select>
            </el-form-item>
            <el-form-item label="预警级别">
              <el-select v-model="fenceDrawer.form.level" placeholder="请选择" popper-class="site-config-select-dropdown">
                <el-option label="提示" value="1" />
                <el-option label="警告" value="2" />
                <el-option label="严重" value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="颜色">
              <el-color-picker v-model="fenceDrawer.form.color" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="fenceDrawer.form.desc" type="textarea" :rows="2" placeholder="描述" />
            </el-form-item>
            <el-form-item label="坐标（JSON）*" required>
              <el-input
                v-model="fenceDrawer.coordsText"
                type="textarea"
                :rows="7"
                placeholder='绘制后自动填入；可手动粘贴 JSON，至少 3 个点：[{"lng":112.68,"lat":26.89},...]'
                class="fence-coords-input"
              />
              <p class="fence-coords-hint">至少 3 个顶点；支持 lng/lat 或 longitude/latitude。绘制：依次点击顶点，双击结束多边形。</p>
            </el-form-item>
          </el-form>
        </div>
        <div class="fence-drawer-bar">
          <el-button @click="fenceDrawer.visible = false">取消</el-button>
          <el-button type="primary" @click="saveFence" :loading="fenceDrawer.saving">保存到数据库</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox, ElTabs, ElTabPane } from 'element-plus'
import { Refresh, Check, Plus } from '@element-plus/icons-vue'
import axios from 'axios'
import request from '@/utils/system/request'
import {
  getGeofenceList,
  getGeofenceEnabled,
  createGeofence,
  updateGeofence,
  deleteGeofence,
  toggleGeofence,
  getGeofenceAlerts,
  processGeofenceAlert,
  getGeofenceSummary
} from '@/api/geofence'

// 当前激活的标签页
const activeConfigTab = ref('violation')

// 违规类型数据
const violationTypes = ref([])

// 围栏数据
const fenceList = ref([])
const fenceSummary = ref(null)
const fenceAlerts = ref([])
const fenceEnabledList = ref([])
let map = null
let drawingManager = null
let currentPolygon = null
let isFenceManualDrawing = false
let fenceDrawingPoints = []
let fenceDrawingTempPolyline = null
const FENCE_MAP_CENTER = { lng: 112.6827629, lat: 26.8860959 }
const FENCE_MAP_ZOOM = 18
const FENCE_MAP_STYLE = [
  { featureType: 'all', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'poilabel', elementType: 'all', stylers: { visibility: 'off' } },
  { featureType: 'poi', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'poi', elementType: 'icons', stylers: { visibility: 'off' } },
  { featureType: 'road', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'building', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'district', elementType: 'labels', stylers: { visibility: 'off' } }
]
/** 仅移除业务围栏多边形，禁止 map.clearOverlays()（会拆掉 DrawingManager 控件导致绘制异常） */
const fencePolygonOverlays = []
const fenceDrawer = reactive({
  visible: false,
  isEdit: false,
  fenceId: null,
  saving: false,
  form: { name: '', type: '1', level: '2', color: '#00d4ff', desc: '' },
  coordsText: ''
})

// 阈值配置
const thresholds = reactive({
  speed: 15,
  battery: 20,
  offline: 30,
  appealDeadline: 72
})

// 通知配置
const notifyTemplates = reactive({
  violation: '您的车辆${plate}于${time}在${location}发生${type}违规，扣${points}分。请尽快处理。',
  rectify: '您的违规记录（ID:${id}）已整改，请等待核实。',
  appealResult: '您的申诉（ID:${id}）处理结果为：${result}。备注：${remark}'
})
const notifyConfig = reactive({ retryCount: 3, timeout: 10 })

// ---------- 违规类型 API ----------
const fetchViolationTypes = async () => {
  try {
    const res = await request({
      url: '/api/system/violation-types',
      method: 'get'
    })
    const rows = res.data || []
    violationTypes.value = rows.map((r) => ({
      typeId: r.type_id ?? r.typeId,
      typeName: r.type_name ?? r.typeName ?? '',
      description: r.description ?? '',
      deductionPoints: Number(r.deduction_points ?? r.deductionPoints ?? 1),
      penaltyAmount: Number(r.penalty_amount ?? r.penaltyAmount ?? 0),
      appealDeadline: Number(r.appeal_deadline ?? r.appealDeadline ?? 72)
    }))
  } catch (e) {
    ElMessage.error('获取违规类型失败：' + (e?.message || e?.msg || '未知错误'))
  }
}

const addViolationType = () => {
  violationTypes.value.push({
    typeId: 0,
    typeName: '',
    description: '',
    deductionPoints: 1,
    penaltyAmount: 0,
    appealDeadline: 72
  })
}

const saveViolationType = async (type) => {
  if (!type.typeName) {
    ElMessage.warning('类型名称不能为空')
    return
  }
  try {
    if (type.typeId === 0) {
      await request({
        url: '/api/system/violation-types',
        method: 'post',
        data: type
      })
    } else {
      await request({
        url: `/api/system/violation-types/${type.typeId}`,
        method: 'put',
        data: type
      })
    }
    ElMessage.success('保存成功')
    await fetchViolationTypes()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || e?.msg || '未知错误'))
  }
}

const deleteViolationType = async (type) => {
  if (type.typeId === 0) {
    // 本地未保存，直接移除
    const index = violationTypes.value.findIndex(t => t === type)
    if (index !== -1) violationTypes.value.splice(index, 1)
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除类型 "${type.typeName}" 吗？`, '提示', { type: 'warning' })
    await request({
      url: `/api/system/violation-types/${type.typeId}`,
      method: 'delete'
    })
    ElMessage.success('删除成功')
    await fetchViolationTypes()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e?.message || e?.msg || '未知错误'))
  }
}

// ---------- 电子围栏 API（统一走 @/api/geofence，与后端 9 个接口对齐） ----------
const formatFenceTime = (v) => {
  if (v == null || v === '') return '—'
  const s = String(v).replace('T', ' ')
  return s.length > 19 ? s.slice(0, 19) : s
}

const alertLevelLabel = (l) => ({ 1: '提示', 2: '警告', 3: '严重' }[l] || '—')

const createNewFence = () => {
  fenceDrawer.visible = true
  fenceDrawer.isEdit = false
  fenceDrawer.form = { name: '', type: '1', level: '2', color: '#00d4ff', desc: '' }
  fenceDrawer.coordsText = ''
}

/** 兼容后端 snake_case 与旧数据 */
const normalizeFenceRow = (f) => {
  if (!f || typeof f !== 'object') return f
  return {
    ...f,
    fenceId: f.fenceId ?? f.fence_id,
    fenceName: f.fenceName ?? f.fence_name,
    fenceType: f.fenceType ?? f.fence_type,
    fenceColor: f.fenceColor ?? f.fence_color,
    alertLevel: f.alertLevel ?? f.alert_level,
    coordinates: f.coordinates,
    description: f.description,
    status: f.status
  }
}

/** 百度 GL 多边形 stroke 需稳定 hex，避免 rgba 无法渲染 */
const normalizeColorHex = (c) => {
  if (!c || typeof c !== 'string') return '#00d4ff'
  const s = c.trim()
  if (/^#[0-9a-fA-F]{6}$/.test(s)) return s
  if (/^#[0-9a-fA-F]{3}$/.test(s)) return s
  const m = s.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i)
  if (m) {
    const h = (n) =>
      Math.max(0, Math.min(255, parseInt(n, 10)))
        .toString(16)
        .padStart(2, '0')
    return `#${h(m[1])}${h(m[2])}${h(m[3])}`
  }
  return '#00d4ff'
}

const removeFencePolygonOverlaysOnly = () => {
  if (!map) return
  while (fencePolygonOverlays.length) {
    const o = fencePolygonOverlays.pop()
    try {
      map.removeOverlay(o)
    } catch (e) {
      /* ignore */
    }
  }
}

const fetchFenceList = async () => {
  try {
    const res = await getGeofenceList()
    const raw = res.data
    fenceList.value = Array.isArray(raw) ? raw.map(normalizeFenceRow) : []
  } catch (e) {
    fenceList.value = []
    ElMessage.error('获取围栏列表失败：' + (e?.message || e?.msg || '未知错误'))
  }
}

const fetchFenceSummary = async () => {
  try {
    const res = await getGeofenceSummary()
    fenceSummary.value = res.data || {}
  } catch {
    fenceSummary.value = null
  }
}

const fetchFenceAlerts = async () => {
  try {
    const res = await getGeofenceAlerts(80)
    fenceAlerts.value = res.data || []
  } catch {
    fenceAlerts.value = []
  }
}

const fetchFenceEnabled = async () => {
  try {
    const res = await getGeofenceEnabled()
    fenceEnabledList.value = res.data || []
  } catch {
    fenceEnabledList.value = []
  }
}

const refreshFenceData = async () => {
  await Promise.all([
    fetchFenceList(),
    fetchFenceSummary(),
    fetchFenceAlerts(),
    fetchFenceEnabled()
  ])
  nextTick(() => drawAllFences())
}

const markAlertProcessed = async (row) => {
  if (!row?.alertId) return
  try {
    await processGeofenceAlert(row.alertId)
    ElMessage.success('已标记处理')
    await fetchFenceAlerts()
    await fetchFenceSummary()
  } catch (e) {
    ElMessage.error('操作失败：' + (e?.message || e?.msg || '未知错误'))
  }
}

/**
 * GL 版鼠标绘制（BMapGLLib）由百度托管在 BOS，必须在主库 api?type=webgl 加载之后再引入。
 * 参考：https://mapopen.baidu.com/faq/api?title=webgl （开源工具 BMapGLLib / DrawingManager）
 */
const BAIDU_DRAWING_CSS =
  'https://mapopen.bj.bcebos.com/github/BMapGLLib/DrawingManager/src/DrawingManager.min.css'
const BAIDU_DRAWING_JS =
  'https://mapopen.bj.bcebos.com/github/BMapGLLib/DrawingManager/src/DrawingManager.min.js'

let bmapGLDrawingLoadPromise = null

const loadDrawingManagerCssOnce = () =>
  new Promise((resolve, reject) => {
    const hit = document.querySelector(`link[href="${BAIDU_DRAWING_CSS}"]`)
    if (hit) return resolve()
    const link = document.createElement('link')
    link.rel = 'stylesheet'
    link.href = BAIDU_DRAWING_CSS
    link.onload = () => resolve()
    link.onerror = () => reject(new Error('DrawingManager 样式加载失败'))
    document.head.appendChild(link)
  })

const loadDrawingManagerScriptOnce = () =>
  new Promise((resolve, reject) => {
    const existing = [...document.querySelectorAll('script')].find(
      (s) => s.src && s.src.includes('BMapGLLib/DrawingManager')
    )
    if (existing) {
      if (window.BMapGLLib?.DrawingManager) return resolve()
      existing.addEventListener('load', () => resolve(), { once: true })
      existing.addEventListener('error', () => reject(new Error('DrawingManager 脚本加载失败')), {
        once: true
      })
      return
    }
    const s = document.createElement('script')
    s.src = BAIDU_DRAWING_JS
    s.async = true
    s.onload = () => resolve()
    s.onerror = () => reject(new Error('DrawingManager 脚本加载失败'))
    document.head.appendChild(s)
  })

/** 确保 window.BMapGLLib.DrawingManager 可用（与 index.html 手动引入二选一即可） */
const ensureBMapGLLibDrawing = () => {
  if (typeof window !== 'undefined' && window.BMapGLLib?.DrawingManager) {
    return Promise.resolve()
  }
  if (bmapGLDrawingLoadPromise) return bmapGLDrawingLoadPromise
  bmapGLDrawingLoadPromise = loadDrawingManagerCssOnce()
    .then(() => loadDrawingManagerScriptOnce())
    .then(() => {
      if (!window.BMapGLLib?.DrawingManager) {
        bmapGLDrawingLoadPromise = null
        throw new Error('BMapGLLib.DrawingManager 未定义')
      }
    })
    .catch((e) => {
      bmapGLDrawingLoadPromise = null
      throw e
    })
  return bmapGLDrawingLoadPromise
}

const getPolygonDrawingType = () =>
  typeof window.BMAP_DRAWING_POLYGON !== 'undefined' ? window.BMAP_DRAWING_POLYGON : 5

const resizeFenceMap = () => {
  if (!map || typeof map.resize !== 'function') return
  try {
    map.resize()
  } catch (e) {
    console.warn('fence map resize', e)
  }
}

const hideFenceMapLabels = () => {
  if (!map) return
  try {
    map.setDisplayOptions?.({
      poiText: false,
      poiIcon: false,
      building: true
    })
  } catch (e) {
    /* ignore */
  }
  try {
    map.setMapStyleV2?.({ styleJson: FENCE_MAP_STYLE })
  } catch (e) {
    /* ignore */
  }
  try {
    map.setMapStyle?.({ styleJson: FENCE_MAP_STYLE })
  } catch (e) {
    /* ignore */
  }
}

const initFenceMap = () => {
  nextTick(() => {
    const container = document.getElementById('fence_map')
    if (!container) return

    if (typeof BMapGL === 'undefined') {
      loadBaiduMapScript()
      return
    }

    if (map) {
      nextTick(() => {
        hideFenceMapLabels()
        resizeFenceMap()
        drawAllFences()
      })
      return
    }

    map = new BMapGL.Map(container)
    const point = new BMapGL.Point(FENCE_MAP_CENTER.lng, FENCE_MAP_CENTER.lat)
    map.centerAndZoom(point, FENCE_MAP_ZOOM)
    map.setMinZoom?.(15)
    map.setMaxZoom?.(20)
    map.enableScrollWheelZoom()
    hideFenceMapLabels()

    void ensureBMapGLLibDrawing()
      .then(() => {
        try {
          drawingManager?.close?.()
        } catch (e) {
          /* ignore */
        }
        drawingManager = new BMapGLLib.DrawingManager(map, {
          isOpen: false,
          drawingType: getPolygonDrawingType(),
          polygonOptions: { strokeColor: '#00d4ff', fillColor: 'rgba(0,212,255,0.28)' }
        })
        drawingManager.addEventListener('polygoncomplete', onPolygonComplete)
        nextTick(() => {
          resizeFenceMap()
          setTimeout(() => {
            resizeFenceMap()
            drawAllFences()
          }, 200)
        })
      })
      .catch((e) => {
        console.error(e)
        ElMessage.warning(
          `围栏绘制扩展加载失败：${e.message || e}。请检查网络；地图仍可显示已有围栏，但无法鼠标绘制。`
        )
        nextTick(() => {
          resizeFenceMap()
          drawAllFences()
        })
      })
  })
}

const BAIDU_MAP_AK = 'SUBe4C3SHSL9Uyd9P4xgq6GMdaZF2Irj'

const loadBaiduMapScript = () => {
  if (typeof BMapGL !== 'undefined') {
    initFenceMap()
    return
  }
  const existing = document.querySelector('script[src*="api.map.baidu.com/api?type=webgl"]')
  if (existing) {
    let n = 0
    const t = setInterval(() => {
      n += 1
      if (typeof BMapGL !== 'undefined') {
        clearInterval(t)
        initFenceMap()
      } else if (n > 200) {
        clearInterval(t)
        ElMessage.error('百度地图 GL 脚本长时间未就绪，请刷新页面')
      }
    }, 50)
    return
  }
  window.initSiteFenceBaiduMap = () => initFenceMap()
  const script = document.createElement('script')
  script.src = `https://api.map.baidu.com/api?type=webgl&v=1.0&ak=${BAIDU_MAP_AK}&callback=initSiteFenceBaiduMap`
  document.head.appendChild(script)
}

const drawAllFences = () => {
  if (!map || typeof BMapGL === 'undefined') return
  removeFencePolygonOverlaysOnly()
  fenceList.value.forEach((fence) => {
    if (!fence.coordinates) return
    try {
      const arr = JSON.parse(fence.coordinates)
      if (!Array.isArray(arr) || arr.length < 3) return
      const points = arr.map((p) => new BMapGL.Point(Number(p.lng), Number(p.lat)))
      const stroke = normalizeColorHex(fence.fenceColor || '#00d4ff')
      const polygon = new BMapGL.Polygon(points, {
        strokeColor: stroke,
        fillColor: stroke,
        fillOpacity: 0.22,
        strokeWeight: 2,
        strokeOpacity: 0.95
      })
      polygon._fenceId = fence.fenceId
      map.addOverlay(polygon)
      fencePolygonOverlays.push(polygon)
    } catch (e) {
      console.error('绘制围栏失败', fence?.fenceId, e)
    }
  })
}

const attachFenceDrawingClickHandler = () => {
  if (!map || isFenceManualDrawing) return
  isFenceManualDrawing = true
  fenceDrawingPoints = []
  if (typeof map.addEventListener === 'function') {
    map.addEventListener('click', onFenceMapClick)
  }
}

const detachFenceDrawingClickHandler = () => {
  if (!map) return
  isFenceManualDrawing = false
  fenceDrawingPoints = []
  if (typeof map.removeEventListener === 'function') {
    map.removeEventListener('click', onFenceMapClick)
  }
  if (fenceDrawingTempPolyline) {
    try { map.removeOverlay(fenceDrawingTempPolyline) } catch (e) { /* ignore */ }
    fenceDrawingTempPolyline = null
  }
}

const onFenceMapClick = (e) => {
  if (!isFenceManualDrawing || !e || !e.point) return
  const p = e.point
  fenceDrawingPoints.push({ lng: p.lng, lat: p.lat })
  fenceDrawer.coordsText = JSON.stringify(fenceDrawingPoints, null, 2)

  try {
    if (fenceDrawingTempPolyline) {
      map.removeOverlay(fenceDrawingTempPolyline)
      fenceDrawingTempPolyline = null
    }
    if (fenceDrawingPoints.length > 1) {
      const pts = fenceDrawingPoints.map((item) => new BMapGL.Point(item.lng, item.lat))
      fenceDrawingTempPolyline = new BMapGL.Polyline(pts, {
        strokeColor: '#00d4ff',
        strokeWeight: 2,
        strokeOpacity: 0.9
      })
      map.addOverlay(fenceDrawingTempPolyline)
    }
  } catch (error) {
    console.warn('手动绘制临时线失败', error)
  }
}

const startDrawFence = () => {
  if (!drawingManager) {
    ElMessage.warning('地图绘制工具未加载，请稍后重试或刷新页面')
    return
  }
  resizeFenceMap()
  drawingManager.open()
  const polygonType = getPolygonDrawingType()
  if (typeof drawingManager.setDrawingType === 'function') {
    drawingManager.setDrawingType(polygonType)
  } else if (typeof drawingManager.setDrawingMode === 'function') {
    drawingManager.setDrawingMode(polygonType)
  } else if (typeof drawingManager.setDrawingModeEnabled === 'function') {
    drawingManager.setDrawingModeEnabled(polygonType)
  } else if (typeof drawingManager.setDrawMode === 'function') {
    drawingManager.setDrawMode(polygonType)
  } else {
    console.warn('DrawingManager 不支持 setDrawingType/setDrawingMode，已使用手动点选模式')
  }
  attachFenceDrawingClickHandler()
  ElMessage.info('在地图上依次点击画出顶点，双击鼠标结束多边形；如果无效，请点击“结束绘制”按钮')
  fenceDrawer.visible = true
  fenceDrawer.isEdit = false
  fenceDrawer.form = { name: '', type: '1', level: '2', color: '#00d4ff', desc: '' }
  fenceDrawer.coordsText = ''
}

const onPolygonComplete = (polygon) => {
  currentPolygon = polygon
  const path = polygon.getPath()
  const coords = path.map(p => ({ lng: p.lng, lat: p.lat }))
  fenceDrawer.coordsText = JSON.stringify(coords, null, 2)
  detachFenceDrawingClickHandler()
  isFenceManualDrawing = false
  // 结束绘制模式
  try {
    drawingManager?.close?.()
  } catch (e) {
    console.warn('绘制收起失败', e)
  }
  // 移除临时多边形，保存时重新绘制
  try {
    map.removeOverlay(polygon)
  } catch (e) {
    console.warn('移除临时多边形失败', e)
  }
  ElMessage.success('绘制完成，坐标已填入。可编辑或直接保存。')
  if (!fenceDrawer.visible) {
  fenceDrawer.visible = true
  ElMessage.info('绘制完成，请点击保存按钮')
  }
}

const completeFenceDrawing = () => {
  if (currentPolygon) {
    const path = currentPolygon.getPath()
    if (path && path.length >= 3) {
      const coords = path.map(p => ({ lng: p.lng, lat: p.lat }))
      fenceDrawer.coordsText = JSON.stringify(coords, null, 2)
      try { drawingManager?.close?.() } catch (e) { console.warn('结束绘制失败', e) }
      try { map.removeOverlay(currentPolygon) } catch (e) { console.warn('移除当前绘制多边形失败', e) }
      currentPolygon = null
      detachFenceDrawingClickHandler()
      // ✅ 确保抽屉可见
      if (!fenceDrawer.visible) {
        fenceDrawer.visible = true
      }
      ElMessage.success('已结束DrawingManager绘制，坐标已生成。请点击"新建围栏"按钮进行编辑和保存。')
      return
    }
  }

  // 以下为手动绘制分支，同样应确保抽屉可见
  if (isFenceManualDrawing && fenceDrawingPoints.length >= 3) {
    fenceDrawer.coordsText = JSON.stringify(fenceDrawingPoints, null, 2)
    detachFenceDrawingClickHandler()
    if (!fenceDrawer.visible) {
      fenceDrawer.visible = true
    }
    ElMessage.success('已手动结束绘制，坐标已生成。请点击"新建围栏"按钮进行编辑和保存。')
    return
  }

  ElMessage.warning('当前未完成多边形绘制，请继续点击地图至少 3 个点后再结束。')
}

/** 解析坐标 JSON，统一为 [{lng,lat},...] 字符串；失败返回 { error } */
const buildCoordinatesPayload = (text) => {
  const trimmed = (text || '').trim()
  if (!trimmed) return { error: '请填写坐标 JSON，或在地图上绘制围栏' }
  let arr
  try {
    arr = JSON.parse(trimmed)
  } catch {
    return { error: '坐标不是合法 JSON' }
  }
  if (!Array.isArray(arr)) return { error: '坐标须为 JSON 数组' }
  const normPoint = (p) => {
    if (!p || typeof p !== 'object') return null
    const lng = Number(p.lng ?? p.longitude ?? p.lon)
    const lat = Number(p.lat ?? p.latitude)
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
    return { lng, lat }
  }
  const pts = arr.map(normPoint).filter(Boolean)
  if (pts.length < 3) return { error: '至少需要 3 个有效顶点（lng/lat）' }
  return { coordinates: JSON.stringify(pts) }
}

const saveFence = async () => {
  if (!fenceDrawer.form.name) {
    ElMessage.warning('请输入围栏名称')
    return
  }
  const coordResult = buildCoordinatesPayload(fenceDrawer.coordsText)
  if (coordResult.error) {
    ElMessage.warning(coordResult.error)
    return
  }
  fenceDrawer.saving = true
  const fenceData = {
    fenceName: fenceDrawer.form.name,
    fenceType: parseInt(fenceDrawer.form.type),
    alertLevel: parseInt(fenceDrawer.form.level),
    fenceColor: normalizeColorHex(fenceDrawer.form.color),
    description: fenceDrawer.form.desc,
    coordinates: coordResult.coordinates,
    status: 1
  }
  console.log('发送围栏数据:', fenceData)
  console.log('坐标数据类型:', typeof fenceData.coordinates)
  console.log('坐标数据内容:', fenceData.coordinates)
  try {
    if (fenceDrawer.isEdit) {
      fenceData.fenceId = fenceDrawer.fenceId
      const res = await updateGeofence(fenceData)
      console.log('更新响应:', res)
      if (res?.data !== true) {
        ElMessage.error('保存失败：后端未更新成功（请确认围栏 ID 是否存在）')
        return
      }
    } else {
      const res = await createGeofence(fenceData)
      console.log('创建响应:', res)
      const created = res?.data
      console.log('创建的围栏对象:', created)
      // 放宽检查：只要有对象返回就算成功，后端会自动设置 fenceId
      if (!created || typeof created !== 'object') {
        ElMessage.error('保存失败：后端未返回围栏数据')
        return
      }
      // 如果没有 fenceId，尝试从响应中获取
      if (!created.fenceId && res?.data?.fenceId) {
        created.fenceId = res.data.fenceId
      }
    }
    ElMessage.success('保存成功')
    fenceDrawer.visible = false
    await refreshFenceData()
  } catch (e) {
    console.error('保存失败详情:', e)
    ElMessage.error('保存失败：' + (e?.message || e?.msg || e?.response?.data?.message || '未知错误'))
  } finally {
    fenceDrawer.saving = false
  }
}

const editFence = (fence) => {
  fenceDrawer.isEdit = true
  fenceDrawer.fenceId = fence.fenceId
  fenceDrawer.form = {
    name: fence.fenceName,
    type: String(fence.fenceType),
    level: String(fence.alertLevel),
    color: fence.fenceColor || '#00d4ff',
    desc: fence.description || ''
  }
  fenceDrawer.coordsText = fence.coordinates || ''
  fenceDrawer.visible = true
  // 地图上高亮该围栏（可选）
}

const toggleFenceStatus = async (fence) => {
  try {
    const res = await toggleGeofence(fence.fenceId)
    if (res?.data !== true) {
      ElMessage.error('状态未更新（围栏可能不存在或已被删除）')
      return
    }
    ElMessage.success('状态已更新')
    await refreshFenceData()
  } catch (e) {
    ElMessage.error('操作失败：' + (e?.message || e?.msg || '未知错误'))
  }
}

const deleteFence = async (id) => {
  const fid = Number(id)
  if (!Number.isFinite(fid) || fid < 1) {
    ElMessage.error('无效围栏 ID，无法删除')
    return
  }
  console.log('准备删除围栏:', fid)
  try {
    await ElMessageBox.confirm(
      '确定删除该围栏？将调用后端接口做逻辑删除，列表与地图将同步更新。',
      '删除围栏',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    console.log('调用deleteGeofence API')
    const res = await deleteGeofence(fid)
    console.log('删除API响应:', res)
    if (res?.data !== true) {
      ElMessage.error('删除失败：后端未删除成功（记录可能不存在或已删除）')
      return
    }
    ElMessage.success('删除成功')
    try {
      drawingManager?.close?.()
    } catch (e) {
      /* ignore */
    }
    await refreshFenceData()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    const msg =
      e?.response?.data?.message ||
      e?.message ||
      e?.msg ||
      (e && typeof e === 'object' && 'message' in e ? String(e.message) : '') ||
      '请求失败'
    console.error('删除失败详情:', e)
    ElMessage.error('删除失败：' + msg)
  }
}

const getFenceTypeName = (t) => ({ 1: '禁止进入', 2: '禁止驶出', 3: '限速', 4: '监控', 5: '允许停车区' }[t] || '未知')
const getAlertLevelName = (l) => ({ 1: '提示', 2: '警告', 3: '严重' }[l] || '未知')

// ---------- 刷新与保存 ----------
const refreshAll = () => {
  fetchViolationTypes()
  refreshFenceData()
  // 阈值和通知配置可以从后端获取，此处省略
  ElMessage.success('数据已刷新')
}

const saveAll = () => {
  ElMessage.info('请在各标签页内单独保存；电子围栏请点击「保存」写入数据库。')
}

watch(activeConfigTab, (name) => {
  if (name !== 'fence') return
  void refreshFenceData()
  nextTick(() => {
    initFenceMap()
    setTimeout(() => {
      resizeFenceMap()
      drawAllFences()
    }, 150)
  })
})

// ---------- 生命周期 ----------
onMounted(() => {
  fetchViolationTypes()
  refreshFenceData()
})

onUnmounted(() => {
  try {
    drawingManager?.close?.()
  } catch (e) {
    /* ignore */
  }
  drawingManager = null
  removeFencePolygonOverlaysOnly()
  map = null
})
</script>

<style scoped>
.system-config-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%);
  color: #fff;
  padding: 20px;
}
.page-header .header-bg {
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid rgba(0,123,255,0.2);
  display: flex;
  justify-content: space-between;
  align-items: center;
  backdrop-filter: blur(10px);
}
.title-text {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: #e8f4ff;
  letter-spacing: 0.02em;
  text-shadow: 0 0 28px rgba(0, 212, 255, 0.35);
}
.title-sub {
  font-size: 14px;
  color: rgba(200, 230, 255, 0.82);
  margin-top: 5px;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.action-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}
/* 顶栏「刷新」等默认按钮：深色底 + 亮字，避免浅底看不清 */
:deep(.header-actions .el-button--default) {
  --el-button-bg-color: rgba(12, 28, 52, 0.85);
  --el-button-border-color: rgba(0, 212, 255, 0.45);
  --el-button-text-color: #c5e8ff;
  --el-button-hover-bg-color: rgba(0, 212, 255, 0.12);
  --el-button-hover-border-color: #00d4ff;
  --el-button-hover-text-color: #e8f8ff;
}
.action-btn.primary {
  background: linear-gradient(90deg, #007bff, #6610f2);
  border: none;
  color: #fff;
}
.action-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,123,255,0.4);
}

.config-tabs {
  margin-top: 20px;
  background: transparent;
}
:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(0,212,255,0.3);
}
:deep(.el-tabs__item) {
  color: rgba(200, 225, 255, 0.88);
  font-size: 15px;
  padding: 0 20px;
}
:deep(.el-tabs__item.is-active) {
  color: #00d4ff;
  font-weight: 500;
}
:deep(.el-tabs__active-bar) {
  background: #00d4ff;
  height: 3px;
}
.tab-icon {
  margin-right: 6px;
  font-size: 16px;
}
.tab-content {
  background: rgba(255,255,255,0.03);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255,255,255,0.08);
  margin-top: 10px;
}
.pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.pane-title {
  font-size: 18px;
  font-weight: 500;
  color: #5ce1ff;
  letter-spacing: 0.5px;
  text-shadow: 0 0 18px rgba(0, 212, 255, 0.25);
}
.pane-title.sub {
  font-size: 16px;
  margin-bottom: 0;
}
.pane-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.fence-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  padding: 14px 16px;
  text-align: center;
}
.stat-val {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #e8f4ff;
}
.stat-val.accent {
  color: #5ce1ff;
}
.stat-val.warn {
  color: #fbbf24;
}
.stat-val.danger {
  color: #f87171;
}
.stat-label {
  display: block;
  font-size: 12px;
  color: rgba(200, 220, 255, 0.55);
  margin-top: 6px;
}
.fence-enabled-bar {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 10px;
}
.fence-enabled-label {
  display: block;
  font-size: 13px;
  color: #6ee7b7;
  margin-bottom: 10px;
}
.fence-enabled-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.fence-tag {
  border-color: rgba(16, 185, 129, 0.45) !important;
  color: #a7f3d0 !important;
}
.fence-alerts-wrap {
  margin-bottom: 18px;
}
.fence-alerts-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 10px;
}
.fence-alerts-tip {
  font-size: 12px;
  color: rgba(200, 220, 255, 0.45);
}
.fence-alerts-table {
  border-radius: 8px;
  overflow: hidden;
}
.fence-alerts-empty {
  padding: 16px 0;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 8px;
}
.fence-alerts-empty :deep(.el-empty__description p) {
  color: rgba(200, 220, 255, 0.5);
}
.muted {
  color: rgba(200, 220, 255, 0.35);
  font-size: 12px;
}
@media (max-width: 1100px) {
  .fence-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
.config-table {
  background: transparent;
  color: #fff;
}
:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(0,0,0,0.3);
  --el-table-row-hover-bg-color: rgba(0,212,255,0.1);
  --el-table-border-color: rgba(255,255,255,0.1);
  --el-table-text-color: #fff;
}
:deep(.el-table th) {
  color: #7ee8ff;
  font-weight: 500;
  background: rgba(0,0,0,0.3);
  border-bottom: 1px solid rgba(0,212,255,0.3);
}
:deep(.el-table td) {
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
:deep(.el-table .cell) {
  color: #e8f4ff;
}
:deep(.el-input__wrapper) {
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.2);
  box-shadow: none;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: #00d4ff;
  box-shadow: 0 0 0 1px #00d4ff;
}
:deep(.el-input__inner) {
  color: #e8f4ff;
}
:deep(.el-input__inner::placeholder) {
  color: rgba(200, 220, 255, 0.42);
}
/* 带 show-input 的滑块旁数字框 */
:deep(.el-slider__input) {
  min-width: 52px;
}
:deep(.el-slider__input .el-input__wrapper) {
  background: rgba(0, 0, 0, 0.35) !important;
  border: 1px solid rgba(0, 212, 255, 0.28);
  box-shadow: none;
}
:deep(.el-slider__input .el-input__inner) {
  color: #e8f4ff;
}
:deep(.el-input-number .el-input__wrapper) {
  padding: 1px 0;
}
:deep(.el-input-number__increase),
:deep(.el-input-number__decrease) {
  background: rgba(255,255,255,0.1);
  color: #fff;
  border: none;
}
:deep(.el-input-number__increase:hover),
:deep(.el-input-number__decrease:hover) {
  background: #00d4ff;
  color: #000;
}

/* 电子围栏布局 */
.fence-content {
  min-height: 500px;
}
.fence-layout {
  display: flex;
  gap: 20px;
  height: 550px;
}
.fence-list {
  width: 340px;
  overflow-y: auto;
  background: rgba(0,0,0,0.2);
  border-radius: 8px;
  padding: 8px;
}
.fence-card {
  margin-bottom: 12px;
  --el-card-bg-color: rgba(12, 20, 42, 0.92);
  background: rgba(12, 20, 42, 0.92);
  border: 1px solid rgba(0, 212, 255, 0.22);
  border-radius: 8px;
  transition: all 0.3s;
}
:deep(.fence-card .el-card__body) {
  color: #e8f4ff;
}
.fence-card:hover {
  border-color: #00d4ff;
  transform: translateY(-2px);
}
.fence-item {
  padding-left: 12px;
  border-left: 4px solid #00d4ff;
}
.fence-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}
.fence-name {
  font-size: 16px;
  font-weight: 600;
  color: #e8f4ff;
}
.fence-desc {
  font-size: 13px;
  color: rgba(200, 220, 255, 0.78);
  margin-bottom: 8px;
  line-height: 1.4;
}
.fence-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #7ee8ff;
  margin-bottom: 10px;
}
.fence-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
:deep(.fence-actions .el-button.is-plain) {
  --el-button-bg-color: rgba(0, 212, 255, 0.1);
  --el-button-border-color: rgba(0, 212, 255, 0.45);
  --el-button-text-color: #9aefff;
  --el-button-hover-bg-color: rgba(0, 212, 255, 0.2);
  --el-button-hover-text-color: #e8fcff;
}
/* 空状态：避免默认灰字在深色底上发虚 */
:deep(.fence-empty .el-empty__description p) {
  color: rgba(200, 225, 255, 0.85);
}
:deep(.fence-empty .el-empty__image svg) {
  opacity: 0.85;
  filter: drop-shadow(0 0 12px rgba(0, 212, 255, 0.25));
}
.fence-map {
  flex: 1;
  min-width: 280px;
  min-height: 480px;
  height: 100%;
  background: #001a36;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(0,212,255,0.2);
}

/* 阈值配置 */
.threshold-form {
  padding: 10px 0;
}
:deep(.el-form-item__label) {
  color: rgba(255,255,255,0.8);
  font-size: 14px;
  padding-bottom: 5px;
}
:deep(.el-slider__runway) {
  background: rgba(255,255,255,0.1);
}
:deep(.el-slider__bar) {
  background: #00d4ff;
}
:deep(.el-slider__button) {
  border-color: #00d4ff;
  background: #fff;
}

/* 通知配置 */
.notification-form {
  max-width: 800px;
}
:deep(.el-textarea__inner) {
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.2);
  color: #fff;
  font-family: monospace;
}
:deep(.el-textarea__inner:focus) {
  border-color: #00d4ff;
}

/* 围栏抽屉（面板挂 body 时外层样式见文件底部非 scoped 块） */
.fence-drawer-inner {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  max-height: calc(100vh - 100px);
}
.fence-drawer-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px 8px;
}
.fence-drawer-bar {
  flex-shrink: 0;
  padding: 14px 20px 18px;
  border-top: 1px solid rgba(0, 212, 255, 0.28);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  background: rgba(8, 12, 32, 0.96);
}
.fence-coords-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: rgba(200, 220, 255, 0.55);
  line-height: 1.45;
}
.fence-coords-input :deep(.el-textarea__inner) {
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
.fence-drawer-bar :deep(.el-button--default) {
  --el-button-bg-color: rgba(255, 255, 255, 0.06);
  --el-button-border-color: rgba(0, 212, 255, 0.35);
  --el-button-text-color: #c5e8ff;
}
</style>

<!-- 下拉挂到 body，需非 scoped -->
<style>
.site-config-select-dropdown.el-select__popper.el-popper {
  background: linear-gradient(180deg, #152238 0%, #0f1828 100%) !important;
  border: 1px solid rgba(0, 212, 255, 0.35) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
}
.site-config-select-dropdown .el-select-dropdown__item {
  color: #e2eef8;
}
.site-config-select-dropdown .el-select-dropdown__item.is-hovering,
.site-config-select-dropdown .el-select-dropdown__item:hover {
  background: rgba(0, 212, 255, 0.15) !important;
  color: #ffffff;
}
.site-config-select-dropdown .el-select-dropdown__item.is-selected {
  color: #5ce1ff;
  font-weight: 500;
}
.site-config-select-dropdown .el-popper__arrow::before {
  background: #152238 !important;
  border-color: rgba(0, 212, 255, 0.35) !important;
}

/* 围栏抽屉挂到 body，与 scoped 分离避免样式丢失 */
.fence-drawer-panel.el-drawer {
  background: linear-gradient(135deg, #1a1f3c, #0d1228);
  border-left: 1px solid #0d8bbd;
  color: #e8f4ff;
}
.fence-drawer-panel .el-drawer__header {
  color: #7ee8ff;
  font-size: 18px;
  margin-bottom: 0;
}
.fence-drawer-panel .el-drawer__body {
  padding: 0;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.fence-drawer-panel .el-drawer .el-select .el-select__wrapper {
  background: rgba(0, 0, 0, 0.35);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.18);
}
.fence-drawer-panel .el-drawer .el-select__placeholder,
.fence-drawer-panel .el-drawer .el-select__selected-item {
  color: #e8f4ff;
}
.fence-drawer-panel .el-drawer .el-select__caret {
  color: rgba(200, 225, 255, 0.75);
}
.fence-drawer-panel .el-drawer .el-form-item__label {
  color: rgba(210, 230, 255, 0.92);
}
.fence-drawer-panel .el-drawer .el-textarea__inner {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #e8f4ff;
}
.fence-drawer-panel .el-drawer .el-input__wrapper {
  background: rgba(0, 0, 0, 0.3);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.2);
}
.fence-drawer-panel .el-drawer .el-input__inner {
  color: #e8f4ff;
}
</style>

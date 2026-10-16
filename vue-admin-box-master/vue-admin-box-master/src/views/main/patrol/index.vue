<template>
  <div class="patrol-page">
    <header class="command-bar">
      <div class="brand-block"><span class="eyebrow">LOW-ALTITUDE OPERATIONS / 01</span><h1>低空巡检指挥台</h1><p>任务规划 · 三维仿真 · 实时遥测</p></div>
      <div class="header-status"><span class="mission-code">{{ mission?.mission_code || '未创建任务' }}</span><div class="mission-state"><span :class="['state-dot', stateClass]"></span>{{ stateLabel }}</div></div>
    </header>

    <section class="overview-strip">
      <article class="overview-card"><span class="overview-label">任务状态</span><strong>{{ stateLabel }}</strong><small>{{ mission?.area_name || ACTIVE_CAMPUS.name }}</small></article>
      <article class="overview-card accent-cyan"><span class="overview-label">航线距离</span><strong>{{ routeMetrics.distanceM || 0 }}<em>m</em></strong><small>{{ routeQuality.label }} · {{ waypoints.length }} 个航点</small></article>
      <article class="overview-card accent-amber"><span class="overview-label">预计用时</span><strong>{{ routeMetrics.estimatedSeconds || 0 }}<em>s</em></strong><small>巡航 {{ routeMetrics.cruiseSpeedMps || 4.5 }}m/s</small></article>
      <article class="overview-card accent-green"><span class="overview-label">当前电量</span><strong>{{ mission?.battery_current ?? '--' }}<em>%</em></strong><small>遥测点 {{ telemetry.length }} · 版本 V{{ mission?.plan_version || 0 }}</small></article>
    </section>

    <main class="command-grid">
      <aside class="left-rail">
        <section class="panel task-panel">
          <div class="panel-heading"><div><span class="panel-kicker">MISSION</span><h2>巡检任务</h2></div><span class="panel-index">01</span></div>
          <textarea v-model="form.goal" rows="4" placeholder="描述巡检目标" />
          <div class="field-row"><label>区域<input v-model="form.areaName" /></label><label>时限<input v-model.number="durationMinutes" type="number" min="1" max="60" /></label></div>
          <button class="primary" :disabled="loading" @click="create">创建新任务</button>
        </section>

        <section class="panel route-editor-panel">
          <div class="panel-heading"><div><span class="panel-kicker">ROUTE DESIGN</span><h2>航线规划</h2></div><span class="panel-index">02</span></div>
          <div class="route-mode-row"><span :class="['mode-led', { on: planningMode }]" /><b>{{ planningMode ? '编辑草稿' : '已保存航线' }}</b><button class="text-button" :disabled="!mission || !canEditRoute" @click="togglePlanning">{{ planningMode ? '退出编辑' : '编辑航线' }}</button></div>
          <div v-if="!planningMode" class="route-health"><strong>{{ routeQuality.label }}</strong><span>{{ routeQuality.detail }}</span><button v-if="mission && canEditRoute" class="recommend-button" @click="applyRouteTemplate('building')">生成建议航线</button></div>
          <div v-if="planningMode" class="route-settings">
            <div class="template-row"><span>快速模板</span><button @click="applyRouteTemplate('building')">系统建议</button><button @click="applyRouteTemplate('coverage')">区域蛇形</button></div>
            <div class="settings-grid"><label>默认高度<input v-model.number="defaultAltitude" type="number" min="10" max="120" step="1" /><small>m</small></label><label>悬停时间<input v-model.number="defaultDwell" type="number" min="0" max="60" step="1" /><small>s</small></label></div>
            <label class="check-row"><input v-model="returnHome" type="checkbox" /> 自动返航到任务起点</label>
            <div class="route-actions"><button @click="undoDraft">撤销</button><button @click="clearDraft">清空</button><button class="save-route" :disabled="draftWaypoints.length < 2 || loading" @click="saveCustomRoute">保存航线</button></div>
            <div class="draft-list">
              <div v-for="(point, index) in draftWaypoints" :key="point.localId" class="draft-row"><span class="draft-index">{{ String(index + 1).padStart(2, '0') }}</span><span class="draft-coord">{{ Number(point.lng).toFixed(5) }}, {{ Number(point.lat).toFixed(5) }}</span><input v-model.number="point.altitudeM" type="number" min="10" max="120" aria-label="航点高度" /><button class="remove-point" title="删除航点" @click="removeDraft(index)">×</button></div>
              <div v-if="!draftWaypoints.length" class="draft-empty">切换到地图视图后点击校园道路或建筑外围添加航点</div>
            </div>
          </div>
        </section>

        <section class="panel actions-panel">
          <div class="panel-heading"><div><span class="panel-kicker">CONTROL</span><h2>任务控制</h2></div><span class="panel-index">03</span></div>
          <div class="primary-actions"><button :disabled="!mission || ms!=='DRAFT'" @click="run(planMission)">规划航线</button><button class="primary-action" :disabled="!mission || !(ms==='PLANNED'||ms==='PAUSED')" @click="run(startMission)">启动仿真</button><button :disabled="!mission || ms!=='RUNNING'" @click="run(pauseMission)">暂停任务</button><button :disabled="!mission || (ms!=='PLANNED'&&ms!=='RUNNING'&&ms!=='PAUSED')" @click="replan">动态重规划</button></div>
          <details class="advanced-actions"><summary>仿真测试与报告</summary><div class="action-grid"><button :disabled="!mission || (ms!=='PLANNED'&&ms!=='RUNNING')" @click="injectEvent">注入 AI 事件</button><button :disabled="!mission || ms!=='RUNNING'" @click="injectFault('GPS_OFFSET')">GPS 偏移</button><button :disabled="!mission || ms!=='RUNNING'" @click="injectFault('PACKET_LOSS')">丢包实验</button><button :disabled="!mission || (ms!=='RUNNING'&&ms!=='PAUSED')" @click="clearFault">恢复故障</button><button class="wide" :disabled="!mission || ms!=='COMPLETED'" @click="report">生成巡检报告</button></div></details>
        </section>
      </aside>

      <section class="map-stage panel">
        <div class="stage-head"><div><span>{{ ACTIVE_CAMPUS.name }} / 三维巡检空间</span><small>{{ viewMode === '3d' ? '建筑轮廓 · 真实高度 · PyBullet 遥测' : (coverageRoute ? 'Fields2Cover 区域覆盖预览' : '二维底图 / 航点编辑') }}</small></div><div class="view-switch"><button :class="{active:viewMode==='3d'}" @click="viewMode='3d'">3D 空间</button><button :class="{active:viewMode==='map'}" @click="viewMode='map'">地图编辑</button></div></div>
        <CesiumPatrolViewer v-if="viewMode === '3d'" :waypoints="activeWaypoints" :telemetry="telemetry" :fences="fences" class="cesium-stage" />
        <div v-else ref="mapCanvas" class="map-canvas">
          <div ref="mapHost" class="baidu-map-host" :class="{ ready: mapReady }"></div><div v-if="mapError" class="map-fallback-hint">地图服务暂不可用，仍可使用航点列表编辑</div>
          <div class="grid-layer"><div v-for="cell in sortedGrid" :key="cell.grid_id" class="risk-cell" :class="riskClass(cell.risk_score)" :title="`风险 ${Number(cell.risk_score).toFixed(2)}`"></div></div>
          <svg class="route-layer" viewBox="0 0 100 100" preserveAspectRatio="none"><polyline v-if="routePoints" :points="routePoints" fill="none" stroke="#39d7ff" stroke-width="0.8" stroke-dasharray="2 1" /></svg>
          <template v-if="!coverageRoute"><div v-for="(point, index) in waypointMarkers" :key="point.id" class="waypoint" :style="{left:point.x+'%',top:point.y+'%'}"><span>{{ index + 1 }}</span></div></template><div class="map-label north">N</div><div class="coverage-legend"><i></i>规划航线 · 点击地图添加航点</div>
        </div>
        <div class="stage-foot"><span>建筑 {{ buildingCountLabel }}</span><span>风险网格 {{ riskGrid.length }}</span><span>遥测点 {{ telemetry.length }}</span><span>AI事件 {{ events.length }}</span><span>数据源 {{ viewMode === '3d' ? 'Cesium + PyBullet' : 'BMapGL' }}</span></div>
      </section>

      <aside class="right-rail panel">
        <div class="panel-heading"><div><span class="panel-kicker">AUTONOMOUS LOOP</span><h2>执行监控</h2></div><span class="panel-index">04</span></div>
        <div class="execution-summary"><div class="phase-track"><span v-for="phase in phases" :key="phase" :class="{active: activePhases.has(phase)}">{{ phase }}</span></div><div class="latest-step"><i></i><div><strong>{{ latestStep?.title || '等待任务开始' }}</strong><p>{{ latestStep?.detail || '创建任务后，这里会显示当前执行阶段。' }}</p></div><em>{{ latestStep?.state || 'IDLE' }}</em></div></div>
        <details class="monitor-section" open><summary>执行日志 <em>{{ steps.length }}</em></summary><div class="timeline"><article v-for="step in steps" :key="step.step_id"><i></i><div><header><b>{{ step.title }}</b><span>{{ step.state }}</span></header><p>{{ step.detail }}</p><small>{{ step.phase }}</small></div></article><div v-if="!steps.length" class="empty">暂无执行记录</div></div></details>
        <details class="monitor-section"><summary>工具调用 <em>{{ tools.length }}</em></summary><div class="tool-list"><div v-for="tool in tools" :key="tool.execution_id"><b>{{ tool.tool_name }}</b><span>{{ tool.state }}</span><p>{{ tool.output_summary }}</p></div></div></details>
        <details v-if="goalPlan" class="monitor-section"><summary>任务规则 <em>{{ goalPlan.provider }}</em></summary><div class="plan-summary"><p><b>{{ goalPlan.model_name || 'patrol-rule-v1' }}</b></p><p>目标：{{ listText(goalPlan.plan.targetTypes) }}</p><p>传感器：{{ listText(goalPlan.plan.sensorRequirements) }}</p><p>触发：{{ listText(goalPlan.plan.replanTriggers) }}</p></div></details>
        <details v-if="faults.length" class="monitor-section"><summary>故障实验 <em>{{ faults.length }}</em></summary><div class="plan-summary fault-summary"><p v-for="fault in faults" :key="`${fault.fault_type}-${fault.injected_at}`">{{ fault.fault_type }} · {{ fault.state }}</p></div></details>
      </aside>
    </main>

    <el-dialog v-model="reportVisible" title="巡检报告" width="620px"><pre class="report-json">{{ JSON.stringify(reportData, null, 2) }}</pre></el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { attachPatrolEvent, clearPatrolFault, createMission, customPlanMission, generateMissionReport, getMission, injectPatrolFault, listMissions, pauseMission, planMission, replanMission, startMission } from '@/api/patrol'
import { loadBaiduMapGL } from '@/utils/map/baiduMap'
import { ACTIVE_CAMPUS, normalizeCampusPoint } from '@/config/campusMap'
import CesiumPatrolViewer from '@/components/patrol/CesiumPatrolViewer.vue'

const loading = ref(false)
const mission = ref<any>(null)
const durationMinutes = ref(12)
const form = reactive({ goal: '巡查校园晚高峰电动车安全情况，重点关注未戴头盔、禁行区和异常聚集。', areaName: ACTIVE_CAMPUS.name, centerLng: ACTIVE_CAMPUS.lng, centerLat: ACTIVE_CAMPUS.lat })
const reportVisible = ref(false)
const reportData = ref<any>({})
const mapCanvas = ref<HTMLElement | null>(null)
const mapHost = ref<HTMLElement | null>(null)
const mapReady = ref(false)
const mapError = ref(false)
const mapInstance = ref<any>(null)
const mapApi = ref<any>(null)
const viewMode = ref<'3d' | 'map'>('3d')
const planningMode = ref(false)
const defaultAltitude = ref(30)
const defaultDwell = ref(3)
const returnHome = ref(true)
const draftWaypoints = ref<any[]>([])
const riskGrid = computed(() => mission.value?.riskGrid || [])
const waypoints = computed(() => mission.value?.waypoints || [])
const activeWaypoints = computed(() => planningMode.value ? draftWaypoints.value : waypoints.value)
const canEditRoute = computed(() => ['DRAFT', 'PLANNED'].includes(ms.value))
const routeMetrics = computed(() => mission.value?.routeMetrics || {})
const stateLabel = computed(() => ({ DRAFT: '待规划', PLANNED: '已规划', RUNNING: '仿真中', PAUSED: '已暂停', RETURNING: '返航中', COMPLETED: '已完成', FAILED: '异常' } as Record<string, string>)[ms.value] || '未创建任务')
const routeQuality = computed(() => {
  const count = activeWaypoints.value.length
  if (!count) return { label: '未规划', detail: '需要生成或编辑航线' }
  if (count < 6) return { label: '航点偏少', detail: '建议使用建筑外围或区域蛇形模板' }
  return { label: '路线完整', detail: '包含连续巡检段与返航约束' }
})
const latestStep = computed(() => steps.value.length ? steps.value[steps.value.length - 1] : null)
const buildingCountLabel = 'OSM 建筑'
const telemetry = computed(() => mission.value?.telemetry || [])
const telemetryPath = computed(() => [...telemetry.value].reverse())
const fences = computed(() => mission.value?.fences || [])
const events = computed(() => mission.value?.events || [])
const steps = computed(() => mission.value?.steps || [])
const tools = computed(() => mission.value?.tools || [])
const faults = computed(() => mission.value?.faults || [])
const goalPlan = computed(() => mission.value?.goalPlan ? {
  ...mission.value.goalPlan,
  plan: parseJson(mission.value.goalPlan.plan_json)
} : null)
const phases = ['UNDERSTAND','OBSERVE','PLAN','EXECUTE','MONITOR','REPLAN','DELIVER']
const activePhases = computed(() => new Set(steps.value.map((s:any) => s.phase)))
const stateClass = computed(() => String(mission.value?.state || '').toLowerCase())
// 当前任务状态（按钮按状态启用，避免对 COMPLETED 暂停、对 DRAFT 启动等 400 报错）
const ms = computed(() => String(mission.value?.state || ''))
const sortedGrid = computed(() => [...riskGrid.value].sort((a:any,b:any) => a.row_no-b.row_no || a.col_no-b.col_no))
const bounds = computed(() => {
  const pts = activeWaypoints.value.map(parsePoint).filter(Boolean) as Array<{ lng: number; lat: number }>
  if (!pts.length) return null
  const lngs = pts.map((p) => p.lng)
  const lats = pts.map((p) => p.lat)
  const lngSpan = Math.max(Math.max(...lngs) - Math.min(...lngs), 0.00025)
  const latSpan = Math.max(Math.max(...lats) - Math.min(...lats), 0.0002)
  const lngPad = Math.max(lngSpan * 0.18, 0.00008)
  const latPad = Math.max(latSpan * 0.18, 0.00006)
  return {
    minLng: Math.min(...lngs) - lngPad,
    maxLng: Math.max(...lngs) + lngPad,
    minLat: Math.min(...lats) - latPad,
    maxLat: Math.max(...lats) + latPad
  }
})
const waypointMarkers = computed(() => {
  const b = bounds.value
  if (!b) return []
  return activeWaypoints.value.map((raw: any) => {
    const point = parsePoint(raw)
    if (!point) return { id: raw?.waypoint_id, x: 50, y: 50 }
    return {
      id: raw.waypoint_id,
      x: 8 + (point.lng - b.minLng) / (b.maxLng - b.minLng) * 84,
      y: 92 - (point.lat - b.minLat) / (b.maxLat - b.minLat) * 84
    }
  })
})
const routePoints = computed(() => waypointMarkers.value.map((p:any)=>`${p.x},${p.y}`).join(' '))
// 覆盖式航线航点密集（牛耕式扫掠），改用纯折线呈现，避免编号标记遮挡
const coverageRoute = computed(() => waypoints.value.length > 16)
const riskClass = (score:any) => Number(score)>=1.1?'critical':Number(score)>=.95?'high':Number(score)>=.7?'medium':'low'
let telemetryTimer: number | null = null
let mapRetryTimer: number | null = null
let mapRetryCount = 0

function localPoint(row:any) {
  const point = parsePoint(row)
  return point ? { ...point, altitudeM: Number(row?.altitude_m ?? row?.altitudeM ?? defaultAltitude.value), dwellSec: Number(row?.dwell_sec ?? row?.dwellSec ?? defaultDwell.value), localId: `${Date.now()}-${Math.random()}` } : null
}

function togglePlanning() {
  if (!mission.value || !canEditRoute.value) return
  if (!planningMode.value) {
    draftWaypoints.value = waypoints.value.map(localPoint).filter(Boolean)
    planningMode.value = true
    viewMode.value = 'map'
    ElMessage.info('点击地图添加航点，航点高度可在左侧列表调整')
  } else planningMode.value = false
}

async function applyRouteTemplate(kind: 'building' | 'coverage') {
  if (!mission.value || !canEditRoute.value) return
  loading.value = true
  try {
    const planned = await planMission(mission.value.mission_id)
    mission.value = planned
    draftWaypoints.value = (planned.waypoints || []).map(localPoint).filter(Boolean)
  } catch (error:any) {
    ElMessage.error(error.message || '系统规划器暂时无法生成可行航线')
    return
  } finally {
    loading.value = false
  }
  planningMode.value = true
  viewMode.value = '3d'
  ElMessage.success(kind === 'building' ? '已生成经过禁飞区校验的系统建议航线，可继续编辑' : '已生成经过约束校验的区域覆盖航线，可继续编辑')
}

function undoDraft() { draftWaypoints.value.pop() }
function clearDraft() { draftWaypoints.value = [] }
function removeDraft(index:number) { draftWaypoints.value.splice(index, 1) }
async function saveCustomRoute() {
  if (!mission.value || draftWaypoints.value.length < 2) return
  loading.value = true
  try {
    mission.value = await customPlanMission(mission.value.mission_id, {
      returnHome: returnHome.value,
      waypoints: draftWaypoints.value.map((point:any) => ({ lng: Number(point.lng), lat: Number(point.lat), altitudeM: Number(point.altitudeM), dwellSec: Number(point.dwellSec ?? defaultDwell.value) }))
    })
    planningMode.value = false
    viewMode.value = '3d'
    ElMessage.success('自定义航线已保存，3D 视图将使用该版本执行')
    syncTelemetryPolling()
  } catch (e:any) { ElMessage.error(e.message || '自定义航线保存失败') } finally { loading.value = false }
}

function onBaiduMapClick(event:any) {
  if (!planningMode.value) return
  const point = event?.latLng || event?.point
  const lng = Number(point?.lng)
  const lat = Number(point?.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return
  draftWaypoints.value.push({ lng, lat, altitudeM: defaultAltitude.value, dwellSec: defaultDwell.value, localId: `${Date.now()}-${draftWaypoints.value.length}` })
}

function parsePoint(row:any) {
  const lng = Number(row?.lng ?? row?.longitude ?? row?.center_lng)
  const lat = Number(row?.lat ?? row?.latitude ?? row?.center_lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  return normalizeCampusPoint({ lng, lat })
}

function clearMapOverlays() {
  if (mapInstance.value) mapInstance.value.clearOverlays()
}

function parseFenceCoordinates(value:any) {
  if (Array.isArray(value)) return value.map(parsePoint).filter(Boolean)
  if (typeof value !== 'string' || !value.trim()) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map(parsePoint).filter(Boolean) : []
  } catch {
    return []
  }
}

function drawBaiduOverlays() {
  const map = mapInstance.value
  const B = mapApi.value
  if (!map || !B) return
  clearMapOverlays()
  const waypointsRaw = activeWaypoints.value.map(parsePoint).filter(Boolean)
  const routePointsRaw = waypointsRaw.map((p:any) => new B.Point(p.lng, p.lat))
  if (routePointsRaw.length > 1) {
    map.addOverlay(new B.Polyline(routePointsRaw, { strokeColor: '#22d3ee', strokeWeight: 5, strokeOpacity: 0.9 }))
  }
  const tracePoints = telemetryPath.value.map(parsePoint).filter(Boolean).map((p:any) => new B.Point(p.lng, p.lat))
  if (tracePoints.length > 1) {
    map.addOverlay(new B.Polyline(tracePoints, { strokeColor: '#35e58a', strokeWeight: 4, strokeOpacity: 0.9 }))
  }
  telemetryPath.value.length && (() => {
    const latest = parsePoint(telemetryPath.value[telemetryPath.value.length - 1])
    if (!latest) return
    const marker = new B.Marker(new B.Point(latest.lng, latest.lat))
    marker.setTitle(`仿真无人机 · 电量 ${Number(telemetryPath.value[telemetryPath.value.length - 1].battery ?? 0).toFixed(1)}%`)
    map.addOverlay(marker)
  })()
  fences.value.forEach((fence:any) => {
    const points = parseFenceCoordinates(fence.coordinates).map((p:any) => new B.Point(p.lng, p.lat))
    if (points.length >= 3) {
      const isRestricted = Number(fence.fence_type ?? fence.fenceType) <= 2
      const color = isRestricted ? '#ef4444' : '#f59e0b'
      map.addOverlay(new B.Polygon(points, { strokeColor: color, strokeWeight: 2, strokeOpacity: 0.8, fillColor: color, fillOpacity: 0.12 }))
    }
  })
  events.value.forEach((event:any) => {
    const point = parsePoint(event)
    if (!point) return
    const marker = new B.Marker(new B.Point(point.lng, point.lat))
    marker.setTitle(`${event.event_type || 'AI事件'} · ${event.risk_level || '待研判'}`)
    map.addOverlay(marker)
  })
  waypointsRaw.forEach((p:any, index:number) => {
    const marker = new B.Marker(new B.Point(p.lng, p.lat))
    marker.setTitle(`航点 ${index + 1}`)
    map.addOverlay(marker)
  })
  const gridPoints = riskGrid.value.map(parsePoint).filter(Boolean)
  gridPoints.forEach((p:any, index:number) => {
    const cell = riskGrid.value[index]
    const score = Number(cell?.risk_score || 0)
    const delta = 0.00009
    const corners = [
      new B.Point(p.lng - delta, p.lat - delta), new B.Point(p.lng + delta, p.lat - delta),
      new B.Point(p.lng + delta, p.lat + delta), new B.Point(p.lng - delta, p.lat + delta)
    ]
    const color = score >= 1.1 ? '#ef4444' : score >= 0.95 ? '#f97316' : score >= 0.7 ? '#f59e0b' : '#38bdf8'
    map.addOverlay(new B.Polygon(corners, { strokeColor: color, strokeWeight: 1, strokeOpacity: 0.65, fillColor: color, fillOpacity: 0.2 }))
  })
  const center = parsePoint({ lng: mission.value?.center_lng ?? form.centerLng, lat: mission.value?.center_lat ?? form.centerLat })
  if (center) map.centerAndZoom(new B.Point(center.lng, center.lat), 18)
}

async function initBaiduMap() {
  try {
    const B = await loadBaiduMapGL()
    await nextTick()
    if (!mapHost.value) throw new Error('地图容器未就绪')
    mapApi.value = B
    mapInstance.value = new B.Map(mapHost.value, { enableAutoResize: true })
    mapInstance.value.enableScrollWheelZoom(true)
    mapInstance.value.setMinZoom?.(15)
    mapInstance.value.setMaxZoom?.(20)
    mapReady.value = true
    mapError.value = false
    mapRetryCount = 0
    mapInstance.value.addEventListener?.('click', onBaiduMapClick)
    drawBaiduOverlays()
    window.setTimeout(() => mapInstance.value?.checkResize?.(), 200)
  } catch (error) {
    mapError.value = true
    mapReady.value = false
    if (mapRetryCount < 1) {
      mapRetryCount += 1
      mapRetryTimer = window.setTimeout(() => {
        mapRetryTimer = null
        initBaiduMap()
      }, 1800)
    }
  }
}

async function create(){ loading.value=true; try{ mission.value=await createMission({...form,durationLimitSec:durationMinutes.value*60}); draftWaypoints.value=[]; planningMode.value=false; ElMessage.success('巡检任务已创建'); drawBaiduOverlays() }catch(e:any){ElMessage.error(e.message||'创建失败')}finally{loading.value=false} }
async function refreshMission(){
  if (!mission.value?.mission_id) return
  try {
    mission.value = await getMission(mission.value.mission_id)
    await nextTick()
    drawBaiduOverlays()
  } catch {
    // 轮询失败时保留最近一次任务状态，下一次继续尝试
  }
}
function syncTelemetryPolling(){
  const running = String(mission.value?.state || '').toUpperCase() === 'RUNNING'
  if (running && telemetryTimer == null) telemetryTimer = window.setInterval(refreshMission, 1000)
  if (!running && telemetryTimer != null) { window.clearInterval(telemetryTimer); telemetryTimer = null }
}
async function run(fn:any){ if(!mission.value)return; loading.value=true; try{mission.value=await fn(mission.value.mission_id);ElMessage.success('操作完成'); await nextTick(); drawBaiduOverlays(); syncTelemetryPolling()}catch(e:any){ElMessage.error(e.message||'操作失败')}finally{loading.value=false} }
async function replan(){ if(!mission.value)return; mission.value=await replanMission(mission.value.mission_id,'发现高风险事件，插入补拍航点'); await nextTick(); drawBaiduOverlays(); ElMessage.success('已生成新航线版本') }
async function injectEvent(){
  if(!mission.value)return
  loading.value=true
  try{
    mission.value=await attachPatrolEvent(mission.value.mission_id,{eventType:'NO_HELMET',sourceType:'DRONE',triggerReplan:true,autoAnalyze:true})
    await nextTick(); drawBaiduOverlays(); syncTelemetryPolling()
    ElMessage.success('AI事件已关联并触发航线复核')
  }catch(e:any){ElMessage.error(e.message||'AI事件接入失败')}finally{loading.value=false}
}
async function injectFault(type:string){
  if(!mission.value)return
  loading.value=true
  try{
    const parameters = type === 'GPS_OFFSET' ? { meters: 12 } : { rate: 0.35 }
    await injectPatrolFault(mission.value.mission_id, type, parameters)
    await refreshMission()
    ElMessage.success(type === 'GPS_OFFSET' ? '已注入GPS偏移，观察轨迹与质量变化' : '已注入遥测丢包，观察恢复能力')
  }catch(e:any){ElMessage.error(e.message||'故障注入失败')}finally{loading.value=false}
}
async function clearFault(){
  if(!mission.value)return
  loading.value=true
  try{ await clearPatrolFault(mission.value.mission_id); await refreshMission(); ElMessage.success('故障已恢复') }
  catch(e:any){ElMessage.error(e.message||'故障恢复失败')}finally{loading.value=false}
}
function parseJson(value:any){ try { return JSON.parse(value || '{}') } catch { return {} } }
function listText(value:any){ return Array.isArray(value) ? value.join('、') : (value || '—') }
async function report(){ if(!mission.value)return; reportData.value=await generateMissionReport(mission.value.mission_id); reportVisible.value=true }
watch([riskGrid, waypoints, telemetry, fences, events, draftWaypoints, planningMode], () => drawBaiduOverlays(), { deep: true })
watch(viewMode, async (mode) => {
  if (mode === 'map') {
    await nextTick()
    if (!mapInstance.value) initBaiduMap()
    else mapInstance.value.checkResize?.()
  }
})
onMounted(async()=>{ try{const list=await listMissions();if(list.length){mission.value=await getMission(list[0].mission_id);syncTelemetryPolling()}}catch{/* 后端未启动时允许页面打开 */} })
onUnmounted(() => {
  if (telemetryTimer != null) window.clearInterval(telemetryTimer)
  telemetryTimer = null
  if (mapRetryTimer != null) window.clearTimeout(mapRetryTimer)
  mapRetryTimer = null
  mapInstance.value?.removeEventListener?.('click', onBaiduMapClick)
  mapInstance.value?.clearOverlays?.()
  mapInstance.value = null
})
</script>

<style scoped>
.patrol-page{min-height:calc(100vh - 110px);padding:16px;background:#07111c;color:#d9e8f5;font-family:"Microsoft YaHei",sans-serif}.command-bar{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;padding:0 4px}.eyebrow{color:#38d7c4;font-size:11px}.command-bar h1{margin:3px 0 0;font-size:24px;letter-spacing:0}.mission-state{display:flex;align-items:center;gap:8px;color:#8fa9bc}.state-dot{width:9px;height:9px;border-radius:50%;background:#738091}.state-dot.running{background:#36d399;box-shadow:0 0 10px #36d399}.state-dot.planned{background:#39d7ff}.state-dot.paused{background:#ffb447}.command-grid{display:grid;grid-template-columns:270px minmax(440px,1fr) 330px;gap:12px;height:calc(100vh - 175px)}.panel{border:1px solid #17364b;border-radius:6px;background:#0b1a28;box-shadow:0 12px 30px rgba(0,0,0,.2)}.left-rail{display:flex;min-height:0;flex-direction:column;gap:12px}.left-rail .panel{padding:14px}.panel-title{margin-bottom:11px;color:#7fa6bf;font-size:12px;text-transform:uppercase}.task-panel textarea,.task-panel input{box-sizing:border-box;width:100%;border:1px solid #21465d;border-radius:4px;outline:0;color:#e4f0f7;background:#07141f}.task-panel textarea{padding:10px;resize:none;line-height:1.55}.field-row{display:grid;grid-template-columns:1fr 76px;gap:8px;margin:9px 0}.field-row label{color:#708ea2;font-size:11px}.field-row input{display:block;margin-top:4px;padding:7px}.primary,.actions-panel button{border:1px solid #28566e;border-radius:4px;padding:9px;color:#bcd8e8;background:#10283a;cursor:pointer}.primary{width:100%;border-color:#1ab8ac;color:#fff;background:#148f88}.action-grid{display:grid;grid-template-columns:1fr 1fr;gap:7px}.action-grid .wide{grid-column:1/-1}.actions-panel button:disabled,.primary:disabled{opacity:.45;cursor:not-allowed}.telemetry-panel{margin-top:auto}.metrics{display:grid;grid-template-columns:1fr 1fr;gap:9px}.metrics div{padding:9px;border-left:2px solid #1ab8ac;background:#0e2231}.metrics strong,.metrics span{display:block}.metrics strong{font-size:18px}.metrics span{margin-top:3px;color:#64859a;font-size:10px}.map-stage{display:flex;min-height:0;flex-direction:column;padding:13px}.stage-head,.stage-foot{display:flex;align-items:center;justify-content:space-between;color:#82a7bd;font-size:12px}.stage-head small{display:block;margin-top:3px;color:#4f7287}.map-canvas{position:relative;flex:1;margin:12px 0;overflow:hidden;border:1px solid #1c4259;background-color:#071722;background-image:linear-gradient(rgba(39,85,107,.2) 1px,transparent 1px),linear-gradient(90deg,rgba(39,85,107,.2) 1px,transparent 1px);background-size:28px 28px}.baidu-map-host{position:absolute;inset:0;z-index:1}.map-fallback-hint{position:absolute;right:12px;top:12px;z-index:4;padding:6px 9px;border:1px solid rgba(255,180,71,.5);background:rgba(15,32,45,.88);color:#ffd28a;font-size:11px}.grid-layer,.route-layer,.waypoint,.coverage-legend,.north{z-index:2}.grid-layer{position:absolute;inset:12%;display:grid;grid-template-columns:repeat(5,1fr);grid-template-rows:repeat(5,1fr);gap:3px;transform:rotate(-5deg)}.risk-cell{border:1px solid rgba(93,153,177,.2);background:rgba(67,135,157,.12)}.risk-cell.medium{background:rgba(255,187,73,.18)}.risk-cell.high{background:rgba(255,122,61,.3)}.risk-cell.critical{background:rgba(255,60,82,.45);box-shadow:inset 0 0 12px rgba(255,60,82,.5)}.route-layer{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}.waypoint{position:absolute;width:22px;height:22px;transform:translate(-50%,-50%);border:1px solid #39d7ff;border-radius:50%;background:#08283a;box-shadow:0 0 10px rgba(57,215,255,.35);text-align:center;line-height:20px}.waypoint span{color:#b9f3ff;font-size:10px}.north{position:absolute;right:15px;top:14px;color:#39d7ff;font-weight:700}.coverage-legend{position:absolute;left:14px;bottom:14px;color:#6f91a5;font-size:11px}.coverage-legend i{display:inline-block;width:8px;height:8px;margin-right:5px;background:#ff7a3d}.stage-foot{gap:18px;justify-content:flex-start}.right-rail{display:flex;min-height:0;flex-direction:column;padding:14px}.agent-loop{display:flex;gap:4px;flex-wrap:wrap}.agent-loop span{padding:4px 6px;border:1px solid #1b3a4d;border-radius:3px;color:#4f7186;font-size:9px}.agent-loop span.active{border-color:#1ab8ac;color:#75e4d9;background:rgba(26,184,172,.1)}.timeline{margin-top:12px;overflow:auto}.timeline article{display:grid;grid-template-columns:11px 1fr;gap:8px;padding-bottom:13px}.timeline article>i{width:7px;height:7px;margin-top:5px;border-radius:50%;background:#39d7ff}.timeline header{display:flex;justify-content:space-between}.timeline b{font-size:12px}.timeline header span{color:#5bcdbf;font-size:9px}.timeline p{margin:4px 0;color:#7898ac;font-size:11px;line-height:1.45}.timeline small{color:#3f647a}.empty{padding:28px 0;color:#4f7186;text-align:center}.tools-title{margin-top:8px}.tool-list{overflow:auto}.tool-list div{margin-bottom:7px;padding:8px;border-left:2px solid #ffb447;background:#0e2231}.tool-list b{font-size:10px}.tool-list span{float:right;color:#69d5c8;font-size:9px}.tool-list p{margin:4px 0 0;color:#67879b;font-size:9px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.report-json{max-height:420px;overflow:auto;color:#24445a;background:#f3f7f9;padding:12px}@media(max-width:1200px){.command-grid{grid-template-columns:240px 1fr}.right-rail{display:none}}@media(max-width:800px){.command-grid{display:block;height:auto}.left-rail{margin-bottom:12px}.map-stage{height:520px}}
.route-editor-panel button{border:1px solid #28566e;border-radius:4px;padding:5px 7px;color:#bcd8e8;background:#10283a;cursor:pointer;font-size:10px}.route-editor-panel button:disabled{opacity:.45;cursor:not-allowed}.route-mode-row{display:flex;align-items:center;gap:7px;color:#b6d3df;font-size:11px}.mode-led{width:7px;height:7px;border-radius:50%;background:#64798a}.mode-led.on{background:#f7c873;box-shadow:0 0 8px #f7c873}.text-button{margin-left:auto}.route-settings{margin-top:10px}.route-settings label{display:flex;align-items:center;gap:5px;margin:7px 0;color:#7695a8;font-size:10px}.route-settings input[type=number]{width:48px;padding:4px;border:1px solid #21465d;border-radius:3px;color:#e4f0f7;background:#07141f}.route-settings small{color:#547286}.check-row input{accent-color:#1ab8ac}.route-actions{display:grid;grid-template-columns:1fr 1fr 1.2fr;gap:5px;margin:9px 0}.route-actions .save-route{border-color:#1ab8ac;color:#fff;background:#148f88}.draft-list{max-height:138px;overflow:auto;border-top:1px solid #17364b}.draft-row{display:grid;grid-template-columns:20px 1fr 47px 20px;align-items:center;gap:4px;padding:5px 0;border-bottom:1px solid rgba(31,72,93,.55)}.draft-index{color:#f7c873;font-size:10px}.draft-coord{overflow:hidden;color:#7ea4b7;font-size:9px;text-overflow:ellipsis;white-space:nowrap}.draft-row input{width:43px;box-sizing:border-box;padding:3px;border:1px solid #21465d;border-radius:3px;color:#dff8ff;background:#07141f;font-size:10px}.remove-point{padding:0!important;border:0!important;color:#ff8d81!important;background:transparent!important;font-size:16px!important}.draft-empty{padding:10px 2px;color:#52758a;font-size:10px;line-height:1.45}.route-summary{display:flex;gap:10px;margin-top:10px;color:#87aabb;font-size:10px}.view-switch{display:flex;align-items:center;gap:5px}.view-switch button{border:1px solid #28566e;border-radius:3px;padding:4px 8px;color:#7ea4b7;background:#10283a;font-size:10px;cursor:pointer}.view-switch button.active{border-color:#1ab8ac;color:#d8fffb;background:#11675f}.cesium-stage{flex:1;margin:12px 0;min-height:0}.grid-layer{pointer-events:none}

/* Refined command-center layout: hierarchy first, diagnostics on demand. */
.patrol-page{min-height:calc(100vh - 110px);padding:18px 20px 22px;background:#061019;color:#d9e8f5}.command-bar{margin-bottom:14px;padding:0 2px}.brand-block h1{margin:4px 0 0;font-size:27px;line-height:1.15;letter-spacing:.01em}.brand-block p{margin:6px 0 0;color:#7594a8;font-size:11px}.header-status{display:flex;align-items:center;gap:18px}.mission-code{color:#7594a8;font-size:11px;letter-spacing:.08em}.overview-strip{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:12px}.overview-card{min-height:70px;padding:12px 14px;border:1px solid #17364b;border-radius:7px;background:#0a1a27;box-shadow:0 10px 26px rgba(0,0,0,.16)}.overview-card strong{display:block;margin:4px 0 3px;color:#f0f9fc;font-size:22px;font-weight:650}.overview-card strong em{margin-left:3px;color:#7da4b6;font-size:11px;font-style:normal;font-weight:500}.overview-card small{color:#7293a7;font-size:10px}.overview-label{color:#7899ac;font-size:10px}.overview-card.accent-cyan{border-top:2px solid #28d8ef}.overview-card.accent-amber{border-top:2px solid #f7c873}.overview-card.accent-green{border-top:2px solid #28d8a0}.command-grid{grid-template-columns:300px minmax(520px,1fr) 315px;gap:12px;height:calc(100vh - 300px);min-height:620px}.left-rail{gap:10px;overflow:auto;padding-right:2px}.panel{border:1px solid #17364b;border-radius:7px;background:#0a1926;box-shadow:0 10px 26px rgba(0,0,0,.18)}.left-rail .panel,.right-rail{padding:14px}.panel-heading{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:12px}.panel-heading h2{margin:3px 0 0;color:#d7edf4;font-size:14px;font-weight:600}.panel-kicker{color:#4ea8b6;font-size:9px;letter-spacing:.14em}.panel-index{color:#365e71;font-size:10px}.task-panel textarea{min-height:88px;margin-bottom:8px}.task-panel .primary{height:37px}.route-health{display:grid;grid-template-columns:auto 1fr;gap:3px 8px;align-items:center;margin-top:10px;padding:9px;border:1px solid #173b4d;background:#0d2432}.route-health strong{color:#f7c873;font-size:11px}.route-health span{color:#7595a7;font-size:10px}.recommend-button{grid-column:1/-1!important;margin-top:4px;padding:6px!important;border-color:#1ab8ac!important;color:#c5fffa!important;background:#0b514c!important}.template-row{display:flex;align-items:center;gap:5px;margin:8px 0;color:#7595a7;font-size:10px}.template-row span{margin-right:auto}.template-row button{padding:5px 6px!important}.settings-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}.settings-grid label{margin:4px 0!important}.primary-actions{display:grid;grid-template-columns:1fr 1fr;gap:6px}.primary-actions button{min-height:34px;border:1px solid #28566e;border-radius:4px;color:#bcd8e8;background:#10283a;cursor:pointer}.primary-actions .primary-action{border-color:#1ab8ac;color:#fff;background:#127f79}.primary-actions button:disabled{opacity:.4;cursor:not-allowed}.advanced-actions{margin-top:9px;border-top:1px solid #17364b}.advanced-actions summary,.monitor-section summary{padding:10px 0;color:#86a8b9;font-size:11px;cursor:pointer;list-style:none}.advanced-actions summary::-webkit-details-marker,.monitor-section summary::-webkit-details-marker{display:none}.advanced-actions summary:before,.monitor-section summary:before{content:'+';display:inline-block;width:14px;color:#31d4c1}.advanced-actions[open] summary:before,.monitor-section[open] summary:before{content:'−'}.action-grid{gap:6px}.action-grid button{min-height:31px;padding:6px 7px;font-size:10px}.telemetry-panel{display:none}.map-stage{padding:13px;min-width:0}.stage-head{min-height:35px}.stage-head>div:first-child>span{color:#d4eaf2;font-size:13px}.stage-head small{font-size:10px}.view-switch{gap:6px}.view-switch button{padding:6px 9px}.cesium-stage,.map-canvas{margin:10px 0;min-height:0}.stage-foot{gap:14px;flex-wrap:wrap;padding-top:2px;color:#68899d;font-size:10px}.right-rail{display:flex;min-height:0;overflow:auto}.execution-summary{padding:10px 0 12px;border-bottom:1px solid #17364b}.phase-track{display:flex;flex-wrap:wrap;gap:4px}.phase-track span{padding:4px 6px;border:1px solid #1b3a4d;border-radius:3px;color:#4f7186;font-size:9px}.phase-track span.active{border-color:#1ab8ac;color:#75e4d9;background:rgba(26,184,172,.1)}.latest-step{display:grid;grid-template-columns:10px 1fr auto;gap:8px;align-items:start;margin-top:14px}.latest-step>i{width:7px;height:7px;margin-top:5px;border-radius:50%;background:#28d8ef;box-shadow:0 0 9px rgba(40,216,239,.6)}.latest-step strong{color:#d9edf3;font-size:12px}.latest-step p{margin:4px 0 0;color:#7595a8;font-size:10px;line-height:1.45}.latest-step em{color:#5dd4c7;font-size:9px;font-style:normal}.monitor-section{border-bottom:1px solid #17364b}.monitor-section summary em{float:right;color:#4f9eaa;font-size:9px;font-style:normal}.monitor-section .timeline{max-height:220px;margin-top:0}.monitor-section .tool-list{max-height:180px}.plan-summary{padding:0 0 8px;color:#7898a9;font-size:10px;line-height:1.5}.plan-summary p{margin:6px 0}.tools-title{margin-top:0}.report-json{max-height:420px;overflow:auto}.empty{padding:18px 0}.cesium-credit{font-size:9px}@media(max-width:1350px){.command-grid{grid-template-columns:275px minmax(420px,1fr) 285px}.overview-card strong{font-size:19px}}@media(max-width:1100px){.command-grid{grid-template-columns:250px 1fr}.right-rail{display:none}}@media(max-width:800px){.patrol-page{padding:12px}.overview-strip{grid-template-columns:1fr 1fr}.command-grid{display:block;height:auto;min-height:0}.left-rail{overflow:visible;margin-bottom:12px}.map-stage{height:580px}.map-stage{margin-bottom:12px}}
</style>

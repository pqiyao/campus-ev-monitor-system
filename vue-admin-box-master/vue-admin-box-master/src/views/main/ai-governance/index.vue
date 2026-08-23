<template>
  <div class="gov-page page-bg-shell">
    <div class="gov-shell">
      <header class="page-intro">
        <div class="intro-copy">
          <h1>全链路治理</h1>
          <p>接入事件后先研判再人工确认，确认后才进入正式违规与通知流程。</p>
        </div>
        <div class="intro-actions">
            <span class="quiet-tag">多模态视觉引擎 · {{ modeLabel }}</span>
          <el-button text :icon="Refresh" @click="refreshAll">刷新</el-button>
        </div>
      </header>

      <div class="stat-flow">
        <div v-for="metric in metricCards" :key="metric.label" class="stat-chip" :class="metric.tone">
          <em>{{ metric.value }}</em>
          <span>{{ metric.label }}</span>
        </div>
      </div>

      <section class="patrol-band surface" v-loading="patrolLoading">
        <div class="patrol-band__title">
          <strong>无人机巡检任务</strong>
          <span>任务结果统一回到本页 AI 事件和原有治理流程</span>
        </div>
        <div v-if="activePatrol" class="patrol-band__mission">
          <div>
            <b>{{ activePatrol.mission_code || `任务 #${activePatrol.mission_id}` }}</b>
            <span>{{ activePatrol.area_name || '未指定区域' }} · {{ patrolStateLabel(activePatrol.state) }}</span>
          </div>
          <div class="patrol-band__facts">
            <span>电量 {{ activePatrol.battery_current ?? '--' }}%</span>
            <span>航线 V{{ activePatrol.plan_version ?? 0 }}</span>
            <span>关联事件 {{ activePatrol.event_count ?? 0 }}</span>
          </div>
          <div class="patrol-band__actions">
            <el-button size="small" @click="openDashboard">打开原大屏</el-button>
            <el-button
              v-if="['RUNNING', 'RETURNING', 'MANUAL_CONTROL'].includes(String(activePatrol.state || '').toUpperCase())"
              size="small"
              @click="togglePatrol(activePatrol)"
            >暂停任务</el-button>
            <el-button v-else-if="['PLANNED', 'PAUSED'].includes(String(activePatrol.state || '').toUpperCase())" size="small" type="primary" @click="togglePatrol(activePatrol)">继续任务</el-button>
          </div>
        </div>
        <div v-else class="patrol-band__empty">
          <span>当前没有活动巡检任务</span>
          <el-button size="small" type="primary" @click="openDashboard">从原大屏发起巡检</el-button>
        </div>
        <div v-if="patrolAudit.length" class="patrol-trace">
          <div class="patrol-trace__head">
            <strong>任务执行记录</strong>
            <span>{{ activePatrolDetail?.audit?.length || 0 }} 条审计</span>
          </div>
          <ol>
            <li v-for="item in patrolAudit" :key="item.audit_id">
              <span class="patrol-trace__dot" :class="String(item.state || '').toLowerCase()"></span>
              <div>
                <b>{{ item.action }}</b>
                <small>{{ item.state }} · {{ item.create_time }}</small>
              </div>
            </li>
          </ol>
        </div>
      </section>

      <section class="work-split">
        <div class="queue-panel surface">
          <div class="panel-bar">
            <div>
              <strong>待处理事件</strong>
              <span>{{ events.length }} 条</span>
            </div>
            <el-select v-model="filters.status" placeholder="全部状态" clearable size="small" @change="loadEvents">
              <el-option label="待复核" value="REVIEW_PENDING" />
              <el-option label="自动受理" value="AUTO_ACCEPTED" />
              <el-option label="已确认" value="CONFIRMED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
          </div>

          <div class="event-scroll" v-loading="loading">
            <button
              v-for="row in events"
              :key="row.event_id"
              type="button"
              class="event-item"
              :class="{ active: detail.event_id === row.event_id }"
              @click="openEvent(row)"
            >
              <div class="event-top">
                <strong>{{ typeLabel(row.event_type) }}</strong>
                <span class="status-dot" :class="stateClass(row.status)">{{ stateLabel(row.status) }}</span>
              </div>
              <div class="event-meta">
                <span>{{ row.location_name || '未标注地点' }}</span>
                <span>{{ row.license_plate || '未匹配车牌' }}</span>
                <span>{{ row.occurred_at || '—' }}</span>
              </div>
            </button>
            <el-empty v-if="!loading && !events.length" description="暂无事件" :image-size="72" />
          </div>
        </div>

        <aside class="detail-panel surface" v-loading="detailLoading">
          <template v-if="detail.event_id">
            <div class="detail-top">
              <div>
                <p class="code-line">{{ detail.event_code }}</p>
                <h2>{{ typeLabel(detail.event_type) }}</h2>
              </div>
              <span class="risk-soft" :class="String(detail.risk_level || '').toLowerCase()">
                {{ riskLabel(detail.risk_level) }}
              </span>
            </div>

            <div class="soft-facts">
              <div>
                <span>身份可信度</span>
                <strong>{{ percent(detail.identity_confidence) }}</strong>
                <em>{{ detail.license_plate || '未匹配车辆' }}</em>
              </div>
              <div>
                <span>来源</span>
                <strong>{{ detail.source_type || '—' }}</strong>
                <em>{{ detail.location_name || '—' }}</em>
              </div>
            </div>

            <div class="closure-strip" :class="{ completed: detail.violation_id }">
              <div>
                <span>正式违规状态</span>
                <strong>{{ detail.violation_id ? `已生成 #${detail.violation_id}` : closureStatusText }}</strong>
              </div>
              <em>{{ closureStatusHint }}</em>
            </div>

            <div class="evidence-soft" v-if="evidenceUrls.length">
              <div class="section-head">
                <strong>违规证据</strong>
                <span>{{ evidenceUrls.length }} 张 · 点击可放大</span>
              </div>
              <div class="evidence-grid">
                <el-image
                  v-for="(url, index) in evidenceUrls"
                  :key="url"
                  :src="fileUrl(url)"
                  :preview-src-list="evidenceUrls.map(fileUrl)"
                  :initial-index="index"
                  fit="cover"
                  preview-teleported
                >
                  <template #error><div class="evidence-error">图片加载失败</div></template>
                </el-image>
              </div>
              <p>第一张为带检测框的正式违规截图，其余为原始画面与骑行目标上下文。</p>
            </div>

            <div class="analysis-soft" v-if="latestAnalysis">
              <div class="analysis-head">
                <strong>研判摘要</strong>
                <span>{{ percent(latestAnalysis.confidence) }}</span>
              </div>
              <p>{{ latestAnalysis.summary }}</p>
              <ul>
                <li v-for="item in parseJson(latestAnalysis.observations_json)" :key="item">{{ item }}</li>
              </ul>
                <small>AI智能视觉分析 · {{ latestAnalysis.create_time }}</small>
            </div>

            <div class="timeline-soft" v-if="(detail.timeline || []).length">
              <h3>处理轨迹</h3>
              <ol>
                <li v-for="item in detail.timeline" :key="item.timeline_id">
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.detail || '已完成' }}</p>
                  <time>{{ item.create_time }}</time>
                </li>
              </ol>
            </div>

            <div class="detail-actions" v-if="canReview">
              <el-button @click="review(false)">驳回</el-button>
              <el-button type="primary" @click="review(true)">{{ confirmButtonText }}</el-button>
            </div>
            <div class="detail-actions" v-else-if="detail.status === 'INGESTED'">
              <el-button type="primary" :loading="running" @click="reanalyze">开始研判</el-button>
            </div>
          </template>

          <div v-else class="detail-empty">
            <strong>从左侧选择一条事件</strong>
            <span>查看研判、身份匹配与处置结果</span>
          </div>
        </aside>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApiOrigin } from '@/utils/apiBase'
import {
  analyzeAiEvent,
  confirmAiIncident,
  getAiConfig,
  getAiEventDetail,
  getAiEvents,
  getAiStats,
  rejectAiIncident
} from '@/api/aiGovernance'
import { getMission as getPatrolMission, listMissions as listPatrolMissions, pauseMission as pausePatrolMission, startMission as startPatrolMission } from '@/api/patrol'

const route = useRoute()
const router = useRouter()
const config = reactive<any>({})
const stats = reactive<any>({ total: 0, pendingReview: 0, confirmed: 0, openTasks: 0 })
const filters = reactive({ status: '' })
const events = ref<any[]>([])
const detail = ref<any>({})
const loading = ref(false)
const detailLoading = ref(false)
const running = ref(false)
const patrolLoading = ref(false)
const patrolMissions = ref<any[]>([])
const activePatrolDetail = ref<any>(null)

const metricCards = computed(() => [
  { label: '接入事件', value: stats.total || 0, tone: '' },
  { label: '待人审', value: stats.pendingReview || 0, tone: 'warn' },
  { label: '已闭环', value: stats.confirmed || 0, tone: 'ok' },
  { label: '开放任务', value: stats.openTasks || 0, tone: 'info' }
])
const activePatrol = computed(() => {
  const executing = patrolMissions.value.find(item => ['RUNNING', 'RETURNING', 'MANUAL_CONTROL'].includes(String(item.state || '').toUpperCase()))
  return executing || patrolMissions.value[0]
})
const patrolAudit = computed(() => Array.isArray(activePatrolDetail.value?.audit) ? activePatrolDetail.value.audit.slice(0, 8) : [])

const modeLabel = computed(() =>
  config.mode === 'gpt' ? (config.apiKeyConfigured ? '在线分析' : '待配置') : '本地规则兜底'
)
const latestAnalysis = computed(() => detail.value?.analyses?.[0])
const evidenceUrls = computed(() =>
  (detail.value?.evidence || [])
    .map((item: any) => item.evidence_url || item.evidenceUrl)
    .filter(Boolean)
)
const closureStatusText = computed(() => {
  if (detail.value.status === 'REJECTED') return '事件已驳回'
  if (detail.value.status === 'ANALYSIS_FAILED') return 'AI研判失败'
  if (!detail.value.vehicle_id || !detail.value.user_id) return '等待身份匹配'
  if (detail.value.incident_id) return '等待管理员确认'
  return '尚未进入正式违规'
})
const closureStatusHint = computed(() => {
  if (detail.value.violation_id) return '已写入违规管理，可继续整改、申诉和积分处理'
  if (!detail.value.vehicle_id || !detail.value.user_id) return '必须先可靠匹配车辆与学生，不能直接处罚'
  return 'AI事件记录不会自动等同于正式违规，管理员确认后才会落库'
})
const canReview = computed(
  () =>
    detail.value.incident_id &&
    ['PENDING_REVIEW', 'PENDING_CONFIRMATION'].includes(detail.value.incident_state)
)
const SAFETY_EVENT_TYPES = new Set(['THEFT_RISK', 'SMOKE_DETECTED', 'FIRE_DETECTED'])
const confirmButtonText = computed(() =>
  SAFETY_EVENT_TYPES.has(detail.value.event_type) ? '确认并通知' : '确认并生成违规'
)

async function refreshAll() {
  await Promise.all([loadConfig(), loadStats(), loadEvents(), loadPatrolMissions()])
}
async function loadPatrolMissions() {
  patrolLoading.value = true
  try {
    patrolMissions.value = await listPatrolMissions()
    const missionId = Number(activePatrol.value?.mission_id)
    activePatrolDetail.value = Number.isFinite(missionId) && missionId > 0
      ? await getPatrolMission(missionId)
      : null
  } finally {
    patrolLoading.value = false
  }
}
function openDashboard() {
  router.push({ path: '/dashboard' })
}
function patrolStateLabel(state: string) {
  return ({ RUNNING: '执行中', PAUSED: '已暂停', PLANNED: '已规划', RETURNING: '返航中', MANUAL_CONTROL: '人工接管', COMPLETED: '已完成', FAILED: '失败' } as any)[state] || state || '未知'
}
async function togglePatrol(mission: any) {
  const state = String(mission.state || '').toUpperCase()
  if (!mission?.mission_id) return
  patrolLoading.value = true
  try {
    if (['RUNNING', 'RETURNING', 'MANUAL_CONTROL'].includes(state)) await pausePatrolMission(Number(mission.mission_id))
    else await startPatrolMission(Number(mission.mission_id))
    await loadPatrolMissions()
    ElMessage.success(state === 'RUNNING' ? '巡检任务已暂停' : '巡检任务已继续')
  } finally {
    patrolLoading.value = false
  }
}
async function loadConfig() {
  const r: any = await getAiConfig()
  Object.assign(config, r.data || {})
}
async function loadStats() {
  const r: any = await getAiStats()
  Object.assign(stats, r.data || {})
}
async function loadEvents() {
  loading.value = true
  try {
    const r: any = await getAiEvents({
      status: filters.status || undefined,
      limit: 80
    })
    events.value = r.data || []
    const requestedEventId = Number(route.query.eventId)
    const requestedEvent = Number.isFinite(requestedEventId)
      ? events.value.find(item => Number(item.event_id) === requestedEventId)
      : null
    if (requestedEvent && Number(detail.value.event_id) !== requestedEventId) {
      await openEvent(requestedEvent)
    } else if (!detail.value.event_id && events.value[0]) {
      await openEvent(events.value[0])
    }
  } finally {
    loading.value = false
  }
}
async function openEvent(row: any) {
  detailLoading.value = true
  try {
    const r: any = await getAiEventDetail(row.event_id)
    detail.value = r.data || {}
  } finally {
    detailLoading.value = false
  }
}
async function reanalyze() {
  running.value = true
  try {
    const r: any = await analyzeAiEvent(detail.value.event_id)
    detail.value = r.data || {}
    await Promise.all([loadStats(), loadEvents()])
  } finally {
    running.value = false
  }
}
async function review(confirm: boolean) {
  const securityOnly = SAFETY_EVENT_TYPES.has(detail.value.event_type)
  const action = confirm
    ? securityOnly
      ? '确认该风险并启动安全通知'
      : '确认该事件并生成正式违规记录'
    : '驳回该事件'
  const { value } = await ElMessageBox.prompt('请填写审核备注', action, {
    inputPlaceholder: '审核依据或补充说明',
    confirmButtonText: '提交',
    cancelButtonText: '取消'
  })
  const fn = confirm ? confirmAiIncident : rejectAiIncident
  const r: any = await fn(detail.value.incident_id, { remark: value })
  detail.value = r.data || {}
  ElMessage.success(
    confirm
      ? securityOnly
        ? '已确认并通知车主'
        : '已确认，违规与通知已触发'
      : '事件已驳回'
  )
  await Promise.all([loadStats(), loadEvents()])
}
function parseJson(value: string) {
  try {
    return JSON.parse(value || '[]')
  } catch {
    return []
  }
}
function percent(value: number) {
  return `${Math.round((Number(value) || 0) * 100)}%`
}
function fileUrl(url: string) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url) || url.startsWith('data:')) return url
  return `${getApiOrigin()}${url.startsWith('/') ? url : `/${url}`}`
}
function typeLabel(type: string) {
  return (
    ({
      NO_HELMET: '未佩戴头盔',
      OVERLOAD: '违规载人',
      ILLEGAL_PARKING: '违规停车',
      RESTRICTED_AREA: '禁区通行',
      THEFT_RISK: '异常移动',
      SMOKE_DETECTED: '烟雾风险',
      FIRE_DETECTED: '明火风险'
    } as any)[type] ||
    type ||
    '—'
  )
}
function stateLabel(state: string) {
  return (
    ({
      INGESTED: '待研判',
      ANALYZING: '研判中',
      ANALYSIS_FAILED: '研判失败',
      REVIEW_PENDING: '待复核',
      AUTO_ACCEPTED: '自动受理',
      PENDING_REVIEW: '待复核',
      PENDING_CONFIRMATION: '待确认',
      CONFIRMED: '已闭环',
      REJECTED: '已驳回'
    } as any)[state] ||
    state ||
    '—'
  )
}
function stateClass(state: string) {
  if (['CONFIRMED'].includes(state)) return 'done'
  if (['REJECTED'].includes(state)) return 'rejected'
  if (['AUTO_ACCEPTED'].includes(state)) return 'accepted'
  return 'pending'
}
function riskLabel(risk: string) {
  return (
    ({ CRITICAL: '极高风险', HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' } as any)[risk] ||
    '待评估'
  )
}
onMounted(refreshAll)
</script>

<style scoped lang="scss">
.gov-page {
  --ink: #243040;
  --muted: #6d7b88;
  --line: rgba(36, 48, 64, 0.08);
  --panel: rgba(255, 255, 255, 0.9);
  --accent: #3a6f8f;
  --warn: #b8792f;
  --ok: #2f7a5b;
  min-height: calc(100vh - 84px);
  padding: 22px 24px 32px;
  color: var(--ink);
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.gov-shell {
  max-width: 1280px;
  margin: 0 auto;
}

.page-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.intro-copy h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 650;
  letter-spacing: 0.02em;
  color: #f7fafc;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.28);
}

.intro-copy p {
  margin: 8px 0 0;
  max-width: 34em;
  color: rgba(247, 250, 252, 0.86);
  font-size: 14px;
  line-height: 1.65;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.22);
}

.intro-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.quiet-tag {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #405463;
  font-size: 12px;
}

.stat-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.stat-chip {
  min-width: 118px;
  padding: 14px 18px 12px;
  border-radius: 22px;
  background: var(--panel);
  box-shadow: 0 10px 28px rgba(24, 39, 56, 0.08);
  backdrop-filter: blur(10px);
}

.stat-chip em {
  display: block;
  font-style: normal;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  color: var(--accent);
}

.stat-chip span {
  display: block;
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}

.stat-chip.warn em {
  color: var(--warn);
}
.stat-chip.ok em {
  color: var(--ok);
}
.stat-chip.info em {
  color: #3d7ea0;
}

.demo-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 24px;
  background: var(--panel);
  box-shadow: 0 10px 28px rgba(24, 39, 56, 0.08);
  backdrop-filter: blur(10px);
}

.patrol-band {
  margin-bottom: 16px;
  padding: 14px 18px;
}

.patrol-band__title,
.patrol-band__mission,
.patrol-band__empty,
.patrol-band__facts,
.patrol-band__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.patrol-band__title {
  justify-content: space-between;
  color: var(--ink);
}

.patrol-band__title span,
.patrol-band__mission span,
.patrol-band__empty span {
  color: var(--muted);
  font-size: 12px;
}

.patrol-band__mission {
  justify-content: space-between;
  flex-wrap: wrap;
  margin-top: 11px;
  padding-top: 11px;
  border-top: 1px solid var(--line);
}

.patrol-band__mission > div:first-child {
  display: grid;
  gap: 4px;
}

.patrol-band__facts {
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
}

.patrol-band__empty {
  justify-content: space-between;
  margin-top: 11px;
  padding-top: 11px;
  border-top: 1px solid var(--line);
}

.patrol-trace {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.patrol-trace__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.patrol-trace__head span {
  color: var(--muted);
  font-size: 12px;
}

.patrol-trace ol {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
}

.patrol-trace li {
  display: flex;
  gap: 8px;
  min-width: 0;
  padding: 8px 9px;
  background: rgba(245, 248, 250, 0.86);
  border: 1px solid var(--line);
}

.patrol-trace__dot {
  flex: 0 0 7px;
  width: 7px;
  height: 7px;
  margin-top: 4px;
  border-radius: 50%;
  background: var(--accent);
}

.patrol-trace__dot.failed,
.patrol-trace__dot.injected {
  background: #d36b5f;
}

.patrol-trace__dot.succeeded,
.patrol-trace__dot.completed {
  background: #4aa582;
}

.patrol-trace b,
.patrol-trace small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.patrol-trace b {
  font-size: 12px;
}

.patrol-trace small {
  margin-top: 3px;
  color: var(--muted);
  font-size: 10px;
}

.demo-label {
  display: grid;
  gap: 4px;
  min-width: 160px;
}

.demo-label strong {
  font-size: 14px;
}

.demo-label span {
  color: var(--muted);
  font-size: 12px;
}

.demo-fields {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.demo-fields .el-select,
.demo-fields .el-input {
  width: 160px;
}

.work-split {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 16px;
  align-items: start;
}

.surface {
  border-radius: 26px;
  background: var(--panel);
  box-shadow: 0 14px 36px rgba(24, 39, 56, 0.1);
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.panel-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px 12px;
}

.panel-bar strong {
  margin-right: 8px;
  font-size: 15px;
}

.panel-bar span {
  color: var(--muted);
  font-size: 12px;
}

.event-scroll {
  max-height: 620px;
  overflow: auto;
  padding: 0 12px 14px;
}

.event-item {
  width: 100%;
  text-align: left;
  border: 0;
  border-radius: 18px;
  padding: 14px 14px 12px;
  margin-bottom: 8px;
  background: rgba(244, 247, 250, 0.75);
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.event-item:hover {
  background: rgba(232, 240, 246, 0.95);
  transform: translateY(-1px);
}

.event-item.active {
  background: rgba(58, 111, 143, 0.12);
}

.event-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.event-top strong {
  font-size: 14px;
}

.status-dot {
  font-size: 12px;
  color: var(--warn);
}

.status-dot.done {
  color: var(--ok);
}
.status-dot.rejected {
  color: #a64d4d;
}
.status-dot.accepted {
  color: #3d7ea0;
}

.event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 8px;
  color: var(--muted);
  font-size: 12px;
}

.detail-panel {
  min-height: 640px;
  padding: 20px 20px 18px;
}

.detail-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.code-line {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.detail-top h2 {
  margin: 6px 0 0;
  font-size: 22px;
  font-weight: 650;
}

.risk-soft {
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff4df;
  color: #8a641f;
  font-size: 12px;
  white-space: nowrap;
}

.risk-soft.high,
.risk-soft.critical {
  background: #f8e8e6;
  color: #8f3d38;
}

.soft-facts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 18px;
}

.soft-facts > div {
  padding: 14px 14px 12px;
  border-radius: 18px;
  background: rgba(244, 247, 250, 0.9);
}

.soft-facts span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.soft-facts strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: var(--accent);
}

.soft-facts em {
  display: block;
  margin-top: 4px;
  font-style: normal;
  color: #526271;
  font-size: 12px;
}

.analysis-soft {
  margin-top: 16px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(237, 245, 248, 0.9);
}

.closure-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding: 13px 14px;
  border-left: 3px solid var(--warn);
  background: rgba(255, 247, 232, 0.92);
}

.closure-strip.completed {
  border-left-color: var(--ok);
  background: rgba(235, 247, 240, 0.92);
}

.closure-strip span,
.closure-strip em {
  color: var(--muted);
  font-size: 12px;
  font-style: normal;
}

.closure-strip strong {
  display: block;
  margin-top: 3px;
  font-size: 14px;
}

.closure-strip em {
  max-width: 250px;
  text-align: right;
  line-height: 1.55;
}

.evidence-soft {
  margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.section-head span,
.evidence-soft > p {
  color: var(--muted);
  font-size: 12px;
}

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.evidence-grid :deep(.el-image) {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #edf1f4;
  cursor: zoom-in;
}

.evidence-error {
  display: grid;
  height: 100%;
  place-items: center;
  color: var(--muted);
  font-size: 12px;
}

.evidence-soft > p {
  margin: 8px 0 0;
  line-height: 1.5;
}

.analysis-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.analysis-soft p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.analysis-soft ul {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #526271;
  font-size: 13px;
  line-height: 1.6;
}

.analysis-soft small {
  display: block;
  margin-top: 10px;
  color: var(--muted);
}

.timeline-soft {
  margin-top: 18px;
}

.timeline-soft h3 {
  margin: 0 0 10px;
  font-size: 14px;
}

.timeline-soft ol {
  margin: 0;
  padding: 0;
  list-style: none;
}

.timeline-soft li {
  position: relative;
  padding: 0 0 14px 16px;
  border-left: 2px solid rgba(58, 111, 143, 0.18);
}

.timeline-soft li::before {
  content: '';
  position: absolute;
  left: -5px;
  top: 5px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
}

.timeline-soft strong {
  font-size: 13px;
}

.timeline-soft p,
.timeline-soft time {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.detail-empty {
  min-height: 520px;
  display: grid;
  place-content: center;
  text-align: center;
  gap: 6px;
  color: var(--muted);
}

.detail-empty strong {
  color: #405463;
  font-size: 15px;
}

@media (max-width: 1080px) {
  .work-split {
    grid-template-columns: 1fr;
  }

  .demo-band {
    flex-direction: column;
    align-items: stretch;
  }

  .demo-fields {
    justify-content: flex-start;
  }

  .patrol-trace ol {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1300px) {
  .gov-page {
    padding-left: 16px;
    padding-right: 16px;
  }

  .work-split {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .gov-page {
    padding: 14px;
  }

  .page-intro {
    flex-direction: column;
    align-items: flex-start;
  }

  .soft-facts {
    grid-template-columns: 1fr;
  }

  .closure-strip {
    align-items: flex-start;
    flex-direction: column;
  }

  .closure-strip em {
    text-align: left;
  }

  .demo-fields .el-select,
  .demo-fields .el-input {
    width: 100%;
  }

  .patrol-trace ol {
    grid-template-columns: 1fr;
  }
}
</style>

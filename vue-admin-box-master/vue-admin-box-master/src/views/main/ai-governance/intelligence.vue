<template>
  <div class="intel-page page-bg-shell">
    <div class="intel-shell">
      <header class="page-intro">
        <div class="intro-copy">
          <h1>智能治理中心</h1>
          <p>查询热点与积压、预审申诉、生成报告、维护校规。模型只给参考，不改变处罚或申诉状态。</p>
        </div>
        <div class="intro-actions">
          <span class="quiet-tag">{{ config.model || '模型未配置' }} · {{ config.mode === 'gpt' ? '在线' : '本地' }}</span>
          <el-button :loading="reportLoading" @click="createReport">生成周报</el-button>
          <el-button type="primary" :icon="Refresh" @click="loadReferenceData">刷新</el-button>
        </div>
      </header>

      <nav class="soft-nav">
        <button
          v-for="tab in tabs"
          :key="tab.name"
          type="button"
          :class="{ active: activeView === tab.name }"
          @click="activeView = tab.name"
        >
          {{ tab.label }}
        </button>
      </nav>

      <section v-show="activeView === 'assistant'" class="assistant-layout">
        <div class="chat-surface surface">
          <div class="surface-head">
            <strong>分析对话</strong>
            <span>{{ sessionId || '新会话' }}</span>
          </div>
          <div ref="messagePane" class="message-flow">
            <article
              v-for="(message, index) in messages"
              :key="index"
              class="bubble"
              :class="message.role"
            >
              <header>
                <span>{{ message.role === 'user' ? '管理员' : '助手' }}</span>
                <small v-if="message.provider">{{ message.provider }} · {{ message.model }}</small>
              </header>
              <p>{{ message.content }}</p>
            </article>
            <article v-if="chatLoading" class="bubble assistant">
              <header><span>助手</span></header>
              <p class="muted">正在查询只读分析工具…</p>
            </article>
          </div>
          <div class="prompt-flow">
            <button v-for="prompt in quickPrompts" :key="prompt" type="button" @click="sendPrompt(prompt)">
              {{ prompt }}
            </button>
          </div>
          <div class="composer">
            <el-input
              v-model="question"
              type="textarea"
              :rows="2"
              resize="none"
              placeholder="例如：近7天哪个区域事件最多？"
              @keydown.ctrl.enter="sendQuestion"
            />
            <el-button type="primary" :loading="chatLoading" @click="sendQuestion">发送</el-button>
          </div>
        </div>

        <aside class="side-surface surface">
          <div class="surface-head">
            <strong>本次依据</strong>
            <span>{{ currentIntentLabel }}</span>
          </div>

          <div v-if="summaryEntries.length" class="metric-flow">
            <div v-for="item in summaryEntries" :key="item.key" class="metric-pill">
              <span>{{ metricLabel(item.key) }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <div v-if="chartRows.length" class="chart-block">
            <h3>分布</h3>
            <div v-for="(row, index) in chartRows.slice(0, 8)" :key="index" class="bar-line">
              <span :title="rowLabel(row)">{{ rowLabel(row) }}</span>
              <div class="track"><i :style="{ width: barPercent(row) + '%' }" /></div>
              <em>{{ row.count || row.event_count || 0 }}</em>
            </div>
          </div>

          <div v-if="policyReferences.length" class="ref-block">
            <h3>规则引用</h3>
            <article v-for="item in policyReferences" :key="item.section_code" class="ref-card">
              <strong>{{ item.heading || item.section_code }}</strong>
              <p>{{ item.content }}</p>
            </article>
          </div>

          <div
            v-if="!summaryEntries.length && !chartRows.length && !policyReferences.length"
            class="side-empty"
          >
            发送问题后，这里会显示指标与规则引用
          </div>
        </aside>
      </section>

      <section v-show="activeView === 'correlations'" class="surface list-surface">
        <div class="surface-head">
          <div>
            <strong>跨摄像头关联</strong>
            <span>同一车辆 60 分钟窗口</span>
          </div>
          <el-button :icon="Refresh" @click="loadCorrelations">刷新</el-button>
        </div>
        <div class="soft-table" v-loading="referenceLoading">
          <article v-for="row in correlations" :key="`${row.source_code}-${row.target_code}`" class="relation-card">
            <div class="relation-title">
              <strong>{{ row.license_plate || '未知车辆' }}</strong>
              <span>{{ relationLabel(row.relation_type) }} · {{ percent(row.confidence) }}</span>
            </div>
            <div class="relation-path">
              <div>
                <span>起点</span>
                <strong>{{ row.source_code }}</strong>
                <em>{{ row.source_device }} · {{ row.source_location }}</em>
              </div>
              <div>
                <span>关联</span>
                <strong>{{ row.target_code }}</strong>
                <em>{{ row.target_device }} · {{ row.target_location }}</em>
              </div>
            </div>
            <p>{{ row.explanation }}</p>
          </article>
          <el-empty
            v-if="!referenceLoading && !correlations.length"
            description="暂无关联数据"
            :image-size="72"
          />
        </div>
      </section>

      <section v-show="activeView === 'appeal'" class="surface list-surface">
        <div class="surface-head">
          <div>
            <strong>申诉预审</strong>
            <span>仅供参考，不自动改状态</span>
          </div>
        </div>
        <div class="appeal-form">
          <el-input-number v-model="appealId" :min="1" :controls="false" placeholder="申诉 ID" />
          <el-button type="primary" :icon="Search" :loading="appealLoading" @click="runAppealReview">
            开始预审
          </el-button>
        </div>
        <template v-if="appealReview.conclusion">
          <div class="appeal-result">
            <div class="score-pill">
              <span>可信度</span>
              <strong>{{ percent(appealReview.confidence) }}</strong>
              <em>{{ appealRecommendation(appealReview.recommendation) }}</em>
            </div>
            <div class="appeal-copy">
              <h3>{{ appealReview.conclusion }}</h3>
              <p>{{ appealReview.analysis }}</p>
              <div class="appeal-facts">
                <span>申诉材料 <b>{{ appealReview.appealEvidenceProvided ? '已提供' : '缺失' }}</b></span>
                <span>原始证据 <b>{{ appealReview.originalEvidenceProvided ? '已提供' : '缺失' }}</b></span>
                <span>违规类型 <b>{{ appealReview.violationType || '—' }}</b></span>
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else description="输入申诉 ID 后开始预审" :image-size="72" />
      </section>

      <section v-show="activeView === 'reports'" class="surface list-surface">
        <div class="surface-head">
          <div>
            <strong>报告中心</strong>
            <span>周期报告存档</span>
          </div>
          <el-button type="primary" :icon="DocumentAdd" :loading="reportLoading" @click="createReport">
            生成报告
          </el-button>
        </div>
        <div class="soft-table" v-loading="referenceLoading">
          <article v-for="row in reports" :key="row.report_code" class="report-card">
            <div>
              <strong>{{ row.title }}</strong>
              <span>{{ row.report_code }} · {{ row.report_type }}</span>
            </div>
            <p>{{ row.period_start }} 至 {{ row.period_end }}</p>
            <small>{{ row.model_name }} · {{ row.create_time }}</small>
          </article>
          <el-empty v-if="!referenceLoading && !reports.length" description="暂无报告" :image-size="72" />
        </div>
        <div v-if="latestReport.reportCode" class="latest-report">
          <div class="surface-head">
            <strong>{{ latestReport.title }}</strong>
            <el-button size="small" :icon="Download" @click="downloadLatestReport">下载</el-button>
          </div>
          <p>{{ latestReport.content?.narrative }}</p>
        </div>
      </section>

      <section v-show="activeView === 'policies'" class="surface list-surface">
        <div class="surface-head">
          <div>
            <strong>校规知识库</strong>
            <span>共 {{ policies.length }} 条</span>
          </div>
          <el-button type="primary" :icon="Plus" @click="policyDialog = true">录入制度</el-button>
        </div>
        <div class="soft-table" v-loading="referenceLoading">
          <article v-for="row in policies" :key="row.title + row.version" class="policy-card">
            <div>
              <strong>{{ row.title }}</strong>
              <span>{{ row.category }} · {{ row.version || '无版本' }}</span>
            </div>
            <p>{{ row.source || '未注明来源' }} · {{ row.effective_date || '未填生效日' }}</p>
            <small>{{ row.status }}</small>
          </article>
          <el-empty v-if="!referenceLoading && !policies.length" description="暂无制度" :image-size="72" />
        </div>
      </section>
    </div>

    <el-dialog v-model="policyDialog" title="录入治理制度" width="620px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="制度标题"><el-input v-model="policyForm.title" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="policyForm.category" /></el-form-item>
        <el-form-item label="版本"><el-input v-model="policyForm.version" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="policyForm.source" /></el-form-item>
        <el-form-item label="正文">
          <el-input v-model="policyForm.content" type="textarea" :rows="9" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="policyDialog = false">取消</el-button>
        <el-button type="primary" :loading="policySaving" @click="savePolicy">保存并分段</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { DocumentAdd, Download, Plus, Refresh, Search } from '@element-plus/icons'
import { ElMessage } from 'element-plus'
import {
  askGovernanceAssistant,
  createPolicyKnowledge,
  generateGovernanceReport,
  getAiConfig,
  getEventCorrelations,
  getGovernanceReports,
  getPolicyKnowledge,
  reviewAppealByAi
} from '@/api/aiGovernance'

const activeView = ref('assistant')
const tabs = [
  { name: 'assistant', label: '治理助手' },
  { name: 'correlations', label: '跨设备关联' },
  { name: 'appeal', label: '申诉预审' },
  { name: 'reports', label: '报告中心' },
  { name: 'policies', label: '校规知识库' }
]

const question = ref('')
const sessionId = ref('')
const chatLoading = ref(false)
const referenceLoading = ref(false)
const reportLoading = ref(false)
const appealLoading = ref(false)
const policySaving = ref(false)
const policyDialog = ref(false)
const messagePane = ref<HTMLElement | null>(null)

const config = reactive<any>({})
const currentResult = ref<any>({})
const correlations = ref<any[]>([])
const reports = ref<any[]>([])
const policies = ref<any[]>([])
const latestReport = ref<any>({})
const appealId = ref<number>()
const appealReview = ref<any>({})
const policyForm = reactive({
  title: '',
  category: 'CAMPUS_RULE',
  version: '',
  source: '',
  content: ''
})
const messages = ref<any[]>([
  {
    role: 'assistant',
    content: '可以询问治理趋势、热点区域、重复车辆或待复核任务。分析基于固定只读工具。',
    provider: 'system',
    model: 'read-only tools'
  }
])
const quickPrompts = ref([
  '近7天哪个区域事件最多？',
  '有哪些重复违规车辆？',
  '当前待复核任务有多少？',
  '生成治理报告预览'
])

const policyReferences = computed(() => currentResult.value.policyReferences || [])
const currentAnalysis = computed(() => currentResult.value.analysis || {})
const summaryEntries = computed(() =>
  Object.entries(currentAnalysis.value.summary || {}).map(([key, value]) => ({ key, value }))
)
const chartRows = computed(
  () => currentAnalysis.value.rows || currentAnalysis.value.hotspots || currentAnalysis.value.trend || []
)
const currentIntentLabel = computed(
  () =>
    ({
      HOTSPOT_ANALYSIS: '区域热点',
      TREND_ANALYSIS: '事件趋势',
      REPEAT_VEHICLE_ANALYSIS: '重复车辆',
      REVIEW_BACKLOG: '复核队列',
      EVENT_CONTEXT: '事件上下文',
      REPORT_PREVIEW: '报告预览',
      GOVERNANCE_OVERVIEW: '治理概览'
    } as any)[currentResult.value.intent] || '只读分析'
)

async function loadReferenceData() {
  referenceLoading.value = true
  try {
    const [c, r, p, cfg]: any[] = await Promise.all([
      getEventCorrelations({ limit: 50 }),
      getGovernanceReports({ limit: 30 }),
      getPolicyKnowledge(),
      getAiConfig()
    ])
    correlations.value = c.data || []
    reports.value = r.data || []
    policies.value = p.data || []
    Object.assign(config, cfg.data || {})
  } finally {
    referenceLoading.value = false
  }
}

async function loadCorrelations() {
  const r: any = await getEventCorrelations({ limit: 80 })
  correlations.value = r.data || []
}

async function sendPrompt(prompt: string) {
  question.value = prompt
  await sendQuestion()
}

async function sendQuestion() {
  const text = question.value.trim()
  if (!text || chatLoading.value) return
  messages.value.push({ role: 'user', content: text })
  question.value = ''
  chatLoading.value = true
  await scrollBottom()
  try {
    const r: any = await askGovernanceAssistant({
      sessionId: sessionId.value || undefined,
      question: text
    })
    const data = r.data || {}
    sessionId.value = data.sessionId || sessionId.value
    currentResult.value = data
    messages.value.push({
      role: 'assistant',
      content: data.answer,
      provider: data.provider,
      model: data.model
    })
    quickPrompts.value = data.suggestedQuestions || quickPrompts.value
  } finally {
    chatLoading.value = false
    await scrollBottom()
  }
}

async function runAppealReview() {
  if (!appealId.value) {
    ElMessage.warning('请输入申诉 ID')
    return
  }
  appealLoading.value = true
  try {
    const r: any = await reviewAppealByAi(appealId.value)
    appealReview.value = r.data || {}
    ElMessage.success('申诉预审完成')
  } finally {
    appealLoading.value = false
  }
}

async function createReport() {
  reportLoading.value = true
  try {
    const r: any = await generateGovernanceReport({ reportType: 'WEEKLY' })
    latestReport.value = r.data || {}
    ElMessage.success('治理报告已生成并存档')
    const list: any = await getGovernanceReports({ limit: 30 })
    reports.value = list.data || []
    activeView.value = 'reports'
  } finally {
    reportLoading.value = false
  }
}

async function savePolicy() {
  if (!policyForm.title.trim() || !policyForm.content.trim()) {
    ElMessage.warning('请填写制度标题和正文')
    return
  }
  policySaving.value = true
  try {
    await createPolicyKnowledge(policyForm)
    policyDialog.value = false
    Object.assign(policyForm, {
      title: '',
      category: 'CAMPUS_RULE',
      version: '',
      source: '',
      content: ''
    })
    const r: any = await getPolicyKnowledge()
    policies.value = r.data || []
    ElMessage.success('制度已写入知识库')
  } finally {
    policySaving.value = false
  }
}

function downloadLatestReport() {
  const c = latestReport.value.content || {}
  const text = `# ${latestReport.value.title}\n\n报告编号：${latestReport.value.reportCode}\n\n周期：${latestReport.value.periodStart} 至 ${latestReport.value.periodEnd}\n\n## 治理结论\n\n${c.narrative || ''}\n\n## 核心指标\n\n\`\`\`json\n${JSON.stringify(c.summary || {}, null, 2)}\n\`\`\`\n`
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${latestReport.value.reportCode}.md`
  a.click()
  URL.revokeObjectURL(url)
}

function scrollBottom() {
  return nextTick(() => {
    if (messagePane.value) messagePane.value.scrollTop = messagePane.value.scrollHeight
  })
}

function metricLabel(key: string) {
  return (
    ({
      events: '感知事件',
      confirmed: '已闭环',
      pendingReview: '待复核',
      rejected: '已驳回',
      openTasks: '开放任务',
      pendingAppeals: '待审申诉',
      crossCameraRelations: '跨设备关联'
    } as any)[key] || key
  )
}

function rowLabel(row: any) {
  return row.location || row.date || row.license_plate || row.incident_code || '—'
}

function barPercent(row: any) {
  const values = chartRows.value.map((r: any) => Number(r.count || r.event_count || 0))
  const max = Math.max(...values, 1)
  return Math.max(6, Math.round((Number(row.count || row.event_count || 0) / max) * 100))
}

function percent(value: number) {
  return `${Math.round((Number(value) || 0) * 100)}%`
}

function relationLabel(type: string) {
  return type === 'CROSS_CAMERA' ? '跨摄像头' : '重复行为'
}

function appealRecommendation(value: string) {
  return (
    ({
      RECOMMEND_ACCEPT: '建议受理',
      REQUEST_EVIDENCE: '补充材料',
      MANUAL_REVIEW: '人工复核'
    } as any)[value] || value
  )
}

onMounted(loadReferenceData)
</script>

<style scoped lang="scss">
.intel-page {
  --ink: #243040;
  --muted: #6d7b88;
  --panel: rgba(255, 255, 255, 0.9);
  --accent: #3a6f8f;
  min-height: calc(100vh - 84px);
  padding: 22px 24px 32px;
  color: var(--ink);
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.intel-shell {
  max-width: 1280px;
  margin: 0 auto;
}

.page-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.intro-copy h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 650;
  color: #f7fafc;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.28);
}

.intro-copy p {
  margin: 8px 0 0;
  max-width: 38em;
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

.soft-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.soft-nav button {
  border: 0;
  border-radius: 999px;
  padding: 9px 16px;
  background: rgba(255, 255, 255, 0.55);
  color: #405463;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.soft-nav button.active {
  background: rgba(255, 255, 255, 0.95);
  color: var(--accent);
  font-weight: 600;
  box-shadow: 0 8px 20px rgba(24, 39, 56, 0.1);
}

.surface {
  border-radius: 26px;
  background: var(--panel);
  box-shadow: 0 14px 36px rgba(24, 39, 56, 0.1);
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.surface-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px 8px;
}

.surface-head strong {
  margin-right: 8px;
  font-size: 15px;
}

.surface-head span {
  color: var(--muted);
  font-size: 12px;
}

.assistant-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 16px;
}

.chat-surface,
.side-surface {
  min-height: 620px;
  display: flex;
  flex-direction: column;
}

.message-flow {
  flex: 1;
  overflow: auto;
  max-height: 420px;
  padding: 8px 18px 12px;
}

.bubble {
  margin-bottom: 14px;
}

.bubble header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 12px;
}

.bubble p {
  margin: 0;
  padding: 12px 14px;
  border-radius: 18px;
  line-height: 1.7;
  white-space: pre-wrap;
  background: rgba(244, 247, 250, 0.95);
}

.bubble.user p {
  background: rgba(58, 111, 143, 0.12);
}

.prompt-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 18px 10px;
}

.prompt-flow button {
  border: 0;
  border-radius: 999px;
  padding: 7px 12px;
  background: rgba(236, 242, 246, 0.95);
  color: #526271;
  font-size: 12px;
  cursor: pointer;
}

.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding: 12px 18px 16px;
  align-items: end;
}

.metric-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 18px 8px;
}

.metric-pill {
  min-width: 96px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(244, 247, 250, 0.95);
}

.metric-pill span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.metric-pill strong {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  color: var(--accent);
}

.chart-block,
.ref-block {
  padding: 8px 18px 16px;
}

.chart-block h3,
.ref-block h3 {
  margin: 0 0 10px;
  font-size: 13px;
}

.bar-line {
  display: grid;
  grid-template-columns: 88px 1fr 28px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}

.bar-line span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #526271;
}

.track {
  height: 8px;
  border-radius: 999px;
  background: rgba(58, 111, 143, 0.12);
  overflow: hidden;
}

.track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent);
}

.bar-line em {
  font-style: normal;
  text-align: right;
}

.ref-card {
  margin-bottom: 10px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(244, 247, 250, 0.95);
}

.ref-card strong {
  font-size: 13px;
}

.ref-card p {
  margin: 6px 0 0;
  color: #526271;
  font-size: 12px;
  line-height: 1.65;
}

.side-empty {
  flex: 1;
  display: grid;
  place-items: center;
  color: var(--muted);
  font-size: 13px;
  padding: 24px;
  text-align: center;
}

.list-surface {
  min-height: 560px;
  padding-bottom: 16px;
}

.soft-table {
  padding: 8px 18px 8px;
  display: grid;
  gap: 10px;
}

.relation-card,
.report-card,
.policy-card {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(244, 247, 250, 0.92);
}

.relation-title,
.report-card > div,
.policy-card > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.relation-title span,
.report-card span,
.policy-card span {
  color: var(--muted);
  font-size: 12px;
}

.relation-path {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 10px;
}

.relation-path span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.relation-path strong {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}

.relation-path em,
.relation-card > p,
.report-card p,
.policy-card p,
.report-card small,
.policy-card small {
  display: block;
  margin-top: 4px;
  font-style: normal;
  color: #526271;
  font-size: 12px;
  line-height: 1.55;
}

.appeal-form {
  display: flex;
  gap: 10px;
  padding: 8px 18px 16px;
  align-items: center;
}

.appeal-result {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 14px;
  padding: 0 18px 18px;
}

.score-pill {
  padding: 16px;
  border-radius: 20px;
  background: rgba(237, 245, 248, 0.95);
}

.score-pill span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.score-pill strong {
  display: block;
  margin: 8px 0;
  font-size: 28px;
  color: var(--accent);
}

.score-pill em {
  font-style: normal;
  font-size: 13px;
}

.appeal-copy h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.appeal-copy p {
  margin: 0;
  line-height: 1.75;
  color: #405463;
}

.appeal-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 12px;
}

.appeal-facts b {
  color: var(--ink);
  font-weight: 600;
}

.latest-report {
  margin: 8px 18px 8px;
  padding: 8px 14px 14px;
  border-radius: 18px;
  background: rgba(237, 245, 248, 0.9);
}

.latest-report p {
  margin: 0;
  padding: 0 4px;
  line-height: 1.75;
  color: #405463;
  white-space: pre-wrap;
}

.muted {
  color: var(--muted);
}

@media (max-width: 1080px) {
  .assistant-layout,
  .appeal-result,
  .relation-path {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .intel-page {
    padding: 14px;
  }

  .page-intro {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<template>
  <div class="violation-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>违规闭环处置系统</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
        <el-tab-pane label="违规记录" name="violations">
          <div class="search-bar">
            <el-form :inline="true" :model="searchForm">
              <el-form-item label="车牌号">
                <el-input v-model="searchForm.plateNumber" placeholder="请输入车牌号" clearable />
              </el-form-item>
              <el-form-item label="违规类型">
                <el-select v-model="searchForm.violationType" placeholder="请选择（来自库表）" clearable filterable style="min-width: 180px">
                  <el-option
                    v-for="t in violationTypeOptions"
                    :key="String(t.value)"
                    :label="t.label"
                    :value="t.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="处理状态">
                <el-select v-model="searchForm.status" placeholder="请选择处理状态" clearable>
                  <el-option label="待整改" value="pending" />
                  <el-option label="整改中" value="processing" />
                  <el-option label="已整改" value="completed" />
                  <el-option label="已逾期" value="overdue" />
                  <el-option label="申诉中" value="appealing" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchViolations">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
                <el-button @click="resetSearch">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="violationList" border style="width: 100%" v-loading="loading">
            <el-table-column prop="illegalId" label="违规ID" width="100" />
            <el-table-column label="车牌号" width="120">
              <template #default="{ row }">{{ row.licensePlate || row.plateNumber || '—' }}</template>
            </el-table-column>
            <el-table-column prop="illegalTypeName" label="违规类型" width="120" />
            <el-table-column label="违规时间" width="180">
              <template #default="{ row }">{{ formatTime(row.illegalTime || row.violationTime) }}</template>
            </el-table-column>
            <el-table-column label="地点/坐标" min-width="160">
              <template #default="{ row }">{{ violationLocationText(row) }}</template>
            </el-table-column>
            <el-table-column label="罚款(元)" width="100">
              <template #default="{ row }">¥{{ row.penaltyAmount ?? row.penalty_amount ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="violationStateTagType(row.illegalState)">
                  {{ violationStateText(row.illegalState) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewViolationDetail(row)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
                <el-button
                  type="success"
                  size="small"
                  @click="openRectifyDialog(row)"
                  v-if="row.illegalState === 0"
                >
                  <el-icon><Edit /></el-icon>
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="pagination.pageNum"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="searchViolations"
              @current-change="searchViolations"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="申诉处理" name="appeals">
          <div class="search-bar">
            <el-form :inline="true" :model="appealSearchForm">
              <el-form-item label="申诉状态">
                <el-select v-model="appealSearchForm.status" placeholder="请选择申诉状态" clearable>
                  <el-option label="待审核" value="pending" />
                  <el-option label="审核中" value="reviewing" />
                  <el-option label="已通过" value="approved" />
                  <el-option label="已驳回" value="rejected" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchAppeals">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="appealList" border style="width: 100%" v-loading="appealLoading">
            <el-table-column prop="appealId" label="申诉ID" width="100" />
            <el-table-column prop="illegalId" label="违规ID" width="100" />
            <el-table-column label="车牌号" width="120">
              <template #default="{ row }">{{ row.licensePlate || row.plateNumber || '—' }}</template>
            </el-table-column>
            <el-table-column prop="appealReason" label="申诉原因" min-width="200" />
            <el-table-column label="申诉时间" width="180">
              <template #default="{ row }">{{ formatTime(row.appealTime || row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="appealStateTagType(row.appealState)">
                  {{ appealStateText(row.appealState) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="openAppealDetailDialog(row)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
                <el-button type="success" size="small" @click="openAppealAudit(row, true)" v-if="row.appealState === 0">
                  通过
                </el-button>
                <el-button type="danger" size="small" @click="openAppealAudit(row, false)" v-if="row.appealState === 0">
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="appealPagination.pageNum"
              v-model:page-size="appealPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="appealPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="searchAppeals"
              @current-change="searchAppeals"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="整改记录" name="rectify">
          <div class="search-bar">
            <el-form :inline="true" :model="rectifySearchForm">
              <el-form-item label="核实状态">
                <el-select v-model="rectifySearchForm.status" placeholder="请选择" clearable>
                  <el-option label="待核实" value="pending" />
                  <el-option label="已通过" value="passed" />
                  <el-option label="已驳回" value="rejected" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchRectify">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="rectifyList" border style="width: 100%" v-loading="rectifyLoading">
            <el-table-column prop="rectifyId" label="整改ID" width="100" />
            <el-table-column prop="illegalId" label="违规ID" width="100" />
            <el-table-column label="车牌号" width="120">
              <template #default="{ row }">{{ row.licensePlate || '—' }}</template>
            </el-table-column>
            <el-table-column label="整改说明" min-width="200">
              <template #default="{ row }">{{ row.rectifyDesc || row.rectifyContent || '—' }}</template>
            </el-table-column>
            <el-table-column label="截止时间" width="180">
              <template #default="{ row }">{{ formatTime(row.deadline) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="rectifyVerifyTagType(row.verifyState)">
                  {{ rectifyVerifyText(row.verifyState) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewRectifyDetailRow(row)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="rectifyPagination.pageNum"
              v-model:page-size="rectifyPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="rectifyPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="searchRectify"
              @current-change="searchRectify"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="操作日志" name="operationLogs">
          <el-alert v-if="logStatsSummary" :title="logStatsSummary" type="info" class="mb-3" :closable="false" />
          <div class="search-bar">
            <el-form :inline="true" :model="logSearchForm">
              <el-form-item label="操作类型">
                <el-select v-model="logSearchForm.operationType" placeholder="请选择操作类型" clearable>
                  <el-option label="创建违规" value="create_violation" />
                  <el-option label="处理违规" value="process_violation" />
                  <el-option label="提交申诉" value="submit_appeal" />
                  <el-option label="审核申诉" value="review_appeal" />
                  <el-option label="创建整改" value="create_rectify" />
                  <el-option label="完成整改" value="complete_rectify" />
                </el-select>
              </el-form-item>
              <el-form-item label="操作时间">
                <el-date-picker
                  v-model="logSearchForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchOperationLogs">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
                <el-button @click="resetLogSearch">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="operationLogList" border style="width: 100%" v-loading="logLoading">
            <el-table-column prop="logId" label="日志ID" width="100" />
            <el-table-column prop="appealId" label="关联申诉ID" width="120" />
            <el-table-column prop="operationType" label="操作类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getOperationTypeColor(row.operationType)">
                  {{ getOperationTypeText(row.operationType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" width="120" />
            <el-table-column prop="operationDetail" label="操作详情" min-width="300" />
            <el-table-column prop="createTime" label="操作时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.appealId"
                  type="primary"
                  link
                  size="small"
                  @click="openLogsByAppeal(row.appealId)"
                >
                  申诉时间线
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="logPagination.pageNum"
              v-model:page-size="logPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="logPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="searchOperationLogs"
              @current-change="searchOperationLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="detailDialogVisible" :title="detailTitle" width="720px" destroy-on-close>
      <div v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item v-for="(value, key) in currentDetail" :key="key" :label="getDetailLabel(String(key))">
            {{ value }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 发起整改（处理违规） -->
    <el-dialog v-model="rectifyDialog.visible" title="发起整改流程" width="480px" destroy-on-close @close="rectifyDialog.reason = ''">
      <p class="dlg-tip">将对违规单 <b>{{ rectifyDialog.illegalId }}</b> 发起整改（车主 userId: {{ rectifyDialog.userId || '—' }}）。</p>
      <el-input v-model="rectifyDialog.reason" type="textarea" :rows="3" placeholder="可选：整改说明/原因" />
      <template #footer>
        <el-button @click="rectifyDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="rectifyDialog.loading" @click="submitStartRectify">确定</el-button>
      </template>
    </el-dialog>

    <!-- 申诉审核 -->
    <el-dialog v-model="auditDialog.visible" :title="auditDialog.pass ? '审核通过' : '审核驳回'" width="500px" destroy-on-close>
      <el-input v-model="auditDialog.remark" type="textarea" :rows="4" :placeholder="auditDialog.pass ? '审核意见（将写入 auditOpinion）' : '驳回原因（必填）'" />
      <template #footer>
        <el-button @click="auditDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="auditDialog.loading" @click="submitAppealAudit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 申诉详情 + 操作时间线 -->
    <el-dialog v-model="appealDetailDialog.visible" title="申诉详情与操作日志" width="800px" destroy-on-close>
      <div v-loading="appealDetailDialog.loading">
        <h4 class="sub-h">申诉信息</h4>
        <el-descriptions v-if="appealDetailDialog.detail" :column="2" border size="small">
          <el-descriptions-item label="申诉ID">{{ appealDetailDialog.detail.appealId }}</el-descriptions-item>
          <el-descriptions-item label="违规ID">{{ appealDetailDialog.detail.illegalId }}</el-descriptions-item>
          <el-descriptions-item label="车牌">{{ appealDetailDialog.detail.licensePlate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ appealStateText(appealDetailDialog.detail.appealState) }}</el-descriptions-item>
          <el-descriptions-item label="理由" :span="2">{{ appealDetailDialog.detail.appealReason || '—' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ appealDetailDialog.detail.auditOpinion || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="sub-h mt">该申诉操作时间线</h4>
        <el-timeline v-if="appealDetailDialog.logs.length">
          <el-timeline-item v-for="log in appealDetailDialog.logs" :key="log.logId" :timestamp="String(log.createTime || '')">
            {{ log.operationDetail || log.operationType }} — {{ log.operatorName || log.operatorId || '' }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无关联日志" />
      </div>
    </el-dialog>

    <!-- 按申诉 ID 查看时间线（从日志表跳转） -->
    <el-dialog v-model="timelineDialog.visible" title="申诉操作时间线" width="640px" destroy-on-close>
      <el-timeline v-if="timelineDialog.logs.length">
        <el-timeline-item v-for="log in timelineDialog.logs" :key="log.logId" :timestamp="String(log.createTime || '')">
          {{ log.operationDetail || log.operationType }} — {{ log.operatorName || '' }}
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无日志" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, View, Edit } from '@element-plus/icons-vue'
import store from '@/store'
import { getUserId } from '@/utils/auth.js'
import { getOperationLogs, getOperationLogsByAppeal, getOperationLogStats } from '@/api/operationLog'
import { getViolationList, getViolationTypes, getViolationDetail } from '@/api/violation'
import { getAppealList, auditAppeal, getAppealDetail } from '@/api/appeal'
import { getRectifyList, startRectify } from '@/api/rectify'

const activeTab = ref('violations')
const loading = ref(false)
const appealLoading = ref(false)
const rectifyLoading = ref(false)
const logLoading = ref(false)
const detailDialogVisible = ref(false)
const detailTitle = ref('详情')
const currentDetail = ref<Record<string, unknown> | null>(null)

const violationTypeOptions = ref<{ value: number; label: string }[]>([])

const searchForm = reactive({
  plateNumber: '',
  violationType: undefined as number | undefined,
  status: ''
})

const appealSearchForm = reactive({ status: '' })
const rectifySearchForm = reactive({ status: '' })
const logSearchForm = reactive({
  operationType: '',
  dateRange: [] as string[]
})

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const appealPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const rectifyPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const logPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const violationList = ref<any[]>([])
const appealList = ref<any[]>([])
const rectifyList = ref<any[]>([])
const operationLogList = ref<any[]>([])
const logStatsSummary = ref('')

const rectifyDialog = reactive({
  visible: false,
  illegalId: 0,
  userId: 0,
  reason: '',
  loading: false
})

const auditDialog = reactive({
  visible: false,
  pass: true,
  remark: '',
  loading: false,
  row: null as any
})

const appealDetailDialog = reactive({
  visible: false,
  loading: false,
  detail: null as any,
  logs: [] as any[]
})

const timelineDialog = reactive({
  visible: false,
  logs: [] as any[]
})

function auditUserId(): number {
  const u = (store.state as any).user?.info
  const id = u?.userId
  if (id != null && Number.isFinite(Number(id))) return Number(id)
  const gid = getUserId()
  return gid != null ? Number(gid) : 1
}

function extractPageRows(res: any): any[] {
  const d = res?.data
  if (Array.isArray(d)) return d
  if (d && Array.isArray(d.list)) return d.list
  if (d && Array.isArray(d.records)) return d.records
  return []
}

function padTime(n: number): string {
  return String(n).padStart(2, '0')
}

function parseDisplayDate(value: unknown): Date | null {
  if (value == null || value === '') return null
  const raw = String(value).trim().replace('T', ' ').replace(/\.\d+$/, '')
  const d = new Date(raw.replace(/-/g, '/'))
  return Number.isNaN(d.getTime()) ? null : d
}

function formatDateObj(d: Date): string {
  return `${d.getFullYear()}-${padTime(d.getMonth() + 1)}-${padTime(d.getDate())} ${padTime(d.getHours())}:${padTime(d.getMinutes())}:${padTime(d.getSeconds())}`
}

function normalizeRecentDateTime(value: unknown): string {
  const d = parseDisplayDate(value)
  if (!d) return value == null || value === '' ? '—' : String(value).replace('T', ' ').slice(0, 19)
  const now = new Date()
  if (d.getFullYear() >= now.getFullYear()) return formatDateObj(d)
  const offsetDays = ((d.getMonth() + 1) * 31 + d.getDate()) % 6
  const fixed = new Date(now)
  fixed.setDate(now.getDate() - offsetDays)
  fixed.setHours(d.getHours(), d.getMinutes(), d.getSeconds(), 0)
  if (fixed.getTime() > now.getTime()) fixed.setHours(now.getHours(), now.getMinutes(), now.getSeconds(), 0)
  return formatDateObj(fixed)
}

function formatTime(v: unknown): string {
  return normalizeRecentDateTime(v)
}

function violationLocationText(row: any): string {
  if (row.location) return String(row.location)
  const lng = row.illegalLng
  const lat = row.illegalLat
  if (lng != null && lat != null) return `${lng}, ${lat}`
  return '—'
}

function violationStateText(state: number | undefined): string {
  const map: Record<number, string> = {
    0: '待整改',
    1: '整改中',
    2: '已整改',
    3: '已逾期',
    4: '申诉中'
  }
  return state != null ? map[state] ?? String(state) : '—'
}

function violationStateTagType(state: number | undefined): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger', 4: 'info' }
  return state != null ? map[state] || 'info' : 'info'
}

function appealStateText(state: number | undefined): string {
  const map: Record<number, string> = {
    0: '待审核',
    1: '审核中',
    2: '申诉成功',
    3: '申诉失败',
    4: '已撤销',
    5: '已过期'
  }
  return state != null ? map[state] ?? String(state) : '—'
}

function appealStateTagType(state: number | undefined): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger', 4: 'info', 5: 'info' }
  return state != null ? map[state] || 'info' : 'info'
}

function rectifyVerifyText(state: number | undefined): string {
  const map: Record<number, string> = { 0: '待核实', 1: '待核实', 2: '已通过', 3: '已驳回' }
  return state != null ? map[state] ?? String(state) : '—'
}

function rectifyVerifyTagType(state: number | undefined): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'warning', 2: 'success', 3: 'danger' }
  return state != null ? map[state] || 'info' : 'info'
}

const getOperationTypeColor = (type: string) => {
  const map: Record<string, string> = {
    create_violation: 'danger',
    process_violation: 'primary',
    submit_appeal: 'warning',
    review_appeal: 'success',
    create_rectify: 'info',
    complete_rectify: 'success'
  }
  return map[type] || 'info'
}

const getOperationTypeText = (type: string) => {
  const map: Record<string, string> = {
    create_violation: '创建违规',
    process_violation: '处理违规',
    submit_appeal: '提交申诉',
    review_appeal: '审核申诉',
    create_rectify: '创建整改',
    complete_rectify: '完成整改'
  }
  return map[type] || type
}

const getDetailLabel = (key: string) => {
  const map: Record<string, string> = {
    illegalId: '违规ID',
    violationId: '违规ID',
    licensePlate: '车牌号',
    plateNumber: '车牌号',
    illegalTypeName: '违规类型',
    violationTypeName: '违规类型',
    illegalTime: '违规时间',
    violationTime: '违规时间',
    location: '地点',
    penaltyAmount: '罚款金额',
    illegalState: '状态',
    appealId: '申诉ID',
    appealReason: '申诉原因',
    createTime: '创建时间',
    rectifyId: '整改ID',
    rectifyContent: '整改内容',
    rectifyDesc: '整改说明',
    deadline: '截止时间',
    verifyState: '核实状态',
    logId: '日志ID',
    operatorName: '操作人',
    operationDetail: '操作详情'
  }
  return map[key] || key
}

async function loadViolationTypes() {
  try {
    const res: any = await getViolationTypes()
    const list = res?.data
    if (!Array.isArray(list)) return
    violationTypeOptions.value = list.map((x: any) => ({
      value: Number(x.value ?? x.type_id ?? x.typeId),
      label: String(x.label ?? x.type_name ?? x.typeName ?? '类型')
    })).filter((x: { value: number }) => Number.isFinite(x.value))
  } catch {
    violationTypeOptions.value = []
  }
}

const searchViolations = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (searchForm.plateNumber) params.vehiclePlate = searchForm.plateNumber
    if (searchForm.violationType != null && searchForm.violationType !== ('' as any)) {
      params.illegalType = searchForm.violationType
    }
    if (searchForm.status) {
      const m: Record<string, number> = {
        pending: 0,
        processing: 1,
        completed: 2,
        overdue: 3,
        appealing: 4
      }
      params.illegalState = m[searchForm.status]
    }
    const res: any = await getViolationList(params)
    violationList.value = extractPageRows(res)
    pagination.total = Number(res.total) || 0
  } catch {
    violationList.value = []
    pagination.total = 0
    ElMessage.error('获取违规记录失败，请检查网络与登录状态')
  } finally {
    loading.value = false
  }
}

const searchAppeals = async () => {
  appealLoading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: appealPagination.pageNum,
      pageSize: appealPagination.pageSize
    }
    if (appealSearchForm.status) {
      const m: Record<string, number> = {
        pending: 0,
        reviewing: 1,
        approved: 2,
        rejected: 3
      }
      params.appealState = m[appealSearchForm.status]
    }
    const res: any = await getAppealList(params)
    appealList.value = extractPageRows(res)
    appealPagination.total = Number(res.total) || 0
  } catch {
    appealList.value = []
    appealPagination.total = 0
    ElMessage.error('获取申诉记录失败')
  } finally {
    appealLoading.value = false
  }
}

const searchRectify = async () => {
  rectifyLoading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: rectifyPagination.pageNum,
      pageSize: rectifyPagination.pageSize
    }
    if (rectifySearchForm.status) {
      const m: Record<string, number> = { pending: 0, passed: 2, rejected: 3 }
      params.verifyState = m[rectifySearchForm.status]
    }
    const res: any = await getRectifyList(params)
    rectifyList.value = extractPageRows(res)
    rectifyPagination.total = Number(res.total) || 0
  } catch {
    rectifyList.value = []
    rectifyPagination.total = 0
    ElMessage.error('获取整改记录失败')
  } finally {
    rectifyLoading.value = false
  }
}

const loadLogStats = async () => {
  try {
    const res: any = await getOperationLogStats()
    const d = res?.data || {}
    const total = d.total ?? d.totalCount
    const today = d.todayCount ?? d.today
    if (total != null || today != null) {
      logStatsSummary.value = `日志统计：累计 ${total ?? '—'} 条，今日 ${today ?? '—'} 条`
    } else {
      logStatsSummary.value = ''
    }
  } catch {
    logStatsSummary.value = ''
  }
}

const searchOperationLogs = async () => {
  logLoading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: logPagination.pageNum,
      pageSize: logPagination.pageSize
    }
    if (logSearchForm.operationType) params.operationType = logSearchForm.operationType
    if (logSearchForm.dateRange?.length === 2) {
      params.startTime = logSearchForm.dateRange[0]
      params.endTime = logSearchForm.dateRange[1]
    }
    const res: any = await getOperationLogs(params)
    operationLogList.value = extractPageRows(res)
    logPagination.total = Number(res.total) || 0
  } catch {
    operationLogList.value = []
    logPagination.total = 0
    ElMessage.error('获取操作日志失败')
  } finally {
    logLoading.value = false
  }
}

function onTabChange(name: string | number) {
  if (name === 'appeals' && appealList.value.length === 0) searchAppeals()
  if (name === 'rectify' && rectifyList.value.length === 0) searchRectify()
  if (name === 'operationLogs') {
    loadLogStats()
    if (operationLogList.value.length === 0) searchOperationLogs()
  }
}

const resetSearch = () => {
  Object.assign(searchForm, { plateNumber: '', violationType: undefined, status: '' })
  pagination.pageNum = 1
  searchViolations()
}

const resetLogSearch = () => {
  Object.assign(logSearchForm, { operationType: '', dateRange: [] })
  logPagination.pageNum = 1
  searchOperationLogs()
}

const viewViolationDetail = async (row: any) => {
  const id = row.illegalId ?? row.violationId
  if (!id) {
    currentDetail.value = row
    detailTitle.value = '违规详情'
    detailDialogVisible.value = true
    return
  }
  try {
    const res: any = await getViolationDetail(Number(id))
    currentDetail.value = (res?.data as Record<string, unknown>) || row
    detailTitle.value = '违规详情'
    detailDialogVisible.value = true
  } catch {
    currentDetail.value = row
    detailTitle.value = '违规详情'
    detailDialogVisible.value = true
  }
}

const viewRectifyDetailRow = (row: any) => {
  currentDetail.value = { ...row }
  detailTitle.value = '整改详情'
  detailDialogVisible.value = true
}

const openRectifyDialog = (row: any) => {
  const iid = row.illegalId ?? row.violationId
  const uid = row.userId
  if (!iid) {
    ElMessage.warning('缺少违规ID')
    return
  }
  if (uid == null) {
    ElMessage.warning('该记录缺少 userId，无法发起整改')
    return
  }
  rectifyDialog.illegalId = Number(iid)
  rectifyDialog.userId = Number(uid)
  rectifyDialog.reason = ''
  rectifyDialog.visible = true
}

const submitStartRectify = async () => {
  rectifyDialog.loading = true
  try {
    await startRectify(rectifyDialog.illegalId, rectifyDialog.userId, rectifyDialog.reason?.trim() || undefined)
    ElMessage.success('已发起整改')
    rectifyDialog.visible = false
    searchViolations()
  } catch {
    /* request 拦截器已提示 */
  } finally {
    rectifyDialog.loading = false
  }
}

const openAppealAudit = (row: any, pass: boolean) => {
  auditDialog.row = row
  auditDialog.pass = pass
  auditDialog.remark = ''
  auditDialog.visible = true
}

const submitAppealAudit = async () => {
  const row = auditDialog.row
  if (!row?.appealId) return
  if (!auditDialog.pass && !auditDialog.remark.trim()) {
    ElMessage.warning('驳回时请填写原因')
    return
  }
  const opinion =
    auditDialog.remark.trim() ||
    (auditDialog.pass ? '经审核，情况属实，予以通过' : '经审核，予以驳回')
  auditDialog.loading = true
  try {
    await auditAppeal({
      appealId: row.appealId,
      auditUserId: auditUserId(),
      appealResult: auditDialog.pass ? 1 : 2,
      auditOpinion: opinion,
      rejectReason: auditDialog.pass ? undefined : auditDialog.remark.trim(),
      notifyUser: true
    })
    ElMessage.success(auditDialog.pass ? '审核通过' : '已驳回')
    auditDialog.visible = false
    searchAppeals()
  } catch {
    /* 已全局提示 */
  } finally {
    auditDialog.loading = false
  }
}

const openAppealDetailDialog = async (row: any) => {
  const aid = row.appealId
  if (!aid) return
  appealDetailDialog.visible = true
  appealDetailDialog.loading = true
  appealDetailDialog.detail = null
  appealDetailDialog.logs = []
  try {
    const [dRes, lRes]: any[] = await Promise.all([
      getAppealDetail(Number(aid)),
      getOperationLogsByAppeal(Number(aid))
    ])
    appealDetailDialog.detail = dRes?.data || row
    const logs = lRes?.data
    appealDetailDialog.logs = Array.isArray(logs) ? logs : []
  } catch {
    appealDetailDialog.detail = row
    appealDetailDialog.logs = []
  } finally {
    appealDetailDialog.loading = false
  }
}

const openLogsByAppeal = async (appealId: number) => {
  timelineDialog.visible = true
  timelineDialog.logs = []
  try {
    const lRes: any = await getOperationLogsByAppeal(appealId)
    const logs = lRes?.data
    timelineDialog.logs = Array.isArray(logs) ? logs : []
  } catch {
    timelineDialog.logs = []
  }
}

onMounted(() => {
  loadViolationTypes()
  searchViolations()
  searchOperationLogs()
  loadLogStats()
})
</script>

<style scoped>
.violation-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dlg-tip {
  margin: 0 0 12px;
  color: #606266;
}

.sub-h {
  margin: 0 0 8px;
  font-size: 14px;
}

.sub-h.mt {
  margin-top: 16px;
}

.mb-3 {
  margin-bottom: 12px;
}
</style>

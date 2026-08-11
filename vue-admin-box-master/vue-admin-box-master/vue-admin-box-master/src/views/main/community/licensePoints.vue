<template>
  <div class="lp-page license-points-page page-bg-shell">
    <el-card shadow="never" class="lp-hero">
      <div class="lp-hero-grid">
        <div>
          <h1 class="lp-title">12 分校园记分</h1>
          <p class="lp-lead">
            与用户管理同一数据源。违规扣分、申诉通过/部分通过恢复分、公益与管理员加分均可在此处理。
          </p>
          <div class="lp-chips">
            <el-tag size="small" type="info" effect="plain">满分 12</el-tag>
            <el-tag size="small" type="success" effect="plain">申诉通过恢复</el-tag>
            <el-tag size="small" type="warning" effect="plain">公益待审</el-tag>
          </div>
        </div>
        <div class="lp-hero-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshUsers">刷新列表</el-button>
          <el-button :icon="Search" @click="goWelfare">打开公益审核</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="lp-main">
      <el-tabs v-model="tab" class="lp-tabs" @tab-change="onTabChange">
        <el-tab-pane label="注册用户记分" name="users">
          <div class="toolbar">
            <div class="toolbar-filters">
              <el-input
                v-model="userQuery.keyword"
                class="filter-input"
                placeholder="用户名 / 姓名 / 手机 / 学号"
                clearable
                @keyup.enter="loadUsers"
                @clear="loadUsers"
              >
                <template #prefix>
                  <span class="input-hint">搜</span>
                </template>
              </el-input>
              <el-select
                v-model="userQuery.userType"
                class="filter-role"
                clearable
                placeholder="全部角色"
                @change="loadUsers"
              >
                <el-option label="学生" value="student" />
                <el-option label="管理员" value="admin" />
                <el-option label="导员" value="teacher" />
                <el-option label="检查员" value="service" />
                <el-option label="工作人员" value="worker" />
                <el-option label="临时" value="temporary" />
              </el-select>
              <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
              <el-button :icon="Refresh" @click="refreshUsers">重置并刷新</el-button>
            </div>
            <div class="toolbar-meta">
              <span class="meta-text">共 <b>{{ userPage.total }}</b> 人</span>
            </div>
          </div>

          <el-table
            :data="userRows"
            v-loading="usersLoading"
            stripe
            border
            class="lp-table"
            :header-cell-style="{ background: '#f8fafc', color: '#0f172a', fontWeight: '600' }"
            empty-text="暂无数据，请调整筛选或刷新"
          >
            <el-table-column label="剩余记分" width="132" fixed="left" align="center">
              <template #header>
                <el-tooltip content="满分 12 分，扣至 0 需关注" placement="top">
                  <span class="th-tip">剩余记分</span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                <div class="score-cell">
                  <div class="score-top">
                    <span class="score-num" :class="scoreNumClass(row)">{{ formatRem(row) }}</span>
                    <span class="score-denom">/ 12</span>
                  </div>
                  <div class="score-track">
                    <div class="score-fill" :class="scoreBarClass(row)" :style="{ width: scorePct(row) + '%' }" />
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="userId" label="用户ID" width="88" />
            <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />
            <el-table-column prop="realName" label="姓名" min-width="96" show-overflow-tooltip />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ roleLabel(row.userType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机" min-width="118" show-overflow-tooltip />
            <el-table-column prop="studentId" label="学号" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="92" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="168" fixed="right" align="center">
              <template #default="{ row }">
                <el-button-group class="op-btns">
                  <el-button type="primary" size="small" @click="jumpToUserLog(row)">流水</el-button>
                  <el-button type="success" size="small" plain @click="jumpToBonus(row)">加分</el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="userPage.pageNum"
              v-model:page-size="userPage.pageSize"
              :total="userPage.total"
              :page-sizes="[10, 15, 30, 50]"
              background
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadUsers"
              @current-change="loadUsers"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="查询流水" name="log">
          <div class="toolbar toolbar--wrap">
            <div class="toolbar-filters">
              <span class="field-label">用户 ID</span>
              <el-input-number v-model="logQuery.userId" :min="1" controls-position="right" class="log-uid" />
              <el-button type="primary" :icon="Search" @click="loadLog">查询</el-button>
              <el-button :icon="Refresh" @click="loadLog">刷新</el-button>
            </div>
          </div>
          <el-alert
            v-if="!logQuery.userId"
            type="info"
            :closable="false"
            show-icon
            class="lp-alert"
            title="请输入用户 ID 后查询该生的记分流水。"
          />
          <el-table
            v-else
            :data="logRows"
            v-loading="logLoading"
            stripe
            border
            class="lp-table"
            :header-cell-style="{ background: '#f8fafc', color: '#0f172a', fontWeight: '600' }"
            empty-text="暂无流水记录"
          >
            <el-table-column prop="logId" label="ID" width="72" />
            <el-table-column prop="userId" label="用户ID" width="88" />
            <el-table-column label="变动" width="96" align="center">
              <template #default="{ row }">
                <span class="delta" :class="deltaClass(row.delta)">{{ formatDelta(row.delta) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="变更后剩余" width="118" align="center" />
            <el-table-column prop="bizType" label="类型" min-width="140" show-overflow-tooltip />
            <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="172" />
          </el-table>
          <div v-if="logQuery.userId" class="pager">
            <el-pagination
              v-model:current-page="logPage.pageNum"
              v-model:page-size="logPage.pageSize"
              :total="logPage.total"
              background
              layout="total, prev, pager, next"
              @current-change="loadLog"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="公益审核" name="welfare">
          <div class="toolbar">
            <div class="toolbar-filters">
              <el-button type="primary" :icon="Refresh" @click="loadPending">刷新待审</el-button>
              <span class="meta-text">待处理：<b>{{ pendPage.total }}</b> 条</span>
            </div>
          </div>
          <el-table
            :data="pendingRows"
            v-loading="pendLoading"
            stripe
            border
            class="lp-table"
            :header-cell-style="{ background: '#f8fafc', color: '#0f172a', fontWeight: '600' }"
            empty-text="暂无待审申请"
          >
            <el-table-column prop="requestId" label="申请ID" width="88" />
            <el-table-column prop="userId" label="用户ID" width="88" />
            <el-table-column prop="content" label="说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="凭证" min-width="200">
              <template #default="{ row }">
                <div class="evidence-links">
                  <template v-if="evidenceList(row).length">
                    <el-link
                      v-for="(u, i) in evidenceList(row).slice(0, 3)"
                      :key="i"
                      type="primary"
                      :href="u"
                      target="_blank"
                      class="ev-link"
                    >
                      图{{ i + 1 }}
                    </el-link>
                    <span v-if="evidenceList(row).length > 3" class="more-ev">+{{ evidenceList(row).length - 3 }}</span>
                  </template>
                  <span v-else class="text-muted">—</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="172" />
            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="openReview(row, true)">通过</el-button>
                <el-button type="danger" size="small" plain @click="openReview(row, false)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="pendPage.pageNum"
              v-model:page-size="pendPage.pageSize"
              :total="pendPage.total"
              background
              layout="total, prev, pager, next"
              @current-change="loadPending"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="手动加分" name="bonus">
          <el-card shadow="never" class="inner-card">
            <template #header>
              <span class="inner-card-title">管理员手动加分</span>
              <span class="inner-card-sub">从「注册用户记分」点「加分」可自动带入用户 ID</span>
            </template>
            <el-form :model="bonusForm" label-position="top" class="form-grid">
              <el-row :gutter="20">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="用户 ID" required>
                    <el-input-number v-model="bonusForm.userId" :min="1" style="width: 100%" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="加分（1～12）" required>
                    <el-input-number v-model="bonusForm.points" :min="1" :max="12" style="width: 100%" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="备注">
                    <el-input v-model="bonusForm.remark" type="textarea" :rows="3" placeholder="加分原因说明" />
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="凭证 URL（可选）">
                    <el-input v-model="bonusForm.evidenceUrls" placeholder="多个地址可用逗号分隔" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="操作员 ID（可选）">
                    <el-input-number v-model="bonusForm.operatorUserId" :min="1" style="width: 100%" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item>
                    <el-button type="primary" size="large" :loading="bonusLoading" @click="submitBonus">提交加分</el-button>
                    <el-button @click="resetBonusForm">清空表单</el-button>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="删除违规恢复分" name="delv">
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            class="lp-alert"
            title="软删除违规记录，并按该条实际扣分恢复剩余记分（不超过 12）。请谨慎操作。"
          />
          <el-card shadow="never" class="inner-card inner-card--flat">
            <el-form :model="delForm" label-position="top" class="form-del">
              <el-row :gutter="20">
                <el-col :xs="24" :sm="8">
                  <el-form-item label="违规记录 ID" required>
                    <el-input-number v-model="delForm.illegalId" :min="1" style="width: 100%" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="8">
                  <el-form-item label="操作员 ID（可选）">
                    <el-input-number v-model="delForm.operatorUserId" :min="1" style="width: 100%" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="8">
                  <el-form-item label="原因（可选）">
                    <el-input v-model="delForm.reason" placeholder="备注" />
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item>
                    <el-button type="danger" :loading="delLoading" @click="submitDeleteViolation">确认删除违规并恢复分</el-button>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="reviewDlg.visible"
      :title="reviewDlg.approve ? '通过公益加分' : '拒绝申请'"
      width="480px"
      class="lp-dialog"
      destroy-on-close
    >
      <el-form v-if="reviewDlg.approve" label-position="top">
        <el-form-item label="加分分值">
          <el-input-number v-model="reviewDlg.points" :min="1" :max="12" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reviewDlg.remark" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="审核人 ID">
          <el-input-number v-model="reviewDlg.reviewerId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-form v-else label-position="top">
        <el-form-item label="拒绝原因 / 备注">
          <el-input v-model="reviewDlg.remark" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="审核人 ID">
          <el-input-number v-model="reviewDlg.reviewerId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="reviewDlg.loading" @click="confirmReview">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  getUserListForLicense,
  getLicenseLog,
  getWelfarePending,
  reviewWelfare,
  adminBonus,
  deleteViolationRestorePoints
} from '@/api/licensePoint'

const tab = ref('users')
const usersLoading = ref(false)
const userRows = ref<Record<string, unknown>[]>([])
const userQuery = reactive({ keyword: '', userType: '' as string })
const userPage = reactive({ pageNum: 1, pageSize: 15, total: 0 })
const logLoading = ref(false)
const logRows = ref<Record<string, unknown>[]>([])
const logQuery = reactive({ userId: undefined as number | undefined })
const logPage = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const pendLoading = ref(false)
const pendingRows = ref<Record<string, unknown>[]>([])
const pendPage = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const bonusLoading = ref(false)
const bonusForm = reactive({
  userId: undefined as number | undefined,
  points: 1,
  remark: '',
  evidenceUrls: '',
  operatorUserId: undefined as number | undefined
})

const delLoading = ref(false)
const delForm = reactive({
  illegalId: undefined as number | undefined,
  operatorUserId: undefined as number | undefined,
  reason: ''
})

const reviewDlg = reactive({
  visible: false,
  approve: true,
  row: null as Record<string, unknown> | null,
  points: 1,
  remark: '',
  reviewerId: undefined as number | undefined,
  loading: false
})

const ROLE_MAP: Record<string, string> = {
  admin: '管理员',
  teacher: '导员',
  service: '检查员',
  student: '学生',
  temporary: '临时',
  worker: '工作人员'
}

const roleLabel = (t: unknown) => ROLE_MAP[String(t)] || (t ? String(t) : '—')

const statusLabel = (s: unknown) =>
  ({ active: '正常', frozen: '已冻结', deleted: '已删除' }[String(s)] || (s ? String(s) : '—'))

const statusTagType = (s: unknown) => {
  const x = String(s)
  if (x === 'active') return 'success'
  if (x === 'frozen') return 'warning'
  return 'info'
}

const formatRem = (row: Record<string, unknown>) => {
  const v = row.licensePoints
  if (v === null || v === undefined) return 12
  const n = Number(v)
  return Number.isFinite(n) ? Math.max(0, Math.min(12, n)) : 12
}

const scorePct = (row: Record<string, unknown>) => Math.round((formatRem(row) / 12) * 100)

const scoreNumClass = (row: Record<string, unknown>) => {
  const r = formatRem(row)
  if (r <= 3) return 'num-danger'
  if (r <= 6) return 'num-warn'
  return 'num-ok'
}

const scoreBarClass = (row: Record<string, unknown>) => {
  const r = formatRem(row)
  if (r <= 3) return 'fill-danger'
  if (r <= 6) return 'fill-warn'
  return 'fill-ok'
}

const splitUrls = (raw: unknown): string[] => {
  if (raw == null) return []
  return String(raw)
    .split(/[,，;\n]+/)
    .map(s => s.trim())
    .filter(Boolean)
}

const evidenceList = (row: Record<string, unknown>) => splitUrls(row.imageUrls)

const formatDelta = (d: unknown) => {
  if (d === null || d === undefined) return '—'
  const n = Number(d)
  if (!Number.isFinite(n)) return String(d)
  if (n > 0) return `+${n}`
  return String(n)
}

const deltaClass = (d: unknown) => {
  const n = Number(d)
  if (!Number.isFinite(n)) return ''
  if (n > 0) return 'delta-up'
  if (n < 0) return 'delta-down'
  return ''
}

const refreshUsers = () => {
  userQuery.keyword = ''
  userQuery.userType = ''
  userPage.pageNum = 1
  loadUsers()
}

const goWelfare = () => {
  tab.value = 'welfare'
  loadPending()
}

const loadUsers = async () => {
  usersLoading.value = true
  try {
    const res: any = await getUserListForLicense({
      keyword: userQuery.keyword || undefined,
      userType: userQuery.userType || undefined,
      pageNum: userPage.pageNum,
      pageSize: userPage.pageSize
    })
    userRows.value = res.data || []
    userPage.total = Number(res.total) || 0
  } catch (e: any) {
    userRows.value = []
    ElMessage.error(e?.message || '加载用户失败')
  } finally {
    usersLoading.value = false
  }
}

const jumpToUserLog = (row: Record<string, unknown>) => {
  logQuery.userId = row.userId as number
  tab.value = 'log'
  loadLog()
}

const jumpToBonus = (row: Record<string, unknown>) => {
  bonusForm.userId = row.userId as number
  tab.value = 'bonus'
}

const resetBonusForm = () => {
  bonusForm.userId = undefined
  bonusForm.points = 1
  bonusForm.remark = ''
  bonusForm.evidenceUrls = ''
  bonusForm.operatorUserId = undefined
}

const onTabChange = (name: string | number) => {
  const n = String(name)
  if (n === 'users') loadUsers()
  else if (n === 'welfare') loadPending()
  else if (n === 'log' && logQuery.userId) loadLog()
}

const loadLog = async () => {
  if (logQuery.userId == null || logQuery.userId < 1) {
    logRows.value = []
    logPage.total = 0
    return
  }
  logLoading.value = true
  try {
    const res: any = await getLicenseLog({
      userId: logQuery.userId,
      pageNum: logPage.pageNum,
      pageSize: logPage.pageSize
    })
    logRows.value = res.data || []
    logPage.total = Number(res.total) || 0
  } catch (e: any) {
    logRows.value = []
    ElMessage.error(e?.message || '加载失败')
  } finally {
    logLoading.value = false
  }
}

const loadPending = async () => {
  pendLoading.value = true
  try {
    const res: any = await getWelfarePending({
      pageNum: pendPage.pageNum,
      pageSize: pendPage.pageSize
    })
    pendingRows.value = res.data || []
    pendPage.total = Number(res.total) || 0
  } catch (e: any) {
    pendingRows.value = []
    ElMessage.error(e?.message || '加载失败')
  } finally {
    pendLoading.value = false
  }
}

const openReview = (row: Record<string, unknown>, approve: boolean) => {
  reviewDlg.row = row
  reviewDlg.approve = approve
  reviewDlg.points = 1
  reviewDlg.remark = ''
  reviewDlg.reviewerId = undefined
  reviewDlg.visible = true
}

const confirmReview = async () => {
  const row = reviewDlg.row
  if (!row?.requestId) return
  reviewDlg.loading = true
  try {
    await reviewWelfare({
      requestId: row.requestId as number,
      approve: reviewDlg.approve,
      points: reviewDlg.approve ? reviewDlg.points : 0,
      adminRemark: reviewDlg.remark,
      reviewerId: reviewDlg.reviewerId
    })
    ElMessage.success('已处理')
    reviewDlg.visible = false
    loadPending()
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message || '失败')
  } finally {
    reviewDlg.loading = false
  }
}

const submitBonus = async () => {
  if (!bonusForm.userId) {
    ElMessage.warning('请填写用户ID')
    return
  }
  bonusLoading.value = true
  try {
    await adminBonus({
      userId: bonusForm.userId,
      points: bonusForm.points,
      remark: bonusForm.remark,
      evidenceUrls: bonusForm.evidenceUrls || undefined,
      operatorUserId: bonusForm.operatorUserId
    })
    ElMessage.success('已加分')
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message || '失败')
  } finally {
    bonusLoading.value = false
  }
}

const submitDeleteViolation = async () => {
  if (!delForm.illegalId) {
    ElMessage.warning('请填写违规ID')
    return
  }
  try {
    await ElMessageBox.confirm('确定软删该违规并尝试恢复记分？', '确认', { type: 'warning' })
  } catch {
    return
  }
  delLoading.value = true
  try {
    await deleteViolationRestorePoints(
      delForm.illegalId,
      delForm.operatorUserId,
      delForm.reason || undefined
    )
    ElMessage.success('已删除')
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message || '失败')
  } finally {
    delLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
  loadPending()
})
</script>

<style scoped>
.lp-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 12px 12px 24px;
  box-sizing: border-box;
}

.lp-hero {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #fff;
  box-shadow: none;
}

.lp-hero-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px 24px;
}

.lp-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #0f172a;
}

.lp-lead {
  margin: 8px 0 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
  max-width: 640px;
}

.lp-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.lp-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.lp-main {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.lp-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.lp-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.lp-tabs :deep(.el-tabs__item.is-active) {
  color: #0f172a;
  font-weight: 600;
}

.lp-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--el-color-primary);
}

.lp-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #e2e8f0;
}

.lp-table :deep(.el-table__cell) {
  font-size: 14px;
  color: #334155;
}

.lp-table :deep(th.el-table__cell) {
  color: #0f172a;
  font-weight: 600;
  font-size: 13px;
}

.lp-table :deep(.el-table__empty-text) {
  color: #64748b;
  font-size: 14px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 16px;
  margin-bottom: 16px;
  padding: 4px 0 8px;
}

.toolbar--wrap {
  justify-content: flex-start;
}

.toolbar-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.filter-input {
  width: 260px;
  max-width: 100%;
}

.filter-role {
  width: 140px;
}

.input-hint {
  font-size: 12px;
  color: #64748b;
  padding-left: 4px;
}

.toolbar-meta .meta-text,
.meta-text {
  font-size: 13px;
  color: #475569;
}

.meta-text b {
  color: var(--el-color-primary);
  font-weight: 600;
}

.field-label {
  font-size: 13px;
  color: #334155;
  margin-right: 4px;
}

.log-uid {
  width: 160px;
}

.lp-table {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.th-tip {
  cursor: help;
  border-bottom: 1px dashed #94a3b8;
  color: #0f172a;
}

.score-cell {
  padding: 4px 0;
  min-width: 0;
}

.score-top {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 6px;
}

.score-num {
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.score-num.num-ok {
  color: var(--el-color-success);
}

.score-num.num-warn {
  color: var(--el-color-warning);
}

.score-num.num-danger {
  color: var(--el-color-danger);
}

.score-denom {
  font-size: 12px;
  color: #64748b;
}

.score-track {
  height: 6px;
  border-radius: 3px;
  background: #e2e8f0;
  overflow: hidden;
  max-width: 96px;
  margin: 0 auto;
}

.score-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.25s ease;
}

.score-fill.fill-ok {
  background: linear-gradient(90deg, var(--el-color-success-light-5), var(--el-color-success));
}

.score-fill.fill-warn {
  background: linear-gradient(90deg, var(--el-color-warning-light-5), var(--el-color-warning));
}

.score-fill.fill-danger {
  background: linear-gradient(90deg, var(--el-color-danger-light-5), var(--el-color-danger));
}

.op-btns {
  flex-wrap: nowrap;
}

.op-btns :deep(.el-button) {
  padding: 5px 12px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.lp-alert {
  margin-bottom: 14px;
}

.delta {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.delta-up {
  color: var(--el-color-success);
}

.delta-down {
  color: var(--el-color-danger);
}

.evidence-links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.ev-link {
  font-size: 13px;
}

.more-ev {
  font-size: 12px;
  color: #64748b;
}

.text-muted {
  color: #94a3b8;
}

.inner-card {
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.inner-card--flat {
  border-style: dashed;
}

.inner-card :deep(.el-card__header) {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 14px 18px;
}

.inner-card-title {
  font-weight: 600;
  font-size: 15px;
  color: #0f172a;
}

.inner-card-sub {
  font-size: 13px;
  color: #64748b;
  font-weight: normal;
}

.form-grid :deep(.el-form-item__label),
.form-del :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 500;
  font-size: 14px;
}

.lp-dialog :deep(.el-dialog__title) {
  color: #0f172a;
  font-weight: 600;
}

.lp-dialog :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 500;
}

.form-grid {
  max-width: 720px;
}

.form-del {
  max-width: 900px;
}

@media (max-width: 768px) {
  .lp-page {
    padding: 12px 12px 24px;
  }

  .filter-input {
    width: 100%;
  }

  .filter-role {
    width: 100%;
  }
}
</style>

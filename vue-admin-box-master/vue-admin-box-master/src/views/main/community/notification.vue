<template>
  <div class="notification-page page-bg-shell">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h2 class="title">系统通知管理</h2>
          <p class="sub">对接 /api/notification 全部接口：发送、分页、统计、详情、删除及个人/批量通道</p>
        </div>
        <el-button type="primary" :icon="Refresh" @click="refreshAll">刷新</el-button>
      </div>

      <el-row :gutter="16" class="stat-row" v-loading="statsLoading">
        <el-col :xs="12" :sm="6">
          <div class="stat-box">
            <span class="stat-num">{{ stats.total ?? '—' }}</span>
            <span class="stat-label">通知总数</span>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-box ok">
            <span class="stat-num">{{ stats.sent ?? '—' }}</span>
            <span class="stat-label">已发送</span>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-box draft">
            <span class="stat-num">{{ stats.draft ?? '—' }}</span>
            <span class="stat-label">草稿</span>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-box">
            <span class="stat-num">{{ stats.deliveryRate ?? '—' }}</span>
            <span class="stat-label">送达率(统计)</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="main-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <span>通知列表（GET /notification/list）</span>
          </template>
          <el-form :inline="true" class="filter-form" @submit.prevent>
            <el-form-item label="类型">
              <el-input v-model="query.type" placeholder="如 system" clearable style="width: 120px" />
            </el-form-item>
            <el-form-item label="紧急">
              <el-select v-model="query.isUrgent" placeholder="全部" clearable style="width: 100px">
                <el-option label="是" :value="true" />
                <el-option label="否" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-input v-model="query.status" placeholder="如 sent" clearable style="width: 110px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="tableData" border stripe v-loading="listLoading" style="width: 100%">
            <el-table-column prop="notificationId" label="ID" width="72" align="center" />
            <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column label="紧急" width="72" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isUrgent" type="danger" size="small">是</el-tag>
                <span v-else class="muted">否</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="88" show-overflow-tooltip />
            <el-table-column prop="receiverType" label="接收方式" width="100" show-overflow-tooltip />
            <el-table-column prop="sendTime" label="发送时间" width="170" />
            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
                <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty v-if="!listLoading" description="暂无通知数据，可先发送一条或检查后端 /api/notification/list" />
            </template>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="pagination.pageNum"
              v-model:page-size="pagination.pageSize"
              :total="pagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadList"
              @current-change="loadList"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="send-card">
          <template #header>
            <span>发送通知（POST /notification/send）</span>
          </template>
          <el-form :model="sendForm" label-width="100px" class="send-form">
            <el-form-item label="标题" required>
              <el-input v-model="sendForm.title" placeholder="通知标题" />
            </el-form-item>
            <el-form-item label="内容" required>
              <el-input v-model="sendForm.content" type="textarea" :rows="4" placeholder="正文" />
            </el-form-item>
            <el-form-item label="类型" required>
              <el-input v-model="sendForm.type" placeholder="如 system、announcement" />
            </el-form-item>
            <el-form-item label="紧急">
              <el-switch v-model="sendForm.isUrgent" />
            </el-form-item>
            <el-form-item label="接收类型" required>
              <el-select v-model="sendForm.receiverType" placeholder="选择" style="width: 100%">
                <el-option label="全体用户 (all)" value="all" />
                <el-option label="指定用户ID (specified)" value="specified" />
                <el-option label="按班级名 (class)" value="class" />
              </el-select>
            </el-form-item>
            <el-form-item
              v-if="sendForm.receiverType === 'specified' || sendForm.receiverType === 'class'"
              label="接收列表"
            >
              <el-input
                v-model="sendForm.receiverIdsText"
                type="textarea"
                :rows="3"
                :placeholder="
                  sendForm.receiverType === 'class'
                    ? '班级名称，逗号分隔，如：计算机2101,软件工程2202'
                    : '用户ID，逗号分隔，如：1,2,3'
                "
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="sendLoading" @click="submitSend">发送</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="mt16">
          <template #header>
            <span>快捷通道</span>
          </template>
          <el-form label-width="100px" size="small">
            <el-divider content-position="left">个人通知 POST /send-personal</el-divider>
            <el-form-item label="标题">
              <el-input v-model="quickPersonal.title" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="quickPersonal.content" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="用户ID">
              <el-input-number v-model="quickPersonal.receiverId" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" plain :loading="quickPersonal.loading" @click="doSendPersonal">
                发送个人通知
              </el-button>
            </el-form-item>

            <el-divider content-position="left">批量用户 POST /send-batch</el-divider>
            <el-form-item label="标题">
              <el-input v-model="quickBatch.title" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="quickBatch.content" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="用户ID列表">
              <el-input v-model="quickBatch.idsText" placeholder="逗号分隔，如 1,2,3" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" plain :loading="quickBatch.loading" @click="doSendBatch">批量发送</el-button>
            </el-form-item>

            <el-divider content-position="left">测试已读/隐藏（需真实 userId）</el-divider>
            <el-form-item label="通知ID">
              <el-input-number v-model="inboxTest.notificationId" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="用户ID">
              <el-input-number v-model="inboxTest.userId" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item>
              <el-button @click="doMarkRead">标记已读</el-button>
              <el-button @click="doHide">从收件箱移除</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" title="通知详情" width="560px" destroy-on-close>
      <div v-if="detailRow" class="detail-body">
        <p><b>标题：</b>{{ detailRow.title }}</p>
        <p><b>类型：</b>{{ detailRow.type }} / <b>状态：</b>{{ detailRow.status }}</p>
        <p><b>接收类型：</b>{{ detailRow.receiverType }}</p>
        <p><b>接收范围：</b>{{ detailRow.receiverIds || '—' }}</p>
        <p><b>发送时间：</b>{{ detailRow.sendTime }}</p>
        <el-divider />
        <div class="content-block">{{ detailRow.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { AxiosError } from 'axios'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getNotificationList,
  getNotificationStats,
  getNotificationDetail,
  sendNotification,
  sendPersonalNotification,
  sendBatchNotification,
  markNotificationRead,
  hideNotificationForUser,
  deleteNotification
} from '@/api/notification'

const stats = reactive({
  total: null as number | null,
  sent: null as number | null,
  draft: null as number | null,
  deliveryRate: '' as string
})
const statsLoading = ref(false)
const tableData = ref<Record<string, unknown>[]>([])
const listLoading = ref(false)
const sendLoading = ref(false)
const detailVisible = ref(false)
const detailRow = ref<Record<string, unknown> | null>(null)

const query = reactive({
  type: '',
  isUrgent: undefined as boolean | undefined,
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const sendForm = reactive({
  title: '',
  content: '',
  type: 'system',
  isUrgent: false,
  receiverType: 'all',
  receiverIdsText: ''
})

const quickPersonal = reactive({
  title: '系统提醒',
  content: '',
  receiverId: 1,
  loading: false
})

const quickBatch = reactive({
  title: '批量提醒',
  content: '',
  idsText: '',
  loading: false
})

const inboxTest = reactive({
  notificationId: 1,
  userId: 1
})

const parseIds = (text: string): string[] => {
  if (!text || !text.trim()) return []
  return text
    .split(/[,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
}

const errText = (e: unknown, fallback: string) => {
  const ax = e as AxiosError<{ message?: string; msg?: string }>
  const d = ax.response?.data
  return (d && (d.message || d.msg)) || ax.message || fallback
}

/** 兼容 PageResponse：data 为数组，或嵌套 records/list */
const extractPageRows = (res: unknown): Record<string, unknown>[] => {
  const r = res as Record<string, unknown>
  let chunk = r?.data
  if (chunk && typeof chunk === 'object' && !Array.isArray(chunk)) {
    const o = chunk as Record<string, unknown>
    if (Array.isArray(o.records)) chunk = o.records
    else if (Array.isArray(o.list)) chunk = o.list
  }
  return Array.isArray(chunk) ? (chunk as Record<string, unknown>[]) : []
}

/** 统一行字段（含 snake_case） */
const normalizeNotificationRow = (row: Record<string, unknown>) => {
  const id = row.notificationId ?? row.notification_id
  return {
    ...row,
    notificationId: typeof id === 'number' ? id : Number(id),
    title: row.title,
    type: row.type,
    isUrgent: row.isUrgent ?? row.is_urgent,
    receiverType: row.receiverType ?? row.receiver_type,
    receiverIds: row.receiverIds ?? row.receiver_ids,
    sendTime: row.sendTime ?? row.send_time,
    status: row.status,
    content: row.content
  }
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getNotificationStats()
    let d = (res as { data?: Record<string, unknown> }).data || {}
    if (d && typeof d === 'object' && 'data' in d && typeof (d as { data: unknown }).data === 'object') {
      d = (d as { data: Record<string, unknown> }).data
    }
    stats.total = (d.total as number) ?? null
    stats.sent = (d.sent as number) ?? null
    stats.draft = (d.draft as number) ?? null
    stats.deliveryRate = String(d.deliveryRate ?? d.delivery_rate ?? '—')
  } catch (e) {
    stats.total = stats.sent = stats.draft = null
    stats.deliveryRate = '—'
    ElMessage.error('统计加载失败：' + errText(e, '网络或服务异常'))
  } finally {
    statsLoading.value = false
  }
}

const loadList = async () => {
  listLoading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (query.type) params.type = query.type
    if (query.isUrgent !== undefined && query.isUrgent !== null) params.isUrgent = query.isUrgent
    if (query.status) params.status = query.status
    const res = (await getNotificationList(params)) as {
      data?: unknown
      total?: number
      pageNum?: number
      pageSize?: number
    }
    const rows = extractPageRows(res).map(normalizeNotificationRow)
    tableData.value = rows
    pagination.total = Number(res.total) || 0
    if (res.pageNum) pagination.pageNum = res.pageNum
    if (res.pageSize) pagination.pageSize = res.pageSize
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    ElMessage.error('列表加载失败：' + errText(e, '请确认已登录且代理 /api 指向后端'))
  } finally {
    listLoading.value = false
  }
}

const resetQuery = () => {
  query.type = ''
  query.isUrgent = undefined
  query.status = ''
  pagination.pageNum = 1
  loadList()
}

const openDetail = async (row: Record<string, unknown>) => {
  const id = Number(row.notificationId)
  if (!Number.isFinite(id)) {
    ElMessage.error('无效的通知 ID')
    return
  }
  try {
    const res = await getNotificationDetail(id)
    const raw = (res as { data?: Record<string, unknown> }).data || row
    detailRow.value = normalizeNotificationRow(raw as Record<string, unknown>) as Record<string, unknown>
    detailVisible.value = true
  } catch (e) {
    detailRow.value = normalizeNotificationRow(row) as Record<string, unknown>
    detailVisible.value = true
    ElMessage.warning('详情接口失败，已显示列表行数据：' + errText(e, ''))
  }
}

const onDelete = async (row: Record<string, unknown>) => {
  const id = Number(row.notificationId)
  if (!Number.isFinite(id)) {
    ElMessage.error('无效的通知 ID')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除通知 #${id} ？`, '删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = (await deleteNotification(id)) as { data?: boolean }
    if (res?.data !== true) {
      ElMessage.error('删除失败：后端未删除成功（记录可能不存在）')
      return
    }
    ElMessage.success('已删除')
    loadList()
    loadStats()
  } catch (e) {
    ElMessage.error('删除失败：' + errText(e, '请求异常'))
  }
}

const submitSend = async () => {
  if (!sendForm.title.trim() || !sendForm.content.trim() || !sendForm.type.trim()) {
    ElMessage.warning('请填写标题、内容与类型')
    return
  }
  const receiverType = sendForm.receiverType
  let receiverIds: string[] | undefined
  if (receiverType === 'specified' || receiverType === 'class') {
    receiverIds = parseIds(sendForm.receiverIdsText)
    if (!receiverIds.length) {
      ElMessage.warning('请填写接收列表')
      return
    }
  }
  sendLoading.value = true
  try {
    const sendRes = (await sendNotification({
      title: sendForm.title.trim(),
      content: sendForm.content.trim(),
      type: sendForm.type.trim(),
      isUrgent: sendForm.isUrgent,
      receiverType,
      receiverIds
    })) as { data?: boolean }
    if (sendRes?.data !== true) {
      ElMessage.error('发送失败：后端返回未成功')
      return
    }
    ElMessage.success('已发送')
    loadList()
    loadStats()
  } catch (e) {
    ElMessage.error('发送失败：' + errText(e, '请求异常'))
  } finally {
    sendLoading.value = false
  }
}

const doSendPersonal = async () => {
  if (!quickPersonal.title.trim() || !quickPersonal.content.trim()) {
    ElMessage.warning('请填写标题与内容')
    return
  }
  quickPersonal.loading = true
  try {
    const r = (await sendPersonalNotification(
      quickPersonal.title.trim(),
      quickPersonal.content.trim(),
      quickPersonal.receiverId
    )) as { data?: boolean }
    if (r?.data !== true) {
      ElMessage.error('个人通知发送未成功')
      return
    }
    ElMessage.success('个人通知已发送')
    loadList()
    loadStats()
  } catch (e) {
    ElMessage.error('发送失败：' + errText(e, '请求异常'))
  } finally {
    quickPersonal.loading = false
  }
}

const doSendBatch = async () => {
  if (!quickBatch.title.trim() || !quickBatch.content.trim()) {
    ElMessage.warning('请填写标题与内容')
    return
  }
  const nums = parseIds(quickBatch.idsText)
    .map((s) => parseInt(s, 10))
    .filter((n) => Number.isFinite(n) && n > 0)
  if (!nums.length) {
    ElMessage.warning('请填写有效用户ID')
    return
  }
  quickBatch.loading = true
  try {
    const r = (await sendBatchNotification(
      quickBatch.title.trim(),
      quickBatch.content.trim(),
      nums
    )) as { data?: boolean }
    if (r?.data !== true) {
      ElMessage.error('批量发送未成功')
      return
    }
    ElMessage.success('批量通知已发送')
    loadList()
    loadStats()
  } catch (e) {
    ElMessage.error('发送失败：' + errText(e, '请求异常'))
  } finally {
    quickBatch.loading = false
  }
}

const doMarkRead = async () => {
  try {
    const r = (await markNotificationRead(inboxTest.notificationId, inboxTest.userId)) as { data?: boolean }
    if (r?.data !== true) {
      ElMessage.error('标记已读未成功')
      return
    }
    ElMessage.success('已标记已读')
  } catch (e) {
    ElMessage.error('操作失败：' + errText(e, '请求异常'))
  }
}

const doHide = async () => {
  try {
    const r = (await hideNotificationForUser(inboxTest.notificationId, inboxTest.userId)) as { data?: boolean }
    if (r?.data !== true) {
      ElMessage.error('移除未成功')
      return
    }
    ElMessage.success('已从该用户收件箱移除展示')
  } catch (e) {
    ElMessage.error('操作失败：' + errText(e, '请求异常'))
  }
}

const refreshAll = () => {
  loadStats()
  loadList()
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.notification-page {
  padding: 12px;
}
.header-card {
  margin-bottom: 16px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 20px;
}
.sub {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}
.stat-row {
  margin-top: 8px;
}
.stat-box {
  background: #f8fafc;
  border-radius: 10px;
  padding: 14px 16px;
  text-align: center;
  border: 1px solid #e2e8f0;
}
.stat-box.ok {
  background: #ecfdf5;
  border-color: #a7f3d0;
}
.stat-box.draft {
  background: #fffbeb;
  border-color: #fde68a;
}
.stat-num {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.stat-label {
  font-size: 12px;
  color: #64748b;
}
.main-row {
  align-items: flex-start;
}
.filter-form {
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.send-card {
  margin-bottom: 0;
}
.send-form {
  max-width: 100%;
}
.mt16 {
  margin-top: 16px;
}
.muted {
  color: #94a3b8;
  font-size: 12px;
}
.detail-body p {
  margin: 6px 0;
  font-size: 14px;
  color: #334155;
}
.content-block {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #1e293b;
}
</style>

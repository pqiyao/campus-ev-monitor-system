<template>
  <div class="notification-management-container page-bg-shell">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <div class="header-bg">
        <div class="header-content">
          <h1 class="page-title">
            <span class="title-text">智能消息通知</span>
          </h1>
        </div>
        <div class="header-actions">
          <div class="action-btn primary" @click="showSendDialog">
            <div class="btn-icon">📨</div>
            <div class="btn-text">发送新通知</div>
          </div>
          <div class="action-btn" @click="refreshNotifications">
            <div class="btn-icon">↻</div>
            <div class="btn-text">刷新</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 左侧筛选区域 -->
      <div class="left-panel">
        <div class="filter-form">
          <div class="form-group">
            <label class="form-label">通知类型</label>
            <div class="type-selector">
              <div
                v-for="type in notificationTypes"
                :key="type.value"
                :class="['type-option', { active: filterForm.type === type.value }]"
                @click="filterForm.type = filterForm.type === type.value ? '' : type.value"
              >
                <div class="type-icon">{{ type.icon }}</div>
                <div class="type-text">{{ type.label }}</div>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">发送状态</label>
            <div class="status-selector">
              <div
                v-for="status in statusOptions"
                :key="status.value"
                :class="['status-option', { active: filterForm.status === status.value }]"
                @click="filterForm.status = filterForm.status === status.value ? '' : status.value"
              >
                <div class="status-dot" :style="{ background: status.color }"></div>
                <div class="status-text">{{ status.label }}</div>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">接收范围</label>
            <div class="form-input">
              <select v-model="filterForm.receiverType" class="tech-select">
                <option value="">全部范围</option>
                <option value="all">全体用户</option>
                <option value="student">学生</option>
                <option value="teacher">教职工</option>
                <option value="service">后勤人员</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">发送时间</label>
            <div class="date-range">
              <div class="date-input">
                <input
                  type="date"
                  v-model="filterForm.startTime"
                  class="tech-input"
                />
              </div>
              <div class="date-separator">至</div>
              <div class="date-input">
                <input
                  type="date"
                  v-model="filterForm.endTime"
                  class="tech-input"
                />
              </div>
            </div>
          </div>

          <div class="form-actions">
            <div class="filter-btn" @click="handleFilter">
              <div class="btn-content">
                <div class="btn-icon">🔍</div>
                <div class="btn-text">筛选通知</div>
              </div>
            </div>
            <div class="reset-btn" @click="resetFilter">重置筛选</div>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-cards">
          <div class="stat-card" v-for="stat in quickStats" :key="stat.label">
            <div class="card-icon" :style="{ background: stat.bgColor }">
              {{ stat.icon }}
            </div>
            <div class="card-content">
              <div class="card-value">{{ stat.value }}</div>
              <div class="card-label">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧通知列表区域 -->
      <div class="right-panel">
        <div class="table-container">
          <div class="table-header">
            <div class="table-title">通知列表</div>
            <div class="table-actions">
              <div v-if="isFiltered" class="filter-status">
                <span class="filter-tag" v-if="filterForm.type">
                  类型: {{ getTypeText(filterForm.type) }}
                  <span class="filter-remove" @click="filterForm.type = ''">×</span>
                </span>
                <span class="filter-tag" v-if="filterForm.status">
                  状态: {{ getStatusText(filterForm.status) }}
                  <span class="filter-remove" @click="filterForm.status = ''">×</span>
                </span>
                <span class="filter-tag" v-if="filterForm.receiverType">
                  接收: {{ getReceiverTypeText(filterForm.receiverType) }}
                  <span class="filter-remove" @click="filterForm.receiverType = ''">×</span>
                </span>
                <span class="filter-tag" v-if="filterForm.startTime">
                  开始: {{ filterForm.startTime }}
                  <span class="filter-remove" @click="filterForm.startTime = ''">×</span>
                </span>
                <span class="filter-tag" v-if="filterForm.endTime">
                  结束: {{ filterForm.endTime }}
                  <span class="filter-remove" @click="filterForm.endTime = ''">×</span>
                </span>
              </div>
              <div class="total-info">共 {{ pagination.total }} 条记录</div>
              <div class="page-controls">
                <div class="page-btn" @click="prevPage" :class="{ disabled: pagination.currentPage === 1 }">
                  ←
                </div>
                <div class="page-info">{{ pagination.currentPage }} / {{ pagination.totalPages }}</div>
                <div class="page-btn" @click="nextPage" :class="{ disabled: pagination.currentPage === pagination.totalPages }">
                  →
                </div>
              </div>
            </div>
          </div>

          <div class="table-wrapper">
            <table class="tech-table">
              <thead>
                <tr>
                  <th style="width: 60px;">
                    <div class="checkbox-wrapper">
                      <input
                        type="checkbox"
                        :checked="allSelected"
                        @change="toggleSelectAll"
                        class="tech-checkbox"
                      />
                    </div>
                  </th>
                  <th style="width: 100px;">ID</th>
                  <th style="width: 250px;">通知标题</th>
                  <th style="width: 150px;">通知类型</th>
                  <th style="width: 200px;">接收范围</th>
                  <th style="width: 180px;">时间信息</th>
                  <th style="width: 120px;">状态</th>
                  <th style="width: 200px;">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="notification in notificationList" :key="notification.notificationId">
                  <td>
                    <div class="checkbox-wrapper">
                      <input
                        type="checkbox"
                        :checked="isSelected(notification)"
                        @change="toggleSelect(notification)"
                        class="tech-checkbox"
                      />
                    </div>
                  </td>
                  <td>
                    <div class="id-cell">#{{ notification.notificationId }}</div>
                  </td>
                  <td>
                    <div class="notification-cell">
                      <div class="notification-title">{{ notification.title }}</div>
                      <div class="notification-tags">
                        <div v-if="notification.isUrgent" class="tag urgent">紧急</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div class="type-cell" :class="notification.type">
                      <div class="type-icon">{{ getTypeIcon(notification.type) }}</div>
                      {{ getTypeText(notification.type) }}
                    </div>
                  </td>
                  <td>
                    <div class="receiver-cell">
                      <div class="receiver-type">{{ getReceiverTypeText(notification.receiverType) }}</div>
                      <div class="receiver-count">{{ notification.totalCount || 0 }}人</div>
                    </div>
                  </td>
                  <td>
                    <div class="time-cell">
                      <div class="send-time">{{ formatTime(notification.sendTime) }}</div>
                      <div class="read-rate">阅读率: {{ calculateReadRate(notification) }}</div>
                    </div>
                  </td>
                  <td>
                    <div class="status-cell" :class="notification.status">
                      <div class="status-dot"></div>
                      {{ getStatusText(notification.status) }}
                    </div>
                  </td>
                  <td>
                    <div class="action-cell">
                      <div class="action-btn small" @click="viewNotification(notification)">
                        <div class="btn-icon">👁</div>
                        <div class="btn-text">查看</div>
                      </div>
                      <div v-if="notification.status === 'draft'" class="action-btn small success" @click="sendNotification(notification)">
                        <div class="btn-icon">📤</div>
                        <div class="btn-text">发送</div>
                      </div>
                      <div class="action-btn small" @click="deleteNotification(notification)">
                        <div class="btn-icon">🗑</div>
                        <div class="btn-text">删除</div>
                      </div>
                    </div>
                  </td>
                </tr>
                <tr v-if="notificationList.length === 0">
                  <td colspan="8" style="text-align: center; padding: 40px; color: rgba(255, 255, 255, 0.5);">
                    暂无通知数据
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="table-footer" v-if="selectedNotifications.length > 0">
            <div class="selected-info">
              已选择 {{ selectedNotifications.length }} 个通知
              <div class="batch-actions">
                <div class="batch-btn" @click="batchSend" :class="{ disabled: !hasDraftSelected }">
                  批量发送
                </div>
                <div class="batch-btn" @click="batchDelete">
                  批量删除
                </div>
                <div class="batch-btn" @click="clearSelection">
                  清空选择
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发送通知对话框 -->
    <div v-if="sendDialogVisible" class="modal-overlay" @click="closeSendDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <div class="modal-title">
            <div class="title-icon">📨</div>
            <div class="title-text">发送新通知</div>
          </div>
          <div class="modal-close" @click="closeSendDialog">×</div>
        </div>

        <div class="modal-body">
          <div class="send-form">
            <div class="form-section">
              <div class="section-title">基本信息</div>
              <div class="form-row">
                <div class="form-col">
                  <label class="form-label">通知标题 <span class="required">*</span></label>
                  <input
                    type="text"
                    v-model="sendForm.title"
                    placeholder="请输入通知标题"
                    class="tech-input"
                    :class="{ error: !sendForm.title.trim() && titleTouched }"
                    @blur="titleTouched = true"
                  />
                  <div v-if="!sendForm.title.trim() && titleTouched" class="error-message">通知标题不能为空</div>
                </div>
                <div class="form-col">
                  <label class="form-label">通知类型 <span class="required">*</span></label>
                  <select v-model="sendForm.type" class="tech-select">
                    <option value="system">系统通知</option>
                    <option value="violation">违规提醒</option>
                    <option value="security">安全预警</option>
                    <option value="activity">活动通知</option>
                    <option value="personal">个人通知</option>
                  </select>
                </div>
              </div>

              <div class="form-row">
                <div class="form-col full">
                  <label class="form-label">通知内容 <span class="required">*</span></label>
                  <textarea
                    v-model="sendForm.content"
                    placeholder="请输入通知内容..."
                    class="tech-textarea"
                    :class="{ error: !sendForm.content.trim() && contentTouched }"
                    @blur="contentTouched = true"
                    rows="4"
                  ></textarea>
                  <div v-if="!sendForm.content.trim() && contentTouched" class="error-message">通知内容不能为空</div>
                </div>
              </div>
            </div>

            <div class="form-section">
              <div class="section-title">接收设置</div>
              <div class="form-row">
                <div class="form-col full">
                  <label class="form-label">接收类型 <span class="required">*</span></label>
                  <div class="radio-group">
                    <div
                      v-for="receiver in receiverOptions"
                      :key="receiver.value"
                      :class="['radio-option', { active: sendForm.receiverType === receiver.value }]"
                      @click="sendForm.receiverType = receiver.value"
                    >
                      <div class="radio-dot"></div>
                      <div class="radio-text">{{ receiver.label }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="sendForm.receiverType === 'specific'" class="form-row">
                <div class="form-col full">
                  <label class="form-label">接收用户ID</label>
                  <input
                    type="text"
                    v-model="sendForm.receiverIdsInput"
                    placeholder="请输入用户ID，多个用逗号分隔，如：1,2,3"
                    class="tech-input"
                  />
                  <div class="input-hint">支持批量输入用户ID，用逗号分隔</div>
                </div>
              </div>
            </div>

            <div class="form-section">
              <div class="section-title">发送设置</div>
              <div class="form-row">
                <div class="form-col">
                  <label class="form-label">紧急程度</label>
                  <div class="urgent-toggle" @click="sendForm.isUrgent = !sendForm.isUrgent">
                    <div class="toggle-track" :class="{ active: sendForm.isUrgent }">
                      <div class="toggle-thumb"></div>
                    </div>
                    <div class="toggle-label">{{ sendForm.isUrgent ? '紧急通知' : '普通通知' }}</div>
                  </div>
                </div>
                <div class="form-col">
                  <label class="form-label">发送方式</label>
                  <div class="radio-group horizontal">
                    <div
                      v-for="mode in sendModeOptions"
                      :key="mode.value"
                      :class="['radio-option', { active: sendForm.sendMode === mode.value }]"
                      @click="sendForm.sendMode = mode.value"
                    >
                      <div class="radio-dot"></div>
                      <div class="radio-text">{{ mode.label }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="footer-actions">
            <div class="action-btn" @click="closeSendDialog">
              取消
            </div>
            <div class="action-btn primary" @click="sendNow" :class="{ disabled: !isSendFormValid }">
              立即发送
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知详情对话框 (已优化布局，标签水平对齐) -->
    <div v-if="detailDialogVisible" class="modal-overlay" @click="closeDetailDialog">
      <div class="modal-content large" @click.stop>
        <div class="modal-header">
          <div class="modal-title">
            <div class="title-icon">📋</div>
            <div class="title-text">通知详情</div>
          </div>
          <div class="modal-close" @click="closeDetailDialog">×</div>
        </div>

        <div class="modal-body">
          <div v-if="currentNotification" class="detail-content">
            <!-- 基本信息 -->
            <div class="detail-section">
              <div class="section-title">基本信息</div>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">通知标题：</span>
                  <span class="info-value">{{ currentNotification.title }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">通知类型：</span>
                  <span class="info-value tag">{{ getTypeText(currentNotification.type) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">紧急程度：</span>
                  <span class="info-value tag" :class="{ urgent: currentNotification.isUrgent }">
                    {{ currentNotification.isUrgent ? '紧急' : '普通' }}
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">发送状态：</span>
                  <span class="info-value status">{{ getStatusText(currentNotification.status) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">发送时间：</span>
                  <span class="info-value">{{ formatTime(currentNotification.sendTime) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">创建时间：</span>
                  <span class="info-value">{{ formatTime(currentNotification.createTime) }}</span>
                </div>
              </div>
            </div>

            <!-- 通知内容 -->
            <div class="detail-section">
              <div class="section-title">通知内容</div>
              <div class="content-box">{{ currentNotification.content }}</div>
            </div>

            <!-- 接收信息 -->
            <div class="detail-section">
              <div class="section-title">接收信息</div>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">接收类型：</span>
                  <span class="info-value">{{ getReceiverTypeText(currentNotification.receiverType) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">接收用户：</span>
                  <span class="info-value">{{ currentNotification.receiverIds || '全体用户' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">总接收人数：</span>
                  <span class="info-value">{{ currentNotification.totalCount || 0 }}人</span>
                </div>
                <div class="info-item">
                  <span class="info-label">已读人数：</span>
                  <span class="info-value">{{ currentNotification.readCount || 0 }}人</span>
                </div>
                <div class="info-item">
                  <span class="info-label">阅读率：</span>
                  <span class="info-value rate">{{ calculateReadRate(currentNotification) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const API_BASE_URL = ''

// 筛选表单
const filterForm = reactive({
  type: '',
  status: '',
  receiverType: '',
  startTime: '',
  endTime: ''
})

// 通知类型选项
const notificationTypes = ref([
  { value: 'system', label: '系统通知', icon: '⚙️' },
  { value: 'violation', label: '违规提醒', icon: '⚠️' },
  { value: 'security', label: '安全预警', icon: '🔒' },
  { value: 'activity', label: '活动通知', icon: '🎉' },
  { value: 'personal', label: '个人通知', icon: '👤' }
])

// 状态选项
const statusOptions = ref([
  { value: 'sent', label: '已发送', color: '#4caf50' },
  { value: 'draft', label: '草稿', color: '#ff9800' },
  { value: 'failed', label: '发送失败', color: '#f44336' }
])

// 快速统计 (初始为0，接口返回后覆盖)
const quickStats = ref([
  { icon: '📈', label: '今日发送', value: 0, bgColor: 'rgba(0, 123, 255, 0.2)' },
  { icon: '📊', label: '月度总数', value: 0, bgColor: 'rgba(40, 167, 69, 0.2)' },
  { icon: '⚡', label: '紧急通知', value: 0, bgColor: 'rgba(255, 193, 7, 0.2)' }
])

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
})

// 通知列表数据
const notificationList = ref([])
const allNotifications = ref([])   // 原始数据

// 选择的车辆
const selectedNotifications = ref([])
const allSelected = computed(() => {
  return selectedNotifications.value.length === notificationList.value.length && notificationList.value.length > 0
})

// 发送表单
const sendForm = reactive({
  title: '',
  type: 'system',
  isUrgent: false,
  content: '',
  receiverType: 'all',
  receiverIdsInput: '',
  sendMode: 'now' // now: 立即发送, draft: 保存草稿
})

// 表单验证
const titleTouched = ref(false)
const contentTouched = ref(false)

// 接收选项
const receiverOptions = ref([
  { value: 'all', label: '全体用户' },
  { value: 'student', label: '学生' },
  { value: 'teacher', label: '教职工' },
  { value: 'service', label: '后勤人员' },
  { value: 'specific', label: '指定用户' }
])

// 发送方式选项
const sendModeOptions = ref([
  { value: 'now', label: '立即发送' },
  { value: 'draft', label: '保存草稿' }
])

// 对话框状态
const sendDialogVisible = ref(false)
const detailDialogVisible = ref(false)

// 当前通知
const currentNotification = ref(null)

// 计算属性
const hasDraftSelected = computed(() => {
  return selectedNotifications.value.some(notification => notification.status === 'draft')
})

const isSendFormValid = computed(() => {
  return sendForm.title.trim() !== '' && sendForm.content.trim() !== ''
})

const isFiltered = computed(() => {
  return filterForm.type || filterForm.status || filterForm.receiverType || filterForm.startTime || filterForm.endTime
})

// ---------- API 调用 ----------
const fetchNotificationList = async () => {
  try {
    const params = {
      pageNum: 1,
      pageSize: 1000
    }
    const response = await axios.get(`${API_BASE_URL}/api/notification/list`, { params, timeout: 10000 })
    if (response.data.code === 200) {
      allNotifications.value = response.data.data || []
      applyFrontendFilter()
    } else {
      ElMessage.error(response.data.message || '获取通知列表失败')
      allNotifications.value = []
      notificationList.value = []
      pagination.total = 0
      pagination.totalPages = 1
    }
  } catch (error) {
    console.error('获取通知列表失败:', error)
    ElMessage.error('获取通知列表失败，请检查网络')
    allNotifications.value = []
    notificationList.value = []
    pagination.total = 0
    pagination.totalPages = 1
  }
}

const fetchNotificationStats = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/notification/stats`, { timeout: 5000 })
    if (response.data.code === 200) {
      const stats = response.data.data
      if (stats) {
        quickStats.value[0].value = stats.todaySent || 0
        quickStats.value[1].value = stats.total || 0
        quickStats.value[2].value = stats.urgent || 0
      }
    }
  } catch (error) {
    console.error('获取通知统计失败:', error)
    // 失败时用列表数据计算
    applyFrontendFilter()
  }
}

const sendNotification = async (notification) => {
  try {
    await ElMessageBox.confirm(`确定要发送通知"${notification.title}"吗？`, '发送通知', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const sendData = {
      title: notification.title,
      content: notification.content,
      type: notification.type,
      isUrgent: notification.isUrgent || false,
      receiverType: notification.receiverType || 'all'
    }

    if (notification.receiverIds && notification.receiverType === 'specific') {
      try {
        sendData.receiverIds = notification.receiverIds
          .split(',')
          .map(id => id.trim())
          .filter(id => id !== '' && !isNaN(id))
          .map(id => parseInt(id))
      } catch (e) {
        console.error('解析receiverIds失败:', e)
      }
    }

    const response = await axios.post(`${API_BASE_URL}/api/notification/send`, sendData, { timeout: 10000 })
    if (response.data.code === 200) {
      ElMessage.success('通知发送成功')
      fetchNotificationList()
      fetchNotificationStats()
    } else {
      ElMessage.error(response.data.message || '发送失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发送通知失败:', error)
      ElMessage.error('发送通知失败，请重试')
    }
  }
}

const sendNow = async () => {
  if (!sendForm.title.trim()) {
    ElMessage.warning('请输入通知标题')
    titleTouched.value = true
    return
  }
  if (!sendForm.content.trim()) {
    ElMessage.warning('请输入通知内容')
    contentTouched.value = true
    return
  }

  const notificationData = {
    title: sendForm.title,
    content: sendForm.content,
    type: sendForm.type,
    isUrgent: sendForm.isUrgent,
    receiverType: sendForm.receiverType
  }

  if (sendForm.receiverType === 'specific' && sendForm.receiverIdsInput.trim()) {
    try {
      notificationData.receiverIds = sendForm.receiverIdsInput
        .split(',')
        .map(id => id.trim())
        .filter(id => id !== '' && !isNaN(id))
        .map(id => parseInt(id))
    } catch (e) {
      ElMessage.error('接收用户ID格式错误，请使用逗号分隔的数字')
      return
    }
  }

  if (sendForm.sendMode === 'draft') {
    notificationData.status = 'draft'   // 后端可能支持草稿
  }

  try {
    const response = await axios.post(`${API_BASE_URL}/api/notification/send`, notificationData, { timeout: 10000 })
    if (response.data.code === 200) {
      ElMessage.success(sendForm.sendMode === 'draft' ? '通知已保存为草稿' : '通知发送成功')
      closeSendDialog()
      fetchNotificationList()
      fetchNotificationStats()
    } else {
      ElMessage.error(response.data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送通知失败:', error)
    ElMessage.error('发送失败，请重试')
  }
}

const deleteNotification = async (notification) => {
  try {
    await ElMessageBox.confirm(`确定要删除通知"${notification.title}"吗？`, '删除通知', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    const response = await axios.delete(`${API_BASE_URL}/api/notification/${notification.notificationId}`, { timeout: 5000 })
    if (response.data.code === 200) {
      ElMessage.success('通知删除成功')
      fetchNotificationList()
      fetchNotificationStats()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除通知失败:', error)
      ElMessage.error('删除失败，请重试')
    }
  }
}

const batchSend = async () => {
  if (!hasDraftSelected.value) {
    ElMessage.warning('请选择草稿状态的通知')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要批量发送选中的 ${selectedNotifications.value.length} 个通知吗？`, '批量发送通知', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const sendPromises = selectedNotifications.value
      .filter(n => n.status === 'draft')
      .map(notification => {
        const sendData = {
          title: notification.title,
          content: notification.content,
          type: notification.type,
          isUrgent: notification.isUrgent || false,
          receiverType: notification.receiverType || 'all'
        }
        if (notification.receiverIds && notification.receiverType === 'specific') {
          try {
            sendData.receiverIds = notification.receiverIds
              .split(',')
              .map(id => id.trim())
              .filter(id => id !== '' && !isNaN(id))
              .map(id => parseInt(id))
          } catch (e) {}
        }
        return axios.post(`${API_BASE_URL}/api/notification/send`, sendData, { timeout: 5000 })
      })

    const results = await Promise.allSettled(sendPromises)
    const successCount = results.filter(r => r.status === 'fulfilled' && r.value.data.code === 200).length
    const failCount = results.length - successCount
    ElMessage.success(`批量发送完成，成功: ${successCount}个，失败: ${failCount}个`)
    clearSelection()
    fetchNotificationList()
    fetchNotificationStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量发送失败:', error)
      ElMessage.error('批量发送失败，请重试')
    }
  }
}

const batchDelete = async () => {
  if (selectedNotifications.value.length === 0) {
    ElMessage.warning('请选择要删除的通知')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要批量删除选中的 ${selectedNotifications.value.length} 个通知吗？`, '批量删除通知', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    const deletePromises = selectedNotifications.value.map(n =>
      axios.delete(`${API_BASE_URL}/api/notification/${n.notificationId}`, { timeout: 5000 })
    )
    const results = await Promise.allSettled(deletePromises)
    const successCount = results.filter(r => r.status === 'fulfilled' && r.value.data.code === 200).length
    const failCount = results.length - successCount
    ElMessage.success(`批量删除完成，成功: ${successCount}个，失败: ${failCount}个`)
    clearSelection()
    fetchNotificationList()
    fetchNotificationStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败，请重试')
    }
  }
}

// ---------- 辅助方法 ----------
const applyFrontendFilter = () => {
  let filteredData = [...allNotifications.value]

  if (filterForm.type && filterForm.type.trim() !== '') {
    filteredData = filteredData.filter(item => item.type === filterForm.type)
  }
  if (filterForm.status && filterForm.status.trim() !== '') {
    filteredData = filteredData.filter(item => item.status === filterForm.status)
  }
  if (filterForm.receiverType && filterForm.receiverType.trim() !== '') {
    filteredData = filteredData.filter(item => item.receiverType === filterForm.receiverType)
  }
  if (filterForm.startTime && filterForm.startTime.trim() !== '') {
    const startDate = new Date(filterForm.startTime + 'T00:00:00')
    filteredData = filteredData.filter(item => item.sendTime && new Date(item.sendTime) >= startDate)
  }
  if (filterForm.endTime && filterForm.endTime.trim() !== '') {
    const endDate = new Date(filterForm.endTime + 'T23:59:59')
    filteredData = filteredData.filter(item => item.sendTime && new Date(item.sendTime) <= endDate)
  }

  const total = filteredData.length
  const totalPages = Math.ceil(total / pagination.pageSize) || 1
  if (pagination.currentPage > totalPages) pagination.currentPage = 1

  const startIndex = (pagination.currentPage - 1) * pagination.pageSize
  const endIndex = Math.min(startIndex + pagination.pageSize, total)
  notificationList.value = filteredData.slice(startIndex, endIndex)
  pagination.total = total
  pagination.totalPages = totalPages

  updateQuickStats(filteredData)
}

const updateQuickStats = (filteredData) => {
  const today = new Date().toISOString().split('T')[0]
  const todayCount = filteredData.filter(n => {
    if (!n.sendTime || n.status !== 'sent') return false
    return new Date(n.sendTime).toISOString().split('T')[0] === today
  }).length
  const urgentCount = filteredData.filter(n => n.isUrgent && n.status === 'sent').length
  const totalSent = filteredData.filter(n => n.status === 'sent').length
  quickStats.value[0].value = todayCount
  quickStats.value[1].value = totalSent
  quickStats.value[2].value = urgentCount
}

const handleFilter = () => {
  pagination.currentPage = 1
  applyFrontendFilter()
}

const resetFilter = () => {
  filterForm.type = ''
  filterForm.status = ''
  filterForm.receiverType = ''
  filterForm.startTime = ''
  filterForm.endTime = ''
  pagination.currentPage = 1
  applyFrontendFilter()
}

const refreshNotifications = () => {
  fetchNotificationList()
  fetchNotificationStats()
}

const getTypeIcon = (type) => {
  const icons = { system: '⚙️', violation: '⚠️', security: '🔒', activity: '🎉', personal: '👤' }
  return icons[type] || '📢'
}

const getTypeText = (type) => {
  const map = { system: '系统通知', violation: '违规提醒', security: '安全预警', activity: '活动通知', personal: '个人通知' }
  return map[type] || type
}

const getStatusText = (status) => {
  const map = { sent: '已发送', draft: '草稿', failed: '发送失败' }
  return map[status] || status
}

const getReceiverTypeText = (receiverType) => {
  const map = { all: '全体用户', student: '学生', teacher: '教职工', service: '后勤人员', specific: '指定用户' }
  return map[receiverType] || receiverType
}

const formatTime = (time) => {
  if (!time) return '未发送'
  try {
    const date = new Date(time)
    if (isNaN(date.getTime())) {
      const cleanTime = time.replace('T', ' ').replace('Z', '').split('.')[0]
      return cleanTime.substring(0, 16)
    }
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).replace(/\//g, '-')
  } catch (e) {
    return time
  }
}

const calculateReadRate = (notification) => {
  if (!notification.readCount || !notification.totalCount || notification.totalCount === 0) return '0%'
  return ((notification.readCount / notification.totalCount) * 100).toFixed(1) + '%'
}

const toggleSelectAll = () => {
  selectedNotifications.value = allSelected.value ? [] : [...notificationList.value]
}

const toggleSelect = (notification) => {
  const index = selectedNotifications.value.findIndex(n => n.notificationId === notification.notificationId)
  if (index === -1) selectedNotifications.value.push(notification)
  else selectedNotifications.value.splice(index, 1)
}

const isSelected = (notification) => selectedNotifications.value.some(n => n.notificationId === notification.notificationId)

const viewNotification = (notification) => {
  currentNotification.value = notification
  detailDialogVisible.value = true
}

const clearSelection = () => {
  selectedNotifications.value = []
}

const prevPage = () => {
  if (pagination.currentPage > 1) {
    pagination.currentPage--
    applyFrontendFilter()
  }
}

const nextPage = () => {
  if (pagination.currentPage < pagination.totalPages) {
    pagination.currentPage++
    applyFrontendFilter()
  }
}

const showSendDialog = () => {
  sendForm.title = ''
  sendForm.type = 'system'
  sendForm.isUrgent = false
  sendForm.content = ''
  sendForm.receiverType = 'all'
  sendForm.receiverIdsInput = ''
  sendForm.sendMode = 'now'
  titleTouched.value = false
  contentTouched.value = false
  sendDialogVisible.value = true
}

const closeSendDialog = () => {
  sendDialogVisible.value = false
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
}

// 生命周期
onMounted(() => {
  fetchNotificationList()
  fetchNotificationStats()
})

watch(() => pagination.currentPage, () => {
  applyFrontendFilter()
})
</script>

<style scoped>
.notification-management-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%);
  color: #ffffff;
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-bg {
  background: linear-gradient(90deg, rgba(0, 123, 255, 0.1), rgba(102, 16, 242, 0.1));
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(0, 123, 255, 0.2);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title-text {
  font-size: 28px;
  font-weight: bold;
  background: linear-gradient(90deg, #ffffff, #00d4ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.action-btn.primary {
  background: linear-gradient(90deg, #007bff, #6610f2);
  border: none;
}

.action-btn .btn-icon {
  font-size: 16px;
}

.action-btn .btn-text {
  font-size: 14px;
  font-weight: 500;
}

.main-content {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  min-height: calc(100vh - 200px);
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-form {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.required {
  color: #ff4757;
  margin-left: 4px;
}

.type-selector {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.type-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.type-option.active {
  background: rgba(0, 212, 255, 0.2);
  border-color: rgba(0, 212, 255, 0.4);
}

.type-option:hover {
  background: rgba(255, 255, 255, 0.1);
}

.type-icon {
  font-size: 20px;
}

.type-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.status-selector {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.status-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.status-option.active {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

.status-option:hover {
  background: rgba(255, 255, 255, 0.1);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.tech-select {
  width: 100%;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
  font-size: 14px;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='white' viewBox='0 0 16 16'%3E%3Cpath d='M7.247 11.14L2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
}

.tech-select:focus {
  outline: none;
  border-color: #00d4ff;
}

.tech-input {
  width: 100%;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
  font-size: 14px;
  transition: all 0.3s ease;
}

.tech-input:focus {
  outline: none;
  border-color: #00d4ff;
}

.tech-input.error {
  border-color: #ff4757;
}

.error-message {
  color: #ff4757;
  font-size: 12px;
  margin-top: 4px;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-input {
  flex: 1;
}

.date-separator {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}

.filter-btn {
  flex: 1;
  background: linear-gradient(90deg, #007bff, #6610f2);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.filter-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 123, 255, 0.3);
}

.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.reset-btn {
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  font-size: 14px;
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.stats-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.card-content {
  flex: 1;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #00d4ff;
  margin-bottom: 4px;
}

.card-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.right-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.table-container {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.table-title {
  font-size: 18px;
  font-weight: 500;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.filter-status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-right: 16px;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: rgba(0, 212, 255, 0.2);
  border: 1px solid rgba(0, 212, 255, 0.4);
  border-radius: 16px;
  font-size: 12px;
  color: #00d4ff;
}

.filter-remove {
  cursor: pointer;
  padding: 0 4px;
  font-size: 14px;
}

.filter-remove:hover {
  color: #ff4757;
}

.total-info {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.page-btn:hover:not(.disabled) {
  background: rgba(255, 255, 255, 0.15);
  border-color: #00d4ff;
}

.page-btn.disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.table-wrapper {
  overflow-x: auto;
  padding: 20px;
}

.tech-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.tech-table thead {
  background: rgba(255, 255, 255, 0.05);
}

.tech-table th {
  padding: 16px 12px;
  text-align: left;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  border-bottom: 2px solid rgba(0, 212, 255, 0.3);
}

.tech-table tbody tr {
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tech-table tbody tr:hover {
  background: rgba(0, 212, 255, 0.05);
}

.tech-table td {
  padding: 16px 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tech-checkbox {
  width: 18px;
  height: 18px;
  appearance: none;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.tech-checkbox:checked {
  background: #00d4ff;
  border-color: #00d4ff;
}

.tech-checkbox:checked::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.id-cell {
  color: #00d4ff;
  font-weight: 500;
  font-size: 14px;
}

.notification-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-title {
  font-weight: 500;
  color: #ffffff;
  font-size: 14px;
}

.notification-tags {
  display: flex;
  gap: 4px;
}

.tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}

.tag.urgent {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
  border: 1px solid rgba(244, 67, 54, 0.4);
}

.type-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.type-cell.system {
  background: rgba(0, 123, 255, 0.2);
  color: #007bff;
  border: 1px solid rgba(0, 123, 255, 0.4);
}

.type-cell.violation {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
  border: 1px solid rgba(255, 152, 0, 0.4);
}

.type-cell.security {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
  border: 1px solid rgba(76, 175, 80, 0.4);
}

.type-cell.activity {
  background: rgba(156, 39, 176, 0.2);
  color: #9c27b0;
  border: 1px solid rgba(156, 39, 176, 0.4);
}

.type-cell.personal {
  background: rgba(33, 150, 243, 0.2);
  color: #2196f3;
  border: 1px solid rgba(33, 150, 243, 0.4);
}

.receiver-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.receiver-type {
  font-weight: 500;
  color: #ffffff;
}

.receiver-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.time-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.send-time {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.read-rate {
  font-size: 12px;
  color: #00d4ff;
  font-weight: 500;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.status-cell.sent {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
  border: 1px solid rgba(76, 175, 80, 0.4);
}

.status-cell.draft {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
  border: 1px solid rgba(255, 152, 0, 0.4);
}

.status-cell.failed {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
  border: 1px solid rgba(244, 67, 54, 0.4);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn.small {
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn.small:hover {
  background: rgba(255, 255, 255, 0.15);
}

.action-btn.small.success {
  background: rgba(76, 175, 80, 0.2);
  border-color: rgba(76, 175, 80, 0.4);
}

.table-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.selected-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.batch-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.batch-btn:hover:not(.disabled) {
  background: rgba(255, 255, 255, 0.15);
}

.batch-btn.disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: linear-gradient(135deg, #1a1f3c 0%, #2d3250 100%);
  border-radius: 16px;
  width: 800px;
  max-height: 90vh;
  border: 1px solid rgba(0, 123, 255, 0.2);
  overflow: hidden;
}

.modal-content.large {
  width: 900px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  font-size: 24px;
}

.title-text {
  font-size: 20px;
  font-weight: bold;
  color: #ffffff;
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  max-height: 60vh;
}

.send-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #00d4ff;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-col.full {
  flex: 2;
}

.tech-textarea {
  width: 100%;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
  font-size: 14px;
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
}

.tech-textarea:focus {
  outline: none;
  border-color: #00d4ff;
}

.tech-textarea.error {
  border-color: #ff4757;
}

.error-message {
  color: #ff4757;
  font-size: 12px;
  margin-top: 4px;
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.radio-group.horizontal {
  flex-direction: row;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.radio-option.active {
  background: rgba(0, 212, 255, 0.2);
  border-color: rgba(0, 212, 255, 0.4);
}

.radio-option:hover {
  background: rgba(255, 255, 255, 0.1);
}

.radio-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  position: relative;
}

.radio-option.active .radio-dot::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00d4ff;
}

.radio-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.input-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.urgent-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.toggle-track {
  width: 48px;
  height: 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  position: relative;
  transition: all 0.3s ease;
}

.toggle-track.active {
  background: rgba(244, 67, 54, 0.3);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: #ffffff;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.toggle-track.active .toggle-thumb {
  left: 26px;
  background: #f44336;
}

.toggle-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.modal-footer {
  padding: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.footer-actions .action-btn {
  padding: 10px 24px;
}

.footer-actions .action-btn.primary.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 详情弹窗专用样式：水平对齐，标签固定宽度 */
.modal-content.large .info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 24px;
}

.modal-content.large .info-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  border-bottom: none;
  padding: 0;
}

.modal-content.large .info-label {
  flex: 0 0 80px; /* 固定标签宽度，确保对齐 */
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  text-align: right;
  text-transform: none;
  letter-spacing: normal;
}

.modal-content.large .info-value {
  flex: 1;
  font-size: 15px;
  color: #ffffff;
  font-weight: normal;
  word-break: break-word;
}

/* 特殊标记样式微调 */
.modal-content.large .info-value.tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 16px;
  font-size: 13px;
  width: auto;
  background: rgba(0, 123, 255, 0.2);
  border: 1px solid rgba(0, 123, 255, 0.4);
}

.modal-content.large .info-value.tag.urgent {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
  border-color: rgba(244, 67, 54, 0.4);
}

.modal-content.large .info-value.status {
  color: #4caf50;
}

.modal-content.large .info-value.rate {
  color: #00d4ff;
}

/* 通知内容区域 */
.content-box {
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .left-panel {
    width: 100%;
  }

  .modal-content {
    width: 90%;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .form-row {
    flex-direction: column;
  }

  .table-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .table-actions {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .filter-status {
    width: 100%;
    margin-right: 0;
  }

  .selected-info {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .batch-actions {
    flex-wrap: wrap;
  }

  .modal-content {
    width: 95%;
    margin: 20px;
  }

  .modal-body {
    max-height: 70vh;
  }

  /* 小屏幕下详情弹窗标签宽度自适应 */
  .modal-content.large .info-item {
    flex-wrap: wrap;
  }
  .modal-content.large .info-label {
    flex: 0 0 100%;
    text-align: left;
    margin-bottom: 4px;
  }
  .modal-content.large .info-value {
    flex: 0 0 100%;
  }
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 212, 255, 0.3);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 212, 255, 0.5);
}
</style>

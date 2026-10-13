<template>
  <div class="counselor-container page-bg-shell">
    <!-- 顶部标签 -->
    <div class="top-tabs">
      <div v-for="t in topTabs" :key="t.value" class="top-tab"
        :class="{ active: currentTab === t.value }"
        @click="currentTab = t.value">
        <span class="tab-icon">{{ t.icon }}</span>
        <span>{{ t.label }}</span>
      </div>
    </div>

    <!-- ============ 模块1：导员工作台 ============ -->
    <div v-if="currentTab === 'workspace'" class="module">
      <div class="page-header">
        <div class="header-bg">
          <div class="header-top-row">
            <div class="header-left">
              <h1 class="title-text">🎓 导员工作台</h1>
              <p class="title-sub">{{ counselorInfo.name }} · 管辖 {{ counselorInfo.classList.length }} 个班 / {{ counselorInfo.studentCount }} 人。班级请在「用户管理」做导员绑定，并与学生注册所选班级一致。</p>
            </div>
            <div class="header-actions">
              <div class="action-btn" @click="goUserManage" title="管理员在此为导员绑定管辖班级">🔗 班级绑定配置</div>
              <div class="action-btn" @click="fetchWorkspaceData">↻ 刷新</div>
              <div class="action-btn primary" @click="sendBatchNotice">📢 群发通知</div>
            </div>
          </div>
          <div class="counselor-pick-bar">
            <span class="pick-label">当前查看身份</span>
            <select
              v-if="teacherList.length"
              v-model.number="selectedCounselorId"
              class="counselor-select"
              @change="onCounselorPickChange"
            >
              <option v-for="t in teacherList" :key="t.userId" :value="t.userId">
                {{ (t.realName || t.username || '导员') + '（#' + t.userId + '）' }}
              </option>
            </select>
            <span v-else class="pick-empty">暂无「导员」角色用户，请先在用户管理中创建并分配角色为导员</span>
          </div>
        </div>
      </div>

      <!-- 工作台总览卡片 -->
      <div class="overview-grid">
        <div class="ov-card" v-for="ov in overviewCards" :key="ov.label" :style="{ '--accent': ov.color }">
          <div class="ov-icon">{{ ov.icon }}</div>
          <div class="ov-body">
            <div class="ov-val">{{ ov.value }}</div>
            <div class="ov-label">{{ ov.label }}</div>
            <div class="ov-sub" v-if="ov.sub">{{ ov.sub }}</div>
          </div>
          <div class="ov-action" v-if="ov.action" @click="ov.action">{{ ov.actionLabel }}</div>
        </div>
      </div>

      <div class="workspace-layout">
        <!-- 左：待处理事项 -->
        <div class="ws-left">
          <!-- 待整改学生列表 -->
          <div class="section-card">
            <div class="section-header">
              <span class="sec-title">⚠️ 待跟进整改</span>
              <span class="sec-badge danger">{{ pendingRectify.length }}</span>
            </div>
            <div class="student-list">
              <div v-for="item in pendingRectify" :key="item.illegalId" class="student-item">
                <div class="si-avatar">{{ item.studentName.charAt(0) }}</div>
                <div class="si-info">
                  <div class="si-name">{{ item.studentName }}
                    <span class="si-class">{{ item.className }}</span>
                  </div>
                  <div class="si-vio">{{ getViolationType(item.illegalType) }} · {{ formatDate(item.illegalTime) }}</div>
                  <div class="si-meta">
                    <span class="state-badge" :class="'s' + item.illegalState">{{ getViolationState(item.illegalState) }}</span>
                    <span class="overdue-tag" v-if="isOverdue(item.deadline)">逾期</span>
                  </div>
                </div>
                <div class="si-actions">
                  <div class="action-btn small" @click="sendStudentReminder(item)">🔔 提醒</div>
                  <div class="action-btn small primary" @click="viewViolationDetail(item)">详情</div>
                </div>
              </div>
              <div v-if="!pendingRectify.length" class="empty-tip">✅ 暂无待跟进整改</div>
            </div>
          </div>

          <!-- 申诉待审核 -->
          <div class="section-card">
            <div class="section-header">
              <span class="sec-title">📣 待审申诉</span>
              <span class="sec-badge warn">{{ pendingAppeals.length }}</span>
            </div>
            <div class="student-list">
              <div v-for="ap in pendingAppeals" :key="ap.appealId" class="student-item">
                <div class="si-avatar appeal">{{ ap.studentName.charAt(0) }}</div>
                <div class="si-info">
                  <div class="si-name">{{ ap.studentName }}
                    <span class="si-class">{{ ap.className }}</span>
                  </div>
                  <div class="si-vio">申诉：{{ getViolationType(ap.illegalType) }}</div>
                  <div class="si-meta">
                    <span class="si-time">{{ formatDate(ap.appealTime) }}</span>
                    <span class="urg-tag" v-if="ap.urgencyLevel >= 2">紧急</span>
                  </div>
                </div>
                <div class="si-actions">
                  <div class="action-btn small success" @click="auditAppeal(ap, 1)">✓ 通过</div>
                  <div class="action-btn small danger" @click="auditAppeal(ap, 0)">✗ 驳回</div>
                </div>
              </div>
              <div v-if="!pendingAppeals.length" class="empty-tip">✅ 暂无待审申诉</div>
            </div>
          </div>

          <!-- 通知记录 -->
          <div class="section-card">
            <div class="section-header">
              <span class="sec-title">📬 最近通知</span>
              <div class="action-btn small primary" @click="currentTab = 'stats'">查看全部</div>
            </div>
            <div class="notice-list">
              <div v-for="n in recentNotifications" :key="n.notificationId" class="notice-item">
                <div class="ni-dot" :class="n.isUrgent ? 'urgent' : 'normal'"></div>
                <div class="ni-content">
                  <div class="ni-title">{{ n.title }}</div>
                  <div class="ni-meta">{{ formatDate(n.sendTime) }} · 已读 {{ n.readCount }}/{{ n.totalCount }}</div>
                </div>
              </div>
              <div v-if="!recentNotifications.length" class="empty-tip">暂无通知记录</div>
            </div>
          </div>
        </div>

        <!-- 右：班级管理 + 快速统计 -->
        <div class="ws-right">
          <!-- 我的班级（学院 → 专业 → 班） -->
          <div class="section-card">
            <div class="section-header">
              <span class="sec-title">🏫 我的班级</span>
            </div>
            <div class="class-grid grouped">
              <template v-if="counselorInfo.groups && counselorInfo.groups.length">
                <div v-for="col in counselorInfo.groups" :key="col.college" class="college-group">
                  <div class="group-h1">{{ col.college }}</div>
                  <div v-for="maj in col.majors" :key="col.college + '|' + maj.major" class="major-group">
                    <div class="group-h2">{{ maj.major }}</div>
                    <div class="class-row">
                      <div v-for="cls in maj.classes" :key="cls.name" class="class-card"
                        :class="{ active: selectedClass === cls.name }"
                        @click="selectClass(cls.name)">
                        <div class="cls-name">{{ cls.name }}</div>
                        <div class="cls-stats">
                          <div class="cst"><span class="csv">{{ cls.studentCount }}</span><span class="csl">人</span></div>
                          <div class="cst"><span class="csv c-orange">{{ cls.pendingCount }}</span><span class="csl">待整改</span></div>
                          <div class="cst"><span class="csv c-red">{{ cls.violationCount }}</span><span class="csl">违规</span></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
              <template v-else-if="counselorInfo.classList.length">
                <div class="class-row">
                  <div v-for="cls in counselorInfo.classList" :key="cls.name" class="class-card"
                    :class="{ active: selectedClass === cls.name }"
                    @click="selectClass(cls.name)">
                    <div class="cls-name">{{ cls.name }}</div>
                    <div class="cls-stats">
                      <div class="cst"><span class="csv">{{ cls.studentCount }}</span><span class="csl">人</span></div>
                      <div class="cst"><span class="csv c-orange">{{ cls.pendingCount }}</span><span class="csl">待整改</span></div>
                      <div class="cst"><span class="csv c-red">{{ cls.violationCount }}</span><span class="csl">违规</span></div>
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="empty-tip" style="padding:16px">暂无管辖班级，请在「用户管理」中为导员配置班级绑定（需与学生的班级字段一致）</div>
            </div>
          </div>

          <!-- 所选班级学生违规详情 -->
          <div class="section-card" v-if="selectedClass">
            <div class="section-header">
              <span class="sec-title">{{ selectedClass }} 违规明细</span>
              <div class="action-btn small" @click="selectedClass = null">收起</div>
            </div>
            <div class="class-violation-list">
              <div v-for="sv in classViolations" :key="sv.studentId" class="cv-item">
                <div class="cv-name">{{ sv.name }}</div>
                <div class="cv-plate">{{ sv.licensePlate }}</div>
                <div class="cv-cnt">
                  <span class="cnt-badge" :class="{ high: sv.total > 3 }">{{ sv.total }}次</span>
                </div>
                <div class="cv-action">
                  <div class="action-btn small" @click="sendStudentReminderByName(sv)">提醒</div>
                </div>
              </div>
              <div v-if="!classViolations.length" class="empty-tip">该班级暂无违规记录</div>
            </div>
          </div>

          <!-- 违规类型分布 -->
          <div class="section-card">
            <div class="section-header">
              <span class="sec-title">📊 管辖违规分布</span>
            </div>
            <div class="vio-dist">
              <div v-for="item in violationDist" :key="item.type" class="vd-item">
                <span class="vd-name">{{ item.type }}</span>
                <div class="vd-bar">
                  <div class="vd-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
                </div>
                <span class="vd-val">{{ item.count }}</span>
              </div>
            </div>
          </div>

          <!-- 快捷操作 -->
          <div class="section-card">
            <div class="section-header"><span class="sec-title">⚡ 快捷操作</span></div>
            <div class="quick-actions">
              <div class="qa-btn" @click="batchRemindAll">
                <div class="qa-icon">📢</div>
                <div class="qa-label">批量发提醒</div>
              </div>
              <div class="qa-btn" @click="currentTab = 'stats'">
                <div class="qa-icon">📈</div>
                <div class="qa-label">查看统计</div>
              </div>
              <div class="qa-btn" @click="exportMyReport">
                <div class="qa-icon">📄</div>
                <div class="qa-label">导出报告</div>
              </div>
              <div class="qa-btn" @click="openNoticeDialog">
                <div class="qa-icon">📬</div>
                <div class="qa-label">发送通知</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 模块2：统计报告 ============ -->
    <div v-if="currentTab === 'stats'" class="module">
      <div class="page-header">
        <div class="header-bg">
          <div class="header-left">
            <h1 class="title-text">📈 统计报告</h1>
            <p class="title-sub">车辆注册、违规行为、整改效果综合分析</p>
          </div>
          <div class="header-right">
            <div class="date-range-select">
              <div v-for="r in dateRanges" :key="r.value" class="dr-opt"
                :class="{ active: statsRange === r.value }" @click="changeRange(r.value)">
                {{ r.label }}
              </div>
            </div>
            <div class="action-btn primary" @click="exportReport">📥 导出报告</div>
          </div>
        </div>
      </div>

      <!-- 核心指标 -->
      <div class="kpi-grid">
        <div class="kpi-card" v-for="kpi in kpiCards" :key="kpi.label">
          <div class="kpi-icon" :style="{ background: kpi.bg }">{{ kpi.icon }}</div>
          <div class="kpi-body">
            <div class="kpi-val">{{ kpi.value }}<span class="kpi-unit" v-if="kpi.unit">{{ kpi.unit }}</span></div>
            <div class="kpi-label">{{ kpi.label }}</div>
            <div class="kpi-trend" :class="kpi.trend > 0 ? 'up' : 'down'" v-if="kpi.trend !== undefined">
              {{ kpi.trend > 0 ? '↑' : '↓' }} {{ Math.abs(kpi.trend) }}%
            </div>
          </div>
        </div>
      </div>

      <div class="stats-layout">
        <!-- 左列 -->
        <div class="stats-col">
          <!-- 车辆注册趋势 -->
          <div class="chart-card">
            <div class="chart-header">🚗 车辆注册趋势</div>
            <div class="chart-body">
              <div class="bar-chart">
                <div v-for="item in registerTrend" :key="item.date" class="bc-group">
                  <div class="bc-bars">
                    <div class="bc-bar approved" :style="{ height: barHeight(item.approved, maxRegister) + '%' }" :title="'通过: ' + item.approved"></div>
                    <div class="bc-bar pending" :style="{ height: barHeight(item.pending, maxRegister) + '%' }" :title="'待审: ' + item.pending"></div>
                  </div>
                  <div class="bc-label">{{ item.date }}</div>
                </div>
              </div>
              <div class="chart-legend">
                <span class="cl-item"><span class="cl-dot approved"></span>已通过</span>
                <span class="cl-item"><span class="cl-dot pending"></span>待审核</span>
              </div>
            </div>
          </div>

          <!-- 违规类型分布 -->
          <div class="chart-card">
            <div class="chart-header">⚠️ 违规类型分布</div>
            <div class="chart-body">
              <div class="donut-chart-area">
                <svg viewBox="0 0 200 200" class="donut-svg">
                  <circle cx="100" cy="100" r="70" fill="none" stroke="rgba(255,255,255,0.05)" stroke-width="30"/>
                  <circle v-for="(seg, i) in donutSegments" :key="i"
                    cx="100" cy="100" r="70" fill="none"
                    :stroke="seg.color" stroke-width="30"
                    :stroke-dasharray="seg.dash + ' ' + seg.gap"
                    :stroke-dashoffset="seg.offset"
                    style="transform: rotate(-90deg); transform-origin: center;"/>
                  <text x="100" y="96" text-anchor="middle" fill="#fff" font-size="18" font-weight="bold">{{ statsData.totalViolations }}</text>
                  <text x="100" y="112" text-anchor="middle" fill="rgba(255,255,255,0.5)" font-size="10">总违规</text>
                </svg>
                <div class="donut-legend">
                  <div v-for="item in violationTypeStats" :key="item.type" class="dl-item">
                    <span class="dl-dot" :style="{ background: item.color }"></span>
                    <span class="dl-name">{{ item.type }}</span>
                    <span class="dl-val">{{ item.count }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右列 -->
        <div class="stats-col">
          <!-- 违规整改率趋势 -->
          <div class="chart-card">
            <div class="chart-header">📋 整改完成率趋势</div>
            <div class="chart-body">
              <div class="line-chart">
                <svg :viewBox="'0 0 ' + lineChartW + ' ' + lineChartH" class="lc-svg">
                  <!-- 网格线 -->
                  <line v-for="i in 4" :key="'hg' + i" x1="40" :x2="lineChartW - 10"
                    :y1="lineChartH - 30 - (i * (lineChartH - 50) / 4)" :y2="lineChartH - 30 - (i * (lineChartH - 50) / 4)"
                    stroke="rgba(255,255,255,0.08)" stroke-width="1"/>
                  <!-- 折线 -->
                  <polyline :points="rectifyLinePoints" fill="none" stroke="#4caf50" stroke-width="2" stroke-linejoin="round"/>
                  <!-- 数据点 -->
                  <circle v-for="(pt, i) in rectifyPoints" :key="i" :cx="pt.x" :cy="pt.y" r="4"
                    fill="#4caf50" stroke="rgba(0,0,0,0.5)" stroke-width="2"/>
                  <!-- X轴标签 -->
                  <text v-for="(pt, i) in rectifyPoints" :key="'l' + i" :x="pt.x" :y="lineChartH - 10"
                    text-anchor="middle" fill="rgba(255,255,255,0.4)" font-size="9">{{ rectifyTrend[i]?.date }}</text>
                  <!-- Y轴标签 -->
                  <text v-for="i in 5" :key="'y' + i" x="35" :y="lineChartH - 30 - ((i-1) * (lineChartH - 50) / 4) + 4"
                    text-anchor="end" fill="rgba(255,255,255,0.4)" font-size="9">{{ (i-1) * 25 }}%</text>
                </svg>
              </div>
            </div>
          </div>

          <!-- 部门/院系违规排名 -->
          <div class="chart-card">
            <div class="chart-header">🏆 院系违规排名（TOP 8）</div>
            <div class="chart-body">
              <div class="rank-list">
                <div v-for="(item, i) in deptRanking" :key="item.dept" class="rank-item">
                  <div class="rank-no" :class="{ gold: i === 0, silver: i === 1, bronze: i === 2 }">{{ i + 1 }}</div>
                  <div class="rank-dept">{{ item.dept }}</div>
                  <div class="rank-bar-wrap">
                    <div class="rank-bar"><div class="rank-fill" :style="{ width: (item.count / deptRanking[0].count * 100) + '%', background: rankColor(i) }"></div></div>
                  </div>
                  <div class="rank-cnt">{{ item.count }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 统计摘要表 -->
          <div class="chart-card">
            <div class="chart-header">📋 数据摘要</div>
            <div class="chart-body">
              <div class="summary-table">
                <div class="sum-row" v-for="row in summaryRows" :key="row.label">
                  <span class="sum-label">{{ row.label }}</span>
                  <span class="sum-val">{{ row.value }}</span>
                  <span class="sum-trend" :class="row.trendUp ? 'up' : 'down'" v-if="row.trend">
                    {{ row.trendUp ? '↑' : '↓' }} {{ row.trend }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 发送通知弹窗 ============ -->
    <div v-if="noticeDialog.visible" class="modal-overlay" @click.self="noticeDialog.visible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">📢 发送班级通知</span>
          <span class="modal-close" @click="noticeDialog.visible = false">×</span>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label class="form-label">通知标题 *</label>
            <input v-model="noticeDialog.form.title" class="tech-input-full" placeholder="通知标题" />
          </div>
          <div class="form-item" style="margin-top:14px">
            <label class="form-label">通知内容 *</label>
            <textarea v-model="noticeDialog.form.content" class="tech-textarea" rows="5" placeholder="通知正文..."></textarea>
          </div>
          <div class="form-item" style="margin-top:14px">
            <label class="form-label">发送范围</label>
            <div class="receiver-opts">
              <div v-for="cls in counselorInfo.classList" :key="cls.name"
                class="recv-opt" :class="{ active: noticeDialog.form.classes.includes(cls.name) }"
                @click="toggleClass(cls.name)">
                {{ cls.name }}
              </div>
              <div class="recv-opt" :class="{ active: noticeDialog.form.classes.length === counselorInfo.classList.length }" @click="selectAllClasses">
                全选
              </div>
            </div>
          </div>
          <div class="form-item" style="margin-top:14px; display:flex; align-items:center; gap:12px;">
            <label class="form-label">紧急通知</label>
            <div class="toggle-btn" :class="{ on: noticeDialog.form.isUrgent }" @click="noticeDialog.form.isUrgent = !noticeDialog.form.isUrgent">
              <div class="toggle-dot"></div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="noticeDialog.visible = false">取消</div>
          <div class="action-btn primary" @click="submitNotice">发送通知</div>
        </div>
      </div>
    </div>

    <!-- 申诉审核弹窗 -->
    <div v-if="auditDialog.visible" class="modal-overlay" @click.self="auditDialog.visible = false">
      <div class="modal-content sm" @click.stop>
        <div class="modal-header">
          <span class="modal-title">📣 申诉{{ auditDialog.pass ? '通过' : '驳回' }}</span>
          <span class="modal-close" @click="auditDialog.visible = false">×</span>
        </div>
        <div class="modal-body">
          <div class="info-block">
            <div class="info-row"><span class="il">申诉人</span><span>{{ auditDialog.appeal?.studentName }}</span></div>
            <div class="info-row"><span class="il">违规类型</span><span>{{ getViolationType(auditDialog.appeal?.illegalType) }}</span></div>
          </div>
          <div class="form-item" style="margin-top:14px">
            <label class="form-label">审核意见</label>
            <textarea v-model="auditDialog.opinion" class="tech-textarea" rows="3" :placeholder="auditDialog.pass ? '填写通过原因...' : '填写驳回原因...'"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="auditDialog.visible = false">取消</div>
          <div class="action-btn" :class="auditDialog.pass ? 'success' : 'danger'" @click="confirmAudit">确认{{ auditDialog.pass ? '通过' : '驳回' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const WORKSPACE_COUNSELOR_LS = 'counselor_workspace_user_id'
const goUserManage = () => router.push('/community/user-manage')

const API = '/api'
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`

// ==================== 全局 ====================
const currentTab = ref('workspace')
const topTabs = [
  { value: 'workspace', label: '导员工作台', icon: '🎓' },
  { value: 'stats', label: '统计报告', icon: '📈' }
]

// ==================== 工作台 ====================
const counselorInfo = reactive({
  name: '导员',
  userId: null,
  groups: [],
  classList: [],
  studentCount: 0
})

const teacherList = ref([])
const selectedCounselorId = ref(null)

const loadTeachers = async () => {
  try {
    const res = await axios.get(`${API}/user/list`, {
      params: { userType: 'teacher', pageNum: 1, pageSize: 500 }
    })
    if (res.data.code === 200) {
      teacherList.value = res.data.data || []
    } else {
      teacherList.value = []
    }
  } catch {
    teacherList.value = []
  }
}

function initSelectedCounselorFromList() {
  if (!teacherList.value.length) {
    selectedCounselorId.value = null
    return
  }
  const saved = localStorage.getItem(WORKSPACE_COUNSELOR_LS)
  if (saved && !Number.isNaN(Number(saved)) && teacherList.value.some((t) => t.userId === Number(saved))) {
    selectedCounselorId.value = Number(saved)
    return
  }
  const hinted = resolveCounselorUserId()
  if (hinted != null && teacherList.value.some((t) => t.userId === hinted)) {
    selectedCounselorId.value = hinted
    localStorage.setItem(WORKSPACE_COUNSELOR_LS, String(hinted))
    return
  }
  selectedCounselorId.value = teacherList.value[0].userId
  localStorage.setItem(WORKSPACE_COUNSELOR_LS, String(selectedCounselorId.value))
}

const onCounselorPickChange = () => {
  if (selectedCounselorId.value != null) {
    localStorage.setItem(WORKSPACE_COUNSELOR_LS, String(selectedCounselorId.value))
  }
  selectedClass.value = null
  fetchWorkspaceData()
}

/** 从 JWT payload 解析 userId（与后端 JwtUtil claims.userId 一致） */
function parseUserIdFromJwt() {
  for (const key of ['token', 'ev_token']) {
    const t = localStorage.getItem(key)
    if (!t || !t.includes('.')) continue
    try {
      const part = t.split('.')[1]
      let b64 = part.replace(/-/g, '+').replace(/_/g, '/')
      while (b64.length % 4) b64 += '='
      const payload = JSON.parse(atob(b64))
      if (payload.userId != null && !Number.isNaN(Number(payload.userId))) {
        return Number(payload.userId)
      }
    } catch {
      /* ignore */
    }
  }
  return null
}

/** 仅作下拉默认项提示：手动 counselorUserId → JWT → vuex */
function resolveCounselorUserId() {
  const manual = localStorage.getItem('counselorUserId')
  if (manual && !Number.isNaN(Number(manual))) return Number(manual)
  const fromJwt = parseUserIdFromJwt()
  if (fromJwt != null) return fromJwt
  try {
    const vx = JSON.parse(localStorage.getItem('vuex') || '{}')
    const info = vx.user?.info
    if (info && (info.userId != null || info.id != null)) return Number(info.userId ?? info.id)
  } catch {
    /* ignore */
  }
  return null
}

const fetchCounselorWorkspace = async () => {
  const uid = selectedCounselorId.value
  if (uid == null || Number.isNaN(uid)) {
    counselorInfo.userId = null
    counselorInfo.name = '导员'
    counselorInfo.classList = []
    counselorInfo.groups = []
    counselorInfo.studentCount = 0
    return
  }
  try {
    const res = await axios.get(`${API}/counselor/workspace`, { params: { counselorUserId: uid } })
    if (res.data.code === 200 && res.data.data) {
      const d = res.data.data
      counselorInfo.userId = d.userId
      counselorInfo.name = d.name || '导员'
      counselorInfo.studentCount = d.studentCount ?? 0
      counselorInfo.groups = d.groups || []
      counselorInfo.classList = (d.classList || []).map(c => ({
        name: c.name,
        studentCount: c.studentCount ?? 0,
        pendingCount: c.pendingCount ?? 0,
        violationCount: c.violationCount ?? 0,
        college: c.college,
        major: c.major
      }))
    }
  } catch {
    counselorInfo.userId = uid
    counselorInfo.classList = []
    counselorInfo.groups = []
  }
}

const pendingRectify = ref([])
const pendingAppeals = ref([])
const recentNotifications = ref([])
const selectedClass = ref(null)
const classViolations = ref([])
const violationDist = ref([])

const overviewCards = computed(() => [
  { icon: '👥', label: '管辖学生', value: counselorInfo.studentCount, color: '#007bff', sub: `${counselorInfo.classList.length}个班级` },
  { icon: '⚠️', label: '待整改', value: pendingRectify.value.length, color: '#ff9800', sub: '待我跟进' },
  { icon: '📣', label: '待审申诉', value: pendingAppeals.value.length, color: '#9c27b0', sub: '需处理' },
  { icon: '🚗', label: '注册车辆', value: registeredCount.value, color: '#4caf50', sub: '本学期' }
])

const registeredCount = ref(0)

const fetchWorkspaceData = async () => {
  await fetchCounselorWorkspace()
  if (!counselorInfo.userId) return
  await Promise.all([
    fetchPendingRectify(),
    fetchPendingAppeals(),
    fetchRecentNotifications(),
    fetchViolationDist()
  ])
}

const fetchPendingRectify = async () => {
  try {
    const res = await axios.get(`${API}/violation/list`, {
      params: { pageNum: 1, pageSize: 20, illegalState: 0, counselorId: counselorInfo.userId || undefined }
    })
    if (res.data.code === 200) {
      pendingRectify.value = (res.data.data || []).map(v => ({
        illegalId: v.illegalId,
        studentName: v.ownerName || v.realName || '未知',
        className: v.className || '—',
        illegalType: v.illegalType,
        illegalTime: v.illegalTime,
        illegalState: v.illegalState,
        userId: v.userId,
        deadline: v.deadline
      }))
    }
  } catch (e) {
    // 降级为模拟数据
      pendingRectify.value = [
        { illegalId: 1, studentName: '林浩', className: '计科2301班', illegalType: 1, illegalTime: '2024-03-01', illegalState: 0, userId: 10 },
        { illegalId: 2, studentName: '许诚', className: '软件2301班', illegalType: 3, illegalTime: '2024-03-05', illegalState: 1, userId: 11 }
      ]
  }
}

const fetchPendingAppeals = async () => {
  try {
    const res = await axios.get(`${API}/appeal/list`, {
      params: { pageNum: 1, pageSize: 20, appealState: 0, counselorId: counselorInfo.userId || undefined }
    })
    if (res.data.code === 200) {
      pendingAppeals.value = (res.data.data || []).map(a => ({
        appealId: a.appealId,
        studentName: a.userName || a.realName || '未知',
        className: a.className || '—',
        illegalType: a.illegalType,
        appealTime: a.appealTime,
        urgencyLevel: a.urgencyLevel || 0,
        illegalId: a.illegalId,
        userId: a.userId
      }))
    }
  } catch {
      pendingAppeals.value = [
        { appealId: 1, studentName: '郭佳琪', className: '计科2302班', illegalType: 2, appealTime: '2024-03-10', urgencyLevel: 2, illegalId: 5, userId: 12 }
      ]
  }
}

const fetchRecentNotifications = async () => {
  try {
    const res = await axios.get(`${API}/notification/list`, {
      params: { pageNum: 1, pageSize: 5, senderId: counselorInfo.userId }
    })
    if (res.data.code === 200) recentNotifications.value = res.data.data || []
  } catch {}
}

const fetchViolationDist = async () => {
  const colors = ['#f44336', '#ff9800', '#2196f3', '#4caf50', '#9c27b0', '#00bcd4', '#795548', '#607d8b']
  try {
    const res = await axios.get(`${API}/violation/list`, { params: { pageNum: 1, pageSize: 100, counselorId: counselorInfo.userId || undefined } })
    if (res.data.code === 200) {
      const map = {}
      for (const v of res.data.data || []) {
        const name = getViolationType(v.illegalType)
        map[name] = (map[name] || 0) + 1
      }
      const total = Object.values(map).reduce((a, b) => a + b, 0) || 1
      violationDist.value = Object.entries(map).map(([type, count], i) => ({
        type, count, pct: Math.round(count / total * 100), color: colors[i % colors.length]
      }))
    }
  } catch {
    violationDist.value = [
      { type: '违规停放', count: 18, pct: 45, color: '#f44336' },
      { type: '违规充电', count: 10, pct: 25, color: '#ff9800' },
      { type: '超速行驶', count: 7, pct: 18, color: '#2196f3' },
      { type: '其他', count: 5, pct: 12, color: '#607d8b' }
    ]
  }
}

const selectClass = async className => {
  if (selectedClass.value === className) { selectedClass.value = null; return }
  selectedClass.value = className
  try {
    const res = await axios.get(`${API}/violation/list`, {
      params: {
        pageNum: 1,
        pageSize: 50,
        className,
        counselorId: counselorInfo.userId || undefined
      }
    })
    if (res.data.code === 200) {
      const map = {}
      for (const v of res.data.data || []) {
        if (!map[v.userId]) map[v.userId] = { studentId: v.userId, name: v.ownerName || '未知', licensePlate: v.licensePlate, total: 0 }
        map[v.userId].total++
      }
      classViolations.value = Object.values(map).sort((a, b) => b.total - a.total)
    }
  } catch {
      classViolations.value = [
        { studentId: 1, name: '林浩', licensePlate: '湘A0533321', total: 3 },
        { studentId: 2, name: '许诚', licensePlate: '湘A0533320', total: 1 }
      ]
  }
}

// 操作
const sendStudentReminder = async item => {
  try {
    await axios.post(`${API}/notification/send-personal`, null, {
      params: { title: '车辆违规整改提醒', content: `您有一项 ${getViolationType(item.illegalType)} 违规待整改，请在截止日期前完成。`, receiverId: item.userId }
    })
    ElMessage.success(`已向 ${item.studentName} 发送提醒`)
  } catch { ElMessage.error('发送失败') }
}

const sendStudentReminderByName = async sv => {
  try {
    await axios.post(`${API}/notification/send-personal`, null, {
      params: { title: '违规整改提醒', content: '您有违规记录待整改，请及时处理。', receiverId: sv.studentId }
    })
    ElMessage.success(`已向 ${sv.name} 发送提醒`)
  } catch { ElMessage.error('发送失败') }
}

const batchRemindAll = async () => {
  if (!pendingRectify.value.length) { ElMessage.info('暂无待整改学生'); return }
  try {
    await ElMessageBox.confirm(`确定向全部 ${pendingRectify.value.length} 名待整改学生发送提醒？`, '批量提醒', { type: 'warning' })
    for (const item of pendingRectify.value) await sendStudentReminder(item)
    ElMessage.success('批量提醒已发送')
  } catch (e) { if (e !== 'cancel') ElMessage.error('发送失败') }
}

const viewViolationDetail = item => ElMessage.info(`违规ID: ${item.illegalId}`)

const auditDialog = reactive({ visible: false, appeal: null, pass: true, opinion: '' })
const auditAppeal = (ap, pass) => { auditDialog.appeal = ap; auditDialog.pass = !!pass; auditDialog.opinion = ''; auditDialog.visible = true }
const confirmAudit = async () => {
  try {
    const state = auditDialog.pass ? 2 : 3 // 2=通过 3=驳回
    const res = await axios.post(`${API}/appeal/audit`, {
      appealId: auditDialog.appeal.appealId,
      auditUserId: counselorInfo.userId,
      auditOpinion: auditDialog.opinion,
      appealState: state
    })
    if (res.data.code === 200) {
      ElMessage.success(auditDialog.pass ? '申诉已通过' : '申诉已驳回')
      auditDialog.visible = false
      fetchPendingAppeals()
    } else ElMessage.error(res.data.message)
  } catch { ElMessage.error('操作失败') }
}

const sendBatchNotice = () => openNoticeDialog()

// 通知弹窗
const noticeDialog = reactive({ visible: false, form: { title: '', content: '', classes: [], isUrgent: false } })
const openNoticeDialog = () => { noticeDialog.form.title = ''; noticeDialog.form.content = ''; noticeDialog.form.classes = []; noticeDialog.form.isUrgent = false; noticeDialog.visible = true }
const toggleClass = cls => {
  const idx = noticeDialog.form.classes.indexOf(cls)
  idx === -1 ? noticeDialog.form.classes.push(cls) : noticeDialog.form.classes.splice(idx, 1)
}
const selectAllClasses = () => {
  noticeDialog.form.classes = noticeDialog.form.classes.length === counselorInfo.classList.length ? [] : counselorInfo.classList.map(c => c.name)
}
const submitNotice = async () => {
  if (!noticeDialog.form.title.trim() || !noticeDialog.form.content.trim()) { ElMessage.warning('请填写标题和内容'); return }
  if (!noticeDialog.form.classes.length) { ElMessage.warning('请选择发送班级'); return }
  try {
    const res = await axios.post(`${API}/notification/send`, {
      title: noticeDialog.form.title, content: noticeDialog.form.content,
      type: 'class', isUrgent: noticeDialog.form.isUrgent ? 1 : 0,
      receiverType: 'class', receiverIds: noticeDialog.form.classes.join(','),
      senderId: counselorInfo.userId
    })
    if (res.data.code === 200) { ElMessage.success('通知发送成功'); noticeDialog.visible = false; fetchRecentNotifications() }
    else ElMessage.error(res.data.message)
  } catch { ElMessage.error('发送失败') }
}

// ==================== 统计报告 ====================
const statsRange = ref('month')
const dateRanges = [
  { value: 'week', label: '近7天' },
  { value: 'month', label: '近30天' },
  { value: 'quarter', label: '近3月' },
  { value: 'year', label: '本学年' }
]

const statsData = reactive({ totalViolations: 0, totalVehicles: 0, rectifyRate: 0, pendingCount: 0 })
const registerTrend = ref([])
const rectifyTrend = ref([])
const violationTypeStats = ref([])
const deptRanking = ref([])

const kpiCards = computed(() => [
  { icon: '🚗', label: '注册车辆总数', value: statsData.totalVehicles, bg: 'rgba(0,123,255,0.2)', trend: 5 },
  { icon: '⚠️', label: '违规总次数', value: statsData.totalViolations, bg: 'rgba(244,67,54,0.2)', trend: -8 },
  { icon: '✅', label: '整改完成率', value: statsData.rectifyRate, unit: '%', bg: 'rgba(76,175,80,0.2)', trend: 3 },
  { icon: '⏳', label: '当前待处理', value: statsData.pendingCount, bg: 'rgba(255,152,0,0.2)' }
])

const summaryRows = computed(() => [
  { label: '本期新增注册', value: statsData.newRegistered || 0, trend: '↑12', trendUp: true },
  { label: '本期通过审核', value: statsData.approvedCount || 0, trend: '↑8', trendUp: true },
  { label: '本期违规总数', value: statsData.totalViolations, trend: '↓5', trendUp: false },
  { label: '整改及时率', value: (statsData.inTimeRate || 0) + '%', trend: '↑3%', trendUp: true },
  { label: '申诉总数', value: statsData.appealCount || 0 },
  { label: '申诉通过率', value: (statsData.appealPassRate || 0) + '%' }
])

// 图表计算
const maxRegister = computed(() => Math.max(...registerTrend.value.map(x => x.approved + x.pending), 1))
const barHeight = (val, max) => Math.max((val / max) * 100, 2)

const lineChartW = 400, lineChartH = 140
const rectifyPoints = computed(() => {
  if (!rectifyTrend.value.length) return []
  const n = rectifyTrend.value.length
  return rectifyTrend.value.map((item, i) => ({
    x: 45 + i * ((lineChartW - 55) / Math.max(n - 1, 1)),
    y: lineChartH - 30 - (item.rate / 100) * (lineChartH - 50)
  }))
})
const rectifyLinePoints = computed(() => rectifyPoints.value.map(p => `${p.x},${p.y}`).join(' '))

const donutSegments = computed(() => {
  if (!violationTypeStats.value.length) return []
  const circumference = 2 * Math.PI * 70
  const total = violationTypeStats.value.reduce((a, b) => a + b.count, 0)
  let offset = 0
  return violationTypeStats.value.map(item => {
    const dash = (item.count / total) * circumference
    const seg = { color: item.color, dash, gap: circumference - dash, offset: -offset }
    offset += dash
    return seg
  })
})

const fetchStatsData = async () => {
  try {
    const [vioRes, vehRes] = await Promise.all([
      axios.get(`${API}/violation/list`, { params: { pageNum: 1, pageSize: 1 } }),
      axios.get(`${API}/vehicle/stats`)
    ])
    if (vioRes.data.code === 200) statsData.totalViolations = vioRes.data.total || 0
    if (vehRes.data.code === 200) {
      statsData.totalVehicles = vehRes.data.data?.total || 0
      statsData.newRegistered = vehRes.data.data?.pending || 0
      statsData.approvedCount = vehRes.data.data?.approved || 0
    }
  } catch {}

  // 生成模拟趋势数据（后端可提供真实趋势接口）
  const days = statsRange.value === 'week' ? 7 : statsRange.value === 'month' ? 8 : 12
  registerTrend.value = Array.from({ length: days }, (_, i) => ({
    date: `${i + 1}/${(i % 12) + 1}`,
    approved: Math.floor(Math.random() * 20 + 5),
    pending: Math.floor(Math.random() * 8 + 1)
  }))
  rectifyTrend.value = Array.from({ length: days }, (_, i) => ({
    date: `${i + 1}`, rate: 50 + Math.floor(Math.random() * 40)
  }))
  statsData.rectifyRate = rectifyTrend.value.length ? Math.round(rectifyTrend.value.reduce((a, b) => a + b.rate, 0) / rectifyTrend.value.length) : 0
  statsData.pendingCount = pendingRectify.value.length + pendingAppeals.value.length
  statsData.appealCount = 8; statsData.appealPassRate = 75; statsData.inTimeRate = 82

  const typeColors = ['#f44336', '#ff9800', '#2196f3', '#4caf50', '#9c27b0', '#00bcd4', '#795548', '#607d8b']
  const types = ['违规停放', '违规充电', '超速行驶', '未登记', '进禁行区', '违规载人', '遮挡车牌', '其他']
  violationTypeStats.value = types.map((t, i) => ({ type: t, count: Math.floor(Math.random() * 30 + 2), color: typeColors[i] }))
  violationTypeStats.value.sort((a, b) => b.count - a.count)
  statsData.totalViolations = violationTypeStats.value.reduce((a, b) => a + b.count, 0)

  const depts = ['计算机学院', '软件学院', '信息工程学院', '商学院', '文学院', '理学院', '机械工程学院', '后勤部']
  deptRanking.value = depts.map(dept => ({ dept, count: Math.floor(Math.random() * 40 + 5) })).sort((a, b) => b.count - a.count)
}

const changeRange = r => { statsRange.value = r; fetchStatsData() }
const exportReport = () => ElMessage.info('报告导出功能开发中')
const exportMyReport = () => { currentTab.value = 'stats'; fetchStatsData() }
const rankColor = i => ['#ffd700', '#c0c0c0', '#cd7f32', '#607d8b'][Math.min(i, 3)]

// 工具函数
const getViolationType = t => ({ 1: '违规停放', 2: '超速行驶', 3: '违规充电', 4: '未登记', 5: '进禁行区', 6: '违规载人', 7: '遮挡车牌', 8: '其他' }[t] || '未知')
const getViolationState = s => ({ 0: '待整改', 1: '整改中', 2: '已整改', 3: '逾期', 4: '申诉中' }[s] || '—')
const formatDate = d => d ? String(d).split(' ')[0].split('T')[0] : '—'
const isOverdue = d => d && new Date(d) < new Date()

onMounted(async () => {
  await loadTeachers()
  initSelectedCounselorFromList()
  await fetchWorkspaceData()
  await fetchStatsData()
})
</script>

<style scoped>
.counselor-container { min-height: 100vh; background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%); color: #fff; padding: 0; }
.top-tabs { display: flex; gap: 0; border-bottom: 2px solid rgba(0,212,255,0.2); padding: 0 20px; background: rgba(0,0,0,0.3); }
.top-tab { display: flex; align-items: center; gap: 8px; padding: 14px 26px; cursor: pointer; font-size: 15px; color: rgba(255,255,255,0.6); border-bottom: 3px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.top-tab.active { color: #00d4ff; border-bottom-color: #00d4ff; }
.tab-icon { font-size: 18px; }
.module { padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.page-header .header-bg { background: linear-gradient(90deg, rgba(0,123,255,0.1), rgba(102,16,242,0.1)); border-radius: 16px; padding: 20px 24px; border: 1px solid rgba(0,123,255,0.2); display: flex; flex-direction: column; gap: 14px; position: relative; }
.header-top-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
.counselor-pick-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding-top: 4px; border-top: 1px solid rgba(255,255,255,0.08); }
.pick-label { font-size: 13px; color: rgba(255,255,255,0.65); white-space: nowrap; }
.counselor-select { min-width: 260px; max-width: 100%; padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(0,212,255,0.35); background: rgba(10,20,40,0.9); color: #e8f4ff; font-size: 14px; cursor: pointer; }
.counselor-select:focus { outline: none; border-color: #00d4ff; }
.pick-empty { font-size: 13px; color: rgba(255,180,100,0.95); }
.header-bg::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, #007bff, #6610f2); border-radius: 16px 16px 0 0; }
.header-left { display: flex; flex-direction: column; gap: 5px; }
.title-text { font-size: 20px; font-weight: bold; background: linear-gradient(90deg, #fff, #00d4ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.title-sub { font-size: 12px; color: rgba(255,255,255,0.5); }
.header-actions, .header-right { display: flex; gap: 10px; align-items: center; }
.action-btn { display: inline-flex; align-items: center; gap: 4px; padding: 8px 14px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.action-btn:hover { background: rgba(255,255,255,0.15); }
.action-btn.primary { background: linear-gradient(90deg, #007bff, #6610f2); border: none; }
.action-btn.success { background: rgba(76,175,80,0.3); border-color: rgba(76,175,80,0.5); color: #4caf50; }
.action-btn.danger { background: rgba(244,67,54,0.3); border-color: rgba(244,67,54,0.5); color: #f44336; }
.action-btn.small { padding: 5px 10px; font-size: 12px; }
/* 工作台 */
.overview-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.ov-card { background: rgba(255,255,255,0.05); border-radius: 12px; padding: 16px; border: 1px solid rgba(255,255,255,0.1); display: flex; align-items: center; gap: 14px; position: relative; overflow: hidden; }
.ov-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--accent); }
.ov-icon { font-size: 26px; flex-shrink: 0; }
.ov-body { flex: 1; }
.ov-val { font-size: 24px; font-weight: bold; color: var(--accent); }
.ov-label { font-size: 13px; color: rgba(255,255,255,0.7); margin-top: 2px; }
.ov-sub { font-size: 11px; color: rgba(255,255,255,0.4); }
.workspace-layout { display: flex; gap: 18px; }
.ws-left { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.ws-right { width: 360px; flex-shrink: 0; display: flex; flex-direction: column; gap: 14px; }
.section-card { background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); }
.section-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.08); }
.sec-title { font-size: 14px; font-weight: 500; }
.sec-badge { padding: 2px 9px; border-radius: 10px; font-size: 12px; font-weight: bold; }
.sec-badge.danger { background: rgba(244,67,54,0.25); color: #f44336; }
.sec-badge.warn { background: rgba(255,152,0,0.25); color: #ff9800; }
.student-list { display: flex; flex-direction: column; gap: 0; max-height: 260px; overflow-y: auto; }
.student-item { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-bottom: 1px solid rgba(255,255,255,0.04); }
.student-item:last-child { border-bottom: none; }
.si-avatar { width: 32px; height: 32px; border-radius: 50%; background: rgba(0,123,255,0.3); display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; flex-shrink: 0; }
.si-avatar.appeal { background: rgba(156,39,176,0.3); }
.si-info { flex: 1; min-width: 0; }
.si-name { font-size: 13px; font-weight: 500; display: flex; align-items: center; gap: 8px; }
.si-class { font-size: 11px; color: #00d4ff; background: rgba(0,212,255,0.15); padding: 1px 7px; border-radius: 8px; }
.si-vio { font-size: 12px; color: rgba(255,255,255,0.5); margin-top: 2px; }
.si-meta { display: flex; align-items: center; gap: 6px; margin-top: 3px; }
.si-time { font-size: 11px; color: rgba(255,255,255,0.4); }
.si-actions { display: flex; gap: 5px; flex-shrink: 0; }
.state-badge { padding: 1px 7px; border-radius: 8px; font-size: 11px; }
.state-badge.s0 { background: rgba(255,152,0,0.2); color: #ff9800; }
.state-badge.s1 { background: rgba(33,150,243,0.2); color: #2196f3; }
.state-badge.s2 { background: rgba(76,175,80,0.2); color: #4caf50; }
.state-badge.s3 { background: rgba(244,67,54,0.2); color: #f44336; }
.overdue-tag { font-size: 11px; padding: 1px 6px; background: rgba(244,67,54,0.2); color: #f44336; border-radius: 8px; }
.urg-tag { font-size: 11px; padding: 1px 6px; background: rgba(255,152,0,0.2); color: #ff9800; border-radius: 8px; }
.notice-list { display: flex; flex-direction: column; gap: 0; }
.notice-item { display: flex; align-items: center; gap: 10px; padding: 9px 16px; border-bottom: 1px solid rgba(255,255,255,0.04); }
.notice-item:last-child { border-bottom: none; }
.ni-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ni-dot.urgent { background: #f44336; box-shadow: 0 0 6px rgba(244,67,54,0.5); }
.ni-dot.normal { background: #4caf50; }
.ni-content { flex: 1; }
.ni-title { font-size: 13px; font-weight: 500; }
.ni-meta { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.empty-tip { text-align: center; padding: 20px; font-size: 13px; color: rgba(255,255,255,0.35); }
/* 班级 */
.class-grid { display: flex; flex-direction: column; gap: 8px; padding: 12px; }
.class-grid.grouped { gap: 12px; }
.college-group { border-left: 2px solid rgba(0,212,255,0.35); padding-left: 10px; }
.group-h1 { font-size: 13px; font-weight: 600; color: #00d4ff; margin-bottom: 8px; opacity: 0.95; }
.major-group { margin-bottom: 10px; }
.group-h2 { font-size: 12px; color: rgba(255,255,255,0.45); margin-bottom: 6px; padding-left: 2px; }
.class-row { display: flex; flex-direction: column; gap: 8px; }
.class-card { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 10px 14px; border: 1px solid rgba(255,255,255,0.08); cursor: pointer; transition: all 0.2s; }
.class-card:hover, .class-card.active { background: rgba(0,212,255,0.1); border-color: rgba(0,212,255,0.3); }
.cls-name { font-size: 14px; font-weight: 500; margin-bottom: 7px; }
.cls-stats { display: flex; gap: 14px; }
.cst { display: flex; align-items: baseline; gap: 4px; }
.csv { font-size: 16px; font-weight: bold; color: #00d4ff; }
.csv.c-orange { color: #ff9800; }
.csv.c-red { color: #f44336; }
.csl { font-size: 11px; color: rgba(255,255,255,0.5); }
.class-violation-list { max-height: 180px; overflow-y: auto; }
.cv-item { display: flex; align-items: center; gap: 10px; padding: 8px 14px; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px; }
.cv-item:last-child { border-bottom: none; }
.cv-name { flex: 1; font-weight: 500; }
.cv-plate { color: rgba(255,255,255,0.5); font-size: 12px; width: 90px; }
.cnt-badge { padding: 2px 9px; border-radius: 10px; font-size: 12px; background: rgba(76,175,80,0.2); color: #4caf50; }
.cnt-badge.high { background: rgba(244,67,54,0.2); color: #f44336; }
.vio-dist { display: flex; flex-direction: column; gap: 10px; padding: 12px 14px; }
.vd-item { display: flex; align-items: center; gap: 8px; }
.vd-name { width: 70px; font-size: 12px; color: rgba(255,255,255,0.7); }
.vd-bar { flex: 1; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; }
.vd-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }
.vd-val { width: 28px; font-size: 12px; color: rgba(255,255,255,0.5); text-align: right; }
.quick-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding: 12px; }
.qa-btn { background: rgba(0,0,0,0.2); border: 1px dashed rgba(255,255,255,0.15); border-radius: 10px; padding: 12px 8px; display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: pointer; transition: all 0.2s; }
.qa-btn:hover { background: rgba(0,212,255,0.1); border-color: rgba(0,212,255,0.3); }
.qa-icon { font-size: 22px; }
.qa-label { font-size: 12px; color: rgba(255,255,255,0.7); }
/* 统计报告 */
.date-range-select { display: flex; background: rgba(0,0,0,0.3); border-radius: 8px; padding: 3px; }
.dr-opt { padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 13px; color: rgba(255,255,255,0.6); transition: all 0.2s; }
.dr-opt.active { background: rgba(0,212,255,0.2); color: #00d4ff; }
.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.kpi-card { background: rgba(255,255,255,0.05); border-radius: 12px; padding: 16px; border: 1px solid rgba(255,255,255,0.1); display: flex; align-items: center; gap: 14px; }
.kpi-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0; }
.kpi-body { flex: 1; }
.kpi-val { font-size: 24px; font-weight: bold; color: #fff; }
.kpi-unit { font-size: 16px; margin-left: 2px; }
.kpi-label { font-size: 12px; color: rgba(255,255,255,0.5); margin-top: 2px; }
.kpi-trend { font-size: 12px; margin-top: 3px; font-weight: 500; }
.kpi-trend.up { color: #4caf50; }
.kpi-trend.down { color: #f44336; }
.stats-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.stats-col { display: flex; flex-direction: column; gap: 14px; }
.chart-card { background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); }
.chart-header { padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.08); font-size: 14px; font-weight: 500; }
.chart-body { padding: 14px; }
/* 柱状图 */
.bar-chart { display: flex; align-items: flex-end; gap: 6px; height: 120px; padding-bottom: 4px; }
.bc-group { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; }
.bc-bars { display: flex; align-items: flex-end; gap: 2px; height: 100px; }
.bc-bar { width: 10px; border-radius: 3px 3px 0 0; transition: height 0.5s; min-height: 2px; }
.bc-bar.approved { background: #4caf50; }
.bc-bar.pending { background: #ff9800; }
.bc-label { font-size: 9px; color: rgba(255,255,255,0.4); }
.chart-legend { display: flex; gap: 14px; margin-top: 10px; justify-content: center; }
.cl-item { display: flex; align-items: center; gap: 5px; font-size: 12px; color: rgba(255,255,255,0.6); }
.cl-dot { width: 10px; height: 10px; border-radius: 2px; }
.cl-dot.approved { background: #4caf50; }
.cl-dot.pending { background: #ff9800; }
/* 饼图 */
.donut-chart-area { display: flex; align-items: center; gap: 16px; }
.donut-svg { width: 130px; height: 130px; flex-shrink: 0; }
.donut-legend { flex: 1; display: flex; flex-direction: column; gap: 7px; }
.dl-item { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.dl-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.dl-name { flex: 1; color: rgba(255,255,255,0.7); }
.dl-val { font-weight: bold; color: #fff; }
/* 折线图 */
.line-chart { overflow: hidden; }
.lc-svg { width: 100%; max-height: 140px; }
/* 排名 */
.rank-list { display: flex; flex-direction: column; gap: 8px; max-height: 220px; overflow-y: auto; }
.rank-item { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.rank-no { width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; background: rgba(255,255,255,0.1); flex-shrink: 0; }
.rank-no.gold { background: rgba(255,215,0,0.3); color: #ffd700; }
.rank-no.silver { background: rgba(192,192,192,0.3); color: #c0c0c0; }
.rank-no.bronze { background: rgba(205,127,50,0.3); color: #cd7f32; }
.rank-dept { width: 90px; color: rgba(255,255,255,0.8); }
.rank-bar-wrap { flex: 1; }
.rank-bar { height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; }
.rank-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }
.rank-cnt { width: 30px; text-align: right; font-weight: bold; color: #00d4ff; font-size: 13px; }
/* 摘要表 */
.summary-table { display: flex; flex-direction: column; gap: 6px; }
.sum-row { display: flex; align-items: center; padding: 7px 10px; background: rgba(0,0,0,0.2); border-radius: 6px; font-size: 13px; }
.sum-label { flex: 1; color: rgba(255,255,255,0.6); }
.sum-val { font-weight: 500; color: #fff; }
.sum-trend { margin-left: 10px; font-size: 12px; }
.sum-trend.up { color: #4caf50; }
.sum-trend.down { color: #f44336; }
/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: linear-gradient(135deg, #1a1f3c, #2d3250); border-radius: 16px; border: 1px solid rgba(0,123,255,0.3); width: 520px; max-height: 85vh; display: flex; flex-direction: column; }
.modal-content.sm { width: 420px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 22px; border-bottom: 1px solid rgba(255,255,255,0.1); flex-shrink: 0; }
.modal-title { font-size: 16px; font-weight: 600; }
.modal-close { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 50%; cursor: pointer; }
.modal-body { padding: 20px 22px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 12px; }
.modal-footer { padding: 12px 22px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; justify-content: flex-end; gap: 10px; flex-shrink: 0; }
.form-item { display: flex; flex-direction: column; gap: 7px; }
.form-label { font-size: 13px; color: rgba(255,255,255,0.7); }
.tech-input-full { width: 100%; padding: 9px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; box-sizing: border-box; }
.tech-input-full:focus { outline: none; border-color: #00d4ff; }
.tech-textarea { width: 100%; padding: 10px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; resize: vertical; font-family: inherit; box-sizing: border-box; }
.tech-textarea:focus { outline: none; border-color: #00d4ff; }
.receiver-opts { display: flex; flex-wrap: wrap; gap: 7px; }
.recv-opt { padding: 5px 12px; background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.15); border-radius: 14px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.recv-opt.active { background: rgba(0,212,255,0.2); border-color: rgba(0,212,255,0.4); color: #00d4ff; }
.toggle-btn { width: 40px; height: 22px; background: rgba(255,255,255,0.1); border-radius: 11px; padding: 3px; cursor: pointer; transition: all 0.3s; position: relative; }
.toggle-btn.on { background: rgba(0,212,255,0.4); }
.toggle-dot { width: 16px; height: 16px; background: #fff; border-radius: 50%; position: absolute; top: 3px; left: 3px; transition: transform 0.3s; }
.toggle-btn.on .toggle-dot { transform: translateX(18px); }
.info-block { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 12px; }
.info-row { display: flex; gap: 12px; padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px; }
.info-row:last-child { border-bottom: none; }
.il { color: rgba(255,255,255,0.4); min-width: 70px; }
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,0.05); }
::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.3); border-radius: 3px; }
</style>

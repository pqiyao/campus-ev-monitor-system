<template>
  <div class="user-container page-bg-shell">
    <div class="page-header">
      <div class="header-bg">
        <div class="header-left">
          <h1 class="title-text">用户管理</h1>
        </div>
        <div class="header-stats">
          <div class="stat-item" v-for="s in roleStats" :key="s.label">
            <span class="stat-num" :style="{ color: s.color }">{{ s.value }}</span>
            <span class="stat-lab">{{ s.label }}</span>
          </div>
        </div>
        <div class="header-actions">
          <div class="action-btn" @click="fetchUsers">↻ 刷新</div>
          <div class="action-btn primary" @click="openCreateDialog">+ 新建账号</div>
          <div class="action-btn" @click="exportUsers">📊 导出</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <input v-model="filter.keyword" class="tech-input" placeholder="搜索用户名/姓名/手机/学号/工号" style="width:240px" @keyup.enter="fetchUsers" />
      <select v-model="filter.userType" class="tech-select" @change="fetchUsers">
        <option value="">全部角色</option>
        <option value="admin">管理员</option>
        <option value="teacher">导员</option>
        <option value="service">检查员</option>
        <option value="student">学生</option>
        <option value="temporary">临时人员</option>
        <option value="worker">工作人员</option>
      </select>
      <select v-model="filter.status" class="tech-select" @change="fetchUsers">
        <option value="">全部状态</option>
        <option value="active">正常</option>
        <option value="frozen">冻结</option>
      </select>
      <select v-model="filter.department" class="tech-select" @change="fetchUsers">
        <option value="">全部部门</option>
        <option v-for="d in departments" :key="d" :value="d">{{ d }}</option>
      </select>
      <div class="action-btn primary" @click="fetchUsers">🔍 搜索</div>
      <div class="action-btn" @click="resetFilter">重置</div>
    </div>

    <!-- 标签切换 -->
    <div class="role-tabs">
      <div v-for="rt in roleTabs" :key="rt.value" class="role-tab"
        :class="{ active: activeRoleTab === rt.value }"
        @click="switchRoleTab(rt.value)">
        <span class="rt-icon">{{ rt.icon }}</span>
        <span>{{ rt.label }}</span>
        <span class="rt-count" :style="{ background: rt.color }">{{ rt.count }}</span>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="table-container">
      <div class="table-header">
        <span class="table-title">用户列表</span>
        <div class="page-controls">
          <span class="total-info">共 {{ pagination.total }} 条</span>
          <div class="page-btn" :class="{ disabled: pagination.page === 1 }" @click="changePage(-1)">←</div>
          <span class="page-info">{{ pagination.page }} / {{ pagination.totalPages }}</span>
          <div class="page-btn" :class="{ disabled: pagination.page === pagination.totalPages }" @click="changePage(1)">→</div>
        </div>
      </div>
      <div class="table-wrapper">
        <table class="tech-table">
          <thead>
            <tr>
              <th class="th-score" title="校园通行记分，满分 12">剩余记分</th>
              <th>用户ID</th><th>头像/用户名</th><th>真实姓名</th><th>角色</th>
              <th>学号/工号/编号</th><th>联系方式</th><th>部门/院系</th>
              <th>注册时间</th><th>状态</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in userList" :key="user.userId">
              <td class="td-score">
                <span class="score-pill" :class="scoreClass(user)">{{ formatLicenseRemain(user) }}/12</span>
              </td>
              <td><span class="id-badge">#{{ user.userId }}</span></td>
              <td>
                <div class="user-cell">
                  <div class="avatar" :style="{ background: getAvatarColor(user.userType) }">
                    {{ (user.realName || user.username || '?').charAt(0) }}
                  </div>
                  <div>
                    <div class="username">{{ user.username }}</div>
                    <div class="user-dept">{{ user.department || '' }}</div>
                  </div>
                </div>
              </td>
              <td>{{ user.realName || '—' }}</td>
              <td>
                <span class="role-badge" :class="user.userType">{{ getRoleLabel(user.userType) }}</span>
              </td>
              <td>
                <span class="id-code">{{ user.studentId || user.jobId || '—' }}</span>
              </td>
              <td>
                <div class="contact-cell">
                  <div>{{ user.phone || '—' }}</div>
                  <div class="contact-email">{{ user.email || '' }}</div>
                </div>
              </td>
              <td>{{ user.department || '—' }}</td>
              <td>{{ formatDate(user.createTime) }}</td>
              <td>
                <span class="status-badge" :class="user.status">{{ getStatusLabel(user.status) }}</span>
              </td>
              <td>
                <div class="action-cell">
                  <div class="action-btn small" @click="editUser(user)">✏️ 编辑</div>
                  <div class="action-btn small" @click="changeRole(user)">🎭 角色</div>
                  <div class="action-btn small" :class="user.status === 'active' ? 'warn' : 'success'" @click="toggleFreeze(user)">
                    {{ user.status === 'active' ? '🔒 冻结' : '🔓 解冻' }}
                  </div>
                  <div class="action-btn small danger" @click="resetPwd(user)">🔑 重置</div>
                  <div class="action-btn small danger" @click="deleteUser(user)">🗑 删除</div>
                  <div class="action-btn small" @click="viewUserDetail(user)">👁 详情</div>
                </div>
              </td>
            </tr>
            <tr v-if="userList.length === 0"><td colspan="11" class="empty-row">暂无用户数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 导员-班级绑定配置（始终显示，避免找不到入口；与学生「班级名称」一致） -->
    <div class="panel">
      <div class="panel-header">
        <span>🎓 导员–班级绑定配置</span>
        <div class="action-btn small primary" @click="addBinding">+ 新增绑定</div>
      </div>
      <div class="panel-body">
        <div class="binding-grid">
          <div class="binding-card" v-for="b in counselorBindings" :key="b.counselorUserId">
            <div class="binding-top">
              <div class="counselor-name">{{ b.counselorName }} <span class="bind-id">#{{ b.counselorUserId }}</span></div>
            </div>
            <div class="class-tags">
              <span class="class-tag" v-for="r in b.rows" :key="r.id">
                <span class="tag-txt">{{ r.className }}</span>
                <span class="tag-x" @click.stop="removeClassBinding(r.id)" title="删除该班级绑定">×</span>
              </span>
            </div>
            <div class="bind-stat">管辖约 {{ b.studentCount }} 名学生（按学生「班级名称」统计）</div>
          </div>
          <div class="binding-card add-card" @click="openBindDialog">
            <span class="add-icon">+</span><span>添加导员班级绑定</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 临时人员管理面板 -->
    <div class="panel" v-if="activeRoleTab === 'temporary' || activeRoleTab === 'all'">
      <div class="panel-header">
        <span>⏰ 临时人员管理</span>
        <div class="action-btn small primary" @click="openCreateDialog('temporary')">+ 添加临时人员</div>
      </div>
      <div class="panel-body">
        <table class="tech-table">
          <thead>
            <tr><th>姓名</th><th>证件类型</th><th>联系方式</th><th>来访原因</th><th>有效期</th><th>审批状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="tmp in temporaryUsers" :key="tmp.userId">
              <td>{{ tmp.realName }}</td>
              <td>{{ tmp.idType || '身份证' }}</td>
              <td>{{ tmp.phone }}</td>
              <td>{{ tmp.visitReason || '—' }}</td>
              <td><span :class="{ 'expire-warn': isExpireSoon(tmp.expireDate) }">{{ tmp.expireDate || '未设置' }}</span></td>
              <td><span class="status-badge" :class="tmp.status">{{ getStatusLabel(tmp.status) }}</span></td>
              <td>
                <div class="action-cell">
                  <div class="action-btn small" @click="editUser(tmp)">编辑</div>
                  <div class="action-btn small danger" @click="toggleFreeze(tmp)">{{ tmp.status === 'active' ? '停用' : '启用' }}</div>
                  <div class="action-btn small danger" @click="deleteUser(tmp)">删除</div>
                </div>
              </td>
            </tr>
            <tr v-if="temporaryUsers.length === 0"><td colspan="7" class="empty-row">暂无临时人员</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新建/编辑用户弹窗 -->
    <div v-if="userDialog.visible" class="modal-overlay" @click.self="userDialog.visible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">{{ userDialog.isEdit ? '✏️ 编辑用户' : '+ 新建账号' }}</span>
          <span class="modal-close" @click="userDialog.visible = false">×</span>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-item">
              <label class="form-label">用户名 *</label>
              <input v-model="userDialog.form.username" class="tech-input" placeholder="登录用户名" :disabled="userDialog.isEdit" />
            </div>
            <div class="form-item" v-if="!userDialog.isEdit">
              <label class="form-label">初始密码</label>
              <input type="password" v-model="userDialog.form.password" class="tech-input" placeholder="默认 123456" />
            </div>
            <div class="form-item">
              <label class="form-label">真实姓名 *</label>
              <input v-model="userDialog.form.realName" class="tech-input" placeholder="真实姓名" />
            </div>
            <div class="form-item">
              <label class="form-label">角色 *</label>
              <select v-model="userDialog.form.userType" class="tech-select-full" @change="onUserDialogTypeChangeCard">
                <option value="student">学生</option>
                <option value="teacher">导员</option>
                <option value="service">检查员</option>
                <option value="worker">工作人员</option>
                <option value="temporary">临时人员</option>
                <option value="admin">管理员</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">{{ getIdLabel(userDialog.form.userType) }}</label>
              <input v-model="userDialog.form.identifier" class="tech-input" :placeholder="'请输入' + getIdLabel(userDialog.form.userType)" />
            </div>
            <div class="form-item">
              <label class="form-label">手机号 *</label>
              <input v-model="userDialog.form.phone" class="tech-input" placeholder="手机号" />
            </div>
            <div class="form-item">
              <label class="form-label">邮箱</label>
              <input v-model="userDialog.form.email" class="tech-input" placeholder="邮箱" />
            </div>
            <div class="form-item">
              <label class="form-label">部门/院系</label>
              <select v-model="userDialog.form.department" class="tech-select-full">
                <option value="">请选择</option>
                <option v-for="d in departments" :key="d" :value="d">{{ d }}</option>
              </select>
            </div>
            <template v-if="userDialog.form.userType === 'student'">
              <div class="form-item" style="grid-column: 1 / -1">
                <label class="form-label">学籍班级 *</label>
                <select v-model.number="studentOrgSelectIndex" class="tech-select-full" @change="syncStudentOrgFromSelectCard">
                  <option :value="-1">请选择</option>
                  <option v-for="(o, i) in orgClassOptions" :key="i" :value="i">{{ o.college }} · {{ o.major }} · {{ o.className }}</option>
                </select>
              </div>
            </template>
            <!-- 临时人员专有字段 -->
            <div v-if="userDialog.form.userType === 'temporary'" class="form-item">
              <label class="form-label">来访原因</label>
              <input v-model="userDialog.form.visitReason" class="tech-input" placeholder="来访事由" />
            </div>
            <div v-if="userDialog.form.userType === 'temporary'" class="form-item">
              <label class="form-label">有效期至</label>
              <input type="date" v-model="userDialog.form.expireDate" class="tech-input" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="userDialog.visible = false">取消</div>
          <div class="action-btn primary" @click="submitUser">{{ userDialog.isEdit ? '保存修改' : '创建账号' }}</div>
        </div>
      </div>
    </div>

    <!-- 导员班级绑定 -->
    <div v-if="bindDialog.visible" class="modal-overlay" @click.self="bindDialog.visible = false">
      <div class="modal-content sm" @click.stop>
        <div class="modal-header">
          <span class="modal-title">🎓 新增导员–班级绑定</span>
          <span class="modal-close" @click="bindDialog.visible = false">×</span>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label class="form-label">导员 *</label>
            <select v-model.number="bindDialog.form.counselorUserId" class="tech-select-full">
              <option v-for="t in teacherList" :key="t.userId" :value="t.userId">{{ t.realName || t.username }} (#{{ t.userId }})</option>
            </select>
          </div>
          <div class="form-item" style="margin-top:12px">
            <label class="form-label">选择班级 *</label>
            <select v-model.number="bindOrgSelectIndex" class="tech-select-full">
              <option :value="-1">请选择（与学生注册下拉同源）</option>
              <option v-for="(o, i) in orgClassOptions" :key="i" :value="i">{{ o.college }} · {{ o.major }} · {{ o.className }}</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="bindDialog.visible = false">取消</div>
          <div class="action-btn primary" @click="submitBind">保存</div>
        </div>
      </div>
    </div>

    <!-- 角色分配弹窗 -->
    <div v-if="roleDialog.visible" class="modal-overlay" @click.self="roleDialog.visible = false">
      <div class="modal-content sm" @click.stop>
        <div class="modal-header">
          <span class="modal-title">🎭 角色分配</span>
          <span class="modal-close" @click="roleDialog.visible = false">×</span>
        </div>
        <div class="modal-body">
          <div class="role-user-info">
            <div class="ru-name">{{ roleDialog.user?.username }}</div>
            <div class="ru-cur">当前：<span class="role-badge" :class="roleDialog.user?.userType">{{ getRoleLabel(roleDialog.user?.userType) }}</span></div>
          </div>
          <div class="role-options">
            <div v-for="r in roleOptions" :key="r.value" class="role-opt"
              :class="{ active: roleDialog.newRole === r.value }"
              @click="roleDialog.newRole = r.value">
              <div class="ro-icon">{{ r.icon }}</div>
              <div class="ro-info">
                <div class="ro-name">{{ r.label }}</div>
                <div class="ro-desc">{{ r.desc }}</div>
              </div>
              <span v-if="roleDialog.newRole === r.value" class="ro-check">✓</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="roleDialog.visible = false">取消</div>
          <div class="action-btn primary" @click="submitRole">确认分配</div>
        </div>
      </div>
    </div>

    <!-- 用户详情弹窗 -->
    <div v-if="detailDialog.visible" class="modal-overlay" @click.self="detailDialog.visible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">👤 用户详情</span>
          <span class="modal-close" @click="detailDialog.visible = false">×</span>
        </div>
        <div class="modal-body" v-if="detailDialog.user">
          <div class="user-profile">
            <div class="profile-avatar" :style="{ background: getAvatarColor(detailDialog.user.userType) }">
              {{ (detailDialog.user.realName || detailDialog.user.username || '?').charAt(0) }}
            </div>
            <div class="profile-info">
              <div class="profile-name">{{ detailDialog.user.realName }}</div>
              <div class="profile-username">@{{ detailDialog.user.username }}</div>
              <span class="role-badge" :class="detailDialog.user.userType">{{ getRoleLabel(detailDialog.user.userType) }}</span>
            </div>
          </div>
          <div class="detail-grid">
            <div class="dr"><span class="dl">手机</span><span class="dv">{{ detailDialog.user.phone }}</span></div>
            <div class="dr"><span class="dl">邮箱</span><span class="dv">{{ detailDialog.user.email || '—' }}</span></div>
            <div class="dr"><span class="dl">部门</span><span class="dv">{{ detailDialog.user.department || '—' }}</span></div>
            <div class="dr"><span class="dl">学号/工号</span><span class="dv">{{ detailDialog.user.studentId || detailDialog.user.jobId || '—' }}</span></div>
            <div class="dr"><span class="dl">状态</span><span class="dv"><span class="status-badge" :class="detailDialog.user.status">{{ getStatusLabel(detailDialog.user.status) }}</span></span></div>
            <div class="dr"><span class="dl">注册时间</span><span class="dv">{{ formatDate(detailDialog.user.createTime) }}</span></div>
            <div class="dr">
              <span class="dl">校园记分</span>
              <span class="dv"><span class="score-pill" :class="scoreClass(detailDialog.user)">{{ formatLicenseRemain(detailDialog.user) }}/12</span>（违规扣分、申诉通过恢复、公益/管理员加分）</span>
            </div>
          </div>
          <div class="user-stats">
            <div class="us-item"><div class="us-val">{{ detailDialog.stats.vehicleCount || 0 }}</div><div class="us-lab">注册车辆</div></div>
            <div class="us-item"><div class="us-val us-red">{{ detailDialog.stats.violationCount || 0 }}</div><div class="us-lab">违规记录</div></div>
            <div class="us-item"><div class="us-val">{{ detailDialog.stats.notificationCount || 0 }}</div><div class="us-lab">收到通知</div></div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="detailDialog.visible = false">关闭</div>
          <div class="action-btn primary" @click="editUser(detailDialog.user); detailDialog.visible = false">编辑用户</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { getApiBaseUrl } from '@/utils/apiBase'

const API = getApiBaseUrl()
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`

// ==================== 数据 ====================
const userList = ref([])
const filter = reactive({ keyword: '', userType: '', status: '', department: '' })
const pagination = reactive({ page: 1, pageSize: 15, total: 0, totalPages: 1 })
const activeRoleTab = ref('all')

const departments = ['计算机学院', '软件学院', '信息工程学院', '商学院', '文学院', '理学院', '机械工程学院', '后勤部', '保卫处', '教务处', '学生处', '其他']

const roleStats = ref([
  { label: '管理员', value: 0, color: '#f44336' },
  { label: '导员', value: 0, color: '#ff9800' },
  { label: '检查员', value: 0, color: '#2196f3' },
  { label: '工作人员', value: 0, color: '#00bcd4' },
  { label: '学生', value: 0, color: '#4caf50' },
  { label: '临时人员', value: 0, color: '#9c27b0' },
  { label: '总计', value: 0, color: '#00d4ff' },
])

const roleTabs = computed(() => [
  { value: 'all', label: '全部', icon: '👥', count: roleStats.value[6].value, color: '#607d8b' },
  { value: 'admin', label: '管理员', icon: '👑', count: roleStats.value[0].value, color: '#f44336' },
  { value: 'teacher', label: '导员', icon: '🎓', count: roleStats.value[1].value, color: '#ff9800' },
  { value: 'service', label: '检查员', icon: '🔍', count: roleStats.value[2].value, color: '#2196f3' },
  { value: 'worker', label: '工作人员', icon: '🔧', count: roleStats.value[3].value, color: '#00bcd4' },
  { value: 'student', label: '学生', icon: '🧑‍🎓', count: roleStats.value[4].value, color: '#4caf50' },
  { value: 'temporary', label: '临时人员', icon: '⏰', count: roleStats.value[5].value, color: '#9c27b0' }
])

const roleOptions = [
  { value: 'admin', label: '管理员', icon: '👑', desc: '系统全权管理' },
  { value: 'teacher', label: '导员', icon: '🎓', desc: '管理所辖学生、跟进违规整改' },
  { value: 'service', label: '检查员', icon: '🔍', desc: '违规核实与整改审核' },
  { value: 'worker', label: '工作人员', icon: '🔧', desc: '后勤、行政等工作人员' },
  { value: 'student', label: '学生', icon: '🧑‍🎓', desc: '普通用户，管理自己账户' },
  { value: 'temporary', label: '临时人员', icon: '⏰', desc: '访客、临时工作人员' }
]

const counselorBindings = ref([])
const teacherList = ref([])
const orgClassOptions = ref([])
const studentOrgSelectIndex = ref(-1)
const bindOrgSelectIndex = ref(-1)
const bindDialog = reactive({
  visible: false,
  form: { counselorUserId: null }
})

const loadOrgClassOptions = async () => {
  try {
    const res = await axios.get(`${API}/org-class/options`)
    if (res.data.code === 200) orgClassOptions.value = res.data.data || []
  } catch {
    orgClassOptions.value = []
  }
}

const findStudentOrgIndexCard = () => {
  const c = (userDialog.form.college || '').trim()
  const m = (userDialog.form.major || '').trim()
  const cn = (userDialog.form.className || '').trim()
  return orgClassOptions.value.findIndex(
    (o) =>
      (o.college || '').trim() === c &&
      (o.major || '').trim() === m &&
      (o.className || '').trim() === cn
  )
}

const syncStudentOrgFromSelectCard = () => {
  const i = studentOrgSelectIndex.value
  if (i < 0) {
    userDialog.form.college = ''
    userDialog.form.major = ''
    userDialog.form.className = ''
    return
  }
  const o = orgClassOptions.value[i]
  if (o) {
    userDialog.form.college = o.college || ''
    userDialog.form.major = o.major || ''
    userDialog.form.className = o.className || ''
  }
}

const onUserDialogTypeChangeCard = () => {
  if (userDialog.form.userType !== 'student') {
    studentOrgSelectIndex.value = -1
    userDialog.form.college = ''
    userDialog.form.major = ''
    userDialog.form.className = ''
  }
}

const temporaryUsers = ref([])

const userDialog = reactive({
  visible: false, isEdit: false,
  form: { username: '', password: '123456', realName: '', userType: 'student', identifier: '', phone: '', email: '', department: '', college: '', major: '', className: '', userId: null, visitReason: '', expireDate: '' }
})
const roleDialog = reactive({ visible: false, user: null, newRole: '' })
const detailDialog = reactive({ visible: false, user: null, stats: {} })

// ==================== 数据获取 ====================
const fetchUsers = async () => {
  try {
    const params = {
      keyword: filter.keyword || undefined,
      userType: (activeRoleTab.value !== 'all' ? activeRoleTab.value : filter.userType) || undefined,
      status: filter.status || undefined,
      pageNum: pagination.page, pageSize: pagination.pageSize
    }
    Object.keys(params).forEach(k => params[k] === undefined && delete params[k])
    const res = await axios.get(`${API}/user/list`, { params })
    if (res.data.code === 200) {
      userList.value = res.data.data || []
      pagination.total = res.data.total || 0
      pagination.totalPages = Math.max(1, res.data.totalPages || 1)
      // 分离临时人员
      temporaryUsers.value = userList.value.filter(u => u.userType === 'temporary')
      fetchStats()
      fetchBindings()
    }
  } catch { ElMessage.error('获取用户数据失败') }
}

const fetchBindings = async () => {
  try {
    const res = await axios.get(`${API}/counselor/bindings`)
    if (res.data.code === 200) counselorBindings.value = res.data.data || []
  } catch { counselorBindings.value = [] }
}

const loadTeachers = async () => {
  try {
    const res = await axios.get(`${API}/user/list`, { params: { userType: 'teacher', pageNum: 1, pageSize: 200 } })
    if (res.data.code === 200) teacherList.value = res.data.data || []
  } catch { teacherList.value = [] }
}

const openBindDialog = async () => {
  await loadTeachers()
  if (!teacherList.value.length) {
    ElMessage.warning('暂无导员账号，请先创建角色为「导员」的用户')
    return
  }
  await loadOrgClassOptions()
  bindOrgSelectIndex.value = -1
  bindDialog.form = {
    counselorUserId: teacherList.value[0].userId
  }
  bindDialog.visible = true
}

const submitBind = async () => {
  if (bindOrgSelectIndex.value < 0) {
    ElMessage.warning('请从下拉选择班级')
    return
  }
  const o = orgClassOptions.value[bindOrgSelectIndex.value]
  if (!o?.className?.trim()) {
    ElMessage.warning('班级数据无效')
    return
  }
  try {
    const res = await axios.post(`${API}/counselor/class`, {
      counselorUserId: bindDialog.form.counselorUserId,
      college: o.college || undefined,
      major: o.major || undefined,
      className: o.className.trim()
    })
    if (res.data.code === 200) {
      ElMessage.success('绑定已保存')
      bindDialog.visible = false
      fetchBindings()
    } else ElMessage.error(res.data.message || '保存失败')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败（班级是否已绑定该导员？）')
  }
}

const removeClassBinding = async id => {
  try {
    await ElMessageBox.confirm('确定删除该班级绑定？', '删除绑定', { type: 'warning' })
    const res = await axios.delete(`${API}/counselor/class/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除')
      fetchBindings()
    } else ElMessage.error(res.data.message)
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

const fetchStats = async () => {
  try {
    const res = await axios.get(`${API}/user/stats`)
    if (res.data.code === 200) {
      const ts = res.data.data?.typeStats || []
      const getCount = t => ts.find(s => s.type === t)?.count || 0
      roleStats.value[0].value = getCount('admin')
      roleStats.value[1].value = getCount('teacher')
      roleStats.value[2].value = getCount('service')
      roleStats.value[3].value = getCount('worker')
      roleStats.value[4].value = getCount('student')
      roleStats.value[5].value = getCount('temporary')
      roleStats.value[6].value = res.data.data?.total || 0
    }
  } catch {}
}

// ==================== 操作 ====================
const switchRoleTab = tab => { activeRoleTab.value = tab; pagination.page = 1; fetchUsers() }

const openCreateDialog = (preType) => {
  userDialog.isEdit = false
  Object.assign(userDialog.form, { username: '', password: '123456', realName: '', userType: preType || 'student', identifier: '', phone: '', email: '', department: '', college: '', major: '', className: '', userId: null, visitReason: '', expireDate: '' })
  studentOrgSelectIndex.value = -1
  userDialog.visible = true
}

const editUser = async user => {
  userDialog.isEdit = true
  Object.assign(userDialog.form, {
    userId: user.userId, username: user.username, realName: user.realName,
    userType: user.userType, identifier: user.studentId || user.jobId || '',
    phone: user.phone, email: user.email || '', department: user.department || '',
    college: user.college || '', major: user.major || '', className: user.className || '',
    visitReason: user.visitReason || '', expireDate: user.expireDate || ''
  })
  await loadOrgClassOptions()
  studentOrgSelectIndex.value = user.userType === 'student' ? findStudentOrgIndexCard() : -1
  userDialog.visible = true
}

const submitUser = async () => {
  if (!userDialog.form.realName?.trim() || !userDialog.form.phone?.trim()) { ElMessage.warning('请填写必填项'); return }
  if (userDialog.form.userType === 'student' && !userDialog.form.className?.trim()) {
    ElMessage.warning('请从下拉选择学籍班级')
    return
  }
  try {
    if (userDialog.isEdit) {
      const data = {
        userId: userDialog.form.userId,
        realName: userDialog.form.realName,
        phone: userDialog.form.phone,
        email: userDialog.form.email,
        department: userDialog.form.department,
        college: userDialog.form.college || undefined,
        major: userDialog.form.major || undefined,
        className: userDialog.form.className || undefined
      }
      const res = await axios.put(`${API}/user/${userDialog.form.userId}`, data)
      if (res.data.code === 200) { ElMessage.success('已更新'); userDialog.visible = false; fetchUsers() }
      else ElMessage.error(res.data.message)
    } else {
      if (!userDialog.form.username?.trim()) { ElMessage.warning('请填写用户名'); return }
      const data = {
        username: userDialog.form.username, password: userDialog.form.password || '123456',
        realName: userDialog.form.realName, userType: userDialog.form.userType,
        phone: userDialog.form.phone, email: userDialog.form.email, department: userDialog.form.department,
        college: userDialog.form.userType === 'student' ? (userDialog.form.college || undefined) : undefined,
        major: userDialog.form.userType === 'student' ? (userDialog.form.major || undefined) : undefined,
        className: userDialog.form.userType === 'student' ? (userDialog.form.className || undefined) : undefined,
        studentId: userDialog.form.userType === 'student' ? userDialog.form.identifier : undefined,
        jobId: ['teacher', 'service', 'worker', 'admin'].includes(userDialog.form.userType) ? userDialog.form.identifier : undefined
      }
      const res = await axios.post(`${API}/user/register`, data)
      if (res.data.code === 200) { ElMessage.success('账号创建成功'); userDialog.visible = false; fetchUsers() }
      else ElMessage.error(res.data.message)
    }
  } catch (e) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

const changeRole = user => { roleDialog.user = user; roleDialog.newRole = user.userType; roleDialog.visible = true }

const submitRole = async () => {
  if (roleDialog.newRole === roleDialog.user.userType) { roleDialog.visible = false; return }
  try {
    const res = await axios.post(`${API}/user/${roleDialog.user.userId}/role`, null, { params: { role: roleDialog.newRole } })
    if (res.data.code === 200) { ElMessage.success(`角色已变更为：${getRoleLabel(roleDialog.newRole)}`); roleDialog.visible = false; fetchUsers() }
    else ElMessage.error(res.data.message)
  } catch { ElMessage.error('操作失败') }
}

const toggleFreeze = async user => {
  const newStatus = user.status === 'active' ? 'frozen' : 'active'
  const action = newStatus === 'frozen' ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(`确定要${action}"${user.realName || user.username}"吗？`, `${action}用户`, { type: 'warning' })
    const res = await axios.post(`${API}/user/${user.userId}/status`, null, { params: { status: newStatus } })
    if (res.data.code === 200) { ElMessage.success(`已${action}`); fetchUsers() }
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const resetPwd = async user => {
  try {
    await ElMessageBox.confirm(`确定重置"${user.realName}"的密码为 123456？`, '重置密码', { type: 'warning' })
    const res = await axios.post(`${API}/user/reset-password`, null, { params: { userId: user.userId, newPassword: '123456' } })
    if (res.data.code === 200) ElMessage.success('密码已重置为 123456')
    else ElMessage.error(res.data.message)
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const deleteUser = async user => {
  const name = user.realName || user.username || `用户#${user.userId}`
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${name}」？删除后该账号将无法登录。`,
      '删除用户',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await axios.delete(`${API}/user/${user.userId}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除用户')
      fetchUsers()
    } else ElMessage.error(res.data.message || '删除失败')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

const viewUserDetail = async user => {
  detailDialog.user = user
  detailDialog.stats = { vehicleCount: 0, violationCount: 0, notificationCount: 0 }
  detailDialog.visible = true
  // 拉取统计数据
  try {
    const vRes = await axios.get(`${API}/vehicle/list`, { params: { pageNum: 1, pageSize: 1 } })
    if (vRes.data.code === 200) detailDialog.stats.vehicleCount = vRes.data.total || 0
  } catch {}
}

const exportUsers = () => ElMessage.info('导出功能开发中')
const resetFilter = () => { filter.keyword = ''; filter.userType = ''; filter.status = ''; filter.department = ''; pagination.page = 1; fetchUsers() }
const changePage = dir => { const np = pagination.page + dir; if (np >= 1 && np <= pagination.totalPages) { pagination.page = np; fetchUsers() } }
// ==================== 工具函数 ====================
const getRoleLabel = t => ({ admin: '管理员', teacher: '导员', service: '检查员', student: '学生', temporary: '临时', worker: '工作人员' }[t] || t)
const getStatusLabel = s => ({ active: '正常', frozen: '已冻结', deleted: '已删除' }[s] || s)
const getAvatarColor = t => ({ admin: '#f44336', teacher: '#ff9800', service: '#2196f3', student: '#4caf50', temporary: '#9c27b0', worker: '#00bcd4' }[t] || '#607d8b')
const formatDate = d => d ? new Date(d).toLocaleDateString('zh-CN') : '—'
const licenseRemainNum = u => {
  if (!u) return null
  const v = u.licensePoints
  if (v === null || v === undefined) return 12
  const n = Number(v)
  return Number.isFinite(n) ? Math.max(0, Math.min(12, n)) : 12
}
const formatLicenseRemain = u => {
  const n = licenseRemainNum(u)
  return n == null ? '—' : n
}
const scoreClass = u => {
  const r = licenseRemainNum(u)
  if (r == null) return ''
  if (r <= 3) return 'sc-low'
  if (r <= 6) return 'sc-mid'
  return 'sc-ok'
}
const getIdLabel = t => ({ student: '学号', teacher: '工号', service: '工号', worker: '工号', admin: '工号', temporary: '证件号' }[t] || '编号')
const isExpireSoon = d => { if (!d) return false; const diff = new Date(d) - Date.now(); return diff > 0 && diff < 30 * 86400000 }

onMounted(() => {
  loadOrgClassOptions()
  fetchUsers()
})
</script>

<style scoped>
.user-container { min-height: 100vh; background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%); color: #fff; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.page-header .header-bg { background: linear-gradient(90deg, rgba(0,123,255,0.1), rgba(102,16,242,0.1)); border-radius: 16px; padding: 20px 24px; border: 1px solid rgba(0,123,255,0.2); display: flex; justify-content: space-between; align-items: center; position: relative; }
.header-bg::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, #007bff, #6610f2); border-radius: 16px 16px 0 0; }
.header-left { display: flex; flex-direction: column; gap: 4px; }
.title-text { font-size: 22px; font-weight: bold; background: linear-gradient(90deg, #fff, #00d4ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.header-stats { display: flex; align-items: center; gap: 12px; background: rgba(0,0,0,0.3); padding: 8px 16px; border-radius: 10px; flex-wrap: wrap; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 18px; font-weight: bold; }
.stat-lab { font-size: 10px; color: rgba(255,255,255,0.5); }
.header-actions { display: flex; gap: 8px; }
.action-btn { display: inline-flex; align-items: center; gap: 4px; padding: 8px 14px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.action-btn:hover { background: rgba(255,255,255,0.15); }
.action-btn.primary { background: linear-gradient(90deg, #007bff, #6610f2); border: none; }
.action-btn.success { background: rgba(76,175,80,0.3); border-color: rgba(76,175,80,0.5); color: #4caf50; }
.action-btn.warn { background: rgba(255,152,0,0.2); border-color: rgba(255,152,0,0.3); color: #ff9800; }
.action-btn.danger { background: rgba(244,67,54,0.3); border-color: rgba(244,67,54,0.5); color: #f44336; }
.action-btn.small { padding: 5px 10px; font-size: 12px; }
.filter-bar { display: flex; gap: 10px; align-items: center; background: rgba(255,255,255,0.05); border-radius: 10px; padding: 10px 14px; border: 1px solid rgba(255,255,255,0.08); flex-wrap: wrap; }
.tech-input { padding: 8px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; }
.tech-input:focus { outline: none; border-color: #00d4ff; }
.tech-select { padding: 8px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; min-width: 100px; }
.tech-select-full { width: 100%; padding: 9px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; }
.role-tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.role-tab { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 20px; cursor: pointer; font-size: 13px; color: rgba(255,255,255,0.6); transition: all 0.2s; }
.role-tab.active { background: rgba(0,212,255,0.15); border-color: rgba(0,212,255,0.3); color: #00d4ff; }
.rt-icon { font-size: 15px; }
.rt-count { padding: 1px 7px; border-radius: 10px; font-size: 11px; color: #fff; }
.table-container { background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); }
.table-header { display: flex; justify-content: space-between; align-items: center; padding: 13px 18px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.table-title { font-size: 15px; font-weight: 500; }
.page-controls { display: flex; align-items: center; gap: 8px; }
.total-info { font-size: 12px; color: rgba(255,255,255,0.4); }
.page-btn { width: 27px; height: 27px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 6px; cursor: pointer; }
.page-btn.disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 12px; color: rgba(255,255,255,0.6); }
.table-wrapper { overflow-x: auto; padding: 12px; }
.tech-table { width: 100%; border-collapse: separate; border-spacing: 0; min-width: 1180px; }
.tech-table th { padding: 9px 10px; text-align: left; font-size: 12px; color: rgba(255,255,255,0.5); border-bottom: 2px solid rgba(0,212,255,0.2); }
.tech-table td { padding: 10px 10px; font-size: 13px; border-bottom: 1px solid rgba(255,255,255,0.05); }
.tech-table tbody tr:hover { background: rgba(0,212,255,0.04); }
.th-score, .td-score { white-space: nowrap; text-align: center; vertical-align: middle; }
.th-score { color: rgba(0, 212, 255, 0.85); font-weight: 600; }
.td-score { padding-left: 12px; padding-right: 12px; }
.score-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 5px 12px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}
.score-pill.sc-ok { background: rgba(76,175,80,0.2); color: #4caf50; border: 1px solid rgba(76,175,80,0.35); }
.score-pill.sc-mid { background: rgba(255,152,0,0.2); color: #ff9800; border: 1px solid rgba(255,152,0,0.35); }
.score-pill.sc-low { background: rgba(244,67,54,0.2); color: #f44336; border: 1px solid rgba(244,67,54,0.35); }
.id-badge { color: #00d4ff; font-weight: 500; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: bold; color: #fff; flex-shrink: 0; }
.username { font-weight: 500; font-size: 13px; }
.user-dept { font-size: 11px; color: rgba(255,255,255,0.4); }
.role-badge { padding: 3px 9px; border-radius: 14px; font-size: 12px; }
.role-badge.admin { background: rgba(244,67,54,0.2); color: #f44336; border: 1px solid rgba(244,67,54,0.3); }
.role-badge.teacher { background: rgba(255,152,0,0.2); color: #ff9800; border: 1px solid rgba(255,152,0,0.3); }
.role-badge.service { background: rgba(33,150,243,0.2); color: #2196f3; border: 1px solid rgba(33,150,243,0.3); }
.role-badge.student { background: rgba(76,175,80,0.2); color: #4caf50; border: 1px solid rgba(76,175,80,0.3); }
.role-badge.temporary { background: rgba(156,39,176,0.2); color: #9c27b0; border: 1px solid rgba(156,39,176,0.3); }
.role-badge.worker { background: rgba(0,188,212,0.2); color: #00bcd4; border: 1px solid rgba(0,188,212,0.3); }
.id-code { font-family: monospace; font-size: 13px; color: rgba(255,255,255,0.7); }
.contact-cell { display: flex; flex-direction: column; gap: 2px; font-size: 13px; }
.contact-email { font-size: 11px; color: rgba(255,255,255,0.4); }
.status-badge { padding: 3px 9px; border-radius: 14px; font-size: 12px; }
.status-badge.active { background: rgba(76,175,80,0.2); color: #4caf50; border: 1px solid rgba(76,175,80,0.3); }
.status-badge.frozen { background: rgba(33,150,243,0.2); color: #2196f3; border: 1px solid rgba(33,150,243,0.3); }
.action-cell { display: flex; gap: 4px; flex-wrap: wrap; }
.empty-row { text-align: center; padding: 28px; color: rgba(255,255,255,0.3); font-size: 13px; }
.expire-warn { color: #ff9800; }
/* 面板 */
.panel { background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 13px 18px; border-bottom: 1px solid rgba(255,255,255,0.1); font-size: 15px; font-weight: 500; }
.panel-body { padding: 14px 18px; }
.binding-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 12px; }
.binding-card { background: rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.1); border-radius: 10px; padding: 14px; }
.binding-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.counselor-name { font-size: 14px; font-weight: 600; color: #00d4ff; }
.bind-actions { display: flex; gap: 5px; }
.class-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 10px; }
.class-tag { display: inline-flex; align-items: center; gap: 6px; padding: 2px 6px 2px 9px; background: rgba(0,212,255,0.15); border: 1px solid rgba(0,212,255,0.3); border-radius: 9px; font-size: 12px; color: #00d4ff; }
.tag-x { cursor: pointer; opacity: 0.7; font-weight: bold; line-height: 1; }
.tag-x:hover { opacity: 1; color: #f44336; }
.bind-id { font-size: 11px; color: rgba(255,255,255,0.35); font-weight: normal; }
.bind-stat { font-size: 12px; color: rgba(255,255,255,0.4); }
.tech-input-full { width: 100%; padding: 9px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; box-sizing: border-box; }
.tech-input-full:focus { outline: none; border-color: #00d4ff; }
.add-card { display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 8px; border-style: dashed; cursor: pointer; color: rgba(255,255,255,0.3); min-height: 80px; transition: all 0.2s; }
.add-card:hover { border-color: rgba(0,212,255,0.3); color: #00d4ff; }
.add-icon { font-size: 20px; }
/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: linear-gradient(135deg, #1a1f3c, #2d3250); border-radius: 16px; border: 1px solid rgba(0,123,255,0.3); width: 580px; max-height: 85vh; display: flex; flex-direction: column; }
.modal-content.sm { width: 440px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 22px; border-bottom: 1px solid rgba(255,255,255,0.1); flex-shrink: 0; }
.modal-title { font-size: 16px; font-weight: 600; }
.modal-close { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 50%; cursor: pointer; }
.modal-body { padding: 20px 22px; overflow-y: auto; flex: 1; }
.modal-footer { padding: 12px 22px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; justify-content: flex-end; gap: 10px; flex-shrink: 0; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-item { display: flex; flex-direction: column; gap: 7px; }
.form-label { font-size: 13px; color: rgba(255,255,255,0.7); }
/* 角色选择 */
.role-user-info { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 12px; margin-bottom: 14px; }
.ru-name { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.ru-cur { font-size: 13px; color: rgba(255,255,255,0.5); display: flex; align-items: center; gap: 8px; }
.role-options { display: flex; flex-direction: column; gap: 8px; }
.role-opt { display: flex; align-items: center; gap: 12px; padding: 11px 13px; background: rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.1); border-radius: 9px; cursor: pointer; transition: all 0.2s; }
.role-opt:hover { background: rgba(0,212,255,0.07); border-color: rgba(0,212,255,0.2); }
.role-opt.active { background: rgba(0,212,255,0.15); border-color: rgba(0,212,255,0.4); }
.ro-icon { font-size: 18px; width: 26px; text-align: center; }
.ro-info { flex: 1; }
.ro-name { font-size: 13px; font-weight: 500; }
.ro-desc { font-size: 11px; color: rgba(255,255,255,0.4); margin-top: 2px; }
.ro-check { color: #00d4ff; font-size: 15px; font-weight: bold; }
/* 用户详情 */
.user-profile { display: flex; align-items: center; gap: 16px; padding: 16px; background: rgba(0,0,0,0.2); border-radius: 10px; margin-bottom: 16px; }
.profile-avatar { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: bold; color: #fff; flex-shrink: 0; }
.profile-name { font-size: 18px; font-weight: bold; margin-bottom: 4px; }
.profile-username { font-size: 13px; color: rgba(255,255,255,0.4); margin-bottom: 8px; }
.detail-grid { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.dr { display: flex; gap: 12px; padding: 7px 10px; background: rgba(0,0,0,0.2); border-radius: 6px; font-size: 13px; }
.dl { color: rgba(255,255,255,0.4); min-width: 70px; }
.dv { color: #fff; font-weight: 500; }
.user-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.us-item { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 12px; text-align: center; }
.us-val { font-size: 22px; font-weight: bold; color: #00d4ff; }
.us-val.us-red { color: #f44336; }
.us-lab { font-size: 12px; color: rgba(255,255,255,0.5); margin-top: 4px; }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,0.05); }
::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.3); border-radius: 3px; }
</style>

<template>
  <div class="rfid-container page-bg-shell">
    <div class="page-header">
      <div class="header-bg">
        <div class="header-left">
          <h1 class="title-text">BLE 蓝牙标签管理</h1>
          <p class="title-sub">校园电动车 BLE 蓝牙标签管理系统</p>
        </div>
        <div class="header-stats">
          <div class="stat-item"><span class="stat-num">{{ stats.total }}</span><span class="stat-lab">总卡量</span></div>
          <div class="stat-div"></div>
          <div class="stat-item"><span class="stat-num c-green">{{ stats.normal }}</span><span class="stat-lab">正常</span></div>
          <div class="stat-div"></div>
          <div class="stat-item"><span class="stat-num c-red">{{ stats.lost }}</span><span class="stat-lab">挂失</span></div>
          <div class="stat-div"></div>
          <div class="stat-item"><span class="stat-num c-orange">{{ stats.unbound }}</span><span class="stat-lab">未绑定</span></div>
          <div class="stat-div"></div>
          <div class="stat-item"><span class="stat-num c-gray">{{ stats.disabled }}</span><span class="stat-lab">停用</span></div>
        </div>
        <div class="header-actions">
          <div class="action-btn" @click="fetchData">↻ 刷新</div>
          <div class="action-btn primary" @click="openIssueDialog">+ 标签登记</div>
          <div class="action-btn" @click="exportData">📊 导出</div>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="sub-tabs">
      <div v-for="t in subTabs" :key="t.value" class="sub-tab" :class="{ active: activeTab === t.value }" @click="activeTab = t.value">
        {{ t.icon }} {{ t.label }}
        <span v-if="t.badge" class="tab-badge">{{ t.badge }}</span>
      </div>
    </div>

    <!-- ======== 标签页1：BLE标签列表 ======== -->
    <div v-if="activeTab === 'cards'" class="tab-content">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <input v-model="filter.keyword" class="tech-input" placeholder="搜索标签编号/车牌/车主" style="width:200px" @keyup.enter="fetchData" />
        <select v-model="filter.status" class="tech-select" @change="fetchData">
          <option value="">全部状态</option>
          <option value="normal">正常</option>
          <option value="lost">挂失</option>
          <option value="disabled">停用</option>
          <option value="unbound">未绑定</option>
        </select>
        <select v-model="filter.userType" class="tech-select" @change="fetchData">
          <option value="">全部类型</option>
          <option value="student">学生</option>
          <option value="teacher">教职工</option>
          <option value="service">后勤人员</option>
          <option value="temporary">临时人员</option>
        </select>
        <div class="action-btn primary" @click="fetchData">🔍 搜索</div>
        <div class="action-btn" @click="resetFilter">重置</div>
      </div>

      <!-- 卡片列表 -->
      <div class="table-container">
        <div class="table-header">
          <span class="table-title">BLE 标签列表</span>
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
                <th>标签编号</th><th>绑定车辆</th><th>车主信息</th><th>人员类型</th>
                <th>绑定日期</th><th>有效期</th><th>状态</th><th>最后通行</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="card in cardList" :key="card.etcId">
                <td>
                  <div class="card-no-cell">
                    <span class="rfid-icon">📡</span>
                    <span class="card-no">{{ card.etcNumber }}</span>
                  </div>
                </td>
                <td>
                  <span v-if="card.licensePlate" class="plate-badge">{{ card.licensePlate }}</span>
                  <span v-else class="no-bind">未绑定</span>
                </td>
                <td>
                  <div class="owner-cell">
                    <div>{{ card.ownerName || '—' }}</div>
                    <div class="owner-sub">{{ card.phone || '' }}</div>
                  </div>
                </td>
                <td>
                  <span class="utype-badge" :class="card.userType">{{ getUserTypeText(card.userType) }}</span>
                </td>
                <td>{{ formatDate(card.issueDate) }}</td>
                <td>
                  <span :class="{ 'expire-warn': isExpireSoon(card.expireDate), 'expire-bad': isExpired(card.expireDate) }">
                    {{ formatDate(card.expireDate) || '长期' }}
                  </span>
                  <div v-if="isExpireSoon(card.expireDate)" class="expire-tip">⚠️ 即将到期</div>
                </td>
                <td>
                  <span class="status-badge" :class="card.status">{{ getCardStatusLabel(card.status) }}</span>
                </td>
                <td><span class="last-pass">{{ card.lastPassTime ? formatDate(card.lastPassTime) : '—' }}</span></td>
                <td>
                  <div class="action-cell">
                    <div class="action-btn small" @click="viewCardDetail(card)">详情</div>
                    <div v-if="card.status === 'normal'" class="action-btn small warn" @click="reportLost(card)">挂失</div>
                    <div v-if="card.status === 'lost'" class="action-btn small success" @click="cancelLost(card)">解挂</div>
                    <div v-if="card.status !== 'disabled'" class="action-btn small" @click="bindVehicle(card)">绑车辆</div>
                    <div v-if="card.status !== 'disabled'" class="action-btn small danger" @click="disableCard(card)">停用</div>
                    <div v-else class="action-btn small success" @click="enableCard(card)">启用</div>
                  </div>
                </td>
              </tr>
              <tr v-if="cardList.length === 0"><td colspan="9" class="empty-row">暂无 BLE 标签数据</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ======== 标签页2：通行记录 ======== -->
    <div v-if="activeTab === 'pass'" class="tab-content">
      <div class="filter-bar">
        <input v-model="passFilter.keyword" class="tech-input" placeholder="搜索标签编号/车牌" style="width:200px" @keyup.enter="fetchPassRecords" />
        <select v-model="passFilter.location" class="tech-select" @change="fetchPassRecords">
          <option value="">全部卡口</option>
          <option value="east">东校区正门</option>
          <option value="west">西校区侧门</option>
          <option value="lib">图书馆停车区</option>
          <option value="dorm">宿舍区入口</option>
        </select>
        <select v-model="passFilter.result" class="tech-select" @change="fetchPassRecords">
          <option value="">全部结果</option>
          <option value="pass">通过</option>
          <option value="deny">拒绝</option>
        </select>
        <input type="date" v-model="passFilter.date" class="tech-input" @change="fetchPassRecords" style="width:150px" />
        <div class="action-btn primary" @click="fetchPassRecords">🔍 查询</div>
        <div class="action-btn" @click="resetPassFilter">重置</div>
      </div>

      <!-- 实时通行流统计 -->
      <div class="pass-stats-row">
        <div class="pass-stat-card" v-for="ps in passStats" :key="ps.label">
          <div class="ps-icon">{{ ps.icon }}</div>
          <div class="ps-val">{{ ps.value }}</div>
          <div class="ps-lab">{{ ps.label }}</div>
        </div>
      </div>

      <div class="table-container">
        <div class="table-header">
          <span class="table-title">🚦 通行记录</span>
          <div class="page-controls">
            <span class="total-info">共 {{ passPagination.total }} 条</span>
            <div class="page-btn" :class="{ disabled: passPagination.page === 1 }" @click="passChangePage(-1)">←</div>
            <span class="page-info">{{ passPagination.page }} / {{ passPagination.totalPages }}</span>
            <div class="page-btn" :class="{ disabled: passPagination.page === passPagination.totalPages }" @click="passChangePage(1)">→</div>
          </div>
        </div>
        <div class="table-wrapper">
          <table class="tech-table">
            <thead>
              <tr><th>标签编号</th><th>车牌号</th><th>通行位置</th><th>方向</th><th>通行时间</th><th>结果</th><th>拒绝原因</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in passRecords" :key="r.id">
                <td><span class="card-no">{{ r.etcNumber }}</span></td>
                <td><span class="plate-badge">{{ r.plate }}</span></td>
                <td>{{ r.location }}</td>
                <td><span class="dir-badge" :class="r.dir">{{ r.dir === 'in' ? '🔵 入校' : '🟡 出校' }}</span></td>
                <td>{{ r.time }}</td>
                <td><span class="result-badge" :class="r.result">{{ r.result === 'pass' ? '✅ 通过' : '❌ 拒绝' }}</span></td>
                <td><span class="deny-reason">{{ r.denyReason || '—' }}</span></td>
              </tr>
              <tr v-if="passRecords.length === 0"><td colspan="7" class="empty-row">暂无通行记录</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ======== 标签页3：蓝牙网关管理 ======== -->
    <div v-if="activeTab === 'devices'" class="tab-content">
      <div class="filter-bar">
        <input v-model="deviceFilter.keyword" class="tech-input" placeholder="搜索设备编号/位置" style="width:200px" />
        <select v-model="deviceFilter.status" class="tech-select">
          <option value="">全部状态</option>
          <option value="online">在线</option>
          <option value="offline">离线</option>
          <option value="fault">故障</option>
        </select>
        <div class="action-btn primary" @click="fetchReaderDevices">🔍 搜索</div>
        <div class="action-btn" @click="openAddDeviceDialog">+ 添加设备</div>
      </div>

      <div class="device-grid">
        <div v-for="dev in readerDevices" :key="dev.id" class="device-card">
          <div class="dev-header">
            <div class="dev-icon" :class="dev.status">
              {{ dev.status === 'online' ? '🟢' : dev.status === 'fault' ? '🔴' : '⚫' }}
            </div>
            <div class="dev-info">
              <div class="dev-name">{{ dev.name }}</div>
              <div class="dev-location">📍 {{ dev.location }}</div>
            </div>
            <div class="dev-status-badge" :class="dev.status">{{ getDeviceStatusLabel(dev.status) }}</div>
          </div>

          <div class="dev-stats">
            <div class="dev-stat"><span class="ds-label">今日通行</span><span class="ds-val">{{ dev.todayCount || 0 }}</span></div>
            <div class="dev-stat"><span class="ds-label">今日拒绝</span><span class="ds-val c-red">{{ dev.todayDeny || 0 }}</span></div>
            <div class="dev-stat"><span class="ds-label">安装时间</span><span class="ds-val">{{ dev.installDate }}</span></div>
            <div class="dev-stat"><span class="ds-label">最后心跳</span><span class="ds-val">{{ dev.lastHeartbeat }}</span></div>
          </div>

          <div class="dev-progress-bar">
            <div class="dpb-label">今日通行率</div>
            <div class="dpb-bar">
              <div class="dpb-fill" :style="{ width: getPassRate(dev) + '%', background: dev.status === 'online' ? '#4caf50' : '#9e9e9e' }"></div>
            </div>
            <span class="dpb-val">{{ getPassRate(dev) }}%</span>
          </div>

          <div class="dev-actions">
            <div class="action-btn small" @click="viewDeviceLogs(dev)">📋 日志</div>
            <div class="action-btn small" @click="configDevice(dev)">⚙️ 配置</div>
            <div v-if="dev.status === 'fault'" class="action-btn small success" @click="resetDevice(dev)">🔄 重启</div>
          </div>
        </div>
        <div v-if="readerDevices.length === 0" class="empty-state" style="grid-column:1/-1">
          <div class="empty-icon">📡</div>
          <div class="empty-text">暂无蓝牙网关</div>
        </div>
      </div>
    </div>

    <!-- ======== 标签页4：挂失管理 ======== -->
    <div v-if="activeTab === 'lost'" class="tab-content">
      <div class="lost-list">
        <div v-for="card in lostCards" :key="card.etcId" class="lost-card">
          <div class="lost-card-header">
            <span class="rfid-icon">📡</span>
            <div class="lost-info">
              <div class="lost-no">{{ card.etcNumber }}</div>
              <div class="lost-plate">{{ card.licensePlate || '无车牌' }}</div>
            </div>
            <span class="status-badge lost">已挂失</span>
          </div>
          <div class="lost-detail">
            <div class="ld-item"><span class="ld-label">车主</span><span>{{ card.ownerName || '—' }}</span></div>
            <div class="ld-item"><span class="ld-label">手机</span><span>{{ card.phone || '—' }}</span></div>
            <div class="ld-item"><span class="ld-label">挂失时间</span><span>{{ formatDate(card.lostTime) }}</span></div>
            <div class="ld-item"><span class="ld-label">挂失原因</span><span>{{ card.lostReason || '—' }}</span></div>
          </div>
          <div class="lost-actions">
            <div class="action-btn small success" @click="cancelLost(card)">🔓 解挂</div>
            <div class="action-btn small primary" @click="reissueCard(card)">🔄 补发标签</div>
            <div class="action-btn small danger" @click="disableCard(card)">🚫 停用</div>
          </div>
        </div>
        <div v-if="lostCards.length === 0" class="empty-state">
          <div class="empty-icon">✅</div>
          <div class="empty-text">当前无挂失标签</div>
        </div>
      </div>
    </div>

    <!-- ======== 标签登记弹窗 ======== -->
    <div v-if="issueDialog.visible" class="modal-overlay" @click.self="issueDialog.visible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">📡 BLE 标签绑定登记</span>
          <span class="modal-close" @click="issueDialog.visible = false">×</span>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-item">
              <label class="form-label">标签编号 *</label>
              <input v-model="issueForm.etcNumber" class="tech-input" placeholder="BLE 标签编号" />
            </div>
            <div class="form-item">
              <label class="form-label">绑定车辆（车牌号）</label>
              <input v-model="issueForm.licensePlate" class="tech-input" placeholder="可选，输入车牌号自动关联" @blur="searchVehicleByPlate" />
              <div v-if="issueDialog.vehicleInfo" class="bind-vehicle-hint">
                ✓ 找到车辆：{{ issueDialog.vehicleInfo.ownerName }} - {{ issueDialog.vehicleInfo.licensePlate }}
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">有效期</label>
              <input type="date" v-model="issueForm.expireDate" class="tech-input" />
              <div class="form-hint">不填写则为长期有效</div>
            </div>
            <div class="form-item">
              <label class="form-label">绑定说明</label>
              <input v-model="issueForm.remark" class="tech-input" placeholder="备注信息" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="issueDialog.visible = false">取消</div>
          <div class="action-btn primary" @click="submitIssue">确认绑定</div>
        </div>
      </div>
    </div>

    <!-- 卡详情弹窗 -->
    <div v-if="detailDialog.visible" class="modal-overlay" @click.self="detailDialog.visible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">📡 BLE 标签详情</span>
          <span class="modal-close" @click="detailDialog.visible = false">×</span>
        </div>
        <div class="modal-body" v-if="detailDialog.card">
          <!-- 卡片视觉化展示 -->
          <div class="card-visual">
            <div class="card-front">
              <div class="card-top-row">
                <span class="card-brand">校园 BLE</span>
                <span class="card-chip">▣</span>
              </div>
              <div class="card-wave">
                <div class="wave-lines">
                  <div class="wl" v-for="i in 4" :key="i" :style="{ opacity: 0.2 + i * 0.2 }"></div>
                </div>
              </div>
              <div class="card-num-row">{{ detailDialog.card.etcNumber }}</div>
              <div class="card-bottom-row">
                <span>{{ detailDialog.card.ownerName || '未知车主' }}</span>
                <span>{{ detailDialog.card.licensePlate || '无车牌' }}</span>
              </div>
            </div>
          </div>

          <div class="detail-grid">
            <div class="detail-row"><span class="dl">标签编号</span><span class="dv">{{ detailDialog.card.etcNumber }}</span></div>
            <div class="detail-row"><span class="dl">状态</span><span class="dv"><span class="status-badge" :class="detailDialog.card.status">{{ getCardStatusLabel(detailDialog.card.status) }}</span></span></div>
            <div class="detail-row"><span class="dl">绑定车牌</span><span class="dv">{{ detailDialog.card.licensePlate || '未绑定' }}</span></div>
            <div class="detail-row"><span class="dl">车主姓名</span><span class="dv">{{ detailDialog.card.ownerName || '—' }}</span></div>
            <div class="detail-row"><span class="dl">人员类型</span><span class="dv">{{ getUserTypeText(detailDialog.card.userType) }}</span></div>
            <div class="detail-row"><span class="dl">绑定日期</span><span class="dv">{{ formatDate(detailDialog.card.issueDate) }}</span></div>
            <div class="detail-row"><span class="dl">有效期至</span><span class="dv">{{ formatDate(detailDialog.card.expireDate) || '长期有效' }}</span></div>
            <div class="detail-row"><span class="dl">今日通行</span><span class="dv">{{ detailDialog.card.todayPass || 0 }} 次</span></div>
          </div>

          <!-- 最近通行记录 -->
          <div class="recent-pass" v-if="detailDialog.recentPasses.length">
            <div class="rp-title">最近通行记录</div>
            <div class="rp-item" v-for="rp in detailDialog.recentPasses" :key="rp.id">
              <span class="rp-location">{{ rp.location }}</span>
              <span class="dir-badge" :class="rp.dir">{{ rp.dir === 'in' ? '入' : '出' }}</span>
              <span class="rp-time">{{ rp.time }}</span>
              <span class="result-badge" :class="rp.result">{{ rp.result === 'pass' ? '✓' : '✗' }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="detailDialog.visible = false">关闭</div>
          <div v-if="detailDialog.card?.status === 'normal'" class="action-btn warn" @click="reportLost(detailDialog.card); detailDialog.visible = false">挂失此标签</div>
        </div>
      </div>
    </div>

    <!-- 绑定车辆弹窗 -->
    <div v-if="bindDialog.visible" class="modal-overlay" @click.self="bindDialog.visible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">🔗 绑定车辆</span>
          <span class="modal-close" @click="bindDialog.visible = false">×</span>
        </div>
        <div class="modal-body">
          <div class="info-block">
            <div class="info-row"><span class="il">BLE 标签编号</span><span>{{ bindDialog.card?.etcNumber }}</span></div>
            <div class="info-row"><span class="il">当前绑定</span><span>{{ bindDialog.card?.licensePlate || '未绑定' }}</span></div>
          </div>
          <div class="form-item" style="margin-top:16px">
            <label class="form-label">车牌号 *</label>
            <input v-model="bindDialog.licensePlate" class="tech-input" placeholder="输入车牌号进行关联" @blur="searchBindVehicle" />
          </div>
          <div v-if="bindDialog.vehicleInfo" class="bind-result">
            <div class="br-icon">✓</div>
            <div class="br-info">
              <div>{{ bindDialog.vehicleInfo.ownerName }}</div>
              <div class="br-sub">{{ bindDialog.vehicleInfo.brand }} · {{ bindDialog.vehicleInfo.color }}</div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="bindDialog.visible = false">取消</div>
          <div class="action-btn primary" :class="{ disabled: !bindDialog.vehicleInfo }" @click="confirmBind">确认绑定</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

/** 与 vite proxy `/api` → 后端 8083 一致，避免写死 localhost */
const API = '/api'
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`

// ==================== 状态 ====================
const activeTab = ref('cards')
const subTabs = computed(() => [
  { value: 'cards', label: 'BLE标签管理', icon: '📡' },
  { value: 'pass', label: '通行记录', icon: '🚦' },
  { value: 'devices', label: '蓝牙网关', icon: '🖥️' },
  { value: 'lost', label: '挂失管理', icon: '🔒', badge: stats.lost || undefined }
])

const stats = reactive({ total: 0, normal: 0, lost: 0, unbound: 0, disabled: 0 })
const cardList = ref([])
const passRecords = ref([])
const readerDevices = ref([])
const lostCards = ref([])
const pagination = reactive({ page: 1, pageSize: 12, total: 0, totalPages: 1 })
const passPagination = reactive({ page: 1, pageSize: 15, total: 0, totalPages: 1 })
const filter = reactive({ keyword: '', status: '', userType: '' })
const passFilter = reactive({ keyword: '', location: '', result: '', date: '' })
const deviceFilter = reactive({ keyword: '', status: '' })

const issueDialog = reactive({ visible: false, vehicleInfo: null })
const issueForm = reactive({ etcNumber: '', licensePlate: '', expireDate: '', remark: '' })
const detailDialog = reactive({ visible: false, card: null, recentPasses: [] })
const bindDialog = reactive({ visible: false, card: null, licensePlate: '', vehicleInfo: null })

// 模拟蓝牙网关数据（后端如果有就替换）
const mockDevices = [
  { id: 1, name: '东校区正门-入', location: '东校区正门入口', status: 'online', todayCount: 328, todayDeny: 12, installDate: '2023-09-01', lastHeartbeat: '刚刚' },
  { id: 2, name: '东校区正门-出', location: '东校区正门出口', status: 'online', todayCount: 291, todayDeny: 5, installDate: '2023-09-01', lastHeartbeat: '1分钟前' },
  { id: 3, name: '西校区侧门-入', location: '西校区侧门入口', status: 'fault', todayCount: 0, todayDeny: 0, installDate: '2023-11-15', lastHeartbeat: '2小时前' },
  { id: 4, name: '图书馆停车区', location: '图书馆南侧停车区', status: 'online', todayCount: 156, todayDeny: 3, installDate: '2024-01-10', lastHeartbeat: '刚刚' },
  { id: 5, name: '宿舍区入口', location: '学生宿舍区总入口', status: 'offline', todayCount: 0, todayDeny: 0, installDate: '2023-10-20', lastHeartbeat: '1天前' },
  { id: 6, name: '食堂停车区', location: '食堂东侧停车区', status: 'online', todayCount: 89, todayDeny: 1, installDate: '2024-02-15', lastHeartbeat: '刚刚' }
]

const passStats = computed(() => [
  { icon: '🔵', label: '今日入校', value: passRecords.value.filter(r => r.dir === 'in').length },
  { icon: '🟡', label: '今日出校', value: passRecords.value.filter(r => r.dir === 'out').length },
  { icon: '✅', label: '通过总数', value: passRecords.value.filter(r => r.result === 'pass').length },
  { icon: '❌', label: '拒绝总数', value: passRecords.value.filter(r => r.result === 'deny').length }
])

// ==================== 数据获取 ====================
const fetchData = async () => {
  try {
    const [listRes, statRes] = await Promise.all([
      axios.get(`${API}/etc-card/list`, {
        params: {
          pageNum: pagination.page,
          pageSize: pagination.pageSize,
          keyword: filter.keyword || undefined,
          status: filter.status || undefined,
          userType: filter.userType || undefined
        }
      }),
      axios.get(`${API}/etc-card/stats`)
    ])
    const body = listRes.data
    if (body.code === 200) {
      const rows = body.data || []
      cardList.value = rows.map(c => ({
        etcId: c.etcId,
        etcNumber: c.etcNumber,
        vehicleId: c.vehicleId,
        licensePlate: c.licensePlate || '',
        ownerName: c.ownerName,
        phone: c.phone || '',
        userType: c.userType || 'student',
        status: c.status || 'normal',
        issueDate: c.issueDate || '',
        expireDate: c.expireDate || '',
        lastPassTime: null,
        todayPass: 0
      }))

      pagination.total = body.total ?? 0
      pagination.totalPages = Math.max(1, body.totalPages ?? 1)
    }
    const st = statRes.data?.data
    if (st && typeof st.total === 'number') {
      stats.total = st.total
      stats.normal = Number(st.normal) || 0
      stats.lost = Number(st.lost) || 0
      stats.unbound = Number(st.unbound) || 0
      stats.disabled = Number(st.disabled) || 0
    }
  } catch { ElMessage.error('获取 BLE 标签数据失败') }
}

const fetchPassRecords = async () => {
  try {
    const res = await axios.get(`${API}/pass-record/list`, {
      params: {
        keyword: passFilter.keyword || undefined,
        direction: passFilter.location ? { east: 'in', west: 'in', lib: 'in', dorm: 'in' }[passFilter.location] : undefined, // 需要根据实际映射
        passResult: passFilter.result || undefined,
        pageNum: passPagination.page,
        pageSize: passPagination.pageSize
      }
    })
    const body = res.data
    if (body.code === 200) {
      const records = body.data.map(r => ({
        id: r.id,
        etcNumber: r.etcNumber,
        plate: r.licensePlate,
        location: r.location,
        dir: r.direction,
        time: formatDate(r.passTime),
        result: r.passResult,
        denyReason: r.denyReason
      }))
      passRecords.value = records
      passPagination.total = body.total
      passPagination.totalPages = body.totalPages
    } else {
      ElMessage.error(body.message || '获取通行记录失败')
    }
  } catch (e) {
    ElMessage.error('请求失败')
  }
}

const fetchReaderDevices = () => {
  let devices = [...mockDevices]
  if (deviceFilter.status) devices = devices.filter(d => d.status === deviceFilter.status)
  if (deviceFilter.keyword) devices = devices.filter(d => d.name.includes(deviceFilter.keyword) || d.location.includes(deviceFilter.keyword))
  readerDevices.value = devices
}

const fetchLostCards = async () => {
  try {
    const res = await axios.get(`${API}/etc-card/list`, {
      params: { pageNum: 1, pageSize: 500, status: 'lost' }
    })
    const body = res.data
    if (body.code !== 200) {
      lostCards.value = []
      return
    }
    const rows = body.data || []
    lostCards.value = rows.map(c => ({
      etcId: c.etcId,
      etcNumber: c.etcNumber,
      vehicleId: c.vehicleId,
      licensePlate: c.licensePlate || '',
      ownerName: c.ownerName,
      phone: c.phone || '',
      userType: c.userType || 'student',
      status: c.status || 'lost',
      issueDate: c.issueDate || '',
      expireDate: c.expireDate || '',
      lostTime: formatDate(c.updateTime) || new Date().toISOString().split('T')[0],
      lostReason: '申请挂失'
    }))
  } catch {
    ElMessage.error('获取挂失列表失败')
    lostCards.value = []
  }
}

// ==================== 操作 ====================
const reportLost = async card => {
  try {
    await ElMessageBox.confirm(`确定对标签 ${card.etcNumber} 执行挂失操作？`, '挂失确认', { type: 'warning' })
    await axios.post(`${API}/etc-card/${card.etcId}/status`, null, { params: { status: 'lost' } })
    card.status = 'lost'
    ElMessage.success('挂失成功')
    fetchLostCards()
    fetchData()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败') }
}

const cancelLost = async card => {
  try {
    await ElMessageBox.confirm(`确定解除标签 ${card.etcNumber} 的挂失？`, '解挂确认', { type: 'warning' })
    await axios.post(`${API}/etc-card/${card.etcId}/status`, null, { params: { status: 'normal' } })
    card.status = 'normal'
    ElMessage.success('解挂成功')
    fetchLostCards()
    fetchData()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败') }
}

const disableCard = async card => {
  try {
    await ElMessageBox.confirm(`停用标签 ${card.etcNumber}？停用后将无法通行。`, '停用确认', { type: 'error' })
    await axios.post(`${API}/etc-card/${card.etcId}/status`, null, { params: { status: 'disabled' } })
    card.status = 'disabled'
    ElMessage.success('已停用')
    fetchData()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败') }
}

const enableCard = async (card) => {
  try {
    await axios.post(`${API}/etc-card/${card.etcId}/status`, null, { params: { status: 'normal' } })
    card.status = 'normal'
    ElMessage.success('已启用')
    await fetchData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const reissueCard = card => {
  issueForm.etcNumber = `BLE-${Date.now().toString().slice(-8)}`
  issueForm.licensePlate = card.licensePlate || ''
  issueDialog.vehicleInfo = card.licensePlate ? { ownerName: card.ownerName, licensePlate: card.licensePlate } : null
  issueDialog.visible = true
}

const openIssueDialog = () => {
  Object.assign(issueForm, { etcNumber: '', licensePlate: '', expireDate: '', remark: '' })
  issueDialog.vehicleInfo = null
  issueDialog.visible = true
}

const searchVehicleByPlate = async () => {
  if (!issueForm.licensePlate) return
  try {
    const res = await axios.get(`${API}/vehicle/search`, { params: { plate: issueForm.licensePlate } })
    if (res.data.code === 200 && res.data.data?.length) {
      issueDialog.vehicleInfo = res.data.data[0]
    } else issueDialog.vehicleInfo = null
  } catch {}
}

const submitIssue = async () => {
  if (!issueForm.etcNumber.trim()) { ElMessage.warning('请输入标签编号'); return }
  if (!issueForm.licensePlate.trim()) { ElMessage.warning('请输入车牌以关联车辆'); return }
  try {
    const searchRes = await axios.get(`${API}/vehicle/search`, { params: { plate: issueForm.licensePlate.trim() } })
    const list = searchRes.data?.data || []
    if (!list.length) {
      ElMessage.warning('未找到该车牌对应车辆')
      return
    }
    const vehicleId = list[0].vehicleId
    const payload = { vehicleId, etcNumber: issueForm.etcNumber.trim() }
    if (issueForm.expireDate) payload.expireDate = issueForm.expireDate
    const r = await axios.post(`${API}/etc-card/issue`, payload)
    if (r.data.code === 200) {
      ElMessage.success(`BLE 标签 ${issueForm.etcNumber} 绑定登记成功`)
      issueDialog.visible = false
      fetchData()
    } else {
      ElMessage.error(r.data.message || '绑定失败')
    }
  } catch {
    ElMessage.error('绑定失败')
  }
}

const viewCardDetail = async (card) => {
  detailDialog.card = card
  // 调用通行记录接口，获取该卡最近5条记录
  try {
    const res = await axios.get(`${API}/pass-record/list`, {
      params: {
        etcNumber: card.etcNumber,  // 假设接口支持按标签编号查询
        pageNum: 1,
        pageSize: 5
      }
    })
    if (res.data.code === 200) {
      const records = res.data.data.map(r => ({
        id: r.id,
        location: r.location,
        dir: r.direction,
        time: formatDate(r.passTime),
        result: r.passResult
      }))
      detailDialog.recentPasses = records
    } else {
      detailDialog.recentPasses = []
    }
  } catch (e) {
    detailDialog.recentPasses = []
    console.error('获取通行记录失败', e)
  }
  detailDialog.visible = true
}

const bindVehicle = card => {
  bindDialog.card = card; bindDialog.licensePlate = card.licensePlate || ''; bindDialog.vehicleInfo = null; bindDialog.visible = true
}
const searchBindVehicle = async () => {
  if (!bindDialog.licensePlate) return
  try {
    const res = await axios.get(`${API}/vehicle/search`, { params: { plate: bindDialog.licensePlate } })
    if (res.data.code === 200 && res.data.data?.length) bindDialog.vehicleInfo = res.data.data[0]
    else bindDialog.vehicleInfo = null
  } catch {}
}
const confirmBind = async () => {
  if (!bindDialog.vehicleInfo) return
  try {
    await axios.post(`${API}/etc-card/bind`, {
      etcNumber: bindDialog.card?.etcNumber,
      vehicleId: bindDialog.vehicleInfo.vehicleId
    })
    ElMessage.success('绑定成功')
    bindDialog.visible = false
    fetchData()
  } catch {
    ElMessage.error('绑定失败')
  }
}

const resetFilter = () => { filter.keyword = ''; filter.status = ''; filter.userType = ''; fetchData() }
const resetPassFilter = () => { passFilter.keyword = ''; passFilter.location = ''; passFilter.result = ''; passFilter.date = ''; fetchPassRecords() }
const changePage = dir => { const np = pagination.page + dir; if (np >= 1 && np <= pagination.totalPages) { pagination.page = np; fetchData() } }
const passChangePage = dir => { const np = passPagination.page + dir; if (np >= 1 && np <= passPagination.totalPages) passPagination.page = np }
const exportData = () => ElMessage.info('导出功能开发中')

const viewDeviceLogs = dev => ElMessage.info(`查看 ${dev.name} 的日志`)
const configDevice = dev => ElMessage.info(`配置 ${dev.name}`)
const resetDevice = dev => { dev.status = 'online'; ElMessage.success(`${dev.name} 重启成功`) }
const openAddDeviceDialog = () => ElMessage.info('添加设备功能开发中')

// 工具函数
const getCardStatusLabel = s => ({ normal: '正常', lost: '已挂失', disabled: '已停用', unbound: '未绑定', expired: '已过期' }[s] || s)
const getUserTypeText = t => ({ student: '学生', teacher: '教职工', service: '后勤', temporary: '临时', admin: '管理员' }[t] || t || '—')
const getDeviceStatusLabel = s => ({ online: '在线', offline: '离线', fault: '故障' }[s] || s)
const formatDate = d => d ? String(d).split(' ')[0].split('T')[0] : ''
const isExpired = d => d && new Date(d) < new Date()
const isExpireSoon = d => { if (!d) return false; const diff = new Date(d) - Date.now(); return diff > 0 && diff < 30 * 86400000 }
const getPassRate = dev => dev.todayCount + dev.todayDeny > 0 ? Math.round(dev.todayCount / (dev.todayCount + dev.todayDeny) * 100) : 0

watch(activeTab, v => {
  if (v === 'lost') fetchLostCards()
})

onMounted(() => {
  fetchData()
  fetchPassRecords()
  fetchReaderDevices()
  if (activeTab.value === 'lost') fetchLostCards()
})
</script>

<style scoped>
.rfid-container { min-height: 100vh; background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%); color: #fff; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.page-header .header-bg { background: linear-gradient(90deg, rgba(0,123,255,0.1), rgba(102,16,242,0.1)); border-radius: 16px; padding: 20px 24px; border: 1px solid rgba(0,123,255,0.2); display: flex; justify-content: space-between; align-items: center; position: relative; }
.header-bg::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, #007bff, #6610f2); border-radius: 16px 16px 0 0; }
.header-left { display: flex; flex-direction: column; gap: 4px; }
.title-text { font-size: 22px; font-weight: bold; background: linear-gradient(90deg, #fff, #00d4ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.title-sub { font-size: 12px; color: rgba(255,255,255,0.4); }
.header-stats { display: flex; align-items: center; gap: 16px; background: rgba(0,0,0,0.3); padding: 8px 18px; border-radius: 10px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 20px; font-weight: bold; color: #00d4ff; }
.stat-num.c-green { color: #4caf50; } .stat-num.c-red { color: #f44336; } .stat-num.c-orange { color: #ff9800; } .stat-num.c-gray { color: #9e9e9e; }
.stat-lab { font-size: 11px; color: rgba(255,255,255,0.5); }
.stat-div { width: 1px; height: 24px; background: rgba(255,255,255,0.15); }
.header-actions { display: flex; gap: 10px; }
.action-btn { display: inline-flex; align-items: center; gap: 5px; padding: 8px 15px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.action-btn:hover { background: rgba(255,255,255,0.15); }
.action-btn.primary { background: linear-gradient(90deg, #007bff, #6610f2); border: none; }
.action-btn.success { background: rgba(76,175,80,0.3); border-color: rgba(76,175,80,0.5); color: #4caf50; }
.action-btn.danger { background: rgba(244,67,54,0.3); border-color: rgba(244,67,54,0.5); color: #f44336; }
.action-btn.warn { background: rgba(255,152,0,0.2); border-color: rgba(255,152,0,0.3); color: #ff9800; }
.action-btn.small { padding: 4px 10px; font-size: 12px; }
.action-btn.disabled { opacity: 0.4; cursor: not-allowed; }
.sub-tabs { display: flex; gap: 4px; background: rgba(255,255,255,0.05); border-radius: 10px; padding: 5px; }
.sub-tab { display: flex; align-items: center; gap: 6px; padding: 8px 18px; border-radius: 7px; cursor: pointer; font-size: 14px; color: rgba(255,255,255,0.6); transition: all 0.2s; position: relative; }
.sub-tab.active { background: rgba(0,212,255,0.15); color: #00d4ff; }
.tab-badge { background: #f44336; color: #fff; font-size: 10px; padding: 1px 5px; border-radius: 8px; position: absolute; top: 4px; right: 10px; }
.tab-content { display: flex; flex-direction: column; gap: 14px; }
.filter-bar { display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,0.05); border-radius: 10px; padding: 10px 14px; border: 1px solid rgba(255,255,255,0.08); flex-wrap: wrap; }
.tech-input { padding: 8px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; }
.tech-input:focus { outline: none; border-color: #00d4ff; }
.tech-select { padding: 8px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; min-width: 110px; }
.table-container { background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); }
.table-header { display: flex; justify-content: space-between; align-items: center; padding: 13px 18px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.table-title { font-size: 15px; font-weight: 500; }
.page-controls { display: flex; align-items: center; gap: 8px; }
.total-info { font-size: 12px; color: rgba(255,255,255,0.4); }
.page-btn { width: 27px; height: 27px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 6px; cursor: pointer; }
.page-btn.disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 12px; color: rgba(255,255,255,0.6); }
.table-wrapper { overflow-x: auto; padding: 12px; }
.tech-table { width: 100%; border-collapse: separate; border-spacing: 0; min-width: 900px; }
.tech-table th { padding: 9px 10px; text-align: left; font-size: 12px; color: rgba(255,255,255,0.5); border-bottom: 2px solid rgba(0,212,255,0.2); }
.tech-table td { padding: 10px 10px; font-size: 13px; border-bottom: 1px solid rgba(255,255,255,0.05); }
.tech-table tbody tr:hover { background: rgba(0,212,255,0.04); }
.card-no-cell { display: flex; align-items: center; gap: 8px; }
.rfid-icon { font-size: 16px; }
.card-no { font-family: monospace; font-size: 13px; color: #00d4ff; letter-spacing: 0.5px; }
.plate-badge { display: inline-block; background: rgba(0,212,255,0.15); color: #00d4ff; padding: 2px 9px; border-radius: 5px; font-size: 13px; font-weight: bold; }
.no-bind { color: rgba(255,255,255,0.3); font-size: 12px; }
.owner-cell { display: flex; flex-direction: column; gap: 2px; }
.owner-sub { font-size: 11px; color: rgba(255,255,255,0.4); }
.utype-badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.utype-badge.student { background: rgba(0,123,255,0.2); color: #007bff; }
.utype-badge.teacher { background: rgba(40,167,69,0.2); color: #28a745; }
.utype-badge.service { background: rgba(255,193,7,0.2); color: #ffc107; }
.utype-badge.temporary { background: rgba(156,39,176,0.2); color: #9c27b0; }
.expire-warn { color: #ff9800; }
.expire-bad { color: #f44336; }
.expire-tip { font-size: 10px; color: #ff9800; }
.status-badge { padding: 3px 9px; border-radius: 12px; font-size: 12px; }
.status-badge.normal { background: rgba(76,175,80,0.2); color: #4caf50; border: 1px solid rgba(76,175,80,0.3); }
.status-badge.lost { background: rgba(244,67,54,0.2); color: #f44336; border: 1px solid rgba(244,67,54,0.3); }
.status-badge.disabled { background: rgba(158,158,158,0.2); color: #9e9e9e; border: 1px solid rgba(158,158,158,0.3); }
.status-badge.unbound { background: rgba(255,152,0,0.2); color: #ff9800; border: 1px solid rgba(255,152,0,0.3); }
.last-pass { font-size: 12px; color: rgba(255,255,255,0.4); }
.action-cell { display: flex; gap: 5px; flex-wrap: wrap; }
.empty-row { text-align: center; padding: 30px; color: rgba(255,255,255,0.3); font-size: 13px; }
/* 通行统计 */
.pass-stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.pass-stat-card { background: rgba(255,255,255,0.05); border-radius: 10px; padding: 14px 16px; border: 1px solid rgba(255,255,255,0.08); display: flex; align-items: center; gap: 14px; }
.ps-icon { font-size: 24px; }
.ps-val { font-size: 22px; font-weight: bold; color: #00d4ff; }
.ps-lab { font-size: 12px; color: rgba(255,255,255,0.5); }
.dir-badge { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.dir-badge.in { background: rgba(33,150,243,0.2); color: #2196f3; }
.dir-badge.out { background: rgba(255,152,0,0.2); color: #ff9800; }
.result-badge { font-size: 12px; }
.deny-reason { font-size: 12px; color: rgba(255,255,255,0.4); }
/* 设备卡片 */
.device-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.device-card { background: rgba(255,255,255,0.05); border-radius: 14px; padding: 18px; border: 1px solid rgba(255,255,255,0.1); }
.dev-header { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 14px; }
.dev-icon { font-size: 24px; flex-shrink: 0; }
.dev-info { flex: 1; }
.dev-name { font-size: 15px; font-weight: 600; }
.dev-location { font-size: 12px; color: rgba(255,255,255,0.5); margin-top: 3px; }
.dev-status-badge { padding: 3px 10px; border-radius: 12px; font-size: 12px; }
.dev-status-badge.online { background: rgba(76,175,80,0.2); color: #4caf50; }
.dev-status-badge.offline { background: rgba(158,158,158,0.2); color: #9e9e9e; }
.dev-status-badge.fault { background: rgba(244,67,54,0.2); color: #f44336; }
.dev-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px; }
.dev-stat { background: rgba(0,0,0,0.2); border-radius: 6px; padding: 8px 10px; }
.ds-label { display: block; font-size: 11px; color: rgba(255,255,255,0.4); margin-bottom: 3px; }
.ds-val { font-size: 14px; font-weight: 500; }
.ds-val.c-red { color: #f44336; }
.dev-progress-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.dpb-label { font-size: 12px; color: rgba(255,255,255,0.5); white-space: nowrap; width: 65px; }
.dpb-bar { flex: 1; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; }
.dpb-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }
.dpb-val { font-size: 12px; color: rgba(255,255,255,0.6); width: 35px; text-align: right; }
.dev-actions { display: flex; gap: 6px; }
/* 挂失管理 */
.lost-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 16px; }
.lost-card { background: rgba(255,255,255,0.05); border-radius: 14px; border: 1px solid rgba(244,67,54,0.2); padding: 16px; }
.lost-card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.lost-info { flex: 1; }
.lost-no { font-family: monospace; font-size: 15px; font-weight: bold; color: #00d4ff; }
.lost-plate { font-size: 12px; color: rgba(255,255,255,0.5); margin-top: 3px; }
.lost-detail { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.ld-item { display: flex; gap: 10px; font-size: 13px; }
.ld-label { color: rgba(255,255,255,0.4); min-width: 60px; }
.lost-actions { display: flex; gap: 8px; }
/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: linear-gradient(135deg, #1a1f3c, #2d3250); border-radius: 16px; border: 1px solid rgba(0,123,255,0.3); width: 520px; max-height: 85vh; display: flex; flex-direction: column; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 22px; border-bottom: 1px solid rgba(255,255,255,0.1); flex-shrink: 0; }
.modal-title { font-size: 16px; font-weight: 600; }
.modal-close { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 50%; cursor: pointer; }
.modal-body { padding: 20px 22px; overflow-y: auto; flex: 1; }
.modal-footer { padding: 12px 22px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; justify-content: flex-end; gap: 10px; flex-shrink: 0; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-item { display: flex; flex-direction: column; gap: 7px; }
.form-label { font-size: 13px; color: rgba(255,255,255,0.7); }
.form-hint { font-size: 11px; color: rgba(255,255,255,0.3); }
.bind-vehicle-hint { font-size: 12px; color: #4caf50; margin-top: 4px; }
/* 卡片视觉 */
.card-visual { display: flex; justify-content: center; margin-bottom: 18px; }
.card-front { width: 300px; height: 170px; background: linear-gradient(135deg, #0d47a1, #1565c0, #0288d1); border-radius: 14px; padding: 18px; display: flex; flex-direction: column; justify-content: space-between; box-shadow: 0 8px 32px rgba(0,0,0,0.5), inset 0 1px 0 rgba(255,255,255,0.1); position: relative; overflow: hidden; }
.card-front::before { content: ''; position: absolute; top: -50%; right: -20%; width: 200px; height: 200px; background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 60%); }
.card-top-row { display: flex; justify-content: space-between; align-items: center; }
.card-brand { font-size: 14px; font-weight: bold; color: rgba(255,255,255,0.9); letter-spacing: 2px; }
.card-chip { font-size: 20px; color: rgba(255,255,255,0.7); }
.card-wave { position: relative; height: 40px; display: flex; align-items: center; }
.wave-lines { display: flex; gap: 3px; align-items: center; }
.wl { width: 20px; height: 20px; border: 2px solid rgba(255,255,255,0.8); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.wl:nth-child(1) { width: 12px; height: 12px; } .wl:nth-child(2) { width: 18px; height: 18px; } .wl:nth-child(3) { width: 24px; height: 24px; } .wl:nth-child(4) { width: 30px; height: 30px; }
.card-num-row { font-family: monospace; font-size: 16px; letter-spacing: 2px; color: rgba(255,255,255,0.95); text-shadow: 0 0 10px rgba(0,212,255,0.5); }
.card-bottom-row { display: flex; justify-content: space-between; font-size: 12px; color: rgba(255,255,255,0.7); }
.detail-grid { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.detail-row { display: flex; gap: 12px; padding: 7px 10px; background: rgba(0,0,0,0.2); border-radius: 6px; font-size: 13px; }
.dl { color: rgba(255,255,255,0.4); min-width: 70px; }
.dv { color: #fff; font-weight: 500; }
.recent-pass { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 12px; }
.rp-title { font-size: 13px; color: #00d4ff; margin-bottom: 10px; }
.rp-item { display: flex; align-items: center; gap: 10px; padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 12px; }
.rp-location { flex: 1; color: rgba(255,255,255,0.7); }
.rp-time { color: rgba(255,255,255,0.4); }
.info-block { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 12px; }
.info-row { display: flex; gap: 10px; padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px; }
.info-row:last-child { border-bottom: none; }
.il { color: rgba(255,255,255,0.4); min-width: 70px; }
.bind-result { display: flex; align-items: center; gap: 12px; background: rgba(76,175,80,0.1); border: 1px solid rgba(76,175,80,0.2); border-radius: 8px; padding: 12px; margin-top: 12px; }
.br-icon { font-size: 20px; color: #4caf50; }
.br-sub { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 3px; }
.empty-state { text-align: center; padding: 50px 20px; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-text { font-size: 14px; color: rgba(255,255,255,0.4); }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,0.05); border-radius: 3px; }
::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.3); border-radius: 3px; }
</style>

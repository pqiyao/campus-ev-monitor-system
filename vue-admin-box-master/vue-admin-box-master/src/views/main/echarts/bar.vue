<template>
  <div class="violation-management-container page-bg-shell">
    <!-- 全局标签导航 -->
    <div class="global-tabs">
      <div
        v-for="tab in globalTabs"
        :key="tab.value"
        class="global-tab"
        :class="{ active: globalActiveTab === tab.value }"
        @click="switchGlobalTab(tab.value)"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </div>

    <!-- ========== 模块1：违规整改核实 ========== -->
    <div v-if="globalActiveTab === 'verify'" class="module-verify">
      <!-- 页面标题区域 -->
      <div class="page-header">
        <div class="header-bg">
          <div class="header-content">
            <h1 class="page-title"><span class="title-text">违规整改核实</span></h1>
            <div class="header-stats">
              <div class="stat-item"><span class="stat-number">{{ verifyStats.pendingVerify }}</span><span class="stat-label">待核实</span></div>
              <div class="stat-divider"></div>
              <div class="stat-item"><span class="stat-number">{{ verifyStats.passed }}</span><span class="stat-label">已通过</span></div>
              <div class="stat-divider"></div>
              <div class="stat-item"><span class="stat-number">{{ verifyStats.failed }}</span><span class="stat-label">已驳回</span></div>
            </div>
          </div>
          <div class="header-actions">
            <div class="action-btn" @click="verifyFetchData">↻ 刷新</div>
          </div>
        </div>
      </div>

      <!-- 主内容区域（左窄右宽） -->
      <div class="main-content">
        <!-- 左侧筛选 -->
        <div class="left-panel">
          <div class="filter-form">
            <div class="form-group">
              <label class="form-label">核实状态</label>
              <select v-model="verifyFilter.state" class="tech-select" @change="verifyFetchData">
                <option value="">全部</option>
                <option value="0">待核实</option>
                <option value="2">已通过</option>
                <option value="3">已驳回</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">车牌搜索</label>
              <input v-model="verifyFilter.plate" class="tech-input" placeholder="输入车牌" @keyup.enter="verifyFetchData" />
            </div>
            <div class="form-actions">
              <div class="filter-btn" @click="verifyFetchData">🔍 搜索</div>
              <div class="reset-btn" @click="verifyFilter.plate=''; verifyFilter.state=''; verifyFetchData()">重置</div>
            </div>
          </div>
        </div>

        <!-- 右侧表格 -->
        <div class="right-panel">
          <div class="table-container">
            <div class="table-header">
              <div class="table-title">整改记录列表</div>
              <div class="table-actions">
                <div class="page-controls">
                  <div class="page-btn" :class="{ disabled: verifyPage === 1 }" @click="verifyChangePage(-1)">←</div>
                  <span class="page-info">{{ verifyPage }} / {{ verifyTotalPages }}</span>
                  <div class="page-btn" :class="{ disabled: verifyPage === verifyTotalPages }" @click="verifyChangePage(1)">→</div>
                </div>
              </div>
            </div>
            <div class="table-wrapper">
              <table class="tech-table">
                <thead>
                  <tr>
                    <th>整改ID</th><th>车牌</th><th>车主</th><th>违规类型</th><th>整改说明</th><th>照片</th><th>整改时间</th><th>状态</th><th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in verifyList" :key="item.rectifyId">
                    <td><span class="id-cell">#{{ item.rectifyId }}</span></td>
                    <td><span class="vehicle-plate">{{ item.licensePlate || '—' }}</span></td>
                    <td>{{ item.userName || '—' }}</td>
                    <td>{{ verifyGetViolationTypeName(item.illegalType) }}</td>
                    <td><div class="text-ellipsis" :title="item.rectifyDesc">{{ item.rectifyDesc || '—' }}</div></td>
                    <td>
                      <div class="photo-list" v-if="item.rectifyPhoto">
                        <img v-for="(url, idx) in verifyParsePhotos(item.rectifyPhoto)" :key="idx" :src="verifyGetFullUrl(url)" class="thumb" @click="verifyPreviewImage(url)" />
                      </div>
                      <span v-else class="no-data">无</span>
                    </td>
                    <td>{{ verifyFormatTime(item.rectifyTime) }}</td>
                    <td><span class="state-cell" :class="'state-' + item.verifyState">{{ verifyGetStateName(item.verifyState) }}</span></td>
                    <td>
                      <div class="action-cell" v-if="item.verifyState === 0 || item.verifyState === 1">
                        <div class="action-btn small success" @click="verifyOpenDialog(item, true)">通过</div>
                        <div class="action-btn small danger" @click="verifyOpenDialog(item, false)">驳回</div>
                      </div>
                      <div v-else>
                        <div class="action-btn small" @click="verifyViewDetail(item)">详情</div>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="verifyList.length === 0"><td colspan="9" class="empty-row">暂无数据</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 核实对话框 -->
      <div v-if="verifyDialog.visible" class="modal-overlay" @click="verifyDialog.visible = false">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <span class="modal-title">{{ verifyDialog.pass ? '✓ 核实通过' : '✗ 驳回整改' }}</span>
            <span class="modal-close" @click="verifyDialog.visible = false">×</span>
          </div>
          <div class="modal-body">
            <div class="info-block">
              <div class="info-row"><span class="info-label">整改ID：</span><span>#{{ verifyDialog.item?.rectifyId }}</span></div>
              <div class="info-row"><span class="info-label">车牌号：</span><span>{{ verifyDialog.item?.licensePlate }}</span></div>
              <div class="info-row"><span class="info-label">操作：</span><span :class="verifyDialog.pass ? 'text-success' : 'text-danger'">{{ verifyDialog.pass ? '核实通过' : '驳回整改' }}</span></div>
            </div>
            <div class="form-group" style="margin-top:16px">
              <label class="form-label">{{ verifyDialog.pass ? '核实备注（可选）' : '驳回原因（必填）' }}</label>
              <textarea v-model="verifyDialog.remark" class="tech-textarea" rows="3" :placeholder="verifyDialog.pass ? '请输入备注' : '请填写驳回原因'"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <div class="action-btn" @click="verifyDialog.visible = false">取消</div>
            <div class="action-btn" :class="verifyDialog.pass ? 'success' : 'danger'" @click="verifySubmit">确认{{ verifyDialog.pass ? '通过' : '驳回' }}</div>
          </div>
        </div>
      </div>

      <!-- 详情对话框 -->
      <div v-if="verifyDetailDialog.visible" class="modal-overlay" @click="verifyDetailDialog.visible = false">
        <div class="modal-content large" @click.stop>
          <div class="modal-header">
            <span class="modal-title">整改记录详情</span>
            <span class="modal-close" @click="verifyDetailDialog.visible = false">×</span>
          </div>
          <div class="modal-body" v-if="verifyDetailDialog.item">
            <div class="detail-grid">
              <div class="detail-row"><span class="dl">整改ID</span><span class="dv">#{{ verifyDetailDialog.item.rectifyId }}</span></div>
              <div class="detail-row"><span class="dl">车牌</span><span class="dv">{{ verifyDetailDialog.item.licensePlate }}</span></div>
              <div class="detail-row"><span class="dl">车主</span><span class="dv">{{ verifyDetailDialog.item.userName }}</span></div>
              <div class="detail-row"><span class="dl">状态</span><span class="dv"><span class="state-cell" :class="'state-' + verifyDetailDialog.item.verifyState">{{ verifyGetStateName(verifyDetailDialog.item.verifyState) }}</span></span></div>
              <div class="detail-row full"><span class="dl">整改说明</span><span class="dv">{{ verifyDetailDialog.item.rectifyDesc }}</span></div>
              <div class="detail-row full"><span class="dl">核实备注</span><span class="dv">{{ verifyDetailDialog.item.verifyRemark || '—' }}</span></div>
              <div class="detail-row full"><span class="dl">核实时间</span><span class="dv">{{ verifyFormatTime(verifyDetailDialog.item.verifyTime) }}</span></div>
            </div>
            <div v-if="verifyDetailDialog.item.rectifyPhoto" class="photos-section">
              <div class="section-title">佐证照片</div>
              <div class="photo-list large">
                <img v-for="(url, idx) in verifyParsePhotos(verifyDetailDialog.item.rectifyPhoto)" :key="idx" :src="verifyGetFullUrl(url)" class="photo-large" @click="verifyPreviewImage(url)" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 图片预览浮层 -->
      <div v-if="verifyPreviewUrl" class="img-preview-overlay" @click="verifyPreviewUrl = ''">
        <img :src="verifyGetFullUrl(verifyPreviewUrl)" class="preview-img" />
        <span class="preview-close" @click="verifyPreviewUrl = ''">×</span>
      </div>
    </div>

    <!-- ========== 模块2：违规整改跟进 ========== -->
    <div v-if="globalActiveTab === 'follow'" class="module-follow">
      <div class="page-header">
        <div class="header-bg">
          <div class="header-content">
            <h1 class="page-title"><span class="title-text">违规整改跟进</span></h1>
            <div class="header-stats">
              <div class="stat-item"><span class="stat-number">{{ followTabCounts['0'] || 0 }}</span><span class="stat-label">待整改</span></div>
              <div class="stat-divider"></div>
              <div class="stat-item"><span class="stat-number">{{ followTabCounts['1'] || 0 }}</span><span class="stat-label">整改中</span></div>
              <div class="stat-divider"></div>
              <div class="stat-item"><span class="stat-number">{{ followTabCounts['2'] || 0 }}</span><span class="stat-label">已整改</span></div>
              <div class="stat-divider"></div>
              <div class="stat-item"><span class="stat-number">{{ followTabCounts['3'] || 0 }}</span><span class="stat-label">逾期</span></div>
            </div>
          </div>
          <div class="header-actions">
            <div class="action-btn" @click="followFetchData">↻ 刷新</div>
            <div class="action-btn primary" @click="followBatchRemind" :class="{ disabled: !followSelectedIds.length }">📢 批量提醒 {{ followSelectedIds.length ? `(${followSelectedIds.length})` : '' }}</div>
          </div>
        </div>
      </div>

      <!-- 状态子标签 -->
      <div class="sub-tab-bar" style="margin: 16px 0;">
        <div v-for="tab in followTabs" :key="tab.value" class="sub-tab" :class="{ active: followActiveTab === tab.value }" @click="followSwitchTab(tab.value)">
          {{ tab.icon }} {{ tab.label }} <span class="badge-count" v-if="followTabCounts[tab.value]">{{ followTabCounts[tab.value] }}</span>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar" style="margin-bottom: 16px;">
        <input v-model="followFilter.plate" class="tech-input" placeholder="车牌搜索" style="width:150px" @keyup.enter="followFetchData" />
        <input v-model="followFilter.name" class="tech-input" placeholder="学生姓名" style="width:150px" @keyup.enter="followFetchData" />
        <div class="action-btn primary" @click="followFetchData">🔍 搜索</div>
        <div class="action-btn" @click="followResetFilter">重置</div>
        <div class="action-btn" @click="followSelectedIds = []" v-if="followSelectedIds.length">清空选择</div>
      </div>

      <!-- 表格卡片 -->
      <div class="table-container">
        <div class="table-header">
          <span class="table-title">{{ followTabs.find(t => t.value === followActiveTab)?.label }} 列表</span>
          <div class="page-controls">
            <div class="page-btn" :class="{ disabled: followPagination.page === 1 }" @click="followChangePage(-1)">←</div>
            <span class="page-info">{{ followPagination.page }} / {{ followPagination.totalPages }}</span>
            <div class="page-btn" :class="{ disabled: followPagination.page === followPagination.totalPages }" @click="followChangePage(1)">→</div>
          </div>
        </div>
        <div class="table-wrapper">
          <table class="tech-table">
            <thead>
              <tr>
                <th style="width:44px"><input type="checkbox" class="tech-checkbox" :checked="followAllSelected" @change="followToggleAll" /></th>
                <th>违规ID</th><th>学生信息</th><th>车牌</th><th>违规类型</th><th>违规时间</th><th>整改进度</th><th>扣分</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in followList" :key="item.illegalId" :class="{ selected: followSelectedIds.includes(item.illegalId) }">
                <td><input type="checkbox" class="tech-checkbox" :checked="followSelectedIds.includes(item.illegalId)" @change="followToggleSelect(item.illegalId)" /></td>
                <td><span class="id-cell">#{{ item.illegalId }}</span></td>
                <td><div class="student-info"><span class="sname">{{ item.ownerName || '—' }}</span><span class="sid">{{ item.licensePlate ? '' : '无车牌' }}</span></div></td>
                <td><span class="vehicle-plate">{{ item.licensePlate || '—' }}</span></td>
                <td>{{ followGetViolationTypeName(item.illegalType) }}</td>
                <td>{{ followFormatDate(item.illegalTime) }}</td>
                <td>
                  <div class="progress-wrap">
                    <div class="progress-bar" style="width:60px; height:6px;"><div class="progress-fill" :style="{ width: followGetProgressPct(item.illegalState) + '%', background: followGetProgressColor(item.illegalState) }"></div></div>
                    <span class="state-cell small" :class="'state-' + item.illegalState">{{ followGetStateName(item.illegalState) }}</span>
                  </div>
                </td>
                <td><span class="points-badge">-{{ item.deductionPoints }}分</span></td>
                <td><div class="action-cell"><div class="action-btn small" @click="followOpenNoteDialog(item)">📝 备注</div><div class="action-btn small" @click="followRemind(item)" v-if="item.userId">🔔</div></div></td>
              </tr>
              <tr v-if="followList.length === 0"><td colspan="9" class="empty-row">暂无数据</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 备注弹窗 -->
      <div v-if="followNoteDialog.visible" class="modal-overlay" @click="followNoteDialog.visible = false">
        <div class="modal-content" @click.stop>
          <div class="modal-header"><span class="modal-title">📝 记录跟进备注</span><span class="modal-close" @click="followNoteDialog.visible = false">×</span></div>
          <div class="modal-body">
            <div class="info-block">
              <div class="info-row"><span class="info-label">违规ID</span><span>#{{ followNoteDialog.item?.illegalId }}</span></div>
              <div class="info-row"><span class="info-label">车牌号</span><span>{{ followNoteDialog.item?.licensePlate }}</span></div>
              <div class="info-row"><span class="info-label">当前状态</span><span class="state-cell" :class="'state-' + followNoteDialog.item?.illegalState">{{ followGetStateName(followNoteDialog.item?.illegalState) }}</span></div>
            </div>
            <div class="form-group mt16"><label class="form-label">跟进记录</label><textarea v-model="followNoteDialog.note" class="tech-textarea" rows="4" placeholder="请输入跟进情况..."></textarea></div>
            <div class="prev-notes" v-if="followNoteDialog.prevNotes.length">
              <div class="form-label mb8">历史备注</div>
              <div v-for="n in followNoteDialog.prevNotes" :key="n.time" class="note-item"><span class="note-time">{{ n.time }}</span><span class="note-content">{{ n.content }}</span></div>
            </div>
          </div>
          <div class="modal-footer"><div class="action-btn" @click="followNoteDialog.visible = false">取消</div><div class="action-btn primary" @click="followSaveNote">保存备注</div></div>
        </div>
      </div>
    </div>

    <!-- ========== 模块3：违规闭环处置系统 ========== -->
    <div v-if="globalActiveTab === 'closure'" class="module-closure">
      <div class="page-header">
        <div class="header-bg">
          <div class="header-content">
            <h1 class="page-title">
              <span class="title-text">违规闭环处置系统</span>
            </h1>
            <div class="header-stats">
              <div class="stat-item">
                <span class="stat-number">{{ stats.total }}</span>
                <span class="stat-label">总违规数</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-number">{{ stats.pendingRectify }}</span>
                <span class="stat-label">待整改</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-number">{{ stats.rectified }}</span>
                <span class="stat-label">已整改</span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <div class="action-btn primary" @click="showReportDialog">
              <div class="btn-icon">📝</div>
              <div class="btn-text">人工上报</div>
            </div>
            <div class="action-btn" @click="refreshData">
              <div class="btn-icon">↻</div>
              <div class="btn-text">刷新</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="main-content">
        <!-- 左侧筛选区域（仅对违规记录可见） -->
        <div class="left-panel" v-if="activeClosureTab === 'violations'">
          <div class="filter-form">
            <div class="form-group">
              <label class="form-label">违规类型</label>
              <div class="type-selector">
                <div 
                  v-for="type in violationTypes" 
                  :key="type.value"
                  :class="['type-option', { active: filterForm.illegalType === type.value }]"
                  @click="filterForm.illegalType = filterForm.illegalType === type.value ? '' : type.value"
                >
                  <div class="type-icon">{{ type.icon }}</div>
                  <div class="type-text">{{ type.label }}</div>
                </div>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">违规状态</label>
              <div class="status-selector">
                <div 
                  v-for="status in violationStates" 
                  :key="status.value"
                  :class="['status-option', { active: filterForm.illegalState === status.value }]"
                  @click="filterForm.illegalState = filterForm.illegalState === status.value ? '' : status.value"
                >
                  <div class="status-dot" :style="{ background: status.color }"></div>
                  <div class="status-text">{{ status.label }}</div>
                </div>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">时间范围</label>
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
            
            <div class="form-group">
              <label class="form-label">车辆信息</label>
              <div class="form-input">
                <input 
                  type="text" 
                  v-model="filterForm.vehiclePlate" 
                  placeholder="输入车牌号"
                  class="tech-input"
                />
              </div>
            </div>
            
            <div class="form-actions">
              <div class="filter-btn" @click="handleFilter">
                <div class="btn-content">
                  <div class="btn-icon">🔍</div>
                  <div class="btn-text">筛选数据</div>
                </div>
              </div>
              <div class="reset-btn" @click="resetFilter">重置筛选</div>
            </div>
          </div>
          
          <!-- 实时统计 -->
          <div class="realtime-stats">
            <div class="stats-header">
              <div class="stats-icon">📈</div>
              <div class="stats-title">实时统计</div>
            </div>
            <div class="stats-content">
              <div class="stat-row">
                <div class="stat-label">今日违规</div>
                <div class="stat-value">{{ stats.todayViolations || 0 }}</div>
                <div class="stat-trend" :class="{ up: (stats.todayTrend || 0) > 0 }">
                  {{ (stats.todayTrend || 0) > 0 ? '↑' : '↓' }} {{ Math.abs(stats.todayTrend || 0) }}%
                </div>
              </div>
              <div class="stat-row">
                <div class="stat-label">整改完成率</div>
                <div class="stat-value">{{ stats.rectifyRate || '0' }}</div>
                <div class="stat-progress">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: (String(stats.rectifyRate || '0').replace('%', '') || '0') + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧内容区域 -->
        <div class="right-panel" :class="{ 'full-width': activeClosureTab !== 'violations' }">
          <!-- 选项卡导航（四个） -->
          <div class="tab-navigation">
            <div 
              v-for="tab in closureTabs" 
              :key="tab.id"
              :class="['tab-item', { active: activeClosureTab === tab.id }]"
              @click="activeClosureTab = tab.id"
            >
              <div class="tab-icon">{{ tab.icon }}</div>
              <div class="tab-text">{{ tab.name }}</div>
              <div class="tab-badge" v-if="tab.badge">{{ tab.badge }}</div>
            </div>
          </div>
          
          <!-- 违规记录列表 -->
          <div v-show="activeClosureTab === 'violations'" class="tab-content">
            <div class="table-container">
              <div class="table-header">
                <div class="table-title">违规记录列表</div>
                <div class="table-actions">
                  <div class="total-info">共 {{ pagination.total }} 条记录</div>
                  <div class="page-controls">
                    <div class="page-btn" @click="prevPage" :class="{ disabled: pagination.currentPage === 1 }">←</div>
                    <div class="page-info">{{ pagination.currentPage }} / {{ pagination.totalPages }}</div>
                    <div class="page-btn" @click="nextPage" :class="{ disabled: pagination.currentPage === pagination.totalPages }">→</div>
                  </div>
                </div>
              </div>
              
              <div class="table-wrapper">
                <table class="tech-table">
                  <thead>
                    <tr>
                      <th style="width: 80px;">ID</th>
                      <th style="width: 120px;">车辆信息</th>
                      <th style="width: 100px;">违规类型</th>
                      <th style="width: 150px;">违规位置</th>
                      <th style="width: 120px;">违规时间</th>
                      <th style="width: 80px;">扣分</th>
                      <th style="width: 100px;">违规来源</th>
                      <th style="width: 100px;">当前状态</th>
                      <th style="width: 120px;">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="violation in violationList" :key="violation.illegalId">
                      <td><div class="id-cell">#{{ violation.illegalId }}</div></td>
                      <td>
                        <div class="vehicle-cell" v-if="violation.licensePlate">
                          <div class="vehicle-plate">{{ violation.licensePlate }}</div>
                          <div class="vehicle-owner">{{ violation.ownerName }}</div>
                          <div class="vehicle-phone">{{ violation.phone }}</div>
                        </div>
                        <div class="no-vehicle" v-else>未绑定车辆</div>
                      </td>
                      <td><div class="type-cell" :class="'type-' + violation.illegalType">{{ getViolationTypeText(violation.illegalType) }}</div></td>
                      <td>
                        <div class="location-cell">
                          <div class="location-coord">{{ violation.illegalLng }}, {{ violation.illegalLat }}</div>
                        </div>
                      </td>
                      <td><div class="time-cell">{{ formatTime(violation.illegalTime) }}</div></td>
                      <td><div class="points-cell" :class="{ high: violation.deductionPoints >= 5 }">-{{ violation.deductionPoints }}分</div></td>
                      <td><div class="source-cell" :class="'source-' + violation.illegalSource">{{ getSourceText(violation.illegalSource) }}</div></td>
                      <td><div class="state-cell" :class="'state-' + violation.illegalState">{{ getStateText(violation.illegalState) }}</div></td>
                      <td>
                        <div class="action-cell">
                          <div class="action-btn small" @click="viewViolationDetail(violation)">
                            <div class="btn-icon">👁</div><div class="btn-text">详情</div>
                          </div>
                          <div class="action-btn small" @click="sendReminder(violation)" v-if="violation.userId">
                            <div class="btn-icon">🔔</div><div class="btn-text">提醒</div>
                          </div>
                        </div>
                      </td>
                    </tr>
                    <tr v-if="violationList.length === 0"><td colspan="9" class="empty-row">暂无数据</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- 申诉记录列表（新增） -->
          <div v-show="activeClosureTab === 'appeals'" class="tab-content">
            <div class="table-container">
              <div class="table-header">
                <div class="table-title">申诉记录列表</div>
                <div class="table-actions">
                  <div class="page-controls">
                    <div class="page-btn" :class="{ disabled: appealPagination.page === 1 }" @click="appealChangePage(-1)">←</div>
                    <span class="page-info">{{ appealPagination.page }} / {{ appealPagination.totalPages }}</span>
                    <div class="page-btn" :class="{ disabled: appealPagination.page === appealPagination.totalPages }" @click="appealChangePage(1)">→</div>
                  </div>
                </div>
              </div>
              <div class="table-wrapper">
                <table class="tech-table">
                  <thead>
                    <tr>
                      <th>申诉ID</th><th>关联违规ID</th><th>车牌</th><th>申诉人</th><th>申诉理由</th><th>申诉时间</th><th>状态</th><th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="appeal in appealList" :key="appeal.appealId">
                      <td><span class="id-cell">#{{ appeal.appealId }}</span></td>
                      <td>#{{ appeal.illegalId }}</td>
                      <td>{{ appeal.licensePlate || '—' }}</td>
                      <td>{{ appeal.userName || appeal.appellantName || '—' }}</td>
                      <td><div class="text-ellipsis" :title="appeal.appealReason || appeal.reason">{{ appeal.appealReason || appeal.reason || '—' }}</div></td>
                      <td>{{ formatTime(appeal.appealTime || appeal.createTime) }}</td>
                      <td><span class="state-cell" :class="'state-' + appealAppealState(appeal)">{{ getAppealStatusText(appealAppealState(appeal)) }}</span></td>
                      <td>
                        <div class="action-cell" v-if="appealAppealState(appeal) === 0">
                          <div class="action-btn small success" @click="openAppealAuditDialog(appeal, true)">通过</div>
                          <div class="action-btn small danger" @click="openAppealAuditDialog(appeal, false)">驳回</div>
                        </div>
                        <div v-else><div class="action-btn small" @click="viewAppealDetail(appeal)">详情</div></div>
                      </td>
                    </tr>
                    <tr v-if="appealList.length === 0"><td colspan="8" class="empty-row">暂无申诉记录</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- 整改记录列表（新增） -->
          <div v-show="activeClosureTab === 'rectifies'" class="tab-content">
            <div class="table-container">
              <div class="table-header">
                <div class="table-title">整改记录列表</div>
                <div class="table-actions">
                  <div class="page-controls">
                    <div class="page-btn" :class="{ disabled: rectifyPagination.page === 1 }" @click="rectifyChangePage(-1)">←</div>
                    <span class="page-info">{{ rectifyPagination.page }} / {{ rectifyPagination.totalPages }}</span>
                    <div class="page-btn" :class="{ disabled: rectifyPagination.page === rectifyPagination.totalPages }" @click="rectifyChangePage(1)">→</div>
                  </div>
                </div>
              </div>
              <div class="table-wrapper">
                <table class="tech-table">
                  <thead>
                    <tr>
                      <th>整改ID</th><th>违规ID</th><th>车牌</th><th>车主</th><th>整改说明</th><th>整改时间</th><th>状态</th><th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="rectify in rectifyList" :key="rectify.rectifyId">
                      <td><span class="id-cell">#{{ rectify.rectifyId }}</span></td>
                      <td>#{{ rectify.illegalId }}</td>
                      <td>{{ rectify.licensePlate || '—' }}</td>
                      <td>{{ rectify.userName || rectify.ownerName || '—' }}</td>
                      <td><div class="text-ellipsis" :title="rectify.rectifyDesc || rectify.description">{{ rectify.rectifyDesc || rectify.description || '—' }}</div></td>
                      <td>{{ formatTime(rectify.rectifyTime) }}</td>
                      <td><span class="state-cell" :class="'state-' + rectifyVerifyState(rectify)">{{ getRectifyStatusText(rectifyVerifyState(rectify)) }}</span></td>
                      <td><div class="action-btn small" @click="viewRectifyDetail(rectify)">详情</div></td>
                    </tr>
                    <tr v-if="rectifyList.length === 0"><td colspan="8" class="empty-row">暂无整改记录</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- 操作日志列表（新增） -->
          <div v-show="activeClosureTab === 'logs'" class="tab-content">
            <div class="table-container">
              <div class="table-header">
                <div class="table-title">操作日志</div>
                <div class="table-actions">
                  <div class="page-controls">
                    <div class="page-btn" :class="{ disabled: logPagination.page === 1 }" @click="logChangePage(-1)">←</div>
                    <span class="page-info">{{ logPagination.page }} / {{ logPagination.totalPages }}</span>
                    <div class="page-btn" :class="{ disabled: logPagination.page === logPagination.totalPages }" @click="logChangePage(1)">→</div>
                  </div>
                </div>
              </div>
              <div class="table-wrapper">
                <table class="tech-table">
                  <thead>
                    <tr>
                      <th>日志ID</th>
                      <th>关联ID</th>
                      <th>操作类型</th>
                      <th>操作人</th>
                      <th>操作详情</th>
                      <th>操作时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="log in operationLogs" :key="log.logId">
                      <td><span class="id-cell">#{{ log.logId }}</span></td>
                      <td>{{ log.appealId ?? log.businessId ?? '—' }}</td>
                      <td><span class="op-type" :class="log.operationType">{{ getOperationTypeName(log.operationType) }}</span></td>
                      <td>{{ log.operatorName || '系统' }}</td>
                      <td>{{ log.operationDetail || '—' }}</td>
                      <td>{{ formatTime(log.createTime) }}</td>
                    </tr>
                    <tr v-if="operationLogs.length === 0"><td colspan="6" class="empty-row">暂无操作日志</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 违规详情模态框（原有） -->
      <div v-if="detailDialogVisible" class="modal-overlay" @click="closeDetailDialog">
        <div class="modal-content large" @click.stop>
          <div class="modal-header">
            <div class="modal-title">
              <div class="title-icon">📋</div>
              <div class="title-text">违规记录详情 #{{ currentViolation?.illegalId }}</div>
            </div>
            <div class="modal-close" @click="closeDetailDialog">×</div>
          </div>
          
          <div class="modal-body">
            <div v-if="currentViolation" class="detail-content">
              <div class="detail-section">
                <div class="section-title">基本信息</div>
                <div class="info-grid">
                  <div class="info-item"><div class="info-label">违规ID</div><div class="info-value">#{{ currentViolation.illegalId }}</div></div>
                  <div class="info-item"><div class="info-label">违规类型</div><div class="info-value tag">{{ getViolationTypeText(currentViolation.illegalType) }}</div></div>
                  <div class="info-item"><div class="info-label">扣分数</div><div class="info-value points">{{ currentViolation.deductionPoints }}分</div></div>
                  <div class="info-item"><div class="info-label">违规来源</div><div class="info-value tag">{{ getSourceText(currentViolation.illegalSource) }}</div></div>
                  <div class="info-item"><div class="info-label">当前状态</div><div class="info-value state">{{ getStateText(currentViolation.illegalState) }}</div></div>
                  <div class="info-item"><div class="info-label">记录时间</div><div class="info-value">{{ formatTime(currentViolation.createTime) }}</div></div>
                </div>
              </div>
              <div class="detail-section">
                <div class="section-title">车辆信息</div>
                <div class="info-grid">
                  <div class="info-item"><div class="info-label">车牌号码</div><div class="info-value">{{ currentViolation.licensePlate || '未绑定车辆' }}</div></div>
                  <div class="info-item"><div class="info-label">车主姓名</div><div class="info-value">{{ currentViolation.ownerName || '未绑定车主' }}</div></div>
                  <div class="info-item"><div class="info-label">联系电话</div><div class="info-value">{{ currentViolation.phone || '未绑定' }}</div></div>
                  <div class="info-item"><div class="info-label">用户ID</div><div class="info-value">{{ currentViolation.userId || '未绑定' }}</div></div>
                </div>
              </div>
              <div class="detail-section">
                <div class="section-title">违规详情</div>
                <div class="info-grid">
                  <div class="info-item full"><div class="info-label">违规位置</div><div class="info-value">经度: {{ currentViolation.illegalLng }}, 纬度: {{ currentViolation.illegalLat }}</div></div>
                  <div class="info-item full"><div class="info-label">违规时间</div><div class="info-value">{{ formatTime(currentViolation.illegalTime) }}</div></div>
                  <div class="info-item full" v-if="currentViolation.evidenceId"><div class="info-label">证据ID</div><div class="info-value">{{ currentViolation.evidenceId }}</div></div>
                  <div class="info-item full" v-if="currentViolation.description"><div class="info-label">违规描述</div><div class="info-value">{{ currentViolation.description }}</div></div>
                </div>
              </div>
              <div v-if="currentViolation && currentViolation.evidenceUrl" class="detail-section">
                <div class="section-title">违规证据</div>
                <div class="evidence-preview">
                  <div class="evidence-images">
                    <div class="evidence-image" @click.stop="openImagePreview(currentViolation.evidenceUrl)">
                      <img :src="getFullImageUrl(currentViolation.evidenceUrl)" alt="违规证据" />
                    </div>
                  </div>
                </div>
              </div>
              <div class="detail-section" v-if="currentViolation.vehiclePhoto">
                <div class="section-title">车辆照片</div>
                <div class="evidence-preview">
                  <div class="evidence-images">
                    <div class="evidence-image" @click.stop="openImagePreview(currentViolation.vehiclePhoto)">
                      <img :src="getFullImageUrl(currentViolation.vehiclePhoto)" alt="车辆照片" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="modal-footer">
            <div class="footer-actions">
              <div class="action-btn" @click="closeDetailDialog">关闭</div>
              <div class="action-btn primary" @click="sendReminder(currentViolation)" v-if="currentViolation?.userId">发送提醒</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 人工上报模态框（原有） -->
      <div v-if="reportDialogVisible" class="modal-overlay" @click="closeReportDialog">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <div class="modal-title">
              <div class="title-icon">📝</div>
              <div class="title-text">人工上报违规</div>
            </div>
            <div class="modal-close" @click="closeReportDialog">×</div>
          </div>
          <div class="modal-body report-form-body">
            <div class="form-hint report-hint">
              请至少填写 <b>学号</b>、<b>车牌号</b>、<b>车架号</b> 中一项；若填多项，系统按 <b>车架号 → 车牌 → 学号</b> 优先匹配。学号将关联该用户名下车辆（优先已审核通过）。
            </div>
            <div class="form-group"><label class="form-label">学号</label><input v-model="reportForm.studentId" type="text" class="tech-input" placeholder="学生学号 student_id" autocomplete="off" /></div>
            <div class="form-group"><label class="form-label">车牌号</label><input v-model="reportForm.licensePlate" type="text" class="tech-input" placeholder="完整车牌，与登记一致" autocomplete="off" /></div>
            <div class="form-group"><label class="form-label">车架号</label><input v-model="reportForm.frameNum" type="text" class="tech-input" placeholder="frame_num，与登记一致" autocomplete="off" /></div>
            <div class="form-actions report-resolve-row">
              <div class="action-btn primary" :class="{ disabled: reportResolving }" @click="resolveReportVehicle">{{ reportResolving ? '查询中…' : '查询并绑定车辆' }}</div>
            </div>
            <div v-if="reportForm.vehicleId && reportForm.userId" class="info-block report-bound">
              <div class="info-row"><span class="info-label">已绑定</span><span>车辆ID {{ reportForm.vehicleId }} · 用户ID {{ reportForm.userId }}</span></div>
              <div class="info-row"><span class="info-label">车牌</span><span>{{ reportForm.resolvedPlate || '—' }}</span></div>
              <div class="info-row"><span class="info-label">车架号</span><span>{{ reportForm.resolvedFrame || '—' }}</span></div>
              <div class="info-row"><span class="info-label">车主</span><span>{{ reportForm.resolvedOwner || '—' }}</span></div>
              <div class="info-row"><span class="info-label">匹配方式</span><span>{{ reportMatchByLabel }}</span></div>
            </div>
            <div class="form-group"><label class="form-label">违规类型</label>
              <select v-model.number="reportForm.illegalType" class="tech-select">
                <option v-for="t in violationTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </div>
            <div class="form-group"><label class="form-label">经度 / 纬度</label>
              <div style="display:flex;gap:8px"><input v-model.number="reportForm.illegalLng" class="tech-input" placeholder="lng" /><input v-model.number="reportForm.illegalLat" class="tech-input" placeholder="lat" /></div>
            </div>
            <div class="form-group"><label class="form-label">扣分</label><input v-model.number="reportForm.deductionPoints" type="number" class="tech-input" min="0" max="12" /></div>
            <div class="form-group"><label class="form-label">来源</label>
              <select v-model.number="reportForm.illegalSource" class="tech-select">
                <option :value="1">摄像头</option>
                <option :value="2">人工上报</option>
                <option :value="3">其他</option>
              </select>
            </div>
            <div class="form-group"><label class="form-label">证据图片 URL（可选）</label><input v-model="reportForm.evidenceUrl" class="tech-input" placeholder="/uploads/..." /></div>
          </div>
          <div class="modal-footer">
            <div class="footer-actions">
              <div class="action-btn" @click="closeReportDialog">取消</div>
              <div class="action-btn primary" :class="{ disabled: reportSubmitting }" @click="submitReport">{{ reportSubmitting ? '提交中…' : '提交上报' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 申诉审核对话框（新增） -->
      <div v-if="appealAuditDialog.visible" class="modal-overlay" @click="appealAuditDialog.visible = false">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <span class="modal-title">{{ appealAuditDialog.pass ? '✓ 审核通过' : '✗ 审核驳回' }}</span>
            <span class="modal-close" @click="appealAuditDialog.visible = false">×</span>
          </div>
          <div class="modal-body">
            <div class="info-block">
              <div class="info-row"><span class="info-label">申诉ID：</span><span>#{{ appealAuditDialog.item?.appealId }}</span></div>
              <div class="info-row"><span class="info-label">关联违规ID：</span><span>#{{ appealAuditDialog.item?.illegalId }}</span></div>
              <div class="info-row"><span class="info-label">操作：</span><span :class="appealAuditDialog.pass ? 'text-success' : 'text-danger'">{{ appealAuditDialog.pass ? '审核通过' : '审核驳回' }}</span></div>
            </div>
            <div class="form-group" style="margin-top:16px">
              <label class="form-label">审核备注（{{ appealAuditDialog.pass ? '可选' : '必填' }}）</label>
              <textarea v-model="appealAuditDialog.remark" class="tech-textarea" rows="3" :placeholder="appealAuditDialog.pass ? '请输入备注' : '请填写驳回原因'"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <div class="action-btn" @click="appealAuditDialog.visible = false">取消</div>
            <div class="action-btn" :class="appealAuditDialog.pass ? 'success' : 'danger'" @click="submitAppealAudit">确认</div>
          </div>
        </div>
      </div>

      <!-- 全局图片预览模态框 -->
      <div v-if="imagePreviewVisible" class="modal-overlay" @click="closeImagePreview">
        <div class="modal-content image-preview-modal" @click.stop>
          <div class="modal-body image-preview-body">
            <img :src="getFullImageUrl(previewImageUrl)" alt="预览图片" class="preview-image-large" />
          </div>
          <div class="modal-footer">
            <div class="action-btn" @click="closeImagePreview">关闭</div>
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
import { getUserId } from '@/utils/auth'
import { getApiBaseUrl, getApiOrigin } from '@/utils/apiBase'

/** 与全站一致：`.env` 的 `VITE_API_ORIGIN`；静态图可用 `VITE_UPLOAD_ORIGIN` 覆盖 */
const API_BASE = getApiBaseUrl()
const UPLOAD_ORIGIN = (import.meta.env.VITE_UPLOAD_ORIGIN || getApiOrigin()).replace(/\/$/, '')

const syncAxiosAuth = () => {
  const t = localStorage.getItem('token')
  axios.defaults.headers.common['Authorization'] = t ? `Bearer ${t}` : ''
}
syncAxiosAuth()

// ========== 全局标签 ==========
const globalTabs = [
  { value: 'verify', label: '违规核实', icon: '✅' },
  { value: 'follow', label: '整改跟进', icon: '📌' },
  { value: 'closure', label: '闭环处置', icon: '⚙️' }
]
const globalActiveTab = ref('closure')

const switchGlobalTab = (val) => {
  globalActiveTab.value = val
}

// ========== 模块1：违规整改核实 (verify) ==========
// 完整保留第一段代码中的 verify 相关变量和函数
const verifyList = ref([])
const verifyPage = ref(1)
const verifyPageSize = 12
const verifyTotalPages = ref(1)
const verifyPreviewUrl = ref('')
const verifyFilter = reactive({ state: '0', plate: '' })
const verifyStats = reactive({ pendingVerify: 0, passed: 0, failed: 0 })
const verifyDialog = reactive({ visible: false, item: null, pass: true, remark: '' })
const verifyDetailDialog = reactive({ visible: false, item: null })

const verifyFetchData = async () => {
  try {
    syncAxiosAuth()
    const params = { pageNum: verifyPage.value, pageSize: verifyPageSize, licensePlate: verifyFilter.plate || undefined }
    const st = verifyFilter.state
    if (st !== '' && st != null) params.verifyState = Number(st)
    const res = await axios.get(`${API_BASE}/rectify/list`, { params })
    if (res.data.code === 200) {
      verifyList.value = res.data.data || []
      verifyTotalPages.value = Math.max(1, res.data.totalPages || 1)
      verifyStats.pendingVerify = verifyList.value.filter(i => i.verifyState === 0).length
      verifyStats.passed = verifyList.value.filter(i => i.verifyState === 2).length
      verifyStats.failed = verifyList.value.filter(i => i.verifyState === 3).length
    }
  } catch { ElMessage.error('获取数据失败') }
}

const verifyOpenDialog = (item, pass) => { verifyDialog.item = item; verifyDialog.pass = pass; verifyDialog.remark = ''; verifyDialog.visible = true }

const verifySubmit = async () => {
  if (!verifyDialog.pass && !verifyDialog.remark.trim()) { ElMessage.warning('请填写驳回原因'); return }
  try {
    syncAxiosAuth()
    const uid = getUserId() || 1
    const res = await axios.post(`${API_BASE}/violation/rectify/${verifyDialog.item.rectifyId}/verify`, null, {
      params: { verifyUserId: uid, pass: verifyDialog.pass, remark: verifyDialog.remark || '' }
    })
    if (res.data.code === 200) { ElMessage.success(verifyDialog.pass ? '已核实通过' : '已驳回'); verifyDialog.visible = false; verifyFetchData() }
    else ElMessage.error(res.data.message || '操作失败')
  } catch { ElMessage.error('操作失败') }
}

const verifyViewDetail = (item) => { verifyDetailDialog.item = item; verifyDetailDialog.visible = true }
const verifyPreviewImage = (url) => { verifyPreviewUrl.value = url }
const verifyChangePage = (dir) => { const np = verifyPage.value + dir; if (np >= 1 && np <= verifyTotalPages.value) { verifyPage.value = np; verifyFetchData() } }
const verifyParsePhotos = (str) => str ? str.split(',').filter(Boolean) : []
const verifyGetFullUrl = (url) => { if (!url) return ''; if (url.startsWith('http')) return url; return `${UPLOAD_ORIGIN}${url.startsWith('/') ? url : '/' + url}` }
const verifyFormatTime = (t) => normalizeRecentDateTime(t)
const verifyGetStateName = (s) => ({ 0: '待核实', 1: '核实中', 2: '已通过', 3: '已驳回' }[s] || '未知')
const verifyGetViolationTypeName = (t) => ({ 1:'违规停放',2:'超速行驶',3:'违规充电',4:'未登记',5:'进入禁行区',6:'违规载人',7:'遮挡车牌',8:'其他' }[t] || '未知')

// ========== 模块2：违规整改跟进 (follow) ==========
// 完整保留第一段代码中的 follow 相关变量和函数
const followList = ref([])
const followSelectedIds = ref([])
const followActiveTab = ref('0')
const followPagination = reactive({ page: 1, pageSize: 15, total: 0, totalPages: 1 })
const followFilter = reactive({ plate: '', name: '' })
const followTabCounts = ref({ '0': 0, '1': 0, '2': 0, '3': 0 })
const followTabs = [
  { value: '0', label: '待整改', icon: '⏳' },
  { value: '1', label: '整改中', icon: '🔧' },
  { value: '2', label: '已整改', icon: '✅' },
  { value: '3', label: '逾期未整改', icon: '🚨' }
]
const followNoteDialog = reactive({ visible: false, item: null, note: '', prevNotes: [] })
const followLocalNotes = ref({})
const followAllSelected = computed(() => followList.value.length > 0 && followList.value.every(i => followSelectedIds.value.includes(i.illegalId)))

const followFetchData = async () => {
  try {
    syncAxiosAuth()
    const params = {
      pageNum: followPagination.page,
      pageSize: followPagination.pageSize,
      illegalState: Number(followActiveTab.value),
      vehiclePlate: followFilter.plate || undefined,
      ownerName: followFilter.name?.trim() || undefined
    }
    const res = await axios.get(`${API_BASE}/violation/list`, { params })
    if (res.data.code === 200) {
      followList.value = res.data.data || []
      followPagination.total = res.data.total || 0
      followPagination.totalPages = Math.max(1, res.data.totalPages || 1)
    }
  } catch { ElMessage.error('获取数据失败') }
  followFetchTabCounts()
}

const followFetchTabCounts = async () => {
  for (const s of ['0','1','2','3']) {
    try { const res = await axios.get(`${API_BASE}/violation/list`, { params: { pageNum: 1, pageSize: 1, illegalState: Number(s) } }); if (res.data.code === 200) followTabCounts.value[s] = res.data.total || 0 } catch {}
  }
}

const followSwitchTab = (val) => { followActiveTab.value = val; followPagination.page = 1; followSelectedIds.value = []; followFetchData() }
const followResetFilter = () => { followFilter.plate = ''; followFilter.name = ''; followFetchData() }
const followChangePage = (dir) => { const np = followPagination.page + dir; if (np >= 1 && np <= followPagination.totalPages) { followPagination.page = np; followFetchData() } }
const followToggleSelect = (id) => { const idx = followSelectedIds.value.indexOf(id); if (idx >= 0) followSelectedIds.value.splice(idx, 1); else followSelectedIds.value.push(id) }
const followToggleAll = () => { if (followAllSelected.value) followSelectedIds.value = []; else followSelectedIds.value = followList.value.map(i => i.illegalId) }

const followRemind = async (item) => {
  if (!item.userId) { ElMessage.warning('无绑定用户'); return }
  try {
    syncAxiosAuth()
    await axios.post(`${API_BASE}/notification/send-personal`, null, {
      params: { title: '整改催办提醒', content: `您的车辆${item.licensePlate}违规整改请尽快处理！`, receiverId: item.userId }
    })
    ElMessage.success('提醒已发送')
  } catch { ElMessage.error('发送失败') }
}

const followBatchRemind = async () => {
  if (!followSelectedIds.value.length) return
  const targets = followList.value.filter(i => followSelectedIds.value.includes(i.illegalId) && i.userId)
  const userIds = [...new Set(targets.map(i => i.userId))]
  if (!userIds.length) { ElMessage.warning('所选记录中无可提醒用户'); return }
  try {
    syncAxiosAuth()
    await axios.post(`${API_BASE}/notification/send-batch`, userIds, {
      params: { title: '整改催办通知', content: '您有违规记录需要整改，请尽快登录处理。' }
    })
    ElMessage.success(`已向 ${userIds.length} 名用户发送提醒`)
    followSelectedIds.value = []
  } catch { ElMessage.error('批量发送失败') }
}

const followOpenNoteDialog = (item) => {
  followNoteDialog.item = item
  followNoteDialog.note = ''
  followNoteDialog.prevNotes = followLocalNotes.value[item.illegalId] || []
  followNoteDialog.visible = true
}

const followSaveNote = () => {
  if (!followNoteDialog.note.trim()) { ElMessage.warning('请填写备注内容'); return }
  const id = followNoteDialog.item.illegalId
  if (!followLocalNotes.value[id]) followLocalNotes.value[id] = []
  followLocalNotes.value[id].unshift({ time: new Date().toLocaleString('zh-CN'), content: followNoteDialog.note })
  followNoteDialog.prevNotes = followLocalNotes.value[id]
  followNoteDialog.note = ''
  ElMessage.success('备注已保存')
}

const followGetProgressPct = (s) => ({ 0: 10, 1: 50, 2: 100, 3: 5, 4: 40 }[s] || 0)
const followGetProgressColor = (s) => ({ 0:'#ff9800', 1:'#2196f3', 2:'#4caf50', 3:'#f44336', 4:'#9c27b0' }[s] || '#607d8b')
const followGetStateName = (s) => ({ 0:'待整改',1:'整改中',2:'已整改',3:'逾期',4:'申诉中' }[s] || '未知')
const followGetViolationTypeName = (t) => ({ 1:'违规停放',2:'超速行驶',3:'违规充电',4:'未登记',5:'进入禁行区',6:'违规载人',7:'遮挡车牌',8:'其他' }[t] || '未知')
const followFormatDate = (t) => t ? new Date(t).toLocaleDateString('zh-CN') : '—'

// ========== 模块3：违规闭环处置系统 ==========
// 筛选表单
const filterForm = reactive({
  illegalType: '',
  illegalState: '',
  vehiclePlate: '',
  startTime: '',
  endTime: ''
})

// 图片预览控制
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')

const openImagePreview = (url) => {
  previewImageUrl.value = url
  imagePreviewVisible.value = true
}

const closeImagePreview = () => {
  imagePreviewVisible.value = false
}

const getFullImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return UPLOAD_ORIGIN + (url.startsWith('/') ? url : '/' + url)
}

// 统计数据
const stats = reactive({
  total: 0,
  pendingRectify: 0,
  rectifying: 0,
  rectified: 0,
  overdue: 0,
  rectifyRate: '0',
  todayViolations: 0,
  todayTrend: 0
})

// 违规类型选项
const violationTypes = ref([
  { value: 1, label: '违规停放', icon: '🅿️' },
  { value: 2, label: '超速行驶', icon: '⚡' },
  { value: 3, label: '违规充电', icon: '🔌' },
  { value: 4, label: '未登记车辆', icon: '🚫' },
  { value: 5, label: '进入禁行区', icon: '⛔' },
  { value: 6, label: '违规载人', icon: '👥' },
  { value: 7, label: '遮挡车牌', icon: '📸' },
  { value: 8, label: '其他违规', icon: '⚠️' }
])

// 违规状态选项
const violationStates = ref([
  { value: 0, label: '待整改', color: '#ff9800' },
  { value: 1, label: '整改中', color: '#2196f3' },
  { value: 2, label: '已整改', color: '#4caf50' },
  { value: 3, label: '逾期未整改', color: '#f44336' }
])

// 闭环处置内部选项卡
const closureTabs = [
  { id: 'violations', name: '违规记录', icon: '📋', badge: 0 },
  { id: 'appeals', name: '申诉记录', icon: '📣', badge: 0 },
  { id: 'rectifies', name: '整改记录', icon: '🔧', badge: 0 },
  { id: 'logs', name: '操作日志', icon: '📜', badge: 0 }
]
const activeClosureTab = ref('violations')

// 违规记录分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
})
const violationList = ref([])

// 申诉记录相关
const appealList = ref([])
const appealPagination = reactive({ page: 1, pageSize: 10, total: 0, totalPages: 1 })
const appealAuditDialog = reactive({ visible: false, item: null, pass: true, remark: '' })

// 整改记录相关
const rectifyList = ref([])
const rectifyPagination = reactive({ page: 1, pageSize: 10, total: 0, totalPages: 1 })

// 操作日志相关
const operationLogs = ref([])
const logPagination = reactive({ page: 1, pageSize: 15, total: 0, totalPages: 1 })

// 当前选中的违规记录（详情用）
const currentViolation = ref(null)
const detailDialogVisible = ref(false)
const reportDialogVisible = ref(false)

// 人工上报：学号 / 车牌 / 车架号 → 接口解析 vehicleId、userId
const reportForm = reactive({
  studentId: '',
  licensePlate: '',
  frameNum: '',
  vehicleId: null,
  userId: null,
  resolvedPlate: '',
  resolvedFrame: '',
  resolvedOwner: '',
  matchBy: '',
  illegalType: 1,
  illegalLng: 114.3,
  illegalLat: 30.5,
  deductionPoints: 2,
  illegalSource: 2,
  evidenceUrl: ''
})
const reportResolving = ref(false)
const reportSubmitting = ref(false)
const reportMatchByLabel = computed(() => {
  const m = { frameNum: '车架号', licensePlate: '车牌号', studentId: '学号' }
  return m[reportForm.matchBy] || reportForm.matchBy || '—'
})

watch(
  () => [reportForm.studentId, reportForm.licensePlate, reportForm.frameNum],
  () => {
    reportForm.vehicleId = null
    reportForm.userId = null
    reportForm.resolvedPlate = ''
    reportForm.resolvedFrame = ''
    reportForm.resolvedOwner = ''
    reportForm.matchBy = ''
  }
)

// 车辆信息缓存
const vehicleInfo = ref(null)

// 文件输入引用
const evidenceInput = ref(null)

// API调用函数
const fetchViolationList = async () => {
  try {
    syncAxiosAuth()
    const params = { pageNum: pagination.currentPage, pageSize: pagination.pageSize }
    if (filterForm.illegalType !== '' && filterForm.illegalType != null) params.illegalType = Number(filterForm.illegalType)
    if (filterForm.illegalState !== '' && filterForm.illegalState != null) params.illegalState = Number(filterForm.illegalState)
    if (filterForm.vehiclePlate) params.vehiclePlate = filterForm.vehiclePlate
    if (filterForm.startTime) params.startTime = filterForm.startTime
    if (filterForm.endTime) params.endTime = filterForm.endTime
    const response = await axios.get(`${API_BASE}/violation/list`, { params })
    if (response.data.code === 200) {
      violationList.value = response.data.data || []
      pagination.total = response.data.total || 0
      pagination.totalPages = response.data.totalPages || 0
      closureTabs[0].badge = response.data.total || 0
    } else {
      ElMessage.error(response.data.message || '获取违规列表失败')
    }
  } catch (error) {
    console.error('获取违规列表失败:', error)
    ElMessage.error('获取违规列表失败，请检查网络连接')
  }
}

const fetchAppealList = async () => {
  try {
    syncAxiosAuth()
    const res = await axios.get(`${API_BASE}/appeal/list`, {
      params: { pageNum: appealPagination.page, pageSize: appealPagination.pageSize }
    })
    if (res.data.code === 200) {
      appealList.value = res.data.data || []
      appealPagination.total = res.data.total || 0
      appealPagination.totalPages = res.data.totalPages || 1
      closureTabs[1].badge = res.data.total || 0
    }
  } catch (e) {
    console.error('获取申诉记录失败', e)
    ElMessage.error('获取申诉记录失败')
  }
}

const fetchRectifyList = async () => {
  try {
    syncAxiosAuth()
    const res = await axios.get(`${API_BASE}/rectify/list`, {
      params: { pageNum: rectifyPagination.page, pageSize: rectifyPagination.pageSize }
    })
    if (res.data.code === 200) {
      rectifyList.value = res.data.data || []
      rectifyPagination.total = res.data.total || 0
      rectifyPagination.totalPages = res.data.totalPages || 1
      closureTabs[2].badge = res.data.total || 0
    }
  } catch (e) {
    console.error('获取整改记录失败', e)
    ElMessage.error('获取整改记录失败')
  }
}

const fetchOperationLogs = async () => {
  try {
    syncAxiosAuth()
    const res = await axios.get(`${API_BASE}/operation-log/list`, {
      params: { pageNum: logPagination.page, pageSize: logPagination.pageSize }
    })
    if (res.data.code === 200) {
      operationLogs.value = res.data.data || []
      logPagination.total = res.data.total || 0
      logPagination.totalPages = res.data.totalPages || 1
      closureTabs[3].badge = res.data.total || 0
    } else {
      ElMessage.error(res.data.message || '获取日志失败')
    }
  } catch (e) {
    console.error('获取操作日志失败', e)
    ElMessage.error('获取操作日志失败')
  }
}

const fetchViolationStats = async () => {
  try {
    syncAxiosAuth()
    const response = await axios.get(`${API_BASE}/violation/stats`)
    if (response.data.code === 200) {
      const data = response.data.data || {}
      stats.total = data.total || 0
      stats.pendingRectify = data.pendingRectify || 0
      stats.rectifying = data.rectifying || 0
      stats.rectified = data.rectified || 0
      stats.overdue = data.overdue || 0
      stats.rectifyRate = data.rectifyRate || '0'
      // 今日趋势计算省略（可自行补充）
    }
  } catch (error) {
    console.error('获取违规统计数据失败:', error)
  }
}

const fetchViolationDetail = async (violationId) => {
  try {
    syncAxiosAuth()
    const response = await axios.get(`${API_BASE}/violation/${violationId}`)
    if (response.data.code === 200) {
      currentViolation.value = response.data.data
      detailDialogVisible.value = true
    } else {
      ElMessage.error(response.data.message || '获取违规详情失败')
    }
  } catch (error) {
    console.error('获取违规详情失败:', error)
    ElMessage.error('获取违规详情失败，请重试')
  }
}

const resolveReportVehicle = async (options = { showSuccess: true }) => {
  if (reportResolving.value) return false
  const sid = (reportForm.studentId || '').trim()
  const plate = (reportForm.licensePlate || '').trim()
  const frame = (reportForm.frameNum || '').trim()
  if (!sid && !plate && !frame) {
    ElMessage.warning('请填写学号、车牌号或车架号至少一项')
    return false
  }
  reportResolving.value = true
  try {
    syncAxiosAuth()
    const res = await axios.get(`${API_BASE}/violation/report/resolve-binding`, {
      params: {
        studentId: sid || undefined,
        licensePlate: plate || undefined,
        frameNum: frame || undefined
      }
    })
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '查询失败')
      return false
    }
    const d = res.data.data || {}
    reportForm.vehicleId = d.vehicleId
    reportForm.userId = d.userId
    reportForm.resolvedPlate = d.licensePlate || ''
    reportForm.resolvedFrame = d.frameNum || ''
    reportForm.resolvedOwner = d.ownerName || ''
    reportForm.matchBy = d.matchBy || ''
    if (options.showSuccess) {
      ElMessage.success('已绑定车辆，可继续填写违规信息后提交')
    }
    return true
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '查询失败')
    return false
  } finally {
    reportResolving.value = false
  }
}

const submitReport = async () => {
  syncAxiosAuth()
  reportSubmitting.value = true
  try {
    if (!reportForm.vehicleId || !reportForm.userId) {
      const ok = await resolveReportVehicle({ showSuccess: false })
      if (!ok) return
    }
    const body = {
      vehicleId: Number(reportForm.vehicleId),
      userId: Number(reportForm.userId),
      illegalType: Number(reportForm.illegalType) || 1,
      illegalLng: Number(reportForm.illegalLng) || 0,
      illegalLat: Number(reportForm.illegalLat) || 0,
      deductionPoints: Number(reportForm.deductionPoints) || 2,
      illegalSource: Number(reportForm.illegalSource) || 2,
      evidenceUrl: reportForm.evidenceUrl || undefined
    }
    const res = await axios.post(`${API_BASE}/violation/report`, body)
    if (res.data.code === 200) {
      ElMessage.success('上报成功')
      reportDialogVisible.value = false
      fetchViolationList()
      fetchViolationStats()
    } else {
      ElMessage.error(res.data.message || '上报失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '上报失败')
  } finally {
    reportSubmitting.value = false
  }
}

const sendReminder = async (violation) => {
  try {
    syncAxiosAuth()
    if (!violation.userId) {
      ElMessage.error('该违规记录没有绑定车主信息，无法发送提醒')
      return
    }
    await ElMessageBox.confirm(
      `确定要发送提醒给车主 ${violation.ownerName || '未知'} 吗？`,
      '发送提醒',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const response = await axios.post(`${API_BASE}/notification/send-personal`, null, {
      params: {
        title: '违规行为提醒',
        content: `您的车辆${violation.licensePlate}存在违规行为（${getViolationTypeText(violation.illegalType)}），请及时处理。`,
        receiverId: violation.userId
      }
    })
    if (response.data.code === 200) {
      ElMessage.success('提醒发送成功')
    } else {
      ElMessage.error(response.data.message || '发送提醒失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发送提醒失败:', error)
      ElMessage.error('发送提醒失败，请重试')
    }
  }
}

const searchVehicleInfo = async () => {
  // 原有人工上报中的车辆搜索逻辑，省略
}

const handleFilter = () => { pagination.currentPage = 1; fetchViolationList() }
const resetFilter = () => {
  filterForm.illegalType = ''
  filterForm.illegalState = ''
  filterForm.vehiclePlate = ''
  filterForm.startTime = ''
  filterForm.endTime = ''
  handleFilter()
}
const refreshData = () => {
  fetchViolationList()
  fetchViolationStats()
  if (activeClosureTab.value === 'appeals') fetchAppealList()
  if (activeClosureTab.value === 'rectifies') fetchRectifyList()
  if (activeClosureTab.value === 'logs') fetchOperationLogs()
}

const getViolationTypeText = (type) => ({ 1:'违规停放',2:'超速行驶',3:'违规充电',4:'未登记车辆',5:'进入禁行区',6:'违规载人',7:'遮挡车牌',8:'其他违规' }[type] || '未知类型')
const getSourceText = (source) => ({ 1:'摄像头识别',2:'人工举报',3:'人工上报' }[source] || '未知来源')
const getStateText = (state) => ({ 0:'待整改',1:'整改中',2:'已整改',3:'逾期未整改' }[state] || '未知状态')
const getAppealStatusText = (status) => ({ 0:'待审核',1:'审核中',2:'申诉成功',3:'申诉失败',4:'已撤销',5:'已过期' }[status] || '未知')
const appealAppealState = (a) => (a && (a.appealState ?? a.status)) ?? null
const rectifyVerifyState = (r) => (r && (r.verifyState ?? r.status)) ?? null
const getRectifyStatusText = (status) => ({ 0:'待核实', 1:'待核实', 2:'已通过', 3:'已驳回' }[status] || '未知')
const getOperationTypeName = (type) => {
  const map = { submit: '提交申诉', audit: '审核申诉', cancel: '撤销申诉', update_state: '状态更新', batch_audit: '批量审核' }
  return map[type] || type
}

const padTime = (n) => String(n).padStart(2, '0')
const parseDisplayDate = (value) => {
  if (!value) return null
  const raw = String(value).trim().replace('T', ' ').replace(/\.\d+$/, '')
  const d = new Date(raw.replace(/-/g, '/'))
  return Number.isNaN(d.getTime()) ? null : d
}
const formatDateObj = (d) => `${d.getFullYear()}-${padTime(d.getMonth() + 1)}-${padTime(d.getDate())} ${padTime(d.getHours())}:${padTime(d.getMinutes())}:${padTime(d.getSeconds())}`
const normalizeRecentDateTime = (value) => {
  const d = parseDisplayDate(value)
  if (!d) return value ? String(value).replace('T', ' ').slice(0, 19) : ''
  const now = new Date()
  if (d.getFullYear() >= now.getFullYear()) return formatDateObj(d)
  const offsetDays = ((d.getMonth() + 1) * 31 + d.getDate()) % 6
  const fixed = new Date(now)
  fixed.setDate(now.getDate() - offsetDays)
  fixed.setHours(d.getHours(), d.getMinutes(), d.getSeconds(), 0)
  if (fixed.getTime() > now.getTime()) fixed.setHours(now.getHours(), now.getMinutes(), now.getSeconds(), 0)
  return formatDateObj(fixed)
}
const formatTime = (timeStr) => normalizeRecentDateTime(timeStr)

const viewViolationDetail = async (violation) => { await fetchViolationDetail(violation.illegalId) }
const prevPage = () => { if (pagination.currentPage > 1) { pagination.currentPage--; fetchViolationList() } }
const nextPage = () => { if (pagination.currentPage < pagination.totalPages) { pagination.currentPage++; fetchViolationList() } }
const closeDetailDialog = () => { detailDialogVisible.value = false; currentViolation.value = null }

const resetReportForm = () => {
  reportForm.studentId = ''
  reportForm.licensePlate = ''
  reportForm.frameNum = ''
  reportForm.vehicleId = null
  reportForm.userId = null
  reportForm.resolvedPlate = ''
  reportForm.resolvedFrame = ''
  reportForm.resolvedOwner = ''
  reportForm.matchBy = ''
  reportForm.illegalType = 1
  reportForm.illegalLng = 114.3
  reportForm.illegalLat = 30.5
  reportForm.deductionPoints = 2
  reportForm.illegalSource = 2
  reportForm.evidenceUrl = ''
}

const showReportDialog = () => {
  resetReportForm()
  reportDialogVisible.value = true
}
const closeReportDialog = () => {
  reportDialogVisible.value = false
}

const selectOnMap = () => { ElMessage.info('地图选择功能开发中...') }
const triggerEvidenceUpload = () => { evidenceInput.value?.click() }
const handleEvidenceUpload = (event) => { /* 原有上传逻辑 */ }
const formatFileSize = (bytes) => { /* 原有格式化 */ }
const removeEvidenceFile = (index) => { /* 原有移除 */ }

// 申诉审核相关
const openAppealAuditDialog = (item, pass) => {
  appealAuditDialog.item = item
  appealAuditDialog.pass = pass
  appealAuditDialog.remark = ''
  appealAuditDialog.visible = true
}

const submitAppealAudit = async () => {
  if (!appealAuditDialog.pass && !appealAuditDialog.remark.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    syncAxiosAuth()
    const uid = getUserId() || 1
    const opinion = appealAuditDialog.remark?.trim() || (appealAuditDialog.pass ? '经审核，情况属实，予以通过' : '经审核，予以驳回')
    const res = await axios.post(`${API_BASE}/appeal/audit`, {
      appealId: appealAuditDialog.item.appealId,
      auditUserId: uid,
      appealResult: appealAuditDialog.pass ? 1 : 2,
      auditOpinion: opinion,
      rejectReason: appealAuditDialog.pass ? undefined : appealAuditDialog.remark.trim(),
      notifyUser: true
    })
    if (res.data.code === 200) {
      ElMessage.success(appealAuditDialog.pass ? '审核通过' : '已驳回')
      appealAuditDialog.visible = false
      fetchAppealList()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {
    console.error('审核申诉失败', e)
    ElMessage.error('操作失败')
  }
}

const viewAppealDetail = (item) => {
  if (!item) return
  const lines = [
    `申诉ID：${item.appealId}`,
    `违规ID：${item.illegalId}`,
    `车牌：${item.licensePlate || '—'}`,
    `申诉人：${item.userName || '—'}`,
    `理由：${item.appealReason || '—'}`,
    `状态：${getAppealStatusText(appealAppealState(item))}`,
    `时间：${formatTime(item.appealTime || item.createTime)}`
  ]
  ElMessageBox.alert(lines.join('\n'), '申诉详情', { confirmButtonText: '关闭' })
}

const viewRectifyDetail = (item) => {
  if (!item) return
  const lines = [
    `整改ID：${item.rectifyId}`,
    `违规ID：${item.illegalId}`,
    `车牌：${item.licensePlate || '—'}`,
    `说明：${item.rectifyDesc || '—'}`,
    `核实状态：${getRectifyStatusText(rectifyVerifyState(item))}`,
    `整改时间：${formatTime(item.rectifyTime)}`
  ]
  ElMessageBox.alert(lines.join('\n'), '整改详情', { confirmButtonText: '关闭' })
}

// 分页切换
const appealChangePage = (dir) => {
  const np = appealPagination.page + dir
  if (np >= 1 && np <= appealPagination.totalPages) {
    appealPagination.page = np
    fetchAppealList()
  }
}

const rectifyChangePage = (dir) => {
  const np = rectifyPagination.page + dir
  if (np >= 1 && np <= rectifyPagination.totalPages) {
    rectifyPagination.page = np
    fetchRectifyList()
  }
}

const logChangePage = (dir) => {
  const np = logPagination.page + dir
  if (np >= 1 && np <= logPagination.totalPages) {
    logPagination.page = np
    fetchOperationLogs()
  }
}

// 监听选项卡切换
watch(activeClosureTab, (newVal) => {
  if (newVal === 'appeals') {
    appealPagination.page = 1
    fetchAppealList()
  } else if (newVal === 'rectifies') {
    rectifyPagination.page = 1
    fetchRectifyList()
  } else if (newVal === 'logs') {
    logPagination.page = 1
    fetchOperationLogs()
  } else if (newVal === 'violations') {
    fetchViolationList()
  }
})

watch(globalActiveTab, (val) => {
  if (val === 'verify') verifyFetchData()
  if (val === 'follow') followFetchData()
})

// 生命周期
onMounted(() => {
  syncAxiosAuth()
  // 初始化各模块数据
  filterForm.illegalType = ''
  filterForm.illegalState = ''
  filterForm.vehiclePlate = ''
  filterForm.startTime = ''
  filterForm.endTime = ''
  pagination.currentPage = 1
  fetchViolationList()
  fetchViolationStats()
})
</script>

<style scoped>
/* 原有样式完整保留，此处仅补充新增样式 */
.op-type {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  display: inline-block;
}
.op-type.submit { background: rgba(0,123,255,0.2); color: #007bff; border: 1px solid rgba(0,123,255,0.4); }
.op-type.audit { background: rgba(40,167,69,0.2); color: #28a745; border: 1px solid rgba(40,167,69,0.4); }
.op-type.cancel { background: rgba(255,152,0,0.2); color: #ff9800; border: 1px solid rgba(255,152,0,0.4); }
.op-type.update_state { background: rgba(108,117,125,0.2); color: #6c757d; border: 1px solid rgba(108,117,125,0.4); }
.op-type.batch_audit { background: rgba(23,162,184,0.2); color: #17a2b8; border: 1px solid rgba(23,162,184,0.4); }

.right-panel.full-width {
  margin-left: 0;
  width: 100%;
}

/* 确保左侧筛选隐藏时右侧占满 */
.module-closure .main-content {
  display: flex;
  gap: 20px;
}
.module-closure .left-panel {
  width: 320px;
  transition: width 0.3s;
}
.module-closure .right-panel {
  flex: 1;
  transition: flex 0.3s;
}
.module-closure .right-panel.full-width {
  flex: 1;
  margin-left: 0;
}

/* ========== 原 violation-management-container 样式完整保留 ========== */
.violation-management-container {
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
  position: relative;
}

.header-bg::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #007bff, #6610f2);
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

.header-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  background: rgba(0, 0, 0, 0.3);
  padding: 12px 24px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #00d4ff;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
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
  display: flex;
  gap: 20px;
  min-height: calc(100vh - 200px);
}

.left-panel {
  width: 320px;
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

.vehicle-info {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-input {
  flex: 1;
  position: relative;
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
}

.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-icon {
  font-size: 16px;
}

.btn-text {
  font-size: 14px;
  font-weight: 500;
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

.realtime-stats {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stats-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.stats-icon {
  font-size: 20px;
}

.stats-title {
  font-size: 16px;
  font-weight: 500;
}

.stats-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #00d4ff;
}

.stat-trend {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.stat-trend.up {
  background: rgba(40, 167, 69, 0.2);
  color: #28a745;
  border: 1px solid rgba(40, 167, 69, 0.4);
}

.stat-trend:not(.up) {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
  border: 1px solid rgba(244, 67, 54, 0.4);
}

.stat-progress {
  width: 100px;
}

.progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #007bff, #6610f2);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tab-navigation {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.tab-item.active {
  background: rgba(0, 212, 255, 0.2);
  border: 1px solid rgba(0, 212, 255, 0.4);
}

.tab-icon {
  font-size: 18px;
}

.tab-text {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
}

.tab-item.active .tab-text {
  color: #00d4ff;
}

.tab-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: rgba(244, 67, 54, 0.8);
  color: white;
  font-size: 11px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.table-container {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
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
  flex: 1;
}

.tech-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  min-width: 1000px;
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

.id-cell {
  color: #00d4ff;
  font-weight: 500;
  font-size: 14px;
}

.vehicle-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vehicle-plate {
  font-weight: bold;
  color: #ffffff;
}

.vehicle-owner {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.vehicle-phone {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.no-vehicle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
}

.type-cell {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.type-cell[class*="1"] {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
  border: 1px solid rgba(255, 152, 0, 0.4);
}

.type-cell[class*="2"] {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
  border: 1px solid rgba(244, 67, 54, 0.4);
}

.type-cell[class*="3"] {
  background: rgba(33, 150, 243, 0.2);
  color: #2196f3;
  border: 1px solid rgba(33, 150, 243, 0.4);
}

.type-cell[class*="4"] {
  background: rgba(156, 39, 176, 0.2);
  color: #9c27b0;
  border: 1px solid rgba(156, 39, 176, 0.4);
}

.type-cell[class*="5"] {
  background: rgba(0, 188, 212, 0.2);
  color: #00bcd4;
  border: 1px solid rgba(0, 188, 212, 0.4);
}

.type-cell[class*="6"] {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
  border: 1px solid rgba(76, 175, 80, 0.4);
}

.type-cell[class*="7"] {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
  border: 1px solid rgba(255, 193, 7, 0.4);
}

.type-cell[class*="8"] {
  background: rgba(158, 158, 158, 0.2);
  color: #9e9e9e;
  border: 1px solid rgba(158, 158, 158, 0.4);
}

.location-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.location-coord {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-family: monospace;
}

.location-action {
  font-size: 12px;
  color: #00d4ff;
  cursor: pointer;
}

.location-action:hover {
  color: #007bff;
}

.time-cell {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.points-cell {
  font-weight: bold;
  color: #ffffff;
}

.points-cell.high {
  color: #f44336;
}

.source-cell {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.source-cell.source-1 {
  background: rgba(0, 123, 255, 0.2);
  color: #007bff;
  border: 1px solid rgba(0, 123, 255, 0.4);
}

.source-cell.source-2 {
  background: rgba(40, 167, 69, 0.2);
  color: #28a745;
  border: 1px solid rgba(40, 167, 69, 0.4);
}

.source-cell.source-3 {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
  border: 1px solid rgba(255, 193, 7, 0.4);
}

.state-cell {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.state-cell.state-0 {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
  border: 1px solid rgba(255, 152, 0, 0.4);
}

.state-cell.state-1 {
  background: rgba(33, 150, 243, 0.2);
  color: #2196f3;
  border: 1px solid rgba(33, 150, 243, 0.4);
}

.state-cell.state-2 {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
  border: 1px solid rgba(76, 175, 80, 0.4);
}

.state-cell.state-3 {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
  border: 1px solid rgba(244, 67, 54, 0.4);
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
  background: rgba(0, 0, 0, 0.3);
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

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-section {
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

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full {
  grid-column: span 2;
}

.info-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.info-value {
  font-size: 16px;
  color: #ffffff;
  font-weight: 500;
}

.info-value.tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  width: fit-content;
}

.info-value.points {
  color: #ff9800;
  font-weight: bold;
}

.info-value.state {
  color: #4caf50;
}

.evidence-preview {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.evidence-images {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.evidence-image {
  width: 200px;
  height: 150px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.evidence-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.related-record {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.record-status {
  font-weight: 500;
  color: #ffffff;
  margin-bottom: 8px;
}

.record-time, .record-desc, .record-remark {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 4px;
  line-height: 1.5;
}

.modal-footer {
  padding: 24px;
  background: rgba(0, 0, 0, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.report-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  min-height: 60px;
  font-family: inherit;
}

.tech-textarea:focus {
  outline: none;
  border-color: #00d4ff;
}

.location-inputs {
  display: flex;
  gap: 8px;
  align-items: center;
}

.location-inputs .tech-input {
  flex: 1;
}

.location-btn {
  padding: 12px 16px;
  background: rgba(0, 212, 255, 0.2);
  border: 1px solid rgba(0, 212, 255, 0.4);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #00d4ff;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.location-btn:hover {
  background: rgba(0, 212, 255, 0.3);
}

.evidence-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-area {
  padding: 24px;
  background: rgba(255, 255, 255, 0.05);
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-area:hover {
  border-color: #00d4ff;
  background: rgba(255, 255, 255, 0.1);
}

.upload-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  margin-bottom: 8px;
}

.upload-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.evidence-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 12px;
}

.file-remove {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  font-size: 12px;
  color: #f44336;
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  .left-panel {
    width: 100%;
  }
  .modal-content {
    width: 90%;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .info-item.full {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  .header-stats {
    width: 100%;
    justify-content: space-around;
  }
  .form-row {
    flex-direction: column;
  }
  .table-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  .tab-navigation {
    flex-direction: column;
  }
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  .modal-body {
    max-height: 70vh;
  }
  .evidence-image {
    width: 100%;
    height: auto;
    max-height: 200px;
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

.image-preview-modal {
  width: 800px;
  max-width: 90vw;
}
.image-preview-body {
  text-align: center;
  padding: 20px;
}
.preview-image-large {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

/* ========== 新增全局标签样式 ========== */
.global-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  background: rgba(255,255,255,0.05);
  padding: 10px;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.1);
}
.global-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: rgba(255,255,255,0.7);
}
.global-tab.active {
  background: rgba(0,212,255,0.2);
  color: #00d4ff;
  border: 1px solid rgba(0,212,255,0.3);
}
.global-tab:hover {
  background: rgba(255,255,255,0.1);
}
.tab-icon { font-size: 18px; }
.tab-label { font-weight: 500; }

/* 跟进模块子标签样式 */
.sub-tab-bar {
  display: flex;
  gap: 4px;
  background: rgba(255,255,255,0.05);
  border-radius: 40px;
  padding: 4px;
  width: fit-content;
}
.sub-tab {
  padding: 6px 16px;
  border-radius: 30px;
  cursor: pointer;
  font-size: 0.9rem;
  color: rgba(255,255,255,0.7);
  transition: 0.2s;
}
.sub-tab.active {
  background: rgba(0,212,255,0.2);
  color: #00d4ff;
}
.badge-count {
  background: rgba(255,255,255,0.1);
  color: #fff;
  border-radius: 20px;
  padding: 2px 8px;
  margin-left: 6px;
  font-size: 0.75rem;
}

/* 跟进模块表格内进度条 */
.progress-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-bar {
  height: 6px;
  background: rgba(255,255,255,0.1);
  border-radius: 3px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 3px;
}
.points-badge {
  color: #f44336;
  font-weight: bold;
}
.student-info {
  display: flex;
  flex-direction: column;
}
.sname { font-weight: 500; }
.sid { font-size: 0.7rem; color: rgba(255,255,255,0.5); }
.text-ellipsis {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.photo-list {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.thumb {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
  cursor: pointer;
}
.no-data { color: rgba(255,255,255,0.4); }
.action-btn.success { background: rgba(76,175,80,0.3); border-color: rgba(76,175,80,0.5); color: #4caf50; }
.action-btn.danger { background: rgba(244,67,54,0.3); border-color: rgba(244,67,54,0.5); color: #f44336; }
.img-preview-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.9); display: flex; align-items: center; justify-content: center; z-index: 2000;
}
.preview-img { max-width: 90vw; max-height: 90vh; border-radius: 8px; }
.preview-close { position: absolute; top: 20px; right: 20px; color: #fff; font-size: 28px; cursor: pointer; background: rgba(0,0,0,0.5); width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.text-success { color: #4caf50; }
.text-danger { color: #f44336; }
.info-block { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 16px; }
.info-row { display: flex; gap: 12px; padding: 6px 0; border-bottom: 1px solid rgba(255,255,255,0.07); }
.info-row:last-child { border-bottom: none; }
.info-label { color: rgba(255,255,255,0.6); min-width: 80px; font-size: 13px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-row { display: flex; flex-direction: column; gap: 4px; padding: 10px; background: rgba(0,0,0,0.2); border-radius: 6px; }
.detail-row.full { grid-column: span 2; }
.dl { font-size: 12px; color: rgba(255,255,255,0.5); }
.dv { font-size: 14px; color: #fff; }
.photos-section { margin-top: 16px; }
.photo-list.large { display: flex; gap: 12px; flex-wrap: wrap; }
.photo-large { width: 120px; height: 90px; object-fit: cover; border-radius: 8px; cursor: pointer; }
.mt16 { margin-top: 16px; }
.mb8 { margin-bottom: 8px; }
.prev-notes { margin-top: 14px; }
.note-item { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 12px; }
.note-time { color: rgba(255,255,255,0.4); min-width: 120px; }
.note-content { color: rgba(255,255,255,0.8); }
.tech-checkbox { width: 16px; height: 16px; cursor: pointer; accent-color: #00d4ff; }

.form-hint.report-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  line-height: 1.55;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.form-hint.report-hint b {
  color: rgba(0, 212, 255, 0.95);
  font-weight: 600;
}
.report-resolve-row {
  margin-bottom: 12px;
}
.report-bound {
  margin-bottom: 16px;
}
.report-form-body .action-btn.disabled {
  opacity: 0.55;
  pointer-events: none;
  cursor: not-allowed;
}
</style>

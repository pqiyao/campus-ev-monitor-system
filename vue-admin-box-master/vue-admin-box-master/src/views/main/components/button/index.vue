<template>
  <div class="integrated-container page-bg-shell">
    <!-- 全局标签页切换 -->
    <div class="tab-header">
      <div 
        class="tab-item" 
        :class="{ active: currentTab === 'user' }" 
        @click="currentTab = 'user'"
      >
        <span class="tab-icon">👥</span>
        <span>用户管理</span>
      </div>
      <div 
        class="tab-item" 
        :class="{ active: currentTab === 'vehicle' }" 
        @click="currentTab = 'vehicle'"
      >
        <span class="tab-icon">🛵</span>
        <span>电动车管理</span>
      </div>
    </div>

    <!-- ================= 用户管理模块 ================= -->
    <div v-if="currentTab === 'user'" class="module user-module">
      <!-- 用户管理原有结构，仅修改了冲突的变量名：pagination → userPagination，formatDate → formatDateUser -->
      <div class="page-header">
        <div class="header-bg">
          <div class="header-content">
            <h1 class="title-text">用户管理</h1>
            <div class="header-stats">
              <div class="stat-item" v-for="s in roleStats" :key="s.label">
                <span class="stat-number" :style="{ color: s.color }">{{ s.value }}</span>
                <span class="stat-label">{{ s.label }}</span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <div class="action-btn" @click="fetchUsers">↻ 刷新</div>
            <div class="action-btn primary" @click="openCreateDialog">+ 新建账号</div>
          </div>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <input v-model="filter.keyword" class="tech-input" placeholder="搜索用户名/姓名/手机" style="width:200px" @keyup.enter="fetchUsers" />
        <select v-model="filter.userType" class="tech-select" @change="fetchUsers">
          <option value="">全部角色</option>
          <option value="admin">管理员</option>
          <option value="teacher">导员</option>
          <option value="service">检查员</option>
          <option value="student">学生</option>
        </select>
        <select v-model="filter.status" class="tech-select" @change="fetchUsers">
          <option value="">全部状态</option>
          <option value="active">正常</option>
          <option value="frozen">冻结</option>
        </select>
        <div class="action-btn primary" @click="fetchUsers">🔍 搜索</div>
        <div class="action-btn" @click="resetFilter">重置</div>
      </div>

      <!-- 用户列表 -->
      <div class="table-container">
        <div class="table-header">
          <span class="table-title">用户列表</span>
          <div class="page-controls">
            <div class="page-btn" :class="{ disabled: userPagination.page === 1 }" @click="userChangePage(-1)">←</div>
            <span class="page-info">{{ userPagination.page }} / {{ userPagination.totalPages }}</span>
            <div class="page-btn" :class="{ disabled: userPagination.page === userPagination.totalPages }" @click="userChangePage(1)">→</div>
          </div>
        </div>
        <div class="table-wrapper">
          <table class="tech-table">
            <thead>
              <tr>
                <th>用户ID</th>
                <th>头像/用户名</th>
                <th>真实姓名</th>
                <th>角色</th>
                <th>学号/工号</th>
                <th>联系方式</th>
                <th>部门</th>
                <th>注册时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in userList" :key="user.userId">
                <td><span class="id-cell">#{{ user.userId }}</span></td>
                <td>
                  <div class="user-cell">
                    <div class="avatar" :style="{ background: getAvatarColor(user.userType) }">
                      {{ (user.realName || user.username || '?').charAt(0) }}
                    </div>
                    <div class="username">{{ user.username }}</div>
                  </div>
                </td>
                <td>{{ user.realName || '—' }}</td>
                <td>
                  <span class="role-badge" :class="user.userType">{{ getRoleLabel(user.userType) }}</span>
                </td>
                <td>{{ user.studentId || user.jobId || '—' }}</td>
                <td>
                  <div class="contact-info">
                    <div>{{ user.phone || '—' }}</div>
                    <div class="contact-email">{{ user.email || '' }}</div>
                  </div>
                </td>
                <td>{{ user.department || '—' }}</td>
                <td>{{ formatDateUser(user.createTime) }}</td>
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
                    <div class="action-btn small danger" @click="resetPwd(user)">🔑 重置密码</div>
                    <div class="action-btn small danger" @click="deleteUser(user)">🗑 删除</div>
                  </div>
                </td>
              </tr>
              <tr v-if="userList.length === 0"><td colspan="10" class="empty-row">暂无用户数据</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 导员-班级配置 -->
      <div class="panel">
        <div class="panel-header">
          <span>🎓 导员–班级绑定配置</span>
          <div class="action-btn small primary" @click="addBinding">+ 新增绑定</div>
        </div>
        <div class="panel-body">
          <div class="binding-grid">
            <div class="binding-card" v-for="b in counselorBindings" :key="b.id">
              <div class="binding-header">
                <div class="counselor-name">{{ b.counselorName }}</div>
                <div class="binding-actions">
                  <span class="action-btn small" @click="editBinding(b)">✏️</span>
                  <span class="action-btn small danger" @click="removeBinding(b.id)">🗑</span>
                </div>
              </div>
              <div class="binding-classes">
                <span class="class-tag" v-for="cls in b.classes" :key="cls">{{ cls }}</span>
              </div>
              <div class="binding-stats">管辖 {{ b.studentCount }} 名学生</div>
            </div>
            <div class="binding-card add-card" @click="addBinding" v-if="!counselorBindings.length">
              <span class="add-icon">+</span>
              <span>点击添加导员班级绑定</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 新建/编辑用户弹窗 -->
      <div v-if="userDialog.visible" class="modal-overlay" @click="userDialog.visible = false">
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
                <label class="form-label">初始密码 *</label>
                <input type="password" v-model="userDialog.form.password" class="tech-input" placeholder="默认 123456" />
              </div>
              <div class="form-item">
                <label class="form-label">真实姓名 *</label>
                <input v-model="userDialog.form.realName" class="tech-input" placeholder="真实姓名" />
              </div>
              <div class="form-item">
                <label class="form-label">角色 *</label>
                <select v-model="userDialog.form.userType" class="tech-select-full" @change="onUserDialogTypeChangeBtn">
                  <option value="student">学生</option>
                  <option value="teacher">导员</option>
                  <option value="service">检查员</option>
                  <option value="admin">管理员</option>
                </select>
              </div>
              <div class="form-item">
                <label class="form-label">{{ userDialog.form.userType === 'student' ? '学号' : '工号' }}</label>
                <input v-model="userDialog.form.identifier" class="tech-input" :placeholder="userDialog.form.userType === 'student' ? '请输入学号' : '请输入工号'" />
              </div>
              <div class="form-item">
                <label class="form-label">手机号 *</label>
                <input v-model="userDialog.form.phone" class="tech-input" placeholder="手机号" />
              </div>
              <div class="form-item">
                <label class="form-label">邮箱</label>
                <input v-model="userDialog.form.email" class="tech-input" placeholder="邮箱" />
              </div>
              <div class="form-item" v-if="userDialog.form.userType !== 'student'">
                <label class="form-label">部门/院系</label>
                <input v-model="userDialog.form.department" class="tech-input" placeholder="部门或院系" />
              </div>
              <template v-if="userDialog.form.userType === 'student'">
                <div class="form-item" style="grid-column: 1 / -1">
                  <label class="form-label">学籍班级 *</label>
                  <select v-model.number="studentOrgSelectIndex" class="tech-select-full" @change="syncStudentOrgFromSelectBtn">
                    <option :value="-1">请选择</option>
                    <option v-for="(o, i) in orgClassOptions" :key="i" :value="i">{{ o.college }} · {{ o.major }} · {{ o.className }}</option>
                  </select>
                </div>
                <div class="form-item">
                  <label class="form-label">行政院系（选填）</label>
                  <input v-model="userDialog.form.department" class="tech-input" placeholder="可留空或与学院一致" />
                </div>
              </template>
            </div>
          </div>
          <div class="modal-footer">
            <div class="action-btn" @click="userDialog.visible = false">取消</div>
            <div class="action-btn primary" @click="submitUser">{{ userDialog.isEdit ? '保存修改' : '创建账号' }}</div>
          </div>
        </div>
      </div>

      <!-- 角色分配弹窗 -->
      <div v-if="roleDialog.visible" class="modal-overlay" @click="roleDialog.visible = false">
        <div class="modal-content sm" @click.stop>
          <div class="modal-header">
            <span class="modal-title">🎭 角色分配</span>
            <span class="modal-close" @click="roleDialog.visible = false">×</span>
          </div>
          <div class="modal-body">
            <div class="role-user-info">
              <div class="role-username">{{ roleDialog.user?.username }}</div>
              <div class="role-current">当前角色：<span class="role-badge" :class="roleDialog.user?.userType">{{ getRoleLabel(roleDialog.user?.userType) }}</span></div>
            </div>
            <div class="role-options">
              <div v-for="r in roleOptions" :key="r.value" class="role-option" :class="{ active: roleDialog.newRole === r.value }" @click="roleDialog.newRole = r.value">
                <div class="role-opt-icon">{{ r.icon }}</div>
                <div class="role-opt-info">
                  <div class="role-opt-name">{{ r.label }}</div>
                  <div class="role-opt-desc">{{ r.desc }}</div>
                </div>
                <span class="role-check" v-if="roleDialog.newRole === r.value">✓</span>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <div class="action-btn" @click="roleDialog.visible = false">取消</div>
            <div class="action-btn primary" @click="submitRole">确认分配</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ================= 车辆管理模块 ================= -->
    <div v-if="currentTab === 'vehicle'" class="module vehicle-module">
      <!-- 车辆管理原有结构，修改了冲突的变量名：pagination → vehiclePagination，其余保持不变 -->
      <div class="page-header">
        <div class="header-bg">
          <div class="header-content">
            <h1 class="page-title">
              <span class="title-text">电动车智能注册管理</span>
            </h1>
            <div class="header-stats">
              <div class="stat-item">
                <span class="stat-number">{{ stats.total }}</span>
                <span class="stat-label">总注册量</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-number">{{ stats.approved }}</span>
                <span class="stat-label">已通过</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-number">{{ stats.pending }}</span>
                <span class="stat-label">待审核</span>
              </div>
            </div>
            <div class="header-actions">
              <div class="action-btn primary" @click="goToRegisterPage">
                <div class="btn-icon">+</div>
                <div class="btn-text">新增注册</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区域 (左右布局) -->
      <div class="main-content">
        <!-- 左侧搜索筛选区域 -->
        <div class="left-panel">
          <div class="search-form">
            <div class="form-group">
              <label class="form-label">车牌号码</label>
              <div class="form-input">
                <input type="text" v-model="searchForm.licensePlate" placeholder="输入车牌号" class="tech-input" />
                <div class="input-border"></div>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">车主类型</label>
              <div class="form-input">
                <select v-model="searchForm.userType" class="tech-select">
                  <option value="">全部类型</option>
                  <option value="student">学生</option>
                  <option value="teacher">教职工</option>
                  <option value="service">后勤人员</option>
                  <option value="admin">管理员</option>
                </select>
                <div class="input-border"></div>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">注册状态</label>
              <div class="status-selector">
                <div v-for="status in statusOptions" :key="status.value" :class="['status-option', { active: searchForm.status === status.value }]" @click="searchForm.status = searchForm.status === status.value ? '' : status.value">
                  <div class="status-dot" :style="{ background: status.color }"></div>
                  <div class="status-text">{{ status.label }}</div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">注册时间</label>
              <div class="date-range">
                <div class="date-input">
                  <input type="date" v-model="searchForm.startTime" class="tech-input" />
                  <div class="input-border"></div>
                </div>
                <div class="date-separator">至</div>
                <div class="date-input">
                  <input type="date" v-model="searchForm.endTime" class="tech-input" />
                  <div class="input-border"></div>
                </div>
              </div>
            </div>
            <div class="form-actions">
              <div class="search-btn" @click="handleSearch">
                <div class="btn-glow"></div>
                <div class="btn-content">
                  <div class="btn-icon">⌕</div>
                  <div class="btn-text">搜索车辆</div>
                </div>
              </div>
              <div class="reset-btn" @click="resetSearch">重置筛选</div>
            </div>
          </div>
          <div class="stats-cards">
            <div class="stat-card" v-for="stat in quickStats" :key="stat.label">
              <div class="card-icon" :style="{ background: stat.bgColor }">{{ stat.icon }}</div>
              <div class="card-content">
                <div class="card-value">{{ stat.value }}</div>
                <div class="card-label">{{ stat.label }}</div>
              </div>
              <div class="card-wave"></div>
            </div>
          </div>
        </div>

        <!-- 右侧数据表格区域 -->
        <div class="right-panel">
          <div class="table-container">
            <div class="table-header">
              <div class="table-title">车辆信息列表</div>
              <div class="table-actions">
                <div class="total-info">共 {{ vehiclePagination.total }} 条记录</div>
                <div class="page-controls">
                  <div class="page-btn" @click="prevPage" :class="{ disabled: vehiclePagination.currentPage === 1 }">←</div>
                  <div class="page-info">{{ vehiclePagination.currentPage }} / {{ vehiclePagination.totalPages }}</div>
                  <div class="page-btn" @click="nextPage" :class="{ disabled: vehiclePagination.currentPage === vehiclePagination.totalPages }">→</div>
                </div>
              </div>
            </div>
            <div class="table-wrapper">
              <table class="tech-table">
                <thead>
                  <tr>
                    <th style="width: 60px;"><input type="checkbox" :checked="allSelected" @change="toggleSelectAll" class="tech-checkbox" /></th>
                    <th style="width: 80px;">图片</th>
                    <th style="width: 80px;">ID</th>
                    <th style="width: 100px;">车牌号</th>
                    <th style="width: 150px;">车主信息</th>
                    <th style="width: 150px;">车辆信息</th>
                    <th style="width: 180px;">硬件状态</th>
                    <th style="width: 140px;">注册信息</th>
                    <th style="width: 100px;">状态</th>
                    <th style="width: 80px;">违规次数</th>
                    <th style="width: 220px;">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="vehicle in vehicleList" :key="vehicle.vehicleId">
                    <td><input type="checkbox" :checked="isSelected(vehicle)" @change="toggleSelect(vehicle)" class="tech-checkbox" /></td>
                    <td>
                      <div class="vehicle-image-cell" @click="previewImage(vehicle.vehiclePhoto, vehicle)">
                        <div class="image-container">
                          <img v-if="vehicle.vehiclePhoto" :src="getFullImageUrl(vehicle.vehiclePhoto)" :alt="vehicle.licensePlate" class="vehicle-image" @error="handleImageError" />
                          <div v-else class="no-image"><div class="no-image-icon">📷</div><div class="no-image-text">暂无图片</div></div>
                        </div>
                      </div>
                    </td>
                    <td><div class="id-cell">#{{ vehicle.vehicleId }}</div></td>
                    <td>
                      <div class="license-cell">
                        <div class="license-plate">{{ vehicle.licensePlate }}</div>
                        <div v-if="vehicle.isBlacklist" class="blacklist-tag">黑名单</div>
                      </div>
                    </td>
                    <td>
                      <div class="owner-cell">
                        <div class="owner-name">{{ vehicle.ownerName || vehicle.realName }}</div>
                        <div class="owner-type" :class="vehicle.userType">{{ getUserTypeText(vehicle.userType) }}</div>
                        <div class="owner-id">{{ vehicle.studentId || vehicle.jobId || vehicle.userId }}</div>
                      </div>
                    </td>
                    <td>
                      <div class="vehicle-cell">
                        <div class="vehicle-brand">{{ vehicle.brand }} · {{ vehicle.color }}</div>
                        <div class="vehicle-detail">车架: {{ vehicle.frameNum }}</div>
                        <div class="vehicle-detail">电池: {{ vehicle.batteryType || '未知' }}</div>
                      </div>
                    </td>
                    <td>
                      <div class="hardware-cell">
                        <div class="hardware-item"><div class="hardware-icon" :class="vehicle.etcNumber ? '正常' : '离线'">○</div><div class="hardware-text">BLE: {{ vehicle.etcNumber || '未绑定' }}</div></div>
                        <div class="hardware-item"><div class="hardware-icon" :class="vehicle.gpsStatus || '离线'">⊙</div><div class="hardware-text">GPS: {{ vehicle.gpsStatus || '离线' }}</div></div>
                        <div class="hardware-item"><div class="hardware-icon battery" :class="{ low: (vehicle.battery || 0) < 20 }">⚡</div><div class="hardware-text">电量: {{ vehicle.battery !== undefined ? vehicle.battery + '%' : '--' }}</div></div>
                      </div>
                    </td>
                    <td>
                      <div class="register-cell">
                        <div class="register-time">注册: {{ formatDate(vehicle.registerTime) }}</div>
                        <div class="register-time">审核: {{ vehicle.auditTime ? formatDate(vehicle.auditTime) : '待审核' }}</div>
                        <div class="register-time">有效期: {{ formatDate(vehicle.validDate) || '长期' }}</div>
                      </div>
                    </td>
                    <td><div class="status-cell" :class="vehicle.status"><div class="status-dot"></div>{{ getStatusText(vehicle.status) }}</div></td>
                    <td><div class="violation-count-cell"><span class="violation-count" :class="{ high: (violationCountMap[vehicle.vehicleId] || 0) > 3 }">{{ violationCountMap[vehicle.vehicleId] || 0 }}</span></div></td>
                    <td>
                      <div class="action-cell">
                        <div class="action-btn small" @click="viewArchive(vehicle)"><div class="btn-icon">📂</div><div class="btn-text">档案</div></div>
                        <div v-if="vehicle.status === 'pending'" class="action-btn small success" @click="approveVehicle(vehicle)"><div class="btn-icon">✓</div><div class="btn-text">通过</div></div>
                        <div v-if="vehicle.status === 'pending'" class="action-btn small danger" @click="rejectVehicle(vehicle)"><div class="btn-icon">✗</div><div class="btn-text">拒绝</div></div>
                        <div class="action-btn small" @click="uploadPhoto(vehicle)"><div class="btn-icon">📷</div><div class="btn-text">更新图片</div></div>
                        <div class="more-actions">
                          <div class="more-btn" @click="toggleMoreActions(vehicle)">⋮</div>
                          <div v-if="vehicle.showMore" class="more-menu">
                            <div class="menu-item" @click="editVehicle(vehicle)">编辑信息</div>
                            <div class="menu-item" @click="sendReminder(vehicle)">发送提醒</div>
                            <div v-if="vehicle.status === 'approved'" class="menu-item" @click="toggleFreeze(vehicle)">{{ vehicle.isFrozen ? '解冻车辆' : '冻结车辆' }}</div>
                            <div v-if="vehicle.status === 'approved'" class="menu-item" @click="toggleBlacklist(vehicle)">{{ vehicle.isBlacklist ? '移出黑名单' : '加入黑名单' }}</div>
                            <div class="menu-item delete" @click="deleteVehicle(vehicle)">注销车辆</div>
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="table-footer">
              <div class="selected-info" v-if="selectedVehicles.length > 0">
                已选择 {{ selectedVehicles.length }} 个车辆
                <div class="batch-actions">
                  <div class="batch-btn" @click="batchApprove" :class="{ disabled: !hasPendingSelected }">批量通过</div>
                  <div class="batch-btn" @click="batchReject" :class="{ disabled: !hasPendingSelected }">批量拒绝</div>
                  <div class="batch-btn" @click="batchFreeze" :class="{ disabled: !hasApprovedSelected }">批量冻结</div>
                  <div class="batch-btn" @click="clearSelection">清空选择</div>
                </div>
              </div>
            </div>
          </div>
          <div class="overview-cards">
            <div class="overview-card">
              <div class="overview-header"><div class="overview-icon">📊</div><div class="overview-title">车辆分布</div></div>
              <div class="overview-content">
                <div class="distribution-item" v-for="item in vehicleDistribution" :key="item.type">
                  <div class="distribution-label">{{ item.type }}</div>
                  <div class="distribution-bar"><div class="bar-fill" :style="{ width: item.percentage + '%', background: item.color }"></div></div>
                  <div class="distribution-value">{{ item.count }}辆</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 车辆档案抽屉弹窗 -->
      <div v-if="archiveVisible" class="drawer-overlay" @click="archiveVisible = false">
        <div class="drawer" @click.stop>
          <div class="drawer-header"><span class="drawer-title">📂 车辆全档案</span><span class="drawer-close" @click="archiveVisible = false">×</span></div>
          <div class="drawer-body" v-if="currentVehicle">
            <div class="archive-section">
              <div class="section-title">车主 & 车辆基本信息</div>
              <div class="info-grid">
                <div class="info-item"><div class="il">姓名</div><div class="iv">{{ currentVehicle.ownerName || currentVehicle.realName }}</div></div>
                <div class="info-item"><div class="il">类型</div><div class="iv">{{ getUserTypeText(currentVehicle.userType) }}</div></div>
                <div class="info-item"><div class="il">学号/工号</div><div class="iv">{{ currentVehicle.studentId || currentVehicle.jobId || '—' }}</div></div>
                <div class="info-item"><div class="il">手机号</div><div class="iv">{{ currentVehicle.phone }}</div></div>
                <div class="info-item"><div class="il">车牌号</div><div class="iv"><b>{{ currentVehicle.licensePlate }}</b></div></div>
                <div class="info-item"><div class="il">品牌颜色</div><div class="iv">{{ currentVehicle.brand }} · {{ currentVehicle.color }}</div></div>
                <div class="info-item"><div class="il">车架号</div><div class="iv">{{ currentVehicle.frameNum }}</div></div>
                <div class="info-item"><div class="il">电池类型</div><div class="iv">{{ currentVehicle.batteryType || '—' }}</div></div>
              </div>
            </div>
            <div class="archive-section">
              <div class="section-title">BLE 蓝牙标签信息</div>
              <div v-if="currentVehicle.etcNumber" class="info-grid">
                <div class="info-item"><div class="il">标签编号</div><div class="iv">{{ currentVehicle.etcNumber }}</div></div>
                <div class="info-item"><div class="il">状态</div><div class="iv"><span class="status-badge approved">正常</span></div></div>
              </div>
              <div v-else class="no-data-tip">未绑定 BLE 蓝牙标签</div>
            </div>
            <div class="archive-section">
              <div class="section-title">定位设备信息</div>
              <div class="info-grid">
                <div class="info-item"><div class="il">GPS状态</div><div class="iv"><span class="gps-badge" :class="currentVehicle.gpsStatus">{{ currentVehicle.gpsStatus === 'online' ? '🟢 在线' : '🔴 离线' }}</span></div></div>
                <div class="info-item"><div class="il">电量</div><div class="iv">{{ currentVehicle.battery != null ? currentVehicle.battery + '%' : '—' }}</div></div>
              </div>
            </div>
            <div class="archive-section">
              <div class="section-title">近期违规记录（最近10条）</div>
              <div v-if="archiveViolations.length">
                <div class="violation-item" v-for="vi in archiveViolations" :key="vi.illegalId">
                  <div class="vi-type">{{ getViolationTypeName(vi.illegalType) }}</div>
                  <div class="vi-time">{{ formatDate(vi.illegalTime) }}</div>
                  <div class="vi-state"><span class="state-badge" :class="'state-' + vi.illegalState">{{ getViolationStateName(vi.illegalState) }}</span></div>
                  <div class="vi-points">-{{ vi.deductionPoints }}分</div>
                </div>
              </div>
              <div v-else class="no-data-tip">暂无违规记录</div>
            </div>
          </div>
          <div class="drawer-footer">
            <div class="action-btn primary" @click="sendReminder(currentVehicle)">🔔 发送整改提醒</div>
            <div class="action-btn" @click="archiveVisible = false">关闭</div>
          </div>
        </div>
      </div>

      <!-- 车辆编辑模态框 -->
      <div v-if="showEditModal" class="edit-modal" @click="closeEditModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header"><h3>编辑车辆信息</h3><div class="close-btn" @click="closeEditModal">×</div></div>
          <div class="modal-body" v-if="editingVehicle">
            <div class="edit-form">
              <div class="form-group"><label class="form-label">车牌号</label><div class="form-input"><input type="text" v-model="editingVehicle.licensePlate" disabled class="tech-input" /><div class="input-border"></div></div><div class="field-note">车牌号不可修改</div></div>
              <div class="form-group"><label class="form-label">车架号</label><div class="form-input"><input type="text" :value="editingVehicle.frameNum" disabled class="tech-input" /><div class="input-border"></div></div><div class="field-note">车架号不可修改</div></div>
              <div class="form-group"><label class="form-label">品牌</label><div class="form-input"><input type="text" v-model="editForm.brand" placeholder="输入品牌" class="tech-input" /><div class="input-border"></div></div></div>
              <div class="form-group"><label class="form-label">颜色</label><div class="form-input"><input type="text" v-model="editForm.color" placeholder="输入颜色" class="tech-input" /><div class="input-border"></div></div></div>
              <div class="form-group"><label class="form-label">电池类型</label><div class="form-input"><select v-model="editForm.batteryType" class="tech-select"><option value="">请选择</option><option value="铅酸电池">铅酸电池</option><option value="锂电池">锂电池</option><option value="石墨烯电池">石墨烯电池</option><option value="其他">其他</option></select><div class="input-border"></div></div></div>
              <div class="form-group"><label class="form-label">车辆类型</label><div class="form-input"><select v-model="editForm.vehicleType" class="tech-select"><option value="">请选择</option><option value="电动自行车">电动自行车</option><option value="电动摩托车">电动摩托车</option><option value="电动三轮车">电动三轮车</option><option value="其他">其他</option></select><div class="input-border"></div></div></div>
              <div class="form-group">
                <label class="form-label">车辆照片</label>
                <div class="photo-upload-area">
                  <div class="current-photo" v-if="editForm.vehiclePhoto">
                    <img :src="getFullImageUrl(editForm.vehiclePhoto)" alt="当前照片" class="preview-thumb" />
                    <div class="photo-actions"><div class="photo-btn" @click="triggerFileInput">更换图片</div><div class="photo-btn" @click="removePhoto" v-if="editForm.vehiclePhoto">移除</div></div>
                  </div>
                  <div v-else class="no-photo" @click="triggerFileInput"><div class="upload-icon">📷</div><div class="upload-text">点击上传照片</div></div>
                  <input type="file" ref="fileInput" style="display: none" accept="image/*" @change="handlePhotoChange" />
                </div>
              </div>
            </div>
          </div>
          <div class="modal-actions"><div class="cancel-btn" @click="closeEditModal">取消</div><div class="confirm-btn" @click="updateVehicle" :class="{ disabled: !isEditFormValid }">保存修改</div></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const API = '/api'
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`

// ==================== 全局状态 ====================
const currentTab = ref('vehicle') // 'user' 或 'vehicle'

// ==================== 用户模块 ====================
const userList = ref([])
const filter = reactive({ keyword: '', userType: '', status: '' })
const userPagination = reactive({ page: 1, pageSize: 12, total: 0, totalPages: 1 })

const roleStats = ref([
  { label: '管理员', value: 0, color: '#f44336' },
  { label: '导员', value: 0, color: '#ff9800' },
  { label: '检查员', value: 0, color: '#2196f3' },
  { label: '学生', value: 0, color: '#4caf50' },
  { label: '总计', value: 0, color: '#00d4ff' },
])

const roleOptions = [
  { value: 'admin', label: '管理员', icon: '👑', desc: '系统全权管理，可配置所有功能' },
  { value: 'teacher', label: '导员', icon: '🎓', desc: '管理所辖学生，跟进违规整改' },
  { value: 'service', label: '检查员', icon: '🔍', desc: '违规核实与整改审核' },
  { value: 'student', label: '学生', icon: '🧑‍🎓', desc: '普通用户，只能操作自己账户' },
]

const userDialog = reactive({
  visible: false, isEdit: false,
  form: { username: '', password: '123456', realName: '', userType: 'student', identifier: '', phone: '', email: '', department: '', college: '', major: '', className: '', userId: null }
})

const orgClassOptions = ref([])
const studentOrgSelectIndex = ref(-1)

const loadOrgClassOptions = async () => {
  try {
    const res = await axios.get(`${API}/org-class/options`)
    if (res.data.code === 200) orgClassOptions.value = res.data.data || []
  } catch {
    orgClassOptions.value = []
  }
}

const findStudentOrgIndex = () => {
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

const syncStudentOrgFromSelectBtn = () => {
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

const onUserDialogTypeChangeBtn = () => {
  if (userDialog.form.userType !== 'student') {
    studentOrgSelectIndex.value = -1
    userDialog.form.college = ''
    userDialog.form.major = ''
    userDialog.form.className = ''
  }
}

const roleDialog = reactive({ visible: false, user: null, newRole: '' })

const counselorBindings = ref([
      { id: 1, counselorName: '许诚', classes: ['计科2101班','计科2102班','软件2101班'], studentCount: 90 },
  { id: 2, counselorName: '孙七', classes: ['机械2101班','机械2102班'], studentCount: 60 },
])

// 用户模块方法
const fetchUsers = async () => {
  try {
    const res = await axios.get(`${API}/user/list`, {
      params: {
        keyword: filter.keyword || undefined,
        userType: filter.userType || undefined,
        status: filter.status || undefined,
        pageNum: userPagination.page, pageSize: userPagination.pageSize
      }
    })
    if (res.data.code === 200) {
      userList.value = res.data.data || []
      userPagination.total = res.data.total || 0
      userPagination.totalPages = Math.max(1, res.data.totalPages || 1)
      const statsRes = await axios.get(`${API}/user/stats`)
      if (statsRes.data.code === 200) {
        const typeStats = statsRes.data.data?.typeStats || []
        const getCount = (type) => typeStats.find(s => s.type === type)?.count || 0
        roleStats.value[0].value = getCount('admin')
        roleStats.value[1].value = getCount('teacher')
        roleStats.value[2].value = getCount('service')
        roleStats.value[3].value = getCount('student')
        roleStats.value[4].value = statsRes.data.data?.total || 0
      }
    }
  } catch { ElMessage.error('获取用户数据失败') }
}

const openCreateDialog = () => {
  userDialog.isEdit = false
  Object.assign(userDialog.form, { username: '', password: '123456', realName: '', userType: 'student', identifier: '', phone: '', email: '', department: '', college: '', major: '', className: '', userId: null })
  studentOrgSelectIndex.value = -1
  userDialog.visible = true
}

const editUser = async (user) => {
  userDialog.isEdit = true
  Object.assign(userDialog.form, {
    userId: user.userId, username: user.username, realName: user.realName,
    userType: user.userType, identifier: user.studentId || user.jobId || '',
    phone: user.phone, email: user.email || '', department: user.department || '',
    college: user.college || '', major: user.major || '', className: user.className || ''
  })
  await loadOrgClassOptions()
  studentOrgSelectIndex.value = user.userType === 'student' ? findStudentOrgIndex() : -1
  userDialog.visible = true
}

const submitUser = async () => {
  if (!userDialog.form.realName.trim() || !userDialog.form.phone.trim()) {
    ElMessage.warning('请填写必填项')
    return
  }
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
        college: userDialog.form.userType === 'student' ? userDialog.form.college : undefined,
        major: userDialog.form.userType === 'student' ? userDialog.form.major : undefined,
        className: userDialog.form.userType === 'student' ? userDialog.form.className : undefined,
      }
      const res = await axios.put(`${API}/user/${userDialog.form.userId}`, data)
      if (res.data.code === 200) { ElMessage.success('用户信息已更新'); userDialog.visible = false; fetchUsers() }
      else ElMessage.error(res.data.message || '更新失败')
    } else {
      if (!userDialog.form.username.trim()) { ElMessage.warning('请填写用户名'); return }
      const data = {
        username: userDialog.form.username,
        password: userDialog.form.password || '123456',
        realName: userDialog.form.realName,
        userType: userDialog.form.userType,
        phone: userDialog.form.phone,
        email: userDialog.form.email,
        department: userDialog.form.userType === 'student'
          ? (userDialog.form.department?.trim() || userDialog.form.college?.trim() || '')
          : userDialog.form.department,
        college: userDialog.form.userType === 'student' ? userDialog.form.college?.trim() : undefined,
        major: userDialog.form.userType === 'student' ? userDialog.form.major?.trim() : undefined,
        className: userDialog.form.userType === 'student' ? userDialog.form.className?.trim() : undefined,
        studentId: userDialog.form.userType === 'student' ? userDialog.form.identifier : undefined,
        jobId: userDialog.form.userType !== 'student' ? userDialog.form.identifier : undefined,
      }
      const res = await axios.post(`${API}/user/register`, data)
      if (res.data.code === 200) { ElMessage.success('账号创建成功'); userDialog.visible = false; fetchUsers() }
      else ElMessage.error(res.data.message || '创建失败')
    }
  } catch (e) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

const changeRole = (user) => {
  roleDialog.user = user
  roleDialog.newRole = user.userType
  roleDialog.visible = true
}

const submitRole = async () => {
  if (roleDialog.newRole === roleDialog.user.userType) { roleDialog.visible = false; return }
  try {
    const res = await axios.post(`${API}/user/${roleDialog.user.userId}/role`, null, { params: { role: roleDialog.newRole } })
    if (res.data.code === 200) {
      ElMessage.success(`角色已变更为：${getRoleLabel(roleDialog.newRole)}`)
      roleDialog.visible = false; fetchUsers()
    } else ElMessage.error(res.data.message || '角色变更失败')
  } catch { ElMessage.error('操作失败') }
}

const toggleFreeze = async (user) => {
  const newStatus = user.status === 'active' ? 'frozen' : 'active'
  const action = newStatus === 'frozen' ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(`确定要${action}用户"${user.realName || user.username}"吗？`, `${action}用户`, { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await axios.post(`${API}/user/${user.userId}/status`, null, { params: { status: newStatus } })
    if (res.data.code === 200) { ElMessage.success(`已${action}`); fetchUsers() }
    else ElMessage.error(res.data.message)
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const resetPwd = async (user) => {
  try {
    await ElMessageBox.confirm(`确定重置"${user.realName}"的密码为 123456 吗？`, '重置密码', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await axios.post(`${API}/user/reset-password`, null, { params: { userId: user.userId, newPassword: '123456' } })
    if (res.data.code === 200) ElMessage.success('密码已重置为 123456')
    else ElMessage.error(res.data.message)
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const deleteUser = async (user) => {
  const name = user.realName || user.username || `用户#${user.userId}`
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${name}」？删除后该账号将无法登录，相关数据请确认已备份。`,
      '删除用户',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
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

const addBinding = () => {
  counselorBindings.value.push({
    id: Date.now(), counselorName: '新导员', classes: ['待配置班级'], studentCount: 0
  })
}
const editBinding = (b) => ElMessage.info(`编辑 ${b.counselorName} 的班级配置`)
const removeBinding = (id) => { counselorBindings.value = counselorBindings.value.filter(b => b.id !== id) }
const resetFilter = () => { filter.keyword = ''; filter.userType = ''; filter.status = ''; fetchUsers() }
const userChangePage = (dir) => {
  const np = userPagination.page + dir
  if (np >= 1 && np <= userPagination.totalPages) { userPagination.page = np; fetchUsers() }
}

const getRoleLabel = (t) => ({ admin:'管理员', teacher:'导员', service:'检查员', student:'学生', temporary:'临时' }[t] || t)
const getStatusLabel = (s) => ({ active:'正常', frozen:'已冻结', deleted:'已删除' }[s] || s)
const getAvatarColor = (t) => ({ admin:'#f44336', teacher:'#ff9800', service:'#2196f3', student:'#4caf50' }[t] || '#607d8b')
const formatDateUser = (t) => t ? new Date(t).toLocaleDateString('zh-CN') : '—'

// ==================== 车辆模块 ====================
const searchForm = reactive({ licensePlate: '', userType: '', status: '', startTime: '', endTime: '' })
const statusOptions = ref([ { value: 'pending', label: '待审核', color: '#ff9800' }, { value: 'approved', label: '已通过', color: '#4caf50' }, { value: 'rejected', label: '已拒绝', color: '#f44336' }, { value: 'frozen', label: '已冻结', color: '#2196f3' }, { value: 'expired', label: '已过期', color: '#9e9e9e' } ])
const stats = reactive({ total: 0, approved: 0, pending: 0, blacklist: 0, frozen: 0, studentCount: 0, teacherCount: 0, serviceCount: 0 })
const vehicleDistribution = ref([])
const quickStats = computed(() => [ { icon: '🛵', label: '学生车辆', value: stats.studentCount || 0, bgColor: 'rgba(0, 123, 255, 0.2)' }, { icon: '🚗', label: '教职工车辆', value: stats.teacherCount || 0, bgColor: 'rgba(40, 167, 69, 0.2)' }, { icon: '🚚', label: '后勤车辆', value: stats.serviceCount || 0, bgColor: 'rgba(255, 193, 7, 0.2)' } ])
const vehiclePagination = reactive({ currentPage: 1, pageSize: 10, total: 0, totalPages: 0 })
const vehicleList = ref([])
const selectedVehicles = ref([])
const allSelected = computed(() => selectedVehicles.value.length === vehicleList.value.length && vehicleList.value.length > 0)
const violationCountMap = ref({})
const archiveVisible = ref(false)
const currentVehicle = ref(null)
const archiveViolations = ref([])
const showEditModal = ref(false)
const editingVehicle = ref(null)
let originalEditForm = {}
const editForm = reactive({ brand: '', color: '', batteryType: '', vehicleType: '', vehiclePhoto: '' })
const fileInput = ref(null)
const isEditFormValid = computed(() => {
  if (!editingVehicle.value) return false
  const editableFields = ['brand', 'color', 'batteryType', 'vehicleType', 'vehiclePhoto']
  for (const field of editableFields) {
    const original = originalEditForm[field] ?? ''
    const current = editForm[field] ?? ''
    if (original !== current) return true
  }
  return false
})
const hasPendingSelected = computed(() => selectedVehicles.value.some(v => v.status === 'pending'))
const hasApprovedSelected = computed(() => selectedVehicles.value.some(v => v.status === 'approved'))

// 车辆模块方法
const fetchVehicleList = async () => {
  try {
    const params = {
      pageNum: vehiclePagination.currentPage,
      pageSize: vehiclePagination.pageSize,
      licensePlate: searchForm.licensePlate,
      userType: searchForm.userType,
      status: searchForm.status,
      startTime: searchForm.startTime ? searchForm.startTime + ' 00:00:00' : '',
      endTime: searchForm.endTime ? searchForm.endTime + ' 23:59:59' : ''
    }
    Object.keys(params).forEach(key => { if (!params[key] && params[key] !== 0) delete params[key] })
    const response = await axios.get(`${API}/vehicle/list`, { params })
    if (response.data.code === 200) {
      vehicleList.value = response.data.data.map(vehicle => ({ ...vehicle, showMore: false }))
      vehiclePagination.total = response.data.total || 0
      vehiclePagination.totalPages = response.data.totalPages || 1
      fetchViolationCounts()
    } else { ElMessage.error(response.data.message || '获取车辆列表失败') }
  } catch (error) {
    if (error.response?.status === 401) { ElMessage.error('登录已过期，请重新登录'); router.push('/login') } 
    else ElMessage.error('获取车辆列表失败，请检查网络连接')
  }
}
const fetchViolationCounts = async () => {
  for (const vehicle of vehicleList.value) {
    try {
      const res = await axios.get(`${API}/violation/list`, { params: { pageNum: 1, pageSize: 1, vehiclePlate: vehicle.licensePlate } })
      if (res.data.code === 200) violationCountMap.value[vehicle.vehicleId] = res.data.total || 0
    } catch (error) { console.error(`获取车辆 ${vehicle.licensePlate} 违规次数失败`, error) }
  }
}
const fetchVehicleStats = async () => {
  try {
    const response = await axios.get(`${API}/vehicle/stats`)
    if (response.data.code === 200) {
      const data = response.data.data
      stats.total = data.total || 0
      stats.approved = data.approved || 0
      stats.pending = data.pending || 0
      stats.blacklist = data.blacklist || 0
      stats.frozen = data.frozen || 0
      if (data.typeStats) {
        stats.studentCount = data.typeStats.find(item => item.type === 'student')?.count || 0
        stats.teacherCount = data.typeStats.find(item => item.type === 'teacher')?.count || 0
        stats.serviceCount = data.typeStats.find(item => item.type === 'service')?.count || 0
        vehicleDistribution.value = data.typeStats.map(item => ({
          type: item.type === 'student' ? '学生' : item.type === 'teacher' ? '教职工' : '后勤',
          count: item.count,
          percentage: ((item.count / data.total) * 100).toFixed(1),
          color: item.type === 'student' ? '#007bff' : item.type === 'teacher' ? '#28a745' : '#ffc107'
        }))
      }
    }
  } catch (error) { console.error('获取车辆统计数据失败:', error) }
}
const viewArchive = async (vehicle) => {
  currentVehicle.value = vehicle
  archiveVisible.value = true
  archiveViolations.value = []
  try {
    const res = await axios.get(`${API}/violation/list`, { params: { pageNum: 1, pageSize: 10, vehiclePlate: vehicle.licensePlate } })
    if (res.data.code === 200) archiveViolations.value = res.data.data || []
  } catch (error) { console.error('获取违规记录失败', error) }
}
const sendReminder = async (v) => {
  if (!v || !v.userId) { ElMessage.warning('该记录无绑定用户'); return }
  try {
    await axios.post(`${API}/notification/send-personal`, null, { params: { title: '车辆整改提醒', content: `您的车辆${v.licensePlate}有违规记录待处理，请及时整改。`, receiverId: v.userId } })
    ElMessage.success('提醒已发送')
  } catch (error) { console.error('发送提醒失败', error); ElMessage.error('发送失败') }
}
const approveVehicle = async (vehicle) => {
  try {
    await ElMessageBox.confirm(`确定要通过车牌号 ${vehicle.licensePlate} 的车辆注册吗？`, '审核通过', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const response = await axios.post(`${API}/vehicle/approve/${vehicle.vehicleId}`, null, { params: { auditorId: 1, remark: '审核通过' } })
    if (response.data.code === 200) { ElMessage.success('车辆审核通过'); fetchVehicleList(); fetchVehicleStats() } 
    else ElMessage.error(response.data.message || '审核失败')
  } catch (error) { if (error !== 'cancel') ElMessage.error('审核失败，请重试') }
}
const rejectVehicle = async (vehicle) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝', { confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'textarea', inputPlaceholder: '请详细说明拒绝原因...' })
    if (reason) {
      const response = await axios.post(`${API}/vehicle/reject/${vehicle.vehicleId}`, null, { params: { auditorId: 1, reason } })
      if (response.data.code === 200) { ElMessage.success('车辆审核已拒绝'); fetchVehicleList(); fetchVehicleStats() } 
      else ElMessage.error(response.data.message || '拒绝失败')
    }
  } catch (error) { if (error !== 'cancel') ElMessage.error('操作失败，请重试') }
}
const toggleFreezeVehicle = async (vehicle) => {
  try {
    const action = vehicle.isFrozen ? '解冻' : '冻结'
    await ElMessageBox.confirm(`确定要${action}车牌号 ${vehicle.licensePlate} 的车辆吗？`, `${action}车辆`, { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const response = await axios.post(`${API}/vehicle/freeze/${vehicle.vehicleId}`, null, { params: { freeze: !vehicle.isFrozen } })
    if (response.data.code === 200) { ElMessage.success(`车辆已${action}`); fetchVehicleList(); fetchVehicleStats() } 
    else ElMessage.error(response.data.message || `${action}失败`)
  } catch (error) { if (error !== 'cancel') ElMessage.error('操作失败，请重试') }
}
const toggleBlacklist = async (vehicle) => {
  try {
    const action = vehicle.isBlacklist ? '移出黑名单' : '加入黑名单'
    await ElMessageBox.confirm(`确定要将车牌号 ${vehicle.licensePlate} 的车辆${action}吗？`, action, { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const response = await axios.post(`${API}/vehicle/blacklist/${vehicle.vehicleId}`, null, { params: { blacklist: !vehicle.isBlacklist } })
    if (response.data.code === 200) { ElMessage.success(`车辆已${action}`); fetchVehicleList(); fetchVehicleStats() } 
    else ElMessage.error(response.data.message || '操作失败')
  } catch (error) { if (error !== 'cancel') ElMessage.error('操作失败，请重试') }
}
const batchApprove = async () => {
  try {
    if (!hasPendingSelected.value) { ElMessage.warning('请选择待审核的车辆'); return }
    await ElMessageBox.confirm(`确定要批量通过选中的 ${selectedVehicles.value.length} 个车辆吗？`, '批量审核通过', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const vehicleIds = selectedVehicles.value.filter(v => v.status === 'pending').map(v => v.vehicleId)
    const response = await axios.post(`${API}/vehicle/batch-approve`, { vehicleIds, auditorId: 1 })
    if (response.data.code === 200) { ElMessage.success('批量审核完成'); clearSelection(); fetchVehicleList(); fetchVehicleStats() } 
    else ElMessage.error(response.data.message || '批量审核失败')
  } catch (error) { if (error !== 'cancel') ElMessage.error('批量审核失败，请重试') }
}
const batchReject = async () => {
  try {
    if (!hasPendingSelected.value) { ElMessage.warning('请选择待审核的车辆'); return }
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因（所有选中车辆将使用相同原因）', '批量拒绝', { confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'textarea', inputPlaceholder: '请详细说明拒绝原因...' })
    if (reason) {
      const promises = selectedVehicles.value.filter(v => v.status === 'pending').map(v => axios.post(`${API}/vehicle/reject/${v.vehicleId}`, null, { params: { auditorId: 1, reason } }))
      await Promise.all(promises)
      ElMessage.success('批量拒绝完成'); clearSelection(); fetchVehicleList(); fetchVehicleStats()
    }
  } catch (error) { if (error !== 'cancel') ElMessage.error('批量拒绝失败，请重试') }
}
const batchFreeze = async () => {
  try {
    if (!hasApprovedSelected.value) { ElMessage.warning('请选择已通过的车辆'); return }
    await ElMessageBox.confirm(`确定要批量冻结选中的 ${selectedVehicles.value.length} 个车辆吗？`, '批量冻结', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const promises = selectedVehicles.value.filter(v => v.status === 'approved').map(v => axios.post(`${API}/vehicle/freeze/${v.vehicleId}`, null, { params: { freeze: true } }))
    await Promise.all(promises)
    ElMessage.success('批量冻结完成'); clearSelection(); fetchVehicleList(); fetchVehicleStats()
  } catch (error) { if (error !== 'cancel') ElMessage.error('批量冻结失败，请重试') }
}
const deleteVehicle = async (vehicle) => {
  try {
    await ElMessageBox.confirm(`确定要注销车牌号 ${vehicle.licensePlate} 的车辆吗？此操作不可恢复。`, '注销车辆', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'error' })
    const response = await axios.delete(`${API}/vehicle/${vehicle.vehicleId}`)
    if (response.data.code === 200) { ElMessage.success('车辆已注销'); fetchVehicleList(); fetchVehicleStats() } 
    else ElMessage.error(response.data.message || '注销失败')
  } catch (error) { if (error !== 'cancel') ElMessage.error('注销失败，请重试') }
}
const uploadPhoto = async (vehicle) => {
  try {
    const input = document.createElement('input'); input.type = 'file'; input.accept = 'image/*';
    input.onchange = async (e) => {
      const file = e.target.files[0]; if (!file) return;
      const formData = new FormData(); formData.append('file', file);
      ElMessage.info('正在上传图片...');
      const response = await axios.post(`${API}/vehicle/upload-photo/${vehicle.vehicleId}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      if (response.data.code === 200) { ElMessage.success('图片上传成功'); await fetchVehicleList() } 
      else ElMessage.error(response.data.message || '上传失败')
    }; input.click()
  } catch (error) { console.error('上传图片失败:', error); ElMessage.error('上传图片失败，请重试') }
}
const previewImage = (imageUrl, vehicle) => { if (!imageUrl) { ElMessage.warning('暂无图片'); return } window.open(getFullImageUrl(imageUrl), '_blank') }
const editVehicle = (vehicle) => {
  editingVehicle.value = vehicle
  editForm.brand = vehicle.brand || ''
  editForm.color = vehicle.color || ''
  editForm.batteryType = vehicle.batteryType || ''
  editForm.vehicleType = vehicle.vehicleType || ''
  editForm.vehiclePhoto = vehicle.vehiclePhoto || ''
  originalEditForm = { brand: vehicle.brand || '', color: vehicle.color || '', batteryType: vehicle.batteryType || '', vehicleType: vehicle.vehicleType || '', vehiclePhoto: vehicle.vehiclePhoto || '' }
  showEditModal.value = true
}
const closeEditModal = () => { showEditModal.value = false; editingVehicle.value = null; editForm.brand = ''; editForm.color = ''; editForm.batteryType = ''; editForm.vehicleType = ''; editForm.vehiclePhoto = ''; originalEditForm = {} }
const triggerFileInput = () => { fileInput.value.click() }
const handlePhotoChange = async (event) => {
  const file = event.target.files[0]; if (!file) return
  try {
    ElMessage.info('正在上传照片...')
    const formData = new FormData(); formData.append('file', file)
    const response = await axios.post(`${API}/vehicle/upload-photo/${editingVehicle.value.vehicleId}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (response.data.code === 200) { editForm.vehiclePhoto = response.data.data; ElMessage.success('照片上传成功') } 
    else ElMessage.error(response.data.message || '照片上传失败')
  } catch (error) { console.error('上传照片失败:', error); ElMessage.error('上传照片失败，请重试') } 
  finally { fileInput.value.value = '' }
}
const removePhoto = () => { editForm.vehiclePhoto = '' }
const updateVehicle = async () => {
  if (!editingVehicle.value) return
  const updateData = {}
  const editableFields = ['brand', 'color', 'batteryType', 'vehicleType', 'vehiclePhoto']
  for (const field of editableFields) {
    const original = originalEditForm[field] ?? ''
    const current = editForm[field] ?? ''
    if (original !== current) updateData[field] = current
  }
  if (Object.keys(updateData).length === 0) { ElMessage.warning('请至少修改一项内容'); return }
  try {
    const response = await axios.put(`${API}/vehicle/${editingVehicle.value.vehicleId}`, updateData)
    if (response.data.code === 200) { ElMessage.success('车辆信息更新成功'); await fetchVehicleList(); closeEditModal() } 
    else ElMessage.error(response.data.message || '更新失败')
  } catch (error) { console.error('更新车辆信息失败:', error); ElMessage.error('更新失败，请检查网络或稍后重试') }
}
const handleSearch = () => { vehiclePagination.currentPage = 1; fetchVehicleList() }
const resetSearch = () => { searchForm.licensePlate = ''; searchForm.userType = ''; searchForm.status = ''; searchForm.startTime = ''; searchForm.endTime = ''; handleSearch() }
const formatDate = (dateStr) => { if (!dateStr) return ''; return dateStr.split(' ')[0] }
const getFullImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  if (url.startsWith('/uploads')) return url;
  return `/uploads/${url.replace(/^\/+/, '')}`;
}
const getStatusText = (status) => ({ pending: '待审核', approved: '已通过', rejected: '已拒绝', frozen: '已冻结', expired: '已过期' }[status] || status)
const getUserTypeText = (userType) => ({ student: '学生', teacher: '教师', service: '后勤', admin: '管理员' }[userType] || userType)
const getViolationTypeName = (t) => ({ 1:'违规停放',2:'超速行驶',3:'违规充电',4:'未登记',5:'进入禁行区',6:'违规载人',7:'遮挡车牌',8:'其他' }[t] || '未知')
const getViolationStateName = (s) => ({ 0:'待整改',1:'整改中',2:'已整改',3:'逾期',4:'申诉中' }[s] || '未知')
const toggleSelectAll = () => { allSelected.value ? selectedVehicles.value = [] : selectedVehicles.value = [...vehicleList.value] }
const toggleSelect = (vehicle) => { const index = selectedVehicles.value.findIndex(v => v.vehicleId === vehicle.vehicleId); index === -1 ? selectedVehicles.value.push(vehicle) : selectedVehicles.value.splice(index, 1) }
const isSelected = (vehicle) => selectedVehicles.value.some(v => v.vehicleId === vehicle.vehicleId)
const toggleMoreActions = (vehicle) => { vehicleList.value.forEach(v => { if (v !== vehicle) v.showMore = false }); vehicle.showMore = !vehicle.showMore }
const clearSelection = () => { selectedVehicles.value = [] }
const prevPage = () => { if (vehiclePagination.currentPage > 1) { vehiclePagination.currentPage--; fetchVehicleList() } }
const nextPage = () => { if (vehiclePagination.currentPage < vehiclePagination.totalPages) { vehiclePagination.currentPage++; fetchVehicleList() } }
const goToRegisterPage = () => { router.push('/directive/dragable') }
const handleImageError = (event) => { event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA2MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjYwIiBoZWlnaHQ9IjQwIiBmaWxsPSIjMkEyQjJDIiBmaWxsLW9wYWNpdHk9IjAuMSIvPgo8cGF0aCBkPSJNMzAgMTZDMzIuMjA5MSAxNiAzNCAxNy43OTA5IDM0IDIwQzM0IDIyLjIwOTEgMzIuMjA5MSAyNCAzMCAyNEMyNy43OTA5IDI0IDI2IDIyLjIwOTEgMjYgMjBDMjYgMTcuNzkwOSAyNy43OTA5IDE2IDMwIDE2WiIgZmlsbD0iIzc1NzU3NSIvPgo8cGF0aCBkPSJNMzQgMzJDMzQgMzIuNTUyMyAzMy41NTIzIDMzIDMzIDMzSDI3QzI2LjQ0NzcgMzMgMjYgMzIuNTUyMyAyNiAzMlYzMUMyNiAzMC4yMjgzIDI2LjcyODMgMjkuNSAyNy41IDI5LjVIMzIuNUMzMy4yNzE3IDI5LjUgMzQgMzAuMjI4MyAzNCAzMVYzMloiIGZpbGw9IiM3NTc1NzUiLz4KPC9zdmc+Cg==' }

onMounted(() => {
  loadOrgClassOptions()
  fetchUsers()
  fetchVehicleList()
  fetchVehicleStats()
})
</script>

<style scoped>
/* 全局样式与标签页 */
.integrated-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%);
  color: #fff;
  padding: 20px;
}
.tab-header {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  padding-bottom: 10px;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255,255,255,0.05);
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
  border-bottom: none;
}
.tab-item.active {
  background: rgba(0,212,255,0.1);
  border-color: rgba(0,212,255,0.3);
  color: #00d4ff;
}
.tab-icon {
  font-size: 18px;
}
.module {
  width: 100%;
}

/* 以下是两个组件原有的样式，已通过 scoped 自动隔离，直接合并即可 */
/* 用户模块样式 */
.page-header .header-bg {
  background: linear-gradient(90deg,rgba(0,123,255,.1),rgba(102,16,242,.1));
  border-radius: 16px;
  padding: 22px 24px;
  border: 1px solid rgba(0,123,255,.2);
  position: relative;
}
.header-bg::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg,#007bff,#6610f2);
  border-radius: 16px 16px 0 0;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-text {
  font-size: 24px;
  font-weight: bold;
  background: linear-gradient(90deg,#fff,#00d4ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.header-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  background: rgba(0,0,0,.3);
  padding: 10px 20px;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,.1);
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-number {
  font-size: 22px;
  font-weight: bold;
}
.stat-label {
  font-size: 12px;
  color: rgba(255,255,255,.6);
}
.header-actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 9px 16px;
  background: rgba(255,255,255,.1);
  border: 1px solid rgba(255,255,255,.2);
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: all .2s;
}
.action-btn:hover {
  background: rgba(255,255,255,.15);
}
.action-btn.primary {
  background: linear-gradient(90deg,#007bff,#6610f2);
  border: none;
}
.action-btn.warn {
  background: rgba(255,152,0,.2);
  border-color: rgba(255,152,0,.4);
  color: #ff9800;
}
.action-btn.success {
  background: rgba(76,175,80,.2);
  border-color: rgba(76,175,80,.4);
  color: #4caf50;
}
.action-btn.danger {
  background: rgba(244,67,54,.2);
  border-color: rgba(244,67,54,.4);
  color: #f44336;
}
.action-btn.small {
  padding: 5px 10px;
  font-size: 12px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255,255,255,.05);
  border-radius: 12px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,.1);
  flex-wrap: wrap;
}
.tech-input,.tech-select {
  padding: 8px 12px;
  background: rgba(0,0,0,.3);
  border: 1px solid rgba(255,255,255,.2);
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
}
.tech-select {
  min-width: 110px;
}
.tech-input:focus,.tech-select:focus {
  outline: none;
  border-color: #00d4ff;
}
.table-container {
  background: rgba(255,255,255,.05);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,.1);
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(255,255,255,.1);
}
.table-title {
  font-size: 15px;
  font-weight: 500;
}
.page-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,.1);
  border-radius: 6px;
  cursor: pointer;
}
.page-btn:hover:not(.disabled) {
  background: rgba(0,212,255,.2);
}
.page-btn.disabled {
  opacity: .3;
  cursor: not-allowed;
}
.page-info {
  font-size: 13px;
  color: rgba(255,255,255,.7);
}
.table-wrapper {
  overflow-x: auto;
  padding: 14px;
}
.tech-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  min-width: 1000px;
}
.tech-table th {
  padding: 10px 12px;
  text-align: left;
  font-size: 12px;
  color: rgba(255,255,255,.6);
  border-bottom: 2px solid rgba(0,212,255,.2);
}
.tech-table td {
  padding: 12px 12px;
  font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,.07);
}
.tech-table tbody tr:hover {
  background: rgba(0,212,255,.04);
}
.id-cell {
  color: #00d4ff;
  font-weight: 500;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  flex-shrink: 0;
}
.username {
  font-weight: 500;
  color: #fff;
}
.role-badge {
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
}
.role-badge.admin {
  background: rgba(244,67,54,.2);
  color: #f44336;
  border: 1px solid rgba(244,67,54,.3);
}
.role-badge.teacher {
  background: rgba(255,152,0,.2);
  color: #ff9800;
  border: 1px solid rgba(255,152,0,.3);
}
.role-badge.service {
  background: rgba(33,150,243,.2);
  color: #2196f3;
  border: 1px solid rgba(33,150,243,.3);
}
.role-badge.student {
  background: rgba(76,175,80,.2);
  color: #4caf50;
  border: 1px solid rgba(76,175,80,.3);
}
.role-badge.temporary {
  background: rgba(158,158,158,.2);
  color: #9e9e9e;
  border: 1px solid rgba(158,158,158,.3);
}
.contact-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.contact-email {
  font-size: 11px;
  color: rgba(255,255,255,.4);
}
.status-badge {
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
}
.status-badge.active {
  background: rgba(76,175,80,.2);
  color: #4caf50;
  border: 1px solid rgba(76,175,80,.3);
}
.status-badge.frozen {
  background: rgba(33,150,243,.2);
  color: #2196f3;
  border: 1px solid rgba(33,150,243,.3);
}
.action-cell {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}
.empty-row {
  text-align: center;
  padding: 30px;
  color: rgba(255,255,255,.4);
}
.panel {
  background: rgba(255,255,255,.05);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,.1);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(255,255,255,.1);
  font-size: 15px;
  font-weight: 500;
}
.panel-body {
  padding: 16px 20px;
}
.binding-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill,minmax(260px,1fr));
  gap: 14px;
}
.binding-card {
  background: rgba(0,0,0,.2);
  border: 1px solid rgba(255,255,255,.1);
  border-radius: 10px;
  padding: 16px;
}
.binding-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.counselor-name {
  font-size: 15px;
  font-weight: 600;
  color: #00d4ff;
}
.binding-actions {
  display: flex;
  gap: 6px;
}
.binding-classes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.class-tag {
  padding: 3px 10px;
  background: rgba(0,212,255,.15);
  border: 1px solid rgba(0,212,255,.3);
  border-radius: 10px;
  font-size: 12px;
  color: #00d4ff;
}
.binding-stats {
  font-size: 12px;
  color: rgba(255,255,255,.5);
}
.add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  border-style: dashed;
  cursor: pointer;
  color: rgba(255,255,255,.4);
  transition: all .2s;
  min-height: 100px;
}
.add-card:hover {
  border-color: rgba(0,212,255,.4);
  color: #00d4ff;
}
.add-icon {
  font-size: 24px;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content {
  background: linear-gradient(135deg,#1a1f3c,#2d3250);
  border-radius: 16px;
  border: 1px solid rgba(0,123,255,.3);
  width: 560px;
}
.modal-content.sm {
  width: 440px;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(255,255,255,.1);
}
.modal-title {
  font-size: 16px;
  font-weight: 600;
}
.modal-close {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,.1);
  border-radius: 50%;
  cursor: pointer;
}
.modal-body {
  padding: 20px 24px;
  max-height: 60vh;
  overflow-y: auto;
}
.modal-footer {
  padding: 14px 24px;
  border-top: 1px solid rgba(255,255,255,.1);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.form-label {
  font-size: 12px;
  color: rgba(255,255,255,.6);
}
.tech-select-full {
  width: 100%;
  padding: 9px 12px;
  background: rgba(0,0,0,.3);
  border: 1px solid rgba(255,255,255,.2);
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
}
.tech-select-full:focus {
  outline: none;
  border-color: #00d4ff;
}
.role-user-info {
  background: rgba(0,0,0,.2);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}
.role-username {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
}
.role-current {
  font-size: 13px;
  color: rgba(255,255,255,.6);
  display: flex;
  align-items: center;
  gap: 8px;
}
.role-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.role-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(0,0,0,.2);
  border: 1px solid rgba(255,255,255,.1);
  border-radius: 10px;
  cursor: pointer;
  transition: all .2s;
}
.role-option:hover {
  background: rgba(0,212,255,.07);
  border-color: rgba(0,212,255,.2);
}
.role-option.active {
  background: rgba(0,212,255,.15);
  border-color: rgba(0,212,255,.4);
}
.role-opt-icon {
  font-size: 20px;
  width: 30px;
  text-align: center;
}
.role-opt-info {
  flex: 1;
}
.role-opt-name {
  font-size: 14px;
  font-weight: 500;
}
.role-opt-desc {
  font-size: 11px;
  color: rgba(255,255,255,.5);
  margin-top: 2px;
}
.role-check {
  color: #00d4ff;
  font-size: 16px;
  font-weight: bold;
}

/* 车辆模块样式 (从第二个组件复制) */
.vehicle-management-container { /* 此处原为容器，现作为模块内样式，但类名可能冲突，但 scoped 会隔离 */ }
.page-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.stat-divider {
  width: 1px;
  height: 30px;
  background: rgba(255,255,255,0.2);
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
.search-form {
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255,255,255,0.1);
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
  color: rgba(255,255,255,0.8);
}
.form-input {
  position: relative;
}
.tech-input, .tech-select {
  width: 100%;
  padding: 12px;
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  color: white;
  font-size: 14px;
  transition: all 0.3s ease;
}
.tech-input:focus, .tech-select:focus {
  outline: none;
  border-color: #00d4ff;
}
.tech-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='white' viewBox='0 0 16 16'%3E%3Cpath d='M7.247 11.14L2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
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
  padding: 8px 12px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.status-option.active {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.3);
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-text {
  font-size: 12px;
  color: rgba(255,255,255,0.8);
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
  color: rgba(255,255,255,0.6);
}
.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}
.search-btn {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: linear-gradient(90deg, #007bff, #6610f2);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.search-btn:hover {
  transform: translateY(-2px);
}
.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  animation: glow 2s infinite;
}
@keyframes glow {
  0% { left: -100%; }
  50% { left: 100%; }
  100% { left: 100%; }
}
.btn-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.reset-btn {
  padding: 12px 20px;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  font-size: 14px;
}
.reset-btn:hover {
  background: rgba(255,255,255,0.15);
}
.stats-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.stat-card {
  position: relative;
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  overflow: hidden;
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
  color: rgba(255,255,255,0.6);
}
.card-wave {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(0,212,255,0.1) 0%, transparent 70%);
  border-radius: 50%;
}
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.table-container {
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.1);
  overflow: hidden;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: rgba(255,255,255,0.05);
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.table-title {
  font-size: 15px;
  font-weight: 500;
}
.table-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}
.total-info {
  font-size: 14px;
  color: rgba(255,255,255,0.6);
}
.page-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.page-btn:hover:not(.disabled) {
  background: rgba(0,212,255,0.2);
  border-color: #00d4ff;
}
.page-btn.disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.page-info {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}
.table-wrapper {
  overflow-x: auto;
  padding: 14px;
}
.tech-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  min-width: 1200px;
}
.tech-table th {
  padding: 11px 12px;
  text-align: left;
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  border-bottom: 2px solid rgba(0,212,255,0.2);
}
.tech-table td {
  padding: 12px 12px;
  font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
}
.tech-table tbody tr:hover {
  background: rgba(0,212,255,0.04);
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
  background: rgba(255,255,255,0.1);
  border: 2px solid rgba(255,255,255,0.3);
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
.vehicle-image-cell {
  cursor: pointer;
}
.image-container {
  width: 60px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(0,0,0,0.2);
}
.vehicle-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.vehicle-image:hover {
  transform: scale(1.1);
}
.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.3);
  font-size: 12px;
}
.no-image-icon {
  font-size: 16px;
  margin-bottom: 2px;
}
.no-image-text {
  font-size: 10px;
}
.id-cell {
  color: #00d4ff;
  font-weight: 500;
  font-size: 14px;
}
.license-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.license-plate {
  font-weight: bold;
  font-size: 16px;
  color: #ffffff;
}
.blacklist-tag {
  background: rgba(244,67,54,0.2);
  color: #f44336;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  border: 1px solid rgba(244,67,54,0.4);
}
.owner-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.owner-name {
  font-weight: 500;
  color: #ffffff;
}
.owner-type {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
}
.owner-type.student {
  background: rgba(0,123,255,0.2);
  color: #007bff;
  border: 1px solid rgba(0,123,255,0.4);
}
.owner-type.teacher {
  background: rgba(40,167,69,0.2);
  color: #28a745;
  border: 1px solid rgba(40,167,69,0.4);
}
.owner-type.service {
  background: rgba(255,193,7,0.2);
  color: #ffc107;
  border: 1px solid rgba(255,193,7,0.4);
}
.owner-id {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
}
.vehicle-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.vehicle-brand {
  font-weight: 500;
  color: #ffffff;
}
.vehicle-detail {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
}
.hardware-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.hardware-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hardware-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}
.hardware-icon.正常,
.hardware-icon.在线 {
  color: #4caf50;
}
.hardware-icon.离线 {
  color: #f44336;
}
.hardware-icon.battery.low {
  color: #ff9800;
}
.hardware-text {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
}
.register-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.register-time {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
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
.status-cell.pending {
  background: rgba(255,152,0,0.2);
  color: #ff9800;
  border: 1px solid rgba(255,152,0,0.4);
}
.status-cell.approved {
  background: rgba(76,175,80,0.2);
  color: #4caf50;
  border: 1px solid rgba(76,175,80,0.4);
}
.status-cell.rejected {
  background: rgba(244,67,54,0.2);
  color: #f44336;
  border: 1px solid rgba(244,67,54,0.4);
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.violation-count-cell {
  text-align: center;
}
.violation-count {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}
.violation-count.high {
  color: #f44336;
}
.action-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.more-actions {
  position: relative;
}
.more-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.more-btn:hover {
  background: rgba(255,255,255,0.15);
}
.more-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: rgba(26,31,60,0.95);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  padding: 8px 0;
  min-width: 120px;
  z-index: 100;
  backdrop-filter: blur(10px);
}
.menu-item {
  padding: 8px 16px;
  font-size: 12px;
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  transition: all 0.3s ease;
}
.menu-item:hover {
  background: rgba(255,255,255,0.1);
  color: #00d4ff;
}
.menu-item.delete {
  color: #f44336;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.table-footer {
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.selected-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: rgba(255,255,255,0.8);
  font-size: 14px;
}
.batch-actions {
  display: flex;
  gap: 8px;
}
.batch-btn {
  padding: 8px 16px;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}
.batch-btn:hover:not(.disabled) {
  background: rgba(255,255,255,0.15);
  transform: translateY(-1px);
}
.batch-btn.disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.overview-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.overview-card {
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255,255,255,0.1);
}
.overview-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.overview-icon {
  font-size: 20px;
}
.overview-title {
  font-size: 16px;
  font-weight: 500;
}
.distribution-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.distribution-label {
  width: 60px;
  font-size: 13px;
  color: rgba(255,255,255,0.8);
}
.distribution-bar {
  flex: 1;
  height: 8px;
  background: rgba(255,255,255,0.1);
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  transition: width 0.3s ease;
}
.distribution-value {
  width: 60px;
  text-align: right;
  font-size: 13px;
  color: rgba(255,255,255,0.6);
}
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}
.drawer {
  width: 560px;
  background: linear-gradient(135deg, #1a1f3c, #0d1228);
  border-left: 1px solid rgba(0,123,255,0.3);
  display: flex;
  flex-direction: column;
  height: 100%;
  box-shadow: -10px 0 40px rgba(0,0,0,0.5);
}
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.drawer-title {
  font-size: 17px;
  font-weight: 600;
}
.drawer-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.1);
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
}
.drawer-close:hover {
  background: rgba(255,255,255,0.2);
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.drawer-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.archive-section {
  background: rgba(0,0,0,0.2);
  border-radius: 10px;
  padding: 16px;
}
.section-title {
  font-size: 13px;
  color: #00d4ff;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(0,212,255,0.2);
  font-weight: 500;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.info-item {
  background: rgba(255,255,255,0.04);
  border-radius: 6px;
  padding: 10px 12px;
}
.il {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  margin-bottom: 4px;
}
.iv {
  font-size: 13px;
  color: #fff;
  font-weight: 500;
}
.no-data-tip {
  color: rgba(255,255,255,0.4);
  font-size: 13px;
  text-align: center;
  padding: 12px;
}
.gps-badge {
  font-size: 12px;
}
.status-badge {
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
}
.status-badge.approved {
  background: rgba(76,175,80,0.2);
  color: #4caf50;
  border: 1px solid rgba(76,175,80,0.4);
}
.violation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255,255,255,0.07);
}
.vi-type {
  flex: 1;
  font-size: 13px;
}
.vi-time {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  width: 100px;
}
.vi-state {
  width: 70px;
}
.state-badge {
  padding: 3px 8px;
  border-radius: 10px;
  font-size: 11px;
}
.state-0 {
  background: rgba(255,152,0,0.2);
  color: #ff9800;
  border: 1px solid rgba(255,152,0,0.3);
}
.state-2 {
  background: rgba(76,175,80,0.2);
  color: #4caf50;
  border: 1px solid rgba(76,175,80,0.3);
}
.state-3 {
  background: rgba(244,67,54,0.2);
  color: #f44336;
  border: 1px solid rgba(244,67,54,0.3);
}
.vi-points {
  font-size: 13px;
  color: #f44336;
  font-weight: bold;
  width: 40px;
  text-align: right;
}
.edit-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.edit-modal .modal-content {
  background: linear-gradient(135deg, #1a1f3c 0%, #0a0e29 100%);
  border-radius: 16px;
  border: 1px solid rgba(0,123,255,0.3);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
}
.edit-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.edit-modal .modal-header h3 {
  margin: 0;
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
}
.edit-modal .close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.1);
  border-radius: 50%;
  cursor: pointer;
  font-size: 20px;
  color: rgba(255,255,255,0.6);
  transition: all 0.3s ease;
}
.edit-modal .close-btn:hover {
  background: rgba(255,255,255,0.2);
  color: #ffffff;
}
.edit-modal .modal-body {
  padding: 20px;
  overflow-y: auto;
  max-height: calc(90vh - 130px);
}
.edit-form .form-group {
  margin-bottom: 16px;
}
.edit-form .form-label {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  margin-bottom: 4px;
  display: block;
}
.field-note {
  font-size: 12px;
  color: rgba(255,255,255,0.5);
  margin-top: 4px;
}
.photo-upload-area {
  border: 1px dashed rgba(255,255,255,0.2);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255,255,255,0.02);
}
.current-photo {
  display: flex;
  align-items: center;
  gap: 16px;
}
.preview-thumb {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid rgba(255,255,255,0.2);
}
.photo-actions {
  display: flex;
  gap: 12px;
}
.photo-btn {
  padding: 6px 12px;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.photo-btn:hover {
  background: rgba(255,255,255,0.15);
}
.no-photo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.no-photo:hover {
  background: rgba(255,255,255,0.05);
}
.upload-icon {
  font-size: 32px;
  color: rgba(255,255,255,0.3);
}
.upload-text {
  font-size: 14px;
  color: rgba(255,255,255,0.6);
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.cancel-btn {
  padding: 10px 24px;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  transition: all 0.3s ease;
}
.confirm-btn {
  padding: 10px 24px;
  background: linear-gradient(90deg, #007bff, #6610f2);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  transition: all 0.3s ease;
}
.confirm-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cancel-btn:hover {
  background: rgba(255,255,255,0.15);
}
.confirm-btn:hover:not(.disabled) {
  transform: translateY(-2px);
}
/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: rgba(255,255,255,0.05);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb {
  background: rgba(0,212,255,0.3);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(0,212,255,0.5);
}
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  .left-panel {
    width: 100%;
  }
  .overview-cards {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  .table-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .selected-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .batch-actions {
    flex-wrap: wrap;
  }
}
</style>

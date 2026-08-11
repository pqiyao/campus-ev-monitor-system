<template>
  <div class="vehicle-container">
    <!-- 顶部标签 -->
    <div class="top-tabs">
      <div v-for="tab in topTabs" :key="tab.value"
        class="top-tab" :class="{ active: currentTopTab === tab.value }"
        @click="currentTopTab = tab.value">
        <span class="tab-icon">{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
      </div>
    </div>

    <!-- ===================== 模块1：管理员-电动车审核管理 ===================== -->
    <div v-if="currentTopTab === 'admin'" class="module">
      <div class="page-header">
        <div class="header-bg">
          <div class="header-left">
            <h1 class="title-text">电动车智能注册管理</h1>
            <div class="header-stats">
              <div class="stat-item"><span class="stat-num">{{ adminStats.total }}</span><span class="stat-lab">总注册</span></div>
              <div class="stat-div"></div>
              <div class="stat-item"><span class="stat-num c-green">{{ adminStats.approved }}</span><span class="stat-lab">已通过</span></div>
              <div class="stat-div"></div>
              <div class="stat-item"><span class="stat-num c-orange">{{ adminStats.pending }}</span><span class="stat-lab">待审核</span></div>
              <div class="stat-div"></div>
              <div class="stat-item"><span class="stat-num c-red">{{ adminStats.blacklist }}</span><span class="stat-lab">黑名单</span></div>
            </div>
          </div>
          <div class="header-actions">
            <div class="action-btn" @click="fetchAdminVehicles">↻ 刷新</div>
            <div class="action-btn primary" @click="openRegisterDialog">+ 新增注册</div>
          </div>
        </div>
      </div>

      <!-- 主内容：左右布局 -->
      <div class="main-content">
        <!-- 左侧筛选 -->
        <div class="left-panel">
          <div class="search-form">
            <div class="form-group">
              <label class="form-label">车牌号码</label>
              <input v-model="adminSearch.licensePlate" class="tech-input" placeholder="输入车牌号" />
            </div>
            <div class="form-group">
              <label class="form-label">车主类型</label>
              <select v-model="adminSearch.userType" class="tech-select">
                <option value="">全部类型</option>
                <option value="student">学生</option>
                <option value="teacher">教职工</option>
                <option value="service">后勤人员</option>
                <option value="temporary">临时人员</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">注册状态</label>
              <div class="status-selector">
                <div v-for="s in statusOptions" :key="s.value"
                  class="status-opt" :class="{ active: adminSearch.status === s.value }"
                  @click="adminSearch.status = adminSearch.status === s.value ? '' : s.value">
                  <span class="sdot" :style="{ background: s.color }"></span>
                  <span>{{ s.label }}</span>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">注册时间</label>
              <div class="date-range">
                <input type="date" v-model="adminSearch.startTime" class="tech-input" />
                <span class="date-sep">至</span>
                <input type="date" v-model="adminSearch.endTime" class="tech-input" />
              </div>
            </div>
            <div class="form-actions">
              <div class="search-btn" @click="fetchAdminVehicles">🔍 搜索车辆</div>
              <div class="reset-btn" @click="resetAdminSearch">重置</div>
            </div>
          </div>

          <!-- 快速统计卡片 -->
          <div class="quick-stats">
            <div class="qstat-card" v-for="qs in quickStats" :key="qs.label">
              <div class="qstat-icon" :style="{ background: qs.bg }">{{ qs.icon }}</div>
              <div class="qstat-info">
                <div class="qstat-val">{{ qs.value }}</div>
                <div class="qstat-lab">{{ qs.label }}</div>
              </div>
            </div>
          </div>

          <!-- 车辆分布图 -->
          <div class="dist-card">
            <div class="dist-title">📊 车辆分布</div>
            <div v-for="item in vehicleDistribution" :key="item.type" class="dist-item">
              <span class="dist-label">{{ item.type }}</span>
              <div class="dist-bar"><div class="dist-fill" :style="{ width: item.percentage + '%', background: item.color }"></div></div>
              <span class="dist-val">{{ item.count }}辆</span>
            </div>
          </div>
        </div>

        <!-- 右侧表格 -->
        <div class="right-panel">
          <div class="table-container">
            <div class="table-header">
              <span class="table-title">车辆信息列表</span>
              <div class="table-actions">
                <span class="total-info">共 {{ adminPagination.total }} 条</span>
                <div class="page-controls">
                  <div class="page-btn" :class="{ disabled: adminPagination.page === 1 }" @click="adminChangePage(-1)">←</div>
                  <span class="page-info">{{ adminPagination.page }} / {{ adminPagination.totalPages }}</span>
                  <div class="page-btn" :class="{ disabled: adminPagination.page === adminPagination.totalPages }" @click="adminChangePage(1)">→</div>
                </div>
              </div>
            </div>
            <div class="table-wrapper">
              <table class="tech-table">
                <thead>
                  <tr>
                    <th><input type="checkbox" class="tech-checkbox" :checked="adminAllSelected" @change="toggleAdminSelectAll" /></th>
                    <th>图片</th><th>ID</th><th>车牌号</th><th>车主信息</th>
                    <th>车辆信息</th><th>注册信息</th><th>状态</th><th>违规</th><th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="v in adminVehicleList" :key="v.vehicleId">
                    <td><input type="checkbox" class="tech-checkbox" :checked="isAdminSelected(v)" @change="toggleAdminSelect(v)" /></td>
                    <td>
                      <div class="vimg-cell" @click="previewImage(v.vehiclePhoto)">
                        <img v-if="v.vehiclePhoto" :src="getFullUrl(v.vehiclePhoto)" class="vehicle-thumb" @error="handleImgError" />
                        <div v-else class="no-img">📷</div>
                      </div>
                    </td>
                    <td><span class="id-badge">#{{ v.vehicleId }}</span></td>
                    <td>
                      <div class="plate-cell">
                        <span class="plate-no">{{ v.licensePlate }}</span>
                        <span v-if="v.isBlacklist" class="black-tag">黑名单</span>
                      </div>
                    </td>
                    <td>
                      <div class="owner-cell">
                        <div class="owner-name">{{ v.ownerName || v.realName || '—' }}</div>
                        <div class="owner-type" :class="v.userType">{{ getUserTypeText(v.userType) }}</div>
                        <div class="owner-id">{{ v.studentId || v.jobId || v.userId }}</div>
                      </div>
                    </td>
                    <td>
                      <div class="vinfo-cell">
                        <div>{{ v.brand }} · {{ v.color }}</div>
                        <div class="vinfo-sub">车架: {{ v.frameNum }}</div>
                        <div class="vinfo-sub">电池: {{ v.batteryType || '未知' }}</div>
                      </div>
                    </td>
                    <td>
                      <div class="reg-cell">
                        <div>注册: {{ formatDate(v.registerTime) }}</div>
                        <div>审核: {{ v.auditTime ? formatDate(v.auditTime) : '待审核' }}</div>
                        <div>有效: {{ formatDate(v.validDate) || '长期' }}</div>
                      </div>
                    </td>
                    <td>
                      <span class="status-badge" :class="v.status">{{ getStatusText(v.status) }}</span>
                    </td>
                    <td>
                      <span class="violation-cnt" :class="{ high: (violationCountMap[v.vehicleId] || 0) > 3 }">
                        {{ violationCountMap[v.vehicleId] || 0 }}
                      </span>
                    </td>
                    <td>
                      <div class="action-cell">
                        <div class="action-btn small" @click="viewAdminArchive(v)">📂 档案</div>
                        <div v-if="v.status === 'pending'" class="action-btn small success" @click="approveVehicle(v)">✓ 通过</div>
                        <div v-if="v.status === 'pending'" class="action-btn small danger" @click="rejectVehicle(v)">✗ 拒绝</div>
                        <div class="action-btn small" @click="uploadVehiclePhoto(v)">📷 图片</div>
                        <div class="more-wrap">
                          <div class="more-btn" @click="toggleMore(v)">⋮</div>
                          <div v-if="v.showMore" class="more-menu">
                            <div class="menu-item" @click="editVehicle(v)">编辑信息</div>
                            <div class="menu-item" @click="sendVehicleReminder(v)">发送提醒</div>
                            <div v-if="v.status === 'approved'" class="menu-item" @click="toggleFreezeVehicle(v)">
                              {{ v.isFrozen ? '解冻车辆' : '冻结车辆' }}
                            </div>
                            <div v-if="v.status === 'approved'" class="menu-item" @click="toggleBlacklist(v)">
                              {{ v.isBlacklist ? '移出黑名单' : '加入黑名单' }}
                            </div>
                            <div class="menu-item danger" @click="deleteVehicle(v)">注销车辆</div>
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="adminVehicleList.length === 0">
                    <td colspan="10" class="empty-row">暂无车辆数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <!-- 批量操作 -->
            <div v-if="adminSelected.length > 0" class="batch-bar">
              <span>已选 {{ adminSelected.length }} 辆</span>
              <div class="batch-actions">
                <div class="batch-btn" :class="{ disabled: !hasPending }" @click="batchApprove">批量通过</div>
                <div class="batch-btn" :class="{ disabled: !hasPending }" @click="batchReject">批量拒绝</div>
                <div class="batch-btn" :class="{ disabled: !hasApproved }" @click="batchFreeze">批量冻结</div>
                <div class="batch-btn" @click="adminSelected = []">清空</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===================== 模块2：学生-我的车辆管理 ===================== -->
    <div v-if="currentTopTab === 'student'" class="module">
      <div class="page-header">
        <div class="header-bg">
          <div class="header-left">
            <h1 class="title-text">我的车辆管理</h1>
            <p class="title-sub">管理您的电动车注册信息及整改记录</p>
          </div>
          <div class="header-actions">
            <div class="action-btn primary" @click="openStudentRegisterDialog">+ 申请注册</div>
          </div>
        </div>
      </div>

      <!-- 学生车辆卡片 -->
      <div class="student-vehicles">
        <div v-if="studentVehicles.length === 0" class="empty-state">
          <div class="empty-icon">🛵</div>
          <div class="empty-text">暂无注册车辆</div>
          <div class="action-btn primary" @click="openStudentRegisterDialog" style="margin-top:16px">立即注册电动车</div>
        </div>

        <div v-for="v in studentVehicles" :key="v.vehicleId" class="vehicle-card">
          <div class="vcard-header">
            <div class="vcard-photo">
              <img v-if="v.vehiclePhoto" :src="getFullUrl(v.vehiclePhoto)" class="vcard-img" />
              <div v-else class="vcard-no-img">🛵</div>
            </div>
            <div class="vcard-info">
              <div class="vcard-plate">{{ v.licensePlate }}</div>
              <div class="vcard-brand">{{ v.brand }} · {{ v.color }}</div>
              <div class="vcard-type">{{ v.vehicleType || '电动自行车' }}</div>
            </div>
            <div class="vcard-status">
              <span class="status-badge" :class="v.status">{{ getStatusText(v.status) }}</span>
              <div v-if="v.isFrozen" class="frozen-badge">已冻结</div>
            </div>
          </div>

          <div class="vcard-body">
            <div class="vcard-detail-grid">
              <div class="vd-item"><span class="vd-label">车架号</span><span class="vd-val">{{ v.frameNum }}</span></div>
              <div class="vd-item"><span class="vd-label">电池类型</span><span class="vd-val">{{ v.batteryType || '—' }}</span></div>
              <div class="vd-item"><span class="vd-label">注册时间</span><span class="vd-val">{{ formatDate(v.registerTime) }}</span></div>
              <div class="vd-item"><span class="vd-label">有效期至</span><span class="vd-val">{{ formatDate(v.validDate) || '长期' }}</span></div>
            </div>

            <!-- 违规记录摘要 -->
            <div class="vcard-violations" v-if="studentViolationMap[v.vehicleId]?.length > 0">
              <div class="vvio-title">⚠️ 违规记录 ({{ studentViolationMap[v.vehicleId]?.length }})</div>
              <div v-for="vio in (studentViolationMap[v.vehicleId] || []).slice(0,3)" :key="vio.illegalId" class="vvio-item">
                <span class="vvio-type">{{ getViolationTypeName(vio.illegalType) }}</span>
                <span class="vvio-time">{{ formatDate(vio.illegalTime) }}</span>
                <span class="vvio-state" :class="'state-' + vio.illegalState">{{ getViolationStateName(vio.illegalState) }}</span>
                <div v-if="vio.illegalState === 0 || vio.illegalState === 1" class="vvio-action">
                  <div class="action-btn small" @click="openRectifyDialog(vio)">📝 整改</div>
                  <div class="action-btn small" @click="openAppealDialog(vio)">📣 申诉</div>
                </div>
              </div>
            </div>
          </div>

          <div class="vcard-footer">
            <div class="action-btn small" @click="viewStudentDetail(v)">📋 详情</div>
            <div v-if="v.status === 'rejected' || v.status === 'pending'" class="action-btn small warn" @click="editStudentVehicle(v)">✏️ 修改申请</div>
            <div class="action-btn small" @click="uploadStudentPhoto(v)">📷 更新图片</div>
          </div>
        </div>
      </div>

      <!-- 注册步骤弹窗 -->
      <div v-if="studentRegDialog.visible" class="modal-overlay" @click.self="studentRegDialog.visible = false">
        <div class="modal-content large" @click.stop>
          <div class="modal-header">
            <span class="modal-title">🛵 电动车注册申请</span>
            <span class="modal-close" @click="studentRegDialog.visible = false">×</span>
          </div>
          <!-- 步骤条 -->
          <div class="steps-bar">
            <div v-for="(step, i) in regSteps" :key="i" class="step-item" :class="{ active: studentRegDialog.step >= i, done: studentRegDialog.step > i }">
              <div class="step-dot">{{ i + 1 }}</div>
              <div class="step-text">{{ step }}</div>
            </div>
          </div>

          <div class="modal-body">
            <!-- 步骤0：基本信息 -->
            <div v-if="studentRegDialog.step === 0" class="form-grid">
              <div class="form-item">
                <label class="form-label">车牌号码 *</label>
                <input v-model="studentRegForm.licensePlate" class="tech-input" placeholder="格式：湘A·12345" />
                <div v-if="plateCheckResult !== null" :class="plateCheckResult ? 'hint-ok' : 'hint-err'">
                  {{ plateCheckResult ? '✓ 车牌可用' : '✗ 车牌已注册' }}
                </div>
              </div>
              <div class="form-item">
                <label class="form-label">车架号 *</label>
                <input v-model="studentRegForm.frameNum" class="tech-input" placeholder="车辆唯一识别码" />
              </div>
              <div class="form-item">
                <label class="form-label">定位器 MAC</label>
                <input v-model="studentRegForm.deviceMac" class="tech-input" placeholder="终端MAC，与 MQTT 字段 D 一致（可选）" />
                <div class="form-tip" style="font-size:11px;opacity:.7;margin-top:4px">上报主题示例：ev/location/{MAC} 或固定 ev/location</div>
              </div>
              <div class="form-item">
                <label class="form-label">车辆品牌 *</label>
                <select v-model="studentRegForm.brand" class="tech-select-full">
                  <option value="雅迪">雅迪</option><option value="爱玛">爱玛</option>
                  <option value="小牛">小牛</option><option value="九号">九号</option>
                  <option value="台铃">台铃</option><option value="新日">新日</option>
                  <option value="绿源">绿源</option><option value="其他">其他</option>
                </select>
              </div>
              <div class="form-item">
                <label class="form-label">车辆颜色 *</label>
                <select v-model="studentRegForm.color" class="tech-select-full">
                  <option value="白色">白色</option><option value="黑色">黑色</option>
                  <option value="红色">红色</option><option value="蓝色">蓝色</option>
                  <option value="灰色">灰色</option><option value="绿色">绿色</option>
                  <option value="黄色">黄色</option><option value="其他">其他</option>
                </select>
              </div>
              <div class="form-item">
                <label class="form-label">电池类型</label>
                <select v-model="studentRegForm.batteryType" class="tech-select-full">
                  <option value="锂电池">锂电池</option><option value="铅酸电池">铅酸电池</option>
                  <option value="石墨烯电池">石墨烯电池</option><option value="其他">其他</option>
                </select>
              </div>
              <div class="form-item">
                <label class="form-label">车辆类型</label>
                <select v-model="studentRegForm.vehicleType" class="tech-select-full">
                  <option value="电动自行车">电动自行车</option><option value="电动摩托车">电动摩托车</option>
                  <option value="电动三轮车">电动三轮车</option>
                </select>
              </div>
            </div>

            <!-- 步骤1：上传照片 -->
            <div v-if="studentRegDialog.step === 1">
              <div class="upload-zone"
                @dragover.prevent="regDragOver = true"
                @dragleave="regDragOver = false"
                @drop.prevent="onRegFileDrop"
                :class="{ 'drag-over': regDragOver }">
                <div v-if="!studentRegForm.photoPreview" class="upload-placeholder">
                  <div class="upload-icon-big">📷</div>
                  <div>拖放图片到此处或</div>
                  <input type="file" ref="regFileInput" accept="image/*" @change="onRegPhotoSelect" style="display:none" />
                  <div class="action-btn primary" @click="$refs.regFileInput.click()">选择图片</div>
                  <div class="upload-tips">支持 JPG、PNG，不超过 10MB</div>
                </div>
                <div v-else class="upload-preview">
                  <img :src="studentRegForm.photoPreview" class="preview-large" />
                  <div class="action-btn" @click="studentRegForm.photoPreview = ''; studentRegForm.vehiclePhoto = null" style="margin-top:10px">重新选择</div>
                </div>
              </div>
            </div>

            <!-- 步骤2：确认提交 -->
            <div v-if="studentRegDialog.step === 2">
              <div class="review-section">
                <div class="review-block">
                  <h5>车辆信息确认</h5>
                  <div class="review-grid">
                    <div class="review-item"><span class="rl">车牌号</span><span class="rv">{{ studentRegForm.licensePlate }}</span></div>
                    <div class="review-item"><span class="rl">车架号</span><span class="rv">{{ studentRegForm.frameNum }}</span></div>
                    <div class="review-item"><span class="rl">定位器MAC</span><span class="rv">{{ studentRegForm.deviceMac || '—' }}</span></div>
                    <div class="review-item"><span class="rl">品牌</span><span class="rv">{{ studentRegForm.brand }}</span></div>
                    <div class="review-item"><span class="rl">颜色</span><span class="rv">{{ studentRegForm.color }}</span></div>
                    <div class="review-item"><span class="rl">电池</span><span class="rv">{{ studentRegForm.batteryType }}</span></div>
                    <div class="review-item"><span class="rl">类型</span><span class="rv">{{ studentRegForm.vehicleType }}</span></div>
                    <div class="review-item"><span class="rl">照片</span><span class="rv">{{ studentRegForm.photoPreview ? '已上传' : '未上传' }}</span></div>
                  </div>
                </div>
                <div class="agreement-check" :class="{ active: studentRegDialog.agreed }" @click="studentRegDialog.agreed = !studentRegDialog.agreed">
                  <div class="check-box"></div>
                  <span>我已阅读并同意 <span class="agree-link">《校园电动车管理协议》</span></span>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <div class="action-btn" v-if="studentRegDialog.step > 0" @click="studentRegDialog.step--">← 上一步</div>
            <div class="action-btn" @click="studentRegDialog.visible = false">取消</div>
            <div class="action-btn primary" v-if="studentRegDialog.step < 2" @click="nextRegStep">下一步 →</div>
            <div class="action-btn success" v-if="studentRegDialog.step === 2"
              :class="{ disabled: !studentRegDialog.agreed || studentRegDialog.submitting }"
              @click="submitStudentRegister">
              {{ studentRegDialog.submitting ? '提交中...' : '提交申请' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 整改提交弹窗 -->
      <div v-if="rectifyDialog.visible" class="modal-overlay" @click.self="rectifyDialog.visible = false">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <span class="modal-title">📝 提交整改</span>
            <span class="modal-close" @click="rectifyDialog.visible = false">×</span>
          </div>
          <div class="modal-body">
            <div class="info-block">
              <div class="info-row"><span class="il">违规类型</span><span>{{ getViolationTypeName(rectifyDialog.violation?.illegalType) }}</span></div>
              <div class="info-row"><span class="il">违规时间</span><span>{{ formatDate(rectifyDialog.violation?.illegalTime) }}</span></div>
            </div>
            <div class="form-item" style="margin-top:16px">
              <label class="form-label">整改说明 *</label>
              <textarea v-model="rectifyDialog.desc" class="tech-textarea" rows="3" placeholder="请详细说明整改情况..."></textarea>
            </div>
            <div class="form-item" style="margin-top:12px">
              <label class="form-label">整改照片（可选）</label>
              <div class="mini-upload" @click="$refs.rectifyInput.click()">
                <input type="file" ref="rectifyInput" accept="image/*" @change="onRectifyPhoto" style="display:none" />
                <div v-if="rectifyDialog.photoPreview">
                  <img :src="rectifyDialog.photoPreview" class="mini-preview" />
                </div>
                <div v-else class="mini-placeholder">📷 点击上传</div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <div class="action-btn" @click="rectifyDialog.visible = false">取消</div>
            <div class="action-btn primary" @click="submitRectify">提交整改</div>
          </div>
        </div>
      </div>

      <!-- 申诉弹窗 -->
      <div v-if="appealDialog.visible" class="modal-overlay" @click.self="appealDialog.visible = false">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <span class="modal-title">📣 提交申诉</span>
            <span class="modal-close" @click="appealDialog.visible = false">×</span>
          </div>
          <div class="modal-body">
            <div class="info-block">
              <div class="info-row"><span class="il">违规ID</span><span>#{{ appealDialog.violation?.illegalId }}</span></div>
              <div class="info-row"><span class="il">违规类型</span><span>{{ getViolationTypeName(appealDialog.violation?.illegalType) }}</span></div>
            </div>
            <div class="form-item" style="margin-top:16px">
              <label class="form-label">申诉理由 *</label>
              <textarea v-model="appealDialog.reason" class="tech-textarea" rows="4" placeholder="请详细说明申诉理由..."></textarea>
            </div>
            <div class="form-item" style="margin-top:12px">
              <label class="form-label">申诉证据（可选）</label>
              <input v-model="appealDialog.evidence" class="tech-input" placeholder="图片URL，多张用逗号分隔" />
            </div>
          </div>
          <div class="modal-footer">
            <div class="action-btn" @click="appealDialog.visible = false">取消</div>
            <div class="action-btn primary" @click="submitAppeal">提交申诉</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===================== 车辆档案抽屉 ===================== -->
    <div v-if="archiveDrawer.visible" class="drawer-overlay" @click.self="archiveDrawer.visible = false">
      <div class="drawer" @click.stop>
        <div class="drawer-header">
          <span class="drawer-title">📂 车辆全档案</span>
          <span class="drawer-close" @click="archiveDrawer.visible = false">×</span>
        </div>
        <div class="drawer-body" v-if="archiveDrawer.vehicle">
          <div class="archive-section">
            <div class="sec-title">基本信息</div>
            <div class="info-grid-2">
              <div class="info-item"><div class="il">姓名</div><div class="iv">{{ archiveDrawer.vehicle.ownerName || archiveDrawer.vehicle.realName }}</div></div>
              <div class="info-item"><div class="il">类型</div><div class="iv">{{ getUserTypeText(archiveDrawer.vehicle.userType) }}</div></div>
              <div class="info-item"><div class="il">学号/工号</div><div class="iv">{{ archiveDrawer.vehicle.studentId || archiveDrawer.vehicle.jobId || '—' }}</div></div>
              <div class="info-item"><div class="il">手机</div><div class="iv">{{ archiveDrawer.vehicle.phone }}</div></div>
              <div class="info-item"><div class="il">车牌号</div><div class="iv"><b>{{ archiveDrawer.vehicle.licensePlate }}</b></div></div>
              <div class="info-item"><div class="il">品牌/颜色</div><div class="iv">{{ archiveDrawer.vehicle.brand }} · {{ archiveDrawer.vehicle.color }}</div></div>
              <div class="info-item"><div class="il">车架号</div><div class="iv">{{ archiveDrawer.vehicle.frameNum }}</div></div>
              <div class="info-item"><div class="il">电池类型</div><div class="iv">{{ archiveDrawer.vehicle.batteryType || '—' }}</div></div>
            </div>
          </div>
          <div class="archive-section">
            <div class="sec-title">近期违规记录</div>
            <div v-if="archiveDrawer.violations.length">
              <div class="vio-item" v-for="vi in archiveDrawer.violations" :key="vi.illegalId">
                <span class="vio-type">{{ getViolationTypeName(vi.illegalType) }}</span>
                <span class="vio-time">{{ formatDate(vi.illegalTime) }}</span>
                <span class="state-badge" :class="'state-' + vi.illegalState">{{ getViolationStateName(vi.illegalState) }}</span>
                <span class="vio-pts">-{{ vi.deductionPoints }}分</span>
              </div>
            </div>
            <div v-else class="no-data">暂无违规记录</div>
          </div>
        </div>
        <div class="drawer-footer">
          <div class="action-btn primary" @click="sendVehicleReminder(archiveDrawer.vehicle)">🔔 发送提醒</div>
          <div class="action-btn" @click="archiveDrawer.visible = false">关闭</div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <div v-if="previewUrl" class="img-overlay" @click="previewUrl = ''">
      <img :src="previewUrl" class="img-preview" />
      <span class="img-close" @click="previewUrl = ''">×</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const API = 'http://localhost:8083/api'
axios.defaults.headers.common['Authorization'] = `Bearer ${localStorage.getItem('token')}`

// ==================== 全局 ====================
const currentTopTab = ref('admin')
const topTabs = [
  { value: 'admin', label: '管理员审核', icon: '🛡️' },
  { value: 'student', label: '学生车辆', icon: '🎓' }
]
const previewUrl = ref('')

// ==================== 管理员模块 ====================
const adminVehicleList = ref([])
const adminSelected = ref([])
const violationCountMap = ref({})
const vehicleDistribution = ref([])
const adminPagination = reactive({ page: 1, pageSize: 10, total: 0, totalPages: 1 })
const adminStats = reactive({ total: 0, approved: 0, pending: 0, blacklist: 0, frozen: 0 })
const adminSearch = reactive({ licensePlate: '', userType: '', status: '', startTime: '', endTime: '' })

const statusOptions = [
  { value: 'pending', label: '待审核', color: '#ff9800' },
  { value: 'approved', label: '已通过', color: '#4caf50' },
  { value: 'rejected', label: '已拒绝', color: '#f44336' },
  { value: 'frozen', label: '已冻结', color: '#2196f3' },
  { value: 'expired', label: '已过期', color: '#9e9e9e' }
]

const quickStats = computed(() => [
  { icon: '🎓', label: '学生车辆', value: adminStats.studentCount || 0, bg: 'rgba(0,123,255,0.2)' },
  { icon: '👨‍🏫', label: '教职工车辆', value: adminStats.teacherCount || 0, bg: 'rgba(40,167,69,0.2)' },
  { icon: '🔧', label: '后勤车辆', value: adminStats.serviceCount || 0, bg: 'rgba(255,193,7,0.2)' },
  { icon: '⏳', label: '临时人员', value: adminStats.temporaryCount || 0, bg: 'rgba(156,39,176,0.2)' }
])

const adminAllSelected = computed(() =>
  adminVehicleList.value.length > 0 && adminVehicleList.value.every(v => isAdminSelected(v))
)
const hasPending = computed(() => adminSelected.value.some(v => v.status === 'pending'))
const hasApproved = computed(() => adminSelected.value.some(v => v.status === 'approved'))

const fetchAdminVehicles = async () => {
  try {
    const params = {
      pageNum: adminPagination.page, pageSize: adminPagination.pageSize,
      licensePlate: adminSearch.licensePlate || undefined,
      userType: adminSearch.userType || undefined,
      status: adminSearch.status || undefined,
      startTime: adminSearch.startTime ? adminSearch.startTime + ' 00:00:00' : undefined,
      endTime: adminSearch.endTime ? adminSearch.endTime + ' 23:59:59' : undefined
    }
    Object.keys(params).forEach(k => params[k] === undefined && delete params[k])
    const res = await axios.get(`${API}/vehicle/list`, { params })
    if (res.data.code === 200) {
      adminVehicleList.value = res.data.data.map(v => ({ ...v, showMore: false }))
      adminPagination.total = res.data.total || 0
      adminPagination.totalPages = res.data.totalPages || 1
      fetchViolationCounts()
    }
  } catch { ElMessage.error('获取车辆列表失败') }
}

const fetchAdminStats = async () => {
  try {
    const res = await axios.get(`${API}/vehicle/stats`)
    if (res.data.code === 200) {
      const d = res.data.data
      adminStats.total = d.total || 0
      adminStats.approved = d.approved || 0
      adminStats.pending = d.pending || 0
      adminStats.blacklist = d.blacklist || 0
      adminStats.frozen = d.frozen || 0
      if (d.typeStats) {
        const getCount = t => d.typeStats.find(s => s.type === t)?.count || 0
        adminStats.studentCount = getCount('student')
        adminStats.teacherCount = getCount('teacher')
        adminStats.serviceCount = getCount('service')
        adminStats.temporaryCount = getCount('temporary')
        vehicleDistribution.value = d.typeStats.map(item => ({
          type: { student: '学生', teacher: '教职工', service: '后勤', temporary: '临时' }[item.type] || item.type,
          count: item.count,
          percentage: d.total > 0 ? ((item.count / d.total) * 100).toFixed(1) : 0,
          color: { student: '#007bff', teacher: '#28a745', service: '#ffc107', temporary: '#9c27b0' }[item.type] || '#aaa'
        }))
      }
    }
  } catch (e) { console.error(e) }
}

const fetchViolationCounts = async () => {
  for (const v of adminVehicleList.value) {
    try {
      const res = await axios.get(`${API}/violation/list`, { params: { pageNum: 1, pageSize: 1, vehiclePlate: v.licensePlate } })
      if (res.data.code === 200) violationCountMap.value[v.vehicleId] = res.data.total || 0
    } catch {}
  }
}

const resetAdminSearch = () => {
  adminSearch.licensePlate = ''; adminSearch.userType = ''; adminSearch.status = ''
  adminSearch.startTime = ''; adminSearch.endTime = ''
  fetchAdminVehicles()
}

const adminChangePage = dir => {
  const np = adminPagination.page + dir
  if (np >= 1 && np <= adminPagination.totalPages) { adminPagination.page = np; fetchAdminVehicles() }
}

const isAdminSelected = v => adminSelected.value.some(s => s.vehicleId === v.vehicleId)
const toggleAdminSelect = v => {
  const idx = adminSelected.value.findIndex(s => s.vehicleId === v.vehicleId)
  idx === -1 ? adminSelected.value.push(v) : adminSelected.value.splice(idx, 1)
}
const toggleAdminSelectAll = () => {
  adminAllSelected.value ? adminSelected.value = [] : adminSelected.value = [...adminVehicleList.value]
}
const toggleMore = v => { adminVehicleList.value.forEach(x => { if (x !== v) x.showMore = false }); v.showMore = !v.showMore }

const approveVehicle = async v => {
  try {
    await ElMessageBox.confirm(`确定通过 ${v.licensePlate} 的注册申请？`, '审核通过', { type: 'warning' })
    const res = await axios.post(`${API}/vehicle/approve/${v.vehicleId}`, null, { params: { auditorId: 1, remark: '审核通过' } })
    if (res.data.code === 200) { ElMessage.success('审核通过'); fetchAdminVehicles(); fetchAdminStats() }
    else ElMessage.error(res.data.message)
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const rejectVehicle = async v => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝', { inputType: 'textarea' })
    if (!reason) return
    const res = await axios.post(`${API}/vehicle/reject/${v.vehicleId}`, null, { params: { auditorId: 1, reason } })
    if (res.data.code === 200) { ElMessage.success('已拒绝'); fetchAdminVehicles(); fetchAdminStats() }
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const toggleFreezeVehicle = async v => {
  try {
    const action = v.isFrozen ? '解冻' : '冻结'
    await ElMessageBox.confirm(`确定${action} ${v.licensePlate}？`, action, { type: 'warning' })
    const res = await axios.post(`${API}/vehicle/freeze/${v.vehicleId}`, null, { params: { freeze: !v.isFrozen } })
    if (res.data.code === 200) { ElMessage.success(`已${action}`); fetchAdminVehicles() }
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const toggleBlacklist = async v => {
  try {
    const action = v.isBlacklist ? '移出黑名单' : '加入黑名单'
    await ElMessageBox.confirm(`确定将 ${v.licensePlate} ${action}？`, action, { type: 'warning' })
    const res = await axios.post(`${API}/vehicle/blacklist/${v.vehicleId}`, null, { params: { blacklist: !v.isBlacklist } })
    if (res.data.code === 200) { ElMessage.success(`已${action}`); fetchAdminVehicles() }
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const deleteVehicle = async v => {
  try {
    await ElMessageBox.confirm(`确定注销 ${v.licensePlate}？此操作不可恢复！`, '注销车辆', { type: 'error' })
    const res = await axios.delete(`${API}/vehicle/${v.vehicleId}`)
    if (res.data.code === 200) { ElMessage.success('已注销'); fetchAdminVehicles(); fetchAdminStats() }
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const batchApprove = async () => {
  if (!hasPending.value) { ElMessage.warning('请选择待审核车辆'); return }
  try {
    await ElMessageBox.confirm(`批量通过 ${adminSelected.value.length} 辆车？`, '批量通过', { type: 'warning' })
    const ids = adminSelected.value.filter(v => v.status === 'pending').map(v => v.vehicleId)
    await axios.post(`${API}/vehicle/batch-approve`, { vehicleIds: ids, auditorId: 1 })
    ElMessage.success('批量通过完成'); adminSelected.value = []; fetchAdminVehicles(); fetchAdminStats()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const batchReject = async () => {
  if (!hasPending.value) return
  try {
    const { value: reason } = await ElMessageBox.prompt('拒绝原因', '批量拒绝', { inputType: 'textarea' })
    if (!reason) return
    const targets = adminSelected.value.filter(v => v.status === 'pending')
    await Promise.all(targets.map(v => axios.post(`${API}/vehicle/reject/${v.vehicleId}`, null, { params: { auditorId: 1, reason } })))
    ElMessage.success('批量拒绝完成'); adminSelected.value = []; fetchAdminVehicles(); fetchAdminStats()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const batchFreeze = async () => {
  if (!hasApproved.value) return
  try {
    await ElMessageBox.confirm('批量冻结选中车辆？', '批量冻结', { type: 'warning' })
    await Promise.all(adminSelected.value.filter(v => v.status === 'approved').map(v =>
      axios.post(`${API}/vehicle/freeze/${v.vehicleId}`, null, { params: { freeze: true } })
    ))
    ElMessage.success('批量冻结完成'); adminSelected.value = []; fetchAdminVehicles()
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const uploadVehiclePhoto = v => {
  const input = document.createElement('input'); input.type = 'file'; input.accept = 'image/*'
  input.onchange = async e => {
    const file = e.target.files[0]; if (!file) return
    const fd = new FormData(); fd.append('file', file)
    const res = await axios.post(`${API}/vehicle/upload-photo/${v.vehicleId}`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (res.data.code === 200) { ElMessage.success('图片上传成功'); fetchAdminVehicles() }
    else ElMessage.error('上传失败')
  }; input.click()
}

const sendVehicleReminder = async v => {
  if (!v || !v.userId) { ElMessage.warning('无绑定用户'); return }
  try {
    await axios.post(`${API}/notification/send-personal`, null, {
      params: { title: '车辆整改提醒', content: `您的车辆 ${v.licensePlate} 有需要处理的事项，请及时登录系统查看。`, receiverId: v.userId }
    })
    ElMessage.success('提醒已发送')
  } catch { ElMessage.error('发送失败') }
}

const editVehicle = v => ElMessage.info('编辑车辆功能开发中')
const openRegisterDialog = () => ElMessage.info('可跳转到注册页面')

// 档案抽屉
const archiveDrawer = reactive({ visible: false, vehicle: null, violations: [] })
const viewAdminArchive = async v => {
  archiveDrawer.vehicle = v; archiveDrawer.violations = []; archiveDrawer.visible = true
  try {
    const res = await axios.get(`${API}/violation/list`, { params: { pageNum: 1, pageSize: 10, vehiclePlate: v.licensePlate } })
    if (res.data.code === 200) archiveDrawer.violations = res.data.data || []
  } catch {}
}

// ==================== 学生模块 ====================
const studentVehicles = ref([])
const studentViolationMap = ref({})
const studentRegDialog = reactive({ visible: false, step: 0, agreed: false, submitting: false })
const studentRegForm = reactive({ licensePlate: '', frameNum: '', deviceMac: '', brand: '雅迪', color: '白色', batteryType: '锂电池', vehicleType: '电动自行车', vehiclePhoto: null, photoPreview: '' })
const regSteps = ['填写信息', '上传照片', '提交确认']
const regDragOver = ref(false)
const plateCheckResult = ref(null)

const rectifyDialog = reactive({ visible: false, violation: null, desc: '', photo: null, photoPreview: '' })
const appealDialog = reactive({ visible: false, violation: null, reason: '', evidence: '' })

const fetchStudentVehicles = async () => {
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    const res = await axios.get(`${API}/vehicle/list`, { params: { pageNum: 1, pageSize: 20 } })
    if (res.data.code === 200) {
      studentVehicles.value = res.data.data || []
      for (const v of studentVehicles.value) {
        const vres = await axios.get(`${API}/violation/list`, { params: { pageNum: 1, pageSize: 20, vehiclePlate: v.licensePlate } })
        if (vres.data.code === 200) studentViolationMap.value[v.vehicleId] = vres.data.data || []
      }
    }
  } catch { ElMessage.error('获取车辆数据失败') }
}

const getCurrentUserId = () => {
  try { return JSON.parse(atob(localStorage.getItem('token')?.split('.')[1] || '') || '{}')?.userId || 1 } catch { return 1 }
}

const openStudentRegisterDialog = () => {
  Object.assign(studentRegForm, { licensePlate: '', frameNum: '', deviceMac: '', brand: '雅迪', color: '白色', batteryType: '锂电池', vehicleType: '电动自行车', vehiclePhoto: null, photoPreview: '' })
  plateCheckResult.value = null
  studentRegDialog.step = 0; studentRegDialog.agreed = false; studentRegDialog.visible = true
}

const nextRegStep = async () => {
  if (studentRegDialog.step === 0) {
    if (!studentRegForm.licensePlate || !studentRegForm.frameNum) { ElMessage.warning('请填写车牌号和车架号'); return }
    try {
      const res = await axios.get(`${API}/vehicle/list`, { params: { pageNum: 1, pageSize: 1, licensePlate: studentRegForm.licensePlate } })
      plateCheckResult.value = !(res.data.data || []).some(v => v.licensePlate === studentRegForm.licensePlate)
      if (!plateCheckResult.value) { ElMessage.warning('该车牌号已注册'); return }
    } catch {}
  }
  studentRegDialog.step++
}

const onRegFileDrop = e => {
  const file = e.dataTransfer.files[0]
  if (file) loadRegPhoto(file)
  regDragOver.value = false
}
const onRegPhotoSelect = e => { const f = e.target.files[0]; if (f) loadRegPhoto(f) }
const loadRegPhoto = file => {
  if (file.size > 10 * 1024 * 1024) { ElMessage.error('图片不能超过10MB'); return }
  studentRegForm.vehiclePhoto = file
  const reader = new FileReader()
  reader.onload = e => { studentRegForm.photoPreview = e.target.result }
  reader.readAsDataURL(file)
}

const submitStudentRegister = async () => {
  if (!studentRegDialog.agreed) { ElMessage.warning('请同意管理协议'); return }
  studentRegDialog.submitting = true
  try {
    let photoUrl = ''
    if (studentRegForm.vehiclePhoto) {
      const fd = new FormData(); fd.append('file', studentRegForm.vehiclePhoto); fd.append('type', 'image')
      const uploadRes = await axios.post(`${API}/file/upload`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      if (uploadRes.data.code === 200) photoUrl = uploadRes.data.data.fileUrl
    }
    const userId = getCurrentUserId()
    const data = { userId, licensePlate: studentRegForm.licensePlate, frameNum: studentRegForm.frameNum, brand: studentRegForm.brand, color: studentRegForm.color, batteryType: studentRegForm.batteryType, vehicleType: studentRegForm.vehicleType, vehiclePhoto: photoUrl }
    if (studentRegForm.deviceMac?.trim()) data.deviceMac = studentRegForm.deviceMac.trim()
    const res = await axios.post(`${API}/vehicle/register`, data)
    if (res.data.code === 200) {
      ElMessage.success('注册申请已提交，等待审核')
      studentRegDialog.visible = false
      fetchStudentVehicles()
    } else ElMessage.error(res.data.message || '提交失败')
  } catch (e) { ElMessage.error('提交失败') }
  finally { studentRegDialog.submitting = false }
}

const openRectifyDialog = v => {
  rectifyDialog.violation = v; rectifyDialog.desc = ''; rectifyDialog.photo = null; rectifyDialog.photoPreview = ''; rectifyDialog.visible = true
}
const onRectifyPhoto = e => {
  const f = e.target.files[0]; if (!f) return
  rectifyDialog.photo = f
  const r = new FileReader(); r.onload = ev => { rectifyDialog.photoPreview = ev.target.result }; r.readAsDataURL(f)
}
const submitRectify = async () => {
  if (!rectifyDialog.desc.trim()) { ElMessage.warning('请填写整改说明'); return }
  try {
    const userId = getCurrentUserId()
    const res = await axios.post(`${API}/violation/${rectifyDialog.violation.illegalId}/rectify`, null, { params: { userId, reason: rectifyDialog.desc } })
    if (res.data.code === 200) { ElMessage.success('整改申请已提交'); rectifyDialog.visible = false; fetchStudentVehicles() }
    else ElMessage.error(res.data.message)
  } catch { ElMessage.error('提交失败') }
}

const openAppealDialog = v => { appealDialog.violation = v; appealDialog.reason = ''; appealDialog.evidence = ''; appealDialog.visible = true }
const submitAppeal = async () => {
  if (!appealDialog.reason.trim()) { ElMessage.warning('请填写申诉理由'); return }
  try {
    const userId = getCurrentUserId()
    const res = await axios.post(`${API}/appeal/submit`, { illegalId: appealDialog.violation.illegalId, userId, appealReason: appealDialog.reason, appealEvidence: appealDialog.evidence })
    if (res.data.code === 200) { ElMessage.success('申诉已提交'); appealDialog.visible = false; fetchStudentVehicles() }
    else ElMessage.error(res.data.message)
  } catch { ElMessage.error('提交失败') }
}

const viewStudentDetail = v => { archiveDrawer.vehicle = v; archiveDrawer.violations = studentViolationMap.value[v.vehicleId] || []; archiveDrawer.visible = true }
const editStudentVehicle = v => ElMessage.info('可重新提交注册信息')
const uploadStudentPhoto = uploadVehiclePhoto

// ==================== 通用工具 ====================
const getFullUrl = url => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/uploads')) return `http://localhost:8083${url}`
  return `http://localhost:8083/uploads/${url.replace(/^\/+/, '')}`
}
const previewImage = url => { if (url) previewUrl.value = getFullUrl(url) }
const handleImgError = e => { e.target.style.display = 'none' }
const formatDate = d => { if (!d) return ''; try { return String(d).split(' ')[0] } catch { return '' } }
const getUserTypeText = t => ({ student: '学生', teacher: '教职工', service: '后勤', admin: '管理员', temporary: '临时' }[t] || t)
const getStatusText = s => ({ pending: '待审核', approved: '已通过', rejected: '已拒绝', frozen: '已冻结', expired: '已过期' }[s] || s)
const getViolationTypeName = t => ({ 1: '违规停放', 2: '超速行驶', 3: '违规充电', 4: '未登记', 5: '进入禁行区', 6: '违规载人', 7: '遮挡车牌', 8: '其他' }[t] || '未知')
const getViolationStateName = s => ({ 0: '待整改', 1: '整改中', 2: '已整改', 3: '逾期', 4: '申诉中' }[s] || '未知')

onMounted(() => {
  fetchAdminVehicles()
  fetchAdminStats()
  fetchStudentVehicles()
})
</script>

<style scoped>
.vehicle-container { min-height: 100vh; background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%); color: #fff; padding: 0; }
.top-tabs { display: flex; gap: 0; border-bottom: 2px solid rgba(0,212,255,0.2); padding: 0 20px; background: rgba(0,0,0,0.3); }
.top-tab { display: flex; align-items: center; gap: 8px; padding: 14px 24px; cursor: pointer; font-size: 15px; color: rgba(255,255,255,0.6); border-bottom: 3px solid transparent; margin-bottom: -2px; transition: all 0.3s; }
.top-tab.active { color: #00d4ff; border-bottom-color: #00d4ff; background: rgba(0,212,255,0.05); }
.top-tab:hover:not(.active) { color: rgba(255,255,255,0.85); }
.tab-icon { font-size: 18px; }
.module { padding: 20px; }
.page-header { margin-bottom: 20px; }
.header-bg { background: linear-gradient(90deg, rgba(0,123,255,0.1), rgba(102,16,242,0.1)); border-radius: 16px; padding: 22px 24px; border: 1px solid rgba(0,123,255,0.2); display: flex; justify-content: space-between; align-items: center; position: relative; }
.header-bg::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px; background: linear-gradient(90deg, #007bff, #6610f2); border-radius: 16px 16px 0 0; }
.header-left { display: flex; flex-direction: column; gap: 12px; }
.title-text { font-size: 22px; font-weight: bold; background: linear-gradient(90deg, #fff, #00d4ff); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.title-sub { font-size: 13px; color: rgba(255,255,255,0.5); }
.header-stats { display: flex; align-items: center; gap: 16px; background: rgba(0,0,0,0.3); padding: 8px 18px; border-radius: 10px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 20px; font-weight: bold; color: #00d4ff; }
.stat-num.c-green { color: #4caf50; } .stat-num.c-orange { color: #ff9800; } .stat-num.c-red { color: #f44336; }
.stat-lab { font-size: 11px; color: rgba(255,255,255,0.5); }
.stat-div { width: 1px; height: 24px; background: rgba(255,255,255,0.15); }
.header-actions { display: flex; gap: 10px; }
.action-btn { display: inline-flex; align-items: center; gap: 5px; padding: 9px 16px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.action-btn:hover { background: rgba(255,255,255,0.15); }
.action-btn.primary { background: linear-gradient(90deg, #007bff, #6610f2); border: none; }
.action-btn.success { background: rgba(76,175,80,0.3); border-color: rgba(76,175,80,0.5); color: #4caf50; }
.action-btn.danger { background: rgba(244,67,54,0.3); border-color: rgba(244,67,54,0.5); color: #f44336; }
.action-btn.warn { background: rgba(255,152,0,0.2); border-color: rgba(255,152,0,0.4); color: #ff9800; }
.action-btn.small { padding: 5px 10px; font-size: 12px; }
.action-btn.disabled { opacity: 0.5; cursor: not-allowed; }
.main-content { display: flex; gap: 20px; }
.left-panel { width: 280px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; }
.search-form { background: rgba(255,255,255,0.05); border-radius: 12px; padding: 16px; border: 1px solid rgba(255,255,255,0.1); display: flex; flex-direction: column; gap: 14px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; color: rgba(255,255,255,0.7); }
.tech-input { width: 100%; padding: 9px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; box-sizing: border-box; }
.tech-input:focus { outline: none; border-color: #00d4ff; }
.tech-select { width: 100%; padding: 9px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; appearance: none; }
.tech-select-full { width: 100%; padding: 10px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; }
.status-selector { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.status-opt { display: flex; align-items: center; gap: 6px; padding: 6px 10px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; cursor: pointer; font-size: 12px; transition: all 0.2s; }
.status-opt.active { background: rgba(0,212,255,0.15); border-color: rgba(0,212,255,0.3); }
.sdot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.date-range { display: flex; align-items: center; gap: 6px; }
.date-sep { color: rgba(255,255,255,0.5); font-size: 12px; }
.form-actions { display: flex; gap: 8px; }
.search-btn { flex: 1; background: linear-gradient(90deg, #007bff, #6610f2); border-radius: 8px; padding: 10px; text-align: center; cursor: pointer; font-size: 13px; }
.reset-btn { padding: 10px 14px; background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.15); border-radius: 8px; cursor: pointer; font-size: 13px; }
.quick-stats { display: flex; flex-direction: column; gap: 10px; }
.qstat-card { display: flex; align-items: center; gap: 12px; background: rgba(255,255,255,0.05); border-radius: 10px; padding: 12px; border: 1px solid rgba(255,255,255,0.08); }
.qstat-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.qstat-val { font-size: 20px; font-weight: bold; color: #00d4ff; }
.qstat-lab { font-size: 12px; color: rgba(255,255,255,0.5); }
.dist-card { background: rgba(255,255,255,0.05); border-radius: 12px; padding: 14px; border: 1px solid rgba(255,255,255,0.08); }
.dist-title { font-size: 13px; color: #00d4ff; margin-bottom: 12px; }
.dist-item { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.dist-label { width: 50px; font-size: 12px; color: rgba(255,255,255,0.7); }
.dist-bar { flex: 1; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; }
.dist-fill { height: 100%; border-radius: 3px; transition: width 0.5s; }
.dist-val { width: 40px; font-size: 12px; color: rgba(255,255,255,0.5); text-align: right; }
.right-panel { flex: 1; min-width: 0; }
.table-container { background: rgba(255,255,255,0.05); border-radius: 12px; border: 1px solid rgba(255,255,255,0.1); }
.table-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.table-title { font-size: 15px; font-weight: 500; }
.table-actions { display: flex; align-items: center; gap: 16px; }
.total-info { font-size: 13px; color: rgba(255,255,255,0.5); }
.page-controls { display: flex; align-items: center; gap: 8px; }
.page-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 6px; cursor: pointer; }
.page-btn.disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 12px; color: rgba(255,255,255,0.6); }
.table-wrapper { overflow-x: auto; padding: 12px; }
.tech-table { width: 100%; border-collapse: separate; border-spacing: 0; min-width: 1200px; }
.tech-table th { padding: 10px 10px; text-align: left; font-size: 12px; color: rgba(255,255,255,0.6); border-bottom: 2px solid rgba(0,212,255,0.2); }
.tech-table td { padding: 10px 10px; font-size: 13px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.tech-table tbody tr:hover { background: rgba(0,212,255,0.04); }
.tech-checkbox { width: 16px; height: 16px; cursor: pointer; accent-color: #00d4ff; }
.vimg-cell { width: 60px; height: 40px; border-radius: 5px; overflow: hidden; background: rgba(0,0,0,0.3); cursor: pointer; display: flex; align-items: center; justify-content: center; }
.vehicle-thumb { width: 100%; height: 100%; object-fit: cover; }
.no-img { color: rgba(255,255,255,0.3); font-size: 16px; }
.id-badge { color: #00d4ff; font-weight: 500; }
.plate-cell { display: flex; flex-direction: column; gap: 3px; }
.plate-no { font-weight: bold; font-size: 14px; }
.black-tag { background: rgba(244,67,54,0.2); color: #f44336; font-size: 11px; padding: 1px 6px; border-radius: 4px; border: 1px solid rgba(244,67,54,0.3); width: fit-content; }
.owner-cell { display: flex; flex-direction: column; gap: 3px; }
.owner-name { font-weight: 500; }
.owner-type { font-size: 11px; padding: 2px 7px; border-radius: 4px; width: fit-content; }
.owner-type.student { background: rgba(0,123,255,0.2); color: #007bff; }
.owner-type.teacher { background: rgba(40,167,69,0.2); color: #28a745; }
.owner-type.service { background: rgba(255,193,7,0.2); color: #ffc107; }
.owner-type.temporary { background: rgba(156,39,176,0.2); color: #9c27b0; }
.owner-id { font-size: 11px; color: rgba(255,255,255,0.4); }
.vinfo-cell { display: flex; flex-direction: column; gap: 2px; font-size: 13px; }
.vinfo-sub { font-size: 11px; color: rgba(255,255,255,0.5); }
.reg-cell { display: flex; flex-direction: column; gap: 2px; font-size: 12px; color: rgba(255,255,255,0.6); }
.status-badge { padding: 3px 9px; border-radius: 14px; font-size: 12px; }
.status-badge.pending { background: rgba(255,152,0,0.2); color: #ff9800; border: 1px solid rgba(255,152,0,0.3); }
.status-badge.approved { background: rgba(76,175,80,0.2); color: #4caf50; border: 1px solid rgba(76,175,80,0.3); }
.status-badge.rejected { background: rgba(244,67,54,0.2); color: #f44336; border: 1px solid rgba(244,67,54,0.3); }
.status-badge.frozen { background: rgba(33,150,243,0.2); color: #2196f3; border: 1px solid rgba(33,150,243,0.3); }
.status-badge.expired { background: rgba(158,158,158,0.2); color: #9e9e9e; border: 1px solid rgba(158,158,158,0.3); }
.violation-cnt { font-size: 15px; font-weight: bold; color: #fff; }
.violation-cnt.high { color: #f44336; }
.action-cell { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; }
.more-wrap { position: relative; }
.more-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 6px; cursor: pointer; }
.more-menu { position: absolute; top: 100%; right: 0; background: rgba(20,25,55,0.97); border: 1px solid rgba(255,255,255,0.15); border-radius: 8px; padding: 6px 0; min-width: 120px; z-index: 100; }
.menu-item { padding: 7px 14px; font-size: 12px; cursor: pointer; transition: all 0.2s; color: rgba(255,255,255,0.8); }
.menu-item:hover { background: rgba(255,255,255,0.1); color: #00d4ff; }
.menu-item.danger { color: #f44336; border-top: 1px solid rgba(255,255,255,0.08); }
.batch-bar { padding: 12px 18px; border-top: 1px solid rgba(255,255,255,0.08); display: flex; align-items: center; gap: 16px; font-size: 13px; }
.batch-actions { display: flex; gap: 8px; }
.batch-btn { padding: 6px 14px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.15); border-radius: 6px; cursor: pointer; font-size: 12px; }
.batch-btn.disabled { opacity: 0.3; cursor: not-allowed; }
/* 学生模块 */
.student-vehicles { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 20px; }
.empty-state { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-text { font-size: 16px; color: rgba(255,255,255,0.5); }
.vehicle-card { background: rgba(255,255,255,0.05); border-radius: 14px; border: 1px solid rgba(255,255,255,0.1); overflow: hidden; }
.vcard-header { display: flex; align-items: flex-start; gap: 14px; padding: 16px; background: rgba(0,0,0,0.2); }
.vcard-photo { width: 80px; height: 60px; border-radius: 8px; overflow: hidden; background: rgba(0,0,0,0.3); flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.vcard-img { width: 100%; height: 100%; object-fit: cover; }
.vcard-no-img { font-size: 32px; }
.vcard-info { flex: 1; }
.vcard-plate { font-size: 18px; font-weight: bold; color: #fff; }
.vcard-brand { font-size: 13px; color: rgba(255,255,255,0.6); margin-top: 4px; }
.vcard-type { font-size: 12px; color: #00d4ff; margin-top: 2px; }
.vcard-status { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
.frozen-badge { font-size: 11px; padding: 2px 8px; background: rgba(33,150,243,0.2); color: #2196f3; border-radius: 10px; }
.vcard-body { padding: 14px 16px; }
.vcard-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px; }
.vd-item { display: flex; flex-direction: column; gap: 3px; background: rgba(0,0,0,0.2); border-radius: 6px; padding: 8px 10px; }
.vd-label { font-size: 11px; color: rgba(255,255,255,0.4); }
.vd-val { font-size: 13px; color: #fff; font-weight: 500; }
.vcard-violations { background: rgba(255,152,0,0.05); border: 1px solid rgba(255,152,0,0.2); border-radius: 8px; padding: 10px 12px; }
.vvio-title { font-size: 12px; color: #ff9800; margin-bottom: 8px; }
.vvio-item { display: flex; align-items: center; gap: 8px; padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 12px; }
.vvio-item:last-child { border-bottom: none; }
.vvio-type { flex: 1; color: rgba(255,255,255,0.8); }
.vvio-time { color: rgba(255,255,255,0.4); width: 80px; }
.vvio-state { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.vvio-action { display: flex; gap: 5px; }
.state-0 { background: rgba(255,152,0,0.2); color: #ff9800; }
.state-1 { background: rgba(33,150,243,0.2); color: #2196f3; }
.state-2 { background: rgba(76,175,80,0.2); color: #4caf50; }
.state-3 { background: rgba(244,67,54,0.2); color: #f44336; }
.state-4 { background: rgba(156,39,176,0.2); color: #9c27b0; }
.vcard-footer { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid rgba(255,255,255,0.08); }
/* 注册弹窗 */
.steps-bar { display: flex; justify-content: center; gap: 0; padding: 16px 24px; background: rgba(0,0,0,0.2); }
.step-item { display: flex; align-items: center; gap: 8px; }
.step-item:not(:last-child)::after { content: '→'; color: rgba(255,255,255,0.2); margin: 0 12px; }
.step-dot { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold; background: rgba(255,255,255,0.1); border: 2px solid rgba(255,255,255,0.2); color: rgba(255,255,255,0.5); }
.step-item.active .step-dot { background: linear-gradient(135deg, #007bff, #6610f2); border-color: #00d4ff; color: #fff; }
.step-item.done .step-dot { background: rgba(76,175,80,0.3); border-color: #4caf50; color: #4caf50; }
.step-text { font-size: 13px; color: rgba(255,255,255,0.5); }
.step-item.active .step-text, .step-item.done .step-text { color: rgba(255,255,255,0.85); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-item { display: flex; flex-direction: column; gap: 7px; }
.hint-ok { font-size: 12px; color: #4caf50; margin-top: 4px; }
.hint-err { font-size: 12px; color: #f44336; margin-top: 4px; }
.upload-zone { min-height: 200px; border: 2px dashed rgba(255,255,255,0.2); border-radius: 12px; display: flex; align-items: center; justify-content: center; transition: all 0.3s; }
.upload-zone.drag-over { border-color: #00d4ff; background: rgba(0,212,255,0.05); }
.upload-placeholder { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 10px; color: rgba(255,255,255,0.6); font-size: 14px; }
.upload-icon-big { font-size: 48px; }
.upload-tips { font-size: 12px; color: rgba(255,255,255,0.4); }
.upload-preview { text-align: center; padding: 16px; }
.preview-large { max-width: 100%; max-height: 260px; border-radius: 8px; }
.review-section { display: flex; flex-direction: column; gap: 16px; }
.review-block { background: rgba(0,0,0,0.2); border-radius: 10px; padding: 14px 16px; }
.review-block h5 { margin: 0 0 12px; font-size: 14px; color: #00d4ff; }
.review-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.review-item { display: flex; gap: 8px; font-size: 13px; }
.rl { color: rgba(255,255,255,0.5); min-width: 50px; }
.rv { color: #fff; font-weight: 500; }
.agreement-check { display: flex; align-items: center; gap: 10px; padding: 12px; background: rgba(0,212,255,0.05); border: 1px solid rgba(0,212,255,0.15); border-radius: 8px; cursor: pointer; font-size: 13px; color: rgba(255,255,255,0.7); }
.agreement-check.active { background: rgba(0,212,255,0.15); border-color: rgba(0,212,255,0.3); }
.check-box { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-radius: 3px; position: relative; flex-shrink: 0; }
.agreement-check.active .check-box { background: #00d4ff; border-color: #00d4ff; }
.agreement-check.active .check-box::after { content: '✓'; position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); color: #000; font-size: 11px; }
.agree-link { color: #00d4ff; }
/* 整改弹窗 */
.mini-upload { min-height: 80px; border: 1px dashed rgba(255,255,255,0.2); border-radius: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.mini-preview { max-width: 100%; max-height: 160px; border-radius: 6px; }
.mini-placeholder { color: rgba(255,255,255,0.4); font-size: 13px; }
.tech-textarea { width: 100%; padding: 10px 12px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; font-size: 13px; resize: vertical; min-height: 80px; font-family: inherit; }
.tech-textarea:focus { outline: none; border-color: #00d4ff; }
/* 档案抽屉 */
.drawer-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 1000; display: flex; justify-content: flex-end; }
.drawer { width: 540px; background: linear-gradient(135deg, #1a1f3c, #0d1228); border-left: 1px solid rgba(0,123,255,0.3); display: flex; flex-direction: column; height: 100%; box-shadow: -10px 0 40px rgba(0,0,0,0.5); }
.drawer-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.drawer-title { font-size: 16px; font-weight: 600; }
.drawer-close { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 50%; cursor: pointer; }
.drawer-body { flex: 1; overflow-y: auto; padding: 18px; display: flex; flex-direction: column; gap: 16px; }
.drawer-footer { padding: 14px 22px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; gap: 10px; justify-content: flex-end; }
.archive-section { background: rgba(0,0,0,0.2); border-radius: 10px; padding: 14px; }
.sec-title { font-size: 13px; color: #00d4ff; margin-bottom: 12px; border-bottom: 1px solid rgba(0,212,255,0.2); padding-bottom: 6px; }
.info-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.info-item { background: rgba(255,255,255,0.04); border-radius: 6px; padding: 8px 10px; }
.il { font-size: 11px; color: rgba(255,255,255,0.4); margin-bottom: 3px; }
.iv { font-size: 13px; color: #fff; font-weight: 500; }
.vio-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 12px; }
.vio-type { flex: 1; color: rgba(255,255,255,0.8); }
.vio-time { color: rgba(255,255,255,0.4); width: 90px; }
.state-badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.vio-pts { color: #f44336; font-weight: bold; width: 40px; text-align: right; }
.no-data { text-align: center; color: rgba(255,255,255,0.3); padding: 16px; font-size: 13px; }
.info-block { background: rgba(0,0,0,0.2); border-radius: 8px; padding: 12px; }
.info-row { display: flex; gap: 10px; padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px; }
.info-row:last-child { border-bottom: none; }
/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: linear-gradient(135deg, #1a1f3c, #2d3250); border-radius: 16px; border: 1px solid rgba(0,123,255,0.3); width: 560px; max-height: 85vh; display: flex; flex-direction: column; }
.modal-content.large { width: 680px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 22px; border-bottom: 1px solid rgba(255,255,255,0.1); flex-shrink: 0; }
.modal-title { font-size: 16px; font-weight: 600; }
.modal-close { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 50%; cursor: pointer; }
.modal-body { padding: 20px 22px; overflow-y: auto; flex: 1; }
.modal-footer { padding: 12px 22px; border-top: 1px solid rgba(255,255,255,0.1); display: flex; justify-content: flex-end; gap: 10px; flex-shrink: 0; }
.empty-row { text-align: center; padding: 30px; color: rgba(255,255,255,0.3); font-size: 13px; }
/* 图片预览 */
.img-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.92); z-index: 2000; display: flex; align-items: center; justify-content: center; }
.img-preview { max-width: 90vw; max-height: 90vh; border-radius: 8px; }
.img-close { position: absolute; top: 20px; right: 24px; font-size: 28px; cursor: pointer; color: rgba(255,255,255,0.6); }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,0.05); border-radius: 3px; }
::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.3); border-radius: 3px; }
</style>
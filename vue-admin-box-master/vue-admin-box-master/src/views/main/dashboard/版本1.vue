<template>
  <div class="monitor-container">
    <!-- 顶部导航 -->
    <header class="header">
      <span class="head-logo">校园电动车智能监控平台</span>
      <ul class="head-ul"></ul>
      <div class="header-right">
        <span>{{ currentTime }}</span>
        <span>{{ dashboardData.weather?.condition || '--' }} {{ dashboardData.weather?.temperature || '--' }}℃</span>
      </div>
    </header>

    <!-- 主内容区域 -->
    <section class="section commanBox">
      <!-- 左侧区域 -->
      <div class="boxLeft">
        <!-- 视频监控 -->
        <div class="boxLeftVideoTop"></div>
        <div class="clear"></div>
        <div class="boxLeftVideoBox">
          <div class="BoxConTop">
            <span class="BoxconTopLeft">东校区正门监控（{{ currentDate }}）</span>
            <span class="BoxconTopRight">
              <img src="/img/ico.png" alt="图标" @click="toggleFullscreen('camera1')" />
            </span>
          </div>
          <div class="boxLeftVideoBoxCon">
            <video autoplay width="100%" height="100%" ref="camera1" poster="/img/camera-placeholder.jpg">
              <source src="/webm/CAMERAV.webm" type="video/mp4">
              您的浏览器不支持视频播放
            </video>
          </div>
          <div class="clear"></div>
          <div class="BoxConTop">
            <span class="BoxconTopLeft">教学楼A区监控（{{ currentDate }}）</span>
            <span class="BoxconTopRight">
              <img src="/img/ico.png" alt="图标" @click="toggleFullscreen('camera2')" />
            </span>
          </div>
          <div class="boxLeftVideoBoxCon">
            <video autoplay width="100%" height="100%" ref="camera2" poster="/img/camera-placeholder.jpg">
              <source src="/webm/CAMERAV.webm" type="video/mp4">
              您的浏览器不支持视频播放
            </video>
          </div>
        </div>
        <div class="clear"></div>

        <!-- 报警信息 - 最近10条违规记录 -->
        <div class="boxLeftAram">
          <div class="boxLeftAlamTop"></div>
          <div class="boxLeftAlamCon">
            <ul class="alamUl">
              <li v-for="(alert, index) in alertList" :key="index" @click="handleAlertClick(alert)">
                <p>案发地点：{{ alert.location }}<span class="alamULspan">{{ alert.time }}</span></p>
                <p>案发备注：{{ alert.description }}</p>
              </li>
              <li v-if="alertList.length === 0" style="color:#85acd5; text-align:center; padding:20px;">
                暂无违规记录
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 中间区域 -->
      <div class="boxCenter">
        <!-- 案件分析 -->
        <div class="boxCenterUp">
          <div class="boxCentterUpLeft">
            <div class="ananBg">
              <img src="/img/ana01.png" class="ana01IMg" />
              <div class="anaImgText">{{ dashboardData.statistics?.totalCases || 0 }}</div>
            </div>
            <div class="anaText">案件总量</div>
          </div>
          <div class="boxCentterUpLeft">
            <div class="ananBg">
              <img src="/img/ana02.png" class="ana01IMg" />
              <div class="anaImgText01">{{ dashboardData.statistics?.processedCases || 0 }}</div>
            </div>
            <div class="anaText">案件处理量</div>
          </div>
          <div class="boxCentterUpLeft">
            <div class="ananBg">
              <img src="/img/ana01.png" class="ana01IMg" />
              <div class="anaImgText02">{{ dashboardData.statistics?.complaintCases || 0 }}</div>
            </div>
            <div class="anaText">案件投诉量</div>
          </div>
        </div>
        <div class="clear"></div>

        <!-- 百度地图3D区域 + 违规趋势折线图 -->
        <div class="map_con left" id="car_control">
          <div class="left map_left">
            <div class="map_box" id="map_box"></div>

            <!-- 3D地图控制面板 -->
            <div class="map_3d_control">
              <div class="control_panel">
                <div
                  class="control_item"
                  :class="{ active: is3D }"
                  title="保持3D视角"
                  aria-label="保持3D视角"
                  @click="toggle3DMode"
                >
                  <svg v-if="is3D" viewBox="0 0 24 24" class="control-svg" aria-hidden="true">
                    <path d="M12 2 4.5 6v12L12 22l7.5-4V6L12 2Z" />
                    <path d="M12 22V10" />
                    <path d="m4.5 6 7.5 4 7.5-4" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="control-svg" aria-hidden="true">
                    <path d="m12 4 8 4-8 4-8-4 8-4Z" />
                    <path d="m4 12 8 4 8-4" />
                    <path d="m4 16 8 4 8-4" />
                  </svg>
                  <span class="control-status-dot"></span>
                </div>
                <div
                  class="control_item"
                  :class="{ active: isSatellite }"
                  :title="isSatellite ? '当前为卫星视图' : '当前为地图视图'"
                  :aria-label="isSatellite ? '当前为卫星视图' : '当前为地图视图'"
                  @click="toggleSatellite"
                >
                  <svg v-if="isSatellite" viewBox="0 0 24 24" class="control-svg" aria-hidden="true">
                    <path d="M4 8a13 13 0 0 1 12-4" />
                    <path d="M4 13a8 8 0 0 1 8-8" />
                    <path d="m13 11 6-6" />
                    <rect x="11" y="10" width="4" height="4" rx="1" transform="rotate(45 13 12)" />
                    <path d="m15.5 8.5 2 2" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="control-svg" aria-hidden="true">
                    <path d="M3 6.5 9 4l6 2.5L21 4v14l-6 2.5-6-2.5L3 20V6.5Z" />
                    <path d="M9 4v13.5" />
                    <path d="M15 6.5V20" />
                  </svg>
                  <span class="control-status-dot"></span>
                </div>
                <div
                  class="control_item"
                  :class="{ active: showLabels }"
                  :title="showLabels ? '当前显示标注' : '当前隐藏标注'"
                  :aria-label="showLabels ? '当前显示标注' : '当前隐藏标注'"
                  @click="toggleLabels"
                >
                  <svg viewBox="0 0 24 24" class="control-svg" aria-hidden="true">
                    <path d="M12 21s6-5.2 6-11a6 6 0 1 0-12 0c0 5.8 6 11 6 11Z" />
                    <circle v-if="showLabels" cx="12" cy="10" r="2.2" class="control-svg-fill" />
                    <path v-else d="M5 5 19 19" />
                  </svg>
                  <span class="control-status-dot"></span>
                </div>
                <div
                  class="control_item control_item--danger"
                  title="清除陈旧点位"
                  aria-label="清除陈旧点位"
                  @click="confirmClearStaleMarkers"
                >
                  <svg viewBox="0 0 24 24" class="control-svg" aria-hidden="true">
                    <path d="M4 7h16" />
                    <path d="M9 7V4h6v3" />
                    <path d="m8 7 1 12h6l1-12" />
                    <path d="M10 11v5" />
                    <path d="M14 11v5" />
                  </svg>
                </div>
              </div>

              <!-- 视角控制 -->
              <div class="view_control">
                <div class="view_up" @click="tiltUp">↑</div>
                <div class="view_middle">
                  <div class="view_left" @click="rotateLeft">←</div>
                  <div class="view_center" @click="resetView">◎</div>
                  <div class="view_right" @click="rotateRight">→</div>
                </div>
                <div class="view_down" @click="tiltDown">↓</div>
              </div>
            </div>

            <div v-if="trackRouteEditing" class="track-route-float">
              <div class="track-route-float-head">
                <span>行驶轨迹配置</span>
                <b>{{ trackRouteSelectedLabel }}</b>
              </div>
              <div class="track-route-float-tip">请沿道路按顺序点击地图，系统会立即生成路线</div>
              <div class="track-route-float-actions">
                <button class="route-float-btn" type="button" @click="undoTrackRoutePoint">撤销一点</button>
                <button class="route-float-btn primary" type="button" @click="saveTrackRouteConfig">保存轨迹</button>
                <button class="route-float-btn" type="button" @click="openLocationDialog">定位管理</button>
                <button class="route-float-btn danger" type="button" @click="finishTrackRouteEditing">结束</button>
              </div>
            </div>

            <!-- 违规趋势折线图（地图左上角） -->
            <div class="trend-chart-wrapper">
              <div class="chart-title">最近7天违规趋势</div>
              <div id="chart-violation-trend" class="trend-chart-container"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧区域 -->
      <div class="boxRight">
        <div class="boxRightTop"></div>
        <div class="clear"></div>

        <!-- 视频点位状态 -->
        <div class="boxRightCon01">
          <ul class="boxRightCon_ul">
            <li v-for="(camera, index) in dashboardData.cameras || []" :key="index" @click="switchCamera(camera)">
              {{ camera.name }}<span><img :src="getCameraStatusIcon(camera.status)" /></span>
            </li>
          </ul>
        </div>
        <div class="clear"></div>

        <!-- 搜索面板（车辆信息 + 违规分析 + 定位管理按钮） -->
        <div class="boxRightCon02">
          <div class="boxRightcon02Top">
            <ul class="con02TopUL">
              <li :class="{ 'con02liAcitve': activeSearchTab === 'vehicle' }" @click="setActiveSearchTab('vehicle')">车辆信息</li>
              <li :class="{ 'con02liAcitve': activeSearchTab === 'analysis' }" @click="setActiveSearchTab('analysis')">违规分析</li>
              <li :class="{ 'con02liAcitve': activeSearchTab === 'location' }" @click="openLocationDialog">定位管理</li>
            </ul>
          </div>

          <div class="boxRithtChangeBox">
            <!-- 车辆信息 -->
            <div class="boxChangeCon" v-if="activeSearchTab === 'vehicle'">
              <div class="boxChangeConFind">
                <p>
                  <label class="boxFindLabel">车辆颜色：</label>
                  <select class="boxFindInput" v-model="vehicleSearch.color">
                    <option value="">全部</option>
                    <option v-for="color in vehicleColors" :value="color" :key="color">{{ color }}</option>
                  </select>
                  <label class="boxFindLabel">车辆品牌：</label>
                  <input type="text" class="boxFindInput" v-model="vehicleSearch.brand" placeholder="请输入品牌" />
                </p>
                <p>
                  <label class="boxFindLabel">车牌号码：</label>
                  <input type="text" class="boxFindInput" v-model="vehicleSearch.plate" placeholder="请输入车牌号" />
                </p>
                <p>
                  <button class="boxFindButton" @click="searchVehicles">开始搜索</button>
                </p>
              </div>
              <div class="clear"></div>

              <ul id="pageAll">
                <li v-if="vehicleResults.length === 0" style="color:#85acd5; text-align:center; padding:20px;">
                  暂无车辆数据
                </li>
                <li v-for="(vehicle, index) in vehicleResults" :key="index">
                  <div class="liImgLeft">
                    <img :src="getVehicleImage(vehicle.vehicleType || 'ebike')" alt="车辆图片" />
                  </div>
                  <div class="liConRight">
                    <p>
                      车牌号：<span>{{ vehicle.licensePlate }}</span>
                      <span class="liconRihtButSpan">
                        <button @click="locateVehicleOnMap(vehicle)">3D定位</button>
                        <button @click="showVehicleDetail(vehicle)">详情</button>
                      </span>
                    </p>
                    <p>车辆颜色：<span>{{ vehicle.color }}</span></p>
                    <p>车辆品牌：<span>{{ vehicle.brand }}</span></p>
                  </div>
                </li>
              </ul>

              <div class="boxChangeFoot" v-if="vehiclePagination.totalPages > 1">
                <span class="page_box">
                  <a class="prev" @click="prevVehiclePage">上一页</a>
                  <span class="num">
                    <span class="current_page">{{ vehiclePagination.pageNum }}</span>
                    <span style="padding:3px;">/</span>
                    <span class="total">{{ vehiclePagination.totalPages }}</span>
                  </span>
                  <a class="next" @click="nextVehiclePage">下一页</a>
                </span>
              </div>
            </div>

            <!-- 违规分析（柱状图 + 热力图） -->
            <div class="boxChangeCon" v-if="activeSearchTab === 'analysis'">
              <div class="analysis-chart-wrapper">
                <div class="chart-item">
                  <div class="chart-title">违规类型分布</div>
                  <div id="chart-violation-dist" class="analysis-chart-container"></div>
                </div>
                <div class="chart-item" style="margin-top: 15px;">
                  <div class="chart-title">违规时段热力图</div>
                  <div id="chart-heatmap" class="analysis-chart-container"></div>
                  <div style="margin-top: 4px; color: #85acd5; font-size: 0.7rem; text-align: center;">
                    横轴：小时（0-23） 纵轴：周一至周日
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 定位管理弹窗（大尺寸） -->
    <el-dialog
      title="定位模块管理"
      v-model="locationDialogVisible"
      width="90%"
      :close-on-click-modal="false"
      destroy-on-close
      class="location-dialog"
    >
      <div class="location-dialog-content">
        <!-- 统计卡片 -->
        <div class="location-stats">
          <div class="stat-item"><span class="stat-number c-green">{{ locationStats.online }}</span>在线</div>
          <div class="stat-item"><span class="stat-number c-red">{{ locationStats.offline }}</span>离线</div>
          <div class="stat-item"><span class="stat-number c-warn">{{ locationStats.lowBattery }}</span>低电量</div>
          <div class="stat-item"><span class="stat-number">{{ locationStats.total }}</span>总计</div>
        </div>

        <!-- 搜索过滤栏 -->
        <div class="location-filter">
          <input v-model="filter.keyword" class="filter-input" placeholder="搜索设备号/车牌" @keyup.enter="fetchDevices" />
          <select v-model="filter.status" class="filter-select" @change="fetchDevices">
            <option value="">全部状态</option>
            <option value="online">在线</option>
            <option value="offline">离线</option>
            <option value="low_battery">低电量</option>
          </select>
          <button class="filter-btn primary" @click="fetchDevices">搜索</button>
          <div class="location-filter-actions">
            <span class="filter-hint">点位</span>
            <button class="filter-btn subtle" type="button" :disabled="simActionLoading" @click="startSimLocationStream(8)">8点</button>
            <button class="filter-btn subtle" type="button" :disabled="simActionLoading" @click="startSimLocationStream(15)">15点</button>
            <button class="filter-btn subtle" type="button" :disabled="simActionLoading" @click="startSimLocationStream(30)">30点</button>
            <button class="filter-btn subtle" type="button" :disabled="simActionLoading" @click="startSimLocationStream(60)">60点</button>
            <button class="filter-btn subtle" type="button" :disabled="simActionLoading" @click="startSimLocationStream(100)">100点</button>
            <button class="filter-btn subtle" type="button" :disabled="simActionLoading" @click="startSimLocationStream(130)">130点</button>
            <button class="filter-btn subtle subtle-danger" type="button" :disabled="simActionLoading" @click="clearSimLocations">停止并清除</button>
          </div>
          <div class="demo-point-panel">
            <div class="demo-point-title">
              <span class="demo-point-dot"></span>
              终端定位点（10个，可保存、可绑定）
              <button class="demo-point-head-btn" type="button" @click="fetchDemoBindingOptions">刷新绑定数据</button>
              <button class="demo-point-head-btn" type="button" @click="spreadAllDemoPoints">重新分布</button>
              <button class="demo-point-head-btn" type="button" @click="showAllDemoPoints">全部显示</button>
              <button class="demo-point-head-btn" type="button" @click="hideAllDemoPoints">全部隐藏</button>
              <button
                class="demo-point-head-btn"
                :class="{ 'motion-on': demoPointMoving }"
                type="button"
                @click="toggleDemoPointMoving"
              >{{ demoPointMoving ? '移动：开' : '移动：关' }}</button>
              <button class="demo-point-head-btn primary" type="button" :disabled="demoPointStreaming" @click="startDemoPointStream">持续上报已显示点</button>
              <button class="demo-point-head-btn danger" type="button" :disabled="!demoPointStreaming" @click="stopDemoPointStream">停止</button>
            </div>
            <div class="demo-point-list">
              <div v-for="(point, index) in demoPointForms" :key="point.id" class="demo-point-row">
                <button
                  class="demo-point-toggle"
                  :class="{ on: point.visible }"
                  type="button"
                  @click="toggleDemoPointVisible(index)"
                >{{ point.visible ? '隐藏' : '显示' }}</button>
                <span class="demo-point-name">{{ point.name }}</span>
                <select
                  v-model="point.vehicleId"
                  class="demo-point-select"
                  @change="bindDemoPointVehicle(index)"
                >
                    <option value="">未绑定用户/车辆/BLE 标签</option>
                  <option v-for="item in demoBindingOptions" :key="item.vehicleId" :value="String(item.vehicleId)">
                    {{ formatDemoBindingOption(item) }}
                  </option>
                </select>
                <input
                  v-model="point.lng"
                  class="demo-point-input"
                  type="text"
                  inputmode="decimal"
                  placeholder="经度"
                  @keyup.enter="applyDemoPoint(index)"
                />
                <input
                  v-model="point.lat"
                  class="demo-point-input"
                  type="text"
                  inputmode="decimal"
                  placeholder="纬度"
                  @keyup.enter="applyDemoPoint(index)"
                />
                <button class="demo-point-btn primary" type="button" @click="applyDemoPoint(index)">显示</button>
                <button class="demo-point-btn warn" type="button" @click="pushDemoPointToBackend(index)">上报</button>
                <button class="demo-point-btn" type="button" @click="resetDemoPoint(index)">默认</button>
                <div class="demo-point-bind-text">{{ getDemoPointBindText(point) }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 设备列表表格 -->
        <div class="device-table-wrap">
          <table class="device-table">
            <thead>
              <tr><th>设备号</th><th>车牌</th><th>电量</th><th>信号</th><th>最后上报</th><th>状态</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="dev in filteredDevices" :key="dev.deviceId">
                <td>{{ dev.deviceNumber }}</td>
                <td>{{ dev.licensePlate || '未绑定' }}</td>
                <td>
                  <div class="battery-bar"><div class="battery-fill" :style="{width:dev.batteryLevel+'%', background:getBatteryColor(dev.batteryLevel)}"></div></div>
                  {{ dev.batteryLevel }}%
                </td>
                <td><span class="signal" :class="'level'+getSignalLevel(dev)">{{ getSignalText(dev) }}</span></td>
                <td>{{ formatTime(dev.lastReportTime) }}</td>
                <td><span class="dev-status" :class="dev.status">{{ getDevStatusLabel(dev.status) }}</span></td>
                <td>
                  <button class="table-btn" @click="locateDeviceOnMap(dev)">定位</button>
                  <button class="table-btn" @click="playDeviceTrack(dev)">轨迹</button>
                  <button class="table-btn" @click="viewDevDetail(dev)">详情</button>
                </td>
              </tr>
              <tr v-if="filteredDevices.length===0"><td colspan="7" class="empty-row">暂无设备数据</td></tr>
            </tbody>
          </table>
          <!-- 分页 -->
          <div class="page-controls">
            <div class="page-btn" :class="{disabled:page===1}" @click="changePage(-1)">←</div>
            <span class="page-info">{{ page }} / {{ totalPages }}</span>
            <div class="page-btn" :class="{disabled:page===totalPages}" @click="changePage(1)">→</div>
          </div>
        </div>

        <!-- 区域配置和阈值设置折叠面板 -->
        <div class="location-extra">
          <div class="extra-header" @click="showExtra = !showExtra">
            <span>区域配置与阈值</span>
            <span class="arrow">{{ showExtra ? '▼' : '▶' }}</span>
          </div>
          <div class="extra-body" v-show="showExtra">
            <!-- 区域列表 -->
            <div class="zone-list">
              <div v-for="zone in zones" :key="zone.id" class="zone-item" :class="{active:selectedZone===zone.id}" @click="selectedZone=zone.id">
                <span class="zone-icon">{{ zone.type==='forbidden' ? '禁' : '停' }}</span>
                <div class="zone-info">
                  <div class="zone-name">{{ zone.name }}</div>
                  <div class="zone-desc">{{ zone.desc }}</div>
                </div>
                <div class="zone-actions">
                  <span class="zone-badge" :class="zone.active?'on':'off'" @click.stop="toggleZone(zone)">{{ zone.active ? '已启用' : '已停用' }}</span>
                  <span class="zone-del" @click.stop="deleteZone(zone.id)">删</span>
                </div>
              </div>
            </div>
            <button class="action-btn primary small" @click="addZone" style="width:100%;margin-top:6px;">+ 新增区域</button>

            <div class="assist-panel">
              <div class="assist-title">
                <span>行驶轨迹配置</span>
                <em>{{ trackRouteSummary }}</em>
              </div>
              <div class="assist-form route-config-form">
                <label>车辆</label>
                <select v-model="trackRouteVehicleKey" class="assist-select route-select" @change="onTrackRouteVehicleChange">
                  <option value="">请选择车辆</option>
                  <option v-for="item in trackVehicleOptions" :key="item.key" :value="item.key">
                    {{ item.label }}
                  </option>
                </select>
              </div>
              <div class="track-route-hint" :class="{ on: trackRouteEditing }">
                {{ trackRouteEditing ? '请沿道路点击地图生成行驶路线' : '选择车辆后开始配置路线' }}
              </div>
              <div class="assist-actions">
                <button class="action-btn primary small" @click="toggleTrackRouteEditing">
                  {{ trackRouteEditing ? '结束配置' : '开始配置' }}
                </button>
                <button class="action-btn small" @click="undoTrackRoutePoint">撤销一点</button>
                <button class="action-btn small" @click="saveTrackRouteConfig">保存轨迹</button>
                <button class="action-btn small" @click="clearTrackRouteConfig">清空轨迹</button>
              </div>
            </div>

            <!-- 阈值设置简约版 -->
            <div class="threshold-mini">
              <div class="threshold-item">
                <label>超速阈值</label>
                <input type="number" v-model="thresholds.speed" min="5" max="50" class="threshold-input" /> km/h
              </div>
              <div class="threshold-item">
                <label>低电量预警</label>
                <input type="number" v-model="thresholds.battery" min="5" max="50" class="threshold-input" /> %
              </div>
              <div class="threshold-item">
                <label>离线预警</label>
                <input type="number" v-model="thresholds.offline" min="1" max="120" class="threshold-input" /> 分钟
              </div>
              <div class="threshold-item">
                <label>违停静置</label>
                <input type="number" v-model.number="parkingRuntimeConfig.stationaryMinutes" min="1" max="1440" class="threshold-input" /> 分钟
              </div>
              <div class="threshold-item">
                <label>移动阈值</label>
                <input type="number" v-model.number="parkingRuntimeConfig.moveThresholdMeters" min="1" max="200" class="threshold-input" /> 米
              </div>
              <div class="threshold-item">
                <label>防盗预警</label>
                <div class="toggle-wrap" @click="thresholds.antitheft = !thresholds.antitheft">
                  <div class="toggle-track" :class="{ on: thresholds.antitheft }">
                    <div class="toggle-thumb"></div>
                  </div>
                  <span>{{ thresholds.antitheft ? '已启用' : '已停用' }}</span>
                </div>
              </div>
              <button class="action-btn primary small" @click="saveParkingRuntimeConfig" style="margin-top:8px;">保存违停判定配置</button>
              <button class="action-btn primary small" @click="openThresholdDialog" style="margin-top:8px;">详细设置</button>
            </div>

            <div class="assist-panel">
              <div class="assist-title">
                <span>停车引导</span>
                <em>{{ parkingGuideSummary }}</em>
              </div>
              <div class="assist-form">
                <label>车位数</label>
                <input v-model.number="parkingGuideConfig.capacity" class="assist-input" type="number" min="1" max="999" />
                <label>满载阈值</label>
                <input v-model.number="parkingGuideConfig.threshold" class="assist-input" type="number" min="1" max="100" /> %
                <label>临时占用</label>
                <input v-model.number="parkingGuideConfig.extraOccupied" class="assist-input" type="number" min="0" max="999" />
              </div>
              <div class="parking-guide-list">
                <div v-for="row in parkingGuideRows" :key="row.id" class="parking-guide-row" :class="{ full: row.full }">
                  <span>{{ row.name }}</span>
                  <b>{{ row.occupied }}/{{ row.capacity }}</b>
                  <em>{{ row.full ? '车位已满' : '可停放' }}</em>
                </div>
                <div v-if="!parkingGuideRows.length" class="parking-guide-empty">暂无停车区域，请先新增停车围栏</div>
              </div>
              <div class="assist-form">
                <select v-model="parkingGuideConfig.receiverType" class="assist-select slim">
                  <option value="all">全部学生</option>
                  <option value="specified">指定用户ID</option>
                </select>
                <input
                  v-if="parkingGuideConfig.receiverType === 'specified'"
                  v-model="parkingGuideConfig.receiverIds"
                  class="assist-text"
                  type="text"
                  placeholder="多个ID用逗号分隔"
                />
              </div>
              <div class="assist-actions">
                <button class="action-btn primary small" @click="saveParkingGuideConfig">保存引导配置</button>
                <button class="action-btn primary small" @click="checkParkingGuide({ notify: true })">检查并下发</button>
                <button class="action-btn small" @click="sendParkingGuidance()">发送引导信息</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 阈值设置弹窗（独立） -->
    <div v-if="settingsDialog" class="modal-overlay" @click="settingsDialog = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">全局阈值配置</span>
          <span class="modal-close" @click="settingsDialog = false">×</span>
        </div>
        <div class="modal-body">
          <div class="settings-grid">
            <div class="setting-item">
              <div class="sl">超速阈值</div>
              <div class="sv"><input type="range" v-model="thresholds.speed" min="5" max="50" class="range-input" /> {{ thresholds.speed }} km/h</div>
            </div>
            <div class="setting-item">
              <div class="sl">低电量预警</div>
              <div class="sv"><input type="range" v-model="thresholds.battery" min="5" max="50" class="range-input" /> {{ thresholds.battery }}%</div>
            </div>
            <div class="setting-item">
              <div class="sl">离线预警</div>
              <div class="sv"><input type="range" v-model="thresholds.offline" min="1" max="120" class="range-input" /> {{ thresholds.offline }} 分钟</div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="action-btn" @click="settingsDialog = false">取消</div>
          <div class="action-btn primary" @click="saveThresholds">保存</div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      :title="detailDialog.title"
      v-model="detailDialog.visible"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="dialog-content" v-html="detailDialog.content"></div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialog.visible = false">关闭</el-button>
          <el-button type="primary" @click="detailDialog.visible = false">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElDialog, ElButton, ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'
import { getApiBaseUrl } from '@/utils/apiBase'

const API_BASE_URL = getApiBaseUrl()

const readTokenFromStorage = () => {
  const direct = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (direct) return direct
  const vuexRaw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex')
  if (!vuexRaw) return ''
  try {
    const parsed = JSON.parse(vuexRaw)
    return parsed?.user?.token || ''
  } catch {
    return ''
  }
}

axios.interceptors.request.use(config => {
  const token = readTokenFromStorage()
  config.headers = config.headers || {}
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
    config.headers.token = token
  }
  return config
})

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      const status = error.response.status
      const msg = error.response.data?.message || '请求失败'
      if (status === 401) ElMessage.error('登录已过期，请重新登录')
      else if (status === 500) ElMessage.error('服务器内部错误')
      else ElMessage.error(msg)
    } else {
      ElMessage.error('网络错误，请检查连接')
    }
    return Promise.reject(error)
  }
)

// ---------- 基础状态 ----------
const currentTime = ref('')
const currentDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})
const activeSearchTab = ref('vehicle')

// 3D地图状态
const is3D = ref(true)
const isSatellite = ref(false)
const showLabels = ref(false)
const DEMO_POINT_CACHE_KEY = 'ev_monitor_demo_points_v1'
const DEMO_POINT_BASE_LNG = 112.6827629
const DEMO_POINT_BASE_LAT = 26.8860959
const buildLegacyDemoPointDefault = (index) => {
  const col = index % 5
  const row = Math.floor(index / 5)
  return {
    lng: DEMO_POINT_BASE_LNG + (col - 2) * 0.00055,
    lat: DEMO_POINT_BASE_LAT + (row - 0.5) * 0.00055
  }
}
const DEMO_POINT_DEFAULTS = Object.freeze(Array.from({ length: 10 }, (_, index) => {
  const col = index % 5
  const row = Math.floor(index / 5)
  const stagger = row % 2 === 0 ? -0.00022 : 0.00032
  const latWave = col % 2 === 0 ? 0.00028 : -0.00022
  return {
    id: index + 1,
    name: `监测点${index + 1}`,
    lng: DEMO_POINT_BASE_LNG + (col - 2) * 0.00135 + stagger,
    lat: DEMO_POINT_BASE_LAT + (row - 0.5) * 0.00135 + latWave
  }
}))

const isSameCoord = (a, b) => Math.abs(Number(a) - Number(b)) < 0.0000001

const shouldUseSpreadDefault = (point, index) => {
  if (!point) return true
  if (point.vehicleId || point.userId || point.ownerName || point.licensePlate || point.rfid || point.frameNum) {
    return false
  }
  const legacy = buildLegacyDemoPointDefault(index)
  return isSameCoord(point.lng, legacy.lng) && isSameCoord(point.lat, legacy.lat)
}

const normalizeDemoPoint = (point, fallback) => ({
  id: fallback.id,
  name: fallback.name,
  visible: point?.visible !== false,
  lng: Number(point?.lng ?? fallback.lng).toFixed(7),
  lat: Number(point?.lat ?? fallback.lat).toFixed(7),
  vehicleId: point?.vehicleId != null ? String(point.vehicleId) : '',
  userId: point?.userId != null ? String(point.userId) : '',
  ownerName: point?.ownerName || '',
  licensePlate: point?.licensePlate || '',
  rfid: point?.rfid || '',
  frameNum: point?.frameNum || ''
})

const loadDemoPointForms = () => {
  try {
    const raw = localStorage.getItem(DEMO_POINT_CACHE_KEY)
    const saved = raw ? JSON.parse(raw) : []
    return DEMO_POINT_DEFAULTS.map((fallback, index) => {
      const point = Array.isArray(saved) ? saved[index] : null
      if (shouldUseSpreadDefault(point, index)) {
        const migrated = point ? { ...point, lng: fallback.lng, lat: fallback.lat } : null
        return normalizeDemoPoint(migrated, fallback)
      }
      return normalizeDemoPoint(point, fallback)
    })
  } catch {
    return DEMO_POINT_DEFAULTS.map((fallback) => normalizeDemoPoint(null, fallback))
  }
}

const demoPointForms = reactive(loadDemoPointForms())
const demoBindingOptions = ref([])
const demoPointStreaming = ref(false)
const demoPointMoving = ref(false)
let demoPointPushTimer = null
let demoPointMotionTick = 0
const demoPointMotionOrigins = new Map()

// ---------- 核心业务数据 ----------
const dashboardData = ref({ statistics: null, cameras: [], weather: null })
const alertList = ref([])

// 车辆搜索
const vehicleSearch = ref({ color: '', brand: '', plate: '' })
const vehicleColors = ref(['白色', '黑色', '红色', '蓝色', '灰色', '绿色', '黄色'])
const vehicleResults = ref([])
const vehiclePagination = ref({ pageNum: 1, pageSize: 6, total: 0, totalPages: 0 })

// 详情弹窗
const detailDialog = ref({ visible: false, title: '', content: '' })

// 百度地图实例
let map = null
const mapDataOverlays = []
const geofenceOverlays = []
const parkingGuideOverlays = []
const demoPointMarkers = new Map()
let mapContainerEl = null
let lastTrackRouteClick = { time: 0, lng: null, lat: null }

// ECharts 实例
let trendChart = null
let distChartAnalysis = null
let heatmapChartAnalysis = null

// ============================================================
// 实时位置：HTTP 轮询
// ============================================================
const vehicleMarkers = new Map()
let markerRefreshTimer = null
let locationPollTimer = null
let simLocationTimer = null
let simMotionTick = 0
const LOCATION_POLL_MS = 5000
const SIM_PUSH_MS = 5000

const LOCATION_CACHE_KEY = 'ev_monitor_vehicle_location_cache_v1'
const STALE_CLEAR_MINUTES = 120

const loadLocationCache = () => {
  try {
    const raw = localStorage.getItem(LOCATION_CACHE_KEY)
    if (!raw) return {}
    const o = JSON.parse(raw)
    return o && typeof o === 'object' ? o : {}
  } catch {
    return {}
  }
}

const saveLocationCache = (cache) => {
  try {
    localStorage.setItem(LOCATION_CACHE_KEY, JSON.stringify(cache))
  } catch (e) {
    console.warn('[Location] 写入本地缓存失败', e)
  }
}

let locationCache = loadLocationCache()

const createMarkerIcon = (color = '#00ff7f') => {
  const canvas = document.createElement('canvas')
  canvas.width = 32
  canvas.height = 40
  const ctx = canvas.getContext('2d')
  ctx.beginPath()
  ctx.arc(16, 14, 13, 0, Math.PI * 2)
  ctx.fillStyle = color + '44'
  ctx.fill()
  ctx.beginPath()
  ctx.arc(16, 14, 10, 0, Math.PI * 2)
  ctx.fillStyle = color
  ctx.fill()
  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 2
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(16, 14, 4, 0, Math.PI * 2)
  ctx.fillStyle = '#ffffff'
  ctx.fill()
  ctx.beginPath()
  ctx.moveTo(10, 22)
  ctx.lineTo(22, 22)
  ctx.lineTo(16, 34)
  ctx.closePath()
  ctx.fillStyle = color
  ctx.fill()
  return canvas.toDataURL()
}

const getMarkerColor = (updateTime) => {
  if (!updateTime) return '#ff4444'
  const diff = (Date.now() - new Date(updateTime).getTime()) / 1000
  if (diff < 5) return '#00ff7f'
  if (diff < 60) return '#ffaa00'
  return '#ff4444'
}

const getMarkerIcon = (updateTime) => {
  if (typeof BMapGL === 'undefined') return null
  const color = getMarkerColor(updateTime)
  const dataUrl = createMarkerIcon(color)
  return new BMapGL.Icon(dataUrl, new BMapGL.Size(32, 40), {
    anchor: new BMapGL.Size(16, 40)
  })
}

const getDemoPointIcon = () => {
  if (typeof BMapGL === 'undefined') return null
  return new BMapGL.Icon(createMarkerIcon('#00ff7f'), new BMapGL.Size(32, 40), {
    anchor: new BMapGL.Size(16, 40)
  })
}

const parseDemoCoord = (value) => Number(String(value).trim())

const saveDemoPointForms = () => {
  try {
    const raw = localStorage.getItem(DEMO_POINT_CACHE_KEY)
    const previous = raw ? JSON.parse(raw) : []
    localStorage.setItem(
      DEMO_POINT_CACHE_KEY,
      JSON.stringify(demoPointForms.map((point, index) => {
        const fallback = DEMO_POINT_DEFAULTS[index]
        const prev = Array.isArray(previous) ? previous[index] : null
        const lng = parseDemoCoord(point.lng)
        const lat = parseDemoCoord(point.lat)
        const prevLng = parseDemoCoord(prev?.lng)
        const prevLat = parseDemoCoord(prev?.lat)
        return {
          id: point.id,
          visible: point.visible !== false,
          lng: Number.isFinite(lng) ? lng : (Number.isFinite(prevLng) ? prevLng : fallback.lng),
          lat: Number.isFinite(lat) ? lat : (Number.isFinite(prevLat) ? prevLat : fallback.lat),
          vehicleId: point.vehicleId || '',
          userId: point.userId || '',
          ownerName: point.ownerName || '',
          licensePlate: point.licensePlate || '',
          rfid: point.rfid || '',
          frameNum: point.frameNum || ''
        }
      }))
    )
  } catch (e) {
    console.warn('[TerminalPoint] 保存定位点失败', e)
  }
}

const setDemoPointMotionOrigin = (pointForm) => {
  const lng = parseDemoCoord(pointForm?.lng)
  const lat = parseDemoCoord(pointForm?.lat)
  if (!pointForm || !Number.isFinite(lng) || !Number.isFinite(lat)) return
  demoPointMotionOrigins.set(pointForm.id, { lng, lat })
}

const resetDemoPointMotionOrigins = () => {
  demoPointMotionOrigins.clear()
  demoPointForms.forEach(setDemoPointMotionOrigin)
}

const toggleDemoPointMoving = () => {
  demoPointMoving.value = !demoPointMoving.value
  if (demoPointMoving.value) {
    resetDemoPointMotionOrigins()
    ElMessage.success('监测点移动已开启')
  } else {
    ElMessage.success('监测点移动已关闭')
  }
}

const getDemoPointBindText = (point) => {
  if (!point?.vehicleId) return '未绑定'
  return `${point.ownerName || '未知用户'} / ${point.licensePlate || '未登记车牌'} / BLE:${point.rfid || '--'}`
}

const trackReplaySources = new Map()
const escapeMapHtml = (value) => String(value ?? '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const normalizePopupKey = (value) => {
  const raw = String(value || `track-${Date.now()}`)
  return raw.replace(/[^\w-]/g, '_').slice(0, 80) || `track-${Date.now()}`
}

const registerTrackReplaySource = (source) => {
  const key = normalizePopupKey(source?.key || source?.label)
  trackReplaySources.set(key, { ...source, key })
  return key
}

const buildTrackReplayControlHtml = (source) => {
  const key = registerTrackReplaySource(source)
  const label = escapeMapHtml(source?.label || '当前点位')
  return `
    <div style="margin-top:10px;padding-top:10px;border-top:1px solid rgba(39,212,255,.32);color:#d7f7ff;">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
        <strong style="color:#7dfcff;font-size:13px;">历史轨迹</strong>
        <span style="max-width:170px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#9fe8ff;font-size:11px;">${label}</span>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px;">
        <label style="display:flex;align-items:center;gap:5px;font-size:12px;">时长
          <input id="track-min-${key}" type="number" min="5" max="180" value="${Number(trackReplayForm.minutes) || 30}"
            style="width:58px;background:#07182c;border:1px solid rgba(39,212,255,.45);border-radius:4px;color:#fff;padding:4px 5px;outline:none;" />分
        </label>
        <label style="display:flex;align-items:center;gap:5px;font-size:12px;">速度
          <input id="track-speed-${key}" type="number" min="1" max="8" value="${Number(trackReplayForm.speed) || 2}"
            style="width:50px;background:#07182c;border:1px solid rgba(39,212,255,.45);border-radius:4px;color:#fff;padding:4px 5px;outline:none;" />倍
        </label>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:6px;">
        <button onclick="window.__evTrackReplay && window.__evTrackReplay('start','${key}')" style="border:0;border-radius:4px;background:#00d4ff;color:#02101f;padding:6px 4px;font-weight:700;cursor:pointer;">开始</button>
        <button onclick="window.__evTrackReplay && window.__evTrackReplay('pause','${key}')" style="border:1px solid rgba(39,212,255,.5);border-radius:4px;background:rgba(39,212,255,.12);color:#bff7ff;padding:6px 4px;cursor:pointer;">暂停</button>
        <button onclick="window.__evTrackReplay && window.__evTrackReplay('clear','${key}')" style="border:1px solid rgba(255,255,255,.24);border-radius:4px;background:rgba(255,255,255,.08);color:#fff;padding:6px 4px;cursor:pointer;">清除</button>
      </div>
    </div>
  `
}

const buildDemoPointInfoHtml = (point, lng, lat) => `
  <div style="padding:10px;min-width:190px;font-size:13px;background:#0a1a2f;color:#27ff9a;border-radius:6px;">
    <div style="font-size:15px;font-weight:bold;margin-bottom:8px;color:#00ff7f;border-bottom:1px solid rgba(0,255,127,.55);padding-bottom:6px;">${escapeMapHtml(point.name)}</div>
    <p style="margin:4px 0">经度：${lng.toFixed(7)}</p>
    <p style="margin:4px 0">纬度：${lat.toFixed(7)}</p>
    <p style="margin:4px 0">绑定：${escapeMapHtml(getDemoPointBindText(point))}</p>
    <p style="margin:6px 0 0;color:#85ffd0;font-size:11px">绿色常驻点位</p>
    ${buildTrackReplayControlHtml({
      key: getVehicleRouteKey(point) || `terminal-${point.id || point.licensePlate || point.name}`,
      routeKey: getVehicleRouteKey(point),
      label: point.licensePlate ? `${point.licensePlate} / ${point.name}` : point.name,
      lng,
      lat
    })}
  </div>
`

const removeDemoPointMarker = (id) => {
  const marker = demoPointMarkers.get(id)
  if (!marker) return
  try { map?.removeOverlay(marker) } catch (e) { /* ignore */ }
  demoPointMarkers.delete(id)
}

const renderDemoPointMarker = (pointForm, { moveMap = false, silent = false, persist = false } = {}) => {
  if (!map || typeof BMapGL === 'undefined') return
  if (pointForm?.visible === false) {
    removeDemoPointMarker(pointForm.id)
    if (persist) saveDemoPointForms()
    return
  }
  const lng = parseDemoCoord(pointForm?.lng)
  const lat = parseDemoCoord(pointForm?.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat) || Math.abs(lng) > 180 || Math.abs(lat) > 90) {
    if (!silent) ElMessage.warning('请输入正确的经纬度')
    return
  }

  const point = new BMapGL.Point(lng, lat)
  const icon = getDemoPointIcon()
  let marker = demoPointMarkers.get(pointForm.id)
  if (!marker) {
    marker = icon ? new BMapGL.Marker(point, { icon }) : new BMapGL.Marker(point)
    marker._demoPoint = pointForm
    marker.addEventListener('click', () => {
      const pos = marker.getPosition()
      const meta = marker._demoPoint || pointForm
      map.openInfoWindow(
        new BMapGL.InfoWindow(buildDemoPointInfoHtml(meta, pos.lng, pos.lat), { width: 340, enableCloseOnClick: true }),
        pos
      )
    })
    map.addOverlay(marker)
    demoPointMarkers.set(pointForm.id, marker)
  } else {
    marker.setPosition(point)
    marker._demoPoint = pointForm
    if (icon) marker.setIcon(icon)
  }

  if (moveMap) {
    map.panTo(point)
    map.setZoom(Math.max(map.getZoom?.() || 18, 18))
    keep3DMapView()
  }
  if (persist) saveDemoPointForms()
  if (!silent) ElMessage.success(`${pointForm.name}已保存并显示`)
}

const renderAllDemoPointMarkers = ({ silent = true } = {}) => {
  demoPointForms.forEach((pointForm) => renderDemoPointMarker(pointForm, { silent }))
}

const applyDemoPoint = (index) => {
  const pointForm = demoPointForms[index]
  if (!pointForm) return
  pointForm.visible = true
  if (demoPointMoving.value) setDemoPointMotionOrigin(pointForm)
  renderDemoPointMarker(pointForm, { moveMap: true, persist: true })
}

const resetDemoPoint = (index) => {
  const pointForm = demoPointForms[index]
  const fallback = DEMO_POINT_DEFAULTS[index]
  if (!pointForm || !fallback) return
  pointForm.lng = fallback.lng.toFixed(7)
  pointForm.lat = fallback.lat.toFixed(7)
  pointForm.visible = true
  if (demoPointMoving.value) setDemoPointMotionOrigin(pointForm)
  renderDemoPointMarker(pointForm, { moveMap: true, persist: true })
}

const toggleDemoPointVisible = (index) => {
  const pointForm = demoPointForms[index]
  if (!pointForm) return
  const willShow = pointForm.visible === false
  pointForm.visible = willShow
  if (willShow) {
    if (demoPointMoving.value) setDemoPointMotionOrigin(pointForm)
    renderDemoPointMarker(pointForm, { persist: true })
    ElMessage.success(`${pointForm.name}已显示`)
  } else {
    removeDemoPointMarker(pointForm.id)
    saveDemoPointForms()
    ElMessage.success(`${pointForm.name}已隐藏`)
  }
}

const showAllDemoPoints = () => {
  demoPointForms.forEach((pointForm) => {
    pointForm.visible = true
    if (demoPointMoving.value) setDemoPointMotionOrigin(pointForm)
    renderDemoPointMarker(pointForm, { silent: true })
  })
  saveDemoPointForms()
  ElMessage.success('全部监测点已显示')
}

const spreadAllDemoPoints = () => {
  demoPointForms.forEach((pointForm, index) => {
    const fallback = DEMO_POINT_DEFAULTS[index]
    if (!fallback) return
    pointForm.lng = fallback.lng.toFixed(7)
    pointForm.lat = fallback.lat.toFixed(7)
    pointForm.visible = true
    if (demoPointMoving.value) setDemoPointMotionOrigin(pointForm)
    renderDemoPointMarker(pointForm, { silent: true })
  })
  saveDemoPointForms()
  ElMessage.success('监测点已重新分布')
}

const hideAllDemoPoints = ({ silent = false } = {}) => {
  demoPointForms.forEach((pointForm) => {
    pointForm.visible = false
    removeDemoPointMarker(pointForm.id)
  })
  saveDemoPointForms()
  if (!silent) ElMessage.success('全部监测点已隐藏')
}

const removeDemoPointMarkers = () => {
  for (const marker of demoPointMarkers.values()) {
    try { map?.removeOverlay(marker) } catch (e) { /* ignore */ }
  }
  demoPointMarkers.clear()
}

const normalizeApiList = (res) => {
  const data = res?.data?.data
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  return []
}

const fetchDemoBindingOptions = async () => {
  try {
    const [vehicleRes, userRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/vehicle/list`, { params: { pageNum: 1, pageSize: 300 } }),
      axios.get(`${API_BASE_URL}/user/list`, { params: { pageNum: 1, pageSize: 300 } }).catch(() => null)
    ])
    const users = normalizeApiList(userRes)
    const userById = new Map(users.map((u) => [String(u.userId), u]))
    demoBindingOptions.value = normalizeApiList(vehicleRes)
      .filter((v) => v && v.vehicleId != null)
      .map((v) => {
        const u = userById.get(String(v.userId)) || {}
        return {
          vehicleId: String(v.vehicleId),
          userId: v.userId != null ? String(v.userId) : '',
          ownerName: v.ownerName || u.realName || u.username || '',
          licensePlate: v.licensePlate || '',
          rfid: v.etcNumber || v.rfid || '',
          frameNum: v.frameNum || '',
          brand: v.brand || '',
          color: v.color || ''
        }
      })
    ElMessage.success('已刷新用户/车辆/BLE 标签绑定数据')
  } catch (e) {
    console.error('刷新绑定数据失败:', e)
    ElMessage.error('刷新绑定数据失败')
  }
}

const formatDemoBindingOption = (item) => {
  const owner = item.ownerName || '未知用户'
  const plate = item.licensePlate || '未登记车牌'
  const rfid = item.rfid ? `BLE:${item.rfid}` : 'BLE:--'
  return `${owner} / ${plate} / ${rfid}`
}

const bindDemoPointVehicle = (index) => {
  const pointForm = demoPointForms[index]
  if (!pointForm) return
  const item = demoBindingOptions.value.find((v) => String(v.vehicleId) === String(pointForm.vehicleId))
  if (!item) {
    pointForm.vehicleId = ''
    pointForm.userId = ''
    pointForm.ownerName = ''
    pointForm.licensePlate = ''
    pointForm.rfid = ''
    pointForm.frameNum = ''
  } else {
    pointForm.userId = item.userId
    pointForm.ownerName = item.ownerName
    pointForm.licensePlate = item.licensePlate
    pointForm.rfid = item.rfid
    pointForm.frameNum = item.frameNum
  }
  saveDemoPointForms()
  renderDemoPointMarker(pointForm, { silent: true })
}

const getDemoPointReportId = (pointForm) => {
  return (pointForm.frameNum || pointForm.licensePlate || '').trim()
}

const pushDemoPointToBackend = async (index, { silent = false } = {}) => {
  const pointForm = demoPointForms[index]
  if (!pointForm) return false
  const lng = parseDemoCoord(pointForm.lng)
  const lat = parseDemoCoord(pointForm.lat)
  if (pointForm.visible === false) {
    if (!silent) ElMessage.warning('该监测点已隐藏，先显示后再上报')
    return false
  }
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    if (!silent) ElMessage.warning('请输入正确的经纬度')
    return false
  }
  const reportId = getDemoPointReportId(pointForm)
  if (!reportId) {
    if (!silent) ElMessage.warning('请先绑定车辆，或确保车辆有车架号/车牌')
    return false
  }
  try {
    await axios.post(`${API_BASE_URL}/location/mock`, {
      topic: 'position',
      N: lat,
      W: lng,
      D: reportId,
      licensePlate: pointForm.licensePlate,
      source: 'screen-terminal-point'
    })
    await fetchAllLocations()
    if (locationDialogVisible.value) await fetchDevices()
    if (!silent) ElMessage.success(`${pointForm.name}已上报，围栏/违停逻辑已联动`)
    return true
  } catch (e) {
    console.error('监测点上报失败:', e)
    if (!silent) ElMessage.error('监测点上报失败')
    return false
  }
}

const pushVisibleDemoPointsToBackend = async ({ silent = false } = {}) => {
  const indexes = demoPointForms
    .map((p, index) => ({ p, index }))
    .filter(({ p }) => p.visible !== false && getDemoPointReportId(p))
    .map(({ index }) => index)
  if (!indexes.length) {
    if (!silent) ElMessage.warning('没有可上报的已显示且已绑定点位')
    return false
  }
  if (demoPointMoving.value) {
    advanceDemoPointMotion(indexes)
  }
  for (const index of indexes) {
    await pushDemoPointToBackend(index, { silent: true })
  }
  if (!silent) ElMessage.success(`已上报 ${indexes.length} 个监测点`)
  return true
}

const startDemoPointStream = async () => {
  const ok = await pushVisibleDemoPointsToBackend()
  if (!ok) return
  stopDemoPointStream({ silent: true })
  demoPointStreaming.value = true
  demoPointPushTimer = setInterval(() => {
    pushVisibleDemoPointsToBackend({ silent: true }).catch((e) => console.error('持续上报绑定监测点失败:', e))
  }, SIM_PUSH_MS)
  ElMessage.success('已开启绑定监测点持续上报')
}

const stopDemoPointStream = ({ silent = false } = {}) => {
  if (demoPointPushTimer) {
    clearInterval(demoPointPushTimer)
    demoPointPushTimer = null
  }
  demoPointStreaming.value = false
  if (!silent) ElMessage.success('已停止绑定监测点持续上报')
}

const locationMapKey = (vo) => {
  if (!vo) return ''
  const plate = vo.licensePlate != null ? String(vo.licensePlate).trim() : ''
  if (plate) return plate
  const dev = vo.deviceId != null ? String(vo.deviceId).trim() : ''
  return dev
}

const mergeVoIntoLocationCache = (vo) => {
  const id = locationMapKey(vo)
  if (!id || vo.lat == null || vo.lng == null) return
  const prev = locationCache[id] || {}
  locationCache[id] = {
    ...prev,
    licensePlate: vo.licensePlate,
    deviceId: vo.deviceId,
    vehicleId: vo.vehicleId,
    ownerName: vo.ownerName,
    userName: vo.userName,
    unbound: vo.unbound,
    lng: Number(vo.lng),
    lat: Number(vo.lat),
    speed: vo.speed,
    battery: vo.battery,
    updateTime:
      vo.updateTime != null && vo.updateTime !== ''
        ? vo.updateTime
        : (prev.updateTime || new Date().toISOString())
  }
}

const formatLocField = (v) => (v === null || v === undefined || v === '' ? '--' : v)

const buildLocationInfoHtml = (v) => {
  const unbound = v.unbound === true || (!v.licensePlate && v.deviceId)
  const title = unbound ? '未注册设备' : `${formatLocField(v.licensePlate)}`
  const lng = Number(v.lng)
  const lat = Number(v.lat)
  return `
    <div style="padding:10px;min-width:200px;font-size:13px;background:#0a1a2f;color:#27d4ff;border-radius:6px;">
      <div style="font-size:15px;font-weight:bold;margin-bottom:8px;color:#00d4ff;border-bottom:1px solid #27d4ff;padding-bottom:6px;">${escapeMapHtml(title)}</div>
      <p style="margin:4px 0">设备号：${escapeMapHtml(formatLocField(v.deviceId))}</p>
      <p style="margin:4px 0">车辆ID：${v.vehicleId != null ? v.vehicleId : '--'}</p>
      <p style="margin:4px 0">车牌：${escapeMapHtml(formatLocField(v.licensePlate))}</p>
      <p style="margin:4px 0">车主：${escapeMapHtml(formatLocField(v.ownerName || v.userName))}</p>
      <p style="margin:4px 0">速度：${v.speed != null ? v.speed : '--'} km/h</p>
      <p style="margin:4px 0">电量：${v.battery != null ? v.battery : '--'}%</p>
      <p style="margin:4px 0">坐标：${Number.isFinite(lng) ? lng.toFixed(6) : '--'}, ${Number.isFinite(lat) ? lat.toFixed(6) : '--'}</p>
      <p style="margin:4px 0;color:#888;font-size:11px">更新时间：${escapeMapHtml(formatLocField(v.updateTime))}</p>
      ${unbound ? '<p style="margin:8px 0 0;color:#ffaa00;font-size:11px">绑定车辆后将显示完整登记信息</p>' : ''}
      ${Number.isFinite(lng) && Number.isFinite(lat) ? buildTrackReplayControlHtml({
        key: getVehicleRouteKey(v) || `vehicle-${v.licensePlate || v.deviceId || v.vehicleId || 'point'}`,
        routeKey: getVehicleRouteKey(v),
        label: v.licensePlate || v.deviceId || '当前车辆',
        lng,
        lat
      }) : ''}
    </div>
  `
}

const updateVehicleMarker = (vo) => {
  if (!map || vo.lat == null || vo.lng == null) return
  if (typeof BMapGL === 'undefined') return
  const id = locationMapKey(vo)
  if (!id) return
  const point = new BMapGL.Point(vo.lng, vo.lat)
  if (vehicleMarkers.has(id)) {
    const marker = vehicleMarkers.get(id)
    marker.setPosition(point)
    marker._updateTime = vo.updateTime
    marker._vo = vo
    const icon = getMarkerIcon(vo.updateTime)
    if (icon) marker.setIcon(icon)
  } else {
    const icon = getMarkerIcon(vo.updateTime)
      || new BMapGL.Icon(createMarkerIcon('#00ff7f'), new BMapGL.Size(32, 40), { anchor: new BMapGL.Size(16, 40) })
    const marker = new BMapGL.Marker(point, { icon })
    marker._updateTime = vo.updateTime
    marker._vo = vo
    marker.addEventListener('click', () => {
      const v = marker._vo || vo
      map.openInfoWindow(
        new BMapGL.InfoWindow(buildLocationInfoHtml(v), { width: 340, enableCloseOnClick: true }),
        marker.getPosition()
      )
    })
    map.addOverlay(marker)
    vehicleMarkers.set(id, marker)
  }
}

const fetchAllLocations = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/location/all`)
    if (res.data.code !== 200) return
    const list = res.data.data || []
    list.forEach(vehicle => {
      if (vehicle.lat !== null && vehicle.lng !== null) {
        console.log(`MQTT收到消息 topic=position, 设备=${vehicle.deviceId || '未知'}, 车牌=${vehicle.licensePlate || '未绑定'}, 经度=${vehicle.lng}, 纬度=${vehicle.lat}`)
      }
    })
    list.forEach(mergeVoIntoLocationCache)
    saveLocationCache(locationCache)
    const validKeys = new Set(Object.keys(locationCache))
    for (const [key, marker] of [...vehicleMarkers.entries()]) {
      if (!validKeys.has(key)) {
        try { map?.removeOverlay(marker) } catch (e) { /* ignore */ }
        vehicleMarkers.delete(key)
      }
    }
    Object.values(locationCache).forEach((vo) => updateVehicleMarker(vo))
    handleParkingGuideState()
  } catch (e) {
    console.error('[Location] 拉取位置失败', e)
  }
}

const countStaleCacheKeys = () => {
  const ms = STALE_CLEAR_MINUTES * 60 * 1000
  const now = Date.now()
  let n = 0
  for (const key of Object.keys(locationCache)) {
    const vo = locationCache[key]
    const t = vo?.updateTime ? new Date(vo.updateTime).getTime() : 0
    if (!t || now - t > ms) n++
  }
  return n
}

const clearStaleVehicleMarkers = () => {
  const ms = STALE_CLEAR_MINUTES * 60 * 1000
  const now = Date.now()
  let n = 0
  for (const key of Object.keys(locationCache)) {
    const vo = locationCache[key]
    const t = vo?.updateTime ? new Date(vo.updateTime).getTime() : 0
    if (!t || now - t > ms) {
      delete locationCache[key]
      n++
      const marker = vehicleMarkers.get(key)
      if (marker) {
        try { map?.removeOverlay(marker) } catch (e) { /* ignore */ }
        vehicleMarkers.delete(key)
      }
    }
  }
  saveLocationCache(locationCache)
  ElMessage.success(n ? `已清理 ${n} 个超时点位标记` : '没有超时标记可清理')
}

const confirmClearStaleMarkers = async () => {
  const n = countStaleCacheKeys()
  try {
    await ElMessageBox.confirm(
      `将清除地图上超过 ${STALE_CLEAR_MINUTES} 分钟未更新的位置标记（当前约 ${n} 个）。确定吗？`,
      '清除陈旧点位',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  clearStaleVehicleMarkers()
}

const startLocationPolling = () => {
  if (locationPollTimer) clearInterval(locationPollTimer)
  locationPollTimer = setInterval(fetchAllLocations, LOCATION_POLL_MS)
}

const stopLocationPolling = () => {
  if (locationPollTimer) {
    clearInterval(locationPollTimer)
    locationPollTimer = null
  }
}

const startMarkerRefreshTimer = () => {
  markerRefreshTimer = setInterval(() => {
    vehicleMarkers.forEach((marker) => {
      if (marker._updateTime) {
        const icon = getMarkerIcon(marker._updateTime)
        if (icon) marker.setIcon(icon)
      }
    })
  }, 30000)
}

// ---------- 时间更新 ----------
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  }).replace(/\//g, '-')
}

// ---------- API 请求 ----------
const enrichDashboardPanels = async () => {
  try {
    const [statsRes, weatherRes, camRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/monitor/statistics`).catch(() => null),
      axios.get(`${API_BASE_URL}/monitor/weather`).catch(() => null),
      axios.get(`${API_BASE_URL}/monitor/cameras`).catch(() => null)
    ])
    const d = dashboardData.value
    if (statsRes?.data?.code === 200 && statsRes.data.data) {
      d.statistics = { ...(d.statistics || {}), ...statsRes.data.data }
    }
    if (weatherRes?.data?.code === 200 && weatherRes.data.data) {
      d.weather = weatherRes.data.data
    }
    if (camRes?.data?.code === 200 && Array.isArray(camRes.data.data) && camRes.data.data.length) {
      if (!d.cameras || !d.cameras.length) d.cameras = camRes.data.data
    }
  } catch { /* 忽略单项失败 */ }
}

const fetchDashboardData = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/monitor/dashboard`)
    if (response.data.code === 200) {
      dashboardData.value = response.data.data
      await enrichDashboardPanels()
    }
  } catch (error) {
    console.error('获取仪表盘数据失败', error)
  }
}

const fetchViolationAlerts = async () => {
  try {
    const [violationResp, geofenceResp] = await Promise.all([
      axios.get(`${API_BASE_URL}/violation/list`, { params: { pageNum: 1, pageSize: 8 } }),
      axios.get(`${API_BASE_URL}/geofence/alerts`, { params: { limit: 8 } })
    ])
    const violationAlerts = []
    if (violationResp.data.code === 200) {
      const list = violationResp.data.data || []
      list.forEach((item) => {
        const hasPoint = Number.isFinite(Number(item.illegalLng)) && Number.isFinite(Number(item.illegalLat))
        violationAlerts.push({
          location: hasPoint ? `${Number(item.illegalLng).toFixed(5)}, ${Number(item.illegalLat).toFixed(5)}` : '校园区域',
          time: item.illegalTime || item.createTime || '未知时间',
          description: item.illegalTypeName || item.typeName || `违规类型: ${item.illegalType || '未知'}`,
          level: item.deductionPoints > 3 ? 'high' : 'medium',
          status: getViolationStatus(item.illegalState),
          originalData: item
        })
      })
    }
    const fenceAlerts = []
    if (geofenceResp.data.code === 200) {
      const rows = geofenceResp.data.data || []
      rows.forEach((item) => {
        const hasPoint = Number.isFinite(Number(item.triggerLng)) && Number.isFinite(Number(item.triggerLat))
        fenceAlerts.push({
          location: hasPoint ? `${Number(item.triggerLng).toFixed(5)}, ${Number(item.triggerLat).toFixed(5)}` : (item.fenceName || '电子围栏'),
          time: item.createTime || '未知时间',
          description: `[围栏] ${item.fenceName || '未命名围栏'} - ${item.alertType === 'exit' ? '驶出告警' : '进入告警'}`,
          level: Number(item.alertLevel) >= 3 ? 'high' : 'medium',
          status: Number(item.isProcessed) === 1 ? '已处理' : '待处理',
          originalData: item
        })
      })
    }
    const monitorAlerts = []
    try {
      const monRes = await axios.get(`${API_BASE_URL}/monitor/alerts`, { params: { limit: 10 } })
      if (monRes.data?.code === 200 && Array.isArray(monRes.data.data)) {
        monRes.data.data.forEach((a) => {
          monitorAlerts.push({
            location: a.location || '—', time: a.time || '—',
            description: a.description || '监控告警', level: a.level || 'medium',
            status: a.status || '—', originalData: a
          })
        })
      }
    } catch { /* ignore */ }
    alertList.value = [...monitorAlerts, ...fenceAlerts, ...violationAlerts]
      .sort((a, b) => {
        const ta = new Date(a.time).getTime()
        const tb = new Date(b.time).getTime()
        if (Number.isNaN(ta) || Number.isNaN(tb)) return 0
        return tb - ta
      })
      .slice(0, 15)
  } catch (error) {
    console.error('获取违规记录失败:', error)
  }
}

const getViolationStatus = (state) => {
  const map = { 0: '待整改', 1: '整改中', 2: '已整改', 3: '已逾期', 4: '申诉中' }
  return map[state] || '未知'
}

const fetchMapData = async () => {
  if (!map) return
  try {
    const response = await axios.get(`${API_BASE_URL}/monitor/map-data`)
    if (response.data.code === 200) updateMapMarkers(response.data.data)
  } catch (error) {
    console.error('获取地图数据失败:', error)
  }
}

const clearMapDataOverlays = () => {
  while (mapDataOverlays.length) {
    const overlay = mapDataOverlays.pop()
    try { map?.removeOverlay(overlay) } catch (e) { /* ignore */ }
  }
}

const clearGeofenceOverlays = () => {
  while (geofenceOverlays.length) {
    const overlay = geofenceOverlays.pop()
    try { map?.removeOverlay(overlay) } catch (e) { /* ignore */ }
  }
}

const parseFenceCoordinates = (coordinates) => {
  if (!coordinates) return []
  try {
    const arr = typeof coordinates === 'string' ? JSON.parse(coordinates) : coordinates
    if (!Array.isArray(arr)) return []
    return arr
      .map((p) => ({ lng: Number(p?.lng), lat: Number(p?.lat) }))
      .filter((p) => Number.isFinite(p.lng) && Number.isFinite(p.lat))
  } catch (e) {
    return []
  }
}

const drawGeofencePolygons = () => {
  if (!map || typeof BMapGL === 'undefined') return
  clearGeofenceOverlays()
  zones.value.forEach((zone) => {
    const pointsRaw = parseFenceCoordinates(zone.coordinates)
    if (pointsRaw.length < 3) return
    const points = pointsRaw.map((p) => new BMapGL.Point(p.lng, p.lat))
    const strokeColor = zone.fenceColor || '#00d4ff'
    const polygon = new BMapGL.Polygon(points, {
      strokeColor, fillColor: strokeColor, strokeWeight: 2, strokeOpacity: 0.95,
      fillOpacity: zone.active ? 0.18 : 0.06,
      strokeStyle: zone.active ? 'solid' : 'dashed'
    })
    polygon.addEventListener('click', () => { selectedZone.value = zone.id })
    map.addOverlay(polygon)
    geofenceOverlays.push(polygon)
  })
}

const updateMapMarkers = (mapData) => {
  if (!map || !mapData) return
  clearMapDataOverlays()
  if (mapData.markers && mapData.markers.length > 0) {
    mapData.markers.forEach(marker => {
      const point = new BMapGL.Point(marker.lng, marker.lat)
      let icon = null
      if (marker.type === 'camera') icon = new BMapGL.Icon('/img/map_camera.png', new BMapGL.Size(32, 32))
      else if (marker.type === 'violation') icon = new BMapGL.Icon('/img/map_parking.png', new BMapGL.Size(32, 32))
      const markerObj = new BMapGL.Marker(point, { icon })
      const infoHtml = `
        <div style="padding:10px; min-width:200px;">
          <h4 style="margin:0 0 10px; color:#4b8df8;">${marker.title}</h4>
          <p><strong>类型：</strong>${marker.type === 'camera' ? '摄像头' : '违规点'}</p>
          <p><strong>状态：</strong>${marker.status || marker.state}</p>
          ${marker.description ? `<p><strong>描述：</strong>${marker.description}</p>` : ''}
        </div>
      `
      markerObj.addEventListener('click', () => {
        map.openInfoWindow(new BMapGL.InfoWindow(infoHtml), point)
      })
      map.addOverlay(markerObj)
      mapDataOverlays.push(markerObj)
    })
  }
  drawGeofencePolygons()
  fetchAllLocations()
}

const fetchRealtimeData = async () => {
  try {
    await axios.get(`${API_BASE_URL}/monitor/realtime`)
  } catch (error) {
    console.error('获取实时数据失败:', error)
  }
}

const searchVehicles = async () => {
  try {
    const params = {
      pageNum: vehiclePagination.value.pageNum,
      pageSize: vehiclePagination.value.pageSize
    }
    if (vehicleSearch.value.color) params.color = vehicleSearch.value.color
    if (vehicleSearch.value.brand) params.brand = vehicleSearch.value.brand
    if (vehicleSearch.value.plate) params.licensePlate = vehicleSearch.value.plate
    const response = await axios.get(`${API_BASE_URL}/vehicle/list`, { params })
    if (response.data.code === 200) {
      vehicleResults.value = response.data.data || []
      vehiclePagination.value.total = response.data.total || 0
      vehiclePagination.value.totalPages = response.data.totalPages || 1
      if (vehicleResults.value.length === 0) ElMessage.info('未搜索到符合条件的车辆')
    } else {
      ElMessage.error(response.data.message || '搜索车辆失败')
    }
  } catch (error) {
    console.error('搜索车辆失败:', error)
    ElMessage.error('搜索车辆失败，请稍后重试')
  }
}

// ---------- 违规统计（图表用） ----------
const fetchViolationStats = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/violation/stats`)
    if (response.data.code === 200) return response.data.data
  } catch (error) {
    console.error('获取违规统计失败:', error)
  }
  return null
}

// ---------- 中间趋势图 ----------
const initTrendChart = async () => {
  const chartDom = document.getElementById('chart-violation-trend')
  if (!chartDom) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(chartDom)
  const dates = [], counts = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth()+1}/${d.getDate()}`)
    counts.push(Math.floor(Math.random() * 20 + 5))
  }
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '3%', top: '15%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#85acd5', fontSize: 10 } },
    yAxis: { type: 'value', axisLabel: { color: '#85acd5' }, splitLine: { lineStyle: { color: 'rgba(133,172,213,0.2)' } } },
    series: [{
      data: counts, type: 'line', smooth: true,
      lineStyle: { color: '#ff9800', width: 2 },
      areaStyle: { color: 'rgba(255,152,0,0.1)' },
      symbol: 'circle', symbolSize: 6
    }]
  })
}

// ---------- 右侧违规类型分布图 ----------
const initDistChartAnalysis = async () => {
  const chartDom = document.getElementById('chart-violation-dist')
  if (!chartDom) return
  if (distChartAnalysis) distChartAnalysis.dispose()
  distChartAnalysis = echarts.init(chartDom)
  let stats = await fetchViolationStats()
  let typeData = stats?.typeStats
    ? stats.typeStats.map(item => ({ name: getViolationTypeName(item.type), value: item.count }))
    : [
        { name: '违规停放', value: 35 }, { name: '超速行驶', value: 28 },
        { name: '违规充电', value: 12 }, { name: '未登记', value: 8 },
        { name: '禁行区', value: 5 }
      ]
  distChartAnalysis.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '10%', right: '3%', top: '10%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', data: typeData.map(d => d.name), axisLabel: { color: '#85acd5', fontSize: 10, rotate: 15 } },
    yAxis: { type: 'value', axisLabel: { color: '#85acd5' }, splitLine: { lineStyle: { color: 'rgba(133,172,213,0.2)' } } },
    series: [{
      data: typeData.map(d => d.value), type: 'bar', barWidth: '60%',
      itemStyle: { color: '#1f9dff', borderRadius: [4, 4, 0, 0] }
    }]
  })
}

// ---------- 右侧热力图 ----------
const initHeatmapChartAnalysis = async () => {
  const chartDom = document.getElementById('chart-heatmap')
  if (!chartDom) return
  if (heatmapChartAnalysis) heatmapChartAnalysis.dispose()
  heatmapChartAnalysis = echarts.init(chartDom)
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const hours = Array.from({ length: 24 }, (_, i) => `${i}时`)
  const data = []
  for (let d = 0; d < 7; d++) {
    for (let h = 0; h < 24; h++) {
      let base = 5
      if (h >= 11 && h <= 13) base = 15
      else if (h >= 17 && h <= 19) base = 18
      else if (h >= 22 || h <= 5) base = 2
      data.push([h, d, Math.floor(base + Math.random() * 8)])
    }
  }
  heatmapChartAnalysis.setOption({
    tooltip: {
      position: 'top',
      formatter: p => `${days[p.data[1]]} ${hours[p.data[0]]}<br/>违规数: ${p.data[2]}`
    },
    grid: { left: '10%', right: '5%', top: '10%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category', data: hours, splitArea: { show: true }, axisLabel: { rotate: 45, color: '#85acd5', fontSize: 9 } },
    yAxis: { type: 'category', data: days, splitArea: { show: true }, axisLabel: { color: '#85acd5', fontSize: 10 } },
    visualMap: {
      min: 0, max: 25, calculable: true, orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#2c7bb6', '#abd9e9', '#ffffbf', '#fdae61', '#d73027'] },
      textStyle: { color: '#fff' }
    },
    series: [{
      name: '违规热度', type: 'heatmap', data,
      label: { show: false },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  })
}

const getViolationTypeName = (type) => {
  const map = { 1: '违规停放', 2: '超速行驶', 3: '违规充电', 4: '未登记', 5: '禁行区', 6: '违规载人', 7: '遮挡车牌', 8: '其他' }
  return map[type] || `类型${type}`
}

// ---------- 百度地图初始化 ----------
const CLEAN_BMAP_STYLE = [
  { featureType: 'all', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'poi', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'poi', elementType: 'icons', stylers: { visibility: 'off' } },
  { featureType: 'poilabel', elementType: 'all', stylers: { visibility: 'off' } },
  { featureType: 'building', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'district', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'road', elementType: 'labels', stylers: { visibility: 'off' } }
]

const applyHiddenMapLabels = () => {
  if (!map) return
  showLabels.value = false
  try {
    map.setDisplayOptions?.({
      poiText: false,
      poiIcon: false,
      indoor: false,
      building: true,
      overlay: true
    })
  } catch (e) {
    console.warn('隐藏地图地点标注失败', e)
  }
  try {
    map.setMapStyleV2?.({ styleJson: CLEAN_BMAP_STYLE })
  } catch (e) {
    console.warn('隐藏地图文字标注失败', e)
  }
  try {
    map.setMapStyle?.({ styleJson: CLEAN_BMAP_STYLE })
  } catch (e) {
    console.warn('隐藏地图文字标注失败', e)
  }
}

const keep3DMapView = () => {
  if (!map) return
  is3D.value = true
  try {
    map.setTilt(60)
    map.setHeading(map.getHeading?.() || 0)
  } catch (e) {
    console.warn('保持3D视角失败', e)
  }
}

const loadBaiduMapAPI = () => {
  if (typeof BMapGL !== 'undefined') { initMap(); return }
  const existing = document.querySelector('script[src*="api.map.baidu.com/api?type=webgl"]')
  if (existing) {
    let retries = 0
    const timer = setInterval(() => {
      retries += 1
      if (typeof BMapGL !== 'undefined') {
        clearInterval(timer)
        initMap()
      } else if (retries > 160) {
        clearInterval(timer)
        ElMessage.error('百度地图底图加载超时，请检查网络或 AK 白名单')
      }
    }, 50)
    return
  }
  const script = document.createElement('script')
  script.type = 'text/javascript'
  script.src = 'https://api.map.baidu.com/api?type=webgl&v=1.0&ak=SUBe4C3SHSL9Uyd9P4xgq6GMdaZF2Irj&callback=baiduMapInit'
  window.baiduMapInit = () => {
    if (typeof BMapGL === 'undefined') {
      ElMessage.error('百度地图底图脚本未就绪，请刷新页面')
      return
    }
    initMap()
  }
  script.onerror = () => {
    ElMessage.error('百度地图底图脚本加载失败，请检查网络或 AK 白名单')
  }
  document.head.appendChild(script)
}

const initMap = () => {
  nextTick(() => {
    const mapContainer = document.getElementById('map_box')
    if (!mapContainer) return
    if (typeof BMapGL === 'undefined') return
    if (mapContainerEl && mapContainerEl !== mapContainer) {
      mapContainerEl.removeEventListener('click', handleTrackRouteDomClick, true)
    }
    mapContainerEl = mapContainer
    map = new BMapGL.Map("map_box", { minZoom: 15, maxZoom: 20, enableAutoResize: true })
    const point = new BMapGL.Point(112.6827629, 26.8860959)
    map.centerAndZoom(point, 18)
    try {
      map.setMapType(BMAP_NORMAL_MAP)
    } catch (e) {
      /* ignore */
    }
    map.enableScrollWheelZoom(true)
    map.enableKeyboard()
    map.enableDragging()
    map.enableDoubleClickZoom()
    keep3DMapView()
    applyHiddenMapLabels()
    map.addControl(new BMapGL.ScaleControl())
    map.addControl(new BMapGL.ZoomControl())
    map.addEventListener('click', handleTrackRouteMapClick)
    mapContainerEl.addEventListener('click', handleTrackRouteDomClick, true)
    renderAllDemoPointMarkers({ silent: true })
    drawTrackRouteConfig()
    fetchMapData()
    fetchFenceZones()
    fetchAllLocations()
    startLocationPolling()
    startMarkerRefreshTimer()
    resizeDashboardMap()
    setTimeout(resizeDashboardMap, 200)
  })
}

const resizeDashboardMap = () => {
  if (!map) return
  try {
    map.resize?.()
  } catch (e) {
    /* ignore */
  }
  try {
    map.checkResize?.()
  } catch (e) {
    /* ignore */
  }
}

// ---------- 地图控制方法 ----------
const toggle3DMode = () => {
  keep3DMapView()
}
const toggleSatellite = () => {
  isSatellite.value = !isSatellite.value
  if (map) {
    map.setMapType(isSatellite.value ? BMAP_EARTH_MAP : BMAP_NORMAL_MAP)
    keep3DMapView()
    applyHiddenMapLabels()
  }
}
const toggleLabels = () => {
  if (!map) return
  applyHiddenMapLabels()
  keep3DMapView()
  ElMessage.info('地图地点、建筑文字标注已隐藏')
}
const tiltUp    = () => { if (map) map.setTilt(Math.min(map.getTilt() + 10, 80)) }
const tiltDown  = () => { if (map) map.setTilt(Math.max(map.getTilt() - 10, 45)) }
const rotateLeft  = () => { if (map) map.setHeading(map.getHeading() - 15) }
const rotateRight = () => { if (map) map.setHeading(map.getHeading() + 15) }
const resetView = () => {
  if (map) { keep3DMapView(); map.setHeading(0); map.setZoom(18); map.panTo(new BMapGL.Point(112.6827629, 26.8860959)) }
}

// ---------- 辅助方法 ----------
const getCameraStatusIcon = (status) => {
  if (status === 'offline') return '/img/li02.png'
  if (status === 'error')   return '/img/li03.png'
  return '/img/li01.png'
}
const getVehicleImage = (type) => {
  if (type === 'car')        return '/img/car.png'
  if (type === 'motorcycle') return '/img/motorcycle.png'
  return '/img/ebike.png'
}

const handleAlertClick = (alert) => {
  detailDialog.value = {
    visible: true, title: '违规详情',
    content: `
      <p><strong>案发地点：</strong>${alert.location}</p>
      <p><strong>案发时间：</strong>${alert.time}</p>
      <p><strong>案发备注：</strong>${alert.description}</p>
      <p><strong>违规级别：</strong>${alert.level === 'high' ? '高危' : alert.level === 'medium' ? '中危' : '低危'}</p>
      <p><strong>处理状态：</strong>${alert.status}</p>
    `
  }
}

const locateVehicleOnMap = (vehicle) => {
  if (!map) return
  const plate = vehicle?.licensePlate ? String(vehicle.licensePlate).trim() : ''
  const cached = plate ? locationCache[plate] : null
  const lng = cached?.lng != null ? Number(cached.lng) : 112.6827629
  const lat = cached?.lat != null ? Number(cached.lat) : 26.8860959
  const point = new BMapGL.Point(lng, lat)
  map.centerAndZoom(point, 19)
  keep3DMapView()
  const marker = new BMapGL.Marker(point, {
    icon: new BMapGL.Icon('/img/vehicle_marker.png', new BMapGL.Size(32, 32))
  })
  map.addOverlay(marker)
  map.openInfoWindow(new BMapGL.InfoWindow(`
    <div style="padding:10px;">
      <h4 style="margin:0 0 10px; color:#4b8df8;">车辆定位</h4>
      <p><strong>车牌号：</strong>${vehicle.licensePlate}</p>
      <p><strong>车主：</strong>${vehicle.ownerName || '--'}</p>
      <p><strong>车辆颜色：</strong>${vehicle.color}</p>
      <p><strong>车辆品牌：</strong>${vehicle.brand}</p>
      <p><strong>坐标：</strong>${lng.toFixed(6)}, ${lat.toFixed(6)}</p>
    </div>
  `), point)
  ElMessage.success(`已定位车辆：${vehicle.licensePlate}`)
}

const showVehicleDetail = (vehicle) => {
  detailDialog.value = {
    visible: true, title: '车辆详情',
    content: `
      <p><strong>车牌号：</strong>${vehicle.licensePlate}</p>
      <p><strong>车辆颜色：</strong>${vehicle.color}</p>
      <p><strong>车辆品牌：</strong>${vehicle.brand}</p>
      <p><strong>车主姓名：</strong>${vehicle.ownerName || '--'}</p>
      <p><strong>车辆状态：</strong>${vehicle.status || '--'}</p>
      <p><strong>车辆类型：</strong>${vehicle.vehicleType || '--'}</p>
    `
  }
}

const toggleFullscreen = (cameraId) => {
  const video = document.querySelector(`video[ref="${cameraId}"]`)
  if (!video) return
  if (!document.fullscreenElement) video.requestFullscreen?.()
  else document.exitFullscreen?.()
}

const switchCamera = (camera) => ElMessage.info(`正在切换摄像头: ${camera.name}`)

const setActiveSearchTab = (tab) => {
  activeSearchTab.value = tab
  if (tab === 'analysis') {
    nextTick(() => {
      if (!distChartAnalysis) initDistChartAnalysis()
      else distChartAnalysis.resize()
      if (!heatmapChartAnalysis) initHeatmapChartAnalysis()
      else heatmapChartAnalysis.resize()
    })
  }
}

const openLocationDialog = () => {
  locationDialogVisible.value = true
  page.value = 1
  fetchDevices()
  fetchFenceZones()
  fetchDemoBindingOptions()
  fetchParkingRuntimeConfig()
  nextTick(() => drawTrackRouteConfig())
}

const prevVehiclePage = () => {
  if (vehiclePagination.value.pageNum > 1) { vehiclePagination.value.pageNum--; searchVehicles() }
}
const nextVehiclePage = () => {
  if (vehiclePagination.value.pageNum < vehiclePagination.value.totalPages) { vehiclePagination.value.pageNum++; searchVehicles() }
}

// ---------- 定位模块状态 ----------
const locationDialogVisible = ref(false)
const locationStats = reactive({ online: 0, offline: 0, lowBattery: 0, total: 0 })
const devices = ref([])
const page = ref(1)
const pageSize = 8
const totalPages = ref(1)
const filter = reactive({ keyword: '', status: '' })
const settingsDialog = ref(false)
const selectedZone = ref(null)
const thresholds = reactive({ speed: 15, battery: 20, offline: 30, antitheft: true })
const parkingRuntimeConfig = reactive({ stationaryMinutes: 150, moveThresholdMeters: 35 })
const zones = ref([])
const showExtra = ref(true)
const simActionLoading = ref(false)
const PARKING_GUIDE_CACHE_KEY = 'ev_monitor_parking_guide_config_v1'
const PARKING_GUIDE_NOTIFY_CACHE_KEY = 'ev_monitor_parking_guide_notify_v1'
const PARKING_GUIDE_AUTO_NOTIFY_MS = 10 * 60 * 1000
const TRACK_ROUTE_CACHE_KEY = 'ev_monitor_track_route_config_v2'
const TRACK_REPLAY_STEPS = 36

const normalizeRoutePoints = (points) => Array.isArray(points)
  ? points
    .map((p) => ({ lng: Number(p.lng), lat: Number(p.lat) }))
    .filter((p) => Number.isFinite(p.lng) && Number.isFinite(p.lat))
  : []

function loadTrackRouteStore() {
  try {
    const saved = JSON.parse(localStorage.getItem(TRACK_ROUTE_CACHE_KEY) || '{}')
    const routes = {}
    if (saved?.routes && typeof saved.routes === 'object') {
      Object.entries(saved.routes).forEach(([key, record]) => {
        const points = normalizeRoutePoints(record?.points)
        if (points.length >= 2) routes[key] = { label: record?.label || key, points }
      })
    }
    return { selectedKey: saved?.selectedKey || '', routes }
  } catch {
    return { selectedKey: '', routes: {} }
  }
}

const loadParkingGuideConfig = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(PARKING_GUIDE_CACHE_KEY) || '{}')
    return {
      capacity: Number(saved.capacity) || 8,
      threshold: Number(saved.threshold) || 80,
      extraOccupied: Number(saved.extraOccupied) || 0,
      receiverType: saved.receiverType === 'specified' ? 'specified' : 'all',
      receiverIds: saved.receiverIds || ''
    }
  } catch {
    return { capacity: 8, threshold: 80, extraOccupied: 0, receiverType: 'all', receiverIds: '' }
  }
}

const trackReplayForm = reactive({ targetKey: '', minutes: 30, speed: 2 })
const trackReplayProgress = ref(0)
const trackReplayStatus = ref('未开始')
let trackReplayTimer = null
let trackReplayMarker = null
const trackReplayOverlays = []
let trackReplayPoints = []
let trackReplayIndex = 0
const trackRouteStore = loadTrackRouteStore()
const trackRouteMap = reactive(trackRouteStore.routes)
const trackRouteVehicleKey = ref(trackRouteStore.selectedKey)
const trackRoutePoints = ref(normalizeRoutePoints(trackRouteMap[trackRouteVehicleKey.value]?.points))
const trackRouteEditing = ref(false)
const trackRouteOverlays = []

const parkingGuideConfig = reactive(loadParkingGuideConfig())
const parkingGuideActiveRow = ref(null)
let lastParkingGuideAutoKey = ''
let parkingGuideAutoSending = false

const SIM_CENTER = { lng: 112.6827629, lat: 26.8860959 }
const SIM_OWNER_NAMES = ['张晨', '李远', '王倩', '周航', '陈宇', '刘洋', '黄杰', '赵宁']
const SIM_ROUTE_CORRIDORS = [
  [
    { lng: 112.680956, lat: 26.886856 },
    { lng: 112.681724, lat: 26.886792 },
    { lng: 112.682572, lat: 26.886642 },
    { lng: 112.683456, lat: 26.886486 },
    { lng: 112.684224, lat: 26.886348 }
  ],
  [
    { lng: 112.681208, lat: 26.885418 },
    { lng: 112.681852, lat: 26.885642 },
    { lng: 112.682566, lat: 26.885892 },
    { lng: 112.683312, lat: 26.886148 },
    { lng: 112.684028, lat: 26.886386 }
  ],
  [
    { lng: 112.681458, lat: 26.887238 },
    { lng: 112.681956, lat: 26.886788 },
    { lng: 112.682326, lat: 26.886214 },
    { lng: 112.682684, lat: 26.885604 },
    { lng: 112.683074, lat: 26.885046 }
  ],
  [
    { lng: 112.683838, lat: 26.887154 },
    { lng: 112.683486, lat: 26.886582 },
    { lng: 112.683104, lat: 26.885988 },
    { lng: 112.682694, lat: 26.885392 },
    { lng: 112.682242, lat: 26.884916 }
  ],
  [
    { lng: 112.681036, lat: 26.884986 },
    { lng: 112.681822, lat: 26.884952 },
    { lng: 112.682668, lat: 26.884984 },
    { lng: 112.683524, lat: 26.885094 },
    { lng: 112.684306, lat: 26.885246 }
  ],
  [
    { lng: 112.681614, lat: 26.887512 },
    { lng: 112.682348, lat: 26.887286 },
    { lng: 112.683154, lat: 26.887128 },
    { lng: 112.683928, lat: 26.886918 }
  ]
]

const filteredDevices = computed(() => devices.value)
const routeKeyPart = (value) => normalizePopupKey(value).replace(/^track-/, '')
const getVehicleRouteKey = (row) => {
  if (!row) return ''
  if (row.vehicleId != null && String(row.vehicleId).trim()) return `vehicle-${routeKeyPart(row.vehicleId)}`
  if (row.licensePlate != null && String(row.licensePlate).trim()) return `plate-${routeKeyPart(row.licensePlate)}`
  const device = row.deviceId || row.deviceNumber
  return device != null && String(device).trim() ? `device-${routeKeyPart(device)}` : ''
}
const getVehicleRouteAliasKeys = (row) => {
  const keys = []
  if (!row) return keys
  if (row.vehicleId != null && String(row.vehicleId).trim()) keys.push(`vehicle-${routeKeyPart(row.vehicleId)}`)
  if (row.licensePlate != null && String(row.licensePlate).trim()) keys.push(`plate-${routeKeyPart(row.licensePlate)}`)
  const device = row.deviceId || row.deviceNumber
  if (device != null && String(device).trim()) keys.push(`device-${routeKeyPart(device)}`)
  return [...new Set(keys)]
}
const getVehicleRouteLabel = (row) => {
  const plate = row?.licensePlate || row?.deviceNumber || row?.deviceId || row?.label || '未登记车辆'
  const owner = row?.ownerName || row?.userName
  return owner ? `${plate} / ${owner}` : String(plate)
}
const addTrackVehicleOption = (bucket, row) => {
  const key = getVehicleRouteKey(row)
  if (!key || bucket.has(key)) return
  bucket.set(key, { key, label: getVehicleRouteLabel(row), raw: row })
}
const trackVehicleOptions = computed(() => {
  const bucket = new Map()
  Object.values(locationCache).forEach((row) => addTrackVehicleOption(bucket, row))
  devices.value.forEach((row) => addTrackVehicleOption(bucket, row))
  demoPointForms.forEach((row) => row?.vehicleId && addTrackVehicleOption(bucket, row))
  demoBindingOptions.value.forEach((row) => addTrackVehicleOption(bucket, row))
  Object.entries(trackRouteMap).forEach(([key, record]) => {
    if (!bucket.has(key)) bucket.set(key, { key, label: record?.label || key })
  })
  return Array.from(bucket.values()).sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
})
const trackTargetOptions = computed(() => {
  const rows = Object.entries(locationCache)
    .map(([key, vo]) => ({
      key,
      label: `${vo.licensePlate || vo.deviceId || key} / ${vo.ownerName || vo.userName || '未知用户'}`,
      routeKey: getVehicleRouteKey(vo),
      lng: Number(vo.lng),
      lat: Number(vo.lat)
    }))
    .filter((item) => Number.isFinite(item.lng) && Number.isFinite(item.lat))
  return rows.slice(0, 120)
})
const parkingGuideRows = computed(() => buildParkingGuideRows())
const parkingGuideSummary = computed(() => {
  const rows = parkingGuideRows.value
  if (!rows.length) return '未配置'
  const full = rows.filter((row) => row.full).length
  return full ? `${full} 个区域已满` : '车位充足'
})
const trackRouteSelectedLabel = computed(() => {
  if (!trackRouteVehicleKey.value) return '未选择车辆'
  const option = trackVehicleOptions.value.find((item) => item.key === trackRouteVehicleKey.value)
  return option?.label || trackRouteMap[trackRouteVehicleKey.value]?.label || '当前车辆'
})
const trackRouteSummary = computed(() => {
  const n = trackRoutePoints.value.length
  if (!trackRouteVehicleKey.value) return '请选择车辆'
  if (trackRouteEditing.value) return n >= 2 ? '配置中，路线已生成' : '配置中'
  return n >= 2 ? '已保存路线' : '未配置'
})

const persistTrackRouteStore = () => {
  const routes = {}
  Object.entries(trackRouteMap).forEach(([key, record]) => {
    const points = normalizeRoutePoints(record?.points)
    if (points.length >= 2) routes[key] = { label: record?.label || key, points }
  })
  localStorage.setItem(TRACK_ROUTE_CACHE_KEY, JSON.stringify({
    selectedKey: trackRouteVehicleKey.value,
    routes
  }))
}

const onTrackRouteVehicleChange = () => {
  const record = trackRouteMap[trackRouteVehicleKey.value]
  trackRoutePoints.value = normalizeRoutePoints(record?.points)
  persistTrackRouteStore()
  if (trackRouteEditing.value) drawTrackRouteConfig()
  else clearTrackRouteOverlays()
}

const clearTrackRouteOverlays = () => {
  while (trackRouteOverlays.length) {
    const overlay = trackRouteOverlays.pop()
    try { map?.removeOverlay(overlay) } catch (e) { /* ignore */ }
  }
}

const drawTrackRouteConfig = () => {
  if (!map || typeof BMapGL === 'undefined') return
  clearTrackRouteOverlays()
  if (!trackRouteEditing.value) return
  const points = trackRoutePoints.value
    .filter((p) => Number.isFinite(Number(p.lng)) && Number.isFinite(Number(p.lat)))
    .map((p) => new BMapGL.Point(Number(p.lng), Number(p.lat)))

  if (points.length >= 2) {
    const glow = new BMapGL.Polyline(points, {
      strokeColor: '#07131f',
      strokeWeight: 10,
      strokeOpacity: 0.58
    })
    const line = new BMapGL.Polyline(points, {
      strokeColor: '#ffd166',
      strokeWeight: 5,
      strokeOpacity: 0.96
    })
    map.addOverlay(glow)
    map.addOverlay(line)
    trackRouteOverlays.push(glow, line)
  }
}

const saveTrackRouteConfig = () => {
  if (!trackRouteVehicleKey.value) {
    ElMessage.warning('请先选择车辆')
    return
  }
  if (trackRoutePoints.value.length < 2) {
    ElMessage.warning('至少点击两处位置生成路线')
    return
  }
  const selectedOption = trackVehicleOptions.value.find((item) => item.key === trackRouteVehicleKey.value)
  const record = {
    label: trackRouteSelectedLabel.value,
    points: normalizeRoutePoints(trackRoutePoints.value)
  }
  trackRouteMap[trackRouteVehicleKey.value] = record
  getVehicleRouteAliasKeys(selectedOption?.raw).forEach((key) => {
    if (key) trackRouteMap[key] = record
  })
  persistTrackRouteStore()
  trackRouteEditing.value = false
  clearTrackRouteOverlays()
  try { map?.setDefaultCursor?.('grab') } catch (e) { /* ignore */ }
  ElMessage.success('行驶轨迹已保存')
}

const startTrackRouteEditing = () => {
  if (!trackRouteVehicleKey.value) {
    ElMessage.warning('请先选择车辆')
    return
  }
  if (!map) {
    ElMessage.warning('地图还未加载完成')
    return
  }
  trackRouteEditing.value = true
  locationDialogVisible.value = false
  try {
    map.closeInfoWindow?.()
    map.setDefaultCursor?.('crosshair')
  } catch (e) { /* ignore */ }
  drawTrackRouteConfig()
  ElMessage.info('请直接点击地图添加路线点')
}

const finishTrackRouteEditing = () => {
  trackRouteEditing.value = false
  try { map?.setDefaultCursor?.('grab') } catch (e) { /* ignore */ }
  clearTrackRouteOverlays()
  ElMessage.info('已结束轨迹配置')
}

const toggleTrackRouteEditing = () => {
  if (trackRouteEditing.value) {
    finishTrackRouteEditing()
    return
  }
  startTrackRouteEditing()
}

const undoTrackRoutePoint = () => {
  if (!trackRoutePoints.value.length) {
    ElMessage.info('暂无路线点')
    return
  }
  trackRoutePoints.value.pop()
  drawTrackRouteConfig()
}

const clearTrackRouteConfig = async () => {
  try {
    if (trackRoutePoints.value.length || trackRouteMap[trackRouteVehicleKey.value]) {
      await ElMessageBox.confirm('确认清空已配置的行驶轨迹？', '清空轨迹', {
        type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
      })
    }
  } catch {
    return
  }
  if (trackRouteVehicleKey.value) {
    const selectedOption = trackVehicleOptions.value.find((item) => item.key === trackRouteVehicleKey.value)
    ;[trackRouteVehicleKey.value, ...getVehicleRouteAliasKeys(selectedOption?.raw)].forEach((key) => {
      if (key) delete trackRouteMap[key]
    })
  }
  trackRoutePoints.value = []
  trackRouteEditing.value = false
  try { map?.setDefaultCursor?.('grab') } catch (e) { /* ignore */ }
  persistTrackRouteStore()
  clearTrackRouteOverlays()
  ElMessage.success('行驶轨迹已清空')
}

const resolveMapClickPoint = (e) => {
  if (!e) return null
  if (e.latlng) return e.latlng
  if (e.latLng) return e.latLng
  if (e.point) return e.point
  if (e.target?.point) return e.target.point
  if (e.pixel && map?.pixelToPoint) {
    try { return map.pixelToPoint(e.pixel) } catch (err) { return null }
  }
  return null
}

const addTrackRoutePoint = (point) => {
  if (!trackRouteEditing.value) return
  const lng = Number(point?.lng)
  const lat = Number(point?.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return
  const now = Date.now()
  if (
    now - lastTrackRouteClick.time < 350 &&
    Math.abs(Number(lastTrackRouteClick.lng) - lng) < 0.0000008 &&
    Math.abs(Number(lastTrackRouteClick.lat) - lat) < 0.0000008
  ) {
    return
  }
  lastTrackRouteClick = { time: now, lng, lat }
  trackRoutePoints.value.push({
    lng: Number(lng.toFixed(7)),
    lat: Number(lat.toFixed(7))
  })
  drawTrackRouteConfig()
  ElMessage.success(trackRoutePoints.value.length >= 2 ? '路线已更新' : '已设置路线起点')
}

const handleTrackRouteMapClick = (e) => {
  addTrackRoutePoint(resolveMapClickPoint(e))
}

const handleTrackRouteDomClick = (e) => {
  if (!trackRouteEditing.value || !map || typeof BMapGL === 'undefined') return
  const target = e?.target
  if (target?.closest?.('.track-route-float')) return
  const box = mapContainerEl || document.getElementById('map_box')
  if (!box || !box.contains(target)) return
  const rect = box.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  if (x < 0 || y < 0 || x > rect.width || y > rect.height) return
  const pixel = new BMapGL.Pixel(x, y)
  const point = map.pixelToPoint ? map.pixelToPoint(pixel) : null
  addTrackRoutePoint(point)
}

const interpolateRoutePoint = (route, ratio) => {
  const safeRatio = Math.max(0, Math.min(1, ratio))
  const segmentCount = route.length - 1
  const rawIndex = safeRatio * segmentCount
  const index = Math.min(segmentCount - 1, Math.floor(rawIndex))
  const local = rawIndex - index
  const a = route[index]
  const b = route[index + 1]
  return {
    lng: a.lng + (b.lng - a.lng) * local,
    lat: a.lat + (b.lat - a.lat) * local
  }
}

const offsetAlongCorridor = (point, route, ratio, index, tick) => {
  const segmentCount = route.length - 1
  const routeIndex = Math.min(segmentCount - 1, Math.floor(Math.max(0, Math.min(1, ratio)) * segmentCount))
  const a = route[routeIndex]
  const b = route[routeIndex + 1]
  const latRadians = point.lat * Math.PI / 180
  const lngScale = 111320 * Math.cos(latRadians)
  const latScale = 111320
  const dx = (b.lng - a.lng) * lngScale
  const dy = (b.lat - a.lat) * latScale
  const len = Math.max(1, Math.sqrt(dx * dx + dy * dy))
  const normalX = -dy / len
  const normalY = dx / len
  const lateralMeters = Math.sin(index * 1.73 + tick * 0.36) * 6
  const forwardMeters = Math.cos(index * 0.91 + tick * 0.28) * 10
  return {
    lng: Number((point.lng + (normalX * lateralMeters + dx / len * forwardMeters) / lngScale).toFixed(6)),
    lat: Number((point.lat + (normalY * lateralMeters + dy / len * forwardMeters) / latScale).toFixed(6))
  }
}

const advanceDemoPointMotion = (indexes = []) => {
  demoPointMotionTick += 1
  indexes.forEach((index) => {
    const pointForm = demoPointForms[index]
    if (!pointForm || pointForm.visible === false) return

    let origin = demoPointMotionOrigins.get(pointForm.id)
    if (!origin) {
      setDemoPointMotionOrigin(pointForm)
      origin = demoPointMotionOrigins.get(pointForm.id)
    }
    if (!origin) return

    const latRadians = origin.lat * Math.PI / 180
    const lngScale = Math.max(1, 111320 * Math.cos(latRadians))
    const latScale = 111320
    const angle = demoPointMotionTick * 0.42 + index * 0.83
    const radiusX = 18 + (index % 4) * 4
    const radiusY = 12 + (index % 3) * 4
    const wobble = Math.sin(demoPointMotionTick * 0.31 + index) * 5
    const nextLng = origin.lng + (Math.cos(angle) * radiusX + wobble) / lngScale
    const nextLat = origin.lat + (Math.sin(angle * 0.92) * radiusY + wobble * 0.35) / latScale

    pointForm.lng = nextLng.toFixed(7)
    pointForm.lat = nextLat.toFixed(7)
    renderDemoPointMarker(pointForm, { silent: true })
  })
}

const buildSimPoints = (count, tick = 0) => {
  return Array.from({ length: count }, (_, index) => {
    const route = SIM_ROUTE_CORRIDORS[index % SIM_ROUTE_CORRIDORS.length]
    const laneIndex = Math.floor(index / SIM_ROUTE_CORRIDORS.length)
    const pointsPerRoute = Math.ceil(count / SIM_ROUTE_CORRIDORS.length)
    const ratio = ((laneIndex + (tick * 0.018) + (index % 3) * 0.09) % pointsPerRoute) / Math.max(1, pointsPerRoute - 1)
    const corridorPoint = offsetAlongCorridor(interpolateRoutePoint(route, ratio), route, ratio, index, tick)
    return {
      id: `screen-demo-${index + 1}`,
      licensePlate: `湘A${String(8300 + index).padStart(4, '0')}`,
      ownerName: SIM_OWNER_NAMES[index % SIM_OWNER_NAMES.length],
      lat: corridorPoint.lat,
      lng: corridorPoint.lng,
      speed: Number((4 + Math.random() * 16).toFixed(1)),
      battery: Math.max(20, Math.min(96, Math.round(45 + Math.random() * 50)))
    }
  })
}

const pushSimLocations = async (count) => {
  simMotionTick += 1
  const res = await axios.post(`${API_BASE_URL}/location/sim/batch`, {
    ttlMinutes: 180,
    points: buildSimPoints(count, simMotionTick)
  })
  if (res.data.code !== 200) {
    throw new Error(res.data.message || '监测点上报失败')
  }
  await fetchAllLocations()
}

const stopSimLocationStream = () => {
  if (simLocationTimer) {
    clearInterval(simLocationTimer)
    simLocationTimer = null
  }
}

const startSimLocationStream = async (count = 60) => {
  simActionLoading.value = true
  stopSimLocationStream()
  try {
    removeSimEntriesFromLocationCache()
    await axios.delete(`${API_BASE_URL}/location/sim`)
    await pushSimLocations(count)
    simLocationTimer = setInterval(() => {
      pushSimLocations(count).catch((e) => console.error('持续上报点位失败:', e))
    }, SIM_PUSH_MS)
    ElMessage.success(`已开启 ${count} 个监测点持续上报`)
  } catch (e) {
    console.error('开启持续点位失败:', e)
    ElMessage.error('开启持续点位失败')
  } finally {
    simActionLoading.value = false
  }
}

const removeSimEntriesFromLocationCache = () => {
  let changed = false
  for (const [key, vo] of Object.entries(locationCache)) {
    const deviceId = String(vo?.deviceId || '')
    if (!deviceId.startsWith('sim:')) continue
    changed = true
    delete locationCache[key]
    const marker = vehicleMarkers.get(key)
    if (marker) {
      try { map?.removeOverlay(marker) } catch (e) { /* ignore */ }
      vehicleMarkers.delete(key)
    }
  }
  if (changed) saveLocationCache(locationCache)
}

const getBoundDemoVehicleIds = () => {
  return [...new Set(demoPointForms
    .map((point) => Number(point.vehicleId))
    .filter((id) => Number.isFinite(id) && id > 0))]
}

const clearBoundDemoPointLocations = async () => {
  const vehicleIds = getBoundDemoVehicleIds()
  if (!vehicleIds.length) return
  await Promise.all(vehicleIds.map((vehicleId) =>
    axios.delete(`${API_BASE_URL}/location/vehicle/${vehicleId}`).catch((e) => {
      console.warn(`清除车辆 ${vehicleId} 点位失败`, e)
      return null
    })
  ))
}

const removeBoundDemoEntriesFromLocationCache = () => {
  const vehicleIds = new Set(demoPointForms.map((point) => String(point.vehicleId || '')).filter(Boolean))
  const plates = new Set(demoPointForms.map((point) => String(point.licensePlate || '').trim()).filter(Boolean))
  let changed = false
  for (const [key, vo] of Object.entries(locationCache)) {
    const vehicleId = String(vo?.vehicleId || '')
    const plate = String(vo?.licensePlate || '').trim()
    if (!vehicleIds.has(vehicleId) && !plates.has(plate)) continue
    changed = true
    delete locationCache[key]
    const marker = vehicleMarkers.get(key)
    if (marker) {
      try { map?.removeOverlay(marker) } catch (e) { /* ignore */ }
      vehicleMarkers.delete(key)
    }
  }
  if (changed) saveLocationCache(locationCache)
}

const refreshLocationModule = async () => {
  await Promise.all([fetchAllLocations(), fetchDevices()])
}

const fetchParkingRuntimeConfig = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/location/parking-config`)
    if (res.data.code !== 200 || !res.data.data) return
    parkingRuntimeConfig.stationaryMinutes = Number(res.data.data.stationaryMinutes || 150)
    parkingRuntimeConfig.moveThresholdMeters = Number(res.data.data.moveThresholdMeters || 35)
  } catch (e) {
    console.warn('读取违停判定配置失败:', e)
  }
}

const saveParkingRuntimeConfig = async () => {
  try {
    const res = await axios.put(`${API_BASE_URL}/location/parking-config`, {
      stationaryMinutes: Number(parkingRuntimeConfig.stationaryMinutes || 150),
      moveThresholdMeters: Number(parkingRuntimeConfig.moveThresholdMeters || 35)
    })
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '保存违停判定配置失败')
      return
    }
    parkingRuntimeConfig.stationaryMinutes = Number(res.data.data.stationaryMinutes || parkingRuntimeConfig.stationaryMinutes)
    parkingRuntimeConfig.moveThresholdMeters = Number(res.data.data.moveThresholdMeters || parkingRuntimeConfig.moveThresholdMeters)
    ElMessage.success('违停判定配置已保存')
  } catch (e) {
    console.error('保存违停判定配置失败:', e)
    ElMessage.error('保存违停判定配置失败')
  }
}

const clearSimLocations = async () => {
  simActionLoading.value = true
  try {
    stopSimLocationStream()
    stopDemoPointStream({ silent: true })
    await axios.delete(`${API_BASE_URL}/location/sim`)
    await clearBoundDemoPointLocations()
    removeSimEntriesFromLocationCache()
    removeBoundDemoEntriesFromLocationCache()
    hideAllDemoPoints({ silent: true })
    if (locationDialogVisible.value) await fetchDevices()
    ElMessage.success('点位已停止并清除')
  } catch (e) {
    console.error('清理点位失败:', e)
    ElMessage.error('清理点位失败')
  } finally {
    simActionLoading.value = false
  }
}

const getCurrentUserId = () => {
  const raw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex')
  if (!raw) return undefined
  try {
    const parsed = JSON.parse(raw)
    const uid = Number(parsed?.user?.info?.userId)
    return Number.isFinite(uid) && uid > 0 ? uid : undefined
  } catch {
    return undefined
  }
}

const mapFenceTypeToZoneType = (fenceType) => Number(fenceType) === 5 ? 'parking' : 'forbidden'

const mapFenceTypeToText = (fenceType) => {
  const map = { 1: '禁止进入', 2: '禁止驶出', 3: '限速区域', 4: '监控区域', 5: '允许停车区' }
  return map[Number(fenceType)] || `类型${fenceType || ''}`
}

const fetchFenceZones = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/geofence/list`)
    if (res.data.code !== 200) return
    const rows = Array.isArray(res.data.data) ? res.data.data : []
    zones.value = rows.map((row) => ({
      id: row.fenceId, fenceId: row.fenceId,
      name: row.fenceName || `围栏${row.fenceId}`,
      desc: row.description || mapFenceTypeToText(row.fenceType),
      type: mapFenceTypeToZoneType(row.fenceType),
      active: Number(row.status) === 1,
      fenceType: row.fenceType,
      fenceColor: row.fenceColor || '#00d4ff',
      coordinates: row.coordinates
    }))
    drawGeofencePolygons()
    handleParkingGuideState()
  } catch (e) {
    console.error('获取围栏数据失败:', e)
  }
}

const normalizeDeviceStatus = (raw, batteryLevel, updateTime) => {
  const normalized = String(raw || '').toLowerCase()
  const reportMs = updateTime ? new Date(updateTime).getTime() : 0
  const offlineByTime = !reportMs || Date.now() - reportMs > Number(thresholds.offline || 30) * 60 * 1000
  if (normalized === 'stale' || normalized === 'offline' || offlineByTime) return 'offline'
  if (Number(batteryLevel) <= Number(thresholds.battery || 20)) return 'low_battery'
  return 'online'
}

const fetchDevices = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/location/all`)
    if (res.data.code !== 200) return
    const locationRows = Array.isArray(res.data.data) ? res.data.data : []
    const normalized = locationRows.map((v, i) => {
      const batteryLevel = Number.isFinite(Number(v.battery)) ? Number(v.battery) : 0
      const status = normalizeDeviceStatus(v.status, batteryLevel, v.updateTime)
      const deviceNumber = v.deviceId || (v.vehicleId != null ? `GPS-${String(v.vehicleId).padStart(4, '0')}` : `DEVICE-${i + 1}`)
      return {
        deviceId: v.deviceId || v.vehicleId || i,
        deviceNumber, vehicleId: v.vehicleId,
        licensePlate: v.licensePlate, ownerName: v.ownerName,
        currentLng: Number(v.lng), currentLat: Number(v.lat),
        status, batteryLevel, lastReportTime: v.updateTime, unbound: !!v.unbound
      }
    })
    locationStats.total = normalized.length
    locationStats.online = normalized.filter((d) => d.status === 'online').length
    locationStats.offline = normalized.filter((d) => d.status === 'offline').length
    locationStats.lowBattery = normalized.filter((d) => d.status === 'low_battery').length
    let list = normalized
    const keyword = String(filter.keyword || '').trim().toLowerCase()
    if (keyword) {
      list = list.filter((d) =>
        [d.deviceNumber, d.licensePlate, d.ownerName].filter(Boolean).some((x) => String(x).toLowerCase().includes(keyword))
      )
    }
    if (filter.status) list = list.filter((d) => d.status === filter.status)
    totalPages.value = Math.max(1, Math.ceil(list.length / pageSize))
    if (page.value > totalPages.value) page.value = totalPages.value
    if (page.value < 1) page.value = 1
    const start = (page.value - 1) * pageSize
    devices.value = list.slice(start, start + pageSize)
  } catch (e) {
    console.error('获取设备列表失败:', e)
    ElMessage.error('获取设备列表失败')
  }
}

const changePage = (dir) => {
  const np = page.value + dir
  if (np >= 1 && np <= totalPages.value) { page.value = np; fetchDevices() }
}

const getSignalLevel = (dev) => dev.status === 'online' ? 4 : dev.status === 'low_battery' ? 3 : 1
const getSignalText = (dev) => ({ online: '强', low_battery: '中', offline: '弱' }[dev.status] || '--')
const getBatteryColor = (pct) => pct <= 20 ? '#f44336' : pct <= 50 ? '#ff9800' : '#4caf50'
const getDevStatusLabel = (s) => ({ online: '在线', offline: '离线', low_battery: '低电量' }[s] || s)
const formatTime = (t) => {
  if (!t) return '--'
  const diff = Date.now() - new Date(t).getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  return new Date(t).toLocaleString('zh-CN').replace(/\//g, '-')
}

const getTrackSourceFromDevice = (dev) => {
  if (!dev) return null
  if (Number.isFinite(Number(dev.lng)) && Number.isFinite(Number(dev.lat))) {
    return {
      key: String(dev.key || dev.licensePlate || dev.deviceNumber || dev.deviceId || Date.now()),
      label: dev.label || dev.licensePlate || dev.deviceNumber || dev.deviceId || '当前点位',
      routeKey: dev.routeKey || getVehicleRouteKey(dev),
      lng: Number(dev.lng),
      lat: Number(dev.lat)
    }
  }
  const lng = Number(dev.currentLng)
  const lat = Number(dev.currentLat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  return {
    key: String(dev.licensePlate || dev.deviceNumber || dev.deviceId || Date.now()),
    label: dev.licensePlate || dev.deviceNumber || '当前车辆',
    routeKey: dev.routeKey || getVehicleRouteKey(dev),
    lng,
    lat
  }
}

const getTrackSourceFromPopupKey = (key) => {
  const source = trackReplaySources.get(String(key || ''))
  if (!source) return null
  const lng = Number(source.lng)
  const lat = Number(source.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  return { ...source, lng, lat }
}

const getTrackSourceFromForm = () => {
  const selected = trackTargetOptions.value.find((item) => item.key === trackReplayForm.targetKey)
  if (selected) return selected
  const first = trackTargetOptions.value[0]
  if (first) return first
  return { key: 'campus-center', label: '校园中心点', lng: SIM_CENTER.lng, lat: SIM_CENTER.lat }
}

const clampTrackRatio = (value) => Math.max(0, Math.min(1, value))

const hashTrackKey = (value) => {
  const s = String(value || '')
  let n = 0
  for (let i = 0; i < s.length; i++) n = (n * 31 + s.charCodeAt(i)) >>> 0
  return n
}

const projectPointToRoadSegment = (source, a, b) => {
  const latRadians = source.lat * Math.PI / 180
  const lngScale = Math.max(1, 111320 * Math.cos(latRadians))
  const latScale = 111320
  const ax = (a.lng - source.lng) * lngScale
  const ay = (a.lat - source.lat) * latScale
  const bx = (b.lng - source.lng) * lngScale
  const by = (b.lat - source.lat) * latScale
  const vx = bx - ax
  const vy = by - ay
  const lenSq = Math.max(1, vx * vx + vy * vy)
  const t = Math.max(0, Math.min(1, -(ax * vx + ay * vy) / lenSq))
  const x = ax + vx * t
  const y = ay + vy * t
  return {
    t,
    distanceSq: x * x + y * y,
    point: {
      lng: source.lng + x / lngScale,
      lat: source.lat + y / latScale
    }
  }
}

const getConfiguredTrackRoute = (source) => {
  const candidates = [
    source?.routeKey,
    getVehicleRouteKey(source),
    source?.key
  ].filter(Boolean)
  for (const key of candidates) {
    const points = normalizeRoutePoints(trackRouteMap[key]?.points)
    if (points.length >= 2) return points
  }
  return []
}

const getRouteDistanceMeters = (a, b) => {
  const midLat = ((Number(a.lat) || 0) + (Number(b.lat) || 0)) / 2
  const lngScale = Math.max(1, 111320 * Math.cos(midLat * Math.PI / 180))
  const latScale = 111320
  const dx = (Number(b.lng) - Number(a.lng)) * lngScale
  const dy = (Number(b.lat) - Number(a.lat)) * latScale
  return Math.sqrt(dx * dx + dy * dy)
}

const buildConfiguredTrackReplayPath = (route) => {
  const points = normalizeRoutePoints(route)
  if (points.length < 2) return []
  const distances = []
  let total = 0
  for (let i = 0; i < points.length - 1; i++) {
    const d = getRouteDistanceMeters(points[i], points[i + 1])
    distances.push(d)
    total += d
  }
  const targetSteps = Math.max(TRACK_REPLAY_STEPS, points.length * 6)
  const replay = [{ ...points[0] }]
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i]
    const b = points[i + 1]
    const steps = Math.max(1, Math.round((distances[i] / Math.max(1, total)) * targetSteps))
    for (let j = 1; j <= steps; j++) {
      const t = j / steps
      replay.push({
        lng: Number((a.lng + (b.lng - a.lng) * t).toFixed(7)),
        lat: Number((a.lat + (b.lat - a.lat) * t).toFixed(7))
      })
    }
  }
  return replay
}

const getTrackReplayCorridors = (source) => {
  const routeKey = source?.routeKey || getVehicleRouteKey(source)
  const configured = normalizeRoutePoints(trackRouteMap[routeKey]?.points)
  return configured.length >= 2 ? [configured] : SIM_ROUTE_CORRIDORS
}

const findNearestRoadCorridor = (source) => {
  let best = null
  getTrackReplayCorridors(source).forEach((route, routeIndex) => {
    for (let i = 0; i < route.length - 1; i++) {
      const projected = projectPointToRoadSegment(source, route[i], route[i + 1])
      if (!best || projected.distanceSq < best.distanceSq) {
        best = {
          route,
          routeIndex,
          segmentIndex: i,
          ratio: (i + projected.t) / Math.max(1, route.length - 1),
          point: projected.point,
          distanceSq: projected.distanceSq
        }
      }
    }
  })
  return best || { route: SIM_ROUTE_CORRIDORS[0], routeIndex: 0, ratio: 0.5, point: SIM_CENTER, distanceSq: 0 }
}

const offsetTrackRoadPoint = (point, route, ratio, seed, step) => {
  const segmentCount = route.length - 1
  const routeIndex = Math.min(segmentCount - 1, Math.floor(clampTrackRatio(ratio) * segmentCount))
  const a = route[routeIndex]
  const b = route[routeIndex + 1]
  const latRadians = point.lat * Math.PI / 180
  const lngScale = Math.max(1, 111320 * Math.cos(latRadians))
  const latScale = 111320
  const dx = (b.lng - a.lng) * lngScale
  const dy = (b.lat - a.lat) * latScale
  const len = Math.max(1, Math.sqrt(dx * dx + dy * dy))
  const normalX = -dy / len
  const normalY = dx / len
  const lateralMeters = Math.sin(seed * 0.17 + step * 0.42) * 1.8
  return {
    lng: Number((point.lng + (normalX * lateralMeters) / lngScale).toFixed(7)),
    lat: Number((point.lat + (normalY * lateralMeters) / latScale).toFixed(7))
  }
}

const buildTrackReplayPath = (source) => {
  const configuredRoute = getConfiguredTrackRoute(source)
  if (configuredRoute.length >= 2) {
    return buildConfiguredTrackReplayPath(configuredRoute)
  }
  const minutes = Math.max(5, Math.min(180, Number(trackReplayForm.minutes) || 30))
  const road = findNearestRoadCorridor(source)
  const seed = hashTrackKey(source.key || source.label)
  const direction = seed % 2 === 0 ? 1 : -1
  const span = Math.min(0.95, Math.max(0.22, minutes / 150))
  let startRatio = direction > 0
    ? clampTrackRatio(road.ratio - span * 0.28)
    : clampTrackRatio(road.ratio + span * 0.28)
  let endRatio = direction > 0
    ? clampTrackRatio(startRatio + span)
    : clampTrackRatio(startRatio - span)
  if (direction > 0 && endRatio - startRatio < span * 0.55) {
    startRatio = clampTrackRatio(endRatio - span * 0.55)
  }
  if (direction < 0 && startRatio - endRatio < span * 0.55) {
    startRatio = clampTrackRatio(endRatio + span * 0.55)
  }
  const points = []
  for (let i = 0; i < TRACK_REPLAY_STEPS; i++) {
    const t = i / (TRACK_REPLAY_STEPS - 1)
    const ratio = startRatio + (endRatio - startRatio) * t
    const roadPoint = interpolateRoutePoint(road.route, ratio)
    points.push(offsetTrackRoadPoint(roadPoint, road.route, ratio, seed, i))
  }
  return points
}

const drawTrackReplayPath = (points) => {
  if (!map || typeof BMapGL === 'undefined' || points.length < 2) return
  const bPoints = points.map((p) => new BMapGL.Point(p.lng, p.lat))
  const line = new BMapGL.Polyline(bPoints, {
    strokeColor: '#00ff7f',
    strokeWeight: 5,
    strokeOpacity: 0.85
  })
  map.addOverlay(line)
  trackReplayOverlays.push(line)
  const marker = new BMapGL.Marker(bPoints[0], {
    icon: new BMapGL.Icon(createMarkerIcon('#27d4ff'), new BMapGL.Size(32, 40), {
      anchor: new BMapGL.Size(16, 40)
    })
  })
  map.addOverlay(marker)
  trackReplayMarker = marker
  map.panTo(bPoints[0])
  map.setZoom(Math.max(map.getZoom?.() || 18, 18))
  keep3DMapView()
}

const stepTrackReplay = () => {
  if (!trackReplayMarker || !trackReplayPoints.length || typeof BMapGL === 'undefined') return
  const point = trackReplayPoints[trackReplayIndex]
  trackReplayMarker.setPosition(new BMapGL.Point(point.lng, point.lat))
  trackReplayProgress.value = Math.round((trackReplayIndex / Math.max(1, trackReplayPoints.length - 1)) * 100)
  trackReplayIndex += 1
  if (trackReplayIndex >= trackReplayPoints.length) {
    clearInterval(trackReplayTimer)
    trackReplayTimer = null
    trackReplayStatus.value = '已完成'
    trackReplayProgress.value = 100
  }
}

const startTrackReplay = (dev, options = {}) => {
  if (!map || typeof BMapGL === 'undefined') {
    ElMessage.warning('地图未初始化')
    return
  }
  if (options.minutes != null) trackReplayForm.minutes = Math.max(5, Math.min(180, Number(options.minutes) || 30))
  if (options.speed != null) trackReplayForm.speed = Math.max(1, Math.min(8, Number(options.speed) || 2))
  const source = getTrackSourceFromDevice(dev) || getTrackSourceFromForm()
  clearTrackReplay()
  trackReplayPoints = buildTrackReplayPath(source)
  trackReplayIndex = 0
  drawTrackReplayPath(trackReplayPoints)
  const speed = Math.max(1, Math.min(8, Number(trackReplayForm.speed) || 2))
  trackReplayStatus.value = `${source.label} 回放中`
  stepTrackReplay()
  trackReplayTimer = setInterval(stepTrackReplay, Math.max(160, 900 / speed))
}

const playDeviceTrack = (dev) => {
  const key = String(dev.licensePlate || dev.deviceNumber || dev.deviceId || '')
  if (key) trackReplayForm.targetKey = key
  startTrackReplay(dev)
}

const pauseTrackReplay = () => {
  if (trackReplayTimer) {
    clearInterval(trackReplayTimer)
    trackReplayTimer = null
    trackReplayStatus.value = '已暂停'
  }
}

const clearTrackReplay = () => {
  if (trackReplayTimer) {
    clearInterval(trackReplayTimer)
    trackReplayTimer = null
  }
  while (trackReplayOverlays.length) {
    const overlay = trackReplayOverlays.pop()
    try { map?.removeOverlay(overlay) } catch (e) { /* ignore */ }
  }
  if (trackReplayMarker) {
    try { map?.removeOverlay(trackReplayMarker) } catch (e) { /* ignore */ }
    trackReplayMarker = null
  }
  trackReplayPoints = []
  trackReplayIndex = 0
  trackReplayProgress.value = 0
  trackReplayStatus.value = '未开始'
}

const readPopupTrackNumber = (id, fallback, min, max) => {
  const raw = typeof document !== 'undefined' ? document.getElementById(id)?.value : ''
  const next = Number(raw)
  const safe = Number.isFinite(next) ? next : fallback
  return Math.max(min, Math.min(max, safe))
}

const handleTrackReplayAction = (action, key) => {
  if (action === 'pause') {
    pauseTrackReplay()
    return
  }
  if (action === 'clear') {
    clearTrackReplay()
    return
  }
  const source = getTrackSourceFromPopupKey(key)
  if (!source) {
    ElMessage.warning('未找到该监测点')
    return
  }
  const minutes = readPopupTrackNumber(`track-min-${key}`, trackReplayForm.minutes, 5, 180)
  const speed = readPopupTrackNumber(`track-speed-${key}`, trackReplayForm.speed, 1, 8)
  startTrackReplay(source, { minutes, speed })
}

const DEFAULT_PARKING_AREAS = Object.freeze([
  {
    id: 'parking-south',
    name: '南门停车场',
    coordinates: JSON.stringify([
      { lng: 112.68150, lat: 26.88495 },
      { lng: 112.68335, lat: 26.88495 },
      { lng: 112.68335, lat: 26.88588 },
      { lng: 112.68150, lat: 26.88588 }
    ])
  },
  {
    id: 'parking-east',
    name: '东侧停车场',
    coordinates: JSON.stringify([
      { lng: 112.68315, lat: 26.88618 },
      { lng: 112.68470, lat: 26.88618 },
      { lng: 112.68470, lat: 26.88722 },
      { lng: 112.68315, lat: 26.88722 }
    ])
  },
  {
    id: 'parking-north',
    name: '北区停车场',
    coordinates: JSON.stringify([
      { lng: 112.68120, lat: 26.88702 },
      { lng: 112.68295, lat: 26.88702 },
      { lng: 112.68295, lat: 26.88786 },
      { lng: 112.68120, lat: 26.88786 }
    ])
  }
])

const pointInPolygon = (lng, lat, points) => {
  if (!Array.isArray(points) || points.length < 3) return false
  let inside = false
  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    const xi = Number(points[i].lng)
    const yi = Number(points[i].lat)
    const xj = Number(points[j].lng)
    const yj = Number(points[j].lat)
    const intersect = ((yi > lat) !== (yj > lat)) && (lng < ((xj - xi) * (lat - yi)) / (yj - yi || 1) + xi)
    if (intersect) inside = !inside
  }
  return inside
}

const getParkingAreas = () => {
  const enabled = zones.value
    .filter((zone) => zone.active && (zone.type === 'parking' || Number(zone.fenceType) === 5))
    .filter((zone) => parseFenceCoordinates(zone.coordinates).length >= 3)
  return enabled.length ? enabled : DEFAULT_PARKING_AREAS
}

const getParkingAreaCenter = (area) => {
  const points = parseFenceCoordinates(area?.coordinates)
  if (!points.length) return null
  const sum = points.reduce((acc, p) => {
    acc.lng += Number(p.lng) || 0
    acc.lat += Number(p.lat) || 0
    return acc
  }, { lng: 0, lat: 0 })
  return {
    lng: Number((sum.lng / points.length).toFixed(7)),
    lat: Number((sum.lat / points.length).toFixed(7))
  }
}

const countParkingOccupancy = (area, index) => {
  const points = parseFenceCoordinates(area.coordinates)
  let count = 0
  Object.values(locationCache).forEach((vo) => {
    const lng = Number(vo?.lng)
    const lat = Number(vo?.lat)
    if (Number.isFinite(lng) && Number.isFinite(lat) && pointInPolygon(lng, lat, points)) {
      count += 1
    }
  })
  if (index === 0) count += Math.max(0, Number(parkingGuideConfig.extraOccupied) || 0)
  return count
}

const buildParkingGuideRows = () => {
  const capacity = Math.max(1, Number(parkingGuideConfig.capacity) || 8)
  const threshold = Math.max(1, Math.min(100, Number(parkingGuideConfig.threshold) || 80))
  return getParkingAreas().map((area, index) => {
    const occupied = Math.min(capacity, countParkingOccupancy(area, index))
    const percent = Math.round((occupied / capacity) * 100)
    return {
      id: area.id || area.fenceId || index,
      name: area.name || area.fenceName || `停车区域${index + 1}`,
      center: getParkingAreaCenter(area),
      occupied,
      capacity,
      percent,
      full: percent >= threshold
    }
  })
}

const saveParkingGuideConfig = () => {
  localStorage.setItem(PARKING_GUIDE_CACHE_KEY, JSON.stringify({ ...parkingGuideConfig }))
  handleParkingGuideState()
  ElMessage.success('停车引导配置已保存')
}

const getRecommendedParking = (fullRow) => {
  const rows = parkingGuideRows.value.filter((row) => row.id !== fullRow?.id)
  return rows.filter((row) => !row.full).sort((a, b) => a.percent - b.percent)[0] || rows[0] || null
}

const buildParkingGuideMessage = (fullRow) => {
  const target = getRecommendedParking(fullRow)
  const targetName = target?.name || '就近空闲停车场'
  return `附近区域车位已满，推荐前往 ${targetName}`
}

const buildParkingGuidePopupMessage = (fullRow) => {
  return `${fullRow.name} 车位已满（${fullRow.occupied}/${fullRow.capacity}），${buildParkingGuideMessage(fullRow)}`
}

const createParkingFullIcon = () => {
  const canvas = document.createElement('canvas')
  canvas.width = 42
  canvas.height = 50
  const ctx = canvas.getContext('2d')
  ctx.beginPath()
  ctx.arc(21, 18, 15, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(255, 77, 79, 0.28)'
  ctx.fill()
  ctx.beginPath()
  ctx.arc(21, 18, 11, 0, Math.PI * 2)
  ctx.fillStyle = '#ff4d4f'
  ctx.fill()
  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 2
  ctx.stroke()
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 14px Arial'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('满', 21, 18)
  ctx.beginPath()
  ctx.moveTo(15, 30)
  ctx.lineTo(21, 48)
  ctx.lineTo(27, 30)
  ctx.closePath()
  ctx.fillStyle = '#ff4d4f'
  ctx.fill()
  return canvas.toDataURL()
}

const clearParkingGuideOverlays = () => {
  while (parkingGuideOverlays.length) {
    const overlay = parkingGuideOverlays.pop()
    try { map?.removeOverlay(overlay) } catch (e) { /* ignore */ }
  }
}

const drawParkingGuideMarkers = () => {
  if (!map || typeof BMapGL === 'undefined') return
  clearParkingGuideOverlays()
  parkingGuideRows.value.filter((row) => row.full && row.center).forEach((row) => {
    const point = new BMapGL.Point(row.center.lng, row.center.lat)
    const marker = new BMapGL.Marker(point, {
      icon: new BMapGL.Icon(createParkingFullIcon(), new BMapGL.Size(42, 50), {
        anchor: new BMapGL.Size(21, 48)
      })
    })
    const label = new BMapGL.Label(`${row.name} 已满`, {
      position: point,
      offset: new BMapGL.Size(16, -36)
    })
    label.setStyle({
      padding: '5px 9px',
      color: '#fff4d7',
      background: 'rgba(43,18,8,.88)',
      border: '1px solid rgba(255,179,71,.72)',
      borderRadius: '6px',
      fontSize: '12px',
      fontWeight: '700',
      boxShadow: '0 0 12px rgba(255,77,79,.24)'
    })
    map.addOverlay(marker)
    map.addOverlay(label)
    parkingGuideOverlays.push(marker, label)
  })
}

const readParkingGuideNotifyCache = () => {
  try {
    const data = JSON.parse(localStorage.getItem(PARKING_GUIDE_NOTIFY_CACHE_KEY) || '{}')
    return data && typeof data === 'object' ? data : {}
  } catch {
    return {}
  }
}

const writeParkingGuideNotifyCache = (cache) => {
  localStorage.setItem(PARKING_GUIDE_NOTIFY_CACHE_KEY, JSON.stringify(cache))
}

const buildParkingGuideNotifyKey = (row) => {
  const receivers = parkingGuideConfig.receiverType === 'specified'
    ? String(parkingGuideConfig.receiverIds || '').replace(/\s+/g, '')
    : 'all'
  const target = getRecommendedParking(row)
  return `${row.id}|${row.capacity}|${parkingGuideConfig.threshold}|${target?.id || target?.name || 'near'}|${receivers}`
}

const autoSendParkingGuidance = async (row) => {
  if (!row || parkingGuideAutoSending) return
  const key = buildParkingGuideNotifyKey(row)
  const now = Date.now()
  const cache = readParkingGuideNotifyCache()
  if (key === lastParkingGuideAutoKey && now - Number(cache[key] || 0) < PARKING_GUIDE_AUTO_NOTIFY_MS) return
  if (now - Number(cache[key] || 0) < PARKING_GUIDE_AUTO_NOTIFY_MS) return
  parkingGuideAutoSending = true
  try {
    const ok = await sendParkingGuidance(row, { silent: true })
    if (ok) {
      cache[key] = now
      writeParkingGuideNotifyCache(cache)
      lastParkingGuideAutoKey = key
    }
  } finally {
    parkingGuideAutoSending = false
  }
}

const handleParkingGuideState = () => {
  drawParkingGuideMarkers()
  const fullRow = parkingGuideRows.value.find((row) => row.full)
  parkingGuideActiveRow.value = fullRow || null
  if (fullRow) autoSendParkingGuidance(fullRow).catch((e) => console.error('自动发送停车引导失败:', e))
}

const checkParkingGuide = async ({ notify = false } = {}) => {
  const fullRow = parkingGuideRows.value.find((row) => row.full)
  parkingGuideActiveRow.value = fullRow || null
  drawParkingGuideMarkers()
  if (!fullRow) {
    ElMessage.success('当前停车区域未达到满载阈值')
    return null
  }
  const msg = buildParkingGuidePopupMessage(fullRow)
  ElMessage.warning(msg)
  if (notify) await sendParkingGuidance(fullRow)
  return fullRow
}

const sendParkingGuidance = async (row, { silent = false } = {}) => {
  const fullRow = row || parkingGuideActiveRow.value || parkingGuideRows.value.find((item) => item.full)
  if (!fullRow) {
    if (!silent) ElMessage.warning('没有达到满载阈值的停车区域')
    return false
  }
  const receiverIds = String(parkingGuideConfig.receiverIds || '')
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean)
  if (parkingGuideConfig.receiverType === 'specified' && !receiverIds.length) {
    if (!silent) ElMessage.warning('请输入接收用户ID')
    return false
  }
  try {
    await axios.post(`${API_BASE_URL}/notification/send`, {
      title: '停车引导',
      content: `${buildParkingGuideMessage(fullRow)}。${fullRow.name} 当前占用 ${fullRow.occupied}/${fullRow.capacity}，请按现场指引规范停放。`,
      type: 'guide',
      isUrgent: true,
      receiverType: parkingGuideConfig.receiverType,
      receiverIds
    })
    if (!silent) ElMessage.success('引导信息已发送，学生端服务中心可查看')
    return true
  } catch (e) {
    console.error('发送引导信息失败:', e)
    if (!silent) ElMessage.error('发送引导信息失败')
    return false
  }
}

const locateDeviceOnMap = (dev) => {
  if (!map) { ElMessage.warning('地图未初始化'); return }
  const point = new BMapGL.Point(dev.currentLng || 112.6827, dev.currentLat || 26.8860)
  map.centerAndZoom(point, 19)
  keep3DMapView()
  const marker = new BMapGL.Marker(point, { icon: new BMapGL.Icon('/img/vehicle_marker.png', new BMapGL.Size(32, 32)) })
  map.addOverlay(marker)
  map.openInfoWindow(new BMapGL.InfoWindow(`<div><h4>${dev.licensePlate || '未绑定设备'}</h4><p>设备号: ${dev.deviceNumber}</p></div>`), point)
  ElMessage.success(`已定位车辆：${dev.licensePlate || dev.deviceNumber}`)
}

const viewDevDetail = (dev) => {
  detailDialog.value = {
    visible: true, title: '设备详情',
    content: `<p>设备号：${dev.deviceNumber}</p><p>车牌：${dev.licensePlate || '未绑定'}</p><p>车主：${dev.ownerName || '--'}</p><p>电量：${dev.batteryLevel}%</p><p>最后上报：${formatTime(dev.lastReportTime)}</p>`
  }
}

const addZone = async () => {
  if (!map || typeof BMapGL === 'undefined') { ElMessage.warning('地图未初始化，无法创建围栏'); return }
  const center = map.getCenter?.()
  if (!center) { ElMessage.warning('无法获取地图中心点'); return }
  const d = 0.0008
  const coordinates = JSON.stringify([
    { lng: center.lng - d, lat: center.lat - d }, { lng: center.lng + d, lat: center.lat - d },
    { lng: center.lng + d, lat: center.lat + d }, { lng: center.lng - d, lat: center.lat + d }
  ])
  const body = {
    fenceName: `快速围栏-${Date.now().toString().slice(-4)}`, fenceType: 1,
    fenceColor: '#00d4ff', coordinates, alertLevel: 2, alertMode: 'both',
    description: '大屏快速创建', status: 1
  }
  const uid = getCurrentUserId()
  if (uid) body.createUserId = uid
  try {
    const res = await axios.post(`${API_BASE_URL}/geofence/create`, body)
    if (res.data.code === 200) { ElMessage.success('电子围栏创建成功'); await fetchFenceZones(); return }
    ElMessage.error(res.data.message || '电子围栏创建失败')
  } catch (e) {
    console.error('创建围栏失败:', e)
    ElMessage.error('创建围栏失败')
  }
}

const toggleZone = async (zone) => {
  if (!zone?.id) return
  try {
    const res = await axios.put(`${API_BASE_URL}/geofence/${zone.id}/toggle`)
    if (res.data.code === 200 && res.data.data !== false) { await fetchFenceZones(); return }
    ElMessage.error(res.data.message || '切换围栏状态失败')
  } catch (e) {
    console.error('切换围栏状态失败:', e)
    ElMessage.error('切换围栏状态失败')
  }
}

const deleteZone = async (id) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确认删除该围栏？删除后不可恢复。', '删除围栏', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    const res = await axios.delete(`${API_BASE_URL}/geofence/${id}`)
    if (res.data.code === 200 && res.data.data !== false) { ElMessage.success('围栏已删除'); await fetchFenceZones(); return }
    ElMessage.error(res.data.message || '删除围栏失败')
  } catch (e) {
    console.error('删除围栏失败:', e)
    ElMessage.error('删除围栏失败')
  }
}

const openThresholdDialog = () => { settingsDialog.value = true }
const saveThresholds = () => {
  ElMessage.success('阈值设置已保存')
  settingsDialog.value = false
  page.value = 1
  fetchDevices()
}

// ---------- 定时器 ----------
let dashboardTimer = null, violationTimer = null, mapTimer = null, fenceTimer = null

const startTimers = () => {
  dashboardTimer = setInterval(fetchDashboardData, 30000)
  violationTimer = setInterval(fetchViolationAlerts, 30000)
  mapTimer = setInterval(fetchMapData, 60000)
  fenceTimer = setInterval(fetchFenceZones, 60000)
}

const getHt = () => {
  const divHeight = window.innerHeight - 84
  const el = document.getElementById('car_control')
  if (el) el.style.height = divHeight + 'px'
  setTimeout(resizeDashboardMap, 0)
}

// ---------- 生命周期 ----------
onMounted(() => {
  window.__evTrackReplay = handleTrackReplayAction
  updateTime()
  setInterval(updateTime, 1000)
  getHt()
  window.addEventListener('resize', getHt)
  loadBaiduMapAPI()
  fetchDashboardData()
  fetchViolationAlerts()
  fetchFenceZones()
  fetchRealtimeData()
  searchVehicles()
  initTrendChart()
  startTimers()
})

onUnmounted(() => {
  if (window.__evTrackReplay === handleTrackReplayAction) delete window.__evTrackReplay
  clearInterval(dashboardTimer)
  clearInterval(violationTimer)
  clearInterval(mapTimer)
  clearInterval(fenceTimer)
  if (markerRefreshTimer) clearInterval(markerRefreshTimer)
  stopSimLocationStream()
  stopDemoPointStream({ silent: true })
  stopLocationPolling()
  clearTrackReplay()
  removeDemoPointMarkers()
  clearTrackRouteOverlays()
  clearParkingGuideOverlays()
  if (mapContainerEl) {
    mapContainerEl.removeEventListener('click', handleTrackRouteDomClick, true)
    mapContainerEl = null
  }
  clearMapDataOverlays()
  clearGeofenceOverlays()
  window.removeEventListener('resize', getHt)
  trendChart?.dispose()
  distChartAnalysis?.dispose()
  heatmapChartAnalysis?.dispose()
})
</script>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }
a { text-decoration: none; }
.monitor-container { width: 100%; height: 100vh; position: relative; overflow: hidden; background: url("/img/campus-page-bg.jpg") no-repeat center center; background-size: cover; display: flex; flex-direction: column; font-family: 'Microsoft YaHei', 'PingFang SC', 'Hiragino Sans GB', Arial, sans-serif; }
.clear { clear: both; }

/* ===================== 顶部导航 ===================== */
.header {
  width: 100%;
  height: 84px;
  color: #ffffff;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 12px 20px 0;
  flex-shrink: 0;
  position: relative;
  background: transparent;
  z-index: 10;
}

/* 标题：绝对居中，挂在顶部边框装饰区域内 */
.head-logo {
  position: absolute;
  left: 50%;
  top: 10px;
  transform: translateX(-50%);
  margin: 0;
  padding: 12px 32px 14px;
  font-size: 1.7rem;
  line-height: 1;
  font-weight: 700;
  letter-spacing: 4px;
  white-space: nowrap;
  color: #eefbff;
  text-shadow: 0 0 10px rgba(39, 212, 255, 0.75), 0 0 24px rgba(39, 212, 255, 0.35);
  background: linear-gradient(180deg, rgba(4, 17, 50, 0.56), rgba(4, 17, 50, 0.2));
  border: 1px solid rgba(39, 212, 255, 0.35);
  border-radius: 0 0 18px 18px;
  backdrop-filter: blur(5px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.18);
}

.head-ul { list-style: none; flex: 1; display: flex; flex-wrap: wrap; justify-content: center; visibility: hidden; }
.head-ul>li { float: left; padding: 5px 12px; line-height: 1.5; }
.head-ul>li>a { color: #ffffff; display: inline-block; padding: 3px; cursor: pointer; font-size: 0.9rem; white-space: nowrap; }
.active-a { color: #ffffff !important; font-weight: bold; text-shadow: 1px 2px 2px #ffffff; border-bottom: 2px solid #ffffff; }

/* 右侧时间天气 */
.header-right {
  float: right;
  margin-right: 0;
  line-height: 1.6;
  color: #c8f4ff;
  font-size: 0.8rem;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  white-space: nowrap;
  padding: 8px 14px;
  background: rgba(4, 17, 50, 0.42);
  border: 1px solid rgba(39, 212, 255, 0.25);
  border-radius: 12px;
  backdrop-filter: blur(5px);
}
.header-right span { margin-right: 0; }

/* ===================== 主内容 ===================== */
.section { width: 100%; height: calc(100vh - 84px); flex: 1; }
.commanBox { background: transparent; overflow: hidden; height: 100%; display: flex; padding: 0; gap: 0; }

/* 左侧 */
.boxLeft { width: 28%; min-width: 280px; max-width: 350px; height: 100%; display: flex; flex-direction: column; gap: 10px; flex-shrink: 0; padding: 10px; }
.boxLeftVideoTop { width: 100%; height: 40px; background-image: url("/img/videoTop.png"); background-size: 100% 100%; background-repeat: no-repeat; flex-shrink: 0; }
.boxLeftVideoBox { width: 100%; flex: 1; min-height: 300px; background-image: url("/img/videoBox.png"); background-size: 100% 100%; background-repeat: no-repeat; padding: 15px; display: flex; flex-direction: column; gap: 10px; overflow: hidden; }
.boxLeftVideoBoxCon { width: 100%; height: 200px; min-height: 180px; overflow: hidden; flex-shrink: 0; }
.BoxConTop { color: #33c2fe; font-size: 0.8rem; display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; flex-shrink: 0; }
.BoxconTopLeft { float: left; margin-left: 3%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.BoxconTopRight { float: right; margin-right: 2%; flex-shrink: 0; }
.boxLeftVideoBoxCon video { width: 100%; height: 100%; object-fit: cover; border-radius: 4px; }
.boxLeftAram { width: 100%; height: 35%; min-height: 220px; margin-top: 1%; display: flex; flex-direction: column; flex-shrink: 0; }
.boxLeftAlamTop { width: 100%; height: 40px; background-image: url("/img/alamTop.png"); background-repeat: no-repeat; background-size: 100% 100%; flex-shrink: 0; }
.boxLeftAlamCon { width: 100%; flex: 1; min-height: 180px; background-image: url("/img/alamCon.png"); background-repeat: no-repeat; background-size: 100% 100%; overflow: hidden; padding: 10px; display: flex; flex-direction: column; }
.alamUl { list-style: none; font-size: 0.75rem; color: #1f9dff; padding: 0; margin: 0; flex: 1; overflow-y: auto; }
.alamULspan { float: right; font-size: 0.7rem; color: #00ffea; }
.alamUl li { padding: 8px; background-image: url("/img/li_bg.png"); background-size: 100% 100%; background-repeat: no-repeat; margin-bottom: 8px; cursor: pointer; transition: all 0.3s ease; word-break: break-word; }
.alamUl li:hover { transform: translateX(5px); background: rgba(31, 157, 255, 0.2); }
.alamUl li p { padding: 2px 0; margin: 0; line-height: 1.3; }

/* 中间 */
.boxCenter { width: 44%; min-width: 350px; flex: 1; height: 100%; display: flex; flex-direction: column; gap: 10px; padding: 10px; position: relative; }
.boxCenterUp { width: 100%; height: 120px; min-height: 100px; display: flex; justify-content: space-around; align-items: center; background: url("/img/anaBg.png") no-repeat center center; background-size: 100% 100%; flex-shrink: 0; padding: 10px; }
.boxCentterUpLeft { color: #00f0ff; font-size: 1rem; text-align: center; position: relative; flex: 1; max-width: 30%; }
.anaText { text-align: center; margin-top: 5px; width: 100%; font-size: 0.9rem; }
.ananBg { width: 80px; height: 80px; margin: 0 auto; background-image: url("/img/anaBg.png"); background-size: 100% 100%; background-repeat: no-repeat; font-size: 1rem; text-align: center; line-height: 80px; position: relative; }
.ana01IMg { animation: anima01 8s linear infinite; width: 100%; height: 100%; }
.anaImgText, .anaImgText01, .anaImgText02 { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); z-index: 100; color: #27d4ff; font-size: 1.2rem; font-weight: bold; }
@keyframes anima01 { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

/* 地图容器 */
.map_con { width: 100%; height: 100%; background-color: #081832cc; padding: 0; box-sizing: border-box; overflow: auto; position: relative; }
.map_con.left { float: left; }
.map_left { width: 100%; height: 100%; background-color: transparent; position: relative; }
.map_box { width: 100%; height: 100%; background-color: #0a1a2f; overflow: hidden; position: absolute; top: 0; left: 0; z-index: 1; }
.track-route-float {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 1200;
  width: min(420px, calc(100% - 24px));
  padding: 12px;
  border: 1px solid rgba(255,209,102,0.72);
  border-radius: 8px;
  background: rgba(6, 19, 37, 0.88);
  box-shadow: 0 0 18px rgba(255,209,102,0.18);
  backdrop-filter: blur(6px);
  pointer-events: auto;
}
.track-route-float-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; color: #ffe8a3; font-size: 0.9rem; font-weight: 700; }
.track-route-float-head b { color: #27d4ff; font-size: 0.82rem; }
.track-route-float-tip { margin-top: 7px; color: #c9e6ff; font-size: 0.78rem; line-height: 1.5; }
.track-route-float-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.route-float-btn { height: 28px; padding: 0 12px; border: 1px solid rgba(39,212,255,0.35); border-radius: 5px; background: rgba(9,43,78,0.72); color: #c9e6ff; font-size: 0.76rem; cursor: pointer; }
.route-float-btn:hover { border-color: rgba(39,212,255,0.7); background: rgba(18,75,118,0.82); }
.route-float-btn.primary { background: linear-gradient(135deg, #0984e3, #00c2ff); border-color: rgba(39,212,255,0.75); color: #fff; font-weight: 700; }
.route-float-btn.danger { border-color: rgba(255,126,126,0.42); color: #ffc3c3; background: rgba(91,20,28,0.62); }
/* ===================== 3D地图控制面板 ===================== */
.map_3d_control {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 按钮组：横排 */
.control_panel {
  background: rgba(4, 16, 40, 0.88);
  border: 1px solid rgba(39, 212, 255, 0.3);
  border-radius: 10px;
  padding: 8px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(39, 212, 255, 0.1);
}

.control_item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  color: rgba(180, 230, 255, 0.75);
  background: rgba(13, 139, 189, 0.1);
  border: 1px solid rgba(39, 212, 255, 0.15);
  position: relative;
}

.control_item:hover {
  background: rgba(13, 139, 189, 0.3);
  color: #fff;
  border-color: rgba(39, 212, 255, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(0, 212, 255, 0.15);
}

.control_item.active {
  background: rgba(24, 158, 255, 0.25);
  border-color: rgba(39, 212, 255, 0.6);
  color: #27d4ff;
  box-shadow: 0 0 12px rgba(39, 212, 255, 0.2);
}

/* 删除按钮悬停红色 */
.control_item--danger {
  margin-left: 2px;
  border-left: 1px solid rgba(39, 212, 255, 0.15);
}
.control_item--danger:hover {
  background: rgba(255, 80, 60, 0.2);
  border-color: rgba(255, 120, 100, 0.5);
  color: #ffb3a7;
  box-shadow: 0 3px 10px rgba(255, 80, 60, 0.15);
}

.control-svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.control-svg-fill { fill: currentColor; }

/* 状态指示点 */
.control-status-dot {
  position: absolute;
  right: 5px;
  bottom: 5px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #27d4ff;
  box-shadow: 0 0 6px rgba(39, 212, 255, 0.9);
}
.control_item:not(.active) .control-status-dot {
  background: rgba(255, 255, 255, 0.2);
  box-shadow: none;
}

/* Tooltip（复用 title 属性） */
.control_item::after {
  content: attr(title);
  position: absolute;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(4, 16, 40, 0.95);
  color: #27d4ff;
  font-size: 11px;
  white-space: nowrap;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid rgba(39, 212, 255, 0.3);
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.15s ease;
  z-index: 10;
}
.control_item:hover::after { opacity: 1; }

/* 视角控制 */
.view_control { background: rgba(8,24,50,0.9); border: 1px solid #0D8BBD; border-radius: 8px; padding: 10px; display: flex; flex-direction: column; align-items: center; gap: 5px; backdrop-filter: blur(5px); }
.view_up, .view_down, .view_left, .view_right, .view_center { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: rgba(13,139,189,0.3); border: 1px solid #27d4ff; border-radius: 4px; color: #27d4ff; font-size: 1.2rem; cursor: pointer; transition: all 0.3s ease; user-select: none; }
.view_middle { display: flex; gap: 5px; }
.view_up:hover, .view_down:hover, .view_left:hover, .view_right:hover, .view_center:hover { background: rgba(13,139,189,0.5); transform: scale(1.1); }
.view_center { background: rgba(39,212,255,0.2); }

/* 终端定位点 */
.demo-point-panel {
  flex: 1 1 100%;
  width: 100%;
  min-width: 0;
  padding: 10px;
  background: rgba(4, 16, 40, 0.9);
  border: 1px solid rgba(0, 255, 127, 0.4);
  border-radius: 8px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(0, 255, 127, 0.12);
  backdrop-filter: blur(8px);
}
.demo-point-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #00ff7f;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
  margin-bottom: 8px;
  white-space: nowrap;
  flex-wrap: wrap;
}
.demo-point-head-btn {
  height: 26px;
  padding: 0 8px;
  color: #85acd5;
  font-size: 12px;
  cursor: pointer;
  background: rgba(13,139,189,0.18);
  border: 1px solid rgba(39,212,255,0.28);
  border-radius: 4px;
}
.demo-point-head-btn.primary { color: #042818; background: #00ff7f; border-color: #00ff7f; }
.demo-point-head-btn.danger { color: #ffc3c3; border-color: rgba(255,126,126,0.35); }
.demo-point-head-btn.motion-on { color: #06172d; font-weight: 700; background: #27d4ff; border-color: #27d4ff; }
.demo-point-head-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.demo-point-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #00ff7f;
  box-shadow: 0 0 10px rgba(0, 255, 127, 0.95);
}
.demo-point-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 2px;
}
.demo-point-row {
  display: grid;
  grid-template-columns: 44px 58px minmax(150px, 1.4fr) minmax(0, 1fr) minmax(0, 1fr) 50px 50px 50px;
  align-items: center;
  gap: 6px;
  padding: 6px;
  background: rgba(6, 28, 50, 0.5);
  border: 1px solid rgba(39, 212, 255, 0.16);
  border-radius: 5px;
}
.demo-point-name {
  color: #85ffd0;
  font-size: 12px;
  white-space: nowrap;
}
.demo-point-toggle {
  height: 30px;
  color: #bde6ff;
  font-size: 12px;
  cursor: pointer;
  background: rgba(158,158,158,0.18);
  border: 1px solid rgba(158,158,158,0.35);
  border-radius: 4px;
}
.demo-point-toggle.on {
  color: #042818;
  font-weight: 700;
  background: #00ff7f;
  border-color: #00ff7f;
}
.demo-point-input,
.demo-point-select {
  min-width: 0;
  height: 30px;
  padding: 0 8px;
  color: #d8fff0;
  font-size: 12px;
  background: rgba(6, 28, 50, 0.86);
  border: 1px solid rgba(39, 212, 255, 0.35);
  border-radius: 4px;
  outline: none;
}
.demo-point-select {
  color: #d8fff0;
}
.demo-point-input:focus,
.demo-point-select:focus {
  border-color: rgba(0, 255, 127, 0.75);
  box-shadow: 0 0 8px rgba(0, 255, 127, 0.18);
}
.demo-point-input::placeholder { color: rgba(216, 255, 240, 0.45); }
.demo-point-bind-text {
  grid-column: 3 / -1;
  color: rgba(133,255,208,0.72);
  font-size: 11px;
  line-height: 16px;
}
.demo-point-btn {
  height: 30px;
  color: #85acd5;
  font-size: 12px;
  cursor: pointer;
  background: rgba(13,139,189,0.2);
  border: 1px solid rgba(39,212,255,0.32);
  border-radius: 4px;
  transition: all 0.2s ease;
}
.demo-point-btn:hover {
  color: #fff;
  border-color: rgba(39,212,255,0.65);
  background: rgba(13,139,189,0.34);
}
.demo-point-btn.primary {
  color: #042818;
  font-weight: 700;
  background: #00ff7f;
  border-color: #00ff7f;
}
.demo-point-btn.primary:hover {
  background: #29ff96;
  border-color: #29ff96;
}
.demo-point-btn.warn {
  color: #ffe7a3;
  border-color: rgba(255,170,0,0.38);
  background: rgba(255,170,0,0.12);
}

/* 趋势图（地图左上角） */
.trend-chart-wrapper { position: absolute; top: 10px; left: 10px; z-index: 1000; background: rgba(8,24,50,0.8); border: 1px solid #0D8BBD; border-radius: 8px; padding: 8px; width: 220px; backdrop-filter: blur(5px); }
.trend-chart-container { width: 100%; height: 100px; }

/* 右侧 */
.boxRight { width: 28%; min-width: 280px; max-width: 350px; height: 100%; display: flex; flex-direction: column; gap: 10px; flex-shrink: 0; padding: 10px; }
.boxRightTop { width: 100%; height: 40px; background-image: url("/img/rightTop.png"); background-size: 100% 100%; background-repeat: no-repeat; flex-shrink: 0; }
.boxRightCon01 { width: 100%; height: 26.5%; min-height: 200px; background-image: url("/img/rightCon01.png"); background-size: 100% 100%; background-repeat: no-repeat; margin-top: 2%; overflow: hidden; padding: 10px; }
.boxRightCon_ul { list-style: none; margin: 0; padding: 0; width: 100%; display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; height: 100%; overflow-y: auto; }
.boxRightCon_ul li { background-image: url("/img/li_border.png"); background-repeat: no-repeat; background-size: 100% 100%; font-size: 0.7rem; color: #0fc9ff; cursor: pointer; display: flex; align-items: center; justify-content: space-between; padding: 6px; height: 32px; min-height: 30px; word-break: break-all; overflow: hidden; }
.boxRightCon_ul li span { width: 16px; height: 16px; display: inline-block; flex-shrink: 0; }
.boxRightCon_ul li span img { width: 100%; height: 100%; }
.boxRightCon02 { width: 100%; flex: 1; min-height: 320px; margin-top: 6%; }
.boxRightcon02Top { width: 100%; height: 40px; background-image: url("/img/rightCon02Top.png"); background-size: 100% 100%; background-repeat: no-repeat; flex-shrink: 0; }
.con02TopUL { width: 80%; list-style: none; margin-left: 20%; height: 100%; display: flex; padding: 0; }
.con02TopUL li { float: left; width: 33%; text-align: center; color: #1fbdff; font-size: 0.8rem; cursor: pointer; line-height: 40px; height: 100%; display: flex; align-items: center; justify-content: center; white-space: nowrap; }
.con02liAcitve { background-image: url("/img/rightCon02LiBg.png"); background-repeat: no-repeat; background-size: 100% 100%; }
.boxRithtChangeBox { width: 100%; height: calc(100% - 40px); background: url("/img/rightcon2_bg.png") no-repeat center center; background-size: 100% 100%; padding: 15px; overflow: hidden; }
.boxChangeCon { width: 100%; height: 100%; display: flex; flex-direction: column; overflow: hidden; }
.boxChangeConFind { width: 100%; padding-top: 2%; padding-bottom: 2%; border-bottom: 1px solid #32a1f7; flex-shrink: 0; }
.boxFindLabel { color: #85acd5; font-size: 0.75rem; margin-left: 2%; width: 25%; text-align: right; display: inline-block; white-space: nowrap; }
.addLi { height: 40px !important; padding: 8px !important; }
.boxFindInput { width: 25%; background-color: transparent; border: 1px solid #85acd5; margin-right: 2%; color: #85acd5; margin-top: 2%; padding: 4px 8px; border-radius: 3px; font-size: 0.75rem; min-width: 0; }
.boxFindInput:focus { outline: none; border-color: #27d4ff; }
.boxFindButton { width: 30%; height: 28px; border: none; background-color: #0f77cd; color: #ffffff; border-radius: 3px; float: right; margin-right: 2%; font-size: 0.75rem; cursor: pointer; transition: background-color 0.3s; white-space: nowrap; min-width: 80px; }
.boxFindButton:hover { background-color: #1db1cd; }
.boxChangeCon ul { list-style: none; width: 100%; margin: 0; padding: 0; flex: 1; overflow-y: auto; margin-top: 10px; min-height: 160px; }
.boxChangeCon ul li { width: 98%; padding: 8px; border-bottom: 1px solid #32a1f7; min-height: 70px; display: flex; align-items: center; margin-bottom: 8px; background: rgba(31,157,255,0.05); border-radius: 4px; }
.liImgLeft { width: 55px; height: 55px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.liImgLeft img { width: 100%; height: 100%; object-fit: contain; }
.liConRight, .liConRight01 { flex: 1; height: 100%; margin-left: 12px; color: #1f9dff; font-size: 0.8rem; min-width: 0; overflow: hidden; }
.liconRihtButSpan { float: right; width: 130px; display: flex; gap: 6px; flex-shrink: 0; }
.liconRihtButSpan01 { float: right; width: 55px; flex-shrink: 0; }
.liconRihtButSpan01 button, .liconRihtButSpan button { width: 100%; height: 25px; color: #ffffff; font-size: 0.75rem; background-image: url("/img/tailBut.png"); background-repeat: no-repeat; background-size: 100% 100%; border: none; display: inline-block; border-radius: 3px; cursor: pointer; white-space: nowrap; }
.liconRihtButSpan button { width: 48%; }
.liConRight p { padding: 2px 0; display: flex; justify-content: space-between; align-items: center; margin: 0; line-height: 1.4; }

/* 分页 */
.boxChangeFoot { width: 100%; height: 24px; text-align: center; margin-top: 10px; flex-shrink: 0; }
.boxChangeFoot a { text-decoration: none; color: #7597ba; font-size: 0.8rem; cursor: pointer; padding: 5px; margin: 0 5px; }
.boxChangeFoot span { color: #1f9dff; }
.page_box { display: inline-block; }

/* 右侧分析图表 */
.analysis-chart-wrapper { width: 100%; height: 100%; display: flex; flex-direction: column; overflow-y: auto; padding-right: 5px; }
.chart-item { width: 100%; background: rgba(8,24,50,0.6); border-radius: 8px; padding: 10px; border: 1px solid rgba(13,139,189,0.3); margin-bottom: 10px; }
.chart-title { color: #27d4ff; font-size: 0.8rem; margin-bottom: 8px; padding-left: 5px; border-left: 3px solid #27d4ff; }
.analysis-chart-container { width: 100%; height: 150px; }

/* 弹窗 */
:deep(.el-dialog) { background: linear-gradient(135deg, #1a2a3a 0%, #2c3e50 100%); border: 1px solid #27d4ff; box-shadow: 0 0 20px rgba(39,212,255,0.5); }
:deep(.el-dialog__header) { background: rgba(39,212,255,0.1); border-bottom: 1px solid #27d4ff; color: #27d4ff; padding: 15px 20px; }
:deep(.el-dialog__title) { color: #27d4ff; font-size: 1rem; }
:deep(.el-dialog__body) { color: #85acd5; padding: 20px; font-size: 0.9rem; }
:deep(.el-dialog__body p) { margin: 10px 0; line-height: 1.6; }
:deep(.el-button) { background: #0f77cd; border: none; color: #ffffff; font-size: 0.8rem; padding: 8px 15px; }
:deep(.el-button--primary) { background: #27d4ff; }
:deep(.el-button:hover) { background: #1db1cd; }

/* 定位管理弹窗内容样式 */
.location-dialog :deep(.el-dialog__body) { padding: 20px; }
.location-dialog-content { color: #fff; }
.location-stats { display: flex; justify-content: space-around; background: rgba(8,24,50,0.8); border: 1px solid #0D8BBD; border-radius: 6px; padding: 12px 10px; margin-bottom: 15px; }
.stat-item { text-align: center; color: #85acd5; font-size: 0.9rem; }
.stat-number { display: block; font-size: 1.4rem; font-weight: bold; }
.c-green { color: #4caf50; }
.c-red { color: #f44336; }
.c-warn { color: #ff9800; }
.location-filter { display: flex; gap: 10px; margin-bottom: 15px; align-items: center; flex-wrap: wrap; }
.filter-input, .filter-select { padding: 8px 12px; background: rgba(0,0,0,0.3); border: 1px solid #27d4ff; border-radius: 4px; color: #fff; font-size: 0.9rem; }
.filter-input { flex: 1; }
.filter-select { width: 120px; }
.filter-btn { padding: 8px 16px; background: #27d4ff; border: none; border-radius: 4px; color: #000; cursor: pointer; font-weight: bold; }
.filter-btn.primary { background: #0f77cd; color: #fff; }
.location-filter-actions { margin-left: auto; display: flex; align-items: center; gap: 6px; opacity: 0.78; }
.filter-hint { color: rgba(133,172,213,0.78); font-size: 0.72rem; letter-spacing: 1px; }
.filter-btn.subtle { padding: 6px 10px; background: rgba(9,43,78,0.55); border: 1px solid rgba(39,212,255,0.24); color: rgba(181,230,255,0.9); font-size: 0.74rem; font-weight: 500; }
.filter-btn.subtle:hover { background: rgba(16,83,136,0.45); border-color: rgba(39,212,255,0.42); }
.filter-btn.subtle:disabled { opacity: 0.45; cursor: not-allowed; }
.filter-btn.subtle.subtle-danger { color: rgba(255,195,195,0.88); border-color: rgba(255,126,126,0.22); }
.device-table-wrap { max-height: 300px; overflow-y: auto; margin-bottom: 15px; background: rgba(0,0,0,0.2); border-radius: 6px; }
.device-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
.device-table th { background: #0a1a2f; color: #27d4ff; padding: 10px 5px; position: sticky; top: 0; }
.device-table td { padding: 8px 5px; border-bottom: 1px solid rgba(39,212,255,0.2); color: #85acd5; }
.battery-bar { width: 50px; height: 8px; background: rgba(255,255,255,0.2); border-radius: 4px; overflow: hidden; display: inline-block; vertical-align: middle; margin-right: 6px; }
.battery-fill { height: 100%; border-radius: 4px; }
.signal { padding: 4px 8px; border-radius: 12px; background: rgba(255,255,255,0.1); }
.signal.level4 { background: #4caf50; color: #000; }
.signal.level3 { background: #ff9800; color: #000; }
.signal.level1 { background: #f44336; color: #000; }
.dev-status { padding: 4px 8px; border-radius: 12px; font-size: 0.75rem; }
.dev-status.online { background: rgba(76,175,80,0.2); color: #4caf50; }
.dev-status.offline { background: rgba(244,67,54,0.2); color: #f44336; }
.dev-status.low_battery { background: rgba(255,152,0,0.2); color: #ff9800; }
.table-btn { background: transparent; border: 1px solid #27d4ff; color: #27d4ff; padding: 4px 8px; border-radius: 4px; font-size: 0.75rem; cursor: pointer; margin: 0 3px; }
.table-btn:hover { background: rgba(39,212,255,0.2); }
.page-controls { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding: 10px 0; }
.page-btn { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.1); border-radius: 4px; cursor: pointer; color: #85acd5; }
.page-btn.disabled { opacity: 0.3; cursor: not-allowed; }
.page-info { font-size: 0.9rem; color: #85acd5; }
.location-extra { background: rgba(8,24,50,0.8); border: 1px solid #0D8BBD; border-radius: 8px; overflow: hidden; margin-top: 10px; }
.extra-header { display: flex; justify-content: space-between; padding: 12px 15px; background: rgba(13,139,189,0.2); color: #27d4ff; cursor: pointer; font-size: 0.9rem; }
.extra-body { padding: 15px; max-height: 430px; overflow-y: auto; }
.zone-list { display: flex; flex-direction: column; gap: 10px; }
.zone-item { display: flex; align-items: center; gap: 10px; padding: 8px 10px; background: rgba(0,0,0,0.2); border-radius: 6px; cursor: pointer; font-size: 0.8rem; }
.zone-item.active { border: 1px solid #27d4ff; }
.zone-icon { font-size: 1.2rem; width: 30px; text-align: center; }
.zone-info { flex: 1; }
.zone-name { color: #fff; font-weight: bold; }
.zone-desc { color: #85acd5; font-size: 0.7rem; }
.zone-actions { display: flex; gap: 8px; }
.zone-badge { padding: 3px 8px; border-radius: 12px; font-size: 0.7rem; cursor: pointer; }
.zone-badge.on { background: rgba(76,175,80,0.2); color: #4caf50; border: 1px solid rgba(76,175,80,0.3); }
.zone-badge.off { background: rgba(158,158,158,0.2); color: #9e9e9e; border: 1px solid rgba(158,158,158,0.3); }
.zone-del { cursor: pointer; opacity: 0.6; }
.zone-del:hover { opacity: 1; }
.threshold-mini { margin-top: 15px; }
.threshold-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; font-size: 0.8rem; color: #85acd5; border-bottom: 1px solid rgba(255,255,255,0.1); }
.threshold-item label { width: 80px; }
.threshold-input { width: 60px; background: rgba(0,0,0,0.3); border: 1px solid #27d4ff; color: #fff; padding: 4px; border-radius: 4px; font-size: 0.8rem; text-align: center; }
.assist-panel { margin-top: 14px; padding: 10px; border: 1px solid rgba(39,212,255,0.2); background: rgba(0,0,0,0.18); border-radius: 6px; }
.assist-title { display: flex; justify-content: space-between; align-items: center; gap: 10px; color: #27d4ff; font-size: 0.9rem; font-weight: 700; margin-bottom: 9px; }
.assist-title em { color: #85acd5; font-style: normal; font-size: 0.76rem; font-weight: 500; }
.assist-form { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; color: #85acd5; font-size: 0.78rem; margin-top: 8px; }
.assist-select, .assist-input, .assist-text { height: 28px; background: rgba(0,0,0,0.32); border: 1px solid rgba(39,212,255,0.45); color: #fff; border-radius: 4px; padding: 0 8px; outline: none; }
.assist-select { min-width: 210px; }
.assist-select.slim { min-width: 112px; }
.assist-input { width: 68px; text-align: center; }
.assist-text { min-width: 190px; }
.assist-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.track-progress { width: 100%; height: 6px; background: rgba(255,255,255,0.08); border-radius: 999px; overflow: hidden; margin-top: 10px; }
.track-progress span { display: block; height: 100%; background: linear-gradient(90deg, #00ff7f, #27d4ff); transition: width 0.18s ease; }
.track-route-hint { padding: 8px 10px; color: #85acd5; font-size: 0.78rem; border: 1px dashed rgba(39,212,255,0.28); border-radius: 5px; background: rgba(5, 23, 42, 0.45); }
.track-route-hint.on { color: #ffe8a3; border-color: rgba(255,209,102,0.58); background: rgba(122, 79, 0, 0.18); }
.track-route-points { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 7px; max-height: 116px; overflow-y: auto; margin-top: 8px; padding-right: 2px; }
.track-route-point { display: flex; align-items: center; gap: 8px; min-width: 0; padding: 6px 8px; color: #c9e6ff; font-size: 0.76rem; background: rgba(5,23,42,0.72); border: 1px solid rgba(39,212,255,0.18); border-radius: 5px; }
.track-route-point b { width: 18px; height: 18px; line-height: 18px; flex: 0 0 18px; text-align: center; color: #06172a; background: #27d4ff; border-radius: 50%; }
.track-route-point span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.track-route-empty { padding: 8px; color: #85acd5; font-size: 0.78rem; background: rgba(5,23,42,0.45); border-radius: 5px; }
.parking-guide-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 8px; margin-top: 8px; }
.parking-guide-row { display: grid; grid-template-columns: 1fr auto; gap: 3px 8px; padding: 8px; background: rgba(5, 23, 42, 0.72); border: 1px solid rgba(39,212,255,0.18); border-radius: 5px; color: #c9e6ff; font-size: 0.78rem; }
.parking-guide-row span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.parking-guide-row b { color: #27d4ff; font-weight: 700; }
.parking-guide-row em { grid-column: 1 / -1; color: #85acd5; font-style: normal; }
.parking-guide-row.full { border-color: rgba(255,126,126,0.65); background: rgba(76, 12, 20, 0.48); }
.parking-guide-row.full b, .parking-guide-row.full em { color: #ff9b9b; }
.parking-guide-empty { padding: 10px; color: #85acd5; font-size: 0.78rem; border: 1px dashed rgba(39,212,255,0.28); border-radius: 5px; }
.toggle-wrap { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.toggle-track { width: 40px; height: 20px; background: rgba(255,255,255,0.2); border-radius: 10px; position: relative; }
.toggle-track.on { background: rgba(76,175,80,0.5); }
.toggle-thumb { width: 16px; height: 16px; background: #fff; border-radius: 50%; position: absolute; top: 2px; left: 2px; transition: left 0.2s; }
.toggle-track.on .toggle-thumb { left: 22px; }
.action-btn.small { padding: 6px 12px; font-size: 0.8rem; }

/* 滚动条 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,0.1); border-radius: 3px; }
::-webkit-scrollbar-thumb { background: rgba(39,212,255,0.5); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(39,212,255,0.8); }

/* ===================== 响应式 ===================== */
@media screen and (max-width: 1600px) {
  .boxLeft, .boxRight { min-width: 260px; }
  .boxCenter { min-width: 320px; }
  .header-right { font-size: 0.7rem; }
  .head-logo { font-size: 1.3rem; letter-spacing: 3px; }
  .map_3d_control { top: 5px; right: 5px; }
  .control_item { width: 36px; height: 36px; }
  .control-svg { width: 16px; height: 16px; }
  .view_up, .view_down, .view_left, .view_right, .view_center { width: 35px; height: 35px; }
}
@media screen and (max-width: 1400px) {
  .boxLeft, .boxRight { min-width: 240px; }
  .boxCenter { min-width: 280px; }
  .boxLeftVideoBoxCon { height: 180px; }
  .head-logo { font-size: 1.1rem; padding: 10px 20px 12px; letter-spacing: 2px; }
}
@media screen and (max-width: 1200px) {
  .commanBox { flex-wrap: wrap; height: auto; overflow-y: auto; }
  .boxLeft, .boxCenter { width: 48%; min-width: 300px; max-width: none; height: 600px; }
  .boxRight { width: 100%; min-width: auto; max-width: none; height: 500px; margin-top: 10px; }
  .header { height: auto; min-height: 132px; padding: 72px 10px 10px; flex-wrap: wrap; }
  .head-logo { top: 8px; max-width: calc(100% - 24px); padding: 10px 20px 12px; font-size: 1.2rem; letter-spacing: 2px; overflow: hidden; text-overflow: ellipsis; }
  .header-right { order: 2; margin-left: auto; flex-direction: row; gap: 15px; }
  .map_3d_control { position: relative; top: 0; right: 0; margin: 10px auto; width: auto; }
  .control_panel { flex-direction: row; flex-wrap: wrap; justify-content: center; }
  .view_control { flex-direction: row; }
}
@media screen and (max-width: 768px) {
  .commanBox { padding: 5px; gap: 5px; }
  .boxLeft, .boxCenter, .boxRight { width: 100%; min-width: auto; height: auto; min-height: 380px; }
  .boxLeftVideoBox, .boxLeftAram, .boxCenterUp, .map_con, .boxRightCon01, .boxRightCon02 { height: auto !important; min-height: 280px; }
  .boxLeftVideoBoxCon { height: 180px; }
  .map_con { min-height: 220px; }
  .header { flex-direction: column; align-items: center; height: auto; min-height: 118px; padding: 66px 10px 10px; }
  .head-logo { width: auto; max-width: calc(100% - 20px); text-align: center; margin: 0; top: 8px; font-size: 1rem; letter-spacing: 1px; padding: 9px 14px 11px; }
  .header-right { width: auto; justify-content: center; margin: 5px 0 0; padding: 8px 10px; gap: 10px; }
}
@media screen and (max-width: 480px) {
  .head-logo { font-size: 0.88rem; letter-spacing: 0.5px; }
  .header-right { font-size: 0.7rem; flex-direction: column; align-items: center; gap: 5px; }
  .boxFindInput { width: 40%; margin-right: 1%; }
  .boxFindLabel { width: 30%; }
  .liconRihtButSpan { width: 110px; }
  .liImgLeft { width: 45px; height: 45px; }
  .boxCenter { min-width: 250px; }
  .map_con { min-height: 200px; }
  .demo-point-panel { padding: 8px; }
  .demo-point-list { grid-template-columns: 1fr; }
  .demo-point-row { grid-template-columns: 52px 58px minmax(0, 1fr); }
  .demo-point-select, .demo-point-bind-text { grid-column: 1 / -1; }
  .demo-point-btn { width: 100%; }
  .control_item { width: 34px; height: 34px; }
  .view_up, .view_down, .view_left, .view_right, .view_center { width: 30px; height: 30px; font-size: 1rem; }
}
</style>

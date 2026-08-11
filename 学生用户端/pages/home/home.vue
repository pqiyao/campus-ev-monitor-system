<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-main">
          <view class="hero-brand">
            <image class="hero-logo" src="/static/logo.png" mode="aspectFit" />
            <view>
              <text class="hero-brand-label">校园电动车</text>
              <text class="hero-brand-sub">学生端</text>
            </view>
          </view>
          <text class="hello">{{ greeting }}</text>
          <text class="sub">{{ heroSub }}</text>

          <view class="hero-signal-row">
          <view class="hero-signal">
              <ev-icon name="bike" size="28rpx" color="#2563eb" />
              <text>{{ vehicleLine }}</text>
            </view>
            <view class="hero-signal">
              <ev-icon name="location" size="28rpx" color="#2563eb" />
              <text>{{ gpsLine }}</text>
            </view>
          </view>
        </view>

        <view :class="['hero-badge', hasPendingViolation ? 'warn' : 'ok']">
          <view class="hero-badge-icon">
            <ev-icon :name="hasPendingViolation ? 'warning' : 'shield'" size="32rpx" :color="hasPendingViolation ? '#c2410c' : '#2563eb'" />
          </view>
          <text class="hero-badge-num">{{ pendingViolationCount }}</text>
          <text class="hero-badge-txt">{{ hasPendingViolation ? '待整改' : '状态良好' }}</text>
        </view>
      </view>

      <view class="hero-strip">
        <view v-for="item in heroHighlights" :key="item.label" class="hero-strip-item">
          <view class="hero-strip-icon">
            <ev-icon :name="item.icon" size="28rpx" color="#2563eb" />
          </view>
          <view class="hero-strip-copy">
            <text class="hero-strip-k">{{ item.label }}</text>
            <text class="hero-strip-v">{{ item.value }}</text>
          </view>
        </view>
      </view>
    </view>

    <view :class="['alert', hasPendingViolation ? 'alert-warn' : 'alert-ok']">
      <view class="alert-mark">
        <ev-icon :name="hasPendingViolation ? 'warning' : 'check'" size="30rpx" :color="hasPendingViolation ? '#c2410c' : '#2563eb'" />
      </view>
      <view class="alert-copy">
        <text class="alert-title">{{ hasPendingViolation ? '整改提醒' : '运行状态正常' }}</text>
        <text class="alert-msg">{{ bannerText }}</text>
      </view>
    </view>

    <view class="stats">
      <view v-for="item in statCards" :key="item.label" class="stat-card" @click="item.url ? go(item.url) : null">
        <view :class="['stat-ico', item.tone]">
          <ev-icon :name="item.icon" size="34rpx" :color="item.color" />
        </view>
        <text class="stat-label">{{ item.label }}</text>
        <text class="stat-val">{{ item.value }}</text>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <view class="panel-title-wrap">
          <view class="panel-title-icon">
              <ev-icon name="radar" size="30rpx" color="#2563eb" />
          </view>
          <view>
            <text class="panel-title">车辆实时位置</text>
            <text class="panel-sub">与后台 Redis 定位数据同源</text>
          </view>
        </view>
        <view class="panel-link" @click="go('/pages/location/location')">
          <text>全屏查看</text>
          <ev-icon name="arrow-right" size="24rpx" color="#2563eb" />
        </view>
      </view>

      <view class="map-status-row">
        <view class="map-status-chip">
          <text class="map-status-dot"></text>
          <ev-icon name="location" size="24rpx" color="#2563eb" />
          <text class="map-status-text">{{ gpsLine }}</text>
        </view>
        <view class="map-status-side">
          <ev-icon name="bike" size="24rpx" color="#6b7c90" />
          <text>{{ vehicle && vehicle.licensePlate ? vehicle.licensePlate : '未绑定车辆' }}</text>
        </view>
      </view>

      <!-- #ifdef H5 -->
      <baidu-map-web
        class="map"
        :latitude="mapLat"
        :longitude="mapLng"
        :markers="markers"
        :scale="18"
      />
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <map
        class="map"
        :latitude="mapLat"
        :longitude="mapLng"
        :markers="markers"
        :scale="18"
        enable-zoom
      />
      <!-- #endif -->
      <text class="map-foot">位置与后台 Redis 实时数据一致；长时间无更新请检查车载终端或网络状态。</text>
    </view>

    <view class="section-head">
      <view>
        <text class="section-title">常用功能</text>
        <text class="section-tip">高频业务入口集中在这里</text>
      </view>
    </view>

    <view class="grid">
      <view v-for="item in actionCards" :key="item.title" class="grid-item" @click="go(item.url)">
        <view :class="['grid-ico', item.tone]">
          <ev-icon :name="item.icon" size="36rpx" :color="item.color" />
        </view>
        <text class="grid-t">{{ item.title }}</text>
        <text class="grid-d">{{ item.desc }}</text>
        <view class="grid-go">
          <text>进入</text>
          <ev-icon name="arrow-right" size="22rpx" color="#2563eb" />
        </view>
      </view>
    </view>
  </view>
</template>

<script>
// #ifdef H5
import BaiduMapWeb from '../../components/baidu-map-web/baidu-map-web.vue'
// #endif
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser } from '../../utils/auth.js'
import { fetchMyVehicle } from '../../utils/vehicle.js'
import {
  getAllLocations,
  getLocationByPlate,
  listViolationsByPlate,
  listEtcCards,
  getLicenseSummary
} from '../../api/index.js'

const DEF_LAT = 26.8860959
const DEF_LNG = 112.6827629
const LOCATION_POLL_MS = 5000

export default {
  components: {
    EvIcon,
    // #ifdef H5
    BaiduMapWeb
    // #endif
  },
  data() {
    return {
      vehicle: null,
      mapLat: DEF_LAT,
      mapLng: DEF_LNG,
      markers: [],
      pendingViolationCount: 0,
      rfidShort: '',
      gpsStatus: '未知',
      licenseRemain: null,
      licenseMax: 12,
      locationTimer: null
    }
  },
  computed: {
    hasPendingViolation() {
      return this.pendingViolationCount > 0
    },
    greeting() {
      const u = getUser()
      const name = (u && (u.realName || u.username)) || '同学'
      return '你好，' + name
    },
    heroSub() {
      return this.vehicle && this.vehicle.licensePlate
        ? '当前绑定车辆 ' + this.vehicle.licensePlate + '，可以直接查看实时位置与整改进度。'
        : '尚未绑定车辆，完成注册后即可使用全部车辆服务。'
    },
    bannerText() {
      return this.hasPendingViolation
        ? `您有 ${this.pendingViolationCount} 条待处理违规，请及时整改或发起申诉。`
        : '暂无待处理违规，请继续保持规范停车与合规通行。'
    },
    vehicleLine() {
      if (!this.vehicle) return '未绑定'
      const st =
        this.vehicle.status === 'approved'
          ? '正常'
          : this.vehicle.status === 'pending'
            ? '审核中'
            : this.vehicle.status === 'rejected'
              ? '未通过'
              : '已登记'
      return (this.vehicle.licensePlate || '-') + ' · ' + st
    },
    rfidLine() {
      return this.rfidShort || '未查询到'
    },
    gpsLine() {
      return this.gpsStatus
    },
    licenseLine() {
      if (this.licenseRemain == null) return '— / ' + this.licenseMax
      return this.licenseRemain + ' / ' + this.licenseMax
    },
    heroHighlights() {
      return [
        { icon: 'bike', label: '车辆档案', value: this.vehicleLine },
        { icon: 'location', label: '当前位置', value: this.gpsLine }
      ]
    },
    statCards() {
      return [
        {
          icon: 'bike',
          label: '我的车辆',
          value: this.vehicleLine,
          tone: 'tone-mint',
          color: '#2563eb'
        },
        {
          icon: 'card',
          label: '蓝牙标签',
          value: this.rfidLine,
          tone: 'tone-ink',
          color: '#155e75'
        },
        {
          icon: 'location',
          label: '定位状态',
          value: this.gpsLine,
          tone: 'tone-amber',
          color: '#b45309'
        },
        {
          icon: 'score',
          label: '剩余记分',
          value: this.licenseLine,
          tone: 'tone-blue',
          color: '#2563eb',
          url: '/pages/license-points/license-points'
        }
      ]
    },
    actionCards() {
      return [
        {
          icon: 'folder',
          title: '车辆档案',
          desc: '查看车辆状态、绑定信息与审核结果',
          url: '/pages/vehicle/vehicle',
          tone: 'g-mint',
          color: '#2563eb'
        },
        {
          icon: 'card',
          title: 'BLE 标签',
          desc: '管理蓝牙标签和设备绑定状态',
          url: '/pages/rfid/rfid',
          tone: 'g-ink',
          color: '#155e75'
        },
        {
          icon: 'radar',
          title: '车辆位置',
          desc: '查看当前点位与轨迹状态',
          url: '/pages/location/location',
          tone: 'g-amber',
          color: '#b45309'
        },
        {
          icon: 'warning',
          title: '违规整改',
          desc: '处理待整改记录与申诉流程',
          url: '/pages/violation/violation',
          tone: 'g-warn',
          color: '#c54c4c'
        },
        {
          icon: 'score',
          title: '记分公益',
          desc: '查看记分与公益加分情况',
          url: '/pages/license-points/license-points',
          tone: 'g-blue',
          color: '#2563eb'
        }
      ]
    }
  },
  onShow() {
    const u = getUser()
    if (!u || !u.userId) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.refresh()
    this.startLocationPolling()
  },
  onHide() {
    this.stopLocationPolling()
  },
  onUnload() {
    this.stopLocationPolling()
  },
  onPullDownRefresh() {
    this.refresh().finally(() => {
      uni.stopPullDownRefresh()
    })
  },
  methods: {
    go(url) {
      uni.navigateTo({ url })
    },
    buildLocationMarkers(rows, plate) {
      const seen = new Set()
      return (Array.isArray(rows) ? rows : [])
        .filter((item) => item && Number.isFinite(Number(item.lat)) && Number.isFinite(Number(item.lng)))
        .map((item, index) => {
          const key = String(item.licensePlate || item.deviceId || item.vehicleId || index)
          const markerId = key === String(plate || '') ? 1 : index + 100
          if (seen.has(markerId)) return null
          seen.add(markerId)
          return {
            id: markerId,
            latitude: Number(item.lat),
            longitude: Number(item.lng),
            width: key === String(plate || '') ? 32 : 24,
            height: key === String(plate || '') ? 32 : 24
          }
        })
        .filter(Boolean)
    },
    async loadLinkedLocationMarkers(plate) {
      try {
        const rows = await getAllLocations()
        this.markers = this.buildLocationMarkers(rows, plate)
      } catch (e) {
        this.markers = []
      }
    },
    upsertOwnLocationMarker(loc) {
      if (!loc || !Number.isFinite(Number(loc.lat)) || !Number.isFinite(Number(loc.lng))) return
      const linked = this.markers.filter((m) => m.id !== 1)
      this.markers = [
        {
          id: 1,
          latitude: Number(loc.lat),
          longitude: Number(loc.lng),
          width: 32,
          height: 32
        },
        ...linked
      ]
    },
    async refreshMapLocations() {
      const plate = this.vehicle && this.vehicle.licensePlate ? this.vehicle.licensePlate : ''
      await this.loadLinkedLocationMarkers(plate)
      if (!plate) return
      try {
        const loc = await getLocationByPlate(plate)
        if (loc && loc.lat && loc.lng) {
          this.mapLat = Number(loc.lat)
          this.mapLng = Number(loc.lng)
          this.gpsStatus =
            loc.status === 'online' ? '在线' : loc.status === 'stale' ? '信号弱' : '已上报'
          this.upsertOwnLocationMarker(loc)
        }
      } catch (e) {
        /* keep current map state */
      }
    },
    startLocationPolling() {
      this.stopLocationPolling()
      this.locationTimer = setInterval(() => {
        this.refreshMapLocations()
      }, LOCATION_POLL_MS)
    },
    stopLocationPolling() {
      if (!this.locationTimer) return
      clearInterval(this.locationTimer)
      this.locationTimer = null
    },
    async refresh() {
      const u = getUser()
      try {
        const lic = await getLicenseSummary(u.userId)
        if (lic) {
          this.licenseRemain = lic.remaining != null ? lic.remaining : null
          this.licenseMax = lic.maxPoints != null ? lic.maxPoints : 12
        }
      } catch (e) {
        this.licenseRemain = null
      }
      const v = await fetchMyVehicle(u.userId)
      this.vehicle = v
      if (!v || !v.licensePlate) {
        this.mapLat = DEF_LAT
        this.mapLng = DEF_LNG
        await this.loadLinkedLocationMarkers('')
        this.pendingViolationCount = 0
        this.rfidShort = ''
        this.gpsStatus = '无车辆'
        return
      }
      const plate = v.licensePlate
      await this.loadLinkedLocationMarkers(plate)
      try {
        const loc = await getLocationByPlate(plate)
        if (loc && loc.lat && loc.lng) {
          this.mapLat = loc.lat
          this.mapLng = loc.lng
          this.gpsStatus =
            loc.status === 'online' ? '在线' : loc.status === 'stale' ? '信号弱' : '已上报'
          this.upsertOwnLocationMarker(loc)
        } else {
          this.mapLat = DEF_LAT
          this.mapLng = DEF_LNG
          this.gpsStatus = '暂无位置'
        }
      } catch (e) {
        this.gpsStatus = '暂无位置'
        this.mapLat = DEF_LAT
        this.mapLng = DEF_LNG
      }

      try {
        const { list } = await listViolationsByPlate(plate, 80)
        const rows = list || []
        this.pendingViolationCount = rows.filter((x) => x.illegalState === 0 || x.illegalState === 3).length
      } catch (e) {
        this.pendingViolationCount = 0
      }

      try {
        const { list: cards } = await listEtcCards(plate, 40)
        const c =
          (cards || []).find((x) => x.vehicleId === v.vehicleId) || (cards || [])[0]
        if (c && c.etcNumber) {
          const n = c.etcNumber
          this.rfidShort = n.length > 8 ? n.slice(0, 4) + '…' + n.slice(-4) : n
        } else {
          this.rfidShort = ''
        }
      } catch (e) {
        this.rfidShort = ''
      }
    }
  }
}
</script>

<style scoped>
.page {
  position: relative;
  padding: 24rpx 24rpx 140rpx;
  box-sizing: border-box;
  min-height: 100vh;
  background:
    linear-gradient(180deg, #edf8f6 0%, #f4f8fb 32%, #f7f9fc 100%);
}

.hero {
  position: relative;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 255, 255, 0.94));
  border: 1rpx solid var(--ev-border);
  border-radius: 18rpx;
  padding: 30rpx 26rpx 24rpx;
  margin-bottom: 24rpx;
  box-shadow: var(--ev-shadow);
  overflow: hidden;
}

.hero::after {
  display: none;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  position: relative;
  z-index: 1;
}

.hero-main {
  flex: 1;
}

.hero-brand {
  display: inline-flex;
  align-items: center;
  gap: 14rpx;
  padding: 0;
  margin-bottom: 22rpx;
}

.hero-logo {
  width: 46rpx;
  height: 46rpx;
  padding: 0;
  border-radius: 10rpx;
  background: transparent;
}

.hero-brand-label {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #111827;
}

.hero-brand-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  color: var(--ev-muted);
}

.hello {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0;
}

.sub {
  display: block;
  margin-top: 14rpx;
  font-size: 25rpx;
  color: #667085;
  line-height: 1.58;
  max-width: 470rpx;
}

.hero-signal-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}

.hero-signal {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 16rpx;
  border-radius: 12rpx;
  background: #eef8f6;
  font-size: 22rpx;
  font-weight: 600;
  color: #344054;
}

.hero-badge {
  min-width: 142rpx;
  padding: 18rpx 20rpx;
  border-radius: 16rpx;
  text-align: center;
  position: relative;
  z-index: 1;
  border: 1rpx solid var(--ev-border);
}

.hero-badge.warn {
  background: #fff7ed;
}

.hero-badge.ok {
  background: var(--ev-primary-soft);
}

.hero-badge-icon {
  width: 58rpx;
  height: 58rpx;
  margin: 0 auto 12rpx;
  border-radius: 14rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-badge-num {
  display: block;
  font-size: 42rpx;
  font-weight: 800;
  color: var(--ev-primary);
}

.hero-badge.warn .hero-badge-num {
  color: var(--ev-warn-text);
}

.hero-badge-txt {
  margin-top: 8rpx;
  display: block;
  font-size: 21rpx;
  color: #64748b;
}

.hero-strip {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 28rpx;
}

.hero-strip-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: #f8fafc;
  border: 1rpx solid var(--ev-border);
}

.hero-strip-icon {
  width: 46rpx;
  height: 46rpx;
  border-radius: 12rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-strip-copy {
  flex: 1;
}

.hero-strip-k {
  display: block;
  font-size: 20rpx;
  color: #667085;
  letter-spacing: 0;
}

.hero-strip-v {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #111827;
  line-height: 1.45;
}

.alert {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 20rpx 22rpx;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  box-shadow: var(--ev-shadow);
}

.alert-ok {
  background: rgba(237, 249, 246, 0.95);
  border: 1rpx solid rgba(16, 185, 129, 0.18);
}

.alert-warn {
  background: rgba(255, 243, 241, 0.98);
  border: 1rpx solid rgba(197, 76, 76, 0.14);
}

.alert-mark {
  width: 72rpx;
  height: 72rpx;
  border-radius: 14rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-ok .alert-mark {
  background: rgba(16, 185, 129, 0.12);
}

.alert-warn .alert-mark {
  background: rgba(197, 76, 76, 0.1);
}

.alert-copy {
  flex: 1;
}

.alert-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #203243;
}

.alert-msg {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.58;
  color: #4f6476;
}

.stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.stat-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 22rpx 18rpx 20rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
  min-height: 170rpx;
}

.stat-ico {
  width: 68rpx;
  height: 68rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18rpx;
}

.tone-mint {
  background: #eff6ff;
}

.tone-ink {
  background: #f3f4f6;
}

.tone-amber {
  background: rgba(245, 158, 11, 0.14);
}

.tone-blue {
  background: rgba(59, 130, 246, 0.12);
}

.stat-label {
  font-size: 22rpx;
  color: #7b8b9b;
  display: block;
  letter-spacing: 1rpx;
}

.stat-val {
  font-size: 24rpx;
  color: #0f172a;
  margin-top: 12rpx;
  display: block;
  line-height: 1.5;
  word-break: break-all;
  font-weight: 600;
}

.panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx;
  margin-bottom: 32rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18rpx;
  gap: 16rpx;
}

.panel-title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.panel-title-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 12rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.panel-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.panel-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7b8b9b;
}

.panel-link {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: var(--ev-primary);
  font-weight: 600;
}

.map-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18rpx;
  gap: 12rpx;
}

.map-status-chip {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 16rpx;
  border-radius: 10rpx;
  background: var(--ev-primary-soft);
}

.map-status-dot {
  display: inline-block;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--ev-primary);
  box-shadow: none;
}

.map-status-text {
  font-size: 22rpx;
  color: var(--ev-primary-dark);
  font-weight: 600;
}

.map-status-side {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
  color: #6b7c90;
}

.map {
  width: 100%;
  height: 360rpx;
  border-radius: 14rpx;
  overflow: hidden;
}

.map-foot {
  display: block;
  font-size: 22rpx;
  color: #7d8e9f;
  margin-top: 18rpx;
  line-height: 1.58;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 18rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.section-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7b8b9b;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.grid-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 22rpx 22rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
  min-height: 224rpx;
}

.grid-ico {
  width: 78rpx;
  height: 78rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18rpx;
}

.g-mint {
  background: #eff6ff;
}

.g-ink {
  background: #f3f4f6;
}

.g-amber {
  background: rgba(245, 158, 11, 0.14);
}

.g-warn {
  background: rgba(229, 111, 74, 0.12);
}

.g-blue {
  background: rgba(59, 130, 246, 0.12);
}

.grid-t {
  font-size: 28rpx;
  color: #223042;
  font-weight: 700;
}

.grid-d {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #788a9a;
  line-height: 1.6;
  flex: 1;
}

.grid-go {
  margin-top: 16rpx;
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
  color: #2563eb;
  font-weight: 600;
}

@media screen and (max-width: 360px) {
  .hero-strip,
  .stats,
  .grid {
    grid-template-columns: 1fr;
  }

  .hero-top,
  .panel-head,
  .map-status-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-badge {
    align-self: flex-start;
  }
}
</style>

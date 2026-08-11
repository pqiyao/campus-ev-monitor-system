<template>
  <view class="page">
    <view class="hero">
      <view class="hero-main">
        <view class="hero-chip">
          <ev-icon name="location" size="24rpx" color="#dcfff8" />
          <text>LIVE LOCATION</text>
        </view>
        <text class="hero-title">车辆位置与围栏</text>
        <text class="hero-sub">查看车辆实时位置、电子围栏和近次轨迹示意，支持防盗提醒开关。</text>
      </view>
      <view class="anti-box">
        <view class="anti-copy">
          <text class="anti-label">防盗提醒</text>
          <text class="anti-state">{{ antiTheft ? '已开启' : '已关闭' }}</text>
        </view>
        <switch :checked="antiTheft" color="#0d9488" @change="onAnti" />
      </view>
    </view>

    <view v-if="plate" class="status-grid">
      <view class="status-card">
        <view class="status-icon mint">
          <ev-icon name="bike" size="28rpx" color="#0b7068" />
        </view>
        <text class="status-k">当前车辆</text>
        <text class="status-v">{{ plate }}</text>
      </view>
      <view class="status-card">
        <view class="status-icon blue">
          <ev-icon name="shield" size="28rpx" color="#2563eb" />
        </view>
        <text class="status-k">电子围栏</text>
        <text class="status-v">{{ fencePolygons.length }} 个</text>
      </view>
      <view class="status-card">
        <view class="status-icon amber">
          <ev-icon name="radar" size="28rpx" color="#b45309" />
        </view>
        <text class="status-k">轨迹示意</text>
        <text class="status-v">{{ polyline.length ? '已生成' : '暂无' }}</text>
      </view>
    </view>

    <view v-if="!plate" class="empty-card">
      <view class="empty-icon">
        <ev-icon name="location" size="54rpx" color="#0b7068" />
      </view>
      <text class="empty-title">暂未绑定车辆</text>
      <text class="empty-desc">完成车辆登记后，位置模块才会同步车辆坐标与电子围栏信息。</text>
      <button class="ghost-btn" @click="goVehicle">
        <view class="btn-inner plain">
          <text>查看车辆档案</text>
          <ev-icon name="arrow-right" size="24rpx" color="#0b7068" />
        </view>
      </button>
    </view>

    <view v-else class="panel">
      <view class="panel-head">
        <view class="panel-title-wrap">
          <view class="panel-icon">
            <ev-icon name="radar" size="28rpx" color="#0b7068" />
          </view>
          <view>
            <text class="panel-title">实时地图</text>
            <text class="panel-sub">主标记代表车辆最新上报位置，轨迹为演示路径预览。</text>
          </view>
        </view>
        <button class="primary-btn" type="primary" @click="refresh">
          <view class="btn-inner">
            <ev-icon name="radar" size="22rpx" color="#ffffff" />
            <text>刷新位置</text>
          </view>
        </button>
      </view>

      <!-- #ifdef H5 -->
      <baidu-map-web
        class="map"
        :latitude="lat"
        :longitude="lng"
        :markers="markers"
        :polylines="polyline"
        :polygons="fencePolygons"
        :scale="18"
      />
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <map
        class="map"
        :latitude="lat"
        :longitude="lng"
        :markers="markers"
        :polyline="polyline"
        :polygons="fencePolygons"
        :scale="18"
      />
      <!-- #endif -->

      <view class="legend">
        <view class="legend-row">
          <view class="legend-mark dot-green"></view>
          <text class="legend-text">绿色轨迹线表示最近一次轨迹示意。</text>
        </view>
        <view class="legend-row">
          <view class="legend-mark dot-red"></view>
          <text class="legend-text">红色半透明区域表示已启用电子围栏。</text>
        </view>
        <view class="legend-row">
          <view class="legend-mark icon-mark">
            <ev-icon name="location" size="20rpx" color="#0b7068" />
          </view>
          <text class="legend-text">定位数据来自服务端实时位置接口与围栏接口。</text>
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
import { getAllLocations, getLocationByPlate, getGeofenceEnabledList } from '../../api/index.js'

const KEY_ANTI = 'ev_anti_theft'
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
      lat: DEF_LAT,
      lng: DEF_LNG,
      markers: [],
      polyline: [],
      fencePolygons: [],
      antiTheft: true,
      plate: '',
      locationTimer: null
    }
  },
  onShow() {
    const v = uni.getStorageSync(KEY_ANTI)
    this.antiTheft = v !== '0'
    this.init()
    this.startLocationPolling()
  },
  onHide() {
    this.stopLocationPolling()
  },
  onUnload() {
    this.stopLocationPolling()
  },
  methods: {
    goVehicle() {
      uni.navigateTo({ url: '/pages/vehicle/vehicle' })
    },
    onAnti(e) {
      this.antiTheft = e.detail.value
      uni.setStorageSync(KEY_ANTI, this.antiTheft ? '1' : '0')
      uni.showToast({ title: this.antiTheft ? '防盗提醒已开启' : '防盗提醒已关闭', icon: 'none' })
    },
    async init() {
      const u = getUser()
      if (!u || !u.userId) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      const veh = await fetchMyVehicle(u.userId)
      if (!veh) {
        this.plate = ''
        this.markers = []
        this.polyline = []
        this.fencePolygons = []
        return
      }
      this.plate = veh.licensePlate
      await this.loadFences()
      await this.refresh()
      this.buildDemoTrack()
    },
    buildFencePolygonsFromList(fences) {
      const polys = []
      if (!Array.isArray(fences)) return polys
      fences.forEach((f) => {
        if (f.status !== 1 || !f.coordinates) return
        try {
          const raw = JSON.parse(f.coordinates)
          const points = raw
            .map((p) => ({
              latitude: Number(p.lat),
              longitude: Number(p.lng)
            }))
            .filter((p) => Number.isFinite(p.latitude) && Number.isFinite(p.longitude))
          if (points.length < 3) return
          const stroke = (f.fenceColor || '#dc2626').replace(/\s/g, '').slice(0, 7)
          const fillColor = stroke.startsWith('#') && stroke.length === 7 ? stroke + '44' : '#dc262644'
          polys.push({
            points,
            strokeWidth: 2,
            strokeColor: stroke,
            fillColor
          })
        } catch (e) {
          /* ignore */
        }
      })
      return polys
    },
    async loadFences() {
      try {
        const list = await getGeofenceEnabledList()
        this.fencePolygons = this.buildFencePolygonsFromList(Array.isArray(list) ? list : [])
      } catch (e) {
        this.fencePolygons = []
      }
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
    async loadLinkedLocationMarkers() {
      try {
        const rows = await getAllLocations()
        this.markers = this.buildLocationMarkers(rows, this.plate)
      } catch (e) {
        this.markers = []
      }
    },
    startLocationPolling() {
      this.stopLocationPolling()
      this.locationTimer = setInterval(() => {
        this.refresh()
      }, LOCATION_POLL_MS)
    },
    stopLocationPolling() {
      if (!this.locationTimer) return
      clearInterval(this.locationTimer)
      this.locationTimer = null
    },
    buildDemoTrack() {
      if (!this.lat || !this.lng) return
      const d = 0.004
      const pts = [
        { lat: this.lat + d, lng: this.lng },
        { lat: this.lat + d * 0.5, lng: this.lng + d },
        { lat: this.lat, lng: this.lng + d * 0.6 }
      ]
      this.polyline = [
        {
          points: pts.map((p) => ({ latitude: p.lat, longitude: p.lng })),
          color: '#0d9488aa',
          width: 5
        }
      ]
      const base = this.markers.filter((m) => m.id === 1)
      const linked = this.markers.filter((m) => m.id !== 1 && m.id !== 9)
      const warn = [
        {
          id: 9,
          latitude: pts[1].lat,
          longitude: pts[1].lng,
          width: 24,
          height: 24
        }
      ]
      this.markers = [...base, ...linked, ...warn]
    },
    async refresh() {
      if (!this.plate) return
      await this.loadFences()
      await this.loadLinkedLocationMarkers()
      try {
        const loc = await getLocationByPlate(this.plate)
        if (loc && loc.lat && loc.lng) {
          this.lat = loc.lat
          this.lng = loc.lng
          const linked = this.markers.filter((m) => m.id !== 1 && m.id !== 9)
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
          this.buildDemoTrack()
        }
      } catch (e) {
        uni.showToast({ title: '暂无实时位置', icon: 'none' })
      }
    }
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx 24rpx 56rpx;
  box-sizing: border-box;
}

.hero {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent 42%),
    linear-gradient(125deg, #0f8f83, #0b7068);
  border-radius: 32rpx;
  padding: 32rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  box-shadow: 0 18rpx 52rpx rgba(11, 112, 104, 0.2);
  margin-bottom: 22rpx;
}

.hero-main {
  flex: 1;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 20rpx;
  letter-spacing: 3rpx;
  color: rgba(255, 255, 255, 0.94);
  font-weight: 700;
}

.hero-title {
  display: block;
  margin-top: 18rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
}

.hero-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.58;
}

.anti-box {
  min-width: 190rpx;
  padding: 18rpx 18rpx 14rpx;
  border-radius: 24rpx;
  background: rgba(246, 255, 253, 0.94);
}

.anti-copy {
  margin-bottom: 12rpx;
}

.anti-label {
  display: block;
  font-size: 22rpx;
  color: #64748b;
}

.anti-state {
  display: block;
  margin-top: 8rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #0b7068;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 22rpx;
}

.status-card,
.empty-card,
.panel {
  background: rgba(255, 255, 255, 0.95);
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}

.status-card {
  border-radius: 24rpx;
  padding: 22rpx 18rpx;
}

.status-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon.mint {
  background: rgba(65, 201, 182, 0.14);
}

.status-icon.blue {
  background: rgba(59, 130, 246, 0.12);
}

.status-icon.amber {
  background: rgba(245, 158, 11, 0.14);
}

.status-k {
  display: block;
  margin-top: 14rpx;
  font-size: 22rpx;
  color: #7b8b9b;
}

.status-v {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.45;
  word-break: break-all;
}

.empty-card {
  border-radius: 30rpx;
  padding: 72rpx 36rpx;
  text-align: center;
}

.empty-icon {
  width: 110rpx;
  height: 110rpx;
  margin: 0 auto 24rpx;
  border-radius: 30rpx;
  background: rgba(65, 201, 182, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #0f172a;
}

.empty-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #64748b;
  line-height: 1.58;
}

.panel {
  border-radius: 30rpx;
  padding: 26rpx;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.panel-title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.panel-icon {
  width: 50rpx;
  height: 50rpx;
  border-radius: 16rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.panel-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.panel-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7b8b9b;
  line-height: 1.5;
}

.map {
  width: 100%;
  height: 560rpx;
  border-radius: 22rpx;
  border: 1rpx solid #e2e8f0;
}

.legend {
  margin-top: 18rpx;
  padding: 18rpx 0 0;
  border-top: 1rpx solid #edf2f7;
}

.legend-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-top: 14rpx;
}

.legend-row:first-child {
  margin-top: 0;
}

.legend-mark {
  width: 20rpx;
  height: 20rpx;
  margin-top: 8rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-green {
  background: #0d9488;
}

.dot-red {
  background: rgba(220, 38, 38, 0.72);
}

.icon-mark {
  width: 32rpx;
  height: 32rpx;
  margin-top: 0;
  border-radius: 10rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.legend-text {
  font-size: 22rpx;
  color: #64748b;
  line-height: 1.58;
}

.primary-btn,
.ghost-btn {
  height: 78rpx;
  line-height: normal;
  border-radius: 18rpx !important;
}

.primary-btn {
  margin: 0;
  background: linear-gradient(90deg, #18a897, #0f8f83, #0b7068) !important;
  border: none !important;
}

.ghost-btn {
  margin-top: 34rpx;
  background: rgba(239, 253, 250, 0.96) !important;
  border: 1rpx solid rgba(15, 143, 131, 0.16) !important;
}

.btn-inner {
  height: 78rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: #ffffff;
}

.btn-inner.plain {
  color: #0b7068;
}

@media screen and (max-width: 520px) {
  .hero,
  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }

  .primary-btn {
    width: 100%;
  }
}
</style>

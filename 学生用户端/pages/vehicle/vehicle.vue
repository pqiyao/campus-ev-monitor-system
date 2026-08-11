<template>
  <view class="page">
    <view class="page-orb orb-a"></view>
    <view class="page-orb orb-b"></view>

    <view v-if="!vehicle" class="empty-card">
      <view class="empty-icon">
        <ev-icon name="bike" size="56rpx" color="#0b7068" />
      </view>
      <text class="empty-title">还没有绑定车辆</text>
      <text class="empty-desc">先在注册流程里登记车辆，后续才能同步 BLE 蓝牙标签、定位和违规整治信息。</text>
      <button class="empty-btn" @click="goRegister">
        <view class="btn-inner light">
          <text>去登记车辆</text>
          <ev-icon name="arrow-right" size="24rpx" color="#0b7068" />
        </view>
      </button>
    </view>

    <view v-else class="wrap">
      <view class="hero">
        <view class="hero-main">
          <view class="hero-chip">
            <ev-icon name="bike" size="24rpx" color="#dcfff8" />
            <text>VEHICLE PROFILE</text>
          </view>
          <text class="plate">{{ vehicle.licensePlate }}</text>
          <text class="hero-sub">{{ vehicle.brand || '未填写品牌' }} / {{ vehicle.color || '未填写颜色' }}</text>
        </view>
        <view class="status-box">
          <view class="status-icon">
            <ev-icon :name="statusIcon" size="28rpx" :color="statusColor" />
          </view>
          <text :class="['status-text', statusClass]">{{ statusText }}</text>
        </view>
      </view>

      <view class="overview-grid">
        <view class="overview-item">
          <view class="overview-icon mint">
            <ev-icon name="card" size="28rpx" color="#0b7068" />
          </view>
          <text class="overview-k">BLE 蓝牙标签</text>
          <text class="overview-v">{{ rfidHint }}</text>
        </view>
        <view class="overview-item">
          <view class="overview-icon amber">
            <ev-icon name="location" size="28rpx" color="#b45309" />
          </view>
          <text class="overview-k">定位状态</text>
          <text class="overview-v">{{ gpsHint }}</text>
        </view>
        <view class="overview-item">
          <view class="overview-icon blue">
            <ev-icon name="battery" size="28rpx" color="#2563eb" />
          </view>
          <text class="overview-k">电池类型</text>
          <text class="overview-v">{{ vehicle.batteryType || '未填写' }}</text>
        </view>
      </view>

      <view class="card">
        <view class="card-head">
          <view class="card-title-wrap">
            <view class="card-title-icon">
              <ev-icon name="folder" size="28rpx" color="#0b7068" />
            </view>
            <view>
              <text class="card-title">车辆详细档案</text>
              <text class="card-sub">以下信息来自学生端与管理端的车辆登记记录</text>
            </view>
          </view>
        </view>

        <view class="row">
          <view class="row-main">
            <view class="row-icon">
              <ev-icon name="spark" size="24rpx" color="#0b7068" />
            </view>
            <text class="k">品牌 / 颜色</text>
          </view>
          <text class="v">{{ vehicle.brand || '未填写' }} / {{ vehicle.color || '未填写' }}</text>
        </view>

        <view class="row">
          <view class="row-main">
            <view class="row-icon">
              <ev-icon name="frame" size="24rpx" color="#0b7068" />
            </view>
            <text class="k">车架号</text>
          </view>
          <text class="v mono">{{ vehicle.frameNum || '未填写' }}</text>
        </view>

        <view class="row">
          <view class="row-main">
            <view class="row-icon">
              <ev-icon name="battery" size="24rpx" color="#0b7068" />
            </view>
            <text class="k">电池 / 类型</text>
          </view>
          <text class="v">{{ vehicle.batteryType || '未填写' }} / {{ vehicle.vehicleType || '未填写' }}</text>
        </view>

        <view class="row">
          <view class="row-main">
            <view class="row-icon">
              <ev-icon name="shield" size="24rpx" color="#0b7068" />
            </view>
            <text class="k">车辆状态</text>
          </view>
          <text class="v">{{ statusText }}</text>
        </view>

        <view v-if="photo" class="photo-card">
          <view class="photo-head">
            <view class="photo-title-wrap">
              <view class="row-icon">
                <ev-icon name="camera" size="24rpx" color="#0b7068" />
              </view>
              <text class="photo-title">车辆照片</text>
            </view>
            <text class="photo-tip">用于核验登记信息</text>
          </view>
          <image :src="photo" class="photo" mode="aspectFill" />
        </view>

        <button class="danger" @click="onCancel">
          <view class="btn-inner">
            <ev-icon name="warning" size="24rpx" color="#b91c1c" />
            <text>注销车辆绑定</text>
          </view>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser } from '../../utils/auth.js'
import { fetchMyVehicle, vehicleStatusText } from '../../utils/vehicle.js'
import { deleteVehicle, getLocationByPlate, listEtcCards } from '../../api/index.js'
import { fileUrl } from '../../utils/config.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      vehicle: null,
      rfidHint: '加载中',
      gpsHint: '未查询'
    }
  },
  computed: {
    statusText() {
      return this.vehicle ? vehicleStatusText(this.vehicle.status) : ''
    },
    statusClass() {
      const s = this.vehicle && this.vehicle.status
      if (s === 'approved') return 'ok'
      if (s === 'pending') return 'pending'
      if (s === 'rejected') return 'bad'
      return 'idle'
    },
    statusIcon() {
      if (this.statusClass === 'ok') return 'check'
      if (this.statusClass === 'pending') return 'warning'
      if (this.statusClass === 'bad') return 'warning'
      return 'shield'
    },
    statusColor() {
      if (this.statusClass === 'ok') return '#047857'
      if (this.statusClass === 'pending') return '#b45309'
      if (this.statusClass === 'bad') return '#b91c1c'
      return '#0b7068'
    },
    photo() {
      if (!this.vehicle || !this.vehicle.vehiclePhoto) return ''
      return fileUrl(this.vehicle.vehiclePhoto)
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    goRegister() {
      uni.navigateTo({ url: '/pages/register/register' })
    },
    async load() {
      const u = getUser()
      if (!u || !u.userId) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      const v = await fetchMyVehicle(u.userId)
      this.vehicle = v
      if (!v) return
      try {
        const loc = await getLocationByPlate(v.licensePlate)
        this.gpsHint =
          loc && loc.lat
            ? loc.status === 'online'
              ? '在线'
              : loc.status === 'stale'
                ? '信号弱'
                : '已上报'
            : '无信号'
      } catch (e) {
        this.gpsHint = '未知'
      }
      try {
        const { list } = await listEtcCards(v.licensePlate, 30)
        const c = (list || []).find((x) => x.vehicleId === v.vehicleId)
        this.rfidHint = c && c.etcNumber ? c.etcNumber : '未绑定蓝牙标签'
      } catch (e) {
        this.rfidHint = '未查询到'
      }
    },
    onCancel() {
      if (!this.vehicle) return
      uni.showModal({
        title: '注销车辆',
        content: '将删除车辆记录（软删除），确认继续吗？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await deleteVehicle(this.vehicle.vehicleId)
            uni.showToast({ title: '已提交注销', icon: 'success' })
            this.vehicle = null
          } catch (e) {
            uni.showToast({ title: e.message || '操作失败', icon: 'none' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.page {
  position: relative;
  padding: 28rpx 24rpx 96rpx;
  box-sizing: border-box;
  min-height: 100vh;
  overflow: hidden;
}

.page-orb {
  display: none;
}

.orb-a {
  width: 220rpx;
  height: 220rpx;
  top: -90rpx;
  right: -40rpx;
  background: rgba(65, 201, 182, 0.18);
}

.orb-b {
  width: 180rpx;
  height: 180rpx;
  left: -70rpx;
  top: 420rpx;
  background: rgba(37, 99, 235, 0.08);
}

.empty-card,
.card,
.overview-item {
  position: relative;
  z-index: 1;
}

.empty-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28rpx;
  padding: 82rpx 40rpx;
  text-align: center;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
}

.empty-icon {
  width: 112rpx;
  height: 112rpx;
  margin: 0 auto 26rpx;
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
  font-size: 26rpx;
  color: #64748b;
  margin-top: 16rpx;
  line-height: 1.58;
}

.empty-btn {
  margin-top: 36rpx;
  background: rgba(239, 253, 250, 0.96);
  border-radius: 18rpx;
  border: 1rpx solid rgba(15, 143, 131, 0.16);
}

.wrap {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.hero {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent 42%),
    linear-gradient(125deg, #0f8f83, #0b7068);
  border-radius: 30rpx;
  padding: 34rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18rpx;
  box-shadow: 0 18rpx 52rpx rgba(11, 112, 104, 0.2);
}

.hero-main {
  flex: 1;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 16rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 20rpx;
  letter-spacing: 3rpx;
  color: rgba(255, 255, 255, 0.92);
  font-weight: 700;
}

.plate {
  display: block;
  margin-top: 20rpx;
  font-size: 44rpx;
  font-weight: 800;
  color: #fff;
  letter-spacing: 4rpx;
}

.hero-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.88);
}

.status-box {
  min-width: 146rpx;
  padding: 18rpx 18rpx 14rpx;
  border-radius: 20rpx;
  text-align: center;
  background: rgba(246, 255, 253, 0.94);
}

.status-icon {
  width: 54rpx;
  height: 54rpx;
  margin: 0 auto 12rpx;
  border-radius: 18rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-text {
  display: block;
  font-size: 24rpx;
  font-weight: 700;
}

.status-text.ok {
  color: #047857;
}

.status-text.pending {
  color: #b45309;
}

.status-text.bad {
  color: #b91c1c;
}

.status-text.idle {
  color: #0b7068;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
}

.overview-item {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20rpx;
  padding: 22rpx 18rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
}

.overview-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overview-icon.mint {
  background: rgba(65, 201, 182, 0.14);
}

.overview-icon.amber {
  background: rgba(245, 158, 11, 0.14);
}

.overview-icon.blue {
  background: rgba(59, 130, 246, 0.12);
}

.overview-k {
  display: block;
  font-size: 22rpx;
  color: #7b8b9b;
  margin-top: 14rpx;
}

.overview-v {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  color: #122234;
  font-weight: 700;
  line-height: 1.45;
  word-break: break-all;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24rpx;
  padding: 26rpx 24rpx 32rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
}

.card-head {
  margin-bottom: 10rpx;
}

.card-title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.card-title-icon,
.row-icon {
  width: 50rpx;
  height: 50rpx;
  border-radius: 16rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #132334;
}

.card-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7b8b9b;
  line-height: 1.5;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
  gap: 16rpx;
}

.row-main {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
  max-width: 46%;
}

.k {
  color: #64748b;
  font-size: 26rpx;
}

.v {
  color: #0f172a;
  text-align: right;
  flex: 1;
  word-break: break-all;
  font-size: 26rpx;
}

.mono {
  font-family: ui-monospace, monospace;
  font-size: 24rpx;
}

.photo-card {
  margin-top: 24rpx;
  padding: 18rpx;
  border-radius: 22rpx;
  background: linear-gradient(180deg, #fbfefe, #f5faf9);
  border: 1rpx solid rgba(120, 150, 157, 0.14);
}

.photo-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.photo-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
}

.photo-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #132334;
}

.photo-tip {
  font-size: 22rpx;
  color: #7b8b9b;
}

.photo {
  width: 100%;
  height: 320rpx;
  border-radius: 18rpx;
  background: #f1f5f9;
}

.danger {
  margin-top: 36rpx;
  background: #fff2f2;
  color: #b91c1c;
  border-radius: 18rpx;
  font-size: 28rpx;
  border: 1rpx solid #fecaca;
}

.btn-inner {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.btn-inner.light {
  color: #0b7068;
}

@media screen and (max-width: 360px) {
  .hero,
  .photo-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .row {
    flex-direction: column;
  }

  .row-main {
    max-width: none;
  }
}
</style>

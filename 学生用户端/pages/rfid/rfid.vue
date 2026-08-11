<template>
  <view class="page">
    <view v-if="!card" class="empty-card">
      <view class="empty-ico">
        <ev-icon name="card" size="54rpx" color="#0f8f83" />
      </view>
      <text class="empty-title">未绑定 BLE 蓝牙标签</text>
      <text class="empty-desc">请在管理端完成蓝牙标签绑定到您的车辆后，下拉刷新本页。</text>
      <button class="refresh" @click="load">
        <view class="btn-inner">
          <ev-icon name="radar" size="24rpx" color="#ffffff" />
          <text>重新加载</text>
        </view>
      </button>
    </view>
    <view v-else class="wrap">
      <view class="hero">
        <view class="hero-top">
          <view>
            <view class="hero-chip">
              <ev-icon name="card" size="24rpx" color="#dcfff8" />
              <text>BLE PASS</text>
            </view>
            <text class="hero-label">校园蓝牙通行标签</text>
          </view>
          <text :class="['hero-state', statusClass]">{{ statusLabel }}</text>
        </view>
        <text class="hero-num">{{ card.etcNumber }}</text>
        <text class="hero-sub">绑定车辆 {{ vehiclePlate || '已绑定' }}</text>
      </view>
      <view class="card">
        <view class="row">
          <view class="row-key">
            <view class="row-icon blue"><ev-icon name="shield" size="24rpx" color="#2563eb" /></view>
            <text class="k">有效期</text>
          </view>
          <text class="v">{{ card.expireDate || '—' }}</text>
        </view>
        <view class="row">
          <view class="row-key">
            <view class="row-icon mint"><ev-icon name="bike" size="24rpx" color="#0f8f83" /></view>
            <text class="k">绑定车辆</text>
          </view>
          <text class="v">{{ vehiclePlate || '已绑定' }}</text>
        </view>
        <view class="hint-box">
          <view class="hint-title-row">
            <ev-icon name="radar" size="24rpx" color="#b45309" />
            <text class="hint-title">通行记录</text>
          </view>
          <text class="hint-body">蓝牙识别明细需对接网关采集系统；当前仅展示标签状态，如有疑问请联系管理中心。</text>
        </view>
        <view class="btns">
          <button class="b1" @click="setLost">
            <view class="btn-inner warn">
              <ev-icon name="warning" size="24rpx" color="#b45309" />
              <text>挂失</text>
            </view>
          </button>
          <button class="b2" @click="setNormal">
            <view class="btn-inner ok">
              <ev-icon name="check" size="24rpx" color="#047857" />
              <text>解挂</text>
            </view>
          </button>
        </view>
        <button class="linkbtn" @click="reissue">
          <view class="btn-inner link">
            <text>补办说明</text>
            <ev-icon name="arrow-right" size="22rpx" color="#0f8f83" />
          </view>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser } from '../../utils/auth.js'
import { fetchMyVehicle } from '../../utils/vehicle.js'
import { listEtcCards, setEtcStatus } from '../../api/index.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      card: null,
      vehiclePlate: ''
    }
  },
  computed: {
    statusLabel() {
      if (!this.card) return ''
      const m = { normal: '正常', lost: '已挂失', disabled: '停用' }
      return m[this.card.status] || this.card.status || '未知'
    },
    statusClass() {
      const s = this.card && this.card.status
      if (s === 'normal') return 'st-ok'
      if (s === 'lost') return 'st-lost'
      return ''
    }
  },
  onShow() {
    this.load()
  },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh())
  },
  methods: {
    async load() {
      const u = getUser()
      if (!u || !u.userId) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      const v = await fetchMyVehicle(u.userId)
      if (!v || !v.licensePlate) {
        this.card = null
        this.vehiclePlate = ''
        return
      }
      this.vehiclePlate = v.licensePlate
      try {
        const { list } = await listEtcCards(v.licensePlate, 40)
        this.card = (list || []).find((x) => x.vehicleId === v.vehicleId) || null
      } catch (e) {
        this.card = null
      }
    },
    async setLost() {
      if (!this.card) return
      try {
        await setEtcStatus(this.card.etcId, 'lost')
        uni.showToast({ title: '已挂失', icon: 'success' })
        this.load()
      } catch (e) {
        uni.showToast({ title: e.message || '失败', icon: 'none' })
      }
    },
    async setNormal() {
      if (!this.card) return
      try {
        await setEtcStatus(this.card.etcId, 'normal')
        uni.showToast({ title: '已解挂', icon: 'success' })
        this.load()
      } catch (e) {
        uni.showToast({ title: e.message || '失败', icon: 'none' })
      }
    },
    reissue() {
      uni.showModal({
        title: '补办说明',
        content: '请携带有效证件前往校园卡服务中心办理实体卡补办，或通过管理端登记补卡申请。',
        showCancel: false
      })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx 24rpx 64rpx;
  box-sizing: border-box;
}
.empty-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30rpx;
  padding: 78rpx 38rpx;
  text-align: center;
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}
.empty-ico {
  width: 112rpx;
  height: 112rpx;
  margin: 0 auto 24rpx;
  border-radius: 30rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}
.empty-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #64748b;
  line-height: 1.5;
}
.refresh {
  margin-top: 36rpx;
  height: 86rpx;
  line-height: normal;
  background: linear-gradient(90deg, #18a897, #0f8f83, #0b7068);
  color: #fff;
  border-radius: 18rpx;
  width: 320rpx;
  font-size: 28rpx;
  border: none;
}
.wrap {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.hero {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.1), transparent 42%),
    linear-gradient(125deg, #155e75, #0f8f83 58%, #0b7068);
  border-radius: 32rpx;
  padding: 34rpx 30rpx;
  box-shadow: var(--ev-shadow-strong);
}
.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18rpx;
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
.hero-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  display: block;
  margin-top: 18rpx;
}
.hero-num {
  font-size: 42rpx;
  font-weight: 800;
  color: #fff;
  margin-top: 26rpx;
  display: block;
  letter-spacing: 2rpx;
  word-break: break-all;
}
.hero-sub {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.84);
}
.hero-state {
  display: inline-block;
  margin-top: 2rpx;
  font-size: 22rpx;
  padding: 9rpx 20rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  flex-shrink: 0;
}
.hero-state.st-ok {
  background: #ecfdf5;
  color: #047857;
}
.hero-state.st-lost {
  background: #fef3c7;
  color: #b45309;
}
.card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28rpx;
  padding: 14rpx 26rpx 30rpx;
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
  font-size: 26rpx;
}
.row-key {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}
.row-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.row-icon.blue {
  background: rgba(37, 99, 235, 0.1);
}
.row-icon.mint {
  background: var(--ev-primary-soft);
}
.k {
  color: #64748b;
}
.v {
  color: #0f172a;
  max-width: 420rpx;
  text-align: right;
}
.hint-box {
  margin-top: 28rpx;
  padding: 22rpx;
  background: #fffbeb;
  border-radius: 18rpx;
  border: 1rpx solid #e2e8f0;
}
.hint-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.hint-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #334155;
  display: block;
}
.hint-body {
  font-size: 24rpx;
  color: #64748b;
  line-height: 1.5;
  margin-top: 10rpx;
  display: block;
}
.btns {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}
.b1,
.b2 {
  flex: 1;
  font-size: 28rpx;
  border-radius: 18rpx;
  height: 82rpx;
  line-height: normal;
}
.b1 {
  background: #fef3c7;
  color: #b45309;
}
.b2 {
  background: #d1fae5;
  color: #047857;
}
.linkbtn {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #0d9488;
  background: transparent;
  height: 76rpx;
  line-height: normal;
}
.btn-inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  font-weight: 700;
}
.btn-inner.warn {
  color: #b45309;
}
.btn-inner.ok {
  color: #047857;
}
.btn-inner.link {
  color: #0f8f83;
}
</style>

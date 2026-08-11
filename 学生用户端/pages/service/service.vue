<template>
  <view class="page">
    <view class="head-card">
      <view class="head-top">
        <view class="head-copy">
          <view class="head-chip">
            <ev-icon name="bell" size="26rpx" color="#2563eb" />
            <text>消息与服务</text>
          </view>
          <text class="head-title">服务中心</text>
          <text class="head-desc">系统通知、申诉进度与个人设置，统一收在这里处理。</text>
        </view>

        <view class="head-badge">
          <view class="head-badge-icon">
            <ev-icon name="bell" size="28rpx" color="#2563eb" />
          </view>
          <text class="head-badge-num">{{ unreadNotificationCount }}</text>
          <text class="head-badge-txt">未读通知</text>
        </view>
      </view>

      <view class="hero-metrics">
        <view v-for="item in metrics" :key="item.label" class="hero-metric">
          <view class="metric-ico">
            <ev-icon :name="item.icon" size="28rpx" color="#2563eb" />
          </view>
          <text class="hero-metric-k">{{ item.label }}</text>
          <text class="hero-metric-v">{{ item.value }}</text>
        </view>
      </view>
    </view>

    <view class="quick-row">
      <view v-for="item in quickLinks" :key="item.title" class="quick-item" @click="item.action">
        <view :class="['quick-ico', item.tone]">
          <ev-icon :name="item.icon" size="34rpx" :color="item.color" />
        </view>
        <view class="quick-copy">
          <text class="quick-txt">{{ item.title }}</text>
          <text class="quick-desc">{{ item.desc }}</text>
        </view>
        <view class="quick-go">
          <ev-icon name="arrow-right" size="22rpx" color="#94a3b8" />
        </view>
      </view>
    </view>

    <view class="block">
      <view class="block-h">
        <view class="block-title-wrap">
          <view class="block-ico">
            <ev-icon name="bell" size="28rpx" color="#2563eb" />
          </view>
          <view>
            <text class="block-title">系统通知</text>
            <text class="block-tip">管理端下发的公告、整改通知与提醒</text>
          </view>
        </view>
        <text v-if="notifications.length" class="block-count">{{ notifications.length }} 条</text>
      </view>

      <view v-if="loading" class="loading">
        <ev-icon name="radar" size="28rpx" color="#2563eb" />
        <text>加载中…</text>
      </view>

      <view v-else-if="!notifications.length" class="empty-wrap">
        <view class="empty-ico">
          <ev-icon name="bell" size="34rpx" color="#2563eb" />
        </view>
        <text class="empty">暂无通知</text>
      </view>

      <view v-else>
        <view
          v-for="n in notifications"
          :key="n.notificationId || n.id"
          class="notify"
          :class="[n.isUrgent ? 'notify-urgent' : '', !n.readByMe ? 'notify-unread' : '', isGuideNotification(n) ? 'notify-guide' : '']"
          @click="openNotify(n)"
        >
          <view class="notify-mark">
            <ev-icon :name="notifyIcon(n)" size="26rpx" :color="notifyColor(n)" />
          </view>

          <view class="notify-top">
            <view class="notify-title-wrap">
              <text v-if="!n.readByMe" class="dot"></text>
              <text class="notify-title">{{ n.title || '系统通知' }}</text>
              <text v-if="isGuideNotification(n)" class="tag-guide">停车引导</text>
              <text v-if="n.isUrgent" class="tag-urgent">紧急</text>
            </view>
            <text class="notify-del" @click.stop="removeNotify(n)">移除</text>
          </view>

          <text class="notify-body">{{ n.content || '' }}</text>

          <view class="notify-meta-row">
            <view class="notify-meta">
              <ev-icon name="radar" size="20rpx" color="#94a3b8" />
              <text>{{ notifyTime(n) }}</text>
            </view>
            <view class="notify-meta">
              <ev-icon name="folder" size="20rpx" color="#94a3b8" />
              <text>{{ notifyTypeText(n) }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="block">
      <view class="block-h">
        <view class="block-title-wrap">
          <view class="block-ico appeal-tone">
            <ev-icon name="appeal" size="28rpx" color="#2563eb" />
          </view>
          <view>
            <text class="block-title">申诉记录</text>
            <text class="block-tip">查看申诉审核状态与处理结果</text>
          </view>
        </view>
      </view>

      <view v-if="!appeals.length" class="empty-wrap">
        <view class="empty-ico blue">
          <ev-icon name="appeal" size="34rpx" color="#2563eb" />
        </view>
        <text class="empty">暂无申诉</text>
      </view>

      <view v-else>
        <view
          v-for="a in appeals"
          :key="a.appealId"
          class="appeal appeal-tap"
          @click="openAppeal(a.appealId)"
        >
          <view class="appeal-row">
            <view class="appeal-id-wrap">
              <view class="appeal-icon">
                <ev-icon name="appeal" size="24rpx" color="#2563eb" />
              </view>
              <text class="appeal-id">申诉 #{{ a.appealId }}</text>
            </view>
            <text :class="['appeal-state', appealClass(a.appealState)]">{{
              appealStateText(a.appealState)
            }}</text>
          </view>
          <text v-if="a.illegalTypeName || a.licensePlate" class="appeal-sub">{{
            a.illegalTypeName || '违规记录'
          }}{{ a.licensePlate ? ' · ' + a.licensePlate : '' }}</text>
          <text class="appeal-reason">{{ a.appealReason || '（无摘要）' }}</text>
          <view class="appeal-foot">
            <text class="appeal-meta">{{ formatDateTime(a.appealTime || a.createTime) }}</text>
            <view class="appeal-go">
              <text>查看详情</text>
              <ev-icon name="arrow-right" size="20rpx" color="#2563eb" />
            </view>
          </view>
        </view>
      </view>
    </view>

    <button class="logout" @click="logout">
      <view class="logout-inner">
        <ev-icon name="logout" size="26rpx" color="#64748b" />
        <text>退出登录</text>
      </view>
    </button>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser, clearAuth } from '../../utils/auth.js'
import { getUserNotifications, getUserAppeals, hideNotificationForUser } from '../../api/index.js'
import { formatDateTime, appealStateText, notificationTime } from '../../utils/display.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      notifications: [],
      appeals: [],
      loading: false
    }
  },
  computed: {
    unreadNotificationCount() {
      return this.notifications.filter((item) => !item.readByMe).length
    },
    urgentNotificationCount() {
      return this.notifications.filter((item) => !!item.isUrgent).length
    },
    pendingAppealCount() {
      return this.appeals.filter((item) => item.appealState === 0 || item.appealState === 1).length
    },
    metrics() {
      return [
        { icon: 'bell', label: '通知总数', value: this.notifications.length },
        { icon: 'warning', label: '紧急通知', value: this.urgentNotificationCount },
        { icon: 'appeal', label: '待处理申诉', value: this.pendingAppealCount }
      ]
    },
    quickLinks() {
      return [
        {
          icon: 'score',
          title: '记分与公益加分',
          desc: '查看剩余记分与公益补分记录',
          action: this.goLicensePoints,
          tone: 'q1',
          color: '#2563eb'
        },
        {
          icon: 'settings',
          title: '修改密码',
          desc: '更新账号口令与安全信息',
          action: this.goPassword,
          tone: 'q2',
          color: '#2563eb'
        }
      ]
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    formatDateTime,
    appealStateText,
    notifyTime(n) {
      return notificationTime(n)
    },
    isGuideNotification(n) {
      return String((n && n.type) || '').toLowerCase() === 'guide' || String((n && n.title) || '').includes('停车引导')
    },
    notifyIcon(n) {
      if (this.isGuideNotification(n)) return 'radar'
      return n && n.isUrgent ? 'warning' : 'bell'
    },
    notifyColor(n) {
      if (this.isGuideNotification(n)) return '#0f766e'
      return n && n.isUrgent ? '#b26d08' : '#2563eb'
    },
    notifyTypeText(n) {
      if (this.isGuideNotification(n)) return '停车引导'
      return (n && n.type) || '公告'
    },
    appealClass(code) {
      if (code === 2) return 's-ok'
      if (code === 3 || code === 5) return 's-bad'
      if (code === 0 || code === 1) return 's-pending'
      return ''
    },
    async load() {
      const u = getUser()
      if (!u || !u.userId) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      this.loading = true
      try {
        const list = await getUserNotifications(u.userId, 30)
        this.notifications = list || []
      } catch (e) {
        this.notifications = []
        const msg = (e && e.message) || '通知加载失败'
        uni.showToast({ title: msg, icon: 'none', duration: 2500 })
      }
      try {
        const { list } = await getUserAppeals(u.userId, 1, 30)
        this.appeals = list || []
      } catch (e) {
        this.appeals = []
      }
      this.loading = false
    },
    openNotify(n) {
      const id = n && (n.notificationId != null ? n.notificationId : n.id)
      if (!id) return
      uni.navigateTo({
        url: '/pages/notification-detail/notification-detail?id=' + encodeURIComponent(String(id))
      })
    },
    removeNotify(n) {
      const u = getUser()
      if (!u || !u.userId) return
      const id = n && (n.notificationId != null ? n.notificationId : n.id)
      if (!id) return
      uni.showModal({
        title: '移除通知',
        content: '从列表中移除后可在管理端仍保留记录。',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await hideNotificationForUser(id, u.userId)
            this.notifications = this.notifications.filter(
              (x) => (x.notificationId || x.id) !== id
            )
            uni.showToast({ title: '已移除', icon: 'success' })
          } catch (e) {
            uni.showToast({ title: e.message || '移除失败', icon: 'none' })
          }
        }
      })
    },
    openAppeal(appealId) {
      if (!appealId) return
      uni.navigateTo({
        url: '/pages/appeal-detail/appeal-detail?id=' + encodeURIComponent(String(appealId))
      })
    },
    goPassword() {
      uni.navigateTo({ url: '/pages/password/password' })
    },
    goLicensePoints() {
      uni.navigateTo({ url: '/pages/license-points/license-points' })
    },
    logout() {
      clearAuth()
      uni.reLaunch({ url: '/pages/login/login' })
    }
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx 24rpx 96rpx;
  box-sizing: border-box;
  background:
    linear-gradient(180deg, #edf8f6 0%, #f4f8fb 32%, #f7f9fc 100%);
}

.head-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1rpx solid var(--ev-border);
  border-radius: 18rpx;
  padding: 30rpx 26rpx 24rpx;
  margin-bottom: 24rpx;
  box-shadow: var(--ev-shadow);
}

.head-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18rpx;
}

.head-copy {
  flex: 1;
}

.head-chip {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 12rpx;
  border-radius: 12rpx;
  background: var(--ev-primary-soft);
  font-size: 22rpx;
  color: var(--ev-primary);
  font-weight: 600;
}

.head-title {
  display: block;
  margin-top: 18rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #111827;
}

.head-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #667085;
  line-height: 1.58;
}

.head-badge {
  min-width: 140rpx;
  padding: 16rpx 16rpx 14rpx;
  border-radius: 16rpx;
  text-align: center;
  background: #f8fafc;
  border: 1rpx solid var(--ev-border);
}

.head-badge-icon {
  width: 52rpx;
  height: 52rpx;
  margin: 0 auto 12rpx;
  border-radius: 12rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.head-badge-num {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  color: var(--ev-primary-dark);
}

.head-badge-txt {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #6b7c90;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
  margin-top: 26rpx;
}

.hero-metric {
  padding: 18rpx 16rpx;
  border-radius: 14rpx;
  background: #f8fafc;
  border: 1rpx solid var(--ev-border);
}

.metric-ico {
  width: 48rpx;
  height: 48rpx;
  border-radius: 12rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-metric-k {
  display: block;
  margin-top: 12rpx;
  font-size: 20rpx;
  color: #667085;
}

.hero-metric-v {
  display: block;
  margin-top: 10rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
}

.quick-row {
  margin-bottom: 24rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.quick-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 20rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
}

.quick-ico {
  width: 68rpx;
  height: 68rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.q1 {
  background: rgba(59, 130, 246, 0.12);
}

.q2 {
  background: var(--ev-primary-soft);
}

.quick-copy {
  flex: 1;
}

.quick-txt {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
}

.quick-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #7b8b9b;
  line-height: 1.55;
}

.quick-go {
  margin-top: 6rpx;
}

.block {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 22rpx;
  margin-bottom: 24rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
}

.block-h {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.block-title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.block-ico {
  width: 50rpx;
  height: 50rpx;
  border-radius: 12rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.block-ico.appeal-tone {
  background: rgba(59, 130, 246, 0.12);
}

.block-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.block-count {
  font-size: 24rpx;
  color: var(--ev-primary);
}

.block-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7b8b9b;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  color: #64748b;
  padding: 32rpx;
  font-size: 26rpx;
}

.empty-wrap {
  text-align: center;
  padding: 40rpx 24rpx;
}

.empty-ico {
  width: 84rpx;
  height: 84rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14rpx;
  border-radius: 14rpx;
  background: var(--ev-primary-soft);
}

.empty-ico.blue {
  background: rgba(59, 130, 246, 0.12);
}

.empty {
  color: #94a3b8;
  font-size: 26rpx;
}

.notify {
  position: relative;
  padding: 24rpx 0 24rpx 74rpx;
  border-bottom: 1rpx solid #edf2f7;
}

.notify:last-child {
  border-bottom: none;
}

.notify-unread .notify-title {
  font-weight: 700;
}

.notify-mark {
  position: absolute;
  left: 0;
  top: 24rpx;
  width: 56rpx;
  height: 56rpx;
  border-radius: 12rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.notify-urgent {
  background: #fff7ed;
  margin: 0 -8rpx;
  padding: 24rpx 8rpx 24rpx 82rpx;
  border-radius: 14rpx;
  border-bottom: none;
}

.notify-urgent .notify-mark {
  left: 8rpx;
  background: rgba(212, 135, 28, 0.12);
}

.notify-guide {
  background: linear-gradient(135deg, #ecfdf5 0%, #f0fdfa 100%);
  margin: 0 -8rpx;
  padding: 24rpx 8rpx 24rpx 82rpx;
  border: 1rpx solid #a7f3d0;
  border-radius: 14rpx;
}

.notify-guide .notify-mark {
  left: 8rpx;
  background: rgba(15, 118, 110, 0.12);
}

.notify-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12rpx;
  margin-bottom: 10rpx;
}

.notify-title-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex: 1;
  flex-wrap: wrap;
}

.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #2563eb;
  flex-shrink: 0;
}

.notify-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
}

.tag-urgent {
  font-size: 20rpx;
  color: #b45309;
  background: #fef3c7;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.tag-guide {
  font-size: 20rpx;
  color: #047857;
  background: #d1fae5;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.notify-del {
  font-size: 24rpx;
  color: #94a3b8;
  padding: 6rpx 8rpx;
  flex-shrink: 0;
}

.notify-body {
  font-size: 26rpx;
  color: #475569;
  line-height: 1.5;
  display: block;
}

.notify-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 12rpx;
}

.notify-meta {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.appeal {
  padding: 22rpx 0;
  border-bottom: 1rpx solid #edf2f7;
}

.appeal:last-child {
  border-bottom: none;
}

.appeal-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
}

.appeal-id-wrap {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
}

.appeal-icon {
  width: 42rpx;
  height: 42rpx;
  border-radius: 10rpx;
  background: rgba(59, 130, 246, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
}

.appeal-id {
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
}

.appeal-state {
  font-size: 22rpx;
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  background: #f1f5f9;
  color: #475569;
}

.appeal-state.s-ok {
  background: #d1fae5;
  color: #047857;
}

.appeal-state.s-bad {
  background: #fee2e2;
  color: #b91c1c;
}

.appeal-state.s-pending {
  background: #e0f2fe;
  color: #0369a1;
}

.appeal-sub {
  display: block;
  font-size: 24rpx;
  color: #64748b;
  margin-top: 10rpx;
}

.appeal-reason {
  display: block;
  font-size: 26rpx;
  color: #334155;
  margin-top: 10rpx;
  line-height: 1.48;
}

.appeal-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
  margin-top: 12rpx;
}

.appeal-meta {
  font-size: 22rpx;
  color: #94a3b8;
}

.appeal-go {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  font-size: 22rpx;
  color: #2563eb;
  font-weight: 600;
}

.logout {
  margin-top: 18rpx;
  background: #fff;
  color: #64748b;
  border-radius: 14rpx;
  font-size: 28rpx;
  border: 1rpx solid #e2e8f0;
}

.logout-inner {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  font-size: 28rpx;
}

@media screen and (max-width: 360px) {
  .head-top,
  .block-h,
  .notify-top,
  .appeal-row,
  .appeal-foot {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-metrics,
  .quick-row {
    grid-template-columns: 1fr;
  }
}
</style>

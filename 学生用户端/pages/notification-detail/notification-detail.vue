<template>
  <view class="page">
    <view v-if="loading" class="loading">加载中…</view>
    <view v-else-if="!detail" class="empty-wrap">
      <text class="empty">无法加载该通知</text>
    </view>
    <view v-else>
      <view class="card" :class="[detail.isUrgent ? 'card-urgent' : '', isGuideNotification(detail) ? 'card-guide' : '']">
        <view class="card-icon" :class="[detail.isUrgent ? 'urgent' : '', isGuideNotification(detail) ? 'guide' : '']">
          <ev-icon :name="notifyIcon(detail)" size="34rpx" :color="notifyColor(detail)" />
        </view>
        <view class="row-top">
          <text class="title">{{ detail.title || '系统通知' }}</text>
          <text v-if="isGuideNotification(detail)" class="tag guide-tag">停车引导</text>
          <text v-if="detail.isUrgent" class="tag">紧急</text>
        </view>
        <text class="body">{{ detail.content || '' }}</text>
        <text class="meta">{{ notifyTime(detail) }} · {{ notifyTypeText(detail) }}</text>
      </view>
      <view class="actions">
        <button class="btn secondary" @click="doRemove">从列表移除</button>
      </view>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser } from '../../utils/auth.js'
import {
  getNotificationDetail,
  markNotificationRead,
  hideNotificationForUser
} from '../../api/index.js'
import { notificationTime } from '../../utils/display.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      notificationId: null,
      detail: null,
      loading: true
    }
  },
  onLoad(q) {
    const id = q.id != null ? parseInt(String(q.id), 10) : NaN
    this.notificationId = Number.isFinite(id) ? id : null
  },
  onShow() {
    this.load()
  },
  methods: {
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
      return n && n.isUrgent ? '#b45309' : '#0f8f83'
    },
    notifyTypeText(n) {
      if (this.isGuideNotification(n)) return '停车引导'
      return (n && n.type) || '公告'
    },
    async load() {
      const u = getUser()
      if (!u || !u.userId) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      if (!this.notificationId) {
        this.detail = null
        this.loading = false
        return
      }
      this.loading = true
      try {
        this.detail = await getNotificationDetail(this.notificationId, u.userId)
        try {
          await markNotificationRead(this.notificationId, u.userId)
        } catch (e) {
          /* 已读失败不阻塞详情展示 */
        }
      } catch (e) {
        this.detail = null
      }
      this.loading = false
    },
    doRemove() {
      const u = getUser()
      if (!u || !u.userId || !this.notificationId) return
      uni.showModal({
        title: '移除通知',
        content: '将从你的通知列表中移除，不影响系统记录。',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await hideNotificationForUser(this.notificationId, u.userId)
            uni.showToast({ title: '已移除', icon: 'success' })
            setTimeout(() => uni.navigateBack(), 400)
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
  min-height: 100vh;
  padding: 28rpx 24rpx 56rpx;
  box-sizing: border-box;
}
.loading {
  text-align: center;
  color: #94a3b8;
  padding: 48rpx;
}
.empty-wrap {
  text-align: center;
  padding: 80rpx 24rpx;
}
.empty {
  color: #94a3b8;
  font-size: 28rpx;
}
.card {
  position: relative;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30rpx;
  padding: 34rpx 30rpx;
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}
.card-urgent {
  background: #fffbeb;
  border-color: #fde68a;
}

.card-guide {
  background: linear-gradient(135deg, #ecfdf5 0%, #f0fdfa 100%);
  border-color: #a7f3d0;
}
.card-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 22rpx;
}
.card-icon.urgent {
  background: #fef3c7;
}

.card-icon.guide {
  background: rgba(15, 118, 110, 0.12);
}
.row-top {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.title {
  flex: 1;
  font-size: 34rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
}
.tag {
  font-size: 22rpx;
  color: #b45309;
  background: #fef3c7;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.guide-tag {
  color: #047857;
  background: #d1fae5;
}
.body {
  font-size: 28rpx;
  color: #334155;
  line-height: 1.7;
  display: block;
}
.meta {
  display: block;
  margin-top: 24rpx;
  font-size: 24rpx;
  color: #94a3b8;
}
.actions {
  margin-top: 32rpx;
}
.btn {
  height: 84rpx;
  line-height: normal;
  border-radius: 18rpx;
  font-size: 28rpx;
}
.btn.secondary {
  background: rgba(255, 255, 255, 0.96);
  color: #64748b;
  border: 1rpx solid var(--ev-border);
}
</style>

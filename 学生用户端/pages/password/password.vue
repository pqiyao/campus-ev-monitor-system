<template>
  <view class="page">
    <view class="head">
      <view class="head-icon">
        <ev-icon name="lock" size="42rpx" color="#ffffff" />
      </view>
      <view class="head-copy">
        <text class="title">修改密码</text>
        <text class="sub">需验证当前密码，新密码至少 6 位</text>
      </view>
    </view>
    <view class="card">
      <view class="field">
        <view class="field-head">
          <text class="label">当前密码</text>
          <text class="field-tip">用于确认身份</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="shield" size="30rpx" color="#0f8f83" /></view>
          <input v-model="oldPassword" class="input" password placeholder="请输入当前密码" />
        </view>
      </view>
      <view class="field">
        <view class="field-head">
          <text class="label">新密码</text>
          <text class="field-tip">至少 6 位</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="lock" size="30rpx" color="#0f8f83" /></view>
          <input v-model="newPassword" class="input" password placeholder="请输入新密码" />
        </view>
      </view>
      <view class="field">
        <view class="field-head">
          <text class="label">确认新密码</text>
          <text class="field-tip">保持一致</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="check" size="30rpx" color="#0f8f83" /></view>
          <input v-model="confirmPassword" class="input" password placeholder="再次输入新密码" />
        </view>
      </view>
      <button class="btn" type="primary" :loading="loading" @click="onSubmit">
        <view class="btn-inner">
          <text>保存并重新登录</text>
          <ev-icon name="arrow-right" size="26rpx" color="#ffffff" />
        </view>
      </button>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { updatePassword } from '../../api/index.js'
import { getUser, clearAuth } from '../../utils/auth.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
      loading: false
    }
  },
  methods: {
    async onSubmit() {
      const u = getUser()
      if (!u || u.userId == null) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      const oldP = (this.oldPassword || '').trim()
      const newP = (this.newPassword || '').trim()
      const cfm = (this.confirmPassword || '').trim()
      if (!oldP || !newP) {
        uni.showToast({ title: '请填写完整', icon: 'none' })
        return
      }
      if (newP.length < 6) {
        uni.showToast({ title: '新密码至少 6 位', icon: 'none' })
        return
      }
      if (newP !== cfm) {
        uni.showToast({ title: '两次新密码不一致', icon: 'none' })
        return
      }
      this.loading = true
      try {
        await updatePassword(u.userId, oldP, newP)
        clearAuth()
        uni.showToast({ title: '已修改，请重新登录', icon: 'success' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/login/login' })
        }, 800)
      } catch (e) {
        uni.showToast({ title: (e && e.message) || '修改失败', icon: 'none', duration: 2500 })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 34rpx 28rpx 56rpx;
  box-sizing: border-box;
}
.head {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 32rpx 28rpx;
  margin-bottom: 24rpx;
  border-radius: 30rpx;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.1), transparent 42%),
    linear-gradient(125deg, #155e75, #0f8f83);
  box-shadow: var(--ev-shadow-strong);
}
.head-icon {
  width: 82rpx;
  height: 82rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.head-copy {
  flex: 1;
}
.title {
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
  display: block;
}
.sub {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
  display: block;
}
.card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28rpx;
  padding: 32rpx 28rpx 30rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid var(--ev-border);
}
.field {
  margin-bottom: 28rpx;
}
.field-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 12rpx;
}
.label {
  display: block;
  font-size: 26rpx;
  color: #334155;
  font-weight: 700;
}
.field-tip {
  font-size: 22rpx;
  color: #94a3b8;
}
.input-wrap {
  display: flex;
  align-items: center;
  height: 94rpx;
  padding: 0 14rpx;
  background: #f8fafc;
  border-radius: 18rpx;
  box-sizing: border-box;
  border: 1rpx solid #e2e8f0;
}
.input-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.input {
  flex: 1;
  height: 88rpx;
  padding: 0 18rpx;
  box-sizing: border-box;
  font-size: 28rpx;
  color: #0f172a;
}
.btn {
  margin-top: 16rpx;
  height: 92rpx;
  line-height: normal;
  background: linear-gradient(90deg, #18a897, #0f8f83, #0b7068) !important;
  color: #fff;
  border-radius: 20rpx !important;
  font-size: 30rpx;
  border: none !important;
}
.btn-inner {
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-weight: 700;
}
</style>

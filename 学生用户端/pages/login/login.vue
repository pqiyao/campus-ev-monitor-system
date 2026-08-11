<template>
  <view class="page">
    <view class="bg-orb orb-a"></view>
    <view class="bg-orb orb-b"></view>

    <view class="hero">
      <view class="hero-copy">
        <view class="hero-chip">
          <ev-icon name="shield" size="28rpx" color="#dbfff7" />
          <text>SMART CAMPUS MOBILITY</text>
        </view>
        <text class="title">校园电动车学生端</text>
        <text class="sub">把车辆档案、定位同步、违规整改和申诉流程收进一个更清晰的入口里。</text>
        <view class="feature-row">
          <view class="feature-pill">
            <ev-icon name="bike" size="28rpx" color="#0f8f83" />
            <text>车辆档案</text>
          </view>
          <view class="feature-pill">
            <ev-icon name="location" size="28rpx" color="#0f8f83" />
            <text>定位同步</text>
          </view>
          <view class="feature-pill">
            <ev-icon name="appeal" size="28rpx" color="#0f8f83" />
            <text>违规申诉</text>
          </view>
        </view>
      </view>

      <view class="hero-panel">
        <view class="panel-card">
          <view class="panel-ico">
            <ev-icon name="radar" size="40rpx" color="#0b7068" />
          </view>
          <text class="panel-k">实时位置</text>
          <text class="panel-v">与后台定位数据同源</text>
        </view>
        <view class="panel-card">
          <view class="panel-ico warm">
            <ev-icon name="warning" size="40rpx" color="#c66a2a" />
          </view>
          <text class="panel-k">整改提醒</text>
          <text class="panel-v">待办状态一眼可见</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-head">
        <view>
          <text class="card-title">欢迎登录</text>
          <text class="card-sub">使用已注册的学生账号进入个人车辆工作台</text>
        </view>
        <view class="card-badge">
          <text class="card-badge-num">01</text>
          <text class="card-badge-txt">Sign In</text>
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">用户名</text>
          <text class="field-tip">学号或自定义账号</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon">
            <ev-icon name="user" size="34rpx" color="#0b7068" />
          </view>
          <input v-model="username" class="input" placeholder="请输入用户名" />
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">密码</text>
          <text class="field-tip">不少于 6 位</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon">
            <ev-icon name="lock" size="34rpx" color="#0b7068" />
          </view>
          <input v-model="password" class="input" password placeholder="请输入密码" />
        </view>
      </view>

      <button class="btn" type="primary" :loading="loading" @click="onLogin">
        <view class="btn-inner">
          <text>进入学生端</text>
          <ev-icon name="arrow-right" size="28rpx" color="#ffffff" />
        </view>
      </button>

      <view class="helper">
        <text class="helper-label">首次使用</text>
        <text class="link" @click="goRegister">去注册账号</text>
      </view>
    </view>

    <view class="foot-note">
      <ev-icon name="shield" size="24rpx" color="#5f7287" />
      <text>登录后可查看车辆位置、BLE 蓝牙标签状态、违规整改与申诉进度。</text>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { login } from '../../api/index.js'
import { setUser, setToken } from '../../utils/auth.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      username: '',
      password: '',
      loading: false
    }
  },
  methods: {
    async onLogin() {
      const u = (this.username || '').trim()
      const p = (this.password || '').trim()
      if (!u || !p) {
        uni.showToast({ title: '请填写用户名和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const user = await login(u, p)
        if (user.token) setToken(user.token)
        setUser(user)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/home/home' })
        }, 400)
      } catch (e) {
        uni.showToast({ title: e.message || '登录失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/register/register' })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  padding: 56rpx 30rpx 52rpx;
  box-sizing: border-box;
  overflow: hidden;
  background:
    linear-gradient(180deg, #edf8f6 0%, #f4f8fb 42%, #f7f9fc 100%);
}

.bg-orb {
  display: none;
}

.orb-a {
  width: 320rpx;
  height: 320rpx;
  top: -120rpx;
  right: -90rpx;
  background: rgba(65, 201, 182, 0.26);
}

.orb-b {
  width: 280rpx;
  height: 280rpx;
  left: -120rpx;
  top: 430rpx;
  background: rgba(11, 112, 104, 0.12);
}

.hero {
  position: relative;
  z-index: 1;
  padding: 34rpx 28rpx 28rpx;
  border-radius: 30rpx;
  margin-bottom: 28rpx;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.2), transparent 28%),
    linear-gradient(140deg, #0f8f83 0%, #0b7068 56%, #0a5e58 100%);
  box-shadow: 0 24rpx 64rpx rgba(11, 112, 104, 0.22);
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  right: -80rpx;
  bottom: -80rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 18rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(244, 255, 253, 0.96);
  font-size: 20rpx;
  letter-spacing: 2rpx;
  font-weight: 700;
}

.title {
  margin-top: 24rpx;
  display: block;
  font-size: 50rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1.5rpx;
}

.sub {
  margin-top: 16rpx;
  display: block;
  font-size: 25rpx;
  color: rgba(236, 255, 251, 0.88);
  line-height: 1.68;
}

.feature-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 22rpx;
}

.feature-pill {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 18rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.92);
  color: #274554;
  font-size: 22rpx;
  font-weight: 600;
}

.hero-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 28rpx;
  position: relative;
  z-index: 1;
}

.panel-card {
  border-radius: 20rpx;
  padding: 20rpx 18rpx;
  background: rgba(255, 255, 255, 0.14);
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8rpx);
}

.panel-ico {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-ico.warm {
  background: rgba(255, 243, 233, 0.96);
}

.panel-k {
  margin-top: 16rpx;
  display: block;
  font-size: 25rpx;
  color: #ffffff;
  font-weight: 700;
}

.panel-v {
  margin-top: 10rpx;
  display: block;
  font-size: 21rpx;
  color: rgba(234, 251, 248, 0.86);
  line-height: 1.5;
}

.card {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 30rpx;
  padding: 38rpx 30rpx 30rpx;
  box-shadow: 0 20rpx 58rpx rgba(11, 112, 104, 0.08);
  border: 1rpx solid rgba(120, 150, 157, 0.14);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.card-title {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #142334;
}

.card-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #738698;
  line-height: 1.55;
}

.card-badge {
  min-width: 118rpx;
  padding: 14rpx 14rpx 12rpx;
  border-radius: 18rpx;
  background: linear-gradient(180deg, #effdfa, #dcf8f2);
  text-align: center;
}

.card-badge-num {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  color: #0b7068;
}

.card-badge-txt {
  display: block;
  margin-top: 6rpx;
  font-size: 19rpx;
  letter-spacing: 1rpx;
  color: #5e7b8c;
}

.field {
  margin-bottom: 26rpx;
}

.field-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.label {
  font-size: 25rpx;
  color: #3f5568;
  font-weight: 600;
}

.field-tip {
  font-size: 21rpx;
  color: #8aa0ae;
}

.input-wrap {
  display: flex;
  align-items: center;
  height: 96rpx;
  padding: 0 10rpx 0 12rpx;
  background: linear-gradient(180deg, #fbfefe, #f4faf9);
  border-radius: 20rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(120, 150, 157, 0.18);
}

.input-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 18rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.input {
  flex: 1;
  height: 88rpx;
  padding: 0 18rpx;
  box-sizing: border-box;
  font-size: 28rpx;
  color: #142334;
}

.btn {
  margin-top: 8rpx;
  height: 94rpx;
  line-height: normal;
  background: linear-gradient(90deg, #18a897, #0f8f83, #0b7068) !important;
  border: none !important;
  border-radius: 20rpx !important;
}

.btn-inner {
  height: 94rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1rpx;
}

.helper {
  margin-top: 26rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.helper-label {
  font-size: 24rpx;
  color: #8393a2;
}

.link {
  color: var(--ev-primary);
  font-size: 27rpx;
  font-weight: 700;
}

.foot-note {
  position: relative;
  z-index: 1;
  margin-top: 28rpx;
  padding: 0 12rpx;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 10rpx;
  font-size: 22rpx;
  color: #728595;
  line-height: 1.65;
  text-align: center;
}

@media screen and (max-width: 360px) {
  .page {
    padding-left: 24rpx;
    padding-right: 24rpx;
  }

  .hero-panel {
    grid-template-columns: 1fr;
  }

  .card-head,
  .field-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

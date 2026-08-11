<script>
import { getToken, getUser } from './utils/auth.js'
import { refreshClientConfig } from './utils/runtimeConfig.js'

export default {
  async onLaunch() {
    await refreshClientConfig()
    const token = getToken()
    const user = getUser()
    if (token && user && user.userId) {
      uni.reLaunch({ url: '/pages/home/home' })
    }
  },
  onShow() {},
  onHide() {}
}
</script>

<style>
page {
  --ev-primary: #0f8f83;
  --ev-primary-dark: #0b7068;
  --ev-primary-soft: #e7f8f5;
  --ev-accent: #2563eb;
  --ev-ink: #0f172a;
  --ev-blue: #2563eb;
  --ev-amber: #b45309;
  --ev-rose: #c2410c;
  --ev-bg: #f4f8fb;
  --ev-bg-deep: #eaf2f5;
  --ev-card: #ffffff;
  --ev-text: #0f172a;
  --ev-muted: #64748b;
  --ev-border: #e2e8f0;
  --ev-shadow: 0 16rpx 44rpx rgba(15, 23, 42, 0.07);
  --ev-shadow-strong: 0 22rpx 62rpx rgba(11, 112, 104, 0.18);
  --ev-warn-bg: #fff7ed;
  --ev-warn-text: #c2410c;
  --ev-safe: #f0fdf4;
  --ev-panel: #ffffff;
  background:
    linear-gradient(180deg, #edf8f6 0%, #f4f8fb 34%, #f7f9fc 100%);
  font-size: 28rpx;
  color: #334155;
  font-family: "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
}

button::after {
  border: none;
}

button {
  letter-spacing: 0;
}
</style>

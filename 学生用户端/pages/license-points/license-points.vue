<template>
  <view class="page">
    <view class="card top">
      <view class="top-copy">
        <view class="top-chip">
          <ev-icon name="score" size="24rpx" color="#dcfff8" />
          <text>LICENSE POINTS</text>
        </view>
        <text class="t1">校园通行记分</text>
        <text class="t2">满分 {{ summary.maxPoints != null ? summary.maxPoints : 12 }} 分，当前剩余</text>
      </view>
      <view class="score-box">
        <text class="big">{{ summary.remaining != null ? summary.remaining : '—' }}</text>
        <text class="score-unit">分</text>
      </view>
      <view class="top-foot">
        <view class="mini-stat">
          <text class="mini-k">已记扣</text>
          <text class="mini-v">{{ summary.usedPoints != null ? summary.usedPoints : '—' }}</text>
        </view>
        <view class="mini-stat">
          <text class="mini-k">同步来源</text>
          <text class="mini-v">首页 / 后台</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="section-head">
        <view class="section-icon"><ev-icon name="appeal" size="28rpx" color="#0f8f83" /></view>
        <view>
          <text class="h">公益加分申请</text>
          <text class="tip">参加志愿服务等可提交说明与凭证，审核通过后记分恢复（不超过满分）。</text>
        </view>
      </view>
      <textarea
        class="area"
        v-model="welfareContent"
        placeholder="请填写公益活动内容，至少6个字"
        maxlength="800"
      />
      <button class="btn-img" @click="pickImages">
        <view class="btn-inner plain">
          <ev-icon name="camera" size="24rpx" color="#334155" />
          <text>上传凭证图（可选）</text>
        </view>
      </button>
      <view v-if="imageUrls.length" class="imgs">
        <text v-for="(u, i) in imageUrls" :key="i" class="img-line">{{ u.slice(0, 48) }}…</text>
      </view>
      <button class="btn-submit" type="primary" :loading="submitting" @click="submitWelfare">
        <view class="btn-inner">
          <text>提交申请</text>
          <ev-icon name="arrow-right" size="24rpx" color="#ffffff" />
        </view>
      </button>
    </view>

    <view class="card">
      <view class="section-head slim">
        <view class="section-icon blue"><ev-icon name="folder" size="28rpx" color="#2563eb" /></view>
        <text class="h">我的申请</text>
      </view>
      <view v-if="!myReqs.length" class="empty">暂无记录</view>
      <view v-for="r in myReqs" :key="r.requestId" class="req">
        <view class="req-top">
          <text class="req-id">#{{ r.requestId }}</text>
          <text :class="['req-st', stClass(r.status)]">{{ stText(r.status) }}</text>
        </view>
        <text class="req-c">{{ r.content }}</text>
        <text v-if="r.pointsAwarded" class="req-p">加分 {{ r.pointsAwarded }}</text>
      </view>
    </view>

    <view class="card">
      <view class="section-head slim">
        <view class="section-icon amber"><ev-icon name="radar" size="28rpx" color="#b45309" /></view>
        <text class="h">最近流水</text>
      </view>
      <view v-if="!logs.length" class="empty">暂无</view>
      <view v-for="l in logs" :key="l.logId" class="log">
        <text class="log-d">{{ l.delta > 0 ? '+' + l.delta : l.delta }}</text>
        <text class="log-b">余 {{ l.balanceAfter }}</text>
        <text class="log-t">{{ l.bizType }}</text>
        <text class="log-time">{{ l.createTime }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser } from '../../utils/auth.js'
import {
  getLicenseSummary,
  getLicensePointLog,
  submitWelfarePoint,
  getMyWelfareRequests,
  uploadFile
} from '../../api/index.js'
import { fileUrl } from '../../utils/config.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      summary: {},
      welfareContent: '',
      imageUrls: [],
      submitting: false,
      myReqs: [],
      logs: []
    }
  },
  onShow() {
    const u = getUser()
    if (!u || !u.userId) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.loadAll(u.userId)
  },
  methods: {
    stText(s) {
      if (s === 1) return '已通过'
      if (s === 2) return '已拒绝'
      return '待审核'
    },
    stClass(s) {
      if (s === 1) return 'ok'
      if (s === 2) return 'bad'
      return 'pend'
    },
    async loadAll(userId) {
      try {
        this.summary = (await getLicenseSummary(userId)) || {}
      } catch (e) {
        this.summary = {}
      }
      try {
        const { list } = await getLicensePointLog(userId, 1, 40)
        this.logs = list || []
      } catch (e) {
        this.logs = []
      }
      try {
        this.myReqs = (await getMyWelfareRequests(userId)) || []
      } catch (e) {
        this.myReqs = []
      }
    },
    pickImages() {
      uni.chooseImage({
        count: 3,
        success: async (res) => {
          const paths = res.tempFilePaths || []
          const urls = []
          for (const p of paths) {
            try {
              const data = await uploadFile(p)
              const raw = data && (data.fileUrl || data.url)
              if (typeof raw === 'string' && raw) urls.push(fileUrl(raw))
            } catch (e) {
              uni.showToast({ title: '某张上传失败', icon: 'none' })
            }
          }
          this.imageUrls = [...this.imageUrls, ...urls]
        }
      })
    },
    async submitWelfare() {
      const u = getUser()
      const c = (this.welfareContent || '').trim()
      if (c.length < 6) {
        uni.showToast({ title: '说明至少6个字', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        await submitWelfarePoint(u.userId, c, this.imageUrls.join(','))
        uni.showToast({ title: '已提交', icon: 'success' })
        this.welfareContent = ''
        this.imageUrls = []
        this.loadAll(u.userId)
      } catch (e) {
        uni.showToast({ title: (e && e.message) || '失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
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
.card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28rpx;
  padding: 28rpx 26rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}
.top {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.1), transparent 42%),
    linear-gradient(125deg, #155e75, #0f8f83 58%, #0b7068);
  color: #fff;
  box-shadow: var(--ev-shadow-strong);
}
.top-chip {
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
.t1 {
  margin-top: 20rpx;
  font-size: 36rpx;
  font-weight: 800;
  display: block;
}
.t2 {
  font-size: 24rpx;
  opacity: 0.85;
  display: block;
  margin-top: 8rpx;
}
.score-box {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  margin-top: 10rpx;
}
.big {
  font-size: 72rpx;
  font-weight: 800;
  display: block;
}
.score-unit {
  font-size: 26rpx;
  opacity: 0.86;
}
.top-foot {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 20rpx;
}
.mini-stat {
  padding: 16rpx 18rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.14);
  border: 1rpx solid rgba(255, 255, 255, 0.16);
}
.mini-k {
  display: block;
  font-size: 21rpx;
  opacity: 0.76;
}
.mini-v {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  font-weight: 700;
}
.h {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  display: block;
}
.tip {
  font-size: 24rpx;
  color: #64748b;
  display: block;
  margin-top: 8rpx;
  line-height: 1.5;
}
.section-head {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  margin-bottom: 18rpx;
}
.section-head.slim {
  align-items: center;
}
.section-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 16rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.section-icon.blue {
  background: rgba(37, 99, 235, 0.1);
}
.section-icon.amber {
  background: rgba(245, 158, 11, 0.14);
}
.area {
  width: 100%;
  min-height: 178rpx;
  padding: 20rpx;
  box-sizing: border-box;
  background: #f8fafc;
  border-radius: 18rpx;
  border: 1rpx solid #e2e8f0;
  font-size: 26rpx;
  margin-bottom: 16rpx;
}
.btn-img {
  height: 78rpx;
  line-height: normal;
  font-size: 26rpx;
  background: #f1f5f9;
  color: #334155;
  margin-bottom: 12rpx;
  border-radius: 18rpx;
}
.imgs {
  margin-bottom: 16rpx;
}
.img-line {
  font-size: 20rpx;
  color: #94a3b8;
  display: block;
}
.btn-submit {
  height: 84rpx;
  line-height: normal;
  background: linear-gradient(90deg, #18a897, #0f8f83, #0b7068) !important;
  color: #fff;
  font-size: 28rpx;
  border-radius: 18rpx !important;
  border: none !important;
}
.btn-inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  font-weight: 700;
}
.btn-inner.plain {
  color: #334155;
}
.empty {
  color: #94a3b8;
  font-size: 26rpx;
  text-align: center;
  padding: 24rpx;
}
.req {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
}
.req-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
}
.req-id {
  font-size: 24rpx;
  color: #64748b;
}
.req-st {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
}
.req-st.pend {
  background: #e0f2fe;
  color: #0369a1;
}
.req-st.ok {
  background: #d1fae5;
  color: #047857;
}
.req-st.bad {
  background: #fee2e2;
  color: #b91c1c;
}
.req-c {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #334155;
}
.req-p {
  font-size: 22rpx;
  color: #0d9488;
  margin-top: 8rpx;
  display: block;
}
.log {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f8fafc;
  font-size: 24rpx;
}
.log-d {
  color: #b91c1c;
  font-weight: 600;
  width: 80rpx;
}
.log-b {
  color: #0f766e;
  width: 120rpx;
}
.log-t {
  flex: 1;
  color: #475569;
}
.log-time {
  color: #94a3b8;
  font-size: 22rpx;
  width: 100%;
}

@media screen and (max-width: 360px) {
  .top-foot {
    grid-template-columns: 1fr;
  }
}
</style>

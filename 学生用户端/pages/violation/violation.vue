<template>
  <view class="page">
    <view class="hero">
      <view class="hero-main">
        <view class="hero-chip">
          <ev-icon name="warning" size="24rpx" color="#ffe9de" />
          <text>RECTIFY & APPEAL</text>
        </view>
        <text class="hero-title">违规整治中心</text>
        <text class="hero-sub">集中处理待整改记录、上传佐证材料，并在需要时发起申诉。</text>
      </view>
      <view class="hero-badge">
        <text class="hero-badge-num">{{ filtered.length }}</text>
        <text class="hero-badge-text">当前筛选</text>
      </view>
    </view>

    <view class="tabs-wrap">
      <view
        v-for="t in tabs"
        :key="t.key"
        :class="['tab', tabKey === t.key ? 'on' : '']"
        @click="tabKey = t.key"
      >
        <view :class="['tab-icon', tabKey === t.key ? 'active' : '']">
          <ev-icon :name="t.icon" size="24rpx" :color="tabKey === t.key ? '#ffffff' : '#6b7c90'" />
        </view>
        <view class="tab-copy">
          <text class="tab-label">{{ t.label }}</text>
          <text class="tab-count">{{ countByStates(t.states) }} 条</text>
        </view>
      </view>
    </view>

    <view v-if="!plate" class="empty">
      <view class="empty-icon">
        <ev-icon name="bike" size="48rpx" color="#0b7068" />
      </view>
      <text class="empty-title">请先绑定车辆</text>
      <text class="empty-desc">车辆绑定完成后，违规记录、整改流程和申诉入口才会出现。</text>
    </view>

    <view v-else-if="filtered.length === 0" class="empty">
      <view class="empty-icon soft">
        <ev-icon name="check" size="48rpx" color="#0b7068" />
      </view>
      <text class="empty-title">当前分类暂无记录</text>
      <text class="empty-desc">切换上方分类，或者稍后再查看最新的违规处理状态。</text>
    </view>

    <view v-else class="list">
      <view v-for="item in filtered" :key="item.illegalId" class="card">
        <view class="card-top">
          <view class="title-wrap">
            <view class="title-icon">
              <ev-icon :name="cardIcon(item.illegalState)" size="24rpx" :color="cardIconColor(item.illegalState)" />
            </view>
            <view>
              <text class="type">{{ item.illegalTypeName || '违规记录' }}</text>
              <text class="plate">{{ item.licensePlate || plate }}</text>
            </view>
          </view>
          <text :class="['state-chip', chipClass(item.illegalState)]">{{ violationStateText(item.illegalState) }}</text>
        </view>

        <image v-if="evidence(item)" :src="evidence(item)" class="ev" mode="aspectFill" />

        <view class="meta-row">
          <view class="meta">
            <ev-icon name="radar" size="20rpx" color="#94a3b8" />
            <text>{{ formatDateTime(item.illegalTime) }}</text>
          </view>
          <view class="meta">
            <ev-icon name="warning" size="20rpx" color="#94a3b8" />
            <text>ID {{ item.illegalId }}</text>
          </view>
        </view>

        <view class="ops">
          <button
            v-if="item.illegalState === 0 || item.illegalState === 3"
            class="btn-main"
            @click="start(item)"
          >
            <view class="btn-inner">
              <ev-icon name="spark" size="22rpx" color="#ffffff" />
              <text>开始整改</text>
            </view>
          </button>

          <button
            v-if="item.illegalState === 1"
            class="btn-main"
            @click="uploadRect(item)"
          >
            <view class="btn-inner">
              <ev-icon name="camera" size="22rpx" color="#ffffff" />
              <text>上传佐证</text>
            </view>
          </button>

          <button
            v-if="canAppeal(item)"
            class="btn-sub"
            @click="appeal(item)"
          >
            <view class="btn-inner sub">
              <ev-icon name="appeal" size="22rpx" color="#476072" />
              <text>发起申诉</text>
            </view>
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser } from '../../utils/auth.js'
import { fetchMyVehicle } from '../../utils/vehicle.js'
import {
  listViolationsByPlate,
  startRectify,
  submitRectify,
  listRectify,
  submitAppeal,
  uploadFile,
  checkAppealable
} from '../../api/index.js'
import { fileUrl } from '../../utils/config.js'
import { formatDateTime, violationStateText } from '../../utils/display.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      plate: '',
      list: [],
      tabKey: 'todo',
      tabs: [
        { key: 'todo', label: '待处理', states: [0, 3], icon: 'warning' },
        { key: 'doing', label: '整改中', states: [1], icon: 'camera' },
        { key: 'appeal', label: '申诉中', states: [4], icon: 'appeal' },
        { key: 'done', label: '已完结', states: [2, 5, 6], icon: 'check' }
      ]
    }
  },
  computed: {
    filtered() {
      const tab = this.tabs.find((t) => t.key === this.tabKey)
      const st = tab ? tab.states : []
      return this.list.filter((x) => st.indexOf(x.illegalState) >= 0)
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    formatDateTime,
    violationStateText,
    countByStates(states) {
      return this.list.filter((x) => states.indexOf(x.illegalState) >= 0).length
    },
    chipClass(code) {
      if (code === 0 || code === 3) return 'c-warn'
      if (code === 1) return 'c-doing'
      if (code === 4) return 'c-wait'
      if (code === 2 || code === 5 || code === 6) return 'c-done'
      return ''
    },
    cardIcon(code) {
      if (code === 1) return 'camera'
      if (code === 4) return 'appeal'
      if (code === 2 || code === 5 || code === 6) return 'check'
      return 'warning'
    },
    cardIconColor(code) {
      if (code === 1) return '#2563eb'
      if (code === 4) return '#7c3aed'
      if (code === 2 || code === 5 || code === 6) return '#047857'
      return '#b45309'
    },
    canAppeal(item) {
      return item.illegalState === 0 || item.illegalState === 3
    },
    evidence(item) {
      if (!item.evidenceUrl) return ''
      return fileUrl(item.evidenceUrl)
    },
    async load() {
      const u = getUser()
      if (!u || !u.userId) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      const v = await fetchMyVehicle(u.userId)
      if (!v || !v.licensePlate) {
        this.plate = ''
        this.list = []
        return
      }
      this.plate = v.licensePlate
      try {
        const { list } = await listViolationsByPlate(this.plate, 80)
        this.list = list || []
      } catch (e) {
        this.list = []
      }
    },
    async start(item) {
      const u = getUser()
      try {
        await startRectify(item.illegalId, u.userId, '学生端发起整改')
        uni.showToast({ title: '已进入整改中', icon: 'success' })
        this.load()
        this.tabKey = 'doing'
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    },
    async findRectifyId(illegalId) {
      const { list } = await listRectify(this.plate, 50)
      const row = (list || []).find((r) => r.illegalId === illegalId)
      return row ? row.rectifyId : null
    },
    async uploadRect(item) {
      const rid = await this.findRectifyId(item.illegalId)
      if (!rid) {
        uni.showToast({ title: '未找到整改单，请先开始整改', icon: 'none' })
        return
      }
      uni.chooseImage({
        count: 3,
        sizeType: ['compressed'],
        success: async (res) => {
          const paths = res.tempFilePaths || []
          const urls = []
          try {
            for (const p of paths) {
              const up = await uploadFile(p)
              if (up.fileUrl) urls.push(up.fileUrl)
            }
            if (!urls.length) throw new Error('未上传成功')
            await submitRectify(rid, urls.join(','), '学生上传整改佐证')
            uni.showToast({ title: '已提交', icon: 'success' })
            this.load()
          } catch (e) {
            uni.showToast({ title: e.message || '上传失败', icon: 'none' })
          }
        }
      })
    },
    async appeal(item) {
      const u = getUser()
      try {
        const chk = await checkAppealable(item.illegalId, u.userId)
        if (chk && chk.appealable === false) {
          uni.showToast({ title: chk.message || '当前不可申诉', icon: 'none', duration: 2800 })
          return
        }
      } catch (e) {
        /* 接口校验异常时仍允许尝试提交，由后端再次校验 */
      }
      uni.showModal({
        title: '申诉理由',
        editable: true,
        placeholderText: '请填写申诉理由',
        success: async (res) => {
          if (!res.confirm) return
          const reason = (res.content || '').trim()
          if (!reason) {
            uni.showToast({ title: '理由不能为空', icon: 'none' })
            return
          }
          try {
            await submitAppeal({
              illegalId: item.illegalId,
              userId: u.userId,
              appealReason: reason,
              contactName: u.realName || u.username,
              contactPhone: u.phone || '',
              urgencyLevel: 1
            })
            uni.showToast({ title: '申诉已提交', icon: 'success' })
            this.load()
            this.tabKey = 'appeal'
          } catch (e2) {
            uni.showToast({ title: e2.message || '提交失败', icon: 'none' })
          }
        }
      })
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
    linear-gradient(125deg, #cf6d2f, #b45309);
  border-radius: 32rpx;
  padding: 32rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  box-shadow: 0 18rpx 52rpx rgba(180, 83, 9, 0.18);
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
  color: rgba(255, 249, 244, 0.96);
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
  color: rgba(255, 247, 240, 0.88);
  line-height: 1.58;
}

.hero-badge {
  min-width: 150rpx;
  padding: 18rpx 16rpx 14rpx;
  border-radius: 24rpx;
  text-align: center;
  background: rgba(255, 248, 242, 0.94);
}

.hero-badge-num {
  display: block;
  font-size: 42rpx;
  font-weight: 800;
  color: #b45309;
}

.hero-badge-text {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #7c5b48;
}

.tabs-wrap {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
  margin-bottom: 22rpx;
}

.tab {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 18rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 22rpx;
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}

.tab.on {
  border-color: rgba(180, 83, 9, 0.24);
  background: linear-gradient(135deg, #d97706, #b45309);
}

.tab-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  background: rgba(245, 248, 250, 0.96);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tab-icon.active {
  background: rgba(255, 255, 255, 0.16);
}

.tab-copy {
  flex: 1;
}

.tab-label {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #0f172a;
}

.tab-count {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #7b8b9b;
}

.tab.on .tab-label,
.tab.on .tab-count {
  color: #ffffff;
}

.empty,
.card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 28rpx;
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}

.empty {
  padding: 72rpx 36rpx;
  text-align: center;
}

.empty-icon {
  width: 104rpx;
  height: 104rpx;
  margin: 0 auto 24rpx;
  border-radius: 30rpx;
  background: rgba(65, 201, 182, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon.soft {
  background: rgba(237, 249, 246, 0.96);
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

.list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.card {
  padding: 24rpx 22rpx;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12rpx;
}

.title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  flex: 1;
}

.title-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 16rpx;
  background: rgba(245, 158, 11, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type {
  display: block;
  font-weight: 700;
  font-size: 30rpx;
  color: #0f172a;
}

.plate {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #64748b;
}

.state-chip {
  align-self: flex-start;
  font-size: 22rpx;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.c-warn {
  background: #fef3c7;
  color: #b45309;
}

.c-doing {
  background: #dbeafe;
  color: #2563eb;
}

.c-wait {
  background: #ede9fe;
  color: #7c3aed;
}

.c-done {
  background: #d1fae5;
  color: #047857;
}

.ev {
  width: 100%;
  height: 250rpx;
  border-radius: 18rpx;
  background: #f1f5f9;
  margin-top: 18rpx;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 16rpx;
}

.meta {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.ops {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 20rpx;
}

.btn-main,
.btn-sub {
  margin: 0;
  height: 76rpx;
  line-height: normal;
  border-radius: 18rpx !important;
}

.btn-main {
  min-width: 220rpx;
  background: linear-gradient(90deg, #d97706, #b45309) !important;
  border: none !important;
}

.btn-sub {
  min-width: 220rpx;
  background: rgba(246, 250, 251, 0.96) !important;
  border: 1rpx solid rgba(120, 150, 157, 0.2) !important;
}

.btn-inner {
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  color: #ffffff;
  font-size: 25rpx;
  font-weight: 700;
}

.btn-inner.sub {
  color: #476072;
}

@media screen and (max-width: 520px) {
  .hero,
  .card-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .tabs-wrap {
    grid-template-columns: 1fr;
  }

  .btn-main,
  .btn-sub {
    width: 100%;
  }
}
</style>

<template>
  <view class="page">
    <view v-if="loading" class="loading">加载中…</view>
    <view v-else-if="!detail" class="empty-wrap">
      <text class="empty">未找到申诉记录</text>
    </view>
    <view v-else>
      <view class="card">
        <view class="detail-head">
          <view class="detail-icon">
            <ev-icon name="appeal" size="34rpx" color="#0f8f83" />
          </view>
          <view>
            <text class="detail-title">申诉处理详情</text>
            <text class="detail-sub">查看审核状态、补充材料与后续操作</text>
          </view>
        </view>
        <view class="row">
          <text class="label">状态</text>
          <text :class="['chip', chipClass(detail.appealState)]">{{
            appealStateText(detail.appealState)
          }}</text>
        </view>
        <view class="row">
          <text class="label">违规</text>
          <text class="val">{{ detail.illegalTypeName || '—' }} {{ detail.licensePlate || '' }}</text>
        </view>
        <view class="row col">
          <text class="label">申诉理由</text>
          <text class="val multiline">{{ detail.appealReason || '—' }}</text>
        </view>
        <view class="row">
          <text class="label">提交时间</text>
          <text class="val">{{ formatDateTime(detail.appealTime || detail.createTime) }}</text>
        </view>
        <view v-if="detail.auditOpinion || detail.rejectReason" class="audit">
          <text class="audit-t">审核信息</text>
          <text v-if="detail.auditOpinion" class="audit-line">意见：{{ detail.auditOpinion }}</text>
          <text v-if="detail.rejectReason" class="audit-line">说明：{{ detail.rejectReason }}</text>
        </view>
      </view>

      <view v-if="evidenceUrls.length" class="card">
        <text class="sec-title">申诉材料</text>
        <view class="imgs">
          <image
            v-for="(u, i) in evidenceUrls"
            :key="i"
            :src="u"
            class="img"
            mode="aspectFill"
            @click="preview(i)"
          />
        </view>
      </view>

      <view class="actions">
        <button v-if="canCancel" class="btn secondary" @click="doCancel">撤销申诉</button>
        <button v-if="canRemind" class="btn secondary" @click="doRemind">催办</button>
        <button v-if="canUpload" class="btn secondary" @click="doUploadEvidence">补充证据</button>
        <button v-if="canResubmit" class="btn primary" @click="doResubmit">重新提交申诉</button>
      </view>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { getUser } from '../../utils/auth.js'
import {
  getAppealDetail,
  cancelAppeal,
  resubmitAppeal,
  remindAppeal,
  uploadAppealEvidence,
  uploadFile
} from '../../api/index.js'
import { fileUrl } from '../../utils/config.js'
import { formatDateTime, appealStateText } from '../../utils/display.js'

/** 与后端 AppealStateEnum 一致：0待审 1审核中 2成功 3失败 4撤销 5过期 */
const PENDING = 0
const PROCESSING = 1
const FAILED = 3

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      appealId: null,
      detail: null,
      loading: true
    }
  },
  computed: {
    evidenceUrls() {
      const s = this.detail && this.detail.appealEvidence
      if (!s) return []
      return String(s)
        .split(',')
        .map((x) => x.trim())
        .filter(Boolean)
        .map((p) => fileUrl(p))
    },
    canCancel() {
      return this.detail && this.detail.appealState === PENDING
    },
    canRemind() {
      return (
        this.detail &&
        (this.detail.appealState === PENDING || this.detail.appealState === PROCESSING)
      )
    },
    canResubmit() {
      return this.detail && this.detail.appealState === FAILED
    },
    canUpload() {
      return (
        this.detail &&
        (this.detail.appealState === PENDING || this.detail.appealState === PROCESSING)
      )
    }
  },
  onLoad(options) {
    const id = parseInt(options.id, 10)
    if (!id) {
      uni.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }
    this.appealId = id
    this.load()
  },
  methods: {
    formatDateTime,
    appealStateText,
    chipClass(code) {
      if (code === 2) return 'ok'
      if (code === 3 || code === 5) return 'bad'
      if (code === 0 || code === 1) return 'wait'
      return ''
    },
    preview(index) {
      uni.previewImage({ urls: this.evidenceUrls, current: this.evidenceUrls[index] })
    },
    async load() {
      this.loading = true
      const u = getUser()
      if (!u || !u.userId) {
        uni.reLaunch({ url: '/pages/login/login' })
        return
      }
      try {
        const d = await getAppealDetail(this.appealId)
        this.detail = d
        if (d && d.userId != null && Number(d.userId) !== Number(u.userId)) {
          uni.showToast({ title: '无权查看', icon: 'none' })
          this.detail = null
        }
      } catch (e) {
        this.detail = null
        uni.showToast({ title: e.message || '加载失败', icon: 'none' })
      }
      this.loading = false
    },
    doCancel() {
      uni.showModal({
        title: '撤销申诉',
        editable: true,
        placeholderText: '选填：撤销原因',
        success: async (res) => {
          if (!res.confirm) return
          const u = getUser()
          const reason = (res.content || '').trim() || '用户撤销'
          try {
            await cancelAppeal(this.appealId, u.userId, reason)
            uni.showToast({ title: '已撤销', icon: 'success' })
            await this.load()
          } catch (e) {
            uni.showToast({ title: e.message || '失败', icon: 'none' })
          }
        }
      })
    },
    doRemind() {
      uni.showModal({
        title: '催办处理',
        editable: true,
        placeholderText: '选填：给管理员的留言',
        success: async (res) => {
          if (!res.confirm) return
          const u = getUser()
          const msg = (res.content || '').trim()
          try {
            await remindAppeal(this.appealId, u.userId, msg || undefined)
            uni.showToast({ title: '已发送催办', icon: 'success' })
          } catch (e) {
            uni.showToast({ title: e.message || '失败', icon: 'none' })
          }
        }
      })
    },
    async doUploadEvidence() {
      const u = getUser()
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: async (r) => {
          const p = (r.tempFilePaths && r.tempFilePaths[0]) || ''
          if (!p) return
          try {
            await uploadAppealEvidence(p, this.appealId, u.userId)
            uni.showToast({ title: '已上传', icon: 'success' })
            this.load()
          } catch (e) {
            uni.showToast({ title: e.message || '上传失败', icon: 'none' })
          }
        }
      })
    },
    doResubmit() {
      uni.showModal({
        title: '重新提交',
        editable: true,
        placeholderText: '请填写新的申诉理由',
        success: async (res) => {
          if (!res.confirm) return
          const reason = (res.content || '').trim()
          if (!reason) {
            uni.showToast({ title: '理由不能为空', icon: 'none' })
            return
          }
          const u = getUser()
          const d = this.detail
          uni.showModal({
            title: '补充图片',
            content: '是否选择图片作为补充证据？',
            confirmText: '选择',
            cancelText: '跳过',
            success: async (r2) => {
              let evidence = d.appealEvidence || ''
              if (r2.confirm) {
                try {
                  const pick = await new Promise((resolve, reject) => {
                    uni.chooseImage({
                      count: 3,
                      sizeType: ['compressed'],
                      success: (x) => resolve(x.tempFilePaths || []),
                      fail: reject
                    })
                  })
                  const urls = []
                  for (const path of pick) {
                    const up = await uploadFile(path)
                    if (up.fileUrl) urls.push(up.fileUrl)
                  }
                  if (urls.length) {
                    evidence = [evidence, urls.join(',')].filter(Boolean).join(',')
                  }
                } catch (e) {
                  uni.showToast({ title: e.message || '图片上传失败', icon: 'none' })
                  return
                }
              }
              try {
                await resubmitAppeal(this.appealId, {
                  illegalId: d.illegalId,
                  userId: u.userId,
                  appealReason: reason,
                  appealEvidence: evidence || undefined,
                  contactName: u.realName || u.username,
                  contactPhone: u.phone || '',
                  urgencyLevel: d.urgencyLevel || 1
                })
                uni.showToast({ title: '已提交，请回列表查看最新申诉', icon: 'success' })
                setTimeout(() => uni.navigateBack(), 1600)
              } catch (e) {
                uni.showToast({ title: e.message || '失败', icon: 'none' })
              }
            }
          })
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
.loading,
.empty-wrap {
  text-align: center;
  padding: 48rpx;
  color: #94a3b8;
  font-size: 28rpx;
}
.card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30rpx;
  padding: 30rpx 26rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid var(--ev-border);
  box-shadow: var(--ev-shadow);
}
.detail-head {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding-bottom: 22rpx;
  margin-bottom: 8rpx;
  border-bottom: 1rpx solid #f1f5f9;
}
.detail-icon {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: var(--ev-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.detail-title {
  display: block;
  font-size: 32rpx;
  font-weight: 800;
  color: #0f172a;
}
.detail-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #64748b;
  line-height: 1.5;
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f8fafc;
}
.row:last-of-type {
  border-bottom: none;
}
.row.col {
  flex-direction: column;
}
.label {
  font-size: 24rpx;
  color: #94a3b8;
  flex-shrink: 0;
}
.val {
  font-size: 26rpx;
  color: #0f172a;
  text-align: right;
  flex: 1;
}
.val.multiline {
  text-align: left;
  line-height: 1.5;
}
.chip {
  font-size: 22rpx;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #f1f5f9;
  color: #475569;
}
.chip.ok {
  background: #d1fae5;
  color: #047857;
}
.chip.bad {
  background: #fee2e2;
  color: #b91c1c;
}
.chip.wait {
  background: #e0f2fe;
  color: #0369a1;
}
.audit {
  margin-top: 20rpx;
  padding: 22rpx;
  background: #f8fafc;
  border-radius: 18rpx;
  border: 1rpx solid #e2e8f0;
}
.audit-t {
  font-size: 24rpx;
  font-weight: 700;
  color: #334155;
  display: block;
  margin-bottom: 12rpx;
}
.audit-line {
  font-size: 24rpx;
  color: #475569;
  line-height: 1.5;
  display: block;
  margin-top: 8rpx;
}
.sec-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
  display: block;
  margin-bottom: 16rpx;
}
.imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
  background: #f1f5f9;
}
.actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 8rpx;
}
.btn {
  height: 82rpx;
  line-height: normal;
  border-radius: 18rpx;
  font-size: 28rpx;
  border: none;
}
.btn.primary {
  background: linear-gradient(120deg, #0d9488, #0f766e);
  color: #fff;
}
.btn.secondary {
  background: #fff;
  color: #0f766e;
  border: 1rpx solid #99f6e4;
}
</style>

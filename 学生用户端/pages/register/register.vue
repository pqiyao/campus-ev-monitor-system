<template>
  <view class="page">
    <view class="hero">
      <view class="hero-copy">
        <view class="hero-chip">
          <ev-icon name="shield" size="28rpx" color="#dbfff7" />
          <text>STUDENT ENROLLMENT</text>
        </view>
        <text class="hero-title">完成学生与车辆绑定</text>
        <text class="hero-sub">一次登记后，班级、车辆、定位与后续整改流程会一起串起来，不再反复填表。</text>
      </view>
      <view class="hero-side">
        <text class="hero-side-num">0{{ step }}</text>
        <text class="hero-side-txt">{{ currentStepMeta.title }}</text>
      </view>
    </view>

    <view class="steps-card">
      <view class="steps">
        <view
          v-for="item in stepMeta"
          :key="item.step"
          :class="['step-item', step >= item.step ? 'active' : '', step === item.step ? 'current' : '']"
        >
          <view class="step-dot">
            <ev-icon :name="item.icon" size="28rpx" :color="step >= item.step ? '#ffffff' : '#88a0ad'" />
          </view>
          <text class="step-name">{{ item.title }}</text>
          <text class="step-tip">{{ item.tip }}</text>
        </view>
      </view>
      <view class="step-line">
        <view class="step-line-fill" :style="{ width: ((step - 1) / 3) * 100 + '%' }"></view>
      </view>
    </view>

    <view v-if="step === 1" class="card">
      <view class="section-head">
        <view class="section-ico">
          <ev-icon name="lock" size="34rpx" color="#0b7068" />
        </view>
        <view class="section-copy">
          <text class="section-title">设置登录账号</text>
          <text class="section-sub">先创建一个后续登录学生端用的账号密码。</text>
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">用户名</text>
          <text class="field-tip">登录时使用</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="user" size="32rpx" color="#0b7068" /></view>
          <input v-model="form.username" class="input" placeholder="请输入登录用用户名" />
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">密码</text>
          <text class="field-tip">自定义</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="lock" size="32rpx" color="#0b7068" /></view>
          <input v-model="form.password" class="input" password placeholder="请输入密码" />
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">确认密码</text>
          <text class="field-tip">再次输入以确认</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="check" size="32rpx" color="#0b7068" /></view>
          <input v-model="form.password2" class="input" password placeholder="请再次输入密码" />
        </view>
      </view>

      <button class="primary-btn" type="primary" @click="next1">
        <view class="btn-inner">
          <text>填写身份信息</text>
          <ev-icon name="arrow-right" size="28rpx" color="#ffffff" />
        </view>
      </button>
    </view>

    <view v-if="step === 2" class="card">
      <view class="section-head">
        <view class="section-ico">
          <ev-icon name="school" size="34rpx" color="#0b7068" />
        </view>
        <view class="section-copy">
          <text class="section-title">补全学生资料</text>
          <text class="section-sub">这部分会用于学生身份识别、班级匹配和导员绑定。</text>
        </view>
      </view>

      <view class="field-grid">
        <view class="field">
          <view class="field-head">
            <text class="label">真实姓名</text>
            <text class="field-tip">可后补</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="user" size="32rpx" color="#0b7068" /></view>
            <input v-model="form.realName" class="input" placeholder="请输入姓名" />
          </view>
        </view>

        <view class="field">
          <view class="field-head">
            <text class="label">学号</text>
            <text class="field-tip">可后补</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="frame" size="32rpx" color="#0b7068" /></view>
            <input v-model="form.studentId" class="input" placeholder="请输入学号" />
          </view>
        </view>
      </view>

      <view class="field-grid">
        <view class="field">
          <view class="field-head">
            <text class="label">手机号</text>
            <text class="field-tip">可后补</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="phone" size="32rpx" color="#0b7068" /></view>
            <input v-model="form.phone" class="input" type="text" maxlength="11" placeholder="请输入手机号" />
          </view>
        </view>

        <view class="field">
          <view class="field-head">
            <text class="label">邮箱</text>
            <text class="field-tip">可选接收通知</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="mail" size="32rpx" color="#0b7068" /></view>
            <input v-model="form.email" class="input" placeholder="请输入邮箱" />
          </view>
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">学籍班级</text>
          <text class="field-tip required">可后补</text>
        </view>
        <picker mode="selector" :range="orgLabels" :value="orgPickIndex" @change="onOrgPick">
          <view class="select-shell">
            <view class="input-icon"><ev-icon name="class" size="32rpx" color="#0b7068" /></view>
            <view class="select-copy">
              <text v-if="form.className" class="select-value">{{ form.college }} · {{ form.major }} · {{ form.className }}</text>
              <text v-else class="select-placeholder">请选择标准班级，便于后续导员绑定</text>
            </view>
            <ev-icon name="arrow-right" size="24rpx" color="#8aa0ae" />
          </view>
        </picker>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">行政院系</text>
          <text class="field-tip">选填</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="department" size="32rpx" color="#0b7068" /></view>
          <input v-model="form.department" class="input" placeholder="可填计算机学院等" />
        </view>
      </view>

      <view class="btn-row">
        <button class="ghost-btn" @click="step = 1">
          <view class="btn-inner dark">
            <view class="btn-prev-icon">
              <ev-icon name="arrow-right" size="26rpx" color="#476072" />
            </view>
            <text>返回账号</text>
          </view>
        </button>
        <button class="primary-btn" type="primary" @click="next2">
          <view class="btn-inner">
            <text>填写车辆信息</text>
            <ev-icon name="arrow-right" size="28rpx" color="#ffffff" />
          </view>
        </button>
      </view>
    </view>

    <view v-if="step === 3" class="card">
      <view class="section-head">
        <view class="section-ico">
          <ev-icon name="bike" size="34rpx" color="#0b7068" />
        </view>
        <view class="section-copy">
          <text class="section-title">登记车辆档案</text>
          <text class="section-sub">车辆信息可先留空，后续再补充蓝牙标签和定位模块。</text>
        </view>
      </view>

      <view class="field-grid">
        <view class="field">
          <view class="field-head">
            <text class="label">车牌号</text>
            <text class="field-tip">可后补</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="bike" size="32rpx" color="#0b7068" /></view>
            <input v-model="veh.licensePlate" class="input" placeholder="请输入车牌号" />
          </view>
        </view>

        <view class="field">
          <view class="field-head">
            <text class="label">车架号</text>
            <text class="field-tip">可后补</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="frame" size="32rpx" color="#0b7068" /></view>
            <input v-model="veh.frameNum" class="input" placeholder="请输入车架号" />
          </view>
        </view>
      </view>

      <view class="field-grid">
        <view class="field">
          <view class="field-head">
            <text class="label">品牌</text>
            <text class="field-tip">可后补</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="spark" size="32rpx" color="#0b7068" /></view>
            <input v-model="veh.brand" class="input" placeholder="如 雅迪 / 爱玛" />
          </view>
        </view>

        <view class="field">
          <view class="field-head">
            <text class="label">颜色</text>
            <text class="field-tip">可后补</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="palette" size="32rpx" color="#0b7068" /></view>
            <input v-model="veh.color" class="input" placeholder="如 白色 / 黑色" />
          </view>
        </view>
      </view>

      <view class="field-grid">
        <view class="field">
          <view class="field-head">
            <text class="label">电池类型</text>
            <text class="field-tip">选填</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="battery" size="32rpx" color="#0b7068" /></view>
            <input v-model="veh.batteryType" class="input" placeholder="如 锂电 / 铅酸" />
          </view>
        </view>

        <view class="field">
          <view class="field-head">
            <text class="label">车辆类型</text>
            <text class="field-tip">选填</text>
          </view>
          <view class="input-wrap">
            <view class="input-icon"><ev-icon name="folder" size="32rpx" color="#0b7068" /></view>
            <input v-model="veh.vehicleType" class="input" placeholder="如 电动自行车" />
          </view>
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">定位模块 MAC</text>
          <text class="field-tip">后续可同步绑定</text>
        </view>
        <view class="input-wrap">
          <view class="input-icon"><ev-icon name="location" size="32rpx" color="#0b7068" /></view>
          <input v-model="veh.deviceMac" class="input" placeholder="选填，绑定车载定位模块" />
        </view>
      </view>

      <view class="field">
        <view class="field-head">
          <text class="label">车辆照片</text>
          <text class="field-tip">建议上传正侧面照片</text>
        </view>
        <view class="upload-shell" @click="pickPhoto">
          <image v-if="vehPhotoPath" :src="vehPhotoPath" class="upload-preview" mode="aspectFill" />
          <view v-else class="upload-placeholder">
            <view class="upload-icon">
              <ev-icon name="camera" size="40rpx" color="#0b7068" />
            </view>
            <text class="upload-title">上传车辆照片</text>
            <text class="upload-sub">点击选择一张清晰照片</text>
          </view>
        </view>
      </view>

      <view class="btn-row">
        <button class="ghost-btn" @click="step = 2">
          <view class="btn-inner dark">
            <view class="btn-prev-icon">
              <ev-icon name="arrow-right" size="26rpx" color="#476072" />
            </view>
            <text>返回资料</text>
          </view>
        </button>
        <button class="primary-btn" type="primary" @click="next3">
          <view class="btn-inner">
            <text>确认并提交</text>
            <ev-icon name="arrow-right" size="28rpx" color="#ffffff" />
          </view>
        </button>
      </view>
    </view>

    <view v-if="step === 4" class="card">
      <view class="section-head">
        <view class="section-ico">
          <ev-icon name="check" size="34rpx" color="#0b7068" />
        </view>
        <view class="section-copy">
          <text class="section-title">提交前确认</text>
          <text class="section-sub">核对学生信息与车辆信息，确认后将进入后台审核。</text>
        </view>
      </view>

      <view class="summary-grid">
        <view class="summary-card">
          <view class="summary-head">
            <view class="summary-badge mint">
              <ev-icon name="school" size="30rpx" color="#0b7068" />
            </view>
            <text class="summary-title">学生资料</text>
          </view>
          <text class="sum-line">用户名：{{ form.username }}</text>
          <text class="sum-line">姓名：{{ form.realName }} · 学号：{{ form.studentId }}</text>
          <text class="sum-line">手机：{{ form.phone }}</text>
          <text class="sum-line">班级：{{ form.college }} / {{ form.className }}</text>
          <text class="sum-line">院系：{{ form.department || '未填写' }}</text>
        </view>

        <view class="summary-card">
          <view class="summary-head">
            <view class="summary-badge blue">
              <ev-icon name="bike" size="30rpx" color="#2563eb" />
            </view>
            <text class="summary-title">车辆资料</text>
          </view>
          <text class="sum-line">车牌：{{ veh.licensePlate }}</text>
          <text class="sum-line">车架：{{ veh.frameNum }}</text>
          <text class="sum-line">品牌 / 颜色：{{ veh.brand }} / {{ veh.color }}</text>
          <text class="sum-line">电池 / 类型：{{ veh.batteryType || '未填' }} / {{ veh.vehicleType || '未填' }}</text>
          <text class="sum-line">定位 MAC：{{ veh.deviceMac || '未填' }}</text>
        </view>
      </view>

      <label class="agree" @tap="agreed = !agreed">
        <checkbox :checked="agreed" color="#0d9488" />
        <text>已阅读并同意用户协议与隐私说明，允许平台用于车辆管理与通知提醒。</text>
      </label>

      <button class="primary-btn" type="primary" :loading="submitting" @click="submitAll">
        <view class="btn-inner">
          <text>提交审核</text>
          <ev-icon name="check" size="28rpx" color="#ffffff" />
        </view>
      </button>

      <button class="ghost-btn single" @click="step = 3">
        <view class="btn-inner dark">
          <view class="btn-prev-icon">
            <ev-icon name="arrow-right" size="26rpx" color="#476072" />
          </view>
          <text>返回修改车辆信息</text>
        </view>
      </button>
    </view>
  </view>
</template>

<script>
import EvIcon from '../../components/ev-icon/ev-icon.vue'
import { registerUser, registerVehicle, uploadFile, getOrgClassOptions } from '../../api/index.js'

export default {
  components: {
    EvIcon
  },
  data() {
    return {
      orgOptions: [],
      orgPickIndex: 0,
      step: 1,
      agreed: false,
      submitting: false,
      vehPhotoPath: '',
      stepMeta: [
        { step: 1, title: '账号', tip: '登录凭证', icon: 'lock' },
        { step: 2, title: '资料', tip: '学生信息', icon: 'school' },
        { step: 3, title: '车辆', tip: '车辆档案', icon: 'bike' },
        { step: 4, title: '提交', tip: '审核确认', icon: 'check' }
      ],
      form: {
        username: '',
        password: '',
        password2: '',
        realName: '',
        studentId: '',
        phone: '',
        email: '',
        department: '',
        college: '',
        major: '',
        className: ''
      },
      veh: {
        licensePlate: '',
        frameNum: '',
        brand: '',
        color: '',
        batteryType: '',
        vehicleType: '',
        deviceMac: ''
      }
    }
  },
  computed: {
    currentStepMeta() {
      return this.stepMeta[this.step - 1] || this.stepMeta[0]
    },
    orgLabels() {
      return (this.orgOptions || []).map(
        (o) => `${o.college || ''} · ${o.major || ''} · ${o.className || ''}`
      )
    }
  },
  onLoad() {
    this.loadOrgOptions()
  },
  methods: {
    async loadOrgOptions() {
      try {
        const list = await getOrgClassOptions()
        this.orgOptions = Array.isArray(list) ? list : []
        if (!this.orgOptions.length) {
          uni.showToast({ title: '暂无班级数据，请联系管理员执行 org_class.sql', icon: 'none' })
        }
      } catch (e) {
        this.orgOptions = []
        uni.showToast({ title: '班级列表加载失败', icon: 'none' })
      }
    },
    onOrgPick(e) {
      const i = Number(e.detail.value)
      if (Number.isNaN(i) || i < 0) return
      this.orgPickIndex = i
      const o = this.orgOptions[i]
      if (o) {
        this.form.college = o.college || ''
        this.form.major = o.major || ''
        this.form.className = o.className || ''
      }
    },
    next1() {
      const { username, password, password2 } = this.form
      if (!username.trim() || !password) {
        uni.showToast({ title: '请填写账号密码', icon: 'none' })
        return
      }
      if (password2 && password !== password2) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return
      }
      this.step = 2
    },
    next2() {
      const phone = String(this.form.phone != null ? this.form.phone : '').trim()
      this.form.phone = phone
      this.step = 3
    },
    pickPhoto() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: (res) => {
          const p = res.tempFilePaths && res.tempFilePaths[0]
          if (p) this.vehPhotoPath = p
        }
      })
    },
    next3() {
      this.step = 4
    },
    async submitAll() {
      if (!this.agreed) {
        uni.showToast({ title: '请先勾选协议', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        const username = this.form.username.trim()
        const userPayload = {
          username,
          password: this.form.password,
          realName: this.form.realName.trim() || username,
          studentId: this.form.studentId.trim(),
          phone: String(this.form.phone != null ? this.form.phone : '').trim(),
          email: (this.form.email || '').trim(),
          department: (this.form.department || '').trim(),
          college: String(this.form.college || '').trim(),
          major: String(this.form.major || '').trim(),
          className: String(this.form.className || '').trim(),
          userType: 'student'
        }
        const user = await registerUser(userPayload)
        const userId = user.userId
        if (!userId) throw new Error('注册未返回用户ID')

        let vehiclePhoto = ''
        if (this.vehPhotoPath) {
          try {
            const up = await uploadFile(this.vehPhotoPath)
            vehiclePhoto = up.fileUrl || ''
          } catch (e) {
            console.warn(e)
          }
        }

        const hasVehicleInfo = vehiclePhoto || Object.keys(this.veh).some((key) => String(this.veh[key] || '').trim())
        if (hasVehicleInfo) {
          const fallbackSuffix = `${userId}-${Date.now() % 10000}`
          await registerVehicle({
            userId,
            licensePlate: this.veh.licensePlate.trim() || `TEMP-${fallbackSuffix}`,
            frameNum: this.veh.frameNum.trim() || `TMPF-${fallbackSuffix}`,
            brand: this.veh.brand.trim() || '未填写',
            color: this.veh.color.trim() || '未填写',
            batteryType: (this.veh.batteryType || '').trim(),
            vehicleType: (this.veh.vehicleType || '').trim(),
            vehiclePhoto: vehiclePhoto || '',
            deviceMac: (this.veh.deviceMac || '').trim()
          })
        }

        uni.showToast({ title: '提交成功，请登录', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 800)
      } catch (e) {
        uni.showToast({ title: e.message || '提交失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  padding: 24rpx 24rpx 80rpx;
  box-sizing: border-box;
}

.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  padding: 30rpx 28rpx;
  border-radius: 32rpx;
  margin-bottom: 22rpx;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.16), transparent 30%),
    linear-gradient(140deg, #0f8f83 0%, #0b7068 58%, #0a5e58 100%);
  box-shadow: 0 22rpx 58rpx rgba(11, 112, 104, 0.18);
}

.hero-copy {
  flex: 1;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 20rpx;
  color: rgba(245, 255, 252, 0.96);
  font-weight: 700;
  letter-spacing: 2rpx;
}

.hero-title {
  display: block;
  margin-top: 18rpx;
  font-size: 42rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1rpx;
}

.hero-sub {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.65;
  color: rgba(239, 255, 250, 0.88);
}

.hero-side {
  min-width: 128rpx;
  padding: 18rpx 18rpx 14rpx;
  border-radius: 24rpx;
  background: rgba(246, 255, 253, 0.96);
  text-align: center;
}

.hero-side-num {
  display: block;
  font-size: 38rpx;
  font-weight: 800;
  color: #0b7068;
}

.hero-side-txt {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #607c8d;
}

.steps-card {
  position: relative;
  padding: 24rpx 20rpx 26rpx;
  border-radius: 28rpx;
  margin-bottom: 22rpx;
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(120, 150, 157, 0.16);
  box-shadow: var(--ev-shadow);
}

.steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
}

.step-item {
  text-align: center;
  position: relative;
  z-index: 1;
}

.step-dot {
  width: 58rpx;
  height: 58rpx;
  margin: 0 auto;
  border-radius: 20rpx;
  background: #eef5f4;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-item.active .step-dot {
  background: linear-gradient(135deg, #1ab09c, #0b7068);
}

.step-name {
  display: block;
  margin-top: 12rpx;
  font-size: 23rpx;
  font-weight: 700;
  color: #607687;
}

.step-tip {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #93a7b3;
}

.step-item.active .step-name {
  color: #0f172a;
}

.step-item.current .step-name {
  color: #0b7068;
}

.step-line {
  position: absolute;
  left: 62rpx;
  right: 62rpx;
  top: 52rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: #e9f0ef;
  z-index: 0;
}

.step-line-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #26c3ad, #0b7068);
  transition: width 0.25s ease;
}

.card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 30rpx;
  padding: 30rpx 24rpx;
  box-shadow: var(--ev-shadow);
  border: 1rpx solid rgba(120, 150, 157, 0.14);
}

.section-head {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 26rpx;
}

.section-ico {
  width: 68rpx;
  height: 68rpx;
  border-radius: 22rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.section-copy {
  flex: 1;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #132436;
}

.section-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.58;
  color: #708496;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.field {
  margin-bottom: 20rpx;
}

.field-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.label {
  font-size: 24rpx;
  color: #385065;
  font-weight: 700;
}

.field-tip {
  font-size: 21rpx;
  color: #8da3b0;
}

.field-tip.required {
  color: #c66a2a;
}

.input-wrap,
.select-shell {
  display: flex;
  align-items: center;
  min-height: 92rpx;
  padding: 0 12rpx;
  border-radius: 20rpx;
  background: linear-gradient(180deg, #fbfefe, #f4faf9);
  border: 1rpx solid rgba(120, 150, 157, 0.18);
  box-sizing: border-box;
}

.input-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.input {
  flex: 1;
  height: 84rpx;
  padding: 0 16rpx;
  box-sizing: border-box;
  font-size: 27rpx;
  color: #132436;
}

.select-copy {
  flex: 1;
  padding: 0 16rpx;
}

.select-value {
  font-size: 26rpx;
  color: #132436;
  line-height: 1.48;
}

.select-placeholder {
  font-size: 25rpx;
  color: #97aab5;
  line-height: 1.48;
}

.upload-shell {
  border-radius: 24rpx;
  background: linear-gradient(180deg, #fbfefe, #f4faf9);
  border: 1rpx dashed rgba(15, 143, 131, 0.34);
  overflow: hidden;
}

.upload-preview {
  width: 100%;
  height: 280rpx;
  display: block;
}

.upload-placeholder {
  padding: 38rpx 24rpx;
  text-align: center;
}

.upload-icon {
  width: 78rpx;
  height: 78rpx;
  margin: 0 auto;
  border-radius: 22rpx;
  background: rgba(15, 143, 131, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-title {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #183247;
}

.upload-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  color: #7d93a2;
}

.btn-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 10rpx;
}

.primary-btn,
.ghost-btn {
  height: 92rpx;
  line-height: normal;
  border-radius: 20rpx !important;
}

.primary-btn {
  background: linear-gradient(90deg, #19aa98, #0f8f83, #0b7068) !important;
  border: none !important;
}

.ghost-btn {
  background: rgba(246, 250, 251, 0.96) !important;
  color: #476072 !important;
  border: 1rpx solid rgba(120, 150, 157, 0.2) !important;
}

.ghost-btn.single {
  margin-top: 14rpx;
}

.btn-inner {
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 29rpx;
  color: #ffffff;
  font-weight: 700;
}

.btn-inner.dark {
  color: #476072;
}

.btn-prev-icon {
  transform: rotate(180deg);
  display: inline-flex;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
  margin-bottom: 24rpx;
}

.summary-card {
  padding: 22rpx 20rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #fbfefe, #f6fbfa);
  border: 1rpx solid rgba(120, 150, 157, 0.16);
}

.summary-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.summary-badge {
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-badge.mint {
  background: rgba(15, 143, 131, 0.1);
}

.summary-badge.blue {
  background: rgba(59, 130, 246, 0.12);
}

.summary-title {
  font-size: 27rpx;
  font-weight: 700;
  color: #173044;
}

.sum-line {
  display: block;
  font-size: 23rpx;
  color: #51687a;
  line-height: 1.6;
  margin-bottom: 10rpx;
}

.agree {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  font-size: 23rpx;
  color: #5f7287;
  margin-bottom: 22rpx;
  line-height: 1.6;
}

@media screen and (max-width: 520px) {
  .hero,
  .field-head,
  .section-head {
    flex-direction: column;
  }

  .hero-side {
    align-self: flex-start;
  }

  .steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .step-line {
    display: none;
  }

  .field-grid,
  .btn-row,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>

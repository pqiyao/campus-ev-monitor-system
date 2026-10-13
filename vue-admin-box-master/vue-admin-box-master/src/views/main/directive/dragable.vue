<template>
  <div class="register-container page-bg-shell">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-bg">
        <div class="header-content">
          <h1 class="page-title">电动车用户注册</h1>
          <span class="page-subtitle">为新用户注册电动车信息</span>
        </div>
      </div>
    </div>

    <!-- 注册步骤 -->
    <div class="step-section">
      <div class="steps">
        <div 
          v-for="(step, index) in steps" 
          :key="step"
          :class="['step-item', { active: currentStep >= index, completed: currentStep > index }]"
        >
          <div class="step-icon">{{ index + 1 }}</div>
          <div class="step-text">{{ step }}</div>
        </div>
      </div>
    </div>

    <!-- 步骤1：车主信息 -->
    <div v-if="currentStep === 0" class="step-content">
      <div class="form-card">
        <div class="card-header">
          <h3>车主信息</h3>
          <span class="card-subtitle">填写车主基本信息</span>
        </div>
        
        <div class="form-section">
          <h4 class="section-title">身份信息</h4>
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">用户名</label>
              <input 
                v-model="userForm.username" 
                placeholder="请输入用户名" 
                class="tech-input"
                :class="{ 'error': errors.username }"
                @blur="checkUsername"
              />
              <div v-if="errors.username" class="error-message">{{ errors.username }}</div>
              <div v-if="usernameAvailable !== null && !errors.username" class="success-message">
                {{ usernameAvailable ? '用户名可用' : '用户名已存在' }}
              </div>
            </div>
            <div class="form-col">
              <label class="form-label">密码</label>
              <input 
                v-model="userForm.password" 
                type="password"
                placeholder="请输入密码" 
                class="tech-input"
                :class="{ 'error': errors.password }"
              />
              <div v-if="errors.password" class="error-message">{{ errors.password }}</div>
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">真实姓名</label>
              <input 
                v-model="userForm.realName" 
                placeholder="请输入真实姓名" 
                class="tech-input"
                :class="{ 'error': errors.realName }"
              />
              <div v-if="errors.realName" class="error-message">{{ errors.realName }}</div>
            </div>
            <div class="form-col">
              <label class="form-label">手机号码</label>
              <input 
                v-model="userForm.phone" 
                placeholder="请输入手机号码" 
                maxlength="11"
                class="tech-input"
                :class="{ 'error': errors.phone }"
                @blur="checkPhone"
              />
              <div v-if="errors.phone" class="error-message">{{ errors.phone }}</div>
              <div v-if="phoneAvailable !== null && !errors.phone" class="success-message">
                {{ phoneAvailable ? '手机号可用' : '手机号已注册' }}
              </div>
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">用户类型</label>
              <select v-model="userForm.userType" class="tech-select" @change="handleUserTypeChange">
                <option value="student">学生</option>
                <option value="teacher">教职工</option>
                <option value="service">后勤人员</option>
                <option value="temporary">临时人员</option>
              </select>
            </div>
            <div class="form-col">
              <label class="form-label">{{ userForm.userType === 'student' ? '学号' : userForm.userType === 'teacher' ? '工号' : '编号' }}</label>
              <input 
                v-model="userForm.identifier" 
                :placeholder="userForm.userType === 'student' ? '请输入学号' : userForm.userType === 'teacher' ? '请输入工号' : '请输入编号'" 
                class="tech-input"
                :class="{ 'error': errors.identifier }"
              />
              <div v-if="errors.identifier" class="error-message">{{ errors.identifier }}</div>
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">邮箱</label>
              <input 
                v-model="userForm.email" 
                placeholder="请输入邮箱" 
                class="tech-input"
                :class="{ 'error': errors.email }"
              />
              <div v-if="errors.email" class="error-message">{{ errors.email }}</div>
            </div>
            <div class="form-col" v-if="userForm.userType !== 'student'">
              <label class="form-label">部门/院系</label>
              <select v-model="userForm.department" class="tech-select">
                <option value="">请选择</option>
                <option value="计算机学院">计算机学院</option>
                <option value="软件学院">软件学院</option>
                <option value="信息工程学院">信息工程学院</option>
                <option value="商学院">商学院</option>
                <option value="文学院">文学院</option>
                <option value="理学院">理学院</option>
                <option value="后勤部">后勤部</option>
                <option value="保卫处">保卫处</option>
                <option value="教务处">教务处</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div class="form-col" v-else></div>
          </div>
          <div v-if="userForm.userType === 'student'" class="form-section" style="margin-top:12px">
            <h4 class="section-title">学籍信息（下拉与导员绑定同源）</h4>
            <div class="form-row">
              <div class="form-col" style="flex: 1 1 100%; max-width: 100%">
                <label class="form-label">学籍班级 *</label>
                <select v-model.number="studentOrgSelectIndex" class="tech-select" style="width:100%" @change="syncStudentOrgFromSelect">
                  <option :value="-1">请选择</option>
                  <option v-for="(o, i) in orgClassOptions" :key="i" :value="i">{{ o.college }} · {{ o.major }} · {{ o.className }}</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 步骤2：车辆信息 -->
    <div v-else-if="currentStep === 1" class="step-content">
      <div class="form-card">
        <div class="card-header">
          <h3>车辆信息</h3>
          <span class="card-subtitle">填写电动车基本信息</span>
        </div>
        
        <div class="form-section">
          <h4 class="section-title">基础信息</h4>
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">车牌号码</label>
              <input 
                v-model="vehicleForm.licensePlate" 
                placeholder="请输入车牌号码" 
                class="tech-input"
                maxlength="10"
                :class="{ 'error': errors.licensePlate }"
                @blur="checkLicensePlate"
              />
              <div class="form-tip">格式：湘A·12345</div>
              <div v-if="errors.licensePlate" class="error-message">{{ errors.licensePlate }}</div>
              <div v-if="licensePlateAvailable !== null && !errors.licensePlate" class="success-message">
                {{ licensePlateAvailable ? '车牌号可用' : '车牌号已注册' }}
              </div>
            </div>
            <div class="form-col">
              <label class="form-label">车架号</label>
              <input 
                v-model="vehicleForm.frameNum" 
                placeholder="请输入车架号" 
                class="tech-input"
                :class="{ 'error': errors.frameNum }"
              />
              <div class="form-tip">车辆唯一识别码</div>
              <div v-if="errors.frameNum" class="error-message">{{ errors.frameNum }}</div>
            </div>
            <div class="form-col">
              <label class="form-label">定位器 MAC（可选）</label>
              <input v-model="vehicleForm.deviceMac" placeholder="与 MQTT 上报字段 D 一致" class="tech-input" />
              <div class="form-tip">注册后写入设备表，主题可为 ev/location/{MAC}</div>
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">车辆品牌</label>
              <select v-model="vehicleForm.brand" class="tech-select">
                <option value="雅迪">雅迪</option>
                <option value="爱玛">爱玛</option>
                <option value="小牛">小牛</option>
                <option value="九号">九号</option>
                <option value="台铃">台铃</option>
                <option value="新日">新日</option>
                <option value="绿源">绿源</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div class="form-col">
              <label class="form-label">车辆颜色</label>
              <select v-model="vehicleForm.color" class="tech-select">
                <option value="白色">白色</option>
                <option value="黑色">黑色</option>
                <option value="红色">红色</option>
                <option value="蓝色">蓝色</option>
                <option value="灰色">灰色</option>
                <option value="绿色">绿色</option>
                <option value="黄色">黄色</option>
                <option value="其他">其他</option>
              </select>
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">电池类型</label>
              <select v-model="vehicleForm.batteryType" class="tech-select">
                <option value="锂电池">锂电池</option>
                <option value="铅酸电池">铅酸电池</option>
                <option value="石墨烯电池">石墨烯电池</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div class="form-col">
              <label class="form-label">车辆类型</label>
              <select v-model="vehicleForm.vehicleType" class="tech-select">
                <option value="电动自行车">电动自行车</option>
                <option value="电动摩托车">电动摩托车</option>
                <option value="电动三轮车">电动三轮车</option>
                <option value="其他">其他</option>
              </select>
            </div>
          </div>
        </div>
        
        <!-- 上传车辆照片 -->
        <div class="form-section">
          <h4 class="section-title">车辆照片</h4>
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">车辆照片</label>
              <div class="upload-area" 
                   @dragover.prevent="dragOver = true"
                   @dragleave="dragOver = false"
                   @drop="onFileDrop"
                   :class="{ 'drag-over': dragOver }"
              >
                <div class="upload-guide" v-if="!vehiclePhoto">
                  <div class="upload-icon">📷</div>
                  <div class="upload-text">拖放图片到此处，或</div>
                  <input 
                    type="file" 
                    ref="fileInput"
                    accept="image/*"
                    @change="onVehiclePhotoSelect"
                    style="display: none"
                  />
                  <div class="upload-btn" @click="triggerFileInput">选择图片</div>
                  <div class="upload-tips">
                    支持 JPG、PNG 格式，大小不超过 10MB
                  </div>
                </div>
                
                <div v-else class="photo-preview">
                  <img :src="vehiclePhotoPreview" alt="预览" class="preview-image" />
                  <div class="preview-overlay">
                    <div class="overlay-content">
                      <div class="overlay-icon" @click="triggerFileInput">📷</div>
                      <div class="overlay-text">重新选择</div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="errors.vehiclePhoto" class="error-message">{{ errors.vehiclePhoto }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 步骤3：提交审核 -->
    <div v-else-if="currentStep === 2" class="step-content">
      <div class="form-card">
        <div class="card-header">
          <h3>提交审核</h3>
          <span class="card-subtitle">确认注册信息并提交审核</span>
        </div>
        
        <div class="review-section">
          <h4 class="section-title">信息总览</h4>
          
          <div class="review-block">
            <h5>车主信息</h5>
            <div class="review-grid">
              <div class="review-item">
                <div class="review-label">用户名</div>
                <div class="review-value">{{ userForm.username }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">真实姓名</div>
                <div class="review-value">{{ userForm.realName }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">用户类型</div>
                <div class="review-tag" :class="userForm.userType">
                  {{ getUserTypeText(userForm.userType) }}
                </div>
              </div>
              <div class="review-item">
                <div class="review-label">{{ userForm.userType === 'student' ? '学号' : userForm.userType === 'teacher' ? '工号' : '编号' }}</div>
                <div class="review-value">{{ userForm.identifier }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">手机号码</div>
                <div class="review-value">{{ userForm.phone }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">邮箱</div>
                <div class="review-value">{{ userForm.email || '未填写' }}</div>
              </div>
              <div class="review-item" v-if="userForm.userType !== 'student'">
                <div class="review-label">部门/院系</div>
                <div class="review-value">{{ userForm.department || '未选择' }}</div>
              </div>
              <template v-else>
                <div class="review-item">
                  <div class="review-label">学院</div>
                  <div class="review-value">{{ userForm.college || '—' }}</div>
                </div>
                <div class="review-item">
                  <div class="review-label">专业</div>
                  <div class="review-value">{{ userForm.major || '—' }}</div>
                </div>
                <div class="review-item">
                  <div class="review-label">班级</div>
                  <div class="review-value">{{ userForm.className || '—' }}</div>
                </div>
              </template>
            </div>
          </div>
          
          <div class="review-block">
            <h5>车辆信息</h5>
            <div class="review-grid">
              <div class="review-item">
                <div class="review-label">车牌号</div>
                <div class="review-value">{{ vehicleForm.licensePlate }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">车架号</div>
                <div class="review-value">{{ vehicleForm.frameNum }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">定位器MAC</div>
                <div class="review-value">{{ vehicleForm.deviceMac || '—' }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">车辆品牌</div>
                <div class="review-value">{{ vehicleForm.brand }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">车辆颜色</div>
                <div class="review-value">{{ vehicleForm.color }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">电池类型</div>
                <div class="review-value">{{ vehicleForm.batteryType }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">车辆类型</div>
                <div class="review-value">{{ vehicleForm.vehicleType }}</div>
              </div>
              <div class="review-item">
                <div class="review-label">车辆照片</div>
                <div class="review-value">{{ vehiclePhoto ? '已上传' : '未上传' }}</div>
              </div>
            </div>
          </div>
          
          <div class="agreement-section">
            <div class="checkbox-option" :class="{ active: agreed }" @click="agreed = !agreed">
              <div class="checkbox-box"></div>
              <div class="checkbox-text">
                我已阅读并同意
                <span class="agreement-link" @click.stop="showAgreement">《校园电动车管理协议》</span>
              </div>
            </div>
            <div v-if="errors.agreement" class="error-message">{{ errors.agreement }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <div class="action-btn" v-if="currentStep > 0" @click="prevStep">
        ← 上一步
      </div>
      
      <div class="action-btn primary" v-if="currentStep < 2" @click="nextStep">
        下一步 →
      </div>
      
      <div 
        class="action-btn success" 
        v-if="currentStep === 2"
        @click="submitRegistration"
        :class="{ disabled: !agreed || isSubmitting }"
      >
        {{ isSubmitting ? '提交中...' : '提交审核' }}
      </div>
      
      <div class="action-btn" v-if="currentStep < 2" @click="saveDraft">
        保存草稿
      </div>
      
      <div class="action-btn danger" @click="resetForm">
        重置
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()

// 配置axios
const API_BASE_URL = '/api'
// 从localStorage获取token
const token = localStorage.getItem('token')
if (token) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

const orgClassOptions = ref([])
const studentOrgSelectIndex = ref(-1)

const loadOrgClassOptions = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/org-class/options`)
    if (res.data.code === 200) orgClassOptions.value = res.data.data || []
  } catch {
    orgClassOptions.value = []
  }
}

const syncStudentOrgFromSelect = () => {
  const i = studentOrgSelectIndex.value
  if (i < 0) {
    userForm.college = ''
    userForm.major = ''
    userForm.className = ''
    return
  }
  const o = orgClassOptions.value[i]
  if (o) {
    userForm.college = o.college || ''
    userForm.major = o.major || ''
    userForm.className = o.className || ''
  }
}

// 当前步骤
const currentStep = ref(0)
const steps = ['车主信息', '车辆信息', '提交审核']

// 用户表单
const userForm = reactive({
  username: '',
  password: '123456',
  realName: '',
  userType: 'student',
  identifier: '',
  phone: '',
  email: '',
  department: '',
  college: '',
  major: '',
  className: ''
})

// 车辆表单
const vehicleForm = reactive({
  licensePlate: '',
  frameNum: '',
  deviceMac: '',
  brand: '雅迪',
  color: '白色',
  batteryType: '锂电池',
  vehicleType: '电动自行车',
  vehiclePhoto: ''
})

// 文件上传相关
const dragOver = ref(false)
const vehiclePhoto = ref(null)
const vehiclePhotoPreview = ref('')
const fileInput = ref(null)

// 验证状态
const usernameAvailable = ref(null)
const phoneAvailable = ref(null)
const licensePlateAvailable = ref(null)

// 其他状态
const agreed = ref(false)
const isSubmitting = ref(false)
const errors = reactive({})

// 检查用户名是否可用
const checkUsername = async () => {
  if (!userForm.username.trim()) {
    usernameAvailable.value = null
    return
  }
  
  try {
    // 这里需要一个检查用户名是否存在的接口
    // 如果没有专用接口，可以尝试调用用户列表接口进行筛选
    const response = await axios.get(`${API_BASE_URL}/user/list`, {
      params: {
        keyword: userForm.username,
        pageSize: 1
      }
    })
    
    if (response.data.code === 200) {
      const users = response.data.data || []
      const exists = users.some(user => user.username === userForm.username)
      usernameAvailable.value = !exists
    }
  } catch (error) {
    console.error('检查用户名失败:', error)
  }
}

// 检查手机号是否可用
const checkPhone = async () => {
  if (!userForm.phone.trim()) {
    phoneAvailable.value = null
    return
  }
  
  try {
    // 检查手机号是否已注册
    const response = await axios.get(`${API_BASE_URL}/user/list`, {
      params: {
        keyword: userForm.phone,
        pageSize: 1
      }
    })
    
    if (response.data.code === 200) {
      const users = response.data.data || []
      const exists = users.some(user => user.phone === userForm.phone)
      phoneAvailable.value = !exists
    }
  } catch (error) {
    console.error('检查手机号失败:', error)
  }
}

// 检查车牌号是否可用
const checkLicensePlate = async () => {
  if (!vehicleForm.licensePlate.trim()) {
    licensePlateAvailable.value = null
    return
  }
  
  try {
    // 检查车牌号是否已注册
    const response = await axios.get(`${API_BASE_URL}/vehicle/list`, {
      params: {
        licensePlate: vehicleForm.licensePlate,
        pageSize: 1
      }
    })
    
    if (response.data.code === 200) {
      const vehicles = response.data.data || []
      const exists = vehicles.some(vehicle => vehicle.licensePlate === vehicleForm.licensePlate)
      licensePlateAvailable.value = !exists
    }
  } catch (error) {
    console.error('检查车牌号失败:', error)
  }
}

// 表单验证
const validateStep = (step) => {
  Object.keys(errors).forEach(key => delete errors[key])
  
  let isValid = true
  
  switch (step) {
    case 0:
      if (!userForm.username?.trim()) {
        errors.username = '请输入用户名'
        isValid = false
      } else if (userForm.username.length < 3) {
        errors.username = '用户名至少3个字符'
        isValid = false
      } else if (usernameAvailable.value === false) {
        errors.username = '用户名已存在'
        isValid = false
      }
      
      if (!userForm.password?.trim()) {
        errors.password = '请输入密码'
        isValid = false
      } else if (userForm.password.length < 6) {
        errors.password = '密码至少6个字符'
        isValid = false
      }
      
      if (!userForm.realName?.trim()) {
        errors.realName = '请输入真实姓名'
        isValid = false
      } else if (userForm.realName.length < 2) {
        errors.realName = '姓名至少2个字符'
        isValid = false
      }
      
      if (!userForm.phone?.trim()) {
        errors.phone = '请输入手机号码'
        isValid = false
      } else if (!/^1[3-9]\d{9}$/.test(userForm.phone)) {
        errors.phone = '请输入正确的手机号码'
        isValid = false
      } else if (phoneAvailable.value === false) {
        errors.phone = '手机号已注册'
        isValid = false
      }
      
// 修改后的验证逻辑（以临时人员为例）
      if (!userForm.identifier?.trim() && userForm.userType !== 'temporary') {
        errors.identifier = userForm.userType === 'student' ? '请输入学号' : userForm.userType === 'teacher' ? '请输入工号' : '请输入编号'
        isValid = false
      }
            
      if (userForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userForm.email)) {
        errors.email = '邮箱格式不正确'
        isValid = false
      }

      if (userForm.userType === 'student') {
        if (!userForm.className?.trim()) {
          errors.className = '请从下拉选择学籍班级'
          isValid = false
        }
      }
      break
      
    case 1:
      if (!vehicleForm.licensePlate?.trim()) {
        errors.licensePlate = '请输入车牌号码'
        isValid = false
      } else if (!/^[\u4e00-\u9fa5]{1}[A-Z]{1}·[A-Z0-9]{5,6}$/.test(vehicleForm.licensePlate)) {
        errors.licensePlate = '车牌格式不正确，示例：湘A·12345'
        isValid = false
      } else if (licensePlateAvailable.value === false) {
        errors.licensePlate = '车牌号已注册'
        isValid = false
      }
      
      if (!vehicleForm.frameNum?.trim()) {
        errors.frameNum = '请输入车架号'
        isValid = false
      } else if (vehicleForm.frameNum.length < 6) {
        errors.frameNum = '车架号至少6位'
        isValid = false
      }
      
      // 车辆照片不是必须的
      // if (!vehiclePhoto.value) {
      //   errors.vehiclePhoto = '请上传车辆照片'
      //   isValid = false
      // }
      break
      
    case 2:
      if (!agreed.value) {
        errors.agreement = '请同意相关协议'
        isValid = false
      }
      break
  }
  
  if (!isValid) {
    ElMessage.error('请完善当前步骤的信息')
  }
  
  return isValid
}

// 文件上传方法
const triggerFileInput = () => {
  fileInput.value.click()
}

const onVehiclePhotoSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    validateAndLoadFile(file)
  }
}

const onFileDrop = (event) => {
  event.preventDefault()
  dragOver.value = false
  const file = event.dataTransfer.files[0]
  if (file) {
    validateAndLoadFile(file)
  }
}

const validateAndLoadFile = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件（JPG、PNG格式）')
    return
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过10MB')
    return
  }
  
  vehiclePhoto.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    vehiclePhotoPreview.value = e.target.result
  }
  reader.readAsDataURL(file)
}

// 上传文件到服务器
const uploadFile = async (file) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', 'image')
    
    const response = await axios.post(`${API_BASE_URL}/file/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (response.data.code === 200) {
      return response.data.data.fileUrl   // 返回 fileUrl
    } else {
      throw new Error(response.data.message || '文件上传失败')
    }
  } catch (error) {
    console.error('文件上传失败:', error)
    throw error
  }
}

// 步骤切换
const nextStep = () => {
  if (validateStep(currentStep.value)) {
    if (currentStep.value < 2) {
      currentStep.value++
    }
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 保存草稿
const saveDraft = () => {
  const draft = {
    userForm,
    vehicleForm,
    currentStep: currentStep.value,
    vehiclePhotoPreview: vehiclePhotoPreview.value
  }
  localStorage.setItem('vehicle_registration_draft', JSON.stringify(draft))
  ElMessage.success('草稿已保存')
}

// 重置表单
const resetForm = () => {
  ElMessageBox.confirm('确定要重置所有表单内容吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    Object.keys(userForm).forEach(key => {
      userForm[key] = ''
    })
    userForm.userType = 'student'
    userForm.password = '123456'
    studentOrgSelectIndex.value = -1
    
    Object.keys(vehicleForm).forEach(key => {
      vehicleForm[key] = ''
    })
    vehicleForm.brand = '雅迪'
    vehicleForm.color = '白色'
    vehicleForm.batteryType = '锂电池'
    vehicleForm.vehicleType = '电动自行车'
    
    currentStep.value = 0
    agreed.value = false
    vehiclePhoto.value = null
    vehiclePhotoPreview.value = ''
    usernameAvailable.value = null
    phoneAvailable.value = null
    licensePlateAvailable.value = null
    
    localStorage.removeItem('vehicle_registration_draft')
    
    ElMessage.success('表单已重置')
  })
}

// 获取用户类型文本
const getUserTypeText = (type) => {
  const map = {
    student: '学生',
    teacher: '教职工',
    service: '后勤人员',
    temporary: '临时人员',
    admin: '管理员'
  }
  return map[type] || type
}

// 用户类型变更处理
const handleUserTypeChange = () => {
  userForm.department = ''
  userForm.college = ''
  userForm.major = ''
  userForm.className = ''
  studentOrgSelectIndex.value = -1
  userForm.identifier = ''
}

// 显示协议
const showAgreement = () => {
  ElMessageBox.alert(
    '校园电动车管理协议\n\n' +
    '第一章 总则\n' +
    '第一条 为规范校园电动车管理，保障师生安全，特制定本协议。\n' +
    '第二条 所有在校园内使用的电动车必须在平台注册登记。\n\n' +
    '第二章 注册登记\n' +
    '第三条 电动车车主需提供真实有效的身份信息和车辆信息。\n' +
    '第四条 车辆必须符合国家安全标准，严禁改装车辆。\n\n' +
    '第三章 使用规范\n' +
    '第五条 必须遵守校园交通安全规定，限速15公里/小时。\n' +
    '第六条 禁止在禁行区域行驶，禁止违规停放。\n' +
    '第七条 禁止违规充电，禁止飞线充电。\n\n' +
    '第四章 违规处理\n' +
    '第八条 违规行为将根据情节轻重给予警告、扣分、冻结、加入黑名单等处罚。\n' +
    '第九条 累计扣分达到12分将取消校园通行资格。\n\n' +
    '第五章 附则\n' +
    '第十条 本协议最终解释权归学校所有。',
    '校园电动车管理协议',
    {
      confirmButtonText: '我已知晓',
      customClass: 'agreement-modal'
    }
  )
}

// 提交注册
const submitRegistration = async () => {
  if (!validateStep(2)) return
  
  isSubmitting.value = true
  
  try {
    // 1. 构造用户注册数据
    const userData = {
      username: userForm.username,
      password: userForm.password,
      realName: userForm.realName,
      userType: userForm.userType,
      phone: userForm.phone,
      email: userForm.email,
      department: userForm.userType === 'student'
        ? (userForm.department?.trim() || userForm.college?.trim() || null)
        : userForm.department,
      college: userForm.userType === 'student' ? userForm.college?.trim() : undefined,
      major: userForm.userType === 'student' ? userForm.major?.trim() : undefined,
      className: userForm.userType === 'student' ? userForm.className?.trim() : undefined
    }
    
    // 根据用户类型添加不同的标识字段
    if (userForm.userType === 'student') {
      userData.studentId = userForm.identifier
    } else if (userForm.userType === 'teacher') {
      userData.jobId = userForm.identifier
    } else {
      userData.jobId = userForm.identifier
    }
    
    // 2. 注册用户
    const userResponse = await axios.post(`${API_BASE_URL}/user/register`, userData)
    
    if (userResponse.data.code !== 200) {
      throw new Error(userResponse.data.message || '用户注册失败')
    }
    
    // 获取用户ID（注册接口返回完整用户对象；兼容数字/字符串）
    const rawId = userResponse.data.data?.userId ?? userResponse.data.data?.user_id
    let userId = rawId != null && rawId !== '' ? Number(rawId) : null
    if (userId != null && Number.isNaN(userId)) {
      userId = null
    }
    if (!userId) {
      // 如果后端没有返回userId，可能需要通过用户名获取用户ID
      const userListResponse = await axios.get(`${API_BASE_URL}/user/list`, {
        params: { keyword: userForm.username, pageSize: 1 }
      })
      
      if (userListResponse.data.code === 200 && userListResponse.data.data.length > 0) {
        userId = userListResponse.data.data[0].userId
      }
    }
    
    if (!userId) {
      throw new Error('无法获取用户ID')
    }
    
    // 3. 上传车辆照片（如果有）
    // 上传车辆照片
    let vehiclePhotoUrl = ''
    if (vehiclePhoto.value) {
      try {
        vehiclePhotoUrl = await uploadFile(vehiclePhoto.value)
        console.log('车辆照片上传成功，URL:', vehiclePhotoUrl)  // 调试输出
      } catch (uploadError) {
        console.error('上传车辆照片失败:', uploadError)
        ElMessage.error('车辆照片上传失败，请重试')
        isSubmitting.value = false
        return
      }
    }

    // 车辆注册数据
    const vehicleData = {
      userId: userId,
      licensePlate: vehicleForm.licensePlate,
      brand: vehicleForm.brand,
      color: vehicleForm.color,
      frameNum: vehicleForm.frameNum,
      batteryType: vehicleForm.batteryType,
      vehicleType: vehicleForm.vehicleType,
      vehiclePhoto: vehiclePhotoUrl   // 关键：传入 URL
    }
    if (vehicleForm.deviceMac?.trim()) {
      vehicleData.deviceMac = vehicleForm.deviceMac.trim()
    }

    const vehicleResponse = await axios.post(`${API_BASE_URL}/vehicle/register`, vehicleData)
    
    if (vehicleResponse.data.code !== 200) {
      throw new Error(vehicleResponse.data.message || '车辆注册失败')
    }
    
    // 清除本地草稿
    localStorage.removeItem('vehicle_registration_draft')
    
    ElMessage.success({
      message: '注册信息已提交审核，审核结果将通过短信通知。',
      duration: 5000
    })
    
    // 重置表单
    setTimeout(() => {
      resetForm()
      router.push('/vehicle-management')
    }, 2000)
    
  } catch (error) {
    console.error('提交失败:', error)
    let errorMessage = '提交失败，请稍后重试'
    
    if (error.response) {
      // 处理HTTP错误（后端 ApiResponse: code + message）
      const data = error.response.data
      if (data && data.message) {
        errorMessage = data.message
      } else if (data && typeof data.code === 'number' && data.code !== 200) {
        errorMessage = data.message || `错误码 ${data.code}`
      } else if (error.response.status === 400) {
        errorMessage = '请求参数错误'
      } else if (error.response.status === 401) {
        errorMessage = '登录已过期，请重新登录'
        router.push('/login')
      } else if (error.response.status === 403) {
        errorMessage = '权限不足'
      } else if (error.response.status === 404) {
        errorMessage = '接口不存在'
      } else if (error.response.status === 500) {
        errorMessage = '服务器内部错误'
      }
    } else if (error.request) {
      errorMessage = '网络连接失败，请检查网络'
    } else {
      errorMessage = error.message || '提交失败'
    }
    
    ElMessage.error(errorMessage)
  } finally {
    isSubmitting.value = false
  }
}

// 页面加载时检查草稿
onMounted(() => {
  loadOrgClassOptions()
  const draft = localStorage.getItem('vehicle_registration_draft')
  if (draft) {
    ElMessageBox.confirm('检测到未完成的注册草稿，是否加载？', '提示', {
      confirmButtonText: '加载',
      cancelButtonText: '不加载',
      type: 'info'
    }).then(() => {
      try {
        const parsed = JSON.parse(draft)
        Object.assign(userForm, parsed.userForm)
        Object.assign(vehicleForm, parsed.vehicleForm)
        currentStep.value = parsed.currentStep
        vehiclePhotoPreview.value = parsed.vehiclePhotoPreview || ''
        
        // 重新检查可用性
        if (userForm.username) checkUsername()
        if (userForm.phone) checkPhone()
        if (vehicleForm.licensePlate) checkLicensePlate()
        
        ElMessage.success('已加载草稿')
      } catch (error) {
        console.error('加载草稿失败:', error)
        ElMessage.error('加载草稿失败')
      }
    })
  }
})
</script>

<style scoped>
/* 样式与之前相同，添加一些新样式 */
.success-message {
  color: #4caf50;
  font-size: 12px;
  margin-top: 4px;
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.agreement-modal {
  max-width: 600px;
}

.agreement-modal .el-message-box__content {
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-line;
  line-height: 1.6;
}

/* 样式与之前的相同，保持不变 */
.register-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0e29 0%, #1a1f3c 100%);
  color: #ffffff;
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-bg {
  background: linear-gradient(90deg, rgba(0, 123, 255, 0.1), rgba(102, 16, 242, 0.1));
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(0, 123, 255, 0.2);
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: bold;
  background: linear-gradient(90deg, #ffffff, #00d4ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.step-section {
  margin-bottom: 30px;
}

.steps {
  display: flex;
  justify-content: space-between;
  position: relative;
}

.steps::before {
  content: '';
  position: absolute;
  top: 24px;
  left: 60px;
  right: 60px;
  height: 2px;
  background: rgba(255, 255, 255, 0.1);
  z-index: 1;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 2;
}

.step-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.step-item.active .step-icon {
  background: linear-gradient(90deg, #007bff, #6610f2);
  border-color: #00d4ff;
  color: #ffffff;
}

.step-item.completed .step-icon {
  background: linear-gradient(90deg, #28a745, #20c997);
  border-color: #28a745;
  color: #ffffff;
}

.step-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.step-item.active .step-text {
  color: #ffffff;
  font-weight: 500;
}

.step-content {
  margin-bottom: 30px;
}

.form-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: #ffffff;
  font-weight: 600;
}

.card-subtitle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.form-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #00d4ff;
  font-weight: 500;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.tech-input {
  width: 100%;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
  font-size: 14px;
  transition: all 0.3s ease;
}

.tech-input:focus {
  outline: none;
  border-color: #00d4ff;
  box-shadow: 0 0 0 2px rgba(0, 212, 255, 0.2);
}

.tech-select {
  width: 100%;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
  font-size: 14px;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='white' viewBox='0 0 16 16'%3E%3Cpath d='M7.247 11.14L2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
}

.form-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.tech-input.error {
  border-color: #f56c6c;
}

.upload-area {
  min-height: 200px;
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.02);
}

.upload-area.drag-over {
  border-color: #00d4ff;
  background: rgba(0, 212, 255, 0.05);
}

.upload-guide {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  width: 100%;
  padding: 20px;
}

.upload-icon {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.3);
  margin-bottom: 16px;
}

.upload-text {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  margin-bottom: 8px;
  font-weight: 500;
}

.upload-btn {
  display: inline-block;
  padding: 10px 24px;
  background: linear-gradient(90deg, #007bff, #6610f2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  margin-bottom: 8px;
}

.upload-btn:hover {
  transform: translateY(-2px);
}

.upload-tips {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.photo-preview {
  width: 100%;
  height: 100%;
  position: relative;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.photo-preview:hover .preview-overlay {
  opacity: 1;
}

.overlay-content {
  text-align: center;
}

.overlay-icon {
  font-size: 32px;
  margin-bottom: 8px;
  cursor: pointer;
  color: #ffffff;
  transition: transform 0.3s ease;
}

.overlay-icon:hover {
  transform: scale(1.2);
}

.overlay-text {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.review-section {
  padding: 10px 0;
}

.review-block {
  margin-bottom: 25px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.review-block h5 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #00d4ff;
  font-weight: 500;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.review-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.review-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.review-value {
  font-size: 15px;
  color: #ffffff;
  font-weight: 500;
}

.review-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.review-tag.student {
  background: rgba(0, 123, 255, 0.2);
  color: #007bff;
  border: 1px solid rgba(0, 123, 255, 0.4);
}

.review-tag.teacher {
  background: rgba(40, 167, 69, 0.2);
  color: #28a745;
  border: 1px solid rgba(40, 167, 69, 0.4);
}

.review-tag.service {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
  border: 1px solid rgba(255, 193, 7, 0.4);
}

.agreement-section {
  margin-top: 30px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  text-align: center;
}

.checkbox-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.checkbox-option.active {
  background: rgba(0, 212, 255, 0.2);
  border-color: rgba(0, 212, 255, 0.4);
}

.checkbox-option:hover {
  background: rgba(255, 255, 255, 0.1);
}

.checkbox-box {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  position: relative;
  transition: all 0.3s ease;
}

.checkbox-option.active .checkbox-box {
  background: #00d4ff;
  border-color: #00d4ff;
}

.checkbox-option.active .checkbox-box::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.checkbox-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.agreement-link {
  color: #00d4ff;
  cursor: pointer;
  text-decoration: underline;
}

.agreement-link:hover {
  color: #ffffff;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 30px;
}

.action-btn {
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
}

.action-btn.primary {
  background: linear-gradient(90deg, #007bff, #6610f2);
  border: none;
  color: #ffffff;
}

.action-btn.success {
  background: linear-gradient(90deg, #28a745, #20c997);
  border: none;
  color: #ffffff;
}

.action-btn.danger {
  background: linear-gradient(90deg, rgba(220, 53, 69, 0.8), rgba(220, 53, 69, 0.6));
  border: none;
  color: #ffffff;
}

.action-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.disabled:hover {
  transform: none;
  box-shadow: none;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 15px;
  }
  
  .review-grid {
    grid-template-columns: 1fr;
  }
  
  .steps {
    flex-wrap: wrap;
    gap: 20px;
  }
  
  .steps::before {
    display: none;
  }
  
  .action-buttons {
    flex-wrap: wrap;
  }
  
  .action-btn {
    flex: 1;
    min-width: 120px;
    justify-content: center;
  }
}
</style>

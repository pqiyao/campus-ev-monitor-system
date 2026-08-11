import { request, requestPage, postForm } from '../utils/request.js'
import { BASE_URL } from '../utils/config.js'
import { getToken } from '../utils/auth.js'

/** 与后端、大屏统一的百度 AK 等（无需登录） */
export function getClientConfig() {
  return request({ url: '/api/client/config', method: 'GET' })
}

export function login(username, password) {
  return request({
    url: '/api/user/login',
    method: 'POST',
    data: { username, password }
  })
}

/** 与后台 passwordLayer 一致：Query userId、oldPassword、newPassword */
export function updatePassword(userId, oldPassword, newPassword) {
  return request({
    url: '/api/user/update-password',
    method: 'POST',
    params: { userId, oldPassword, newPassword },
    data: {}
  })
}

export function registerUser(payload) {
  return request({
    url: '/api/user/register',
    method: 'POST',
    data: payload
  })
}

/** 学籍班级下拉（标准库 + 已有学生班级），注册页无需登录 */
export function getOrgClassOptions() {
  return request({ url: '/api/org-class/options', method: 'GET' })
}

export function registerVehicle(payload) {
  return request({
    url: '/api/vehicle/register',
    method: 'POST',
    data: payload
  })
}

export function getDashboard(userId) {
  return request({ url: '/api/user/dashboard/' + userId, method: 'GET' })
}

/** 12 分制概要：remaining / maxPoints */
export function getLicenseSummary(userId) {
  return request({ url: '/api/license-point/summary', method: 'GET', params: { userId } })
}

export function getLicensePointLog(userId, pageNum = 1, pageSize = 30) {
  return requestPage({
    url: '/api/license-point/log',
    params: { userId, pageNum, pageSize }
  })
}

export function submitWelfarePoint(userId, content, imageUrls) {
  return request({
    url: '/api/license-point/welfare/submit',
    method: 'POST',
    data: { userId, content, imageUrls: imageUrls || '' }
  })
}

export function getMyWelfareRequests(userId) {
  return request({ url: '/api/license-point/welfare/my', method: 'GET', params: { userId } })
}

/** @param {number} [userId] 传入时只查该用户的车辆（需后端支持） */
export function getVehicleList(pageNum = 1, pageSize = 50, userId) {
  const params = { pageNum, pageSize }
  if (userId != null && userId !== '') params.userId = userId
  return requestPage({
    url: '/api/vehicle/list',
    params
  })
}

export function deleteVehicle(vehicleId) {
  return request({ url: '/api/vehicle/' + vehicleId, method: 'DELETE' })
}

export function getLocationByPlate(licensePlate) {
  const q = encodeURIComponent(licensePlate)
  return request({ url: '/api/location/' + q, method: 'GET' })
}

/** 与大屏同源的全部实时点位，包含模拟点、绑定终端点和车辆点 */
export function getAllLocations() {
  return request({ url: '/api/location/all', method: 'GET' })
}

/** 已启用电子围栏（学生端地图叠加禁行/停车区等边界） */
export function getGeofenceEnabledList() {
  return request({ url: '/api/geofence/enabled', method: 'GET' })
}

export function listViolationsByPlate(licensePlate, pageSize = 50) {
  return requestPage({
    url: '/api/violation/list',
    params: { vehiclePlate: licensePlate, pageNum: 1, pageSize }
  })
}

export function startRectify(violationId, userId, reason) {
  const q =
    'userId=' +
    encodeURIComponent(userId) +
    (reason ? '&reason=' + encodeURIComponent(reason) : '')
  return request({
    url: '/api/violation/' + violationId + '/rectify?' + q,
    method: 'POST',
    data: {}
  })
}

export function submitRectify(rectifyId, photos, description) {
  return postForm('/api/violation/rectify/' + rectifyId + '/submit', {
    photos,
    description: description || '整改佐证'
  })
}

export function submitAppeal(body) {
  return request({
    url: '/api/appeal/submit',
    method: 'POST',
    data: body
  })
}

/** 发起申诉前校验：时限、次数、是否已有进行中申诉、是否本人车辆等 */
export function checkAppealable(illegalId, userId) {
  return request({
    url: '/api/appeal/check',
    method: 'GET',
    params: { illegalId, userId }
  })
}

export function getUserAppeals(userId, pageNum = 1, pageSize = 20) {
  return requestPage({
    url: '/api/appeal/user/' + userId,
    params: { pageNum, pageSize }
  })
}

export function getAppealDetail(appealId) {
  return request({
    url: '/api/appeal/' + appealId,
    method: 'GET'
  })
}

/** 仅「待审核」可撤；后端会把关联违规从申诉中恢复为待处理 */
export function cancelAppeal(appealId, userId, reason) {
  const q =
    'appealId=' +
    encodeURIComponent(appealId) +
    '&userId=' +
    encodeURIComponent(userId) +
    (reason ? '&reason=' + encodeURIComponent(reason) : '')
  return request({
    url: '/api/appeal/cancel?' + q,
    method: 'POST',
    data: {}
  })
}

/** 仅「申诉失败」记录可重提；会生成一条新的待审核申诉 */
export function resubmitAppeal(appealId, body) {
  return request({
    url: '/api/appeal/resubmit?appealId=' + encodeURIComponent(appealId),
    method: 'POST',
    data: body
  })
}

/** 待审核/审核中可催办，通知管理员 */
export function remindAppeal(appealId, userId, message) {
  let q =
    'appealId=' +
    encodeURIComponent(appealId) +
    '&userId=' +
    encodeURIComponent(userId)
  if (message) q += '&message=' + encodeURIComponent(message)
  return request({
    url: '/api/appeal/remind?' + q,
    method: 'POST',
    data: {}
  })
}

/** 个人申诉统计（可选，用于服务页摘要） */
export function getUserAppealStats(userId) {
  return request({
    url: '/api/appeal/stats/user/' + userId,
    method: 'GET'
  })
}

export function uploadAppealEvidence(filePath, appealId, userId) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header = {}
    if (token) header.Authorization = 'Bearer ' + token
    uni.uploadFile({
      url: BASE_URL.replace(/\/$/, '') + '/api/appeal/upload-evidence',
      filePath,
      name: 'file',
      formData: {
        appealId: String(appealId),
        userId: String(userId)
      },
      header,
      success: (res) => {
        try {
          const body = JSON.parse(res.data || '{}')
          if (body.code !== 200) {
            reject(new Error(body.message || '上传失败'))
            return
          }
          resolve(body.data)
        } catch (e) {
          reject(new Error('上传解析失败'))
        }
      },
      fail: (e) => reject(e || new Error('上传失败'))
    })
  })
}

export function listRectify(licensePlate, pageSize = 30) {
  return requestPage({
    url: '/api/rectify/list',
    params: { licensePlate, pageNum: 1, pageSize }
  })
}

export function listEtcCards(keyword, pageSize = 30) {
  return requestPage({
    url: '/api/etc-card/list',
    params: { keyword: keyword || undefined, pageNum: 1, pageSize }
  })
}

export function setEtcStatus(etcId, status) {
  return request({
    url: '/api/etc-card/' + etcId + '/status?status=' + encodeURIComponent(status),
    method: 'POST',
    data: {}
  })
}

export function getUserNotifications(userId, limit = 20) {
  return request({
    url: '/api/notification/user/' + userId,
    method: 'GET',
    params: { limit }
  })
}

export function getNotificationDetail(notificationId, userId) {
  return request({
    url: '/api/notification/' + notificationId,
    method: 'GET',
    params: { userId }
  })
}

export function markNotificationRead(notificationId, userId) {
  return request({
    url: '/api/notification/' + notificationId + '/read',
    method: 'POST',
    data: {},
    params: { userId }
  })
}

/** 从当前用户收件箱移除（非管理端删除） */
export function hideNotificationForUser(notificationId, userId) {
  return request({
    url: '/api/notification/' + notificationId + '/hide',
    method: 'POST',
    data: {},
    params: { userId }
  })
}

export function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header = {}
    if (token) header.Authorization = 'Bearer ' + token
    uni.uploadFile({
      url: BASE_URL.replace(/\/$/, '') + '/api/file/upload',
      filePath,
      name: 'file',
      formData: { type: 'image' },
      header,
      success: (res) => {
        try {
          const body = JSON.parse(res.data || '{}')
          if (body.code !== 200) {
            reject(new Error(body.message || '上传失败'))
            return
          }
          resolve(body.data)
        } catch (e) {
          reject(new Error('上传解析失败'))
        }
      },
      fail: (e) => reject(e || new Error('上传失败'))
    })
  })
}

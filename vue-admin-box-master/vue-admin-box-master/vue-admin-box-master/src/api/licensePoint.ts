import request from '@/utils/system/request'

/** 与「用户管理」同一分页接口，含 licensePoints */
export function getUserListForLicense(params: {
  keyword?: string
  userType?: string
  status?: string
  pageNum?: number
  pageSize?: number
}) {
  return request({
    url: '/user/list',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function getLicenseSummary(userId: number) {
  return request({
    url: '/license-point/summary',
    method: 'get',
    params: { userId },
    baseURL: '/api'
  })
}

export function getLicenseLog(params: { userId?: number; pageNum?: number; pageSize?: number }) {
  return request({
    url: '/license-point/log',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function getWelfarePending(params: { pageNum?: number; pageSize?: number }) {
  return request({
    url: '/license-point/welfare/pending',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function reviewWelfare(data: {
  requestId: number
  approve: boolean
  points?: number
  adminRemark?: string
  reviewerId?: number
}) {
  return request({
    url: '/license-point/welfare/review',
    method: 'post',
    data,
    baseURL: '/api'
  })
}

export function adminBonus(data: {
  userId: number
  points: number
  remark?: string
  evidenceUrls?: string
  operatorUserId?: number
}) {
  return request({
    url: '/license-point/admin/bonus',
    method: 'post',
    data,
    baseURL: '/api'
  })
}

export function deleteViolationRestorePoints(illegalId: number, operatorUserId?: number, reason?: string) {
  return request({
    url: `/violation/${illegalId}`,
    method: 'delete',
    params: { operatorUserId, reason },
    baseURL: '/api'
  })
}

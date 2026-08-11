import request from '@/utils/system/request'

export function getAppealList(params: Record<string, unknown>) {
  return request({
    url: '/appeal/list',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function getAppealDetail(appealId: number) {
  return request({
    url: `/appeal/${appealId}`,
    method: 'get',
    baseURL: '/api'
  })
}

export function auditAppeal(data: object) {
  return request({
    url: '/appeal/audit',
    method: 'post',
    data,
    baseURL: '/api'
  })
}

export function submitAppeal(data: object) {
  return request({
    url: '/appeal/submit',
    method: 'post',
    data,
    baseURL: '/api'
  })
}

export function getAppealStats() {
  return request({
    url: '/appeal/stats',
    method: 'get',
    baseURL: '/api'
  })
}

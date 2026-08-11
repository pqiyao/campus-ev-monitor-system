import request from '@/utils/system/request'

export function getViolationList(params: Record<string, unknown>) {
  return request({
    url: '/violation/list',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function getViolationTypes() {
  return request({
    url: '/violation/types',
    method: 'get',
    baseURL: '/api'
  })
}

export function getViolationStats() {
  return request({
    url: '/violation/stats',
    method: 'get',
    baseURL: '/api'
  })
}

export function getViolationDetail(violationId: number) {
  return request({
    url: `/violation/${violationId}`,
    method: 'get',
    baseURL: '/api'
  })
}

export function reportViolation(data: object) {
  return request({
    url: '/violation/report',
    method: 'post',
    data,
    baseURL: '/api'
  })
}

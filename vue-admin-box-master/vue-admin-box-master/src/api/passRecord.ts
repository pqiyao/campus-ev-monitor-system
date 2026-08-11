import request from '@/utils/system/request'

export function getPassRecordList(params: Record<string, unknown>) {
  return request({
    url: '/pass-record/list',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function getPassRecordRecent(limit = 20) {
  return request({
    url: '/pass-record/recent',
    method: 'get',
    params: { limit },
    baseURL: '/api'
  })
}

export function getPassRecordStats() {
  return request({
    url: '/pass-record/stats',
    method: 'get',
    baseURL: '/api'
  })
}

export function getPassRecordDetail(passId: number) {
  return request({
    url: `/pass-record/${passId}`,
    method: 'get',
    baseURL: '/api'
  })
}

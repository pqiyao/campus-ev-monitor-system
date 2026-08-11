import request from '@/utils/system/request'

/** 闭环处置-操作日志分页（对接 /api/operation-log/list） */
export function getOperationLogs(params: Record<string, unknown>) {
  return request({
    url: '/operation-log/list',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function getOperationLogsByAppeal(appealId: number) {
  return request({
    url: `/operation-log/appeal/${appealId}`,
    method: 'get',
    baseURL: '/api'
  })
}

export function getOperationLogStats() {
  return request({
    url: '/operation-log/stats',
    method: 'get',
    baseURL: '/api'
  })
}

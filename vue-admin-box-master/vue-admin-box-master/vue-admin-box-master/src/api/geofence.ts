import request from '@/utils/system/request'

/** 全部围栏（管理 CRUD 列表） */
export function getGeofenceList() {
  return request({
    url: '/geofence/list',
    method: 'get',
    baseURL: '/api'
  })
}

/** 当前已启用的围栏（运行时生效集合） */
export function getGeofenceEnabled() {
  return request({
    url: '/geofence/enabled',
    method: 'get',
    baseURL: '/api'
  })
}

export function createGeofence(data: object) {
  return request({
    url: '/geofence/create',
    method: 'post',
    data,
    baseURL: '/api'
  })
}

export function updateGeofence(data: object) {
  return request({
    url: '/geofence/update',
    method: 'put',
    data,
    baseURL: '/api'
  })
}

export function deleteGeofence(fenceId: number) {
  return request({
    url: `/geofence/${fenceId}`,
    method: 'delete',
    baseURL: '/api'
  })
}

export function toggleGeofence(fenceId: number) {
  return request({
    url: `/geofence/${fenceId}/toggle`,
    method: 'put',
    baseURL: '/api'
  })
}

/** 围栏告警列表 */
export function getGeofenceAlerts(limit = 50) {
  return request({
    url: '/geofence/alerts',
    method: 'get',
    params: { limit },
    baseURL: '/api'
  })
}

export function processGeofenceAlert(alertId: number) {
  return request({
    url: `/geofence/alerts/${alertId}/process`,
    method: 'put',
    baseURL: '/api'
  })
}

/** 汇总：围栏总数、启用数、今日告警、未处理数 */
export function getGeofenceSummary() {
  return request({
    url: '/geofence/summary',
    method: 'get',
    baseURL: '/api'
  })
}

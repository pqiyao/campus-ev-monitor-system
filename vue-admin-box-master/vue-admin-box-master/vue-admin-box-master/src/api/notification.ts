import request from '@/utils/system/request'

export function getNotificationList(params: Record<string, unknown>) {
  return request({
    url: '/notification/list',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function getNotificationStats() {
  return request({
    url: '/notification/stats',
    method: 'get',
    baseURL: '/api'
  })
}

export function getNotificationDetail(notificationId: number, userId?: number) {
  return request({
    url: `/notification/${notificationId}`,
    method: 'get',
    params: userId != null ? { userId } : undefined,
    baseURL: '/api'
  })
}

export function sendNotification(data: Record<string, unknown>) {
  return request({
    url: '/notification/send',
    method: 'post',
    data,
    baseURL: '/api'
  })
}

export function sendPersonalNotification(title: string, content: string, receiverId: number) {
  return request({
    url: '/notification/send-personal',
    method: 'post',
    params: { title, content, receiverId },
    baseURL: '/api'
  })
}

export function sendBatchNotification(title: string, content: string, receiverIds: number[]) {
  return request({
    url: '/notification/send-batch',
    method: 'post',
    params: { title, content },
    data: receiverIds,
    baseURL: '/api'
  })
}

export function markNotificationRead(notificationId: number, userId: number) {
  return request({
    url: `/notification/${notificationId}/read`,
    method: 'post',
    params: { userId },
    baseURL: '/api'
  })
}

export function hideNotificationForUser(notificationId: number, userId: number) {
  return request({
    url: `/notification/${notificationId}/hide`,
    method: 'post',
    params: { userId },
    baseURL: '/api'
  })
}

export function deleteNotification(notificationId: number) {
  return request({
    url: `/notification/${notificationId}`,
    method: 'delete',
    baseURL: '/api'
  })
}

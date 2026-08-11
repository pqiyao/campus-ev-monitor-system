import request from '@/utils/system/request'

export function getRectifyList(params: Record<string, unknown>) {
  return request({
    url: '/rectify/list',
    method: 'get',
    params,
    baseURL: '/api'
  })
}

export function startRectify(violationId: number, userId: number, reason?: string) {
  return request({
    url: `/violation/${violationId}/rectify`,
    method: 'post',
    params: { userId, reason },
    baseURL: '/api'
  })
}

export function submitRectify(rectifyId: number, photos: string, description: string) {
  return request({
    url: `/violation/rectify/${rectifyId}/submit`,
    method: 'post',
    params: { photos, description },
    baseURL: '/api'
  })
}

export function verifyRectify(rectifyId: number, verifyUserId: number, pass: boolean, remark?: string, evidence?: string) {
  return request({
    url: `/violation/rectify/${rectifyId}/verify`,
    method: 'post',
    params: { verifyUserId, pass, remark, evidence },
    baseURL: '/api'
  })
}

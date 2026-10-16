import axios from 'axios'
import { getApiOrigin } from '@/utils/apiBase'

const api = axios.create({ baseURL: `${getApiOrigin()}/api/dispatch`, timeout: 15000 })

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (token) {
        config.headers = config.headers || {}
        config.headers.token = token
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

export const listRobots = () => api.get('/robots').then(r => r.data.data || [])
export const registerRobot = (data: any) => api.post('/robots', data).then(r => r.data.data)
export const previewScores = (eventId: number) => api.get(`/events/${eventId}/scores`).then(r => r.data.data || [])
export const dispatchEvent = (eventId: number) => api.post(`/events/${eventId}/dispatch`).then(r => r.data.data)
export const listOrders = (state?: string, limit = 50) =>
    api.get('/orders', { params: state ? { state, limit } : { limit } }).then(r => r.data.data || [])
export const getOrder = (orderId: number) => api.get(`/orders/${orderId}`).then(r => r.data.data)
export const injectFault = (orderId: number, faultType: string, parameters: Record<string, unknown> = {}) =>
    api.post(`/orders/${orderId}/fault`, { faultType, parameters }).then(r => r.data.data)
export const clearFault = (orderId: number) => api.post(`/orders/${orderId}/clear-fault`).then(r => r.data.data)

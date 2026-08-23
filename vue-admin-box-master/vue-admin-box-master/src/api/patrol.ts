import axios from 'axios'
import { getApiOrigin } from '@/utils/apiBase'

const api = axios.create({ baseURL: `${getApiOrigin()}/api/patrol`, timeout: 15000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.token = token
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export const createMission = (data: any) => api.post('/missions', data).then(r => r.data.data)
export const listMissions = () => api.get('/missions').then(r => r.data.data || [])
export const getMission = (id: number) => api.get(`/missions/${id}`).then(r => r.data.data)
export const getMissionAudit = (id: number) => api.get(`/missions/${id}`).then(r => r.data.data?.audit || [])
export const planMission = (id: number) => api.post(`/missions/${id}/plan`).then(r => r.data.data)
export const startMission = (id: number) => api.post(`/missions/${id}/start`).then(r => r.data.data)
export const pauseMission = (id: number) => api.post(`/missions/${id}/pause`).then(r => r.data.data)
export const replanMission = (id: number, reason: string) => api.post(`/missions/${id}/replan`, { reason }).then(r => r.data.data)
export const attachPatrolEvent = (id: number, data: any = {}) => api.post(`/missions/${id}/events`, data).then(r => r.data.data)
export const generateMissionReport = (id: number) => api.post(`/missions/${id}/report`).then(r => r.data.data)
export const injectPatrolFault = (id: number, faultType: string, parameters: Record<string, unknown> = {}) => api.post(`/missions/${id}/faults`, { faultType, parameters }).then(r => r.data.data)
export const clearPatrolFault = (id: number) => api.delete(`/missions/${id}/faults`).then(r => r.data.data)

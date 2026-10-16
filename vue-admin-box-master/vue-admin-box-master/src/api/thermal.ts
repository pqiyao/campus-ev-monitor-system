import axios from 'axios'
import { getApiOrigin } from '@/utils/apiBase'

const api = axios.create({ baseURL: `${getApiOrigin()}/api/thermal`, timeout: 15000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.token = token
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export const listFrames = (limit = 50, riskLevel?: string) =>
  api.get('/frames', { params: riskLevel ? { limit, riskLevel } : { limit } }).then(r => r.data.data || [])
export const getFrame = (frameId: number) => api.get(`/frames/${frameId}`).then(r => r.data.data)

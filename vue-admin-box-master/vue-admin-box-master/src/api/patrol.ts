import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8083/api/patrol', timeout: 15000 })

export const createMission = (data: any) => api.post('/missions', data).then(r => r.data.data)
export const listMissions = () => api.get('/missions').then(r => r.data.data || [])
export const getMission = (id: number) => api.get(`/missions/${id}`).then(r => r.data.data)
export const planMission = (id: number) => api.post(`/missions/${id}/plan`).then(r => r.data.data)
export const startMission = (id: number) => api.post(`/missions/${id}/start`).then(r => r.data.data)
export const pauseMission = (id: number) => api.post(`/missions/${id}/pause`).then(r => r.data.data)
export const replanMission = (id: number, reason: string) => api.post(`/missions/${id}/replan`, { reason }).then(r => r.data.data)
export const generateMissionReport = (id: number) => api.post(`/missions/${id}/report`).then(r => r.data.data)

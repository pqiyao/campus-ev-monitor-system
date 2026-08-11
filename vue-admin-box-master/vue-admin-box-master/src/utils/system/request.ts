import axios, { AxiosError, AxiosRequestConfig, AxiosResponse, AxiosInstance } from 'axios'
import store from '@/store'
import { ElMessage } from 'element-plus'

const baseURL: any = import.meta.env.VITE_BASE_URL || ''

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 5000
})

function readTokenFromStorage(): string {
  const directToken = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (directToken) return directToken

  const vuexRaw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex')
  if (!vuexRaw) return ''

  try {
    const parsed = JSON.parse(vuexRaw)
    return parsed?.user?.token || ''
  } catch {
    return ''
  }
}

service.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    config.headers = config.headers || {}

    const storeToken = store.getters['user/token'] || store?.state?.user?.token
    const token = storeToken || readTokenFromStorage()

    if (token) {
      config.headers['token'] = token
      config.headers['Authorization'] = `Bearer ${token}`
    }

    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    showError(res)
    return Promise.reject(res)
  },
  (error: AxiosError) => {
    const badMessage: any = error.message || error
    const code = parseInt(String(badMessage).replace('Error: Request failed with status code ', ''), 10)
    showError({ code, message: badMessage })
    return Promise.reject(error)
  }
)

function showError(error: any) {
  if (error.code === 401 || error.code === 403) {
    store.dispatch('user/loginOut')
    return
  }

  ElMessage({
    message: error.msg || error.message || '服务异常',
    type: 'error',
    duration: 3000
  })
}

export default service

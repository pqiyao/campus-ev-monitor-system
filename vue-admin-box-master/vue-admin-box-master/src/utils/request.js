import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { getToken, removeToken } from './auth'

// 创建axios实例
const service = axios.create({
    baseURL: '/api',
    timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 添加token到请求头
        const token = getToken()
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    response => {
        const res = response.data

        // 根据后端接口规范处理
        if (res.code === 200) {
            return res.data
        } else if (res.code === 401) {
            // token失效，跳转到登录页
            removeToken()
            router.push('/login')
            ElMessage.error('登录已过期，请重新登录')
            return Promise.reject(new Error(res.message || '登录已过期'))
        } else if (res.code === 403) {
            ElMessage.error('权限不足')
            return Promise.reject(new Error(res.message || '权限不足'))
        } else {
            ElMessage.error(res.message || '请求失败')
            return Promise.reject(new Error(res.message || '请求失败'))
        }
    },
    error => {
        console.error('响应错误:', error)
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    removeToken()
                    router.push('/login')
                    ElMessage.error('登录已过期，请重新登录')
                    break
                case 403:
                    ElMessage.error('权限不足')
                    break
                case 404:
                    ElMessage.error('请求的资源不存在')
                    break
                case 500:
                    ElMessage.error('服务器内部错误')
                    break
                default:
                    ElMessage.error('请求失败，请稍后重试')
            }
        } else if (error.request) {
            ElMessage.error('网络连接失败，请检查网络')
        } else {
            ElMessage.error('请求发送失败')
        }
        return Promise.reject(error)
    }
)

export default service

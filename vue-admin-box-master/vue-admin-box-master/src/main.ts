/*
 * @Date: 2022-05-22 20:44:25
 * @Description:
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import axios from 'axios'
import { baidu } from './utils/system/statistics'
import 'element-plus/theme-chalk/display.css'
import 'element-plus/dist/index.css'
import 'normalize.css'
import './assets/style/common.scss'
import './theme/modules/chinese/index.scss'
import App from './App.vue'
import store from './store'
import router from './router'
import { getAuthRoutes } from './router/permission'
import i18n from './locale'

function readTokenFromStorage(): string {
  const direct = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (direct) return direct

  const vuexRaw = localStorage.getItem('vuex') || sessionStorage.getItem('vuex')
  if (!vuexRaw) return ''

  try {
    const parsed = JSON.parse(vuexRaw)
    return parsed?.user?.token || ''
  } catch {
    return ''
  }
}

// Global axios auth injection for pages using direct axios calls.
axios.interceptors.request.use((config) => {
  const token = store?.state?.user?.token || readTokenFromStorage()
  if (token) {
    const headers = (config.headers || {}) as Record<string, any>
    if (!headers.Authorization) headers.Authorization = `Bearer ${token}`
    if (!headers.token) headers.token = token
    config.headers = headers
  }
  return config
})

if (import.meta.env.MODE !== 'development') {
  baidu()
}

getAuthRoutes().then(() => {
  const app = createApp(App)
  app.use(ElementPlus, { size: store.state.app.elementSize })
  app.use(store)
  app.use(router)
  app.use(i18n)
  app.mount('#app')
})
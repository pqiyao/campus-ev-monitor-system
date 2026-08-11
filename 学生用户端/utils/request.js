import { BASE_URL } from './config.js'
import { getToken, clearAuth } from './auth.js'

/** Spring 等业务错误常返回 HTTP 400/500，但具体原因在 JSON 的 message 里 */
function messageFromResponseBody(body, statusCode) {
  if (body && typeof body === 'object') {
    const m = body.message || body.msg
    if (m) return String(m)
  }
  return '请求失败 (' + statusCode + ')'
}

function buildQuery(params) {
  if (!params) return ''
  const q = Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== null && params[k] !== '')
    .map((k) => encodeURIComponent(k) + '=' + encodeURIComponent(params[k]))
    .join('&')
  return q ? '?' + q : ''
}

export function request(options) {
  const { url, method = 'GET', data = null, params = null, header = {} } = options
  const token = getToken()
  if (token) header.Authorization = 'Bearer ' + token

  let full = BASE_URL.replace(/\/$/, '') + (url.startsWith('/') ? url : '/' + url)
  if (params) full += buildQuery(params)

  return new Promise((resolve, reject) => {
    uni.request({
      url: full,
      method,
      data: method === 'GET' || method === 'DELETE' ? undefined : data,
      header: {
        'Content-Type': 'application/json',
        ...header
      },
      success: (res) => {
        const body = res.data || {}
        if (res.statusCode === 401) {
          clearAuth()
          try {
            uni.reLaunch({ url: '/pages/login/login' })
          } catch (e) {}
          reject(new Error(body.message || '登录已失效，请重新登录'))
          return
        }
        if (res.statusCode !== 200) {
          reject(new Error(messageFromResponseBody(body, res.statusCode)))
          return
        }
        if (body.code !== undefined && body.code !== 200) {
          reject(new Error(body.message || body.msg || '请求失败'))
          return
        }
        resolve(body.data !== undefined ? body.data : body)
      },
      fail: (e) => reject(e || new Error('网络错误'))
    })
  })
}

/** PageResponse：整页对象在 data 里，code 在根上 — uni 拿到的 res.data 是完整 JSON */
export function requestPage(options) {
  const { url, params = {} } = options
  const token = getToken()
  const header = {}
  if (token) header.Authorization = 'Bearer ' + token
  let full = BASE_URL.replace(/\/$/, '') + (url.startsWith('/') ? url : '/' + url) + buildQuery(params)

  return new Promise((resolve, reject) => {
    uni.request({
      url: full,
      method: 'GET',
      header,
      success: (res) => {
        const body = res.data || {}
        if (res.statusCode === 401) {
          clearAuth()
          try {
            uni.reLaunch({ url: '/pages/login/login' })
          } catch (e) {}
          reject(new Error(body.message || '登录已失效'))
          return
        }
        if (res.statusCode !== 200) {
          reject(new Error(messageFromResponseBody(body, res.statusCode)))
          return
        }
        if (body.code !== 200) {
          reject(new Error(body.message || body.msg || '请求失败'))
          return
        }
        resolve({
          list: body.data || [],
          pageNum: body.pageNum,
          pageSize: body.pageSize,
          total: body.total
        })
      },
      fail: (e) => reject(e || new Error('网络错误'))
    })
  })
}

export function postForm(path, fields) {
  const token = getToken()
  const header = { 'Content-Type': 'application/x-www-form-urlencoded' }
  if (token) header.Authorization = 'Bearer ' + token
  const body = Object.keys(fields)
    .map((k) => encodeURIComponent(k) + '=' + encodeURIComponent(fields[k] == null ? '' : String(fields[k])))
    .join('&')
  const full = BASE_URL.replace(/\/$/, '') + (path.startsWith('/') ? path : '/' + path)
  return new Promise((resolve, reject) => {
    uni.request({
      url: full,
      method: 'POST',
      data: body,
      header,
      success: (res) => {
        const b = res.data || {}
        if (res.statusCode === 401) {
          clearAuth()
          try {
            uni.reLaunch({ url: '/pages/login/login' })
          } catch (e) {}
          reject(new Error(b.message || '登录已失效'))
          return
        }
        if (res.statusCode !== 200 || b.code !== 200) {
          reject(new Error(b.message || b.msg || messageFromResponseBody(b, res.statusCode)))
          return
        }
        resolve(b.data)
      },
      fail: (e) => reject(e || new Error('网络错误'))
    })
  })
}

const KEY_USER = 'ev_student_user'
const KEY_TOKEN = 'ev_student_token'

export function getUser() {
  try {
    const s = uni.getStorageSync(KEY_USER)
    return s ? JSON.parse(s) : null
  } catch (e) {
    return null
  }
}

export function setUser(user) {
  uni.setStorageSync(KEY_USER, JSON.stringify(user || {}))
}

export function getToken() {
  return uni.getStorageSync(KEY_TOKEN) || ''
}

export function setToken(token) {
  if (token) uni.setStorageSync(KEY_TOKEN, token)
  else uni.removeStorageSync(KEY_TOKEN)
}

export function clearAuth() {
  uni.removeStorageSync(KEY_USER)
  uni.removeStorageSync(KEY_TOKEN)
}

export function requireUser() {
  const u = getUser()
  if (!u || !u.userId) {
    uni.reLaunch({ url: '/pages/login/login' })
    return null
  }
  return u
}

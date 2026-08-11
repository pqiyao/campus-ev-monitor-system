const TOKEN_KEY = 'ev_token'
const USER_INFO_KEY = 'ev_user_info'

// 获取token
export function getToken() {
    return localStorage.getItem(TOKEN_KEY)
}

// 设置token
export function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token)
}

// 移除token
export function removeToken() {
    localStorage.removeItem(TOKEN_KEY)
}

// 获取用户信息
export function getUserInfo() {
    const userInfo = localStorage.getItem(USER_INFO_KEY)
    return userInfo ? JSON.parse(userInfo) : null
}

// 设置用户信息
export function setUserInfo(userInfo) {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

// 移除用户信息
export function removeUserInfo() {
    localStorage.removeItem(USER_INFO_KEY)
}

// 检查是否已登录
export function isLoggedIn() {
    return !!getToken()
}

// 获取用户ID
export function getUserId() {
    const userInfo = getUserInfo()
    return userInfo ? userInfo.userId : null
}

// 获取用户类型
export function getUserType() {
    const userInfo = getUserInfo()
    return userInfo ? userInfo.userType : null
}

// 检查是否是管理员
export function isAdmin() {
    return getUserType() === 'admin'
}
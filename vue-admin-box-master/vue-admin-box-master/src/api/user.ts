import request from '@/utils/system/request'

/** 登录 */
export function loginApi(data: any) {
  const payload = {
    username: data?.username ?? data?.name ?? '',
    password: data?.password ?? ''
  }
  return request({
    url: '/api/user/login',
    method: 'post',
    data: payload
  })
}

/** 获取用户信息（当前项目登录后直接使用 login 返回数据，此方法保留兼容） */
export function getInfoApi() {
  return Promise.resolve({
    code: 200,
    data: {
      info: {}
    }
  })
}

/** 退出登录（前端本地清理为主） */
export function loginOutApi() {
  return Promise.resolve({
    code: 200,
    data: {}
  })
}

/** 修改密码 */
export function passwordChange(data: { userId: number; oldPassword: string; newPassword: string }) {
  return request({
    url: '/api/user/update-password',
    method: 'post',
    params: data
  })
}

/** 获取菜单（当前使用前端静态路由，保留兼容） */
export function getMenuApi() {
  return Promise.resolve({ code: 200, data: [] })
}

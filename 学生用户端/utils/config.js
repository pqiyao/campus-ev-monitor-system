/** 后端根地址：真机 / 小程序真机调试请改为本机局域网 IP，如 http://192.168.1.5:8083 */
export const BASE_URL = 'http://localhost:8083'

/**
 * 百度地图 AK：与 vue-admin 大屏（dashboard、全屏）一致。
 * 启动时仍会请求 GET /api/client/config 覆盖为后端 application.yml 中的值。
 */
export const BAIDU_MAP_AK = 'SUBe4C3SHSL9Uyd9P4xgq6GMdaZF2Irj'

export function fileUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return BASE_URL.replace(/\/$/, '') + (path.startsWith('/') ? path : '/' + path)
}

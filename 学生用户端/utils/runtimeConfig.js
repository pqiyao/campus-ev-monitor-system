import { BASE_URL, BAIDU_MAP_AK } from './config.js'

let baiduMapAk = BAIDU_MAP_AK

export function getBaiduMapAk() {
  return (baiduMapAk && String(baiduMapAk).trim()) || BAIDU_MAP_AK
}

/**
 * 从后端拉取公开配置（与大屏共用同一份 AK），失败则沿用 config.js 默认值。
 */
export function refreshClientConfig() {
  return new Promise((resolve) => {
    const url = BASE_URL.replace(/\/$/, '') + '/api/client/config'
    uni.request({
      url,
      method: 'GET',
      timeout: 10000,
      success: (res) => {
        try {
          const body = res.data || {}
          if (res.statusCode === 200 && body.code === 200 && body.data) {
            const ak = body.data.baiduMapAk
            if (typeof ak === 'string' && ak.trim()) {
              baiduMapAk = ak.trim()
            }
          }
        } catch (e) {
          /* ignore */
        }
        resolve()
      },
      fail: () => resolve()
    })
  })
}

// #ifdef H5
let loading = null

export function loadBaiduMapApi(ak) {
  const key = (ak && String(ak).trim()) || ''
  if (!key) {
    return Promise.reject(new Error('empty ak'))
  }
  if (typeof window !== 'undefined' && window.BMap) {
    return Promise.resolve(window.BMap)
  }
  if (loading) {
    return loading
  }
  loading = new Promise((resolve, reject) => {
    const cb = '__initBMap_' + Date.now()
    window[cb] = function () {
      try {
        delete window[cb]
      } catch (e) {
        /* ignore */
      }
      loading = null
      if (window.BMap) {
        resolve(window.BMap)
      } else {
        reject(new Error('BMap missing'))
      }
    }
    const s = document.createElement('script')
    s.async = true
    s.src =
      'https://api.map.baidu.com/api?v=3.0&ak=' +
      encodeURIComponent(key) +
      '&callback=' +
      cb
    s.onerror = function () {
      loading = null
      try {
        delete window[cb]
      } catch (e) {
        /* ignore */
      }
      reject(new Error('script error'))
    }
    document.head.appendChild(s)
  })
  return loading
}
// #endif

// #ifndef H5
export function loadBaiduMapApi() {
  return Promise.reject(new Error('n/a'))
}
// #endif

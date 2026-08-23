import axios from 'axios'
import { getApiBaseUrl } from '@/utils/apiBase'

type BMapGlobal = any

let loadPromise: Promise<BMapGlobal> | null = null
let classicLoadPromise: Promise<BMapGlobal> | null = null
let akPromise: Promise<string> | null = null

const FALLBACK_AK = 'SUBe4C3SHSL9Uyd9P4xgq6GMdaZF2Irj'

export function getBaiduMapAk(): Promise<string> {
  if (akPromise) return akPromise
  akPromise = axios.get(`${getApiBaseUrl()}/client/config`, { timeout: 5000 })
    .then((response) => {
      const value = response?.data?.data?.baiduMapAk
      return typeof value === 'string' && value.trim() ? value.trim() : FALLBACK_AK
    })
    .catch(() => FALLBACK_AK)
  return akPromise
}

export async function loadBaiduMapGL(): Promise<BMapGlobal> {
  if (typeof window !== 'undefined' && (window as any).BMapGL) return (window as any).BMapGL
  if (loadPromise) return loadPromise

  loadPromise = getBaiduMapAk().then((ak) => new Promise<BMapGlobal>((resolve, reject) => {
    // Reuse any Baidu WebGL loader already present (the legacy dashboard also loads BMapGL).
    const existing = document.querySelector(
      'script[data-ev-baidu-map="gl"], script[src*="api.map.baidu.com/api"]'
    ) as HTMLScriptElement | null
    let settled = false
    let timer = 0
    const finish = (error?: Error) => {
      if (settled) return
      settled = true
      window.clearTimeout(timer)
      if (error) reject(error)
      else if ((window as any).BMapGL) resolve((window as any).BMapGL)
      else reject(new Error('BMapGL 未就绪'))
    }

    const onReady = () => finish()
    ;(window as any).__evBaiduMapReady = onReady

    if (existing) {
      timer = window.setTimeout(() => finish(new Error('百度地图加载超时')), 12000)
      const poll = window.setInterval(() => {
        if ((window as any).BMapGL) {
          window.clearInterval(poll)
          finish()
        }
      }, 80)
      window.setTimeout(() => window.clearInterval(poll), 12000)
      return
    }

    const script = document.createElement('script')
    script.async = true
    script.defer = true
    script.dataset.evBaiduMap = 'gl'
    script.src = `https://api.map.baidu.com/api?type=webgl&v=1.0&ak=${encodeURIComponent(ak)}&callback=__evBaiduMapReady`
    script.onerror = () => finish(new Error('百度地图脚本加载失败'))
    document.head.appendChild(script)
    timer = window.setTimeout(() => finish(new Error('百度地图加载超时')), 12000)
  })).finally(() => {
    loadPromise = null
  })

  return loadPromise
}

/** Load the classic BMap API for legacy camera/fence pages through the same AK source. */
export async function loadBaiduMapClassic(): Promise<BMapGlobal> {
  if (typeof window !== 'undefined' && (window as any).BMap) return (window as any).BMap
  if (classicLoadPromise) return classicLoadPromise

  classicLoadPromise = getBaiduMapAk().then((ak) => new Promise<BMapGlobal>((resolve, reject) => {
    let settled = false
    let timer = 0
    const finish = (error?: Error) => {
      if (settled) return
      settled = true
      window.clearTimeout(timer)
      if (error) reject(error)
      else if ((window as any).BMap) resolve((window as any).BMap)
      else reject(new Error('BMap 未就绪'))
    }
    ;(window as any).__evBaiduMapClassicReady = () => finish()
    const existing = document.querySelector('script[data-ev-baidu-map="classic"], script[src*="api.map.baidu.com/api?v=3.0"]') as HTMLScriptElement | null
    if (existing) {
      timer = window.setTimeout(() => finish(new Error('百度地图经典 API 加载超时')), 12000)
      const poll = window.setInterval(() => {
        if ((window as any).BMap) { window.clearInterval(poll); finish() }
      }, 80)
      window.setTimeout(() => window.clearInterval(poll), 12000)
      return
    }
    const script = document.createElement('script')
    script.async = true
    script.defer = true
    script.dataset.evBaiduMap = 'classic'
    script.src = `https://api.map.baidu.com/api?v=3.0&ak=${encodeURIComponent(ak)}&callback=__evBaiduMapClassicReady`
    script.onerror = () => finish(new Error('百度地图经典 API 加载失败'))
    document.head.appendChild(script)
    timer = window.setTimeout(() => finish(new Error('百度地图经典 API 加载超时')), 12000)
  })).finally(() => { classicLoadPromise = null })

  return classicLoadPromise
}

export function resetBaiduMapConfig() {
  akPromise = null
}

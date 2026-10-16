export type CampusKey = 'glutPingfeng' | 'hyNormalEast'

export type CampusPreset = {
  name: string
  lng: number
  lat: number
  platePrefix: string
}

export const CAMPUS_PRESETS: Readonly<Record<CampusKey, CampusPreset>> = Object.freeze({
  glutPingfeng: Object.freeze({
    name: '桂林理工大学屏风校区',
    // 屏风校区主体（建干路12号）公开坐标转换为百度 BD-09 后的中心点。
    lng: 110.3239130,
    lat: 25.2875179,
    platePrefix: '桂C'
  }),
  hyNormalEast: Object.freeze({
    name: '衡阳师范学院东校区',
    // OSM 衡阳师范学院（东校区）边界 way 280571392 转换为百度坐标后的中心点。
    lng: 112.6835772,
    lat: 26.8872727,
    platePrefix: '湘D'
  })
})

// 修改此处即可同步切换大屏地图和电子围栏地图。
export const ACTIVE_CAMPUS_KEY: CampusKey = 'hyNormalEast'
export const ACTIVE_CAMPUS = CAMPUS_PRESETS[ACTIVE_CAMPUS_KEY]
export const LEGACY_CAMPUS_CENTER = CAMPUS_PRESETS.hyNormalEast

export function normalizeCampusPoint(point: { lng?: unknown; lat?: unknown }) {
  const lng = Number(point?.lng)
  const lat = Number(point?.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return { lng, lat }
  if (ACTIVE_CAMPUS_KEY === 'hyNormalEast') return { lng, lat }

  const nearLegacyCampus = Math.abs(lng - LEGACY_CAMPUS_CENTER.lng) <= 0.1
    && Math.abs(lat - LEGACY_CAMPUS_CENTER.lat) <= 0.1
  if (!nearLegacyCampus) return { lng, lat }

  return {
    lng: ACTIVE_CAMPUS.lng + (lng - LEGACY_CAMPUS_CENTER.lng),
    lat: ACTIVE_CAMPUS.lat + (lat - LEGACY_CAMPUS_CENTER.lat)
  }
}

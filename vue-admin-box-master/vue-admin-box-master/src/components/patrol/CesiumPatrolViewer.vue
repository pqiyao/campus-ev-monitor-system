<template>
  <section class="cesium-viewer-shell">
    <div ref="host" class="cesium-host"></div>
    <div class="scene-toolbar">
      <button :class="{ active: sceneView === 'overview' }" title="按航线与建筑范围取景" @click="setSceneView('overview')">总览</button>
      <button :class="{ active: sceneView === 'top' }" title="俯视巡检区域" @click="setSceneView('top')">俯视</button>
      <button :class="{ active: sceneView === 'follow' }" title="跟随仿真无人机" @click="setSceneView('follow')">跟随无人机</button>
    </div>
    <div class="cesium-hud">
      <div class="hud-title"><span class="live-dot"></span>真实空间巡检视图</div>
      <div class="hud-meta">WGS84 · CesiumJS · PyBullet 遥测驱动</div>
      <div class="hud-stats">
        <span>航点 {{ waypoints.length }}</span>
        <span>建筑 {{ buildingCount }}</span>
        <span>高度 {{ latestAltitude }}m</span>
      </div>
    </div>
    <div v-if="error" class="cesium-error">3D 资源加载失败，已保留二维航线视图</div>
    <div class="cesium-credit">建筑轮廓：OpenStreetMap · 高度为巡检仿真航点值</div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import * as Cesium from 'cesium'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import { ACTIVE_CAMPUS } from '@/config/campusMap'

type Point = { lng: number; lat: number; altitude?: number }

const props = defineProps<{
  waypoints: any[]
  telemetry: any[]
  fences: any[]
}>()

const host = ref<HTMLElement | null>(null)
const error = ref(false)
const buildingCount = ref(0)
const viewer = ref<Cesium.Viewer | null>(null)
let routeEntities: Cesium.Entity[] = []
let buildingEntities: Cesium.Entity[] = []
let fenceEntities: Cesium.Entity[] = []
let droneEntity: Cesium.Entity | null = null
let groundDropEntity: Cesium.Entity | null = null
const sceneView = ref<'overview' | 'top' | 'follow'>('overview')
const buildingPositions: Cesium.Cartesian3[] = []
const routePositions: Cesium.Cartesian3[] = []

const latestTelemetry = computed(() => {
  const points = [...(props.telemetry || [])]
  const sorted = points.sort((a, b) => String(a.recorded_at || a.recordedAt || '').localeCompare(String(b.recorded_at || b.recordedAt || '')))
  return sorted.length ? sorted[sorted.length - 1] : null
})
const latestAltitude = computed(() => Number(latestTelemetry.value?.altitude_m ?? latestTelemetry.value?.altitudeM ?? props.waypoints?.[0]?.altitude_m ?? 0).toFixed(1))

function transformLat(x: number, y: number) {
  let ret = -100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20 * Math.sin(6 * x * Math.PI) + 20 * Math.sin(2 * x * Math.PI)) * 2 / 3
  ret += (20 * Math.sin(y * Math.PI) + 40 * Math.sin(y / 3 * Math.PI)) * 2 / 3
  ret += (160 * Math.sin(y / 12 * Math.PI) + 320 * Math.sin(y * Math.PI / 30)) * 2 / 3
  return ret
}

function transformLng(x: number, y: number) {
  let ret = 300 + x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20 * Math.sin(6 * x * Math.PI) + 20 * Math.sin(2 * x * Math.PI)) * 2 / 3
  ret += (20 * Math.sin(x * Math.PI) + 40 * Math.sin(x / 3 * Math.PI)) * 2 / 3
  ret += (150 * Math.sin(x / 12 * Math.PI) + 300 * Math.sin(x / 30 * Math.PI)) * 2 / 3
  return ret
}

function bd09ToWgs84(lng: number, lat: number) {
  const x = lng - 0.0065
  const y = lat - 0.006
  const z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * Math.PI)
  const theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * Math.PI)
  const gcjLng = z * Math.cos(theta)
  const gcjLat = z * Math.sin(theta)
  const a = 6378245
  const ee = 0.00669342162296594323
  const dLat = transformLat(gcjLng - 105, gcjLat - 35)
  const dLng = transformLng(gcjLng - 105, gcjLat - 35)
  const radLat = gcjLat / 180 * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  const wgsLat = gcjLat - (dLat * 180) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI)
  const wgsLng = gcjLng - (dLng * 180) / (a / sqrtMagic * Math.cos(radLat) * Math.PI)
  return { lng: wgsLng, lat: wgsLat }
}

function parsePoint(row: any, withAltitude = true): Point | null {
  const lng = Number(row?.lng ?? row?.longitude)
  const lat = Number(row?.lat ?? row?.latitude)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  const rawAltitude = Number(row?.altitude_m ?? row?.altitudeM ?? row?.altitude ?? 0)
  return { lng, lat, altitude: withAltitude && Number.isFinite(rawAltitude) ? rawAltitude : 0 }
}

function toCartesian(point: Point, altitude = point.altitude || 0) {
  const wgs = bd09ToWgs84(point.lng, point.lat)
  return Cesium.Cartesian3.fromDegrees(wgs.lng, wgs.lat, altitude)
}

function clearEntities(items: Cesium.Entity[]) {
  const v = viewer.value
  if (!v) return
  items.forEach((entity) => v.entities.remove(entity))
  items.splice(0, items.length)
}

function fitScene(topDown = false) {
  const v = viewer.value
  if (!v) return
  const points = [...buildingPositions, ...routePositions].length ? [...buildingPositions, ...routePositions] : [Cesium.Cartesian3.fromDegrees(ACTIVE_CAMPUS.lng, ACTIVE_CAMPUS.lat, 0)]
  const sphere = Cesium.BoundingSphere.fromPoints(points)
  const radius = Math.max(sphere.radius, 140)
  v.camera.flyToBoundingSphere(sphere, {
    duration: 0.7,
    offset: new Cesium.HeadingPitchRange(0.16, topDown ? -Cesium.Math.PI_OVER_TWO + 0.04 : -0.72, radius * (topDown ? 2.25 : 1.85))
  })
}

function setSceneView(view: 'overview' | 'top' | 'follow') {
  sceneView.value = view
  if (view === 'follow' && viewer.value && droneEntity) {
    viewer.value.trackedEntity = droneEntity
    return
  }
  if (viewer.value) viewer.value.trackedEntity = undefined
  fitScene(view === 'top')
}

function parseFence(value: any): Point[] {
  let raw = value
  if (typeof raw === 'string') {
    try { raw = JSON.parse(raw) } catch { return [] }
  }
  return Array.isArray(raw) ? raw.map((item) => parsePoint(item, false)).filter(Boolean) as Point[] : []
}

function renderBuildings() {
  const v = viewer.value
  if (!v) return
  clearEntities(buildingEntities)
  buildingPositions.length = 0
  fetch('/campus-buildings.json')
    .then((response) => response.ok ? response.json() : Promise.reject(new Error('建筑数据不可用')))
    .then((data) => {
      const elements = Array.isArray(data?.elements) ? data.elements : []
      elements.forEach((element: any, index: number) => {
        const ring = Array.isArray(element?.geometry) ? element.geometry.map((node: any) => {
          const lng = Number(node?.lon)
          const lat = Number(node?.lat)
          return Number.isFinite(lng) && Number.isFinite(lat) ? Cesium.Cartesian3.fromDegrees(lng, lat, 18 + (index % 3) * 6) : null
        }).filter(Boolean) : []
        if (ring.length < 3) return
        buildingPositions.push(...(ring as Cesium.Cartesian3[]))
        const entity = v.entities.add({
          name: element?.tags?.name || `校园建筑 ${index + 1}`,
          polygon: {
            hierarchy: new Cesium.PolygonHierarchy(ring as Cesium.Cartesian3[]),
            height: 0,
            extrudedHeight: 18 + (index % 3) * 6,
            material: Cesium.Color.fromCssColorString('#4b7690').withAlpha(0.72),
            outline: true,
            outlineColor: Cesium.Color.fromCssColorString('#a9d7df').withAlpha(0.85)
          },
          label: {
            text: element?.tags?.name || '建筑',
            font: '12px Microsoft YaHei',
            fillColor: Cesium.Color.WHITE,
            showBackground: true,
            backgroundColor: Cesium.Color.fromCssColorString('#0a1b27').withAlpha(0.78),
            pixelOffset: new Cesium.Cartesian2(0, -24),
            distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 1800)
          }
        })
        buildingEntities.push(entity)
      })
      buildingCount.value = buildingEntities.length
      if (sceneView.value !== 'follow') fitScene(sceneView.value === 'top')
    })
    .catch(() => { buildingCount.value = 0 })
}

function renderRoute() {
  const v = viewer.value
  if (!v) return
  clearEntities(routeEntities)
  routePositions.length = 0
  const points = (props.waypoints || []).map((row) => parsePoint(row)).filter(Boolean) as Point[]
  if (points.length < 1) return
  const elevated = points.map((point) => toCartesian(point, Math.max(2, point.altitude || 30)))
  const ground = points.map((point) => toCartesian(point, 0))
  routePositions.push(...elevated, ...ground)
  routeEntities.push(v.entities.add({
    name: '规划航线（真实高度）',
    polyline: { positions: elevated, width: 5, material: new Cesium.PolylineGlowMaterialProperty({ glowPower: 0.18, color: Cesium.Color.fromCssColorString('#27d7ff') }) }
  }))
  routeEntities.push(v.entities.add({
    name: '航线地面投影',
    polyline: { positions: ground, width: 2, material: new Cesium.PolylineDashMaterialProperty({ color: Cesium.Color.fromCssColorString('#7de7e0').withAlpha(0.65), dashLength: 12 }) }
  }))
  points.forEach((point, index) => {
    const elevatedPoint = toCartesian(point, Math.max(2, point.altitude || 30))
    routeEntities.push(v.entities.add({
      name: `航点 ${index + 1}`,
      position: elevatedPoint,
      point: { pixelSize: 8, color: Cesium.Color.fromCssColorString('#f7c873'), outlineColor: Cesium.Color.WHITE, outlineWidth: 1 },
      label: { text: `${index + 1} · ${(point.altitude || 0).toFixed(0)}m`, font: '11px Microsoft YaHei', fillColor: Cesium.Color.WHITE, showBackground: true, backgroundColor: Cesium.Color.fromCssColorString('#0a1b27').withAlpha(0.8), pixelOffset: new Cesium.Cartesian2(10, -8) },
      polyline: { positions: [ground[index], elevatedPoint], width: 1, material: Cesium.Color.fromCssColorString('#f7c873').withAlpha(0.4) }
    }))
  })
}

function renderFences() {
  const v = viewer.value
  if (!v) return
  clearEntities(fenceEntities)
  ;(props.fences || []).forEach((fence: any) => {
    const points = parseFence(fence.coordinates)
    if (points.length < 3) return
    const positions = points.map((point) => toCartesian(point, 0))
    const restricted = Number(fence.fence_type ?? fence.fenceType) <= 2
    fenceEntities.push(v.entities.add({
      name: restricted ? '禁飞区' : '关注围栏',
      polygon: { hierarchy: new Cesium.PolygonHierarchy(positions), height: 0, extrudedHeight: 4, material: (restricted ? Cesium.Color.RED : Cesium.Color.ORANGE).withAlpha(0.2), outline: true, outlineColor: restricted ? Cesium.Color.RED : Cesium.Color.ORANGE }
    }))
  })
}

function renderDrone() {
  const v = viewer.value
  if (!v) return
  const latest = parsePoint(latestTelemetry.value)
  const first = parsePoint(props.waypoints?.[0])
  const point = latest || first
  if (!point) {
    if (droneEntity) v.entities.remove(droneEntity)
    droneEntity = null
    return
  }
  const position = toCartesian(point, Math.max(1, point.altitude || 30))
  if (!droneEntity) {
    droneEntity = v.entities.add({
      name: '仿真无人机（PyBullet）',
      position,
      box: { dimensions: new Cesium.Cartesian3(3.2, 3.2, 0.9), material: Cesium.Color.fromCssColorString('#1de3c2').withAlpha(0.96), outline: true, outlineColor: Cesium.Color.WHITE },
      label: { text: 'DRONE · PyBullet', font: '12px Microsoft YaHei', fillColor: Cesium.Color.fromCssColorString('#c5fff5'), showBackground: true, backgroundColor: Cesium.Color.fromCssColorString('#071a23').withAlpha(0.9), pixelOffset: new Cesium.Cartesian2(0, -28) }
    })
  } else droneEntity.position = position as any
  const ground = toCartesian(point, 0)
  if (!groundDropEntity) {
    groundDropEntity = v.entities.add({ polyline: { positions: [ground, position], width: 2, material: Cesium.Color.fromCssColorString('#1de3c2').withAlpha(0.46) } })
  } else if (groundDropEntity.polyline) groundDropEntity.polyline.positions = new Cesium.ConstantProperty([ground, position])
}

function render() {
  renderRoute()
  renderFences()
  renderDrone()
}

onMounted(() => {
  try {
    ;(window as any).CESIUM_BASE_URL = '/cesium/'
    if (!host.value) throw new Error('3D 容器未就绪')
    viewer.value = new Cesium.Viewer(host.value, {
      animation: false,
      timeline: false,
      geocoder: false,
      homeButton: false,
      sceneModePicker: false,
      navigationHelpButton: false,
      fullscreenButton: false,
      baseLayerPicker: false,
      infoBox: false,
      selectionIndicator: false,
      baseLayer: new Cesium.ImageryLayer(new Cesium.OpenStreetMapImageryProvider({ url: 'https://tile.openstreetmap.org/' })),
      terrainProvider: new Cesium.EllipsoidTerrainProvider()
    })
    viewer.value.scene.globe.enableLighting = true
    viewer.value.scene.globe.baseColor = Cesium.Color.fromCssColorString('#15232b')
    viewer.value.scene.skyAtmosphere.show = true
    const campus = bd09ToWgs84(ACTIVE_CAMPUS.lng, ACTIVE_CAMPUS.lat)
    viewer.value.camera.setView({ destination: Cesium.Cartesian3.fromDegrees(campus.lng, campus.lat, 900), orientation: { heading: 0.15, pitch: Cesium.Math.toRadians(-48), roll: 0 } })
    renderBuildings()
    render()
    window.setTimeout(() => fitScene(false), 500)
    error.value = false
  } catch (e) {
    error.value = true
  }
})

watch(() => [props.waypoints, props.fences, props.telemetry], render, { deep: true })

onUnmounted(() => {
  viewer.value?.destroy()
  viewer.value = null
  droneEntity = null
  groundDropEntity = null
})
</script>

<style scoped>
.cesium-viewer-shell{position:relative;min-height:460px;height:100%;overflow:hidden;background:#07141e;border:1px solid #21506a}.cesium-host{position:absolute;inset:0}.scene-toolbar{position:absolute;right:14px;top:14px;z-index:3;display:flex;gap:5px}.scene-toolbar button{border:1px solid rgba(90,151,171,.65);border-radius:4px;padding:5px 8px;color:#b9dbe5;background:rgba(7,24,34,.86);font-size:10px;cursor:pointer}.scene-toolbar button.active{border-color:#1de3c2;color:#e4fffb;background:rgba(16,117,105,.9)}.cesium-hud{position:absolute;left:15px;top:14px;z-index:2;padding:10px 12px;background:rgba(5,18,27,.88);border:1px solid rgba(57,215,255,.34);box-shadow:0 10px 28px rgba(0,0,0,.26);color:#dff8ff;pointer-events:none}.hud-title{font-size:13px;font-weight:700}.live-dot{display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#1de3c2;box-shadow:0 0 10px #1de3c2}.hud-meta{margin-top:5px;color:#8db5c9;font-size:10px}.hud-stats{display:flex;gap:12px;margin-top:9px;color:#b1e6ee;font-size:10px}.cesium-credit{position:absolute;right:8px;bottom:6px;z-index:2;color:rgba(221,241,246,.68);font-size:10px;pointer-events:none}.cesium-error{position:absolute;right:12px;top:52px;z-index:3;padding:7px 10px;color:#ffd48b;background:rgba(45,28,14,.92);border:1px solid rgba(255,180,71,.55);font-size:11px}
</style>

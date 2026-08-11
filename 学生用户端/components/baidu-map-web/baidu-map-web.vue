<template>
  <!-- #ifdef H5 -->
  <view class="bmap-wrap">
    <view :id="mapId" class="bmap-inner" />
    <view v-if="hint" class="bmap-hint">{{ hint }}</view>
  </view>
  <!-- #endif -->
  <!-- #ifndef H5 -->
  <view class="bmap-wrap" />
  <!-- #endif -->
</template>

<script>
// #ifdef H5
import { getBaiduMapAk } from '../../utils/runtimeConfig.js'
import { loadBaiduMapApi } from '../../utils/baiduMapLoader.js'

function parseStroke(hex) {
  if (!hex || typeof hex !== 'string') {
    return { color: '#0d9488', opacity: 0.65 }
  }
  const m = hex.replace(/\s/g, '').match(/^#([0-9a-fA-F]{6})([0-9a-fA-F]{2})?$/)
  if (!m) {
    return { color: hex.slice(0, 7) || '#0d9488', opacity: 0.65 }
  }
  const color = '#' + m[1]
  const opacity = m[2] ? parseInt(m[2], 16) / 255 : 0.65
  return { color, opacity }
}

const CLEAN_BMAP_STYLE = [
  { featureType: 'all', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'poi', elementType: 'all', stylers: { visibility: 'off' } },
  { featureType: 'poilabel', elementType: 'all', stylers: { visibility: 'off' } },
  { featureType: 'building', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'district', elementType: 'labels', stylers: { visibility: 'off' } },
  { featureType: 'road', elementType: 'labels', stylers: { visibility: 'off' } }
]

export default {
  name: 'BaiduMapWeb',
  props: {
    latitude: {
      type: Number,
      default: 26.8860959
    },
    longitude: {
      type: Number,
      default: 112.6827629
    },
    scale: {
      type: Number,
      default: 18
    },
    markers: {
      type: Array,
      default() {
        return []
      }
    },
    polylines: {
      type: Array,
      default() {
        return []
      }
    },
    /** 多边形围栏：{ points:[{latitude,longitude}], strokeColor, fillColor, strokeWidth } */
    polygons: {
      type: Array,
      default() {
        return []
      }
    }
  },
  data() {
    return {
      mapId: '',
      hint: ''
    }
  },
  created() {
    this.mapId = 'bm_' + (this._uid != null ? this._uid : Date.now())
  },
  watch: {
    latitude() {
      this.scheduleRedraw()
    },
    longitude() {
      this.scheduleRedraw()
    },
    scale() {
      this.scheduleRedraw()
    },
    markers: {
      deep: true,
      handler() {
        this.scheduleRedraw()
      }
    },
    polylines: {
      deep: true,
      handler() {
        this.scheduleRedraw()
      }
    },
    polygons: {
      deep: true,
      handler() {
        this.scheduleRedraw()
      }
    }
  },
  mounted() {
    this.initMap()
    if (typeof window !== 'undefined') {
      this._onResize = () => {
        if (this._map) this._map.checkResize()
      }
      window.addEventListener('resize', this._onResize)
    }
  },
  beforeDestroy() {
    if (typeof window !== 'undefined' && this._onResize) {
      window.removeEventListener('resize', this._onResize)
    }
    this._map = null
    if (this._raf) {
      cancelAnimationFrame(this._raf)
      this._raf = 0
    }
  },
  methods: {
    scheduleRedraw() {
      if (!this._map) return
      if (this._raf) cancelAnimationFrame(this._raf)
      this._raf = requestAnimationFrame(() => {
        this._raf = 0
        this.redrawOverlays()
      })
    },
    async initMap() {
      this.hint = ''
      const ak = getBaiduMapAk()
      if (!ak) {
        this.hint = '未配置百度地图 AK'
        return
      }
      let BMapCtor
      try {
        BMapCtor = await loadBaiduMapApi(ak)
      } catch (e) {
        this.hint = '地图脚本加载失败，请检查网络与 AK'
        return
      }
      await this.$nextTick()
      const tryMount = (retry) => {
        const el =
          typeof document !== 'undefined' ? document.getElementById(this.mapId) : null
        if (!el) {
          if (retry < 8) {
            setTimeout(() => tryMount(retry + 1), 50)
          } else {
            this.hint = '地图容器未就绪'
          }
          return
        }
        this._map = new BMapCtor.Map(el)
        this._map.enableScrollWheelZoom(true)
        try {
          this._map.setMinZoom(15)
          this._map.setMaxZoom(20)
        } catch (e) {
          /* ignore */
        }
        this.hideMapLabels()
        try {
          this._map.addControl(new BMapCtor.NavigationControl())
        } catch (e) {
          /* ignore */
        }
        this.redrawFull(BMapCtor)
        setTimeout(() => {
          if (this._map) this._map.checkResize()
        }, 200)
      }
      tryMount(0)
    },
    redrawFull(BMapCtor) {
      if (!this._map || !BMapCtor) return
      const pt = new BMapCtor.Point(this.longitude, this.latitude)
      this._map.centerAndZoom(pt, this.scale)
      this.hideMapLabels()
      this.redrawOverlays(BMapCtor)
    },
    hideMapLabels() {
      if (!this._map) return
      try {
        this._map.setDisplayOptions?.({
          poiText: false,
          poiIcon: false,
          indoor: false,
          overlay: true
        })
      } catch (e) {
        /* ignore */
      }
      try {
        this._map.setMapStyleV2?.({ styleJson: CLEAN_BMAP_STYLE })
      } catch (e) {
        /* ignore */
      }
      try {
        this._map.setMapStyle?.({ styleJson: CLEAN_BMAP_STYLE })
      } catch (e) {
        /* ignore */
      }
    },
    redrawOverlays(BMapCtor) {
      if (!this._map) return
      const B = BMapCtor || window.BMap
      if (!B) return
      this._map.clearOverlays()
      const pt = new B.Point(this.longitude, this.latitude)
      this._map.centerAndZoom(pt, this.scale)
      this.hideMapLabels()

      const list = this.markers || []
      list.forEach((m) => {
        if (m.latitude == null || m.longitude == null) return
        const p = new B.Point(m.longitude, m.latitude)
        const mk = new B.Marker(p)
        this._map.addOverlay(mk)
      })

      const lines = this.polylines || []
      lines.forEach((line) => {
        const pts = (line.points || [])
          .filter((q) => q.latitude != null && q.longitude != null)
          .map((q) => new B.Point(q.longitude, q.latitude))
        if (pts.length < 2) return
        const { color, opacity } = parseStroke(line.color)
        const poly = new B.Polyline(pts, {
          strokeColor: color,
          strokeWeight: line.width || 4,
          strokeOpacity: opacity
        })
        this._map.addOverlay(poly)
      })

      const polys = this.polygons || []
      polys.forEach((pg) => {
        const pts = (pg.points || [])
          .filter((q) => q.latitude != null && q.longitude != null)
          .map((q) => new B.Point(q.longitude, q.latitude))
        if (pts.length < 3) return
        const stroke = (pg.strokeColor || '#dc2626').replace(/\s/g, '')
        const fill = (pg.fillColor || '#dc262633').replace(/\s/g, '')
        const polygon = new B.Polygon(pts, {
          strokeColor: stroke.length >= 7 ? stroke.slice(0, 7) : '#dc2626',
          strokeWeight: pg.strokeWidth || 2,
          strokeOpacity: 0.9,
          fillColor: fill.length >= 7 ? fill.slice(0, 7) : '#dc2626',
          fillOpacity: 0.18
        })
        this._map.addOverlay(polygon)
      })
    }
  }
}
// #endif

// #ifndef H5
export default {
  name: 'BaiduMapWeb'
}
// #endif
</script>

<style scoped>
.bmap-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 120px;
  overflow: hidden;
  border-radius: inherit;
}
.bmap-inner {
  width: 100%;
  height: 100%;
  min-height: 120px;
}
.bmap-hint {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  font-size: 13px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.92);
  text-align: center;
  box-sizing: border-box;
}
</style>

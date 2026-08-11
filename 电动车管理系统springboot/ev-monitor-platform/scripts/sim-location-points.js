#!/usr/bin/env node

const fs = require('fs')
const path = require('path')
const { spawn } = require('child_process')

const SCRIPT_PATH = __filename
const RUNTIME_DIR = path.join(__dirname, '.sim-location-runtime')
const PID_FILE = path.join(RUNTIME_DIR, 'sim-location.pid')
const STATE_FILE = path.join(RUNTIME_DIR, 'sim-location.state.json')
const LOG_FILE = path.join(RUNTIME_DIR, 'sim-location.log')

const DEFAULT_CONFIG = {
  apiBase: 'http://localhost:8083/api/location',
  token: '',
  count: 20,
  interval: 5000,
  ttlMinutes: 60,
  centerLng: 112.6827629,
  centerLat: 26.8860959,
  radiusMeters: 220,
  platePrefix: '湘D',
  speedMax: 24,
  batteryMin: 35,
  batteryMax: 98,
  clearOnStop: false
}

function ensureRuntimeDir() {
  fs.mkdirSync(RUNTIME_DIR, { recursive: true })
}

function readJson(filePath, fallback) {
  try {
    if (!fs.existsSync(filePath)) return fallback
    return JSON.parse(fs.readFileSync(filePath, 'utf8'))
  } catch {
    return fallback
  }
}

function writeJson(filePath, value) {
  ensureRuntimeDir()
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function removeFileIfExists(filePath) {
  try {
    if (fs.existsSync(filePath)) fs.unlinkSync(filePath)
  } catch {
    // ignore
  }
}

function readPid() {
  if (!fs.existsSync(PID_FILE)) return null
  const value = String(fs.readFileSync(PID_FILE, 'utf8')).trim()
  const pid = Number(value)
  return Number.isInteger(pid) && pid > 0 ? pid : null
}

function writePid(pid) {
  ensureRuntimeDir()
  fs.writeFileSync(PID_FILE, `${pid}\n`, 'utf8')
}

function isProcessRunning(pid) {
  if (!pid) return false
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function rand(min, max) {
  return min + Math.random() * (max - min)
}

function randInt(min, max) {
  return Math.round(rand(min, max))
}

function normalizeApiBase(raw) {
  const text = String(raw || '').trim().replace(/\/+$/, '')
  if (!text) return DEFAULT_CONFIG.apiBase
  if (text.endsWith('/api/location')) return text
  if (text.endsWith('/api')) return `${text}/location`
  if (/^https?:\/\//i.test(text)) return `${text}/api/location`
  return text
}

function parseBool(value, fallback) {
  if (value === undefined) return fallback
  if (typeof value === 'boolean') return value
  const text = String(value).trim().toLowerCase()
  if (['1', 'true', 'yes', 'y', 'on'].includes(text)) return true
  if (['0', 'false', 'no', 'n', 'off'].includes(text)) return false
  return fallback
}

function parseArgs(argv) {
  const args = [...argv]
  const command = args.shift() || 'help'
  const options = {}

  while (args.length > 0) {
    const current = args.shift()
    if (!current.startsWith('--')) continue

    const body = current.slice(2)
    if (!body) continue

    if (body.includes('=')) {
      const [key, ...rest] = body.split('=')
      options[key] = rest.join('=')
      continue
    }

    if (body.startsWith('no-')) {
      options[body.slice(3)] = false
      continue
    }

    if (args[0] && !args[0].startsWith('--')) {
      options[body] = args.shift()
    } else {
      options[body] = true
    }
  }

  return { command, options }
}

function buildConfig(options = {}) {
  const apiBase = normalizeApiBase(options.apiBase || options.api || options.origin)
  const config = {
    apiBase,
    token: String(options.token ?? process.env.SIM_LOCATION_TOKEN ?? DEFAULT_CONFIG.token).trim(),
    count: clamp(Number(options.count ?? DEFAULT_CONFIG.count), 1, 200),
    interval: clamp(Number(options.interval ?? DEFAULT_CONFIG.interval), 1000, 60000),
    ttlMinutes: clamp(Number(options.ttl ?? options.ttlMinutes ?? DEFAULT_CONFIG.ttlMinutes), 1, 1440 * 7),
    centerLng: Number(options.centerLng ?? DEFAULT_CONFIG.centerLng),
    centerLat: Number(options.centerLat ?? DEFAULT_CONFIG.centerLat),
    radiusMeters: clamp(Number(options.radius ?? options.radiusMeters ?? DEFAULT_CONFIG.radiusMeters), 20, 5000),
    platePrefix: String(options.platePrefix ?? DEFAULT_CONFIG.platePrefix).trim() || DEFAULT_CONFIG.platePrefix,
    speedMax: clamp(Number(options.speedMax ?? DEFAULT_CONFIG.speedMax), 5, 80),
    batteryMin: clamp(Number(options.batteryMin ?? DEFAULT_CONFIG.batteryMin), 1, 100),
    batteryMax: clamp(Number(options.batteryMax ?? DEFAULT_CONFIG.batteryMax), 1, 100),
    clearOnStop: parseBool(options.clearOnStop, DEFAULT_CONFIG.clearOnStop)
  }

  if (!Number.isFinite(config.centerLng) || !Number.isFinite(config.centerLat)) {
    throw new Error('中心点坐标无效，请检查 --centerLng 和 --centerLat')
  }

  if (config.batteryMin > config.batteryMax) {
    const swapped = config.batteryMin
    config.batteryMin = config.batteryMax
    config.batteryMax = swapped
  }

  return config
}

function stateWithDefaults() {
  return {
    running: false,
    pid: null,
    startedAt: null,
    lastPushAt: null,
    ticks: 0,
    simulatedCount: 0,
    apiBase: DEFAULT_CONFIG.apiBase,
    config: DEFAULT_CONFIG,
    lastError: null,
    lastErrorAt: null,
    stopReason: null
  }
}

function saveState(patch) {
  const current = readJson(STATE_FILE, stateWithDefaults())
  writeJson(STATE_FILE, { ...current, ...patch })
}

function loadState() {
  return readJson(STATE_FILE, stateWithDefaults())
}

async function requestJson(method, url, body) {
  const token = requestJson._token ? String(requestJson._token).trim() : ''
  const response = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}`, token } : {})
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(10000)
  })

  const text = await response.text()
  let payload = null
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = { message: text || '非 JSON 响应' }
  }

  if (!response.ok) {
    const message = payload?.message || payload?.msg || `${response.status} ${response.statusText}`
    throw new Error(message)
  }

  return payload
}

function buildSeedPoints(config) {
  return Array.from({ length: config.count }, (_, index) => ({
    id: `sim-${String(index + 1).padStart(2, '0')}`,
    licensePlate: `${config.platePrefix}${String(1000 + index).padStart(4, '0')}`,
    ownerName: `学生${String(index + 1).padStart(2, '0')}`,
    angle: (Math.PI * 2 * index) / config.count,
    distanceMeters: rand(config.radiusMeters * 0.28, config.radiusMeters * 0.92),
    orbitSpeed: rand(0.03, 0.08),
    battery: randInt(config.batteryMin + 8, config.batteryMax),
    speed: randInt(6, config.speedMax),
    holdTicks: 0
  }))
}

function polarToLngLat(centerLng, centerLat, distanceMeters, angleRad) {
  const latMeters = distanceMeters * Math.sin(angleRad)
  const lngMeters = distanceMeters * Math.cos(angleRad)
  const latDelta = latMeters / 111320
  const lngDelta = lngMeters / (111320 * Math.cos((centerLat * Math.PI) / 180))

  return {
    lng: Number((centerLng + lngDelta).toFixed(6)),
    lat: Number((centerLat + latDelta).toFixed(6))
  }
}

function nextPayloadPoints(seedPoints, config, tick) {
  return seedPoints.map((point, index) => {
    point.angle += point.orbitSpeed * rand(0.75, 1.25)

    if (Math.random() < 0.16) {
      point.distanceMeters = clamp(
        point.distanceMeters + rand(-18, 18),
        config.radiusMeters * 0.18,
        config.radiusMeters * 1.08
      )
    }

    if (point.holdTicks > 0) {
      point.holdTicks -= 1
      point.speed = 0
    } else if (Math.random() < 0.12) {
      point.holdTicks = randInt(1, 3)
      point.speed = 0
    } else {
      point.speed = clamp(point.speed + randInt(-5, 5), 6, config.speedMax)
    }

    if (Math.random() < 0.45) {
      point.battery = clamp(point.battery + randInt(-2, 1), config.batteryMin, config.batteryMax)
    }

    const wave = Math.sin((tick + index) / 3) * 10
    const location = polarToLngLat(
      config.centerLng,
      config.centerLat,
      point.distanceMeters + wave,
      point.angle
    )

    return {
      id: point.id,
      licensePlate: point.licensePlate,
      ownerName: point.ownerName,
      lng: location.lng,
      lat: location.lat,
      speed: point.speed,
      battery: point.battery
    }
  })
}

async function pushBatch(config, points) {
  const response = await requestJson('POST', `${config.apiBase}/sim/batch`, {
    ttlMinutes: config.ttlMinutes,
    points
  })

  if (response?.code !== 200) {
    throw new Error(response?.message || response?.msg || '模拟点写入失败')
  }

  return typeof response.data === 'number' ? response.data : points.length
}

async function clearSimPoints(config) {
  const response = await requestJson('DELETE', `${config.apiBase}/sim`)
  if (response?.code !== 200) {
    throw new Error(response?.message || response?.msg || '清空模拟点失败')
  }
}

async function fetchLocationSummary(config) {
  const response = await requestJson('GET', `${config.apiBase}/all`)
  if (response?.code !== 200 || !Array.isArray(response.data)) {
    return { total: 0, simulated: 0 }
  }

  const rows = response.data
  const simulated = rows.filter((item) => String(item?.deviceId || '').startsWith('sim:')).length
  return {
    total: rows.length,
    simulated
  }
}

function configToArgs(config) {
  return [
    'run',
    `--apiBase=${config.apiBase}`,
    ...(config.token ? [`--token=${config.token}`] : []),
    `--count=${config.count}`,
    `--interval=${config.interval}`,
    `--ttlMinutes=${config.ttlMinutes}`,
    `--centerLng=${config.centerLng}`,
    `--centerLat=${config.centerLat}`,
    `--radiusMeters=${config.radiusMeters}`,
    `--platePrefix=${config.platePrefix}`,
    `--speedMax=${config.speedMax}`,
    `--batteryMin=${config.batteryMin}`,
    `--batteryMax=${config.batteryMax}`,
    `--clearOnStop=${config.clearOnStop}`
  ]
}

function printUsage() {
  const text = `
模拟定位监测点脚本

用法:
  node scripts/sim-location-points.js start
  node scripts/sim-location-points.js stop
  node scripts/sim-location-points.js once
  node scripts/sim-location-points.js clear
  node scripts/sim-location-points.js status

常用参数:
  --count=20              模拟点数量
  --interval=5000         后台刷新间隔(毫秒)
  --ttlMinutes=60         模拟点在 Redis 中存活时间(分钟)
  --centerLng=112.6827629 地图中心经度
  --centerLat=26.8860959  地图中心纬度
  --radiusMeters=220      围绕中心扩散半径(米)
  --platePrefix=湘D       模拟车牌前缀
  --apiBase=http://localhost:8083/api/location
  --token=xxxx            可选，接口启用鉴权时传登录 token
  --clearOnStop=true      停止后台脚本时自动清空模拟点

示例:
  node scripts/sim-location-points.js start --count=24 --interval=4000
  node scripts/sim-location-points.js once --count=12
  node scripts/sim-location-points.js clear
`
  process.stdout.write(text.trimStart() + '\n')
}

async function runOnce(config) {
  const seedPoints = buildSeedPoints(config)
  const points = nextPayloadPoints(seedPoints, config, 0)
  const saved = await pushBatch(config, points)
  process.stdout.write(`已推送 ${saved} 个模拟定位监测点到 ${config.apiBase}\n`)
}

async function runDaemon(config) {
  ensureRuntimeDir()
  writePid(process.pid)
  saveState({
    running: true,
    pid: process.pid,
    startedAt: new Date().toISOString(),
    lastPushAt: null,
    ticks: 0,
    simulatedCount: 0,
    apiBase: config.apiBase,
    config,
    lastError: null,
    lastErrorAt: null,
    stopReason: null
  })

  const seedPoints = buildSeedPoints(config)
  let ticks = 0
  let shuttingDown = false
  let intervalId = null

  const cleanup = async (reason) => {
    if (shuttingDown) return
    shuttingDown = true

    if (intervalId) clearInterval(intervalId)

    try {
      if (config.clearOnStop) {
        await clearSimPoints(config)
      }
    } catch (error) {
      saveState({
        lastError: error.message,
        lastErrorAt: new Date().toISOString()
      })
    }

    saveState({
      running: false,
      pid: null,
      stopReason: reason,
      stoppedAt: new Date().toISOString()
    })
    removeFileIfExists(PID_FILE)
    process.exit(0)
  }

  process.on('SIGINT', () => cleanup('SIGINT'))
  process.on('SIGTERM', () => cleanup('SIGTERM'))
  process.on('SIGHUP', () => cleanup('SIGHUP'))

  process.on('uncaughtException', (error) => {
    saveState({
      running: false,
      pid: null,
      lastError: error.stack || error.message,
      lastErrorAt: new Date().toISOString(),
      stopReason: 'uncaughtException'
    })
    removeFileIfExists(PID_FILE)
    process.exit(1)
  })

  process.on('unhandledRejection', (error) => {
    saveState({
      running: false,
      pid: null,
      lastError: String(error?.stack || error?.message || error),
      lastErrorAt: new Date().toISOString(),
      stopReason: 'unhandledRejection'
    })
    removeFileIfExists(PID_FILE)
    process.exit(1)
  })

  const tick = async () => {
    try {
      const points = nextPayloadPoints(seedPoints, config, ticks)
      const saved = await pushBatch(config, points)
      ticks += 1
      saveState({
        running: true,
        pid: process.pid,
        lastPushAt: new Date().toISOString(),
        ticks,
        simulatedCount: saved,
        lastError: null,
        lastErrorAt: null
      })
    } catch (error) {
      saveState({
        running: true,
        pid: process.pid,
        lastError: error.message,
        lastErrorAt: new Date().toISOString()
      })
    }
  }

  await tick()
  intervalId = setInterval(tick, config.interval)
}

async function startBackground(config) {
  const currentPid = readPid()
  if (currentPid && isProcessRunning(currentPid)) {
    console.log(`模拟脚本已在运行，PID=${currentPid}`)
    return
  }

  ensureRuntimeDir()
  const logFd = fs.openSync(LOG_FILE, 'a')
  const child = spawn(process.execPath, [SCRIPT_PATH, ...configToArgs(config)], {
    cwd: process.cwd(),
    detached: true,
    stdio: ['ignore', logFd, logFd],
    windowsHide: true
  })

  fs.closeSync(logFd)
  child.unref()

  await sleep(600)

  const pid = readPid() || child.pid
  console.log(`模拟定位监测点已启动，PID=${pid}`)
  console.log(`日志文件: ${LOG_FILE}`)
}

async function stopBackground(config, options) {
  const state = loadState()
  const effectiveConfig = state?.config?.apiBase ? state.config : config
  const shouldClear = parseBool(options.clear, parseBool(effectiveConfig.clearOnStop, false))
  const pid = readPid()
  if (!pid || !isProcessRunning(pid)) {
    removeFileIfExists(PID_FILE)
    saveState({ running: false, pid: null })
    process.stdout.write('当前没有运行中的模拟脚本\n')
    if (shouldClear) {
      await clearSimPoints(effectiveConfig)
      process.stdout.write('已额外清空模拟点\n')
    }
    return
  }

  process.kill(pid)

  for (let index = 0; index < 20; index += 1) {
    await sleep(200)
    if (!isProcessRunning(pid)) break
  }

  if (!isProcessRunning(pid)) {
    removeFileIfExists(PID_FILE)
    saveState({
      running: false,
      pid: null,
      stoppedAt: new Date().toISOString(),
      stopReason: 'manual-stop'
    })
  }

  process.stdout.write(`已发送停止信号到 PID=${pid}\n`)

  if (shouldClear) {
    await clearSimPoints(effectiveConfig)
    process.stdout.write('已清空模拟点\n')
  }
}

async function printStatus(config) {
  const pid = readPid()
  const running = pid ? isProcessRunning(pid) : false
  const state = loadState()
  const effectiveConfig = state?.config?.apiBase ? state.config : config

  process.stdout.write(`运行状态: ${running ? '运行中' : '未运行'}\n`)
  process.stdout.write(`PID: ${running ? pid : '—'}\n`)
  process.stdout.write(`接口地址: ${effectiveConfig.apiBase}\n`)
  process.stdout.write(`点位数量: ${effectiveConfig.count}\n`)
  process.stdout.write(`刷新间隔: ${effectiveConfig.interval} ms\n`)
  process.stdout.write(`TTL: ${effectiveConfig.ttlMinutes} 分钟\n`)
  process.stdout.write(`最近推送: ${state.lastPushAt || '—'}\n`)
  process.stdout.write(`累计推送轮次: ${state.ticks || 0}\n`)

  if (state.lastError) {
    process.stdout.write(`最近错误: ${state.lastError}\n`)
  }

  try {
    const summary = await fetchLocationSummary(effectiveConfig)
    process.stdout.write(`接口当前点位: 总计 ${summary.total}，其中模拟点 ${summary.simulated}\n`)
  } catch (error) {
    process.stdout.write(`接口探测失败: ${error.message}\n`)
  }
}

async function main() {
  const { command, options } = parseArgs(process.argv.slice(2))
  const config = buildConfig(options)
  requestJson._token = config.token

  switch (command) {
    case 'start':
      await startBackground(config)
      return
    case 'run':
      await runDaemon(config)
      return
    case 'stop':
      await stopBackground(config, options)
      return
    case 'once':
      await runOnce(config)
      return
    case 'clear':
      await clearSimPoints(config)
      process.stdout.write('已清空模拟点\n')
      return
    case 'status':
      await printStatus(config)
      return
    case 'help':
    case '--help':
    case '-h':
    default:
      printUsage()
  }
}

main().catch((error) => {
  process.stderr.write(`脚本执行失败: ${error.message}\n`)
  process.exit(1)
})

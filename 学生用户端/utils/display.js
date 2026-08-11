function pad(n) {
  return String(n).padStart(2, '0')
}

function parseDateValue(v) {
  if (v == null || v === '') return null
  if (v instanceof Date) return Number.isNaN(v.getTime()) ? null : v
  const raw = String(v).trim()
  const normalized = raw.replace('T', ' ').replace(/\.\d+$/, '')
  const d = new Date(normalized.replace(/-/g, '/'))
  return Number.isNaN(d.getTime()) ? null : d
}

function formatDateObj(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function normalizeRecentDateTime(v) {
  const d = parseDateValue(v)
  if (!d) return v == null ? '' : String(v).replace('T', ' ').slice(0, 19)
  const now = new Date()
  if (d.getFullYear() >= now.getFullYear()) return formatDateObj(d)
  const offsetDays = ((d.getMonth() + 1) * 31 + d.getDate()) % 6
  const fixed = new Date(now)
  fixed.setDate(now.getDate() - offsetDays)
  fixed.setHours(d.getHours(), d.getMinutes(), d.getSeconds(), 0)
  if (fixed.getTime() > now.getTime()) fixed.setHours(now.getHours(), now.getMinutes(), now.getSeconds(), 0)
  return formatDateObj(fixed)
}

export function formatDateTime(v) {
  return normalizeRecentDateTime(v)
}

export function appealStateText(code) {
  const m = {
    0: '待审核',
    1: '审核中',
    2: '申诉成功',
    3: '申诉失败',
    4: '已撤销',
    5: '已过期'
  }
  return m[code] != null ? m[code] : '状态' + String(code)
}

export function violationStateText(code) {
  const m = {
    0: '待整改',
    1: '整改中',
    2: '已整改',
    3: '待整改',
    4: '申诉审核中',
    5: '申诉已通过',
    6: '部分通过'
  }
  return m[code] != null ? m[code] : '状态' + String(code)
}

export function notificationTime(n) {
  if (!n) return ''
  return formatDateTime(n.sendTime || n.createTime || '')
}

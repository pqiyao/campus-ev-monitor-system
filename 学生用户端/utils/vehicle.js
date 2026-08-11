import { getVehicleList } from '../api/index.js'

export async function fetchMyVehicle(userId) {
  if (!userId) return null
  const pickLatest = (rows) => {
    const r = rows.slice().sort((a, b) => (b.vehicleId || 0) - (a.vehicleId || 0))
    return r[0] || null
  }
  try {
    const { list } = await getVehicleList(1, 40, userId)
    const all = list || []
    const mine = all.filter((v) => v.userId === userId)
    if (mine.length) return pickLatest(mine)
    if (!all.length) return null
    const { list: list2 } = await getVehicleList(1, 150)
    const mine2 = (list2 || []).filter((v) => v.userId === userId)
    return pickLatest(mine2)
  } catch (e) {
    return null
  }
}

export function vehicleStatusText(status) {
  const m = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return m[status] || status || '未知'
}

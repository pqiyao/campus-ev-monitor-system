/** 后端 HTTP 根（不含路径），默认本地；可在 .env 设置 VITE_API_ORIGIN */
export function getApiOrigin(): string {
  const v = import.meta.env.VITE_API_ORIGIN as string | undefined
  if (v && String(v).trim()) return String(v).replace(/\/$/, '')
  return 'http://localhost:8083'
}

export function getApiBaseUrl(): string {
  return `${getApiOrigin()}/api`
}

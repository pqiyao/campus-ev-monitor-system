/** 后端 HTTP 根（不含路径）。生产环境默认同源，由 Nginx 代理 /api。 */
export function getApiOrigin(): string {
  const v = import.meta.env.VITE_API_ORIGIN as string | undefined
  if (v && String(v).trim()) return String(v).replace(/\/$/, '')
  return ''
}

export function getApiBaseUrl(): string {
  return `${getApiOrigin()}/api`
}

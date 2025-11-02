export function setToken(token: string, days = 7) {
  const expires = new Date()
  expires.setDate(expires.getDate() + days)
  document.cookie = `mcga_token=${encodeURIComponent(token)}; path=/; expires=${expires.toUTCString()}`
}

export function getToken(): string | null {
  const m = document.cookie.match(/(?:^|; )mcga_token=([^;]+)/)
  return m ? decodeURIComponent(m[1]) : null
}

export function clearToken() {
  document.cookie = `mcga_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`
}

// baseUrl helpers (store backend base URL in cookie `mcga_baseurl`)
export function setBaseUrl(url: string, days = 7) {
  const expires = new Date()
  expires.setDate(expires.getDate() + days)
  document.cookie = `mcga_baseurl=${encodeURIComponent(url)}; path=/; expires=${expires.toUTCString()}`
}

export function getBaseUrl(): string | null {
  const m = document.cookie.match(/(?:^|; )mcga_baseurl=([^;]+)/)
  return m ? decodeURIComponent(m[1]) : null
}

export function clearBaseUrl() {
  document.cookie = `mcga_baseurl=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`
}

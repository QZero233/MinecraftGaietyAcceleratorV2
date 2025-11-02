import axios from 'axios'
import { getToken, getBaseUrl } from './utils/cookie'

// Create axios instance without fixed baseURL: we'll set baseURL per-request from cookie
const client = axios.create({
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000
})

// Add token and dynamic baseURL automatically
client.interceptors.request.use(cfg => {
  try {
    const token = getToken()
    if (token) cfg.headers = { ...cfg.headers, Authorization: token }
    const baseUrl = getBaseUrl()
    if (baseUrl) cfg.baseURL = baseUrl
  } catch (e) {
    // swallow any cookie read errors and proceed with existing config
  }
  return cfg
})

export type ActionResult = { responseCode?: any, message?: string, data?: any }

// Server controller endpoints
export async function listServers(): Promise<ActionResult> {
  const r = await client.get('/server/')
  return r.data
}

export async function startServer(serverName: string) {
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/start`)
  return r.data
}

export async function stopServer(serverName: string) {
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/stop`)
  return r.data
}

export async function sendCommand(serverName: string, command: string) {
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/command`, null, { params: { command } })
  return r.data
}

export async function sendCommandRcon(serverName: string, command: string) {
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/rcon`, null, { params: { command } })
  return r.data
}

export async function listProperties(serverName: string) {
  const r = await client.get(`/server/${encodeURIComponent(serverName)}/properties`)
  return r.data
}

export async function updateProperty(serverName: string, key: string, value: string) {
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/property`, null, { params: { key, value } })
  return r.data
}

export async function reloadServerContainer(serverName: string) {
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/reload`)
  return r.data
}

export async function loadMap(serverName: string, mapName: string) {
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/loadMap`, null, { params: { mapName } })
  return r.data
}

export async function backupMap(serverName: string, fileName?: string) {
  const opts = fileName ? { params: { fileName } } : undefined
  const r = await client.post(`/server/${encodeURIComponent(serverName)}/backup`, null, opts)
  return r.data
}

// Chest info
export async function chestInfo(coords: { x1:number,y1:number,z1:number,x2:number,y2:number,z2:number }) {
  const r = await client.get('/chest_info', { params: coords })
  return r.data
}

// Overhead
export async function getOverhead() {
  const r = await client.get('/stat/overhead')
  return r.data
}

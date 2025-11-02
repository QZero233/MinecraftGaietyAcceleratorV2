<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:center; gap:12px">
      <h3 style="margin:0">服务器列表</h3>
    </div>

    <div v-if="error" class="small" style="margin-top:8px">错误：{{error}}</div>

    <div class="server-grid" style="margin-top:12px">
      <div v-for="s in servers" :key="s.serverName" class="server-card">
        <div class="server-card-header">
          <div>
            <div class="server-name">{{ s.serverName }}</div>
            <div class="small server-meta">Jar: {{ s.serverJarName || '—' }}</div>
          </div>
          <div style="display:flex; flex-direction:column; align-items:flex-end; gap:6px">
            <div :class="['badge', statusClass(s.serverStatus)]">{{ s.serverStatus || 'UNKNOWN' }}</div>
          </div>
        </div>

        <div class="server-card-body">
          <div class="form-row" style="margin-top:8px">
            <!-- 启动/停止：运行时启动不可点、停止可点；停止时启动可点、停止不可点 -->
            <button
              :class="['btn', isRunning(s.serverStatus) ? 'btn-disabled' : 'btn-primary']"
              :disabled="isRunning(s.serverStatus)"
              @click="start(s.serverName)">
              启动
            </button>

            <button
              :class="['btn', isRunning(s.serverStatus) ? 'btn-danger' : 'btn-disabled']"
              :disabled="!isRunning(s.serverStatus)"
              @click="stop(s.serverName)">
              停止
            </button>
          </div>
        </div>

      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as api from '../api'

const servers = ref<any[]>([])
const error = ref<string | null>(null)
const loading = ref(false)

function isRunning(status: string | undefined) {
  if (!status) return false
  const s = String(status).toLowerCase()
  return s.includes('run') || s.includes('up') || s.includes('online')
}

function statusClass(status: string | undefined) {
  if (!status) return 'badge-unknown'
  const s = String(status).toLowerCase()
  if (s.includes('run') || s.includes('up') || s.includes('online')) return 'badge-running'
  if (s.includes('stop') || s.includes('down') || s.includes('off')) return 'badge-stopped'
  return 'badge-unknown'
}

async function reload() {
  loading.value = true
  try {
    error.value = null
    const res = await api.listServers()
    servers.value = res.data?.servers || []
  } catch (e:any) { error.value = e?.message || String(e) }
  finally { loading.value = false }
}

async function start(name:string){
  try { await api.startServer(name); await reload() } catch(e:any){ error.value = String(e) }
}

async function stop(name:string){
  try { await api.stopServer(name); await reload() } catch(e:any){ error.value = String(e) }
}

onMounted(() => reload())
</script>

<style>
.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
}

.server-card {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.server-card-header {
  background: #f5f5f5;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.server-name {
  font-weight: 500;
  font-size: 16px;
}

.server-meta {
  color: #666;
}

.badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  text-transform: uppercase;
}

.badge-running {
  background: #d4edda;
  color: #155724;
}

.badge-stopped {
  background: #f8d7da;
  color: #721c24;
}

.badge-unknown {
  background: #e2e3e5;
  color: #383d41;
}

.server-card-body {
  padding: 12px;
  flex: 1;
}

.form-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn {
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #007bff;
  color: #fff;
}

.btn-danger {
  background: #dc3545;
  color: #fff;
}

.btn-disabled {
  background: #9ca3af;
  color: #fff;
}

.server-card-footer {
  background: #f1f1f1;
  padding: 8px;
  text-align: right;
}

.spinner {
  display:inline-block;
  width:14px;
  height:14px;
  border:2px solid rgba(0,0,0,0.1);
  border-left-color: #fff;
  border-radius:50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg) } }

.mono { font-family: ui-monospace, "SFMono-Regular", Menlo, Monaco, "Roboto Mono", "Segoe UI Mono", monospace; }
</style>

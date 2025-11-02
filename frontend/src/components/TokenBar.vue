<template>
  <div class="card header">
    <div>
      <strong>MCGA 控制台</strong>
      <div class="small">请输入 token 与后端地址；保存后会存入 cookie（mcga_token / mcga_baseurl）。</div>
    </div>
    <div style="display:flex; gap:8px; align-items:center">
      <input v-model="tokenInput" placeholder="令牌（token）" />
      <input v-model="baseUrlInput" placeholder="后端地址，例如 http://localhost:8080" style="width:320px" />
      <button class="btn btn-primary" @click="save">保存</button>
      <button class="btn btn-ghost" @click="clearLocal">清除</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { setToken, getToken, clearToken, setBaseUrl, getBaseUrl, clearBaseUrl } from '../utils/cookie'

const tokenInput = ref<string>(getToken() || '')
const baseUrlInput = ref<string>(getBaseUrl() || '')

function save() {
  setToken(tokenInput.value || '')
  setBaseUrl(baseUrlInput.value || '')
  window.alert('已保存令牌与后端地址')
}

function clearLocal() {
  clearToken()
  clearBaseUrl()
  tokenInput.value = ''
  baseUrlInput.value = ''
  window.alert('已清除令牌与后端地址')
}
</script>

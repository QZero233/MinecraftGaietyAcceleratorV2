<template>
  <div class="card">
    <h3>服务器聊天控制（管理员）</h3>

    <div class="chat-window">
      <div v-for="(m, idx) in messages" :key="idx" :class="['chat-row', m.from]">
        <div class="chat-bubble">{{ m.text }}</div>
      </div>
    </div>

    <div class="chat-input">
      <input v-model="input" @keyup.enter="send" placeholder="输入指令或消息，按回车发送" />
      <button class="btn btn-primary" @click="send" :disabled="sending || !input.trim()">发送</button>
      <button class="btn btn-ghost" @click="clear">清空</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import * as api from '../api'

const messages = ref<{ from: 'me'|'bot', text: string }[]>([])
const input = ref('')
const sending = ref(false)

async function send() {
  const text = input.value.trim()
  if (!text) return
  messages.value.push({ from: 'me', text })
  input.value = ''
  sending.value = true
  try {
    const res = await api.adminChat(text)
    // backend returns result in various shapes; display raw
    const botText = typeof res === 'string' ? res : JSON.stringify(res)
    messages.value.push({ from: 'bot', text: botText })
  } catch (e:any) {
    messages.value.push({ from: 'bot', text: '错误：' + (e?.message || String(e)) })
  } finally {
    sending.value = false
  }
}

function clear() {
  messages.value = []
  input.value = ''
}
</script>

<style>
.chat-window {
  max-height: 240px;
  overflow: auto;
  padding: 8px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 6px;
}
.chat-row { display:flex; margin:6px 0 }
.chat-row.me { justify-content: flex-end }
.chat-row.bot { justify-content: flex-start }
.chat-bubble {
  max-width: 80%;
  padding: 8px 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
}
.chat-row.me .chat-bubble { background:#dbeafe; border-color:#7dd3fc }

.chat-input { display:flex; gap:8px; margin-top:8px; align-items:center }
.chat-input input { flex:1; padding:8px; border-radius:6px; border:1px solid #ccc }
</style>


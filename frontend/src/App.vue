<template>
  <div>
    <TokenBar />

    <div class="card" style="margin-top:12px">
      <ServerPanel />
    </div>

    <!-- 管理员聊天控制放在页面底部 -->
    <div style="margin-top:12px">
      <ChatControl />
    </div>
  </div>
</template>

<script lang="ts">
import TokenBar from './components/TokenBar.vue'
import ServerPanel from './components/ServerPanel.vue'
import ChatControl from './components/ChatControl.vue'

export default {
  components: { TokenBar, ServerPanel, ChatControl }
}
</script>

<style>
/* 仅保留默认样式；文本显示由组件内控制（已翻译为中文） */
</style>

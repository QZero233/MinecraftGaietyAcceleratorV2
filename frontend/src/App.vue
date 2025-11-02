<template>
  <div>
    <TokenBar />

    <div class="card" style="margin-top:12px">
      <ServerPanel />
    </div>
  </div>
</template>

<script lang="ts">
import TokenBar from './components/TokenBar.vue'
import ServerPanel from './components/ServerPanel.vue'

export default {
  components: { TokenBar, ServerPanel }
}
</script>

<style>
/* 仅保留默认样式；文本显示由组件内控制（已翻译为中文） */
</style>

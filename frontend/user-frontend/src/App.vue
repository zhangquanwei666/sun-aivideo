<template>
  <div class="shell">
    <AppHeader v-if="route.path !== '/login'" :status="status" />
    <main class="main">
      <router-view :status="status" />
    </main>
    <ToastHost />
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
import AppHeader from '@/components/AppHeader.vue'
import ToastHost from '@/components/ToastHost.vue'
import { getSystemStatus, getUserAiStatus } from '@/api'

const status = ref({ mockMode: false, llmAvailable: false })

let revision = 0
async function refreshStatus() {
  const current = ++revision
  status.value = { mockMode: false, llmAvailable: false }
  try {
    const global = await getSystemStatus()
    const personal = route.path !== '/login' && localStorage.getItem('aivideo-token') ? await getUserAiStatus() : {}
    if (current === revision) status.value = { ...global, ...personal }
  } catch { /* 接口拦截器处理登录失效；不保留上一用户状态。 */ }
}
watch(() => route.path, refreshStatus, { immediate: true })
window.addEventListener('ai-credentials-changed', refreshStatus)
onBeforeUnmount(() => { revision += 1; window.removeEventListener('ai-credentials-changed', refreshStatus) })
</script>

<style scoped>
.shell {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.main {
  flex: 1;
  min-height: 0;
}
</style>

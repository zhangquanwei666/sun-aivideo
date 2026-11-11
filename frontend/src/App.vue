<template>
  <div class="shell">
    <AppHeader :status="status" />
    <main class="main">
      <router-view :status="status" />
    </main>
    <ToastHost />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import ToastHost from '@/components/ToastHost.vue'
import { getSystemStatus } from '@/api'

const status = ref({ mockMode: false, llmAvailable: false })

onMounted(async () => {
  try {
    status.value = await getSystemStatus()
  } catch {
    // 后端未启动时保持默认
  }
})
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

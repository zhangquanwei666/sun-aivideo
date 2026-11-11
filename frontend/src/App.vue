<template>
  <div class="shell">
    <AppSidebar :status="status" />
    <main class="main">
      <router-view :status="status" />
    </main>
    <ToastHost />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
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
  height: 100%;
}
.main {
  flex: 1;
  min-width: 0;
  height: 100%;
}
@media (max-width: 720px) {
  .shell {
    flex-direction: column;
  }
  .main {
    height: auto;
    flex: 1;
    min-height: 0;
  }
}
</style>

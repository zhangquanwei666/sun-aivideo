<template>
  <div class="toasts">
    <transition-group name="toast">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="t.tone">{{ t.message }}</div>
    </transition-group>
  </div>
</template>

<script setup>
import { useToast } from '@/composables/useToast'

const { toasts } = useToast()
</script>

<style scoped>
.toasts {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 100;
  pointer-events: none;
}
.toast {
  max-width: min(520px, calc(100vw - 32px));
  padding: 9px 16px;
  border-radius: 999px;
  background: var(--surface-3);
  box-shadow: var(--shadow-pop);
  font-size: 13px;
}
.toast.success {
  color: var(--success);
}
.toast.danger {
  color: var(--danger);
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
.toast-enter-active,
.toast-leave-active {
  transition: all 0.2s;
}
</style>

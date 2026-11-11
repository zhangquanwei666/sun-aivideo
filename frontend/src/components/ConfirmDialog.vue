<template>
  <Teleport to="body">
    <transition name="dialog">
      <div v-if="open" class="backdrop" @click.self="!busy && $emit('cancel')">
        <div class="dialog" role="alertdialog" :aria-label="title">
          <div v-if="danger" class="icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" /></svg>
          </div>
          <h3 class="title">{{ title }}</h3>
          <p class="message"><slot>{{ message }}</slot></p>
          <div class="actions">
            <button class="btn btn-ghost" :disabled="busy" @click="$emit('cancel')">取消</button>
            <button ref="confirmBtn" class="btn" :class="danger ? 'btn-danger' : 'btn-primary'" :disabled="busy" @click="$emit('confirm')">
              {{ busy ? '处理中…' : confirmText }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  open: Boolean,
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '确定' },
  danger: Boolean,
  busy: Boolean
})
const emit = defineEmits(['confirm', 'cancel'])
const confirmBtn = ref(null)

// 打开时聚焦确认按钮，Esc 取消
watch(
  () => props.open,
  async (v) => {
    if (v) {
      await nextTick()
      confirmBtn.value?.focus()
    }
  }
)
function onKey(e) {
  if (props.open && !props.busy && e.key === 'Escape') emit('cancel')
}
onMounted(() => document.addEventListener('keydown', onKey))
onBeforeUnmount(() => document.removeEventListener('keydown', onKey))
</script>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.6);
}
.dialog {
  width: min(400px, 100%);
  padding: 24px;
  border-radius: var(--radius-xl);
  background: var(--surface-3);
  box-shadow: var(--shadow-pop);
}
.icon {
  width: 40px;
  height: 40px;
  margin-bottom: 14px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: var(--danger);
  background: rgba(242, 184, 181, 0.12);
}
.title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 400;
}
.message {
  margin: 0 0 24px;
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.6;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.btn-danger {
  background: var(--danger);
  color: #601410;
}
.btn-danger:hover {
  background: #f6cbc8;
}
.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 0.18s;
}
.dialog-enter-active .dialog,
.dialog-leave-active .dialog {
  transition: transform 0.18s ease;
}
.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}
.dialog-enter-from .dialog,
.dialog-leave-to .dialog {
  transform: scale(0.96);
}
</style>

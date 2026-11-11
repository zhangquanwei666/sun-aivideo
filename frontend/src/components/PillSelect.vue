<template>
  <div ref="root" class="pill-select" :class="{ open }">
    <button type="button" class="pill" :disabled="disabled" @click="open = !open">
      <slot name="icon" />
      <span class="pill-label">{{ current ? current.label : placeholder }}</span>
      <svg class="chev" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6" /></svg>
    </button>
    <div v-if="open" class="menu" :class="placement">
      <div v-if="title" class="menu-title">{{ title }}</div>
      <button
        v-for="opt in options"
        :key="opt.value"
        type="button"
        class="menu-item"
        :class="{ active: opt.value === modelValue }"
        @click="choose(opt.value)"
      >
        <span class="menu-main">
          <span>{{ opt.label }}</span>
          <span v-if="opt.hint" class="menu-hint">{{ opt.hint }}</span>
        </span>
        <svg v-if="opt.value === modelValue" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="m5 12 5 5 9-10" /></svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: null },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择' },
  title: { type: String, default: '' },
  disabled: Boolean,
  placement: { type: String, default: 'up' }
})
const emit = defineEmits(['update:modelValue'])

const open = ref(false)
const root = ref(null)
const current = computed(() => props.options.find((o) => o.value === props.modelValue))

function choose(v) {
  emit('update:modelValue', v)
  open.value = false
}
function onDocClick(e) {
  if (root.value && !root.value.contains(e.target)) open.value = false
}
onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))
</script>

<style scoped>
.pill-select {
  position: relative;
}
.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px 0 12px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: var(--surface-2);
  color: var(--text);
  font-size: 13px;
  transition: background 0.15s;
}
.pill:hover:not(:disabled),
.open .pill {
  background: var(--surface-3);
}
.pill:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.pill-label {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chev {
  color: var(--muted);
  transition: transform 0.15s;
}
.open .chev {
  transform: rotate(180deg);
}
.menu {
  position: absolute;
  left: 0;
  min-width: 220px;
  max-height: 320px;
  overflow-y: auto;
  padding: 6px;
  border-radius: var(--radius);
  background: var(--surface-2);
  box-shadow: var(--shadow-pop);
  z-index: 30;
}
.menu.up {
  bottom: calc(100% + 8px);
}
.menu.down {
  top: calc(100% + 8px);
}
.menu-title {
  padding: 6px 10px 4px;
  font-size: 11px;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  text-align: left;
  font-size: 13px;
}
.menu-item:hover {
  background: var(--surface-3);
}
.menu-main {
  display: flex;
  flex-direction: column;
}
.menu-hint {
  font-size: 12px;
  color: var(--muted);
}
</style>

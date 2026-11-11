<template>
  <div ref="root" class="pill-select" :class="{ open }">
    <button type="button" class="pill" :disabled="disabled" @click="open = !open">
      <slot name="icon" />
      <span class="pill-label">{{ current ? current.label : placeholder }}</span>
      <svg class="chev" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="m7 10 5 5 5-5" /></svg>
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
        <svg v-if="opt.value === modelValue" class="check" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="m5 12 5 5 9-10" /></svg>
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
  height: 36px;
  padding: 0 10px 0 14px;
  border-radius: 999px;
  border: 0;
  background: var(--surface-3);
  color: var(--text);
  font-size: 13px;
  font-weight: 500;
  transition: background 0.15s;
}
.pill:hover:not(:disabled),
.open .pill {
  background: var(--surface-4);
}
.pill:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.pill :slotted(svg) {
  color: var(--primary);
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
  min-width: 240px;
  max-height: 340px;
  overflow-y: auto;
  padding: 8px;
  border-radius: var(--radius);
  background: var(--surface-3);
  box-shadow: var(--shadow-pop);
  z-index: 30;
}
.menu.up {
  bottom: calc(100% + 10px);
}
.menu.down {
  top: calc(100% + 10px);
  left: auto;
  right: 0;
}
.menu-title {
  padding: 8px 12px 6px;
  font-size: 12px;
  color: var(--muted);
}
.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  text-align: left;
  font-size: 14px;
}
.menu-item:hover {
  background: var(--surface-4);
}
.menu-item.active {
  background: rgba(168, 199, 250, 0.12);
}
.menu-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.menu-hint {
  font-size: 12px;
  color: var(--muted);
}
.check {
  color: var(--primary);
  flex: none;
}
</style>

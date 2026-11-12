<template>
  <Teleport to="body"><div v-if="open" class="backdrop" @mousedown.self="$emit('close')"><section class="modal glass"><button class="close" @click="$emit('close')">×</button><slot /></section></div></Teleport>
</template>
<script setup>defineProps({open:Boolean});defineEmits(['close'])</script>
<style scoped>
.backdrop{position:fixed;inset:0;z-index:100;display:grid;place-items:center;padding:20px;background:rgba(0,0,0,.68);backdrop-filter:blur(8px)}.modal{position:relative;width:min(470px,100%);padding:28px;border-radius:24px;animation:rise .22s ease}.close{position:absolute;right:16px;top:14px;border:0;background:none;color:var(--muted);font-size:26px}.modal :deep(h2){margin:0 0 6px;font-size:24px}.modal :deep(.modal-sub){margin:0 0 22px;color:var(--muted)}
</style>

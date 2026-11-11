<template>
  <header class="topbar">
    <div class="brand">
      <span class="logo" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <defs>
            <linearGradient id="logoGrad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#4c8df6" />
              <stop offset="0.55" stop-color="#8f6be8" />
              <stop offset="1" stop-color="#d16ba5" />
            </linearGradient>
          </defs>
          <rect x="2" y="2" width="20" height="20" rx="6" fill="url(#logoGrad)" />
          <path d="M10 8.2v7.6l6-3.8z" fill="#fff" />
        </svg>
      </span>
      <span class="brand-name">Sun AI Video</span>
      <span v-if="envName" class="env-tag" :class="envName" :title="`当前后端环境：${envName}`">{{ envName.toUpperCase() }}</span>
    </div>

    <nav class="tabs">
      <router-link to="/generate" class="tab">生成视频</router-link>
      <router-link to="/usage" class="tab">用量与费用</router-link>
    </nav>

    <div class="status">
      <span
        class="chip"
        :class="status.mockMode ? 'mock' : 'live'"
        :title="status.mockMode ? '未配置 DASHSCOPE_API_KEY，视频生成走 Mock，不产生真实费用' : '已连接阿里云百炼'"
      >
        <span class="dot" />{{ status.mockMode ? 'Mock 模式' : '百炼已连接' }}
      </span>
      <span class="chip subtle" title="提示词润色使用的模型">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M12 2l1.8 5.2L19 9l-5.2 1.8L12 16l-1.8-5.2L5 9l5.2-1.8z" /></svg>
        {{ status.llmAvailable ? status.llmModel : '润色未启用' }}
      </span>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: Object, required: true } })

// 优先显示后端实际环境，后端未连上时显示前端构建环境
const envName = computed(() => props.status.env || import.meta.env.VITE_APP_ENV || '')
</script>

<style scoped>
.topbar {
  height: 64px;
  flex: none;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  background: var(--bg);
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.logo {
  display: grid;
  place-items: center;
}
.brand-name {
  font-size: 18px;
  font-weight: 500;
  letter-spacing: -0.01em;
  white-space: nowrap;
}
.env-tag {
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  display: inline-flex;
  align-items: center;
  background: var(--surface-3);
  color: var(--text-2);
}
.env-tag.uat {
  background: rgba(253, 214, 99, 0.14);
  color: var(--warning);
}
.env-tag.pro {
  background: rgba(109, 213, 140, 0.14);
  color: var(--success);
}

.tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: var(--surface-2);
}
.tab {
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  color: var(--text-2);
  font-weight: 500;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.tab:hover {
  color: var(--text);
}
.tab.router-link-active {
  background: var(--surface-4);
  color: var(--text);
}

.status {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}
.chip {
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  background: var(--surface-2);
  color: var(--text-2);
  white-space: nowrap;
}
.chip .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}
.chip.mock {
  color: var(--warning);
}
.chip.live {
  color: var(--success);
}
.chip.subtle svg {
  color: #b39af0;
}

@media (max-width: 900px) {
  .chip.subtle {
    display: none;
  }
}
@media (max-width: 720px) {
  .topbar {
    grid-template-columns: auto 1fr;
    grid-template-rows: 56px auto;
    height: auto;
    padding: 0 16px 8px;
    row-gap: 4px;
  }
  .brand-name {
    font-size: 16px;
  }
  .tabs {
    grid-column: 1 / -1;
    grid-row: 2;
    justify-self: stretch;
  }
  .tab {
    flex: 1;
    justify-content: center;
  }
  .status {
    grid-column: 2;
    grid-row: 1;
  }
}
</style>

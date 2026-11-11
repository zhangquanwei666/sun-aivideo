<template>
  <aside class="sidebar">
    <div class="brand">
      <span class="logo">
        <svg viewBox="0 0 32 32" width="18" height="18"><path d="M11 8.5v15l13-7.5z" fill="currentColor" /></svg>
      </span>
      <span class="brand-name">Sun AI Video</span>
      <span v-if="envName" class="env-tag" :class="envName" :title="`当前后端环境：${envName}`">{{ envName.toUpperCase() }}</span>
    </div>

    <nav class="nav">
      <router-link to="/generate" class="nav-item">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="5" width="13" height="14" rx="2.5" /><path d="m16 10 5-3v10l-5-3" />
        </svg>
        <span>生成视频</span>
      </router-link>
      <router-link to="/usage" class="nav-item">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />
        </svg>
        <span>用量与费用</span>
      </router-link>
    </nav>

    <div class="foot">
      <div v-if="status.mockMode" class="env mock" title="未配置 DASHSCOPE_API_KEY，视频生成走 Mock，不产生真实费用">
        <span class="dot" /> Mock 模式
      </div>
      <div v-else class="env live"><span class="dot" /> 百炼已连接</div>
      <div class="env-sub">润色：{{ status.llmAvailable ? status.llmModel : '未启用' }}</div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: Object, required: true } })

// 优先显示后端实际环境，后端未连上时显示前端构建环境
const envName = computed(() => props.status.env || import.meta.env.VITE_APP_ENV || '')
</script>

<style scoped>
.sidebar {
  width: 220px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  background: var(--bg);
  padding: 16px 12px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 20px;
}
.logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary);
  color: var(--primary-text);
  display: grid;
  place-items: center;
}
.brand-name {
  font-weight: 600;
  letter-spacing: -0.01em;
}
.env-tag {
  height: 18px;
  padding: 0 6px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
  display: inline-flex;
  align-items: center;
  background: var(--surface-3);
  color: var(--text-2);
}
.env-tag.uat {
  background: rgba(245, 165, 36, 0.15);
  color: var(--warning);
}
.env-tag.pro {
  background: rgba(62, 207, 142, 0.15);
  color: var(--success);
}
.nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 10px;
  border-radius: var(--radius-sm);
  color: var(--text-2);
  transition: background 0.15s, color 0.15s;
}
.nav-item:hover {
  background: var(--surface-2);
  color: var(--text);
}
.nav-item.router-link-active {
  background: var(--surface-3);
  color: var(--text);
}
.foot {
  margin-top: auto;
  padding: 12px 10px 4px;
  border-top: 1px solid var(--border);
}
.env {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
}
.env .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}
.env.mock {
  color: var(--warning);
}
.env.live {
  color: var(--success);
}
.env-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 720px) {
  .sidebar {
    width: 100%;
    flex-direction: row;
    align-items: center;
    padding: 8px 12px;
    border-right: 0;
    border-bottom: 1px solid var(--border);
    gap: 8px;
  }
  .brand {
    padding: 0;
  }
  .brand-name,
  .env-sub {
    display: none;
  }
  .nav {
    flex-direction: row;
  }
  .foot {
    margin: 0 0 0 auto;
    padding: 0;
    border: 0;
  }
}
</style>

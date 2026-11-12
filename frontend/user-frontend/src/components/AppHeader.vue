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

    <div class="header-actions">
      <div class="status">
        <span v-if="!connected" class="chip off" title="后端没有响应，请确认后端已启动"><span class="dot" />后端未连接</span>
        <span v-else class="chip" :class="status.mockMode ? 'mock' : 'live'" :title="status.mockMode ? '未配置 DASHSCOPE_API_KEY，视频生成走 Mock，不产生真实费用' : '已连接阿里云百炼'">
          <span class="dot" />{{ status.mockMode ? 'Mock 模式' : '百炼已连接' }}
        </span>
        <span class="chip subtle" title="提示词润色使用的模型">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M12 2l1.8 5.2L19 9l-5.2 1.8L12 16l-1.8-5.2L5 9l5.2-1.8z" /></svg>
          {{ status.llmAvailable ? status.llmModel : '润色未启用' }}
        </span>
      </div>

      <div ref="accountRoot" class="account">
        <button class="account-trigger" type="button" :aria-expanded="menuOpen" aria-haspopup="menu" @click="menuOpen = !menuOpen">
          <span class="avatar">{{ initial }}</span>
          <span class="account-copy"><b>{{ me.username || '用户' }}</b><small>{{ memberLabel }}</small></span>
          <svg class="chevron" :class="{ open: menuOpen }" viewBox="0 0 24 24" aria-hidden="true"><path d="m8 10 4 4 4-4" /></svg>
        </button>
        <transition name="menu">
          <div v-if="menuOpen" class="account-menu" role="menu">
            <div class="menu-profile">
              <span class="avatar large">{{ initial }}</span>
              <span><b>{{ me.username || '当前用户' }}</b><small>{{ memberLabel }}<template v-if="me.id"> · ID {{ me.id }}</template></small></span>
            </div>
            <div class="menu-separator" />
            <button type="button" role="menuitem" :disabled="loggingOut" @click="signOut">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 5H5v14h5M14 8l4 4-4 4m4-4H9" /></svg>
              {{ loggingOut ? '正在退出…' : '退出登录' }}
            </button>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'

const props = defineProps({ status: { type: Object, required: true } })
const router = useRouter()
const me = ref({})
const menuOpen = ref(false)
const loggingOut = ref(false)
const accountRoot = ref(null)

// 优先显示后端实际环境，后端未连上时显示前端构建环境
const envName = computed(() => props.status.env || import.meta.env.VITE_APP_ENV || '')
// 拿到后端返回的环境信息才算连上
const connected = computed(() => !!props.status.env)
const initial = computed(() => (me.value.username || 'U').slice(0, 1).toUpperCase())
const memberLabel = computed(() => me.value.memberLevel === 'VIP' ? 'VIP 会员' : '普通用户')

function closeOnOutside(event) {
  if (menuOpen.value && !accountRoot.value?.contains(event.target)) menuOpen.value = false
}
function closeOnEscape(event) {
  if (event.key === 'Escape') menuOpen.value = false
}
async function signOut() {
  loggingOut.value = true
  try { await http.post('/auth/logout') } catch {} finally {
    localStorage.removeItem('aivideo-token')
    menuOpen.value = false
    loggingOut.value = false
    await router.replace('/login')
  }
}
onMounted(async () => {
  document.addEventListener('pointerdown', closeOnOutside)
  document.addEventListener('keydown', closeOnEscape)
  try { me.value = await http.get('/auth/me') } catch {}
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', closeOnOutside)
  document.removeEventListener('keydown', closeOnEscape)
})
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
  gap: 8px;
  min-width: 0;
}
.header-actions { display:flex;align-items:center;justify-content:flex-end;gap:10px;min-width:0 }
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
.chip.off {
  color: var(--danger);
}
.chip.subtle svg {
  color: #b39af0;
}
.account{position:relative;flex:none}.account-trigger{height:42px;display:flex;align-items:center;gap:9px;padding:0 9px 0 6px;border:1px solid transparent;border-radius:14px;background:transparent;text-align:left;transition:.18s}.account-trigger:hover,.account-trigger[aria-expanded="true"]{background:var(--surface-2);border-color:var(--border)}
.avatar{width:31px;height:31px;display:grid;place-items:center;border-radius:10px;background:linear-gradient(145deg,#465c92,#6f4f82);color:#eef2ff;font-size:12px;font-weight:700;box-shadow:inset 0 0 0 1px rgba(255,255,255,.09)}.avatar.large{width:38px;height:38px;border-radius:12px;font-size:14px}.account-copy{min-width:0;display:grid;line-height:1.2}.account-copy b{max-width:100px;overflow:hidden;text-overflow:ellipsis;font-size:12px}.account-copy small,.menu-profile small{margin-top:3px;color:var(--muted);font-size:9px}.chevron{width:15px;fill:none;stroke:var(--muted);stroke-width:1.8;transition:transform .18s}.chevron.open{transform:rotate(180deg)}
.account-menu{position:absolute;z-index:100;right:0;top:calc(100% + 9px);width:240px;padding:8px;border:1px solid var(--border-strong);border-radius:16px;background:rgba(30,31,36,.96);box-shadow:0 18px 50px rgba(0,0,0,.48);backdrop-filter:blur(22px)}.menu-profile{display:flex;align-items:center;gap:11px;padding:9px}.menu-profile>span:last-child{min-width:0;display:grid}.menu-profile b{overflow:hidden;text-overflow:ellipsis;font-size:13px}.menu-separator{height:1px;margin:5px 3px;background:var(--border)}.account-menu>button{width:100%;height:40px;display:flex;align-items:center;gap:10px;padding:0 10px;border:0;border-radius:10px;background:transparent;color:var(--danger);font-size:12px}.account-menu>button:hover{background:rgba(242,184,181,.08)}.account-menu>button:disabled{opacity:.55}.account-menu>button svg{width:17px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.menu-enter-active,.menu-leave-active{transition:.16s ease}.menu-enter-from,.menu-leave-to{opacity:0;transform:translateY(-5px) scale(.98)}

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
  .header-actions{grid-column:2;grid-row:1}.status{display:none}.account-copy{display:none}.account-trigger{padding:0 5px}.chevron{display:none}
}
</style>

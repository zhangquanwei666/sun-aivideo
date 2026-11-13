<template>
  <div class="shell">
    <aside class="sidebar glass">
      <div class="brand">
        <div class="mark"><span /></div>
        <div><strong>Sun Studio</strong><small>CONTROL ROOM</small></div>
      </div>
      <nav>
        <router-link v-for="item in nav" :key="item.to" :to="item.to">
          <span class="nav-icon" v-html="item.icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>
    <section class="workspace">
      <header class="topbar">
        <div ref="accountRoot" class="account">
          <button class="account-trigger" type="button" :aria-expanded="menuOpen" aria-haspopup="menu" @click="menuOpen = !menuOpen">
            <div class="avatar">{{ initial }}</div>
            <div class="identity"><strong>{{ me.nickname || me.username || '管理员' }}</strong><small>{{ me.username || '加载中…' }}</small></div>
            <svg class="chevron" :class="{ open: menuOpen }" viewBox="0 0 24 24" aria-hidden="true"><path d="m8 10 4 4 4-4" /></svg>
          </button>
          <transition name="menu">
            <div v-if="menuOpen" class="account-menu glass" role="menu">
              <div class="menu-profile">
                <div class="avatar large">{{ initial }}</div>
                <div><strong>{{ me.nickname || '系统管理员' }}</strong><small>@{{ me.username || 'admin' }}<template v-if="me.id"> · ID {{ me.id }}</template></small></div>
              </div>
              <div class="menu-role"><span /> 管理后台账户</div>
              <div class="separator" />
              <button type="button" role="menuitem" :disabled="loggingOut" @click="signOut">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 5H5v14h5M14 8l4 4-4 4m4-4H9" /></svg>
                {{ loggingOut ? '正在退出…' : '退出登录' }}
              </button>
            </div>
          </transition>
        </div>
      </header>
      <main><router-view /></main>
    </section>
    <div class="mobile-nav glass">
      <router-link v-for="item in nav" :key="item.to" :to="item.to"><span v-html="item.icon"/><small>{{ item.short }}</small></router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMe, logout } from '@/api'

const router = useRouter()
const me = ref({})
const menuOpen = ref(false)
const loggingOut = ref(false)
const accountRoot = ref(null)
const initial = computed(() => (me.value.nickname || me.value.username || 'A').slice(0, 1).toUpperCase())
const icons = {
  home: '<svg viewBox="0 0 24 24"><path d="M4 11.5 12 5l8 6.5V20h-5v-5H9v5H4z"/></svg>',
  users: '<svg viewBox="0 0 24 24"><path d="M16 20v-1.5c0-2.5-2-4.5-4.5-4.5h-3C6 14 4 16 4 18.5V20m14-6c1.6.5 2.8 2 2.8 3.8V20M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8m7-1a3 3 0 1 0 0-6"/></svg>',
  film: '<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="3"/><path d="M8 4v16m8-16v16M3 9h5m8 0h5M3 15h5m8 0h5"/></svg>'
}
const nav = [
  { to: '/dashboard', label: '总览', short: '总览', icon: icons.home },
  { to: '/users', label: '用户管理', short: '用户', icon: icons.users },
  { to: '/records', label: '生成记录', short: '记录', icon: icons.film }
]

async function signOut() {
  loggingOut.value = true
  try { await logout() } catch {} finally {
    localStorage.removeItem('admin-token')
    menuOpen.value = false
    loggingOut.value = false
    await router.replace('/login')
  }
}
function closeOnOutside(event) {
  if (menuOpen.value && !accountRoot.value?.contains(event.target)) menuOpen.value = false
}
function closeOnEscape(event) {
  if (event.key === 'Escape') menuOpen.value = false
}
onMounted(async () => {
  document.addEventListener('pointerdown', closeOnOutside)
  document.addEventListener('keydown', closeOnEscape)
  try { me.value = await getMe() } catch {}
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', closeOnOutside)
  document.removeEventListener('keydown', closeOnEscape)
})
</script>

<style scoped>
.shell { min-height: 100vh; display: grid; grid-template-columns: 252px minmax(0, 1fr); }
.sidebar { position: sticky; top: 0; height: 100vh; z-index: 20; padding: 22px 16px; display: flex; flex-direction: column; border-width: 0 1px 0 0; border-radius: 0; background: rgba(11,12,15,.76); }
.brand { height: 60px; padding: 0 10px; display: flex; align-items: center; gap: 12px; }
.brand strong { display: block; font-size: 17px; letter-spacing: -.02em; }
.brand small { display: block; margin-top: 2px; color: var(--muted); font-size: 8px; letter-spacing: .18em; }
.mark { width: 35px; height: 35px; border-radius: 11px; padding: 1px; background: var(--gradient); box-shadow: 0 0 30px rgba(129,139,255,.22); }
.mark::before { content: ''; display: block; width: 100%; height: 100%; border-radius: 10px; background: #101116; }
.mark span { position: absolute; width: 11px; height: 11px; margin: -23px 0 0 12px; border: 2px solid #d9dcff; border-left-color: transparent; border-radius: 50%; }
nav { margin-top: 35px; display: grid; gap: 5px; }
nav a { height: 45px; padding: 0 13px; border-radius: 12px; display: flex; align-items: center; gap: 12px; color: var(--muted); transition: .2s; }
nav a:hover { color: var(--text-2); background: rgba(255,255,255,.035); }
nav a.router-link-active { color: var(--text); background: rgba(255,255,255,.075); box-shadow: inset 0 0 0 1px rgba(255,255,255,.04); }
.nav-icon { width: 19px; height: 19px; display: grid; place-items: center; }
.nav-icon :deep(svg), .mobile-nav :deep(svg) { width: 19px; height: 19px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.workspace { min-width: 0; min-height: 100vh; display: grid; grid-template-rows: 64px minmax(0, 1fr); }
.topbar { position: sticky; top: 0; z-index: 25; display: flex; align-items: center; justify-content: flex-end; padding: 0 32px; border-bottom: 1px solid rgba(255,255,255,.055); background: rgba(8,9,11,.76); backdrop-filter: blur(20px); }
.account { position: relative; }
.account-trigger { min-width: 174px; height: 46px; padding: 0 8px; border: 1px solid transparent; border-radius: 14px; display: flex; align-items: center; gap: 10px; background: transparent; text-align: left; transition: .2s; }
.account-trigger:hover,.account-trigger[aria-expanded="true"] { border-color: var(--line); background: rgba(255,255,255,.045); }
.avatar { width: 35px; height: 35px; flex: none; border-radius: 11px; display: grid; place-items: center; background: linear-gradient(145deg,#44527a,#57405f); color: #dce3ff; font-weight: 650; box-shadow: inset 0 0 0 1px rgba(255,255,255,.08); }
.avatar.large { width: 42px; height: 42px; border-radius: 13px; font-size: 15px; }
.identity { min-width: 0; flex: 1; }
.identity strong,.identity small { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.identity strong { font-size: 12px; }.identity small { color: var(--muted); font-size: 10px; }
.chevron { width: 16px; flex: none; fill: none; stroke: var(--muted); stroke-width: 1.8; transition: transform .18s; }.chevron.open { transform: rotate(180deg); }
.account-menu { position: absolute; right: 0; top: calc(100% + 8px); width: 254px; padding: 9px; border-radius: 17px; background: rgba(20,21,25,.96); box-shadow: 0 22px 60px rgba(0,0,0,.5); }
.menu-profile { display: flex; align-items: center; gap: 11px; padding: 9px; }.menu-profile>div:last-child { min-width: 0; }.menu-profile strong,.menu-profile small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.menu-profile strong { font-size: 13px; }.menu-profile small { margin-top: 3px; color: var(--muted); font-size: 10px; }
.menu-role { display: flex; align-items: center; gap: 7px; padding: 3px 10px 8px; color: var(--muted); font-size: 10px; }.menu-role span { width: 6px; height: 6px; border-radius: 50%; background: var(--green); box-shadow: 0 0 9px rgba(118,217,154,.65); }
.separator { height: 1px; margin: 3px; background: var(--line); }
.account-menu>button { width: 100%; height: 40px; margin-top: 5px; padding: 0 10px; border: 0; border-radius: 10px; display: flex; align-items: center; gap: 10px; background: transparent; color: var(--red); font-size: 12px; }.account-menu>button:hover { background: rgba(255,145,140,.08); }.account-menu>button:disabled { opacity: .55; }.account-menu>button svg { width: 17px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.menu-enter-active,.menu-leave-active { transition: .16s ease; }.menu-enter-from,.menu-leave-to { opacity: 0; transform: translateY(-5px) scale(.98); }
main { min-width: 0; min-height: 0; }
.mobile-nav { display: none; }
@media (max-width: 850px) {
  .shell { display: block; }.sidebar { display: none; }.workspace { grid-template-rows: 58px minmax(0,1fr); }.topbar { padding: 0 16px; }.account-trigger { min-width: 0; padding: 0 5px; }.account-trigger>.identity,.chevron { display: none; }
  .mobile-nav { position: fixed; display: grid; grid-template-columns: repeat(3,1fr); z-index: 30; left: 12px; right: 12px; bottom: 12px; height: 65px; border-radius: 20px; padding: 7px; }
  .mobile-nav a { display:flex; flex-direction:column; align-items:center; justify-content:center; gap:3px; color:var(--muted); border-radius:14px; }
  .mobile-nav a.router-link-active { color:var(--text); background:rgba(255,255,255,.08); }
  .mobile-nav small { font-size:9px; }
}
</style>

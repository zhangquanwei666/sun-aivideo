<template>
  <main class="login-page">
    <div class="aurora one"/><div class="aurora two"/>
    <section class="visual">
      <p class="eyebrow">SUN AI VIDEO · CONTROL ROOM</p>
      <h1>把每一次生成，<br><span class="gradient-text">看得清清楚楚。</span></h1>
      <p class="lead">管理用户、会员和创作记录。所有正在发生的想象，都汇聚在这里。</p>
      <div class="frame-stack" aria-hidden="true"><i/><i/><i/><div class="play">▶</div></div>
    </section>
    <form class="login-card glass" @submit.prevent="submit">
      <div class="mini-mark">S</div>
      <p class="eyebrow">SECURE ACCESS</p>
      <h2>欢迎回来</h2>
      <p class="hint">使用管理员账号进入控制台</p>
      <label class="field"><span>用户名</span><input v-model.trim="username" class="input" autocomplete="username" autofocus required placeholder="admin" /></label>
      <label class="field"><span>密码</span><input v-model="password" class="input" type="password" autocomplete="current-password" required placeholder="输入管理员密码" /></label>
      <div v-if="error" class="error">{{ error }}</div>
      <button class="btn btn-primary submit" :disabled="busy">{{ busy ? '正在验证…' : '进入控制台' }} <span>→</span></button>
      <small>管理员会话通过独立的 Sa-Token 安全验证</small>
    </form>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api'
const router = useRouter(), username = ref(''), password = ref(''), error = ref(''), busy = ref(false)
async function submit() {
  busy.value = true; error.value = ''
  try {
    const result = await login({ username: username.value, password: password.value })
    localStorage.setItem('admin-token', result.token)
    password.value = ''
    router.replace('/dashboard')
  } catch (e) { error.value = e.message } finally { busy.value = false }
}
</script>

<style scoped>
.login-page { min-height: 100vh; position: relative; overflow: hidden; display: grid; grid-template-columns: 1.25fr .75fr; align-items: center; gap: 8vw; padding: 7vw; }
.aurora { position:absolute; border-radius:50%; filter:blur(80px); opacity:.25; pointer-events:none; }.aurora.one{width:42vw;height:42vw;background:#355dff;left:-22vw;top:-15vw}.aurora.two{width:35vw;height:35vw;background:#b248d0;right:-18vw;bottom:-20vw}
.visual,.login-card { position:relative; z-index:1; animation:rise .65s ease both; }
.visual h1 { margin: 15px 0 22px; font-size: clamp(46px, 6vw, 88px); font-weight: 480; letter-spacing: -.065em; line-height: 1.02; }
.lead { max-width: 590px; color: var(--text-2); font-size: 17px; }
.frame-stack { position:relative; width:320px; height:130px; margin-top:55px; perspective:800px; }
.frame-stack i { position:absolute; inset:0; border:1px solid rgba(255,255,255,.14); border-radius:18px; background:linear-gradient(135deg,rgba(74,100,160,.38),rgba(103,52,118,.2)); transform:rotateX(58deg) rotateZ(-8deg); box-shadow:0 30px 80px #000; }
.frame-stack i:nth-child(2){transform:translate(26px,-16px) rotateX(58deg) rotateZ(-8deg);opacity:.7}.frame-stack i:nth-child(3){transform:translate(52px,-32px) rotateX(58deg) rotateZ(-8deg);opacity:.45}
.play { position:absolute; left:145px; top:35px; width:48px;height:48px;border-radius:50%;display:grid;place-items:center;background:rgba(255,255,255,.88);color:#17181c;box-shadow:0 10px 40px #000; }
.login-card { width:min(430px,100%); justify-self:end; padding:38px; border-radius:26px; }
.mini-mark { width:42px;height:42px;border-radius:13px;display:grid;place-items:center;margin-bottom:30px;background:var(--gradient);color:#111;font-weight:800; }
.login-card h2 { margin:4px 0;font-size:31px;letter-spacing:-.04em; }.hint{color:var(--muted);margin:0 0 28px}.field{margin:15px 0}.submit{width:100%;margin-top:12px;height:46px}.login-card>small{display:block;text-align:center;color:var(--muted);margin-top:18px;font-size:10px}
@media(max-width:850px){.login-page{display:flex;align-items:center;justify-content:center;padding:24px}.visual{display:none}.login-card{justify-self:auto;padding:28px}}
</style>

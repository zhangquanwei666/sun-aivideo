<template>
  <main class="login-page">
    <div class="ambient blue"/><div class="ambient violet"/><div class="noise"/>
    <header class="brand"><span class="brand-mark"><svg viewBox="0 0 24 24"><path d="M7 4.5 18.5 12 7 19.5v-15Z"/></svg></span><span>Sun AI Video</span></header>

    <section class="showcase" aria-labelledby="login-title">
      <p class="eyebrow"><span/> CREATE BEYOND THE FRAME</p>
      <h1 id="login-title">让每一个想法，<br><span>成为一段影像。</span></h1>
      <p class="lead">从一句描述到完整画面，在同一个创作空间里生成、打磨并管理你的 AI 视频。</p>
      <div class="film-stage" aria-hidden="true">
        <div class="film-glow"/><div class="film-card film-back"><div class="scene night"><i/><i/><i/></div></div>
        <div class="film-card film-front">
          <div class="scene sunrise"><i/><i/><i/></div>
          <div class="film-meta"><span><b/>生成预览</span><span>00:08</span></div>
          <span class="play"><svg viewBox="0 0 24 24"><path d="m9 7 8 5-8 5V7Z"/></svg></span>
        </div>
        <div class="prompt-chip"><span>✦</span> 将晨雾变成一场电影</div>
      </div>
      <div class="features" aria-hidden="true"><span>文本生成视频</span><i/><span>图片动态化</span><i/><span>创作记录同步</span></div>
    </section>

    <section class="account-panel">
      <form class="login-card" @submit.prevent="submit">
        <div class="mobile-mark"><span class="brand-mark"><svg viewBox="0 0 24 24"><path d="M7 4.5 18.5 12 7 19.5v-15Z"/></svg></span></div>
        <p class="card-eyebrow">{{ registering ? 'START CREATING' : 'WELCOME BACK' }}</p>
        <h2>{{ registering ? '创建创作者账号' : '继续你的创作' }}</h2>
        <p class="card-copy">{{ registering ? '注册后即可进入工作台，开启第一段 AI 影像。' : '登录 Sun AI Video，回到你的生成工作台。' }}</p>
        <div class="mode-switch" role="tablist" aria-label="账号操作">
          <button type="button" role="tab" :aria-selected="!registering" :class="{ active: !registering }" @click="switchMode(false)">登录</button>
          <button type="button" role="tab" :aria-selected="registering" :class="{ active: registering }" @click="switchMode(true)">注册</button>
        </div>
        <label class="field"><span>用户名</span><span class="input-wrap">
          <svg viewBox="0 0 24 24"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm7 8a7 7 0 0 0-14 0"/></svg>
          <input v-model.trim="username" required pattern="[a-zA-Z0-9_]{4,32}" autocomplete="username" placeholder="4–32 位字母、数字或下划线" autofocus>
        </span></label>
        <label class="field"><span>密码</span><span class="input-wrap">
          <svg viewBox="0 0 24 24"><rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
          <input v-model="password" required :type="showPassword ? 'text' : 'password'" minlength="8" maxlength="64" :autocomplete="registering ? 'new-password' : 'current-password'" placeholder="8–64 位密码">
          <button class="password-toggle" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
            <svg v-if="!showPassword" viewBox="0 0 24 24"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"/><circle cx="12" cy="12" r="2.5"/></svg>
            <svg v-else viewBox="0 0 24 24"><path d="m4 4 16 16M9.6 6.4A9.8 9.8 0 0 1 12 6c6 0 9.5 6 9.5 6a14 14 0 0 1-2 2.6M6.2 7.8A16 16 0 0 0 2.5 12s3.5 6 9.5 6a9.8 9.8 0 0 0 3-.5"/></svg>
          </button>
        </span></label>
        <p v-if="error" class="error" role="alert"><span>!</span>{{ error }}</p>
        <button class="submit" :disabled="busy"><span>{{ submitText }}</span><svg v-if="!busy" viewBox="0 0 24 24"><path d="M5 12h14m-5-5 5 5-5 5"/></svg><i v-else/></button>
        <p class="security"><svg viewBox="0 0 24 24"><path d="M12 3 5 6v5c0 4.6 2.9 8 7 10 4.1-2 7-5.4 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-4"/></svg>会话通过 Sa-Token 安全保护</p>
      </form>
    </section>
    <footer>© 2026 Sun AI Video · 想象，从此刻开始</footer>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
const router = useRouter()
const username = ref(''), password = ref(''), error = ref(''), busy = ref(false), registering = ref(false), showPassword = ref(false)
const submitText = computed(() => busy.value ? (registering.value ? '正在创建账号…' : '正在登录…') : (registering.value ? '注册并开始创作' : '进入创作空间'))
function switchMode(value) { registering.value = value; error.value = ''; password.value = ''; showPassword.value = false }
async function submit() {
  busy.value = true; error.value = ''
  try {
    const payload = { username: username.value, password: password.value }
    const result = await http.post(registering.value ? '/auth/register' : '/auth/login', payload)
    localStorage.setItem('aivideo-token', result.token); password.value = ''; await router.replace('/generate')
  } catch (e) { error.value = e.message } finally { busy.value = false }
}
</script>

<style scoped>
.login-page{position:relative;min-height:100vh;overflow:hidden;display:grid;grid-template-columns:minmax(0,1.18fr) minmax(380px,.82fr);background:#090a0d;isolation:isolate}.login-page:before{content:'';position:absolute;inset:0;z-index:-3;background:linear-gradient(rgba(255,255,255,.025) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.025) 1px,transparent 1px);background-size:64px 64px;mask-image:linear-gradient(to right,#000,transparent 70%)}
.ambient{position:absolute;z-index:-2;border-radius:50%;filter:blur(110px);pointer-events:none}.blue{width:48vw;height:48vw;left:-22vw;top:-19vw;background:rgba(37,91,255,.34)}.violet{width:40vw;height:40vw;left:38vw;bottom:-30vw;background:rgba(167,70,219,.27)}.noise{position:absolute;inset:0;z-index:-1;opacity:.035;pointer-events:none;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E")}
.brand{position:absolute;left:clamp(28px,4vw,68px);top:34px;display:flex;align-items:center;gap:12px;font-size:16px;font-weight:650;z-index:3}.brand-mark{width:36px;height:36px;border-radius:12px;display:grid;place-items:center;background:linear-gradient(135deg,#d8e6ff,#b6a4ff 55%,#e99ada);box-shadow:0 8px 30px rgba(120,116,255,.24);color:#111319}.brand-mark svg{width:20px;fill:currentColor}
.showcase{display:flex;flex-direction:column;justify-content:center;padding:110px clamp(42px,7vw,112px) 70px;min-width:0}.eyebrow,.card-eyebrow{margin:0;color:#9ca6b8;font-size:10px;font-weight:700;letter-spacing:.24em}.eyebrow{display:flex;align-items:center;gap:10px}.eyebrow span{width:22px;height:1px;background:#8aa9ff;box-shadow:0 0 10px #6a8fff}.showcase h1{margin:18px 0 24px;font-size:clamp(48px,5.7vw,86px);font-weight:450;letter-spacing:-.068em;line-height:1.04}.showcase h1 span{background:linear-gradient(100deg,#a9c8ff,#c2afff 48%,#ec9ed3);background-clip:text;-webkit-background-clip:text;color:transparent}.lead{max-width:590px;margin:0;color:#a2a5ad;font-size:16px;line-height:1.8}
.film-stage{position:relative;width:min(570px,85%);height:210px;margin-top:55px;perspective:900px}.film-glow{position:absolute;width:65%;height:55%;left:12%;top:25%;background:#6a74ff;filter:blur(75px);opacity:.19}.film-card{position:absolute;border:1px solid rgba(255,255,255,.13);border-radius:20px;background:#13151b;padding:8px;box-shadow:0 32px 80px rgba(0,0,0,.55);overflow:hidden}.film-back{width:65%;height:145px;right:0;top:4px;transform:rotate(4deg);opacity:.5}.film-front{width:68%;height:178px;left:0;top:18px;transform:rotate(-3deg);animation:float 6s ease-in-out infinite}.scene{position:relative;height:128px;border-radius:13px;overflow:hidden}.sunrise{background:radial-gradient(circle at 65% 25%,#ffd8b0 0 3%,transparent 4%),linear-gradient(165deg,#4f3c80,#cc7d97 48%,#e1b27d 65%,#293f4b 66%,#121c26)}.night{height:100%;background:radial-gradient(circle at 25% 25%,#80aaff 0 1%,transparent 2%),linear-gradient(150deg,#121a38,#493568 55%,#171927)}.scene i{position:absolute;bottom:-25px;width:48%;height:75%;background:#111821;clip-path:polygon(0 100%,35% 30%,48% 55%,70% 0,100% 100%)}.scene i:nth-child(1){left:-8%}.scene i:nth-child(2){left:30%;height:88%;opacity:.9}.scene i:nth-child(3){right:-18%;height:62%;opacity:.75}
.film-meta{display:flex;justify-content:space-between;padding:10px 5px 0;color:#9b9ea7;font-size:10px}.film-meta span:first-child{display:flex;align-items:center;gap:7px}.film-meta b{width:6px;height:6px;border-radius:50%;background:#9cc6ff;box-shadow:0 0 8px #69a4ff}.play{position:absolute;left:50%;top:52%;translate:-50% -50%;width:44px;height:44px;border-radius:50%;display:grid;place-items:center;background:rgba(250,250,255,.9);color:#15161b;box-shadow:0 10px 35px #0006}.play svg{width:21px;fill:currentColor}.prompt-chip{position:absolute;right:2%;bottom:1px;padding:10px 14px;border:1px solid #ffffff1f;border-radius:999px;background:#13141bc7;backdrop-filter:blur(14px);color:#c9cbd3;font-size:11px}.prompt-chip span{color:#c1a9ff;margin-right:6px}.features{display:flex;align-items:center;gap:14px;color:#6f747f;font-size:10px;letter-spacing:.08em;margin-top:25px}.features i{width:3px;height:3px;border-radius:50%;background:#4e5360}
.account-panel{display:grid;place-items:center;padding:70px clamp(30px,5vw,84px);border-left:1px solid #ffffff0f;background:linear-gradient(160deg,#17181ee0,#0b0c10f5);backdrop-filter:blur(24px)}.login-card{width:min(430px,100%);animation:rise .65s ease both}.mobile-mark{display:none}.login-card h2{margin:9px 0 8px;font-size:34px;font-weight:520;letter-spacing:-.045em}.card-copy{color:#878b95;margin:0 0 27px;line-height:1.7}.mode-switch{display:grid;grid-template-columns:1fr 1fr;gap:4px;padding:4px;margin-bottom:24px;border-radius:14px;background:#111217;border:1px solid #262831}.mode-switch button{height:38px;border:0;border-radius:10px;background:transparent;color:#777b85;font-size:13px;transition:.2s}.mode-switch button:hover{color:#cfd1d7}.mode-switch button.active{background:#292b34;color:#f1f2f4;box-shadow:0 3px 10px #0005}
.field{display:block;margin:16px 0}.field>span:first-child{display:block;margin-bottom:8px;color:#aeb1b9;font-size:12px}.input-wrap{height:50px;display:flex;align-items:center;gap:11px;padding:0 14px;border:1px solid #32343d;border-radius:13px;background:#15161b;transition:.2s}.input-wrap:focus-within{border-color:#8faeff;box-shadow:0 0 0 3px #6e91ff1c}.input-wrap>svg{width:18px;flex:none;fill:none;stroke:#6f7480;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.input-wrap input{min-width:0;flex:1;height:100%;border:0;outline:0;background:transparent;color:#eef0f5;font-size:13px}.input-wrap input::placeholder{color:#555a64}.password-toggle{width:30px;height:30px;display:grid;place-items:center;flex:none;border:0;background:transparent;color:#737985;border-radius:8px}.password-toggle:hover{background:#24262e;color:#b5bbc7}.password-toggle svg{width:18px;fill:none;stroke:currentColor;stroke-width:1.6;stroke-linecap:round;stroke-linejoin:round}
.error{display:flex;align-items:center;gap:8px;margin:12px 0 0;color:#f3aaa6;font-size:12px}.error span{width:18px;height:18px;display:grid;place-items:center;border-radius:50%;background:#ee706924;font-weight:700}.submit{width:100%;height:50px;margin-top:20px;padding:0 18px;display:flex;align-items:center;justify-content:center;gap:10px;border:0;border-radius:14px;background:linear-gradient(105deg,#b6ceff,#c8b4ff 50%,#e6acd5);color:#15151a;font-weight:700;box-shadow:0 15px 40px #7e74dc33;transition:.2s}.submit:hover:not(:disabled){transform:translateY(-1px);filter:brightness(1.06);box-shadow:0 18px 48px #7e74dc4d}.submit:disabled{opacity:.65;cursor:wait}.submit svg{width:18px;fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}.submit i{width:16px;height:16px;border:2px solid #14141940;border-top-color:#15151a;border-radius:50%;animation:spin .7s linear infinite}.security{display:flex;align-items:center;justify-content:center;gap:7px;margin:18px 0 0;color:#5f646e;font-size:10px}.security svg{width:14px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}footer{position:absolute;left:clamp(28px,4vw,68px);bottom:26px;color:#4e525c;font-size:10px}
@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}@keyframes float{0%,100%{transform:rotate(-3deg)}50%{transform:rotate(-3deg) translateY(-7px)}}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.film-front,.login-card,.submit i{animation:none}}
@media(max-width:960px){.login-page{grid-template-columns:1fr;min-height:100dvh}.brand,.showcase,footer{display:none}.account-panel{min-height:100dvh;border-left:0;padding:38px 22px;background:radial-gradient(circle at 50% -10%,#5964d233,transparent 42%),linear-gradient(160deg,#14151b,#090a0d)}.mobile-mark{display:block;margin-bottom:35px}.mobile-mark .brand-mark{display:grid}}@media(max-width:480px){.account-panel{place-items:start center;padding-top:36px}.login-card h2{font-size:30px}.card-copy{font-size:13px}}
</style>

<template>
  <section class="page">
    <header class="hero">
      <div><p class="eyebrow">LIVE OVERVIEW</p><h1>创作系统，<span class="gradient-text">此刻正在发生。</span></h1><p>用户、会员与生成任务的实时概览。</p></div>
      <div class="live-pill"><i/> 系统在线 <span>{{ now }}</span></div>
    </header>
    <div v-if="error" class="error">{{ error }}</div>
    <div class="metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric card">
        <div class="metric-top"><span>{{ metric.label }}</span><i :style="{background:metric.color}"/></div>
        <strong>{{ loading ? '—' : metric.value }}</strong><small>{{ metric.note }}</small>
      </article>
    </div>
    <div class="dashboard-grid">
      <article class="spotlight card">
        <div class="section-head"><div><p class="eyebrow">LATEST FRAMES</p><h2>最近生成</h2></div><router-link to="/records">查看全部 →</router-link></div>
        <div class="frame-grid">
          <div v-for="item in feed" :key="`${item.type}-${item.id}`" class="frame">
            <img v-if="cover(item)" :src="cover(item)" alt="生成预览" loading="lazy" />
            <div v-else class="frame-empty"><span>{{ item.type === 'VIDEO' ? '▶' : '✦' }}</span></div>
            <div class="frame-meta"><span>{{ item.task?.modelCode }}</span><b :class="statusClass(item.task?.status)">{{ item.task?.status }}</b></div>
          </div>
          <div v-if="!feed.length && !loading" class="empty">暂无生成记录</div>
        </div>
      </article>
      <article class="new-users card">
        <div class="section-head"><div><p class="eyebrow">NEW ARRIVALS</p><h2>最新用户</h2></div><router-link to="/users">管理 →</router-link></div>
        <div class="user-list">
          <div v-for="user in users" :key="user.id" class="user-row">
            <div class="user-avatar">{{ (user.nickname || user.username)[0].toUpperCase() }}</div>
            <div><strong>{{ user.nickname || user.username }}</strong><small>@{{ user.username }}</small></div>
            <span :class="['badge', user.memberLevel === 'VIP' ? 'vip' : '']">{{ user.memberLevel }}</span>
          </div>
          <div v-if="!users.length && !loading" class="empty">暂无用户</div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getFeed, getOverview, getUsers } from '@/api'
import { compact, fileUrl } from '@/utils/format'
const overview=ref({}),feed=ref([]),feedTotal=ref(0),users=ref([]),loading=ref(true),error=ref(''),now=ref('')
const metrics=computed(()=>[
  {label:'全部用户',value:compact(overview.value.totalUsers),note:`今日新增 ${overview.value.todayNewUsers||0}`,color:'#86a9ff'},
  {label:'有效会员',value:compact(overview.value.vipUsers),note:'当前 VIP 用户',color:'#ad8cff'},
  {label:'生成作品',value:compact(feedTotal.value),note:'当前可见生成记录',color:'#66d7e6'},
  {label:'已禁用',value:compact(overview.value.disabledUsers),note:'需要关注的账号',color:'#ff918c'}
])
function cover(item){const t=item.task||{};if(item.type==='IMAGE')return fileUrl(t.imagePaths?.split(',')[0]);return fileUrl(t.localVideoPath ? '' : t.imageUrl?.startsWith('/files/') ? t.imageUrl : '')}
function statusClass(status){return ['SUCCEEDED'].includes(status)?'ok':['FAILED','TIMEOUT'].includes(status)?'bad':'running'}
let clock
onMounted(async()=>{clock=setInterval(()=>now.value=new Date().toLocaleTimeString('zh-CN',{hour12:false}),1000);try{const[o,f,u]=await Promise.all([getOverview(),getFeed({pageNum:1,pageSize:6}),getUsers({pageNum:1,pageSize:5})]);overview.value=o;feed.value=f.records;feedTotal.value=f.total;users.value=u.records}catch(e){error.value=e.message}finally{loading.value=false}})
onBeforeUnmount(()=>clearInterval(clock))
</script>

<style scoped>
.hero{min-height:230px;display:flex;align-items:center;justify-content:space-between;gap:30px}.hero h1{max-width:850px;margin:10px 0 12px;font-size:clamp(39px,5vw,70px);font-weight:470;line-height:1.04;letter-spacing:-.06em}.hero>div>p:last-child{color:var(--muted);font-size:16px}.live-pill{padding:11px 15px;border:1px solid var(--line);border-radius:999px;color:var(--text-2);white-space:nowrap}.live-pill i{display:inline-block;width:7px;height:7px;border-radius:50%;background:var(--green);box-shadow:0 0 12px var(--green);margin-right:7px}.live-pill span{color:var(--muted);margin-left:8px;font-family:monospace}
.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.metric{padding:20px;animation:rise .5s ease both}.metric-top{display:flex;justify-content:space-between;color:var(--muted)}.metric-top i{width:8px;height:8px;border-radius:50%;box-shadow:0 0 18px currentColor}.metric strong{display:block;margin:18px 0 2px;font-size:35px;font-weight:520;letter-spacing:-.04em}.metric small{color:var(--muted)}
.dashboard-grid{display:grid;grid-template-columns:1.55fr .75fr;gap:16px;margin-top:16px}.spotlight,.new-users{padding:22px}.section-head{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:18px}.section-head h2{margin:0;font-size:20px}.section-head a{color:var(--blue);font-size:12px}.frame-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.frame{position:relative;aspect-ratio:16/10;overflow:hidden;border-radius:13px;background:#0d0e11}.frame img{width:100%;height:100%;object-fit:cover}.frame-empty{height:100%;display:grid;place-items:center;background:radial-gradient(circle at 30% 20%,rgba(116,143,255,.32),transparent 45%),linear-gradient(135deg,#161924,#221729)}.frame-empty span{font-size:25px;color:#d7d8ee}.frame-meta{position:absolute;left:0;right:0;bottom:0;padding:22px 9px 8px;background:linear-gradient(transparent,rgba(0,0,0,.8));display:flex;justify-content:space-between;font-size:9px}.frame-meta b{font-weight:500}.frame-meta .ok{color:var(--green)}.frame-meta .bad{color:var(--red)}.frame-meta .running{color:var(--blue)}
.user-list{display:grid}.user-row{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:10px;padding:13px 0;border-bottom:1px solid var(--line)}.user-row:last-child{border:0}.user-avatar{width:35px;height:35px;border-radius:50%;display:grid;place-items:center;background:#252735;color:#cbd3ff}.user-row strong,.user-row small{display:block}.user-row strong{font-size:12px}.user-row small{color:var(--muted);font-size:10px}
@media(max-width:1100px){.metrics{grid-template-columns:repeat(2,1fr)}.dashboard-grid{grid-template-columns:1fr}.new-users{order:-1}}@media(max-width:650px){.hero{min-height:200px}.live-pill{display:none}.metrics{grid-template-columns:1fr 1fr}.frame-grid{grid-template-columns:1fr 1fr}.metric strong{font-size:27px}}
</style>

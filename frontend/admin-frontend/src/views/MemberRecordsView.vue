<template>
  <section class="page">
    <header class="page-head"><div><p class="eyebrow">MEMBERSHIP LEDGER</p><h1 class="page-title">会员流水</h1><p class="page-sub">每次权益变更都有迹可循。</p></div><button class="btn" @click="load">↻ 刷新</button></header>
    <div class="filter-row"><input v-model="userId" class="input member-user" inputmode="numeric" placeholder="按用户 ID 筛选" @keyup.enter="search"/><button class="btn btn-primary" @click="search">查询</button></div>
    <div v-if="error" class="error ledger-error">{{error}}</div>
    <div class="timeline">
      <article v-for="item in records" :key="item.id" class="event card">
        <div :class="['event-icon',item.action==='OPEN'?'open':'revoke']">{{item.action==='OPEN'?'✦':'×'}}</div>
        <div class="event-main"><div><b>{{item.action==='OPEN'?'开通 / 续期会员':'取消会员'}}</b><span>#{{item.id}}</span></div><p><strong>{{item.nickname||item.username}}</strong> @{{item.username}} · 用户 #{{item.userId}}</p><small>{{ expiryText(item) }}</small></div>
        <div class="operator"><span>{{dateTime(item.createdAt)}}</span><small>由 {{item.operatorName}} 操作</small><em v-if="item.remark">{{item.remark}}</em></div>
      </article>
      <div v-if="!records.length&&!loading" class="empty">暂无会员变更记录</div>
    </div>
    <div class="pager"><button class="btn btn-small" :disabled="page===1||loading" @click="go(page-1)">← 上一页</button><span>第 {{page}} / {{pages}} 页 · {{total}} 条</span><button class="btn btn-small" :disabled="page>=pages||loading" @click="go(page+1)">下一页 →</button></div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMemberRecords } from '@/api'
import { dateTime } from '@/utils/format'
const records=ref([]),total=ref(0),page=ref(1),userId=ref(''),loading=ref(false),error=ref(''),pages=computed(()=>Math.max(1,Math.ceil(total.value/20)))
async function load(){loading.value=true;error.value='';try{const r=await getMemberRecords({pageNum:page.value,pageSize:20,userId:userId.value||undefined});records.value=r.records;total.value=r.total}catch(e){error.value=e.message}finally{loading.value=false}}
function search(){page.value=1;load()}function go(p){page.value=p;load()}
function expiryText(item){if(item.action==='REVOKE')return'会员权益已取消';if(!item.expireAfter)return item.days?'到期时间未设置':'已设为永久会员';return `${item.days||'—'} 天 · 到期 ${dateTime(item.expireAfter)}`}
onMounted(load)
</script>

<style scoped>
.member-user{max-width:230px}.ledger-error{margin-top:16px}.timeline{position:relative;display:grid;gap:10px;margin-top:24px}.timeline::before{content:'';position:absolute;left:24px;top:24px;bottom:24px;width:1px;background:linear-gradient(var(--violet),rgba(255,255,255,.06))}.event{position:relative;display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:16px;padding:17px 20px}.event-icon{position:relative;z-index:1;width:34px;height:34px;border-radius:50%;display:grid;place-items:center;background:#272335;color:#d7c5ff;box-shadow:0 0 0 6px var(--bg)}.event-icon.revoke{background:#332326;color:var(--red)}.event-main>div{display:flex;gap:9px;align-items:center}.event-main span{color:var(--muted);font-size:10px}.event-main p{margin:4px 0;color:var(--text-2)}.event-main small,.operator span,.operator small{color:var(--muted);font-size:10px}.operator{text-align:right;display:grid;gap:2px}.operator em{margin-top:5px;color:var(--text-2);font-size:11px;font-style:normal}.operator span{color:var(--text-2)}
@media(max-width:650px){.event{grid-template-columns:auto 1fr}.operator{grid-column:2;text-align:left}}
</style>

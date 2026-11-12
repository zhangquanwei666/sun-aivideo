<template>
  <section class="page">
    <header class="page-head"><div><p class="eyebrow">AUDIENCE</p><h1 class="page-title">用户管理</h1><p class="page-sub">查看用户使用情况，管理账号状态和会员权益。</p></div><button class="btn" @click="load">↻ 刷新</button></header>
    <div class="filter-row filters">
      <input v-model="filters.keyword" class="input" placeholder="搜索用户名或昵称" @keyup.enter="search" />
      <select v-model="filters.member" class="select"><option value="">全部会员</option><option value="VIP">有效会员</option><option value="NORMAL">普通用户</option></select>
      <select v-model="filters.status" class="select"><option value="">全部状态</option><option value="ENABLED">正常</option><option value="DISABLED">已禁用</option></select>
      <button class="btn btn-primary" @click="search">筛选</button>
    </div>
    <div v-if="error" class="error">{{ error }}</div>
    <div class="table-wrap">
      <table class="data-table"><thead><tr><th>用户</th><th>会员</th><th>任务</th><th>累计费用</th><th>最近登录</th><th>状态</th><th></th></tr></thead>
        <tbody><tr v-for="user in users" :key="user.id">
          <td><div class="person"><span>{{ (user.nickname||user.username)[0].toUpperCase() }}</span><div><b>{{ user.nickname||user.username }}</b><small>@{{ user.username }} · #{{ user.id }}</small></div></div></td>
          <td><span :class="['badge',user.memberLevel==='VIP'?'vip':'']">{{ user.memberLevel }}</span><small class="expire">{{ user.memberLevel==='VIP' ? memberExpiry(user) : '' }}</small></td>
          <td><b class="strong mono">{{ user.videoCount }}</b> 视频 · <b class="strong mono">{{ user.imageCount }}</b> 图片</td>
          <td class="strong mono">{{ money(user.totalCost) }}</td><td>{{ dateTime(user.lastLoginAt) }}</td>
          <td><span :class="['badge',user.status==='ENABLED'?'success':'danger']">{{ user.status==='ENABLED'?'正常':'已禁用' }}</span></td>
          <td><div class="actions"><button class="btn btn-small" @click="openMember(user)">会员</button><button :class="['btn','btn-small',user.status==='ENABLED'?'btn-danger':'']" @click="toggleStatus(user)">{{ user.status==='ENABLED'?'禁用':'启用' }}</button></div></td>
        </tr></tbody></table>
      <div v-if="!users.length && !loading" class="empty">没有找到符合条件的用户</div>
    </div>
    <div class="pager"><button class="btn btn-small" :disabled="page===1||loading" @click="go(page-1)">← 上一页</button><span>第 {{ page }} / {{ pages }} 页 · {{ total }} 位用户</span><button class="btn btn-small" :disabled="page>=pages||loading" @click="go(page+1)">下一页 →</button></div>

    <ModalDialog :open="!!editing" @close="editing=null">
      <h2>会员权益</h2><p class="modal-sub">{{ editing?.nickname||editing?.username }} · 当前 {{ editing?.memberLevel }}</p>
      <div class="member-options"><button v-for="option in durations" :key="String(option.days)" :class="['duration',memberDays===option.days?'active':'']" @click="memberDays=option.days"><b>{{ option.label }}</b><small>{{ option.note }}</small></button></div>
      <label class="field"><span>操作备注</span><textarea v-model="remark" class="textarea" placeholder="可选，记录本次操作原因" /></label>
      <div v-if="modalError" class="error">{{ modalError }}</div>
      <div class="modal-actions"><button v-if="editing?.memberLevel==='VIP'" class="btn btn-danger" :disabled="saving" @click="revoke">取消会员</button><span/><button class="btn" @click="editing=null">返回</button><button class="btn btn-primary" :disabled="saving" @click="saveMember">{{saving?'保存中…':'确认开通'}}</button></div>
    </ModalDialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ModalDialog from '@/components/ModalDialog.vue'
import { getUsers, openMembership, revokeMembership, setUserStatus } from '@/api'
import { dateTime, money } from '@/utils/format'
const users=ref([]),total=ref(0),page=ref(1),loading=ref(false),error=ref(''),editing=ref(null),memberDays=ref(30),remark=ref(''),saving=ref(false),modalError=ref('')
const filters=reactive({keyword:'',member:'',status:''}),pages=computed(()=>Math.max(1,Math.ceil(total.value/20)))
const durations=[{days:30,label:'30 天',note:'标准月度'},{days:90,label:'90 天',note:'季度会员'},{days:365,label:'365 天',note:'年度会员'},{days:null,label:'永久',note:'无到期日'}]
async function load(){loading.value=true;error.value='';try{const r=await getUsers({pageNum:page.value,pageSize:20,...filters});users.value=r.records;total.value=r.total}catch(e){error.value=e.message}finally{loading.value=false}}
function search(){page.value=1;load()} function go(p){page.value=p;load()}
async function toggleStatus(user){if(!confirm(`确认${user.status==='ENABLED'?'禁用':'启用'}用户 ${user.username}？`))return;try{await setUserStatus(user.id,user.status==='ENABLED'?'DISABLED':'ENABLED');load()}catch(e){error.value=e.message}}
function openMember(user){editing.value=user;memberDays.value=30;remark.value='';modalError.value=''}
async function saveMember(){saving.value=true;modalError.value='';try{await openMembership(editing.value.id,{days:memberDays.value,remark:remark.value});editing.value=null;load()}catch(e){modalError.value=e.message}finally{saving.value=false}}
async function revoke(){saving.value=true;modalError.value='';try{await revokeMembership(editing.value.id,remark.value);editing.value=null;load()}catch(e){modalError.value=e.message}finally{saving.value=false}}
function memberExpiry(user){return user.memberExpireAt?`至 ${dateTime(user.memberExpireAt).slice(0,10)}`:'永久'}
onMounted(load)
</script>

<style scoped>
.filters{margin-bottom:18px}.table-wrap{margin-top:14px}.person{display:flex;align-items:center;gap:10px}.person>span{width:35px;height:35px;border-radius:50%;display:grid;place-items:center;background:linear-gradient(145deg,#27345a,#302840);color:#d5ddff}.person b,.person small{display:block}.person small,.expire{color:var(--muted);font-size:10px}.expire{display:block;margin-top:4px}.member-options{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-bottom:20px}.duration{padding:14px;text-align:left;border:1px solid var(--line);border-radius:14px;background:rgba(255,255,255,.03)}.duration b,.duration small{display:block}.duration small{color:var(--muted)}.duration.active{border-color:rgba(134,169,255,.6);background:rgba(134,169,255,.09)}.modal-actions{display:flex;align-items:center;gap:8px;margin-top:20px}.modal-actions span{flex:1}
</style>

<template>
  <section class="page">
    <header class="page-head"><div><p class="eyebrow">AUDIENCE</p><h1 class="page-title">用户管理</h1><p class="page-sub">查看用户使用情况，管理账号状态。</p></div><button class="btn" @click="load">↻ 刷新</button></header>
    <div class="filter-row filters">
      <input v-model="filters.keyword" class="input" placeholder="搜索用户名或昵称" @keyup.enter="search" />
      <select v-model="filters.status" class="select"><option value="">全部状态</option><option value="ENABLED">正常</option><option value="DISABLED">已禁用</option></select>
      <button class="btn btn-primary" @click="search">筛选</button>
    </div>
    <div v-if="error" class="error">{{ error }}</div>
    <div class="table-wrap">
      <table class="data-table"><thead><tr><th>用户</th><th>任务</th><th>累计费用</th><th>最近登录</th><th>状态</th><th></th></tr></thead>
        <tbody><tr v-for="user in users" :key="user.id">
          <td><div class="person"><span>{{ (user.nickname||user.username)[0].toUpperCase() }}</span><div><b>{{ user.nickname||user.username }}</b><small>@{{ user.username }} · #{{ user.id }}</small></div></div></td>
          <td><b class="strong mono">{{ user.videoCount }}</b> 视频 · <b class="strong mono">{{ user.imageCount }}</b> 图片</td>
          <td class="strong mono">{{ money(user.totalCost) }}</td><td>{{ dateTime(user.lastLoginAt) }}</td>
          <td><span :class="['badge',user.status==='ENABLED'?'success':'danger']">{{ user.status==='ENABLED'?'正常':'已禁用' }}</span></td>
          <td><div class="actions"><button :class="['btn','btn-small',user.status==='ENABLED'?'btn-danger':'']" @click="toggleStatus(user)">{{ user.status==='ENABLED'?'禁用':'启用' }}</button></div></td>
        </tr></tbody></table>
      <div v-if="!users.length && !loading" class="empty">没有找到符合条件的用户</div>
    </div>
    <div class="pager"><button class="btn btn-small" :disabled="page===1||loading" @click="go(page-1)">← 上一页</button><span>第 {{ page }} / {{ pages }} 页 · {{ total }} 位用户</span><button class="btn btn-small" :disabled="page>=pages||loading" @click="go(page+1)">下一页 →</button></div>

  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getUsers, setUserStatus } from '@/api'
import { dateTime, money } from '@/utils/format'
const users=ref([]),total=ref(0),page=ref(1),loading=ref(false),error=ref('')
const filters=reactive({keyword:'',status:''}),pages=computed(()=>Math.max(1,Math.ceil(total.value/20)))
async function load(){loading.value=true;error.value='';try{const r=await getUsers({pageNum:page.value,pageSize:20,...filters});users.value=r.records;total.value=r.total}catch(e){error.value=e.message}finally{loading.value=false}}
function search(){page.value=1;load()} function go(p){page.value=p;load()}
async function toggleStatus(user){if(!confirm(`确认${user.status==='ENABLED'?'禁用':'启用'}用户 ${user.username}？`))return;try{await setUserStatus(user.id,user.status==='ENABLED'?'DISABLED':'ENABLED');load()}catch(e){error.value=e.message}}
onMounted(load)
</script>

<style scoped>
.filters{margin-bottom:18px}.table-wrap{margin-top:14px}.person{display:flex;align-items:center;gap:10px}.person>span{width:35px;height:35px;border-radius:50%;display:grid;place-items:center;background:linear-gradient(145deg,#27345a,#302840);color:#d5ddff}.person b,.person small{display:block}.person small{color:var(--muted);font-size:10px}
</style>

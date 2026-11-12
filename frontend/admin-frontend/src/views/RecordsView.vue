<template>
  <section class="page">
    <header class="page-head"><div><p class="eyebrow">GENERATIONS</p><h1 class="page-title">生成记录</h1><p class="page-sub">浏览全站图片与视频任务，追踪状态和费用。</p></div><div class="view-tabs"><button :class="{active:view==='grid'}" @click="view='grid'">▦</button><button :class="{active:view==='table'}" @click="view='table'">☷</button></div></header>
    <div class="filter-row"><input v-model="filters.keyword" class="input" placeholder="搜索提示词" @keyup.enter="search"/><input v-model="filters.userId" class="input user-id" inputmode="numeric" placeholder="用户 ID"/><select v-model="filters.type" class="select"><option value="">全部类型</option><option value="VIDEO">视频</option><option value="IMAGE">图片</option></select><button class="btn btn-primary" @click="search">筛选</button></div>
    <div v-if="error" class="error record-error">{{ error }}</div>
    <div v-if="view==='grid'" class="records-grid">
      <article v-for="item in records" :key="`${item.type}-${item.id}`" class="record-card card">
        <div class="preview">
          <img v-if="cover(item)" :src="cover(item)" alt="生成内容" loading="lazy"/>
          <video v-else-if="video(item)" :src="video(item)" muted preload="metadata"/>
          <div v-else class="placeholder"><span>{{item.type==='VIDEO'?'▶':'✦'}}</span></div>
          <span class="type">{{ item.type }}</span><span :class="['state',stateClass(item.task?.status)]">{{ item.task?.status }}</span>
        </div>
        <div class="record-body"><div class="record-title"><b>{{ item.task?.modelCode }}</b><span>#{{item.id}}</span></div><p>{{ item.task?.prompt || '未填写提示词' }}</p><div class="record-foot"><span>@{{ item.username || `用户 ${item.userId||'—'}` }}</span><b>{{ money(item.task?.totalCost) }}</b></div></div>
      </article>
      <div v-if="!records.length&&!loading" class="empty">暂无生成记录</div>
    </div>
    <div v-else class="table-wrap record-table"><table class="data-table"><thead><tr><th>ID</th><th>类型/模型</th><th>用户</th><th>状态</th><th>费用</th><th>创建时间</th></tr></thead><tbody><tr v-for="item in records" :key="`${item.type}-${item.id}`"><td class="mono">#{{item.id}}</td><td><b class="strong">{{item.type}}</b><br><small>{{item.task?.modelCode}}</small></td><td>@{{item.username||item.userId||'—'}}</td><td><span :class="['badge',stateClass(item.task?.status)]">{{item.task?.status}}</span></td><td class="mono strong">{{money(item.task?.totalCost)}}</td><td>{{dateTime(item.createdAt)}}</td></tr></tbody></table><div v-if="!records.length&&!loading" class="empty">暂无生成记录</div></div>
    <div class="pager"><button class="btn btn-small" :disabled="page===1||loading" @click="go(page-1)">← 上一页</button><span>第 {{page}} / {{pages}} 页 · {{total}} 条记录</span><button class="btn btn-small" :disabled="page>=pages||loading" @click="go(page+1)">下一页 →</button></div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getFeed } from '@/api'
import { dateTime, fileUrl, money } from '@/utils/format'
const records=ref([]),total=ref(0),page=ref(1),loading=ref(false),error=ref(''),view=ref('grid'),filters=reactive({keyword:'',userId:'',type:''}),pages=computed(()=>Math.max(1,Math.ceil(total.value/18)))
async function load(){loading.value=true;error.value='';try{const r=await getFeed({pageNum:page.value,pageSize:18,keyword:filters.keyword||undefined,userId:filters.userId||undefined,type:filters.type||undefined});records.value=r.records;total.value=r.total}catch(e){error.value=e.message}finally{loading.value=false}}
function search(){page.value=1;load()}function go(p){page.value=p;load()}
function cover(item){const t=item.task||{};if(item.type==='IMAGE')return fileUrl(t.imagePaths?.split(',')[0]);return t.imageUrl?.startsWith('/files/')?fileUrl(t.imageUrl):''}function video(item){return fileUrl(item.task?.localVideoPath)}
function stateClass(status){if(status==='SUCCEEDED')return'success';if(['FAILED','TIMEOUT','CANCELED'].includes(status))return'danger';return'processing'}
onMounted(load)
</script>

<style scoped>
.view-tabs{display:flex;padding:3px;border:1px solid var(--line);border-radius:999px}.view-tabs button{width:38px;height:34px;border:0;border-radius:999px;background:none;color:var(--muted)}.view-tabs button.active{background:rgba(255,255,255,.1);color:var(--text)}.user-id{max-width:130px}.record-error{margin-top:15px}.records-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:15px;margin-top:22px}.record-card{overflow:hidden;animation:rise .4s ease both}.preview{position:relative;aspect-ratio:16/10;background:#101116;overflow:hidden}.preview img,.preview video{width:100%;height:100%;object-fit:cover}.placeholder{width:100%;height:100%;display:grid;place-items:center;background:radial-gradient(circle at 25% 20%,rgba(78,116,215,.4),transparent 40%),linear-gradient(135deg,#151927,#28172c)}.placeholder span{font-size:30px;color:#d9dafa}.type,.state{position:absolute;top:10px;padding:4px 8px;border-radius:999px;font-size:9px;backdrop-filter:blur(10px)}.type{left:10px;background:rgba(0,0,0,.45)}.state{right:10px;background:rgba(0,0,0,.5)}.state.success{color:var(--green)}.state.danger{color:var(--red)}.state.processing{color:var(--blue)}.record-body{padding:15px}.record-title,.record-foot{display:flex;justify-content:space-between;gap:10px}.record-title span,.record-foot{color:var(--muted);font-size:11px}.record-body p{height:42px;overflow:hidden;color:var(--text-2);font-size:12px}.record-foot b{color:var(--text)}.record-table{margin-top:22px}
@media(max-width:1150px){.records-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:600px){.records-grid{grid-template-columns:1fr}}
</style>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">用量与参考费用</h1>
        <p class="page-sub">费用按模型参考单价估算，实际扣费、免费额度和折扣以你的厂商账单为准。</p>
        <p class="page-sub">
          视频与文生图的费用和耗时 · 合计花费 <span class="num">{{ money(Number(stats?.totalCost || 0) + Number(imageStats?.totalCost || 0)) }}</span>
        </p>
      </div>
      <button class="btn" :disabled="loading" @click="reload">刷新</button>
    </header>

    <div class="content">
      <section class="tiles">
        <div class="tile">
          <span class="tile-label">视频累计花费</span>
          <span class="tile-value num">{{ money(stats?.totalCost) }}</span>
          <span class="tile-sub num">视频 {{ money(stats?.totalVideoCost) }} · 润色 {{ money(stats?.totalLlmCost) }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">视频数</span>
          <span class="tile-value num">{{ stats?.succeededCount ?? '—' }}</span>
          <span class="tile-sub num">共 {{ stats?.totalCount ?? 0 }} · 失败 {{ stats?.failedCount ?? 0 }} · 进行中 {{ stats?.inProgressCount ?? 0 }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">视频总时长</span>
          <span class="tile-value num">{{ stats ? stats.totalVideoSeconds + 's' : '—' }}</span>
          <span class="tile-sub">已成功生成的计费秒数</span>
        </div>
        <div class="tile">
          <span class="tile-label">平均每条费用</span>
          <span class="tile-value num">{{ money(stats?.avgCost) }}</span>
          <span class="tile-sub">仅统计成功的视频</span>
        </div>
        <div class="tile">
          <span class="tile-label">平均耗时</span>
          <span class="tile-value num">{{ elapsed(stats?.avgElapsedMs) }}</span>
          <span class="tile-sub">从点击生成到视频完成</span>
        </div>
      </section>

      <section v-if="stats?.byModel?.length" class="panel block">
        <h3>视频 · 按模型</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>模型</th><th class="r">任务数</th><th class="r">成功</th><th class="r">视频时长</th><th class="r">平均耗时</th><th class="r">花费</th></tr>
            </thead>
            <tbody>
              <tr v-for="m in stats.byModel" :key="m.modelCode">
                <td>{{ m.modelName }}<span class="code">{{ m.modelCode }}</span></td>
                <td class="r num">{{ m.totalCount }}</td>
                <td class="r num">{{ m.succeededCount }}</td>
                <td class="r num">{{ m.totalVideoSeconds }}s</td>
                <td class="r num">{{ elapsed(m.avgElapsedMs) }}</td>
                <td class="r num strong">{{ money(m.totalCost) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 文生图 -->
      <section class="tiles image-tiles">
        <div class="tile">
          <span class="tile-label">文生图累计花费</span>
          <span class="tile-value num">{{ money(imageStats?.totalCost) }}</span>
          <span class="tile-sub num">图片 {{ money(imageStats?.totalImageCost) }} · 润色 {{ money(imageStats?.totalLlmCost) }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">图片张数</span>
          <span class="tile-value num">{{ imageStats?.totalImages ?? '—' }}</span>
          <span class="tile-sub num">共 {{ imageStats?.totalCount ?? 0 }} 次 · 失败 {{ imageStats?.failedCount ?? 0 }} · 进行中 {{ imageStats?.inProgressCount ?? 0 }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">平均每张费用</span>
          <span class="tile-value num">{{ money(imageStats?.avgCost) }}</span>
          <span class="tile-sub">美元计价的按汇率折算成人民币</span>
        </div>
        <div class="tile">
          <span class="tile-label">平均耗时</span>
          <span class="tile-value num">{{ elapsed(imageStats?.avgElapsedMs) }}</span>
          <span class="tile-sub">从点击生成到图片完成</span>
        </div>
      </section>

      <section v-if="imageStats?.byModel?.length" class="panel block">
        <h3>文生图 · 按模型</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>模型</th><th class="r">次数</th><th class="r">成功</th><th class="r">图片张数</th><th class="r">平均耗时</th><th class="r">花费</th></tr>
            </thead>
            <tbody>
              <tr v-for="m in imageStats.byModel" :key="m.modelCode">
                <td>{{ m.modelName }}<span class="code">{{ m.modelCode }}</span></td>
                <td class="r num">{{ m.totalCount }}</td>
                <td class="r num">{{ m.succeededCount }}</td>
                <td class="r num">{{ m.totalImages }}</td>
                <td class="r num">{{ elapsed(m.avgElapsedMs) }}</td>
                <td class="r num strong">{{ money(m.totalCost) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel block">
        <div class="block-head">
          <h3>视频任务明细</h3>
          <PillSelect v-model="statusFilter" :options="statusOptions" placement="down" />
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th><th>模型</th><th>状态</th><th>规格</th>
                <th class="r">计费时长</th><th class="r">单价</th><th class="r">视频费用</th><th class="r">润色费用</th><th class="r">总费用</th>
                <th class="r">排队</th><th class="r">生成</th><th class="r">总耗时</th><th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in page.records" :key="t.id">
                <td class="num">#{{ t.id }}<span v-if="t.mock" class="mock">mock</span></td>
                <td class="nowrap">{{ t.modelName }}</td>
                <td><span class="badge" :class="STATUS_TONE[t.status]"><span class="dot" />{{ t.statusText }}</span></td>
                <td class="nowrap muted">{{ t.resolution }}<template v-if="t.ratio"> · {{ t.ratio }}</template> · {{ t.duration }}s</td>
                <td class="r num">{{ seconds(t.cost.billedDuration) }}</td>
                <td class="r num">{{ t.cost.unitPrice != null ? (t.cost.currency === 'USD' ? '$' + Number(t.cost.unitPrice).toFixed(3) : '¥' + Number(t.cost.unitPrice).toFixed(2)) + '/s' : '—' }}</td>
                <td class="r num">{{ money(t.cost.videoCost) }}</td>
                <td class="r num">{{ money(t.cost.llmCost) }}</td>
                <td class="r num strong">{{ t.finished ? money(t.cost.totalCost) : '≈' + money(t.cost.estimatedCost) }}</td>
                <td class="r num">{{ elapsed(t.timing.queueMs) }}</td>
                <td class="r num">{{ elapsed(t.timing.generateMs) }}</td>
                <td class="r num">{{ elapsed(t.timing.totalElapsedMs) }}</td>
                <td class="nowrap muted num">{{ shortTime(t.createdAt) }}</td>
              </tr>
              <tr v-if="!page.records.length">
                <td colspan="13" class="empty">{{ loading ? '加载中…' : '暂无数据' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer v-if="page.total > pageSize" class="pager">
          <span class="muted num">第 {{ pageNum }} / {{ totalPages }} 页 · 共 {{ page.total }} 条</span>
          <button class="btn" :disabled="pageNum <= 1" @click="go(pageNum - 1)">上一页</button>
          <button class="btn" :disabled="pageNum >= totalPages" @click="go(pageNum + 1)">下一页</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import PillSelect from '@/components/PillSelect.vue'
import { getImageStats, getStats, pageVideos } from '@/api'
import { elapsed, money, seconds, shortTime, STATUS_TONE } from '@/utils/format'
import { useToast } from '@/composables/useToast'

defineProps({ status: { type: Object, default: () => ({}) } })

const toast = useToast()
const pageSize = 20
const stats = ref(null)
const imageStats = ref(null)
const page = ref({ total: 0, records: [] })
const pageNum = ref(1)
const loading = ref(false)
const statusFilter = ref('')

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'SUCCEEDED', label: '已完成' },
  { value: 'RUNNING', label: '生成中' },
  { value: 'PENDING', label: '排队中' },
  { value: 'FAILED', label: '失败' },
  { value: 'TIMEOUT', label: '超时' }
]

const totalPages = computed(() => Math.max(1, Math.ceil(page.value.total / pageSize)))

async function loadPage() {
  loading.value = true
  try {
    page.value = await pageVideos({ pageNum: pageNum.value, pageSize, status: statusFilter.value || undefined })
  } catch (e) {
    toast.error('加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function reload() {
  const [v, i] = await Promise.allSettled([getStats(), getImageStats()])
  if (v.status === 'fulfilled') stats.value = v.value
  else toast.error('加载视频统计失败：' + v.reason.message)
  if (i.status === 'fulfilled') imageStats.value = i.value
  else toast.error('加载文生图统计失败：' + i.reason.message)
  await loadPage()
}

function go(n) {
  pageNum.value = n
  loadPage()
}

watch(statusFilter, () => {
  pageNum.value = 1
  loadPage()
})

onMounted(reload)
</script>

<style scoped>
.page {
  padding-bottom: 8px;
}
.page-header,
.content {
  max-width: 1440px;
  margin-left: auto;
  margin-right: auto;
}
.content {
  padding: 24px 40px 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.tiles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
.tile {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 20px 22px;
  border-radius: var(--radius-lg);
  background: var(--surface-2);
}
.tile:first-child {
  background: var(--accent-soft), var(--surface-2);
  background-blend-mode: normal;
}
.tile-label {
  font-size: 13px;
  color: var(--text-2);
}
.tile-value {
  font-size: 32px;
  font-weight: 400;
  letter-spacing: -0.01em;
  line-height: 1.2;
}
.tile-sub {
  font-size: 12px;
  color: var(--muted);
}
.block {
  padding: 20px 0 8px;
  overflow: hidden;
}
.block h3 {
  margin: 0 24px 14px;
  font-size: 16px;
  font-weight: 500;
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 16px;
}
.block-head h3 {
  margin-bottom: 14px;
}
.table-wrap {
  overflow-x: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
th,
td {
  padding: 12px 24px;
  text-align: left;
}
th {
  font-weight: 500;
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  border-bottom: 1px solid var(--border-strong);
}
td {
  border-bottom: 1px solid var(--border);
}
tbody tr:last-child td {
  border-bottom: 0;
}
tbody tr:hover td {
  background: var(--surface-3);
}
.r {
  text-align: right;
}
.strong {
  font-weight: 500;
  color: var(--text);
}
.muted {
  color: var(--muted);
}
.nowrap {
  white-space: nowrap;
}
.code {
  margin-left: 8px;
  font-family: var(--mono);
  font-size: 11px;
  color: var(--faint);
}
.mock {
  margin-left: 6px;
  font-size: 10px;
  color: var(--warning);
  text-transform: uppercase;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 24px 8px;
}
.pager .muted {
  margin-right: auto;
  font-size: 12px;
}
@media (max-width: 720px) {
  .content {
    padding: 16px;
  }
  th,
  td {
    padding: 10px 14px;
  }
}
</style>

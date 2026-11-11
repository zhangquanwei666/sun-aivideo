<template>
  <div class="generate">
    <header class="page-header">
      <div>
        <h1 class="page-title">生成视频</h1>
        <p class="page-sub">共 {{ total }} 条 · 累计花费 <span class="num">{{ money(stats?.totalCost) }}</span></p>
      </div>
      <div v-if="status.mockMode" class="badge warning" title="在后端配置 DASHSCOPE_API_KEY 后即可真实调用百炼">
        Mock 模式 · 不产生真实费用
      </div>
    </header>

    <section class="feed">
      <div v-if="loading && !tasks.length" class="empty">加载中…</div>
      <div v-else-if="!tasks.length" class="intro">
        <h2>用一句话生成你的第一个视频</h2>
        <p>选择模型、分辨率和时长后点击生成。每条视频都会记录实际费用和各阶段耗时。</p>
        <div class="ideas">
          <button v-for="idea in ideas" :key="idea" class="idea" @click="composer?.reuse({ mode: 'T2V', modelCode: defaultT2V, prompt: idea, resolution: '720P', ratio: '16:9', duration: 5 })">
            {{ idea }}
          </button>
        </div>
      </div>
      <div v-else class="grid">
        <VideoCard v-for="t in tasks" :key="t.id" :task="t" :now="now" @open="openTask" />
      </div>
      <div v-if="tasks.length < total" class="more">
        <button class="btn" :disabled="loading" @click="loadMore">{{ loading ? '加载中…' : '加载更多' }}</button>
      </div>
    </section>

    <div class="composer-dock">
      <PromptComposer ref="composer" :models="models" :status="status" :submitting="submitting" @submit="onSubmit" />
    </div>

    <TaskDetailDrawer :task="activeTask" @close="activeId = null" @reuse="onReuse" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import PromptComposer from '@/components/PromptComposer.vue'
import VideoCard from '@/components/VideoCard.vue'
import TaskDetailDrawer from '@/components/TaskDetailDrawer.vue'
import { generateVideo, getStats, getVideo, listModels, pageVideos } from '@/api'
import { money } from '@/utils/format'
import { useToast } from '@/composables/useToast'
import { useNow } from '@/composables/useNow'

defineProps({ status: { type: Object, default: () => ({}) } })

const PAGE_SIZE = 24
const POLL_MS = 3000
const toast = useToast()
const now = useNow()

const composer = ref(null)
const models = ref([])
const tasks = ref([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const submitting = ref(false)
const stats = ref(null)
const activeId = ref(null)

const activeTask = computed(() => tasks.value.find((t) => t.id === activeId.value) || null)
const defaultT2V = computed(() => models.value.find((m) => m.mode === 'T2V')?.modelCode)

const ideas = [
  '雨夜的霓虹街头，一只橘猫踩着水洼奔跑，镜头低角度跟随，电影感',
  '清晨的海边灯塔，海浪拍打礁石，航拍镜头缓慢推进',
  '一杯咖啡上方升起热气，微距特写，暖色调，浅景深'
]

async function loadPage(reset = false) {
  loading.value = true
  try {
    if (reset) pageNum.value = 1
    const res = await pageVideos({ pageNum: pageNum.value, pageSize: PAGE_SIZE })
    total.value = res.total
    tasks.value = reset ? res.records : mergeTasks(tasks.value, res.records)
  } catch (e) {
    toast.error('加载任务失败：' + e.message)
  } finally {
    loading.value = false
  }
}

function mergeTasks(current, incoming) {
  const ids = new Set(current.map((t) => t.id))
  return current.concat(incoming.filter((t) => !ids.has(t.id)))
}

function loadMore() {
  pageNum.value += 1
  loadPage()
}

async function refreshStats() {
  try {
    stats.value = await getStats()
  } catch {
    /* 忽略 */
  }
}

async function onSubmit(payload) {
  submitting.value = true
  try {
    const task = await generateVideo(payload)
    tasks.value.unshift(task)
    total.value += 1
    toast.success('已提交，生成中…')
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}

function openTask(task) {
  activeId.value = task.id
}

function onReuse(task) {
  composer.value?.reuse(task)
  activeId.value = null
}

// 轮询未完成的任务
let timer
async function pollUnfinished() {
  const pending = tasks.value.filter((t) => !t.finished)
  if (!pending.length) return
  const results = await Promise.allSettled(pending.map((t) => getVideo(t.id)))
  let finishedAny = false
  results.forEach((r) => {
    if (r.status !== 'fulfilled') return
    const i = tasks.value.findIndex((t) => t.id === r.value.id)
    if (i >= 0) {
      if (r.value.finished && !tasks.value[i].finished) finishedAny = true
      tasks.value[i] = r.value
    }
  })
  if (finishedAny) refreshStats()
}

onMounted(async () => {
  try {
    models.value = await listModels()
  } catch (e) {
    toast.error('加载模型失败：' + e.message)
  }
  await Promise.all([loadPage(true), refreshStats()])
  timer = setInterval(pollUnfinished, POLL_MS)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.generate {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.feed {
  flex: 1;
  overflow-y: auto;
  padding: 20px 32px 24px;
  min-height: 0;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.more {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px;
}
.intro {
  max-width: 640px;
  margin: 10vh auto 0;
  text-align: center;
}
.intro h2 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -0.02em;
}
.intro p {
  margin: 0 0 24px;
  color: var(--muted);
}
.ideas {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.idea {
  padding: 12px 16px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-2);
  text-align: left;
  transition: all 0.15s;
}
.idea:hover {
  border-color: var(--border-strong);
  color: var(--text);
  background: var(--surface-2);
}
.composer-dock {
  flex: none;
  padding: 0 32px 20px;
  position: relative;
}
.composer-dock::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 100%;
  height: 32px;
  background: linear-gradient(to bottom, transparent, var(--bg));
  pointer-events: none;
}
.composer-dock > * {
  max-width: 960px;
  margin: 0 auto;
}
@media (max-width: 720px) {
  .feed {
    padding: 16px;
  }
  .composer-dock {
    padding: 0 16px 16px;
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

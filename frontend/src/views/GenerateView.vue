<template>
  <div class="generate">
    <section class="feed">
      <div v-if="loading && !tasks.length" class="empty">加载中…</div>

      <div v-else-if="!tasks.length" class="hero">
        <h1 class="hero-title"><span class="grad-text">想生成什么样的视频？</span></h1>
        <p class="hero-sub">在下方描述画面，选好模型、分辨率和时长后生成。每条视频都会记录实际费用和各阶段耗时。</p>
        <div class="ideas">
          <button v-for="idea in ideas" :key="idea.text" class="idea" @click="composer?.reuse({ mode: 'T2V', modelCode: defaultT2V, prompt: idea.text, resolution: '720P', ratio: '16:9', duration: 5 })">
            <span class="idea-icon">{{ idea.icon }}</span>
            <span>{{ idea.text }}</span>
          </button>
        </div>
      </div>

      <template v-else>
        <header class="feed-head">
          <div>
            <h1 class="page-title">生成视频</h1>
            <p class="page-sub">共 {{ total }} 条 · 累计花费 <span class="num">{{ money(stats?.totalCost) }}</span></p>
          </div>
          <span v-if="status.mockMode" class="badge warning" title="在后端配置 DASHSCOPE_API_KEY 后即可真实调用百炼">
            Mock 模式 · 不产生真实费用
          </span>
        </header>
        <div class="grid">
          <VideoCard v-for="t in tasks" :key="t.id" :task="t" :now="now" @open="openTask" />
        </div>
        <div v-if="tasks.length < total" class="more">
          <button class="btn" :disabled="loading" @click="loadMore">{{ loading ? '加载中…' : '加载更多' }}</button>
        </div>
      </template>
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
  { icon: '城市', text: '雨夜的霓虹街头，一只橘猫踩着水洼奔跑，镜头低角度跟随，电影感' },
  { icon: '风景', text: '清晨的海边灯塔，海浪拍打礁石，航拍镜头缓慢推进' },
  { icon: '特写', text: '一杯咖啡上方升起热气，微距特写，暖色调，浅景深' }
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
  padding: 8px 40px 32px;
  min-height: 0;
}
.feed-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  max-width: 1440px;
  margin: 12px auto 24px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 28px 20px;
  max-width: 1440px;
  margin: 0 auto;
}
.more {
  display: flex;
  justify-content: center;
  padding: 32px 0 8px;
}

/* 空状态：渐变大标题 + 灵感卡片 */
.hero {
  max-width: 760px;
  margin: 12vh auto 0;
  text-align: center;
}
.hero-title {
  margin: 0 0 12px;
  font-size: 44px;
  font-weight: 400;
  letter-spacing: -0.02em;
  line-height: 1.2;
}
.hero-sub {
  margin: 0 auto 36px;
  max-width: 560px;
  color: var(--muted);
  font-size: 15px;
}
.ideas {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.idea {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 16px;
  min-height: 120px;
  border-radius: var(--radius-lg);
  border: 0;
  background: var(--surface-2);
  color: var(--text-2);
  text-align: left;
  font-size: 14px;
  line-height: 1.55;
  transition: background 0.15s, color 0.15s, transform 0.15s;
}
.idea:hover {
  background: var(--surface-3);
  color: var(--text);
  transform: translateY(-2px);
}
.idea-icon {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  background: var(--accent-soft);
  color: var(--primary);
}

/* 底部悬浮输入框 */
.composer-dock {
  flex: none;
  padding: 0 40px 24px;
  position: relative;
}
.composer-dock::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 100%;
  height: 48px;
  background: linear-gradient(to bottom, transparent, var(--bg));
  pointer-events: none;
}
.composer-dock > * {
  max-width: 960px;
  margin: 0 auto;
}
@media (max-width: 720px) {
  .feed {
    padding: 8px 16px 16px;
  }
  .composer-dock {
    padding: 0 12px 12px;
  }
  .grid {
    grid-template-columns: 1fr;
  }
  .hero {
    margin-top: 6vh;
  }
  .hero-title {
    font-size: 30px;
  }
  .ideas {
    grid-template-columns: 1fr;
  }
  .idea {
    min-height: 0;
  }
}
</style>

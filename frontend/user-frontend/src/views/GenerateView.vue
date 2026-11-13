<template>
  <div class="generate">
    <section class="feed">
      <div v-if="loading && !items.length" class="empty">加载中…</div>

      <div v-else-if="!items.length" class="hero">
        <h1 class="hero-title"><span class="grad-text">想生成什么样的视频？</span></h1>
        <p class="hero-sub">在下方描述画面，选好模型、分辨率和时长后生成。也可以先用「文生图」做出首帧，再生成视频。每条记录都会统计实际费用和各阶段耗时。</p>
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
            <h1 class="page-title">生成记录</h1>
            <p class="page-sub">共 {{ total }} 条 · 累计花费 <span class="num">{{ money(totalCost) }}</span></p>
          </div>
          <span v-if="status.mockMode" class="badge warning" title="在后端配置 API Key 后即可真实调用">
            Mock 模式 · 不产生真实费用
          </span>
        </header>
        <div class="grid">
          <template v-for="item in items" :key="item.key">
            <VideoCard v-if="item.type === 'VIDEO'" :task="item.task" :now="now" @open="openItem(item)" @delete="askDelete(item)" />
            <ImageCard
              v-else
              :task="item.task"
              :now="now"
              @open="openItem(item)"
              @delete="askDelete(item)"
              @use-as-frame="useAsFirstFrame"
            />
          </template>
        </div>
        <div v-if="items.length < total" class="more">
          <button class="btn" :disabled="loading" @click="loadMore">{{ loading ? '加载中…' : '加载更多' }}</button>
        </div>
      </template>
    </section>

    <div class="composer-dock">
      <PromptComposer
        ref="composer"
        :providers="providers"
        :models="models"
        :image-providers="imageProviders"
        :image-models="imageModels"
        :status="status"
        :submitting="submitting"
        @submit="onSubmit"
      />
    </div>

    <TaskDetailDrawer
      :task="activeItem?.type === 'VIDEO' ? activeItem.task : null"
      @close="activeKey = null"
      @reuse="onReuseVideo"
      @delete="(t) => askDelete(activeItem)"
    />
    <ImageDetailDrawer
      :task="activeItem?.type === 'IMAGE' ? activeItem.task : null"
      @close="activeKey = null"
      @reuse="onReuseImage"
      @delete="(t) => askDelete(activeItem)"
      @use-as-frame="useAsFirstFrame"
    />

    <ConfirmDialog
      :open="!!pendingDelete"
      danger
      :title="pendingDelete?.type === 'IMAGE' ? '删除这张图片？' : '删除这条视频？'"
      confirm-text="删除"
      :busy="deleting"
      @confirm="confirmDelete"
      @cancel="pendingDelete = null"
    >
      删除后将从生成记录中隐藏，不再显示。已产生的费用仍会计入用量统计。
    </ConfirmDialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import PromptComposer from '@/components/PromptComposer.vue'
import VideoCard from '@/components/VideoCard.vue'
import ImageCard from '@/components/ImageCard.vue'
import TaskDetailDrawer from '@/components/TaskDetailDrawer.vue'
import ImageDetailDrawer from '@/components/ImageDetailDrawer.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import {
  deleteImage,
  deleteVideo,
  generateImage,
  generateVideo,
  getImage,
  getImageStats,
  getStats,
  getVideo,
  listImageModels,
  listModels,
  pageFeed
} from '@/api'
import { money } from '@/utils/format'
import { useToast } from '@/composables/useToast'
import { useNow } from '@/composables/useNow'

defineProps({ status: { type: Object, default: () => ({}) } })

const PAGE_SIZE = 24
const POLL_MS = 10000
const toast = useToast()
const now = useNow()

const composer = ref(null)
const providers = ref([])
const models = ref([])
const imageProviders = ref([])
const imageModels = ref([])

/** 生成记录：[{ key, type: 'VIDEO' | 'IMAGE', task }] */
const items = ref([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const submitting = ref(false)
const videoStats = ref(null)
const imageStats = ref(null)
const activeKey = ref(null)

const activeItem = computed(() => items.value.find((i) => i.key === activeKey.value) || null)
const defaultT2V = computed(() => models.value.find((m) => m.mode === 'T2V')?.modelCode)
const totalCost = computed(() => Number(videoStats.value?.totalCost || 0) + Number(imageStats.value?.totalCost || 0))

const ideas = [
  { icon: '城市', text: '雨夜的霓虹街头，一只橘猫踩着水洼奔跑，镜头低角度跟随，电影感' },
  { icon: '风景', text: '清晨的海边灯塔，海浪拍打礁石，航拍镜头缓慢推进' },
  { icon: '特写', text: '一杯咖啡上方升起热气，微距特写，暖色调，浅景深' }
]

function toItem(type, task) {
  return { key: `${type}-${task.id}`, type, task }
}

async function loadPage(reset = false) {
  loading.value = true
  try {
    if (reset) pageNum.value = 1
    const res = await pageFeed({ pageNum: pageNum.value, pageSize: PAGE_SIZE })
    total.value = res.total
    const incoming = res.records.map((r) => (r.type === 'IMAGE' ? toItem('IMAGE', r.image) : toItem('VIDEO', r.video)))
    if (reset) {
      items.value = incoming
    } else {
      const keys = new Set(items.value.map((i) => i.key))
      items.value = items.value.concat(incoming.filter((i) => !keys.has(i.key)))
    }
  } catch (e) {
    toast.error('加载生成记录失败：' + e.message)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  pageNum.value += 1
  loadPage()
}

async function refreshStats() {
  const [v, i] = await Promise.allSettled([getStats(), getImageStats()])
  if (v.status === 'fulfilled') videoStats.value = v.value
  if (i.status === 'fulfilled') imageStats.value = i.value
}

async function onSubmit({ kind, payload }) {
  submitting.value = true
  try {
    if (kind === 'image') {
      const task = await generateImage(payload)
      items.value.unshift(toItem('IMAGE', task))
    } else {
      const task = await generateVideo(payload)
      items.value.unshift(toItem('VIDEO', task))
    }
    total.value += 1
    toast.success('已提交，生成中…')
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}

function openItem(item) {
  activeKey.value = item.key
}

function onReuseVideo(task) {
  composer.value?.reuse(task)
  activeKey.value = null
}

function onReuseImage(task) {
  composer.value?.reuseImage(task)
  activeKey.value = null
}

/** 生成的图片作为首帧 → 切换到图生视频 */
function useAsFirstFrame(task) {
  const url = task.imageUrls?.[0]
  if (!url) return
  composer.value?.useAsFirstFrame(url)
  activeKey.value = null
  toast.info('已设为首帧，描述一下画面要怎么动，然后生成视频')
}

// ---------- 删除（逻辑删除，后端只是隐藏） ----------
const pendingDelete = ref(null)
const deleting = ref(false)

function askDelete(item) {
  pendingDelete.value = item
}

async function confirmDelete() {
  const item = pendingDelete.value
  if (!item) return
  deleting.value = true
  try {
    await (item.type === 'IMAGE' ? deleteImage(item.task.id) : deleteVideo(item.task.id))
    items.value = items.value.filter((i) => i.key !== item.key)
    total.value = Math.max(0, total.value - 1)
    if (activeKey.value === item.key) activeKey.value = null
    pendingDelete.value = null
    toast.success('已删除')
    refreshStats()
  } catch (e) {
    toast.error('删除失败：' + e.message)
  } finally {
    deleting.value = false
  }
}

// ---------- 轮询未完成的任务 ----------
let timer
async function pollUnfinished() {
  const pending = items.value.filter((i) => !i.task.finished)
  if (!pending.length) return
  const results = await Promise.allSettled(
    pending.map((i) => (i.type === 'IMAGE' ? getImage(i.task.id) : getVideo(i.task.id)).then((task) => ({ key: i.key, task })))
  )
  let finishedAny = false
  results.forEach((r) => {
    if (r.status !== 'fulfilled') return
    const idx = items.value.findIndex((i) => i.key === r.value.key)
    if (idx >= 0) {
      if (r.value.task.finished && !items.value[idx].task.finished) finishedAny = true
      items.value[idx] = { ...items.value[idx], task: r.value.task }
    }
  })
  if (finishedAny) refreshStats()
}

onMounted(async () => {
  const [video, image] = await Promise.allSettled([listModels(), listImageModels()])
  if (video.status === 'fulfilled') {
    providers.value = video.value.providers
    models.value = video.value.models
  } else {
    toast.error('加载视频模型失败：' + video.reason.message)
  }
  if (image.status === 'fulfilled') {
    imageProviders.value = image.value.providers
    imageModels.value = image.value.models
  } else {
    toast.error('加载文生图模型失败：' + image.reason.message)
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

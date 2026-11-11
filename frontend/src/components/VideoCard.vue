<template>
  <article class="card" @click="$emit('open', task)">
    <div class="media" @mouseenter="play" @mouseleave="pause">
      <video
        v-if="task.status === 'SUCCEEDED' && task.videoUrl"
        ref="video"
        :src="task.videoUrl"
        muted
        loop
        playsinline
        preload="metadata"
      />
      <div v-else-if="!task.finished" class="placeholder generating">
        <div class="shimmer" />
        <div class="ph-text">
          <span class="badge info"><span class="dot" />{{ task.statusText }}</span>
          <span class="num ph-time">{{ elapsed(waitedMs) }}</span>
        </div>
      </div>
      <div v-else class="placeholder failed">
        <span class="badge" :class="tone">{{ task.statusText }}</span>
        <p class="err">{{ task.errorMessage || '生成失败' }}</p>
      </div>

      <div class="overlay-top">
        <span v-if="task.mock" class="tag">MOCK</span>
        <span class="tag">{{ task.mode === 'I2V' ? '图生' : '文生' }}</span>
      </div>
    </div>

    <div class="info">
      <p class="prompt">{{ task.prompt || '（无提示词，仅首帧图片）' }}</p>
      <div class="meta">
        <span class="spec">{{ task.modelName }} · {{ task.resolution }}<template v-if="task.ratio"> · {{ task.ratio }}</template> · {{ task.duration }}s</span>
        <span class="figures num">
          <span :title="task.finished ? '实际费用' : '预计费用'">{{ task.finished ? money(task.cost.totalCost) : '≈' + money(task.cost.estimatedCost) }}</span>
          <span class="sep" />
          <span title="生成耗时">{{ task.finished ? elapsed(task.timing.totalElapsedMs) : '…' }}</span>
        </span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { elapsed, money, parseTime, STATUS_TONE } from '@/utils/format'

const props = defineProps({
  task: { type: Object, required: true },
  now: { type: Number, default: () => Date.now() }
})
defineEmits(['open'])

const video = ref(null)
const tone = computed(() => STATUS_TONE[props.task.status] || 'muted')
const waitedMs = computed(() => {
  const created = parseTime(props.task.createdAt)
  return created ? Math.max(0, props.now - created.getTime()) : null
})

function play() {
  video.value?.play().catch(() => {})
}
function pause() {
  if (video.value) video.value.pause()
}
</script>

<style scoped>
.card {
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s;
}
.card:hover {
  border-color: var(--border-strong);
}
.media {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
}
.media video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  background: var(--surface-2);
}
.shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, transparent 30%, rgba(255, 255, 255, 0.05) 50%, transparent 70%);
  background-size: 200% 100%;
  animation: shimmer 1.8s linear infinite;
}
@keyframes shimmer {
  from { background-position: 150% 0; }
  to { background-position: -50% 0; }
}
.ph-text {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.ph-time {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.02em;
}
.failed .err {
  margin: 0;
  max-width: 90%;
  color: var(--muted);
  font-size: 12px;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.overlay-top {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  gap: 6px;
}
.tag {
  height: 20px;
  padding: 0 7px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  display: inline-flex;
  align-items: center;
  color: var(--text-2);
}
.info {
  padding: 10px 12px 12px;
}
.prompt {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}
.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: var(--muted);
}
.spec {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.figures {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
  color: var(--text-2);
}
.sep {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--faint);
}
</style>

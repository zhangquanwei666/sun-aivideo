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
        <div class="glow" />
        <div class="ph-text">
          <span class="ph-status"><span class="dot" />{{ task.statusText }}</span>
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
      <div v-if="task.status === 'SUCCEEDED'" class="overlay-bottom num">
        <span class="tag">{{ task.duration }}s</span>
      </div>
    </div>

    <div class="info">
      <p class="prompt">{{ task.prompt || '（无提示词，仅首帧图片）' }}</p>
      <div class="meta">
        <span class="spec">{{ task.modelName }} · {{ task.resolution }}<template v-if="task.ratio"> · {{ task.ratio }}</template></span>
        <span class="figures num">
          <span :title="task.finished ? '实际费用' : '预计费用'">{{ task.finished ? money(task.cost.totalCost) : '≈' + money(task.cost.estimatedCost) }}</span>
          <span class="sep" />
          <span title="生成耗时">{{ task.finished ? elapsed(task.timing.totalElapsedMs) : '生成中' }}</span>
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
  cursor: pointer;
  min-width: 0;
}
.media {
  position: relative;
  aspect-ratio: 16 / 9;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: #000;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.card:hover .media {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 0 2px rgba(168, 199, 250, 0.35);
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
/* 生成中：缓慢流动的蓝紫光晕 */
.glow {
  position: absolute;
  inset: -40%;
  background: radial-gradient(circle at 30% 40%, rgba(76, 141, 246, 0.35), transparent 45%),
    radial-gradient(circle at 70% 60%, rgba(143, 107, 232, 0.3), transparent 45%);
  filter: blur(20px);
  animation: drift 6s ease-in-out infinite alternate;
}
@keyframes drift {
  from { transform: translate(-6%, -4%) rotate(0deg); }
  to { transform: translate(6%, 4%) rotate(20deg); }
}
.ph-text {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.ph-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-2);
}
.ph-status .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary);
  animation: pulse 1.4s ease-in-out infinite;
}
.ph-time {
  font-size: 28px;
  font-weight: 400;
  letter-spacing: -0.01em;
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
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
}
.overlay-bottom {
  position: absolute;
  right: 10px;
  bottom: 10px;
}
.tag {
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(19, 19, 20, 0.7);
  backdrop-filter: blur(8px);
  font-size: 11px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  color: var(--text-2);
}
.info {
  padding: 12px 4px 0;
}
.prompt {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
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

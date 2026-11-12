<template>
  <article class="card" @click="$emit('open', task)">
    <div class="media">
      <img v-if="task.status === 'SUCCEEDED' && cover" :src="cover" :alt="task.prompt" loading="lazy" />
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
        <span class="tag image">文生图</span>
      </div>
      <div v-if="task.status === 'SUCCEEDED'" class="overlay-bottom">
        <button type="button" class="use-btn" title="用这张图作为首帧生成视频" @click.stop="$emit('use-as-frame', task)">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="13" height="14" rx="2.5" /><path d="m16 10 5-3v10l-5-3" /></svg>
          生成视频
        </button>
      </div>
      <button
        v-if="task.finished"
        type="button"
        class="delete-btn"
        title="删除"
        aria-label="删除图片"
        @click.stop="$emit('delete', task)"
      >
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" /></svg>
      </button>
    </div>

    <div class="info">
      <p class="prompt">{{ task.prompt }}</p>
      <div class="meta">
        <span class="spec">{{ task.modelName }} · {{ task.imageSize }} · {{ task.aspectRatio }}</span>
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
import { computed } from 'vue'
import { elapsed, money, parseTime, STATUS_TONE } from '@/utils/format'

const props = defineProps({
  task: { type: Object, required: true },
  now: { type: Number, default: () => Date.now() }
})
defineEmits(['open', 'delete', 'use-as-frame'])

const cover = computed(() => props.task.imageUrls?.[0])
const tone = computed(() => STATUS_TONE[props.task.status] || 'muted')
const waitedMs = computed(() => {
  const created = parseTime(props.task.createdAt)
  return created ? Math.max(0, props.now - created.getTime()) : null
})
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
  background: #0d0d0e;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.card:hover .media {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 0 2px rgba(168, 199, 250, 0.35);
}
.media img {
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
.glow {
  position: absolute;
  inset: -40%;
  background: radial-gradient(circle at 35% 45%, rgba(143, 107, 232, 0.35), transparent 45%),
    radial-gradient(circle at 65% 55%, rgba(209, 107, 165, 0.28), transparent 45%);
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
  background: #c58af9;
  animation: pulse 1.4s ease-in-out infinite;
}
.ph-time {
  font-size: 28px;
  font-weight: 400;
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
  opacity: 0;
  transition: opacity 0.15s;
}
.card:hover .overlay-bottom {
  opacity: 1;
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
.tag.image {
  color: #d7b8fb;
}
.use-btn {
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  background: var(--accent-grad);
  color: #fff;
  box-shadow: 0 4px 14px rgba(110, 120, 240, 0.4);
}
.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 0;
  display: grid;
  place-items: center;
  background: rgba(19, 19, 20, 0.72);
  backdrop-filter: blur(8px);
  color: var(--text-2);
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, color 0.15s;
}
.card:hover .delete-btn,
.delete-btn:focus-visible {
  opacity: 1;
}
.delete-btn:hover {
  background: rgba(242, 184, 181, 0.9);
  color: #601410;
}
@media (hover: none) {
  .delete-btn,
  .overlay-bottom {
    opacity: 1;
  }
}
.info {
  padding: 12px 4px 0;
}
.prompt {
  margin: 0 0 6px;
  font-size: 14px;
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

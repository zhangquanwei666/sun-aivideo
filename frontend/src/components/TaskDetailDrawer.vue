<template>
  <transition name="drawer">
    <div v-if="task" class="backdrop" @click.self="$emit('close')">
      <aside class="drawer" role="dialog" aria-label="任务详情">
        <header class="head">
          <div>
            <div class="title">任务 #{{ task.id }}</div>
            <div class="sub">{{ task.modelName }} · {{ task.createdAt }}</div>
          </div>
          <button class="btn btn-ghost btn-icon" title="关闭" @click="$emit('close')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 6l12 12M18 6 6 18" /></svg>
          </button>
        </header>

        <div class="scroll">
          <div class="player">
            <video v-if="task.status === 'SUCCEEDED' && task.videoUrl" :src="task.videoUrl" controls autoplay loop playsinline />
            <div v-else class="player-ph">
              <span class="badge" :class="tone"><span class="dot" />{{ task.statusText }}</span>
              <p v-if="task.errorMessage" class="err">{{ task.errorMessage }}</p>
            </div>
          </div>

          <div class="actions">
            <button class="btn" @click="$emit('reuse', task)">复用参数</button>
            <a v-if="task.videoUrl" class="btn" :href="task.videoUrl" :download="`task-${task.id}.mp4`" target="_blank">下载视频</a>
            <span v-if="task.mock" class="badge warning">Mock 结果，未产生真实费用</span>
          </div>

          <section class="section">
            <h4>费用</h4>
            <div class="cost-grid num">
              <div class="row">
                <span>视频生成</span>
                <span class="formula">{{ task.cost.unitPrice != null ? `¥${Number(task.cost.unitPrice).toFixed(2)}/秒 × ${task.cost.billedDuration}s` : '—' }}</span>
                <span>{{ money(task.cost.videoCost) }}</span>
              </div>
              <div class="row">
                <span>千问润色</span>
                <span class="formula">{{ task.enhancePrompt ? `${task.cost.llmInputTokens} + ${task.cost.llmOutputTokens} tokens` : '未使用' }}</span>
                <span>{{ money(task.cost.llmCost) }}</span>
              </div>
              <div class="row total">
                <span>{{ task.finished ? '合计' : '预计' }}</span>
                <span />
                <span>{{ task.finished ? money(task.cost.totalCost) : money(task.cost.estimatedCost) }}</span>
              </div>
            </div>
          </section>

          <section class="section">
            <h4>耗时 <span class="total-time num">{{ elapsed(task.timing.totalElapsedMs) }}</span></h4>
            <div class="timeline">
              <div
                v-for="seg in segments"
                :key="seg.key"
                class="seg"
                :class="seg.key"
                :style="{ flexGrow: seg.ms }"
                :title="`${seg.label} ${elapsed(seg.ms)}`"
              />
            </div>
            <div class="legend num">
              <span v-for="seg in segments" :key="seg.key"><i :class="seg.key" />{{ seg.label }} {{ elapsed(seg.ms) }}</span>
            </div>
          </section>

          <section class="section">
            <h4>提示词</h4>
            <p class="text">{{ task.prompt || '（无）' }}</p>
            <template v-if="task.finalPrompt && task.finalPrompt !== task.prompt">
              <h5>润色后（实际提交）</h5>
              <p class="text">{{ task.finalPrompt }}</p>
            </template>
            <template v-if="task.negativePrompt">
              <h5>反向提示词</h5>
              <p class="text">{{ task.negativePrompt }}</p>
            </template>
          </section>

          <section class="section">
            <h4>参数</h4>
            <dl class="params">
              <dt>模型</dt><dd>{{ task.modelCode }}</dd>
              <dt>方式</dt><dd>{{ task.mode === 'I2V' ? '图生视频' : '文生视频' }}</dd>
              <dt>分辨率</dt><dd>{{ task.resolution }}</dd>
              <template v-if="task.ratio"><dt>画幅</dt><dd>{{ task.ratio }}</dd></template>
              <dt>时长</dt><dd>{{ task.duration }} 秒</dd>
              <dt>智能改写</dt><dd>{{ task.promptExtend ? '开' : '关' }}</dd>
              <template v-if="task.seed != null"><dt>Seed</dt><dd>{{ task.seed }}</dd></template>
              <dt>厂商</dt><dd>{{ task.provider }}</dd>
              <template v-if="task.finishedAt"><dt>完成时间</dt><dd>{{ task.finishedAt }}</dd></template>
            </dl>
            <img v-if="task.imageUrl" class="first-frame" :src="task.imageUrl" alt="首帧图片" />
          </section>
        </div>
      </aside>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import { elapsed, money, STATUS_TONE } from '@/utils/format'

const props = defineProps({ task: { type: Object, default: null } })
defineEmits(['close', 'reuse'])

const tone = computed(() => (props.task ? STATUS_TONE[props.task.status] || 'muted' : 'muted'))

/** 总耗时拆分：润色 / 排队 / 生成 / 其他（提交、轮询间隔等） */
const segments = computed(() => {
  const t = props.task?.timing
  if (!t) return []
  const list = [
    { key: 'enhance', label: '润色', ms: t.enhanceMs || 0 },
    { key: 'queue', label: '排队', ms: t.queueMs || 0 },
    { key: 'generate', label: '生成', ms: t.generateMs || 0 }
  ]
  const known = list.reduce((s, x) => s + x.ms, 0)
  const other = (t.totalElapsedMs || 0) - known
  if (other > 0) list.push({ key: 'other', label: '其他', ms: other })
  return list.filter((x) => x.ms > 0)
})
</script>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 50;
  display: flex;
  justify-content: flex-end;
}
.drawer {
  width: min(520px, 100vw);
  height: 100%;
  background: var(--surface);
  border-left: 1px solid var(--border-strong);
  display: flex;
  flex-direction: column;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px 20px;
  border-bottom: 1px solid var(--border);
}
.title {
  font-weight: 600;
}
.sub {
  font-size: 12px;
  color: var(--muted);
}
.scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 32px;
}
.player {
  border-radius: var(--radius);
  overflow: hidden;
  background: #000;
  aspect-ratio: 16 / 9;
}
.player video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.player-ph {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: var(--surface-2);
  padding: 16px;
}
.err {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  text-align: center;
  word-break: break-all;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0 4px;
  flex-wrap: wrap;
}
.section {
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
}
.section:last-child {
  border-bottom: 0;
}
h4 {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
}
h5 {
  margin: 12px 0 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--muted);
}
.total-time {
  font-size: 13px;
  color: var(--text);
}
.cost-grid .row {
  display: grid;
  grid-template-columns: 80px 1fr auto;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}
.formula {
  color: var(--muted);
}
.row.total {
  border-top: 1px solid var(--border);
  margin-top: 4px;
  padding-top: 10px;
  font-weight: 600;
}
.timeline {
  display: flex;
  gap: 3px;
  height: 10px;
  border-radius: 6px;
  overflow: hidden;
  background: var(--surface-3);
}
.seg {
  flex-basis: 0;
  min-width: 4px;
}
.enhance { background: #c9a0ff; }
.queue { background: #f5a524; }
.generate { background: #7aa2ff; }
.other { background: #55555c; }
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-2);
}
.legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin-right: 6px;
}
.text {
  margin: 0;
  font-size: 13px;
  color: var(--text-2);
  white-space: pre-wrap;
  word-break: break-word;
}
.params {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 6px 12px;
  margin: 0;
  font-size: 13px;
}
.params dt {
  color: var(--muted);
}
.params dd {
  margin: 0;
  word-break: break-all;
}
.first-frame {
  margin-top: 12px;
  max-width: 160px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.2s;
}
.drawer-enter-active .drawer,
.drawer-leave-active .drawer {
  transition: transform 0.22s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}
.drawer-enter-from .drawer,
.drawer-leave-to .drawer {
  transform: translateX(24px);
}
</style>

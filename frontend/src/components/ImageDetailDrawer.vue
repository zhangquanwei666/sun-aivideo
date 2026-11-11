<template>
  <transition name="drawer">
    <div v-if="task" class="backdrop" @click.self="$emit('close')">
      <aside class="drawer" role="dialog" aria-label="图片详情">
        <header class="head">
          <div>
            <div class="title">图片 #{{ task.id }}</div>
            <div class="sub">{{ task.modelName }} · {{ task.createdAt }}</div>
          </div>
          <button class="btn btn-ghost btn-icon" title="关闭" @click="$emit('close')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 6l12 12M18 6 6 18" /></svg>
          </button>
        </header>

        <div class="scroll">
          <div class="player image">
            <img v-if="task.status === 'SUCCEEDED' && task.imageUrls?.length" :src="task.imageUrls[0]" :alt="task.prompt" />
            <div v-else class="player-ph">
              <span class="badge" :class="tone"><span class="dot" />{{ task.statusText }}</span>
              <p v-if="task.errorMessage" class="err">{{ task.errorMessage }}</p>
            </div>
          </div>

          <div class="actions">
            <button v-if="task.status === 'SUCCEEDED'" class="btn btn-primary" @click="$emit('use-as-frame', task)">用作首帧生成视频</button>
            <button class="btn" @click="$emit('reuse', task)">复用参数</button>
            <a v-if="task.imageUrls?.length" class="btn" :href="task.imageUrls[0]" :download="`image-${task.id}.png`" target="_blank">下载图片</a>
            <span v-if="task.mock" class="badge warning">Mock 结果，未产生真实费用</span>
            <button
              v-if="task.finished"
              class="btn btn-ghost delete"
              title="删除后从列表中隐藏，费用统计不受影响"
              @click="$emit('delete', task)"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" /></svg>
              删除
            </button>
          </div>

          <section class="section">
            <h4>费用</h4>
            <div class="cost-grid num">
              <div class="row">
                <span>图片生成</span>
                <span class="formula">{{ unitFormula }}</span>
                <span>{{ money(task.cost.imageCost) }}</span>
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
              <div v-for="seg in segments" :key="seg.key" class="seg" :class="seg.key" :style="{ flexGrow: seg.ms }" :title="`${seg.label} ${elapsed(seg.ms)}`" />
            </div>
            <div class="legend num">
              <span v-for="seg in segments" :key="seg.key"><i :class="seg.key" />{{ seg.label }} {{ elapsed(seg.ms) }}</span>
            </div>
          </section>

          <section class="section">
            <h4>提示词</h4>
            <p class="text">{{ task.prompt }}</p>
            <template v-if="task.finalPrompt && task.finalPrompt !== task.prompt">
              <h5>润色后（实际提交）</h5>
              <p class="text">{{ task.finalPrompt }}</p>
            </template>
          </section>

          <section class="section">
            <h4>参数</h4>
            <dl class="params">
              <dt>模型</dt><dd>{{ task.modelCode }}</dd>
              <dt>厂商</dt><dd>{{ task.providerName || task.provider }}</dd>
              <dt>清晰度</dt><dd>{{ task.imageSize }}</dd>
              <dt>画幅</dt><dd>{{ task.aspectRatio }}</dd>
              <dt>张数</dt><dd>{{ task.imageCount }}</dd>
              <template v-if="task.finishedAt"><dt>完成时间</dt><dd>{{ task.finishedAt }}</dd></template>
            </dl>
            <template v-if="task.referenceImages?.length">
              <h5>参考图</h5>
              <div class="refs">
                <img v-for="src in task.referenceImages" :key="src" :src="src" alt="参考图" />
              </div>
            </template>
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
defineEmits(['close', 'reuse', 'delete', 'use-as-frame'])

const tone = computed(() => (props.task ? STATUS_TONE[props.task.status] || 'muted' : 'muted'))

/** 单价说明：美元计价的显示原价和折算后的人民币 */
const unitFormula = computed(() => {
  const c = props.task?.cost
  if (!c || c.unitPrice == null) return '—'
  const count = props.task.imageCount || 1
  if (c.currency === 'USD') {
    return `$${Number(c.unitPrice).toFixed(3)}/张（≈¥${Number(c.unitPriceCny ?? 0).toFixed(2)}）× ${count}`
  }
  return `¥${Number(c.unitPrice).toFixed(2)}/张 × ${count}`
})

/** 总耗时拆分：润色 / 生成 / 其他（排队、保存图片等） */
const segments = computed(() => {
  const t = props.task?.timing
  if (!t) return []
  const list = [
    { key: 'enhance', label: '润色', ms: t.enhanceMs || 0 },
    { key: 'generate', label: '生成', ms: t.generateMs || 0 }
  ]
  const other = (t.totalElapsedMs || 0) - list.reduce((s, x) => s + x.ms, 0)
  if (other > 0) list.push({ key: 'other', label: '其他', ms: other })
  return list.filter((x) => x.ms > 0)
})
</script>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(2px);
  z-index: 50;
  display: flex;
  justify-content: flex-end;
  padding: 12px;
}
.drawer {
  width: min(540px, 100%);
  height: 100%;
  background: var(--surface-2);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-pop);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 14px 10px 24px;
}
.title {
  font-size: 20px;
  font-weight: 400;
}
.sub {
  font-size: 12px;
  color: var(--muted);
}
.scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px 20px 28px;
}
.player {
  border-radius: var(--radius-lg);
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
  background: var(--surface-3);
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
  margin: 14px 0 6px;
  flex-wrap: wrap;
}
.section {
  margin-top: 12px;
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  background: var(--surface-3);
}
h4 {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 500;
}
h5 {
  margin: 14px 0 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--muted);
}
.total-time {
  font-size: 15px;
  color: var(--text);
}
.cost-grid .row {
  display: grid;
  grid-template-columns: 80px 1fr auto;
  gap: 8px;
  padding: 6px 0;
  font-size: 14px;
}
.formula {
  color: var(--muted);
}
.row.total {
  border-top: 1px solid var(--border-strong);
  margin-top: 6px;
  padding-top: 12px;
  font-weight: 500;
}
.row.total span:last-child {
  font-size: 18px;
}
.timeline {
  display: flex;
  gap: 3px;
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: var(--surface-4);
}
.seg {
  flex-basis: 0;
  min-width: 4px;
}
.enhance { background: #c58af9; }
.queue { background: #fdd663; }
.generate { background: #a8c7fa; }
.other { background: #5f6368; }
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-2);
}
.legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.text {
  margin: 0;
  font-size: 14px;
  color: var(--text-2);
  white-space: pre-wrap;
  word-break: break-word;
}
.params {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 8px 12px;
  margin: 0;
  font-size: 14px;
}
.params dt {
  color: var(--muted);
}
.params dd {
  margin: 0;
  word-break: break-all;
}
.first-frame {
  margin-top: 14px;
  max-width: 160px;
  border-radius: var(--radius);
}
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.2s;
}
.drawer-enter-active .drawer,
.drawer-leave-active .drawer {
  transition: transform 0.25s cubic-bezier(0.2, 0, 0, 1);
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}
.drawer-enter-from .drawer,
.drawer-leave-to .drawer {
  transform: translateX(32px);
}
@media (max-width: 720px) {
  .backdrop {
    padding: 0;
  }
  .drawer {
    border-radius: 0;
  }
}
.player.image {
  aspect-ratio: auto;
  min-height: 200px;
  max-height: 60vh;
  display: grid;
  place-items: center;
}
.player.image img {
  max-width: 100%;
  max-height: 60vh;
  display: block;
}
.actions .delete {
  margin-left: auto;
  color: var(--danger);
}
.actions .delete:hover {
  background: rgba(242, 184, 181, 0.12);
  color: var(--danger);
}
.refs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.refs img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}
</style>

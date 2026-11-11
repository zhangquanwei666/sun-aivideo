<template>
  <form class="composer" :class="{ focused }" @submit.prevent="submit">
    <div class="input-row">
      <div
        v-if="form.mode === 'I2V'"
        class="image-slot"
        :class="{ dragging, filled: !!form.imagePreview }"
        :title="form.imagePreview ? '' : '上传首帧图片（JPG / PNG / WEBP / BMP，10MB 以内）'"
        @click="pickImage"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <template v-if="form.imagePreview">
          <img :src="form.imagePreview" alt="首帧图片" />
          <button type="button" class="remove" title="移除图片" @click.stop="clearImage">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M6 6l12 12M18 6 6 18" /></svg>
          </button>
        </template>
        <template v-else>
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 5v14M5 12h14" /></svg>
          <span>首帧</span>
        </template>
        <div v-if="uploading" class="uploading">上传中…</div>
        <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/webp,image/bmp" hidden @change="onFile" />
      </div>

      <textarea
        v-model="form.prompt"
        class="prompt"
        rows="2"
        maxlength="5000"
        :placeholder="form.mode === 'T2V' ? '描述你想生成的视频：主体、动作、场景、光线、镜头…' : '描述画面要怎么动：人物动作、镜头运动、环境变化…（可选）'"
        @focus="focused = true"
        @blur="focused = false"
        @keydown.ctrl.enter.prevent="submit"
        @keydown.meta.enter.prevent="submit"
      />
    </div>

    <div class="bar">
      <div class="controls">
        <PillSelect v-model="form.provider" :options="providerOptions" title="厂商" placeholder="暂无可用厂商">
          <template #icon>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M7 18a4.5 4.5 0 0 1-.4-9 6 6 0 0 1 11.6 1.6A3.8 3.8 0 0 1 17.5 18z" /></svg>
          </template>
        </PillSelect>
        <PillSelect v-model="form.mode" :options="modeOptions" title="生成方式">
          <template #icon>
            <svg v-if="form.mode === 'T2V'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 6h16M4 12h10M4 18h7" /></svg>
            <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="4" width="18" height="16" rx="3" /><circle cx="9" cy="10" r="2" /><path d="m21 16-5-5-8 9" /></svg>
          </template>
        </PillSelect>
        <PillSelect v-model="form.modelCode" :options="modelOptions" title="模型">
          <template #icon>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3 4 7.5v9L12 21l8-4.5v-9z" /><path d="m4 7.5 8 4.5 8-4.5M12 12v9" /></svg>
          </template>
        </PillSelect>
        <PillSelect v-model="form.resolution" :options="resolutionOptions" title="分辨率" />
        <PillSelect v-if="form.mode === 'T2V' && ratioOptions.length" v-model="form.ratio" :options="ratioOptions" title="画幅" />
        <PillSelect v-model="form.duration" :options="durationOptions" title="时长" />

        <button
          type="button"
          class="chip"
          :class="{ on: form.enhancePrompt }"
          :disabled="!status.llmAvailable"
          :title="status.llmAvailable ? '提交前用千问扩写提示词（按 tokens 计费）' : '配置 DASHSCOPE_API_KEY 后可用'"
          @click="form.enhancePrompt = !form.enhancePrompt"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 2l1.8 5.2L19 9l-5.2 1.8L12 16l-1.8-5.2L5 9l5.2-1.8zM19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9z" /></svg>
          千问润色
        </button>

        <div ref="settingsRoot" class="settings">
          <button type="button" class="chip icon" :class="{ on: settingsOpen }" title="高级设置" @click="settingsOpen = !settingsOpen">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 7h10M18 7h2M4 17h4M12 17h8" /><circle cx="16" cy="7" r="2" /><circle cx="10" cy="17" r="2" /></svg>
          </button>
          <div v-if="settingsOpen" class="settings-pop">
            <div class="pop-title">高级设置</div>
            <label class="field-label">反向提示词</label>
            <textarea v-model="form.negativePrompt" class="input" rows="2" maxlength="500" placeholder="不希望出现的内容，如：模糊、变形、文字" />
            <label class="field-label">随机种子（可选）</label>
            <input v-model.number="form.seed" class="input" type="number" min="0" max="2147483647" placeholder="留空则随机" />
            <div class="toggle-row" @click="form.promptExtend = !form.promptExtend">
              <span>百炼智能改写<small>模型侧的 prompt_extend</small></span>
              <span class="switch" :class="{ on: form.promptExtend }" />
            </div>
            <div class="toggle-row" @click="form.watermark = !form.watermark">
              <span>添加「AI生成」水印</span>
              <span class="switch" :class="{ on: form.watermark }" />
            </div>
            <div v-if="currentModel?.supportsAudio" class="toggle-row" @click="form.audio = !form.audio">
              <span>生成音频</span>
              <span class="switch" :class="{ on: form.audio }" />
            </div>
          </div>
        </div>
      </div>

      <div class="actions">
        <span class="estimate num" :title="currentModel?.priceNote || ''">
          预计 <strong>{{ money(estimate) }}</strong>
        </span>
        <button type="submit" class="send" :disabled="!canSubmit" title="生成（Ctrl + Enter）" aria-label="生成">
          <span v-if="submitting" class="spinner" />
          <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M5 12l7-7 7 7" /></svg>
        </button>
      </div>
    </div>
  </form>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import PillSelect from './PillSelect.vue'
import { uploadImage } from '@/api'
import { money } from '@/utils/format'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  /** 厂商列表：[{ code, name, available, mockFallback, modelCount }] */
  providers: { type: Array, default: () => [] },
  models: { type: Array, default: () => [] },
  status: { type: Object, default: () => ({}) },
  submitting: Boolean
})
const emit = defineEmits(['submit'])
const toast = useToast()

const modes = [
  { value: 'T2V', label: '文生视频', hint: '输入文字描述生成视频' },
  { value: 'I2V', label: '图生视频', hint: '上传首帧图片，让画面动起来' }
]

const form = reactive({
  provider: '',
  mode: 'T2V',
  modelCode: '',
  prompt: '',
  negativePrompt: '',
  imageUrl: '',
  imagePreview: '',
  resolution: '720P',
  ratio: '16:9',
  duration: 5,
  enhancePrompt: false,
  promptExtend: true,
  watermark: false,
  audio: true,
  seed: null
})
const focused = ref(false)

const providerOptions = computed(() =>
  props.providers.map((p) => ({
    value: p.code,
    label: p.name,
    hint: `${p.modelCount} 个模型` + (p.available ? '' : p.mockFallback ? ' · 未配置 Key，将用 Mock 模拟' : ' · 未配置 Key')
  }))
)
/** 所选厂商下的模型 */
const providerModels = computed(() => props.models.filter((m) => m.provider === form.provider))
/** 只列出所选厂商有模型的生成方式 */
const modeOptions = computed(() => modes.filter((m) => providerModels.value.some((x) => x.mode === m.value)))
const modeModels = computed(() => providerModels.value.filter((m) => m.mode === form.mode))
const currentModel = computed(() => props.models.find((m) => m.modelCode === form.modelCode))

const modelOptions = computed(() =>
  modeModels.value.map((m) => ({
    value: m.modelCode,
    label: m.displayName,
    hint: Object.entries(m.prices).map(([r, p]) => `${r} ¥${Number(p).toFixed(2)}/秒`).join(' · ')
  }))
)
const resolutionOptions = computed(() =>
  (currentModel.value?.resolutions || []).map((r) => ({
    value: r,
    label: r,
    hint: currentModel.value.prices[r] != null ? `¥${Number(currentModel.value.prices[r]).toFixed(2)} / 秒` : '未配置单价'
  }))
)
const ratioOptions = computed(() => (currentModel.value?.ratios || []).map((r) => ({ value: r, label: r })))
const durationOptions = computed(() => {
  const m = currentModel.value
  if (!m) return []
  const list = []
  for (let s = m.minDuration; s <= m.maxDuration; s++) list.push({ value: s, label: `${s} 秒` })
  return list
})

const estimate = computed(() => {
  const price = currentModel.value?.prices?.[form.resolution]
  return price == null ? null : Number(price) * form.duration
})

const canSubmit = computed(() => {
  if (props.submitting || uploading.value || !currentModel.value) return false
  if (form.mode === 'T2V') return form.prompt.trim().length > 0
  return !!form.imageUrl
})

// 厂商列表加载后默认选第一个
watch(
  () => props.providers,
  (list) => {
    if (!list.some((p) => p.code === form.provider)) form.provider = list[0]?.code || ''
  },
  { immediate: true }
)
// 切换厂商 / 模式时：模式不在该厂商支持的范围内就换成第一个，模型不在列表里就换成第一个
watch(
  () => [form.provider, form.mode, props.models],
  () => {
    if (modeOptions.value.length && !modeOptions.value.some((m) => m.value === form.mode)) {
      form.mode = modeOptions.value[0].value
    }
    if (!modeModels.value.some((m) => m.modelCode === form.modelCode)) {
      form.modelCode = modeModels.value[0]?.modelCode || ''
    }
  },
  { immediate: true }
)
watch(currentModel, (m) => {
  if (!m) return
  if (!m.resolutions.includes(form.resolution)) {
    form.resolution = m.resolutions.includes('720P') ? '720P' : m.resolutions[0]
  }
  if (m.ratios.length && !m.ratios.includes(form.ratio)) form.ratio = m.ratios[0]
  if (form.duration < m.minDuration || form.duration > m.maxDuration) form.duration = m.defaultDuration
})

// ---------- 首帧图片 ----------
const fileInput = ref(null)
const uploading = ref(false)
const dragging = ref(false)

function pickImage() {
  if (!form.imagePreview) fileInput.value?.click()
}
function onDrop(e) {
  dragging.value = false
  const file = e.dataTransfer.files?.[0]
  if (file) handleFile(file)
}
function onFile(e) {
  const file = e.target.files?.[0]
  if (file) handleFile(file)
  e.target.value = ''
}
async function handleFile(file) {
  if (!/^image\/(png|jpe?g|webp|bmp)$/.test(file.type)) {
    toast.error('仅支持 JPG / PNG / WEBP / BMP 图片')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.error('图片不能超过 10MB')
    return
  }
  form.imagePreview = URL.createObjectURL(file)
  uploading.value = true
  try {
    const res = await uploadImage(file)
    form.imageUrl = res.url
  } catch (e) {
    toast.error('上传失败：' + e.message)
    clearImage()
  } finally {
    uploading.value = false
  }
}
function clearImage() {
  if (form.imagePreview) URL.revokeObjectURL(form.imagePreview)
  form.imagePreview = ''
  form.imageUrl = ''
}

// ---------- 高级设置弹层 ----------
const settingsOpen = ref(false)
const settingsRoot = ref(null)
function onDocClick(e) {
  if (settingsRoot.value && !settingsRoot.value.contains(e.target)) settingsOpen.value = false
}
onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))

function submit() {
  if (!canSubmit.value) return
  const m = currentModel.value
  emit('submit', {
    modelCode: form.modelCode,
    prompt: form.prompt.trim() || null,
    negativePrompt: form.negativePrompt.trim() || null,
    imageUrl: form.mode === 'I2V' ? form.imageUrl : null,
    resolution: form.resolution,
    ratio: form.mode === 'T2V' ? form.ratio : null,
    duration: form.duration,
    enhancePrompt: form.enhancePrompt,
    promptExtend: form.promptExtend,
    watermark: form.watermark,
    audio: m.supportsAudio ? form.audio : null,
    seed: form.seed === '' || form.seed === null ? null : Number(form.seed)
  })
}

/** 用历史任务的参数填充（"复用"） */
function reuse(task) {
  const model = props.models.find((m) => m.modelCode === task.modelCode)
  if (model) form.provider = model.provider
  form.mode = task.mode
  form.modelCode = task.modelCode
  form.prompt = task.prompt || ''
  form.negativePrompt = task.negativePrompt || ''
  form.resolution = task.resolution
  if (task.ratio) form.ratio = task.ratio
  form.duration = task.duration
  if (task.mode === 'I2V' && task.imageUrl) {
    form.imageUrl = task.imageUrl
    form.imagePreview = task.imageUrl
  }
}

function resetPrompt() {
  form.prompt = ''
}

defineExpose({ reuse, resetPrompt })
</script>

<style scoped>
.composer {
  position: relative;
  background: var(--surface-2);
  border-radius: var(--radius-xl);
  padding: 16px 16px 12px 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45), inset 0 0 0 1px var(--border);
  transition: box-shadow 0.2s;
}
/* 聚焦时显示一圈蓝紫渐变描边 */
.composer::before {
  content: '';
  position: absolute;
  inset: 0;
  padding: 1px;
  border-radius: inherit;
  background: var(--accent-grad);
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}
.composer.focused::before {
  opacity: 1;
}
.composer.focused {
  box-shadow: 0 8px 40px rgba(76, 141, 246, 0.18), 0 8px 32px rgba(0, 0, 0, 0.45);
}

.input-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.image-slot {
  position: relative;
  flex: none;
  width: 72px;
  height: 72px;
  border-radius: var(--radius);
  background: var(--surface-3);
  color: var(--text-2);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  cursor: pointer;
  overflow: hidden;
  outline: 1px dashed var(--border-strong);
  outline-offset: -1px;
  transition: background 0.15s, outline-color 0.15s;
}
.image-slot:hover,
.image-slot.dragging {
  background: var(--surface-4);
  outline-color: var(--primary);
  color: var(--text);
}
.image-slot.filled {
  outline: none;
  cursor: default;
}
.image-slot img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 0;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
}
.uploading {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.6);
  color: var(--text);
  font-size: 12px;
}
.prompt {
  flex: 1;
  min-height: 52px;
  max-height: 220px;
  border: 0;
  outline: none;
  resize: none;
  background: transparent;
  font-size: 16px;
  line-height: 1.6;
  padding: 4px 0;
  field-sizing: content;
}
.prompt::placeholder {
  color: var(--muted);
}

.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 0;
  background: transparent;
  color: var(--text-2);
  font-size: 13px;
  font-weight: 500;
  transition: background 0.15s, color 0.15s;
}
.chip.icon {
  width: 36px;
  padding: 0;
  justify-content: center;
}
.chip:hover:not(:disabled) {
  background: var(--surface-3);
  color: var(--text);
}
.chip.on {
  background: var(--accent-soft);
  color: var(--text);
}
.chip.on svg {
  color: #b39af0;
}
.chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.settings {
  position: relative;
}
.settings-pop {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 0;
  width: 320px;
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--surface-3);
  box-shadow: var(--shadow-pop);
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pop-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 10px;
}
.settings-pop .input {
  margin-bottom: 12px;
  background: var(--surface-2);
}
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
  font-size: 14px;
}
.toggle-row small {
  display: block;
  color: var(--muted);
  font-size: 12px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
}
.estimate {
  font-size: 13px;
  color: var(--muted);
}
.estimate strong {
  color: var(--text);
  font-weight: 500;
}
.send {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 0;
  display: grid;
  place-items: center;
  background: var(--accent-grad);
  color: #fff;
  box-shadow: 0 4px 16px rgba(110, 120, 240, 0.35);
  transition: transform 0.15s, box-shadow 0.15s, opacity 0.15s;
}
.send:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 22px rgba(110, 120, 240, 0.5);
}
.send:disabled {
  background: var(--surface-4);
  color: var(--faint);
  box-shadow: none;
  cursor: not-allowed;
}
.spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 720px) {
  .composer {
    padding: 12px 12px 10px 14px;
    border-radius: var(--radius-lg);
  }
  .image-slot {
    width: 60px;
    height: 60px;
  }
  .prompt {
    font-size: 15px;
  }
  .estimate {
    display: none;
  }
}
</style>

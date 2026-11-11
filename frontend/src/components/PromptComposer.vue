<template>
  <form class="composer" @submit.prevent="submit">
    <div class="mode-tabs" role="tablist">
      <button
        v-for="m in modes"
        :key="m.value"
        type="button"
        role="tab"
        class="mode-tab"
        :class="{ active: form.mode === m.value }"
        :disabled="!models.some((x) => x.mode === m.value)"
        @click="form.mode = m.value"
      >
        {{ m.label }}
      </button>
    </div>

    <div class="body">
      <div
        v-if="form.mode === 'I2V'"
        class="image-slot"
        :class="{ dragging, filled: !!form.imagePreview }"
        @click="pickImage"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <template v-if="form.imagePreview">
          <img :src="form.imagePreview" alt="首帧图片" />
          <button type="button" class="remove" title="移除图片" @click.stop="clearImage">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M6 6l12 12M18 6 6 18" /></svg>
          </button>
        </template>
        <template v-else>
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="3" y="3" width="18" height="18" rx="3" /><circle cx="9" cy="9" r="2" /><path d="m21 15-5-5L5 21" /></svg>
          <span>首帧图片</span>
        </template>
        <div v-if="uploading" class="uploading">上传中…</div>
        <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/webp,image/bmp" hidden @change="onFile" />
      </div>

      <textarea
        v-model="form.prompt"
        class="prompt"
        rows="3"
        maxlength="5000"
        :placeholder="form.mode === 'T2V' ? '描述你想生成的画面：主体、动作、场景、光线、镜头…' : '描述画面要怎么动：人物动作、镜头运动、环境变化…（可选）'"
        @keydown.ctrl.enter.prevent="submit"
        @keydown.meta.enter.prevent="submit"
      />
    </div>

    <div class="bar">
      <div class="controls">
        <PillSelect v-model="form.modelCode" :options="modelOptions" title="模型">
          <template #icon>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3 4 7.5v9L12 21l8-4.5v-9z" /><path d="m4 7.5 8 4.5 8-4.5M12 12v9" /></svg>
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
          <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M12 2l1.8 5.2L19 9l-5.2 1.8L12 16l-1.8-5.2L5 9l5.2-1.8zM19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9z" /></svg>
          千问润色
        </button>

        <div ref="settingsRoot" class="settings">
          <button type="button" class="chip icon" :class="{ on: settingsOpen }" title="高级设置" @click="settingsOpen = !settingsOpen">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 7h10M18 7h2M4 17h4M12 17h8" /><circle cx="16" cy="7" r="2" /><circle cx="10" cy="17" r="2" /></svg>
          </button>
          <div v-if="settingsOpen" class="settings-pop">
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
        <button type="submit" class="btn btn-primary generate" :disabled="!canSubmit">
          <span v-if="submitting" class="spinner" />
          生成
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
  models: { type: Array, default: () => [] },
  status: { type: Object, default: () => ({}) },
  submitting: Boolean
})
const emit = defineEmits(['submit'])
const toast = useToast()

const modes = [
  { value: 'T2V', label: '文生视频' },
  { value: 'I2V', label: '图生视频' }
]

const form = reactive({
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

const modeModels = computed(() => props.models.filter((m) => m.mode === form.mode))
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

// 切换模式 / 模型时，把参数收敛到模型支持的范围
watch(
  () => [form.mode, props.models],
  () => {
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
  background: var(--surface);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}
.mode-tabs {
  display: flex;
  gap: 4px;
  padding: 10px 12px 0;
}
.mode-tab {
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 0;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
}
.mode-tab:hover:not(:disabled) {
  color: var(--text);
}
.mode-tab.active {
  background: var(--surface-3);
  color: var(--text);
}
.mode-tab:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.body {
  display: flex;
  gap: 12px;
  padding: 10px 16px 4px;
}
.image-slot {
  position: relative;
  flex: none;
  width: 92px;
  height: 92px;
  border-radius: var(--radius);
  border: 1px dashed var(--border-strong);
  background: var(--surface-2);
  color: var(--muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.15s, color 0.15s;
}
.image-slot:hover,
.image-slot.dragging {
  border-color: #6a6a72;
  color: var(--text);
}
.image-slot.filled {
  border-style: solid;
  cursor: default;
}
.image-slot img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 0;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
}
.uploading {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.6);
  color: var(--text);
}
.prompt {
  flex: 1;
  min-height: 72px;
  max-height: 220px;
  border: 0;
  outline: none;
  resize: vertical;
  background: transparent;
  font-size: 15px;
  line-height: 1.6;
  padding: 4px 0;
}
.prompt::placeholder {
  color: var(--faint);
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px 12px;
  flex-wrap: wrap;
}
.controls {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--text-2);
  font-size: 13px;
  transition: all 0.15s;
}
.chip.icon {
  width: 32px;
  padding: 0;
  justify-content: center;
}
.chip:hover:not(:disabled) {
  background: var(--surface-2);
  color: var(--text);
}
.chip.on {
  background: var(--surface-3);
  color: var(--text);
  border-color: #55555c;
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
  bottom: calc(100% + 8px);
  left: 0;
  width: 300px;
  padding: 14px;
  border-radius: var(--radius);
  background: var(--surface-2);
  box-shadow: var(--shadow-pop);
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.settings-pop .input {
  margin-bottom: 10px;
  background: var(--surface);
}
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  cursor: pointer;
  font-size: 13px;
  border-top: 1px solid var(--border);
}
.toggle-row small {
  display: block;
  color: var(--muted);
  font-size: 12px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}
.estimate {
  font-size: 13px;
  color: var(--muted);
}
.estimate strong {
  color: var(--text);
  font-weight: 600;
}
.generate {
  min-width: 88px;
  height: 36px;
}
.spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(0, 0, 0, 0.2);
  border-top-color: var(--primary-text);
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 720px) {
  .body {
    padding: 10px 12px 4px;
  }
  .image-slot {
    width: 72px;
    height: 72px;
  }
}
</style>

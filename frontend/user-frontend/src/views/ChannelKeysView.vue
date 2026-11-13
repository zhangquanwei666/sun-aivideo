<template>
  <div class="keys-page">
    <header>
      <h1 class="page-title">模型密钥</h1>
      <p class="page-sub">使用你自己的厂商账户生成图片和视频。费用由厂商从对应账户扣取。</p>
    </header>
    <p class="notice">密钥加密保存，仅显示末四位。保存表示完成配置，不代表已验证权限或余额。千问润色需要百炼密钥。</p>
    <p v-if="loading" role="status">加载中…</p>
    <div v-else class="channel-grid">
      <form v-for="channel in channels" :key="channel.channel" class="channel-card" @submit.prevent="save(channel)">
        <div class="card-heading">
          <h2>{{ channel.name }}</h2>
          <span :class="['state', { enabled: channel.enabled }]">{{ channel.enabled ? '已配置 · 已启用' : channel.configured ? '已停用' : '待配置' }}</span>
        </div>
        <p>{{ channel.channel === 'DASHSCOPE' ? '万相图片、万相视频和千问润色共用此密钥。' : '用于 Gemini 图片和视频；千问润色需另配百炼。' }}</p>
        <div v-if="channel.configured" class="saved">当前密钥 {{ channel.maskedKey }}<small>更新后新任务使用新密钥，已有任务继续使用原版本。</small></div>
        <label :for="`key-${channel.channel}`">{{ channel.configured ? '替换密钥' : 'API Key' }}</label>
        <input :id="`key-${channel.channel}`" v-model="drafts[channel.channel]" class="input" type="password"
          autocomplete="new-password" spellcheck="false" :disabled="busy || loadFailed" minlength="8" maxlength="2048"
          :placeholder="channel.configured ? '输入完整新密钥，留空不修改' : '输入此厂商的完整密钥'" />
        <div class="card-actions">
          <button class="btn primary" type="submit" :disabled="busy || loadFailed || !drafts[channel.channel]">保存密钥</button>
          <button v-if="channel.configured" class="btn" type="button" :disabled="busy || loadFailed" @click="toggle(channel)">{{ channel.enabled ? '停用' : '启用' }}</button>
          <button v-if="channel.configured" class="btn danger" type="button" :disabled="busy || loadFailed" @click="pendingDelete = channel">删除</button>
        </div>
      </form>
    </div>
    <button v-if="loadFailed" class="btn" @click="load">重新加载</button>
    <p class="footnote">停用只阻止新任务。存在未完成任务时不能删除密钥；在厂商侧撤销旧密钥仍可能导致已有任务失败。</p>
    <ConfirmDialog :open="!!pendingDelete" danger title="删除此厂商密钥？" confirm-text="删除密钥" :busy="busy"
      @confirm="remove" @cancel="pendingDelete = null">
      删除后需重新配置才能生成。密钥无法查看或恢复，请先确认已在其他安全位置保存。
    </ConfirmDialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { listChannelCredentials, saveChannelCredential, setChannelCredentialEnabled, deleteChannelCredential } from '@/api'
import { useToast } from '@/composables/useToast'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const channels = ref([])
const drafts = ref({})
const loading = ref(true)
const loadFailed = ref(false)
const busy = ref(false)
const pendingDelete = ref(null)
const toast = useToast()
async function load() {
  loading.value = true
  try { channels.value = (await listChannelCredentials()).channels; loadFailed.value = false }
  catch (e) { loadFailed.value = true; toast.error(e.message) }
  finally { loading.value = false }
}
async function change(action) {
  if (busy.value) return
  busy.value = true
  try {
    await action()
    toast.success('密钥配置已更新')
    window.dispatchEvent(new Event('ai-credentials-changed'))
    await load()
  } catch (e) { toast.error(e.message) }
  finally { busy.value = false }
}
function save(channel) {
  const key = drafts.value[channel.channel]
  if (!key || /\s/.test(key)) { toast.error('密钥不能包含空白字符'); return }
  change(async () => { await saveChannelCredential(channel.channel, key); drafts.value[channel.channel] = '' })
}
function toggle(channel) { change(() => setChannelCredentialEnabled(channel.channel, !channel.enabled)) }
function remove() {
  const channel = pendingDelete.value
  if (!channel) return
  change(async () => { await deleteChannelCredential(channel.channel); pendingDelete.value = null; drafts.value[channel.channel] = '' })
}
onMounted(load)
onBeforeUnmount(() => { drafts.value = {} })
</script>

<style scoped>
.keys-page { max-width: 1100px; margin: auto; padding: 32px 28px; overflow-y: auto; height: 100%; }
.notice { padding: 16px 20px; border: 1px solid #374366; border-radius: 12px; background: #1c2540; margin: 24px 0; color: #c8d2ed; line-height: 1.7; }
.channel-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 340px), 1fr)); gap: 20px; }
.channel-card { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 24px; }
.card-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
h2 { font-size: 19px; margin: 0; }
.channel-card p, .footnote { font-size: 13px; color: var(--muted, #a1a8bb); line-height: 1.7; }
.state { font-size: 12px; color: #b5bad0; }
.state.enabled { color: #7bd8ad; }
.saved { padding: 12px 0; font-variant-numeric: tabular-nums; }
.saved small { display: block; color: var(--muted, #a1a8bb); margin-top: 8px; font-size: 12px; line-height: 1.6; }
label { display: block; margin: 18px 0 8px; font-size: 13px; }
.input { width: 100%; }
.card-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 20px; }
.danger { color: #ff999f; }
.footnote { margin-top: 24px; }
</style>

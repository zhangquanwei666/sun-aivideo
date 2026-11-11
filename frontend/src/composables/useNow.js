import { ref, onMounted, onBeforeUnmount } from 'vue'

/** 每秒刷新的当前时间，用于显示进行中任务的已等待时长 */
export function useNow(interval = 1000) {
  const now = ref(Date.now())
  let timer
  onMounted(() => {
    timer = setInterval(() => (now.value = Date.now()), interval)
  })
  onBeforeUnmount(() => clearInterval(timer))
  return now
}

import { reactive } from 'vue'

const state = reactive({ items: [] })
let seq = 0

export function useToast() {
  function show(message, tone = 'info', timeout = 3200) {
    const id = ++seq
    state.items.push({ id, message, tone })
    setTimeout(() => {
      const i = state.items.findIndex((t) => t.id === id)
      if (i >= 0) state.items.splice(i, 1)
    }, timeout)
  }
  return {
    toasts: state.items,
    info: (m) => show(m, 'info'),
    success: (m) => show(m, 'success'),
    error: (m) => show(m, 'danger', 5000)
  }
}

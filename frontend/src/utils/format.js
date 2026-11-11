/** 金额：¥3.00；小于 1 分时显示 4 位小数 */
export function money(value) {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  if (n === 0) return '¥0.00'
  if (Math.abs(n) < 0.01) return '¥' + n.toFixed(4)
  return '¥' + n.toFixed(2)
}

/** 毫秒 → 14.2s / 1m 32s */
export function elapsed(ms) {
  if (ms === null || ms === undefined) return '—'
  const s = ms / 1000
  if (s < 60) return s.toFixed(1) + 's'
  const m = Math.floor(s / 60)
  const rest = Math.round(s % 60)
  if (m < 60) return `${m}m ${rest}s`
  return `${Math.floor(m / 60)}h ${m % 60}m`
}

export function parseTime(value) {
  if (!value) return null
  return new Date(String(value).replace(' ', 'T'))
}

/** 2026-11-11 11:15:52 → 11-11 11:15 */
export function shortTime(value) {
  const d = parseTime(value)
  if (!d || isNaN(d)) return '—'
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

export function seconds(value) {
  if (value === null || value === undefined) return '—'
  return `${value}s`
}

export const STATUS_TONE = {
  SUBMITTING: 'info',
  PENDING: 'info',
  RUNNING: 'info',
  SUCCEEDED: 'success',
  FAILED: 'danger',
  CANCELED: 'muted',
  TIMEOUT: 'danger'
}

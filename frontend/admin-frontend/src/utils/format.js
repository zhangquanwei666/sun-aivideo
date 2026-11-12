export const money = value => `¥${Number(value || 0).toFixed(4)}`
export const dateTime = value => value ? String(value).replace('T', ' ').slice(0, 19) : '—'
export const compact = value => new Intl.NumberFormat('zh-CN', { notation: 'compact', maximumFractionDigits: 1 }).format(Number(value || 0))
export const fileUrl = path => path ? `${import.meta.env.VITE_FILE_BASE_URL || ''}${path}` : ''

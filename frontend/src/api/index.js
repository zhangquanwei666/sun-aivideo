import http from './http'

export const getSystemStatus = () => http.get('/system/status')

/** 返回模型数组（后端响应为 ModelListVO：{ code, message, models }） */
export const listModels = () => http.get('/models').then((res) => res.models || [])

/** 创建视频生成任务 */
export const generateVideo = (data) => http.post('/videos', data)

export const getVideo = (id) => http.get(`/videos/${id}`)

export const pageVideos = (params) => http.get('/videos', { params })

export const getStats = () => http.get('/videos/stats')

export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return http.post('/files/images', form, { timeout: 60000 })
}

import http from './http'

export const getSystemStatus = () => http.get('/system/status')

// ---------- 视频 ----------

/** 可选的视频厂商与模型（后端响应为 ModelListVO：{ code, message, providers, models }） */
export const listModels = () =>
  http.get('/models').then((res) => ({ providers: res.providers || [], models: res.models || [] }))

/** 创建视频生成任务 */
export const generateVideo = (data) => http.post('/videos', data)

export const getVideo = (id) => http.get(`/videos/${id}`)

/** 删除视频（逻辑删除，只能删除已结束的任务） */
export const deleteVideo = (id) => http.delete(`/videos/${id}`)

export const pageVideos = (params) => http.get('/videos', { params })

export const getStats = () => http.get('/videos/stats')

// ---------- 文生图 ----------

/** 可选的文生图厂商与模型（ImageModelListVO：{ providers, models }） */
export const listImageModels = () =>
  http.get('/image-models').then((res) => ({ providers: res.providers || [], models: res.models || [] }))

/** 创建文生图任务 */
export const generateImage = (data) => http.post('/images', data, { timeout: 60000 })

export const getImage = (id) => http.get(`/images/${id}`)

/** 删除图片（逻辑删除，只能删除已结束的任务） */
export const deleteImage = (id) => http.delete(`/images/${id}`)

export const getImageStats = () => http.get('/images/stats')

// ---------- 生成记录（视频 + 图片） ----------

/** 分页查询生成记录，records 为 [{ type: 'VIDEO' | 'IMAGE', video, image }] */
export const pageFeed = (params) => http.get('/feed', { params })

// ---------- 文件 ----------

export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return http.post('/files/images', form, { timeout: 60000 })
}

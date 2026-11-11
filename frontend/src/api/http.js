import axios from 'axios'

const http = axios.create({
  // 各环境的接口地址见 frontend/.env.*
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 后端响应统一继承 BaseResponse：{ code, message, ...业务字段 }，code = 0 为成功
http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default http

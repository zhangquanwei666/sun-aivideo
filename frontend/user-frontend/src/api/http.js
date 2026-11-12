import axios from 'axios'

const http = axios.create({
  // 各环境的接口地址见 frontend/.env.*
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('aivideo-token')
  if (token) config.headers.satoken = token
  return config
})
function expired() {
  localStorage.removeItem('aivideo-token')
  if (location.pathname !== '/login') location.replace('/login')
}
// 后端响应统一继承 BaseResponse：{ code, message, ...业务字段 }，code = 0 为成功
http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 401 && !res.config.url?.startsWith('/auth/login')) expired()
      if (body.code === 0) return body
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    if (err.response?.status === 401) expired()
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default http

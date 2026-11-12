import axios from 'axios'

const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api/admin', timeout: 20000 })

http.interceptors.request.use(config => {
  const token = localStorage.getItem('admin-token')
  if (token) config.headers['admin-token'] = token
  return config
})

http.interceptors.response.use(response => {
  const body = response.data
  if (body && typeof body.code === 'number' && body.code !== 0) {
    if (body.code === 401 && !response.config.url?.includes('/auth/login')) {
      localStorage.removeItem('admin-token')
      location.replace('/login')
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return body
}, error => {
  if (error.response?.status === 401) {
    localStorage.removeItem('admin-token')
    location.replace('/login')
  }
  return Promise.reject(new Error(error.response?.data?.message || error.message || '网络连接失败'))
})

export default http

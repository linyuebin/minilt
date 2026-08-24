import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api/admin',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 后端统一返回 { code, msg, data }，HTTP 状态恒为 200，未登录时 code=401
http.interceptors.response.use(
  (res) => {
    const { code, msg, data } = res.data || {}
    if (code === 0) return data
    if (code === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_name')
      router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      return Promise.reject(new Error(msg || '请先登录'))
    }
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (err) => {
    ElMessage.error(err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default http

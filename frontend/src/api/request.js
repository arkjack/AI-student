/* ============================================================
   Axios 统一实例：baseURL /api，自动携带 JWT，统一错误提示
   后端约定：{ code, msg, data }，code=0 成功
   ============================================================ */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截：注入 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理业务码
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) return body.data
      if (body.code === 401) {
        ElMessage.warning(body.msg || '登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
        return Promise.reject(new Error(body.msg))
      }
      if (body.code === 403) {
        // 已登录但无权限：仅提示，不清除登录态（避免被误踢出登录）
        ElMessage.error(body.msg || '无权限访问')
        return Promise.reject(new Error(body.msg))
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return body
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      ElMessage.warning('请先登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    } else {
      ElMessage.error('网络异常，请稍后再试')
    }
    return Promise.reject(err)
  }
)

export default request

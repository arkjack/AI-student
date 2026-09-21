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

/* ------------------------------------------------------------
   登录失效提示去重
   一次页面渲染可能并发多个受保护请求，全部 401 时会各弹一条
   「请先登录」，在屏幕上堆成一摞。这里做 3 秒窗口去重：
   同一波失效只提示一次，用户看到的是干净的一条。
   ------------------------------------------------------------ */
let authNoticeAt = 0
function notifyAuthRequired(msg) {
  const now = Date.now()
  if (now - authNoticeAt < 3000) return
  authNoticeAt = now
  ElMessage.warning(msg)
}

/** 清除登录态（token + 用户信息），退出登录与 401 都走这里 */
export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

/**
 * 把业务码挂到 Error 上再抛出。
 * 调用方原先只能拿到一句 msg，无法区分「AI 未配置 1101」和「内容不合规 1201」，
 * 于是两种截然不同的情况走了同一段兜底逻辑。挂上 code 后页面才能分别处理。
 */
function withCode(err, code) {
  err.code = code
  return err
}

/** 跳登录页；已经在登录页就不再重复跳转 */
function toLogin() {
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

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
        notifyAuthRequired(body.msg || '登录已过期，请重新登录')
        clearAuth()
        toLogin()
        return Promise.reject(new Error(body.msg))
      }
      if (body.code === 403) {
        // 已登录但无权限：仅提示，不清除登录态（避免被误踢出登录）
        ElMessage.error(body.msg || '无权限访问')
        return Promise.reject(withCode(new Error(body.msg), body.code))
      }
      // AI 相关的这几类业务码交给 AI 实验室页面呈现：它们要配合「已用本地示例代替」
      // 「换个主题再试试」这类上下文才讲得清，这里不再弹通用错误框，否则会连弹两条。
      // 1101 未配置 / 1103 密钥无效 / 1104 余额不足 / 1105 上游限流 / 1201 输入不合规
      if (body.code === 1101 || body.code === 1103 || body.code === 1104
          || body.code === 1105 || body.code === 1201) {
        return Promise.reject(withCode(new Error(body.msg), body.code))
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(withCode(new Error(body.msg), body.code))
    }
    return body
  },
  (err) => {
    const status = err.response && err.response.status
    if (status === 401) {
      notifyAuthRequired('请先登录')
      clearAuth()
      toLogin()
    } else if (status === 403) {
      // HTTP 层 403：跨域被拒 / 被安全策略拦截，与业务码 403 区分开提示
      ElMessage.error('无权限访问或被安全策略拒绝')
    } else {
      ElMessage.error('网络异常，请稍后再试')
    }
    return Promise.reject(err)
  }
)

export default request

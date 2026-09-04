import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

// App 原生环境（Capacitor 打包的 Android/iOS）下无 Vite 代理，
// 直连线上后端；Web 环境继续走相对路径（开发走 Vite 代理 / 生产同源）
export const NATIVE_API_BASE = 'https://lingoplay.pocketbay.app/api'
const isNativeApp = typeof window !== 'undefined' && window.Capacitor && window.Capacitor.isNativePlatform()

const http = axios.create({
  baseURL: isNativeApp ? NATIVE_API_BASE : '/api',
  timeout: 30000
})

// 请求拦截：附加 JWT
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('lingolearn_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 401 统一处理：清空登录态并弹出全局登录弹窗（游客浏览模式，不强制跳登录页）
function handle401() {
  const store = useUserStore()
  const wasLoggedIn = !!store.token
  store.token = ''
  store.user = null
  localStorage.removeItem('lingolearn_token')
  localStorage.removeItem('lingolearn_user')
  if (!store.loginModalVisible) {
    ElMessage.warning(wasLoggedIn ? '登录状态已过期，请重新登录' : '该操作需要登录')
    store.requireLogin()
  }
}

// 响应拦截：统一错误处理
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== undefined && body.code !== 0) {
      // 后端 requireUserId 抛出的 BusinessException(401) 走 HTTP 200 + code=401
      if (body.code === 401) {
        handle401()
        return Promise.reject(new Error(body.message || '请先登录'))
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body ? body.data : response.data
  },
  (error) => {
    const status = error.response ? error.response.status : 0
    const bodyCode = error.response && error.response.data && error.response.data.code
    const msg = error.response && error.response.data && error.response.data.message
    if (status === 401 || bodyCode === 401) {
      handle401()
    } else {
      ElMessage.error(msg || '网络异常，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export default http

import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// App 原生环境（Capacitor 打包的 Android/iOS）下无 Vite 代理，
// 直连线上后端；Web 环境继续走相对路径（开发走 Vite 代理 / 生产同源）
const NATIVE_API_BASE = 'https://lingoplay.pocketbay.app/api'
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

// 响应拦截：统一错误处理
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== undefined && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body ? body.data : response.data
  },
  (error) => {
    const status = error.response ? error.response.status : 0
    const msg = error.response && error.response.data && error.response.data.message
    if (status === 401) {
      localStorage.removeItem('lingolearn_token')
      localStorage.removeItem('lingolearn_user')
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.warning('登录状态已过期，请重新登录')
        router.push('/login')
      }
    } else {
      ElMessage.error(msg || '网络异常，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export default http
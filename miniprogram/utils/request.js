// 统一请求封装：带 JWT、错误提示、401 弹窗引导登录（游客浏览模式）
const { BASE_URL } = require('./config')

// 防止多个请求同时 401 时重复弹窗
let loginPrompting = false

function clearAuth() {
  wx.removeStorageSync('token')
  wx.removeStorageSync('user')
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.user = null
  }
}

/** 需要登录：弹窗引导，确认后前往登录页（不强制，可继续浏览） */
function promptLogin() {
  if (loginPrompting) return
  loginPrompting = true
  wx.showModal({
    title: '需要登录',
    content: '登录后即可使用该功能，是否前往登录？',
    confirmText: '去登录',
    cancelText: '继续逛逛',
    success(res) {
      loginPrompting = false
      if (res.confirm) {
        wx.navigateTo({ url: '/pages/login/login' })
      }
    },
    fail() {
      loginPrompting = false
    }
  })
}

function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: Object.assign(
        { 'Content-Type': 'application/json' },
        token ? { Authorization: 'Bearer ' + token } : {}
      ),
      success(res) {
        const body = res.data
        if (res.statusCode === 401) {
          clearAuth()
          promptLogin()
          reject(new Error('未登录或登录已过期'))
          return
        }
        if (body && body.code !== undefined && body.code !== 0) {
          // 后端 requireUserId 抛出的 BusinessException(401) 走 HTTP 200 + code=401
          if (body.code === 401) {
            clearAuth()
            promptLogin()
            reject(new Error(body.message || '请先登录'))
            return
          }
          // silent 模式下不弹提示（如 VIP 拦截由页面自行引导），错误仍向上抛
          if (!options.silent) {
            wx.showToast({ title: body.message || '请求失败', icon: 'none' })
          }
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve(body ? body.data : null)
      },
      fail() {
        if (!options.silent) {
          wx.showToast({ title: '网络异常，请稍后再试', icon: 'none' })
        }
        reject(new Error('网络异常'))
      }
    })
  })
}

const get = (url, data, silent) => request({ url, method: 'GET', data, silent })
const post = (url, data, silent) => request({ url, method: 'POST', data, silent })
const put = (url, data, silent) => request({ url, method: 'PUT', data, silent })

module.exports = { request, get, post, put, BASE_URL }

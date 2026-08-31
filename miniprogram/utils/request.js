// 统一请求封装：带 JWT、错误提示、401 自动跳登录
const { BASE_URL } = require('./config')

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
          wx.removeStorageSync('token')
          wx.removeStorageSync('user')
          wx.reLaunch({ url: '/pages/login/login' })
          reject(new Error('未登录或登录已过期'))
          return
        }
        if (body && body.code !== undefined && body.code !== 0) {
          wx.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message || '请求失败'))
          return
        }
        resolve(body ? body.data : null)
      },
      fail() {
        wx.showToast({ title: '网络异常，请稍后再试', icon: 'none' })
        reject(new Error('网络异常'))
      }
    })
  })
}

const get = (url, data) => request({ url, method: 'GET', data })
const post = (url, data) => request({ url, method: 'POST', data })
const put = (url, data) => request({ url, method: 'PUT', data })

module.exports = { request, get, post, put, BASE_URL }
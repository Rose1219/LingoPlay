const { post } = require('../../utils/request')

Page({
  data: {
    account: '',
    password: '',
    wxLoading: false,
    pwdLoading: false,
    mode: 'wx'
  },

  switchMode() {
    this.setData({ mode: this.data.mode === 'wx' ? 'pwd' : 'wx' })
  },

  /** 微信一键登录：wx.login 拿 code，后端换 openid */
  wxLogin() {
    if (this.data.wxLoading) return
    this.setData({ wxLoading: true })
    wx.login({
      success: (res) => {
        if (!res.code) {
          wx.showToast({ title: '获取微信登录凭证失败', icon: 'none' })
          this.setData({ wxLoading: false })
          return
        }
        post('/auth/wx-login', { code: res.code })
          .then((data) => {
            this.saveAndEnter(data)
          })
          .catch(() => {
            this.setData({ wxLoading: false, mode: 'pwd' })
          })
      },
      fail: () => {
        wx.showToast({ title: '微信登录失败，请重试', icon: 'none' })
        this.setData({ wxLoading: false })
      }
    })
  },

  onAccountInput(e) {
    this.setData({ account: e.detail.value })
  },

  onPwdInput(e) {
    this.setData({ password: e.detail.value })
  },

  pwdLogin() {
    const { account, password } = this.data
    if (!account.trim() || !password.trim()) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    this.setData({ pwdLoading: true })
    post('/auth/login', { account: account.trim(), password })
      .then((data) => {
        this.saveAndEnter(data)
      })
      .catch(() => {
        this.setData({ pwdLoading: false })
      })
  },

  saveAndEnter(data) {
    wx.setStorageSync('token', data.token)
    wx.setStorageSync('user', data.user)
    getApp().globalData.user = data.user
    // 从游客引导弹窗进入时（页面栈有来源页），登录后返回原页并自动刷新；
    // 直接启动进入登录页时，回主页
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({ url: '/pages/index/index' })
    }
  }
})
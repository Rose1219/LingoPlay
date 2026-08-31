const { get, put } = require('../../utils/request')

Page({
  data: {
    user: null,
    nickname: '',
    languages: [],
    selected: []
  },

  onShow() {
    Promise.all([get('/languages'), get('/users/me')]).then(([languages, user]) => {
      this.setData({
        languages,
        user,
        nickname: user.nickname || '',
        selected: (user.preferredLanguages || '').split(',').filter((c) => c)
      })
    })
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value })
  },

  toggleLang(e) {
    const code = e.currentTarget.dataset.code
    let selected = this.data.selected.slice()
    if (selected.indexOf(code) >= 0) {
      selected = selected.filter((c) => c !== code)
    } else {
      selected.push(code)
    }
    this.setData({ selected })
  },

  save() {
    put('/users/me', {
      nickname: this.data.nickname.trim(),
      preferredLanguages: this.data.selected.join(',')
    }).then((user) => {
      wx.setStorageSync('user', user)
      getApp().globalData.user = user
      this.setData({ user })
      wx.showToast({ title: '保存成功', icon: 'success' })
    })
  },

  goAchievements() {
    wx.navigateTo({ url: '/pages/achievements/achievements' })
  },

  logout() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('user')
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
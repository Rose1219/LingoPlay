const { get, put } = require('../../utils/request')
const { t, currentLang, setLang, SUPPORTED } = require('../../utils/i18n')

Page({
  data: {
    i18n: {},
    user: null,
    nickname: '',
    languages: [],
    selected: [],
    uiLangs: SUPPORTED,
    uiLang: 'zh-CN'
  },

  onShow() {
    this.setData({ uiLang: currentLang() })
    this.applyI18n()
    Promise.all([get('/languages'), get('/users/me')]).then(([languages, user]) => {
      this.setData({
        languages,
        user,
        nickname: user.nickname || '',
        selected: (user.preferredLanguages || '').split(',').filter((c) => c)
      })
      wx.setStorageSync('user', user)
      getApp().globalData.user = user
    })
  },

  applyI18n() {
    this.setData({
      i18n: {
        title: t('profile.title'),
        subtitle: t('profile.subtitle'),
        nickname: t('profile.nickname'),
        nicknamePh: t('profile.nicknamePh'),
        prefLang: t('profile.prefLang'),
        uiLang: t('profile.uiLang'),
        save: t('common.save'),
        logout: t('common.logout'),
        achievements: t('profile.achievements')
      }
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

  switchUiLang(e) {
    const code = e.currentTarget.dataset.code
    setLang(code)
    this.setData({ uiLang: code })
    this.applyI18n()
    // 同步底部标签文案
    const labels = [t('tab.home'), t('tab.courses'), t('tab.progress'), t('tab.community'), t('tab.profile')]
    labels.forEach((text, index) => {
      wx.setTabBarItem({ index, text })
    })
  },

  save() {
    put('/users/me', {
      nickname: this.data.nickname.trim(),
      preferredLanguages: this.data.selected.join(',')
    }).then((user) => {
      wx.setStorageSync('user', user)
      getApp().globalData.user = user
      this.setData({ user })
      wx.showToast({ title: '✓', icon: 'success' })
    })
  },

  goVip() {
    wx.navigateTo({ url: '/pages/vip/vip' })
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

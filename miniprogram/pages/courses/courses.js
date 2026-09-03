const { get } = require('../../utils/request')
const { t } = require('../../utils/i18n')

Page({
  data: {
    i18n: { all: '全部', vipOnly: 'VIP 专属' },
    languages: [],
    courses: [],
    activeLang: '',
    loading: false
  },

  onShow() {
    this.setData({ i18n: { all: t('courses.all'), vipOnly: t('courses.vipOnly') } })
    this.init()
  },

  init() {
    get('/languages').then((languages) => {
      this.setData({ languages })
      this.loadCourses()
    })
  },

  loadCourses() {
    this.setData({ loading: true })
    const params = this.data.activeLang ? { language: this.data.activeLang } : {}
    get('/courses', params)
      .then((courses) => {
        this.setData({ courses, loading: false })
      })
      .catch(() => {
        this.setData({ loading: false })
      })
  },

  switchTab(e) {
    const lang = e.currentTarget.dataset.lang || ''
    this.setData({ activeLang: lang })
    this.loadCourses()
  },

  goDetail(e) {
    const { id, viponly } = e.currentTarget.dataset
    // VIP 专属语种：非 VIP 用户先引导开通（后端也会兜底校验）
    const user = getApp().globalData.user || {}
    if (viponly && !user.vip) {
      wx.showModal({
        title: t('courses.vipOnly'),
        content: t('courses.locked'),
        confirmText: t('courses.goVip'),
        cancelText: t('common.cancel'),
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/vip/vip' })
          }
        }
      })
      return
    }
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + id })
  }
})

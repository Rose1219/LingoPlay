const { get } = require('../../utils/request')

Page({
  data: {
    languages: [],
    courses: [],
    activeLang: '',
    loading: false
  },

  onShow() {
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
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/course-detail/course-detail?id=' + id })
  }
})
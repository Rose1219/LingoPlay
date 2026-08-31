const { get, post } = require('../../utils/request')
const { relativeTime } = require('../../utils/game')

Page({
  data: {
    languages: [],
    posts: [],
    activeLang: '',
    page: 1,
    total: 0,
    showCreate: false,
    form: { title: '', content: '', languageCode: '' },
    creating: false
  },

  onShow() {
    if (!this.data.languages.length) {
      get('/languages').then((languages) => this.setData({ languages }))
    }
    this.loadPosts()
  },

  loadPosts() {
    const params = { page: this.data.page, size: 10 }
    if (this.data.activeLang) params.language = this.data.activeLang
    get('/posts', params).then((data) => {
      const posts = (data.records || []).map((p) => ({ ...p, timeText: relativeTime(p.createdAt) }))
      this.setData({ posts, total: data.total })
    })
  },

  switchTab(e) {
    this.setData({ activeLang: e.currentTarget.dataset.lang || '', page: 1 })
    this.loadPosts()
  },

  openCreate() {
    this.setData({ showCreate: true, form: { title: '', content: '', languageCode: '' } })
  },

  closeCreate() {
    this.setData({ showCreate: false })
  },

  stop() {},

  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  onContentInput(e) {
    this.setData({ 'form.content': e.detail.value })
  },

  onLangChange(e) {
    const lang = this.data.languages[e.detail.value]
    this.setData({ 'form.languageCode': lang ? lang.code : '' })
  },

  submitCreate() {
    const { title, content } = this.data.form
    if (!title.trim() || !content.trim()) {
      wx.showToast({ title: '请填写标题和内容', icon: 'none' })
      return
    }
    this.setData({ creating: true })
    post('/posts', this.data.form)
      .then(() => {
        wx.showToast({ title: '发布成功', icon: 'success' })
        this.setData({ showCreate: false, creating: false, page: 1 })
        this.loadPosts()
      })
      .catch(() => {
        this.setData({ creating: false })
      })
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/post-detail/post-detail?id=' + e.currentTarget.dataset.id })
  },

  loadMore() {
    if (this.data.posts.length < this.data.total) {
      this.setData({ page: this.data.page + 1 })
      this.loadPosts()
    }
  }
})
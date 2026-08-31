const { get, post } = require('../../utils/request')
const { relativeTime } = require('../../utils/game')

Page({
  data: {
    detail: null,
    commentText: '',
    sending: false
  },

  onLoad(query) {
    this.postId = query.id
    this.load()
  },

  load() {
    get('/posts/' + this.postId).then((detail) => {
      detail.post.timeText = relativeTime(detail.post.createdAt)
      detail.comments = (detail.comments || []).map((c) => ({ ...c, timeText: relativeTime(c.createdAt) }))
      this.setData({ detail })
      wx.setNavigationBarTitle({ title: detail.post.title })
    })
  },

  toggleLike() {
    post('/posts/' + this.postId + '/like', {}).then((res) => {
      this.setData({
        'detail.post.liked': res.liked,
        'detail.post.likeCount': res.likeCount
      })
    })
  },

  onCommentInput(e) {
    this.setData({ commentText: e.detail.value })
  },

  submitComment() {
    const content = this.data.commentText.trim()
    if (!content) {
      wx.showToast({ title: '评论内容不能为空', icon: 'none' })
      return
    }
    this.setData({ sending: true })
    post('/posts/' + this.postId + '/comments', { content })
      .then(() => {
        wx.showToast({ title: '评论成功', icon: 'success' })
        this.setData({ commentText: '', sending: false })
        this.load()
      })
      .catch(() => {
        this.setData({ sending: false })
      })
  }
})
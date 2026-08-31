const { get } = require('../../utils/request')
const { typeInfo, starsOf } = require('../../utils/game')

Page({
  data: {
    loading: true,
    course: null,
    units: []
  },

  onLoad(query) {
    this.lessonId = query.id
    this.load()
  },

  load() {
    get('/courses/' + this.lessonId).then((data) => {
      const units = (data.units || []).map((u) => ({
        ...u,
        lessons: (u.lessons || []).map((l) => ({
          ...l,
          typeLabel: typeInfo(l.type).label,
          typeEmoji: typeInfo(l.type).emoji,
          stars: starsOf(l.bestScore),
          starList: [1, 2, 3],
          statusText: l.status === 'COMPLETED' ? '已通关' : l.status === 'IN_PROGRESS' ? '闯关中' : '未开启',
          btnText: l.status === 'COMPLETED' ? '再战' : l.status === 'IN_PROGRESS' ? '继续' : '开战'
        }))
      }))
      this.setData({ course: data.course, units, loading: false })
      wx.setNavigationBarTitle({ title: data.course.title })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  goLearn(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/learn/learn?id=' + id })
  }
})
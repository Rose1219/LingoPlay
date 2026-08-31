const { get } = require('../../utils/request')

Page({
  data: {
    achievements: []
  },

  onShow() {
    get('/achievements').then((achievements) => {
      this.setData({
        achievements: achievements.map((a) => ({
          ...a,
          dateText: a.unlockedAt ? String(a.unlockedAt).slice(0, 10) : ''
        }))
      })
    })
  }
})
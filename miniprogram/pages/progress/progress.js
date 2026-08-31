const { get } = require('../../utils/request')
const { typeInfo } = require('../../utils/game')

Page({
  data: {
    stats: null,
    heatWeeks: [],
    typeRows: []
  },

  onShow() {
    this.load()
  },

  load() {
    get('/study/stats').then((stats) => {
      // 热力图：90 天按周分列
      const heat = stats.heatmap || []
      const weeks = []
      let week = []
      heat.forEach((d, i) => {
        const day = new Date(d.date.replace(/-/g, '/')).getDay()
        if (i === 0 && day > 0) {
          for (let k = 0; k < day; k++) week.push(null)
        }
        week.push(d)
        if (week.length === 7) {
          weeks.push(week)
          week = []
        }
      })
      if (week.length) weeks.push(week)

      const typeRows = (stats.typeAccuracy || []).map((t) => ({
        label: typeInfo(t.type).label,
        accuracy: t.accuracy || 0,
        scoreText: t.accuracy == null ? '暂无' : t.accuracy + ' 分'
      }))

      this.setData({ stats, heatWeeks: weeks, typeRows })
    })
  }
})
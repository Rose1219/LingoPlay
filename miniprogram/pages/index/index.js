const { get } = require('../../utils/request')
const { speak } = require('../../utils/tts')

Page({
  data: {
    daily: null,
    revealed: false,
    loadingWord: false,
    user: null,
    games: [
      { type: 'WORD', emoji: '🎯', name: '单词闯关', desc: '五选一答题 · 六关挑战' },
      { type: 'GRAMMAR', emoji: '🗺️', name: '语法探险', desc: '闯关解谜学语法' },
      { type: 'SPEAK', emoji: '🎙️', name: '口语星球', desc: '挑战发音闯关' },
      { type: 'LISTEN', emoji: '🕵️', name: '听力侦探', desc: '听音破译句子' },
      { type: 'DIALOG', emoji: '🎭', name: '对话剧场', desc: '沉浸式角色扮演' }
    ]
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({ user })
    if (!this.data.daily) {
      this.loadDaily(false)
    }
  },

  loadDaily(random) {
    this.setData({ loadingWord: true })
    const task = random ? get('/game/daily-word', { random: true }) : get('/game/daily-word')
    // 至少 600ms 加载态，让换词反馈可感知
    Promise.all([
      task.catch(() => null),
      new Promise((resolve) => setTimeout(resolve, 600))
    ]).then(([data]) => {
      if (data) {
        this.setData({ daily: data, revealed: false, loadingWord: false })
      } else {
        this.setData({ loadingWord: false })
      }
    })
  },

  toggleReveal() {
    this.setData({ revealed: !this.data.revealed })
  },

  nextWord() {
    if (this.data.loadingWord) return
    this.loadDaily(true)
  },

  playWord() {
    const d = this.data.daily
    if (!d) return
    speak(d.word, d.languageCode)
  },

  playExample() {
    const d = this.data.daily
    if (!d || !d.example) return
    speak(d.example, d.languageCode)
  },

  goPlay() {
    wx.navigateTo({ url: '/pages/word-quiz/word-quiz' })
  },

  goTranslate() {
    wx.navigateTo({ url: '/pages/translate/translate' })
  },

  goVip() {
    wx.navigateTo({ url: '/pages/vip/vip' })
  },

  goCourses() {
    wx.switchTab({ url: '/pages/courses/courses' })
  }
})
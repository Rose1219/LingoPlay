const { get, post } = require('../../utils/request')
const { shuffle } = require('../../utils/game')
const { speak } = require('../../utils/tts')

const localeMap = { en: 'en-US', ja: 'ja-JP', ko: 'ko-KR' }
const iconMap = { en: '🇬🇧', ja: '🇯🇵', ko: '🇰🇷' }

// 关卡配置：难度递增（不限时 → 5 秒限时）
const LEVELS = [
  { name: '热身训练', desc: '不限时作答，先熟悉单词', questions: 8, timeLimit: 0 },
  { name: '限时初练', desc: '每题限时 15 秒，从容作答', questions: 8, timeLimit: 15 },
  { name: '稳步进阶', desc: '每题限时 10 秒，考验反应', questions: 8, timeLimit: 10 },
  { name: '高手过招', desc: '每题限时 8 秒，节奏加快', questions: 8, timeLimit: 8 },
  { name: '闪电风暴', desc: '每题限时 6 秒，手速对决', questions: 8, timeLimit: 6 },
  { name: '大师之路', desc: '每题限时 5 秒，登顶挑战', questions: 8, timeLimit: 5 }
]

Page({
  data: {
    // 语种选择
    step: 'select', // select | playing
    languages: [],
    wordCounts: {},
    loadingLang: '',
    // 游戏状态
    phase: 'intro', // intro | playing | passed | gameover | complete
    langIcon: '',
    langName: '',
    levelIndex: 0,
    levels: LEVELS,
    // 本关配置
    levelName: '',
    levelDesc: '',
    levelQuestions: 8,
    levelTimeLimit: 0,
    // 作答状态
    qIndex: 0,
    lives: 3,
    score: 0,
    combo: 0,
    maxCombo: 0,
    levelCorrect: 0,
    totalCorrect: 0,
    totalAnswered: 0,
    timeLeft: 0,
    timePercent: 100,
    progressPercent: 0,
    answered: false,
    lastCorrect: false,
    selectedIndex: -1,
    question: null, // { display, phonetic, isWordToMeaning, options, answerIndex, answer }
    qKey: 0,
    gameOverReason: '',
    // 结算
    stars: 0,
    accuracy: 0,
    starList: [1, 2, 3],
    lastGain: 0,
    lifeBonus: 0
  },

  onLoad() {
    this.words = []
    this.langCode = 'en'
    this.askedWords = []
    this.answerLog = []
    this.levelPool = []
    this.loadLanguages()
  },

  onUnload() {
    this.stopTimer()
    if (this.resolveTimer) clearTimeout(this.resolveTimer)
  },

  // ---------- 语种选择 ----------
  loadLanguages() {
    get('/languages')
      .then((languages) => {
        const list = (languages || []).map((l) => ({
          code: l.code,
          nameCn: l.nameCn,
          icon: iconMap[l.code] || '🌍',
          isDefault: l.code === 'en'
        }))
        this.setData({ languages: list })
        // 默认英语卡片高亮，并预取各语种词库数量
        list.forEach((l) => this.loadCount(l.code))
      })
      .catch(() => {
        this.setData({ languages: [{ code: 'en', nameCn: '英语', icon: '🇬🇧', isDefault: true }] })
        this.loadCount('en')
      })
  },

  loadCount(code) {
    get('/game/word-quiz', { lang: code })
      .then((list) => {
        this.setData({ ['wordCounts.' + code]: (list || []).length })
      })
      .catch(() => {
        this.setData({ ['wordCounts.' + code]: 0 })
      })
  },

  startGame(e) {
    const code = e.currentTarget.dataset.code
    if (this.data.loadingLang) return
    const count = this.data.wordCounts[code]
    if (count === 0) {
      wx.showToast({ title: '该语种暂无词库', icon: 'none' })
      return
    }
    this.setData({ loadingLang: code })
    get('/game/word-quiz', { lang: code })
      .then((list) => {
        if (!list || !list.length) {
          this.setData({ loadingLang: '' })
          wx.showToast({ title: '该语种暂无词库', icon: 'none' })
          return
        }
        this.words = list
        this.langCode = code
        const lang = this.data.languages.find((l) => l.code === code)
        this.setData({
          loadingLang: '',
          step: 'playing',
          langIcon: lang ? lang.icon : '',
          langName: lang ? lang.nameCn : '',
          phase: 'intro',
          levelIndex: 0,
          qIndex: 0,
          lives: 3,
          score: 0,
          combo: 0,
          maxCombo: 0,
          levelCorrect: 0,
          totalCorrect: 0,
          totalAnswered: 0,
          question: null,
          gameOverReason: ''
        })
        this.askedWords = []
        this.answerLog = []
        this.syncLevel()
      })
      .catch(() => {
        this.setData({ loadingLang: '' })
        wx.showToast({ title: '词库加载失败', icon: 'none' })
      })
  },

  quitGame() {
    this.stopTimer()
    if (this.resolveTimer) clearTimeout(this.resolveTimer)
    this.setData({ step: 'select' })
  },

  // ---------- 关卡同步 ----------
  syncLevel() {
    const lv = LEVELS[Math.min(this.data.levelIndex, LEVELS.length - 1)]
    this.setData({
      levelName: lv.name,
      levelDesc: lv.desc,
      levelQuestions: lv.questions,
      levelTimeLimit: lv.timeLimit,
      progressPercent: 0,
      timePercent: 100
    })
  },

  // ---------- 出题 ----------
  makeQuestion(target) {
    const isWordToMeaning = Math.random() < 0.5
    const display = isWordToMeaning ? target.word : target.meaning
    const answer = isWordToMeaning ? target.meaning : target.word
    // 混淆项：优先选未考过的词
    const others = this.words.filter((w) => w.word !== target.word)
    const preferred = others.filter((w) => this.askedWords.indexOf(w.word) < 0)
    const pool = preferred.length >= 4 ? preferred : others
    const distractors = shuffle(pool).slice(0, 4).map((w) => (isWordToMeaning ? w.meaning : w.word))
    const options = shuffle([answer].concat(distractors))
    return {
      display,
      answer,
      options,
      answerIndex: options.indexOf(answer),
      isWordToMeaning,
      phonetic: target.phonetic || '',
      // 记录用
      word: target.word,
      meaning: target.meaning
    }
  },

  startLevel() {
    if (!this.words.length) {
      wx.showToast({ title: '词库为空', icon: 'none' })
      return
    }
    const lv = LEVELS[this.data.levelIndex]
    // 本关题池：优先考未考过的词
    const fresh = this.words.filter((w) => this.askedWords.indexOf(w.word) < 0)
    const pool = fresh.length >= lv.questions ? fresh : this.words
    this.levelPool = shuffle(pool).slice(0, Math.min(lv.questions, pool.length))
    // 词库不足时循环补齐
    while (this.levelPool.length < lv.questions) {
      this.levelPool.push(this.words[Math.floor(Math.random() * this.words.length)])
    }
    this.setData({ levelCorrect: 0, qIndex: 0, phase: 'playing' })
    this.showQuestion()
  },

  showQuestion() {
    const target = this.levelPool[this.data.qIndex]
    this.askedWords.push(target.word)
    const question = this.makeQuestion(target)
    this.setData({
      question,
      answered: false,
      selectedIndex: -1,
      qKey: (this.data.qKey || 0) + 1
    })
    if (this.data.levelTimeLimit > 0) {
      this.setData({ timeLeft: this.data.levelTimeLimit, timePercent: 100 })
      this.startTimer()
    }
  },

  startTimer() {
    this.stopTimer()
    const limit = this.data.levelTimeLimit
    this.timer = setInterval(() => {
      const left = this.data.timeLeft - 0.1
      if (left <= 0) {
        this.stopTimer()
        this.setData({ timeLeft: 0, timePercent: 0 })
        // 超时视为答错
        this.resolveAnswer(-1)
      } else {
        this.setData({ timeLeft: left, timePercent: Math.round((left / limit) * 100) })
      }
    }, 100)
  },

  stopTimer() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },

  // ---------- 作答 ----------
  choose(e) {
    if (this.data.answered) return
    this.stopTimer()
    this.resolveAnswer(Number(e.currentTarget.dataset.index))
  },

  resolveAnswer(i) {
    const q = this.data.question
    const correct = i === q.answerIndex
    const patch = {
      answered: true,
      selectedIndex: i,
      lastCorrect: correct,
      totalAnswered: this.data.totalAnswered + 1
    }
    this.answerLog.push({ word: q.word, meaning: q.meaning, correct })

    if (correct) {
      let gain = 100
      if (this.data.levelTimeLimit > 0) {
        gain += Math.round(Math.max(0, this.data.timeLeft) * 10)
      }
      const combo = this.data.combo + 1
      if (combo >= 2) gain += combo * 20
      if (combo >= 5) gain *= 2
      patch.combo = combo
      patch.maxCombo = Math.max(this.data.maxCombo, combo)
      patch.levelCorrect = this.data.levelCorrect + 1
      patch.totalCorrect = this.data.totalCorrect + 1
      patch.score = this.data.score + gain
      patch.lastGain = gain
    } else {
      patch.combo = 0
      patch.lives = this.data.lives - 1
    }
    this.setData(patch)

    // 生命耗尽：短暂展示错误后结束
    if (!correct && patch.lives <= 0) {
      this.resolveTimer = setTimeout(() => {
        this.finishGame('gameover', '生命耗尽，重整旗鼓再来！')
      }, 1400)
      return
    }
    this.resolveTimer = setTimeout(() => this.nextQuestion(), 1000)
  },

  nextQuestion() {
    const qIndex = this.data.qIndex + 1
    const total = this.data.levelQuestions
    if (qIndex >= total) {
      // 本关结束：判定通过率
      const rate = this.data.levelCorrect / total
      if (rate >= 0.75) {
        const bonus = this.data.lives * 200
        this.setData({ score: this.data.score + bonus, lifeBonus: bonus, phase: 'passed' })
      } else {
        this.finishGame('gameover', '通过率 ' + Math.round(rate * 100) + '%，差一点点（需 75%）')
      }
      return
    }
    this.setData({
      qIndex,
      progressPercent: Math.round((qIndex / total) * 100)
    })
    this.showQuestion()
  },

  nextLevel() {
    const levelIndex = this.data.levelIndex + 1
    if (levelIndex >= LEVELS.length) {
      this.finishGame('complete', '')
      return
    }
    this.setData({ levelIndex, phase: 'intro', lifeBonus: 0 })
    this.syncLevel()
  },

  finishGame(endPhase, reason) {
    this.stopTimer()
    const total = this.data.totalAnswered
    const correct = this.data.totalCorrect
    const accuracy = total ? Math.round((correct / total) * 100) : 0
    const stars = accuracy >= 90 ? 3 : accuracy >= 75 ? 2 : 1
    this.setData({ phase: endPhase, gameOverReason: reason, accuracy, stars })
    // 提交成绩（失败不阻塞结算界面）
    post('/game/word-quiz/submit', {
      languageCode: this.langCode,
      minutes: 2,
      score: accuracy,
      correctCount: correct,
      totalCount: total,
      words: this.answerLog
    }).catch(() => {})
  },

  restart() {
    this.askedWords = []
    this.answerLog = []
    this.setData({
      levelIndex: 0,
      qIndex: 0,
      lives: 3,
      score: 0,
      combo: 0,
      maxCombo: 0,
      levelCorrect: 0,
      totalCorrect: 0,
      totalAnswered: 0,
      phase: 'intro',
      question: null,
      gameOverReason: '',
      lifeBonus: 0
    })
    this.syncLevel()
  },

  // ---------- 发音 ----------
  speakQuestion() {
    const q = this.data.question
    if (q) speak(q.display, localeMap[this.langCode] || 'en-US')
  }
})

const { get, post } = require('../../utils/request')
const { shuffle, similarity, scoreOf, starsOf, typeInfo } = require('../../utils/game')
const { speak } = require('../../utils/tts')

const localeMap = { en: 'en-US', ja: 'ja-JP', ko: 'ko-KR' }

Page({
  data: {
    lesson: null,
    typeLabel: '',
    nextLessonId: 0,
    nextLessonTitle: '',
    // 单词答题（5选1）
    quizQ: null,
    quizQKey: 0,
    quizIndex: 0,
    quizTotal: 0,
    quizAnswered: false,
    quizSelected: -1,
    quizCorrectCount: 0,
    quizLastCorrect: false,
    quizProgress: 0,
    // 语法
    points: [],
    pointIndex: 0,
    qIndex: 0,
    phase: 'explain',
    answered: false,
    selected: -1,
    correctCount: 0,
    // 口语
    sentences: [],
    sIndex: 0,
    showTranslation: false,
    scored: false,
    lastScore: 0,
    speakScores: [],
    // 听力
    listenItems: [],
    lIndex: 0,
    listenInput: '',
    listenChecked: false,
    listenScore: 0,
    showHint: false,
    listenScores: [],
    // 对话
    turns: [],
    tIndex: 0,
    chat: [],
    typed: '',
    userScores: [],
    // 结算
    finished: false,
    score: 0,
    stars: 0,
    starList: [1, 2, 3],
    newAchievements: []
  },

  onLoad(query) {
    this.lessonId = query.id
    get('/lessons/' + this.lessonId).then((lesson) => {
      this.setData({ lesson, typeLabel: typeInfo(lesson.type).label })
      wx.setNavigationBarTitle({ title: lesson.title })
      post('/lessons/' + this.lessonId + '/start', {}).catch(() => {})
      this.initMode(lesson)
    })
  },

  lang() {
    const code = this.data.lesson ? this.data.lesson.languageCode : 'en'
    return localeMap[code] || 'en-US'
  },

  // ---------- 初始化各模式 ----------
  initMode(lesson) {
    const content = lesson.content || {}
    if (lesson.type === 'WORD') {
      this.quizWords = (content.words || []).slice()
      this.quizLog = []
      this.setData({
        quizTotal: this.quizWords.length,
        quizIndex: 0,
        quizCorrectCount: 0,
        quizProgress: 0,
        combo: 0,
        maxCombo: 0,
        quizQ: null
      })
      this.showQuizQuestion()
    } else if (lesson.type === 'GRAMMAR') {
      this.setData({ points: content.points || [] })
    } else if (lesson.type === 'SPEAK') {
      this.setData({ sentences: content.sentences || [] })
    } else if (lesson.type === 'LISTEN') {
      this.setData({ listenItems: content.items || [] })
    } else if (lesson.type === 'DIALOG') {
      this.setData({ turns: content.turns || [], chat: [] })
    }
  },

  onUnload() {
    if (this.quizResolveTimer) clearTimeout(this.quizResolveTimer)
  },

  // ---------- 单词答题（5选1） ----------
  /** 生成题目：随机「显示外文选中文」或「显示中文选外文」，5 个选项 */
  makeQuizQuestion(target) {
    const isWordToMeaning = Math.random() < 0.5
    const display = isWordToMeaning ? target.word : target.meaning
    const answer = isWordToMeaning ? target.meaning : target.word
    const others = this.quizWords.filter((w) => w.word !== target.word)
    const distractors = shuffle(others).slice(0, 4).map((w) => (isWordToMeaning ? w.meaning : w.word))
    const options = shuffle([answer].concat(distractors))
    return {
      display,
      answer,
      options,
      answerIndex: options.indexOf(answer),
      isWordToMeaning,
      phonetic: target.phonetic || '',
      word: target.word,
      meaning: target.meaning
    }
  },

  showQuizQuestion() {
    const target = this.quizWords[this.data.quizIndex]
    const quizQ = this.makeQuizQuestion(target)
    this.setData({
      quizQ,
      quizAnswered: false,
      quizSelected: -1,
      quizQKey: this.data.quizQKey + 1
    })
  },

  quizSpeak() {
    const q = this.data.quizQ
    if (q) speak(q.display, this.lang())
  },

  quizChoose(e) {
    if (this.data.quizAnswered) return
    const i = Number(e.currentTarget.dataset.index)
    const q = this.data.quizQ
    const correct = i === q.answerIndex
    this.quizLog.push({ word: q.word, meaning: q.meaning, correct })
    const patch = { quizAnswered: true, quizSelected: i, quizLastCorrect: correct }
    if (correct) {
      const combo = this.data.combo + 1
      patch.quizCorrectCount = this.data.quizCorrectCount + 1
      patch.combo = combo
      patch.maxCombo = Math.max(this.data.maxCombo, combo)
    } else {
      patch.combo = 0
    }
    this.setData(patch)
    this.quizResolveTimer = setTimeout(() => this.quizNext(), 1000)
  },

  quizNext() {
    const next = this.data.quizIndex + 1
    if (next >= this.data.quizTotal) {
      this.finishWord()
      return
    }
    this.setData({
      quizIndex: next,
      quizProgress: Math.round((next / this.data.quizTotal) * 100)
    })
    this.showQuizQuestion()
  },

  finishWord() {
    const total = this.data.quizTotal
    const score = total ? Math.round((this.data.quizCorrectCount / total) * 100) : 0
    this.setData({ score, stars: starsOf(score) })
    this.submit(score, this.data.quizCorrectCount, total, this.quizLog)
  },

  // ---------- 语法 ----------
  grammarStart() {
    this.setData({ phase: 'question', answered: false, selected: -1 })
  },

  grammarAnswer(e) {
    if (this.data.answered) return
    const oi = Number(e.currentTarget.dataset.index)
    const point = this.data.points[this.data.pointIndex]
    const question = point.questions[this.data.qIndex]
    const correct = question.answer === oi
    const patch = { answered: true, selected: oi }
    if (correct) {
      const combo = this.data.combo + 1
      patch.correctCount = this.data.correctCount + 1
      patch.combo = combo
      patch.maxCombo = Math.max(this.data.maxCombo, combo)
    } else {
      patch.combo = 0
    }
    this.setData(patch)
  },

  grammarNext() {
    const point = this.data.points[this.data.pointIndex]
    const last = this.data.pointIndex >= this.data.points.length - 1 && this.data.qIndex >= point.questions.length - 1
    if (last) {
      const total = this.data.points.reduce((s, p) => s + p.questions.length, 0)
      const score = Math.round((this.data.correctCount / total) * 100)
      this.setData({ score, stars: starsOf(score) })
      this.submit(score, this.data.correctCount, total)
    } else if (this.data.qIndex < point.questions.length - 1) {
      this.setData({ qIndex: this.data.qIndex + 1, answered: false, selected: -1 })
    } else {
      this.setData({ pointIndex: this.data.pointIndex + 1, qIndex: 0, phase: 'explain', answered: false, selected: -1 })
    }
  },

  // ---------- 口语 ----------
  speakPlay() {
    const s = this.data.sentences[this.data.sIndex]
    if (s) speak(s.text, this.lang())
  },

  speakPlaySlow() {
    const s = this.data.sentences[this.data.sIndex]
    if (s) speak(s.text, this.lang(), 0.6)
  },

  toggleTranslation() {
    this.setData({ showTranslation: !this.data.showTranslation })
  },

  speakSelfRate(e) {
    const score = Number(e.currentTarget.dataset.score)
    const speakScores = this.data.speakScores.concat(score)
    this.setData({ scored: true, lastScore: score, speakScores })
  },

  speakNext() {
    if (this.data.sIndex < this.data.sentences.length - 1) {
      this.setData({ sIndex: this.data.sIndex + 1, scored: false, showTranslation: false })
    } else {
      const scores = this.data.speakScores
      const avg = Math.round(scores.reduce((s, v) => s + v, 0) / scores.length)
      this.setData({ score: avg, stars: starsOf(avg) })
      this.submit(avg, scores.filter((s) => s >= 60).length, scores.length)
    }
  },

  // ---------- 听力 ----------
  listenPlay() {
    const item = this.data.listenItems[this.data.lIndex]
    if (item) speak(item.text, this.lang())
  },

  onListenInput(e) {
    this.setData({ listenInput: e.detail.value })
  },

  toggleHint() {
    this.setData({ showHint: true })
  },

  listenCheck() {
    if (!this.data.listenInput.trim()) return
    const item = this.data.listenItems[this.data.lIndex]
    const score = scoreOf(similarity(this.data.listenInput, item.text))
    this.setData({ listenChecked: true, listenScore: score })
  },

  listenNext() {
    const listenScores = this.data.listenScores.concat(this.data.listenScore)
    if (this.data.lIndex < this.data.listenItems.length - 1) {
      this.setData({
        lIndex: this.data.lIndex + 1,
        listenInput: '',
        listenChecked: false,
        showHint: false,
        listenScores
      })
    } else {
      const avg = Math.round(listenScores.reduce((s, v) => s + v, 0) / listenScores.length)
      this.setData({ score: avg, stars: starsOf(avg) })
      this.submit(avg, listenScores.filter((s) => s >= 60).length, listenScores.length)
    }
  },

  // ---------- 对话 ----------
  dialogStart() {
    this.dialogNextTurn()
  },

  dialogNextTurn() {
    const turn = this.data.turns[this.data.tIndex]
    if (!turn) return
    const chat = this.data.chat.concat([{ role: 'ai', text: turn.text, translation: turn.translation }])
    this.setData({ chat })
    // 朗读后等待用户作答；语音不可用时直接进入作答
    speak(turn.text, this.lang()).then(() => {
      this.setData({ waitingUser: true })
    })
    this.setData({ waitingUser: true })
  },

  onTypedInput(e) {
    this.setData({ typed: e.detail.value })
  },

  dialogSubmit() {
    const text = this.data.typed.trim()
    if (!text) return
    const turn = this.data.turns[this.data.tIndex]
    const score = scoreOf(similarity(text, turn.text))
    const userScores = this.data.userScores.concat(score)
    const chat = this.data.chat.concat([
      { role: 'user', text, score, correct: score >= 60 }
    ])
    if (this.data.tIndex < this.data.turns.length - 1) {
      this.setData({ chat, userScores, typed: '', tIndex: this.data.tIndex + 1 })
      this.dialogNextTurn()
    } else {
      this.setData({ chat, userScores, typed: '' })
      const avg = Math.round(userScores.reduce((s, v) => s + v, 0) / userScores.length)
      this.setData({ score: avg, stars: starsOf(avg) })
      this.submit(avg, userScores.filter((s) => s >= 60).length, userScores.length)
    }
  },

  dialogPlayTurn() {
    const turn = this.data.turns[this.data.tIndex]
    if (turn) speak(turn.text, this.lang())
  },

  // ---------- 提交与结算 ----------
  submit(score, correctCount, totalCount, words) {
    const payload = { minutes: 1, score, correctCount, totalCount }
    if (words) payload.words = words
    post('/lessons/' + this.lessonId + '/submit', payload)
      .then((res) => {
        this.setData({
          finished: true,
          newAchievements: (res && res.newAchievements) || [],
          nextLessonId: (res && res.hasNextLesson && res.nextLessonId) || 0,
          nextLessonTitle: (res && res.nextLessonTitle) || ''
        })
      })
      .catch(() => {
        this.setData({ finished: true, newAchievements: [], nextLessonId: 0, nextLessonTitle: '' })
      })
  },

  /** 进入课程内下一关 */
  goNext() {
    if (!this.data.nextLessonId) return
    wx.redirectTo({ url: '/pages/learn/learn?id=' + this.data.nextLessonId })
  },

  retry() {
    this.quizLog = []
    if (this.quizResolveTimer) clearTimeout(this.quizResolveTimer)
    this.setData({
      quizQ: null, quizIndex: 0, quizCorrectCount: 0, quizAnswered: false, quizSelected: -1, quizProgress: 0,
      combo: 0, maxCombo: 0,
      pointIndex: 0, qIndex: 0, phase: 'explain', answered: false, selected: -1, correctCount: 0,
      sIndex: 0, scored: false, showTranslation: false, lastScore: 0, speakScores: [],
      lIndex: 0, listenInput: '', listenChecked: false, listenScore: 0, showHint: false, listenScores: [],
      tIndex: 0, chat: [], typed: '', userScores: [],
      finished: false, score: 0, stars: 0, newAchievements: [],
      nextLessonId: 0, nextLessonTitle: ''
    })
    this.initMode(this.data.lesson)
  },

  backToMap() {
    wx.navigateBack({ delta: 1 })
  }
})
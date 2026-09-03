const { get, post } = require('../../utils/request')
const { t, currentLang } = require('../../utils/i18n')
const { speak } = require('../../utils/tts')

Page({
  data: {
    i18n: {},
    languages: [],
    langNames: [],
    sourceNames: [],
    sourceIndex: 0,
    targetIndex: 1,
    text: '',
    result: '',
    detectedName: '',
    translating: false
  },

  onLoad() {
    this.applyI18n()
    get('/translate/languages').then((languages) => {
      const langNames = languages.map((l) => l.flag + ' ' + l.nativeName)
      this.setData({
        languages,
        langNames,
        // 源语言第 0 项固定为「自动检测」
        sourceNames: [this.data.i18n.auto].concat(langNames),
        // 默认目标语种：简体中文（输入英文时直接译为中文）
        targetIndex: Math.max(0, languages.findIndex((l) => l.code === 'zh-CN'))
      })
    }).catch(() => {})
  },

  onShow() {
    this.applyI18n()
  },

  applyI18n() {
    const i = currentLang()
    this.setData({
      i18n: {
        title: t('translate.title'),
        subtitle: t('translate.subtitle'),
        auto: t('translate.auto'),
        placeholder: t('translate.placeholder'),
        translateBtn: t('translate.translateBtn'),
        translating: t('translate.translating'),
        detected: t('translate.detected'),
        resultPlaceholder: t('translate.resultPlaceholder'),
        copyResult: t('translate.copyResult'),
        play: t('translate.play'),
        back: t('common.back')
      }
    })
    wx.setNavigationBarTitle({ title: t('translate.title') })
  },

  onSourceChange(e) {
    this.setData({ sourceIndex: Number(e.detail.value) })
  },

  onTargetChange(e) {
    this.setData({ targetIndex: Number(e.detail.value) })
  },

  onTextInput(e) {
    this.setData({ text: e.detail.value })
  },

  swap() {
    const { sourceIndex, targetIndex, languages } = this.data
    let newTarget
    if (sourceIndex === 0) {
      // 源为自动检测：目标优先切到最近一次检测到的语种，避免互换后源/目标相同
      const di = this.lastDetected ? languages.findIndex((l) => l.code === this.lastDetected) : -1
      if (di >= 0 && di !== targetIndex) {
        newTarget = di
      } else {
        const alt = (languages[targetIndex] && languages[targetIndex].code.indexOf('zh') === 0) ? 'en' : 'zh-CN'
        newTarget = Math.max(0, languages.findIndex((l) => l.code === alt))
      }
    } else {
      newTarget = sourceIndex
    }
    const newSource = targetIndex
    this.setData({ sourceIndex: newSource, targetIndex: newTarget })
    // 互换后把原文与译文对调，方便反向修改
    if (this.data.text && this.data.result) {
      const oldText = this.data.text
      this.setData({ text: this.data.result, result: oldText })
    }
  },

  doTranslate() {
    const { text, translating, sourceIndex, targetIndex, languages } = this.data
    const content = (text || '').trim()
    if (!content || translating) return
    // 源语言索引 0 = 自动检测，其余与 languages 数组错位 1
    const source = sourceIndex === 0 ? 'auto' : languages[sourceIndex - 1].code
    let target = languages[targetIndex].code
    this.setData({ translating: true, result: '', detectedName: '' })

    const render = (r = {}) => {
      // 后端返回 translatedText / detectedLanguage，统一成组件字段名
      const translated = r.translatedText || r.translated || ''
      const detected = r.detectedLanguage || r.detected || ''
      if (detected) this.lastDetected = detected
      let detectedName = ''
      if (detected) {
        const found = languages.find((l) => l.code === detected)
        detectedName = found ? found.nativeName : detected
      }
      this.setData({ result: translated, detectedName })
    }

    post('/translate', { text: content, source, target })
      .then((r = {}) => {
        const detected = r.detectedLanguage || r.detected || ''
        // 同语言避让：源语言与目标相同（如英文→英文）时自动切换目标并重译，不回显原文
        if (detected && detected === target) {
          const alt = detected.indexOf('zh') === 0 ? 'en' : 'zh-CN'
          const idx = languages.findIndex((l) => l.code === alt)
          if (idx >= 0) {
            target = alt
            this.setData({ targetIndex: idx })
            wx.showToast({ title: t('translate.autoSwitchedTo') + (languages[idx].nativeName || alt), icon: 'none' })
            return post('/translate', { text: content, source, target }).then((r2 = {}) => render(r2))
          }
        }
        render(r)
      })
      .catch(() => {
        wx.showToast({ title: t('translate.failed'), icon: 'none' })
      })
      .finally(() => {
        this.setData({ translating: false })
      })
  },

  copyResult() {
    if (!this.data.result) return
    wx.setClipboardData({
      success: () => wx.showToast({ title: t('common.copied'), icon: 'success' })
    })
  },

  playResult() {
    const { result, languages, targetIndex } = this.data
    if (!result) return
    const code = languages && languages[targetIndex] ? languages[targetIndex].code : 'en'
    speak(result, code)
  }
})

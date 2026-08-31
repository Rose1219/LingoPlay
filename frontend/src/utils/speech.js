// 基于 Web Speech API 的语音识别工具

/** 是否支持语音识别 */
export function recognitionSupported() {
  return typeof window !== 'undefined' &&
    (window.SpeechRecognition || window.webkitSpeechRecognition)
}

/**
 * 录音识别一句话
 * @param {string} lang 语言代码，如 en-US / ja-JP / ko-KR
 * @param {number} timeoutMs 最长录音时长
 * @returns {Promise<string>} 识别文本；不支持或失败返回 ''
 */
export function recognize(lang, timeoutMs = 8000) {
  return new Promise((resolve) => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SR) {
      resolve('')
      return
    }
    const rec = new SR()
    rec.lang = lang
    rec.interimResults = false
    rec.maxAlternatives = 1
    let done = false
    const finish = (text) => {
      if (done) return
      done = true
      resolve(text)
    }
    rec.onresult = (event) => {
      let text = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        if (event.results[i].isFinal) {
          text += event.results[i][0].transcript
        }
      }
      finish(text)
    }
    rec.onerror = () => finish('')
    rec.onend = () => finish('')
    setTimeout(() => {
      try { rec.stop() } catch (e) { /* 忽略 */ }
      finish('')
    }, timeoutMs)
    try {
      rec.start()
    } catch (e) {
      finish('')
    }
  })
}

/** 语言代码 → 语音识别代码 */
export function langToSpeechLocale(lang) {
  const map = { en: 'en-US', ja: 'ja-JP', ko: 'ko-KR' }
  return map[lang] || 'en-US'
}
// 基于 Web Speech API 的发音工具

import { ElMessage } from 'element-plus'

let cachedVoices = []
const warnedLangs = new Set()

/** 是否支持语音合成 */
export function ttsSupported() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/**
 * 预热语音列表：Chrome 的 getVoices() 异步加载，
 * 应用启动时监听 voiceschanged 缓存，避免首次点击时列表为空。
 */
export function initVoices() {
  if (!ttsSupported()) return
  const load = () => {
    const voices = window.speechSynthesis.getVoices()
    if (voices.length) {
      cachedVoices = voices
    }
  }
  load()
  window.speechSynthesis.onvoiceschanged = load
}

/** 等待语音列表就绪（Chrome 冷启动时可能延迟几百毫秒） */
function waitVoices(maxMs = 500) {
  return new Promise((resolve) => {
    if (cachedVoices.length) {
      resolve()
      return
    }
    const start = Date.now()
    const timer = setInterval(() => {
      if (cachedVoices.length || Date.now() - start > maxMs) {
        clearInterval(timer)
        resolve()
      }
    }, 50)
  })
}

/** 获取匹配语言的语音 */
function pickVoice(lang) {
  const voices = cachedVoices.length
    ? cachedVoices
    : window.speechSynthesis.getVoices()
  const prefix = lang.toLowerCase().split('-')[0]
  return (
    voices.find((v) => v.lang && v.lang.toLowerCase() === lang.toLowerCase()) ||
    voices.find((v) => v.lang && v.lang.toLowerCase().startsWith(prefix)) ||
    null
  )
}

/** 该语言缺失语音包时提示一次（不重复打扰） */
function warnMissingOnce(lang) {
  if (warnedLangs.has(lang)) return
  warnedLangs.add(lang)
  const names = { 'en-US': '英语', 'ja-JP': '日语', 'ko-KR': '韩语' }
  const name = names[lang] || lang
  ElMessage({
    type: 'warning',
    duration: 6000,
    message: `系统未安装${name}语音包，已用系统默认语音朗读。如需标准发音，请在 Windows「设置 → 时间和语言 → 语音」中添加${name}语音。`
  })
}

/**
 * 朗读文本
 * @returns {Promise<'ok'|'fallback'|'failed'>}
 *   ok       找到匹配语音，标准朗读
 *   fallback 无匹配语音，由浏览器默认语音兜底
 *   failed   不支持或朗读失败
 * Promise 在朗读结束后 resolve（对话场景依赖此节奏推进回合）
 */
export async function speak(text, lang, rate = 0.9) {
  if (!ttsSupported() || !text) {
    return 'failed'
  }
  await waitVoices()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.rate = rate
  const voice = pickVoice(lang)
  if (voice) {
    // 找到匹配语音：标准朗读
    utterance.voice = voice
    utterance.lang = voice.lang
  } else {
    // 关键修复：无匹配语音包时若仍设置目标语言（如 en-US），
    // Chrome 会因找不到对应语音而静默跳过（事件照常触发但无声音）。
    // 必须显式改用系统默认语音兜底朗读（中文 TTS 可朗读英文单词，带口音但有声）。
    const fallback = cachedVoices.find((v) => v.default) || cachedVoices[0]
      || window.speechSynthesis.getVoices()[0]
    if (fallback) {
      utterance.voice = fallback
      utterance.lang = fallback.lang
    }
  }
  // Chrome 已知 bug：cancel() 后立即 speak() 会偶发完全无声，加短延迟规避
  window.speechSynthesis.cancel()
  await new Promise((resolve) => setTimeout(resolve, 60))
  return new Promise((resolve) => {
    let status = voice ? 'ok' : 'fallback'
    let settled = false
    const finish = (value) => {
      if (!settled) {
        settled = true
        resolve(value)
      }
    }
    // 朗读开始即确定状态，缺语音包时立刻提示（不等读完）
    utterance.onstart = () => {
      if (status === 'fallback') {
        warnMissingOnce(lang)
      }
    }
    // 出错立即结束，避免 Promise 挂起
    utterance.onerror = () => finish('failed')
    // 朗读结束对外 resolve，调用方可据此推进流程
    utterance.onend = () => finish(status)
    window.speechSynthesis.speak(utterance)
    // 兜底：环境不触发事件时按文本长度估算超时
    setTimeout(() => finish(status), 5000 + text.length * 150)
  })
}

/** 停止朗读 */
export function stopSpeak() {
  if (ttsSupported()) {
    window.speechSynthesis.cancel()
  }
}
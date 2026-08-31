// 发音工具：App 原生环境（Capacitor）走系统 TTS 引擎，浏览器走 Web Speech API
//
// 为什么需要双路径：
// 1. Android WebView 不内置 TTS 引擎，speechSynthesis.speak() 永远无声；
//    iOS WKWebView 的 speechSynthesis 在部分系统版本上静默失效。
//    原生环境必须用 @capacitor-community/text-to-speech（Android 系统
//    TextToSpeech / iOS AVSpeechSynthesizer）。
// 2. 浏览器端 Chrome 的 getVoices() 为异步懒加载，冷启动可能数秒才就绪；
//    未就绪时朗读会静默无声（首次进入站点点发音没声音的根因），
//    需延长等待并持续轮询预热。

import { ElMessage } from 'element-plus'
import { Capacitor } from '@capacitor/core'
import { TextToSpeech } from '@capacitor-community/text-to-speech'

const isNative = typeof window !== 'undefined' && !!window.Capacitor && Capacitor.isNativePlatform()

let cachedVoices = []
const warnedLangs = new Set()
let nativeFailedWarned = false
// 朗读中的 utterance 强引用（防 GC：避免被提前回收导致事件丢失、朗读中断）
let activeUtterance = null

/** 是否支持语音合成（原生插件或 Web Speech API） */
export function ttsSupported() {
  if (isNative) return true
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/**
 * 预热语音列表（仅浏览器环境有意义）：
 * Chrome 冷启动时 voices 可能延迟数秒，启动后持续轮询直到就绪。
 */
export function initVoices() {
  if (isNative || !ttsSupported()) return
  const load = () => {
    const voices = window.speechSynthesis.getVoices()
    if (voices.length) {
      cachedVoices = voices
    }
  }
  load()
  window.speechSynthesis.onvoiceschanged = load
  // 冷启动预热：最多轮询 5 秒，voices 就绪即停
  let tries = 0
  const warm = setInterval(() => {
    load()
    if (cachedVoices.length || ++tries > 25) {
      clearInterval(warm)
    }
  }, 200)
}

/**
 * 等待语音列表就绪：voices 一到立即返回，最长等 maxMs。
 * 修复冷启动首点无声：旧实现只等 500ms，Chrome 首次访问常超时。
 */
function waitVoices(maxMs = 3000) {
  return new Promise((resolve) => {
    if (cachedVoices.length) {
      resolve()
      return
    }
    const synth = window.speechSynthesis
    const start = Date.now()
    const timer = setInterval(() => {
      const voices = synth.getVoices()
      if (voices.length) {
        cachedVoices = voices
      }
      if (cachedVoices.length || Date.now() - start > maxMs) {
        clearInterval(timer)
        resolve()
      }
    }, 60)
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
    message: `系统未安装${name}语音包，已用系统默认语音朗读。如需标准发音，请在系统语音设置中添加${name}语音。`
  })
}

/** 原生 TTS 朗读（Android 系统 TextToSpeech / iOS AVSpeechSynthesizer） */
async function nativeSpeak(text, lang, rate) {
  try {
    // category=playback：iOS 下不受静音开关影响，朗读类学习场景需要出声
    await TextToSpeech.speak({
      text,
      lang,
      rate: rate || 1,
      pitch: 1.0,
      volume: 1.0,
      category: 'playback'
    })
    return 'ok'
  } catch (e) {
    // 极端情况：设备无 TTS 引擎（如未装 Google 语音服务的 ROM）
    if (!nativeFailedWarned) {
      nativeFailedWarned = true
      ElMessage({
        type: 'warning',
        duration: 6000,
        message: '设备语音引擎不可用，请在系统设置中检查/安装文字转语音（TTS）引擎。'
      })
    }
    return 'failed'
  }
}

/** 浏览器 Web Speech API 朗读 */
async function webSpeak(text, lang, rate) {
  await waitVoices()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.rate = rate
  const voice = pickVoice(lang)
  if (voice) {
    // 找到匹配语音：标准朗读
    utterance.voice = voice
    utterance.lang = voice.lang
  } else {
    // 无匹配语音包时若仍设置目标语言（如 en-US），Chrome 会因找不到
    // 对应语音而静默跳过，必须显式改用系统默认语音兜底朗读。
    const fallback =
      cachedVoices.find((v) => v.default) ||
      cachedVoices[0] ||
      window.speechSynthesis.getVoices()[0]
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
    // 防 GC：朗读期间保持 utterance 引用，避免被提前回收导致事件丢失
    activeUtterance = utterance
    const finish = (value) => {
      if (!settled) {
        settled = true
        activeUtterance = null
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

/**
 * 朗读文本
 * @returns {Promise<'ok'|'fallback'|'failed'>}
 *   ok       找到匹配语音，标准朗读
 *   fallback 无匹配语音，由浏览器默认语音兜底
 *   failed   不支持或朗读失败
 * Promise 在朗读结束后 resolve（对话场景依赖此节奏推进回合）
 */
export async function speak(text, lang, rate = 0.9) {
  if (!text) {
    return 'failed'
  }
  if (isNative) {
    return nativeSpeak(text, lang, rate)
  }
  if (!ttsSupported()) {
    return 'failed'
  }
  return webSpeak(text, lang, rate)
}

/** 停止朗读 */
export function stopSpeak() {
  if (isNative) {
    TextToSpeech.stop().catch(() => {})
    return
  }
  if (ttsSupported()) {
    window.speechSynthesis.cancel()
  }
}

// 发音工具：App 原生环境（Capacitor）走系统 TTS 引擎，浏览器走 Web Speech API
//
// 原生路径的三个已知坑（v1.0.3 修复）：
// 1. 国产 ROM 的默认 TTS 引擎常只有中文语音数据，读 en-US/ja-JP/ko-KR 时
//    插件直接 reject("This language is not supported.")——加入语言回退链：
//    精确匹配 → 同语言任意变体（en-GB/en-IN…）→ 引擎默认语音，保证出声。
// 2. TTS 引擎初始化是异步的，App 冷启动后立即点发音会得到 unavailable
//    ——speak 前轮询等待引擎就绪（最多 3s，只等一次）。
// 3. 引擎偶发不回调 onDone/onError 导致 Promise 永久挂起——加超时保护。
//
// 浏览器端 Chrome 的 getVoices() 为异步懒加载，冷启动可能数秒才就绪；
// 未就绪时朗读会静默无声（首次进入站点点发音没声音的根因）。

import { ElMessage } from 'element-plus'
import { Capacitor } from '@capacitor/core'
import { TextToSpeech } from '@capacitor-community/text-to-speech'

/** 语言代码归一化：en_US / en-us → en-us，便于比较 */
function normLang(lang) {
  return String(lang || '').toLowerCase().replace(/_/g, '-')
}

/** 原生环境检测：每次调用时动态判断（避免模块加载时序问题） */
function checkNative() {
  return !!(
    typeof window !== 'undefined' &&
    window.Capacitor &&
    Capacitor.isNativePlatform &&
    Capacitor.isNativePlatform()
  )
}

let cachedVoices = []
const warnedLangs = new Set()
let engineLanguages = null // 原生引擎支持的语言列表（成功获取后缓存）
// 朗读中的 utterance 强引用（防 GC：避免被提前回收导致事件丢失、朗读中断）
let activeUtterance = null

/** 是否支持语音合成（原生插件或 Web Speech API） */
export function ttsSupported() {
  if (checkNative()) return true
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/**
 * 预热语音列表（仅浏览器环境有意义）：
 * Chrome 冷启动时 voices 可能延迟数秒，启动后持续轮询直到就绪。
 */
export function initVoices() {
  if (checkNative() || !ttsSupported()) return
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
function warnMissingOnce(lang, tip) {
  const key = `${lang}|${tip ? 'tip' : ''}`
  if (warnedLangs.has(key)) return
  warnedLangs.add(key)
  const names = { 'en-US': '英语', 'ja-JP': '日语', 'ko-KR': '韩语' }
  const name = names[lang] || lang
  if (tip) {
    // 原生路径：给出可操作的安装指引
    ElMessage({
      type: 'warning',
      duration: 8000,
      message: `当前设备缺少${name}语音包，已用其他可用语音朗读。如需标准发音：系统设置 → 语言和输入法 → 文字转语音（TTS）→ 安装/下载${name}语音数据；另请确认媒体音量已打开。`
    })
  } else {
    ElMessage({
      type: 'warning',
      duration: 6000,
      message: `系统未安装${name}语音包，已用系统默认语音朗读。如需标准发音，请在系统语音设置中添加${name}语音。`
    })
  }
}

/**
 * 等待原生 TTS 引擎就绪并返回其支持的语言列表。
 * 引擎初始化是异步的，冷启动后立即 speak 会得到 unavailable；
 * 首次调用最多轮询 3s，成功后缓存（只等一次）。
 * @returns {Promise<string[]|null>} null 表示引擎不可用
 */
async function waitNativeEngine(maxMs = 3000) {
  if (engineLanguages) return engineLanguages
  const start = Date.now()
  let lastErr = null
  while (Date.now() - start < maxMs) {
    try {
      const { languages } = await TextToSpeech.getSupportedLanguages()
      if (languages && languages.length) {
        engineLanguages = languages
        return languages
      }
    } catch (e) {
      lastErr = e
    }
    await new Promise((r) => setTimeout(r, 200))
  }
  // 慢引擎兜底：再试最后一次（可能恰好在窗口边界初始化完成）
  try {
    const { languages } = await TextToSpeech.getSupportedLanguages()
    if (languages && languages.length) {
      engineLanguages = languages
      return languages
    }
  } catch (e) {
    lastErr = e
  }
  console.warn('[TTS] native engine not ready', lastErr)
  return null
}

/**
 * 从引擎支持的语言中挑选最合适的朗读语言（回退链）：
 * 1. 精确匹配（en-us == en-us）
 * 2. 前缀匹配（en-us 请求 → 引擎有 en-gb 也接受，返回引擎实际标签）
 * 3. 都没有 → null（交给调用方降级）
 * @returns {string|null} 引擎实际支持的语言标签
 */
function pickNativeLang(requested, supported) {
  const set = supported.map(normLang)
  const want = normLang(requested)
  // 1. 精确
  const idx = set.indexOf(want)
  if (idx >= 0) return supported[idx]
  // 2. 前缀（同语言任意地区变体）
  const prefix = want.split('-')[0]
  for (let i = 0; i < set.length; i++) {
    if (set[i].split('-')[0] === prefix) return supported[i]
  }
  // 3. 兜底：引擎默认（取第一个，通常是设备语言）
  return null
}

/** 带超时的 Promise 包装：引擎不回调时按超时处理，避免流程挂起 */
function withTimeout(promise, ms) {
  return Promise.race([
    promise,
    new Promise((resolve) => setTimeout(() => resolve('timeout'), ms))
  ])
}

/**
 * 原生 TTS 朗读（Android 系统 TextToSpeech / iOS AVSpeechSynthesizer）
 * @returns {Promise<'ok'|'fallback'|'failed'>}
 */
async function nativeSpeak(text, lang, rate) {
  // 1. 等引擎就绪（冷启动竞态修复）
  const supported = await waitNativeEngine()
  if (!supported) {
    warnNativeError('引擎不可用')
    return 'failed'
  }

  // 2. 语言回退链
  const useLang = pickNativeLang(lang, supported)
  const isFallback = useLang === null
  const finalLang = useLang || supported[0] // 引擎默认语音，保证出声

  // 3. 朗读（超时按完成处理，不阻塞对话流程）
  try {
    await withTimeout(
      TextToSpeech.speak({
        text,
        lang: finalLang,
        rate: rate || 1,
        pitch: 1.0,
        volume: 1.0,
        category: 'playback'
      }),
      5000 + text.length * 200
    )
    if (isFallback) {
      warnMissingOnce(lang, true)
    }
    return isFallback ? 'fallback' : 'ok'
  } catch (e) {
    const msg = String((e && e.message) || e || '')
    console.warn('[TTS] native speak failed:', msg)
    // 语言不支持：拒绝后改用引擎默认语音重试一次（保证出声）
    if (msg.includes('not supported')) {
      try {
        await withTimeout(
          TextToSpeech.speak({
            text,
            lang: supported[0],
            rate: rate || 1,
            pitch: 1.0,
            volume: 1.0,
            category: 'playback'
          }),
          5000 + text.length * 200
        )
        warnMissingOnce(lang, true)
        return 'fallback'
      } catch (e2) {
        // 继续走浏览器兜底
      }
    }
    // 4. 最终兜底：Android WebView 的 speechSynthesis 部分设备可用
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      const r = await webSpeak(text, lang, rate)
      if (r !== 'failed') return r
    }
    warnNativeError(msg)
    return 'failed'
  }
}

/** 原生路径失败的提示（细分原因，方便定位） */
function warnNativeError(detail) {
  ElMessage({
    type: 'warning',
    duration: 8000,
    message: `语音朗读失败（${detail}）。请检查：① 媒体音量已打开；② 系统设置 → 语言和输入法 → 文字转语音（TTS）已启用引擎并安装语音数据。`
  })
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
 *   fallback 无匹配语音，由系统可用语音兜底
 *   failed   不支持或朗读失败
 * Promise 在朗读结束后 resolve（对话场景依赖此节奏推进回合）
 */
export async function speak(text, lang, rate = 0.9) {
  if (!text) {
    return 'failed'
  }
  if (checkNative()) {
    return nativeSpeak(text, lang, rate)
  }
  if (!ttsSupported()) {
    return 'failed'
  }
  return webSpeak(text, lang, rate)
}

/** 停止朗读 */
export function stopSpeak() {
  if (checkNative()) {
    TextToSpeech.stop().catch(() => {})
    return
  }
  if (ttsSupported()) {
    window.speechSynthesis.cancel()
  }
}

// 语音合成：三级降级链，目标是不管什么语种都能出声
//
// 【级别 1】微信同声传译插件（官方）
//   - 只支持 zh_CN / en_US。旧实现把日/韩/法等统统回退成 en_US，
//     等于用英语朗读日语——这是"能出声但完全不对"的假成功，这里改为直接跳过。
//   - 必须在 app.json 的 plugins 中声明，否则 requirePlugin 抛错被吞掉，
//     plugin 恒为 null（这正是此前"提示需要添加插件"却怎么加都没用的根因）。
//
// 【级别 2】后端 TTS 代理 /api/tts
//   - 覆盖多语种（法/西/阿/日/韩…）以及粤语等方言音色，取决于后端接了哪家服务。
//   - 后端未配置任何 TTS 服务时返回 501，本地熔断 5 分钟，避免每次点发音都白等一次网络往返。
//
// 【级别 3】明确提示，不再假装成功。

const { BASE_URL } = require('./config')

// ---------------------------------------------------------------- 插件

let plugin = null
try {
  plugin = requirePlugin('WechatSI')
} catch (e) {
  plugin = null
}

/** 同声传译 TTS 实际支持的全部语种，其他语种请勿塞给它 */
const SI_LANGS = { zh: 'zh_CN', en: 'en_US' }

// ---------------------------------------------------------------- 音频

let audioCtx = null

/** 复用同一个 InnerAudioContext：每次 speak 新建会导致实例泄漏、iOS 上偶发无声 */
function getAudio() {
  if (!audioCtx) {
    audioCtx = wx.createInnerAudioContext()
    audioCtx.obeyMuteSwitch = false // iOS 静音键下仍发声（学习类 App 期望行为）
  }
  return audioCtx
}

/** 播放远程/本地音频，Promise 在播完或出错时 resolve */
function playSrc(src) {
  return new Promise((resolve) => {
    const audio = getAudio()
    let settled = false
    const finish = (ok) => {
      if (settled) return
      settled = true
      audio.offEnded(onEnd)
      audio.offError(onErr)
      resolve(ok)
    }
    const onEnd = () => finish(true)
    const onErr = (e) => {
      console.warn('[TTS] audio error', e && e.errMsg)
      finish(false)
    }
    audio.onEnded(onEnd)
    audio.onError(onErr)
    // 连续点发音时先停掉上一条，否则新的会被旧的覆盖状态干扰
    try {
      audio.stop()
    } catch (e) {}
    audio.src = src
    audio.play()
  })
}

// ---------------------------------------------------------------- 后端 TTS 熔断

const BACKEND_COOLDOWN_MS = 5 * 60 * 1000
let backendDeadUntil = 0

function backendAvailable() {
  return Date.now() >= backendDeadUntil
}

function markBackendDead() {
  backendDeadUntil = Date.now() + BACKEND_COOLDOWN_MS
}

/**
 * 后端 TTS 代理
 * @returns {Promise<{ok:boolean, approximate:boolean}>}
 *   approximate 为 true 表示并非该语种的真实发音（方言用普通话近似）
 */
function backendSpeak(text, langCode, rate) {
  return new Promise((resolve) => {
    const url =
      BASE_URL +
      '/tts?text=' +
      encodeURIComponent(String(text || '')) +
      '&lang=' +
      encodeURIComponent(langCode || '') +
      '&rate=' +
      encodeURIComponent(rate == null ? 1 : rate)

    // 先用 request 探测：能拿到 200 说明后端接了 TTS 服务，再交给音频组件播放。
    // 直接把 URL 塞给 InnerAudioContext 的话，失败只有 onError 拿不到状态码，
    // 无法区分"后端没配 TTS"和"网络抖动"，也就没法熔断。
    wx.request({
      url,
      method: 'GET',
      responseType: 'arraybuffer',
      success(res) {
        if (res.statusCode === 200 && res.data) {
          // 微信把响应头 key 全转小写
          const approximate = (res.header || {})['x-tts-approximate'] === '1'
          // 落盘后播放：innerAudioContext 对二进制流支持不稳，写文件最可靠
          const fs = wx.getFileSystemManager()
          const file = `${wx.env.USER_DATA_PATH}/tts_${Date.now()}.mp3`
          try {
            fs.writeFileSync(file, res.data, 'binary')
            playSrc(file).then((ok) => resolve({ ok, approximate }))
          } catch (e) {
            console.warn('[TTS] write file failed', e)
            resolve({ ok: false, approximate })
          }
          return
        }
        if (res.statusCode === 501) {
          // 后端明确告知该语种没有发音源 → 熔断，别再问了
          markBackendDead()
        }
        resolve({ ok: false, approximate: false })
      },
      fail() {
        resolve({ ok: false, approximate: false })
      }
    })
  })
}

// ---------------------------------------------------------------- 提示

let lastTipAt = 0

/** 同一原因的提示 3 秒内不重复弹，避免连点发音刷屏 */
function tipOnce(title) {
  const now = Date.now()
  if (now - lastTipAt < 3000) return
  lastTipAt = now
  wx.showToast({ title, icon: 'none', duration: 2500 })
}

// ---------------------------------------------------------------- 对外

/** 插件是否可用（仅供页面判断是否展示发音入口，不保证所有语种都行） */
function ttsAvailable() {
  return !!plugin
}

/**
 * 朗读文本
 * @param {string} text 要朗读的内容
 * @param {string} langCode 语种：'en' / 'en-US' / 'zh-CN' 均可
 * @param {number} [rate] 语速，0.6 为慢速
 * @returns {Promise<'ok'|'fallback'|'failed'>}
 *   ok       命中理想发音源
 *   fallback 用了替代发音源（如同声传译不可用时的后端代理）
 *   failed   彻底发不出声
 */
async function speak(text, langCode, rate) {
  let content = String(text || '').trim()
  if (!content) return 'failed'
  if (content.length > 200) {
    // 第三方 TTS 按字符计费且长文本延迟明显，超长直接截断而非拒绝
    content = content.slice(0, 200)
  }

  // 注意：base 是主语言标签（zh-yue → zh），用于判断插件是否支持；
  // full 保留完整标签（zh-yue），必须原样传给后端，否则方言会被当成普通话。
  const full = String(langCode || '')
    .toLowerCase()
    .replace(/_/g, '-')
  const base = full.split('-')[0]

  // 级别 1：同声传译（仅中英，发音质量最好）
  const siLang = SI_LANGS[base]
  if (plugin && siLang) {
    const ok = await siSpeak(content, siLang)
    if (ok) return 'ok'
    // 插件失败不打扰用户，直接落后端
  }

  // 级别 2：后端 TTS 代理（多语种 + 方言）
  if (backendAvailable()) {
    const r = await backendSpeak(content, full, rate)
    if (r.ok) {
      if (r.approximate) {
        tipOnce('该方言发音开发中，当前为普通话发音')
      }
      return 'fallback'
    }
  }

  // 级别 3：明确告知
  tipOnce(plugin ? '该语种暂无可用发音源' : '发音功能未就绪，请联系开发者配置语音服务')
  return 'failed'
}

function siSpeak(content, siLang) {
  return new Promise((resolve) => {
    plugin.textToSpeech({
      lang: siLang,
      content,
      success(res) {
        if (!res || !res.filename) {
          resolve(false)
          return
        }
        playSrc(res.filename).then(resolve)
      },
      fail(err) {
        console.warn('[TTS] WechatSI failed', err)
        resolve(false)
      }
    })
  })
}

/** 停止朗读 */
function stopSpeak() {
  if (audioCtx) {
    try {
      audioCtx.stop()
    } catch (e) {}
  }
}

module.exports = { speak, stopSpeak, ttsAvailable }

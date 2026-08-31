// 语音合成：基于「微信同声传译」插件（官方）
// 插件需在小程序后台「设置-第三方设置-插件管理」搜索并添加「微信同声传译」，
// 未添加时调用会给出提示并优雅降级（不影响其他功能）
let plugin = null
try {
  plugin = requirePlugin('WechatSI')
} catch (e) {
  plugin = null
}

function ttsAvailable() {
  return !!plugin
}

/** 同声传译目前支持 zh_CN / en_US，其他语种回退英语 */
function ttsLang(langCode) {
  if (!langCode) return 'en_US'
  const prefix = String(langCode).toLowerCase().split('-')[0]
  return prefix === 'zh' ? 'zh_CN' : 'en_US'
}

/** 朗读文本，返回 Promise<boolean> */
function speak(text, langCode) {
  return new Promise((resolve) => {
    if (!plugin) {
      wx.showToast({ title: '发音需在小程序后台添加「微信同声传译」插件', icon: 'none', duration: 2500 })
      resolve(false)
      return
    }
    plugin.textToSpeech({
      lang: ttsLang(langCode),
      content: String(text || ''),
      success(res) {
        const audio = wx.createInnerAudioContext()
        audio.src = res.filename
        audio.onEnded(() => resolve(true))
        audio.onError(() => resolve(false))
        audio.play()
      },
      fail() {
        wx.showToast({ title: '语音合成失败', icon: 'none' })
        resolve(false)
      }
    })
  })
}

module.exports = { speak, ttsAvailable }
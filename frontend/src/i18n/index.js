import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import zhTW from './locales/zh-TW'
import en from './locales/en'
import ja from './locales/ja'
import ko from './locales/ko'
import es from './locales/es'
import fr from './locales/fr'
import vi from './locales/vi'
import th from './locales/th'

const LANG_KEY = 'lingoplay-lang'

/** 界面支持的全部语言（含语言切换器展示信息） */
export const SUPPORTED_LANGS = [
  { code: 'zh-CN', name: '简体中文', flag: '🇨🇳' },
  { code: 'zh-TW', name: '繁體中文', flag: '🇹🇼' },
  { code: 'en', name: 'English', flag: '🇺🇸' },
  { code: 'ja', name: '日本語', flag: '🇯🇵' },
  { code: 'ko', name: '한국어', flag: '🇰🇷' },
  { code: 'es', name: 'Español', flag: '🇪🇸' },
  { code: 'fr', name: 'Français', flag: '🇫🇷' },
  { code: 'vi', name: 'Tiếng Việt', flag: '🇻🇳' },
  { code: 'th', name: 'ไทย', flag: '🇹🇭' }
]

/** 浏览器语言 → 支持语种的最近匹配 */
function resolveInitial() {
  try {
    const saved = localStorage.getItem(LANG_KEY)
    if (saved && SUPPORTED_LANGS.some((l) => l.code === saved)) {
      return saved
    }
  } catch (e) { /* 隐私模式下读取失败，走浏览器语言 */ }
  const nav = (navigator.language || 'zh-CN').toLowerCase()
  if (nav.startsWith('zh-tw') || nav.startsWith('zh-hk') || nav.startsWith('zh-hant')) return 'zh-TW'
  if (nav.startsWith('zh')) return 'zh-CN'
  for (const l of SUPPORTED_LANGS) {
    if (l.code !== 'zh-CN' && l.code !== 'zh-TW' && nav.startsWith(l.code)) return l.code
  }
  return 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  locale: resolveInitial(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'zh-TW': zhTW,
    en, ja, ko, es, fr, vi, th
  }
})

/** 切换界面语言并持久化（App / Web 共用同一份构建，设置天然互通） */
export function setLocale(code) {
  if (!SUPPORTED_LANGS.some((l) => l.code === code)) return
  i18n.global.locale.value = code
  try {
    localStorage.setItem(LANG_KEY, code)
  } catch (e) { /* ignore */ }
  document.documentElement.setAttribute('lang', code)
}

export function currentLocale() {
  return i18n.global.locale.value
}

export default i18n

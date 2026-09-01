/**
 * App 检查更新工具
 *
 * - 原生 App（Capacitor 打包）通过 @capacitor/app 读取真实原生版本号，
 *   与后端 /api/app/latest 下发的最新版本对比；
 * - 浏览器环境页面始终最新，仅提示无更新；
 * - Android 有新版时跳转系统浏览器下载 APK；iOS 提示下载工程包/使用网页版。
 */
import { App as CapApp } from '@capacitor/app'
import { Browser } from '@capacitor/browser'
import { Capacitor } from '@capacitor/core'
import { ElMessageBox, ElMessage } from 'element-plus'
import { appApi } from '../api'
import { NATIVE_API_BASE } from '../api/http'

/** Web 环境展示用版本号（与 Android versionName 保持同步） */
export const APP_VERSION = '1.0.5'

/** 忽略提醒的版本号存储键（用户点“暂不更新”后，该版本不再自动弹窗） */
const IGNORED_KEY = 'lingoplay_ignored_version'

export const isNativeApp =
  typeof window !== 'undefined' &&
  !!window.Capacitor &&
  window.Capacitor.isNativePlatform()

/** 下载地址基座：原生环境直连线上站点，Web 环境同源 */
const DL_BASE = isNativeApp ? NATIVE_API_BASE.replace(/\/api\/?$/, '') : ''

/** 语义化版本比较：a<b 返回 -1，相等 0，a>b 返回 1（支持 1.0.4 / 1.0.10） */
export function compareVersion(a, b) {
  const pa = String(a || '').split('.').map((n) => parseInt(n, 10) || 0)
  const pb = String(b || '').split('.').map((n) => parseInt(n, 10) || 0)
  const len = Math.max(pa.length, pb.length)
  for (let i = 0; i < len; i++) {
    const x = pa[i] || 0
    const y = pb[i] || 0
    if (x !== y) return x < y ? -1 : 1
  }
  return 0
}

/** 当前运行的版本号：原生取 App 真实 versionName，Web 用常量 */
export async function getCurrentVersion() {
  if (isNativeApp) {
    try {
      const info = await CapApp.getInfo()
      return info.version || APP_VERSION
    } catch (e) {
      return APP_VERSION
    }
  }
  return APP_VERSION
}

/** 获取服务端最新版本信息 */
export async function fetchLatest() {
  return await appApi.latest()
}

/** 当前平台：'android' | 'ios' | 'web' */
export function appPlatform() {
  return Capacitor.getPlatform()
}

/** 按平台拼出安装包下载地址 */
export function downloadUrl(latest) {
  if (!latest) return ''
  const file = appPlatform() === 'ios' ? latest.iosFile : latest.androidFile
  return file ? `${DL_BASE}/downloads/${file}` : ''
}

/** 打开下载页：原生用内置浏览器（Chrome Custom Tab / SFSafariViewController），Web 新标签页 */
export async function openDownload(url) {
  if (!url) return
  if (isNativeApp) {
    try {
      await Browser.open({ url, windowName: '_system' })
      return
    } catch (e) {
      // 内置浏览器打开失败时退回 window.open
    }
  }
  window.open(url, '_blank')
}

/**
 * 手动检查更新（个人中心按钮触发）：返回是否发现新版本
 */
export async function checkUpdateManually() {
  const [latest, current] = await Promise.all([fetchLatest(), getCurrentVersion()])
  const hasUpdate = compareVersion(current, latest.versionName) < 0
  if (!hasUpdate) {
    ElMessage.success(`当前已是最新版本 v${current}`)
    return false
  }
  showUpdateDialog(latest, current, { manual: true })
  return true
}

/**
 * 启动时自动检查（仅原生 App）：静默失败，尊重用户“暂不更新”的选择
 */
export async function checkUpdateOnLaunch() {
  if (!isNativeApp) return
  try {
    const [latest, current] = await Promise.all([fetchLatest(), getCurrentVersion()])
    if (compareVersion(current, latest.versionName) >= 0) return
    // 用户已选择忽略该版本，则不再自动提醒（手动检查不受影响）
    if (localStorage.getItem(IGNORED_KEY) === latest.versionName && !latest.forceUpdate) return
    showUpdateDialog(latest, current, { manual: false })
  } catch (e) {
    // 网络异常等情况静默跳过，不打扰启动
  }
}

/** 更新弹窗：展示新旧版本与更新说明，提供“立即更新”跳转下载 */
function showUpdateDialog(latest, current, { manual }) {
  const url = downloadUrl(latest)
  const isIOS = appPlatform() === 'ios'
  const notes = (latest.updateNotes || '').split('\n').filter(Boolean)
  const content = `
    <div class="upd-dialog">
      <div class="upd-row"><span class="upd-label">当前版本</span><span>v${current}</span></div>
      <div class="upd-row"><span class="upd-label">最新版本</span><span class="upd-new">v${latest.versionName}</span></div>
      ${notes.length ? `<div class="upd-notes"><div class="upd-notes-title">更新内容</div>${notes.map((n) => `<div class="upd-note-item">${n}</div>`).join('')}</div>` : ''}
      ${isIOS ? '<div class="upd-tip">iOS 端更新需下载最新工程包，用 Xcode 重新构建安装</div>' : ''}
    </div>`
  ElMessageBox.confirm(content, `发现新版本 v${latest.versionName}`, {
    dangerouslyUseHTMLString: true,
    confirmButtonText: url ? '立即更新' : '前往官网',
    cancelButtonText: '暂不更新',
    showCancelButton: !latest.forceUpdate,
    closeOnClickModal: false,
    closeOnPressEscape: !latest.forceUpdate,
    showClose: !latest.forceUpdate,
    type: 'info',
    autofocus: false
  })
    .then(async () => {
      await openDownload(url || `${DL_BASE}/`)
    })
    .catch(() => {
      // 用户选择暂不更新：记住该版本，启动时不再自动提醒（手动检查仍会提示）
      if (!manual && !latest.forceUpdate) {
        localStorage.setItem(IGNORED_KEY, latest.versionName)
      }
    })
}

<template>
  <div class="page-container">
    <h2 class="page-title">下载 LingoPlay App</h2>
    <p class="page-subtitle">
      移动端安装包由本站当前代码构建（Capacitor 原生壳 + 同一份前端产物），与网页版功能完全一致
    </p>

    <div class="dl-grid">
      <!-- Android 卡片 -->
      <div class="dl-card hover-card">
        <div class="dl-card-head">
          <span class="dl-logo dl-logo-android">🤖</span>
          <div class="dl-head-text">
            <div class="dl-name">LingoPlay for Android</div>
            <div class="dl-tags">
              <el-tag size="small" effect="plain">v{{ APP_VERSION }}</el-tag>
              <el-tag size="small" effect="plain" type="success">Android 7.0+</el-tag>
              <el-tag size="small" effect="plain" type="info">APK 直装</el-tag>
            </div>
          </div>
        </div>

        <div class="dl-meta">
          <div class="dl-meta-row">
            <span class="dl-meta-label">安装包</span>
            <span class="dl-meta-value">{{ APK_FILE }}</span>
          </div>
          <div class="dl-meta-row">
            <span class="dl-meta-label">大小</span>
            <span class="dl-meta-value">
              {{ apk.ready ? formatSize(apk.size) : '检测中…' }}
            </span>
          </div>
          <div class="dl-meta-row">
            <span class="dl-meta-label">包名</span>
            <span class="dl-meta-value">com.lingoplay.app</span>
          </div>
        </div>

        <button class="dl-btn" :disabled="!apk.ready" @click="download(APK_URL)">
          <span>{{ apk.ready ? '下载 Android 安装包' : '安装包暂未就绪' }}</span>
          <span class="dl-btn-arrow">↓</span>
        </button>
      </div>

      <!-- iOS 卡片 -->
      <div class="dl-card hover-card">
        <div class="dl-card-head">
          <span class="dl-logo dl-logo-ios"></span>
          <div class="dl-head-text">
            <div class="dl-name">LingoPlay for iOS</div>
            <div class="dl-tags">
              <el-tag size="small" effect="plain">v{{ APP_VERSION }}</el-tag>
              <el-tag size="small" effect="plain" type="success">iOS 15+</el-tag>
              <el-tag size="small" effect="plain" type="warning">Xcode 工程包</el-tag>
            </div>
          </div>
        </div>

        <div class="dl-meta">
          <div class="dl-meta-row">
            <span class="dl-meta-label">工程包</span>
            <span class="dl-meta-value">{{ IOS_FILE }}</span>
          </div>
          <div class="dl-meta-row">
            <span class="dl-meta-label">大小</span>
            <span class="dl-meta-value">
              {{ ios.ready ? formatSize(ios.size) : '检测中…' }}
            </span>
          </div>
          <div class="dl-meta-row">
            <span class="dl-meta-label">Bundle ID</span>
            <span class="dl-meta-value">com.lingoplay.app</span>
          </div>
        </div>

        <button class="dl-btn dl-btn-ios" :disabled="!ios.ready" @click="download(IOS_URL)">
          <span>{{ ios.ready ? '下载 iOS 工程包' : '工程包暂未就绪' }}</span>
          <span class="dl-btn-arrow">↓</span>
        </button>
      </div>
    </div>

    <!-- 安装指南 -->
    <div class="guide-grid">
      <div class="guide-card hover-card">
        <div class="guide-title">📦 Android 安装步骤</div>
        <ol class="guide-steps">
          <li>点击上方按钮，下载 <b>LingoPlay</b> APK 安装包</li>
          <li>打开系统通知栏，点击「下载完成」的安装包（或到「文件管理 → Downloads」找到它）</li>
          <li>首次安装会提示「未知来源应用」，选择<b>允许本次安装</b>即可</li>
          <li>安装完成后桌面出现 🛰️ LingoPlay 图标，打开即玩</li>
        </ol>
        <div class="guide-tip">App 内已内置与网页版相同的最新前端界面，登录账号后学习进度实时同步。</div>
      </div>

      <div class="guide-card hover-card">
        <div class="guide-title">🍎 iOS 构建/安装步骤</div>
        <ol class="guide-steps">
          <li>下载上方 iOS 工程包（zip，已含与当前网页版同步的前端资源）</li>
          <li>解压后得到 <b>ios/</b> 目录，在 macOS 的 Xcode 中打开 <b>App.xcodeproj</b></li>
          <li>在 Signing &amp; Capabilities 中选择你自己的 Apple 开发者证书（Bundle ID 可改为你的）</li>
          <li>连接 iPhone 后点击 Run 运行；或通过 Product → Archive 导出 IPA / 上传 TestFlight 分发</li>
        </ol>
        <div class="guide-tip">
          iOS 应用必须经 Apple 证书签名后才能安装，故本站提供工程包而非 IPA；工程与网页版由同一份代码构建。
        </div>
      </div>
    </div>

    <div class="dl-note">
      <el-icon><InfoFilled /></el-icon>
      两个安装包均构建自本仓库当前版本的 Android / iOS 工程（versionName {{ APP_VERSION }}），
      更新站点代码后重新打包即可获得最新版 App。
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { NATIVE_API_BASE } from '../api/http'

const APP_VERSION = '1.0.1'
const APK_FILE = 'LingoPlay-v1.0.1-android.apk'
const IOS_FILE = 'LingoPlay-v1.0.1-ios-project.zip'

// 原生 App 内访问时无同源服务，下载地址指向线上后端；Web 环境走同源相对路径
const isNative = typeof window !== 'undefined' && window.Capacitor && window.Capacitor.isNativePlatform()
const DL_BASE = isNative ? NATIVE_API_BASE.replace(/\/api\/?$/, '') : ''
const APK_URL = `${DL_BASE}/downloads/${APK_FILE}`
const IOS_URL = `${DL_BASE}/downloads/${IOS_FILE}`

const apk = reactive({ ready: false, size: 0 })
const ios = reactive({ ready: false, size: 0 })

async function probe(url) {
  try {
    const res = await fetch(url, { method: 'HEAD' })
    if (!res.ok) return null
    const len = res.headers.get('content-length')
    return { ready: true, size: len ? Number(len) : 0 }
  } catch (e) {
    return null
  }
}

function formatSize(bytes) {
  if (!bytes) return '—'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function download(url) {
  const a = document.createElement('a')
  a.href = url
  a.download = url.substring(url.lastIndexOf('/') + 1)
  document.body.appendChild(a)
  a.click()
  a.remove()
  ElMessage.success('开始下载，请留意浏览器下载列表')
}

onMounted(async () => {
  const [apkInfo, iosInfo] = await Promise.all([probe(APK_URL), probe(IOS_URL)])
  Object.assign(apk, apkInfo || { ready: false, size: 0 })
  Object.assign(ios, iosInfo || { ready: false, size: 0 })
})
</script>

<style scoped>
.dl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 18px;
  margin-bottom: 22px;
}

.dl-card {
  border: 1px solid var(--ll-border);
  border-radius: 18px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dl-card-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dl-logo {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  flex-shrink: 0;
}

.dl-logo-android {
  background: linear-gradient(135deg, rgba(61, 220, 132, 0.18), rgba(34, 211, 238, 0.10));
  border: 1px solid rgba(61, 220, 132, 0.35);
}

.dl-logo-ios {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.16), rgba(160, 180, 255, 0.10));
  border: 1px solid rgba(255, 255, 255, 0.30);
}

.dl-head-text {
  min-width: 0;
}

.dl-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--ll-text);
  margin-bottom: 8px;
}

.dl-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.dl-meta {
  border-top: 1px dashed rgba(120, 150, 255, 0.18);
  border-bottom: 1px dashed rgba(120, 150, 255, 0.18);
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dl-meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.dl-meta-label {
  color: var(--ll-text-muted);
  flex-shrink: 0;
}

.dl-meta-value {
  color: var(--ll-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dl-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 46px;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  font-family: inherit;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #04122e;
  background: linear-gradient(135deg, #4f7cff 0%, #22d3ee 100%);
  box-shadow: 0 6px 24px rgba(79, 124, 255, 0.35);
  transition: all 0.2s;
}

.dl-btn-ios {
  background: linear-gradient(135deg, #e2e8f0 0%, #a5b4fc 100%);
}

.dl-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.08);
}

.dl-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

.dl-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}

.dl-btn-arrow {
  font-size: 18px;
  font-weight: 800;
}

/* ---- 安装指南 ---- */
.guide-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 18px;
  margin-bottom: 22px;
}

.guide-card {
  border: 1px solid var(--ll-border);
  border-radius: 18px;
  padding: 22px 24px;
}

.guide-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--ll-text);
  margin-bottom: 14px;
}

.guide-steps {
  margin: 0;
  padding-left: 20px;
  color: var(--ll-text-muted);
  font-size: 13.5px;
  line-height: 2;
}

.guide-steps b {
  color: var(--ll-text);
}

.guide-tip {
  margin-top: 14px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(34, 211, 238, 0.07);
  border: 1px solid rgba(34, 211, 238, 0.2);
  color: var(--ll-text-muted);
  font-size: 12.5px;
  line-height: 1.7;
}

.dl-note {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ll-text-muted);
  font-size: 12.5px;
  line-height: 1.7;
}
</style>

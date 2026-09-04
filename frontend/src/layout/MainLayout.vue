<template>
  <div class="layout" :class="{ 'is-mobile': isMobile }">
    <!-- 桌面端：左侧固定导航 -->
    <aside v-if="!isMobile" class="aside">
      <div class="brand">
        <span class="brand-logo">🛰️</span>
        <div>
          <div class="brand-name">Lingo<span class="brand-hl">Play</span></div>
          <div class="brand-sub">{{ t('brand.slogan') }}</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in allMenus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="layout-right">
      <!-- 顶栏：移动端显示品牌名，桌面端显示问候语 -->
      <header class="header">
        <div v-if="isMobile" class="brand brand-mobile">
          <span class="brand-logo">🛰️</span>
          <div class="brand-name">Lingo<span class="brand-hl">Play</span></div>
        </div>
        <div v-else class="header-greeting">{{ greeting }}</div>
        <div class="header-actions">
          <!-- 界面语言切换 -->
          <el-dropdown @command="onLangCommand" trigger="click">
            <span class="lang-chip" :title="t('lang.label')">
              <el-icon><Globe /></el-icon>
              <span class="lang-code">{{ currentLang.code }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="l in SUPPORTED_LANGS"
                  :key="l.code"
                  :command="l.code"
                  :class="{ 'lang-active': l.code === currentLang.code }"
                >
                  <span class="lang-menu-item">
                    <span class="lang-menu-flag">{{ l.flag }}</span>{{ l.name }}
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown v-if="store.isLoggedIn" @command="onCommand">
            <span class="user-chip">
              <span class="avatar">{{ store.user ? store.user.avatar || '🙂' : '🙂' }}</span>
              <span class="nickname">{{ store.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">{{ t('nav.profile') }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>{{ t('nav.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- 游客：点击弹出登录/注册 -->
          <button v-else class="user-chip user-chip-guest" @click="store.requireLogin()">
            <span class="avatar">🙋</span>
            <span class="nickname">{{ t('nav.guest') }}</span>
          </button>
        </div>
      </header>

      <main class="main">
        <router-view />
      </main>
    </div>

    <!-- 全局登录/注册弹窗（游客触发受保护操作时弹出） -->
    <LoginDialog />

    <!-- 移动端：底部标签导航（5 项，覆盖全部主要入口） -->
    <nav v-if="isMobile" class="tabbar">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        class="tab-item"
        :class="{ active: activeMenu === tab.path }"
        @click="go(tab.path)"
      >
        <el-icon class="tab-icon"><component :is="tab.icon" /></el-icon>
        <span class="tab-label">{{ tab.label }}</span>
      </button>
      <button class="tab-item" :class="{ active: drawerVisible }" @click="drawerVisible = true">
        <el-icon class="tab-icon"><Menu /></el-icon>
        <span class="tab-label">{{ t('nav.more') }}</span>
      </button>
    </nav>

    <!-- 移动端：更多菜单抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      size="72%"
      :show-close="true"
      :with-header="false"
      class="more-drawer"
    >
      <div class="drawer-brand brand">
        <span class="brand-logo">🛰️</span>
        <div>
          <div class="brand-name">Lingo<span class="brand-hl">Play</span></div>
          <div class="brand-sub">{{ t('brand.slogan') }}</div>
        </div>
      </div>
      <div class="drawer-menu">
        <button
          v-for="item in moreMenus"
          :key="item.path"
          class="drawer-item"
          :class="{ active: activeMenu === item.path }"
          @click="goDrawer(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
        <div class="drawer-divider"></div>
        <!-- 移动端语言切换 -->
        <div class="drawer-lang">
          <span class="drawer-lang-label">{{ t('lang.label') }}</span>
          <div class="drawer-lang-chips">
            <button
              v-for="l in SUPPORTED_LANGS"
              :key="l.code"
              class="lang-chip-btn"
              :class="{ active: l.code === currentLang.code }"
              @click="onLangCommand(l.code)"
            >{{ l.flag }} {{ l.name }}</button>
          </div>
        </div>
        <div class="drawer-divider"></div>
        <button class="drawer-item" @click="goDrawer('/profile')">
          <el-icon><User /></el-icon>
          <span>{{ t('nav.profile') }}</span>
        </button>
        <button class="drawer-item danger" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>{{ t('nav.logout') }}</span>
        </button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '../store/user'
import { SUPPORTED_LANGS, setLocale, currentLocale } from '../i18n'
import { checkUpdateOnLaunch } from '../utils/updater'
import LoginDialog from '../components/LoginDialog.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const store = useUserStore()

const currentLang = computed(() => {
  const code = currentLocale()
  return SUPPORTED_LANGS.find((l) => l.code === code) || SUPPORTED_LANGS[0]
})

function onLangCommand(code) {
  setLocale(code)
}

// 响应式断点：≤768px 视为移动端（覆盖主流手机竖屏/小平板）
const mobileQuery = window.matchMedia('(max-width: 768px)')
const isMobile = ref(mobileQuery.matches)
function onMediaChange(e) {
  isMobile.value = e.matches
}
onMounted(() => {
  // 兼容旧内核（Android 7~9 部分机型）不支持 addEventListener 的情况
  if (mobileQuery.addEventListener) {
    mobileQuery.addEventListener('change', onMediaChange)
  } else if (mobileQuery.addListener) {
    mobileQuery.addListener(onMediaChange)
  }
  // App 启动自动检查更新（仅原生环境，静默失败，不打扰启动）
  setTimeout(() => {
    checkUpdateOnLaunch()
  }, 800)
})
onBeforeUnmount(() => {
  if (mobileQuery.removeEventListener) {
    mobileQuery.removeEventListener('change', onMediaChange)
  } else if (mobileQuery.removeListener) {
    mobileQuery.removeListener(onMediaChange)
  }
})

// 全部导航项（桌面侧边栏用），label 跟随界面语言
const allMenus = computed(() => [
  { path: '/', icon: 'Lightning', label: t('nav.dashboard') },
  { path: '/courses', icon: 'MapLocation', label: t('nav.courses') },
  { path: '/translate', icon: 'Promotion', label: t('nav.translate') },
  { path: '/recommend', icon: 'MagicStick', label: t('nav.recommend') },
  { path: '/progress', icon: 'DataLine', label: t('nav.progress') },
  { path: '/community', icon: 'ChatDotRound', label: t('nav.community') },
  { path: '/achievements', icon: 'Trophy', label: t('nav.achievements') },
  { path: '/vip', icon: 'GoldMedal', label: t('nav.vip') },
  { path: '/download', icon: 'Cellphone', label: t('nav.download') }
])

// 底部标签（高频入口）
const tabs = computed(() => [
  { path: '/', icon: 'Lightning', label: t('nav.dashboard') },
  { path: '/courses', icon: 'MapLocation', label: t('nav.courses') },
  { path: '/progress', icon: 'DataLine', label: t('nav.progress') },
  { path: '/community', icon: 'ChatDotRound', label: t('nav.community') }
])

// 抽屉中的次级入口
const moreMenus = computed(() => [
  { path: '/translate', icon: 'Promotion', label: t('nav.translate') },
  { path: '/recommend', icon: 'MagicStick', label: t('nav.recommend') },
  { path: '/vip', icon: 'GoldMedal', label: t('nav.vip') },
  { path: '/achievements', icon: 'Trophy', label: t('nav.achievements') },
  { path: '/download', icon: 'Cellphone', label: t('nav.download') }
])

const drawerVisible = ref(false)

const activeMenu = computed(() => {
  if (route.path.startsWith('/courses') || route.path.startsWith('/learn')) return '/courses'
  if (route.path.startsWith('/community')) return '/community'
  return route.path
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return t('greet.night')
  if (hour < 12) return t('greet.morning')
  if (hour < 18) return t('greet.afternoon')
  return t('greet.evening')
})

function go(path) {
  if (route.path !== path) {
    router.push(path)
  }
}

function goDrawer(path) {
  drawerVisible.value = false
  router.push(path)
}

function logout() {
  drawerVisible.value = false
  store.logout()
  router.push('/login')
}

function onCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
  display: flex;
}

/* ---------- 桌面端侧边栏 ---------- */
.aside {
  width: 216px;
  flex-shrink: 0;
  background: transparent;
  border-right: 1px solid rgba(120, 150, 255, 0.10);
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px 16px;
}

.brand-mobile {
  padding: 0;
  gap: 8px;
}

.brand-logo {
  font-size: 26px;
  filter: drop-shadow(0 0 8px rgba(34, 211, 238, 0.7));
}

.brand-mobile .brand-logo {
  font-size: 22px;
}

.brand-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--ll-text);
  letter-spacing: 0.5px;
}

.brand-hl {
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-sub {
  font-size: 11px;
  color: var(--ll-text-muted);
}

.menu {
  border-right: none;
  flex: 1;
  padding: 4px 10px;
}

.menu :deep(.el-menu-item) {
  color: var(--ll-text-muted);
  border-radius: 10px;
  margin-bottom: 4px;
  height: 44px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(79, 124, 255, 0.12);
  color: var(--ll-text);
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(79, 124, 255, 0.22), rgba(34, 211, 238, 0.06));
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(34, 211, 238, 0.35);
}

/* ---------- 右侧主区域 ---------- */
.layout-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.header {
  height: 56px;
  flex-shrink: 0;
  background: transparent;
  border-bottom: 1px solid rgba(120, 150, 255, 0.10);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-greeting {
  font-size: 14px;
  color: var(--ll-text-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.lang-chip {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  border: 1px solid rgba(120, 150, 255, 0.28);
  color: var(--ll-text);
  font-size: 13px;
  transition: all 0.2s;
  outline: none;
}

.lang-chip:hover {
  border-color: rgba(120, 150, 255, 0.5);
  background: rgba(255, 255, 255, 0.04);
}

.lang-code {
  font-size: 12px;
  letter-spacing: 0.5px;
}

.lang-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lang-menu-flag {
  font-size: 15px;
}

:deep(.lang-active) {
  color: var(--ll-cyan, #22d3ee);
  font-weight: 700;
}

.drawer-lang {
  padding: 4px 0;
}

.drawer-lang-label {
  font-size: 12px;
  color: var(--ll-text-muted);
  display: block;
  margin-bottom: 8px;
}

.drawer-lang-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.lang-chip-btn {
  border: 1px solid rgba(120, 150, 255, 0.22);
  background: rgba(255, 255, 255, 0.04);
  color: var(--ll-text);
  border-radius: 18px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}

.lang-chip-btn.active {
  border-color: var(--ll-cyan, #22d3ee);
  color: var(--ll-cyan, #22d3ee);
  background: rgba(34, 211, 238, 0.08);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  border: 1px solid transparent;
  transition: all 0.2s;
  outline: none;
}

.user-chip:hover {
  border-color: rgba(120, 150, 255, 0.35);
  background: rgba(255, 255, 255, 0.04);
}

/* 游客登录入口（button 元素需要重置默认样式） */
.user-chip-guest {
  background: none;
  border: 1px solid rgba(120, 150, 255, 0.35);
  font-family: inherit;
  outline: none;
}

.user-chip-guest:hover {
  border-color: rgba(34, 211, 238, 0.55);
  background: rgba(34, 211, 238, 0.08);
}

.avatar {
  font-size: 20px;
}

.nickname {
  font-size: 14px;
  color: var(--ll-text);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  background: transparent;
}

/* ---------- 移动端适配 ---------- */
.layout.is-mobile {
  flex-direction: column;
}

.layout.is-mobile .header {
  height: auto;
  min-height: 52px;
  padding: 8px 14px;
  /* 刘海屏/挖孔屏：顶部安全区留白 */
  padding-top: calc(8px + env(safe-area-inset-top, 0px));
}

.layout.is-mobile .main {
  /* 底部给标签栏让位（含手势条安全区） */
  padding-bottom: calc(58px + env(safe-area-inset-bottom, 0px));
}

/* 底部标签栏 */
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  background: rgba(10, 15, 30, 0.92);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border-top: 1px solid rgba(120, 150, 255, 0.16);
  padding-bottom: env(safe-area-inset-bottom, 0px);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 8px 0 6px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ll-text-muted);
  font-family: inherit;
  transition: color 0.2s;
}

.tab-item.active {
  color: var(--ll-cyan);
}

.tab-icon {
  font-size: 22px;
}

.tab-label {
  font-size: 11px;
  line-height: 1;
}

/* 抽屉菜单 */
.drawer-brand {
  padding: 8px 4px 16px;
  border-bottom: 1px solid rgba(120, 150, 255, 0.14);
  margin-bottom: 12px;
}

.drawer-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 14px;
  border: none;
  border-radius: 10px;
  background: none;
  cursor: pointer;
  font-family: inherit;
  font-size: 15px;
  color: var(--ll-text);
  transition: background 0.2s;
}

.drawer-item:active {
  background: rgba(79, 124, 255, 0.14);
}

.drawer-item.active {
  background: linear-gradient(90deg, rgba(79, 124, 255, 0.22), rgba(34, 211, 238, 0.06));
  color: #fff;
}

.drawer-item.danger {
  color: #f87171;
}

.drawer-divider {
  height: 1px;
  background: rgba(120, 150, 255, 0.14);
  margin: 8px 0;
}
</style>

<style>
/* 抽屉全局样式（非 scoped，覆盖 Element Plus 默认浅色） */
.more-drawer .el-drawer {
  background: #0e1630 !important;
}

.more-drawer .el-drawer__body {
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom, 0px));
}
</style>

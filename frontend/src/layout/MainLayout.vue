<template>
  <el-container class="layout">
    <el-aside width="216px" class="aside">
      <div class="brand">
        <span class="brand-logo">🛰️</span>
        <div>
          <div class="brand-name">Lingo<span class="brand-hl">Play</span></div>
          <div class="brand-sub">游戏化多语学习</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/">
          <el-icon><Lightning /></el-icon>
          <span>游戏大厅</span>
        </el-menu-item>
        <el-menu-item index="/courses">
          <el-icon><MapLocation /></el-icon>
          <span>关卡地图</span>
        </el-menu-item>
        <el-menu-item index="/recommend">
          <el-icon><MagicStick /></el-icon>
          <span>智能推荐</span>
        </el-menu-item>
        <el-menu-item index="/progress">
          <el-icon><DataLine /></el-icon>
          <span>我的战绩</span>
        </el-menu-item>
        <el-menu-item index="/community">
          <el-icon><ChatDotRound /></el-icon>
          <span>学习社区</span>
        </el-menu-item>
        <el-menu-item index="/achievements">
          <el-icon><Trophy /></el-icon>
          <span>成就殿堂</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-greeting">{{ greeting }}</div>
        <el-dropdown @command="onCommand">
          <span class="user-chip">
            <span class="avatar">{{ store.user ? store.user.avatar || '🙂' : '🙂' }}</span>
            <span class="nickname">{{ store.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/courses') || route.path.startsWith('/learn')) return '/courses'
  if (route.path.startsWith('/community')) return '/community'
  return route.path
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜猫子模式启动 🌙'
  if (hour < 12) return '今日能量已充满，开一把！ ⚡'
  if (hour < 18) return '下午茶时间，来局单词消消乐 🍵'
  return '晚间训练场已开放 🎮'
})

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
}

.aside {
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

.brand-logo {
  font-size: 26px;
  filter: drop-shadow(0 0 8px rgba(34, 211, 238, 0.7));
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

.header {
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

.avatar {
  font-size: 20px;
}

.nickname {
  font-size: 14px;
  color: var(--ll-text);
}

.main {
  padding: 0;
  overflow-y: auto;
  background: transparent;
}
</style>
<template>
  <el-dialog
    v-model="store.loginModalVisible"
    width="360px"
    align-center
    :show-close="true"
    class="login-dialog"
    @closed="resetForm"
  >
    <template #header>
      <div class="dlg-head">
        <span class="dlg-logo">🛰️</span>
        <span class="dlg-title">{{ isLogin ? '登录 LingoPlay' : '注册 LingoPlay' }}</span>
      </div>
    </template>

    <!-- 登录 -->
    <el-form v-if="isLogin" @submit.prevent>
      <el-form-item>
        <el-input v-model="loginForm.account" placeholder="用户名 / 邮箱" :prefix-icon="User" size="large" @keyup.enter="submitLogin" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="loginForm.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" size="large" @keyup.enter="submitLogin" />
      </el-form-item>
      <el-button class="dlg-submit" type="primary" size="large" :loading="loading" @click="submitLogin">
        登 录
      </el-button>
      <div class="dlg-switch">
        还没有账号？<a @click="isLogin = false">立即注册</a>
      </div>
    </el-form>

    <!-- 注册 -->
    <el-form v-else @submit.prevent>
      <el-form-item>
        <el-input v-model="regForm.username" placeholder="用户名（3-20 位）" :prefix-icon="User" size="large" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="regForm.email" placeholder="邮箱" :prefix-icon="Message" size="large" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="regForm.nickname" placeholder="昵称（选填）" :prefix-icon="Star" size="large" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="regForm.password" type="password" show-password placeholder="密码（6-32 位）" :prefix-icon="Lock" size="large" @keyup.enter="submitRegister" />
      </el-form-item>
      <el-button class="dlg-submit" type="primary" size="large" :loading="loading" @click="submitRegister">
        注 册
      </el-button>
      <div class="dlg-switch">
        已有账号？<a @click="isLogin = true">直接登录</a>
      </div>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Star } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const store = useUserStore()
const router = useRouter()

const isLogin = ref(true)
const loading = ref(false)
const loginForm = reactive({ account: '', password: '' })
const regForm = reactive({ username: '', email: '', nickname: '', password: '' })

function resetForm() {
  loading.value = false
}

async function afterAuth(welcome) {
  store.closeLoginModal()
  ElMessage.success(welcome)
  const target = store.loginRedirect
  store.loginRedirect = ''
  if (target) {
    router.push(target)
  }
}

async function submitLogin() {
  if (loading.value) return
  if (!loginForm.account.trim() || !loginForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await store.login(loginForm.account.trim(), loginForm.password)
    await afterAuth('欢迎回来！')
  } catch (e) {
    // 错误提示由 http 拦截器统一弹出
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  if (loading.value) return
  const f = regForm
  if (!f.username.trim() || !f.email.trim() || !f.password) {
    ElMessage.warning('请填写用户名、邮箱和密码')
    return
  }
  loading.value = true
  try {
    await store.register({
      username: f.username.trim(),
      email: f.email.trim(),
      nickname: f.nickname.trim(),
      password: f.password
    })
    await afterAuth('注册成功，开始学习吧！')
  } catch (e) {
    // 错误提示由 http 拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style>
/* 登录弹窗全局样式（非 scoped，覆盖 Element Plus 默认） */
.login-dialog {
  border-radius: 18px !important;
  background: #101832 !important;
  border: 1px solid rgba(120, 150, 255, 0.25) !important;
}

.login-dialog .el-dialog__header {
  padding-bottom: 8px;
}

.login-dialog .el-dialog__body {
  padding-top: 6px;
}

.login-dialog .dlg-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.login-dialog .dlg-logo {
  font-size: 24px;
  filter: drop-shadow(0 0 8px rgba(34, 211, 238, 0.7));
}

.login-dialog .dlg-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--ll-text, #e8ecf8);
}

.login-dialog .dlg-submit {
  width: 100%;
  margin-top: 4px;
  font-weight: 700;
  letter-spacing: 4px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f7cff, #22d3ee);
}

.login-dialog .dlg-switch {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--ll-text-muted, #8a97b8);
}

.login-dialog .dlg-switch a {
  color: var(--ll-cyan, #22d3ee);
  cursor: pointer;
  font-weight: 600;
}
</style>

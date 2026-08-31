<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="auth-logo">🌐</span>
        <h1 class="auth-title">LingoLearn</h1>
        <p class="auth-slogan">沉浸式多语学习 · 英日韩一站式进阶</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="用户名 / 邮箱" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="auth-btn" :loading="loading" @click="submit">登 录</el-button>
        <div class="auth-switch">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ account: '', password: '' })
const rules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.login(form.account, form.password)
    ElMessage.success('登录成功，欢迎回来！')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d2b53 0%, #4f7cff 60%, #6b9bff 100%);
  padding: 24px;
}

.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.auth-brand {
  text-align: center;
  margin-bottom: 28px;
}

.auth-logo {
  font-size: 44px;
}

.auth-title {
  margin: 8px 0 4px;
  font-size: 26px;
  font-weight: 800;
  color: #1d2b53;
}

.auth-slogan {
  margin: 0;
  color: #8492a6;
  font-size: 13px;
}

.auth-btn {
  width: 100%;
}

.auth-switch {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #8492a6;
}

.auth-switch a {
  color: #4f7cff;
  font-weight: 600;
  text-decoration: none;
}
</style>
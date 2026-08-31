<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="auth-logo">🌐</span>
        <h1 class="auth-title">加入 LingoLearn</h1>
        <p class="auth-slogan">开启你的多语种学习之旅</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（3-20 位）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="Star" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码（6-32 位）" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="auth-btn" :loading="loading" @click="submit">注 册</el-button>
        <div class="auth-switch">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Star } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', email: '', nickname: '', password: '' })
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在 3-20 位之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.register(form)
    ElMessage.success('注册成功，欢迎加入！')
    router.push('/')
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
<template>
  <div class="page-container">
    <PageBack to="/" label="返回游戏大厅" />
    <h1 class="page-title">个人中心</h1>
    <p class="page-subtitle">管理你的资料与学习偏好</p>

    <el-card shadow="never" class="profile-card">
      <div class="profile-head">
        <div class="profile-avatar">{{ store.user ? store.user.avatar || '🙂' : '🙂' }}</div>
        <div>
          <div class="profile-nickname">{{ store.nickname }}</div>
          <div class="text-muted text-sm">@{{ store.user ? store.user.username : '' }}</div>
        </div>
      </div>

      <el-form :model="form" label-width="90px" class="profile-form">
        <el-form-item label="邮箱">
          <el-input :model-value="store.user ? store.user.email : ''" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="20" show-word-limit placeholder="设置你的昵称" />
        </el-form-item>
        <el-form-item label="学习语种">
          <el-checkbox-group v-model="form.preferredLanguages">
            <el-checkbox v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.icon }} {{ lang.nameCn }}
            </el-checkbox>
          </el-checkbox-group>
          <div class="text-muted text-sm" style="margin-top: 4px;">
            选择感兴趣的语种，个性化推荐将优先向你展示对应课程
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 关于：显示版本与运行环境，便于问题排查 -->
    <div class="about-box">
      <span>LingoPlay v{{ APP_VERSION }}</span>
      <span class="about-dot">·</span>
      <span>{{ isNativeApp ? 'App 原生环境' : '浏览器环境' }}</span>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { languageApi } from '../api'
import { useUserStore } from '../store/user'
import PageBack from '../components/PageBack.vue'

const APP_VERSION = '1.0.3'
const isNativeApp =
  typeof window !== 'undefined' &&
  !!window.Capacitor &&
  window.Capacitor.isNativePlatform()

const store = useUserStore()
const languages = ref([])
const saving = ref(false)
const form = reactive({ nickname: '', preferredLanguages: [] })

async function save() {
  saving.value = true
  try {
    await store.updateProfile({
      nickname: form.nickname.trim(),
      preferredLanguages: form.preferredLanguages.join(',')
    })
    ElMessage.success('保存成功！')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  languages.value = await languageApi.list()
  await store.fetchMe()
  form.nickname = store.user.nickname || ''
  form.preferredLanguages = (store.user.preferredLanguages || '')
    .split(',')
    .filter((c) => c)
})
</script>

<style scoped>
.profile-card {
  max-width: 560px;
  border-radius: 14px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(120, 150, 255, 0.14);
  margin-bottom: 24px;
}

.profile-avatar {
  font-size: 48px;
  width: 72px;
  height: 72px;
  background: rgba(79, 124, 255, 0.16);
  border: 1px solid rgba(120, 150, 255, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-nickname {
  font-size: 20px;
  font-weight: 700;
}

.profile-form {
  max-width: 460px;
}

.about-box {
  margin-top: 18px;
  text-align: center;
  color: var(--ll-text-muted);
  font-size: 12px;
}

.about-dot {
  margin: 0 6px;
}
</style>
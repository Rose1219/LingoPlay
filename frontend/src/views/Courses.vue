<template>
  <div class="page-container">
    <PageBack to="/" :label="t('courses.backHome')" />
    <h1 class="page-title">{{ t('courses.title') }}</h1>
    <p class="page-subtitle">{{ t('courses.subtitle') }}</p>

    <el-tabs v-model="activeLang" @tab-change="loadCourses">
      <el-tab-pane :label="t('courses.all')" name="" />
      <el-tab-pane v-for="lang in languages" :key="lang.code" :name="lang.code">
        <template #label>
          <span>{{ lang.icon }} {{ lang.nameCn }}<span v-if="lang.vipOnly" class="tab-vip">👑</span></span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div v-loading="loading">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="12" :md="8" v-for="course in courses" :key="course.id">
          <el-card shadow="never" class="course-card hover-card" @click="goDetail(course)">
            <div class="course-cover" :class="{ 'vip-cover': course.vipOnly }">
              {{ course.cover }}
              <span v-if="course.vipOnly" class="vip-ribbon">👑 {{ t('courses.vipOnly') }}</span>
            </div>
            <div class="course-body">
              <div class="course-title">
                {{ course.title }}
                <span v-if="course.ttsApproximate" class="approx-tag">{{ t('courses.approximate') }}</span>
              </div>
              <div class="course-meta">
                <el-tag size="small" type="primary" effect="light">{{ course.level }} · {{ course.levelName }}</el-tag>
                <span class="text-muted text-sm">{{ course.lessonCount }} {{ t('courses.lessonUnit') }}</span>
              </div>
              <div class="course-desc ellipsis">{{ course.description }}</div>
              <el-progress
                :percentage="course.progressPercent"
                :stroke-width="8"
                :color="'#4f7cff'"
              />
              <div class="course-progress-text text-muted text-sm">
                {{ t('courses.completed') }} {{ course.completedLessons }} / {{ course.lessonCount }} {{ t('courses.lessonUnit') }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && !courses.length" :description="t('courses.noCourses')" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { languageApi, courseApi } from '../api'
import { useUserStore } from '../store/user'
import PageBack from '../components/PageBack.vue'

const { t } = useI18n()
const router = useRouter()
const store = useUserStore()
const languages = ref([])
const courses = ref([])
const activeLang = ref('')
const loading = ref(false)

async function loadCourses() {
  loading.value = true
  try {
    courses.value = await courseApi.list(activeLang.value)
  } finally {
    loading.value = false
  }
}

function goDetail(course) {
  // VIP 专属语种：非 VIP 用户直接引导去开通，避免点进去吃 403
  if (course.vipOnly && !(store.user && store.user.vip)) {
    ElMessage.warning(t('courses.vipLockedMsg'))
    router.push('/vip')
    return
  }
  router.push(`/courses/${course.id}`)
}

onMounted(async () => {
  try {
    languages.value = await languageApi.list()
    // 登录态下刷新一次用户信息，保证 user.vip 判断实时
    if (store.isLoggedIn) {
      store.fetchMe().catch(() => {})
    }
  } catch (e) { /* ignore */ }
  await loadCourses()
})
</script>

<style scoped>
.course-card {
  cursor: pointer;
  margin-bottom: 16px;
  border-radius: 12px;
}

.course-cover {
  position: relative;
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 52px;
  background: linear-gradient(135deg, rgba(79, 124, 255, 0.22), rgba(34, 211, 238, 0.08));
  border: 1px solid rgba(120, 150, 255, 0.22);
  border-radius: 8px;
  filter: drop-shadow(0 4px 14px rgba(79, 124, 255, 0.25));
}

.course-cover.vip-cover {
  background: linear-gradient(135deg, rgba(255, 184, 79, 0.24), rgba(255, 154, 60, 0.08));
  border-color: rgba(255, 184, 79, 0.35);
}

.vip-ribbon {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  font-weight: 700;
  color: #2b1a00;
  background: linear-gradient(90deg, #ffb84f, #ff9a3c);
  padding: 2px 8px;
  border-radius: 10px;
}

.tab-vip {
  margin-left: 2px;
  font-size: 11px;
}

.course-body {
  padding: 14px 4px 4px;
}

.course-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
}

.approx-tag {
  font-size: 11px;
  font-weight: 400;
  color: var(--ll-text-muted, #8a97b8);
  border: 1px solid rgba(120, 150, 255, 0.25);
  border-radius: 8px;
  padding: 1px 6px;
  margin-left: 6px;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.course-desc {
  font-size: 13px;
  color: #8492a6;
  margin-bottom: 12px;
}

.course-progress-text {
  margin-top: 6px;
}
</style>

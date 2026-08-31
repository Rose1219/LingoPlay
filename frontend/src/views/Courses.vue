<template>
  <div class="page-container">
    <PageBack to="/" label="返回游戏大厅" />
    <h1 class="page-title">课程中心</h1>
    <p class="page-subtitle">英语 / 日语 / 韩语 · CEFR 分级课程体系，从入门到进阶循序渐进</p>

    <el-tabs v-model="activeLang" @tab-change="loadCourses">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane v-for="lang in languages" :key="lang.code" :name="lang.code">
        <template #label>
          <span>{{ lang.icon }} {{ lang.nameCn }}</span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div v-loading="loading">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="12" :md="8" v-for="course in courses" :key="course.id">
          <el-card shadow="never" class="course-card hover-card" @click="goDetail(course)">
            <div class="course-cover">{{ course.cover }}</div>
            <div class="course-body">
              <div class="course-title">{{ course.title }}</div>
              <div class="course-meta">
                <el-tag size="small" type="primary" effect="light">{{ course.level }} · {{ course.levelName }}</el-tag>
                <span class="text-muted text-sm">{{ course.lessonCount }} 课时</span>
              </div>
              <div class="course-desc ellipsis">{{ course.description }}</div>
              <el-progress
                :percentage="course.progressPercent"
                :stroke-width="8"
                :color="'#4f7cff'"
              />
              <div class="course-progress-text text-muted text-sm">
                已完成 {{ course.completedLessons }} / {{ course.lessonCount }} 课时
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && !courses.length" description="暂无课程" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { languageApi, courseApi } from '../api'
import PageBack from '../components/PageBack.vue'

const router = useRouter()
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
  router.push(`/courses/${course.id}`)
}

onMounted(async () => {
  languages.value = await languageApi.list()
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

.course-body {
  padding: 14px 4px 4px;
}

.course-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
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
<template>
  <div class="page-container" v-loading="loading">
    <PageBack label="返回关卡地图" />
    <template v-if="course">
      <div class="course-head">
        <div class="course-cover-lg">{{ course.cover }}</div>
        <div class="course-head-info">
          <h1 class="page-title">{{ course.title }}</h1>
          <div class="course-head-meta">
            <el-tag size="small" effect="light">{{ course.level }} · {{ course.levelName }}</el-tag>
            <span class="text-muted text-sm">{{ course.languageName }} · {{ course.lessonCount }} 关</span>
          </div>
          <p class="text-muted">{{ course.description }}</p>
          <el-progress :percentage="course.progressPercent" :stroke-width="10" color="#22d3ee" class="head-progress" />
        </div>
      </div>

      <div v-for="(unit, ui) in units" :key="unit.id" class="unit-block">
        <div class="unit-title">
          <span class="unit-dot">{{ ui + 1 }}</span>
          <div>
            <div class="unit-name">{{ unit.title }}</div>
            <div class="text-muted text-sm">{{ unit.description }}</div>
          </div>
        </div>
        <div class="lesson-list">
          <div
            v-for="lesson in unit.lessons"
            :key="lesson.id"
            class="lesson-item hover-card"
            @click="$router.push(`/learn/${lesson.id}`)"
          >
            <div class="lesson-emoji">{{ gameEmoji(lesson.type) }}</div>
            <div class="lesson-title">{{ lesson.title }}</div>
            <el-tag size="small" effect="plain" :color="typeInfo(lesson.type).color" style="color: #fff; border: none;">
              {{ typeInfo(lesson.type).label }}
            </el-tag>
            <div class="stars-box">
              <span v-for="s in 3" :key="s" class="mini-star" :class="{ on: s <= starsOf(lesson.bestScore) }">★</span>
            </div>
            <div class="lesson-status" :class="'st-' + lesson.status.toLowerCase()">
              {{ statusText(lesson.status) }}
            </div>
            <el-button type="primary" size="small" round class="neon-btn">
              {{ lesson.status === 'COMPLETED' ? '再战' : lesson.status === 'IN_PROGRESS' ? '继续' : '开战' }}
            </el-button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { courseApi } from '../api'
import { typeInfo, starsOf } from '../utils/format'
import PageBack from '../components/PageBack.vue'

const route = useRoute()
const loading = ref(false)
const course = ref(null)
const units = ref([])

function gameEmoji(type) {
  return { WORD: '🧩', GRAMMAR: '🗺️', SPEAK: '🎙️', LISTEN: '🕵️', DIALOG: '🎭' }[type] || '🎯'
}

function statusText(status) {
  return status === 'COMPLETED' ? '已通关' : status === 'IN_PROGRESS' ? '闯关中' : '未开启'
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await courseApi.detail(route.params.id)
    course.value = data.course
    units.value = data.units
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.course-head {
  display: flex;
  gap: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(120, 150, 255, 0.14);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  padding: 22px;
  margin-bottom: 20px;
}

.course-cover-lg {
  width: 130px;
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60px;
  background: linear-gradient(135deg, rgba(79, 124, 255, 0.22), rgba(34, 211, 238, 0.10));
  border: 1px solid rgba(120, 150, 255, 0.25);
  border-radius: 12px;
  flex-shrink: 0;
  filter: drop-shadow(0 0 14px rgba(79, 124, 255, 0.4));
}

.course-head-info {
  flex: 1;
}

.course-head-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0 8px;
}

.head-progress {
  max-width: 420px;
}

.unit-block {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(120, 150, 255, 0.14);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
}

.unit-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.unit-dot {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f7cff, #22d3ee);
  color: #04122e;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.unit-name {
  font-weight: 700;
  font-size: 16px;
  color: var(--ll-text);
}

.lesson-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.lesson-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border: 1px solid rgba(120, 150, 255, 0.14);
  border-radius: 10px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.02);
}

.lesson-emoji {
  font-size: 22px;
}

.lesson-title {
  flex: 1;
  font-weight: 600;
  font-size: 14px;
  color: var(--ll-text);
  min-width: 0;
}

.stars-box {
  display: flex;
  gap: 2px;
}

.mini-star {
  color: rgba(255, 255, 255, 0.14);
  font-size: 16px;
}

.mini-star.on {
  color: #fbbf24;
  text-shadow: 0 0 8px rgba(251, 191, 36, 0.6);
}

.lesson-status {
  font-size: 12px;
  color: var(--ll-text-muted);
  white-space: nowrap;
}

.lesson-status.st-completed { color: #34d399; }
.lesson-status.st-in_progress { color: #fbbf24; }
</style>
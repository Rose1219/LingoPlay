<template>
  <div class="page-container learn-page" v-loading="loading">
    <PageBack label="返回关卡地图" />
    <template v-if="lesson">
      <div class="learn-head">
        <div>
          <div class="text-muted text-sm">{{ lesson.courseTitle }} · {{ lesson.unitTitle }}</div>
          <h1 class="learn-title">{{ lesson.title }}</h1>
        </div>
        <el-tag size="large" effect="light" :color="typeInfo(lesson.type).color" style="color: #fff; border: none;">
          {{ typeInfo(lesson.type).label }}
        </el-tag>
      </div>

      <!-- 五大互动学习模块 -->
      <WordQuiz
        v-if="lesson.type === 'WORD'"
        :words="lesson.content.words"
        :lang="lang"
        :levels="courseLevels"
        @finished="onFinished"
      />
      <GrammarPractice
        v-else-if="lesson.type === 'GRAMMAR'"
        :content="lesson.content"
        @finished="onFinished"
      />
      <SpeakingFollow
        v-else-if="lesson.type === 'SPEAK'"
        :content="lesson.content"
        :lang="lang"
        @finished="onFinished"
      />
      <ListeningTrain
        v-else-if="lesson.type === 'LISTEN'"
        :content="lesson.content"
        :lang="lang"
        @finished="onFinished"
      />
      <DialogueSim
        v-else-if="lesson.type === 'DIALOG'"
        :content="lesson.content"
        :lang="lang"
        @finished="onFinished"
      />

      <!-- 通关结算弹窗 -->
      <el-dialog v-model="resultVisible" :title="resultTitle" width="420px" :close-on-click-modal="false" center>
        <div class="result-body">
          <div class="result-stars">
            <span v-for="i in 3" :key="i" class="star" :class="{ on: i <= resultStars }">★</span>
          </div>
          <div class="result-message">{{ resultMessage }}</div>
          <div v-if="result && result.newAchievements && result.newAchievements.length" class="achievement-new">
            <div class="achievement-new-title">🎁 解锁新成就</div>
            <div v-for="a in result.newAchievements" :key="a.code" class="achievement-new-item">
              <span class="achievement-new-icon">{{ a.icon }}</span>
              <span>{{ a.name }}</span>
            </div>
          </div>
        </div>
        <template #footer>
          <el-button @click="$router.push(`/courses/${lesson.courseId}`)">返回关卡地图</el-button>
          <el-button @click="relearn">再战一局</el-button>
          <el-button
            v-if="result && result.nextLessonId"
            type="primary"
            class="neon-btn"
            @click="goNext"
          >
            进入下一关 →
          </el-button>
          <el-button v-else-if="result && result.completed" type="primary" class="neon-btn" @click="$router.push(`/courses/${lesson.courseId}`)">
            🎉 本课程全部通关
          </el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { lessonApi } from '../api'
import { typeInfo, starsOf } from '../utils/format'
import WordQuiz from '../components/learn/WordQuiz.vue'
import GrammarPractice from '../components/learn/GrammarPractice.vue'
import SpeakingFollow from '../components/learn/SpeakingFollow.vue'
import ListeningTrain from '../components/learn/ListeningTrain.vue'
import DialogueSim from '../components/learn/DialogueSim.vue'
import PageBack from '../components/PageBack.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const lesson = ref(null)
const result = ref(null)
const resultVisible = ref(false)

const lang = computed(() => (lesson.value ? ttsLang(lesson.value.languageCode) : 'en-US'))

// 课程内单词关：单关卡模式（本课全部单词，不限时）
const courseLevels = [{ name: '词汇挑战', desc: '选出正确释义，不限时作答', questions: 10, timeLimit: 0 }]

function ttsLang(code) {
  return { en: 'en-US', ja: 'ja-JP', ko: 'ko-KR' }[code] || 'en-US'
}

const resultTitle = computed(() => {
  if (!result.value) return ''
  return result.value.score >= 60 ? '🎉 通关成功！' : '💪 再接再厉！'
})

const resultStars = computed(() => {
  if (!result.value) return 0
  return starsOf(result.value.score)
})

const resultMessage = computed(() => {
  if (!result.value) return ''
  return result.value.score >= 60
    ? `本次战绩 ${result.value.score} 分，关卡已通关并记入战绩！`
    : `本次获得 ${result.value.score} 分，再战一局冲击三星评价吧！`
})

async function onFinished(payload) {
  result.value = await lessonApi.submit(route.params.id, payload)
  resultVisible.value = true
}

function relearn() {
  resultVisible.value = false
  window.location.reload()
}

/** 通关后跳到课程内下一关 */
function goNext() {
  if (!result.value || !result.value.nextLessonId) return
  resultVisible.value = false
  router.push(`/learn/${result.value.nextLessonId}`)
}

onMounted(async () => {
  loading.value = true
  try {
    lesson.value = await lessonApi.detail(route.params.id)
    lessonApi.start(route.params.id).catch(() => {})
  } catch (e) {
    ElMessage.error('课时加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.learn-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}

.learn-title {
  margin: 4px 0 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--ll-text);
}

.result-body {
  text-align: center;
}

.result-stars {
  display: flex;
  justify-content: center;
  gap: 10px;
  font-size: 48px;
}

.star {
  color: rgba(255, 255, 255, 0.12);
}

.star.on {
  color: #fbbf24;
  text-shadow: 0 0 18px rgba(251, 191, 36, 0.75);
  animation: star-in 0.5s ease;
}

@keyframes star-in {
  0% { transform: scale(0); }
  70% { transform: scale(1.4); }
  100% { transform: scale(1); }
}

.result-message {
  color: var(--ll-text-muted);
  font-size: 14px;
  margin: 12px 0;
}

.achievement-new {
  background: rgba(251, 191, 36, 0.08);
  border: 1px solid rgba(251, 191, 36, 0.25);
  border-radius: 10px;
  padding: 12px;
}

.achievement-new-title {
  font-weight: 700;
  margin-bottom: 8px;
  color: #fbbf24;
}

.achievement-new-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 3px 0;
}

.achievement-new-icon {
  font-size: 18px;
}
</style>
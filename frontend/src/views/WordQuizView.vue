<template>
  <div class="page-container">
    <PageBack to="/" label="返回游戏大厅" />

    <!-- ===== 语种选择 ===== -->
    <div v-if="step === 'select'" class="select-stage">
      <div class="select-title">单词闯关</div>
      <div class="select-sub">选择语种开始挑战 · 6 个关卡 · 难度递增</div>

      <div class="lang-grid">
        <div
          v-for="lang in languages"
          :key="lang.code"
          class="lang-card"
          :class="{ default: lang.code === 'en' }"
          @click="startGame(lang)"
        >
          <div class="lang-icon">{{ lang.icon }}</div>
          <div class="lang-name">{{ lang.nameCn }}</div>
          <div class="lang-words" v-if="wordCounts[lang.code] !== undefined">
            {{ wordCounts[lang.code] }} 个单词
          </div>
          <div class="lang-play">开始 →</div>
          <div class="default-tag" v-if="lang.code === 'en'">默认</div>
        </div>
      </div>

      <!-- 关卡预览 -->
      <div class="levels-preview">
        <div class="levels-title">关卡一览</div>
        <div class="levels-list">
          <div v-for="(lv, i) in levels" :key="i" class="level-pill">
            <span class="pill-num">{{ i + 1 }}</span>
            <span class="pill-name">{{ lv.name }}</span>
            <span class="pill-time">{{ lv.timeLimit > 0 ? lv.timeLimit + 's' : '不限时' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 游戏中 ===== -->
    <div v-else-if="step === 'playing'" class="game-stage">
      <div class="game-head">
        <button class="quit-btn" @click="quitGame">✕ 退出</button>
        <span class="game-lang">{{ currentLang ? currentLang.icon + ' ' + currentLang.nameCn : '' }}</span>
      </div>
      <WordQuiz
        :key="gameKey"
        :words="words"
        :lang="locale"
        :levels="levels"
        @finished="onFinished"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { languageApi, gameApi } from '../api'
import WordQuiz from '../components/learn/WordQuiz.vue'
import PageBack from '../components/PageBack.vue'

const step = ref('select') // select | playing
const languages = ref([])
const currentLang = ref(null)
const words = ref([])
const wordCounts = ref({})
const gameKey = ref(0)

const levels = [
  { name: '热身训练', desc: '不限时作答，先熟悉单词', questions: 8, timeLimit: 0 },
  { name: '限时初练', desc: '每题限时 15 秒，从容作答', questions: 8, timeLimit: 15 },
  { name: '稳步进阶', desc: '每题限时 10 秒，考验反应', questions: 8, timeLimit: 10 },
  { name: '高手过招', desc: '每题限时 8 秒，节奏加快', questions: 8, timeLimit: 8 },
  { name: '闪电风暴', desc: '每题限时 6 秒，手速对决', questions: 8, timeLimit: 6 },
  { name: '大师之路', desc: '每题限时 5 秒，登顶挑战', questions: 8, timeLimit: 5 }
]

const locale = computed(() => {
  const map = { en: 'en-US', ja: 'ja-JP', ko: 'ko-KR' }
  return currentLang.value ? map[currentLang.value.code] || 'en-US' : 'en-US'
})

async function loadCounts() {
  for (const lang of languages.value) {
    try {
      const list = await gameApi.quizWords(lang.code)
      wordCounts.value = { ...wordCounts.value, [lang.code]: list.length }
    } catch (e) {
      wordCounts.value = { ...wordCounts.value, [lang.code]: 0 }
    }
  }
}

async function startGame(lang) {
  currentLang.value = lang
  try {
    const list = await gameApi.quizWords(lang.code)
    if (!list.length) {
      ElMessage.warning('该语种暂无词库')
      return
    }
    words.value = list
    gameKey.value++
    step.value = 'playing'
  } catch (e) {
    ElMessage.error('词库加载失败')
  }
}

function quitGame() {
  step.value = 'select'
}

async function onFinished(payload) {
  try {
    await gameApi.submitQuiz({
      languageCode: currentLang.value ? currentLang.value.code : 'en',
      minutes: 2,
      score: payload.score,
      correctCount: payload.correctCount,
      totalCount: payload.totalCount,
      words: payload.words
    })
    ElMessage.success('战绩已保存！')
  } catch (e) {
    // 保存失败不阻塞游戏结算界面
  }
}

onMounted(async () => {
  languages.value = await languageApi.list()
  loadCounts()
})
</script>

<style scoped>
/* ===== 语种选择 ===== */
.select-stage {
  text-align: center;
  padding-top: 3vh;
}

.select-title {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.select-sub {
  color: var(--ll-text-muted);
  font-size: 14px;
  margin: 8px 0 36px;
}

.lang-grid {
  display: flex;
  justify-content: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 40px;
}

.lang-card {
  position: relative;
  width: 180px;
  padding: 30px 16px 24px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(120, 150, 255, 0.16);
  cursor: pointer;
  transition: all 0.22s;
}

.lang-card:hover {
  transform: translateY(-5px);
  border-color: rgba(34, 211, 238, 0.5);
  box-shadow: 0 14px 40px rgba(34, 211, 238, 0.12);
}

.lang-card.default {
  border-color: rgba(79, 124, 255, 0.45);
  background: linear-gradient(160deg, rgba(79, 124, 255, 0.1), rgba(34, 211, 238, 0.04));
}

.default-tag {
  position: absolute;
  top: -10px;
  right: 14px;
  font-size: 11px;
  font-weight: 700;
  color: #04122e;
  background: linear-gradient(135deg, #4f7cff, #22d3ee);
  border-radius: 12px;
  padding: 3px 12px;
}

.lang-icon {
  font-size: 52px;
  margin-bottom: 12px;
  filter: drop-shadow(0 4px 16px rgba(79, 124, 255, 0.4));
}

.lang-name {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}

.lang-words {
  font-size: 12px;
  color: var(--ll-text-muted);
  margin-bottom: 14px;
}

.lang-play {
  font-size: 13px;
  color: #22d3ee;
  opacity: 0.85;
}

/* 关卡预览 */
.levels-preview {
  text-align: left;
  max-width: 640px;
  margin: 0 auto;
}

.levels-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
  color: var(--ll-text);
}

.levels-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.level-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(120, 150, 255, 0.14);
  border-radius: 12px;
  padding: 10px 14px;
}

.pill-num {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
  color: #04122e;
  background: linear-gradient(135deg, #4f7cff, #22d3ee);
  flex-shrink: 0;
}

.pill-name {
  flex: 1;
  font-size: 13px;
  color: var(--ll-text);
}

.pill-time {
  font-size: 12px;
  color: var(--ll-text-muted);
}

/* ===== 游戏中 ===== */
.game-stage {
  max-width: 720px;
  margin: 0 auto;
}

.game-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.quit-btn {
  font-family: inherit;
  font-size: 13px;
  color: var(--ll-text-muted);
  background: transparent;
  border: 1px solid rgba(120, 150, 255, 0.2);
  border-radius: 16px;
  padding: 6px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.quit-btn:hover {
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.4);
}

.game-lang {
  font-size: 13px;
  color: var(--ll-text-muted);
}

/* ---------- 移动端适配 ---------- */
@media (max-width: 768px) {
  .select-title {
    font-size: 24px;
  }

  .select-sub {
    margin-bottom: 24px;
  }

  .lang-card {
    width: calc(50% - 9px);
    min-width: 150px;
    padding: 22px 12px 18px;
  }

  .lang-grid {
    gap: 12px;
    margin-bottom: 28px;
  }

  /* 关卡一览：小屏改两列，避免挤压 */
  .levels-list {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
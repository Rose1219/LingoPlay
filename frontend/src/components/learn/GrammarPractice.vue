<template>
  <div class="game-panel">
    <template v-if="!finished">
      <!-- 战况栏 -->
      <div class="hud">
        <div class="hud-item"><span class="hud-label">能量</span><span class="hud-value">{{ energy }} / {{ energyMax }}</span></div>
        <div class="hud-item combo" :class="{ hot: combo >= 2 }"><span class="hud-label">连击</span><span class="hud-value">{{ combo >= 2 ? `x${combo}` : '--' }}</span></div>
        <div class="hud-item"><span class="hud-label">得分</span><span class="hud-value">{{ correctCount }}</span></div>
      </div>

      <!-- 探险讲解阶段 -->
      <template v-if="phase === 'explain'">
        <el-tag effect="dark" color="#818cf8" class="phase-tag">第 {{ pointIndex + 1 }} 关 · {{ points.length }}</el-tag>
        <div class="explain-card">
          <div class="explain-title">📜 秘笈 · {{ point.title }}</div>
          <div class="explain-text">{{ point.explanation }}</div>
        </div>
        <div class="action-row">
          <el-button type="primary" round class="neon-btn" @click="startQuestions">开始挑战</el-button>
        </div>
      </template>

      <!-- 挑战阶段 -->
      <template v-else>
        <el-progress :percentage="Math.round(((qIndex + pointIndex * qCount) / totalCount) * 100)" :stroke-width="8" color="#818cf8" class="progress-line" />
        <div class="question-card">
          <div class="question-text">{{ question.question }}</div>
          <div class="question-options">
            <div
              v-for="(opt, oi) in question.options"
              :key="oi"
              class="q-option"
              :class="qOptionClass(oi)"
              @click="answerQuestion(oi)"
            >
              <span class="q-option-key">{{ 'ABCD'[oi] }}</span>
              <span>{{ opt }}</span>
            </div>
          </div>
          <div v-if="answered" class="answer-feedback" :class="lastCorrect ? 'ok' : 'bad'">
            {{ lastCorrect ? `✅ 正确！能量 +1` : `💥 失误！正确答案是 ${'ABCD'[question.answer]} · ${question.options[question.answer]}` }}
          </div>
          <div v-if="answered" class="action-row">
            <el-button type="primary" round class="neon-btn" @click="nextQuestion">
              {{ done ? '查看战果' : '下一题' }}
            </el-button>
          </div>
        </div>
      </template>
    </template>

    <!-- 战果画面 -->
    <div v-else class="result">
      <div class="result-stars">
        <span v-for="i in 3" :key="i" class="star" :class="{ on: i <= stars }">★</span>
      </div>
      <div class="result-title">{{ stars >= 3 ? '完美通关！' : stars === 2 ? '顺利过关！' : '挑战成功！' }}</div>
      <div class="result-score">得分 {{ score }} 分 · 最高连击 x{{ maxCombo }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  content: { type: Object, required: true }
})
const emit = defineEmits(['finished'])

const points = computed(() => props.content.points || [])
const totalCount = computed(() => points.value.reduce((s, p) => s + (p.questions ? p.questions.length : 0), 0))
const energyMax = computed(() => totalCount.value)

const phase = ref('explain')
const pointIndex = ref(0)
const qIndex = ref(0)
const answered = ref(false)
const lastCorrect = ref(false)
const selected = ref(-1)
const correctCount = ref(0)
const energy = ref(0)
const combo = ref(0)
const maxCombo = ref(0)
const finished = ref(false)
const score = ref(0)
const stars = ref(0)

const point = computed(() => points.value[pointIndex.value] || {})
const question = computed(() => (point.value.questions || [])[qIndex.value] || {})
const qCount = computed(() => (point.value.questions || []).length)
const done = computed(() => pointIndex.value >= points.value.length - 1 && qIndex.value >= qCount.value - 1)

function startQuestions() {
  phase.value = 'question'
  answered.value = false
}

function answerQuestion(oi) {
  if (answered.value) return
  answered.value = true
  selected.value = oi
  lastCorrect.value = question.value.answer === oi
  if (lastCorrect.value) {
    correctCount.value++
    energy.value = Math.min(energyMax.value, energy.value + 1)
    combo.value++
    maxCombo.value = Math.max(maxCombo.value, combo.value)
  } else {
    combo.value = 0
  }
}

function qOptionClass(oi) {
  if (!answered.value) return ''
  if (oi === question.value.answer) return 'correct'
  if (oi === selected.value) return 'wrong'
  return 'dim'
}

function nextQuestion() {
  if (qIndex.value < qCount.value - 1) {
    qIndex.value++
  } else if (pointIndex.value < points.value.length - 1) {
    pointIndex.value++
    qIndex.value = 0
    phase.value = 'explain'
  } else {
    score.value = Math.round((correctCount.value / totalCount.value) * 100)
    stars.value = score.value >= 90 ? 3 : score.value >= 60 ? 2 : 1
    finished.value = true
    emit('finished', { score: score.value, correctCount: correctCount.value, totalCount: totalCount.value })
  }
  answered.value = false
  selected.value = -1
}

onMounted(() => {
  if (!points.value.length) {
    ElMessage.error('课时内容为空')
  }
})
</script>

<style scoped>
.game-panel {
  max-width: 720px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(129, 140, 248, 0.2);
  border-radius: 18px;
  padding: 28px;
  min-height: 420px;
}

.hud {
  display: flex;
  justify-content: center;
  gap: 26px;
  margin-bottom: 22px;
}

.hud-item {
  text-align: center;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(120, 150, 255, 0.16);
  border-radius: 12px;
  padding: 8px 20px;
  transition: all 0.2s;
}

.hud-item.combo.hot {
  border-color: rgba(129, 140, 248, 0.6);
  box-shadow: 0 0 18px rgba(129, 140, 248, 0.35);
  transform: scale(1.06);
}

.hud-label {
  display: block;
  font-size: 11px;
  color: var(--ll-text-muted);
  margin-bottom: 2px;
}

.hud-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--ll-text);
}

.combo.hot .hud-value { color: #818cf8; }

.phase-tag {
  margin-bottom: 16px;
}

.explain-card {
  background: rgba(129, 140, 248, 0.1);
  border: 1px solid rgba(129, 140, 248, 0.25);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.explain-title {
  font-size: 18px;
  font-weight: 700;
  color: #a5b4fc;
  margin-bottom: 10px;
}

.explain-text {
  color: var(--ll-text);
  line-height: 1.8;
}

.action-row {
  text-align: center;
  margin-top: 14px;
}

.progress-line {
  margin-bottom: 24px;
}

.question-text {
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 22px;
  color: var(--ll-text);
}

.question-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-width: 620px;
  margin: 0 auto;
}

.q-option {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(120, 150, 255, 0.2);
  border-radius: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.15s;
  color: var(--ll-text);
}

.q-option:hover {
  border-color: #818cf8;
  background: rgba(129, 140, 248, 0.08);
}

.q-option-key {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(79, 124, 255, 0.2);
  color: #a5b4fc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.q-option.correct {
  border-color: rgba(52, 211, 153, 0.7);
  background: rgba(52, 211, 153, 0.12);
}

.q-option.wrong {
  border-color: rgba(248, 113, 113, 0.7);
  background: rgba(248, 113, 113, 0.12);
}

.q-option.dim { opacity: 0.45; }

.answer-feedback {
  text-align: center;
  margin-top: 18px;
  font-weight: 700;
}

.answer-feedback.ok { color: #34d399; }
.answer-feedback.bad { color: #f87171; }

.result {
  text-align: center;
  padding: 40px 0 20px;
}

.result-stars {
  font-size: 52px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.star {
  color: rgba(255, 255, 255, 0.12);
  transition: all 0.3s;
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

.result-title {
  font-size: 22px;
  font-weight: 800;
  margin: 14px 0 8px;
  background: linear-gradient(90deg, #818cf8, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.result-score {
  color: var(--ll-text-muted);
  font-size: 14px;
}

/* ---------- 移动端适配 ---------- */
@media (max-width: 768px) {
  /* 选项可能较长，小屏改单列避免挤压换行错乱 */
  .question-options {
    grid-template-columns: 1fr;
  }
}
</style>
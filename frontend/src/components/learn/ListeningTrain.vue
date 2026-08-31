<template>
  <div class="game-panel">
    <template v-if="!finished">
      <el-progress :percentage="Math.round((index / items.length) * 100)" :stroke-width="8" color="#f59e0b" class="progress-line" />
      <div class="listen-card">
        <div class="listen-order">案件 {{ index + 1 }} / {{ items.length }}</div>
        <div class="hint-box">
          <el-icon><InfoFilled /></el-icon>
          <span class="text-sm">点击播放，破译你听到的神秘句子（可反复播放）</span>
        </div>
        <div class="play-area">
          <el-button type="primary" size="large" circle :icon="VideoPlay" class="play-btn" @click="play" />
          <div class="play-tip">点击播放音频</div>
        </div>
        <div v-if="current.hint && showHint" class="listen-hint text-muted text-sm">💡 线索：{{ current.hint }}</div>
        <div v-if="!showHint" class="hint-toggle">
          <el-button text type="info" size="small" @click="showHint = true">查看线索</el-button>
        </div>
        <el-input
          v-model="input"
          size="large"
          placeholder="输入你破译出的句子"
          clearable
          @keyup.enter="check"
        />
        <div class="check-row">
          <el-button round @click="showHint = true">线索</el-button>
          <el-button type="primary" round class="neon-btn" :disabled="!input.trim()" @click="check">破译确认</el-button>
        </div>
        <div v-if="checked" class="check-result">
          <div class="result-score-num" :class="scoreLevel">{{ score }} 分</div>
          <div class="answer-reveal">
            <div class="answer-line">原文：{{ current.text }}</div>
            <div class="answer-line text-muted">译文：{{ current.meaning }}</div>
          </div>
          <el-button type="primary" round class="neon-btn" @click="next">
            {{ index < items.length - 1 ? '下一个案件' : '查看战果' }}
          </el-button>
        </div>
      </div>
    </template>

    <div v-else class="result">
      <div class="result-stars">
        <span v-for="i in 3" :key="i" class="star" :class="{ on: i <= stars }">★</span>
      </div>
      <div class="result-title">{{ stars >= 3 ? '名侦探！' : stars === 2 ? '破案成功！' : '任务完成！' }}</div>
      <div class="result-score">平均得分 {{ Math.round(avgScore) }} 分</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, InfoFilled } from '@element-plus/icons-vue'
import { speak, ttsSupported } from '../../utils/tts'
import { similarity, scoreOf } from '../../utils/similarity'

const props = defineProps({
  content: { type: Object, required: true },
  lang: { type: String, default: 'en-US' }
})
const emit = defineEmits(['finished'])

const items = computed(() => props.content.items || [])
const index = ref(0)
const input = ref('')
const showHint = ref(false)
const checked = ref(false)
const score = ref(0)
const scores = ref([])
const finished = ref(false)
const stars = ref(0)

const current = computed(() => items.value[index.value] || {})
const avgScore = computed(() =>
  scores.value.length ? scores.value.reduce((s, v) => s + v, 0) / scores.value.length : 0
)
const scoreLevel = computed(() =>
  score.value >= 80 ? 'great' : score.value >= 60 ? 'good' : 'poor'
)

function play() {
  if (!ttsSupported()) {
    ElMessage.warning('当前浏览器不支持语音合成')
    return
  }
  speak(current.value.text, props.lang, 0.85)
}

function check() {
  if (!input.value.trim()) return
  score.value = scoreOf(similarity(input.value, current.value.text))
  checked.value = true
}

function next() {
  scores.value.push(score.value)
  if (index.value < items.value.length - 1) {
    index.value++
    input.value = ''
    showHint.value = false
    checked.value = false
  } else {
    stars.value = avgScore.value >= 90 ? 3 : avgScore.value >= 60 ? 2 : 1
    finished.value = true
    const correct = scores.value.filter((s) => s >= 60).length
    emit('finished', {
      score: Math.round(avgScore.value),
      correctCount: correct,
      totalCount: scores.value.length
    })
  }
}

onMounted(() => {
  if (!items.value.length) {
    ElMessage.error('课时内容为空')
  }
})
</script>

<style scoped>
.game-panel {
  max-width: 640px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 18px;
  padding: 28px;
  min-height: 380px;
}

.progress-line {
  margin-bottom: 24px;
}

.listen-card {
  max-width: 560px;
  margin: 0 auto;
  text-align: center;
}

.listen-order {
  color: var(--ll-text-muted);
  font-size: 13px;
  margin-bottom: 14px;
}

.hint-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 8px;
  padding: 8px 14px;
  color: #fcd34d;
  margin-bottom: 18px;
}

.play-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.play-btn {
  width: 64px;
  height: 64px;
  font-size: 30px;
  background: #f59e0b;
  border-color: #f59e0b;
  box-shadow: 0 0 26px rgba(245, 158, 11, 0.45);
}

.play-btn:hover {
  background: #d97706;
  border-color: #d97706;
}

.play-tip {
  color: var(--ll-text-muted);
  font-size: 13px;
}

.listen-hint {
  margin-bottom: 12px;
}

.hint-toggle {
  margin-bottom: 8px;
}

.check-row {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.check-result {
  margin-top: 18px;
}

.result-score-num {
  font-size: 34px;
  font-weight: 800;
  margin-bottom: 8px;
}

.result-score-num.great { color: #34d399; }
.result-score-num.good { color: #fbbf24; }
.result-score-num.poor { color: #f87171; }

.answer-reveal {
  background: rgba(255, 255, 255, 0.04);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 16px;
}

.answer-line {
  padding: 3px 0;
  color: var(--ll-text);
}

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
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.result-score {
  color: var(--ll-text-muted);
  font-size: 14px;
}
</style>
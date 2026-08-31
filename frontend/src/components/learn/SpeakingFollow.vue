<template>
  <div class="game-panel">
    <template v-if="!finished">
      <el-progress :percentage="Math.round((index / sentences.length) * 100)" :stroke-width="8" color="#22d3ee" class="progress-line" />
      <div class="sentence-card">
        <div class="sentence-order">第 {{ index + 1 }} 句 / 共 {{ sentences.length }} 句</div>
        <div class="sentence-text">{{ current.text }}</div>
        <div class="sentence-translation text-muted" v-if="showTranslation">{{ current.translation }}</div>
        <div class="sentence-tip">
          <el-icon><InfoFilled /></el-icon>
          <span class="text-sm">{{ current.tip }}</span>
        </div>
        <div class="speak-actions">
          <el-button :icon="VideoPlay" round @click="playOriginal">原音</el-button>
          <el-button :icon="Headset" round @click="playSlow">慢速</el-button>
          <el-button round @click="showTranslation = !showTranslation">
            {{ showTranslation ? '藏译文' : '看译文' }}
          </el-button>
        </div>
        <el-divider>我的跟读</el-divider>
        <div class="record-row">
          <template v-if="recSupported">
            <el-button type="primary" size="large" :icon="Microphone" :loading="recording" round class="neon-btn" @click="record">
              {{ recording ? '聆听中…' : '🎙️ 开始跟读' }}
            </el-button>
          </template>
          <template v-else>
            <div class="text-muted text-sm">当前浏览器不支持语音识别，自行跟读后自评</div>
            <div class="self-rate">
              <el-button type="success" size="large" @click="selfRate(100)">完美（100）</el-button>
              <el-button type="warning" size="large" @click="selfRate(75)">不错（75）</el-button>
              <el-button type="danger" size="large" @click="selfRate(50)">加油（50）</el-button>
            </div>
          </template>
        </div>
        <div v-if="scored" class="score-result">
          <div class="score-ring" :class="scoreLevel">
            <span class="score-num">{{ lastScore }}</span>
            <span class="score-unit">分</span>
          </div>
          <div v-if="lastTranscript" class="text-muted text-sm">识别：{{ lastTranscript }}</div>
          <div class="score-level-text">{{ scoreLevelText }}</div>
          <el-button type="primary" round class="neon-btn" @click="next">
            {{ index < sentences.length - 1 ? '下一句' : '查看战果' }}
          </el-button>
        </div>
      </div>
    </template>

    <div v-else class="result">
      <div class="result-stars">
        <span v-for="i in 3" :key="i" class="star" :class="{ on: i <= stars }">★</span>
      </div>
      <div class="result-title">{{ stars >= 3 ? '语音之星！' : stars === 2 ? '发音达人！' : '挑战完成！' }}</div>
      <div class="result-score">平均得分 {{ Math.round(avgScore) }} 分</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, Headset, Microphone, InfoFilled } from '@element-plus/icons-vue'
import { speak } from '../../utils/tts'
import { recognize, recognitionSupported } from '../../utils/speech'
import { similarity, scoreOf } from '../../utils/similarity'

const props = defineProps({
  content: { type: Object, required: true },
  lang: { type: String, default: 'en-US' }
})
const emit = defineEmits(['finished'])

const sentences = computed(() => props.content.sentences || [])
const index = ref(0)
const showTranslation = ref(false)
const recording = ref(false)
const scored = ref(false)
const lastScore = ref(0)
const lastTranscript = ref('')
const scores = ref([])
const finished = ref(false)
const stars = ref(0)
const recSupported = recognitionSupported()

const current = computed(() => sentences.value[index.value] || {})
const avgScore = computed(() =>
  scores.value.length ? Math.round(scores.value.reduce((s, v) => s + v, 0) / scores.value.length) : 0
)
const scoreLevel = computed(() =>
  lastScore.value >= 80 ? 'great' : lastScore.value >= 60 ? 'good' : 'poor'
)
const scoreLevelText = computed(() =>
  lastScore.value >= 80 ? '发音超棒，接近原音！' : lastScore.value >= 60 ? '不错，继续打磨细节！' : '别灰心，再听一遍试试！'
)

function playOriginal() {
  speak(current.value.text, props.lang, 0.9)
}

function playSlow() {
  speak(current.value.text, props.lang, 0.6)
}

async function record() {
  if (recording.value) return
  recording.value = true
  const transcript = await recognize(props.lang)
  recording.value = false
  if (!transcript) {
    ElMessage.warning('没有识别到声音，请检查麦克风权限')
    return
  }
  finishScore(scoreOf(similarity(transcript, current.value.text)), transcript)
}

function selfRate(score) {
  finishScore(score, '')
}

function finishScore(score, transcript) {
  lastScore.value = score
  lastTranscript.value = transcript
  scores.value.push(score)
  scored.value = true
}

function next() {
  if (index.value < sentences.value.length - 1) {
    index.value++
    scored.value = false
    lastTranscript.value = ''
  } else {
    stars.value = avgScore.value >= 90 ? 3 : avgScore.value >= 60 ? 2 : 1
    finished.value = true
    emit('finished', {
      score: avgScore.value,
      correctCount: scores.value.filter((s) => s >= 60).length,
      totalCount: scores.value.length
    })
  }
}

onMounted(() => {
  if (!sentences.value.length) {
    ElMessage.error('课时内容为空')
  }
})
</script>

<style scoped>
.game-panel {
  max-width: 680px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(34, 211, 238, 0.2);
  border-radius: 18px;
  padding: 28px;
  min-height: 380px;
}

.progress-line {
  margin-bottom: 24px;
}

.sentence-card {
  max-width: 620px;
  margin: 0 auto;
  text-align: center;
}

.sentence-order {
  color: var(--ll-text-muted);
  font-size: 13px;
  margin-bottom: 12px;
}

.sentence-text {
  font-size: 28px;
  font-weight: 800;
  color: var(--ll-text);
  margin-bottom: 8px;
}

.sentence-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.2);
  border-radius: 8px;
  padding: 8px 14px;
  color: #67e8f9;
  margin: 10px 0 18px;
}

.speak-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.self-rate {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.record-row {
  margin-top: 8px;
}

.score-result {
  margin-top: 16px;
}

.score-ring {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4px solid;
  box-shadow: 0 0 24px currentColor;
}

.score-ring.great { color: #34d399; }
.score-ring.good { color: #fbbf24; }
.score-ring.poor { color: #f87171; }

.score-num {
  font-size: 36px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 0 12px currentColor;
}

.score-unit {
  font-size: 13px;
  color: #fff;
  margin-left: 3px;
}

.score-level-text {
  margin: 6px 0 16px;
  color: var(--ll-text-muted);
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
  background: linear-gradient(90deg, #22d3ee, #4f7cff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.result-score {
  color: var(--ll-text-muted);
  font-size: 14px;
}
</style>
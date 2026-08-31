<template>
  <div class="game-panel">
    <template v-if="!finished">
      <div class="scene-tag">
        <el-tag effect="dark" color="#ec4899">🎭 场景 · {{ scene }}</el-tag>
        <el-progress :percentage="Math.round((turnIndex / totalTurns) * 100)" :stroke-width="8" color="#ec4899" class="scene-progress" />
      </div>

      <!-- 对话气泡区 -->
      <div class="chat-area">
        <div v-for="(t, i) in visibleTurns" :key="i">
          <!-- AI 气泡 -->
          <div v-if="t.role === 'ai'" class="bubble-row ai">
            <div class="chat-avatar">🤖</div>
            <div class="bubble ai-bubble">
              <div>{{ t.text }}</div>
              <div class="bubble-translation text-muted text-sm">{{ t.translation }}</div>
            </div>
            <el-button circle size="small" :icon="VideoPlay" @click="playAudio(t.text)" />
          </div>
          <!-- 用户气泡 -->
          <div v-else class="bubble-row user">
            <div class="chat-avatar">{{ userAvatar }}</div>
            <div class="bubble user-bubble">
              <template v-if="t.userText">
                <div>{{ t.userText }}</div>
                <div class="bubble-score" :class="t.correct ? 'ok' : 'bad'">{{ t.score }} 分</div>
              </template>
              <template v-else>
                <div class="text-muted text-sm">等待回答…</div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- 当前用户回合作答区 -->
      <div v-if="currentTurn && currentTurn.role === 'user' && !currentTurn.userText" class="answer-area">
        <div class="answer-hint">
          <el-icon><ChatLineSquare /></el-icon>
          <span class="text-sm">根据译文说出对应台词：</span>
        </div>
        <div class="answer-translation">{{ currentTurn.translation }}</div>
        <div class="answer-actions">
          <el-button :icon="VideoPlay" round size="small" @click="speakExample">原音提词</el-button>
          <template v-if="recSupported">
            <el-button type="primary" :icon="Microphone" :loading="recording" round class="neon-btn" @click="record">
              {{ recording ? '聆听中…' : '🎙️ 开口表演' }}
            </el-button>
          </template>
          <el-button round @click="showTypeInput = !showTypeInput">打字输入</el-button>
        </div>
        <div v-if="showTypeInput" class="type-row">
          <el-input v-model="typed" placeholder="输入你的台词" @keyup.enter="checkTyped" />
          <el-button type="primary" round class="neon-btn" :disabled="!typed.trim()" @click="checkTyped">提交</el-button>
        </div>
      </div>

      <!-- AI 回合推进按钮 -->
      <div v-if="currentTurn && currentTurn.role === 'ai'" class="action-row">
        <el-button type="primary" round :icon="Headset" class="neon-btn" @click="speakAndNext">
          {{ turnIndex === 0 ? '开始演出' : '听下一句台词' }}
        </el-button>
      </div>
    </template>

    <div v-else class="result">
      <div class="result-stars">
        <span v-for="i in 3" :key="i" class="star" :class="{ on: i <= stars }">★</span>
      </div>
      <div class="result-title">{{ stars >= 3 ? '影帝诞生！' : stars === 2 ? '演出精彩！' : '顺利谢幕！' }}</div>
      <div class="result-score">流畅度得分 {{ Math.round(avgScore) }} 分</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, Microphone, ChatLineSquare, Headset } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { speak } from '../../utils/tts'
import { recognize, recognitionSupported } from '../../utils/speech'
import { similarity, scoreOf } from '../../utils/similarity'

const props = defineProps({
  content: { type: Object, required: true },
  lang: { type: String, default: 'en-US' }
})
const emit = defineEmits(['finished'])

const store = useUserStore()
const userAvatar = computed(() => (store.user ? store.user.avatar || '🙂' : '🙂'))

const scene = computed(() => props.content.scene || '情景对话')
const turns = computed(() => props.content.turns || [])
const totalTurns = computed(() => Math.max(1, turns.value.length))

const turnIndex = ref(0)
const turnsState = ref([])
const recording = ref(false)
const showTypeInput = ref(false)
const typed = ref('')
const userScores = ref([])
const finished = ref(false)
const stars = ref(0)
const recSupported = recognitionSupported()

const currentTurn = computed(() => turns.value[turnIndex.value] || null)
const visibleTurns = computed(() => turnsState.value.filter((t) => t.role === 'ai' || t.userText))
const avgScore = computed(() =>
  userScores.value.length ? userScores.value.reduce((s, v) => s + v, 0) / userScores.value.length : 0
)

function playAudio(text) {
  speak(text, props.lang, 0.9)
}

function speakExample() {
  if (currentTurn.value) speak(currentTurn.value.text, props.lang, 0.9)
}

function speakAndNext() {
  const turn = currentTurn.value
  if (!turn) return
  turnsState.value.push({ ...turn })
  speak(turn.text, props.lang).then(() => {
    turnIndex.value++
  })
}

async function record() {
  if (recording.value) return
  recording.value = true
  const transcript = await recognize(props.lang)
  recording.value = false
  if (!transcript) {
    ElMessage.warning('没有识别到声音，请检查麦克风权限，或使用打字输入')
    return
  }
  submitAnswer(transcript)
}

function checkTyped() {
  if (!typed.value.trim()) return
  submitAnswer(typed.value.trim())
  typed.value = ''
  showTypeInput.value = false
}

function submitAnswer(userText) {
  const turn = currentTurn.value
  if (!turn) return
  const score = scoreOf(similarity(userText, turn.text))
  userScores.value.push(score)
  turnsState.value.push({ ...turn, userText, score, correct: score >= 60 })
  if (turnIndex.value < turns.value.length - 1) {
    turnIndex.value++
  } else {
    finish()
  }
}

function finish() {
  stars.value = avgScore.value >= 90 ? 3 : avgScore.value >= 60 ? 2 : 1
  finished.value = true
  emit('finished', {
    score: Math.round(avgScore.value),
    correctCount: userScores.value.filter((s) => s >= 60).length,
    totalCount: userScores.value.length
  })
}

onMounted(() => {
  if (!turns.value.length) {
    ElMessage.error('课时内容为空')
  }
})
</script>

<style scoped>
.game-panel {
  max-width: 720px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(236, 72, 153, 0.2);
  border-radius: 18px;
  padding: 28px;
  min-height: 460px;
  display: flex;
  flex-direction: column;
}

.scene-tag {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.scene-progress {
  flex: 1;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 640px;
  width: 100%;
  margin: 0 auto;
  padding: 10px 0 20px;
}

.bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.bubble-row.ai { justify-content: flex-start; }
.bubble-row.user { justify-content: flex-end; }

.chat-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(79, 124, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.bubble {
  max-width: 70%;
  border-radius: 14px;
  padding: 10px 14px;
  color: var(--ll-text);
}

.ai-bubble {
  background: rgba(79, 124, 255, 0.14);
  border: 1px solid rgba(79, 124, 255, 0.25);
  border-bottom-left-radius: 4px;
}

.user-bubble {
  background: rgba(236, 72, 153, 0.14);
  border: 1px solid rgba(236, 72, 153, 0.3);
  border-bottom-right-radius: 4px;
}

.bubble-translation {
  margin-top: 4px;
}

.bubble-score {
  margin-top: 6px;
  font-weight: 700;
  font-size: 13px;
}

.bubble-score.ok { color: #34d399; }
.bubble-score.bad { color: #f87171; }

.answer-area {
  max-width: 640px;
  width: 100%;
  margin: 0 auto;
  background: rgba(236, 72, 153, 0.06);
  border: 1px solid rgba(236, 72, 153, 0.22);
  border-radius: 12px;
  padding: 18px;
}

.answer-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ll-text-muted);
  margin-bottom: 8px;
}

.answer-translation {
  font-size: 17px;
  font-weight: 700;
  color: var(--ll-text);
  margin-bottom: 12px;
}

.answer-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.type-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.action-row {
  text-align: center;
  padding: 10px 0;
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
  background: linear-gradient(90deg, #ec4899, #818cf8);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.result-score {
  color: var(--ll-text-muted);
  font-size: 14px;
}
</style>
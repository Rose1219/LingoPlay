<template>
  <div class="quiz-wrap">
    <!-- ===== 关卡开场 ===== -->
    <div v-if="phase === 'intro'" class="stage-card intro-card">
      <div class="intro-glow"></div>
      <div class="level-badge">LEVEL {{ levelIndex + 1 }}<span class="level-total">/ {{ levels.length }}</span></div>
      <div class="level-name">{{ level.name }}</div>
      <div class="level-desc">{{ level.desc }}</div>
      <div class="rule-chips">
        <span class="chip">{{ level.questions }} 题</span>
        <span class="chip">{{ level.timeLimit > 0 ? `每题 ${level.timeLimit}s` : '不限时' }}</span>
        <span class="chip">3 条生命</span>
        <span class="chip">通过率 75%</span>
      </div>
      <button class="cta-btn" @click="startLevel">开始挑战 →</button>
    </div>

    <!-- ===== 答题中 ===== -->
    <div v-else-if="phase === 'playing'" class="stage-card playing-card">
      <!-- 顶部 HUD -->
      <div class="hud">
        <div class="hud-left">
          <span class="lives">
            <span v-for="i in 3" :key="i" class="life" :class="{ lost: i > lives }">❤</span>
          </span>
        </div>
        <div class="hud-center">
          <span class="hud-chip">Lv.{{ levelIndex + 1 }}</span>
          <span class="hud-chip combo-chip" :class="{ hot: combo >= 3 }">🔥 x{{ combo }}</span>
        </div>
        <div class="hud-right">
          <span class="score-num">{{ score }}</span>
          <span class="score-label">分</span>
        </div>
      </div>

      <!-- 时间条 -->
      <div class="time-bar" v-if="level.timeLimit > 0">
        <div class="time-fill" :class="{ danger: timeLeft <= 3 }" :style="{ width: timePercent + '%' }"></div>
      </div>

      <!-- 题目进度 -->
      <div class="progress-row">
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: (qIndex / level.questions) * 100 + '%' }"></div>
        </div>
        <span class="progress-text">{{ qIndex }} / {{ level.questions }}</span>
      </div>

      <!-- 题卡 -->
      <div class="question-card" :key="qKey">
        <div class="q-label">{{ question.isWordToMeaning ? '选出正确的释义' : '选出对应的单词' }}</div>
        <div class="q-word">{{ question.display }}</div>
        <button
          v-if="question.isWordToMeaning"
          class="q-voice"
          title="播放发音"
          @click="speakQuestion"
        >🔊</button>
        <div class="q-phonetic" v-if="question.isWordToMeaning && question.phonetic">/ {{ question.phonetic }} /</div>
      </div>

      <!-- 选项 -->
      <div class="options">
        <button
          v-for="(opt, i) in question.options"
          :key="i"
          class="option"
          :class="optionClass(i)"
          :disabled="answered"
          @click="choose(i)"
        >
          <span class="opt-key">{{ 'ABCDE'[i] }}</span>
          <span class="opt-text">{{ opt }}</span>
          <span class="opt-mark" v-if="answered && i === question.answerIndex">✓</span>
          <span class="opt-mark wrong" v-if="answered && i === selectedIndex && i !== question.answerIndex">✕</span>
        </button>
      </div>

      <!-- 反馈浮条 -->
      <div class="feedback" v-if="answered" :class="lastCorrect ? 'ok' : 'bad'">
        <span v-if="lastCorrect">
          ✅ 正确！<template v-if="gainFlash">+{{ lastGain }}</template>
        </span>
        <span v-else>❌ {{ feedbackText }}</span>
      </div>
    </div>

    <!-- ===== 关卡通过 ===== -->
    <div v-else-if="phase === 'passed'" class="stage-card result-card">
      <div class="result-emoji">🎉</div>
      <div class="result-title">第 {{ levelIndex + 1 }} 关通过！</div>
      <div class="result-sub">{{ level.name }} 完成</div>
      <div class="result-stats">
        <div class="stat"><span class="stat-v">{{ levelCorrect }}</span><span class="stat-k">答对</span></div>
        <div class="stat"><span class="stat-v">{{ maxCombo }}</span><span class="stat-k">最高连击</span></div>
        <div class="stat"><span class="stat-v">{{ score }}</span><span class="stat-k">总得分</span></div>
      </div>
      <div class="life-bonus" v-if="lives > 0">剩余生命奖励 +{{ lives * 200 }}</div>
      <button class="cta-btn" @click="nextLevel">
        {{ levelIndex + 1 < levels.length ? `进入第 ${levelIndex + 2} 关 →` : '查看最终战绩 →' }}
      </button>
    </div>

    <!-- ===== 游戏结束（失败） ===== -->
    <div v-else-if="phase === 'gameover'" class="stage-card result-card fail">
      <div class="result-emoji">💔</div>
      <div class="result-title">闯关失败</div>
      <div class="result-sub">{{ gameOverReason }}</div>
      <div class="result-stats">
        <div class="stat"><span class="stat-v">{{ levelIndex + 1 }}</span><span class="stat-k">到达关卡</span></div>
        <div class="stat"><span class="stat-v">{{ totalCorrect }}</span><span class="stat-k">累计答对</span></div>
        <div class="stat"><span class="stat-v">{{ score }}</span><span class="stat-k">总得分</span></div>
      </div>
      <button class="cta-btn" @click="restart">再来一局</button>
    </div>

    <!-- ===== 全部通关 ===== -->
    <div v-else-if="phase === 'complete'" class="stage-card result-card complete">
      <div class="crown">👑</div>
      <div class="result-stars">
        <span v-for="i in 3" :key="i" class="star" :class="{ on: i <= stars }">★</span>
      </div>
      <div class="result-title">全部通关！</div>
      <div class="result-sub">{{ levels.length }} 个关卡全部制霸</div>
      <div class="result-stats">
        <div class="stat"><span class="stat-v">{{ totalCorrect }}/{{ totalAnswered }}</span><span class="stat-k">正确率 {{ accuracy }}%</span></div>
        <div class="stat"><span class="stat-v">x{{ maxCombo }}</span><span class="stat-k">最高连击</span></div>
        <div class="stat"><span class="stat-v">{{ score }}</span><span class="stat-k">总得分</span></div>
      </div>
      <button class="cta-btn" @click="restart">再来一局</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { speak } from '../../utils/tts'

const props = defineProps({
  /** 词库：[{word, meaning, phonetic}] */
  words: { type: Array, required: true },
  /** TTS 语言（如 en-US） */
  lang: { type: String, default: 'en-US' },
  /** 关卡配置 */
  levels: {
    type: Array,
    default: () => [
      { name: '热身训练', desc: '不限时作答，先熟悉单词', questions: 8, timeLimit: 0 },
      { name: '限时初练', desc: '每题限时 15 秒，从容作答', questions: 8, timeLimit: 15 },
      { name: '稳步进阶', desc: '每题限时 10 秒，考验反应', questions: 8, timeLimit: 10 },
      { name: '高手过招', desc: '每题限时 8 秒，节奏加快', questions: 8, timeLimit: 8 },
      { name: '闪电风暴', desc: '每题限时 6 秒，手速对决', questions: 8, timeLimit: 6 },
      { name: '大师之路', desc: '每题限时 5 秒，登顶挑战', questions: 8, timeLimit: 5 }
    ]
  }
})
const emit = defineEmits(['finished'])

// ------- 状态 -------
const phase = ref('intro') // intro | playing | passed | gameover | complete
const levelIndex = ref(0)
const qIndex = ref(0)
const lives = ref(3)
const score = ref(0)
const combo = ref(0)
const maxCombo = ref(0)
const levelCorrect = ref(0)
const totalCorrect = ref(0)
const totalAnswered = ref(0)
const timeLeft = ref(0)
const answered = ref(false)
const lastCorrect = ref(false)
const selectedIndex = ref(-1)
const lastGain = ref(0)
const gainFlash = ref(false)
const gameOverReason = ref('')
const qKey = ref(0)

// 作答记录（提交后端用）
const answerLog = ref([])

const level = computed(() => props.levels[Math.min(levelIndex.value, props.levels.length - 1)])
const stars = computed(() => {
  const acc = totalAnswered.value ? totalCorrect.value / totalAnswered.value : 0
  if (acc >= 0.9) return 3
  if (acc >= 0.75) return 2
  return 1
})
const accuracy = computed(() =>
  totalAnswered.value ? Math.round((totalCorrect.value / totalAnswered.value) * 100) : 0
)
const timePercent = computed(() =>
  level.value.timeLimit > 0 ? (timeLeft.value / level.value.timeLimit) * 100 : 100
)
const feedbackText = computed(() => {
  if (!question.value) return ''
  return question.value.isWordToMeaning
    ? `正确释义：${question.value.answer}`
    : `正确单词：${question.value.answer}`
})

// ------- 当前题目 -------
const question = ref(null)
let levelPool = []
let askedWords = []

function shuffle(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

/** 生成题目：随机「显示英文选中文」或「显示中文选英文」，5 个选项 */
function makeQuestion(target) {
  const isWordToMeaning = Math.random() < 0.5
  const display = isWordToMeaning ? target.word : target.meaning
  const answer = isWordToMeaning ? target.meaning : target.word
  // 混淆项：优先选未考过的词
  const others = props.words.filter((w) => w.word !== target.word)
  const preferred = others.filter((w) => !askedWords.includes(w.word))
  const pool = preferred.length >= 4 ? preferred : others
  const distractors = shuffle(pool).slice(0, 4).map((w) => (isWordToMeaning ? w.meaning : w.word))
  const options = shuffle([answer, ...distractors])
  return {
    target,
    display,
    answer,
    options,
    answerIndex: options.indexOf(answer),
    isWordToMeaning,
    phonetic: target.phonetic || ''
  }
}

/** 开始一个关卡 */
function startLevel() {
  if (!props.words.length) {
    ElMessage.error('词库为空')
    return
  }
  // 本关题池：优先考未考过的词
  const fresh = props.words.filter((w) => !askedWords.includes(w.word))
  const pool = fresh.length >= level.value.questions ? fresh : props.words
  levelPool = shuffle(pool).slice(0, Math.min(level.value.questions, pool.length))
  // 词库不足时循环补齐
  while (levelPool.length < level.value.questions) {
    levelPool.push(props.words[Math.floor(Math.random() * props.words.length)])
  }
  levelCorrect.value = 0
  qIndex.value = 0
  phase.value = 'playing'
  showQuestion()
}

function showQuestion() {
  const target = levelPool[qIndex.value]
  askedWords.push(target.word)
  question.value = makeQuestion(target)
  answered.value = false
  selectedIndex.value = -1
  qKey.value++
  // 限时题启动计时
  if (level.value.timeLimit > 0) {
    timeLeft.value = level.value.timeLimit
    startTimer()
  }
}

let timer = null
function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    timeLeft.value -= 0.1
    if (timeLeft.value <= 0) {
      timeLeft.value = 0
      stopTimer()
      // 超时视为答错
      resolveAnswer(-1)
    }
  }, 100)
}
function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function optionClass(i) {
  if (!answered.value) return ''
  if (i === question.value.answerIndex) return 'correct'
  if (i === selectedIndex.value) return 'wrong'
  return 'dim'
}

function speakQuestion() {
  if (question.value) speak(question.value.display, props.lang)
}

/** 作答 */
function choose(i) {
  if (answered.value) return
  stopTimer()
  resolveAnswer(i)
}

let resolveTimer = null
function resolveAnswer(i) {
  answered.value = true
  selectedIndex.value = i
  lastCorrect.value = i === question.value.answerIndex
  totalAnswered.value++

  const target = question.value.target
  answerLog.value.push({
    word: target.word,
    meaning: target.meaning,
    correct: lastCorrect.value
  })

  if (lastCorrect.value) {
    totalCorrect.value++
    levelCorrect.value++
    combo.value++
    maxCombo.value = Math.max(maxCombo.value, combo.value)
    // 计分：基础 100 + 时间奖励 + 连击奖励；连击≥5 双倍
    let gain = 100
    if (level.value.timeLimit > 0) {
      gain += Math.round(Math.max(0, timeLeft.value) * 10)
    }
    if (combo.value >= 2) {
      gain += combo.value * 20
    }
    if (combo.value >= 5) {
      gain *= 2
    }
    score.value += gain
    lastGain.value = gain
    gainFlash.value = true
    setTimeout(() => (gainFlash.value = false), 800)
  } else {
    combo.value = 0
    lives.value--
    if (lives.value <= 0) {
      // 生命耗尽，短暂展示错误后结束
      resolveTimer = setTimeout(() => {
        gameOverReason.value = '生命耗尽，重整旗鼓再来！'
        finishGame('gameover')
      }, 1400)
      return
    }
  }

  resolveTimer = setTimeout(() => nextQuestion(), 1000)
}

function nextQuestion() {
  qIndex.value++
  if (qIndex.value >= level.value.questions) {
    // 本关结束：判定通过率
    const rate = levelCorrect.value / level.value.questions
    if (rate >= 0.75) {
      // 生命奖励
      score.value += lives.value * 200
      phase.value = 'passed'
    } else {
      gameOverReason.value = `通过率 ${Math.round(rate * 100)}%，差一点点（需 75%）`
      finishGame('gameover')
    }
  } else {
    showQuestion()
  }
}

function nextLevel() {
  levelIndex.value++
  if (levelIndex.value >= props.levels.length) {
    finishGame('complete')
    return
  }
  phase.value = 'intro'
}

function finishGame(endPhase) {
  stopTimer()
  phase.value = endPhase
  emit('finished', {
    score: accuracy.value,
    correctCount: totalCorrect.value,
    totalCount: totalAnswered.value,
    words: answerLog.value,
    completed: endPhase === 'complete'
  })
}

function restart() {
  levelIndex.value = 0
  lives.value = 3
  score.value = 0
  combo.value = 0
  maxCombo.value = 0
  totalCorrect.value = 0
  totalAnswered.value = 0
  askedWords = []
  answerLog.value = []
  phase.value = 'intro'
}

onBeforeUnmount(() => {
  stopTimer()
  if (resolveTimer) clearTimeout(resolveTimer)
})

onMounted(() => {
  if (!props.words.length) {
    ElMessage.error('词库为空')
  }
})
</script>

<style scoped>
.quiz-wrap {
  max-width: 640px;
  margin: 0 auto;
}

/* ===== 通用舞台卡 ===== */
.stage-card {
  position: relative;
  border-radius: 24px;
  padding: 32px 28px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(120, 150, 255, 0.16);
  overflow: hidden;
}

/* ===== 关卡开场 ===== */
.intro-card {
  text-align: center;
  padding: 56px 32px;
}

.intro-glow {
  position: absolute;
  top: -80px;
  left: 50%;
  transform: translateX(-50%);
  width: 320px;
  height: 220px;
  background: radial-gradient(ellipse at center, rgba(79, 124, 255, 0.25), transparent 70%);
  pointer-events: none;
}

.level-badge {
  display: inline-block;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 4px;
  color: #22d3ee;
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 20px;
  padding: 6px 18px;
  margin-bottom: 18px;
}

.level-total {
  color: var(--ll-text-muted);
  margin-left: 4px;
  letter-spacing: 0;
}

.level-name {
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 10px;
}

.level-desc {
  color: var(--ll-text-muted);
  font-size: 14px;
  margin-bottom: 24px;
}

.rule-chips {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 32px;
}

.chip {
  font-size: 12px;
  color: var(--ll-text);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(120, 150, 255, 0.2);
  border-radius: 16px;
  padding: 5px 14px;
}

/* ===== 主按钮 ===== */
.cta-btn {
  font-family: inherit;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #04122e;
  background: linear-gradient(135deg, #4f7cff 0%, #22d3ee 100%);
  border: none;
  border-radius: 24px;
  padding: 14px 48px;
  cursor: pointer;
  box-shadow: 0 8px 28px rgba(79, 124, 255, 0.4);
  transition: all 0.2s;
}

.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 36px rgba(34, 211, 238, 0.45);
}

.cta-btn:active {
  transform: translateY(0) scale(0.97);
}

/* ===== 答题 ===== */
.hud {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.lives {
  display: flex;
  gap: 4px;
}

.life {
  font-size: 20px;
  filter: drop-shadow(0 0 6px rgba(248, 113, 113, 0.6));
  transition: all 0.3s;
}

.life.lost {
  filter: grayscale(1);
  opacity: 0.25;
  transform: scale(0.85);
}

.hud-center {
  display: flex;
  gap: 8px;
}

.hud-chip {
  font-size: 12px;
  font-weight: 700;
  color: var(--ll-text);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(120, 150, 255, 0.2);
  border-radius: 14px;
  padding: 4px 12px;
}

.combo-chip.hot {
  color: #fbbf24;
  border-color: rgba(251, 191, 36, 0.5);
  box-shadow: 0 0 14px rgba(251, 191, 36, 0.25);
  animation: combo-pop 0.3s ease;
}

@keyframes combo-pop {
  50% { transform: scale(1.15); }
}

.hud-right {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.score-num {
  font-size: 24px;
  font-weight: 800;
  color: #22d3ee;
  font-variant-numeric: tabular-nums;
}

.score-label {
  font-size: 12px;
  color: var(--ll-text-muted);
}

/* 时间条 */
.time-bar {
  height: 5px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
  margin-bottom: 8px;
}

.time-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  transition: width 0.1s linear;
}

.time-fill.danger {
  background: linear-gradient(90deg, #f87171, #fbbf24);
  animation: time-blink 0.5s ease infinite;
}

@keyframes time-blink {
  50% { opacity: 0.6; }
}

/* 题目进度 */
.progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.progress-track {
  flex: 1;
  height: 4px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--ll-text-muted);
  font-variant-numeric: tabular-nums;
}

/* 题卡 */
.question-card {
  position: relative;
  text-align: center;
  background: linear-gradient(160deg, rgba(79, 124, 255, 0.1), rgba(34, 211, 238, 0.04));
  border: 1px solid rgba(120, 150, 255, 0.25);
  border-radius: 20px;
  padding: 34px 20px 28px;
  margin-bottom: 20px;
  animation: q-in 0.35s ease;
}

@keyframes q-in {
  from { opacity: 0; transform: translateY(12px) scale(0.98); }
  to { opacity: 1; transform: none; }
}

.q-label {
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--ll-cyan);
  margin-bottom: 14px;
}

.q-word {
  font-size: 44px;
  font-weight: 800;
  line-height: 1.2;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  word-break: break-word;
}

.q-phonetic {
  color: var(--ll-text-muted);
  font-size: 14px;
  margin-top: 8px;
}

.q-voice {
  position: absolute;
  top: 12px;
  right: 14px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-size: 15px;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.06));
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
  transition: all 0.2s;
}

.q-voice:hover {
  border-color: rgba(34, 211, 238, 0.6);
  box-shadow: 0 0 14px rgba(34, 211, 238, 0.3);
}

/* 选项 */
.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option {
  display: flex;
  align-items: center;
  gap: 14px;
  font-family: inherit;
  font-size: 16px;
  color: var(--ll-text);
  text-align: left;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(120, 150, 255, 0.18);
  border-radius: 14px;
  padding: 13px 18px;
  cursor: pointer;
  transition: all 0.15s;
}

.option:hover:not(:disabled) {
  border-color: rgba(34, 211, 238, 0.55);
  background: rgba(34, 211, 238, 0.05);
  transform: translateX(4px);
}

.option:disabled {
  cursor: default;
}

.opt-key {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  color: #a5b4fc;
  background: rgba(79, 124, 255, 0.16);
  border: 1px solid rgba(79, 124, 255, 0.25);
}

.opt-text {
  flex: 1;
  min-width: 0;
}

.opt-mark {
  font-size: 16px;
  font-weight: 800;
  color: #34d399;
}

.opt-mark.wrong {
  color: #f87171;
}

.option.correct {
  border-color: rgba(52, 211, 153, 0.7);
  background: rgba(52, 211, 153, 0.1);
  animation: opt-correct 0.4s ease;
}

.option.correct .opt-key {
  color: #34d399;
  background: rgba(52, 211, 153, 0.15);
  border-color: rgba(52, 211, 153, 0.5);
}

@keyframes opt-correct {
  30% { transform: scale(1.02); }
}

.option.wrong {
  border-color: rgba(248, 113, 113, 0.7);
  background: rgba(248, 113, 113, 0.1);
  animation: opt-shake 0.4s ease;
}

.option.wrong .opt-key {
  color: #f87171;
  background: rgba(248, 113, 113, 0.15);
  border-color: rgba(248, 113, 113, 0.5);
}

@keyframes opt-shake {
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}

.option.dim {
  opacity: 0.4;
}

/* 反馈条 */
.feedback {
  text-align: center;
  margin-top: 16px;
  font-size: 15px;
  font-weight: 700;
  animation: q-in 0.25s ease;
}

.feedback.ok {
  color: #34d399;
}

.feedback.bad {
  color: #f87171;
}

/* ===== 结果卡 ===== */
.result-card {
  text-align: center;
  padding: 48px 32px;
}

.result-emoji {
  font-size: 56px;
  margin-bottom: 16px;
  animation: emoji-in 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes emoji-in {
  from { transform: scale(0) rotate(-30deg); }
  to { transform: scale(1) rotate(0); }
}

.crown {
  font-size: 56px;
  margin-bottom: 12px;
  filter: drop-shadow(0 0 24px rgba(251, 191, 36, 0.6));
  animation: emoji-in 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.result-title {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 6px;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.result-sub {
  color: var(--ll-text-muted);
  font-size: 14px;
  margin-bottom: 28px;
}

.result-stats {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-bottom: 28px;
}

.stat {
  flex: 1;
  max-width: 140px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(120, 150, 255, 0.16);
  border-radius: 14px;
  padding: 14px 8px;
}

.stat-v {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: var(--ll-text);
  margin-bottom: 4px;
}

.stat-k {
  font-size: 12px;
  color: var(--ll-text-muted);
}

.life-bonus {
  font-size: 13px;
  color: #fbbf24;
  margin: -16px 0 24px;
}

.result-card.fail .result-title {
  background: linear-gradient(90deg, #f87171, #fbbf24);
  -webkit-background-clip: text;
  background-clip: text;
}

.result-stars {
  font-size: 44px;
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
}

.star {
  color: rgba(255, 255, 255, 0.12);
}

.star.on {
  color: #fbbf24;
  text-shadow: 0 0 18px rgba(251, 191, 36, 0.75);
  animation: star-pop 0.5s ease;
}

@keyframes star-pop {
  0% { transform: scale(0); }
  70% { transform: scale(1.35); }
  100% { transform: scale(1); }
}

/* ---------- 移动端适配 ---------- */
@media (max-width: 768px) {
  .stage-card {
    padding: 22px 16px;
  }

  .intro-card {
    padding: 40px 16px;
  }

  .result-stats {
    gap: 8px;
  }

  .stat {
    padding: 12px 4px;
  }
}
</style>
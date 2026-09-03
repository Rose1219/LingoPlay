<template>
  <div class="mini-home">
    <!-- 每日单词 · 极简卡片 -->
    <div class="daily-card" :class="{ revealed }" @click="revealed = !revealed">
      <div class="daily-top">
        <div class="daily-label-row">
          <span class="daily-label">每日单词</span>
          <span v-if="daily && daily.isNew" class="new-badge">NEW</span>
        </div>
        <div class="daily-tags" v-if="daily">
          <el-tag v-if="daily.level" size="small" effect="dark" class="level-tag">{{ daily.level }}</el-tag>
          <el-tag v-if="daily.languageName" size="small" effect="plain" class="daily-lang">
            {{ daily.icon }} {{ daily.languageName }}
          </el-tag>
        </div>
      </div>

      <div v-if="daily" class="daily-body">
        <div class="daily-word">{{ daily.word }}</div>
        <div class="daily-phonetic" v-if="daily.phonetic">/ {{ daily.phonetic }} /</div>
        <div class="daily-meaning" :class="{ hidden: !revealed }">
          {{ revealed ? daily.meaning : '点击卡片查看释义' }}
        </div>
      </div>

      <div class="daily-example" v-if="daily && revealed">
        <div class="example-row">
          <span class="example-text">{{ daily.example }}</span>
          <button class="example-voice" title="朗读例句" @click.stop="playExample">
            <span class="example-voice-icon">🔊</span>
          </button>
        </div>
        <div class="example-trans text-muted text-sm">{{ daily.translation }}</div>
      </div>

      <!-- 词汇进度：已收入单词本的词 / 词库总量 -->
      <div class="daily-progress" v-if="daily && daily.totalCount">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <span class="progress-text">词汇进度 {{ daily.learnedCount || 0 }} / {{ daily.totalCount }}</span>
      </div>
    </div>

    <!-- 玻璃拟态操作按钮（常驻显示） -->
    <div class="glass-tools">
      <button class="glass-btn glass-primary" :disabled="!daily" @click="playWord">
        <span class="glass-emoji">🔊</span>
        <span>播放发音</span>
      </button>
      <button class="glass-btn" :disabled="loadingWord" @click="nextWord">
        <span class="glass-emoji" :class="{ spinning: loadingWord }">🔄</span>
        <span>换一个</span>
      </button>
    </div>

    <!-- 单词闯关入口 -->
    <button class="play-btn" @click="goPlay">
      <span class="play-emoji">🎯</span>
      <span class="play-text">
        <span class="play-title">单词闯关</span>
        <span class="play-sub">语种选择 · 六关挑战 · 难度递增</span>
      </span>
      <span class="play-arrow">→</span>
    </button>

    <!-- 翻译 & VIP 快捷入口 -->
    <div class="quick-entries">
      <button class="quick-entry" @click="router.push('/translate')">
        <span class="quick-emoji">🌐</span>
        <span class="quick-text">
          <span class="quick-title">在线翻译</span>
          <span class="quick-sub">多语种互译 · 自动检测</span>
        </span>
      </button>
      <button class="quick-entry quick-vip" @click="router.push('/vip')">
        <span class="quick-emoji">👑</span>
        <span class="quick-text">
          <span class="quick-title">VIP 会员</span>
          <span class="quick-sub">解锁方言课程 · ¥5/月</span>
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { gameApi } from '../api'
import { speak } from '../utils/tts'

const router = useRouter()
const daily = ref(null)
const revealed = ref(false)
const loadingWord = ref(false)

const progressPercent = computed(() => {
  if (!daily.value || !daily.value.totalCount) return 0
  return Math.min(100, Math.round(((daily.value.learnedCount || 0) / daily.value.totalCount) * 100))
})

async function loadDaily(random) {
  loadingWord.value = true
  try {
    // 至少保持 600ms 加载态，让换词的旋转反馈可感知
    const [data] = await Promise.all([
      gameApi.dailyWord(random ? { random: true } : {}),
      new Promise((resolve) => setTimeout(resolve, 600))
    ])
    daily.value = data
    revealed.value = false
  } catch (e) {
    ElMessage.error('单词加载失败')
  } finally {
    loadingWord.value = false
  }
}

function nextWord() {
  loadDaily(true)
}

function playWord() {
  if (!daily.value) return
  const locale = { en: 'en-US', ja: 'ja-JP', ko: 'ko-KR' }[daily.value.languageCode] || 'en-US'
  speak(daily.value.word, locale)
}

function playExample() {
  if (!daily.value || !daily.value.example) return
  const locale = { en: 'en-US', ja: 'ja-JP', ko: 'ko-KR' }[daily.value.languageCode] || 'en-US'
  speak(daily.value.example, locale, 0.8)
}

function goPlay() {
  router.push('/word-quiz')
}

onMounted(() => {
  loadDaily(false)
})
</script>

<style scoped>
.mini-home {
  max-width: 480px;
  margin: 0 auto;
  padding-top: 8vh;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ---- 单词卡片 ---- */
.daily-card {
  border: 1px solid rgba(120, 150, 255, 0.22);
  border-radius: 20px;
  padding: 26px 28px 22px;
  cursor: pointer;
  transition: all 0.25s;
  background: transparent;
}

.daily-card:hover {
  border-color: rgba(34, 211, 238, 0.5);
  box-shadow: 0 0 32px rgba(34, 211, 238, 0.12);
}

.daily-card.revealed {
  border-color: rgba(79, 124, 255, 0.45);
}

.daily-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.daily-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.daily-label {
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--ll-cyan);
  text-transform: uppercase;
}

/* 新词徽标 */
.new-badge {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #fff;
  padding: 1px 7px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f43f5e, #f97316);
  box-shadow: 0 2px 8px rgba(244, 63, 94, 0.4);
}

.daily-tags {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 难度等级徽标：随词量解锁 A1→A2→B1→B2 */
.level-tag {
  border: none;
  background: linear-gradient(135deg, #4f7cff, #22d3ee);
  color: #fff;
  font-weight: 700;
}

/* 词汇进度条 */
.daily-progress {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-bar {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: rgba(120, 150, 255, 0.15);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  transition: width 0.4s ease;
}

.progress-text {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--ll-text-muted);
}

.daily-body {
  text-align: center;
  padding: 8px 0 4px;
}

.daily-word {
  font-size: 48px;
  font-weight: 800;
  line-height: 1.15;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.daily-phonetic {
  color: var(--ll-text-muted);
  margin-top: 8px;
  font-size: 14px;
}

.daily-meaning {
  margin-top: 16px;
  font-size: 16px;
  color: var(--ll-text);
  min-height: 22px;
  transition: color 0.2s;
}

.daily-meaning.hidden {
  color: var(--ll-text-muted);
  font-size: 12px;
}

.daily-example {
  margin-top: 16px;
  border-top: 1px dashed rgba(120, 150, 255, 0.18);
  padding-top: 12px;
  text-align: center;
  font-size: 14px;
  color: var(--ll-text);
}

.example-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.example-text {
  flex: 1;
  min-width: 0;
  text-align: right;
}

/* 例句朗读：迷你玻璃圆钮（与下方操作按钮同风格） */
.example-voice {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0.06) 100%);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    0 4px 14px rgba(0, 0, 0, 0.26);
  transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1),
    background 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.example-voice:hover {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.1) 100%);
  border-color: rgba(255, 255, 255, 0.34);
  transform: translateY(-1px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.34),
    0 6px 18px rgba(0, 0, 0, 0.34);
}

.example-voice:active {
  transform: translateY(0) scale(0.94);
}

.example-voice-icon {
  font-size: 14px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.3));
}

.example-trans {
  margin-top: 4px;
}

/* ---- 苹果玻璃拟态操作按钮 ---- */
.glass-tools {
  display: flex;
  gap: 12px;
}

.glass-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  height: 48px;
  padding: 0 22px;
  border-radius: 24px;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #fff;
  cursor: pointer;
  /* 液态玻璃：磨砂模糊 + 饱和度提升 + 半透明高光 */
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0.06) 100%);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    inset 0 -1px 0 rgba(255, 255, 255, 0.04),
    0 6px 22px rgba(0, 0, 0, 0.28);
  transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1),
    background 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.glass-btn:hover:not(:disabled) {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.1) 100%);
  border-color: rgba(255, 255, 255, 0.34);
  transform: translateY(-2px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.34),
    0 10px 30px rgba(0, 0, 0, 0.36);
}

.glass-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
}

.glass-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 主操作（播放发音）：玻璃中透出品牌渐变 */
.glass-primary {
  background: linear-gradient(135deg, rgba(79, 124, 255, 0.5) 0%, rgba(34, 211, 238, 0.32) 100%);
  border-color: rgba(160, 200, 255, 0.35);
  text-shadow: 0 1px 2px rgba(4, 18, 46, 0.4);
}

.glass-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(79, 124, 255, 0.62) 0%, rgba(34, 211, 238, 0.42) 100%);
  border-color: rgba(190, 225, 255, 0.45);
}

.glass-emoji {
  font-size: 17px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.3));
}

.glass-emoji.spinning {
  animation: glass-spin 0.8s linear infinite;
}

@keyframes glass-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ---- 翻译 & VIP 快捷入口 ---- */
.quick-entries {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.quick-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(120, 150, 255, 0.22);
  border-radius: 16px;
  padding: 14px 16px;
  cursor: pointer;
  font-family: inherit;
  color: var(--ll-text, #e8ecf8);
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(12px);
  transition: all 0.2s;
}

.quick-entry:hover {
  transform: translateY(-2px);
  border-color: rgba(34, 211, 238, 0.45);
  background: rgba(34, 211, 238, 0.06);
}

.quick-vip {
  border-color: rgba(255, 184, 79, 0.3);
  background: rgba(255, 184, 79, 0.06);
}

.quick-vip:hover {
  border-color: rgba(255, 184, 79, 0.55);
  background: rgba(255, 184, 79, 0.1);
}

.quick-emoji {
  font-size: 26px;
}

.quick-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}

.quick-title {
  font-size: 14px;
  font-weight: 800;
}

.quick-sub {
  font-size: 11px;
  color: var(--ll-text-muted, #8a97b8);
}

@media (max-width: 480px) {
  .quick-entries {
    grid-template-columns: 1fr;
  }
}

/* ---- 闯关按钮 ---- */
.play-btn {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  border: none;
  border-radius: 20px;
  padding: 20px 26px;
  cursor: pointer;
  font-family: inherit;
  color: #04122e;
  background: linear-gradient(135deg, #4f7cff 0%, #22d3ee 100%);
  box-shadow: 0 8px 32px rgba(79, 124, 255, 0.35);
  transition: all 0.2s;
}

.play-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(34, 211, 238, 0.4);
}

.play-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.play-emoji {
  font-size: 34px;
  filter: drop-shadow(0 0 10px rgba(4, 18, 46, 0.4));
}

.play-text {
  flex: 1;
  text-align: left;
}

.play-title {
  display: block;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}

.play-sub {
  display: block;
  font-size: 12px;
  opacity: 0.75;
  margin-top: 3px;
}

.play-arrow {
  font-size: 22px;
  font-weight: 700;
}

/* ---------- 移动端适配 ---------- */
@media (max-width: 768px) {
  .mini-home {
    padding-top: 3vh;
    gap: 14px;
  }

  .daily-card {
    padding: 20px 18px 18px;
  }

  .daily-word {
    font-size: 38px;
  }

  .glass-btn {
    height: 46px;
    font-size: 14px;
    padding: 0 14px;
  }

  .play-btn {
    padding: 16px 18px;
  }

  .play-emoji {
    font-size: 28px;
  }

  .play-title {
    font-size: 17px;
  }
}
</style>
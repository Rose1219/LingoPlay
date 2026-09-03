<template>
  <div class="page-container translate-page">
    <PageBack to="/" :label="t('common.back')" />
    <h1 class="page-title">{{ t('translate.title') }}</h1>
    <p class="page-subtitle">{{ t('translate.subtitle') }}</p>

    <div class="translator">
      <!-- 语种选择条 -->
      <div class="lang-bar">
        <el-select v-model="source" class="lang-select" size="large">
          <el-option :label="t('translate.auto')" value="auto">
            <span class="lang-option"><span class="lang-flag">🌐</span>{{ t('translate.auto') }}</span>
          </el-option>
          <el-option v-for="l in languages" :key="l.code" :label="l.nativeName" :value="l.code">
            <span class="lang-option"><span class="lang-flag">{{ l.flag }}</span>{{ l.nativeName }}</span>
          </el-option>
        </el-select>

        <button class="swap-btn" :title="t('translate.swap')" @click="swap">
          <el-icon :size="18"><RightLeft /></el-icon>
        </button>

        <el-select v-model="target" class="lang-select" size="large">
          <el-option v-for="l in languages" :key="l.code" :label="l.nativeName" :value="l.code">
            <span class="lang-option"><span class="lang-flag">{{ l.flag }}</span>{{ l.nativeName }}</span>
          </el-option>
        </el-select>
      </div>

      <!-- 输入 / 输出双栏 -->
      <div class="panels">
        <div class="panel">
          <el-input
            v-model="text"
            type="textarea"
            :rows="7"
            maxlength="2000"
            resize="none"
            :placeholder="t('translate.placeholder')"
            @keydown.enter.exact.prevent="doTranslate"
          />
          <div class="panel-foot">
            <span class="detect-tag" v-if="detectedName">
              {{ t('translate.detected') }}：{{ detectedName }}
            </span>
            <span class="char-count" :class="{ over: text.length >= 2000 }">{{ text.length }}/2000</span>
          </div>
        </div>

        <div class="panel result-panel">
          <div v-if="result" class="result-text">{{ result }}</div>
          <div v-else-if="translating" class="result-loading">
            <el-icon class="is-loading spin"><Loading /></el-icon>
            <span>{{ t('translate.translating') }}</span>
          </div>
          <div v-else class="result-empty">{{ t('translate.resultPlaceholder') }}</div>
          <div class="panel-foot" v-if="result">
            <button class="mini-btn" @click="copyResult">
              <el-icon><CopyDocument /></el-icon>{{ t('translate.copyResult') }}
            </button>
            <button class="mini-btn" @click="speakResult">
              <el-icon><VideoPlay /></el-icon>{{ t('translate.tts') }}
            </button>
          </div>
        </div>
      </div>

      <div class="actions">
        <el-button
          type="primary"
          size="large"
          round
          class="translate-btn"
          :loading="translating"
          @click="doTranslate"
        >
          {{ t('translate.translateBtn') }}
        </el-button>
      </div>

      <!-- 历史 -->
      <div class="history" v-if="history.length">
        <div class="history-head">
          <span class="history-title">{{ t('translate.history') }}</span>
          <el-button link size="small" @click="clearHistory">{{ t('translate.clearHistory') }}</el-button>
        </div>
        <button
          v-for="(h, i) in history"
          :key="i"
          class="history-item"
          @click="applyHistory(h)"
        >
          <span class="history-text">{{ h.text }}</span>
          <span class="history-arrow">→</span>
          <span class="history-result">{{ h.result }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { translateApi } from '../api'
import { speak } from '../utils/tts'
import PageBack from '../components/PageBack.vue'

const { t } = useI18n()

const languages = ref([])
const source = ref('auto')
// 默认目标语言：简体中文（输入英文时直接译为中文）
const target = ref('zh-CN')
const text = ref('')
const result = ref('')
const detectedName = ref('')
const detectedCode = ref('')
const translating = ref(false)
const history = ref([])

const ZH_DEFAULT = 'zh-CN'

function langName(code) {
  return (languages.value.find((l) => l.code === code) || {}).nativeName || code
}

const LANG_KEY = 'lingoplay-translate-history'

onMounted(async () => {
  try {
    languages.value = await translateApi.languages()
  } catch (e) { /* 下拉为空时仍可手动输入翻译 */ }
  try {
    history.value = JSON.parse(localStorage.getItem(LANG_KEY) || '[]')
  } catch (e) {
    history.value = []
  }
})

async function doTranslate() {
  const content = text.value.trim()
  if (!content) return
  if (content.length > 2000) {
    ElMessage.warning(t('translate.limit'))
    return
  }
  translating.value = true
  try {
    let r = await translateApi.translate(content, source.value, target.value)
    detectedCode.value = r.detected || ''
    // 同语言避让：源语言与目标相同（如英文→英文）时自动切换目标并重译，不回显原文
    if (r.detected && r.detected === target.value) {
      const alt = r.detected.startsWith('zh') ? 'en' : ZH_DEFAULT
      target.value = alt
      ElMessage.info(t('translate.autoSwitched', { lang: langName(alt) }))
      r = await translateApi.translate(content, source.value, target.value)
      detectedCode.value = r.detected || detectedCode.value
    }
    result.value = r.translated
    detectedName.value = r.detected ? langName(r.detected) : ''
    if (r.translated) {
      history.value.unshift({ text: content, result: r.translated, source: r.source, target: r.target })
      history.value = history.value.slice(0, 20)
      localStorage.setItem(LANG_KEY, JSON.stringify(history.value))
    }
  } catch (e) {
    ElMessage.error(t('translate.failed'))
  } finally {
    translating.value = false
  }
}

function swap() {
  if (source.value === 'auto') {
    const t0 = target.value
    source.value = t0
    // 目标优先切换为最近一次检测到的语种，避免互换后源/目标相同
    if (detectedCode.value && detectedCode.value !== t0 && languages.value.some((l) => l.code === detectedCode.value)) {
      target.value = detectedCode.value
    } else {
      target.value = t0.startsWith('zh') ? 'en' : ZH_DEFAULT
    }
    return
  }
  const s = source.value
  source.value = target.value
  target.value = s
  if (text.value && result.value) {
    const t2 = text.value
    text.value = result.value
    result.value = t2
  }
}

function applyHistory(h) {
  text.value = h.text
  result.value = h.result
}

function clearHistory() {
  history.value = []
  localStorage.removeItem(LANG_KEY)
}

async function copyResult() {
  try {
    await navigator.clipboard.writeText(result.value)
    ElMessage.success(t('common.copied'))
  } catch (e) {
    ElMessage.error(t('common.copy') + ' ✕')
  }
}

async function speakResult() {
  if (!result.value) return
  await speak(result.value, target.value)
}
</script>

<style scoped>
.translate-page {
  max-width: 860px;
  margin: 0 auto;
}

.lang-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0 14px;
}

.lang-select {
  flex: 1;
  min-width: 0;
}

.lang-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lang-flag {
  font-size: 16px;
}

.swap-btn {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid rgba(120, 150, 255, 0.35);
  background: rgba(79, 124, 255, 0.12);
  color: var(--ll-cyan, #22d3ee);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.25s ease, background 0.2s;
}

.swap-btn:hover {
  background: rgba(79, 124, 255, 0.22);
  transform: rotate(180deg);
}

.panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 640px) {
  .panels {
    grid-template-columns: 1fr;
  }
}

.panel {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(120, 150, 255, 0.18);
  border-radius: 14px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 190px;
}

.panel :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none;
  color: var(--ll-text, #e8ecf8);
  font-size: 15px;
  line-height: 1.7;
  flex: 1;
}

.panel-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 24px;
}

.detect-tag {
  font-size: 12px;
  color: var(--ll-cyan, #22d3ee);
  background: rgba(34, 211, 238, 0.10);
  border: 1px solid rgba(34, 211, 238, 0.3);
  padding: 2px 10px;
  border-radius: 20px;
}

.char-count {
  font-size: 12px;
  color: var(--ll-text-muted, #8a97b8);
  margin-left: auto;
}

.char-count.over {
  color: #f87171;
}

.result-panel {
  background: linear-gradient(135deg, rgba(79, 124, 255, 0.10), rgba(34, 211, 238, 0.05));
}

.result-text {
  flex: 1;
  font-size: 15px;
  line-height: 1.8;
  color: var(--ll-text, #e8ecf8);
  white-space: pre-wrap;
  word-break: break-word;
  overflow-y: auto;
}

.result-loading,
.result-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--ll-text-muted, #8a97b8);
  font-size: 14px;
}

.spin {
  font-size: 20px;
}

.mini-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid rgba(120, 150, 255, 0.3);
  background: rgba(255, 255, 255, 0.04);
  color: var(--ll-text, #e8ecf8);
  border-radius: 16px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s;
}

.mini-btn:hover {
  background: rgba(79, 124, 255, 0.16);
}

.actions {
  display: flex;
  justify-content: center;
  margin: 18px 0 6px;
}

.translate-btn {
  min-width: 200px;
  background: linear-gradient(90deg, #4f7cff, #22d3ee);
  border: none;
  font-weight: 700;
}

.history {
  margin-top: 22px;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.history-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ll-text, #e8ecf8);
}

.history-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(120, 150, 255, 0.12);
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 6px;
  cursor: pointer;
  color: var(--ll-text, #e8ecf8);
  font-family: inherit;
  font-size: 13px;
  transition: background 0.2s;
}

.history-item:hover {
  background: rgba(79, 124, 255, 0.10);
}

.history-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-arrow {
  color: var(--ll-text-muted, #8a97b8);
}

.history-result {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ll-cyan, #22d3ee);
}
</style>

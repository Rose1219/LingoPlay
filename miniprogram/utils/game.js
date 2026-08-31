// 游戏通用算法：洗牌 / 文本相似度 / 星级 / 模块信息

function shuffle(arr) {
  const a = arr.slice()
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const t = a[i]
    a[i] = a[j]
    a[j] = t
  }
  return a
}

function normalize(text) {
  return String(text || '')
    .toLowerCase()
    .replace(/[.,!?;:'"()[\]¡¿、。！？，；：]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function levenshtein(a, b) {
  const m = a.length
  const n = b.length
  if (m === 0) return n
  if (n === 0) return m
  const dp = []
  for (let i = 0; i <= m; i++) dp.push([i])
  for (let j = 0; j <= n; j++) dp[0][j] = j
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      dp[i][j] = a[i - 1] === b[j - 1]
        ? dp[i - 1][j - 1]
        : Math.min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
    }
  }
  return dp[m][n]
}

function similarity(a, b) {
  const na = normalize(a)
  const nb = normalize(b)
  if (!na && !nb) return 1
  if (!na || !nb) return 0
  return Math.max(0, 1 - levenshtein(na, nb) / Math.max(na.length, nb.length))
}

const scoreOf = (sim) => Math.round(sim * 100)

function starsOf(score) {
  if (score == null) return 0
  if (score >= 90) return 3
  if (score >= 60) return 2
  return 1
}

const TYPE_MAP = {
  WORD: { label: '单词大作战', emoji: '🧩', color: '#4f7cff' },
  GRAMMAR: { label: '语法探险', emoji: '🗺️', color: '#818cf8' },
  SPEAK: { label: '口语星球', emoji: '🎙️', color: '#22d3ee' },
  LISTEN: { label: '听力侦探', emoji: '🕵️', color: '#f59e0b' },
  DIALOG: { label: '对话剧场', emoji: '🎭', color: '#ec4899' }
}

const typeInfo = (type) => TYPE_MAP[type] || { label: type, emoji: '🎯', color: '#909399' }

function relativeTime(dateStr) {
  if (!dateStr) return ''
  const normalized = String(dateStr).replace('T', ' ')
  const time = new Date(normalized.replace(/-/g, '/')).getTime()
  if (isNaN(time)) return ''
  const diff = (Date.now() - time) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 86400 * 30) return Math.floor(diff / 86400) + ' 天前'
  const d = new Date(time)
  return d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate()
}

module.exports = { shuffle, similarity, scoreOf, starsOf, typeInfo, relativeTime }
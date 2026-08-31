// 文本相似度工具（用于口语/听力判分）

/** 归一化：小写、去标点、压缩空格 */
function normalize(text) {
  return String(text || '')
    .toLowerCase()
    .replace(/[.,!?;:'"()\[\]¡!¿?、。！？，；：]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

/** 编辑距离 */
export function levenshtein(a, b) {
  const m = a.length
  const n = b.length
  if (m === 0) return n
  if (n === 0) return m
  const dp = []
  for (let i = 0; i <= m; i++) {
    dp[i] = [i]
  }
  for (let j = 0; j <= n; j++) {
    dp[0][j] = j
  }
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      dp[i][j] = a[i - 1] === b[j - 1]
        ? dp[i - 1][j - 1]
        : Math.min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
    }
  }
  return dp[m][n]
}

/** 相似度 0-1（归一化后按编辑距离计算） */
export function similarity(a, b) {
  const na = normalize(a)
  const nb = normalize(b)
  if (!na && !nb) return 1
  if (!na || !nb) return 0
  const dist = levenshtein(na, nb)
  return Math.max(0, 1 - dist / Math.max(na.length, nb.length))
}

/** 相似度 → 百分制得分 */
export function scoreOf(sim) {
  return Math.round(sim * 100)
}
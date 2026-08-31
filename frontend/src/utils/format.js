// 显示格式化工具

/** 相对时间：刚刚 / n 分钟前 / n 小时前 / n 天前 / 日期 */
export function relativeTime(dateStr) {
  if (!dateStr) return ''
  // 兼容 ISO 格式（2026-08-26T15:54:56）与普通格式（2026-08-26 15:54:56）
  const normalized = String(dateStr).replace('T', ' ')
  const time = new Date(normalized.replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return ''
  const diff = (Date.now() - time) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 86400 * 30) return `${Math.floor(diff / 86400)} 天前`
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 模块类型 → 中文名与图标 */
export function typeInfo(type) {
  const map = {
    WORD: { label: '单词大作战', icon: 'Memo', color: '#4f7cff' },
    GRAMMAR: { label: '语法探险', icon: 'EditPen', color: '#818cf8' },
    SPEAK: { label: '口语星球', icon: 'Microphone', color: '#22d3ee' },
    LISTEN: { label: '听力侦探', icon: 'Headset', color: '#f59e0b' },
    DIALOG: { label: '对话剧场', icon: 'ChatDotRound', color: '#ec4899' }
  }
  return map[type] || { label: type, icon: 'Reading', color: '#909399' }
}

/** 分数 → 星级（1-3 星） */
export function starsOf(score) {
  if (!score && score !== 0) return 0
  if (score >= 90) return 3
  if (score >= 60) return 2
  return 1
}
<template>
  <div>
    <div class="heatmap" :title="tip">
      <template v-for="(cell, i) in cells" :key="i">
        <div
          v-if="cell"
          class="heatmap-cell"
          :class="level(cell.minutes)"
          :title="`${cell.date}：学习 ${cell.minutes} 分钟`"
        />
        <div v-else class="heatmap-cell heatmap-pad" />
      </template>
    </div>
    <div class="heatmap-legend">
      <span class="text-muted text-sm">少</span>
      <span v-for="(lv, i) in ['', 'lv1', 'lv2', 'lv3', 'lv4']" :key="i" class="heatmap-cell" :class="lv" />
      <span class="text-muted text-sm">多</span>
    </div>
  </div>
</template>

<script setup>
// GitHub 风格学习热力图：每列一周，行为周几
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] } // [{ date: '2026-08-26', minutes: 30 }]
})

const cells = computed(() => {
  if (!props.items.length) return []
  const first = new Date(props.items[0].date.replace(/-/g, '/'))
  const offset = first.getDay() // 0=周日
  const padded = []
  for (let i = 0; i < offset; i++) {
    padded.push(null)
  }
  props.items.forEach((it) => padded.push(it))
  return padded
})

const tip = computed(() => {
  const total = props.items.reduce((s, it) => s + (it.minutes || 0), 0)
  return `最近 ${props.items.length} 天共学习 ${total} 分钟`
})

function level(minutes) {
  if (!minutes) return ''
  if (minutes < 10) return 'lv1'
  if (minutes < 20) return 'lv2'
  if (minutes < 30) return 'lv3'
  return 'lv4'
}
</script>

<style scoped>
.heatmap-pad {
  visibility: hidden;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}
</style>
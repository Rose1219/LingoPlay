<template>
  <div class="page-container" v-loading="loading">
    <PageBack to="/" label="返回游戏大厅" />
    <h1 class="page-title">学习进度</h1>
    <p class="page-subtitle">每一次打卡都算数，见证你的成长轨迹</p>

    <template v-if="stats">
      <el-row :gutter="16" class="mb">
        <el-col :xs="12" :sm="12" :md="6" v-for="card in statCards" :key="card.label">
          <el-card shadow="never" class="stat-card">
            <div class="stat-icon">{{ card.icon }}</div>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label text-muted text-sm">{{ card.label }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel">
            <template #header><span>🗓️ 最近 90 天学习热力图</span></template>
            <Heatmap :items="stats.heatmap" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel">
            <template #header><span>📊 各模块掌握度</span></template>
            <div v-if="!stats.typeAccuracy.length" class="empty-tip">
              完成课时后，这里会展示各学习模块的平均得分
            </div>
            <div v-for="ta in stats.typeAccuracy" :key="ta.type" class="type-row">
              <div class="type-head">
                <span class="type-name">{{ typeInfo(ta.type).label }}</span>
                <span class="text-muted text-sm">{{ ta.accuracy == null ? '暂无' : ta.accuracy + ' 分' }}</span>
              </div>
              <el-progress
                :percentage="ta.accuracy || 0"
                :stroke-width="10"
                :color="typeInfo(ta.type).color"
              />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { studyApi } from '../api'
import { typeInfo } from '../utils/format'
import Heatmap from '../components/Heatmap.vue'
import PageBack from '../components/PageBack.vue'

const stats = ref(null)
const loading = ref(false)

const statCards = computed(() => {
  const s = stats.value || {}
  return [
    { icon: '🔥', label: '连续打卡天数', value: s.streakDays || 0 },
    { icon: '⏱️', label: '累计学习分钟', value: s.totalMinutes || 0 },
    { icon: '🧠', label: '已掌握单词', value: `${s.masteredWords ?? 0} / ${s.wordsLearned || 0}` },
    { icon: '🎯', label: '完成课时', value: `${s.lessonsCompleted || 0} / ${s.lessonsStarted || 0}` }
  ]
})

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await studyApi.stats()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.mb {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  border-radius: 12px;
}

.stat-icon {
  font-size: 26px;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  margin: 8px 0 4px;
  color: var(--ll-text);
}

.panel {
  min-height: 260px;
}

.empty-tip {
  color: #8492a6;
  padding: 24px 0;
  text-align: center;
}

.type-row {
  margin-bottom: 18px;
}

.type-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.type-name {
  font-weight: 600;
  font-size: 14px;
}
</style>
<template>
  <div class="page-container" v-loading="loading">
    <PageBack to="/" label="返回游戏大厅" />
    <h1 class="page-title">成就殿堂</h1>
    <p class="page-subtitle">坚持学习，解锁属于你的荣誉勋章</p>

    <el-row :gutter="16">
      <el-col :xs="12" :sm="8" :md="6" v-for="a in achievements" :key="a.code">
        <div class="achievement-card" :class="{ unlocked: a.unlocked }">
          <div class="achievement-icon">{{ a.unlocked ? a.icon : '🔒' }}</div>
          <div class="achievement-name">{{ a.name }}</div>
          <div class="achievement-desc">{{ a.description }}</div>
          <div v-if="a.unlocked && a.unlockedAt" class="achievement-date text-muted text-sm">
            {{ unlockedDate(a.unlockedAt) }} 解锁
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { achievementApi } from '../api'
import PageBack from '../components/PageBack.vue'

const achievements = ref([])
const loading = ref(false)

function unlockedDate(dateStr) {
  return String(dateStr || '').slice(0, 10)
}

onMounted(async () => {
  loading.value = true
  try {
    achievements.value = await achievementApi.list()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.achievement-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(120, 150, 255, 0.14);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  padding: 22px 16px;
  text-align: center;
  margin-bottom: 16px;
  transition: all 0.2s;
  filter: grayscale(1);
  opacity: 0.5;
}

.achievement-card.unlocked {
  filter: none;
  opacity: 1;
  border-color: rgba(251, 191, 36, 0.55);
  background: linear-gradient(180deg, rgba(251, 191, 36, 0.08), rgba(255, 255, 255, 0.02));
  box-shadow: 0 4px 18px rgba(251, 191, 36, 0.18);
}

.achievement-icon {
  font-size: 44px;
  margin-bottom: 10px;
}

.achievement-name {
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 6px;
}

.achievement-desc {
  font-size: 12px;
  color: var(--ll-text-muted);
  margin-bottom: 8px;
}
</style>
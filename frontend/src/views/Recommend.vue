<template>
  <div class="page-container" v-loading="loading">
    <PageBack to="/" label="返回游戏大厅" />
    <h1 class="page-title">个性化推荐</h1>
    <p class="page-subtitle">根据你的学习进度与薄弱环节，定制专属学习路径</p>

    <template v-if="recommend">
      <!-- 学习建议 -->
      <el-alert type="success" :closable="false" class="suggest-box">
        <template #title>
          <b>今日学习建议</b>
        </template>
        {{ recommend.suggestion }}
      </el-alert>

      <el-row :gutter="16" class="mt">
        <el-col :span="14">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-header">
                <span>📚 为你推荐的下一步</span>
                <el-tag size="small" effect="plain" type="warning" v-if="recommend.weakType">
                  薄弱环节：{{ typeInfo(recommend.weakType).label }}
                </el-tag>
              </div>
            </template>
            <div v-if="!recommend.continueLessons.length" class="empty-tip">
              暂无推荐课程，去课程中心看看吧
            </div>
            <div
              v-for="item in recommend.continueLessons"
              :key="item.lessonId"
              class="rec-item hover-card"
              @click="$router.push(`/learn/${item.lessonId}`)"
            >
              <div class="rec-icon">{{ item.languageIcon }}</div>
              <div class="rec-info">
                <div class="rec-title">{{ item.title }}</div>
                <div class="text-muted text-sm">{{ item.courseTitle }} · {{ item.unitTitle }}</div>
              </div>
              <div class="rec-right">
                <el-tag size="small" effect="light">{{ typeInfo(item.type).label }}</el-tag>
                <el-button type="primary" size="small" round>开始学习</el-button>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-header">
                <span>🧠 今日待复习单词</span>
                <el-badge :value="recommend.todayReviewCount" type="warning" v-if="recommend.todayReviewCount" />
              </div>
            </template>
            <div v-if="!recommend.reviewWords.length" class="empty-tip">
              🎉 暂无到期单词，掌握得很好！
            </div>
            <div v-for="(w, i) in recommend.reviewWords" :key="i" class="review-item">
              <div class="review-word">
                <b>{{ w.word }}</b>
                <span class="text-muted">{{ w.meaning }}</span>
              </div>
              <div class="mastery">
                <span v-for="s in 4" :key="s" class="star" :class="{ on: s <= w.mastery }">★</span>
              </div>
            </div>
          </el-card>

          <!-- 记忆曲线说明 -->
          <el-card shadow="never" class="panel mt">
            <template #header><span>📈 记忆曲线复习法</span></template>
            <p class="text-muted text-sm" style="line-height: 1.8; margin: 0;">
              单词按掌握度分级安排复习：新学当天复习 → 1 天后 → 3 天后 → 7 天后，答对升星、答错降星。
              每节课后记得回来看看「今日待复习单词」，一次 10 个刚刚好。
            </p>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { recommendApi } from '../api'
import { typeInfo } from '../utils/format'
import PageBack from '../components/PageBack.vue'

const recommend = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    recommend.value = await recommendApi.get()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.suggest-box {
  border-radius: 10px;
}

.mt {
  margin-top: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-tip {
  color: var(--ll-text-muted);
  padding: 20px 0;
  text-align: center;
}

.rec-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid rgba(120, 150, 255, 0.14);
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.02);
}

.rec-icon {
  font-size: 26px;
}

.rec-info {
  flex: 1;
  min-width: 0;
}

.rec-title {
  font-weight: 600;
  margin-bottom: 2px;
}

.rec-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.review-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  border-bottom: 1px dashed rgba(120, 150, 255, 0.14);
}

.review-word {
  display: flex;
  gap: 10px;
  align-items: baseline;
  color: var(--ll-text);
}

.star {
  color: rgba(255, 255, 255, 0.14);
  font-size: 15px;
}

.star.on {
  color: #fbbf24;
}
</style>
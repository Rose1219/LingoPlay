<template>
  <div class="page-container" v-loading="loading">
    <PageBack label="返回社区列表" />
    <template v-if="detail">
      <el-card shadow="never" class="post-detail">
        <div class="post-head">
          <span class="post-avatar">{{ detail.post.authorAvatar }}</span>
          <div>
            <div class="post-author">{{ detail.post.authorNickname }}</div>
            <div class="text-muted text-sm">{{ relativeTime(detail.post.createdAt) }}</div>
          </div>
          <el-tag v-if="detail.post.languageName" size="small" effect="light" class="post-lang">
            {{ detail.post.languageIcon }} {{ detail.post.languageName }}
          </el-tag>
        </div>
        <h2 class="post-title">{{ detail.post.title }}</h2>
        <div class="post-body">{{ detail.post.content }}</div>
        <div class="post-actions">
          <el-button
            :type="detail.post.liked ? 'danger' : 'default'"
            round
            :icon="Pointer"
            @click="toggleLike"
          >
            {{ detail.post.liked ? '已点赞' : '点赞' }} {{ detail.post.likeCount }}
          </el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="comments">
        <template #header><span>💬 评论（{{ detail.comments.length }}）</span></template>
        <div v-if="!detail.comments.length" class="empty-tip">还没有评论，来抢沙发～</div>
        <div v-for="c in detail.comments" :key="c.id" class="comment-item">
          <span class="comment-avatar">{{ c.authorAvatar }}</span>
          <div class="comment-body">
            <div class="comment-head">
              <span class="comment-author">{{ c.authorNickname }}</span>
              <span class="text-muted text-sm">{{ relativeTime(c.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
        </div>
        <div class="comment-input">
          <el-input v-model="commentText" placeholder="友善交流，写下你的评论…" maxlength="1000" @keyup.enter="submitComment" />
          <el-button type="primary" :loading="sending" @click="submitComment">发表评论</el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { postApi } from '../api'
import { relativeTime } from '../utils/format'
import PageBack from '../components/PageBack.vue'

const route = useRoute()
const loading = ref(false)
const detail = ref(null)
const commentText = ref('')
const sending = ref(false)

async function load() {
  loading.value = true
  try {
    detail.value = await postApi.detail(route.params.id)
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  const data = await postApi.like(detail.value.post.id)
  detail.value.post.liked = data.liked
  detail.value.post.likeCount = data.likeCount
}

async function submitComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  sending.value = true
  try {
    const comment = await postApi.comment(detail.value.post.id, commentText.value)
    detail.value.comments.push({
      ...comment,
      authorAvatar: comment.authorAvatar || '🙂'
    })
    commentText.value = ''
  } finally {
    sending.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.post-detail {
  border-radius: 12px;
  margin-bottom: 16px;
}

.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.post-avatar {
  font-size: 30px;
}

.post-author {
  font-weight: 600;
  font-size: 14px;
}

.post-lang {
  margin-left: auto;
}

.post-title {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
}

.post-body {
  line-height: 1.8;
  white-space: pre-wrap;
  color: var(--ll-text);
  margin-bottom: 14px;
}

.post-actions {
  padding-top: 10px;
  border-top: 1px solid rgba(120, 150, 255, 0.14);
}

.comments {
  border-radius: 12px;
}

.empty-tip {
  color: #8492a6;
  padding: 16px 0;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(120, 150, 255, 0.14);
}

.comment-avatar {
  font-size: 22px;
}

.comment-body {
  flex: 1;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.comment-author {
  font-weight: 600;
  font-size: 13px;
}

.comment-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--ll-text);
}

.comment-input {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
</style>
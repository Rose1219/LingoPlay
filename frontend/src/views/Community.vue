<template>
  <div class="page-container">
    <PageBack to="/" label="返回游戏大厅" />
    <div class="head-row">
      <div>
        <h1 class="page-title">学习社区</h1>
        <p class="page-subtitle" style="margin-bottom: 0;">交流学习心得，寻找语伴，一起进步</p>
      </div>
      <el-button type="primary" round :icon="EditPen" @click="openCreate">发布帖子</el-button>
    </div>

    <el-tabs v-model="activeLang" @tab-change="loadPosts">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane v-for="lang in languages" :key="lang.code" :name="lang.code">
        <template #label>
          <span>{{ lang.icon }} {{ lang.nameCn }}</span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div v-loading="loading">
      <div v-for="post in posts" :key="post.id" class="post-card hover-card" @click="$router.push(`/community/${post.id}`)">
        <div class="post-head">
          <span class="post-avatar">{{ post.authorAvatar }}</span>
          <div>
            <div class="post-author">{{ post.authorNickname }}</div>
            <div class="text-muted text-sm">{{ relativeTime(post.createdAt) }}</div>
          </div>
          <el-tag v-if="post.languageName" size="small" effect="light" class="post-lang">
            {{ post.languageIcon }} {{ post.languageName }}
          </el-tag>
        </div>
        <div class="post-title">{{ post.title }}</div>
        <div class="post-content-preview">{{ post.content }}</div>
        <div class="post-foot">
          <span class="foot-item"><el-icon><Pointer /></el-icon> {{ post.likeCount }}</span>
          <span class="foot-item"><el-icon><ChatLineRound /></el-icon> {{ post.commentCount }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !posts.length" description="还没有帖子，来发布第一篇吧！" />
      <div class="pager" v-if="total > size">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPage"
        />
      </div>
    </div>

    <!-- 发帖弹窗 -->
    <el-dialog v-model="createVisible" title="发布帖子" width="560px">
      <el-form :model="createForm" label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="createForm.title" maxlength="200" show-word-limit placeholder="一句话说清主题" />
        </el-form-item>
        <el-form-item label="语种">
          <el-select v-model="createForm.languageCode" placeholder="选择关联语种（选填）" clearable style="width: 100%;">
            <el-option v-for="lang in languages" :key="lang.code" :label="`${lang.icon} ${lang.nameCn}`" :value="lang.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="6"
            maxlength="5000"
            show-word-limit
            placeholder="分享你的学习心得、疑问或打卡记录…"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Pointer, ChatLineRound } from '@element-plus/icons-vue'
import { languageApi, postApi } from '../api'
import { relativeTime } from '../utils/format'
import PageBack from '../components/PageBack.vue'

const languages = ref([])
const posts = ref([])
const activeLang = ref('')
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ title: '', content: '', languageCode: '' })

async function loadPosts() {
  loading.value = true
  try {
    const data = await postApi.list({ page: page.value, size, language: activeLang.value })
    posts.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  page.value = p
  loadPosts()
}

function openCreate() {
  createForm.title = ''
  createForm.content = ''
  createForm.languageCode = ''
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.title.trim() || !createForm.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  creating.value = true
  try {
    await postApi.create(createForm)
    ElMessage.success('发布成功！')
    createVisible.value = false
    activeLang.value = ''
    page.value = 1
    loadPosts()
  } finally {
    creating.value = false
  }
}

onMounted(async () => {
  languages.value = await languageApi.list()
  await loadPosts()
})
</script>

<style scoped>
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.post-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(120, 150, 255, 0.14);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 14px;
  cursor: pointer;
}

.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.post-avatar {
  font-size: 28px;
}

.post-author {
  font-weight: 600;
  font-size: 14px;
}

.post-lang {
  margin-left: auto;
}

.post-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
}

.post-content-preview {
  color: var(--ll-text-muted);
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
}

.post-foot {
  display: flex;
  gap: 18px;
  color: var(--ll-text-muted);
  font-size: 13px;
}

.foot-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
<template>
  <div class="back-row">
    <button class="back-btn" @click="goBack">
      <span class="back-arrow">←</span>
      <span>{{ label }}</span>
    </button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  // 按钮文案，默认"返回上一级"
  label: { type: String, default: '返回上一级' },
  // 固定跳转目标（如 '/'），不传则回退到上一页
  to: { type: String, default: '' }
})

const router = useRouter()

function goBack() {
  if (props.to) {
    router.push(props.to)
    return
  }
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.back-row {
  margin-bottom: 16px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 1px solid rgba(120, 150, 255, 0.22);
  color: var(--ll-text-muted);
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.back-btn:hover {
  color: var(--ll-cyan);
  border-color: rgba(34, 211, 238, 0.55);
  box-shadow: 0 0 14px rgba(34, 211, 238, 0.18);
}

.back-arrow {
  font-size: 14px;
}
</style>
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      { path: '', name: 'dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'word-quiz', name: 'wordQuiz', component: () => import('../views/WordQuizView.vue') },
      { path: 'courses', name: 'courses', component: () => import('../views/Courses.vue') },
      { path: 'courses/:id', name: 'courseDetail', component: () => import('../views/CourseDetail.vue') },
      { path: 'learn/:id', name: 'learn', component: () => import('../views/Learn.vue') },
      { path: 'progress', name: 'progress', component: () => import('../views/Progress.vue') },
      { path: 'recommend', name: 'recommend', component: () => import('../views/Recommend.vue') },
      { path: 'community', name: 'community', component: () => import('../views/Community.vue') },
      { path: 'community/:id', name: 'postDetail', component: () => import('../views/PostDetail.vue') },
      { path: 'achievements', name: 'achievements', component: () => import('../views/Achievements.vue') },
      { path: 'translate', name: 'translate', component: () => import('../views/Translate.vue') },
      { path: 'vip', name: 'vip', component: () => import('../views/Vip.vue') },
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue') },
      { path: 'download', name: 'download', component: () => import('../views/AppDownload.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：游客可自由浏览主页/课程/翻译/社区等页面，
// 进入需要身份的页面（学习、统计、推荐、成就、VIP、个人中心）时弹出登录弹窗，
// 登录成功后自动跳回目标页
const PROTECTED_ROUTES = ['learn', 'progress', 'recommend', 'achievements', 'vip', 'profile']

router.beforeEach((to) => {
  const store = useUserStore()
  if (PROTECTED_ROUTES.includes(to.name) && !store.isLoggedIn) {
    // 重定向回主页并弹出登录弹窗（弹窗挂在 MainLayout 内，
    // 直接取消首次导航会导致空白页）；登录成功后自动跳回目标页
    store.requireLogin(to.fullPath)
    return { name: 'dashboard' }
  }
  if ((to.name === 'login' || to.name === 'register') && store.isLoggedIn) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
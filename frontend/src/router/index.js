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
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫
router.beforeEach((to) => {
  const store = useUserStore()
  if (to.name !== 'login' && to.name !== 'register' && !store.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'login' || to.name === 'register') && store.isLoggedIn) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
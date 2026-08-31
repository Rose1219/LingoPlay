import http from './http'

// 认证
export const authApi = {
  register: (data) => http.post('/auth/register', data),
  login: (data) => http.post('/auth/login', data)
}

// 用户
export const userApi = {
  me: () => http.get('/users/me'),
  updateMe: (data) => http.put('/users/me', data)
}

// 语种
export const languageApi = {
  list: () => http.get('/languages')
}

// 课程与课时
export const courseApi = {
  list: (language) => http.get('/courses', { params: language ? { language } : {} }),
  detail: (id) => http.get(`/courses/${id}`)
}

export const lessonApi = {
  detail: (id) => http.get(`/lessons/${id}`),
  start: (id) => http.post(`/lessons/${id}/start`),
  submit: (id, data) => http.post(`/lessons/${id}/submit`, data)
}

// 学习统计
export const studyApi = {
  stats: () => http.get('/study/stats')
}

// 个性化推荐
export const recommendApi = {
  get: () => http.get('/recommend')
}

// 社区
export const postApi = {
  list: (params) => http.get('/posts', { params }),
  create: (data) => http.post('/posts', data),
  detail: (id) => http.get(`/posts/${id}`),
  comment: (id, content) => http.post(`/posts/${id}/comments`, { content }),
  like: (id) => http.post(`/posts/${id}/like`)
}

// 成就
export const achievementApi = {
  list: () => http.get('/achievements')
}

// 游戏化接口
export const gameApi = {
  dailyWord: (params) => http.get('/game/daily-word', { params }),
  quizWords: (lang) => http.get('/game/word-quiz', { params: lang ? { lang } : {} }),
  submitQuiz: (data) => http.post('/game/word-quiz/submit', data)
}
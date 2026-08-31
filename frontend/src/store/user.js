import { defineStore } from 'pinia'
import { authApi, userApi } from '../api'

const TOKEN_KEY = 'lingolearn_token'
const USER_KEY = 'lingolearn_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => (state.user ? state.user.nickname || state.user.username : '')
  },
  actions: {
    _persist(user) {
      if (user) {
        this.user = user
        localStorage.setItem(USER_KEY, JSON.stringify(user))
      }
    },
    async login(account, password) {
      const data = await authApi.login({ account, password })
      this.token = data.token
      localStorage.setItem(TOKEN_KEY, data.token)
      this._persist(data.user)
      return data.user
    },
    async register(payload) {
      const data = await authApi.register(payload)
      this.token = data.token
      localStorage.setItem(TOKEN_KEY, data.token)
      this._persist(data.user)
      return data.user
    },
    async fetchMe() {
      const user = await userApi.me()
      this._persist(user)
      return user
    },
    async updateProfile(payload) {
      const user = await userApi.updateMe(payload)
      this._persist(user)
      return user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
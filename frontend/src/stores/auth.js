import { defineStore } from 'pinia'
import { authApi } from '../api'
import { TOKEN_KEY, USER_KEY, tokenStore } from '../api/request'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: tokenStore.get(),
    user: readUser()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token && !!state.user,
    isAdmin: (state) => state.user?.role === 'ADMIN',
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
    roleText: (state) => (state.user?.role === 'ADMIN' ? '管理者' : '客户经理')
  },
  actions: {
    persist() {
      tokenStore.set(this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    async login(payload) {
      const result = await authApi.login(payload)
      this.token = result.token
      this.user = result.user
      this.persist()
      return result
    },
    async refresh() {
      const user = await authApi.me()
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
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

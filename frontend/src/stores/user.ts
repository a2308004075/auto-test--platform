import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshTokenValue = ref<string>(localStorage.getItem('refreshToken') || '')
  const username = ref<string>('')
  const role = ref<string>('')
  const userId = ref<string>('')
  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setRefreshToken(rt: string) {
    refreshTokenValue.value = rt
    localStorage.setItem('refreshToken', rt)
  }

  function setUserInfo(info: { id: string; username: string; role?: string }) {
    userId.value = info.id
    username.value = info.username
    role.value = info.role || 'USER'
  }

  function logout() {
    token.value = ''
    refreshTokenValue.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  return {
    token, refreshTokenValue, username, role, userId, isLoggedIn,
    setToken, setRefreshToken, setUserInfo, logout,
  }
})

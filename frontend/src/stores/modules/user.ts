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
  const userId = ref<number>(0)
  const isLoggedIn = computed(() => !!token.value)

  // ===== 记住密码 =====
  const REMEMBER_KEY = 'rememberedCredentials'

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setRefreshToken(rt: string) {
    refreshTokenValue.value = rt
    localStorage.setItem('refreshToken', rt)
  }

  function setUserInfo(info: { id: number; username: string; role?: string }) {
    userId.value = info.id
    username.value = info.username
    role.value = info.role || 'USER'
  }

  function logout() {
    token.value = ''
    refreshTokenValue.value = ''
    username.value = ''
    role.value = ''
    userId.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  /**
   * 加载已记住的登录凭据
   */
  function loadRememberedCredentials(): { username: string; password: string } | null {
    const raw = localStorage.getItem(REMEMBER_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  /**
   * 保存登录凭据（记住密码）
   */
  function saveRememberedCredentials(username: string, password: string) {
    localStorage.setItem(REMEMBER_KEY, JSON.stringify({ username, password }))
  }

  /**
   * 清除已记住的登录凭据
   */
  function clearRememberedCredentials() {
    localStorage.removeItem(REMEMBER_KEY)
  }

  return {
    token, refreshTokenValue, username, role, userId, isLoggedIn,
    setToken, setRefreshToken, setUserInfo, logout,
    loadRememberedCredentials, saveRememberedCredentials, clearRememberedCredentials,
  }
})

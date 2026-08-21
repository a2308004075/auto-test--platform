import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentUser } from '@/api/auth'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshTokenValue = ref<string>(localStorage.getItem('refreshToken') || '')
  const username = ref<string>(localStorage.getItem('username') || '')
  const displayName = ref<string>(localStorage.getItem('displayName') || '')
  const role = ref<string>(localStorage.getItem('role') || '')
  const userId = ref<number>(Number(localStorage.getItem('userId')) || 0)
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

  function setUserInfo(info: { id: number; username: string; displayName?: string; role?: string }) {
    userId.value = info.id
    username.value = info.username
    displayName.value = info.displayName || info.username
    role.value = info.role || 'USER'
    localStorage.setItem('username', info.username)
    localStorage.setItem('displayName', info.displayName || info.username)
    localStorage.setItem('role', role.value)
    localStorage.setItem('userId', String(info.id))
  }

  /**
   * 从后端获取当前用户信息并更新 store
   * 页面刷新后恢复用户信息时调用
   */
  async function fetchCurrentUser() {
    if (!token.value) return
    try {
      const res: any = await getCurrentUser()
      if (res.data) {
        setUserInfo({
          id: res.data.id,
          username: res.data.username,
          displayName: res.data.displayName,
          role: res.data.role,
        })
      }
    } catch {
      // Token 无效时清除
      logout()
    }
  }

  function logout() {
    token.value = ''
    refreshTokenValue.value = ''
    username.value = ''
    displayName.value = ''
    role.value = ''
    userId.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
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
    token, refreshTokenValue, username, displayName, role, userId, isLoggedIn,
    setToken, setRefreshToken, setUserInfo, fetchCurrentUser, logout,
    loadRememberedCredentials, saveRememberedCredentials, clearRememberedCredentials,
  }
})

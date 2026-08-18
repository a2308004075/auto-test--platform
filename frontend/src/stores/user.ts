import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const username = ref<string>('')
  const isLoggedIn = ref<boolean>(false)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUsername(name: string) {
    username.value = name
  }

  function setLoggedIn(status: boolean) {
    isLoggedIn.value = status
  }

  function logout() {
    token.value = ''
    username.value = ''
    isLoggedIn.value = false
    localStorage.removeItem('token')
  }

  return {
    token,
    username,
    isLoggedIn,
    setToken,
    setUsername,
    setLoggedIn,
    logout,
  }
})

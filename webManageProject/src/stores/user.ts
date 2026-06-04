import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<Record<string, any>>({})

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function clearToken() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
  }

  // 初始化时从 localStorage 恢复 token
  const saved = localStorage.getItem('token')
  if (saved) token.value = saved

  return { token, userInfo, setToken, clearToken }
})

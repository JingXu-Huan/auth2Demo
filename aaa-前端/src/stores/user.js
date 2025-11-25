import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref('')

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => user.value?.id)
  const userName = computed(() => user.value?.nickname || user.value?.username || '用户')
  const userAvatar = computed(() => user.value?.avatar)

  // 初始化 - 从localStorage恢复
  const init = () => {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')
    
    if (savedToken) {
      token.value = savedToken
    }
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }
  }

  // 设置用户信息
  const setUser = (userData) => {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
    if (userData?.id) {
      localStorage.setItem('userId', userData.id)
    }
  }

  // 设置Token
  const setToken = (tokenValue) => {
    token.value = tokenValue
    localStorage.setItem('token', tokenValue)
  }

  // 登录
  const login = (userData, tokenValue) => {
    setUser(userData)
    setToken(tokenValue)
  }

  // 登出
  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('userId')
  }

  // 更新用户信息
  const updateUser = (data) => {
    if (user.value) {
      user.value = { ...user.value, ...data }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  // 立即初始化
  init()

  return {
    user,
    token,
    isLoggedIn,
    userId,
    userName,
    userAvatar,
    setUser,
    setToken,
    login,
    logout,
    updateUser
  }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref(null)
  const token = ref('')

  // 从本地存储恢复
  const restoreState = () => {
    const savedToken = uni.getStorageSync('token')
    const savedUser = uni.getStorageSync('userInfo')
    if (savedToken) {
      token.value = savedToken
    }
    if (savedUser) {
      userInfo.value = savedUser
    }
  }

  // 保存到本地存储
  const saveState = () => {
    if (token.value) {
      uni.setStorageSync('token', token.value)
    }
    if (userInfo.value) {
      uni.setStorageSync('userInfo', userInfo.value)
    }
  }

  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info
    saveState()
  }

  // 设置token
  const setToken = (newToken) => {
    token.value = newToken
    saveState()
  }

  // 登录
  const login = (loginData) => {
    const { token: newToken, user } = loginData
    setToken(newToken)
    setUserInfo(user)
  }

  // 登出
  const logout = () => {
    userInfo.value = null
    token.value = ''
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  // 更新阅读进度
  const updateProgress = (chapterId) => {
    if (userInfo.value) {
      userInfo.value.lastChapterId = chapterId
      saveState()
    }
  }

  // 是否已登录
  const isLoggedIn = () => {
    return !!token.value && !!userInfo.value
  }

  return {
    userInfo,
    token,
    restoreState,
    setUserInfo,
    setToken,
    login,
    logout,
    updateProgress,
    isLoggedIn
  }
})

<template>
  <view class="profile-container">
    <!-- 背景光晕 -->
    <view class="ambient-glow"></view>

    <!-- 主要内容区域 -->
    <view class="profile-content-wrapper">
      <!-- 个人资料部分 -->
      <section class="profile-section">
        <view class="profile-avatar-wrapper">
          <view class="profile-avatar-new">
            <view class="profile-avatar-inner">
              <text class="avatar-char">{{ userInfo?.avatarChar || '道' }}</text>
            </view>
          </view>
        </view>
        <view class="profile-info-new">
          <text class="profile-name-new">{{ userInfo?.nickname || '修道者' }}</text>
          <view class="profile-status-badge">
            <text class="material-symbols-outlined">spa</text>
            <text class="profile-status-text">{{ profileStatusText }}</text>
          </view>
        </view>
      </section>

      <!-- 横向菜单 -->
      <section class="horizontal-menu-section">
        <view class="horizontal-menu-row">
          <view class="menu-card" @click="navigateToComments">
            <text class="material-symbols-outlined">chat_bubble</text>
            <text class="menu-card-text">感悟</text>
          </view>
          <view class="menu-card" @click="navigateToLikes">
            <text class="material-symbols-outlined">thumb_up</text>
            <text class="menu-card-text">点赞</text>
          </view>
          <view class="menu-card" @click="navigateToReplies">
            <text class="material-symbols-outlined">history</text>
            <text class="menu-card-text">互动</text>
          </view>
        </view>

        <!-- 列表菜单 -->
        <view class="list-menu-item" @click="navigateToFavorites">
          <view class="list-menu-item-left">
            <text class="material-symbols-outlined">auto_stories</text>
            <text class="list-menu-text">修道藏经</text>
          </view>
          <text class="material-symbols-outlined list-menu-arrow">arrow_forward_ios</text>
        </view>

        <view class="list-menu-item disabled">
          <view class="list-menu-item-left">
            <text class="material-symbols-outlined">group</text>
            <text class="list-menu-text">道友圈</text>
          </view>
          <text class="list-menu-arrow">即将上线</text>
        </view>
      </section>

      <!-- 底部工具 -->
      <section class="footer-utilities">
        <view class="footer-link" @click="navigateToSettings">
          <view class="footer-link-left">
            <text class="material-symbols-outlined">settings</text>
            <text class="footer-link-text">修行设置与隐私</text>
          </view>
          <text class="material-symbols-outlined footer-link-arrow">chevron_right</text>
        </view>
        <view class="footer-link">
          <view class="footer-link-left">
            <text class="material-symbols-outlined">help</text>
            <text class="footer-link-text">道法指引</text>
          </view>
          <text class="material-symbols-outlined footer-link-arrow">chevron_right</text>
        </view>

        <!-- 退出登录 -->
        <view v-if="isLoggedIn" class="footer-link logout-link" @click="handleLogout">
          <view class="footer-link-left">
            <text class="material-symbols-outlined">logout</text>
            <text class="footer-link-text">退出登录</text>
          </view>
          <text class="material-symbols-outlined footer-link-arrow">chevron_right</text>
        </view>
      </section>

      <!-- 登录提示 -->
      <view v-if="!isLoggedIn" class="login-section">
        <button class="login-btn" @click="navigateToLogin">立即登录</button>
      </view>
    </view>

    <!-- 底部tabBar占位空间 -->
    <view class="tabbar-placeholder"></view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// 初始化用户状态
onMounted(() => {
  userStore.restoreState()

  // 检查登录状态
  if (!userStore.isLoggedIn()) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    uni.reLaunch({
      url: '/pages/login/index'
    })
  }
})

const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn())
const currentChapterId = computed(() => userStore.userInfo?.lastChapterId || 1)

// 计算修行进度百分比
const progressPercent = computed(() => {
  return Math.round((currentChapterId.value / 81) * 100)
})

// 根据进度计算修行称号和状态文本
const userTitle = computed(() => {
  const progress = currentChapterId.value
  if (progress >= 81) return '悟道真人'
  if (progress >= 60) return '得道高人'
  if (progress >= 40) return '修行有成'
  if (progress >= 20) return '初窥门径'
  if (progress >= 10) return '初入道门'
  return '道缘初结'
})

// 个人中心状态文本
const profileStatusText = computed(() => {
  const progress = currentChapterId.value
  const percentage = Math.round((progress / 81) * 100)
  let status = '初悟'
  if (percentage >= 90) status = '悟道'
  else if (percentage >= 60) status = '近道'
  else if (percentage >= 30) status = '修行中'
  return `修行进度：第${progress}章 · ${status}中`
})

// 导航到我的留言
const navigateToComments = () => {
  if (!checkLogin()) return
  uni.navigateTo({
    url: '/pages/profile/comments'
  })
}

// 导航到我的点赞
const navigateToLikes = () => {
  if (!checkLogin()) return
  uni.navigateTo({
    url: '/pages/profile/likes'
  })
}

// 导航到历史回复
const navigateToReplies = () => {
  if (!checkLogin()) return
  uni.navigateTo({
    url: '/pages/profile/replies'
  })
}

// 导航到修道藏经
const navigateToFavorites = () => {
  if (!checkLogin()) return
  uni.navigateTo({
    url: '/pages/profile/favorites'
  })
}

// 导航到登录页
const navigateToLogin = () => {
  uni.navigateTo({
    url: '/pages/login/index'
  })
}

// 导航到设置页
const navigateToSettings = () => {
  uni.showToast({
    title: '设置页面开发中',
    icon: 'none'
  })
}

// 退出登录
const handleLogout = () => {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        // 清除用户状态
        userStore.logout()

        uni.showToast({
          title: '已退出登录',
          icon: 'success',
          duration: 1500
        })

        setTimeout(() => {
          uni.reLaunch({
            url: '/pages/login/index'
          })
        }, 1500)
      }
    }
  })
}

// 检查登录状态
const checkLogin = () => {
  if (!isLoggedIn.value) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    return false
  }
  return true
}
</script>

<style lang="scss" scoped>
.profile-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #0a0a0a 0%, #1a1a1a 100%);
  color: #e2e2e2;
  font-family: "KaiTi", "STKaiti", "KaiTi_GB2312", "Songti SC", serif;
  display: flex;
  flex-direction: column;
  position: relative;
  padding-bottom: calc(50px + env(safe-area-inset-bottom));
}

.tabbar-placeholder {
  height: 50px;
  padding-bottom: env(safe-area-inset-bottom);
}

.profile-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 40px 24px 32px;
  position: relative;
  z-index: 1;
}

// 背景光晕
.ambient-glow {
  position: absolute;
  top: 0;
  right: 0;
  width: 256px;
  height: 256px;
  background: rgba(212, 175, 55, 0.05);
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  z-index: 0;
}

// 个人资料部分
.profile-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  position: relative;
  z-index: 10;
}

.profile-avatar-wrapper {
  position: relative;
}

.profile-avatar-new {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 0.5px solid rgba(212, 175, 55, 0.5);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  background: rgba(42, 42, 42, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transition: transform 0.3s ease;
}

.profile-avatar-new:active {
  transform: scale(0.95);
}

.profile-avatar-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(26, 26, 26, 1);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-char {
  font-size: 36px;
  color: #d4af37;
  font-weight: bold;
}

.profile-info-new {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.profile-name-new {
  font-size: 22px;
  color: rgba(229, 226, 225, 1);
  letter-spacing: 0.1em;
  margin-top: 8px;
}

.profile-status-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 999px;
  padding: 6px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.profile-status-text {
  font-size: 12px;
  letter-spacing: 0.02em;
  color: rgba(226, 226, 226, 1);
  font-weight: 500;
}

// 横向菜单
.horizontal-menu-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  position: relative;
  z-index: 10;
}

.horizontal-menu-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  width: 100%;
  height: 72px;
}

.menu-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px;
  background: rgba(26, 26, 26, 0.6);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.menu-card:active {
  background: rgba(255, 255, 255, 0.05);
  transform: scale(0.95);
}

.icon {
  font-size: 18px;
  color: rgba(226, 226, 226, 1);
  transition: color 0.3s ease;
}

.menu-card:active .icon {
  color: #d4af37;
}

.menu-card-text {
  font-size: 12px;
  letter-spacing: 0.02em;
  color: rgba(226, 226, 226, 1);
  font-weight: 500;
  transition: color 0.3s ease;
}

.menu-card:active .menu-card-text {
  color: rgba(229, 226, 225, 1);
}

// 列表菜单项
.list-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 72px;
  background: rgba(26, 26, 26, 0.6);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.list-menu-item:active {
  background: rgba(255, 255, 255, 0.05);
}

.list-menu-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.list-menu-item-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.list-menu-item .icon {
  font-size: 24px;
}

.list-menu-text {
  font-size: 15px;
  color: rgba(226, 226, 226, 1);
  letter-spacing: 0.1em;
}

.list-menu-arrow {
  color: rgba(226, 226, 226, 0.5);
  font-size: 18px;
  transition: color 0.3s ease;
}

.list-menu-item:active .list-menu-arrow {
  color: rgba(226, 226, 226, 1);
}

// 底部工具
.footer-utilities {
  margin-top: 48px;
  padding-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
  z-index: 10;
  width: 100%;
  padding-left: 8px;
  padding-right: 8px;
}

.footer-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(226, 226, 226, 0.7);
  transition: color 0.3s ease;
  padding: 12px;
  border-radius: 12px;
}

.footer-link:active {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(229, 226, 225, 1);
}

.footer-link-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-link .icon {
  font-size: 20px;
}

.footer-link-text {
  font-size: 14px;
  letter-spacing: 0.05em;
}

.footer-link-arrow {
  font-size: 16px;
  opacity: 0.5;
  transition: opacity 0.3s ease;
}

.footer-link:active .footer-link-arrow {
  opacity: 1;
}

// 登录部分
.login-section {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  padding: 0 8px;
}

.login-btn {
  width: 100%;
  padding: 16px;
  background: rgba(212, 175, 55, 0.1);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 12px;
  color: #d4af37;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 2px;
  text-align: center;
}

.login-btn:active {
  background: rgba(212, 175, 55, 0.2);
}
</style>

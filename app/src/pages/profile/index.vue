<template>
  <view class="profile-container">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="avatar-section">
        <view class="avatar">
          <text class="avatar-char">{{ userInfo?.avatarChar || '道' }}</text>
        </view>
        <view class="user-info">
          <text class="username">{{ userInfo?.nickname || '道友' }}</text>
          <text class="user-title">{{ userTitle }}</text>
        </view>
      </view>

      <!-- 修行进度 -->
      <view class="progress-section">
        <view class="progress-item">
          <text class="progress-label">修行进度</text>
          <text class="progress-value">第{{ currentChapterId }}章/81章</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
        </view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="function-grid">
      <view class="function-item" @click="navigateToComments">
        <view class="function-icon">💬</view>
        <text class="function-name">我的留言</text>
        <text class="function-arrow">›</text>
      </view>

      <view class="function-item" @click="navigateToLikes">
        <view class="function-icon">❤️</view>
        <text class="function-name">我的点赞</text>
        <text class="function-arrow">›</text>
      </view>

      <view class="function-item" @click="navigateToReplies">
        <view class="function-icon">↩️</view>
        <text class="function-name">历史回复</text>
        <text class="function-arrow">›</text>
      </view>

      <view class="function-item" @click="navigateToFavorites">
        <view class="function-icon">📚</view>
        <text class="function-name">修道藏经</text>
        <text class="function-arrow">›</text>
      </view>

      <view class="function-item disabled">
        <view class="function-icon">👥</view>
        <text class="function-name">道友圈</text>
        <text class="function-tip">即将上线</text>
      </view>
    </view>

    <!-- 登录状态检查 -->
    <view v-if="!isLoggedIn" class="login-tip">
      <text class="tip-text">登录后可查看更多功能</text>
      <button class="login-btn" @click="navigateToLogin">立即登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn())
const currentChapterId = computed(() => userStore.userInfo?.lastChapterId || 1)

// 计算修行进度百分比
const progressPercent = computed(() => {
  return Math.round((currentChapterId.value / 81) * 100)
})

// 根据进度计算修行称号
const userTitle = computed(() => {
  const progress = currentChapterId.value
  if (progress >= 81) return '悟道真人'
  if (progress >= 60) return '得道高人'
  if (progress >= 40) return '修行有成'
  if (progress >= 20) return '初窥门径'
  if (progress >= 10) return '初入道门'
  return '道缘初结'
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
  padding: 40rpx;
  box-sizing: border-box;
}

.user-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 16rpx;
  padding: 40rpx;
  margin-bottom: 40rpx;

  .avatar-section {
    display: flex;
    align-items: center;
    margin-bottom: 40rpx;

    .avatar {
      width: 120rpx;
      height: 120rpx;
      background: linear-gradient(135deg, #d4af37 0%, #aa8a2c 100%);
      border-radius: 60rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 30rpx;

      .avatar-char {
        font-size: 48rpx;
        color: #000;
        font-weight: bold;
      }
    }

    .user-info {
      flex: 1;

      .username {
        display: block;
        font-size: 36rpx;
        font-weight: bold;
        color: #fff;
        margin-bottom: 10rpx;
      }

      .user-title {
        display: block;
        font-size: 28rpx;
        color: #d4af37;
      }
    }
  }

  .progress-section {
    .progress-item {
      display: flex;
      justify-content: space-between;
      margin-bottom: 20rpx;

      .progress-label {
        font-size: 28rpx;
        color: #888;
      }

      .progress-value {
        font-size: 28rpx;
        color: #d4af37;
      }
    }

    .progress-bar {
      width: 100%;
      height: 12rpx;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 6rpx;
      overflow: hidden;

      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #d4af37 0%, #aa8a2c 100%);
        border-radius: 6rpx;
        transition: width 0.3s;
      }
    }
  }
}

.function-grid {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 16rpx;
  overflow: hidden;

  .function-item {
    display: flex;
    align-items: center;
    padding: 40rpx;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);

    &:last-child {
      border-bottom: none;
    }

    &.disabled {
      opacity: 0.5;
    }

    .function-icon {
      width: 80rpx;
      height: 80rpx;
      background: rgba(212, 175, 55, 0.1);
      border-radius: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      margin-right: 30rpx;
    }

    .function-name {
      flex: 1;
      font-size: 32rpx;
      color: #e0e0e0;
    }

    .function-arrow {
      font-size: 40rpx;
      color: #666;
    }

    .function-tip {
      font-size: 24rpx;
      color: #666;
    }
  }
}

.login-tip {
  margin-top: 60rpx;
  text-align: center;

  .tip-text {
    display: block;
    font-size: 28rpx;
    color: #888;
    margin-bottom: 30rpx;
  }

  .login-btn {
    width: 300rpx;
    height: 80rpx;
    background: linear-gradient(135deg, #d4af37 0%, #aa8a2c 100%);
    border: none;
    border-radius: 40rpx;
    color: #000;
    font-size: 32rpx;
    font-weight: bold;
  }
}
</style>

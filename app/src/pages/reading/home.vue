<template>
  <view class="reading-home">
    <!-- 顶部标题 -->
    <view class="home-header">
      <text class="home-title">道德经</text>
      <text class="home-subtitle">道可道，非常道</text>
    </view>

    <!-- 加载状态 -->
    <view v-if="isLoading" class="loading-container">
      <view class="loading-spinner"></view>
      <text class="loading-text">正在加载...</text>
    </view>

    <!-- 错误状态 -->
    <view v-else-if="loadError" class="error-container" @click="retryLoad">
      <view class="error-icon">
        <text class="material-symbols-outlined">error_outline</text>
      </view>
      <text class="error-text">{{ loadError }}</text>
      <text class="retry-text">点击屏幕重试</text>
    </view>

    <!-- 主要内容区 -->
    <view v-else class="home-content">
      <!-- 开始阅读卡片 -->
      <view class="reading-card" @click="startReading">
        <view class="card-icon">
          <text class="material-symbols-outlined">menu_book</text>
        </view>
        <view class="card-content">
          <text class="card-title">开始阅读</text>
          <text class="card-desc">继续您的修行之旅</text>
        </view>
        <view class="card-arrow">
          <text class="material-symbols-outlined">arrow_forward</text>
        </view>
      </view>

      <!-- 阅读进度 -->
      <view class="progress-section">
        <view class="progress-title">修行进度</view>
        <view class="progress-card">
          <view class="progress-info">
            <text class="progress-num">{{ currentChapter }}/81</text>
            <text class="progress-text">已完成</text>
          </view>
          <view class="progress-bar">
            <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
          </view>
        </view>
      </view>

      <!-- 章节速览 -->
      <view class="chapters-section">
        <view class="section-header">
          <text class="section-title">章节速览</text>
          <text class="section-more" @click="showAllChapters">全部</text>
        </view>
        <scroll-view class="chapters-list" scroll-x>
          <view
            v-for="chapter in previewChapters"
            :key="chapter.id"
            class="chapter-preview-item"
            @click="jumpToChapter(chapter.id)"
          >
            <view class="chapter-num">{{ chapter.id }}</view>
            <view class="chapter-name">{{ chapter.title }}</view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 底部装饰 -->
    <view class="home-footer">
      <text class="footer-text">道法自然</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getChapters } from '@/api'

const userStore = useUserStore()
const allChapters = ref([])
const currentChapter = ref(1)
const isLoading = ref(true)
const loadError = ref('')

// 预览章节（前10章）
const previewChapters = computed(() => allChapters.value.slice(0, 10))

// 进度百分比
const progressPercent = computed(() => {
  return Math.round((currentChapter.value / 81) * 100)
})

// 加载章节列表
const loadChapters = async () => {
  try {
    isLoading.value = true
    loadError.value = ''
    console.log('开始加载章节列表...')
    const result = await getChapters()
    console.log('章节列表加载成功:', result.length, '章')
    allChapters.value = result
  } catch (error) {
    console.error('加载章节失败:', error)
    loadError.value = '加载失败，请检查网络连接'
    // 如果是网络错误，提供重试提示
    if (error.message && error.message.includes('timeout')) {
      loadError.value = '连接服务器超时，请点击屏幕重试'
    }
  } finally {
    isLoading.value = false
  }
}

// 开始阅读
const startReading = () => {
  uni.navigateTo({
    url: '/pages/reading/index'
  })
}

// 跳转到指定章节
const jumpToChapter = (chapterId) => {
  uni.navigateTo({
    url: `/pages/reading/index?chapterId=${chapterId}`
  })
}

// 显示全部章节
const showAllChapters = () => {
  // 可以跳转到章节列表页面
  uni.showToast({
    title: '开始阅读后可查看全部章节',
    icon: 'none'
  })
}

onMounted(async () => {
  console.log('阅读首页挂载，开始初始化...')

  // 检查登录状态
  if (!userStore.isLoggedIn()) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    uni.reLaunch({
      url: '/pages/login/index'
    })
    return
  }

  await loadChapters()
  // 获取用户进度
  const lastChapterId = userStore.userInfo?.lastChapterId || 1
  currentChapter.value = lastChapterId
  console.log('阅读首页初始化完成，当前章节:', currentChapter.value)
})

// 重试加载
const retryLoad = () => {
  loadChapters()
}
</script>

<style lang="scss" scoped>
.reading-home {
  min-height: 100vh;
  background: linear-gradient(180deg, #0a0a0a 0%, #1a1a1a 100%);
  color: #e2e2e2;
  font-family: "KaiTi", "STKaiti", "KaiTi_GB2312", "Songti SC", serif;
  padding: 40px 20px 20px;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(212, 175, 55, 0.2);
  border-top-color: #d4af37;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 2px;
}

.error-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 107, 107, 0.1);
  border: 1px solid rgba(255, 107, 107, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.error-icon .material-symbols-outlined {
  font-size: 28px;
  color: #ff6b6b;
}

.error-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 10px;
  letter-spacing: 1px;
}

.retry-text {
  font-size: 14px;
  color: rgba(212, 175, 55, 0.7);
  letter-spacing: 1px;
}

.home-header {
  text-align: center;
  margin-bottom: 50px;
  animation: fade-in 1s ease-out;
}

.home-title {
  display: block;
  font-size: 48px;
  color: #d4af37;
  letter-spacing: 12px;
  margin-bottom: 10px;
  font-weight: 300;
}

.home-subtitle {
  display: block;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 4px;
}

.home-content {
  max-width: 600px;
  margin: 0 auto;
}

.reading-card {
  display: flex;
  align-items: center;
  padding: 30px 25px;
  background: rgba(212, 175, 55, 0.08);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 20px;
  margin-bottom: 40px;
  cursor: pointer;
  transition: all 0.3s ease;
  animation: slide-up 0.6s ease-out 0.2s backwards;
}

.reading-card:active {
  transform: scale(0.98);
  background: rgba(212, 175, 55, 0.12);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(212, 175, 55, 0.15);
  border: 1px solid rgba(212, 175, 55, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  flex-shrink: 0;
}

.card-icon .material-symbols-outlined {
  font-size: 28px;
  color: #d4af37;
}

.card-content {
  flex: 1;
}

.card-title {
  display: block;
  font-size: 20px;
  color: #f8f6f2;
  margin-bottom: 6px;
  letter-spacing: 2px;
}

.card-desc {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
}

.card-arrow {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-arrow .material-symbols-outlined {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.6);
}

.progress-section {
  margin-bottom: 40px;
  animation: slide-up 0.6s ease-out 0.4s backwards;
}

.progress-title {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 2px;
  margin-bottom: 15px;
}

.progress-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 20px;
}

.progress-info {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 15px;
}

.progress-num {
  font-size: 32px;
  color: #d4af37;
  font-weight: 300;
}

.progress-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #d4af37 0%, #f4cf67 100%);
  border-radius: 3px;
  transition: width 0.6s ease;
}

.chapters-section {
  animation: slide-up 0.6s ease-out 0.6s backwards;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-title {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 2px;
}

.section-more {
  font-size: 14px;
  color: #d4af37;
  cursor: pointer;
}

.chapters-list {
  white-space: nowrap;
  padding: 5px 0;
}

.chapter-preview-item {
  display: inline-block;
  width: 80px;
  text-align: center;
  padding: 15px 10px;
  margin-right: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chapter-preview-item:active {
  background: rgba(212, 175, 55, 0.1);
  border-color: rgba(212, 175, 55, 0.3);
}

.chapter-num {
  font-size: 18px;
  color: #d4af37;
  margin-bottom: 8px;
}

.chapter-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  white-space: normal;
  letter-spacing: 1px;
}

.home-footer {
  text-align: center;
  margin-top: 60px;
  padding-bottom: 40px;
  opacity: 0.4;
}

.footer-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 6px;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slide-up {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

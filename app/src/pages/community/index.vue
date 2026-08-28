<template>
  <view class="community-container" :class="theme.themeClass">
    <!-- 自定义背景层 -->
    <CustomBackground />

    <!-- 顶部标题 -->
    <view class="header">
      <view class="header-title">
        <text class="title">道友</text>
        <text class="subtitle">同修智慧，共悟大道</text>
      </view>
    </view>

    <!-- 主要内容 -->
    <view class="main-content">
      <!-- 空状态 -->
      <view v-if="!userStore.isLoggedIn()" class="empty-state">
        <view class="empty-icon">
          <text class="material-symbols-outlined">group</text>
        </view>
        <text class="empty-title">暂未登录</text>
        <text class="empty-desc">登录后可与道友交流</text>
        <view class="login-btn" @click="goToLogin">
          <text>前往登录</text>
        </view>
      </view>

      <!-- 道友感悟 -->
      <view v-else class="insights-section">

        <view v-if="insights.length === 0" class="empty-state">
          <view class="empty-icon">
            <text class="material-symbols-outlined">hourglass_empty</text>
          </view>
          <text class="empty-title">暂无感悟</text>
          <text class="empty-desc">期待各位道友分享修行心得...</text>
        </view>

        <view v-else class="insight-list">
          <view
            v-for="insight in insights"
            :key="insight.id"
            class="insight-item"
          >
            <view class="insight-header">
              <view class="insight-avatar">{{ getAvatar(insight.nickname) }}</view>
              <view class="insight-user">
                <text class="insight-name">{{ insight.nickname }}</text>
                <text class="insight-chapter">第{{ insight.chapterId }}章</text>
              </view>
              <text class="insight-time">{{ formatTime(insight.createdAt) }}</text>
            </view>
            <view class="insight-content">
              <text class="insight-text">{{ insight.content }}</text>
            </view>
            <view class="insight-actions">
              <view class="insight-action" @click="likeInsight(insight)">
                <text class="material-symbols-outlined">{{ insight.liked ? 'thumb_up' : 'thumb_up_off' }}</text>
                <text>{{ insight.likeCount || 0 }}</text>
              </view>
              <view class="insight-action" @click="replyInsight(insight)">
                <text class="material-symbols-outlined">chat_bubble</text>
                <text>回复</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 社区活动 -->
      <view class="activity-section">
        <view class="section-title">社区动态</view>
        <view class="activity-list">
          <view class="activity-item">
            <view class="activity-icon">
              <text class="material-symbols-outlined">auto_awesome</text>
            </view>
            <view class="activity-content">
              <text class="activity-text">已有 {{ totalReaders }} 位道友开始修行之旅</text>
              <text class="activity-time">持续增长中</text>
            </view>
          </view>
          <view class="activity-item">
            <view class="activity-icon">
              <text class="material-symbols-outlined">chat_bubble</text>
            </view>
            <view class="activity-content">
              <text class="activity-text">累计 {{ totalComments }} 条悟道留言</text>
              <text class="activity-time">来自各位道友的智慧</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部装饰 -->
    <view class="footer">
      <text class="footer-text">同修共进</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import CustomBackground from '@/components/CustomBackground.vue'

const userStore = useUserStore()
const theme = useThemeStore()
const insights = ref([])
const currentChapter = ref(1)
const totalReaders = ref(0)
const totalComments = ref(0)

// 获取头像字符
const getAvatar = (nickname) => {
  if (!nickname) return '道'
  const chars = ['道', '德', '经', '无', '为', '悟', '玄']
  return nickname.charAt(0) || chars[Math.floor(Math.random() * chars.length)]
}

// 前往登录
const goToLogin = () => {
  uni.navigateTo({
    url: '/pages/login/index'
  })
}

// 模拟加载道友感悟数据
const loadInsights = async () => {
  // 这里可以接入实际的 API
  // 目前使用模拟数据
  insights.value = [
    {
      id: 1,
      nickname: '清风',
      chapterId: 15,
      content: '读到此章，深感上善若水之理。水善利万物而不争，处众人之所恶，故几于道。为人处世亦当如此，以柔克刚，以退为进。',
      createdAt: '2026-08-25 10:30',
      likeCount: 23,
      liked: false
    },
    {
      id: 2,
      nickname: '明月',
      chapterId: 8,
      content: '天地不仁，以万物为刍狗。非天地无情，而是天道无私。圣人之心，当如天地般公正，无偏无私，顺应自然。',
      createdAt: '2026-08-25 09:15',
      likeCount: 45,
      liked: true
    },
    {
      id: 3,
      nickname: '无为',
      chapterId: 42,
      content: '道生一，一生二，二生三，三生万物。万物负阴而抱阳，冲气以为和。此乃宇宙生成之理，阴阳调和，生生不息。',
      createdAt: '2026-08-25 08:20',
      likeCount: 67,
      liked: false
    }
  ]
  totalReaders.value = 128
  totalComments.value = 456
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  return '刚刚'
}

// 点赞感悟
const likeInsight = (insight) => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    return
  }

  insight.liked = !insight.liked
  insight.likeCount = insight.liked ? (insight.likeCount || 0) + 1 : Math.max(0, insight.likeCount - 1)

  // 这里可以调用实际的API
  uni.showToast({
    title: insight.liked ? '点赞成功' : '取消点赞',
    icon: 'none',
    duration: 1000
  })
}

// 回复感悟
const replyInsight = (insight) => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    return
  }

  uni.showToast({
    title: '回复功能开发中',
    icon: 'none'
  })
}

onMounted(async () => {
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

  await loadInsights()
  currentChapter.value = userStore.userInfo?.lastChapterId || 1
})
</script>

<style lang="scss" scoped>
.community-container {
  min-height: 100vh;
  background: var(--page-bg-alt);
  color: var(--text-1);
  font-family: "KaiTi", "STKaiti", "KaiTi_GB2312", "Songti SC", serif;
  padding: 40px 20px 20px;
}

/* 内容区块位于自定义背景层之上 */
.header,
.main-content,
.footer {
  position: relative;
  z-index: 1;
}

.header {
  text-align: center;
  margin-bottom: 50px;
  animation: fade-in 1s ease-out;
}

.header-title {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.title {
  font-size: 48px;
  color: var(--accent);
  letter-spacing: 12px;
  margin-bottom: 10px;
  font-weight: 300;
}

.subtitle {
  font-size: 16px;
  color: var(--text-3);
  letter-spacing: 4px;
}

.main-content {
  max-width: 600px;
  margin: 0 auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  animation: fade-in 0.6s ease-out;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--accent-15);
  border: 1px solid var(--accent-15);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-icon .material-symbols-outlined {
  font-size: 36px;
  color: var(--accent);
}

.empty-title {
  font-size: 18px;
  color: var(--text-1);
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-3);
  margin-bottom: 30px;
  letter-spacing: 1px;
}

.login-btn {
  padding: 12px 32px;
  background: var(--accent-15);
  border: 1px solid var(--accent-30);
  border-radius: 24px;
  color: var(--accent);
  font-size: 14px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.login-btn:active {
  background: var(--accent-30);
}

.insights-section {
  animation: slide-up 0.6s ease-out;
}

.insight-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.insight-item {
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  transition: all 0.3s ease;
}

.insight-item:active {
  background: var(--surface-2);
}

.insight-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.insight-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a2a3a, #2a1a2a);
  border: 1px solid var(--border-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  font-size: 14px;
  flex-shrink: 0;
}

.insight-user {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.insight-name {
  font-size: 14px;
  color: var(--text-1);
  font-weight: 500;
}

.insight-chapter {
  font-size: 12px;
  color: var(--accent-strong);
}

.insight-time {
  font-size: 12px;
  color: var(--text-3);
}

.insight-content {
  margin-bottom: 12px;
  padding-left: 52px;
}

.insight-text {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-2);
  letter-spacing: 0.5px;
}

.insight-actions {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-left: 52px;
}

.insight-action {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-3);
  font-size: 13px;
}

.insight-action:active {
  color: var(--accent);
}

.insight-action .material-symbols-outlined {
  font-size: 16px;
}

.activity-section {
  margin-top: 40px;
  animation: slide-up 0.6s ease-out 0.2s backwards;
}

.section-title {
  font-size: 16px;
  color: var(--text-2);
  letter-spacing: 2px;
  margin-bottom: 15px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: 16px;
  gap: 15px;
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent-15);
  border: 1px solid var(--accent-15);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-icon .material-symbols-outlined {
  font-size: 20px;
  color: var(--accent);
}

.activity-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-text {
  font-size: 14px;
  color: var(--text-1);
  letter-spacing: 1px;
}

.activity-time {
  font-size: 12px;
  color: var(--text-3);
}

.footer {
  text-align: center;
  margin-top: 60px;
  padding-bottom: 40px;
  opacity: 0.4;
}

.footer-text {
  font-size: 14px;
  color: var(--text-3);
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

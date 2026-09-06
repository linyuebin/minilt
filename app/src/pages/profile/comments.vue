<template>
  <view class="comments-page" :class="theme.themeClass">
    <!-- 自定义背景层 -->
    <CustomBackground />

    <!-- 顶部：返回 + 居中标题（两侧缀点）+ 占位 -->
    <view class="page-header">
      <view class="header-btn" @click="goBack">
        <text class="material-symbols-outlined">arrow_back</text>
      </view>
      <view class="header-center">
        <view class="title-row">
          <view class="title-dot"></view>
          <text class="page-title">感悟</text>
          <view class="title-dot"></view>
        </view>
        <text class="header-sub">我的研习心得 · 共 {{ comments.length }} 篇</text>
      </view>
      <view class="header-btn is-placeholder"></view>
    </view>

    <scroll-view scroll-y class="page-body">
      <view v-if="comments.length === 0 && !loading" class="empty-state">
        <text class="material-symbols-outlined empty-glyph">chat_bubble</text>
        <text class="empty-text">暂无感悟</text>
        <text class="empty-tip">去阅读页面分享你的心得吧</text>
      </view>

      <!-- 感悟卡片流：头像 + 全文收起 + 配图 + 章节来源与互动数 -->
      <view
        v-for="comment in comments"
        :key="comment.id"
        class="feed-card"
        @click="jumpToChapter(comment.chapterId)"
      >
        <view class="card-header">
          <view class="card-avatar">
            <text>{{ avatarChar }}</text>
          </view>
          <view class="card-userinfo">
            <text class="card-nickname">{{ nickname }} (我)</text>
            <text class="card-time">{{ comment.createdAt }}</text>
          </view>
        </view>

        <view class="card-content">
          <text class="card-text" :class="{ 'is-clamped': !comment.expanded }">{{ comment.content }}</text>
          <text
            v-if="isLongText(comment.content)"
            class="expand-btn"
            @click.stop="toggleExpand(comment)"
          >{{ comment.expanded ? '收起' : '全文' }}</text>
        </view>

        <!-- 配图：1张单图 / 2张两列 / 3张三列 -->
        <view
          v-if="comment.images && comment.images.length"
          class="card-images"
          :class="'img-count-' + comment.images.length"
        >
          <view
            v-for="(img, idx) in comment.images"
            :key="img"
            class="card-image-item"
            @click.stop="previewImages(comment.images, idx)"
          >
            <image :src="resolveImageUrl(img)" mode="aspectFill" class="card-image" />
          </view>
        </view>

        <view class="card-footer">
          <text class="card-source">{{ comment.chapterTitle ? '《' + comment.chapterTitle + '》' : '随笔心得' }}</text>
          <view class="card-stats">
            <view class="stat-item">
              <text class="material-symbols-outlined stat-icon">chat_bubble</text>
              <text>{{ comment.replyCount || 0 }}</text>
            </view>
            <view class="stat-item stat-accent">
              <text class="material-symbols-outlined stat-icon">thumb_up</text>
              <text>{{ comment.likeCount }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-state">
        <text>加载中...</text>
      </view>
      <view class="body-spacer"></view>
    </scroll-view>

    <!-- 悬浮按钮：写感悟（弹层与道友页共用） -->
    <view class="fab" @click="openCompose">
      <text class="material-symbols-outlined fab-icon">edit</text>
      <text class="fab-text">写感悟</text>
    </view>

    <!-- 写感悟弹层（共享组件） -->
    <ComposeSheet v-model:show="showCompose" @published="loadComments" />

    <!-- 后台诵读悬浮条 -->
    <AudioBar :tab-bar="false" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useThemeStore } from '@/store/theme'
import { useUserStore } from '@/store/user'
import CustomBackground from '@/components/CustomBackground.vue'
import ComposeSheet from '@/components/ComposeSheet.vue'
import AudioBar from '@/components/AudioBar.vue'
import { getMyComments } from '@/api'
import { resolveImageUrl } from '@/utils/request'

const theme = useThemeStore()
const userStore = useUserStore()

// 返回个人中心
const goBack = () => {
  uni.navigateBack({
    fail: () => uni.switchTab({ url: '/pages/profile/index' })
  })
}

// 写感悟弹层（与道友页同款：引用正文可选章节，或直接发随笔）
const showCompose = ref(false)
const openCompose = () => {
  showCompose.value = true
}

const comments = ref([])
const loading = ref(false)

const nickname = computed(() => userStore.userInfo?.nickname || '道友')
const avatarChar = computed(() => userStore.userInfo?.avatarChar || '道')

// 超过 60 字视为长文，显示"全文/收起"
const LONG_TEXT_LEN = 60
const isLongText = (content) => (content || '').length > LONG_TEXT_LEN

const toggleExpand = (comment) => {
  comment.expanded = !comment.expanded
}

// 预览配图
const previewImages = (images, idx) => {
  const urls = (images || []).map(resolveImageUrl)
  if (!urls.length) return
  uni.previewImage({ urls, current: idx })
}

// 后端直接返回留言数组（不分页）
const loadComments = async () => {
  loading.value = true
  try {
    const list = (await getMyComments()) || []
    comments.value = list.map(c => ({ ...c, expanded: false }))
  } catch (error) {
    console.error('加载留言失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadComments()
})

// 跳转到对应章节继续交流
const jumpToChapter = (chapterId) => {
  uni.navigateTo({
    url: `/pages/reading/index?chapterId=${chapterId}`
  })
}
</script>

<style lang="scss" scoped>
.comments-page {
  min-height: 100vh;
  background: var(--bg-flat);
  padding: 30rpx 30rpx 0;
  box-sizing: border-box;
}

/* 内容区块位于自定义背景层之上 */
.page-header,
.page-body {
  position: relative;
  z-index: 1;
}

// 顶部：圆形返回钮 + 居中标题（两侧缀点）+ 副标题统计
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0 30rpx;

  .header-btn {
    width: 68rpx;
    height: 68rpx;
    border-radius: 50%;
    background: var(--surface-1);
    border: 1px solid var(--accent-15);
    color: var(--accent);
    display: flex;
    align-items: center;
    justify-content: center;

    .material-symbols-outlined {
      font-size: 40rpx;
    }

    &.is-placeholder {
      opacity: 0;
      pointer-events: none;
    }
  }

  .header-center {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6rpx;
  }

  .title-row {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  .title-dot {
    width: 10rpx;
    height: 10rpx;
    border-radius: 50%;
    background: var(--accent);
    opacity: 0.8;
  }

  .page-title {
    font-size: 38rpx;
    font-weight: 500;
    color: var(--accent);
    letter-spacing: 8rpx;
  }

  .header-sub {
    font-size: 22rpx;
    color: var(--text-3);
    letter-spacing: 1rpx;
  }
}

.page-body {
  height: calc(100vh - 220rpx);
}

.empty-state {
  text-align: center;
  padding: 150rpx 0;

  .empty-glyph {
    display: block;
    font-size: 80rpx;
    color: var(--accent-strong);
    margin-bottom: 30rpx;
  }

  .empty-text {
    display: block;
    font-size: 32rpx;
    color: var(--text-2);
    margin-bottom: 16rpx;
  }

  .empty-tip {
    display: block;
    font-size: 26rpx;
    color: var(--text-3);
  }
}

// 感悟卡片
.feed-card {
  background: var(--surface-1);
  border: 1px solid var(--accent-15);
  border-radius: 24rpx;
  padding: 30rpx;
  margin-bottom: 28rpx;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.25);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.card-avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-30), var(--surface-2));
  border: 1px solid var(--accent-30);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  font-size: 28rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.card-userinfo {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.card-nickname {
  font-size: 28rpx;
  font-weight: 500;
  color: var(--text-1);
}

.card-time {
  font-size: 22rpx;
  color: var(--text-3);
}

// 正文：默认 3 行截断，"全文"展开
.card-content {
  margin-top: 20rpx;
}

.card-text {
  display: block;
  font-size: 29rpx;
  line-height: 1.7;
  color: var(--text-2);
  letter-spacing: 0.5rpx;
}

.card-text.is-clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.expand-btn {
  display: inline-block;
  font-size: 24rpx;
  color: var(--accent-strong);
  margin-top: 8rpx;
}

// 配图网格
.card-images {
  display: grid;
  gap: 10rpx;
  margin-top: 20rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.card-images.img-count-1 {
  grid-template-columns: 1fr;
  max-width: 420rpx;
}

.card-images.img-count-1 .card-image-item {
  aspect-ratio: 4 / 3;
}

.card-images.img-count-2 {
  grid-template-columns: repeat(2, 1fr);
  max-width: 480rpx;
}

.card-images.img-count-2 .card-image-item,
.card-images.img-count-3 .card-image-item {
  aspect-ratio: 1;
}

.card-images.img-count-3 {
  grid-template-columns: repeat(3, 1fr);
}

.card-image-item {
  border-radius: 12rpx;
  overflow: hidden;
  background: var(--surface-2);
  border: 1px solid var(--accent-15);
}

.card-image {
  display: block;
  width: 100%;
  height: 100%;
}

// 底部：章节来源 + 互动数
.card-footer {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1px solid var(--accent-15);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-source {
  font-size: 22rpx;
  color: var(--text-3);
  letter-spacing: 1rpx;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 30rpx;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
  color: var(--text-3);

  .stat-icon {
    font-size: 26rpx;
  }

  &.stat-accent {
    color: var(--accent-strong);
  }
}

.loading-state {
  text-align: center;
  padding: 40rpx 0;
  color: var(--text-3);
  font-size: 28rpx;
}

// 底部给悬浮按钮留白
.body-spacer {
  height: 150rpx;
}

// 悬浮按钮：写感悟
.fab {
  position: fixed;
  right: 40rpx;
  bottom: calc(60rpx + env(safe-area-inset-bottom));
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 22rpx 34rpx;
  border-radius: 60rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  box-shadow: 0 16rpx 44rpx rgba(0, 0, 0, 0.35);

  .fab-icon {
    font-size: 34rpx;
    color: #16130c;
  }

  .fab-text {
    font-size: 27rpx;
    font-weight: 600;
    color: #16130c;
    letter-spacing: 2rpx;
  }
}
</style>

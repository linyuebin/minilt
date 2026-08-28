<template>
  <view class="favorites-page" :class="theme.themeClass">
    <!-- 自定义背景层 -->
    <CustomBackground />

    <view class="page-header">
      <text class="page-title">修道藏经</text>
      <text class="page-subtitle">你收藏的章节</text>
    </view>

    <scroll-view scroll-y class="favorites-list" @scrolltolower="loadMore">
      <view v-if="favorites.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">📚</text>
        <text class="empty-text">暂无收藏</text>
        <text class="empty-tip">阅读时点击收藏按钮即可收藏章节</text>
      </view>

      <view
        v-for="item in favorites"
        :key="item.id"
        class="favorite-item"
        @click="navigateToChapter(item.chapterId)"
      >
        <view class="favorite-header">
          <text class="chapter-num">第{{ item.chapterId }}章</text>
          <text class="chapter-title">{{ item.chapterTitle }}</text>
          <text class="favorite-icon">★</text>
        </view>

        <view class="chapter-preview">
          <text>{{ item.contentPreview }}</text>
        </view>

        <view class="favorite-meta">
          <text class="favorite-time">收藏于 {{ item.createdAt }}</text>
        </view>
      </view>

      <view v-if="loading" class="loading-state">
        <text>加载中...</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useThemeStore } from '@/store/theme'
import CustomBackground from '@/components/CustomBackground.vue'
import { getFavorites } from '@/api'

const theme = useThemeStore()

const favorites = ref([])
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

const loadFavorites = async () => {
  if (loading.value || !hasMore.value) return

  loading.value = true
  try {
    const result = await getFavorites({
      page: page.value,
      size: 20
    })

    if (page.value === 1) {
      favorites.value = result.records || []
    } else {
      favorites.value.push(...(result.records || []))
    }

    hasMore.value = result.records?.length === 20
  } catch (error) {
    console.error('加载收藏失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    page.value++
    loadFavorites()
  }
}

const navigateToChapter = (chapterId) => {
  uni.switchTab({
    url: '/pages/reading/index'
  })
  // TODO: 可以通过事件总线或store传递章节ID
}

onMounted(() => {
  loadFavorites()
})
</script>

<style lang="scss" scoped>
.favorites-page {
  min-height: 100vh;
  background: var(--bg-flat);
  padding: 40rpx;
  box-sizing: border-box;
}

/* 内容区块位于自定义背景层之上 */
.page-header,
.favorites-list {
  position: relative;
  z-index: 1;
}

.page-header {
  margin-bottom: 40rpx;

  .page-title {
    font-size: 40rpx;
    font-weight: bold;
    color: var(--accent);
    display: block;
  }

  .page-subtitle {
    font-size: 28rpx;
    color: #888;
    margin-top: 10rpx;
    display: block;
  }
}

.favorites-list {
  height: calc(100vh - 300rpx);
}

.empty-state {
  text-align: center;
  padding: 150rpx 0;

  .empty-icon {
    display: block;
    font-size: 80rpx;
    margin-bottom: 30rpx;
  }

  .empty-text {
    display: block;
    font-size: 32rpx;
    color: #666;
    margin-bottom: 20rpx;
  }

  .empty-tip {
    display: block;
    font-size: 28rpx;
    color: #444;
  }
}

.favorite-item {
  background: var(--surface-1);
  border: 1px solid var(--accent-15);
  border-radius: 12rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;

  .favorite-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .chapter-num {
      background: var(--accent-15);
      color: var(--accent);
      font-size: 24rpx;
      padding: 6rpx 12rpx;
      border-radius: 6rpx;
      margin-right: 15rpx;
    }

    .chapter-title {
      flex: 1;
      font-size: 32rpx;
      color: #e0e0e0;
      font-weight: bold;
    }

    .favorite-icon {
      font-size: 32rpx;
      color: var(--accent);
    }
  }

  .chapter-preview {
    font-size: 28rpx;
    line-height: 1.6;
    color: #888;
    margin-bottom: 20rpx;

    text {
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .favorite-meta {
    .favorite-time {
      font-size: 24rpx;
      color: #666;
    }
  }
}

.loading-state {
  text-align: center;
  padding: 40rpx 0;
  color: #666;
  font-size: 28rpx;
}
</style>

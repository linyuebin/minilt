<template>
  <view class="likes-page">
    <view class="page-header">
      <text class="page-title">我的点赞</text>
    </view>

    <scroll-view scroll-y class="likes-list" @scrolltolower="loadMore">
      <view v-if="likes.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">❤️</text>
        <text class="empty-text">暂无点赞</text>
      </view>

      <view
        v-for="item in likes"
        :key="item.id"
        class="like-item"
      >
        <view class="like-chapter">
          <text class="chapter-tag">第{{ item.chapterId }}章</text>
          <text class="chapter-title">{{ item.chapterTitle }}</text>
        </view>

        <view class="comment-preview">
          <text>{{ item.content }}</text>
        </view>

        <view class="like-meta">
          <text class="like-time">{{ item.createdAt }}</text>
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
import { getMyLikes } from '@/api'

const likes = ref([])
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

const loadLikes = async () => {
  if (loading.value || !hasMore.value) return

  loading.value = true
  try {
    const result = await getMyLikes({
      page: page.value,
      size: 20
    })

    if (page.value === 1) {
      likes.value = result.records || []
    } else {
      likes.value.push(...(result.records || []))
    }

    hasMore.value = result.records?.length === 20
  } catch (error) {
    console.error('加载点赞失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    page.value++
    loadLikes()
  }
}

onMounted(() => {
  loadLikes()
})
</script>

<style lang="scss" scoped>
.likes-page {
  min-height: 100vh;
  background: #0a0a0a;
  padding: 40rpx;
  box-sizing: border-box;
}

.page-header {
  margin-bottom: 40rpx;

  .page-title {
    font-size: 40rpx;
    font-weight: bold;
    color: #d4af37;
  }
}

.likes-list {
  height: calc(100vh - 200rpx);
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
  }
}

.like-item {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;

  .like-chapter {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .chapter-tag {
      background: rgba(212, 175, 55, 0.2);
      color: #d4af37;
      font-size: 24rpx;
      padding: 6rpx 12rpx;
      border-radius: 6rpx;
      margin-right: 15rpx;
    }

    .chapter-title {
      font-size: 28rpx;
      color: #888;
    }
  }

  .comment-preview {
    font-size: 30rpx;
    line-height: 1.6;
    color: #e0e0e0;
    margin-bottom: 20rpx;
  }

  .like-meta {
    .like-time {
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

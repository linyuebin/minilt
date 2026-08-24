<template>
  <view class="replies-page">
    <view class="page-header">
      <text class="page-title">历史回复</text>
    </view>

    <scroll-view scroll-y class="replies-list" @scrolltolower="loadMore">
      <view v-if="replies.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">↩️</text>
        <text class="empty-text">暂无回复</text>
      </view>

      <view
        v-for="item in replies"
        :key="item.id"
        class="reply-item"
      >
        <view class="reply-chapter">
          <text class="chapter-tag">第{{ item.chapterId }}章</text>
          <text class="chapter-title">{{ item.chapterTitle }}</text>
        </view>

        <view class="reply-content">
          <text>{{ item.content }}</text>
        </view>

        <view class="reply-meta">
          <text class="reply-time">{{ item.createdAt }}</text>
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
import { getMyReplies } from '@/api'

const replies = ref([])
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

const loadReplies = async () => {
  if (loading.value || !hasMore.value) return

  loading.value = true
  try {
    const result = await getMyReplies({
      page: page.value,
      size: 20
    })

    if (page.value === 1) {
      replies.value = result.records || []
    } else {
      replies.value.push(...(result.records || []))
    }

    hasMore.value = result.records?.length === 20
  } catch (error) {
    console.error('加载回复失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    page.value++
    loadReplies()
  }
}

onMounted(() => {
  loadReplies()
})
</script>

<style lang="scss" scoped>
.replies-page {
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

.replies-list {
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

.reply-item {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;

  .reply-chapter {
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

  .reply-content {
    font-size: 30rpx;
    line-height: 1.6;
    color: #e0e0e0;
    margin-bottom: 20rpx;
  }

  .reply-meta {
    .reply-time {
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

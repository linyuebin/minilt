<template>
  <view class="replies-page">
    <view class="page-header">
      <text class="page-title">历史回复</text>
    </view>

    <scroll-view scroll-y class="replies-list">
      <view v-if="replies.length === 0 && !loading" class="empty-state">
        <text class="material-symbols-outlined empty-glyph">forum</text>
        <text class="empty-text">暂无回复</text>
        <text class="empty-tip">回复道友的留言会出现在这里</text>
      </view>

      <view
        v-for="item in replies"
        :key="item.id"
        class="reply-item"
        @click="jumpToChapter(item.chapterId)"
      >
        <view class="reply-chapter">
          <text class="chapter-tag">第{{ item.chapterId }}章</text>
          <text class="chapter-title">{{ item.chapterTitle }}</text>
          <text class="material-symbols-outlined item-arrow">chevron_right</text>
        </view>

        <!-- 引用原留言，形成对话上下文 -->
        <view class="quote-box">
          <text class="quote-label">回复 @{{ item.targetNickname }}</text>
          <text class="quote-text">{{ item.targetContent }}</text>
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

// 后端直接返回回复列表数组（不分页）
const loadReplies = async () => {
  loading.value = true
  try {
    replies.value = (await getMyReplies()) || []
  } catch (error) {
    console.error('加载回复失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadReplies()
})

// 跳转到对应章节查看对话上下文
const jumpToChapter = (chapterId) => {
  uni.navigateTo({
    url: `/pages/reading/index?chapterId=${chapterId}`
  })
}
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

  .empty-glyph {
    display: block;
    font-size: 80rpx;
    color: rgba(212, 175, 55, 0.4);
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

    .item-arrow {
      margin-left: auto;
      font-size: 28rpx;
      color: rgba(255, 255, 255, 0.3);
    }
  }

  // 引用的原留言
  .quote-box {
    padding: 16rpx 20rpx;
    margin-bottom: 20rpx;
    background: rgba(255, 255, 255, 0.04);
    border-left: 4rpx solid rgba(212, 175, 55, 0.4);
    border-radius: 8rpx;

    .quote-label {
      display: block;
      font-size: 24rpx;
      color: rgba(212, 175, 55, 0.7);
      margin-bottom: 8rpx;
    }

    .quote-text {
      display: block;
      font-size: 26rpx;
      color: #888;
      line-height: 1.5;
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

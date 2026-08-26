<template>
  <view class="comments-page">
    <view class="page-header">
      <text class="page-title">我的留言</text>
    </view>

    <scroll-view scroll-y class="comments-list">
      <view v-if="comments.length === 0 && !loading" class="empty-state">
        <text class="material-symbols-outlined empty-glyph">chat_bubble</text>
        <text class="empty-text">暂无留言</text>
        <text class="empty-tip">去阅读页面分享你的感悟吧</text>
      </view>

      <view
        v-for="comment in comments"
        :key="comment.id"
        class="comment-item"
        @click="jumpToChapter(comment.chapterId)"
      >
        <view class="comment-chapter">
          <text class="chapter-tag">第{{ comment.chapterId }}章</text>
          <text class="chapter-title">{{ comment.chapterTitle }}</text>
          <text class="material-symbols-outlined item-arrow">chevron_right</text>
        </view>

        <view class="comment-content">
          <text>{{ comment.content }}</text>
        </view>

        <view class="comment-meta">
          <view class="meta-left">
            <text class="comment-time">{{ comment.createdAt }}</text>
            <text class="comment-replies">{{ comment.replyCount || 0 }} 条回复</text>
            <text class="comment-likes">{{ comment.likeCount }} 赞</text>
          </view>
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
import { getMyComments } from '@/api'

const comments = ref([])
const loading = ref(false)

// 后端直接返回留言数组（不分页）
const loadComments = async () => {
  loading.value = true
  try {
    comments.value = (await getMyComments()) || []
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

.comments-list {
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

.comment-item {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 12rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;

  .comment-chapter {
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

  .comment-content {
    font-size: 32rpx;
    line-height: 1.6;
    color: #e0e0e0;
    margin-bottom: 20rpx;
  }

  .comment-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .meta-left {
      display: flex;
      gap: 30rpx;

      .comment-time {
        font-size: 24rpx;
        color: #666;
      }

      .comment-replies,
      .comment-likes {
        font-size: 24rpx;
        color: #888;
      }

      .comment-replies {
        color: rgba(212, 175, 55, 0.7);
      }
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

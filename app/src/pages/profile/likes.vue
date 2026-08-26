<template>
  <view class="likes-page">
    <view class="page-header">
      <text class="page-title">我的点赞</text>
    </view>

    <scroll-view scroll-y class="likes-list">
      <view v-if="likes.length === 0 && !loading" class="empty-state">
        <text class="material-symbols-outlined empty-glyph">favorite</text>
        <text class="empty-text">暂无点赞</text>
        <text class="empty-tip">给共鸣的留言点个赞吧</text>
      </view>

      <view
        v-for="item in likes"
        :key="item.commentId"
        class="like-item"
        @click="jumpToChapter(item.chapterId)"
      >
        <view class="like-chapter">
          <text class="chapter-tag">第{{ item.chapterId }}章</text>
          <text class="chapter-title">{{ item.chapterTitle }}</text>
          <text class="material-symbols-outlined item-arrow">chevron_right</text>
        </view>

        <view class="comment-preview">
          <text>{{ item.content }}</text>
        </view>

        <view class="like-meta">
          <text class="like-author">@{{ item.authorNickname }}</text>
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

// 后端直接返回点赞列表数组（不分页）
const loadLikes = async () => {
  loading.value = true
  try {
    likes.value = (await getMyLikes()) || []
  } catch (error) {
    console.error('加载点赞失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLikes()
})

// 跳转到对应章节查看原留言
const jumpToChapter = (chapterId) => {
  uni.navigateTo({
    url: `/pages/reading/index?chapterId=${chapterId}`
  })
}
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

    .item-arrow {
      margin-left: auto;
      font-size: 28rpx;
      color: rgba(255, 255, 255, 0.3);
    }
  }

  .comment-preview {
    font-size: 30rpx;
    line-height: 1.6;
    color: #e0e0e0;
    margin-bottom: 20rpx;
  }

  .like-meta {
    display: flex;
    gap: 24rpx;

    .like-author {
      font-size: 24rpx;
      color: rgba(212, 175, 55, 0.7);
    }

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

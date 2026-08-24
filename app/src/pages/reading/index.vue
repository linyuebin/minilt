<template>
  <view class="reading-container">
    <!-- 章节内容显示 -->
    <view class="chapter-content" :class="orientationClass">
      <!-- 标题 -->
      <view class="chapter-title">
        <text>第{{ currentChapter.id }}章 {{ currentChapter.title }}</text>
      </view>

      <!-- 经文内容 -->
      <!-- 竖排时文字是横向排列的，溢出方向也是横向，必须切成横向滚动才能读到长章节 -->
      <scroll-view
        :scroll-y="isHorizontal"
        :scroll-x="!isHorizontal"
        class="content-scroll"
        :class="orientationClass"
        @scrolltolower="handleScrollToLower"
      >
        <view class="chapter-text" :class="orientationClass">
          <text class="text-line" v-for="(line, index) in displayLines" :key="index">
            {{ line }}
          </text>
        </view>
      </scroll-view>

      <!-- 翻页按钮 -->
      <view class="pagination-controls">
        <button
          class="page-btn prev-btn"
          :disabled="!hasPrevChapter"
          @click="prevChapter"
        >
          上一章
        </button>
        <button
          class="page-btn next-btn"
          :disabled="!hasNextChapter"
          @click="nextChapter"
        >
          下一章
        </button>
      </view>
    </view>

    <!-- 顶部工具栏 -->
    <view class="toolbar top-toolbar">
      <view class="toolbar-btn" @click="toggleChapterDrawer">
        <text>📖</text>
      </view>
      <view class="toolbar-btn" @click="toggleSettings">
        <text>⚙️</text>
      </view>
      <view class="toolbar-btn" @click="toggleZenMode">
        <text>🧘</text>
      </view>
    </view>

    <!-- 底部工具栏 -->
    <view class="toolbar bottom-toolbar">
      <view class="toolbar-btn" @click="toggleOrientation">
        <text>{{ isHorizontal ? '竖' : '横' }}</text>
      </view>
      <view class="toolbar-btn" @click="toggleInterpretation">
        <text>释义</text>
      </view>
      <view class="toolbar-btn" @click="toggleComments">
        <text>💬</text>
      </view>
      <view class="toolbar-btn" @click="toggleFavorite">
        <text>{{ currentChapter.favorited ? '★' : '☆' }}</text>
      </view>
    </view>

    <!-- 章节抽屉 -->
    <view class="chapter-drawer" :class="{ 'drawer-open': showChapterDrawer }">
      <view class="drawer-mask" @click="toggleChapterDrawer"></view>
      <view class="drawer-content">
        <view class="drawer-header">
          <text class="drawer-title">章节目录</text>
          <text class="drawer-close" @click="toggleChapterDrawer">×</text>
        </view>

        <!-- 搜索框 -->
        <view class="search-box">
          <input
            v-model="searchKeyword"
            placeholder="搜索章节..."
            class="search-input"
            @input="handleSearch"
          />
        </view>

        <!-- 章节列表 -->
        <scroll-view scroll-y class="chapter-list">
          <view
            v-for="chapter in filteredChapters"
            :key="chapter.id"
            class="chapter-item"
            :class="{ 'current-chapter': chapter.id === currentChapter.id }"
            @click="selectChapter(chapter)"
          >
            <text class="chapter-num">第{{ chapter.id }}章</text>
            <text class="chapter-name">{{ chapter.title }}</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 释义面板 -->
    <view class="interpretation-panel" :class="{ 'panel-open': showInterpretation }">
      <view class="panel-mask" @click="toggleInterpretation"></view>
      <view class="panel-content">
        <view class="panel-header">
          <text class="panel-title">释义</text>
          <text class="panel-close" @click="toggleInterpretation">×</text>
        </view>
        <scroll-view scroll-y class="interpretation-text">
          <text>{{ currentChapter.interpretation }}</text>
        </scroll-view>
      </view>
    </view>

    <!-- 设置面板 -->
    <view class="settings-panel" :class="{ 'panel-open': showSettings }">
      <view class="panel-mask" @click="toggleSettings"></view>
      <view class="panel-content">
        <view class="panel-header">
          <text class="panel-title">修行设置</text>
          <text class="panel-close" @click="toggleSettings">×</text>
        </view>

        <!-- 配色方案 -->
        <view class="settings-section">
          <text class="section-title">配色方案</text>
          <view class="color-schemes">
            <view
              v-for="scheme in colorSchemes"
              :key="scheme.id"
              class="scheme-item"
              :class="{ 'scheme-active': currentScheme === scheme.id }"
              :style="{ background: scheme.preview }"
              @click="selectScheme(scheme.id)"
            >
              <text class="scheme-name">{{ scheme.name }}</text>
            </view>
          </view>
        </view>

        <!-- 字体大小 -->
        <view class="settings-section">
          <text class="section-title">字体大小</text>
          <view class="font-size-control">
            <button class="size-btn" @click="decreaseFontSize">-</button>
            <text class="size-display">{{ fontSize }}rpx</text>
            <button class="size-btn" @click="increaseFontSize">+</button>
          </view>
        </view>
      </view>
    </view>

    <!-- 评论面板（简化版） -->
    <view class="comments-panel" :class="{ 'panel-open': showComments }">
      <view class="panel-mask" @click="toggleComments"></view>
      <view class="panel-content">
        <view class="panel-header">
          <text class="panel-title">悟道留言</text>
          <text class="panel-close" @click="toggleComments">×</text>
        </view>

        <!-- 评论列表 -->
        <scroll-view scroll-y class="comments-list">
          <view v-if="comments.length === 0" class="no-comments">
            <text>暂无留言，快来分享你的感悟吧</text>
          </view>
          <view
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <view class="comment-header">
              <text class="comment-nickname">{{ comment.nickname }}</text>
              <text class="comment-time">{{ shortTime(comment.createdAt) }}</text>
            </view>
            <text class="comment-content">{{ comment.content }}</text>
            <view class="comment-actions">
              <text class="action-btn" @click="handleCommentLike(comment)">
                {{ comment.liked ? '❤️' : '🤍' }} {{ comment.likeCount }}
              </text>
              <text class="action-btn" @click="startReply(comment)">💬 回复</text>
            </view>

            <!-- 嵌套回复 -->
            <view v-if="comment.replies && comment.replies.length" class="reply-list">
              <view v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <view class="reply-header">
                  <text class="reply-nickname">{{ reply.nickname }}</text>
                  <text v-if="reply.replyToNickname" class="reply-to">回复 {{ reply.replyToNickname }}</text>
                  <text class="reply-time">{{ shortTime(reply.createdAt) }}</text>
                </view>
                <text class="reply-content">{{ reply.content }}</text>
              </view>
            </view>
          </view>
        </scroll-view>

        <!-- 回复对象提示 -->
        <view v-if="replyTarget" class="reply-hint">
          <text>回复 {{ replyTarget.nickname }}</text>
          <text class="reply-cancel" @click="cancelReply">取消</text>
        </view>

        <!-- 发表评论输入框 -->
        <view class="comment-input-section">
          <input
            v-model="newComment"
            :placeholder="replyTarget ? `回复 ${replyTarget.nickname}...` : '分享你的感悟...'"
            class="comment-input"
          />
          <button class="send-btn" @click="sendComment" :disabled="!canSendComment">
            发送
          </button>
        </view>
      </view>
    </view>

    <!-- 入定模式 -->
    <view class="zen-mode" :class="{ 'zen-active': zenMode }">
      <view class="zen-content">
        <text class="zen-text">道可道，非常道</text>
        <text class="zen-subtitle">静心悟道</text>
      </view>
      <view class="zen-close" @click="toggleZenMode">
        <text>退出入定</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getChapters, getChapterDetail, getChapterComments, createComment, toggleCommentLike, addFavorite, removeFavorite, updateProgress } from '@/api'

const userStore = useUserStore()

// 章节相关
const currentChapter = ref({
  id: 1,
  title: '道可道',
  content: '',
  interpretation: '',
  favorited: false
})
const allChapters = ref([])
const searchKeyword = ref('')

// 显示状态
const showChapterDrawer = ref(false)
const showInterpretation = ref(false)
const showSettings = ref(false)
const showComments = ref(false)
const zenMode = ref(false)

// 阅读设置
const isHorizontal = ref(true)
const fontSize = ref(32)
const currentScheme = ref('ink')

// 配色方案
const colorSchemes = [
  { id: 'ink', name: '水墨', preview: '#0a0a0a' },
  { id: 'warm', name: '暖色', preview: '#2d1810' },
  { id: 'forest', name: '森林', preview: '#0a1a0a' },
  { id: 'night', name: '夜色', preview: '#050510' },
  { id: 'parchment', name: '宣纸', preview: '#d4c4a8' }
]

// 评论相关
const comments = ref([])
const replyTarget = ref(null)
const newComment = ref('')

// 计算属性
const filteredChapters = computed(() => {
  if (!searchKeyword.value) return allChapters.value
  const keyword = searchKeyword.value.toLowerCase()
  return allChapters.value.filter(chapter =>
    chapter.title.toLowerCase().includes(keyword) ||
    chapter.id.toString().includes(keyword)
  )
})

const displayLines = computed(() => {
  if (!currentChapter.value.content) return []
  return currentChapter.value.content.split('。').filter(line => line.trim())
})

const orientationClass = computed(() => ({
  'horizontal': isHorizontal.value,
  'vertical': !isHorizontal.value
}))

const hasPrevChapter = computed(() => currentChapter.value.id > 1)
const hasNextChapter = computed(() => currentChapter.value.id < 81)

const canSendComment = computed(() => {
  return newComment.value.trim() && userStore.isLoggedIn()
})

// 加载章节列表
const loadChapters = async () => {
  try {
    const result = await getChapters()
    allChapters.value = result
  } catch (error) {
    console.error('加载章节失败:', error)
  }
}

// 加载章节详情
const loadChapterDetail = async (chapterId) => {
  try {
    const result = await getChapterDetail(chapterId)
    currentChapter.value = result

    // 更新阅读进度
    if (userStore.isLoggedIn()) {
      await updateProgress(chapterId)
      userStore.updateProgress(chapterId)
    }

    // 加载评论
    await loadComments(chapterId)
  } catch (error) {
    console.error('加载章节详情失败:', error)
  }
}

// 加载评论
const loadComments = async (chapterId) => {
  try {
    // 接口直接返回留言数组（每条含 replies 回复列表）
    comments.value = (await getChapterComments(chapterId)) || []
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

// 选择章节
const selectChapter = async (chapter) => {
  if (chapter.id === currentChapter.value.id) return
  await loadChapterDetail(chapter.id)
  toggleChapterDrawer()
}

// 上一章
const prevChapter = async () => {
  if (!hasPrevChapter.value) return
  await loadChapterDetail(currentChapter.value.id - 1)
}

// 下一章
const nextChapter = async () => {
  if (!hasNextChapter.value) return
  await loadChapterDetail(currentChapter.value.id + 1)
}

// 章节抽屉
const toggleChapterDrawer = () => {
  showChapterDrawer.value = !showChapterDrawer.value
}

// 释义面板
const toggleInterpretation = () => {
  showInterpretation.value = !showInterpretation.value
}

// 设置面板
const toggleSettings = () => {
  showSettings.value = !showSettings.value
}

// 评论面板
const toggleComments = async () => {
  if (!showComments.value) {
    // 打开时重新加载评论
    await loadComments(currentChapter.value.id)
  }
  showComments.value = !showComments.value
}

// 入定模式
const toggleZenMode = () => {
  zenMode.value = !zenMode.value
}

// 横竖排切换
const toggleOrientation = () => {
  isHorizontal.value = !isHorizontal.value
  // 保存设置
  uni.setStorageSync('orientation', isHorizontal.value)
}

// 收藏切换
const toggleFavorite = async () => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    return
  }

  try {
    if (currentChapter.value.favorited) {
      await removeFavorite(currentChapter.value.id)
      currentChapter.value.favorited = false
      uni.showToast({
        title: '已取消收藏',
        icon: 'none'
      })
    } else {
      await addFavorite(currentChapter.value.id)
      currentChapter.value.favorited = true
      uni.showToast({
        title: '收藏成功',
        icon: 'success'
      })
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
  }
}

// 配色方案选择
const selectScheme = (schemeId) => {
  currentScheme.value = schemeId
  uni.setStorageSync('colorScheme', schemeId)
}

// 字体大小控制
const increaseFontSize = () => {
  if (fontSize.value < 48) {
    fontSize.value += 2
    uni.setStorageSync('fontSize', fontSize.value)
  }
}

const decreaseFontSize = () => {
  if (fontSize.value > 24) {
    fontSize.value -= 2
    uni.setStorageSync('fontSize', fontSize.value)
  }
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑在computed中处理
}

// 「2026-08-24 18:34」→「08-24 18:34」，面板窄，完整日期会被挤掉
const shortTime = (t) => (t || '').slice(5)

// 选中/取消回复对象
const startReply = (comment) => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  replyTarget.value = comment
}

const cancelReply = () => {
  replyTarget.value = null
}

// 发送评论
const sendComment = async () => {
  if (!canSendComment.value) {
    if (!userStore.isLoggedIn()) {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      })
    }
    return
  }

  try {
    // 有回复对象就带上 parentId，后端据此区分留言与回复
    await createComment({
      chapterId: currentChapter.value.id,
      content: newComment.value.trim(),
      ...(replyTarget.value
        ? { parentId: replyTarget.value.id, replyToUserId: replyTarget.value.userId }
        : {})
    })

    newComment.value = ''
    replyTarget.value = null
    uni.showToast({
      title: '发布成功',
      icon: 'success'
    })

    // 重新加载评论
    await loadComments(currentChapter.value.id)
  } catch (error) {
    console.error('发布评论失败:', error)
  }
}

// 点赞评论
const handleCommentLike = async (comment) => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    return
  }

  try {
    // 接口返回最新的 { liked, likeCount }，直接采信服务端结果
    const res = await toggleCommentLike(comment.id)
    comment.liked = res.liked
    comment.likeCount = res.likeCount
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// 滚动到底部
const handleScrollToLower = () => {
  // 可以加载更多评论等
}

// 初始化
onMounted(async () => {
  // 恢复设置
  const savedOrientation = uni.getStorageSync('orientation')
  const savedFontSize = uni.getStorageSync('fontSize')
  const savedScheme = uni.getStorageSync('colorScheme')

  if (savedOrientation !== undefined) isHorizontal.value = savedOrientation
  if (savedFontSize) fontSize.value = savedFontSize
  if (savedScheme) currentScheme.value = savedScheme

  // 加载数据
  await loadChapters()

  // 获取当前用户阅读进度
  const lastChapterId = userStore.userInfo?.lastChapterId || 1
  await loadChapterDetail(lastChapterId)
})
</script>

<style lang="scss" scoped>
.reading-container {
  min-height: 100vh;
  background: var(--bg-color, #0a0a0a);
  color: var(--text-color, #e0e0e0);
}

// 上下工具栏都是 fixed，正文要自己让出这两条的高度，否则标题压在顶栏下、翻页按钮压在底栏下
.chapter-content {
  padding: 140rpx 40rpx;
  min-height: 100vh;
  box-sizing: border-box;
  /* #ifdef H5 */
  // H5 还要再让开页面内的 tabBar（约 50px）
  padding-bottom: 240rpx;
  /* #endif */

  &.horizontal {
    .chapter-text {
      writing-mode: horizontal-tb;
    }
  }

  &.vertical {
    .chapter-text {
      writing-mode: vertical-rl;
      text-orientation: upright;
      height: 100%;
    }
  }
}

.chapter-title {
  text-align: center;
  font-size: 40rpx;
  font-weight: bold;
  color: #d4af37;
  margin-bottom: 60rpx;
  letter-spacing: 4rpx;
}

// 100vh 减去：上下工具栏 + 正文内边距 + 标题 + 翻页按钮，让整页正好落在一屏里
.content-scroll {
  height: calc(100vh - 540rpx);
  /* #ifdef H5 */
  height: calc(100vh - 640rpx);
  /* #endif */
}

.chapter-text {
  font-size: 32rpx;
  line-height: 2;
  letter-spacing: 2rpx;
  font-family: 'KaiTi', '楷体', serif;

  .text-line {
    display: block;
    margin-bottom: 20rpx;
  }
}

.pagination-controls {
  display: flex;
  justify-content: space-between;
  margin-top: 60rpx;
  padding: 0 40rpx;

  .page-btn {
    width: 200rpx;
    height: 80rpx;
    background: rgba(212, 175, 55, 0.2);
    border: 1px solid rgba(212, 175, 55, 0.5);
    color: #d4af37;
    border-radius: 8rpx;
    font-size: 32rpx;

    &:disabled {
      opacity: 0.3;
    }
  }
}

.toolbar {
  position: fixed;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  padding: 20rpx;
  background: rgba(26, 26, 26, 0.95);
  border-top: 1px solid rgba(212, 175, 55, 0.2);
  z-index: 100;

  &.top-toolbar {
    top: 0;
  }

  &.bottom-toolbar {
    bottom: 0;
    /* #ifdef H5 */
    // H5 的 tabBar 是页面内的固定元素，会盖住 bottom:0 的工具栏；
    // 小程序 tabBar 是原生的、不占页面视口，保持 bottom:0 即可
    bottom: 50px;
    /* #endif */
  }

  .toolbar-btn {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    border-radius: 50%;
    transition: background 0.3s;

    &:active {
      background: rgba(212, 175, 55, 0.2);
    }
  }
}

// 抽屉和面板
.chapter-drawer,
.interpretation-panel,
.settings-panel,
.comments-panel {
  position: fixed;
  top: 0;
  right: -750rpx;
  bottom: 0;
  width: 650rpx;
  transition: right 0.3s;
  z-index: 200;
  /* #ifdef H5 */
  // 让开 H5 页面内的 tabBar，否则面板底部的输入框会被盖住
  bottom: 50px;
  /* #endif */

  &.drawer-open,
  &.panel-open {
    right: 0;

    .drawer-mask,
    .panel-mask {
      opacity: 1;
      pointer-events: auto;
    }
  }
}

// 遮罩是 fixed 定位，不随父面板一起滑出屏幕，所以必须自己隐藏：
// 否则四个面板的遮罩会一直叠在阅读页上，把整页压暗到原亮度的 6%。
.drawer-mask,
.panel-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: -1;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s;
}

.drawer-content,
.panel-content {
  height: 100%;
  background: #1a1a1a;
  border-left: 1px solid rgba(212, 175, 55, 0.3);
  display: flex;
  flex-direction: column;
}

.drawer-header,
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx;
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);

  .drawer-title,
  .panel-title {
    font-size: 36rpx;
    font-weight: bold;
    color: #d4af37;
  }

  .drawer-close,
  .panel-close {
    font-size: 60rpx;
    color: #666;
  }
}

.search-box {
  padding: 30rpx;

  .search-input {
    width: 100%;
    height: 70rpx;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(212, 175, 55, 0.3);
    border-radius: 8rpx;
    padding: 0 30rpx;
    color: #fff;
    font-size: 28rpx;
  }
}

.chapter-list {
  flex: 1;
  padding: 0 30rpx;

  .chapter-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    &.current-chapter {
      background: rgba(212, 175, 55, 0.1);
    }

    .chapter-num {
      font-size: 28rpx;
      color: #888;
      margin-right: 20rpx;
      width: 120rpx;
    }

    .chapter-name {
      flex: 1;
      font-size: 32rpx;
    }
  }
}

.interpretation-text,
.comments-list {
  flex: 1;
  padding: 40rpx;
  // 不加 border-box 的话内边距会把滚动区撑出面板右缘，右侧文字（时间）被裁掉
  box-sizing: border-box;

  .interpretation-text-content {
    font-size: 30rpx;
    line-height: 1.8;
    color: #ccc;
  }
}

.comments-list {
  .comment-item {
    margin-bottom: 40rpx;
    padding-bottom: 40rpx;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .comment-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 20rpx;

      .comment-nickname {
        font-size: 28rpx;
        color: #d4af37;
      }

      .comment-time {
        font-size: 24rpx;
        color: #666;
      }
    }

    .comment-content {
      font-size: 30rpx;
      line-height: 1.6;
      color: #e0e0e0;
      display: block;
      margin-bottom: 20rpx;
    }

    .comment-actions {
      display: flex;
      align-items: center;
      gap: 32rpx;

      .action-btn {
        font-size: 28rpx;
        color: #888;
      }
    }

    // 嵌套回复：靠左侧竖线和缩进和顶级留言区分开
    .reply-list {
      margin-top: 20rpx;
      padding-left: 24rpx;
      border-left: 2rpx solid rgba(212, 175, 55, 0.3);

      .reply-item {
        padding: 12rpx 0;
      }

      .reply-header {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 12rpx;
        margin-bottom: 6rpx;
      }

      .reply-nickname {
        font-size: 26rpx;
        color: #d4af37;
      }

      .reply-to,
      .reply-time {
        font-size: 24rpx;
        color: #888;
      }

      .reply-content {
        display: block;
        font-size: 28rpx;
        color: #ccc;
        line-height: 1.6;
      }
    }
  }

  .no-comments {
    text-align: center;
    padding: 100rpx 0;
    color: #666;
    font-size: 28rpx;
  }
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 30rpx;
  background: rgba(212, 175, 55, 0.1);
  font-size: 26rpx;
  color: #d4af37;

  .reply-cancel {
    color: #888;
  }
}

.comment-input-section {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  .comment-input {
    flex: 1;
    height: 70rpx;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(212, 175, 55, 0.3);
    border-radius: 8rpx;
    padding: 0 20rpx;
    color: #fff;
    font-size: 28rpx;
    margin-right: 20rpx;
  }

  .send-btn {
    width: 120rpx;
    height: 70rpx;
    background: linear-gradient(135deg, #d4af37 0%, #aa8a2c 100%);
    border: none;
    border-radius: 8rpx;
    color: #000;
    font-size: 28rpx;

    &:disabled {
      opacity: 0.5;
    }
  }
}

.settings-section {
  padding: 40rpx;

  .section-title {
    font-size: 32rpx;
    color: #d4af37;
    margin-bottom: 30rpx;
    display: block;
  }

  .color-schemes {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;

    .scheme-item {
      width: 120rpx;
      height: 120rpx;
      border-radius: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid transparent;
      position: relative;

      &.scheme-active {
        border-color: #d4af37;
      }

      .scheme-name {
        font-size: 24rpx;
        color: #fff;
      }
    }
  }

  .font-size-control {
    display: flex;
    align-items: center;
    gap: 30rpx;

    .size-btn {
      width: 60rpx;
      height: 60rpx;
      background: rgba(212, 175, 55, 0.2);
      border: 1px solid rgba(212, 175, 55, 0.5);
      color: #d4af37;
      border-radius: 8rpx;
      font-size: 32rpx;
    }

    .size-display {
      font-size: 32rpx;
      color: #d4af37;
      min-width: 100rpx;
      text-align: center;
    }
  }
}

// 入定模式
.zen-mode {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(10, 10, 10, 0.95);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 300;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.5s;

  &.zen-active {
    opacity: 1;
    pointer-events: auto;
  }

  .zen-content {
    text-align: center;

    .zen-text {
      display: block;
      font-size: 60rpx;
      color: #d4af37;
      margin-bottom: 40rpx;
      letter-spacing: 8rpx;
    }

    .zen-subtitle {
      display: block;
      font-size: 32rpx;
      color: #888;
      letter-spacing: 4rpx;
    }
  }

  .zen-close {
    margin-top: 100rpx;
    padding: 20rpx 60rpx;
    background: rgba(212, 175, 55, 0.2);
    border: 1px solid rgba(212, 175, 55, 0.5);
    border-radius: 50rpx;
    color: #d4af37;
    font-size: 28rpx;
  }
}
</style>

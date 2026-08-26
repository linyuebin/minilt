<template>
  <view class="reading-container">
    <!-- 加载状态 -->
    <view v-if="isLoading" class="page-loading">
      <view class="loading-spinner"></view>
      <text class="loading-text">正在加载经文...</text>
    </view>

    <!-- 错误状态 -->
    <view v-else-if="loadError" class="page-error" @click="handleRetry">
      <view class="error-icon">
        <text class="material-symbols-outlined">error_outline</text>
      </view>
      <text class="error-text">{{ loadError }}</text>
      <text class="retry-text">点击屏幕重试</text>
    </view>

    <!-- 正常内容 -->
    <template v-else>
    <!-- 退出入定模式按钮 -->
    <view v-if="zenMode" class="meditation-exit" @click="toggleZenMode">
      <text>退出入定</text>
    </view>

    <!-- 顶部导航 -->
    <view class="header">
      <view class="header-title">
        <view class="logo">道德经</view>
      </view>
      <view class="header-icons">
        <view class="icon-btn" @click="toggleChapterDrawer" title="章节目录">
          <text class="material-symbols-outlined">menu</text>
        </view>
        <view class="icon-btn" @click="toggleZenMode" title="入定模式">
          <text class="material-symbols-outlined">self_improvement</text>
        </view>
        <view class="icon-btn" @click="navigateToProfile" title="我的">
          <text class="material-symbols-outlined">person</text>
        </view>
      </view>
    </view>

    <!-- 主要内容 -->
    <view class="main-content">
      <view class="chapter-container" id="chapterContainer">
        <view class="chapter-header">
          <view class="chapter-num">第{{ currentChapter.id }}章</view>
          <view class="chapter-divider"></view>
        </view>
        <view
          class="chapter-text"
          :class="isHorizontal ? 'chapter-content-horizontal' : 'chapter-content-vertical'"
        >
          <text class="text-line" v-for="(line, index) in displayLines" :key="index">
            {{ line }}
          </text>
        </view>
      </view>
    </view>

    <!-- 底部导航区 - 已迁移到自定义tabBar，保留隐藏以防兼容性问题 -->
    <view class="bottom-section" style="display: none;">
      <view class="nav-btn" @click="prevChapter">
        <text class="material-symbols-outlined">chevron_left</text>
      </view>
      <view class="center-actions">
        <view
          class="action-btn"
          :class="{ 'active': currentChapter.favorited }"
          @click="toggleFavorite"
        >
          <text class="material-symbols-outlined">favorite</text>
        </view>
        <view
          class="action-btn orientation"
          :class="{ 'active': !isHorizontal }"
          @click="toggleOrientation"
        >
          <text class="material-symbols-outlined">rotate_90_degrees_ccw</text>
        </view>
        <view class="action-btn" @click="toggleInterpretation">
          <text class="material-symbols-outlined">visibility</text>
        </view>
        <view class="action-btn" @click="toggleComments">
          <text class="material-symbols-outlined">chat_bubble</text>
        </view>
      </view>
      <view class="nav-btn" @click="nextChapter">
        <text class="material-symbols-outlined">chevron_right</text>
      </view>
    </view>

    <!-- 遮罩层 -->
    <view class="overlay" :class="{ 'active': showChapterDrawer || showInterpretation || showComments || showSettings }" @click="closeAllPanels"></view>

    <!-- 章节导航面板 -->
    <view class="nav-panel" :class="{ 'active': showChapterDrawer }">
      <view class="nav-header">
        <view class="nav-title">八十一章</view>
        <view class="icon-btn" @click="toggleChapterDrawer">
          <text class="material-symbols-outlined">close</text>
        </view>
      </view>
      <view class="nav-search">
        <input
          v-model="searchKeyword"
          class="nav-search-input"
          placeholder="搜索章节..."
          @input="handleSearch"
        />
      </view>
      <scroll-view class="nav-content" scroll-y>
        <view
          v-for="chapter in filteredChapters"
          :key="chapter.id"
          class="nav-chapter-item"
          :class="{ 'active': chapter.id === currentChapter.id }"
          @click="selectChapter(chapter)"
        >
          <view class="nav-chapter-num">{{ chapter.id }}</view>
          <view class="nav-chapter-title">{{ chapter.title }}</view>
        </view>
      </scroll-view>
      <view class="nav-footer">
        <view class="nav-progress">阅读进度：{{ currentChapter.id }}/81</view>
      </view>
    </view>

    <!-- 解读面板 -->
    <view class="interpretation-panel" :class="{ 'active': showInterpretation }">
      <view class="panel-handle" @click="toggleInterpretation"></view>
      <view class="interpretation-content">
        <view class="interpretation-label">经文释义</view>
        <view class="interpretation-text">
          {{ currentChapter.interpretation }}
        </view>
        <view class="comments-section">
          <view class="comments-header">
            <text class="icon" style="font-size: 18px;">forum</text>
            <text>悟道留言</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 评论面板 -->
    <view class="comments-panel" :class="{ 'active': showComments }">
      <view class="panel-handle" @click="toggleComments"></view>
      <view class="panel-header">
        <view class="panel-title">悟道留言</view>
        <view class="panel-close" @click="toggleComments">
          <text class="material-symbols-outlined">close</text>
        </view>
      </view>

      <!-- 评论列表 -->
      <scroll-view class="comments-list" scroll-y>
        <view v-if="comments.length === 0" class="no-comments">
          <text>空谷回音，虚位以待...</text>
        </view>
        <view
          v-for="comment in comments"
          :key="comment.id"
          class="comment-item"
        >
          <view class="comment-header">
            <view class="comment-avatar">{{ getAvatar(comment.nickname) }}</view>
            <view class="comment-user">{{ comment.nickname }}</view>
            <view class="comment-time">{{ shortTime(comment.createdAt) }}</view>
          </view>
          <view class="comment-text">{{ comment.content }}</view>
          <view class="comment-actions">
            <view
              class="comment-action-btn"
              :class="{ 'liked': comment.liked }"
              @click="handleCommentLike(comment)"
            >
              <text class="material-symbols-outlined">{{ comment.liked ? 'thumb_up' : 'thumb_up_off' }}</text>
              <text class="comment-likes">{{ comment.likeCount || 0 }}</text>
            </view>
            <view class="comment-action-btn" @click="startReply(comment)">
              <text class="material-symbols-outlined">reply</text>
              <text>回复</text>
            </view>
          </view>

          <!-- 嵌套回复 -->
          <view v-if="comment.replies && comment.replies.length" class="replies-section">
            <view v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <view class="reply-header">
                <view class="reply-avatar">{{ getAvatar(reply.nickname) }}</view>
                <view class="reply-user">
                  <text v-if="reply.replyToNickname" class="reply-to">回复</text>
                  {{ reply.nickname }}
                  <text v-if="reply.replyToNickname" class="reply-to">{{ reply.replyToNickname }}</text>
                </view>
                <view class="reply-time">{{ shortTime(reply.createdAt) }}</view>
              </view>
              <view class="reply-text">{{ reply.content }}</view>
            </view>
          </view>

          <view
            v-if="comment.replies && comment.replies.length"
            class="show-replies-btn"
            @click="toggleReplies(comment)"
          >
            <text class="icon" style="font-size: 14px;">expand_more</text>
            <text>{{ comment.replies.length }} 条回复</text>
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
          :placeholder="replyTarget ? `回复 ${replyTarget.nickname}...` : '写下你的感悟，结个善缘...'"
          class="comment-input"
        />
        <view class="send-btn" @click="sendComment">
          <text class="icon" style="font-size: 20px;">send</text>
        </view>
      </view>
    </view>

    <!-- 设置面板 -->
    <view class="settings-panel" :class="{ 'active': showSettings }">
      <view class="panel-header">
        <view class="panel-title">修行设置</view>
        <view class="panel-close" @click="toggleSettings">
          <text class="material-symbols-outlined">close</text>
        </view>
      </view>

      <!-- 入定模式快捷入口 -->
      <view
        class="upload-btn"
        @click="toggleZenMode(); closeAllPanels()"
        style="width: 100%; margin-bottom: 20px; padding: 15px; border: 1px solid rgba(212, 175, 55, 0.3); background: rgba(212, 175, 55, 0.05); color: #d4af37; border-radius: 12px; letter-spacing: 2px; text-align: center;"
      >
        <text class="material-symbols-outlined">self_improvement</text>
        <text>进入入定模式</text>
      </view>

      <!-- 配色方案 -->
      <view class="settings-section">
        <view class="section-title">配色方案</view>
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
        <view class="section-title">字体大小</view>
        <view class="font-size-control">
          <view class="size-btn" @click="decreaseFontSize">
            <text class="material-symbols-outlined">remove</text>
          </view>
          <text class="size-display">{{ fontSize }}</text>
          <view class="size-btn" @click="increaseFontSize">
            <text class="material-symbols-outlined">add</text>
          </view>
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
    </template>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getChapters, getChapterDetail, getChapterComments, createComment, toggleCommentLike, addFavorite, removeFavorite, updateProgress } from '@/api'

// 获取自定义 tabBar 实例（兼容H5和小程序）
const getTabBar = () => {
  try {
    // H5环境下使用不同的方式获取tabBar
    if (typeof uni.$getTabBar === 'function') {
      const tabBar = uni.$getTabBar()
      return tabBar?.children?.[0]?.component?.proxy
    }
    // H5环境备用方案：直接通过事件通信
    return null
  } catch (e) {
    console.log('获取tabBar实例失败:', e)
    return null
  }
}

// 通知 tabBar 更新状态（兼容H5和小程序）
const notifyTabBar = () => {
  try {
    // 优先尝试通过事件通知
    uni.$emit('readingPageUpdate', {
      favorited: currentChapter.value.favorited,
      horizontal: isHorizontal.value
    })

    // 如果有tabBar实例，直接调用方法
    const tabBar = getTabBar()
    if (tabBar && tabBar.updateReadingState) {
      tabBar.updateReadingState({
        favorited: currentChapter.value.favorited,
        horizontal: isHorizontal.value
      })
    }
  } catch (e) {
    console.log('通知tabBar失败（忽略）:', e)
  }
}

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

// 加载状态
const isLoading = ref(true)
const loadError = ref('')

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
    console.log('开始加载章节详情:', chapterId)
    const result = await getChapterDetail(chapterId)
    console.log('章节详情加载成功:', result)
    currentChapter.value = result

    // 更新阅读进度
    if (userStore.isLoggedIn()) {
      await updateProgress(chapterId)
      userStore.updateProgress(chapterId)
    }

    // 加载评论
    await loadComments(chapterId)

    // 通知 tabBar 更新状态
    notifyTabBar()
  } catch (error) {
    console.error('加载章节详情失败:', error)
    throw error // 让错误向上传播，这样可以被统一的错误处理捕获
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
  // 通知 tabBar 更新状态
  notifyTabBar()
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
    // 通知 tabBar 更新状态
    notifyTabBar()
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

// 关闭所有面板
const closeAllPanels = () => {
  showChapterDrawer.value = false
  showInterpretation.value = false
  showComments.value = false
  showSettings.value = false
}

// 跳转到个人中心
const navigateToProfile = () => {
  uni.navigateTo({
    url: '/pages/profile/index'
  })
}

// 获取头像字符（取昵称第一个字或随机字符）
const getAvatar = (nickname) => {
  if (!nickname) return '道'
  const chars = ['道', '德', '经', '无', '为', '悟', '玄']
  return nickname.charAt(0) || chars[Math.floor(Math.random() * chars.length)]
}

// 「2026-08-24 18:34」→「08-24 18:34」，面板窄，完整日期会被挤掉
const shortTime = (t) => (t || '').slice(5)

// 切换回复显示状态
const toggleReplies = (comment) => {
  if (!comment.showReplies) {
    comment.showReplies = true
  } else {
    comment.showReplies = false
  }
}

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
  try {
    console.log('阅读页面初始化开始')
    isLoading.value = true
    loadError.value = ''

    // 检查登录状态
    if (!userStore.isLoggedIn()) {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      })
      uni.reLaunch({
        url: '/pages/login/index'
      })
      isLoading.value = false
      return
    }

    // 恢复设置
    const savedOrientation = uni.getStorageSync('orientation')
    const savedFontSize = uni.getStorageSync('fontSize')
    const savedScheme = uni.getStorageSync('colorScheme')

    if (savedOrientation !== undefined) isHorizontal.value = savedOrientation
    if (savedFontSize) fontSize.value = savedFontSize
    if (savedScheme) currentScheme.value = savedScheme

    // 加载章节数据
    await loadChapters()

    // 获取页面参数，优先使用指定章节（兼容H5和小程序）
    let targetChapterId = userStore.userInfo?.lastChapterId || 1

    try {
      // uni-app全局函数，不需要从Vue导入
      const pages = getCurrentPages()
      if (pages && pages.length > 0) {
        const currentPage = pages[pages.length - 1]
        const options = currentPage?.options || {}
        if (options.chapterId) {
          targetChapterId = parseInt(options.chapterId) || 1
        }
      }
    } catch (e) {
      console.log('获取页面参数失败，使用默认章节:', e)
    }

    console.log('目标章节ID:', targetChapterId)
    await loadChapterDetail(targetChapterId)

    // 通知 tabBar 更新状态
    notifyTabBar()

    console.log('阅读页面初始化完成')
  } catch (error) {
    console.error('阅读页面初始化失败:', error)
    loadError.value = '连接服务器超时，请点击屏幕重试'
  } finally {
    isLoading.value = false
  }

  // 监听自定义 tabBar 的事件
  uni.$on('prevChapter', () => {
    prevChapter()
  })

  uni.$on('nextChapter', () => {
    nextChapter()
  })

  uni.$on('toggleFavorite', () => {
    toggleFavorite()
  })

  uni.$on('toggleOrientation', () => {
    toggleOrientation()
  })

  uni.$on('toggleInterpretation', () => {
    toggleInterpretation()
  })

  uni.$on('toggleComments', () => {
    toggleComments()
  })
})

// 重试加载
const handleRetry = async () => {
  try {
    isLoading.value = true
    loadError.value = ''

    let targetChapterId = userStore.userInfo?.lastChapterId || 1

    try {
      const pages = getCurrentPages()
      if (pages && pages.length > 0) {
        const currentPage = pages[pages.length - 1]
        const options = currentPage?.options || {}
        if (options.chapterId) {
          targetChapterId = parseInt(options.chapterId) || 1
        }
      }
    } catch (e) {
      console.log('获取页面参数失败，使用默认章节:', e)
    }

    await loadChapterDetail(targetChapterId)
  } catch (error) {
    console.error('重试加载失败:', error)
    loadError.value = '连接服务器超时，请点击屏幕重试'
  } finally {
    isLoading.value = false
  }
}

// 监听页面卸载，移除事件监听
onUnmounted(() => {
  try {
    uni.$off('prevChapter')
    uni.$off('nextChapter')
    uni.$off('toggleFavorite')
    uni.$off('toggleOrientation')
    uni.$off('toggleInterpretation')
    uni.$off('toggleComments')
  } catch (e) {
    console.log('移除事件监听失败:', e)
  }
})
</script>

<style lang="scss" scoped>
.reading-container {
  min-height: 100vh;
  background: radial-gradient(ellipse at center, rgb(40,40,40) 0%, #0e0e0e 100%);
  color: #e2e2e2;
  font-family: "KaiTi", "STKaiti", "KaiTi_GB2312", "Songti SC", serif;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-loading,
.page-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  text-align: center;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(ellipse at center, rgb(40,40,40) 0%, #0e0e0e 100%);
  z-index: 1000;
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

// 顶部导航
.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 35px 25px 20px;
  transition: all 0.6s ease;
  z-index: 10;
  flex-shrink: 0;
}

.header-title {
  flex: 1;
}

.logo {
  font-size: 24px;
  letter-spacing: 10px;
  font-weight: 300;
  color: rgba(248, 246, 242, 0.9);
}

.header-icons {
  display: flex;
  gap: 10px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.08);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255,255,255,0.03);
  color: rgba(248, 246, 242, 0.8);
}

.icon-btn:active {
  transform: scale(0.9);
}

// 主要内容区域
.main-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  transition: all 0.6s ease;
  padding: 20px 40px;
  z-index: 10;
  position: relative;
}

.chapter-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 1;
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
  text-align: center;
  position: relative;
  z-index: 10;
  max-width: 100%;
  max-height: 100%;
  overflow: auto;
}

// 章节标题
.chapter-header {
  text-align: center;
  margin-bottom: 15px;
  opacity: 0;
  animation: fade-in 1.5s ease-out 0.3s forwards;
}

.chapter-num {
  font-size: 11px;
  color: rgba(212, 175, 55, 0.5);
  letter-spacing: 3px;
  margin-bottom: 20px;
}

.chapter-divider {
  width: 35px;
  height: 1px;
  background: rgba(212, 175, 55, 0.25);
  margin: 0 auto;
}

// 横向文字样式
.chapter-content-horizontal {
  font-size: 1.25rem;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.95);
  text-align: center;
  letter-spacing: 0.25em;
  opacity: 0;
  animation: fade-in 2s ease-out 1s forwards;
  padding: 0 2rem;
  max-width: 90vw;
}

// 竖向文字样式
.chapter-content-vertical {
  font-size: 1.4rem;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.95);
  text-align: center;
  letter-spacing: 0.35em;
  opacity: 0;
  animation: fade-in 2s ease-out 1s forwards;
  writing-mode: vertical-rl;
  text-orientation: upright;
  height: 55vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1.25em;
  padding: 2rem 0;
}

// 底部导航 - 已迁移到自定义tabBar
.bottom-section {
  display: none !important;
}

.nav-btn, .action-btn {
  width: 48px;
  height: 48px;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 50%;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  background: rgba(255,255,255,0.03);
  color: rgba(232, 228, 220, 0.8);
  position: relative;
  overflow: hidden;
}

.nav-btn:active, .action-btn:active {
  transform: scale(0.9);
}

.center-actions {
  display: flex;
  gap: 15px;
}

.action-btn.active {
  color: #ff6b6b;
  border-color: rgba(255, 107, 107, 0.3);
  box-shadow: 0 0 15px rgba(255, 107, 107, 0.2);
}

.action-btn.orientation.active {
  color: #e2e2e2;
  border-color: rgba(226, 226, 226, 0.3);
  box-shadow: 0 0 15px rgba(226, 226, 226, 0.2);
}

// 章节导航面板
.nav-panel {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 320px;
  max-width: 85vw;
  z-index: 200;
  transform: translateX(-100%);
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: rgba(14, 14, 14, 0.95);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-right: 1px solid rgba(255,255,255,0.06);
}

.nav-panel.active {
  transform: translateX(0);
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30px 20px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.nav-title {
  font-size: 18px;
  font-weight: 500;
  color: #d4af37;
  letter-spacing: 2px;
}

.nav-search {
  padding: 15px 20px;
  flex-shrink: 0;
}

.nav-search-input {
  width: 100%;
  padding: 10px 16px;
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
  color: #e2e2e2;
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
}

.nav-search-input:focus {
  border-color: rgba(212, 175, 55, 0.3);
  background: rgba(255,255,255,0.05);
}

.nav-search-input::placeholder {
  color: rgba(255,255,255,0.3);
}

.nav-content {
  flex: 1;
  min-height: 0;
  padding: 0 20px 20px;
  overflow-y: auto;
}

.nav-chapter-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-radius: 8px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.nav-chapter-item:active {
  background: rgba(255,255,255,0.03);
}

.nav-chapter-item.active {
  background: rgba(212, 175, 55, 0.1);
  border-color: rgba(212, 175, 55, 0.2);
}

.nav-chapter-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(212, 175, 55, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d4af37;
  font-size: 12px;
  flex-shrink: 0;
}

.nav-chapter-item.active .nav-chapter-num {
  background: rgba(212, 175, 55, 0.2);
}

.nav-chapter-title {
  flex: 1;
  font-size: 14px;
  color: rgba(248, 246, 242, 0.8);
  letter-spacing: 1px;
}

.nav-chapter-item.active .nav-chapter-title {
  color: #d4af37;
}

.nav-footer {
  padding: 15px 20px 20px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
  text-align: center;
}

.nav-progress {
  font-size: 12px;
  color: rgba(255,255,255,0.4);
  letter-spacing: 1px;
}

// 解读面板
.interpretation-panel {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-height: 80vh;
  border-radius: 24px 24px 0 0;
  z-index: 200;
  transform: translateY(100%);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  background: rgba(18, 18, 18, 0.85);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-top: 1px solid rgba(255,255,255,0.06);
}

.interpretation-panel.active {
  transform: translateY(0);
}

.panel-handle {
  width: 40px;
  height: 4px;
  background: rgba(255,255,255,0.2);
  border-radius: 2px;
  margin: 15px auto;
}

.interpretation-content {
  padding: 0 25px 30px;
  max-height: 70vh;
  overflow-y: auto;
}

.interpretation-label {
  font-size: 12px;
  letter-spacing: 4px;
  margin-bottom: 20px;
  text-align: center;
  color: #d4af37;
  opacity: 0.8;
}

.interpretation-text {
  font-size: 15px;
  line-height: 2.2;
  margin-bottom: 30px;
  color: #e8e4dc;
  text-align: justify;
}

// 评论面板
.comments-panel {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-height: 80vh;
  border-radius: 24px 24px 0 0;
  z-index: 200;
  transform: translateY(100%);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  background: rgba(18, 18, 18, 0.85);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-top: 1px solid rgba(255,255,255,0.06);
}

.comments-panel.active {
  transform: translateY(0);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 25px 25px 15px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #f8f6f2;
}

.panel-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.1);
  font-size: 14px;
  cursor: pointer;
  background: rgba(255,255,255,0.05);
  color: #f8f6f2;
}

// 评论区
.comments-header {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 20px;
  color: #f8f6f2;
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-item {
  padding: 16px;
  border-radius: 16px;
  margin-bottom: 12px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.05);
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a2a3a, #2a1a2a);
  border: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d4af37;
  font-size: 14px;
}

.comment-user {
  font-size: 14px;
  font-weight: 500;
  color: #e8e4dc;
}

.comment-time {
  font-size: 12px;
  margin-left: auto;
  color: rgba(255,255,255,0.4);
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: rgba(255,255,255,0.8);
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: rgba(255,255,255,0.5);
  transition: all 0.2s;
  padding: 0;
}

.comment-action-btn:active {
  color: rgba(255,255,255,0.7);
}

.comment-action-btn.liked {
  color: #d4af37;
}

.comment-likes {
  font-size: 12px;
}

// 回复样式
.replies-section {
  margin-top: 12px;
  padding-left: 12px;
  border-left: 2px solid rgba(255,255,255,0.08);
}

.reply-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  background: rgba(255,255,255,0.02);
  border-radius: 8px;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.reply-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a2a3a, #2a1a2a);
  border: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d4af37;
  font-size: 10px;
}

.reply-user {
  font-size: 12px;
  font-weight: 500;
  color: #e8e4dc;
}

.reply-time {
  font-size: 11px;
  margin-left: auto;
  color: rgba(255,255,255,0.3);
}

.reply-text {
  font-size: 13px;
  line-height: 1.5;
  color: rgba(255,255,255,0.7);
}

.reply-to {
  color: rgba(212, 175, 55, 0.7);
  font-weight: 500;
}

.reply-input-area {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.reply-input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
  font-size: 12px;
  outline: none;
  color: #e2e2e2;
}

.reply-input::placeholder {
  color: rgba(255,255,255,0.25);
}

.reply-send-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  color: #d4af37;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.reply-send-btn:active {
  background: rgba(212,175,55,0.15);
}

.show-replies-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: rgba(212, 175, 55, 0.7);
  margin-top: 8px;
  padding: 0;
}

.show-replies-btn:active {
  color: #d4af37;
}

.comment-input-area {
  position: sticky;
  bottom: 0;
  padding: 15px 0 5px;
  display: flex;
  gap: 12px;
  background: linear-gradient(0deg, rgba(15,20,25,1) 60%, rgba(15,20,25,0));
}

.comment-input {
  flex: 1;
  padding: 12px 20px;
  border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
  font-size: 14px;
  outline: none;
  color: #e2e2e2;
}

.comment-input::placeholder {
  color: rgba(255,255,255,0.25);
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  color: #d4af37;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:active {
  transform: scale(0.9);
  background: rgba(212,175,55,0.2);
}

// 设置面板
.settings-panel {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  border-radius: 24px 24px 0 0;
  padding: 25px 20px 35px;
  z-index: 200;
  transform: translateY(100%);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(18, 18, 18, 0.85);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-top: 1px solid rgba(255,255,255,0.06);
}

.settings-panel.active {
  transform: translateY(0);
}

.settings-section {
  padding: 16px;
  background: rgba(255,255,255,0.05);
  border-radius: 16px;
  margin-bottom: 15px;
}

.section-title {
  font-size: 13px;
  color: rgba(255,255,255,0.6);
  margin-bottom: 10px;
}

.color-schemes {
  display: flex;
  gap: 12px;
}

.scheme-item {
  flex: 1;
  aspect-ratio: 1;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  border: 2px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.scheme-item:active {
  transform: scale(0.95);
}

.scheme-item.scheme-active {
  border-color: #d4af37;
  box-shadow: 0 0 15px rgba(212, 175, 55, 0.3);
}

.scheme-name {
  font-size: 12px;
  color: #fff;
}

// 入定模式
.zen-mode {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(10, 10, 10, 0.98);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 300;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.5s;
}

.zen-mode.zen-active {
  opacity: 1;
  pointer-events: auto;
}

.zen-content {
  text-align: center;
}

.zen-text {
  display: block;
  font-size: 48rpx;
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

.zen-close {
  margin-top: 100rpx;
  padding: 20rpx 60rpx;
  background: rgba(212, 175, 55, 0.2);
  border: 1px solid rgba(212, 175, 55, 0.5);
  border-radius: 50rpx;
  color: #d4af37;
  font-size: 28rpx;
}

// 入定模式下隐藏工具栏
.reading-container:has(.zen-mode.zen-active) {
  .header,
  .bottom-section {
    opacity: 0;
    pointer-events: none;
  }
}

// 翻页控制
.pagination-controls {
  display: none; // 使用底部按钮替代
}
</style>

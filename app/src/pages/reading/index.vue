<template>
  <view
    class="reading-container"
    :class="[theme.themeClass, { 'is-zen': zenMode }]"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <!-- 自定义背景层 -->
    <CustomBackground />

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
      <!-- 顶部导航：返回 + 第X章 -->
      <view class="header">
        <view class="header-side">
          <view class="icon-btn" @click.stop="goBackHome" title="返回首页">
            <text class="material-symbols-outlined">arrow_back</text>
          </view>
        </view>
        <view class="header-center">
          <view class="chapter-num">第{{ currentChapter.id }}章</view>
          <view class="chapter-divider"></view>
        </view>
        <view class="header-side header-side-right">
          <!-- 右侧占位，保持标题居中 -->
        </view>
      </view>

      <!-- 主要内容：屏幕中间显示经文 -->
      <view class="main-content">
        <view class="chapter-container" id="chapterContainer">
          <view
            class="chapter-text"
            :class="isHorizontal ? 'chapter-content-horizontal' : 'chapter-content-vertical'"
            :style="{ fontSize: fontSize + 'px' }"
          >
            <text class="text-line" v-for="(line, index) in displayLines" :key="index">
              {{ line }}
            </text>
          </view>
        </view>

        <!-- 覆盖翻页滑层：forward 携带新章自右滑入覆盖，back 携带旧章向右滑出露出下层 -->
        <CoverPage
          v-if="flipStage === 'out'"
          :dir="flipDir"
          :lines="flipLines"
          :font-size="fontSize"
          :is-horizontal="isHorizontal"
          :zen="zenMode"
        />

        <!-- 仿真翻页：左右点击热区（透明层，仅承担点击翻页） -->
        <view v-if="showTapZones" class="tap-zone tap-zone-left" @click.stop="prevChapter"></view>
        <view v-if="showTapZones" class="tap-zone tap-zone-right" @click.stop="nextChapter"></view>
      </view>

      <!-- 入定模式：底部上划提示（外层全宽容器 flex 居中，胶囊不做 transform 定位，避免动画/端差异导致水平偏移） -->
      <view v-if="zenMode" class="zen-hint-wrap">
        <view class="zen-swipe-hint" @click.stop="exitZenMode">
          <text class="material-symbols-outlined zen-hint-icon">keyboard_double_arrow_up</text>
          <text class="zen-hint-text">上划退出入定</text>
        </view>
      </view>

      <!-- 底部工具栏：目录 / 横竖排 / 释义 / 修行设置，四键均匀分布，入定模式下收起 -->
      <view class="bottom-toolbar" :class="{ 'toolbar-hidden': zenMode }">
        <view class="action-btn" @click.stop="toggleChapterDrawer">
          <text class="material-symbols-outlined">menu</text>
        </view>
        <view
          class="action-btn"
          :class="{ 'active': !isHorizontal }"
          @click.stop="toggleOrientation"
        >
          <text class="material-symbols-outlined">rotate_90_degrees_ccw</text>
        </view>
        <view class="action-btn" @click.stop="toggleInterpretation">
          <text class="material-symbols-outlined">visibility</text>
        </view>
        <view class="action-btn" @click.stop="toggleSettings">
          <text class="material-symbols-outlined">settings</text>
        </view>
      </view>

    <!-- 遮罩层 -->
    <view class="overlay" :class="{ 'active': showChapterDrawer || showInterpretation }" @click="closeAllPanels"></view>

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

    <!-- 解读面板：释义 + 悟道留言同面板滚动（参考原型交互） -->
    <view class="interpretation-panel" :class="{ 'active': showInterpretation }">
      <view class="panel-handle" @click="toggleInterpretation"></view>

      <scroll-view class="panel-scroll" scroll-y>
        <view class="panel-inner">
          <view class="interpretation-label">经文释义</view>
          <view class="interpretation-text">
            {{ currentChapter.interpretation }}
          </view>

          <!-- 悟道留言区 -->
          <view class="comments-section">
            <view class="comments-header">
              <text class="material-symbols-outlined comments-icon">forum</text>
              <text>悟道留言</text>
            </view>

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
                  <text class="material-symbols-outlined">
                    {{ comment.liked ? 'thumb_up' : 'thumb_up_off_alt' }}
                  </text>
                  <text class="comment-likes">{{ comment.likeCount || 0 }}</text>
                </view>
                <view class="comment-action-btn" @click="toggleReplyInput(comment)">
                  <text class="material-symbols-outlined">reply</text>
                  <text>回复</text>
                </view>
              </view>

              <!-- 行内回复输入框：点击"回复"在该条留言下展开 -->
              <view v-if="comment.showReplyInput" class="reply-input-area">
                <input
                  v-model="comment.replyDraft"
                  :placeholder="'回复 ' + comment.nickname + '...'"
                  class="reply-input"
                  confirm-type="send"
                  @confirm="submitReply(comment)"
                />
                <view class="reply-send-btn" @click="submitReply(comment)">
                  <text class="material-symbols-outlined reply-send-icon">send</text>
                </view>
              </view>

              <!-- 回复折叠展示："N 条回复"点击展开/收起 -->
              <template v-if="comment.replies && comment.replies.length">
                <view class="show-replies-btn" @click="toggleReplies(comment)">
                  <text
                    class="material-symbols-outlined replies-icon"
                    :class="{ 'replies-expanded': comment.showReplies }"
                  >expand_more</text>
                  <text>{{ comment.replies.length }} 条回复</text>
                </view>
                <view v-if="comment.showReplies" class="replies-section">
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
              </template>
            </view>
          </view>
        </view>
      </scroll-view>

      <!-- 底部固定发表输入框 -->
      <view class="comment-input-section">
        <input
          v-model="newComment"
          placeholder="写下你的感悟，结个善缘..."
          class="comment-input"
        />
        <view class="send-btn" @click="sendComment">
          <text class="material-symbols-outlined send-icon">send</text>
        </view>
      </view>
    </view>

    <!-- 修行设置面板（共享组件 + 阅读专属的字体大小区块） -->
    <ThemePanel v-model:show="showSettings" @reset="resetFontSize">
      <template #extra>
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

        <view class="section-title page-mode-title">翻页方式</view>
        <view class="page-mode-control">
          <view
            class="mode-btn"
            :class="{ 'mode-active': pageMode === 'swipe' }"
            @click="setPageMode('swipe')"
          >左右滑动</view>
          <view
            class="mode-btn"
            :class="{ 'mode-active': pageMode === 'cover' }"
            @click="setPageMode('cover')"
          >覆盖翻页</view>
        </view>
      </template>
    </ThemePanel>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import ThemePanel from '@/components/ThemePanel.vue'
import CustomBackground from '@/components/CustomBackground.vue'
import CoverPage from '@/components/CoverPage.vue'
import { getChapters, getChapterDetail, getChapterComments, createComment, toggleCommentLike, updateProgress } from '@/api'

const userStore = useUserStore()
const theme = useThemeStore()

// 章节相关
const currentChapter = ref({
  id: 1,
  title: '道可道',
  content: '',
  interpretation: '',
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
const zenMode = ref(true)

// 阅读设置
const isHorizontal = ref(true)
// 字体大小（px）：与经文原 1.25rem 基准一致，范围 14-30；旧版遗留的越界值会在恢复时归位
const FONT_MIN = 14
const FONT_MAX = 30
const FONT_DEFAULT = 20
const fontSize = ref(FONT_DEFAULT)

// 翻页方式：'swipe' 左右滑动（默认）| 'cover' 覆盖翻页（点击热区 + 滑层覆盖动画）
const pageMode = ref('swipe')
// 覆盖翻页动画状态：flipStage '' | 'out'（滑层移动中）；flipDir 'forward' | 'back'
const flipStage = ref('')
const flipDir = ref('')
const isFlipping = computed(() => flipStage.value !== '')
// 滑层上的旧章快照 / 待露出（或待覆盖）的新章
const flippingChapter = ref(null)
const pendingChapter = ref(null)

// 评论相关
const comments = ref([])
const newComment = ref('')

// 无操作自动入定间隔（毫秒）
const ZEN_IDLE_MS = 5000
let zenIdleTimer = null

// 计算属性
const filteredChapters = computed(() => {
  if (!searchKeyword.value) return allChapters.value
  const keyword = searchKeyword.value.toLowerCase()
  return allChapters.value.filter(chapter =>
    chapter.title.toLowerCase().includes(keyword) ||
    chapter.id.toString().includes(keyword)
  )
})

// 翻页进行中：back 时旧章滑出，下层提前切换为已就绪的新章；
// forward 时下层保持当前章，等新章滑入覆盖
const displayChapter = computed(() =>
  flipStage.value === 'out' && flipDir.value === 'back' && pendingChapter.value
    ? pendingChapter.value
    : currentChapter.value
)

const displayLines = computed(() => {
  if (!displayChapter.value.content) return []
  return displayChapter.value.content.split('。').filter(line => line.trim())
})

// 覆盖滑层携带的内容：forward 新章滑入覆盖，back 旧章滑出
const flipLines = computed(() => {
  const chapter = flipDir.value === 'forward' ? pendingChapter.value : flippingChapter.value
  if (!chapter || !chapter.content) return []
  return chapter.content.split('。').filter(line => line.trim())
})

const anyPanelOpen = computed(() =>
  showChapterDrawer.value || showInterpretation.value || showSettings.value
)

const hasPrevChapter = computed(() => currentChapter.value.id > 1)
const hasNextChapter = computed(() => currentChapter.value.id < 81)

// 点击热区仅在覆盖翻页、非入定、无面板弹层时启用
const showTapZones = computed(() =>
  pageMode.value === 'cover' && !zenMode.value && !anyPanelOpen.value
)

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

// 上一章 / 下一章（左右滑手势与点击热区统一入口；仿真模式播放翻页动画）
const prevChapter = () => {
  if (!hasPrevChapter.value) return
  flipToChapter(currentChapter.value.id - 1, 'back')
}

const nextChapter = () => {
  if (!hasNextChapter.value) return
  flipToChapter(currentChapter.value.id + 1, 'forward')
}

// 覆盖翻页动画时长（毫秒）：滑层动画由 CoverPage 内的纯 CSS 关键帧播放，
// JS 只需等动画播完再收尾，不逐帧参与
const FLIP_MS = 500
let flipTimer = null

// 按翻页方式切换章节：滑动模式直接加载；覆盖模式滑层移入/移出（forward 滑入覆盖 / back 滑出）
const flipToChapter = async (chapterId, dir) => {
  if (isFlipping.value) return
  if (pageMode.value !== 'cover') {
    await loadChapterDetail(chapterId)
    return
  }
  flippingChapter.value = { ...currentChapter.value }
  flipDir.value = dir
  flipStage.value = 'out'
  try {
    // 数据加载与纸张卷曲动画并行；forward 时新章就绪即铺到底层，back 时渲染在铺回的纸面上
    const [detail] = await Promise.all([
      getChapterDetail(chapterId).then((result) => {
        pendingChapter.value = result
        return result
      }),
      new Promise((resolve) => { flipTimer = setTimeout(resolve, FLIP_MS) })
    ])
    // 纸页落定、新章就绪：提交新章并收尾
    currentChapter.value = detail
    if (userStore.isLoggedIn()) {
      await updateProgress(chapterId)
      userStore.updateProgress(chapterId)
    }
    flippingChapter.value = null
    pendingChapter.value = null
    flipStage.value = ''
    // 评论不阻塞翻页收尾
    loadComments(chapterId)
  } catch (error) {
    // 失败复位动画状态，保留旧章内容
    flippingChapter.value = null
    pendingChapter.value = null
    flipStage.value = ''
    flipTimer = null
    throw error
  }
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

// 入定模式
const enterZenMode = () => {
  zenMode.value = true
  stopZenIdleTimer()
}

const exitZenMode = () => {
  if (!zenMode.value) return
  zenMode.value = false
  startZenIdleTimer()
}

// 无操作计时器：正常态下闲置 ZEN_IDLE_MS 后自动回到入定模式
const stopZenIdleTimer = () => {
  if (zenIdleTimer) {
    clearTimeout(zenIdleTimer)
    zenIdleTimer = null
  }
}

const startZenIdleTimer = () => {
  stopZenIdleTimer()
  if (zenMode.value || anyPanelOpen.value || isLoading.value || loadError.value) return
  zenIdleTimer = setTimeout(() => {
    zenIdleTimer = null
    enterZenMode()
  }, ZEN_IDLE_MS)
}

// 任一面板打开期间暂停倒计时，全部关闭后重新计时
watch(anyPanelOpen, (open) => {
  if (open) {
    stopZenIdleTimer()
  } else {
    startZenIdleTimer()
  }
})

// 触摸手势：上划退出入定，左右滑切换章节
let touchStartX = 0
let touchStartY = 0
let gestureReady = false

const handleTouchStart = (e) => {
  gestureReady = false
  if (isLoading.value || loadError.value || anyPanelOpen.value) return

  // 正常态：任何触摸都视为活跃，重置自动入定倒计时
  if (!zenMode.value) {
    startZenIdleTimer()
  }

  const touch = e.touches && e.touches[0]
  if (!touch) return
  touchStartX = touch.clientX
  touchStartY = touch.clientY
  gestureReady = true
}

const handleTouchEnd = (e) => {
  if (!gestureReady) return
  gestureReady = false

  const touch = e.changedTouches && e.changedTouches[0]
  if (!touch) return

  const dx = touch.clientX - touchStartX
  const dy = touch.clientY - touchStartY
  const absX = Math.abs(dx)
  const absY = Math.abs(dy)

  // 纵向手势：上划退出入定
  if (absY > 40 && absY > absX * 1.5) {
    if (dy < 0 && zenMode.value) {
      exitZenMode()
    }
    return
  }

  // 横向手势：右滑上一章，左滑下一章
  if (absX > 60 && absX > absY * 1.5) {
    if (dx < 0) {
      nextChapter()
    } else {
      prevChapter()
    }
  }
}

// 横竖排切换
const toggleOrientation = () => {
  isHorizontal.value = !isHorizontal.value
  // 保存设置
  uni.setStorageSync('orientation', isHorizontal.value)
}

// 字体大小控制
const increaseFontSize = () => {
  if (fontSize.value < FONT_MAX) {
    fontSize.value += 2
    uni.setStorageSync('fontSize', fontSize.value)
  }
}

const decreaseFontSize = () => {
  if (fontSize.value > FONT_MIN) {
    fontSize.value -= 2
    uni.setStorageSync('fontSize', fontSize.value)
  }
}

// 重置字体大小（修行设置面板"重置为默认设置"时触发）
const resetFontSize = () => {
  fontSize.value = FONT_DEFAULT
  uni.setStorageSync('fontSize', fontSize.value)
}

// 切换翻页方式并持久化（取值：'swipe' 左右滑动 | 'cover' 覆盖翻页）
const setPageMode = (mode) => {
  pageMode.value = mode
  uni.setStorageSync('pageMode', mode)
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑在computed中处理
}

// 关闭所有面板
const closeAllPanels = () => {
  showChapterDrawer.value = false
  showInterpretation.value = false
  showSettings.value = false
}

// 返回首页
const goBackHome = () => {
  const pages = getCurrentPages()
  if (pages && pages.length > 1) {
    uni.navigateBack({})
  } else {
    // 直接打开本页（无返回栈）时兜底回阅读首页
    uni.switchTab({ url: '/pages/reading/home' })
  }
}

// 获取头像字符（取昵称第一个字或随机字符）
const getAvatar = (nickname) => {
  if (!nickname) return '道'
  const chars = ['道', '德', '经', '无为', '悟', '玄']
  return nickname.charAt(0) || chars[Math.floor(Math.random() * chars.length)]
}

const shortTime = (t) => (t || '').slice(5)

// 展开/收起回复列表
const toggleReplies = (comment) => {
  comment.showReplies = !comment.showReplies
}

// 展开/收起行内回复输入框（同时只展开一条）
const toggleReplyInput = (comment) => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  const opening = !comment.showReplyInput
  comments.value.forEach(c => {
    c.showReplyInput = false
  })
  if (opening) {
    comment.replyDraft = ''
    comment.showReplyInput = true
    // 展开输入框时顺带展示该条的回复列表，方便对照上下文
    if (comment.replies && comment.replies.length) {
      comment.showReplies = true
    }
  } else {
    comment.replyDraft = ''
  }
}

// 提交行内回复
const submitReply = async (comment) => {
  const content = (comment.replyDraft || '').trim()
  if (!content) return

  try {
    await createComment({
      chapterId: currentChapter.value.id,
      content,
      parentId: comment.id,
      replyToUserId: comment.userId
    })

    comment.replyDraft = ''
    comment.showReplyInput = false
    uni.showToast({
      title: '回复成功',
      icon: 'success'
    })
    // 重新加载评论，并保持该条回复列表展开
    await loadComments(currentChapter.value.id)
    const target = comments.value.find(c => c.id === comment.id)
    if (target) target.showReplies = true
  } catch (error) {
    console.error('回复失败:', error)
  }
}

// 发表留言（面板底部固定输入框）
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
    await createComment({
      chapterId: currentChapter.value.id,
      content: newComment.value.trim()
    })

    newComment.value = ''
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

// 初始化
onMounted(async () => {
  try {
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

    // 恢复设置（配色方案与自定义背景由主题 store 在应用启动时恢复）
    const savedOrientation = uni.getStorageSync('orientation')
    const savedFontSize = Number(uni.getStorageSync('fontSize'))

    if (savedOrientation !== undefined) isHorizontal.value = savedOrientation
    if (savedFontSize >= FONT_MIN && savedFontSize <= FONT_MAX && savedFontSize % 2 === 0) {
      fontSize.value = savedFontSize
    } else {
      uni.setStorageSync('fontSize', fontSize.value)
    }

    // 恢复翻页方式（旧版存过的 'curl' 一律映射为现在的 'cover'，其余非法值回落默认）
    const savedPageMode = uni.getStorageSync('pageMode')
    if (savedPageMode === 'swipe' || savedPageMode === 'cover') pageMode.value = savedPageMode
    else if (savedPageMode === 'curl') pageMode.value = 'cover'

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

    await loadChapterDetail(targetChapterId)
  } catch (error) {
    console.error('阅读页面初始化失败:', error)
    loadError.value = '连接服务器超时，请点击屏幕重试'
  } finally {
    isLoading.value = false
    // 页面默认进入入定模式；若仍在正常态则启动自动入定倒计时
    startZenIdleTimer()
  }

  // 监听底部工具栏的章节目录事件（预留外部触发通道）
  uni.$on('toggleChapterDrawer', () => {
    toggleChapterDrawer()
  })

  uni.$on('toggleOrientation', () => {
    toggleOrientation()
  })

  uni.$on('toggleInterpretation', () => {
    toggleInterpretation()
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
    startZenIdleTimer()
  }
}

// 监听页面卸载，移除事件监听
onUnmounted(() => {
  stopZenIdleTimer()
  if (flipTimer) {
    clearTimeout(flipTimer)
    flipTimer = null
  }
  try {
    uni.$off('toggleChapterDrawer')
    uni.$off('toggleOrientation')
    uni.$off('toggleInterpretation')
  } catch (e) {
    console.log('移除事件监听失败:', e)
  }
})
</script>

<style lang="scss" scoped>
.reading-container {
  min-height: 100vh;
  background: var(--page-bg);
  color: var(--text-1);
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
  background: var(--page-bg);
  z-index: 1000;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid var(--accent-15);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 16px;
  color: var(--text-2);
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
  color: var(--text-2);
  margin-bottom: 10px;
  letter-spacing: 1px;
}

.retry-text {
  font-size: 14px;
  color: var(--accent-strong);
  letter-spacing: 1px;
}

// 顶部导航：返回 + 第X章
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: calc(35px + env(safe-area-inset-top)) 25px 12px;
  transition: all 0.45s ease;
  z-index: 10;
  flex-shrink: 0;
}

.header-side {
  width: 34px;
  display: flex;
  justify-content: flex-start;
}

.header-side-right {
  justify-content: flex-end;
}

.header-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--border);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--surface-1);
  color: var(--text-2);
}

.icon-btn:active {
  transform: scale(0.9);
}

.icon-btn .material-symbols-outlined {
  font-size: 20px;
}

.chapter-num {
  font-size: 13px;
  color: var(--accent-strong);
  letter-spacing: 3px;
  white-space: nowrap;
}

.chapter-divider {
  width: 35px;
  height: 1px;
  background: var(--accent-30);
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
  padding: 10px 40px 70px;
  z-index: 10;
  position: relative;
  /* 覆盖翻页为纯 2D 平移，无需 3D 透视 */
}

.chapter-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 1;
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  position: relative;
  z-index: 10;
  max-width: 100%;
  max-height: 100%;
  overflow: auto;
}

// 横向文字样式
.chapter-content-horizontal {
  font-size: 1.25rem;
  line-height: 1.5;
  color: var(--text-bright);
  text-align: center;
  letter-spacing: 0.25em;
  animation: fade-in 2s ease-out forwards;
  padding: 0 2rem;
  max-width: 90vw;
}

// 竖向文字样式
// 列间距仅由 line-height 控制：1.4rem × 1.45 列宽，列间留白 ≈ 0.63rem，
// 与横排行距留白（1.25rem × 1.5 行距 − 1.25rem 字高 ≈ 0.63rem）一致
.chapter-content-vertical {
  font-size: 1.4rem;
  line-height: 1.45;
  color: var(--text-bright);
  text-align: center;
  letter-spacing: 0.35em;
  animation: fade-in 2s ease-out forwards;
  writing-mode: vertical-rl;
  text-orientation: upright;
  height: 55vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 2rem 0;
}

// 入定模式：底部上划提示（全宽容器 flex 居中，水平居中由布局保证，不依赖 transform）
.zen-hint-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(28px + env(safe-area-inset-bottom));
  display: flex;
  justify-content: center;
  pointer-events: none;
  z-index: 60;
}

.zen-swipe-hint {
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 18px 6px;
  border-radius: 24px;
  background: var(--accent-05);
  border: 1px solid var(--accent-15);
  cursor: pointer;
  animation: hint-float 3s ease-in-out infinite;
  transition: opacity 0.3s ease;
}

.zen-swipe-hint:active {
  opacity: 0.6;
}

.zen-hint-icon {
  font-size: 18px;
  color: var(--accent-strong);
}

.zen-hint-text {
  font-size: 11px;
  color: var(--accent-strong);
  letter-spacing: 2px;
  white-space: nowrap;
}

@keyframes hint-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

// 底部工具栏：目录 / 释义 / 横竖排 / 修行设置，四键 space-between 均匀分布
.bottom-toolbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 28px calc(20px + env(safe-area-inset-bottom));
  min-height: 70px;
  background: var(--toolbar-grad);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  z-index: 50;
  transition: opacity 0.45s ease, transform 0.45s ease;
}

.toolbar-hidden {
  opacity: 0;
  transform: translateY(100%);
  pointer-events: none;
}

.action-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  background: var(--surface-1);
  color: var(--text-2);
}

.action-btn:active {
  transform: scale(0.9);
}

.action-btn.active {
  color: var(--accent);
  border-color: var(--accent-30);
}

.action-btn .material-symbols-outlined {
  font-size: 22px;
}

// 入定模式下隐藏头部与工具栏
.reading-container.is-zen {
  .header {
    opacity: 0;
    pointer-events: none;
    transform: translateY(-15px);
  }

  .main-content {
    padding-bottom: 40px;
  }
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
  background: var(--panel-bg-solid);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-right: 1px solid var(--surface-2);
}

.nav-panel.active {
  transform: translateX(0);
}

.nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30px 20px 20px;
  border-bottom: 1px solid var(--surface-2);
  flex-shrink: 0;
}

.nav-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--accent);
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
  border: 1px solid var(--border);
  background: var(--surface-1);
  color: var(--text-1);
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
}

.nav-search-input:focus {
  border-color: var(--accent-30);
  background: var(--surface-2);
}

.nav-search-input::placeholder {
  color: var(--text-3);
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
  background: var(--surface-1);
}

.nav-chapter-item.active {
  background: var(--accent-15);
  border-color: var(--accent-15);
}

.nav-chapter-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--accent-30);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  font-size: 12px;
  flex-shrink: 0;
}

.nav-chapter-item.active .nav-chapter-num {
  background: var(--accent-15);
}

.nav-chapter-title {
  flex: 1;
  font-size: 14px;
  color: var(--text-2);
  letter-spacing: 1px;
}

.nav-chapter-item.active .nav-chapter-title {
  color: var(--accent);
}

.nav-footer {
  padding: 15px 20px 20px;
  border-top: 1px solid var(--surface-2);
  flex-shrink: 0;
  text-align: center;
}

.nav-progress {
  font-size: 12px;
  color: var(--text-3);
  letter-spacing: 1px;
}

// 解读面板：释义 + 悟道留言 + 底部固定输入框
.interpretation-panel {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80vh;
  border-radius: 24px 24px 0 0;
  z-index: 200;
  transform: translateY(100%);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--panel-bg);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-top: 1px solid var(--surface-2);
}

.interpretation-panel.active {
  transform: translateY(0);
}

.panel-handle {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: var(--text-3);
  margin: 15px auto;
  flex-shrink: 0;
}

.panel-scroll {
  flex: 1;
  min-height: 0;
}

.panel-inner {
  padding: 0 25px 15px;
}

.interpretation-label {
  font-size: 12px;
  letter-spacing: 4px;
  margin-bottom: 20px;
  text-align: center;
  color: var(--accent);
  opacity: 0.8;
}

.interpretation-text {
  font-size: 15px;
  line-height: 2.2;
  margin-bottom: 30px;
  color: var(--text-1);
  text-align: justify;
}

// 悟道留言区（释义面板内）
.comments-section {
  border-top: 1px solid var(--surface-2);
  padding-top: 20px;
}

.comments-header {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.comments-icon {
  font-size: 18px;
  color: var(--accent);
}

.no-comments {
  display: flex;
  justify-content: center;
  padding: 60px 0;
  font-size: 14px;
  color: var(--text-3);
  letter-spacing: 2px;
}

.comment-item {
  padding: 16px;
  border-radius: 16px;
  margin-bottom: 12px;
  background: var(--surface-1);
  border: 1px solid var(--surface-2);
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
  border: 1px solid var(--border-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  font-size: 14px;
}

.comment-user {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

.comment-time {
  font-size: 12px;
  margin-left: auto;
  color: var(--text-3);
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-2);
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--surface-2);
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-3);
  transition: all 0.2s;
  padding: 0;
}

.comment-action-btn:active {
  color: var(--text-2);
}

.comment-action-btn.liked {
  color: var(--accent);
}

.comment-action-btn .material-symbols-outlined {
  font-size: 18px;
}

.comment-likes {
  font-size: 12px;
}

// 回复样式
.replies-section {
  margin-top: 12px;
  padding-left: 12px;
  border-left: 2px solid var(--border);
}

.reply-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  background: var(--surface-1);
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
  border: 1px solid var(--border-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  font-size: 10px;
}

.reply-user {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-1);
}

.reply-time {
  font-size: 11px;
  margin-left: auto;
  color: var(--text-3);
}

.reply-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-2);
}

.reply-to {
  color: var(--accent-strong);
  font-weight: 500;
}

.show-replies-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--accent-strong);
  margin-top: 8px;
  padding: 0;
}

.show-replies-btn:active {
  color: var(--accent);
}

.replies-icon {
  font-size: 14px;
  transition: transform 0.3s ease;
}

.replies-icon.replies-expanded {
  transform: rotate(180deg);
}

.send-icon {
  font-size: 20px;
}

// 行内回复输入框（在对应留言下方展开）
.reply-input-area {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.reply-input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: var(--surface-1);
  font-size: 12px;
  outline: none;
  color: var(--text-1);
}

.reply-input::placeholder {
  color: var(--text-3);
}

.reply-send-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--surface-2);
  border: 1px solid var(--border-soft);
  color: var(--accent);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.reply-send-btn:active {
  background: var(--accent-15);
}

.reply-send-icon {
  font-size: 14px;
}

// 底部固定发表输入框
.comment-input-section {
  position: sticky;
  bottom: 0;
  padding: 15px 25px calc(15px + env(safe-area-inset-bottom));
  display: flex;
  gap: 12px;
  background: linear-gradient(0deg, rgba(18,18,18,1) 60%, rgba(18,18,18,0));
  flex-shrink: 0;
}

.comment-input {
  flex: 1;
  padding: 12px 20px;
  border-radius: 24px;
  border: 1px solid var(--border);
  background: var(--surface-1);
  font-size: 14px;
  outline: none;
  color: var(--text-1);
}

.comment-input::placeholder {
  color: var(--text-3);
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--surface-2);
  border: 1px solid var(--border-soft);
  color: var(--accent);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:active {
  transform: scale(0.9);
  background: var(--accent-15);
}

// 设置面板（共享 ThemePanel 组件，这里只保留插槽内容用到的样式）
.settings-section {
  padding: 16px;
  background: var(--surface-2);
  border-radius: 16px;
  margin-bottom: 15px;
}

.section-title {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 10px;
}

// 字体大小步进器（ThemePanel #extra 插槽内容）
.font-size-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.size-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface-1, rgba(255, 255, 255, 0.03));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-1, #e2e2e2);
}

.size-btn:active {
  transform: scale(0.9);
}

.size-display {
  min-width: 36px;
  text-align: center;
  font-size: 16px;
  color: var(--accent, #d4af37);
  font-weight: 500;
}

// 翻页方式选择（ThemePanel #extra 插槽内容）
.page-mode-title {
  margin-top: 18px;
}

.page-mode-control {
  display: flex;
  gap: 12px;
}

.mode-btn {
  flex: 1;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface-1, rgba(255, 255, 255, 0.03));
  color: var(--text-2, rgba(248, 246, 242, 0.8));
  font-size: 14px;
  letter-spacing: 1px;
}

.mode-btn.mode-active {
  border-color: var(--accent, #d4af37);
  background: var(--accent-15, rgba(212, 175, 55, 0.15));
  color: var(--accent, #d4af37);
}

// 仿真翻页：左右点击热区（透明层，仅承担点击翻页）
.tap-zone {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 28%;
  z-index: 20;
}

.tap-zone-left {
  left: 0;
}

.tap-zone-right {
  right: 0;
}

// 覆盖翻页的滑层视觉（滑层版式、左缘投影、出入场缓动）在 components/CoverPage.vue 内实现

// 遮罩层
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  z-index: 150;
}

.overlay.active {
  opacity: 1;
  pointer-events: auto;
}

@keyframes fade-in {
  from { opacity: 0; filter: blur(6px); }
  to { opacity: 1; filter: blur(0); }
}
</style>

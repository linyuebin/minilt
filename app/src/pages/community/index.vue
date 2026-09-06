<template>
  <view class="community-container" :class="theme.themeClass">
    <!-- 自定义背景层 -->
    <CustomBackground />

    <!-- 顶部标题 -->
    <view class="page-header">
      <view class="header-center">
        <view class="title-row">
          <view class="title-dot"></view>
          <text class="page-title">道友</text>
          <view class="title-dot"></view>
        </view>
        <text class="header-sub">同修智慧 · 共 {{ total }} 篇感悟</text>
      </view>
    </view>

    <!-- 快捷筛选：全部感悟 / 精选心得 / 道经篇 / 德经篇 -->
    <view class="filter-bar">
      <view
        v-for="f in filters"
        :key="f.key"
        class="filter-chip"
        :class="{ active: activeFilter === f.key }"
        @click="setFilter(f.key)"
      >
        <text>{{ f.label }}</text>
      </view>
    </view>

    <!-- 道友感悟卡片流 -->
    <scroll-view scroll-y class="feed-scroll" @scrolltolower="loadMore">
      <view v-if="feed.length === 0 && !loading" class="empty-state">
        <text class="material-symbols-outlined empty-glyph">forum</text>
        <text class="empty-text">{{ emptyText }}</text>
        <text class="empty-tip">{{ emptyTip }}</text>
      </view>

      <view
        v-for="item in feed"
        :key="item.id"
        class="feed-card"
        @click="openDetail(item)"
      >
        <view class="card-header">
          <view class="card-avatar">
            <text>{{ avatarOf(item.avatarChar, item.nickname) }}</text>
          </view>
          <view class="card-userinfo">
            <text class="card-nickname">{{ item.nickname }}</text>
            <text class="card-time">{{ item.createdAt }}</text>
          </view>
        </view>

        <!-- 引用正文摘录 -->
        <view v-if="item.chapterExcerpt" class="card-quote">
          <text class="quote-text">「{{ item.chapterExcerpt }}」</text>
        </view>

        <view class="card-content">
          <text class="card-text" :class="{ 'is-clamped': !item.expanded }">{{ item.content }}</text>
          <text
            v-if="isLongText(item.content)"
            class="expand-btn"
            @click.stop="toggleExpand(item)"
          >{{ item.expanded ? '收起' : '全文' }}</text>
        </view>

        <!-- 配图：1张单图 / 2张两列 / 3张三列 -->
        <view
          v-if="item.images && item.images.length"
          class="card-images"
          :class="'img-count-' + item.images.length"
        >
          <view
            v-for="(img, idx) in item.images"
            :key="img"
            class="card-image-item"
            @click.stop="previewImages(item.images, idx)"
          >
            <image :src="resolveImageUrl(img)" mode="aspectFill" class="card-image" />
          </view>
        </view>

        <view class="card-footer">
          <text class="card-source">{{ item.chapterTitle ? '《' + item.chapterTitle + '》' : '随笔心得' }}</text>
          <view class="card-stats">
            <view class="stat-item">
              <text class="material-symbols-outlined stat-icon">chat_bubble</text>
              <text>{{ item.replyCount || 0 }}</text>
            </view>
            <view class="stat-item" :class="{ 'stat-accent': item.liked }" @click.stop="handleLike(item)">
              <text class="material-symbols-outlined stat-icon">
                {{ item.liked ? 'thumb_up' : 'thumb_up_off_alt' }}
              </text>
              <text>{{ item.likeCount }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-state">
        <text>加载中...</text>
      </view>
      <view v-else-if="feed.length > 0 && finished" class="loading-state">
        <text>道法自然，更有后续</text>
      </view>
      <view class="body-spacer"></view>
    </scroll-view>

    <!-- 悬浮按钮：写感悟 -->
    <view class="fab" @click="openCompose">
      <text class="material-symbols-outlined fab-icon">edit</text>
      <text class="fab-text">写感悟</text>
    </view>

    <!-- 写感悟弹层（共享组件） -->
    <ComposeSheet v-model:show="showCompose" @published="onComposePublished" />

    <!-- 后台诵读悬浮条 -->
    <AudioBar />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import CustomBackground from '@/components/CustomBackground.vue'
import ComposeSheet from '@/components/ComposeSheet.vue'
import AudioBar from '@/components/AudioBar.vue'
import { getCommunityFeed, toggleCommentLike } from '@/api'
import { resolveImageUrl } from '@/utils/request'

const userStore = useUserStore()
const theme = useThemeStore()

// ============= 快捷筛选 =============
const filters = [
  { key: 'all', label: '全部感悟' },
  { key: 'featured', label: '精选心得' },
  { key: 'dao', label: '道经篇' },
  { key: 'de', label: '德经篇' }
]
const activeFilter = ref('all')

const setFilter = (key) => {
  if (activeFilter.value === key) return
  activeFilter.value = key
  reloadFeed()
}

// ============= 感悟卡片流 =============
const PAGE_SIZE = 20
const feed = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = computed(() => total.value > 0 && feed.value.length >= total.value)

const emptyText = computed(() => ({
  all: '虚位以待',
  featured: '精选虚位以待',
  dao: '道经篇暂无感悟',
  de: '德经篇暂无感悟'
})[activeFilter.value] || '暂无感悟')

const emptyTip = computed(() => ({
  all: '写下第一篇感悟，同修共进',
  featured: '多多点赞，好心得自然浮现',
  dao: '《道经》前三十七章，等你落笔',
  de: '《德经》后四十四章，等你落笔'
})[activeFilter.value] || '期待各位道友分享心得')

const loadFeed = async (next = false) => {
  if (loading.value) return
  if (next && finished.value) return
  loading.value = true
  try {
    const target = next ? page.value + 1 : 1
    const res = await getCommunityFeed({
      filter: activeFilter.value,
      page: target,
      size: PAGE_SIZE
    })
    total.value = res.total || 0
    page.value = target
    feed.value = next ? feed.value.concat(res.records || []) : (res.records || [])
  } catch (error) {
    console.error('加载感悟失败:', error)
  } finally {
    loading.value = false
  }
}

const reloadFeed = () => loadFeed(false)
const loadMore = () => loadFeed(true)

// 长文截断与展开
const LONG_TEXT_LEN = 60
const isLongText = (content) => (content || '').length > LONG_TEXT_LEN

const toggleExpand = (item) => {
  item.expanded = !item.expanded
}

// 头像字符：优先用户设置的字，否则取昵称首字
const avatarOf = (avatarChar, nickname) => avatarChar || (nickname || '道').charAt(0)

// 预览配图
const previewImages = (images, idx) => {
  const urls = (images || []).map(resolveImageUrl)
  if (!urls.length) return
  uni.previewImage({ urls, current: idx })
}

// 点进卡片：引用了正文的跳到对应章节交流
const openDetail = (item) => {
  if (item.chapterId) {
    uni.navigateTo({
      url: `/pages/reading/index?chapterId=${item.chapterId}`
    })
  }
}

// 点赞 / 取消点赞
const handleLike = async (item) => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    const res = await toggleCommentLike(item.id)
    item.liked = res.liked
    item.likeCount = res.likeCount
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// ============= 写感悟弹层 =============
const showCompose = ref(false)

const openCompose = () => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  showCompose.value = true
}

// 发布成功后回到"全部感悟"并刷新，让新感悟立即可见
const onComposePublished = () => {
  activeFilter.value = 'all'
  reloadFeed()
}

// ============= 初始化 =============
onMounted(async () => {
  if (!userStore.isLoggedIn()) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    uni.reLaunch({
      url: '/pages/login/index'
    })
    return
  }

  await loadFeed()
})
</script>

<style lang="scss" scoped>
.community-container {
  min-height: 100vh;
  background: var(--bg-flat);
  color: var(--text-1);
  padding: 30rpx 30rpx 0;
  box-sizing: border-box;
}

/* 内容区块位于自定义背景层之上 */
.page-header,
.filter-bar,
.feed-scroll {
  position: relative;
  z-index: 1;
}

// 顶部标题（两侧缀点 + 副标题统计）
.page-header {
  display: flex;
  justify-content: center;
  padding: 16rpx 0 24rpx;

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
    font-size: 44rpx;
    font-weight: 500;
    color: var(--accent);
    letter-spacing: 12rpx;
  }

  .header-sub {
    font-size: 22rpx;
    color: var(--text-3);
    letter-spacing: 1rpx;
  }
}

// 快捷筛选条
.filter-bar {
  display: flex;
  gap: 16rpx;
  padding: 6rpx 0 24rpx;
}

.filter-chip {
  padding: 12rpx 28rpx;
  border-radius: 32rpx;
  background: var(--surface-1);
  border: 1px solid var(--border, var(--accent-15));
  color: var(--text-2);
  font-size: 25rpx;
  letter-spacing: 1rpx;
  transition: all 0.2s ease;
}

.filter-chip.active {
  background: var(--accent-15);
  border-color: var(--accent-30);
  color: var(--accent);
}

// 感悟卡片流
.feed-scroll {
  height: calc(100vh - 320rpx);
}

.empty-state {
  text-align: center;
  padding: 130rpx 0;

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

// 引用正文摘录
.card-quote {
  margin-top: 20rpx;
  padding: 18rpx 22rpx;
  border-radius: 16rpx;
  background: var(--surface-2);
  border-left: 4rpx solid var(--accent-strong);
}

.quote-text {
  display: block;
  font-size: 25rpx;
  line-height: 1.7;
  color: var(--text-3);
}

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

// 底部：来源 + 互动数（点赞可点）
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
  padding: 30rpx 0;
  color: var(--text-3);
  font-size: 26rpx;
}

.body-spacer {
  height: 180rpx;
}

// 悬浮按钮（避开底部 tabbar）
.fab {
  position: fixed;
  right: 40rpx;
  bottom: calc(150rpx + env(safe-area-inset-bottom));
  z-index: 30;
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

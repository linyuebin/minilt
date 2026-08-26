<template>
  <view class="custom-tab-bar">
    <!-- 阅读全屏页的功能栏（无tabBar） -->
    <view v-if="currentPage.includes('reading/index')" class="reading-action-bar">
      <view class="action-bar-left">
        <view class="action-btn" @click="handlePrevChapter">
          <text class="material-symbols-outlined">chevron_left</text>
        </view>
      </view>

      <view class="action-bar-center">
        <view class="action-btn" @click="handleFavorite">
          <text class="material-symbols-outlined" :class="{ 'active': isFavorited }">favorite</text>
        </view>
        <view class="action-btn" @click="handleOrientation">
          <text class="material-symbols-outlined" :class="{ 'active': !isHorizontal }">rotate_90_degrees_ccw</text>
        </view>
        <view class="action-btn" @click="handleInterpretation">
          <text class="material-symbols-outlined">visibility</text>
        </view>
        <view class="action-btn" @click="handleComments">
          <text class="material-symbols-outlined">chat_bubble</text>
        </view>
      </view>

      <view class="action-bar-right">
        <view class="action-btn" @click="handleNextChapter">
          <text class="material-symbols-outlined">chevron_right</text>
        </view>
      </view>
    </view>

    <!-- 普通tabBar（三个tab：我的、道友、阅读） -->
    <view v-else class="normal-tab-bar">
      <view
        v-for="(item, index) in tabList"
        :key="index"
        class="tab-bar-item"
        :class="{ 'active': current === index }"
        @click="switchTab(item, index)"
      >
        <text class="material-symbols-outlined">{{ item.iconPath }}</text>
        <text class="tab-bar-text">{{ item.text }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'

const instance = getCurrentInstance()
const current = ref(0)
const currentPage = ref('')

// 阅读页状态
const isFavorited = ref(false)
const isHorizontal = ref(true)

// 三个tab：阅读、道友、我的
const tabList = [
  {
    pagePath: "pages/reading/home",
    text: "阅读",
    iconPath: "menu_book"
  },
  {
    pagePath: "pages/community/index",
    text: "道友",
    iconPath: "groups"
  },
  {
    pagePath: "pages/profile/index",
    text: "我的",
    iconPath: "person"
  }
]

onMounted(() => {
  updateCurrentPage()

  // 延迟多次更新，确保在路由变化时能获取到正确的页面路径
  setTimeout(() => {
    updateCurrentPage()
  }, 100)

  setTimeout(() => {
    updateCurrentPage()
  }, 500)

  try {
    uni.$on('onPageShow', updateCurrentPage)
  } catch (e) {
    console.log('事件监听失败:', e)
  }

  try {
    uni.$on('readingPageUpdate', (data) => {
      if (data.favorited !== undefined) isFavorited.value = data.favorited
      if (data.horizontal !== undefined) isHorizontal.value = data.horizontal
    })
  } catch (e) {
    console.log('事件监听失败:', e)
  }

  console.log('自定义tabBar已挂载，当前页面:', currentPage.value)
})

const switchTab = (item, index) => {
  current.value = index
  uni.switchTab({
    url: `/${item.pagePath}`
  })
}

// 阅读页功能方法
const handlePrevChapter = () => {
  uni.$emit('prevChapter')
}

const handleNextChapter = () => {
  uni.$emit('nextChapter')
}

const handleFavorite = () => {
  uni.$emit('toggleFavorite')
}

const handleOrientation = () => {
  uni.$emit('toggleOrientation')
}

const handleInterpretation = () => {
  uni.$emit('toggleInterpretation')
}

const handleComments = () => {
  uni.$emit('toggleComments')
}

onUnmounted(() => {
  try {
    uni.$off('onPageShow')
    uni.$off('readingPageUpdate')
  } catch (e) {
    console.log('移除事件监听失败:', e)
  }
})

const updateCurrentPage = () => {
  try {
    const pages = getCurrentPages()
    if (pages.length > 0) {
      const page = pages[pages.length - 1]
      const currentPagePath = page.route || page.__page?.path || ''
      currentPage.value = currentPagePath

      // 更新当前tab索引
      const tabIndex = tabList.findIndex(item => currentPagePath.includes(item.pagePath.split('/')[1]))
      if (tabIndex !== -1) {
        current.value = tabIndex
      }

      // 检查是否是阅读全屏页面
      const isReadingIndexPage = currentPagePath.includes('reading/index')
      console.log('当前页面路径:', currentPage.value, '是否阅读全屏页:', isReadingIndexPage, '当前tab索引:', current.value)
    }
  } catch (e) {
    console.log('获取当前页面失败:', e)
    currentPage.value = 'pages/reading/home'
  }
}

defineExpose({
  updateCurrentPage,
  updateReadingState: (data) => {
    if (data.favorited !== undefined) isFavorited.value = data.favorited
    if (data.horizontal !== undefined) isHorizontal.value = data.horizontal
  }
})
</script>

<style lang="scss" scoped>
.custom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  background: rgba(14, 14, 14, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(212, 175, 55, 0.2);
  padding-bottom: env(safe-area-inset-bottom);
}

// 阅读全屏页的操作栏
.reading-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px 30px;
  min-height: 80px;
}

.action-bar-left,
.action-bar-right {
  width: 60px;
  display: flex;
  justify-content: center;
}

.action-bar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
}

.action-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.08);
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

.action-btn:active {
  transform: scale(0.9);
}

.action-btn .material-symbols-outlined.active {
  color: #ff6b6b;
}

// 普通tabBar
.normal-tab-bar {
  display: flex;
  height: 50px;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0.6;
  transition: opacity 0.3s;
  position: relative;
}

.tab-bar-item.active {
  opacity: 1;
}

.tab-bar-item.active::after {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: #d4af37;
  border-radius: 1px;
}

.tab-bar-item .material-symbols-outlined {
  font-size: 22px;
}

.tab-bar-text {
  font-size: 10px;
  color: #e2e2e2;
  letter-spacing: 1px;
}
</style>

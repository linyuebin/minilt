<template>
  <view class="custom-tab-bar">
    <!-- 普通tabBar（三个tab：我的、道友、阅读） -->
    <view class="normal-tab-bar">
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
import { ref, onMounted, onUnmounted } from 'vue'

const current = ref(0)
const currentPage = ref('')

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

  console.log('自定义tabBar已挂载，当前页面:', currentPage.value)
})

const switchTab = (item, index) => {
  current.value = index
  uni.switchTab({
    url: `/${item.pagePath}`
  })
}

onUnmounted(() => {
  try {
    uni.$off('onPageShow')
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
    }
  } catch (e) {
    console.log('获取当前页面失败:', e)
    currentPage.value = 'pages/reading/home'
  }
}

defineExpose({
  updateCurrentPage
})
</script>

<style lang="scss" scoped>
.custom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 9999;
}

// 普通tabBar
.normal-tab-bar {
  display: flex;
  height: 50px;
  padding-bottom: env(safe-area-inset-bottom);
  background: rgba(14, 14, 14, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(212, 175, 55, 0.2);
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

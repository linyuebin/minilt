<template>
  <!-- 遮罩 -->
  <view v-if="show" class="compose-mask" :style="{ zIndex: overlayZIndex }" @click="close"></view>

  <!-- 写感悟弹层：引用正文（可选章节）/ 随笔 + 配图（最多3张） -->
  <view v-if="show" class="compose-sheet" :style="{ zIndex: zIndex }">
    <view class="compose-header">
      <text class="compose-title">写感悟</text>
      <view class="compose-close" @click="close">
        <text class="material-symbols-outlined">close</text>
      </view>
    </view>

    <scroll-view scroll-y class="compose-body">
      <!-- 引用正文开关 + 章节选择 -->
      <view class="quote-control">
        <view class="quote-toggle" :class="{ on: quoteMode }" @click="toggleQuoteMode">
          <text class="material-symbols-outlined quote-toggle-icon">
            {{ quoteMode ? 'check_box' : 'check_box_outline_blank' }}
          </text>
          <text>引用正文</text>
        </view>
        <view v-if="quoteMode" class="chapter-picker" @click="showChapterPicker = !showChapterPicker">
          <text class="chapter-picker-text">{{ pickerText }}</text>
          <text class="material-symbols-outlined picker-arrow" :class="{ 'is-open': showChapterPicker }">expand_more</text>
        </view>
      </view>

      <!-- 章节下拉列表：弹层内自绘（系统 picker 的 fixed 弹层会被弹层的 backdrop-filter 捕获，H5 上无法弹出） -->
      <view v-if="quoteMode && showChapterPicker" class="chapter-dropdown">
        <scroll-view scroll-y class="chapter-dropdown-scroll">
          <view
            v-for="c in chapters"
            :key="c.id"
            class="chapter-option"
            :class="{ active: selectedChapter && selectedChapter.id === c.id }"
            @click="selectQuoteChapter(c)"
          >
            <text class="chapter-option-text">第{{ c.id }}章 · {{ c.title }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- 引用的正文预览 -->
      <view v-if="quoteMode && selectedChapter" class="quote-preview">
        <text class="quote-preview-text">「{{ selectedChapter.preview }}」</text>
      </view>

      <!-- 感悟内容 -->
      <textarea
        v-model="composeContent"
        class="compose-textarea"
        placeholder="写下你的感悟，结个善缘..."
        :maxlength="500"
        auto-height
      />
      <text class="compose-counter">{{ composeContent.length }}/500</text>

      <!-- 配图：最多3张 -->
      <view v-if="composeImages.length" class="compose-images">
        <view v-for="(img, idx) in composeImages" :key="idx" class="compose-image-item">
          <image
            :src="img"
            mode="aspectFill"
            class="compose-image"
            @click="previewComposeImage(idx)"
          />
          <view class="compose-image-remove" @click.stop="removeComposeImage(idx)">
            <text class="material-symbols-outlined remove-icon">close</text>
          </view>
        </view>
        <view v-if="composeImages.length < MAX_COMPOSE_IMAGES" class="compose-image-add" @click="chooseComposeImages">
          <text class="material-symbols-outlined">add</text>
        </view>
      </view>
      <view class="compose-body-spacer"></view>
    </scroll-view>

    <view class="compose-footer">
      <view
        v-if="composeImages.length < MAX_COMPOSE_IMAGES"
        class="compose-pick-btn"
        @click="chooseComposeImages"
      >
        <text class="material-symbols-outlined">add_photo_alternate</text>
        <text class="compose-pick-text">配图 {{ composeImages.length }}/{{ MAX_COMPOSE_IMAGES }}</text>
      </view>
      <view class="compose-publish-btn" :class="{ disabled: publishing }" @click="publishCompose">
        <text>{{ publishing ? '发布中...' : '发布' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/store/user'
import { getChapters, createComment, uploadCommentImage } from '@/api'

const props = defineProps({
  show: { type: Boolean, default: false },
  // 遮罩与面板层级（道友 tab 页需盖过 custom-tab-bar 的 9999）
  zIndex: { type: Number, default: 10001 },
  overlayZIndex: { type: Number, default: 10000 }
})

const emit = defineEmits(['update:show', 'published'])

const userStore = useUserStore()

const MAX_COMPOSE_IMAGES = 3
const quoteMode = ref(false)
const chapters = ref([])
const chapterIndex = ref(0)
const showChapterPicker = ref(false)
const composeContent = ref('')
const composeImages = ref([])
const publishing = ref(false)

// 首次打开时加载章节列表（供引用正文选择）
watch(() => props.show, async (open) => {
  if (!open) return
  showChapterPicker.value = false
  if (!chapters.value.length) {
    try {
      chapters.value = (await getChapters()) || []
    } catch (e) {
      console.error('加载章节失败:', e)
    }
  }
})

const selectedChapter = computed(() =>
  quoteMode.value ? chapters.value[chapterIndex.value] || null : null
)

const pickerText = computed(() =>
  selectedChapter.value ? `第${selectedChapter.value.id}章 · ${selectedChapter.value.title}` : '选择章节'
)

// 自绘章节下拉：点击选中并收起
const selectQuoteChapter = (chapter) => {
  const idx = chapters.value.findIndex(c => c.id === chapter.id)
  if (idx >= 0) chapterIndex.value = idx
  showChapterPicker.value = false
}

// 关闭引用时收起未收起的下拉
const toggleQuoteMode = () => {
  quoteMode.value = !quoteMode.value
  if (!quoteMode.value) showChapterPicker.value = false
}

const close = () => {
  emit('update:show', false)
}

// 选择配图（最多3张）
const chooseComposeImages = () => {
  const remain = MAX_COMPOSE_IMAGES - composeImages.value.length
  if (remain <= 0) {
    uni.showToast({ title: '最多配3张图', icon: 'none' })
    return
  }
  uni.chooseImage({
    count: remain,
    sizeType: ['compressed'],
    success: (res) => {
      const paths = res.tempFilePaths || (res.tempFiles || []).map(f => f.path) || []
      composeImages.value = composeImages.value.concat(paths).slice(0, MAX_COMPOSE_IMAGES)
    }
  })
}

const removeComposeImage = (idx) => {
  composeImages.value.splice(idx, 1)
}

const previewComposeImage = (idx) => {
  uni.previewImage({ urls: composeImages.value, current: idx })
}

// 发布感悟：引用正文可自选章节，不引用则为随笔；配图先传后发
const publishCompose = async () => {
  const content = composeContent.value.trim()
  if (!content) {
    uni.showToast({ title: '请写下感悟内容', icon: 'none' })
    return
  }
  if (!userStore.isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  if (publishing.value) return
  publishing.value = true

  try {
    let images
    if (composeImages.value.length) {
      const results = await Promise.all(composeImages.value.map(path => uploadCommentImage(path)))
      images = results.map(r => r.url)
    }

    const chapter = selectedChapter.value
    await createComment({
      chapterId: chapter ? chapter.id : undefined,
      content,
      images
    })

    composeContent.value = ''
    composeImages.value = []
    quoteMode.value = false
    chapterIndex.value = 0
    showChapterPicker.value = false
    emit('update:show', false)
    uni.showToast({ title: '发布成功', icon: 'success' })
    emit('published')
  } catch (error) {
    console.error('发布感悟失败:', error)
  } finally {
    publishing.value = false
  }
}
</script>

<style lang="scss" scoped>
.compose-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
}

.compose-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-height: 85vh;
  border-radius: 24rpx 24rpx 0 0;
  background: var(--panel-bg-solid, var(--surface-1));
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border-top: 1px solid var(--accent-15);
  display: flex;
  flex-direction: column;
  animation: sheet-up 0.3s ease-out;
}

@keyframes sheet-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.compose-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 30rpx 10rpx;
  flex-shrink: 0;
}

.compose-title {
  font-size: 32rpx;
  font-weight: 500;
  color: var(--accent);
  letter-spacing: 4rpx;
}

.compose-close {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: var(--surface-2);
  border: 1px solid var(--accent-15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-2);

  .material-symbols-outlined {
    font-size: 36rpx;
  }
}

.compose-body {
  flex: 1;
  min-height: 0;
  padding: 10rpx 30rpx 0;
  box-sizing: border-box;
}

// 引用正文开关 + 章节选择
.quote-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 10rpx 0 20rpx;
}

.quote-toggle {
  display: flex;
  align-items: center;
  gap: 10rpx;
  font-size: 27rpx;
  color: var(--text-2);
  flex-shrink: 0;
}

.quote-toggle.on {
  color: var(--accent);
}

.quote-toggle-icon {
  font-size: 34rpx;
}

.chapter-picker {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  border-radius: 32rpx;
  background: var(--accent-15);
  border: 1px solid var(--accent-30);
}

.chapter-picker-text {
  font-size: 25rpx;
  color: var(--accent);
}

.picker-arrow {
  font-size: 30rpx;
  color: var(--accent);
  transition: transform 0.2s ease;

  &.is-open {
    transform: rotate(180deg);
  }
}

// 章节下拉列表（自绘，规避系统 picker 在弹层内的 fixed 定位问题）
.chapter-dropdown {
  margin-top: 16rpx;
  border-radius: 16rpx;
  background: var(--surface-2);
  border: 1px solid var(--accent-15);
  overflow: hidden;
}

.chapter-dropdown-scroll {
  max-height: 420rpx;
}

.chapter-option {
  display: flex;
  align-items: center;
  padding: 20rpx 26rpx;
  border-bottom: 1px solid var(--border-soft);

  &:last-child {
    border-bottom: none;
  }

  &.active {
    background: var(--accent-15);

    .chapter-option-text {
      color: var(--accent);
      font-weight: 600;
    }
  }
}

.chapter-option-text {
  font-size: 26rpx;
  color: var(--text-2);
}

// 引用正文预览
.quote-preview {
  padding: 18rpx 22rpx;
  margin-bottom: 20rpx;
  border-radius: 16rpx;
  background: var(--surface-2);
  border-left: 4rpx solid var(--accent-strong);
}

.quote-preview-text {
  display: block;
  font-size: 25rpx;
  line-height: 1.7;
  color: var(--text-3);
}

// 感悟内容输入
.compose-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 22rpx 26rpx;
  border-radius: 16rpx;
  background: var(--surface-1);
  border: 1px solid var(--accent-15);
  font-size: 28rpx;
  line-height: 1.7;
  color: var(--text-1);
  box-sizing: border-box;
}

.compose-counter {
  display: block;
  text-align: right;
  font-size: 22rpx;
  color: var(--text-3);
  padding: 10rpx 6rpx 0;
}

// 配图预览条
.compose-images {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
  padding: 16rpx 0 0;
}

.compose-image-item {
  position: relative;
  width: 120rpx;
  height: 120rpx;
}

.compose-image {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
  border: 1px solid var(--accent-15);
}

.compose-image-remove {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.75);
  border: 1px solid var(--accent-15);
  color: var(--text-1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.compose-image-remove .remove-icon {
  font-size: 22rpx;
}

.compose-image-add {
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  border: 1px dashed var(--accent-30);
  background: var(--surface-1);
  color: var(--text-3);
  display: flex;
  align-items: center;
  justify-content: center;

  .material-symbols-outlined {
    font-size: 44rpx;
  }
}

.compose-body-spacer {
  height: 30rpx;
}

// 底部操作条（避开 tabbar 与安全区）
.compose-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 20rpx 30rpx calc(40rpx + env(safe-area-inset-bottom));
  border-top: 1px solid var(--accent-15);
  flex-shrink: 0;
}

.compose-pick-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 26rpx;
  border-radius: 40rpx;
  background: var(--surface-1);
  border: 1px solid var(--accent-15);
  color: var(--text-2);

  .material-symbols-outlined {
    font-size: 36rpx;
  }
}

.compose-pick-text {
  font-size: 24rpx;
}

.compose-publish-btn {
  flex: 1;
  max-width: 320rpx;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 40rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: #16130c;
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 4rpx;
}

.compose-publish-btn.disabled {
  opacity: 0.6;
}
</style>

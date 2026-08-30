<template>
  <!-- 遮罩 -->
  <view
    class="tp-overlay"
    :class="{ 'tp-active': show }"
    :style="{ zIndex: overlayZIndex }"
    @click="close"
  ></view>

  <!-- 底部滑出面板 -->
  <view
    class="tp-sheet"
    :class="{ 'tp-active': show }"
    :style="{ zIndex: zIndex, paddingBottom: paddingBottom }"
  >
    <!-- 顶部横线指示条，与阅读页面板一致，点击也可关闭 -->
    <view class="tp-handle" @click="close"></view>

    <view class="tp-header">
      <view class="tp-title">{{ title }}</view>
    </view>

    <scroll-view scroll-y class="tp-scroll">
      <!-- 配色方案 -->
      <view class="tp-section">
        <view class="tp-section-title">配色方案</view>
        <view class="tp-schemes">
          <view
            v-for="scheme in schemes"
            :key="scheme.id"
            class="tp-scheme-item"
            :class="{ 'tp-scheme-active': theme.schemeId === scheme.id }"
            :style="{ background: scheme.preview }"
            @click="theme.setScheme(scheme.id)"
          >
            <text class="tp-scheme-char" :style="{ color: scheme.charColor }">{{ scheme.char }}</text>
            <text class="tp-scheme-name">{{ scheme.name }}</text>
          </view>
        </view>
      </view>

      <!-- 自定义背景 -->
      <view class="tp-section">
        <view class="tp-section-title">自定义背景</view>
        <view class="tp-bg-row">
          <image
            v-if="theme.customBg"
            class="tp-bg-preview"
            :src="theme.customBg"
            mode="aspectFill"
          />
          <view v-else class="tp-bg-placeholder">
            <text class="material-symbols-outlined">image</text>
          </view>
          <view class="tp-bg-actions">
            <view class="tp-btn tp-btn-primary" @click="handlePickBg">
              <text class="material-symbols-outlined">add</text>
              <text>选择图片</text>
            </view>
            <view
              class="tp-btn"
              :class="{ 'tp-btn-disabled': !theme.customBg }"
              @click="handleClearBg"
            >
              <text class="material-symbols-outlined">close</text>
              <text>清除背景</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 页面级扩展区块（如阅读页的字体大小）；未传插槽时不渲染，避免空白模块 -->
      <view v-if="$slots.extra" class="tp-section">
        <slot name="extra"></slot>
      </view>

      <!-- 重置 -->
      <view class="tp-section">
        <view class="tp-reset-btn" @click="handleReset">
          <text class="material-symbols-outlined">restart_alt</text>
          <text>重置为默认设置</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore, THEME_SCHEMES } from '@/store/theme'
import { pickCustomBg } from '@/utils/background'

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '修行设置' },
  // 面板与遮罩层级（个人中心等 tab 页需盖过原生 tabBar 的 9999）
  zIndex: { type: Number, default: 200 },
  overlayZIndex: { type: Number, default: 150 },
  // tab 页内使用：加高底部内边距，避免内容被 tabBar / 小黑条遮挡
  aboveTabBar: { type: Boolean, default: false }
})

const emit = defineEmits(['update:show', 'reset'])

const theme = useThemeStore()
const schemes = THEME_SCHEMES

const paddingBottom = computed(() =>
  props.aboveTabBar
    ? 'calc(85px + env(safe-area-inset-bottom))'
    : 'calc(35px + env(safe-area-inset-bottom))'
)

const close = () => {
  emit('update:show', false)
}

const handlePickBg = async () => {
  const value = await pickCustomBg()
  if (value) {
    theme.setCustomBg(value)
    uni.showToast({ title: '背景已更新', icon: 'success' })
  }
}

const handleClearBg = () => {
  if (!theme.customBg) return
  theme.clearCustomBg()
}

const handleReset = () => {
  uni.showModal({
    title: '重置设置',
    content: '将恢复默认配色方案、清除自定义背景并重置字体大小',
    confirmText: '重置',
    success: (res) => {
      if (res.confirm) {
        theme.resetTheme()
        // 页面级设置（如阅读页字体大小）由各页面监听 reset 自行重置
        emit('reset')
        uni.showToast({ title: '已重置', icon: 'success' })
      }
    }
  })
}
</script>

<style scoped>
/* 遮罩：opacity 过渡 + pointer-events，常驻不销毁 */
.tp-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}
.tp-overlay.tp-active {
  opacity: 1;
  pointer-events: auto;
}

/* 面板：与阅读页底部面板同款动效 */
.tp-sheet {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  border-radius: 24px 24px 0 0;
  padding: 25px 20px;
  transform: translateY(100%);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--panel-bg, rgba(18, 18, 18, 0.85));
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-top: 1px solid var(--border-soft, rgba(255, 255, 255, 0.06));
}
.tp-sheet.tp-active {
  transform: translateY(0);
}

/* 顶部横线指示条，与阅读页 .panel-handle 同款 */
.tp-handle {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: var(--text-3);
  margin: 0 auto 18px;
  flex-shrink: 0;
}

/* 标题与阅读页「经文释义」标签同款：居中 + accent 色 + 宽字距 */
.tp-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.tp-title {
  font-size: 12px;
  letter-spacing: 4px;
  text-align: center;
  color: var(--accent);
  opacity: 0.8;
}

.tp-scroll {
  max-height: 80vh;
}

.tp-section {
  background: var(--surface-1, rgba(255, 255, 255, 0.03));
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.06));
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 14px;
}
.tp-section-title {
  font-size: 13px;
  color: var(--text-3, rgba(255, 255, 255, 0.45));
  margin-bottom: 12px;
  letter-spacing: 2px;
}

/* 配色网格 */
.tp-schemes {
  display: flex;
  gap: 12px;
}
.tp-scheme-item {
  flex: 1;
  aspect-ratio: 1;
  border-radius: 12px;
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.tp-scheme-active {
  border-color: var(--accent, #d4af37);
  box-shadow: 0 0 15px var(--accent-30, rgba(212, 175, 55, 0.3));
}
.tp-scheme-char {
  font-size: 20px;
  font-family: 'KaiTi', 'STKaiti', serif;
}
.tp-scheme-name {
  font-size: 10px;
  color: var(--text-2, rgba(248, 246, 242, 0.8));
}

/* 自定义背景行 */
.tp-bg-row {
  display: flex;
  gap: 14px;
  align-items: center;
}
.tp-bg-preview {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
}
.tp-bg-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  border: 1px dashed var(--border, rgba(255, 255, 255, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3, rgba(255, 255, 255, 0.45));
}
.tp-bg-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tp-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface-2, rgba(255, 255, 255, 0.05));
  color: var(--text-1, #e2e2e2);
  font-size: 13px;
}
.tp-btn .material-symbols-outlined {
  font-size: 16px;
}
.tp-btn-primary {
  background: var(--accent-15, rgba(212, 175, 55, 0.15));
  border-color: var(--accent-30, rgba(212, 175, 55, 0.3));
  color: var(--accent, #d4af37);
}
.tp-btn-disabled {
  opacity: 0.4;
}

/* 重置按钮 */
.tp-reset-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid rgba(255, 107, 107, 0.3);
  background: rgba(255, 107, 107, 0.08);
  color: #ff6b6b;
  font-size: 15px;
  letter-spacing: 2px;
}
</style>

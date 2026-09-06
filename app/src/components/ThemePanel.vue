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
      <!-- 配色方案：保留最初的方块色卡样式 -->
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

      <!-- 自定义背景：单行布局 -->
      <view class="tp-row">
        <view class="tp-row-label">背景</view>
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

      <!-- 页面级扩展区块（如阅读页的字体大小|翻页方式双列）；未传插槽时不渲染，避免空白模块 -->
      <view v-if="$slots.extra" class="tp-extra">
        <slot name="extra"></slot>
      </view>

      <!-- 诵读音色：两行四列 -->
      <view class="tp-row tp-row-col">
        <view class="tp-row-label">诵读音色</view>
        <view class="tp-pills tp-pills-fill">
          <view
            v-for="v in voiceOptions"
            :key="v.key"
            class="tp-pill"
            :class="{ 'tp-pill-active': audioStore.voiceKey === v.key }"
            @click="audioStore.setVoice(v.key)"
          >{{ v.name }}</view>
        </view>
      </view>

      <!-- 播放方式 -->
      <view class="tp-row">
        <view class="tp-row-label">播放方式</view>
        <view class="tp-pills tp-pills-end">
          <view
            v-for="m in playModes"
            :key="m.value"
            class="tp-pill"
            :class="{ 'tp-pill-active': audioStore.playMode === m.value }"
            @click="audioStore.setPlayMode(m.value)"
          >{{ m.label }}</view>
        </view>
      </view>

      <!-- 重置 -->
      <view class="tp-reset-btn" @click="handleReset">
        <text class="material-symbols-outlined">restart_alt</text>
        <text>重置为默认设置</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useThemeStore, THEME_SCHEMES } from '@/store/theme'
import { useAudioStore } from '@/store/audio'
import { pickCustomBg } from '@/utils/background'
import { getAudioVoices } from '@/api'

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
const audioStore = useAudioStore()
const schemes = THEME_SCHEMES

const paddingBottom = computed(() =>
  props.aboveTabBar
    ? 'calc(24px + env(safe-area-inset-bottom))'
    : 'calc(12px + env(safe-area-inset-bottom))'
)

// ===== 诵读音色 / 播放方式（全局 audio store，后台播放实时生效） =====
// 后端未就绪时兜底音色；正常情况下以 /api/audio/voices 返回为准
const VOICE_FALLBACK = [
  { key: 'xiaoxiao', name: '晓晓' },
  { key: 'yunxia', name: '云夏' }
]
const PLAY_MODES = [
  { value: 'continuous', label: '连播' },
  { value: 'loop', label: '循环本章' }
]
const voiceOptions = ref(VOICE_FALLBACK)
const playModes = PLAY_MODES

onMounted(async () => {
  try {
    const list = await getAudioVoices()
    if (Array.isArray(list) && list.length) {
      voiceOptions.value = list.map(v => ({ key: v.voiceKey, name: v.voiceName }))
    }
  } catch (e) {
    // 拉取失败沿用兜底音色
  }
  // 存储值不合法时校正到第一项
  if (!voiceOptions.value.some(v => v.key === audioStore.voiceKey)) {
    audioStore.setVoice(voiceOptions.value[0].key)
  }
  if (!playModes.some(m => m.value === audioStore.playMode)) {
    audioStore.setPlayMode(playModes[0].value)
  }
})

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

/* 面板：与阅读页底部面板同款动效；高度随内容自适应，极小屏时在面板内滚动兜底 */
.tp-sheet {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-height: 75vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  border-radius: 24px 24px 0 0;
  padding: 10px 16px;
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
  width: 36px;
  height: 3px;
  border-radius: 2px;
  background: var(--text-3);
  margin: 0 auto 6px;
  flex-shrink: 0;
}

/* 标题与阅读页「经文释义」标签同款：居中 + accent 色 + 宽字距 */
.tp-header {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-bottom: 6px;
}
.tp-title {
  font-size: 11px;
  letter-spacing: 3px;
  text-align: center;
  color: var(--accent);
  opacity: 0.8;
}

/* 滚动区：正常内容一屏放完不滚动，超出时内滚兜底 */
.tp-scroll {
  flex-shrink: 1;
}

/* 卡片分区（配色方案保留最初的卡片样式） */
.tp-section {
  background: var(--surface-1, rgba(255, 255, 255, 0.03));
  border: 1px solid var(--border-soft, rgba(255, 255, 255, 0.06));
  border-radius: 14px;
  padding: 12px 14px;
  margin-bottom: 8px;
}
.tp-section-title {
  font-size: 12px;
  color: var(--text-3, rgba(255, 255, 255, 0.45));
  margin-bottom: 10px;
  letter-spacing: 2px;
}

/* 设置行：标签居左 + 控件居右 */
.tp-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
}
.tp-row,
.tp-extra,
.tp-reset-btn {
  border-top: 1px solid var(--border-soft, rgba(255, 255, 255, 0.06));
}
.tp-row-col {
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}
.tp-row-label {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--text-3, rgba(255, 255, 255, 0.45));
  letter-spacing: 2px;
}

/* 配色方块：恢复最初样式（楷体字 + 名称，正方形色卡） */
.tp-schemes {
  display: flex;
  gap: 10px;
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
  box-shadow: 0 0 12px var(--accent-30, rgba(212, 175, 55, 0.3));
}
.tp-scheme-char {
  font-size: 20px;
  font-family: 'KaiTi', 'STKaiti', serif;
}
.tp-scheme-name {
  font-size: 10px;
  color: var(--text-2, rgba(248, 246, 242, 0.8));
}

/* 自定义背景行：预览 + 横排按钮，单行放完 */
.tp-bg-row {
  flex: 1;
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}
.tp-bg-preview {
  width: 38px;
  height: 38px;
  border-radius: 9px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
}
.tp-bg-placeholder {
  width: 38px;
  height: 38px;
  border-radius: 9px;
  border: 1px dashed var(--border, rgba(255, 255, 255, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-3, rgba(255, 255, 255, 0.45));
}
.tp-bg-placeholder .material-symbols-outlined {
  font-size: 16px;
}
.tp-bg-actions {
  display: flex;
  flex-direction: row;
  gap: 8px;
}
.tp-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 12px;
  border-radius: 9px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface-2, rgba(255, 255, 255, 0.05));
  color: var(--text-1, #e2e2e2);
  font-size: 12px;
}
.tp-btn .material-symbols-outlined {
  font-size: 14px;
}
.tp-btn-primary {
  background: var(--accent-15, rgba(212, 175, 55, 0.15));
  border-color: var(--accent-30, rgba(212, 175, 55, 0.3));
  color: var(--accent, #d4af37);
}
.tp-btn-disabled {
  opacity: 0.4;
}

/* 扩展区块（阅读页字体大小|翻页方式双列） */
.tp-extra {
  display: flex;
  gap: 14px;
  padding: 7px 0;
}

/* 音色/播放方式胶囊：音色两行四列铺满；播放方式行内靠右 */
.tp-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tp-pills-fill {
  flex: 1;
}
.tp-pills-fill .tp-pill {
  flex: 1 1 calc(25% - 6px);
  height: 32px;
  padding: 0;
  border-radius: 9px;
  font-size: 13px;
}
.tp-pills-end {
  flex: 1;
  justify-content: flex-end;
}
.tp-pill {
  height: 28px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
  background: var(--surface-2, rgba(255, 255, 255, 0.05));
  color: var(--text-2, rgba(248, 246, 242, 0.8));
  font-size: 12px;
  letter-spacing: 1px;
  white-space: nowrap;
}
.tp-pill-active {
  border-color: var(--accent, #d4af37);
  background: var(--accent-15, rgba(212, 175, 55, 0.15));
  color: var(--accent, #d4af37);
}

/* 重置按钮 */
.tp-reset-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  border-radius: 9px;
  background: rgba(255, 107, 107, 0.08);
  color: #ff6b6b;
  font-size: 12px;
  letter-spacing: 2px;
}
.tp-reset-btn .material-symbols-outlined {
  font-size: 14px;
}
</style>

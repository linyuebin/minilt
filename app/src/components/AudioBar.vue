<template>
  <!-- 后台诵读悬浮条：tabBar 之上，点击回到阅读页，支持播放/暂停与关闭 -->
  <view v-if="audio.barVisible" class="audio-bar" :class="[theme.themeClass, { 'no-tabbar': !tabBar }]">
    <view class="audio-bar-main" @click="openReading">
      <view class="audio-disc" :class="{ spinning: audio.playing }">
        <text class="disc-char">诵</text>
      </view>
      <view class="audio-info">
        <text class="audio-title">第{{ audio.chapterId }}章{{ audio.chapterTitle ? ' · ' + audio.chapterTitle : '' }}</text>
        <text class="audio-status">{{ statusText }}</text>
      </view>
    </view>
    <view class="audio-actions">
      <view class="audio-btn" @click="togglePlay">
        <text class="material-symbols-outlined">{{ audio.playing ? 'pause_circle' : 'play_circle' }}</text>
      </view>
      <view class="audio-btn audio-close" @click="stopPlay">
        <text class="material-symbols-outlined">close</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useAudioStore } from '@/store/audio'
import { useThemeStore } from '@/store/theme'

const audio = useAudioStore()
const theme = useThemeStore()

// 所在页面是否带 tabBar：二级页面没有 tabBar，浮条直接贴底
const props = defineProps({
  tabBar: { type: Boolean, default: true }
})

const VOICE_NAMES = {
  xiaoxiao: '晓晓',
  xiaoyi: '晓伊',
  xiaoxuan: '晓萱',
  xiaoyu: '曉雨',
  yunxia: '云夏',
  yunjian: '云健',
  yunxi: '云希',
  yunyang: '云扬'
}

const statusText = computed(() => {
  const voice = VOICE_NAMES[audio.voiceKey] || audio.voiceKey
  return audio.playing ? `${voice} 诵读中` : '已暂停'
})

// 点击主体：回到正在诵读的章节
const openReading = () => {
  uni.navigateTo({
    url: `/pages/reading/index?chapterId=${audio.chapterId}`
  })
}

const togglePlay = () => {
  if (audio.playing) audio.pause()
  else audio.resume()
}

const stopPlay = () => {
  audio.stop()
}
</script>

<style lang="scss" scoped>
.audio-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(50px + env(safe-area-inset-bottom) + 14rpx);
  z-index: 900;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 14rpx 18rpx 14rpx 14rpx;
  border-radius: 60rpx;
  background: var(--panel-bg, var(--surface-1));
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid var(--accent-30);
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.35);
}

/* 二级页面（无 tabBar）：直接贴底，避开安全区 */
.audio-bar.no-tabbar {
  bottom: calc(18rpx + env(safe-area-inset-bottom));
}

.audio-bar-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

// 旋转光盘：播放时转动
.audio-disc {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-30), var(--surface-2));
  border: 1px solid var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.disc-char {
  font-size: 30rpx;
  color: var(--accent);
  font-family: 'KaiTi', 'STKaiti', serif;
  font-weight: 600;
}

.audio-disc.spinning {
  animation: disc-spin 6s linear infinite;
}

@keyframes disc-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.audio-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.audio-title {
  font-size: 26rpx;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.audio-status {
  font-size: 21rpx;
  color: var(--accent);
  opacity: 0.85;
  letter-spacing: 1rpx;
}

.audio-actions {
  display: flex;
  align-items: center;
  gap: 6rpx;
  flex-shrink: 0;
}

.audio-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);

  .material-symbols-outlined {
    font-size: 48rpx;
  }

  &.audio-close {
    color: var(--text-3);

    .material-symbols-outlined {
      font-size: 34rpx;
    }
  }
}
</style>

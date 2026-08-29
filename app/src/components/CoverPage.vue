<template>
  <!-- 覆盖翻页滑层：forward 时携带新章自右滑入覆盖，back 时携带旧章向右滑出露出下层。
       纯 CSS 关键帧驱动（GPU 合成），逻辑层不逐帧参与。 -->
  <view class="cover-sheet" :class="dir === 'forward' ? 'sheet-in' : 'sheet-out'">
    <view class="cover-copy" :class="{ 'is-zen': zen }">
      <view
        class="cover-text"
        :class="isHorizontal ? 'cover-text-h' : 'cover-text-v'"
        :style="{ fontSize: fontSize + 'px' }"
      >
        <text class="cover-line" v-for="(line, li) in lines" :key="li">{{ line }}</text>
      </view>
    </view>
    <!-- 滑层左缘投影：贴在滑层外侧，随滑层一起移动，压在下层页面上 -->
    <view class="cover-edge"></view>
  </view>
</template>

<script setup>
defineProps({
  // 滑层携带的章节文本行
  lines: { type: Array, default: () => [] },
  fontSize: { type: Number, default: 20 },
  isHorizontal: { type: Boolean, default: true },
  // forward：新章自右滑入覆盖；back：旧章向右滑出
  dir: { type: String, default: 'forward' },
  zen: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
.cover-sheet {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 25;
  pointer-events: none;
  background: var(--page-bg, #0e0e0e);
  will-change: transform;
}

// 滑入减速落位（ease-out-cubic），滑出加速离场（ease-in-cubic）
.sheet-in {
  animation: cover-in 500ms cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.sheet-out {
  animation: cover-out 500ms cubic-bezier(0.64, 0, 0.78, 0) both;
}

@keyframes cover-in {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

@keyframes cover-out {
  from { transform: translateX(0); }
  to { transform: translateX(100%); }
}

// 滑层内容：整体复制主内容版式，落位/离场时与主内容无缝衔接
.cover-copy {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  box-sizing: border-box;
  padding: 10px 40px 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  &.is-zen {
    padding-bottom: 40px;
  }
}

// 文字版式与阅读页 .chapter-content-* 保持一致（两处需同步修改）；
// 关闭 fade-in 动画，避免滑层内容在移动中从透明淡入
.cover-text-h {
  max-width: 90vw;
  line-height: 1.5;
  color: var(--text-bright);
  text-align: center;
  letter-spacing: 0.25em;
  padding: 0 2rem;
  animation: none;
}

.cover-text-v {
  line-height: 1.45;
  color: var(--text-bright);
  text-align: center;
  letter-spacing: 0.35em;
  writing-mode: vertical-rl;
  text-orientation: upright;
  height: 55vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 2rem 0;
  animation: none;
}

// 左缘投影：位于滑层外侧（right: 100%），模拟滑层压在下层页面上的落影
.cover-edge {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 100%;
  width: 32px;
  pointer-events: none;
  background: linear-gradient(to left, rgba(0, 0, 0, 0.4) 0%, rgba(0, 0, 0, 0) 100%);
}
</style>

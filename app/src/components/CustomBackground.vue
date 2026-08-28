<template>
  <!-- 自定义背景层：固定铺满、居中裁切（aspectFill），不响应点击 -->
  <image
    v-if="theme.customBg"
    class="custom-bg-layer"
    :src="theme.customBg"
    mode="aspectFill"
    @error="onImgError"
  />
</template>

<script setup>
import { useThemeStore } from '@/store/theme'

const theme = useThemeStore()

// 图片加载失败的兜底（restore 的 access 校验可能放行的失效路径）
const onImgError = () => {
  theme.clearCustomBg(true)
}
</script>

<style scoped>
.custom-bg-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}
</style>

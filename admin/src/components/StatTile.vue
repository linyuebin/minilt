<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: Number, default: 0 },
  hint: { type: String, default: '' },
})

// 自动紧凑：1,284 / 12.9K
const display = computed(() => {
  const v = props.value ?? 0
  if (v >= 10000) return `${(v / 1000).toFixed(1)}K`
  return v.toLocaleString('zh-CN')
})
</script>

<template>
  <div class="tile">
    <div class="tile-label">{{ label }}</div>
    <div class="tile-value">{{ display }}</div>
    <div v-if="hint" class="tile-hint">{{ hint }}</div>
  </div>
</template>

<style scoped>
.tile {
  background: #fff;
  border: 1px solid rgba(11, 11, 11, 0.1);
  border-radius: 8px;
  padding: 18px 20px;
}

.tile-label {
  color: #52514e;
  font-size: 13px;
  margin-bottom: 8px;
}

/* 大号独立数字用默认比例数字，tabular-nums 只留给需要纵向对齐的列 */
.tile-value {
  color: #0b0b0b;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.1;
}

.tile-hint {
  margin-top: 6px;
  color: #898781;
  font-size: 12px;
}
</style>

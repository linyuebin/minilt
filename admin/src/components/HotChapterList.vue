<script setup>
import { computed } from 'vue'
import { VIZ } from '../utils/vizTokens'

const props = defineProps({
  items: { type: Array, default: () => [] },
})

// 章节是名义类目（顺序换了也不改变含义），所以所有条共用同一个槽位色，
// 不按数值上色 —— 条长已经表达了量级，不该再花掉身份通道。
const barColor = VIZ.series1

const max = computed(() => Math.max(1, ...props.items.map((i) => i.commentCount || 0)))
// 0 就是不画条：给零值留一截「最小宽度」会把没有的量画成有
const widthOf = (n) => (n > 0 ? `${Math.max(1.5, (n / max.value) * 100)}%` : '0%')
</script>

<template>
  <div class="hot-list">
    <div class="hot-head">
      <span class="col-rank">#</span>
      <span class="col-name">章节</span>
      <span class="col-bar"></span>
      <span class="col-num">留言</span>
      <span class="col-num">收藏</span>
    </div>
    <div v-for="(item, idx) in items" :key="item.chapterId" class="hot-row">
      <span class="col-rank">{{ idx + 1 }}</span>
      <span class="col-name" :title="item.title">第{{ item.chapterId }}章 · {{ item.title }}</span>
      <span class="col-bar">
        <span class="bar-track">
          <span class="bar-fill" :style="{ width: widthOf(item.commentCount), background: barColor }" />
        </span>
      </span>
      <span class="col-num">{{ item.commentCount }}</span>
      <span class="col-num muted">{{ item.favoriteCount }}</span>
    </div>
    <p v-if="!items.length" class="empty">暂无数据</p>
  </div>
</template>

<style scoped>
.hot-list {
  font-size: 13px;
}

.hot-head,
.hot-row {
  display: grid;
  grid-template-columns: 28px minmax(120px, 1fr) minmax(80px, 2fr) 52px 52px;
  align-items: center;
  gap: 12px;
  padding: 7px 0;
}

.hot-head {
  color: #898781;
  border-bottom: 1px solid #e1e0d9;
  padding-bottom: 10px;
  margin-bottom: 4px;
}

.col-name {
  color: #0b0b0b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-rank {
  color: #898781;
  font-variant-numeric: tabular-nums;
}

/* 数字成列，需要纵向对齐，用等宽数字 */
.col-num {
  text-align: right;
  color: #0b0b0b;
  font-variant-numeric: tabular-nums;
}

.col-num.muted {
  color: #52514e;
}

.bar-track {
  display: block;
  width: 100%;
  height: 10px;
}

/* 细挑的条 + 数据端 4px 圆角，基线端保持方角 */
.bar-fill {
  display: block;
  height: 100%;
  border-radius: 0 4px 4px 0;
}

.empty {
  color: #898781;
  text-align: center;
  padding: 20px 0;
}
</style>

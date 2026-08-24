<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { AXIS_LABEL, FONT_FAMILY, VIZ } from '../utils/vizTokens'

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  dates: { type: Array, default: () => [] },
  newUsers: { type: Array, default: () => [] },
  newComments: { type: Array, default: () => [] },
})

const el = ref(null)
const chart = shallowRef(null)
let observer = null

// 月-日短标签，30 天下完整日期会挤在一起
const shortDate = (d) => (d || '').slice(5)

function buildOption() {
  const common = {
    type: 'line',
    smooth: false,
    showSymbol: false, // 30 个点全画圆点会糊成一片；hover 时才出标记
    symbol: 'circle',
    symbolSize: 8,
    lineStyle: { width: 2, cap: 'round', join: 'round' },
    emphasis: { focus: 'series', itemStyle: { borderColor: VIZ.surface, borderWidth: 2 } },
    // 直接标注只落在末点，配合图例构成双重身份通道；文字用文本色而非系列色
    endLabel: {
      show: true,
      color: VIZ.textSecondary,
      fontSize: 12,
      fontFamily: FONT_FAMILY,
      formatter: ({ seriesName, value }) => `${seriesName} ${value}`,
    },
    // 两条线收敛到同一点时末端标注会叠在一起，叠放比不标更糟：直接隐藏，
    // 身份仍由图例承载，数值由 tooltip 和表格视图兜底
    labelLayout: { hideOverlap: true },
  }

  return {
    textStyle: { fontFamily: FONT_FAMILY },
    color: [VIZ.series1, VIZ.series2],
    // 两条曲线同为「每日条数」，共用一根 y 轴（绝不做双轴）
    grid: { top: 44, left: 8, right: 96, bottom: 8, containLabel: true },
    legend: {
      show: true,
      top: 0,
      left: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 3,
      itemGap: 20,
      textStyle: { color: VIZ.textSecondary, fontSize: 13 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: VIZ.axis, width: 1 } },
      backgroundColor: VIZ.surface,
      borderColor: VIZ.gridline,
      textStyle: { color: VIZ.textPrimary, fontSize: 13 },
      extraCssText: 'box-shadow:0 4px 16px rgba(11,11,11,.10);',
    },
    xAxis: {
      type: 'category',
      data: props.dates.map(shortDate),
      boundaryGap: false,
      axisLine: { lineStyle: { color: VIZ.axis } },
      axisTick: { show: false },
      axisLabel: { ...AXIS_LABEL, interval: 'auto' },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: AXIS_LABEL,
      splitLine: { lineStyle: { color: VIZ.gridline, width: 1, type: 'solid' } },
    },
    series: [
      { ...common, name: '新增用户', data: props.newUsers },
      { ...common, name: '新增留言', data: props.newComments },
    ],
  }
}

function render() {
  if (!chart.value) return
  chart.value.setOption(buildOption(), true)
}

onMounted(() => {
  chart.value = echarts.init(el.value)
  render()
  observer = new ResizeObserver(() => chart.value?.resize())
  observer.observe(el.value)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  chart.value?.dispose()
})

watch(() => [props.dates, props.newUsers, props.newComments], render, { deep: true })
</script>

<template>
  <div ref="el" class="trend-chart"></div>
</template>

<style scoped>
.trend-chart {
  width: 100%;
  height: 320px;
}
</style>

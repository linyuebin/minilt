<script setup>
import { computed, onMounted, ref } from 'vue'
import * as api from '../api'
import StatTile from '../components/StatTile.vue'
import TrendChart from '../components/TrendChart.vue'
import HotChapterList from '../components/HotChapterList.vue'

const loading = ref(true)
const overview = ref({})
const trend = ref({ dates: [], newUsers: [], newComments: [] })
const hot = ref([])
const showTable = ref(false)

// 表格视图：图表之外的第二条读取通道（可访问性要求）
const trendRows = computed(() =>
  trend.value.dates.map((d, i) => ({
    date: d,
    users: trend.value.newUsers[i] ?? 0,
    comments: trend.value.newComments[i] ?? 0,
  })).reverse()
)

async function load() {
  loading.value = true
  try {
    const [o, t, h] = await Promise.all([
      api.fetchOverview(),
      api.fetchTrend(30),
      api.fetchHotChapters(10),
    ])
    overview.value = o
    trend.value = t
    hot.value = h
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="kpi-row">
      <StatTile label="累计用户" :value="overview.totalUsers" />
      <StatTile label="今日新增用户" :value="overview.todayUsers" />
      <StatTile label="累计留言" :value="overview.totalComments" />
      <StatTile label="今日新增留言" :value="overview.todayComments" />
      <StatTile label="累计收藏" :value="overview.totalFavorites" hint="修道藏经" />
    </div>

    <el-row :gutter="16" class="charts">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-head">
              <span>近 30 天新增趋势</span>
              <el-button link type="primary" @click="showTable = !showTable">
                {{ showTable ? '看图表' : '看表格' }}
              </el-button>
            </div>
          </template>
          <TrendChart v-show="!showTable" :dates="trend.dates" :new-users="trend.newUsers"
            :new-comments="trend.newComments" />
          <el-table v-if="showTable" :data="trendRows" height="320" size="small">
            <el-table-column prop="date" label="日期" />
            <el-table-column prop="users" label="新增用户" align="right" />
            <el-table-column prop="comments" label="新增留言" align="right" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-head"><span>章节热度榜 Top 10</span></div>
          </template>
          <HotChapterList :items="hot" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.charts {
  margin: 0;
}

.chart-card {
  border-color: rgba(11, 11, 11, 0.1);
  margin-bottom: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
</style>

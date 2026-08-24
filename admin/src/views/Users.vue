<script setup>
import { onMounted, reactive, ref } from 'vue'
import * as api from '../api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const query = reactive({ page: 1, size: 20, source: '', keyword: '' })

async function load() {
  loading.value = true
  try {
    const data = await api.fetchUsers({
      page: query.page,
      size: query.size,
      source: query.source || undefined,
      keyword: query.keyword || undefined,
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.source = ''
  query.keyword = ''
  search()
}

// 修行进度：已读到第 N 章 / 81
const progressPct = (n) => Math.round(((n || 0) / 81) * 100)

onMounted(load)
</script>

<template>
  <div>
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="来源">
        <el-select v-model="query.source" placeholder="全部" clearable style="width: 130px">
          <el-option label="H5" value="H5" />
          <el-option label="微信小程序" value="MP" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="昵称 / 用户名" clearable style="width: 220px"
          @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column label="用户名" width="140">
          <template #default="{ row }">{{ row.username || '—' }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.source === 'MP' ? 'success' : 'info'">
              {{ row.source === 'MP' ? '微信小程序' : 'H5' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="修行进度" min-width="180">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="progressPct(row.lastChapterId)" :stroke-width="8" :show-text="false" />
              <span class="progress-text">第 {{ row.lastChapterId || 0 }} / 81 章</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="commentCount" label="留言数" width="90" align="right" />
        <el-table-column prop="favoriteCount" label="收藏数" width="90" align="right" />
        <el-table-column prop="createdAt" label="注册时间" width="150" />
        <el-table-column prop="lastLoginAt" label="最近登录" width="150" />
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="load"
          @size-change="search" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-cell :deep(.el-progress) {
  flex: 1;
  min-width: 70px;
}

.progress-text {
  color: #52514e;
  font-size: 12px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
</style>

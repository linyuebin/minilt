<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '../api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const query = reactive({ page: 1, size: 20, chapterId: null, keyword: '' })

// 展开行用的回复链缓存：key = 顶级留言 id
const replyCache = reactive({})

async function load() {
  loading.value = true
  try {
    const data = await api.fetchComments({
      page: query.page,
      size: query.size,
      chapterId: query.chapterId || undefined,
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
  query.chapterId = null
  query.keyword = ''
  search()
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确认删除该留言？删除后用户端将不再展示（软删除，数据保留）。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
  )
  await api.deleteComment(row.id)
  ElMessage.success('已删除')
  load()
}

// 展开顶级留言时按 chapterId 拉该章全部评论，从中筛出它的回复链
async function onExpand(row, expanded) {
  if (!expanded.length || row.parentId || replyCache[row.id]) return
  const data = await api.fetchComments({ page: 1, size: 200, chapterId: row.chapterId })
  replyCache[row.id] = (data.records || []).filter((c) => c.parentId === row.id)
}

onMounted(load)
</script>

<template>
  <div>
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="章节">
        <el-input-number v-model="query.chapterId" :min="1" :max="81" :controls="false" placeholder="章节号"
          style="width: 110px" />
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="留言内容 / 昵称" clearable style="width: 220px"
          @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" row-key="id" @expand-change="onExpand">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="reply-panel">
              <template v-if="row.parentId">
                <p class="reply-note">这是一条回复，回复对象：{{ row.replyToNickname || '—' }}</p>
              </template>
              <template v-else>
                <p class="reply-note">回复链（{{ (replyCache[row.id] || []).length }} 条）</p>
                <div v-for="r in replyCache[row.id] || []" :key="r.id" class="reply-item">
                  <span class="reply-user">{{ r.nickname }}</span>
                  <span class="reply-to" v-if="r.replyToNickname">回复 {{ r.replyToNickname }}</span>
                  <span class="reply-content">{{ r.content }}</span>
                  <span class="reply-time">{{ r.createdAt }}</span>
                </div>
                <p v-if="!(replyCache[row.id] || []).length" class="reply-empty">暂无回复</p>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column label="章节" width="150">
          <template #default="{ row }">第{{ row.chapterId }}章 · {{ row.chapterTitle }}</template>
        </el-table-column>
        <el-table-column prop="nickname" label="用户" width="120" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.parentId ? 'info' : 'primary'" size="small" effect="plain">
              {{ row.parentId ? '回复' : '留言' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="danger" size="small" effect="plain">已删除</el-tag>
            <span v-else class="status-normal">正常</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="likeCount" label="点赞" width="72" align="right" />
        <el-table-column prop="createdAt" label="时间" width="150" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" link type="danger" @click="remove(row)">删除</el-button>
            <span v-else class="status-normal">—</span>
          </template>
        </el-table-column>
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
.reply-panel {
  padding: 8px 48px;
}

.reply-note {
  margin: 0 0 8px;
  color: #898781;
  font-size: 13px;
}

.reply-item {
  display: flex;
  gap: 10px;
  align-items: baseline;
  padding: 6px 0;
  border-top: 1px solid #f0efec;
  font-size: 13px;
}

.reply-user {
  color: #0b0b0b;
  font-weight: 600;
}

.reply-to,
.reply-time {
  color: #898781;
}

.reply-content {
  flex: 1;
  color: #52514e;
}

.reply-empty {
  color: #898781;
  font-size: 13px;
  margin: 0;
}

.status-normal {
  color: #898781;
  font-size: 13px;
}
</style>

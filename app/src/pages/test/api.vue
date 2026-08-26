<template>
  <view class="test-container">
    <text class="test-title">API 测试页面</text>

    <view class="test-section">
      <text class="section-title">1. 测试 uni.request</text>
      <button @click="testUniRequest">测试请求</button>
      <text class="result">结果: {{ uniRequestResult }}</text>
    </view>

    <view class="test-section">
      <text class="section-title">2. 测试 API 函数</text>
      <button @click="testApiFunction">测试 getChapters()</button>
      <text class="result">结果: {{ apiFunctionResult }}</text>
    </view>

    <view class="test-section">
      <text class="section-title">3. 环境信息</text>
      <text class="info">BASE_URL: {{ baseUrl }}</text>
      <text class="info">当前页面: {{ currentPagePath }}</text>
      <text class="info">UserAgent: {{ userAgent }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getChapters } from '@/api'

const uniRequestResult = ref('等待测试...')
const apiFunctionResult = ref('等待测试...')
const baseUrl = ref('')
const currentPagePath = ref('')
const userAgent = ref('')

// 测试 uni.request
const testUniRequest = () => {
  uniRequestResult.value = '正在请求...'

  uni.request({
    url: '/api/chapters',
    method: 'GET',
    success: (res) => {
      console.log('uni.request 成功:', res)
      uniRequestResult.value = '成功! 获取到 ' + res.data.data.length + ' 章节'
    },
    fail: (err) => {
      console.error('uni.request 失败:', err)
      uniRequestResult.value = '失败: ' + JSON.stringify(err)
    }
  })
}

// 测试 API 函数
const testApiFunction = async () => {
  apiFunctionResult.value = '正在请求...'

  try {
    const result = await getChapters()
    console.log('getChapters 成功:', result)
    apiFunctionResult.value = '成功! 获取到 ' + result.length + ' 章节'
  } catch (error) {
    console.error('getChapters 失败:', error)
    apiFunctionResult.value = '失败: ' + error.message
  }
}

onMounted(() => {
  // 获取环境信息
  const pages = getCurrentPages()
  if (pages.length > 0) {
    const page = pages[pages.length - 1]
    currentPagePath.value = page.route || page.__page?.path || 'unknown'
  }

  // 检测BASE_URL
  baseUrl.value = '/api (开发模式)'
  userAgent.value = navigator.userAgent
})
</script>

<style lang="scss" scoped>
.test-container {
  padding: 20px;
  min-height: 100vh;
  background: #0a0a0a;
  color: #e2e2e2;
}

.test-title {
  font-size: 24px;
  color: #d4af37;
  text-align: center;
  margin-bottom: 30px;
  display: block;
}

.test-section {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  color: #f8f6f2;
  margin-bottom: 15px;
  display: block;
}

button {
  background: rgba(212, 175, 55, 0.2);
  border: 1px solid rgba(212, 175, 55, 0.4);
  color: #d4af37;
  padding: 10px 20px;
  border-radius: 8px;
  margin-bottom: 15px;
  cursor: pointer;
}

.result {
  font-size: 14px;
  color: #888;
  display: block;
  white-space: pre-wrap;
}

.info {
  font-size: 12px;
  color: #888;
  display: block;
  margin-bottom: 8px;
}
</style>

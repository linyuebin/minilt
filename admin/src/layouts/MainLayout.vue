<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

async function logout() {
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  auth.signOut()
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="brand">
        <span class="brand-mark">道</span>
        <span class="brand-text">道德经社区</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#1f1d1a" text-color="#c9c4b8"
        active-text-color="#d4af37">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/comments">
          <el-icon><ChatDotSquare /></el-icon>
          <span>评论管理</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户列表</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="header-title">{{ currentTitle }}</span>
        <div class="header-right">
          <span class="admin-name">{{ auth.name || 'admin' }}</span>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #1f1d1a;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid #2f2c27;
}

.brand-mark {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 4px;
  background: #d4af37;
  color: #1f1d1a;
  font-weight: 700;
}

.brand-text {
  color: #e8e3d6;
  font-size: 15px;
  letter-spacing: 1px;
}

.menu {
  border-right: none;
  flex: 1;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e3dc;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-name {
  color: #666;
  font-size: 14px;
}

.main {
  padding: 20px;
  background: var(--ink-bg);
}
</style>

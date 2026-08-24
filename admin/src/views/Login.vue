<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    await auth.signIn(form.username, form.password)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // 错误提示已在拦截器中弹出
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-head">
        <span class="mark">道</span>
        <h1>道德经社区</h1>
        <p>管理后台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="'User'" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="'Lock'" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #1f1d1a 0%, #322d25 100%);
}

.login-card {
  width: 360px;
  padding: 36px 32px 32px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.28);
}

.login-head {
  text-align: center;
  margin-bottom: 26px;
}

.mark {
  display: inline-block;
  width: 46px;
  height: 46px;
  line-height: 46px;
  border-radius: 8px;
  background: #1f1d1a;
  color: #d4af37;
  font-size: 24px;
  font-weight: 700;
}

.login-head h1 {
  margin: 12px 0 4px;
  font-size: 20px;
  letter-spacing: 3px;
}

.login-head p {
  margin: 0;
  color: #999;
  font-size: 13px;
  letter-spacing: 2px;
}

.submit {
  width: 100%;
}
</style>

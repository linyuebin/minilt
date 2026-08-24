<template>
  <view class="login-container">
    <!-- H5端账密登录表单 -->
    <!-- #ifdef H5 -->
    <view class="h5-login">
      <view class="logo-section">
        <text class="logo-text">道德经</text>
        <text class="logo-subtitle">修身养性 悟道参禅</text>
      </view>

      <view class="form-section">
        <view class="input-group">
          <text class="icon">👤</text>
          <input
            v-model="formData.username"
            placeholder="请输入用户名"
            class="input-field"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="input-group">
          <text class="icon">🔒</text>
          <input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            class="input-field"
            placeholder-class="input-placeholder"
          />
        </view>

        <button
          class="login-btn"
          :class="{ 'btn-disabled': !canSubmit }"
          :disabled="!canSubmit || loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <view class="register-tip">
          <text>还没有账号？</text>
          <text class="link" @click="switchToRegister">立即注册</text>
        </view>
      </view>

      <!-- 注册表单 -->
      <view v-if="isRegisterMode" class="register-modal">
        <view class="modal-content">
          <view class="modal-header">
            <text class="modal-title">注册新账号</text>
            <text class="modal-close" @click="switchToRegister">×</text>
          </view>

          <view class="input-group">
            <text class="icon">👤</text>
            <input
              v-model="registerData.username"
              placeholder="请输入用户名"
              class="input-field"
              placeholder-class="input-placeholder"
            />
          </view>

          <view class="input-group">
            <text class="icon">🔒</text>
            <input
              v-model="registerData.password"
              type="password"
              placeholder="请输入密码"
              class="input-field"
              placeholder-class="input-placeholder"
            />
          </view>

          <view class="input-group">
            <text class="icon">📧</text>
            <input
              v-model="registerData.confirmPassword"
              type="password"
              placeholder="请确认密码"
              class="input-field"
              placeholder-class="input-placeholder"
            />
          </view>

          <button
            class="register-btn"
            :class="{ 'btn-disabled': !canRegister }"
            :disabled="!canRegister || loading"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </view>
      </view>
    </view>
    <!-- #endif -->

    <!-- 微信小程序端一键登录 -->
    <!-- #ifdef MP-WEIXIN -->
    <view class="mp-login">
      <view class="logo-section">
        <text class="logo-text">道德经</text>
        <text class="logo-subtitle">修身养性 悟道参禅</text>
      </view>

      <view class="mp-section">
        <text class="mp-tip">使用微信快速登录</text>

        <button
          class="wx-login-btn"
          open-type="getUserInfo"
          @getuserinfo="handleWxLogin"
        >
          <text class="wx-icon">📱</text>
          <text>微信一键登录</text>
        </button>

        <text class="guest-tip">登录后可发表评论和收藏章节</text>
      </view>

      <!-- 游客入口 -->
      <view class="guest-entry">
        <text class="guest-link" @click="handleGuestLogin">暂不登录，先浏览</text>
      </view>
    </view>
    <!-- #endif -->
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { login as loginApi, register as registerApi, wxLogin } from '@/api'

const userStore = useUserStore()

// H5登录表单数据
const formData = ref({
  username: '',
  password: ''
})

// 注册表单数据
const registerData = ref({
  username: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const isRegisterMode = ref(false)

// H5是否可以提交
const canSubmit = computed(() => {
  return formData.value.username.trim() && formData.value.password.trim()
})

// 是否可以注册
const canRegister = computed(() => {
  const { username, password, confirmPassword } = registerData.value
  return username.trim() && password.trim() && confirmPassword.trim() && password === confirmPassword
})

// H5登录
const handleLogin = async () => {
  if (!canSubmit.value || loading.value) return

  loading.value = true
  try {
    const result = await loginApi({
      username: formData.value.username.trim(),
      password: formData.value.password
    })

    userStore.login(result)

    uni.showToast({
      title: '登录成功',
      icon: 'success',
      duration: 1500
    })

    setTimeout(() => {
      uni.switchTab({
        url: '/pages/reading/index'
      })
    }, 1500)
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// H5注册
const handleRegister = async () => {
  if (!canRegister.value || loading.value) return

  const { username, password } = registerData.value

  loading.value = true
  try {
    const result = await registerApi({
      username: username.trim(),
      password: password
    })

    userStore.login(result)

    uni.showToast({
      title: '注册成功',
      icon: 'success',
      duration: 1500
    })

    setTimeout(() => {
      uni.switchTab({
        url: '/pages/reading/index'
      })
    }, 1500)
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}

// 切换注册模式
const switchToRegister = () => {
  isRegisterMode.value = !isRegisterMode.value
}

// 微信登录
// #ifdef MP-WEIXIN
const handleWxLogin = async (e) => {
  const { userInfo } = e.detail

  if (!userInfo) {
    uni.showToast({
      title: '需要授权登录',
      icon: 'none'
    })
    return
  }

  loading.value = true
  try {
    // 获取code
    const loginRes = await uni.login({
      provider: 'weixin'
    })

    const result = await wxLogin(loginRes.code)
    userStore.login(result)

    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })

    setTimeout(() => {
      uni.switchTab({
        url: '/pages/reading/index'
      })
    }, 1500)
  } catch (error) {
    console.error('微信登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 游客登录
const handleGuestLogin = () => {
  uni.switchTab({
    url: '/pages/reading/index'
  })
}
// #endif
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #0a0a0a 0%, #1a1a1a 100%);
  padding: 40rpx;
  box-sizing: border-box;
}

// H5端样式
// #ifdef H5
.h5-login {
  .logo-section {
    text-align: center;
    padding: 100rpx 0 80rpx;

    .logo-text {
      display: block;
      font-size: 60rpx;
      font-weight: bold;
      color: #d4af37;
      margin-bottom: 20rpx;
      letter-spacing: 8rpx;
    }

    .logo-subtitle {
      display: block;
      font-size: 28rpx;
      color: #888;
      letter-spacing: 4rpx;
    }
  }

  .form-section {
    .input-group {
      display: flex;
      align-items: center;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(212, 175, 55, 0.3);
      border-radius: 8rpx;
      padding: 30rpx;
      margin-bottom: 30rpx;

      .icon {
        font-size: 40rpx;
        margin-right: 20rpx;
        opacity: 0.7;
      }

      .input-field {
        flex: 1;
        font-size: 32rpx;
        color: #fff;
      }

      :deep(.input-placeholder) {
        color: #666;
      }
    }

    .login-btn {
      width: 100%;
      height: 90rpx;
      background: linear-gradient(135deg, #d4af37 0%, #aa8a2c 100%);
      border: none;
      border-radius: 8rpx;
      color: #000;
      font-size: 36rpx;
      font-weight: bold;
      margin-top: 60rpx;

      &.btn-disabled {
        opacity: 0.5;
      }

      &:not(.btn-disabled):active {
        opacity: 0.8;
      }
    }

    .register-tip {
      text-align: center;
      margin-top: 40rpx;
      font-size: 28rpx;
      color: #888;

      .link {
        color: #d4af37;
        margin-left: 10rpx;
      }
    }
  }

  .register-modal {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 999;

    .modal-content {
      background: #1a1a1a;
      border-radius: 16rpx;
      padding: 40rpx;
      width: 90%;
      max-width: 600rpx;
      border: 1px solid rgba(212, 175, 55, 0.3);

      .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 40rpx;

        .modal-title {
          font-size: 36rpx;
          font-weight: bold;
          color: #d4af37;
        }

        .modal-close {
          font-size: 60rpx;
          color: #666;
          line-height: 1;
        }
      }

      .register-btn {
        width: 100%;
        height: 90rpx;
        background: linear-gradient(135deg, #d4af37 0%, #aa8a2c 100%);
        border: none;
        border-radius: 8rpx;
        color: #000;
        font-size: 36rpx;
        font-weight: bold;
        margin-top: 40rpx;

        &.btn-disabled {
          opacity: 0.5;
        }
      }
    }
  }
}
// #endif

// 微信小程序样式
// #ifdef MP-WEIXIN
.mp-login {
  .logo-section {
    text-align: center;
    padding: 120rpx 0 100rpx;

    .logo-text {
      display: block;
      font-size: 64rpx;
      font-weight: bold;
      color: #d4af37;
      margin-bottom: 20rpx;
      letter-spacing: 8rpx;
    }

    .logo-subtitle {
      display: block;
      font-size: 28rpx;
      color: #888;
      letter-spacing: 4rpx;
    }
  }

  .mp-section {
    text-align: center;

    .mp-tip {
      display: block;
      font-size: 32rpx;
      color: #ccc;
      margin-bottom: 60rpx;
    }

    .wx-login-btn {
      width: 80%;
      height: 100rpx;
      background: linear-gradient(135deg, #07c160 0%, #06ae56 100%);
      border: none;
      border-radius: 50rpx;
      color: #fff;
      font-size: 36rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 40rpx;

      .wx-icon {
        font-size: 48rpx;
        margin-right: 16rpx;
      }

      &:active {
        opacity: 0.8;
      }
    }

    .guest-tip {
      display: block;
      font-size: 24rpx;
      color: #666;
    }
  }

  .guest-entry {
    text-align: center;
    margin-top: 80rpx;

    .guest-link {
      font-size: 28rpx;
      color: #d4af37;
    }
  }
}
// #endif
</style>

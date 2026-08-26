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
          <text class="material-symbols-outlined">person</text>
          <input
            v-model="formData.username"
            placeholder="请输入用户名"
            class="input-field"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="input-group">
          <text class="material-symbols-outlined">lock</text>
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
            <text class="material-symbols-outlined modal-close" @click="switchToRegister">close</text>
          </view>

          <view class="input-group">
            <text class="material-symbols-outlined">person</text>
            <input
              v-model="registerData.username"
              placeholder="请输入用户名"
              class="input-field"
              placeholder-class="input-placeholder"
            />
          </view>

          <view class="input-group">
            <text class="material-symbols-outlined">lock</text>
            <input
              v-model="registerData.password"
              type="password"
              placeholder="请输入密码"
              class="input-field"
              placeholder-class="input-placeholder"
            />
          </view>

          <view class="input-group">
            <text class="material-symbols-outlined">lock</text>
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
        url: '/pages/reading/home'
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
        url: '/pages/reading/home'
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
        url: '/pages/reading/home'
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
  background: radial-gradient(ellipse at center, rgb(40,40,40) 0%, #0e0e0e 100%);
  color: #e2e2e2;
  font-family: "KaiTi", "STKaiti", "KaiTi_GB2312", "Songti SC", serif;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
}

// H5端样式
// #ifdef H5
.h5-login {
  width: 100%;
  max-width: 480px;
  animation: fade-in-up 0.8s ease-out;
}

.logo-section {
  text-align: center;
  padding: 80rpx 0 60rpx;

  .logo-text {
    display: block;
    font-size: 48rpx;
    font-weight: 300;
    color: rgba(248, 246, 242, 0.9);
    margin-bottom: 16rpx;
    letter-spacing: 12rpx;
  }

  .logo-subtitle {
    display: block;
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.5);
    letter-spacing: 4rpx;
  }
}

.form-section {
  .input-group {
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 16px;
    padding: 28rpx 32rpx;
    margin-bottom: 24rpx;
    transition: all 0.3s ease;

    &:focus-within {
      border-color: rgba(212, 175, 55, 0.3);
      background: rgba(255, 255, 255, 0.05);
    }

    .icon {
      font-size: 36rpx;
      margin-right: 20rpx;
      opacity: 0.6;
    }

    .input-field {
      flex: 1;
      font-size: 30rpx;
      color: #e2e2e2;
      background: transparent;
      border: none;
      outline: none;
    }

    :deep(.input-placeholder) {
      color: rgba(255, 255, 255, 0.3);
    }
  }

  .login-btn {
    width: 100%;
    height: 96rpx;
    background: rgba(212, 175, 55, 0.1);
    border: 1px solid rgba(212, 175, 55, 0.3);
    border-radius: 16px;
    color: #d4af37;
    font-size: 32rpx;
    font-weight: 500;
    letter-spacing: 4rpx;
    margin-top: 48rpx;
    transition: all 0.3s ease;

    &.btn-disabled {
      opacity: 0.3;
    }

    &:not(.btn-disabled):active {
      background: rgba(212, 175, 55, 0.2);
      transform: scale(0.98);
    }
  }

  .register-tip {
    text-align: center;
    margin-top: 40rpx;
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.5);

    .link {
      color: #d4af37;
      margin-left: 8rpx;
      padding: 8rpx 16rpx;
      border-radius: 8px;
      background: rgba(212, 175, 55, 0.05);
      transition: all 0.2s;
    }

    .link:active {
      background: rgba(212, 175, 55, 0.1);
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
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 40rpx;
  animation: fade-in 0.3s ease;

  .modal-content {
    background: rgba(18, 18, 18, 0.95);
    border-radius: 20rpx;
    padding: 48rpx;
    width: 100%;
    max-width: 520rpx;
    border: 1px solid rgba(212, 175, 55, 0.2);
    backdrop-filter: blur(30px);
    -webkit-backdrop-filter: blur(30px);

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 40rpx;

      .modal-title {
        font-size: 36rpx;
        font-weight: 500;
        color: #d4af37;
        letter-spacing: 2rpx;
      }

      .modal-close {
        font-size: 56rpx;
        color: rgba(255, 255, 255, 0.4);
        line-height: 1;
        width: 56rpx;
        height: 56rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.05);
        transition: all 0.2s;
      }

      .modal-close:active {
        background: rgba(255, 255, 255, 0.1);
      }
    }

    .input-group {
      display: flex;
      align-items: center;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 16px;
      padding: 28rpx 32rpx;
      margin-bottom: 24rpx;
      transition: all 0.3s ease;

      &:focus-within {
        border-color: rgba(212, 175, 55, 0.3);
        background: rgba(255, 255, 255, 0.05);
      }

      .material-symbols-outlined {
        font-size: 36rpx;
        margin-right: 20rpx;
        opacity: 0.6;
        font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
      }

      .input-field {
        flex: 1;
        font-size: 30rpx;
        color: #e2e2e2;
        background: transparent;
        border: none;
        outline: none;
      }

      :deep(.input-placeholder) {
        color: rgba(255, 255, 255, 0.3);
      }
    }

    .register-btn {
      width: 100%;
      height: 96rpx;
      background: rgba(212, 175, 55, 0.1);
      border: 1px solid rgba(212, 175, 55, 0.3);
      border-radius: 16px;
      color: #d4af37;
      font-size: 32rpx;
      font-weight: 500;
      letter-spacing: 4rpx;
      margin-top: 40rpx;
      transition: all 0.3s ease;

      &.btn-disabled {
        opacity: 0.3;
      }

      &:not(.btn-disabled):active {
        background: rgba(212, 175, 55, 0.2);
        transform: scale(0.98);
      }
    }
  }
}
// #endif

// 微信小程序样式
// #ifdef MP-WEIXIN
.mp-login {
  width: 100%;
  max-width: 480px;
  animation: fade-in-up 0.8s ease-out;
}

.logo-section {
  text-align: center;
  padding: 120rpx 0 80rpx;

  .logo-text {
    display: block;
    font-size: 56rpx;
    font-weight: 300;
    color: rgba(248, 246, 242, 0.9);
    margin-bottom: 16rpx;
    letter-spacing: 12rpx;
  }

  .logo-subtitle {
    display: block;
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.5);
    letter-spacing: 4rpx;
  }
}

.mp-section {
  text-align: center;

  .mp-tip {
    display: block;
    font-size: 30rpx;
    color: rgba(255, 255, 255, 0.7);
    margin-bottom: 60rpx;
  }

  .wx-login-btn {
    width: 80%;
    height: 104rpx;
    background: rgba(7, 193, 96, 0.1);
    border: 1px solid rgba(7, 193, 96, 0.3);
    border-radius: 52rpx;
    color: #07c160;
    font-size: 32rpx;
    font-weight: 500;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 40rpx;
    transition: all 0.3s ease;

    .wx-icon {
      font-size: 44rpx;
      margin-right: 16rpx;
    }

    &:active {
      background: rgba(7, 193, 96, 0.2);
      transform: scale(0.98);
    }
  }

  .guest-tip {
    display: block;
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.4);
  }
}

.guest-entry {
  text-align: center;
  margin-top: 80rpx;

  .guest-link {
    font-size: 28rpx;
    color: rgba(212, 175, 55, 0.7);
    padding: 12rpx 32rpx;
    border-radius: 50px;
    background: rgba(212, 175, 55, 0.05);
    transition: all 0.2s;
  }

  .guest-link:active {
    background: rgba(212, 175, 55, 0.1);
  }
}
// #endif
</style>

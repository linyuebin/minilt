// 基础API配置
// H5：开发期走 vite 代理（相对路径 /api），生产走线上域名
// 小程序：没有代理，必须是完整地址；开发期需在微信开发者工具中勾选「不校验合法域名」
const DEV = process.env.NODE_ENV === 'development'
const PROD_API = 'https://api.daodejing.com/api'

// #ifdef H5
const BASE_URL = DEV ? '/api' : PROD_API
// #endif

// #ifdef APP-PLUS
// App 真机调试：localhost 指向手机自身，必须用电脑内网 IP；电脑换网络后同步更新
const BASE_URL = DEV ? 'http://10.178.150.7:8080/api' : PROD_API
// #endif

// #ifndef H5 || APP-PLUS
const BASE_URL = DEV ? 'http://localhost:8080/api' : PROD_API
// #endif

// 请求封装
export const request = (options) => {
  return new Promise((resolve, reject) => {
    // 获取token
    const token = uni.getStorageSync('token')

    // 合并请求配置
    const config = {
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...options.header
      },
      timeout: options.timeout || 15000
    }

    // 添加token
    if (token) {
      config.header.Authorization = `Bearer ${token}`
    }

    // 发起请求
    uni.request({
      ...config,
      success: (res) => {
        const { code, msg, data } = res.data

        // 成功响应
        if (code === 0) {
          resolve(data)
        }
        // 未登录
        else if (code === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')

          // 跳转到登录页
          uni.navigateTo({
            url: '/pages/login/index'
          })

          reject(new Error(msg || '请先登录'))
        }
        // 其他错误
        else {
          uni.showToast({
            title: msg || '请求失败',
            icon: 'none',
            duration: 2000
          })
          reject(new Error(msg || '请求失败'))
        }
      },
      fail: (err) => {
        console.error('请求失败:', err)
        uni.showToast({
          title: '网络请求失败',
          icon: 'none',
          duration: 2000
        })
        reject(err)
      }
    })
  })
}

// GET请求
export const get = (url, data = {}) => {
  return request({
    url,
    method: 'GET',
    data
  })
}

// POST请求
export const post = (url, data = {}) => {
  return request({
    url,
    method: 'POST',
    data
  })
}

// PUT请求
export const put = (url, data = {}) => {
  return request({
    url,
    method: 'PUT',
    data
  })
}

// DELETE请求
export const del = (url, data = {}) => {
  return request({
    url,
    method: 'DELETE',
    data
  })
}

export default request

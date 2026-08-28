import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { validateBgPath, releaseBgFile } from '@/utils/background'

// 五种配色方案（PRD §2.3.2）：preview 用于设置面板色块，charColor 为意象字颜色
export const THEME_SCHEMES = [
  { id: 'ink', char: '墨', name: '经典水墨', preview: 'linear-gradient(150deg, #282828 0%, #0e0e0e 100%)', charColor: '#d4af37' },
  { id: 'night', char: '夜', name: '深邃夜空', preview: 'linear-gradient(150deg, #171a33 0%, #080a18 100%)', charColor: '#8aa0ff' },
  { id: 'bamboo', char: '竹', name: '竹林清幽', preview: 'linear-gradient(150deg, #14231a 0%, #060d09 100%)', charColor: '#6fbf8f' },
  { id: 'purple', char: '紫', name: '紫气东来', preview: 'linear-gradient(150deg, #241530 0%, #0d0714 100%)', charColor: '#b28cf0' },
  { id: 'sea', char: '海', name: '沧海如水', preview: 'linear-gradient(150deg, #0e2432 0%, #050d14 100%)', charColor: '#5fb7d4' }
]
const SCHEME_IDS = THEME_SCHEMES.map(s => s.id)
const DEFAULT_SCHEME_ID = 'ink'

export const useThemeStore = defineStore('theme', () => {
  const schemeId = ref(DEFAULT_SCHEME_ID)
  const customBg = ref('')

  const themeClass = computed(() => 'theme-' + schemeId.value)
  const schemeName = computed(() => {
    const scheme = THEME_SCHEMES.find(s => s.id === schemeId.value)
    return scheme ? scheme.name : '经典水墨'
  })

  // 从本地存储恢复（同步读取防主题闪烁；旧版本非法值回落墨主题）
  const restore = () => {
    const savedScheme = uni.getStorageSync('colorScheme')
    schemeId.value = SCHEME_IDS.includes(savedScheme) ? savedScheme : DEFAULT_SCHEME_ID

    const savedBg = uni.getStorageSync('customBg')
    if (savedBg) {
      customBg.value = savedBg
      // 小程序本地文件可能因清缓存/升级失效，异步校验后静默清理
      validateBgPath(savedBg).then(ok => {
        if (!ok && customBg.value === savedBg) {
          clearCustomBg(true)
        }
      })
    }
  }

  const saveScheme = () => {
    uni.setStorageSync('colorScheme', schemeId.value)
  }

  // 切换配色（PRD：切换方案时清除自定义背景）
  const setScheme = (id) => {
    if (!SCHEME_IDS.includes(id) || id === schemeId.value) return
    if (customBg.value) {
      clearCustomBg()
    }
    schemeId.value = id
    saveScheme()
  }

  const saveBg = () => {
    try {
      uni.setStorageSync('customBg', customBg.value)
    } catch (e) {
      // H5 dataURL 超出存储配额：本次会话仍生效，提示但不中断
      uni.showToast({ title: '图片过大，保存失败', icon: 'none' })
    }
  }

  const setCustomBg = (value) => {
    if (!value) return
    if (customBg.value) {
      releaseBgFile(customBg.value)
    }
    customBg.value = value
    saveBg()
  }

  const clearCustomBg = (silent = false) => {
    if (customBg.value) {
      releaseBgFile(customBg.value)
    }
    customBg.value = ''
    try {
      uni.removeStorageSync('customBg')
    } catch (e) {
      if (!silent) console.log('清除背景存储失败:', e)
    }
  }

  // 重置为默认样式（PRD：清除方案与自定义背景；不动字体大小等阅读设置）
  const resetTheme = () => {
    schemeId.value = DEFAULT_SCHEME_ID
    saveScheme()
    clearCustomBg()
  }

  return {
    schemeId,
    customBg,
    themeClass,
    schemeName,
    restore,
    setScheme,
    setCustomBg,
    clearCustomBg,
    resetTheme
  }
})

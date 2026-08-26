<script>
import { useUserStore } from '@/store/user'

export default {
  onLaunch: function () {
    console.log('App Launch')

    // 检查登录状态
    const userStore = useUserStore()
    userStore.restoreState()

    // 检查当前页面是否是登录页面
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const currentPath = currentPage?.route || ''

    // 如果不是登录页面且用户未登录，则跳转到登录页
    if (currentPath !== 'pages/login/index' && !userStore.isLoggedIn()) {
      console.log('用户未登录，跳转到登录页')
      uni.reLaunch({
        url: '/pages/login/index'
      })
    }
  },
  onShow: function () {
    console.log('App Show')
  },
  onHide: function () {
    console.log('App Hide')
  },
}
</script>

<style>
@import "@/styles/global.css";
</style>

// 微信小程序自定义 tabBar（原生组件，uni-app 原样拷贝到 dist）
// 注意：此目录是小程序专属约定，不能放 .vue 文件（不会被编译，且缺失本组件会导致小程序白屏）
Component({
  data: {
    selected: 0,
    list: [
      { pagePath: 'pages/reading/home', text: '阅读', icon: '📖' },
      { pagePath: 'pages/community/index', text: '道友', icon: '👥' },
      { pagePath: 'pages/profile/index', text: '我的', icon: '👤' }
    ]
  },
  lifetimes: {
    attached() {
      this.syncSelected()
    }
  },
  pageLifetimes: {
    show() {
      this.syncSelected()
    }
  },
  methods: {
    // 按当前页面路由高亮对应 tab，避免每个 tab 页单独调 getTabBar().setData
    syncSelected() {
      const pages = getCurrentPages()
      if (!pages.length) return
      const route = pages[pages.length - 1].route || ''
      const index = this.data.list.findIndex(function (item) {
        return item.pagePath === route
      })
      if (index !== -1 && index !== this.data.selected) {
        this.setData({ selected: index })
      }
    },
    switchTab(e) {
      const index = e.currentTarget.dataset.index
      const item = this.data.list[index]
      wx.switchTab({ url: '/' + item.pagePath })
    }
  }
})

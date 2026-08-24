import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import Comments from '../views/Comments.vue'
import Users from '../views/Users.vue'

// 后台只有四个页面，静态引入即可，省掉懒加载的异步分支
const routes = [
  { path: '/login', name: 'login', component: Login, meta: { public: true } },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: Dashboard, meta: { title: '数据看板' } },
      { path: 'comments', name: 'comments', component: Comments, meta: { title: '评论管理' } },
      { path: 'users', name: 'users', component: Users, meta: { title: '用户列表' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// token 守卫：未登录一律打回登录页，已登录再访问登录页则回看板
router.beforeEach((to) => {
  const authed = !!localStorage.getItem('admin_token')
  if (!to.meta.public && !authed) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (to.path === '/login' && authed) return { path: '/dashboard' }
  return true
})

export default router

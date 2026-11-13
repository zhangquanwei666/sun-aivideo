import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { title: '管理员登录', public: true } },
  {
    path: '/',
    component: () => import('@/components/AppShell.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '总览' } },
      { path: 'users', component: () => import('@/views/UsersView.vue'), meta: { title: '用户管理' } },
      { path: 'records', component: () => import('@/views/RecordsView.vue'), meta: { title: '生成记录' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(to => {
  if (!to.meta.public && !localStorage.getItem('admin-token')) return '/login'
  if (to.path === '/login' && localStorage.getItem('admin-token')) return '/dashboard'
})
router.afterEach(to => { document.title = `${to.meta.title || '管理后台'} · Sun Studio` })

export default router

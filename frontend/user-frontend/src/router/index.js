import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/model-keys', component: () => import('@/views/ChannelKeysView.vue'), meta: { title: '模型密钥' } },
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
  { path: '/', redirect: '/generate' },
  {
    path: '/generate',
    name: 'generate',
    component: () => import('@/views/GenerateView.vue'),
    meta: { title: '生成视频' }
  },
  {
    path: '/usage',
    name: 'usage',
    component: () => import('@/views/UsageView.vue'),
    meta: { title: '用量与费用' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(to => {
  if (to.path !== '/login' && !localStorage.getItem('aivideo-token')) return '/login'
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Sun AI Video` : 'Sun AI Video'
})

export default router

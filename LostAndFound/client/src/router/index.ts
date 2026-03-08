import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const routes = [
  { path: '/', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { hideNav: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { hideNav: true } },
  { path: '/guide', component: () => import('../views/Guide.vue') },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { auth: true } },
  { path: '/my-claims', component: () => import('../views/MyClaims.vue'), meta: { auth: true } },
  { path: '/publish/lost/:id?', component: () => import('../views/LostPublish.vue'), meta: { auth: true } },
  { path: '/publish/found', component: () => import('../views/FoundPublish.vue'), meta: { auth: true } },
  { path: '/query', component: () => import('../views/Query.vue'), meta: { auth: true } },
  { path: '/lost-items', component: () => import('../views/LostItems.vue'), meta: { auth: true } },
  { path: '/messages', component: () => import('../views/MessageCenter.vue'), meta: { auth: true } },
  { path: '/detail/:type/:id', component: () => import('../views/Detail.vue'), meta: { auth: true } },
  { path: '/admin', component: () => import('../views/AdminLayout.vue'), meta: { auth: true, roles: ['ROLE_ADMIN','ROLE_MANAGER'] },
    children: [
      { path: 'users', component: () => import('../views/UserManage.vue') },
      { path: 'audit', component: () => import('../views/ItemAudit.vue') },
      { path: 'notice', component: () => import('../views/NoticeManage.vue') },
      { path: 'stats', component: () => import('../views/StatDashboard.vue') },
      { path: 'map', component: () => import('../views/MapManage.vue'), meta: { roles: ['ROLE_ADMIN'] } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = (store as any).state.auth?.token
  const user = (store as any).state.auth?.user
  const meta = to.meta as any
  if (meta?.auth && !token) return next('/login')
  const required: string[] | undefined = meta?.roles
  if (required && user) {
    const hasSingle = !!user.role && required.includes(user.role)
    const hasArray = Array.isArray(user.roles) && user.roles.some((r: any) => required.includes(r.code))
    if (!hasSingle && !hasArray) return next('/login')
  }
  next()
})

export default router

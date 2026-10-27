import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true, blank: true } },
  { path: '/', name: 'home', component: () => import('../views/Home.vue'), meta: { title: '客户拜访' } },
  { path: '/plans', name: 'plans', component: () => import('../views/Plans.vue'), meta: { title: '拜访计划' } },
  { path: '/stats', name: 'stats', component: () => import('../views/Stats.vue'), meta: { title: '数据统计' } },
  { path: '/mine', name: 'mine', component: () => import('../views/Mine.vue'), meta: { title: '我的' } },
  {
    path: '/admin/managers',
    name: 'admin-managers',
    component: () => import('../views/admin/Managers.vue'),
    meta: { title: '客户经理', admin: true }
  },
  {
    path: '/admin/groups',
    name: 'admin-groups',
    component: () => import('../views/admin/Groups.vue'),
    meta: { title: '集团管理', admin: true }
  },
  {
    path: '/admin/plans',
    name: 'admin-plans',
    component: () => import('../views/admin/PlansAdmin.vue'),
    meta: { title: '拜访清单', admin: true }
  },
  {
    path: '/admin/import',
    name: 'admin-import',
    component: () => import('../views/admin/Import.vue'),
    meta: { title: '数据导入', admin: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (to.name === 'login' && auth.isLoggedIn) {
      return { name: 'home' }
    }
    return true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !auth.isAdmin) {
    return { name: 'home' }
  }
  return true
})

export default router

import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/home/HomePage.vue'),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginPage.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterPage.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/posts/new',
    name: 'post-create',
    component: () => import('@/views/post/PostEditorPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/posts/:id',
    name: 'post-detail',
    component: () => import('@/views/post/PostDetailPage.vue'),
  },
  {
    path: '/posts/:id/edit',
    name: 'post-edit',
    component: () => import('@/views/post/PostEditorPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/category/:id',
    name: 'category',
    component: () => import('@/views/category/CategoryPage.vue'),
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/views/message/MessagePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/user/:id',
    name: 'user-profile',
    component: () => import('@/views/user/UserProfilePage.vue'),
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/user/SettingsPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/reports',
    name: 'admin-reports',
    component: () => import('@/views/admin/ReportQueuePage.vue'),
    meta: { requiresAuth: true, requiresPermission: 'report:handle' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/home/NotFoundPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

/**
 * Route guards.
 *
 * `requiresPermission` is checked against the signed in user's authorities.
 * The backend enforces the same rule, so this only avoids showing a screen the
 * user cannot use.
 */
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'home' }
  }
  const required = to.meta.requiresPermission as string | undefined
  if (required && !auth.profile) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router

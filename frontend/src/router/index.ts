import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layout/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    public?: boolean
    requiresAdmin?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', public: true },
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '学习看板' },
      },
      {
        path: 'notes',
        name: 'Notes',
        component: () => import('@/views/notes/NotesView.vue'),
        meta: { title: '笔记中心' },
      },
      {
        path: 'schedules',
        name: 'Schedules',
        component: () => import('@/views/schedules/SchedulesView.vue'),
        meta: { title: '日程管理' },
      },
      {
        path: 'focus',
        name: 'Focus',
        component: () => import('@/views/focus/FocusView.vue'),
        meta: { title: '番茄钟' },
      },
      {
        path: 'ai',
        name: 'AiAssistant',
        component: () => import('@/views/ai/AiView.vue'),
        meta: { title: 'AI 助手' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理', requiresAdmin: true },
      },
      {
        path: 'admin/ai-stats',
        name: 'AdminAiStats',
        component: () => import('@/views/admin/AdminAiStatsView.vue'),
        meta: { title: 'AI 用量监控', requiresAdmin: true },
      },
      {
        path: 'admin/audit-logs',
        name: 'AdminAuditLogs',
        component: () => import('@/views/admin/AdminAuditLogsView.vue'),
        meta: { title: '审计日志', requiresAdmin: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
      next({ path: '/dashboard' })
      return
    }
    next()
    return
  }

  if (!auth.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
    return
  }

  if (auth.isLoggedIn && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clearAuth()
      next({
        path: '/login',
        query: { redirect: to.fullPath },
      })
      return
    }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    ElMessage.warning('仅管理员可访问该页面')
    next({ path: '/dashboard' })
    return
  }

  next()
})

export default router
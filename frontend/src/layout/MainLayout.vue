<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UpcomingReminders from '@/views/schedules/components/UpcomingReminders.vue'
import {
  ArrowDown,
  Calendar,
  Clock,
  DataAnalysis,
  Document,
  Expand,
  Fold,
  MagicStick,
  Monitor,
  Setting,
  SwitchButton,
  TrendCharts,
  User,
  UserFilled,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string) ?? '循记')
const roleLabel = computed(() => (auth.isAdmin ? '管理员' : '普通用户'))

async function handleCommand(command: string) {
  if (command === 'profile') {
    await router.push('/profile')
    return
  }
  if (command === 'logout') {
    await auth.logout()
    await router.push('/login')
  }
}
</script>

<template>
  <el-container class="main-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
      <div class="brand">
        <el-icon :size="22"><Monitor /></el-icon>
        <span v-show="!collapsed" class="brand-text">循记</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        background-color="#0f172a"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        router
        class="side-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>学习看板</template>
        </el-menu-item>
        <el-menu-item index="/notes">
          <el-icon><Document /></el-icon>
          <template #title>笔记中心</template>
        </el-menu-item>
        <el-menu-item index="/schedules">
          <el-icon><Calendar /></el-icon>
          <template #title>日程管理</template>
        </el-menu-item>
        <el-menu-item index="/focus">
          <el-icon><Clock /></el-icon>
          <template #title>番茄钟</template>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><MagicStick /></el-icon>
          <template #title>AI 助手</template>
        </el-menu-item>
        <el-sub-menu v-if="auth.isAdmin" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>管理后台</span>
          </template>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/ai-stats">
            <el-icon><TrendCharts /></el-icon>
            <template #title>AI 用量监控</template>
          </el-menu-item>
          <el-menu-item index="/admin/audit-logs">
            <el-icon><Document /></el-icon>
            <template #title>审计日志</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="content-wrap">
      <el-header class="top-header">
        <div class="header-left">
          <el-button text @click="collapsed = !collapsed">
            <el-icon :size="18"><component :is="collapsed ? Expand : Fold" /></el-icon>
          </el-button>
          <span class="header-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <UpcomingReminders :compact="true" />
          <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-trigger">
            <el-avatar :size="32" :src="auth.user?.avatar || undefined">
              {{ (auth.user?.nickname || auth.user?.username || '?').charAt(0) }}
            </el-avatar>
            <span class="user-name">{{ auth.user?.nickname || auth.user?.username }}</span>
            <el-tag size="small" :type="auth.isAdmin ? 'danger' : 'primary'" effect="plain">{{ roleLabel }}</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-body">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.sidebar {
  background: var(--ssp-sidebar);
  transition: width 0.2s ease;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-text {
  font-weight: 600;
  font-size: 1rem;
  white-space: nowrap;
}

.side-menu {
  border-right: none;
}

.side-menu:not(.el-menu--collapse) {
  width: 220px;
}

.content-wrap {
  min-width: 0;
}

.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  background: var(--ssp-surface);
  border-bottom: 1px solid var(--ssp-border);
  padding: 0 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 1.05rem;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 0.95rem;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-body {
  padding: 0;
  background: var(--ssp-bg);
}
</style>
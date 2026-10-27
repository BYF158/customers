<template>
  <router-view v-if="route.meta.blank" />

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">访</div>
        <div>
          <div class="name">客户拜访管理</div>
          <div class="desc">Customer Visit</div>
        </div>
      </div>

      <nav class="side-nav">
        <a v-for="item in mainNav" :key="item.path" :class="{ on: isActive(item.path) }" @click="go(item.path)">
          <van-icon :name="item.icon" />
          <span>{{ item.text }}</span>
        </a>
        <template v-if="auth.isAdmin">
          <div class="group-title">管理</div>
          <a v-for="item in adminNav" :key="item.path" :class="{ on: isActive(item.path) }" @click="go(item.path)">
            <van-icon :name="item.icon" />
            <span>{{ item.text }}</span>
          </a>
        </template>
      </nav>

      <div class="side-user">
        <div class="avatar">{{ auth.displayName.slice(0, 1) }}</div>
        <div class="grow">
          <div style="font-size: 14px; font-weight: 650">{{ auth.displayName }}</div>
          <div class="mini">{{ auth.roleText }}</div>
        </div>
        <van-icon name="sign" style="color: var(--text-3); cursor: pointer" @click="logout" />
      </div>
    </aside>

    <div>
      <header class="mobile-topbar">
        <div class="grow">
          <div class="title">{{ route.meta.title || '客户拜访' }}</div>
          <div class="sub">{{ auth.displayName }} · {{ auth.roleText }}</div>
        </div>
        <div class="avatar-badge" @click="go('/mine')">{{ auth.displayName.slice(0, 1) }}</div>
      </header>

      <main class="app-main">
        <router-view />
      </main>
    </div>

    <nav class="tabbar">
      <a v-for="item in mainNav" :key="item.path" :class="{ on: isActive(item.path) }" @click="go(item.path)">
        <van-icon class="pi" :name="item.icon" />
        <span>{{ item.text }}</span>
      </a>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const mainNav = [
  { path: '/', text: '首页', icon: 'wap-home-o' },
  { path: '/plans', text: '拜访', icon: 'calendar-o' },
  { path: '/stats', text: '统计', icon: 'bar-chart-o' },
  { path: '/mine', text: '我的', icon: 'user-o' }
]

const adminNav = [
  { path: '/admin/managers', text: '客户经理', icon: 'friends-o' },
  { path: '/admin/groups', text: '集团管理', icon: 'shop-o' },
  { path: '/admin/plans', text: '拜访清单', icon: 'orders-o' },
  { path: '/admin/import', text: '数据导入', icon: 'upgrade' }
]

const isActive = (path) => (path === '/' ? route.path === '/' : route.path.startsWith(path))

const go = (path) => router.push(path)

const logout = async () => {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定退出当前账号吗？' })
    auth.logout()
    router.replace('/login')
  } catch (e) {
    // 用户取消
  }
}
</script>

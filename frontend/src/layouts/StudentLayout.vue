<script setup lang="ts">
import { ref, computed, h, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import {
  NLayoutSider, NLayoutHeader,
  NButton, NSpace, NIcon, NMenu,
} from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import {
  SunnyOutline, MoonOutline, LogOutOutline,
  BookOutline, HomeOutline, DocumentTextOutline,
} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()
const isDark = computed(() => theme.isDark)

function renderIcon(icon: Component) { return () => h(NIcon, null, () => h(icon)) }

const menuOptions: MenuOption[] = [
  { label: '我的课程', key: '/student/home', icon: renderIcon(BookOutline) },
  { label: '学习评价', key: '/student/evaluation', icon: renderIcon(HomeOutline) },
  { label: '我的网盘', key: '/student/drive', icon: renderIcon(BookOutline) },
]

const activeKey = computed(() => {
  const path = route.path
  if (path.startsWith('/student/drive')) return '/student/drive'
  if (path.startsWith('/student/evaluation')) return '/student/evaluation'
  if (path.startsWith('/student/home') || path.startsWith('/student/courses')) return '/student/home'
  return path
})

function handleMenuChange(key: string) { router.push(key) }

function handleLogout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="layout-root">
    <NLayoutSider
      bordered
      :width="200"
      :style="{ background: isDark ? '#1a1a18' : '#f5f4f1' }"
    >
      <div class="sider-top">
        <span class="brand" :style="{ color: isDark ? '#e8e6e1' : '#1a1a18' }">
          <span class="brand-dot" />信息科技课堂
        </span>
      </div>
      <NMenu
        :value="activeKey"
        :options="menuOptions"
        @update:value="handleMenuChange"
      />
      <div class="sider-footer" :style="{ borderColor: isDark ? '#272725' : '#eae8e4', color: isDark ? '#8a8a84' : '#4a4a44' }">
        <div class="user-avatar" :style="{ background: isDark ? '#272725' : '#e8e6e1' }">{{ auth.userInfo?.name?.charAt(0) }}</div>
        <span class="user-name">{{ auth.userInfo?.name }}</span>
      </div>
    </NLayoutSider>

    <div class="layout-right">
      <NLayoutHeader bordered class="top-header">
        <div class="header-left">
          <span class="section-label" :style="{ color: isDark ? '#8a8a84' : '#6b6b65' }">{{ auth.userInfo?.name }}</span>
        </div>
        <NSpace align="center" :size="2">
          <NButton quaternary circle size="small" @click="theme.toggleTheme()">
            <template #icon>
              <NIcon :size="18"><SunnyOutline v-if="isDark" /><MoonOutline v-else /></NIcon>
            </template>
          </NButton>
          <NButton quaternary circle size="small" @click="handleLogout">
            <template #icon>
              <NIcon :size="18"><LogOutOutline /></NIcon>
            </template>
          </NButton>
        </NSpace>
      </NLayoutHeader>
      <div class="main-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout-root { display: flex; min-height: 100vh; }
.layout-right { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.sider-top {
  padding: 16px 20px;
  display: flex;
  align-items: center;
}
.brand { font-size: 14px; font-weight: 600; letter-spacing: -0.01em; display: flex; align-items: center; gap: 8px; transition: color 0.2s ease; }
.brand-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--n-primary-color); display: inline-block; flex-shrink: 0; }
.top-header {
  height: 52px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.section-label { font-size: 13px; font-weight: 500; transition: color 0.2s ease; }
.main-content { padding: 28px 32px; flex: 1; overflow: auto; display: flex; flex-direction: column; width: 100%; }
.main-content > :deep(*) { flex: 1; min-height: 100%; }
.main-content :deep(.page) { max-width: none; }
.sider-footer { padding: 12px 18px; border-top: 1px solid; display: flex; align-items: center; gap: 10px; margin-top: auto; }
.user-avatar { width: 28px; height: 28px; border-radius: 7px; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; flex-shrink: 0; }
.user-name { font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>

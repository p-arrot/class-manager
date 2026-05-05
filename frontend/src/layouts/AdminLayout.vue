<script setup lang="ts">
import { h, type Component, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import {
  NLayout, NLayoutSider, NLayoutHeader, NLayoutContent,
  NMenu, NButton, NSpace, NIcon, type MenuOption
} from 'naive-ui'
import {
  BookOutline, PeopleOutline, SchoolOutline,
  SunnyOutline, MoonOutline, LogOutOutline,
} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()

const isDark = computed(() => theme.isDark)

function renderIcon(icon: Component) {
  return () => h(NIcon, { size: 18 }, { default: () => h(icon) })
}

const menuOptions: MenuOption[] = [
  { label: '班级管理', key: '/admin/classes', icon: renderIcon(SchoolOutline) },
  { label: '教师管理', key: '/admin/teachers', icon: renderIcon(PeopleOutline) },
  { label: '学生管理', key: '/admin/students', icon: renderIcon(BookOutline) },
]

const activeKey = computed(() => {
  if (route.path.startsWith('/admin/classes')) return '/admin/classes'
  if (route.path.startsWith('/admin/teachers')) return '/admin/teachers'
  if (route.path.startsWith('/admin/students')) return '/admin/students'
  return route.path
})

function handleMenuClick(key: string) {
  router.push(key)
}

function handleLogout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <NLayout has-sider style="min-height: 100vh">
    <NLayoutSider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="230"
      :native-scrollbar="false"
    >
      <div class="sider-header">
        <div class="sider-logo">
          <span class="logo-dot" :style="{ background: isDark ? '#e8e6e1' : '#1a1a18' }" />
          <span class="logo-text" :style="{ color: isDark ? '#e8e6e1' : '#1a1a18' }">课堂管理</span>
        </div>
      </div>

      <div class="sider-nav">
        <NMenu
          :value="activeKey"
          :options="menuOptions"
          @update:value="handleMenuClick"
        />
      </div>

      <div
        class="sider-footer"
        :style="{ borderColor: isDark ? '#272725' : '#eae8e4' }"
      >
        <div class="user-line">
          <span
            class="user-avatar"
            :style="{ background: isDark ? '#272725' : '#e8e6e1', color: isDark ? '#b0b0a8' : '#4a4a44' }"
          >{{ auth.userInfo?.name?.charAt(0) }}</span>
          <span class="user-name" :style="{ color: isDark ? '#8a8a84' : '#4a4a44' }">
            {{ auth.userInfo?.name }}
          </span>
        </div>
        <div class="credit" :style="{ color: isDark ? '#5a5a54' : '#b0b0a8' }">Tatakai</div>
      </div>
    </NLayoutSider>

    <NLayout>
      <NLayoutHeader bordered class="top-header">
        <span style="font-size:13px;font-weight:500;color:var(--n-text-color-2)">管理员工作台</span>
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
      <NLayoutContent class="main-content">
        <router-view />
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>

<style scoped>
.sider-header {
  padding: 20px 20px 12px;
}
.sider-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.2s ease;
}
.logo-text {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  transition: color 0.2s ease;
}
.sider-nav {
  padding: 8px 12px;
  flex: 1;
}
.sider-footer {
  padding: 12px 20px 20px;
  border-top: 1px solid;
  transition: border-color 0.2s ease;
}
.user-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  transition: background 0.2s ease, color 0.2s ease;
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
}
.credit {
  margin-top: 10px;
  font-size: 11px;
  font-weight: 450;
  letter-spacing: 0.03em;
  transition: color 0.2s ease;
}
.top-header {
  height: 52px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.main-content {
  padding: 28px 32px;
  width: 100%;
}
.main-content :deep(.page) { max-width: none; }
</style>

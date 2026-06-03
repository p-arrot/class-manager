<script setup lang="ts">
import { ref, computed, h, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useClassFilterStore } from '@/stores/classFilter'
import {
  NLayoutSider, NLayoutHeader,
  NButton, NSpace, NIcon, NMenu, NSelect,
} from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { listAllClasses } from '@/api/classes'
import type { ClassVO } from '@/types/api'
import {
  SunnyOutline, MoonOutline, LogOutOutline,
  HomeOutline, BookOutline, DocumentTextOutline, PeopleOutline,
} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()
const classFilter = useClassFilterStore()
const isDark = computed(() => theme.isDark)
const collapsed = ref(false)

const classes = ref<ClassVO[]>([])
listAllClasses().then(list => { classes.value = list }).catch(() => {})
const selectedClassId = computed({
  get: () => classFilter.selectedClassId,
  set: (v) => classFilter.setClassId(v),
})

function renderIcon(icon: Component) { return () => h(NIcon, null, () => h(icon)) }

const menuOptions: MenuOption[] = [
  { label: '工作台', key: '/teacher/home', icon: renderIcon(HomeOutline) },
  { label: '我的课程', key: '/teacher/courses', icon: renderIcon(BookOutline) },
  { label: '学生管理', key: '/teacher/students', icon: renderIcon(PeopleOutline) },
]

const activeKey = computed(() => {
  const path = route.path
  if (path.startsWith('/teacher/courses')) return '/teacher/courses'
  if (path.startsWith('/teacher/students')) return '/teacher/students'
  if (path.startsWith('/teacher/home')) return '/teacher/home'
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
      :collapsed="collapsed"
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :style="{ background: isDark ? '#1a1a18' : '#f5f4f1' }"
    >
      <div class="sider-top" :class="{ collapsed }">
        <span v-if="!collapsed" class="brand" :style="{ color: isDark ? '#e8e6e1' : '#1a1a18' }">
          <span class="brand-dot" />课堂管理
        </span>
        <span v-else class="brand-collapsed" :style="{ color: isDark ? '#e8e6e1' : '#1a1a18' }">
          <span class="brand-dot" />
        </span>
      </div>
      <NMenu
        :value="activeKey"
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="20"
        :options="menuOptions"
        @update:value="handleMenuChange"
      />
      <div class="sider-footer" :class="{ collapsed }">
        <div class="user-avatar" :style="{ background: isDark ? '#272725' : '#e8e6e1' }">
          {{ auth.userInfo?.name?.charAt(0) || 'T' }}
        </div>
        <span v-if="!collapsed" class="user-name" :style="{ color: isDark ? '#8a8a84' : '#6b6b65' }">
          {{ auth.userInfo?.name }}
        </span>
      </div>
    </NLayoutSider>

    <div class="layout-right">
      <NLayoutHeader bordered class="top-header">
        <div class="header-left">
          <span class="section-label" :style="{ color: isDark ? '#8a8a84' : '#6b6b65' }">教师工作台</span>
          <NSelect
            v-model:value="selectedClassId"
            :options="classes.map(c => ({ label: c.grade + '级' + c.name, value: c.id }))"
            placeholder="筛选班级"
            clearable
            style="width: 180px"
            size="small"
          />
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
.sider-top.collapsed { padding: 16px 0; justify-content: center; }
.brand { font-size: 14px; font-weight: 600; letter-spacing: -0.01em; display: flex; align-items: center; gap: 8px; transition: color 0.2s ease; }
.brand-collapsed { display: flex; align-items: center; }
.brand-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--n-primary-color); display: inline-block; flex-shrink: 0; }
.sider-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid var(--n-border-color);
}
.sider-footer.collapsed { justify-content: center; padding: 12px 0; }
.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-name { font-size: 13px; font-weight: 500; transition: color 0.2s ease; }
.top-header {
  height: 52px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.section-label { font-size: 13px; font-weight: 500; transition: color 0.2s ease; }
.main-content { padding: 28px 32px; flex: 1; overflow: auto; width: 100%; }
.main-content :deep(.page) { max-width: none; }
</style>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import {
  NLayout, NLayoutHeader, NLayoutContent,
  NButton, NSpace, NIcon,
} from 'naive-ui'
import { SunnyOutline, MoonOutline, LogOutOutline } from '@vicons/ionicons5'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const isDark = computed(() => theme.isDark)

function handleLogout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <NLayout style="min-height: 100vh">
    <NLayoutHeader bordered class="top-header">
      <div class="header-left">
        <span class="brand" :style="{ color: isDark ? '#e8e6e1' : '#1a1a18' }">信息科技课堂</span>
      </div>
      <NSpace align="center" :size="2">
        <span class="user-tag" :style="{ color: isDark ? '#8a8a84' : '#6b6b65' }">
          {{ auth.userInfo?.name }}
        </span>
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
</template>

<style scoped>
.top-header {
  height: 52px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.brand {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  transition: color 0.2s ease;
}
.user-tag {
  font-size: 13px;
  font-weight: 500;
  margin-right: 4px;
  transition: color 0.2s ease;
}
.main-content {
  padding: 28px 32px;
  max-width: 1200px;
}
</style>

<script setup lang="ts">
import { computed, ref, watch, h, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NDrawer,
  NDrawerContent,
  NIcon,
  NLayout,
  NLayoutContent,
  NLayoutHeader,
  NLayoutSider,
  NMenu,
  NSpace,
  NTooltip,
  type MenuOption,
} from 'naive-ui'
import { MenuOutline, SunnyOutline, MoonOutline, LogOutOutline } from '@vicons/ionicons5'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

interface ShellMenuItem {
  label: string
  key: string
  icon: Component
  match?: string[]
  children?: Omit<ShellMenuItem, 'icon'>[]
}

const props = defineProps<{
  brand: string
  section: string
  menu: ShellMenuItem[]
}>()

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()
const isDark = computed(() => theme.isDark)
const collapsed = ref(false)
const drawerVisible = ref(false)

function renderIcon(icon: Component) {
  return () => h(NIcon, null, () => h(icon))
}

const menuOptions = computed<MenuOption[]>(() =>
  props.menu.map(item => ({
    label: item.label,
    key: item.key,
    icon: renderIcon(item.icon),
    children: item.children,
  }))
)

const activeKey = computed(() => {
  const path = route.path
  const flatItems = props.menu.flatMap(item => [item, ...(item.children ?? [])])
  const current = flatItems.find(item => path === item.key || item.match?.some(prefix => path.startsWith(prefix)))
  return current?.key ?? path
})

watch(() => route.fullPath, () => {
  drawerVisible.value = false
})

function handleMenuChange(key: string) {
  router.push(key)
}

function handleLogout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <NLayout has-sider class="app-shell">
    <NLayoutSider
      class="desktop-sider"
      bordered
      collapse-mode="width"
      :collapsed="collapsed"
      :collapsed-width="64"
      :width="224"
      :native-scrollbar="false"
    >
      <div class="sider-inner">
        <div class="sider-top" :class="{ collapsed }">
          <span class="brand" :class="{ collapsed }">
            <span class="brand-dot" />
            <span v-if="!collapsed" class="brand-text">{{ brand }}</span>
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
          <div class="user-avatar">
            {{ auth.userInfo?.name?.charAt(0) || 'U' }}
          </div>
          <div v-if="!collapsed" class="user-meta">
            <span class="user-name">{{ auth.userInfo?.name || '用户' }}</span>
            <span class="user-role">{{ section }}</span>
          </div>
        </div>
      </div>
    </NLayoutSider>

    <NLayout class="layout-right">
      <NLayoutHeader bordered class="top-header">
        <div class="header-left">
          <NButton
            class="mobile-menu"
            quaternary
            circle
            size="small"
            title="打开导航"
            aria-label="打开导航"
            @click="drawerVisible = true"
          >
            <template #icon>
              <NIcon :size="19"><MenuOutline /></NIcon>
            </template>
          </NButton>
          <span class="section-label">{{ section }}</span>
          <slot name="header-left" />
        </div>
        <NSpace align="center" :size="4">
          <slot name="header-actions" />
          <NTooltip trigger="hover">
            <template #trigger>
              <NButton
                class="shell-action"
                quaternary
                circle
                size="small"
                :title="isDark ? '切换到浅色模式' : '切换到深色模式'"
                :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
                @click="theme.toggleTheme()"
              >
                <template #icon>
                  <NIcon :size="18"><SunnyOutline v-if="isDark" /><MoonOutline v-else /></NIcon>
                </template>
              </NButton>
            </template>
            {{ isDark ? '浅色模式' : '深色模式' }}
          </NTooltip>
          <NTooltip trigger="hover">
            <template #trigger>
              <NButton
                class="shell-action"
                quaternary
                circle
                size="small"
                title="退出登录"
                aria-label="退出登录"
                @click="handleLogout"
              >
                <template #icon>
                  <NIcon :size="18"><LogOutOutline /></NIcon>
                </template>
              </NButton>
            </template>
            退出登录
          </NTooltip>
        </NSpace>
      </NLayoutHeader>

      <NLayoutContent class="main-content">
        <router-view />
      </NLayoutContent>
    </NLayout>

    <NDrawer v-model:show="drawerVisible" placement="left" :width="280">
      <NDrawerContent body-content-style="padding: 0">
        <div class="drawer-top">
          <span class="brand">
            <span class="brand-dot" />
            <span class="brand-text">{{ brand }}</span>
          </span>
        </div>
        <div v-if="$slots['drawer-extra']" class="drawer-extra"><slot name="drawer-extra" /></div>
        <NMenu
          :value="activeKey"
          :options="menuOptions"
          @update:value="handleMenuChange"
        />
      </NDrawerContent>
    </NDrawer>
  </NLayout>
</template>

<style scoped>
.app-shell {
  min-height: 100dvh;
}

.desktop-sider {
  display: block;
}

.sider-inner {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}

.sider-top,
.drawer-top {
  padding: 18px 20px 14px;
  display: flex;
  align-items: center;
}

.sider-top.collapsed {
  justify-content: center;
  padding-inline: 0;
}

.brand {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--n-text-color-1);
  font-size: 15px;
  font-weight: 650;
}

.brand.collapsed {
  justify-content: center;
}

.brand-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--n-primary-color);
  flex: 0 0 auto;
}

.brand-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sider-footer {
  margin-top: auto;
  padding: 14px 18px 18px;
  border-top: 1px solid var(--n-border-color);
  display: flex;
  align-items: center;
  gap: 10px;
}

.sider-footer.collapsed {
  justify-content: center;
  padding-inline: 0;
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--n-color-embedded);
  color: var(--n-text-color-2);
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 650;
  flex: 0 0 auto;
}

.user-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 550;
  color: var(--n-text-color-1);
}

.user-role {
  font-size: 11px;
  color: var(--n-text-color-3);
}

.layout-right {
  min-width: 0;
}

.top-header {
  height: 54px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-left {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 550;
  color: var(--n-text-color-2);
}

.mobile-menu {
  display: none;
}

.main-content {
  width: 100%;
  padding: 28px 32px;
  overflow: auto;
}

.main-content :deep(.page) {
  max-width: none;
}

@media (max-width: 900px) {
  .app-shell {
    min-height: 100dvh;
  }

  .desktop-sider {
    display: none;
  }

  .mobile-menu {
    display: inline-flex;
    width: 44px;
    height: 44px;
  }

  .shell-action {
    width: 44px;
    height: 44px;
  }

  .top-header {
    height: 52px;
    padding: 0 14px;
  }

  .main-content {
    padding: 18px 14px 28px;
  }
}

.drawer-extra { padding: 4px 16px 12px; border-bottom: 1px solid var(--n-border-color); }
</style>

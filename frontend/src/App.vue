<script setup lang="ts">
import { useThemeStore } from '@/stores/theme'
import { NConfigProvider, NMessageProvider, NDialogProvider, NGlobalStyle } from 'naive-ui'

const themeStore = useThemeStore()
</script>

<template>
  <NConfigProvider :theme="themeStore.naiveTheme" :theme-overrides="themeStore.themeOverrides">
    <NGlobalStyle />
    <NMessageProvider>
      <NDialogProvider>
        <router-view />
      </NDialogProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>

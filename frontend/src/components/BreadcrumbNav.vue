<script setup lang="ts">
import { NButton, NIcon } from 'naive-ui'
import { ChevronForwardOutline, HomeOutline } from '@vicons/ionicons5'

defineProps<{ items: { label: string; to?: string }[] }>()
defineEmits<{ navigate: [idx: number] }>()
</script>

<template>
  <nav class="breadcrumb">
    <NButton text size="tiny" @click="$emit('navigate', -1)">
      <template #icon><NIcon :size="14"><HomeOutline /></NIcon></template>
    </NButton>
    <template v-for="(item, i) in items" :key="i">
      <NIcon :size="12" color="var(--n-text-color-3)"><ChevronForwardOutline /></NIcon>
      <NButton text size="tiny" @click="$emit('navigate', i)">{{ item.label }}</NButton>
    </template>
  </nav>
</template>

<style scoped>
.breadcrumb { display: flex; align-items: center; gap: 2px; margin-bottom: 16px; flex-wrap: wrap; }
</style>

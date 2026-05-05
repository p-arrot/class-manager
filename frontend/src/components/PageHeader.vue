<script setup lang="ts">
defineProps<{ title: string; hint?: string }>()
</script>

<template>
  <div class="page-head">
    <div>
      <h2 class="page-title">{{ title }}</h2>
      <p v-if="hint" class="page-hint">{{ hint }}</p>
    </div>
    <slot name="actions" />
  </div>
</template>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 600; letter-spacing: -0.01em; margin: 0 0 4px; }
.page-hint { font-size: 14px; color: var(--n-text-color-3); margin: 0; }
</style>

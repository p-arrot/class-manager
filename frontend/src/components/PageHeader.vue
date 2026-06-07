<script setup lang="ts">
const props = defineProps<{ title: string; hint?: string; subtitle?: string }>()
</script>

<template>
  <div class="page-head">
    <div>
      <h2 class="page-title">{{ title }}</h2>
      <p v-if="props.hint || props.subtitle" class="page-hint">{{ props.hint || props.subtitle }}</p>
    </div>
    <slot name="actions" />
  </div>
</template>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 600; margin: 0 0 4px; }
.page-hint { font-size: 14px; color: var(--n-text-color-3); margin: 0; }
</style>

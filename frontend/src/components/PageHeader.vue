<script setup lang="ts">
const props = defineProps<{ title: string; hint?: string; subtitle?: string }>()
</script>

<template>
  <div class="page-head">
    <div>
      <h2 class="page-title">{{ title }}</h2>
      <p v-if="props.hint || props.subtitle" class="page-hint">{{ props.hint || props.subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="page-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 600; margin: 0 0 4px; }
.page-hint { font-size: 14px; color: var(--n-text-color-3); margin: 0; }
.page-actions { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 8px; }
@media (max-width: 720px) {
  .page-head { flex-direction: column; gap: 12px; }
  .page-actions { width: 100%; justify-content: flex-start; }
}
</style>

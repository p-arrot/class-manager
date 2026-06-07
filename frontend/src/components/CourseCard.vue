<script setup lang="ts">
import { computed } from 'vue'
import { NCard, NTag, NEllipsis, NProgress } from 'naive-ui'
import { useThemeStore } from '@/stores/theme'
import type { CourseVO } from '@/types/api'

defineProps<{ course: CourseVO; progress?: { done: number; total: number } }>()
defineEmits<{ enter: [id: number] }>()

const theme = useThemeStore()
const isDark = computed(() => theme.isDark)
</script>

<template>
  <NCard size="small" class="course-card" hoverable @click="$emit('enter', course.id)">
    <div class="card-body">
      <div class="card-cover" :class="{ dark: isDark }">
        {{ course.name.charAt(0) }}
      </div>
      <div class="card-info">
        <h3 class="card-name"><NEllipsis>{{ course.name }}</NEllipsis></h3>
        <p class="card-teacher">{{ course.teacherName }}</p>
        <p class="card-desc">
          <NEllipsis v-if="course.description" :line-clamp="2">{{ course.description }}</NEllipsis>
          <span v-else class="card-desc-empty">&nbsp;</span>
        </p>
        <NTag size="tiny" :bordered="false">{{ course.classCount }} 个班级</NTag>
        <div v-if="progress && progress.total > 0" class="card-progress">
          <NProgress
            type="line"
            :percentage="Math.round(progress.done / progress.total * 100)"
            :height="5"
            :border-radius="3"
            :show-indicator="false"
            processing
          />
          <span class="progress-text">{{ progress.done }}/{{ progress.total }} 已完成</span>
        </div>
      </div>
    </div>
    <div class="card-actions" @click.stop>
      <slot name="actions" :course="course" />
    </div>
  </NCard>
</template>

<style scoped>
.course-card { cursor: pointer; transition: border-color 150ms ease, transform 150ms ease; display: flex; flex-direction: column; }
.course-card:hover { border-color: var(--n-primary-color-hover); transform: translateY(-1px); }
.card-body { display: flex; gap: 14px; align-items: flex-start; flex: 1; min-width: 0; }
.card-cover { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; flex-shrink: 0; background: #f0efeb; color: #5a5a54; }
.card-cover.dark { background: #272725; color: #b0b0a8; }
.card-info { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: flex-start; gap: 2px; }
.card-name { width: 100%; min-width: 0; font-size: 15px; font-weight: 600; margin: 0; line-height: 1.3; }
.card-name,
.card-name :deep(.n-ellipsis) { display: block; width: 100%; max-width: 100%; min-width: 0; }
.card-teacher { font-size: 12px; color: var(--n-text-color-3); margin: 0; line-height: 1.3; }
.card-desc { width: 100%; font-size: 12px; color: var(--n-text-color-2); margin: 2px 0 4px; line-height: 1.5; min-height: 36px; }
.card-desc-empty { visibility: hidden; }
.card-info :deep(.n-tag) { max-width: 100%; }
.card-info :deep(.n-tag__content) { min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.card-actions { display: flex; justify-content: flex-end; gap: 0; margin-top: auto; padding-top: 10px; border-top: 1px solid var(--n-border-color); }
.card-progress { margin-top: 6px; display: flex; flex-direction: column; gap: 3px; }
.progress-text { font-size: 11px; color: var(--n-text-color-3); }
</style>

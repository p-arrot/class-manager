<script setup lang="ts">
import { computed } from 'vue'
import { NCard, NTag, NEllipsis, NIcon } from 'naive-ui'
import { ArrowForwardOutline } from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import type { CourseVO } from '@/types/api'

defineProps<{ course: CourseVO }>()
defineEmits<{ enter: [id: number] }>()

const theme = useThemeStore()
const isDark = computed(() => theme.isDark)
</script>

<template>
  <NCard size="small" class="course-card" hoverable @click="$emit('enter', course.id)">
    <div class="card-body">
      <div class="card-cover" :style="{ background: isDark ? '#272725' : '#f0efeb', color: isDark ? '#b0b0a8' : '#5a5a54' }">
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
.card-body { display: flex; gap: 14px; align-items: flex-start; flex: 1; }
.card-cover { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; flex-shrink: 0; }
.card-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.card-name { font-size: 15px; font-weight: 600; margin: 0; line-height: 1.3; }
.card-teacher { font-size: 12px; color: var(--n-text-color-3); margin: 0; line-height: 1.3; }
.card-desc { font-size: 12px; color: var(--n-text-color-2); margin: 2px 0 4px; line-height: 1.5; min-height: 36px; }
.card-desc-empty { visibility: hidden; }
.card-actions { display: flex; justify-content: flex-end; gap: 0; margin-top: auto; padding-top: 10px; border-top: 1px solid var(--n-border-color); }
</style>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, useMessage } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import { getCourse } from '@/api/courses'
import CourseResourcePanel from '@/components/CourseResourcePanel.vue'
import { getErrorMessage } from '@/utils/error'
import type { CourseDetailVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const courseId = Number(route.params.courseId)
const course = ref<CourseDetailVO | null>(null)

async function loadCourse() {
  try {
    course.value = await getCourse(courseId)
  } catch (e) {
    message.error(getErrorMessage(e, '加载失败'))
    router.push('/student/home')
  }
}

function goBack() {
  router.push(`/student/courses/${courseId}`)
}

onMounted(loadCourse)
</script>

<template>
  <div class="page">
    <div class="back-bar">
      <NButton text size="small" @click="goBack">
        <template #icon><NIcon :size="16"><ArrowBackOutline /></NIcon></template>
        返回课程
      </NButton>
      <span class="sep">/</span>
      <span class="current">{{ course?.name || '课程资源' }}</span>
    </div>

    <div class="page-head">
      <h2 class="page-title">课程资源</h2>
    </div>

    <CourseResourcePanel :course-id="courseId" readonly />
  </div>
</template>

<style scoped>
.page { max-width: 1100px; animation: fadein 200ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.back-bar { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; font-size: 13px; }
.sep { color: var(--n-text-color-3); }
.current { color: var(--n-text-color-2); font-weight: 500; }
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 600; margin: 0; }
</style>

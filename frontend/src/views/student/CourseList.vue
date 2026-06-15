<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NEmpty, NGi, NGrid, NIcon, NSpin, useMessage } from 'naive-ui'
import { ArrowForwardOutline } from '@vicons/ionicons5'
import CourseCard from '@/components/CourseCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import { listCourses } from '@/api/courses'
import { getErrorMessage } from '@/utils/error'
import type { CoursePageQuery, CourseVO } from '@/types/api'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const records = ref<CourseVO[]>([])
const query = reactive<CoursePageQuery>({ page: 1, size: 12 })

async function fetchData() {
  loading.value = true
  try {
    const result = await listCourses(query)
    records.value = result.records
  } catch (error) {
    message.error(getErrorMessage(error, '课程加载失败'))
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/student/courses/${id}`)
}

onMounted(fetchData)
</script>

<template>
  <div class="page">
    <PageHeader title="我的课程" hint="查看你所在班级已开设的课程与学习资源" />

    <NSpin :show="loading">
      <NGrid v-if="records.length" cols="1 s:2 l:3" :x-gap="16" :y-gap="16" responsive="screen">
        <NGi v-for="course in records" :key="course.id">
          <CourseCard :course="course" @enter="goDetail">
            <template #actions="{ course: item }">
              <NButton size="tiny" quaternary @click="goDetail(item.id)">
                <template #icon><NIcon :size="14"><ArrowForwardOutline /></NIcon></template>
                进入课程
              </NButton>
            </template>
          </CourseCard>
        </NGi>
      </NGrid>
      <NEmpty
        v-else-if="!loading"
        description="暂无课程。你所在的班级尚未被分配任何课程，请联系教师。"
        class="empty-wrap"
      />
    </NSpin>
  </div>
</template>

<style scoped>
.empty-wrap {
  padding: 64px 0;
}
</style>

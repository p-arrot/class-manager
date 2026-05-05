<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NGrid, NGi, NEmpty, NPagination, NIcon, useMessage } from 'naive-ui'
import { ArrowForwardOutline } from '@vicons/ionicons5'
import { listCourses } from '@/api/courses'
import CourseCard from '@/components/CourseCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import type { CourseVO, CoursePageQuery } from '@/types/api'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const records = ref<CourseVO[]>([])
const total = ref(0)
const query = reactive<CoursePageQuery>({ page: 1, size: 12, keyword: '' })

async function fetchData() {
  loading.value = true
  try {
    const r = await listCourses(query)
    records.value = r.records
    total.value = r.total
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) { query.page = page; fetchData() }

function goDetail(id: number) { router.push(`/student/courses/${id}`) }

onMounted(() => { fetchData() })
</script>

<template>
  <div class="page">
    <PageHeader title="我的课程" hint="这里展示你所在班级的所有课程" />

    <div v-if="records.length || loading" class="course-grid">
      <NGrid :cols="3" :x-gap="16" :y-gap="16" responsive="screen">
        <NGi v-for="c in records" :key="c.id">
          <CourseCard :course="c" @enter="goDetail">
            <template #actions="{ course }">
              <NButton size="tiny" quaternary @click="goDetail(course.id)">
                <template #icon><NIcon :size="14"><ArrowForwardOutline /></NIcon></template>进入课程
              </NButton>
            </template>
          </CourseCard>
        </NGi>
      </NGrid>
      <div class="pagination-wrap" v-if="total > query.size">
        <NPagination :page="query.page" :page-size="query.size" :item-count="total" @update:page="handlePageChange" />
      </div>
    </div>

    <NEmpty v-else description="暂无课程" class="empty-wrap">
      <template #extra><p style="font-size:13px;color:var(--n-text-color-3)">你所在的班级尚未被分配任何课程，请联系教师</p></template>
    </NEmpty>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; animation: fadein 200ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.course-grid { display: flex; flex-direction: column; gap: 16px; }
.pagination-wrap { display: flex; justify-content: center; padding-top: 16px; }
.empty-wrap { padding: 80px 0; }
</style>

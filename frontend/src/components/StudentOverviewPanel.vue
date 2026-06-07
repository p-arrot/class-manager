<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { NSelect, NDataTable, NTag, NEmpty, NSpin, NButton, NIcon, useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { PersonOutline } from '@vicons/ionicons5'
import { listSemesters } from '@/api/semesters'
import { getSemesterStatsPreview } from '@/api/stats'
import StudentProfileModal from '@/components/StudentProfileModal.vue'
import { getErrorMessage } from '@/utils/error'
import type { SemesterStatsPreviewRow, SemesterVO } from '@/types/api'

const props = defineProps<{ courseId: number }>()
const semesters = ref<SemesterVO[]>([])
const activeSemesterId = ref<number | null>(null)
const rows = ref<SemesterStatsPreviewRow[]>([])
const loading = ref(false)
const profileStudentId = ref<number | null>(null)
const profileStudentName = ref('')
const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))

const message = useMessage()

watch(() => props.courseId, async () => {
  try {
    semesters.value = await listSemesters(props.courseId) || []
  } catch (e) {
    semesters.value = []
    message.error(getErrorMessage(e, '加载学期列表失败'))
  }
}, { immediate: true })
watch(activeSemesterId, async (sid) => {
  if (!sid) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value = await getSemesterStatsPreview(sid) || []
  } catch (e) {
    rows.value = []
    message.error(getErrorMessage(e, '加载学生总览失败'))
  } finally {
    loading.value = false
  }
})

const columns: DataTableColumns<SemesterStatsPreviewRow> = [
  { title: '学号', key: 'studentNo', width: 100 },
  { title: '姓名', key: 'studentName', width: 100 },
  { title: '过程分', key: 'processScore', width: 80, render: row => row.processScore != null ? row.processScore.toFixed(1) : '-' },
  { title: '考试', key: 'examScore', width: 70, render: row => row.examScore != null ? row.examScore.toFixed(1) : '-' },
  { title: '项目', key: 'projectScore', width: 70, render: row => row.projectScore != null ? row.projectScore.toFixed(1) : '-' },
  { title: '总评', key: 'totalScore', width: 90, render: row => row.totalScore != null ? row.totalScore.toFixed(1) : '-' },
  {
    title: '等级',
    key: 'totalGrade',
    width: 70,
    render: row => row.totalGrade && row.totalGrade !== '暂无数据'
      ? h(NTag, { size: 'tiny', color: { color: (row.totalScore ?? 0) >= 60 ? '#4CAF50' : '#F44336', textColor: '#fff' }, bordered: false }, () => row.totalGrade)
      : h('span', {}, '-'),
  },
  {
    title: '',
    key: 'actions',
    width: 70,
    render: row => h(NButton, { size: 'tiny', quaternary: true, onClick: () => openProfile(row) }, () => [
      h(NIcon, { size: 14 }, () => h(PersonOutline)),
      ' 雷达',
    ]),
  },
]

function openProfile(row: SemesterStatsPreviewRow) {
  profileStudentId.value = row.studentId
  profileStudentName.value = row.studentName
}
</script>

<template>
  <div>
    <div class="overview-toolbar">
      <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="semester-select" />
    </div>
    <NSpin :show="loading">
      <NDataTable v-if="rows.length" :data="rows" size="small" :columns="columns" :row-key="r => r.studentNo" />
      <NEmpty v-else-if="activeSemesterId" description="暂无评价数据" class="empty-state" />
    </NSpin>
    <StudentProfileModal :student-id="profileStudentId" :student-name="profileStudentName" :semester-id="activeSemesterId" @close="profileStudentId = null" />
  </div>
</template>

<style scoped>
.overview-toolbar { margin-bottom: 16px; }
.semester-select { width: 200px; }
.empty-state { padding: 40px 0; }
</style>

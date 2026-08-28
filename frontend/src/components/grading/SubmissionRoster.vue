<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { NDataTable, NEmpty, NInput, NSelect, NTag } from 'naive-ui'
import type { DataTableColumns, SelectOption } from 'naive-ui'
import { formatDate } from '@/utils/date'
import { SUBMISSION_STATUS_OPTIONS, submissionStatusLabel, submissionStatusType } from '@/utils/submissionStatus'

export interface SubmissionRosterRow {
  studentId: number
  studentName: string | null
  studentNo: string | null
  classId: number | null
  className: string | null
  submissionId: number | null
  status: string
  submittedAt: string | null
  score?: number | null
  revisionCount?: number
}

const props = defineProps<{
  rows: SubmissionRosterRow[]
  selectedStudentId?: number | null
  loading?: boolean
}>()

const emit = defineEmits<{ select: [row: SubmissionRosterRow] }>()
const query = ref('')
const classId = ref<number | null>(null)
const status = ref<string | null>(null)

const classOptions = computed<SelectOption[]>(() => {
  const seen = new Map<number, string>()
  props.rows.forEach(row => {
    if (row.classId != null) seen.set(row.classId, row.className || '未命名班级')
  })
  return [...seen].map(([value, label]) => ({ value, label }))
})

const statusOptions = [...SUBMISSION_STATUS_OPTIONS]

const filteredRows = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return props.rows.filter(row => {
    if (classId.value != null && row.classId !== classId.value) return false
    if (status.value && row.status !== status.value) return false
    if (!keyword) return true
    return `${row.studentName ?? ''} ${row.studentNo ?? ''}`.toLowerCase().includes(keyword)
  })
})

const groups = computed(() => {
  const grouped = new Map<string, SubmissionRosterRow[]>()
  filteredRows.value.forEach(row => {
    const key = row.className || '未分班'
    grouped.set(key, [...(grouped.get(key) ?? []), row])
  })
  return [...grouped].map(([name, rows]) => ({ name, rows }))
})

const columns: DataTableColumns<SubmissionRosterRow> = [
  { title: '学生', key: 'studentName', width: 110, render: row => row.studentName || '未命名学生' },
  { title: '学号', key: 'studentNo', width: 120, render: row => row.studentNo || '-' },
  { title: '状态', key: 'status', width: 92, render: row => h(NTag, { size: 'small', bordered: false, type: submissionStatusType(row.status) }, () => submissionStatusLabel(row.status)) },
  { title: '提交时间', key: 'submittedAt', width: 150, render: row => row.submittedAt ? formatDate(row.submittedAt, 'datetime') : '尚未提交' },
  { title: '成绩', key: 'score', width: 76, render: row => row.score ?? '-' },
  { title: '修改', key: 'revisionCount', width: 66, render: row => row.revisionCount ? `${row.revisionCount} 次` : '-' },
]

function rowProps(row: SubmissionRosterRow) {
  return {
    class: row.studentId === props.selectedStudentId ? 'selected-row' : '',
    tabindex: 0,
    role: 'button',
    'aria-label': `查看${row.studentName || row.studentNo || '学生'}的提交`,
    onClick: () => emit('select', row),
    onKeydown: (event: KeyboardEvent) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        emit('select', row)
      }
    },
  }
}
</script>

<template>
  <div class="roster">
    <div class="filters">
      <NInput v-model:value="query" clearable placeholder="搜索姓名或学号" class="query" />
      <NSelect v-model:value="classId" clearable :options="classOptions" placeholder="全部班级" class="filter" />
      <NSelect v-model:value="status" clearable :options="statusOptions" placeholder="全部状态" class="filter" />
    </div>
    <div v-if="groups.length" class="groups">
      <section v-for="group in groups" :key="group.name" class="group">
        <div class="group-title"><strong>{{ group.name }}</strong><span>{{ group.rows.length }} 人</span></div>
        <NDataTable
          :columns="columns"
          :data="group.rows"
          :loading="loading"
          :row-key="row => row.studentId"
          :row-props="rowProps"
          :scroll-x="614"
          size="small"
        />
      </section>
    </div>
    <NEmpty v-else description="没有符合条件的学生" class="empty" />
  </div>
</template>

<style scoped>
.roster { min-width: 0; }
.filters { display: grid; grid-template-columns: minmax(220px, 1fr) 180px 160px; gap: 8px; margin-bottom: 16px; }
.groups { display: grid; gap: 20px; }
.group { min-width: 0; }
.group-title { display: flex; align-items: baseline; gap: 8px; margin-bottom: 8px; }
.group-title strong { font-size: 14px; }
.group-title span { color: var(--n-text-color-3); font-size: 12px; }
.empty { padding: 48px 0; }
:deep(.n-data-table-tr) { cursor: pointer; }
:deep(.n-data-table-tr:focus-visible td) { box-shadow: inset 0 0 0 2px var(--n-primary-color); }
:deep(.selected-row td) { background: var(--n-td-color-hover); }
@media (max-width: 720px) {
  .filters { grid-template-columns: 1fr; }
  .query, .filter { width: 100%; }
}
</style>

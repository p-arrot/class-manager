<script setup lang="ts">
import { ref, reactive, onMounted, h, watch, computed } from 'vue'
import { formatDate } from '@/utils/date'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NDataTable, NModal, NForm, NFormItem, NInput, NSelect, NDatePicker, NCard, NTag, NTabs, NTabPane, NSpace, NIcon, useMessage, useDialog } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline, ChevronUpOutline, ChevronDownOutline, ArrowBackOutline } from '@vicons/ionicons5'
import { getCourse } from '@/api/courses'
import { listSemesters, createSemester, updateSemester, deleteSemester } from '@/api/semesters'
import { listLessons, createLesson, updateLesson, deleteLesson, reorderLesson } from '@/api/lessons'
import { listAllClasses } from '@/api/classes'
import { useThemeStore } from '@/stores/theme'
import CourseResourcePanel from '@/components/CourseResourcePanel.vue'
import type { CourseDetailVO, SemesterVO, SemesterCreateDTO, LessonVO, ClassVO } from '@/types/api'
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const theme = useThemeStore()
const isDark = computed(() => theme.isDark)

const courseId = Number(route.params.courseId)
const course = ref<CourseDetailVO | null>(null)
const semesters = ref<SemesterVO[]>([])
const lessons = ref<LessonVO[]>([])
const activeSemesterId = ref<number | null>(null)
const allClasses = ref<ClassVO[]>([])
const classMap = computed(() => new Map(allClasses.value.map(c => [c.id, c])))

// Semester modal
const showSemModal = ref(false)
const semTitle = ref('创建学期')
const editingSemId = ref<number | null>(null)
const semFormRef = ref<FormInst | null>(null)
const semForm = reactive<SemesterCreateDTO & { startEnd: [number, number] | null }>({ name: '', startTime: '', endTime: '', startEnd: null })
const semRules: FormRules = { name: { required: true, message: '请输入学期名称', trigger: 'blur' }, startEnd: { required: true, type: 'array', message: '请选择起止时间', trigger: 'change' } }

// Lesson modal
const showLesModal = ref(false)
const lesTitle = ref('创建课时')
const editingLesId = ref<number | null>(null)
const lesFormRef = ref<FormInst | null>(null)
const lesForm = reactive({ name: '' })
const lesRules: FormRules = { name: { required: true, message: '请输入课时名称', trigger: 'blur' } }

// Lesson columns
const lesColumns: DataTableColumns<LessonVO> = [
  { title: '序号', key: 'sortOrder', width: 60 },
  { title: '课时名称', key: 'name' },
  {
    title: '操作', key: 'actions', width: 190,
    render(row: LessonVO, index: number) {
      return h(NSpace, { size: 2 }, () => [
        h(NButton, { size: 'tiny', quaternary: true, disabled: index === 0, onClick: () => handleMoveUp(row) }, () => h(NIcon, { size: 14 }, () => h(ChevronUpOutline))),
        h(NButton, { size: 'tiny', quaternary: true, disabled: index === lessons.value.length - 1, onClick: () => handleMoveDown(row) }, () => h(NIcon, { size: 14 }, () => h(ChevronDownOutline))),
        h(NButton, { size: 'tiny', quaternary: true, onClick: () => openEditLesson(row) }, () => h(NIcon, { size: 14 }, () => h(CreateOutline))),
        h(NButton, { size: 'tiny', quaternary: true, onClick: () => handleDeleteLesson(row) }, () => h(NIcon, { size: 14 }, () => h(TrashOutline))),
      ])
    },
  },
]

// ---- Fetch ----
async function loadCourse() {
  try { course.value = await getCourse(courseId) } catch (e: any) { message.error(e.message || '加载失败'); router.push('/teacher/courses') }
}

async function loadSemesters() {
  try { semesters.value = await listSemesters(courseId) } catch { /* ignore */ }
}

async function loadLessons() {
  if (!activeSemesterId.value) { lessons.value = []; return }
  try { lessons.value = await listLessons(activeSemesterId.value) } catch { /* ignore */ }
}

// ---- Semester CRUD ----
function openCreateSemester() {
  semTitle.value = '创建学期'; editingSemId.value = null
  semForm.name = ''; semForm.startEnd = null; showSemModal.value = true
}

function openEditSemester(row: SemesterVO) {
  semTitle.value = '编辑学期'; editingSemId.value = row.id
  semForm.name = row.name
  semForm.startEnd = [new Date(row.startTime).getTime(), new Date(row.endTime).getTime()]
  showSemModal.value = true
}

async function handleSemSubmit() {
  try { await semFormRef.value?.validate() } catch { return }
  try {
    const data: SemesterCreateDTO = {
      name: semForm.name,
      startTime: new Date(semForm.startEnd![0]).toISOString().replace('Z', ''),
      endTime: new Date(semForm.startEnd![1]).toISOString().replace('Z', ''),
    }
    if (editingSemId.value) { await updateSemester(editingSemId.value, data); message.success('更新成功') }
    else { await createSemester(courseId, data); message.success('创建成功') }
    showSemModal.value = false; loadSemesters()
  } catch (e: any) { message.error(e.message || '操作失败') }
}

async function handleDeleteSemester(row: SemesterVO) {
  dialog.warning({ title: '确认删除', content: `确定删除学期「${row.name}」吗？学期下有课时将无法删除。`, positiveText: '删除', negativeText: '取消',
    onPositiveClick: async () => { try { await deleteSemester(row.id); message.success('已删除'); loadSemesters() } catch (e: any) { message.error(e.message || '删除失败') } },
  })
}

// ---- Lesson CRUD ----
function openCreateLesson() {
  lesTitle.value = '创建课时'; editingLesId.value = null; lesForm.name = ''; showLesModal.value = true
}

function openEditLesson(row: LessonVO) {
  lesTitle.value = '编辑课时'; editingLesId.value = row.id; lesForm.name = row.name; showLesModal.value = true
}

async function handleLesSubmit() {
  try { await lesFormRef.value?.validate() } catch { return }
  try {
    if (editingLesId.value) { await updateLesson(editingLesId.value, { name: lesForm.name }); message.success('更新成功') }
    else { await createLesson(activeSemesterId.value!, { name: lesForm.name }); message.success('创建成功') }
    showLesModal.value = false; loadLessons()
  } catch (e: any) { message.error(e.message || '操作失败') }
}

async function handleDeleteLesson(row: LessonVO) {
  dialog.warning({ title: '确认删除', content: `确定删除课时「${row.name}」吗？`, positiveText: '删除', negativeText: '取消',
    onPositiveClick: async () => { try { await deleteLesson(row.id); message.success('已删除'); loadLessons() } catch (e: any) { message.error(e.message || '删除失败') } },
  })
}

async function handleMoveUp(row: LessonVO) {
  const idx = lessons.value.findIndex(l => l.id === row.id)
  if (idx <= 0) return
  try { await reorderLesson(row.id, { targetIndex: idx - 1 }); loadLessons() } catch (e: any) { message.error(e.message || '操作失败') }
}

async function handleMoveDown(row: LessonVO) {
  const idx = lessons.value.findIndex(l => l.id === row.id)
  if (idx >= lessons.value.length - 1) return
  try { await reorderLesson(row.id, { targetIndex: idx + 1 }); loadLessons() } catch (e: any) { message.error(e.message || '操作失败') }
}

function goBack() { router.push('/teacher/courses') }



// Auto-select first semester
watch(semesters, (val) => {
  if (val.length && !activeSemesterId.value) activeSemesterId.value = val[0].id
  if (!val.length) activeSemesterId.value = null
})

watch(activeSemesterId, () => { if (activeSemesterId.value) loadLessons() })

onMounted(async () => {
  await loadCourse()
  await loadSemesters()
  try { allClasses.value = await listAllClasses() } catch { /* ignore */ }
})
</script>

<template>
  <div class="page">
    <div class="back-bar">
      <NButton text size="small" @click="goBack"><template #icon><NIcon :size="16"><ArrowBackOutline /></NIcon></template>课程列表</NButton>
    </div>

    <div v-if="course" class="course-header">
      <h2 class="course-name">{{ course.name }}</h2>
      <p v-if="course.description" class="course-desc">{{ course.description }}</p>
      <NSpace :size="4">
        <NTag v-for="(id, i) in course.classIds" :key="i" size="tiny" :bordered="false">{{ classMap.get(id)?.grade }}级{{ classMap.get(id)?.name || id }}</NTag>
        <span v-if="!course.classIds?.length" style="font-size:12px;color:var(--n-text-color-3)">未绑定班级</span>
      </NSpace>
    </div>

    <NTabs type="line" animated>
      <NTabPane name="semesters" tab="学期管理">
        <div class="tab-head">
          <span class="tab-subtitle">{{ semesters.length }} 个学期</span>
          <NButton size="small" @click="openCreateSemester"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>新建学期</NButton>
        </div>
        <div v-if="semesters.length" class="sem-list">
          <NCard v-for="s in semesters" :key="s.id" size="small" class="sem-card">
            <div class="sem-info">
              <h4 class="sem-name">{{ s.name }}</h4>
              <p class="sem-time">{{ formatDate(s.startTime, 'date') }} — {{ formatDate(s.endTime, 'date') }}</p>
              <NTag size="tiny" :bordered="false">{{ s.lessonCount }} 课时</NTag>
            </div>
            <NSpace :size="2">
              <NButton size="tiny" quaternary @click="openEditSemester(s)"><template #icon><NIcon :size="14"><CreateOutline /></NIcon></template></NButton>
              <NButton size="tiny" quaternary @click="handleDeleteSemester(s)"><template #icon><NIcon :size="14"><TrashOutline /></NIcon></template></NButton>
            </NSpace>
          </NCard>
        </div>
        <div v-else class="empty-hint">尚无学期，请先创建一个学期</div>
      </NTabPane>

      <NTabPane name="lessons" tab="课时管理">
        <div class="tab-head">
          <NSelect v-if="semesters.length" v-model:value="activeSemesterId" :options="semesters.map(s => ({ label: s.name, value: s.id }))" size="small" style="width:220px" />
          <NButton v-if="activeSemesterId" size="small" @click="openCreateLesson"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>新建课时</NButton>
        </div>
        <NDataTable v-if="activeSemesterId && lessons.length" :columns="lesColumns" :data="lessons" size="small" :row-key="(r: LessonVO) => r.id" />
        <div v-else class="empty-hint">{{ semesters.length ? '该学期尚无课时' : '请先创建一个学期' }}</div>
      </NTabPane>

      <NTabPane name="resources" tab="课程资源">
        <CourseResourcePanel :course-id="courseId" />
      </NTabPane>
    </NTabs>

    <!-- Semester Modal -->
    <NModal v-model:show="showSemModal" :title="semTitle" preset="card" style="width:440px">
      <NForm ref="semFormRef" :model="semForm" :rules="semRules" label-placement="left" label-width="72">
        <NFormItem label="学期名称" path="name"><NInput v-model:value="semForm.name" placeholder="如：2026年秋季学期" /></NFormItem>
        <NFormItem label="起止时间" path="startEnd"><NDatePicker v-model:value="semForm.startEnd" type="daterange" clearable style="width:100%" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showSemModal = false">取消</NButton><NButton type="primary" @click="handleSemSubmit">确定</NButton></NSpace></template>
    </NModal>

    <!-- Lesson Modal -->
    <NModal v-model:show="showLesModal" :title="lesTitle" preset="card" style="width:400px">
      <NForm ref="lesFormRef" :model="lesForm" :rules="lesRules" label-placement="left" label-width="72">
        <NFormItem label="课时名称" path="name"><NInput v-model:value="lesForm.name" placeholder="如：第一课：认识Python" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showLesModal = false">取消</NButton><NButton type="primary" @click="handleLesSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 900px; animation: fadein 200ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.back-bar { margin-bottom: 12px; }
.course-header { margin-bottom: 24px; }
.course-name { font-size: 22px; font-weight: 600; letter-spacing: -0.01em; margin: 0 0 6px; }
.course-desc { font-size: 14px; color: var(--n-text-color-2); margin: 0 0 10px; line-height: 1.5; }
.tab-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-subtitle { font-size: 13px; color: var(--n-text-color-3); }
.sem-list { display: flex; flex-direction: column; gap: 10px; }
.sem-card { display: flex; flex-direction: row; justify-content: space-between; align-items: center; transition: border-color 150ms ease; }
.sem-card:hover { border-color: var(--n-primary-color-hover); }
.sem-info { display: flex; flex-direction: column; gap: 2px; }
.sem-name { font-size: 15px; font-weight: 600; margin: 0; }
.sem-time { font-size: 12px; color: var(--n-text-color-3); margin: 0; }
.empty-hint { padding: 40px 0; text-align: center; font-size: 14px; color: var(--n-text-color-3); }
.resource-tab { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 40px 0; }
.resource-desc { font-size: 14px; color: var(--n-text-color-2); margin: 0; }
</style>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NGrid, NGi, NModal, NForm, NFormItem, NInput, NSelect, NSpace, NIcon, NEmpty, NPagination, useMessage, useDialog } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline, ArrowForwardOutline, CloudUploadOutline, CloseCircleOutline } from '@vicons/ionicons5'
import CourseCard from '@/components/CourseCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import { listCourses, getCourse, createCourse, updateCourse, deleteCourse, uploadCourseCover } from '@/api/courses'
import { listAllClasses } from '@/api/classes'
import { useClassFilterStore } from '@/stores/classFilter'
import { getErrorMessage } from '@/utils/error'
import type { CourseVO, CourseCreateDTO, CoursePageQuery, ClassVO } from '@/types/api'
import type { FormInst, FormRules } from 'naive-ui'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const classFilter = useClassFilterStore()

const loading = ref(false)
const records = ref<CourseVO[]>([])
const total = ref(0)
const query = reactive<CoursePageQuery>({ page: 1, size: 12, keyword: '' })
const allClasses = ref<ClassVO[]>([])
const classOptions = computed(() => allClasses.value.map(item => ({
  label: `${item.grade}级${item.name}`,
  value: item.id,
})))

// Modal
const showModal = ref(false)
const modalTitle = ref('创建课程')
const editingId = ref<number | null>(null)
const formRef = ref<FormInst | null>(null)
const coverInput = ref<HTMLInputElement | null>(null)
const coverUploading = ref(false)
const formValue = reactive<CourseCreateDTO>({ name: '', description: '', coverUrl: '', classIds: [] })
const rules: FormRules = { name: { required: true, message: '请输入课程名称', trigger: 'blur' } }

async function fetchData() {
  loading.value = true
  try {
    const r = await listCourses(query)
    records.value = r.records
    total.value = r.total
  } catch (e) {
    message.error(getErrorMessage(e, '加载失败'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  query.page = page
  fetchData()
}

function openCreate() {
  modalTitle.value = '创建课程'
  editingId.value = null
  Object.assign(formValue, { name: '', description: '', coverUrl: '', classIds: [] })
  showModal.value = true
}

async function openEdit(row: CourseVO) {
  modalTitle.value = '编辑课程'
  editingId.value = row.id
  Object.assign(formValue, { name: row.name, description: row.description || '', coverUrl: row.coverUrl || '', classIds: [] as number[] })
  showModal.value = true
  try {
    const detail = await getCourse(row.id)
    formValue.classIds = detail.classIds
  } catch (e) {
    message.error(getErrorMessage(e, '加载课程班级失败'))
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  try {
    const data: CourseCreateDTO = {
      name: formValue.name,
      description: formValue.description || undefined,
      coverUrl: formValue.coverUrl || undefined,
      classIds: formValue.classIds?.length ? formValue.classIds : undefined,
    }
    if (editingId.value) {
      await updateCourse(editingId.value, data)
      message.success('更新成功')
    } else {
      await createCourse(data)
      message.success('创建成功')
    }
    showModal.value = false
    fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

function handleDelete(row: CourseVO) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除课程「${row.name}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteCourse(row.id)
        message.success('已删除')
        fetchData()
      } catch (e) {
        message.error(getErrorMessage(e, '删除失败'))
      }
    },
  })
}

function goDetail(id: number) { router.push(`/teacher/courses/${id}`) }

async function handleCoverSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    message.warning('封面图片不能超过 5MB')
    return
  }
  coverUploading.value = true
  try {
    const result = await uploadCourseCover(file)
    if (!result.url) {
      message.error('封面上传失败：服务端未返回图片地址')
      return
    }
    formValue.coverUrl = result.url
    message.success('封面已上传')
  } catch (e) {
    message.error(getErrorMessage(e, '封面上传失败'))
  } finally {
    coverUploading.value = false
  }
}

function clearCover() {
  formValue.coverUrl = ''
}

onMounted(async () => {
  if (classFilter.selectedClassId) query.classId = classFilter.selectedClassId ?? undefined
  fetchData()
  try {
    allClasses.value = await listAllClasses()
  } catch (e) {
    allClasses.value = []
    message.error(getErrorMessage(e, '加载班级列表失败'))
  }
})

watch(() => classFilter.selectedClassId, (v) => {
  query.classId = v ?? undefined
  query.page = 1
  fetchData()
})
</script>

<template>
  <div class="page">
    <PageHeader title="课程管理" hint="创建和管理你的课程，为每门课程添加学期和课时">
      <template #actions>
        <NButton type="primary" size="small" @click="openCreate"><template #icon><NIcon :size="16"><AddOutline /></NIcon></template>创建课程</NButton>
      </template>
    </PageHeader>

    <div v-if="records.length || loading" class="course-grid">
      <NGrid cols="1 s:2 l:3" :x-gap="16" :y-gap="16" responsive="screen">
        <NGi v-for="c in records" :key="c.id">
          <CourseCard :course="c" @enter="goDetail">
            <template #actions="{ course }">
              <NButton size="tiny" quaternary @click="goDetail(course.id)"><template #icon><NIcon :size="14"><ArrowForwardOutline /></NIcon></template>进入</NButton>
              <NButton size="tiny" quaternary title="编辑课程" aria-label="编辑课程" @click="openEdit(course)"><template #icon><NIcon :size="14"><CreateOutline /></NIcon></template></NButton>
              <NButton size="tiny" quaternary title="删除课程" aria-label="删除课程" @click="handleDelete(course)"><template #icon><NIcon :size="14"><TrashOutline /></NIcon></template></NButton>
            </template>
          </CourseCard>
        </NGi>
      </NGrid>
      <div class="pagination-wrap" v-if="total > (query.size ?? 12)">
        <NPagination :page="query.page" :page-size="query.size ?? 12" :item-count="total" @update:page="handlePageChange" />
      </div>
    </div>

    <NEmpty v-else description="暂无课程" class="empty-wrap">
      <template #extra><NButton size="small" @click="openCreate">创建你的第一门课程</NButton></template>
    </NEmpty>

    <NModal v-model:show="showModal" :title="modalTitle" preset="card" class="course-modal">
      <NForm ref="formRef" :model="formValue" :rules="rules" label-placement="left" label-width="72">
        <NFormItem label="课程封面">
          <div class="cover-field">
            <div class="cover-preview">
              <img v-if="formValue.coverUrl" :src="formValue.coverUrl" alt="课程封面预览">
              <span v-else>{{ formValue.name?.charAt(0) || '课' }}</span>
            </div>
            <div class="cover-actions">
              <NSpace :size="8">
                <NButton size="small" :loading="coverUploading" @click="coverInput?.click()">
                  <template #icon><NIcon :size="14"><CloudUploadOutline /></NIcon></template>
                  上传封面
                </NButton>
                <NButton v-if="formValue.coverUrl" size="small" quaternary @click="clearCover">
                  <template #icon><NIcon :size="14"><CloseCircleOutline /></NIcon></template>
                  清除
                </NButton>
              </NSpace>
              <input ref="coverInput" type="file" accept="image/jpeg,image/png,image/webp,image/gif" class="cover-input" @change="handleCoverSelected">
              <div class="cover-hint">支持 jpg、png、webp、gif，最大 5MB。</div>
            </div>
          </div>
        </NFormItem>
        <NFormItem label="课程名称" path="name"><NInput v-model:value="formValue.name" placeholder="如：Python编程基础" /></NFormItem>
        <NFormItem label="课程介绍"><NInput v-model:value="formValue.description" type="textarea" placeholder="选填" :autosize="{ minRows: 2, maxRows: 4 }" /></NFormItem>
        <NFormItem label="授课班级">
          <NSelect v-model:value="formValue.classIds" :options="classOptions" multiple placeholder="选择班级（可多选）" clearable />
        </NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal = false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; animation: fadein 200ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.course-grid { display: flex; flex-direction: column; gap: 16px; }
.pagination-wrap { display: flex; justify-content: center; padding-top: 16px; }
.empty-wrap { padding: 80px 0; }
.course-modal { width: min(480px, calc(100vw - 32px)); }
.cover-field { display: flex; align-items: center; gap: 14px; width: 100%; min-width: 0; }
.cover-preview { width: 72px; height: 72px; border-radius: 8px; flex: 0 0 auto; overflow: hidden; display: flex; align-items: center; justify-content: center; border: 1px solid var(--n-border-color); background: var(--n-color-embedded); color: var(--n-text-color-3); font-size: 22px; font-weight: 700; }
.cover-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover-actions { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.cover-input { display: none; }
.cover-hint { color: var(--n-text-color-3); font-size: 12px; line-height: 1.4; }
@media (max-width: 640px) {
  .cover-field { align-items: flex-start; flex-direction: column; }
}
</style>

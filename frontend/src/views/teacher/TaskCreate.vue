<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NInput, NSelect, NTag, NIcon, NSpace, NForm, NFormItem, NRadio, NCheckbox, NUpload, NModal, useMessage } from 'naive-ui'
import { ArrowBackOutline, AddOutline, TrashOutline, ImageOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute(); const router = useRouter(); const message = useMessage()
const lessonId = Number(route.params.lessonId)
const lessonName = ref('')

const form = ref({ title: '', type: 'worksheet' as string, description: '', deadline: '' })

interface Field {
  id: string; type: string; label: string; required: boolean
  options?: string[]; imageUrl?: string
}
const fields = ref<Field[]>([])

const typeOptions = [
  { label: '单选题', value: 'radio' },
  { label: '多选题', value: 'checkbox' },
  { label: '填空题', value: 'text' },
  { label: '简答题', value: 'textarea' },
]

function addField(type: string) {
  fields.value.push({ id: crypto.randomUUID().slice(0,8), type, label: '', required: true, options: type === 'radio' || type === 'checkbox' ? [''] : undefined })
}

function removeField(idx: number) { fields.value.splice(idx, 1) }
function addOption(fIdx: number) { fields.value[fIdx].options?.push('') }
function removeOption(fIdx: number, oIdx: number) { fields.value[fIdx].options?.splice(oIdx, 1) }

function handleImageUpload(fIdx: number, { file }: any) {
  if (!file.file) return
  const fd = new FormData(); fd.append('file', file.file)
  http.post('/files/upload', fd).then((r: any) => {
    http.get(`/files/${r.resourceId}/stream`).then((stream: any) => {
      fields.value[fIdx].imageUrl = stream.url
    })
  }).catch(() => message.error('图片上传失败'))
}

async function handleSubmit() {
  if (!form.value.title.trim()) { message.warning('请输入任务标题'); return }
  const schema = { version: 1, fields: fields.value.map(f => {
    const base: any = { id: f.id, type: f.type, label: f.label, required: f.required, imageUrl: f.imageUrl }
    if (f.type === 'radio' || f.type === 'checkbox') base.options = f.options
    return base
  })}
  try {
    await http.post(`/lessons/${lessonId}/tasks`, {
      title: form.value.title, type: form.value.type,
      description: form.value.description || undefined,
      deadline: form.value.deadline || undefined,
      formSchema: JSON.stringify(schema),
    })
    message.success('任务创建成功')
    router.push(`/teacher/courses`)
  } catch (e: any) { message.error(e.message || '创建失败') }
}

onMounted(async () => {
  try { const l: any = await http.get(`/lessons/${lessonId}`); lessonName.value = l.name } catch (e) { console.error("TaskCreate.vue failed", e) }
})
</script>

<template>
  <div class="page">
    <div class="back-bar">
      <NButton text @click="router.back()"><template #icon><NIcon><ArrowBackOutline /></NIcon></template>返回</NButton>
    </div>
    <PageHeader title="创建任务" :subtitle="lessonName" />

    <div class="form-section">
      <NForm label-placement="top" style="max-width:640px">
        <NFormItem label="任务标题" required><NInput v-model:value="form.title" placeholder="如：Python基础选择练习" size="large" /></NFormItem>
        <NFormItem label="任务类型" required>
          <NSelect v-model:value="form.type" :options="[
            {label:'学习单(有题目)',value:'worksheet'},{label:'课堂作品(交文件)',value:'artifact'}
          ]" size="large" />
        </NFormItem>
        <NFormItem label="任务说明"><NInput v-model:value="form.description" type="textarea" placeholder="可选的任务说明" :autosize="{minRows:2}" /></NFormItem>
        <NFormItem label="截止时间"><NInput v-model:value="form.deadline" placeholder="2027-06-30T23:59" /></NFormItem>
      </NForm>
    </div>

    <!-- Worksheet editor -->
    <div v-if="form.type === 'worksheet'" class="fields-section">
      <h3 class="section-title">题目设计</h3>
      <div v-if="fields.length" class="field-list">
        <div v-for="(f, fi) in fields" :key="f.id" class="field-card">
          <div class="field-header">
            <NTag size="small" :bordered="false">{{ typeOptions.find(o=>o.value===f.type)?.label }}</NTag>
            <NInput v-model:value="f.label" placeholder="题目标题" style="flex:1" />
            <NButton size="tiny" quaternary @click="removeField(fi)"><template #icon><NIcon :size="14"><TrashOutline /></NIcon></template></NButton>
          </div>

          <!-- Image upload -->
          <div v-if="f.imageUrl" class="field-image">
            <img :src="f.imageUrl" style="max-width:300px;max-height:200px;border-radius:6px" />
            <NButton size="tiny" quaternary @click="f.imageUrl = undefined">移除图片</NButton>
          </div>
          <NUpload v-else :show-file-list="false" accept="image/*" :custom-request="(opt:any) => handleImageUpload(fi, opt)">
            <NButton size="tiny" text><template #icon><NIcon :size="12"><ImageOutline /></NIcon></template>插入图片</NButton>
          </NUpload>

          <!-- Options for radio/checkbox -->
          <div v-if="f.type === 'radio' || f.type === 'checkbox'" class="field-options">
            <div v-for="(opt, oi) in f.options" :key="oi" class="opt-row">
              <span class="opt-marker">{{ f.type === 'radio' ? '○' : '☐' }}</span>
              <NInput v-model:value="f.options![oi]" size="small" placeholder="选项文字" />
              <NButton size="tiny" quaternary @click="removeOption(fi, oi)"><template #icon><NIcon :size="12"><TrashOutline /></NIcon></template></NButton>
            </div>
            <NButton size="tiny" text @click="addOption(fi)">+ 添加选项</NButton>
          </div>
        </div>
      </div>

      <NSpace :size="8" class="add-buttons">
        <NButton v-for="t in typeOptions" :key="t.value" size="small" @click="addField(t.value)">
          <template #icon><NIcon :size="14"><AddOutline /></NIcon></template>{{ t.label }}
        </NButton>
      </NSpace>
    </div>

    <div class="submit-bar">
      <NButton type="primary" size="large" @click="handleSubmit">创建任务</NButton>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 760px; margin: 0 auto; padding: 24px 0; }
.back-bar { margin-bottom: 8px; }
.form-section { margin-bottom: 24px; }
.section-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.field-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.field-card { border: 1px solid var(--n-border-color); border-radius: 8px; padding: 12px 14px; }
.field-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.field-image { margin: 6px 0; }
.field-options { margin-top: 8px; padding-left: 20px; display: flex; flex-direction: column; gap: 4px; }
.opt-row { display: flex; align-items: center; gap: 6px; }
.opt-marker { font-size: 14px; color: var(--n-text-color-3); width: 18px; }
.add-buttons { margin-top: 8px; }
.submit-bar { margin-top: 32px; text-align: center; }
</style>

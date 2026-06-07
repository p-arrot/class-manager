<script setup lang="ts">
import { ref } from 'vue'
import { NButton, NInput, NTag, NIcon, NSpace, NPopconfirm } from 'naive-ui'
import { AddOutline, TrashOutline, ChevronUpOutline, ChevronDownOutline } from '@vicons/ionicons5'

interface Field {
  id: string
  type: string
  label: string
  options?: string[]
  required: boolean
  maxLength?: number
}
interface TableColumn { key: string; label: string; type: string }
interface LegacySchema {
  fields?: Partial<Field>[]
}
interface SerializedField {
  id: string
  type: string
  label: string
  required: boolean
  options?: string[]
  maxLength?: number
  columns?: TableColumn[]
}

const model = defineModel<string>({ default: '{"fields":[]}' })

const fields = ref<Field[]>([])
const tableCols = ref<Record<string, TableColumn[]>>({})

const typeOptions = [
  { label: '单选', value: 'radio' }, { label: '多选', value: 'checkbox' },
  { label: '填空', value: 'text' }, { label: '长回答', value: 'textarea' },
  { label: '表格', value: 'table' },
]

function parseSchema() {
  try {
    const s = JSON.parse(model.value) as LegacySchema
    fields.value = (s.fields || []).map((f) => ({
      id: f.id || crypto.randomUUID().slice(0,8),
      type: f.type || 'text',
      label: f.label || '',
      options: f.options || [],
      required: f.required !== false,
      maxLength: f.maxLength,
    }))
  } catch {
    fields.value = []
  }
}

function emitUpdate() {
  model.value = JSON.stringify({
    version: 1,
    fields: fields.value.map(f => {
      const base: SerializedField = { id: f.id, type: f.type, label: f.label, required: f.required }
      if (f.type === 'radio' || f.type === 'checkbox') base.options = f.options || []
      if (f.type === 'text' || f.type === 'textarea') base.maxLength = f.maxLength
      if (f.type === 'table') base.columns = tableCols.value[f.id] || []
      return base
    })
  })
}

function addField() {
  const id = crypto.randomUUID().slice(0, 8)
  fields.value.push({ id, type: 'text', label: '', required: true })
  emitUpdate()
}

function removeField(idx: number) {
  fields.value.splice(idx, 1)
  emitUpdate()
}

function addOption(idx: number) {
  const f = fields.value[idx]
  if (!f.options) f.options = []
  f.options.push('')
  emitUpdate()
}

function removeOption(fIdx: number, oIdx: number) {
  fields.value[fIdx].options?.splice(oIdx, 1)
  emitUpdate()
}

function moveField(idx: number, dir: number) {
  const target = idx + dir
  if (target < 0 || target >= fields.value.length) return
  const tmp = fields.value[idx]
  fields.value[idx] = fields.value[target]
  fields.value[target] = tmp
  emitUpdate()
}

function addTableCol(fieldId: string) {
  if (!tableCols.value[fieldId]) tableCols.value[fieldId] = []
  tableCols.value[fieldId].push({ key: 'c' + Date.now(), label: '', type: 'text' })
  emitUpdate()
}

function typeLabel(t: string) {
  return typeOptions.find(o => o.value === t)?.label || t
}

parseSchema()
</script>

<template>
  <div class="ws-editor">
    <div v-if="fields.length" class="field-list">
      <div v-for="(f, fi) in fields" :key="f.id" class="field-card">
        <div class="field-head">
          <NTag size="tiny" :bordered="false">{{ typeLabel(f.type) }}</NTag>
          <NInput v-model:value="f.label" size="small" placeholder="题目标题" class="field-title-input" @update:value="emitUpdate" />
          <NSpace :size="2">
            <NButton size="tiny" quaternary title="上移题目" aria-label="上移题目" @click="moveField(fi, -1)" :disabled="fi === 0"><template #icon><NIcon :size="12"><ChevronUpOutline /></NIcon></template></NButton>
            <NButton size="tiny" quaternary title="下移题目" aria-label="下移题目" @click="moveField(fi, 1)" :disabled="fi === fields.length - 1"><template #icon><NIcon :size="12"><ChevronDownOutline /></NIcon></template></NButton>
            <NPopconfirm @positive-click="() => removeField(fi)"><template #trigger><NButton size="tiny" quaternary title="删除题目" aria-label="删除题目"><template #icon><NIcon :size="12"><TrashOutline /></NIcon></template></NButton></template>删除此题？</NPopconfirm>
          </NSpace>
        </div>

        <!-- Options for radio/checkbox -->
        <div v-if="f.type === 'radio' || f.type === 'checkbox'" class="field-options">
          <div v-for="(_opt, oi) in f.options" :key="oi" class="opt-row">
            <span class="opt-marker">{{ f.type === 'radio' ? '○' : '☐' }}</span>
            <NInput v-model:value="f.options![oi]" size="tiny" placeholder="选项文字" @update:value="emitUpdate" />
            <NButton size="tiny" quaternary title="删除选项" aria-label="删除选项" @click="() => removeOption(fi, oi)"><template #icon><NIcon :size="12"><TrashOutline /></NIcon></template></NButton>
          </div>
          <NButton size="tiny" text @click="() => addOption(fi)">+ 添加选项</NButton>
        </div>

        <!-- Table columns -->
        <div v-if="f.type === 'table'" class="field-options">
          <div v-for="(_col, ci) in (tableCols[f.id] || [])" :key="ci" class="opt-row">
            <span class="opt-marker">▦</span>
            <NInput v-model:value="tableCols[f.id][ci].label" size="tiny" placeholder="列名" @update:value="emitUpdate" />
          </div>
          <NButton size="tiny" text @click="() => addTableCol(f.id)">+ 添加列</NButton>
        </div>
      </div>
    </div>
    <NButton size="small" dashed block @click="addField" class="add-btn">
      <template #icon><NIcon :size="14"><AddOutline /></NIcon></template>添加题目
    </NButton>
  </div>
</template>

<style scoped>
.ws-editor { display: flex; flex-direction: column; gap: 10px; }
.field-list { display: flex; flex-direction: column; gap: 8px; }
.field-card { border: 1px solid var(--n-border-color); border-radius: 8px; padding: 10px 12px; }
.field-head { display: flex; align-items: center; gap: 8px; }
.field-title-input { flex: 1; }
.field-options { margin-top: 8px; padding-left: 20px; display: flex; flex-direction: column; gap: 4px; }
.opt-row { display: flex; align-items: center; gap: 6px; }
.opt-marker { font-size: 12px; color: var(--n-text-color-3); width: 16px; }
.add-btn { margin-top: 4px; }
</style>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NInput, NRadio, NCheckbox, NButton, NSpace, NTag } from 'naive-ui'

interface Field { id: string; type: string; label: string; options?: string[]; columns?: { key: string; label: string }[]; required: boolean }
const props = defineProps<{ schema: string }>()
const model = defineModel<Record<string, any>>({ default: () => ({}) })
const fields = ref<Field[]>([])

try {
  const s = JSON.parse(props.schema || '{"fields":[]}')
  fields.value = s.fields || []
} catch { fields.value = [] }

function getAnswer(fId: string): any { return model.value[fId] }
function setAnswer(fId: string, val: any) { model.value[fId] = val }

function radioVal(f: Field) { return model.value[f.id] || '' }
function checkboxVal(f: Field): string[] { return model.value[f.id] || [] }
function toggleCheckbox(f: Field, opt: string) {
  const cur: string[] = model.value[f.id] || []
  const idx = cur.indexOf(opt)
  if (idx >= 0) cur.splice(idx, 1); else cur.push(opt)
  model.value[f.id] = [...cur]
}
</script>

<template>
  <div class="ws-render" v-if="fields.length">
    <div v-for="f in fields" :key="f.id" class="ws-field">
      <div class="ws-label">{{ f.label }} <NTag v-if="f.required" size="tiny" :bordered="false" type="error">必填</NTag></div>

      <!-- Radio -->
      <div v-if="f.type === 'radio'" class="ws-options">
        <NRadio.Group :value="radioVal(f)" @update:value="(v: string) => setAnswer(f.id, v)">
          <NSpace vertical :size="4">
            <NRadio v-for="(opt, oi) in f.options" :key="oi" :value="opt">{{ opt }}</NRadio>
          </NSpace>
        </NRadio.Group>
      </div>

      <!-- Checkbox -->
      <div v-if="f.type === 'checkbox'" class="ws-options">
        <NSpace vertical :size="4">
          <NCheckbox v-for="(opt, oi) in f.options" :key="oi" :checked="checkboxVal(f).includes(opt!)" @update:checked="() => toggleCheckbox(f, opt!)">{{ opt }}</NCheckbox>
        </NSpace>
      </div>

      <!-- Text / Textarea -->
      <NInput v-if="f.type === 'text'" :value="getAnswer(f.id) || ''" @update:value="(v: string) => setAnswer(f.id, v)" placeholder="请输入" :maxlength="f.maxLength" />
      <NInput v-if="f.type === 'textarea'" type="textarea" :value="getAnswer(f.id) || ''" @update:value="(v: string) => setAnswer(f.id, v)" placeholder="请输入" :autosize="{ minRows: 3, maxRows: 8 }" :maxlength="f.maxLength" />

      <!-- Table -->
      <table v-if="f.type === 'table' && f.columns" class="ws-table">
        <thead><tr><th v-for="col in f.columns" :key="col.key">{{ col.label }}</th></tr></thead>
        <tbody><tr v-for="(row, ri) in (getAnswer(f.id) || [{...initRow(f)}])" :key="ri">
          <td v-for="col in f.columns" :key="col.key">
            <NInput size="tiny" :value="row?.[col.key] || ''" @update:value="(v: string) => {
              if (!model[f.id]) model[f.id] = [{}]
              model[f.id][ri] = { ...model[f.id][ri], [col.key]: v }
            }" />
          </td>
        </tr></tbody>
      </table>
    </div>
  </div>
  <div v-else class="ws-empty">无题目</div>
</template>

<script lang="ts">
function initRow(f: any): Record<string, string> {
  const row: Record<string, string> = {}
  if (f.columns) for (const c of f.columns) row[c.key] = ''
  return row
}
</script>

<style scoped>
.ws-render { display: flex; flex-direction: column; gap: 16px; }
.ws-field { display: flex; flex-direction: column; gap: 6px; }
.ws-label { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.ws-options { padding-left: 4px; }
.ws-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.ws-table th, .ws-table td { border: 1px solid var(--n-border-color); padding: 4px 8px; text-align: left; }
.ws-table th { background: var(--n-color-embedded); font-weight: 500; }
.ws-empty { font-size: 13px; color: var(--n-text-color-3); }
</style>

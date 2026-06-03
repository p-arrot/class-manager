<script setup lang="ts">
import { ref } from 'vue'
import { NInput, NRadio, NCheckbox, NTag } from 'naive-ui'

interface Field { id: string; type: string; label: string; options?: string[]; required: boolean; imageUrl?: string }
const props = defineProps<{ schema: string }>()
const model = defineModel<Record<string, any>>({ default: () => ({}) })
const fields = ref<Field[]>([])

try {
  const s = JSON.parse(props.schema || '{"fields":[]}')
  fields.value = s.fields || []
} catch { fields.value = [] }

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
  <div class="survey" v-if="fields.length">
    <div v-for="(f, fi) in fields" :key="f.id" class="question">
      <div class="q-num">{{ fi + 1 }}</div>
      <div class="q-body">
        <div class="q-title">
          {{ f.label }}
          <NTag v-if="f.required" size="tiny" type="error" :bordered="false">必填</NTag>
        </div>
        <img v-if="f.imageUrl" :src="f.imageUrl" class="q-image" />

        <div v-if="f.type === 'radio'" class="q-options">
          <NRadio.Group :value="radioVal(f)" @update:value="(v: string) => setAnswer(f.id, v)">
            <div v-for="(opt, oi) in f.options" :key="oi" class="q-opt">
              <NRadio :value="opt">{{ opt }}</NRadio>
            </div>
          </NRadio.Group>
        </div>

        <div v-else-if="f.type === 'checkbox'" class="q-options">
          <div v-for="(opt, oi) in f.options" :key="oi" class="q-opt">
            <NCheckbox :checked="checkboxVal(f).includes(opt!)" @update:checked="() => toggleCheckbox(f, opt!)">{{ opt }}</NCheckbox>
          </div>
        </div>

        <NInput v-else-if="f.type === 'text'" :value="model[f.id] || ''" @update:value="(v: string) => setAnswer(f.id, v)" placeholder="请输入" />
        <NInput v-else-if="f.type === 'textarea'" type="textarea" :value="model[f.id] || ''" @update:value="(v: string) => setAnswer(f.id, v)" placeholder="请输入" :autosize="{ minRows: 3, maxRows: 10 }" />
      </div>
    </div>
  </div>
  <div v-else class="survey-empty">此学习单暂无题目</div>
</template>

<style scoped>
.survey { display: flex; flex-direction: column; gap: 24px; }
.survey-empty { text-align: center; padding: 40px; color: var(--n-text-color-3); font-size: 14px; }
.question { display: flex; gap: 14px; }
.q-num { width: 32px; height: 32px; border-radius: 50%; background: #7C3AED; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.q-body { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.q-title { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.q-image { max-width: 100%; max-height: 240px; border-radius: 8px; border: 1px solid var(--n-border-color); }
.q-options { display: flex; flex-direction: column; gap: 2px; }
.q-opt { padding: 6px 12px; border-radius: 6px; }
.q-opt:hover { background: var(--n-color-embedded); }
</style>

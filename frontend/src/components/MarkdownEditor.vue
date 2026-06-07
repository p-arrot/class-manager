<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { NButton, NButtonGroup } from 'naive-ui'
import MarkdownView from '@/components/MarkdownView.vue'
import type * as Monaco from 'monaco-editor/esm/vs/editor/editor.api'

const model = defineModel<string>({ default: '' })
const props = withDefaults(defineProps<{ minHeight?: number }>(), { minHeight: 220 })

const mode = ref<'edit' | 'preview'>('edit')
const host = ref<HTMLElement | null>(null)
const editorLoading = ref(false)
let editor: Monaco.editor.IStandaloneCodeEditor | null = null
let disposed = false

onMounted(async () => {
  await nextTick()
  if (!host.value) return
  editorLoading.value = true
  const [monaco] = await Promise.all([
    import('monaco-editor/esm/vs/editor/editor.api'),
    import('monaco-editor/esm/vs/basic-languages/markdown/markdown.contribution'),
  ])
  if (disposed || !host.value) return
  editor = monaco.editor.create(host.value, {
    value: model.value,
    language: 'markdown',
    minimap: { enabled: false },
    lineNumbers: 'off',
    wordWrap: 'on',
    scrollBeyondLastLine: false,
    automaticLayout: true,
    fontSize: 14,
    tabSize: 2,
    padding: { top: 10, bottom: 10 },
  })
  editor.onDidChangeModelContent(() => {
    const value = editor?.getValue() ?? ''
    if (value !== model.value) model.value = value
  })
  editorLoading.value = false
})

watch(model, value => {
  if (editor && editor.getValue() !== value) editor.setValue(value)
})

onBeforeUnmount(() => {
  disposed = true
  editor?.dispose()
  editor = null
})
</script>

<template>
  <div class="markdown-editor">
    <div class="editor-toolbar">
      <NButtonGroup>
        <NButton size="tiny" :type="mode === 'edit' ? 'primary' : 'default'" @click="mode = 'edit'">源码</NButton>
        <NButton size="tiny" :type="mode === 'preview' ? 'primary' : 'default'" @click="mode = 'preview'">预览</NButton>
      </NButtonGroup>
    </div>
    <div v-show="mode === 'edit'" ref="host" class="editor-host" :style="{ minHeight: `${props.minHeight}px` }">
      <span v-if="editorLoading" class="editor-loading">编辑器加载中</span>
    </div>
    <div v-show="mode === 'preview'" class="preview-pane" :style="{ minHeight: `${props.minHeight}px` }">
      <MarkdownView :content="model" />
    </div>
  </div>
</template>

<style scoped>
.markdown-editor { border: 1px solid var(--n-border-color); border-radius: 8px; overflow: hidden; background: var(--n-color); }
.editor-toolbar { display: flex; justify-content: flex-end; padding: 8px; border-bottom: 1px solid var(--n-border-color); background: var(--n-color-embedded); }
.editor-host { width: 100%; }
.editor-loading { display: inline-block; padding: 12px 14px; color: var(--n-text-color-3); font-size: 13px; }
.preview-pane { padding: 14px 16px; overflow: auto; }
</style>

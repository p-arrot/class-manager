<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const props = defineProps<{ content?: string | null }>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const rendered = computed(() => {
  const html = md.render((props.content || '').trim())
  return DOMPurify.sanitize(html)
})
</script>

<template>
  <div class="markdown-view" v-html="rendered" />
</template>

<style scoped>
.markdown-view { font-size: 14px; line-height: 1.75; color: var(--n-text-color); overflow-wrap: anywhere; }
.markdown-view :deep(*) { max-width: 100%; }
.markdown-view :deep(p) { margin: 0 0 10px; }
.markdown-view :deep(p:last-child) { margin-bottom: 0; }
.markdown-view :deep(ul),
.markdown-view :deep(ol) { margin: 8px 0 10px 20px; padding: 0; }
.markdown-view :deep(table) { border-collapse: collapse; width: max-content; max-width: 100%; margin: 10px 0; overflow: auto; display: block; }
.markdown-view :deep(th),
.markdown-view :deep(td) { border: 1px solid var(--n-border-color); padding: 6px 8px; text-align: left; }
.markdown-view :deep(blockquote) { margin: 10px 0; padding: 8px 12px; border-left: 3px solid var(--n-primary-color); background: var(--n-color-embedded); color: var(--n-text-color-2); }
.markdown-view :deep(code) { padding: 2px 5px; border-radius: 4px; background: var(--n-color-embedded); font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.92em; }
.markdown-view :deep(pre) { margin: 10px 0; padding: 12px; border-radius: 8px; background: var(--n-color-embedded); overflow: auto; }
.markdown-view :deep(pre code) { padding: 0; background: transparent; }
.markdown-view :deep(a) { color: var(--n-primary-color); }
</style>

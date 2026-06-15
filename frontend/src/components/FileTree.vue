<script setup lang="ts">
import { computed, h } from 'vue'
import { NTree, NIcon, NEmpty } from 'naive-ui'
import type { TreeOption } from 'naive-ui'
import { FolderOutline } from '@vicons/ionicons5'
import type { CourseResourceVO } from '@/types/api'

const props = defineProps<{
  tree: CourseResourceVO[]
  selectedId: number | null
}>()

const emit = defineEmits<{
  select: [id: number | null]
}>()

function buildTreeOptions(nodes: CourseResourceVO[]): TreeOption[] {
  return nodes
    .filter(n => n.type === 'FOLDER')
    .map(n => ({
      key: n.id,
      label: n.name,
      children: n.children?.length ? buildTreeOptions(n.children) : undefined,
    }))
}

const treeOptions = computed(() => {
  const opts = buildTreeOptions(props.tree)
  return opts.length > 0
    ? [{ key: 0, label: '根目录', children: opts } as TreeOption]
    : [{ key: 0, label: '根目录' } as TreeOption]
})

const selectedKey = computed(() => {
  if (props.selectedId === null || props.selectedId === 0) return 0
  return props.selectedId
})

function handleSelect(keys: (string | number)[], _option: TreeOption[]) {
  const key = keys.length > 0 ? Number(keys[0]) : null
  emit('select', key === 0 ? null : key)
}

function nodeProps({ option }: { option: TreeOption }) {
  return {
    style: { cursor: 'pointer' },
    onClick() {
      if (typeof option.key === 'string' || typeof option.key === 'number') {
        handleSelect([option.key], [])
      }
    },
  }
}

function renderIcon() {
  return h(NIcon, { size: 16 }, () =>
    h(FolderOutline)
  )
}
</script>

<template>
  <div class="file-tree">
    <div class="tree-label">文件夹</div>
    <div class="tree-content">
      <NTree
        v-if="tree.length"
        :data="treeOptions"
        :selected-keys="[selectedKey]"
        :node-props="nodeProps"
        :render-prefix="renderIcon"
        default-expand-all
        block-line
        :indent="16"
        :cancelable="false"
      />
      <NEmpty v-else description="暂无文件夹" size="small" />
    </div>
  </div>
</template>

<style scoped>
.file-tree {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid var(--n-border-color);
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  overflow: auto;
  background: var(--n-color-embedded);
}
.tree-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--n-text-color-3);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0 6px 10px;
}
.tree-content {
  flex: 1;
  overflow: auto;
}
</style>

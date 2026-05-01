<script setup lang="ts">
</script>

<template>
  <div class="page">
    <h2 class="page-title">学生管理</h2>
    <p class="page-hint">导入和管理学生信息，支持 Excel 批量导入。此功能将在下一阶段实现。</p>
    <div class="empty-state">
      <div class="empty-icon">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
          <circle cx="8.5" cy="7" r="4"/>
          <line x1="20" y1="8" x2="20" y2="14"/>
          <line x1="23" y1="11" x2="17" y2="11"/>
        </svg>
      </div>
      <p>暂无学生数据</p>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 900px; }
.page-title { font-size: 22px; font-weight: 600; letter-spacing: -0.01em; color: #1a1a18; margin-bottom: 6px; }
.page-hint { font-size: 14px; color: #8a8a84; margin-bottom: 40px; }
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  border: 1px dashed #d8d6d0;
  border-radius: 12px;
  color: #b0b0a8;
  font-size: 14px;
  gap: 12px;
}
.empty-icon { opacity: 0.3; }
</style>

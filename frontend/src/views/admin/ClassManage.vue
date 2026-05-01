<script setup lang="ts">
</script>

<template>
  <div class="page">
    <h2 class="page-title">班级管理</h2>
    <p class="page-hint">创建和管理学校班级。此功能将在下一阶段实现。</p>
    <div class="empty-state">
      <div class="empty-icon">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
        </svg>
      </div>
      <p>暂无班级数据</p>
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

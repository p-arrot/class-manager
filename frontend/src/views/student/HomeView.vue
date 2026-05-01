<script setup lang="ts">
</script>

<template>
  <div class="page">
    <h2 class="page-title">我的课程</h2>
    <p class="page-hint">课程列表将在教师分配课程后显示。</p>
    <div class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
          <line x1="8" y1="7" x2="16" y2="7"/>
          <line x1="8" y1="11" x2="14" y2="11"/>
        </svg>
      </div>
      <p class="empty-title">暂无课程</p>
      <p class="empty-desc">等待教师分配课程与课时</p>
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
  padding: 80px 0;
  border: 1px dashed #d8d6d0;
  border-radius: 12px;
  gap: 8px;
}
.empty-icon { opacity: 0.25; color: #1a1a18; margin-bottom: 8px; }
.empty-title { font-size: 15px; font-weight: 500; color: #6b6b65; }
.empty-desc { font-size: 13px; color: #b0b0a8; }
</style>

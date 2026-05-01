<script setup lang="ts">
</script>

<template>
  <div class="page">
    <h2 class="page-title">教师工作台</h2>
    <p class="page-hint">课程管理、课堂任务、学生评价等功能将在后续阶段逐步开放。</p>
    <div class="card-grid">
      <div class="feature-card">
        <h3>课程管理</h3>
        <p>创建课程，管理学期与课时</p>
        <span class="badge">即将上线</span>
      </div>
      <div class="feature-card">
        <h3>课堂任务</h3>
        <p>学习单与课堂作品任务</p>
        <span class="badge">即将上线</span>
      </div>
      <div class="feature-card">
        <h3>学生评价</h3>
        <p>四维度评分与雷达图</p>
        <span class="badge">即将上线</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 900px; }
.page-title { font-size: 22px; font-weight: 600; letter-spacing: -0.01em; color: #1a1a18; margin-bottom: 6px; }
.page-hint { font-size: 14px; color: #8a8a84; margin-bottom: 36px; }

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.feature-card {
  padding: 24px;
  border: 1px solid #e8e6e1;
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.2s ease;
}
.feature-card:hover { border-color: #c4c2bc; }
.feature-card h3 { font-size: 15px; font-weight: 600; color: #1a1a18; margin-bottom: 6px; }
.feature-card p { font-size: 13px; color: #8a8a84; margin-bottom: 16px; }
.badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  color: #8a8a84;
  padding: 3px 10px;
  background: #f5f4f1;
  border-radius: 20px;
}
</style>

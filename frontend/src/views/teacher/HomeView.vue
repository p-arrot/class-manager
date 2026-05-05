<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'

const router = useRouter()

function goCourses() { router.push('/teacher/courses') }
</script>

<template>
  <div class="page">
    <h2 class="page-title">教师工作台</h2>
    <p class="page-hint">信息科技课堂的教学管理与评价中心</p>
    <div class="card-grid">
      <div class="feature-card active" @click="goCourses">
        <h3>课程管理</h3>
        <p>创建课程，管理学期与课时</p>
        <NButton size="tiny" text type="primary">立即使用</NButton>
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
.page { max-width: 900px; animation: fadein 200ms ease;}
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
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
  transition: border-color 0.2s ease, transform 0.15s ease, box-shadow 0.15s ease;
}
.feature-card.active {
  cursor: pointer;
}
.feature-card.active:hover {
  border-color: var(--n-primary-color-hover);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}
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

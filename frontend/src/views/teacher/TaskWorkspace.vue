<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NButton, NCard, NGrid, NGi, NIcon } from 'naive-ui'
import { BookOutline, ReaderOutline, StatsChartOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()

const actions = [
  {
    title: '创建学习任务',
    desc: '进入课程课时后，为课时添加作业、练习或作品提交任务。',
    button: '选择课程',
    icon: BookOutline,
    to: '/teacher/courses',
  },
  {
    title: '处理待评分',
    desc: '从工作台查看最近提交，进入评分页面完成评价。',
    button: '查看工作台',
    icon: ReaderOutline,
    to: '/teacher/home',
  },
  {
    title: '导出阶段成绩',
    desc: '按学期预览过程性评价、考试和项目成绩，并导出 Excel。',
    button: '成绩导出',
    icon: StatsChartOutline,
    to: '/teacher/grade-export',
  },
]
</script>

<template>
  <div class="page task-workspace">
    <PageHeader title="作业与评分" hint="围绕发布任务、批改提交和汇总成绩组织教师工作" />

    <NGrid :cols="3" :x-gap="16" :y-gap="16" responsive="screen">
      <NGi v-for="item in actions" :key="item.to">
        <NCard class="action-card" hoverable>
          <div class="action-icon">
            <NIcon :size="22"><component :is="item.icon" /></NIcon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <NButton size="small" type="primary" @click="router.push(item.to)">{{ item.button }}</NButton>
        </NCard>
      </NGi>
    </NGrid>
  </div>
</template>

<style scoped>
.task-workspace {
  animation: fadein 180ms ease;
}

@keyframes fadein {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.action-card {
  min-height: 220px;
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--n-color-embedded);
}

.action-card h3 {
  margin: 16px 0 8px;
  font-size: 16px;
}

.action-card p {
  min-height: 42px;
  margin: 0 0 18px;
  color: var(--n-text-color-3);
  font-size: 13px;
  line-height: 1.6;
}
</style>

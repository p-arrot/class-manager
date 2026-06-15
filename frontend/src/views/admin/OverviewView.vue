<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NCard, NGrid, NGi, NIcon, NSkeleton, useMessage } from 'naive-ui'
import { BookOutline, PeopleOutline, SchoolOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import { listClasses } from '@/api/classes'
import { listStudents } from '@/api/students'
import { listTeachers } from '@/api/teachers'
import { getErrorMessage } from '@/utils/error'

interface StatCard {
  label: string
  value: number
  hint: string
  to: string
  icon: typeof SchoolOutline
}

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const classTotal = ref(0)
const teacherTotal = ref(0)
const studentTotal = ref(0)

const cards = computed<StatCard[]>(() => [
  { label: '班级', value: classTotal.value, hint: '维护年级与班级结构', to: '/admin/classes', icon: SchoolOutline },
  { label: '教师', value: teacherTotal.value, hint: '管理教师账号与任课班级', to: '/admin/teachers', icon: PeopleOutline },
  { label: '学生', value: studentTotal.value, hint: '导入学生、重置密码和分班', to: '/admin/students', icon: BookOutline },
])

async function loadOverview() {
  loading.value = true
  try {
    const [classes, teachers, students] = await Promise.all([
      listClasses({ page: 1, size: 1 }),
      listTeachers({ page: 1, size: 1 }),
      listStudents({ page: 1, size: 1 }),
    ])
    classTotal.value = classes.total
    teacherTotal.value = teachers.total
    studentTotal.value = students.total
  } catch (error) {
    message.error(getErrorMessage(error, '概览加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<template>
  <div class="page admin-overview">
    <PageHeader title="系统概览" hint="先确认基础数据，再进入具体管理模块" />

    <NGrid :cols="3" :x-gap="16" :y-gap="16" responsive="screen">
      <NGi v-for="card in cards" :key="card.to">
        <NCard class="stat-card" :bordered="true" hoverable @click="router.push(card.to)">
          <div class="stat-head">
            <div class="stat-icon">
              <NIcon :size="20"><component :is="card.icon" /></NIcon>
            </div>
            <NButton size="tiny" quaternary @click.stop="router.push(card.to)">进入</NButton>
          </div>
          <NSkeleton v-if="loading" text :repeat="2" />
          <template v-else>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-hint">{{ card.hint }}</div>
          </template>
        </NCard>
      </NGi>
    </NGrid>

    <div class="quick-panel">
      <div>
        <h3>建议工作顺序</h3>
        <p>先建立班级，再维护教师与学生账号，最后由教师创建课程和学习任务。</p>
      </div>
      <NButton type="primary" @click="router.push('/admin/classes')">开始配置班级</NButton>
    </div>
  </div>
</template>

<style scoped>
.admin-overview {
  animation: fadein 180ms ease;
}

@keyframes fadein {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.stat-card {
  cursor: pointer;
  min-height: 180px;
}

.stat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--n-text-color-1);
  background: var(--n-color-embedded);
}

.stat-value {
  font-size: 32px;
  line-height: 1.1;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
}

.stat-hint {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--n-text-color-3);
}

.quick-panel {
  margin-top: 18px;
  padding: 18px 20px;
  border: 1px solid var(--n-border-color);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.quick-panel h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.quick-panel p {
  margin: 0;
  color: var(--n-text-color-3);
  font-size: 13px;
}

@media (max-width: 640px) {
  .quick-panel {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>

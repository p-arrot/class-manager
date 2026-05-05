<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { NButton, NInput } from 'naive-ui'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

const account = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

const roleHomeMap: Record<string, string> = {
  admin: '/admin/classes',
  teacher: '/teacher/courses',
  student: '/student/home',
}

const isDark = computed(() => theme.isDark)

async function handleLogin() {
  if (!account.value || !password.value) {
    errorMsg.value = '请输入账号和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await auth.login(account.value, password.value)
    router.replace(roleHomeMap[data.role] || '/login')
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page" :class="{ dark: isDark }">
    <!-- Subtle geometric accent -->
    <div class="bg-accent" />

    <div class="container">
      <!-- Left: Brand -->
      <div class="brand-panel">
        <div class="brand-content">
          <p class="brand-eyebrow">信息科技 · 课堂管理</p>
          <h1 class="brand-title">让每一堂课<br />都有迹可循</h1>
          <p class="brand-desc">
            为中小学信息科技教师打造的课堂管理系统。<br />
            从课程规划到学期总评，一站式完成。
          </p>
        </div>
        <p class="brand-footer">Designed &amp; Developed by Tatakai</p>
      </div>

      <!-- Right: Login form -->
      <div class="form-panel">
        <div class="form-card">
          <h2 class="form-title">登录</h2>
          <p class="form-subtitle">使用账号与密码登录系统</p>

          <div class="form-fields">
            <div class="field-group">
              <label class="field-label">账号</label>
              <NInput
                v-model:value="account"
                size="large"
                placeholder="管理员/教师用用户名，学生用学号"
                :disabled="loading"
                :input-props="{ autocomplete: 'username' }"
                @keyup.enter="handleLogin"
              />
            </div>

            <div class="field-group">
              <label class="field-label">密码</label>
              <NInput
                v-model:value="password"
                type="password"
                size="large"
                placeholder="请输入密码"
                :disabled="loading"
                show-password-on="click"
                :input-props="{ autocomplete: 'current-password' }"
                @keyup.enter="handleLogin"
              />
            </div>

            <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

            <NButton
              type="default"
              size="large"
              :loading="loading"
              block
              class="submit-btn"
              @click="handleLogin"
            >
              登 录
            </NButton>
          </div>

          <div class="form-footer">
            <span class="hint">默认管理员：admin / admin123</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== Page Layout ===== */
.page {
  display: flex;
  min-height: 100vh;
  background: #fafaf9;
  font-family: 'Geist', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  position: relative;
  overflow: hidden;
}
.page.dark {
  background: #141412;
}

/* Subtle geometric background accent */
.bg-accent {
  position: absolute;
  top: -20%;
  right: -10%;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0,0,0,0.015) 0%, transparent 70%);
  pointer-events: none;
}
.dark .bg-accent {
  background: radial-gradient(circle, rgba(255,255,255,0.02) 0%, transparent 70%);
}

/* ===== Two-column layout ===== */
.container {
  display: flex;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px;
  gap: 60px;
  position: relative;
  z-index: 1;
}

/* ===== Left: Brand Panel ===== */
.brand-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-right: 40px;
}
.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-eyebrow {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8a8a84;
  margin-bottom: 20px;
}
.dark .brand-eyebrow {
  color: #6b6b65;
}

.brand-title {
  font-size: 36px;
  font-weight: 600;
  line-height: 1.25;
  color: #1a1a18;
  letter-spacing: -0.02em;
  margin-bottom: 20px;
}
.dark .brand-title {
  color: #e8e6e1;
}

.brand-desc {
  font-size: 15px;
  line-height: 1.6;
  color: #6b6b65;
  max-width: 360px;
}
.dark .brand-desc {
  color: #8a8a84;
}

.brand-footer {
  font-size: 12px;
  color: #b0b0a8;
  margin-top: 40px;
}
.dark .brand-footer {
  color: #5a5a54;
}

/* ===== Right: Form Panel ===== */
.form-panel {
  flex: 0 0 400px;
  display: flex;
  align-items: center;
}

.form-card {
  width: 100%;
  background: #ffffff;
  border: 1px solid #eae8e4;
  border-radius: 16px;
  padding: 40px 36px;
}
.dark .form-card {
  background: #1a1a18;
  border-color: #272725;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a18;
  margin-bottom: 4px;
  letter-spacing: -0.01em;
}
.dark .form-title {
  color: #e8e6e1;
}

.form-subtitle {
  font-size: 14px;
  color: #8a8a84;
  margin-bottom: 32px;
}
.dark .form-subtitle {
  color: #6b6b65;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: #4a4a44;
}
.dark .field-label {
  color: #b0b0a8;
}

.form-error {
  font-size: 13px;
  color: #d94a3a;
  padding: 10px 14px;
  background: #fef5f4;
  border-radius: 8px;
  border: 1px solid #fde8e5;
  margin: 0;
}
.dark .form-error {
  color: #f08070;
  background: rgba(217,74,58,0.08);
  border-color: rgba(217,74,58,0.15);
}

.submit-btn {
  margin-top: 4px;
  height: 46px;
  font-size: 15px;
  letter-spacing: 0.15em;
  background: #1a1a18 !important;
  border-color: #1a1a18 !important;
  color: #fafaf9 !important;
  font-weight: 500 !important;
  border-radius: 10px !important;
}
.submit-btn:hover {
  background: #2e2e2c !important;
  border-color: #2e2e2c !important;
}
.dark .submit-btn {
  background: #e8e6e1 !important;
  border-color: #e8e6e1 !important;
  color: #141412 !important;
}
.dark .submit-btn:hover {
  background: #fafaf9 !important;
  border-color: #fafaf9 !important;
}

.form-footer {
  margin-top: 28px;
  text-align: center;
}
.hint {
  font-size: 12px;
  color: #b0b0a8;
}
.dark .hint {
  color: #5a5a54;
}

/* ===== Responsive: stack on narrow screens ===== */
@media (max-width: 800px) {
  .container {
    flex-direction: column;
    padding: 24px;
    gap: 32px;
  }
  .brand-panel {
    padding-right: 0;
    text-align: center;
  }
  .brand-desc {
    max-width: 100%;
  }
  .brand-title {
    font-size: 28px;
  }
  .form-panel {
    flex: none;
    width: 100%;
  }
  .brand-footer {
    display: none;
  }
  .bg-accent {
    top: -40%;
    right: -40%;
    width: 400px;
    height: 400px;
  }
}
</style>

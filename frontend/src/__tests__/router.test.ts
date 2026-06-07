import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'

// Minimal localStorage mock
const ls: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: (k: string) => ls[k] ?? null,
  setItem: (k: string, v: string) => { ls[k] = v },
  removeItem: (k: string) => { delete ls[k] },
})

function createMockRouter(routes: RouteRecordRaw[]) {
  const router = createRouter({
    history: createWebHistory(),
    routes,
  })
  setupGuards(router)
  return router
}

describe('router guards', () => {
  beforeEach(() => {
    Object.keys(ls).forEach(k => delete ls[k])
    setActivePinia(createPinia())
  })

  it('allows access to public routes when not logged in', async () => {
    const router = createMockRouter([
      { path: '/login', name: 'Login', component: {}, meta: { public: true } },
    ])
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('redirects unauthenticated users to login', async () => {
    const router = createMockRouter([
      { path: '/login', name: 'Login', component: {}, meta: { public: true } },
      { path: '/admin/classes', name: 'AdminClasses', component: {}, meta: { requiresRole: 'admin' } },
      { path: '/', redirect: '/login' },
    ])
    await router.push('/admin/classes')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('redirects authenticated user away from login page', async () => {
    const router = createMockRouter([
      { path: '/login', name: 'Login', component: {}, meta: { public: true } },
      { path: '/admin/overview', name: 'AdminOverview', component: {}, meta: { requiresRole: 'admin' } },
    ])

    // Set auth state
    const auth = useAuthStore()
    auth.$patch({
      token: 'fake-token',
      userInfo: { token: 'x', userId: 1, username: 'admin', name: 'Admin', role: 'admin', classId: null },
    })

    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/admin/overview')
  })

  it('redirects to correct role home based on role', async () => {
    const router = createMockRouter([
      { path: '/login', name: 'Login', component: {}, meta: { public: true } },
      { path: '/teacher/home', name: 'TeacherHome', component: {}, meta: { requiresRole: 'teacher' } },
      { path: '/teacher/courses', name: 'TeacherCourses', component: {}, meta: { requiresRole: 'teacher' } },
      { path: '/student/home', name: 'StudentHome', component: {}, meta: { requiresRole: 'student' } },
    ])

    const auth = useAuthStore()
    auth.$patch({
      token: 't',
      userInfo: { token: 't', userId: 2, username: 'stu', name: 'S', role: 'student', classId: 1 },
    })

    // Student tries to access teacher page
    await router.push('/teacher/courses')
    expect(router.currentRoute.value.path).toBe('/student/home')
  })

  it('allows matching role to access their pages', async () => {
    const router = createMockRouter([
      { path: '/login', name: 'Login', component: {}, meta: { public: true } },
      { path: '/teacher/courses', name: 'TeacherCourses', component: {}, meta: { requiresRole: 'teacher' } },
    ])

    const auth = useAuthStore()
    auth.$patch({
      token: 't',
      userInfo: { token: 't', userId: 2, username: 't1', name: 'T', role: 'teacher', classId: 1 },
    })

    await router.push('/teacher/courses')
    expect(router.currentRoute.value.path).toBe('/teacher/courses')
  })
})

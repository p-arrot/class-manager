import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresRole: 'admin' },
    children: [
      {
        path: 'classes',
        name: 'AdminClasses',
        component: () => import('@/views/admin/ClassManage.vue'),
      },
      {
        path: 'teachers',
        name: 'AdminTeachers',
        component: () => import('@/views/admin/TeacherManage.vue'),
      },
      {
        path: 'students',
        name: 'AdminStudents',
        component: () => import('@/views/admin/StudentManage.vue'),
      },
      {
        path: '',
        redirect: '/admin/classes',
      },
    ],
  },
  {
    path: '/teacher',
    component: () => import('@/layouts/TeacherLayout.vue'),
    meta: { requiresRole: 'teacher' },
    children: [
      {
        path: 'courses',
        name: 'TeacherCourses',
        component: () => import('@/views/teacher/CourseList.vue'),
      },
      {
        path: 'courses/:courseId',
        name: 'TeacherCourseDetail',
        component: () => import('@/views/teacher/CourseDetail.vue'),
      },
      {
        path: 'courses/:courseId/resources',
        name: 'TeacherCourseResources',
        component: () => import('@/views/teacher/CourseResources.vue'),
      },
      {
        path: 'home',
        name: 'TeacherHome',
        component: () => import('@/views/teacher/HomeView.vue'),
      },
      {
        path: '',
        redirect: '/teacher/courses',
      },
    ],
  },
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { requiresRole: 'student' },
    children: [
      {
        path: 'home',
        name: 'StudentHome',
        component: () => import('@/views/student/HomeView.vue'),
      },
      {
        path: 'courses/:courseId',
        name: 'StudentCourseDetail',
        component: () => import('@/views/student/CourseDetail.vue'),
      },
      {
        path: 'courses/:courseId/resources',
        name: 'StudentCourseResources',
        component: () => import('@/views/student/CourseResources.vue'),
      },
      {
        path: '',
        redirect: '/student/home',
      },
    ],
  },
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

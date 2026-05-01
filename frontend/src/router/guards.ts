import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function setupGuards(router: Router) {
  router.beforeEach((to, _from, next) => {
    const auth = useAuthStore()

    // Public routes
    if (to.meta.public) {
      // If already logged in, redirect to role home
      if (auth.isLoggedIn && to.path === '/login') {
        return next(getRoleHome(auth.role!))
      }
      return next()
    }

    // Not logged in
    if (!auth.isLoggedIn) {
      return next('/login')
    }

    // Role check
    const requiredRole = to.meta.requiresRole as string | undefined
    if (requiredRole && auth.role !== requiredRole) {
      return next(getRoleHome(auth.role!))
    }

    next()
  })
}

function getRoleHome(role: string): string {
  switch (role) {
    case 'admin': return '/admin/classes'
    case 'teacher': return '/teacher/home'
    case 'student': return '/student/home'
    default: return '/login'
  }
}

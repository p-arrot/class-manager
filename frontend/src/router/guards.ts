import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function setupGuards(router: Router) {
  router.beforeEach((to, _from) => {
    const auth = useAuthStore()

    // Public routes
    if (to.meta.public) {
      if (auth.isLoggedIn && to.path === '/login') {
        return getRoleHome(auth.role!)
      }
      return true
    }

    // Not logged in
    if (!auth.isLoggedIn) {
      return '/login'
    }

    // Role check
    const requiredRole = to.meta.requiresRole as string | undefined
    if (requiredRole && auth.role !== requiredRole) {
      return getRoleHome(auth.role!)
    }

    return true
  })
}

function getRoleHome(role: string): string {
  switch (role) {
    case 'admin': return '/admin/overview'
    case 'teacher': return '/teacher/home'
    case 'student': return '/student/home'
    default: return '/login'
  }
}

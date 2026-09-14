/**
 * @author HXN
 * @date 2026-08-18 17:31
 * @description 路由配置文件
 *
 * 静态路由仅保留最低限度的骨架路由（登录、Layout + 首页）。
 * 所有业务路由由 permission store 从后端菜单树动态生成并通过 router.addRoute 注册。
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { usePermissionStore } from '@/stores'

// ===== 静态路由（骨架） =====
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/Layout.vue'),
    name: 'Layout',
    redirect: '/home',
    children: [
      // 首页作为静态兜底路由，确保 API 不可用时仍可访问
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/project/ProjectList.vue'),
        meta: { title: '首页' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// ===== 动态路由守卫 =====
router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')

  // /login 路由重定向到 /home（登录已改为弹窗形式）
  if (to.path === '/login') {
    next('/home')
    return
  }

  // 已登录用户：加载菜单树并生成动态路由
  if (token) {
    const permissionStore = usePermissionStore()
    if (!permissionStore.routesGenerated) {
      try {
        await permissionStore.loadMenusAndGenerateRoutes(router)
      } catch {
        // 加载失败时仍标记为已生成，避免无限重试
        permissionStore.routesGenerated = true
      }
      // 重新导航以匹配新生成的路由
      next({ ...to, replace: true })
      return
    }
  }

  // 公开路由（无需登录）
  const publicPaths = ['/', '/home']
  if (publicPaths.includes(to.path)) {
    next()
    return
  }

  // 其他路由需要登录
  if (!token) {
    next('/home')
  } else {
    next()
  }
})

export default router

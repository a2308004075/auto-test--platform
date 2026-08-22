/**
 * @author HXN
 * @date 2026-08-22
 * @description 动态路由工具
 * 从菜单树生成 Vue Router 路由，实现完全动态路由
 */
import type { Router } from 'vue-router'
import { resolveComponent } from '@/utils/componentRegistry'
import type { MenuTreeNode } from '@/api/menu'

/**
 * 从路由路径生成路由 name
 * 例：'settings/profile' -> 'SettingsProfile'
 *     'project/:id/apis' -> 'ProjectApis'
 */
function generateRouteName(path: string): string {
  return path
    .replace(/^\//, '')
    .split('/')
    .filter(s => s && !s.startsWith(':'))
    .map(s => s.charAt(0).toUpperCase() + s.slice(1))
    .join('')
    || 'DynamicRoute'
}

/**
 * 将菜单树中的菜单项注册为 Layout 子路由
 * 递归处理目录和菜单项，跳过按钮类型（menuType=3）
 */
function addMenuRoutes(
  router: Router,
  menus: MenuTreeNode[],
  layoutName: string,
): void {
  for (const menu of menus) {
    // 跳过按钮类型
    if (menu.menuType === 3) continue

    // 有组件和路由路径的菜单项 → 注册为路由
    if (menu.component && menu.routePath) {
      const component = resolveComponent(menu.component)
      if (component) {
        // 路径去掉前导 /，作为 Layout 的相对子路径
        const childPath = menu.routePath.replace(/^\//, '')
        const isProject = menu.routePath.startsWith('/project/')
        const routeName = generateRouteName(childPath)

        router.addRoute(layoutName, {
          path: childPath,
          name: routeName,
          component,
          meta: {
            title: menu.name,
            inProject: isProject,
          },
        })
      }
    }

    // 递归处理子菜单
    if (menu.children && menu.children.length) {
      addMenuRoutes(router, menu.children, layoutName)
    }
  }
}

/**
 * 从菜单树生成全部动态路由并注册到 Router
 * @param router Vue Router 实例
 * @param menuTree 从后端获取的启用状态菜单树
 */
export function generateDynamicRoutes(
  router: Router,
  menuTree: MenuTreeNode[],
): void {
  addMenuRoutes(router, menuTree, 'Layout')
}

/**
 * 注册 /settings → /settings/profile 重定向路由
 */
export function addSettingsRedirect(router: Router, layoutName: string): void {
  router.addRoute(layoutName, {
    path: 'settings',
    redirect: '/settings/profile',
  })
}

/**
 * 注册全局 404 兜底路由（必须在所有动态路由注册完成后调用）
 */
export function addCatchAllRoute(router: Router): void {
  router.addRoute({
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/project/ProjectList.vue'),
    meta: { title: '404', noSidebar: true },
  })
}

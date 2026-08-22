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
 * 注册不在菜单系统中但需要路由的子页面（新建/编辑/详情/导入等）
 * 这些页面不显示在侧边栏，仅通过列表页导航到达
 * 必须在 generateDynamicRoutes 之后、addCatchAllRoute 之前调用
 */
export function addSupplementaryRoutes(router: Router, layoutName: string): void {
  const routes = [
    // ===== 接口模块 =====
    { path: 'project/:id/apis/new',              component: 'api/ApiEdit',          title: '新建接口' },
    { path: 'project/:id/apis/:apiId/edit',      component: 'api/ApiEdit',          title: '编辑接口' },
    { path: 'project/:id/apis/swagger-import',   component: 'api/SwaggerImport',    title: '导入Swagger' },

    // ===== 关键字模块 =====
    { path: 'project/:id/keywords/new',               component: 'keywords/KeywordEdit',  title: '新建关键字' },
    { path: 'project/:id/keywords/:keywordId/edit',   component: 'keywords/KeywordEdit',  title: '编辑关键字' },

    // ===== Action 模块 =====
    { path: 'project/:id/actions/:actionId/edit',  component: 'action/ActionEditor',  title: '编辑Action' },
    { path: 'project/:id/actions/:actionId/debug', component: 'action/ActionDebug',   title: '调试Action' },

    // ===== 测试用例/套件模块 =====
    { path: 'project/:id/cases/new',                component: 'cases/CaseEdit',   title: '新建用例' },
    { path: 'project/:id/cases/:caseId/edit',       component: 'cases/CaseEdit',   title: '编辑用例' },
    { path: 'project/:id/suites/:suiteId/edit',     component: 'cases/SuiteEdit',  title: '步骤配置' },

    // ===== 测试计划/执行模块 =====
    { path: 'project/:id/plans/new',                       component: 'execution/PlanEdit',        title: '新建计划' },
    { path: 'project/:id/plans/:planId/edit',              component: 'execution/PlanEdit',        title: '编辑计划' },
    { path: 'project/:id/executions/:executionId',         component: 'execution/ExecutionDetail', title: '执行详情' },
  ]

  for (const r of routes) {
    const component = resolveComponent(r.component)
    if (component) {
      router.addRoute(layoutName, {
        path: r.path,
        name: generateRouteName(r.path),
        component,
        meta: { title: r.title, inProject: true },
      })
    }
  }
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

import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

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
    redirect: '/project',
    children: [
      {
        path: 'project',
        name: 'ProjectList',
        component: () => import('@/views/project/ProjectList.vue'),
        meta: { title: '项目管理' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '系统设置' },
      },
      // ===== 项目内路由 =====
      {
        path: 'project/:id/dashboard',
        name: 'ProjectDashboard',
        component: () => import('@/views/project/ProjectDashboard.vue'),
        meta: { title: '仪表板', inProject: true },
      },
      {
        path: 'project/:id/apis',
        name: 'ApiList',
        component: () => import('@/views/api/ApiList.vue'),
        meta: { title: '接口管理', inProject: true },
      },
      {
        path: 'project/:id/apis/new',
        name: 'ApiNew',
        component: () => import('@/views/api/ApiEdit.vue'),
        meta: { title: '新建接口', inProject: true },
      },
      {
        path: 'project/:id/apis/:apiId/edit',
        name: 'ApiEdit',
        component: () => import('@/views/api/ApiEdit.vue'),
        meta: { title: '编辑接口', inProject: true },
      },
      {
        path: 'project/:id/apis/:apiId/debug',
        name: 'ApiDebug',
        component: () => import('@/views/api/ApiDebug.vue'),
        meta: { title: '接口调试', inProject: true },
      },
      {
        path: 'project/:id/apis/swagger-import',
        name: 'SwaggerImport',
        component: () => import('@/views/api/SwaggerImport.vue'),
        meta: { title: 'Swagger 导入', inProject: true },
      },
      {
        path: 'project/:id/environments',
        name: 'EnvironmentList',
        component: () => import('@/views/environment/EnvironmentList.vue'),
        meta: { title: '环境配置', inProject: true },
      },
      {
        path: 'project/:id/keywords',
        name: 'KeywordList',
        component: () => import('@/views/keywords/KeywordList.vue'),
        meta: { title: '接口关键字', inProject: true },
      },
      {
        path: 'project/:id/keywords/new',
        name: 'KeywordNew',
        component: () => import('@/views/keywords/KeywordEdit.vue'),
        meta: { title: '新建关键字', inProject: true },
      },
      {
        path: 'project/:id/keywords/:keywordId/edit',
        name: 'KeywordEdit',
        component: () => import('@/views/keywords/KeywordEdit.vue'),
        meta: { title: '编辑关键字', inProject: true },
      },
      {
        path: 'project/:id/tools',
        name: 'ToolList',
        component: () => import('@/views/tool/ToolList.vue'),
        meta: { title: '工具方法', inProject: true },
      },
      {
        path: 'project/:id/actions',
        name: 'ActionList',
        component: () => import('@/views/action/ActionList.vue'),
        meta: { title: 'Action', inProject: true },
      },
      {
        path: 'project/:id/actions/:actionId/edit',
        name: 'ActionEditor',
        component: () => import('@/views/action/ActionEditor.vue'),
        meta: { title: 'Action 编辑', inProject: true },
      },
      {
        path: 'project/:id/actions/:actionId/debug',
        name: 'ActionDebug',
        component: () => import('@/views/action/ActionDebug.vue'),
        meta: { title: 'Action 调试', inProject: true },
      },
      // ===== M8 测试套件/用例 =====
      {
        path: 'project/:id/suites',
        name: 'SuiteList',
        component: () => import('@/views/cases/SuiteList.vue'),
        meta: { title: '测试套件', inProject: true },
      },
      {
        path: 'project/:id/suites/:suiteId/edit',
        name: 'SuiteEdit',
        component: () => import('@/views/cases/SuiteEdit.vue'),
        meta: { title: '编辑套件', inProject: true },
      },
      {
        path: 'project/:id/cases',
        name: 'CaseList',
        component: () => import('@/views/cases/CaseList.vue'),
        meta: { title: '测试用例', inProject: true },
      },
      {
        path: 'project/:id/cases/new',
        name: 'CaseNew',
        component: () => import('@/views/cases/CaseEdit.vue'),
        meta: { title: '新建用例', inProject: true },
      },
      {
        path: 'project/:id/cases/:caseId/edit',
        name: 'CaseEdit',
        component: () => import('@/views/cases/CaseEdit.vue'),
        meta: { title: '编辑用例', inProject: true },
      },
      // ===== M9 测试计划/执行 =====
      {
        path: 'project/:id/plans',
        name: 'PlanList',
        component: () => import('@/views/execution/PlanList.vue'),
        meta: { title: '测试计划', inProject: true },
      },
      {
        path: 'project/:id/plans/new',
        name: 'PlanNew',
        component: () => import('@/views/execution/PlanEdit.vue'),
        meta: { title: '新建计划', inProject: true },
      },
      {
        path: 'project/:id/plans/:planId/edit',
        name: 'PlanEdit',
        component: () => import('@/views/execution/PlanEdit.vue'),
        meta: { title: '编辑计划', inProject: true },
      },
      {
        path: 'project/:id/executions',
        name: 'ExecutionList',
        component: () => import('@/views/execution/ExecutionList.vue'),
        meta: { title: '执行记录', inProject: true },
      },
      {
        path: 'project/:id/executions/:executionId',
        name: 'ExecutionDetail',
        component: () => import('@/views/execution/ExecutionDetail.vue'),
        meta: { title: '执行详情', inProject: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  // /login 路由重定向到 /project（登录已改为弹窗形式）
  if (to.path === '/login') {
    next('/project')
    return
  }
  // 允许未登录访问的公开路由
  const publicPaths = ['/', '/project']
  if (publicPaths.includes(to.path)) {
    next()
    return
  }
  // 其他路由需要登录
  if (!token) {
    next('/project')
  } else {
    next()
  }
})

export default router

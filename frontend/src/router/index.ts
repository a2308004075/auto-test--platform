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
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/project',
    children: [
      {
        path: 'project',
        name: 'Project',
        component: () => import('@/views/project/ProjectList.vue'),
        meta: { title: '项目管理' },
      },
      {
        path: 'api',
        name: 'Api',
        component: () => import('@/views/api/ApiList.vue'),
        meta: { title: '接口管理' },
      },
      {
        path: 'keywords',
        name: 'Keywords',
        component: () => import('@/views/keywords/KeywordList.vue'),
        meta: { title: '关键字管理' },
      },
      {
        path: 'cases',
        name: 'Cases',
        component: () => import('@/views/cases/CaseList.vue'),
        meta: { title: '测试用例' },
      },
      {
        path: 'execution',
        name: 'Execution',
        component: () => import('@/views/execution/ExecutionList.vue'),
        meta: { title: '执行管理' },
      },
      {
        path: 'environment',
        name: 'Environment',
        component: () => import('@/views/environment/EnvironmentList.vue'),
        meta: { title: '环境配置' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '系统设置' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

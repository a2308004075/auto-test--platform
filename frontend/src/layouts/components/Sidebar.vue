<script setup lang="ts">
/**
 * 侧边栏组件
 * 项目上下文感知菜单（复用 MainLayout 菜单逻辑）
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore, useAppStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// 判断当前是否在项目内页面
const inProject = computed(() => route.meta?.inProject === true)
const projectId = computed(() => Number(route.params.id) || 0)

// 侧边栏选中 key
const selectedKey = computed(() => {
  const name = (route.name as string) || ''
  if (name.startsWith('Api') || name === 'SwaggerImport') return 'apis'
  if (name.startsWith('Environment')) return 'environments'
  if (name.startsWith('Keyword')) return 'keywords'
  if (name.startsWith('Tool')) return 'tools'
  if (name.startsWith('Action')) return 'actions'
  if (name.startsWith('Suite')) return 'suites'
  if (name.startsWith('Case')) return 'cases'
  if (name.startsWith('Plan')) return 'plans'
  if (name.startsWith('Execution')) return 'executions'
  if (name === 'ProjectDashboard') return 'dashboard'
  if (name === 'ProjectList') return 'project'
  if (name === 'Settings') return 'settings'
  return ''
})

const menuItems = computed(() => {
  if (inProject.value) {
    const pid = projectId.value
    return [
      { key: 'dashboard', label: '仪表板', path: `/project/${pid}/dashboard` },
      { key: 'apis', label: '接口管理', path: `/project/${pid}/apis` },
      { key: 'environments', label: '环境配置', path: `/project/${pid}/environments` },
      { key: 'keywords', label: '接口关键字', path: `/project/${pid}/keywords` },
      { key: 'tools', label: '工具方法', path: `/project/${pid}/tools` },
      { key: 'actions', label: 'Action', path: `/project/${pid}/actions` },
      { key: 'suites', label: '测试套件', path: `/project/${pid}/suites` },
      { key: 'cases', label: '测试用例', path: `/project/${pid}/cases` },
      { key: 'plans', label: '测试计划', path: `/project/${pid}/plans` },
      { key: 'executions', label: '执行记录', path: `/project/${pid}/executions` },
    ]
  }
  return [
    { key: 'project', label: '项目管理', path: '/project' },
    ...(userStore.isLoggedIn ? [{ key: 'settings', label: '系统设置', path: '/settings' }] : []),
  ]
})

function handleMenuClick({ key }: { key: string }) {
  const item = menuItems.value.find(m => m.key === key)
  if (item) router.push(item.path)
}
</script>

<template>
  <a-layout-sider
    class="sidebar-container"
    collapsible
    :collapsed="!appStore.sidebarOpened"
    :trigger="null"
    :width="200"
    :collapsed-width="48"
  >
    <div class="sidebar-logo">
      <h1 v-if="appStore.sidebarOpened">项目管理平台</h1>
      <h1 v-else>AT</h1>
    </div>
    <a-menu
      theme="dark"
      mode="inline"
      :selected-keys="[selectedKey]"
      @click="handleMenuClick"
    >
      <a-menu-item v-for="item in menuItems" :key="item.key">
        <span>{{ item.label }}</span>
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
</template>

<script setup lang="ts">
/**
 * 侧边栏组件
 * 使用 Element Plus el-menu 实现暗色主题侧边菜单
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House } from '@element-plus/icons-vue'
import { useUserStore, useAppStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// 判断当前是否在项目内页面
const inProject = computed(() => route.meta?.inProject === true)
const projectId = computed(() => Number(route.params.id) || 0)

// 侧边栏选中 key
const activeMenu = computed(() => {
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

const isCollapse = computed(() => !appStore.sidebarOpened)

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
    { key: 'project', label: '首页', path: '/home', icon: House },
    ...(userStore.isLoggedIn ? [{ key: 'settings', label: '系统设置', path: '/settings' }] : []),
  ]
})

function handleMenuSelect(index: string) {
  const item = menuItems.value.find(m => m.key === index)
  if (item) router.push(item.path)
}
</script>

<template>
  <div :class="{ 'has-logo': true }">
    <div class="sidebar-logo">
      <h1 v-if="!isCollapse">项目管理平台</h1>
      <h1 v-else>项目</h1>
    </div>
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        :unique-opened="false"
        :collapse-transition="false"
        mode="vertical"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in menuItems" :key="item.key" :index="item.key">
          <el-icon v-if="item.icon">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

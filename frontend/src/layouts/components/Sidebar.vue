<script setup lang="ts">
/**
 * 侧边栏组件
 * 使用 Element Plus el-menu 实现暗色主题侧边菜单
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, Setting } from '@element-plus/icons-vue'
import { useUserStore, useAppStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// 判断当前是否在项目内页面
const inProject = computed(() => route.meta?.inProject === true)
// 判断当前是否在系统管理页面
const inSettings = computed(() => route.path.startsWith('/settings'))
const projectId = computed(() => Number(route.params.id) || 0)

// 是否管理员
const isAdmin = computed(() => (userStore.role || '').toUpperCase() === 'ADMIN')

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
  if (name === 'Profile') return 'profile'
  if (name === 'UserManagement') return 'users'
  if (name === 'GlobalConfig') return 'global-config'
  return ''
})

const isCollapse = computed(() => !appStore.sidebarOpened)

const projectMenuItems = computed(() => {
  if (!inProject.value) return []
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
})

// 系统管理子菜单
const settingsMenuItems = computed(() => {
  const items = [
    { key: 'profile', label: '个人资料', path: '/settings/profile' },
  ]
  if (isAdmin.value) {
    items.push({ key: 'users', label: '用户列表', path: '/settings/users' })
    items.push({ key: 'global-config', label: '全局设置', path: '/settings/global-config' })
  }
  return items
})

function handleMenuSelect(index: string) {
  // 先查项目菜单
  const pItem = projectMenuItems.value.find(m => m.key === index)
  if (pItem) { router.push(pItem.path); return }
  // 再查系统设置子菜单
  const sItem = settingsMenuItems.value.find(m => m.key === index)
  if (sItem) { router.push(sItem.path) }
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
        <!-- 项目内菜单 -->
        <template v-if="inProject">
          <el-menu-item v-for="item in projectMenuItems" :key="item.key" :index="item.key">
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
        <!-- 非项目页菜单 -->
        <template v-else>
          <!-- 首页：仅显示首页，不显示系统管理 -->
          <el-menu-item v-if="!inSettings" index="project">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <!-- 系统管理：仅显示系统管理，不显示首页 -->
          <el-sub-menu v-if="userStore.isLoggedIn && inSettings" index="settings">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item v-for="item in settingsMenuItems" :key="item.key" :index="item.key">
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

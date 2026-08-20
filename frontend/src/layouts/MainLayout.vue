<script setup lang="ts">
/**
 * 主布局组件
 * 项目上下文感知侧边栏 + 顶部栏 + 内容区域
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import { message } from 'ant-design-vue'
import { logout as logoutApi } from '@/api/auth'
import LoginModal from '@/views/auth/LoginModal.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()

// 登录弹窗
const loginModalOpen = ref(false)

// 判断当前是否在项目内页面
const inProject = computed(() => route.meta?.inProject === true)
const projectId = computed(() => Number(route.params.id) || 0)

// 侧边栏选中 key
const selectedKey = computed(() => {
  const name = route.name as string || ''
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

async function handleLogout() {
  try { await logoutApi() } catch { /* ignore */ }
  userStore.logout()
  projectStore.clearCurrentProject()
  message.success('已退出登录')
  router.push('/project')
}
</script>

<template>
  <a-layout class="main-layout">
    <a-layout-sider collapsible :width="200">
      <div class="logo">
        <h1>项目管理平台</h1>
      </div>
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="[selectedKey]"
        @click="handleMenuClick"
      >
        <a-menu-item v-for="item in menuItems" :key="item.key">
          {{ item.label }}
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="layout-header">
        <div class="header-left">
          <span v-if="inProject" class="project-badge">
            {{ projectStore.currentProjectName || '项目' }}
          </span>
          <span>{{ route.meta?.title || '' }}</span>
        </div>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <span v-if="userStore.username" class="user-info">{{ userStore.username }}</span>
            <a-button type="link" size="small" @click="handleLogout">退出</a-button>
          </template>
          <template v-else>
            <a-button type="primary" size="small" @click="loginModalOpen = true">立即登录</a-button>
          </template>
        </div>
      </a-layout-header>
      <a-layout-content class="layout-content">
        <router-view />
      </a-layout-content>
    </a-layout>

    <!-- 登录弹窗 -->
    <LoginModal v-model:open="loginModalOpen" @success="() => {}" />
  </a-layout>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.logo h1 {
  font-size: 16px;
  margin: 0;
  white-space: nowrap;
}
.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  height: 48px;
  line-height: 48px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.project-badge {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-info {
  color: #666;
  font-size: 13px;
}
.layout-content {
  margin: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
}
</style>

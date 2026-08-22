<!--
 @author HXN
 @date 2026-08-20 23:57
 @description 顶部导航栏组件
-->
<script setup lang="ts">
/**
 * 顶栏组件
 * 面包屑 + 项目徽标 + 用户/登录区
 * 右上角用户信息：角色头像 + 显示名 + 下拉菜单（首页/系统管理/退出登录）
 */
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore, useProjectStore, useTagsViewStore, usePermissionStore } from '@/stores'
import { logout as logoutApi } from '@/api/auth'
import Hamburger from '@/components/Hamburger/index.vue'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import LoginModal from '@/views/auth/LoginModal.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()
const tagsViewStore = useTagsViewStore()
const permissionStore = usePermissionStore()

// 登录弹窗
const loginModalOpen = ref(false)

// 判断当前是否在项目内页面
const inProject = computed(() => route.meta?.inProject === true)

// 角色首字母（头像显示）
const roleInitial = computed(() => {
  const r = (userStore.role || 'user').toLowerCase()
  if (r === 'admin') return 'A'
  if (r === 'user') return 'U'
  return r.charAt(0).toUpperCase()
})

// 显示名
const displayName = computed(() => userStore.displayName || userStore.username || '')

// 是否管理员（未登录时即使 localStorage 残留 role 也不视为管理员）
const isAdmin = computed(() => userStore.isLoggedIn && (userStore.role || '').toUpperCase() === 'ADMIN')

onMounted(() => {
  // 页面刷新后恢复用户信息
  if (userStore.isLoggedIn && !userStore.username) {
    userStore.fetchCurrentUser()
  }
})

async function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try { await logoutApi() } catch { /* ignore */ }
    userStore.logout()
    projectStore.clearCurrentProject()
    tagsViewStore.delAllViews()
    permissionStore.reset()
    ElMessage.success('已退出登录')
    router.push('/home')
  }).catch(() => {})
}

function handleLoginSuccess() {
  router.push('/home')
}

// 下拉菜单命令
function handleCommand(command: string) {
  switch (command) {
    case 'home':
      router.push('/home')
      break
    case 'settings':
      router.push(isAdmin.value ? '/settings/users' : '/settings/profile')
      break
    case 'logout':
      handleLogout()
      break
  }
}
</script>

<template>
  <div class="navbar">
    <div class="navbar-left">
      <Hamburger />
      <Breadcrumb />
      <span v-if="inProject" class="project-badge">
        {{ projectStore.currentProjectName || '项目' }}
      </span>
    </div>
    <div class="navbar-right">
      <template v-if="userStore.isLoggedIn">
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="header-user">
            <div class="user-role" :title="userStore.role">{{ roleInitial }}</div>
            <span class="user-name">{{ displayName }}</span>
            <span class="user-arrow">▼</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <div class="user-dropdown-header">
                <div class="user-dropdown-meta">
                  <div>账号：{{ userStore.username }}</div>
                  <div>角色：{{ userStore.role }}</div>
                </div>
              </div>
              <el-dropdown-item command="home">
                <span class="dropdown-icon">⌂</span>首页
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <span class="dropdown-icon">⚙</span>系统管理
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <span class="dropdown-icon">→</span>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template v-else>
        <el-button type="primary" size="small" @click="loginModalOpen = true">立即登录</el-button>
      </template>
    </div>
  </div>

  <!-- 登录弹窗 -->
  <LoginModal v-model="loginModalOpen" @success="handleLoginSuccess" />
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding-right: 16px;
}
.navbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.project-badge {
  background: #ecf5ff;
  color: #409eff;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 用户信息区域 */
.header-user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  user-select: none;
  outline: none;
}
.header-user:hover {
  color: #303133;
}
.user-role {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-name {
  font-weight: 500;
  white-space: nowrap;
}
.user-arrow {
  font-size: 10px;
  color: #909399;
  transition: transform 0.2s ease;
}

/* 下拉菜单 */
:deep(.user-dropdown-header) {
  padding: 10px 14px 8px;
  border-bottom: 1px solid #f0f0f0;
}
:deep(.user-dropdown-meta) {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.dropdown-icon {
  display: inline-block;
  width: 16px;
  text-align: center;
  margin-right: 4px;
}
</style>

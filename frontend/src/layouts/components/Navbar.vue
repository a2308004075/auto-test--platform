<script setup lang="ts">
/**
 * 顶栏组件
 * 面包屑 + 项目徽标 + 用户/登录区
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore, useProjectStore } from '@/stores'
import { logout as logoutApi } from '@/api/auth'
import Hamburger from '@/components/Hamburger/index.vue'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import LoginModal from '@/views/auth/LoginModal.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()

// 登录弹窗
const loginModalOpen = ref(false)

// 判断当前是否在项目内页面
const inProject = computed(() => route.meta?.inProject === true)

async function handleLogout() {
  try { await logoutApi() } catch { /* ignore */ }
  userStore.logout()
  projectStore.clearCurrentProject()
  ElMessage.success('已退出登录')
  router.push('/project')
}

function handleLoginSuccess() {
  router.push('/project')
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
        <span v-if="userStore.username" class="user-info">{{ userStore.username }}</span>
        <el-button type="primary" link size="small" @click="handleLogout">退出</el-button>
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
.user-info {
  color: #606266;
  font-size: 13px;
}
</style>

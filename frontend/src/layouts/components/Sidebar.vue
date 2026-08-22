<!--
 @author HXN
 @date 2026-08-20 23:57
 @description 侧边栏导航组件（完全动态菜单）
-->
<script setup lang="ts">
/**
 * 侧边栏组件
 * 所有菜单项从 permission store 的菜单树中动态加载
 * 根据当前页面上下文（首页 / 系统管理 / 项目内）显示对应的菜单子树
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, Setting } from '@element-plus/icons-vue'
import { useUserStore, useAppStore, usePermissionStore } from '@/stores'
import type { MenuTreeNode } from '@/api/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

// ===== 上下文判断 =====
const inProject = computed(() => route.meta?.inProject === true)
const inSettings = computed(() => route.path.startsWith('/settings'))
const projectId = computed(() => Number(route.params.id) || 0)
const isCollapse = computed(() => !appStore.sidebarOpened)

// ===== 动态菜单项 =====
interface MenuItem {
  key: string
  label: string
  path: string
}

/**
 * 将菜单树节点展平为 MenuItem 数组
 * 跳过按钮类型（menuType=3）和无路由路径的目录
 */
function flattenMenuNodes(nodes: MenuTreeNode[], pathPrefix = ''): MenuItem[] {
  const result: MenuItem[] = []
  const sorted = [...nodes].sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
  for (const node of sorted) {
    if (node.menuType === 3) continue
    if (node.routePath) {
      let path = node.routePath
      // 项目菜单路径中的 :id 替换为实际项目 ID
      if (path.includes(':id')) {
        path = path.replace(':id', String(pathPrefix || projectId.value))
      }
      result.push({ key: node.routePath, label: node.name, path })
    }
    if (node.children?.length) {
      result.push(...flattenMenuNodes(node.children, pathPrefix))
    }
  }
  return result
}

/** 系统管理菜单项 */
const settingsMenuItems = computed<MenuItem[]>(() =>
  flattenMenuNodes(permissionStore.settingsMenus),
)

/** 项目内菜单项 */
const projectMenuItems = computed<MenuItem[]>(() =>
  flattenMenuNodes(permissionStore.projectMenus, String(projectId.value)),
)

/** 首页菜单项 */
const homeMenuItem = computed<MenuItem | null>(() => {
  const home = permissionStore.rootMenus.find(n => n.routePath === '/home')
  if (home) return { key: '/home', label: home.name, path: '/home' }
  // 静态兜底
  return { key: '/home', label: '首页', path: '/home' }
})

// ===== 侧边栏选中 key（路径匹配，最长前缀优先） =====
const activeMenu = computed(() => {
  const currentPath = route.path

  // 首页精确匹配
  if (currentPath === '/home') return '/home'

  // 收集当前上下文的所有菜单项
  let candidates: MenuItem[] = []
  if (inProject.value) {
    candidates = projectMenuItems.value
  } else if (inSettings.value) {
    candidates = settingsMenuItems.value
  }

  // 最长前缀匹配
  let bestMatch = ''
  for (const item of candidates) {
    if (currentPath.startsWith(item.path) && item.path.length > bestMatch.length) {
      bestMatch = item.path
    }
  }
  return bestMatch
})

// ===== 菜单点击处理 =====
function handleMenuSelect(index: string) {
  // 查找匹配的菜单项并跳转
  const allItems = inProject.value
    ? projectMenuItems.value
    : inSettings.value
      ? settingsMenuItems.value
      : homeMenuItem.value ? [homeMenuItem.value] : []

  const item = allItems.find(m => m.key === index || m.path === index)
  if (item) {
    router.push(item.path)
  } else if (index === 'home') {
    router.push('/home')
  }
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
        <!-- ===== 项目内菜单 ===== -->
        <template v-if="inProject">
          <el-menu-item
            v-for="item in projectMenuItems"
            :key="item.key"
            :index="item.key"
          >
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>

        <!-- ===== 非项目页面 ===== -->
        <template v-else>
          <!-- 首页 -->
          <el-menu-item v-if="!inSettings && homeMenuItem" index="home">
            <el-icon><House /></el-icon>
            <span>{{ homeMenuItem.label }}</span>
          </el-menu-item>

          <!-- 系统管理 -->
          <el-sub-menu
            v-if="userStore.isLoggedIn && inSettings && settingsMenuItems.length"
            index="settings"
          >
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item
              v-for="item in settingsMenuItems"
              :key="item.key"
              :index="item.key"
            >
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

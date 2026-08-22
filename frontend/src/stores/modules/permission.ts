/**
 * @author HXN
 * @date 2026-08-20 23:57
 * @description 权限状态 Store（动态菜单 + 动态路由）
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Router } from 'vue-router'
import { getMenuTree, type MenuTreeNode } from '@/api/menu'
import { generateDynamicRoutes, addSupplementaryRoutes, addSettingsRedirect, addCatchAllRoute } from '@/utils/routeUtils'

/**
 * 菜单与动态路由状态管理
 * 负责从后端加载菜单树、生成动态路由、驱动侧边栏渲染
 */
export const usePermissionStore = defineStore('permission', () => {
  /** 菜单树（启用状态，从后端加载） */
  const menuTree = ref<MenuTreeNode[]>([])

  /** 动态路由是否已生成 */
  const routesGenerated = ref(false)

  /** 菜单是否正在加载中 */
  const menuLoading = ref(false)

  // ===== 计算属性：按上下文提取菜单子树 =====

  /** 顶级菜单项（首页等） */
  const rootMenus = computed(() =>
    menuTree.value.filter(n => n.parentId === 0 && n.menuType !== 1),
  )

  /** 系统管理目录的子菜单 */
  const settingsMenus = computed(() => {
    const dir = menuTree.value.find(n => n.parentId === 0 && n.name === '系统管理')
    return dir?.children || []
  })

  /** 项目菜单目录的子菜单 */
  const projectMenus = computed(() => {
    const dir = menuTree.value.find(n => n.parentId === 0 && n.name === '项目菜单')
    return dir?.children || []
  })

  /**
   * 加载菜单树并生成动态路由
   * 在路由守卫中调用，确保导航前路由已就绪
   */
  async function loadMenusAndGenerateRoutes(router: Router): Promise<void> {
    if (routesGenerated.value || menuLoading.value) return

    menuLoading.value = true
    try {
      const res: any = await getMenuTree()
      menuTree.value = res.data || []

      // 生成动态路由
      generateDynamicRoutes(router, menuTree.value)
      addSupplementaryRoutes(router, 'Layout')
      addSettingsRedirect(router, 'Layout')
      addCatchAllRoute(router)

      routesGenerated.value = true
    } catch {
      // API 失败时仍然首页可见（静态路由兜底）
      menuTree.value = []
      routesGenerated.value = true
    } finally {
      menuLoading.value = false
    }
  }

  /**
   * 重新加载菜单树（菜单管理页保存后调用）
   */
  async function reloadMenuTree(): Promise<void> {
    try {
      const res: any = await getMenuTree()
      menuTree.value = res.data || []
    } catch {
      // ignore
    }
  }

  /**
   * 重置状态（退出登录时调用）
   */
  function reset(): void {
    menuTree.value = []
    routesGenerated.value = false
    menuLoading.value = false
  }

  return {
    menuTree,
    routesGenerated,
    menuLoading,
    rootMenus,
    settingsMenus,
    projectMenus,
    loadMenusAndGenerateRoutes,
    reloadMenuTree,
    reset,
  }
})

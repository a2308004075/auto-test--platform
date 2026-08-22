/**
 * @author HXN
 * @date 2026-08-20 23:57
 * @description 权限状态 Store
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由权限状态（动态路由生成的桩，当前路由为静态定义，预留扩展）
 */
export const usePermissionStore = defineStore('permission', () => {
  /** 最终可访问的路由 */
  const routes = ref<RouteRecordRaw[]>([])
  /** 动态生成的路由 */
  const dynamicRoutes = ref<RouteRecordRaw[]>([])

  function setRoutes(newRoutes: RouteRecordRaw[]) {
    routes.value = newRoutes
  }

  return { routes, dynamicRoutes, setRoutes }
})

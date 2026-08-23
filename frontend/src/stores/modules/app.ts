/**
 * @author HXN
 * @date 2026-08-20 23:57
 * @description 应用状态 Store
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 应用全局状态（侧边栏折叠）
 */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否展开 */
  const sidebarOpened = ref(true)

  function toggleSideBar() {
    sidebarOpened.value = !sidebarOpened.value
  }

  return { sidebarOpened, toggleSideBar }
})

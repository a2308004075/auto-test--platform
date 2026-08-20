import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 应用全局状态（侧边栏折叠 / 设备类型）
 */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否展开 */
  const sidebarOpened = ref(true)
  /** 设备类型 */
  const device = ref<'desktop' | 'mobile'>('desktop')

  function toggleSideBar() {
    sidebarOpened.value = !sidebarOpened.value
  }

  function setDevice(d: 'desktop' | 'mobile') {
    device.value = d
  }

  return { sidebarOpened, device, toggleSideBar, setDevice }
})

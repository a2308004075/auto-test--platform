/**
 * @author HXN
 * @date 2026-08-30
 * @description el-tabs 激活状态与路由 query 参数同步 composable
 */
import { ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * 页面内 el-tabs 的激活 tab 与 URL ?tab= 参数双向同步：
 * - 初始化时从 route.query.tab 恢复（非法值或不存在时回退默认 tab）
 * - 切换 tab 时通过 router.replace 更新地址栏（默认 tab 移除参数，保持地址干净）
 * 效果：刷新页面后停留在当前选项卡；分享链接可直接定位到指定选项卡
 *
 * @param validTabs 该页面全部合法的 tab name（条件渲染的 tab 需按当前模式过滤后传入）
 * @param defaultTab 默认激活的 tab
 */
export function useRouteTab<T extends string>(validTabs: readonly T[], defaultTab: T): Ref<T> {
  const route = useRoute()
  const router = useRouter()

  const queryTab = typeof route.query.tab === 'string' ? route.query.tab : ''
  // T extends string 无嵌套响应式结构，断言收敛 Vue ref 重载推断出的联合类型
  const activeTab = ref(validTabs.includes(queryTab as T) ? (queryTab as T) : defaultTab) as Ref<T>

  watch(activeTab, (tab) => {
    const query: Record<string, any> = { ...route.query }
    if (tab === defaultTab) {
      delete query.tab
    } else {
      query.tab = tab
    }
    router.replace({ query }).catch(() => {})
  })

  return activeTab
}

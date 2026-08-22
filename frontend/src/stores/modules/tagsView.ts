/**
 * @author HXN
 * @date 2026-08-20 23:57
 * @description 页签状态 Store
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface TagView {
  /** 路由 name，用于 keep-alive 缓存 */
  name: string
  /** 完整路径 */
  path: string
  /** 显示标题 */
  title: string
  /** 是否固钉（不可关闭） */
  affix?: boolean
}

/**
 * 标签页视图状态（已访问视图 + 缓存视图）
 */
export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<TagView[]>([])
  const cachedViews = ref<string[]>([])

  function addView(view: TagView) {
    if (visitedViews.value.some(v => v.path === view.path)) return
    visitedViews.value.push(view)
    if (view.name && !cachedViews.value.includes(view.name)) {
      cachedViews.value.push(view.name)
    }
  }

  function delView(view: TagView) {
    visitedViews.value = visitedViews.value.filter(v => v.path !== view.path)
    cachedViews.value = cachedViews.value.filter(name => name !== view.name)
  }

  function delOthersViews(view: TagView) {
    visitedViews.value = visitedViews.value.filter(v => v.affix || v.path === view.path)
    cachedViews.value = cachedViews.value.filter(name => name === view.name)
  }

  function delAllViews() {
    visitedViews.value = visitedViews.value.filter(v => v.affix)
    cachedViews.value = []
  }

  return { visitedViews, cachedViews, addView, delView, delOthersViews, delAllViews }
})

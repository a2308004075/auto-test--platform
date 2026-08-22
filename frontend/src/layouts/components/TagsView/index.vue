<script setup lang="ts">
/**
 * 标签页栏
 * 读取 tagsView store，支持关闭/切换/右键菜单
 */
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore, type TagView } from '@/stores'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

const contextMenuVisible = ref(false)
const contextMenuTop = ref(0)
const contextMenuLeft = ref(0)
const selectedView = ref<TagView | null>(null)

onMounted(() => {
  // 首次加载时添加当前路由
  addCurrentTag()
  document.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})

watch(
  () => route.path,
  () => addCurrentTag(),
)

function addCurrentTag() {
  const name = (route.name as string) || ''
  const title = (route.meta?.title as string) || ''
  if (name && title) {
    tagsViewStore.addView({
      name,
      path: route.path,
      title,
      affix: name === 'ProjectList',
    } as TagView)
  }
}

function handleClick(view: TagView) {
  router.push(view.path)
}

function handleClose(view: TagView) {
  tagsViewStore.delView(view)
  // 如果关闭的是当前页，跳转到最后一个
  if (view.path === route.path) {
    const last = tagsViewStore.visitedViews[tagsViewStore.visitedViews.length - 1]
    router.push(last ? last.path : '/home')
  }
}

function openContextMenu(e: MouseEvent, view: TagView) {
  e.preventDefault()
  selectedView.value = view
  contextMenuTop.value = e.clientY
  contextMenuLeft.value = e.clientX
  contextMenuVisible.value = true
}

function closeContextMenu() {
  contextMenuVisible.value = false
  selectedView.value = null
}

function handleRefresh() {
  if (!selectedView.value) return
  // 先切换到目标页签，再执行 F5 整页刷新
  if (route.path !== selectedView.value.path) {
    router.push(selectedView.value.path).then(() => {
      window.location.reload()
    })
  } else {
    window.location.reload()
  }
  closeContextMenu()
}

function handleMenuClose() {
  if (!selectedView.value || selectedView.value.affix) return
  handleClose(selectedView.value)
  closeContextMenu()
}

function handleCloseOthers() {
  if (!selectedView.value) return
  tagsViewStore.delOthersViews(selectedView.value)
  // 若当前路由已被关闭，则跳转到保留的目标页签
  if (!tagsViewStore.visitedViews.some(v => v.path === route.path)) {
    router.push(selectedView.value.path)
  }
  closeContextMenu()
}

function handleCloseAll() {
  tagsViewStore.delAllViews()
  // 若当前路由已被关闭，则跳转到保留的第一个页签（通常为首页）
  if (!tagsViewStore.visitedViews.some(v => v.path === route.path)) {
    const first = tagsViewStore.visitedViews[0]
    router.push(first ? first.path : '/home')
  }
  closeContextMenu()
}
</script>

<template>
  <div class="tags-view" v-if="tagsViewStore.visitedViews.length">
    <div class="tags-view-scroll">
      <div
        v-for="view in tagsViewStore.visitedViews"
        :key="view.path"
        class="tag-item"
        :class="{ active: view.path === route.path }"
        @click="handleClick(view)"
        @contextmenu.prevent="openContextMenu($event, view)"
      >
        <span class="tag-title">{{ view.title }}</span>
        <span
          v-if="!view.affix"
          class="tag-close"
          @click.stop="handleClose(view)"
        >
          ×
        </span>
      </div>
    </div>
  </div>

  <ul
    v-show="contextMenuVisible"
    class="context-menu"
    :style="{ top: contextMenuTop + 'px', left: contextMenuLeft + 'px' }"
  >
    <li @click="handleRefresh">刷新</li>
    <li
      :class="{ disabled: selectedView?.affix }"
      @click="handleMenuClose"
    >
      关闭
    </li>
    <li @click="handleCloseOthers">关闭其他</li>
    <li @click="handleCloseAll">关闭所有</li>
  </ul>
</template>

<style scoped>
.tags-view {
  height: 34px;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
  padding: 0 8px;
  display: flex;
  align-items: center;
}
.tags-view-scroll {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
}
.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 8px;
  border: 1px solid #d8dce5;
  border-radius: 3px;
  font-size: 12px;
  color: #495060;
  cursor: pointer;
  transition: all 0.2s;
}
.tag-item:hover {
  border-color: #409eff;
  color: #409eff;
}
.tag-item.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.tag-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  font-size: 12px;
  line-height: 1;
}
.tag-item:hover .tag-close {
  background: rgba(255, 255, 255, 0.2);
}

.context-menu {
  position: fixed;
  z-index: 3000;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  font-size: 13px;
  color: #606266;
  min-width: 100px;
}

.context-menu li {
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.context-menu li:hover:not(.disabled) {
  background: #f5f7fa;
  color: #409eff;
}

.context-menu li.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}
</style>

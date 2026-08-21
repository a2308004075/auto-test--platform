<script setup lang="ts">
/**
 * 标签页栏
 * 读取 tagsView store，支持关闭/切换
 */
import { watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore, type TagView } from '@/stores'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

onMounted(() => {
  // 首次加载时添加当前路由
  addCurrentTag()
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
</style>

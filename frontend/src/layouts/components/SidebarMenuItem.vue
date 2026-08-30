<!--
 @author HXN
 @date 2026-08-22
 @description 递归菜单项组件
 目录（menuType=1）渲染为 el-sub-menu，菜单项（menuType=2）渲染为 el-menu-item，
 按钮（menuType=3）跳过。
-->
<script setup lang="ts">
import { computed } from 'vue'
import type { MenuTreeNode } from '@/api/menu'

const props = defineProps<{
  node: MenuTreeNode
  pathPrefix?: string
}>()

/** 子菜单按 sortNo 升序排序，sortNo 相同时按 id 升序 */
const sortedChildren = computed(() => {
  if (!props.node.children) return []
  return [...props.node.children].sort(
    (a, b) => (a.sortNo || 0) - (b.sortNo || 0) || a.id - b.id,
  )
})

/**
 * 菜单项 index
 * 项目菜单路径中的 :id 替换为实际项目 ID，
 * 与 default-active（实际路由路径）保持一致，确保高亮与父级链定位正确
 */
const itemIndex = computed(() => {
  const raw = props.node.routePath
  if (!raw) return ''
  return raw.includes(':id') ? raw.replace(':id', props.pathPrefix || '') : raw
})
</script>

<template>
  <!-- 目录 → el-sub-menu（有可见子项时渲染） -->
  <el-sub-menu
    v-if="node.menuType === 1 && sortedChildren.length"
    :index="node.name"
  >
    <template #title>
      <span>{{ node.name }}</span>
    </template>
    <SidebarMenuItem
      v-for="child in sortedChildren"
      :key="child.id"
      :node="child"
      :path-prefix="pathPrefix"
    />
  </el-sub-menu>

  <!-- 菜单项 → el-menu-item（跳过按钮类型 menuType=3） -->
  <el-menu-item
    v-else-if="node.menuType === 2 && node.routePath"
    :index="itemIndex"
  >
    <span>{{ node.name }}</span>
  </el-menu-item>
</template>

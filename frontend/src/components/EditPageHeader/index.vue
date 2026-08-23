<!--
 @author HXN
 @date 2026-08-23 15:00
 @description 编辑页统一页头组件
-->
<script setup lang="ts">
/**
 * 编辑/详情页统一页头
 * 左侧：← 返回按钮 + 标题
 * 右侧：操作区（slot）
 */
import { useRouter } from 'vue-router'

interface Props {
  /** 页面标题 */
  title: string
  /** 返回路由（可选，不传则 router.back()） */
  backRoute?: string
}

const props = defineProps<Props>()
const router = useRouter()

function handleBack() {
  if (props.backRoute) {
    router.push(props.backRoute)
  } else {
    router.back()
  }
}
</script>

<template>
  <div class="edit-page-header">
    <div class="edit-page-header-left">
      <el-button type="primary" link @click="handleBack">← 返回</el-button>
      <h2 class="edit-page-title">{{ title }}</h2>
    </div>
    <div class="edit-page-header-right">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.edit-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.edit-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.edit-page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.edit-page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>

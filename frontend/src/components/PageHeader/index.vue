<!--
 @author HXN
 @date 2026-08-21 15:30
 @description 页头组件
-->
<script setup lang="ts">
/**
 * 统一页头
 * 左侧标题 + 右侧操作区（slot）
 * 对齐原型列表页的 page-header（api-list.html 等）
 */
interface Props {
  /** 页面标题 */
  title: string
  /** 副标题（可选） */
  subtitle?: string
}

defineProps<Props>()
</script>

<template>
  <div class="pro-page-header">
    <div class="pro-page-header-left">
      <h1 class="pro-page-title">{{ title }}</h1>
      <p v-if="subtitle" class="pro-page-subtitle">{{ subtitle }}</p>
    </div>
    <div class="pro-page-header-right">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.pro-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pro-page-header-left {
  min-width: 0;
}
.pro-page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
}
.pro-page-subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
}
.pro-page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>

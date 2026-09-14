<!--
 @author HXN
 @date 2026-08-22 13:28
 @description 灵活查询表单组件
-->
<script setup lang="ts">
/**
 * 灵活查询表单
 * 对标 svc-manager-web 的 FlexQueryForm 组件
 * flex 布局容器，默认插槽放查询字段，#button 插槽放操作按钮
 */
</script>

<template>
  <div class="flex-query-form">
    <div class="flex-query-form-fields">
      <slot />
    </div>
    <div class="flex-query-form-actions">
      <slot name="button" />
    </div>
  </div>
</template>

<style scoped>
.flex-query-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 16px;
}

.flex-query-form-fields {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 16px;
  flex: 1;
}

.flex-query-form-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.flex-query-form-actions :deep(.el-button) {
  margin-left: 0;
}

.flex-query-form-fields :deep(.el-form-item) {
  margin-bottom: 0;
}
</style>

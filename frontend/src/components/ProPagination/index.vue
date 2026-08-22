<!--
 @author HXN
 @date 2026-08-21 15:30
 @description 增强分页组件
-->
<script setup lang="ts">
/**
 * 智能分页组件
 * 统一封装 el-pagination：总数 + 每页条数 + 智能页码(省略号) + 跳页输入
 * 对齐原型列表页的分页布局（api-list.html 等）
 */
interface Props {
  currentPage: number
  pageSize: number
  total: number
  pageSizes?: number[]
  layout?: string
  background?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  pageSizes: () => [10, 20, 50, 100],
  layout: 'total, sizes, prev, pager, next, jumper',
  background: true,
})

const emit = defineEmits<{
  (e: 'update:currentPage', v: number): void
  (e: 'update:pageSize', v: number): void
  (e: 'change', page: number, size: number): void
}>()

function onSizeChange(size: number) {
  emit('update:pageSize', size)
  emit('update:currentPage', 1)
  emit('change', 1, size)
}

function onCurrentChange(page: number) {
  emit('update:currentPage', page)
  emit('change', page, props.pageSize)
}
</script>

<template>
  <div class="pro-pagination">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :page-sizes="pageSizes"
      :layout="layout"
      :background="background"
      @size-change="onSizeChange"
      @current-change="onCurrentChange"
    />
  </div>
</template>

<style scoped>
.pro-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

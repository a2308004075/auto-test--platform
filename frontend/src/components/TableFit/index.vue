<!--
 @author HXN
 @date 2026-08-22 13:28
 @description 表格自适应高度容器组件
-->
<script setup lang="ts">
/**
 * 表格自适应高度容器
 * 对标 svc-manager-web 的 ElTableFit 组件
 * 通过 ResizeObserver 监听容器高度，将 maxHeight 通过 scoped slot 传给 el-table
 */
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'

const wrapperRef = ref<HTMLElement>()
const maxHeight = ref<number | undefined>(undefined)

let observer: ResizeObserver | null = null

function updateHeight() {
  if (wrapperRef.value) {
    const h = wrapperRef.value.clientHeight
    if (h > 0) {
      maxHeight.value = h
    }
  }
}

onMounted(() => {
  nextTick(updateHeight)
  observer = new ResizeObserver(updateHeight)
  if (wrapperRef.value) {
    observer.observe(wrapperRef.value)
  }
})

onBeforeUnmount(() => {
  observer?.disconnect()
})
</script>

<template>
  <div ref="wrapperRef" class="table-fit">
    <slot :maxHeight="maxHeight" />
  </div>
</template>

<style scoped>
.table-fit {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>

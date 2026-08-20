<script setup lang="ts">
/**
 * 面包屑组件
 * 基于 useRoute 读取 route.meta.title 渲染
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const items = computed(() => {
  const title = (route.meta?.title as string) || ''
  return title ? [{ title }] : []
})
</script>

<template>
  <a-breadcrumb v-if="items.length" class="app-breadcrumb">
    <a-breadcrumb-item v-for="(item, idx) in items" :key="idx">
      {{ item.title }}
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<style scoped>
.app-breadcrumb {
  display: inline-flex;
  align-items: center;
  font-size: 14px;
}
</style>

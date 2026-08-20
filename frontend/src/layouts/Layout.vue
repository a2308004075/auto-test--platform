<script setup lang="ts">
/**
 * 主布局编排容器
 * Sidebar + 主区（Navbar + TagsView + AppMain）
 * 参考 svc-manager-web 的 CSS Grid 布局方案
 */
import { computed } from 'vue'
import { useAppStore } from '@/stores'
import { Sidebar, Navbar, AppMain, TagsView } from './components'

const appStore = useAppStore()

const classObj = computed(() => ({
  'hide-sidebar': !appStore.sidebarOpened,
  'open-sidebar': appStore.sidebarOpened,
}))
</script>

<template>
  <div :class="classObj" class="app-container">
    <Sidebar class="sidebar-container" />
    <div class="main-container">
      <Navbar />
      <TagsView />
      <div class="app-main-wrapper">
        <AppMain />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
@import '@/styles/variables.less';

.app-container {
  position: relative;
  height: 100%;
  width: 100%;
  display: grid;
  grid-template-rows: 100%;
  grid-template-columns: auto 1fr;
}

.main-container {
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-main-wrapper {
  flex: 1;
  min-height: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: auto;
  background-color: var(--background-color-base);
}
</style>

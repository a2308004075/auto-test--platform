<!--
 @author HXN
 @date 2026-08-20 23:57
 @description 主布局组件
-->
<script setup lang="ts">
/**
 * 主布局编排容器
 * Sidebar + 主区（Navbar + TagsView + AppMain）
 * 参考 svc-manager-web 的 CSS Grid 布局方案
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores'
import { Sidebar, Navbar, AppMain, TagsView } from './components'

const route = useRoute()
const appStore = useAppStore()

const classObj = computed(() => ({
  'hide-sidebar': !appStore.sidebarOpened || route.meta?.noSidebar === true,
  'open-sidebar': appStore.sidebarOpened && route.meta?.noSidebar !== true,
  'no-sidebar': route.meta?.noSidebar === true,
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

/* 无侧边栏布局（首页等页面） */
.no-sidebar {
  grid-template-columns: 0 1fr;

  .sidebar-container {
    width: 0 !important;
    overflow: hidden;
    opacity: 0;
    pointer-events: none;
  }
}
</style>

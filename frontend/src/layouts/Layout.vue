<script setup lang="ts">
/**
 * 主布局编排容器
 * Sidebar + 主区（Navbar + TagsView + AppMain）
 */
import { Sidebar, Navbar, AppMain, TagsView } from './components'
</script>

<template>
  <a-layout class="app-layout">
    <Sidebar />
    <a-layout>
      <a-layout-header class="app-header">
        <Navbar />
      </a-layout-header>
      <TagsView />
      <AppMain />
    </a-layout>
  </a-layout>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
}
.app-header {
  background: #fff;
  padding: 0;
  height: 48px;
  line-height: 48px;
}
</style>

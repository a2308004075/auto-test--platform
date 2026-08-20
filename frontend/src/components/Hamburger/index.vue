<script setup lang="ts">
/**
 * 折叠按钮
 * 触发 app store 的 toggleSideBar
 */
import { useAppStore } from '@/stores/modules/app'

const appStore = useAppStore()

function toggleClick() {
  appStore.toggleSideBar()
}
</script>

<template>
  <div class="hamburger" @click="toggleClick">
    <span class="hamburger-icon">
      <!-- 展开时显示收起图标 -->
      <svg
        v-if="appStore.sidebarOpened"
        viewBox="0 0 1024 1024"
        width="20"
        height="20"
        fill="currentColor"
      >
        <path d="M408 442h480a32 32 0 1 0 0-64H408a32 32 0 0 0 0 64zM136 184h-64a32 32 0 0 0 0 64h64a32 32 0 0 0 0-64z m0 640H72a32 32 0 0 0 0 64h64a32 32 0 1 0 0-64z m0-320H72a32 32 0 1 0 0 64h64a32 32 0 1 0 0-64z m776 96H408a32 32 0 1 0 0 64h480a32 32 0 0 0 0-64z" />
      </svg>
      <!-- 折叠时显示展开图标 -->
      <svg
        v-else
        viewBox="0 0 1024 1024"
        width="20"
        height="20"
        fill="currentColor"
      >
        <path d="M408 442h480a32 32 0 1 0 0-64H408a32 32 0 0 0 0 64zM136 506h-64a32 32 0 1 0 0 64h64a32 32 0 1 0 0-64z m0-322h-64a32 32 0 0 0 0 64h64a32 32 0 0 0 0-64z m0 640H72a32 32 0 0 0 0 64h64a32 32 0 1 0 0-64z m776-96H408a32 32 0 1 0 0 64h480a32 32 0 0 0 0-64z" />
      </svg>
    </span>
  </div>
</template>

<style scoped>
.hamburger {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  padding: 0 12px;
  height: 100%;
}
.hamburger-icon {
  color: #5a5e66;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}
.hamburger:hover .hamburger-icon {
  color: #409eff;
}
</style>

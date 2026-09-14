<!--
 @author HXN
 @date 2026-08-20 23:57
 @description 侧边栏折叠按钮组件
-->
<script setup lang="ts">
/**
 * 折叠按钮
 * 触发 app store 的 toggleSideBar
 * 参考 svc-manager-web 的旋转图标方案
 */
import { useAppStore } from '@/stores/modules/app'

const appStore = useAppStore()

function toggleClick() {
  appStore.toggleSideBar()
}
</script>

<template>
  <div class="hamburger" @click="toggleClick">
    <svg
      :class="{ 'is-active': appStore.sidebarOpened }"
      class="hamburger-icon"
      viewBox="0 0 1024 1024"
      xmlns="http://www.w3.org/2000/svg"
      width="64"
      height="64"
    >
      <path
        d="M408 442h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8zm-8 204c0 4.4 3.6 8 8 8h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56zm504-486H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zm0 632H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zM142.4 642.1L298.7 519a8.84 8.84 0 0 0 0-13.9L142.4 381.9c-5.8-4.6-14.4-.5-14.4 6.9v246.3a8.9 8.9 0 0 0 14.4 7z"
      />
    </svg>
  </div>
</template>

<style scoped>
.hamburger {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  padding: 0 15px;
  height: 100%;
}
.hamburger-icon {
  display: inline-block;
  vertical-align: middle;
  width: 20px;
  height: 20px;
  fill: currentColor;
  color: #5a5e66;
  transition: transform 0.3s, color 0.2s;
}
.hamburger-icon.is-active {
  transform: rotate(180deg);
}
.hamburger:hover .hamburger-icon {
  color: #409eff;
}
</style>

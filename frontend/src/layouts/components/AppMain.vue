<script setup lang="ts">
/**
 * 主内容区
 * a-layout-content + router-view + transition
 */
</script>

<template>
  <a-layout-content class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </a-layout-content>
</template>

<style scoped>
.app-main {
  margin: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 48px);
}
</style>

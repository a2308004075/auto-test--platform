<script setup lang="ts">
/**
 * 主内容区
 * 原生 main 标签 + router-view + transition
 */
</script>

<template>
  <main class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </main>
</template>

<style scoped>
.app-main {
  flex: 1;
  margin: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  min-height: 0;
}
</style>

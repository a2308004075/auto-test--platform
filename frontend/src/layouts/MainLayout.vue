<script setup lang="ts">
/**
 * 主布局组件
 * 包含侧边栏导航、顶部栏和内容区域
 */
</script>

<template>
  <a-layout class="main-layout">
    <a-layout-sider collapsible>
      <div class="logo">
        <h1>测试平台</h1>
      </div>
      <a-menu theme="dark" mode="inline">
        <a-menu-item key="project">
          <router-link to="/project">项目管理</router-link>
        </a-menu-item>
        <a-menu-item key="api">
          <router-link to="/api">接口管理</router-link>
        </a-menu-item>
        <a-menu-item key="keywords">
          <router-link to="/keywords">关键字管理</router-link>
        </a-menu-item>
        <a-menu-item key="cases">
          <router-link to="/cases">测试用例</router-link>
        </a-menu-item>
        <a-menu-item key="execution">
          <router-link to="/execution">执行管理</router-link>
        </a-menu-item>
        <a-menu-item key="settings">
          <router-link to="/settings">系统设置</router-link>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="layout-header">
        <span>关键字驱动测试管理平台</span>
      </a-layout-header>
      <a-layout-content class="layout-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo h1 {
  font-size: 16px;
  margin: 0;
  white-space: nowrap;
}

.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.layout-content {
  margin: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
}
</style>

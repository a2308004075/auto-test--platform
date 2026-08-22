/**
 * @author HXN
 * @date 2026-08-18 17:31
 * @description 应用入口文件
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import router from './router'
import App from './App.vue'
import './styles/global.less'
import { setUnauthorizedHandler } from './api/request'
import { useUserStore, useProjectStore, useTagsViewStore } from './stores'
import dragDialog from '@/directives/drag'
import vPermission from '@/directives/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn, size: 'default' })

// 注册全局弹窗拖拽指令（对标 svc-manager-web 的 v-el-drag-dialog）
app.directive('drag-dialog', dragDialog)

// 注册全局权限控制指令（display 模式隐藏 / click 模式禁用）
app.directive('permission', vPermission)

// 注册 401 / 服务不可用时的全局清理回调，同步清空用户相关 store 状态
setUnauthorizedHandler(() => {
  const userStore = useUserStore()
  const projectStore = useProjectStore()
  const tagsViewStore = useTagsViewStore()
  userStore.logout()
  projectStore.clearCurrentProject()
  tagsViewStore.delAllViews()
})

app.mount('#app')

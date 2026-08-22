/**
 * @author HXN
 * @date 2026-08-22
 * @description 权限控制指令
 */
import type { Directive } from 'vue'
import { useUserStore } from '@/stores'

/**
 * 权限控制指令（按角色 control_mode）
 *
 * <p>control_mode 存储在 role_permission 表上，按角色独立配置：
 * <ul>
 *     <li><b>enabled</b>（默认）：有权限时按钮显示且可点击</li>
 *     <li><b>disabled</b>：有权限时按钮显示但禁用（显示禁点击）</li>
 *     <li>不在 permissionDetails 中：无权限，按钮从 DOM 中移除（隐藏）</li>
 * </ul>
 *
 * <h3>用法：</h3>
 * <pre>
 *   <!-- 默认模式：根据角色 control_mode 决定显示/禁用/隐藏 -->
 *   <el-button v-permission="'system:user:add'">新建用户</el-button>
 * </pre>
 *
 * <p>ADMIN 角色隐式拥有全部权限（后端返回 ["*"]），所有按钮均显示且可点击。
 */

interface PermissionEl extends HTMLElement {
  _permCleanup?: () => void
}

function getPermissionDetail(code: string) {
  const userStore = useUserStore()
  if (userStore.isAdmin) return { hasPermission: true, controlMode: 'enabled' }
  const detail = userStore.permissionDetails.find((d) => d.code === code)
  if (!detail) return { hasPermission: false, controlMode: null }
  return { hasPermission: true, controlMode: detail.controlMode || 'enabled' }
}

const vPermission: Directive<PermissionEl, string> = {
  mounted(el, binding) {
    const code = binding.value
    if (!code) return

    const { hasPermission, controlMode } = getPermissionDetail(code)

    if (!hasPermission) {
      // 无权限：从 DOM 中移除（隐藏）
      el.parentNode?.removeChild(el)
    } else if (controlMode === 'disabled') {
      // 有权限但禁用：设置 disabled 并阻止点击
      el.setAttribute('disabled', 'disabled')
      el.classList.add('is-disabled')
      el.style.pointerEvents = 'none'
      el.title = '暂无操作权限'

      // 阻止所有点击事件
      const blockHandler = (e: Event) => {
        e.stopPropagation()
        e.preventDefault()
      }
      el.addEventListener('click', blockHandler, true)
      el._permCleanup = () => {
        el.removeEventListener('click', blockHandler, true)
      }
    }
    // else: 有权限且 enabled → 正常显示，无需处理
  },

  unmounted(el) {
    el._permCleanup?.()
  },
}

export default vPermission
